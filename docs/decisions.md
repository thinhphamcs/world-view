# Decisions

Why things are the way they are. Newest at the bottom. Each entry: what was decided, why, and what it means for the code.

---

## 1. Single Kafka broker, internal-topic replication factor 1

**Branch:** `feature/kafka-producer` (end-to-end check)

- **Decision:** run one KRaft broker and set the internal-topic replication factor (e.g. `__consumer_offsets`) to 1 in `kafka/server.properties`.
- **Why:** the default of 3 can't be met by a single broker. Consumer groups then fail with `FIND_COORDINATOR` timeouts and nothing gets consumed.
- **Consequence:** fine for local development. Not a production-grade Kafka setup (no redundancy), which matches the plan to run Kafka locally for learning (see [deployment.md](deployment.md)).

## 2. No images

**Branch:** `feature/expand-entity`

- **Decision:** don't store or display images from feeds. The dashboard is text-first: headline, summary, source, time, link back to the original.
- **Why:** feed photos are often licensed from agencies (AP, Getty, Reuters), so the publisher can't pass on rights to them. Hotlinking is a legal gray area (Perfect 10 v. Amazon vs. Goldman v. Breitbart); re-hosting is clearly worse. Images also add nothing for the voice client (decision 3), and they make parsing messier (enclosures, `media:` tags, `<img>` inside HTML).
- **Consequence:** no `imageUrl` field. If a screen-based client ever needs one, it's a single nullable column to add later. A source badge/label can provide visual variety instead.

## 3. Client-agnostic API, plain-text summaries

**Branch:** `feature/expand-entity`

- **Decision:** world-view is a news backend. The Angular dashboard is the first client; a home voice assistant ("what's the news?") is a planned second one. Summaries are stored as **plain text**, with HTML stripped server-side.
- **Why:** text-to-speech would read markup and entities aloud (`<p>`, "amp"). Cleaning once in the backend means every client gets clean text.
- **Consequence:** `RssEntryMapper` runs the description through jsoup (decision 5). The REST API (roadmap step 5) should return clean JSON with paging/limit, not something shaped only for Angular.

## 4. `title` and `summary` are required, stored as `TEXT NOT NULL`

**Branch:** `feature/expand-entity`

- **Decision:** an entry needs **both** a title and a summary; null or blank counts as missing. Both columns are `TEXT NOT NULL`.
- **Why required:** a briefing that is sometimes headline + story and sometimes just a headline is choppy, and a summary without a headline gives the dashboard nothing to show. "Blank" matters because a description containing only `<img>` becomes `""` after HTML stripping.
- **Why `TEXT`:** in Postgres, `TEXT` and `VARCHAR(n)` are stored and perform the same. A length limit would only make `save()` throw on an unusually long value and lose the story. Storage depends on the actual content, not the type.
- **Where it's enforced:** `RssEntryDto.isComplete()` is the single rule. The **scheduler** skips incomplete entries with a warning before they reach Kafka; the **consumer** checks again (old messages from before the change, or any other producer). The `NOT NULL` constraint is only the last safety net. `isComplete()` is `@JsonIgnore`d so Jackson doesn't add a `"complete"` field to the JSON.
- **Consequence:** the existing dev rows had no title, and Postgres can't add a `NOT NULL` column to a table that already has rows, so the dev table was dropped. The poller refills it.

## 5. jsoup for HTML → text

**Branch:** `feature/expand-entity`

- **Decision:** use jsoup's `Jsoup.parse(html).text()` instead of a regex.
- **Why:** a regex like `<[^>]*>` doesn't decode entities (`&amp;` stays) and breaks on tricky HTML. jsoup is the standard Java HTML parser: mature, MIT-licensed, no transitive dependencies. It also collapses whitespace and keeps words apart across block elements.
- **Consequence:** one new dependency (`org.jsoup:jsoup`). `toPlainText` must handle a `null` value itself, because `Jsoup.parse(null)` throws.

## 6. Kafka/direct switch: deliberately not built yet

**Branch:** `feature/expand-entity` (discussion)

- **Decision:** don't add a `kafka` vs `direct` feature flag now.
- **Why:** it's only needed if the cost review picks Hybrid or no-Kafka production (see [deployment.md](deployment.md)). Building it now means two code paths to test before there's any benefit (YAGNI). It stays cheap to add later, because only `RssEntryProducer` and `RssEntryConsumer` know about Kafka.
- **Consequence:** when needed: a property like `world-view.messaging: kafka | direct` plus `@ConditionalOnProperty`. The rough refactor plan is in [deployment.md](deployment.md).

## 7. Context test uses a `test` profile (no side effects)

**Branch:** `chore/test-profile`

- **Decision:** keep `WorldViewApplicationTests` (`contextLoads`) and run it with a `test` profile: scheduling off (`world-view.scheduling.enabled: false`, read by `@ConditionalOnProperty` on `SchedulingConfig`), Kafka listener `auto-startup: false`, and `ddl-auto: validate` against the dev Postgres.
- **Why keep the test:** unit tests build classes by hand, so they never catch wiring bugs: a typo in a repository method name, a bad entity mapping, a bean Spring can't create, config that doesn't bind. Only starting the context does.
- **Why the profile:** before, the test ran the real scheduler (polled NPR and sent 10 real Kafka messages), started the consumer, and could change the dev schema (`ddl-auto: update`). The side effects happened on background threads *after* the test passed, so they were easy to miss.
- **Why dev Postgres + `validate`, not H2 or Testcontainers:** zero new dependencies, real Postgres. `validate` only reads the schema, and with the scheduler and listener off nothing writes. H2 is a fake Postgres (the entity uses `columnDefinition = "TEXT"`). Testcontainers is the better tool for real repository tests later, and can be decided then.
- **Consequence:** the test needs Postgres up and `.env` (`./run_test.sh`), and the `rss_entry` table must exist, so on a fresh Docker volume run the backend once first. Kafka isn't needed. As a bonus, an entity change that the dev table doesn't have yet now fails the test instead of silently altering the table. Checked by breaking it on purpose: `enabled: true` brings back a `[scheduling-1]` fetch; renaming a column to `titlex` fails with `Schema validation: missing column`.

## 8. JaCoCo coverage report: a tool, not a target

**Branch:** `chore/jacoco`

- **Decision:** add `jacoco-maven-plugin` (build plugin only, not in the app jar) with two executions: `prepare-agent` (attaches the recorder to the test JVM via Surefire's `argLine`) and `report` bound to the **`test`** phase, so `./mvnw test` writes `target/site/jacoco/index.html`. No minimum-coverage check (`jacoco:check`) that fails the build.
- **Why:** the report is for **finding untested code** (red lines). A target number invites tests written to turn lines green rather than to check behaviour, and green only means "ran", not "asserted". `report` defaults to the `verify` phase, which `./mvnw test` never reaches, hence the explicit phase.
- **Consequence:** the Spring Boot parent doesn't manage JaCoCo, so its version is pinned explicitly in `pom.xml` (verified on Maven Central). The first report found `RssFeedService.fetchLatestEntries` untested; it's now tested without the network by pointing `NprFeedProperties` at the fixture via a `file:` URL. Remaining red is deliberate: `main()`, `SchedulingConfig` (off in the test profile, decision 7), `RssEntry.getId()` (used once the REST API exists).

