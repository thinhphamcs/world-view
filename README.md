# world-view

## 1. What this project is

An ad-free, paywall-free news dashboard. RSS news, weather alerts, and trending topics, plus an audio/radio player, all in one place. The backend is a client-agnostic news API: the Angular dashboard is the first client, a home voice assistant is a planned second one.

## 2. Tech stack

| Layer      | Tech                                                     |
|------------|----------------------------------------------------------|
| Backend    | Spring Boot 4.1, Java 17, Maven                          |
| RSS parse  | Rome 2.1.0                                               |
| HTML→text  | jsoup 1.23.2                                             |
| Messaging  | Apache Kafka 4.3.1 (KRaft mode, single node, no ZooKeeper) |
| Database   | PostgreSQL 16 via Spring Data JPA / Hibernate            |
| Frontend   | Angular 21 (SCSS, no SSR), Vitest for tests              |
| Infra      | Docker Compose                                           |

## 3. Data flow

```
 NPR RSS feed
      │  (on startup, then every 15 min)
      ▼
 RssPollingScheduler ──► RssFeedService.fetchLatestEntries()     ✅ done
      │
      ▼
 RssEntryMapper: SyndEntry → RssEntryDto                          ✅ done
      │  (description HTML → plain text; incomplete entries skipped)
      ▼
 RssEntryProducer → Kafka topic "rss-entries" (key = link)        ✅ done
      │
      ▼
 RssEntryConsumer (@KafkaListener)                                ✅ done
      │
      ▼
 RssEntryRepository → Postgres table rss_entry                    ✅ done
      │
      ▼
 REST API → Angular dashboard                                     ❌ not started
```

## 4. Docs

| File | What's in it |
|---|---|
| [docs/cheatsheet.md](docs/cheatsheet.md) | Git, Docker, Postgres, Kafka and Maven commands, with explanations |
| [docs/decisions.md](docs/decisions.md) | Why things are the way they are (no images, `TEXT NOT NULL`, required fields, jsoup, ...) |
| [docs/deployment.md](docs/deployment.md) | Hosting options, cost estimates, Hybrid plan, budget |
| [docs/configuration.md](docs/configuration.md) | `application.yaml` keys explained |
| [docs/conventions.md](docs/conventions.md) | Test naming convention |

## 5. Repository layout

```
world-view/
├── README.md                  # this file
├── docker-compose.yml         # postgres + kafka
├── .env                       # NOT committed — POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB
├── kafka/
│   ├── server.properties      # KRaft broker+controller, PLAINTEXT on 9092, controller on 9093,
│   │                          #   internal topic replication factor 1 (single broker)
│   └── kafka-entrypoint.sh    # formats storage on first boot, then starts the broker
├── docs/                      # see section 4
├── backend/world-view/
│   ├── pom.xml
│   ├── mvnw, .mvn/wrapper/    # Maven wrapper: downloads a pinned, checksum-verified Maven (no global install needed)
│   ├── run_backend.sh         # loads ../../.env then `./mvnw spring-boot:run`
│   ├── run_test.sh            # loads ../../.env then `./mvnw test`
│   └── src/
│       ├── main/java/net/thinhpham/world_view/
│       │   ├── WorldViewApplication.java
│       │   ├── config/
│       │   │   ├── NprFeedProperties.java    # binds npr.feed.url
│       │   │   └── SchedulingConfig.java     # @EnableScheduling
│       │   ├── constant/KafkaTopics.java     # topic names shared by producer and consumer
│       │   ├── mapper/RssEntryMapper.java    # SyndEntry → RssEntryDto (date fallback, HTML → plain text)
│       │   ├── service/
│       │   │   ├── RssFeedService.java       # fetch + parse feed with Rome
│       │   │   ├── RssPollingScheduler.java  # @Scheduled every 15 min: fetch, map, skip incomplete, send
│       │   │   ├── RssEntryProducer.java     # sends RssEntryDto to rss-entries, key = link
│       │   │   └── RssEntryConsumer.java     # listens on rss-entries, skips incomplete/duplicate, saves new entries
│       │   ├── dto/RssEntryDto.java          # record: link, title, summary, publishedAt, sourceName + isComplete()
│       │   ├── entity/RssEntry.java          # JPA entity, table rss_entry
│       │   └── repository/RssEntryRepository.java  # JpaRepository<RssEntry, Long> + existsByLink
│       ├── main/resources/application.yaml   # see docs/configuration.md
│       └── test/
│           ├── java/.../WorldViewApplicationTests.java   # contextLoads (needs live DB + Kafka)
│           ├── java/.../dto/RssEntryDtoTest.java
│           ├── java/.../mapper/RssEntryMapperTest.java
│           ├── java/.../service/RssEntryConsumerTest.java
│           ├── java/.../service/RssEntryProducerTest.java
│           ├── java/.../service/RssFeedServiceTest.java
│           ├── java/.../service/RssPollingSchedulerTest.java
│           └── resources/sample-feed-only-2.xml          # 2-entry NPR fixture
└── frontend/                  # bare `ng new` scaffold — no features yet
```

## 6. Running locally

Prerequisites: Java 17, Docker Desktop (running), Node for the frontend. Maven is **not** required. The scripts use `./mvnw`, which downloads the Maven version pinned in `.mvn/wrapper/maven-wrapper.properties` and verifies its SHA-256.

1. Create `.env` at the repo root:
   ```
   POSTGRES_USER=...
   POSTGRES_PASSWORD=...
   POSTGRES_DB=...
   ```
2. Start the infrastructure from the repo root:
   ```bash
   docker compose up -d
   ```
   Postgres runs on `localhost:5432` and Kafka on `localhost:9092`.
3. Start the backend:
   ```bash
   cd backend/world-view
   ./run_backend.sh
   ```
   On startup it polls NPR right away. Look for `Sent N entries to Kafka, skipped M incomplete` and `Processing new entry with link ...` in the log.
4. Run the backend tests. Docker must be up for `contextLoads`.
   ```bash
   cd backend/world-view
   ./run_test.sh
   ./mvnw test -Dtest=RssEntryMapperTest    # a single test class (no Docker needed for unit tests)
   ```
   Note: `contextLoads` starts the whole app, so its scheduler also fetches NPR and sends real messages to Kafka.
5. Start the frontend:
   ```bash
   cd frontend
   npm install
   npm start    # http://localhost:4200
   ```

To check the data in Postgres and Kafka, see [docs/cheatsheet.md](docs/cheatsheet.md). If port 8080 is already in use, find the process with `lsof -nP -iTCP:8080 -sTCP:LISTEN`.

## 7. Roadmap

Rough order. Each item maps to one branch.

1. ✅ **`feature/kafka-consumer`**: listen on `rss-entries`, save new entries, skip duplicate links.
2. ✅ **`feature/kafka-producer`**: mapper (date fallback published → updated → fetch time) and producer (key = link).
3. ✅ **End-to-end check**: rows land in `rss_entry` (needed Kafka replication factor 1, see [decisions](docs/decisions.md#1-single-kafka-broker-internal-topic-replication-factor-1)). Still to observe: `already exists` logs on a repeat poll.
4. ✅ **`feature/expand-entity`**: `title` + `summary` (`TEXT NOT NULL`, both required), HTML stripped with jsoup, incomplete entries skipped in scheduler and consumer. No images. See [decisions 2–5](docs/decisions.md).
   - ✅ **`chore/docs-split`**: cheatsheet; decisions, deployment and configuration moved to `docs/`.
   - **Chore: test profile** for `WorldViewApplicationTests`, so the context test doesn't connect to dev Postgres, change the schema, or poll NPR and send real Kafka messages (confirmed it does today).
   - **Chore: JaCoCo coverage report** (`target/site/jacoco/index.html`). For finding untested code (red lines), not as a target number.
5. **REST API**: e.g. `GET /api/entries?page=…&limit=…`, newest first, client-agnostic JSON.
6. **Frontend**: news list page in Angular, text-first (headline, summary, source badge, time).
7. **More sources**: more RSS feeds (make `NprFeedProperties` a list), then weather alerts, trending topics, audio/radio player. Possibly topic tags from `entry.getCategories()`.
8. **Later: voice assistant client** that reads news briefings aloud (why summaries are plain text, see [decision 3](docs/decisions.md#3-client-agnostic-api-plain-text-summaries)).
9. **Before deploying: cost review**. Budget ~$20–30/year. Deciding question: is there a home machine with 4–8 GB RAM? Yes → Kafka at home; no → Hybrid. See [docs/deployment.md](docs/deployment.md).
