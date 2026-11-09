# Developer cheatsheet

Commands used while building world-view, with a short note on why. Unless noted, run them from the repo root. Credentials come from the containers' own environment (`.env`), so nothing secret appears here.

## Git

### Daily workflow

```bash
git switch dev                  # go to dev
git pull                        # fetch + merge origin/dev into the CURRENT branch
git switch -c feature/my-thing  # start a new branch from the fresh dev
git branch --show-current       # which branch am I on?
```

Uncommitted changes don't belong to any branch yet: they move with you on `git switch`. Handy when you started work on `dev` by accident: `git switch -c feature/my-thing` takes the changes along, and `dev` stays clean.

### fetch vs pull

- `git fetch` downloads new commits into `origin/*` (e.g. `origin/dev`) but **does not touch** your local branches or files. Use it to look before merging.
- `git pull` = `git fetch` + merge into the **current** branch. On a feature branch, `git pull` pulls that branch's remote, **not** `dev`.

```bash
git fetch --prune                   # download new commits; --prune drops origin/* copies of branches deleted on GitHub
git status                          # "behind 'origin/dev' by N commits"
git log --oneline dev..origin/dev   # which commits are new ("on origin/dev but not on my dev")
git diff --stat dev..origin/dev     # which files they change, and how many lines
```

What `git status` tells you after a fetch:

| Message | Meaning | Do |
|---|---|---|
| `behind 'origin/dev' by N commits, and can be fast-forwarded` | GitHub has N commits you don't; you have none of your own | `git pull` is safe, no conflicts possible |
| `ahead of 'origin/dev' by N commits` | you have N commits not pushed yet | `git push` |
| `have diverged` | both sides have commits the other doesn't | stop and look (`git log`) before pulling |
| `up to date` | nothing to do | |

### After a PR is merged on GitHub

The merge happened on GitHub, so your local `dev` doesn't have it yet.

```bash
git fetch --prune
git switch dev
git status                          # expect "behind ... can be fast-forwarded"
git log --oneline dev..origin/dev   # your commits + "Merge pull request #N"
git pull
git branch -d feature/my-thing      # delete the local branch
git switch -c chore/next-thing      # start the next one from the fresh dev
```

- If `git switch` refuses ("your local changes would be overwritten"), put them aside first: `git stash` → do the flow → `git stash pop` on the new branch. `git stash list` shows what's stashed.
- `git branch -d` (lowercase) is safe: it **refuses** if the branch has unmerged commits. `-D` (uppercase) forces the delete. Avoid it unless you're sure.
- Also click **"Delete branch"** on the merged PR on GitHub; the next `git fetch --prune` then removes its `origin/` copy too.

### Small commits, named files

Never `git add .` or `git add -A`: they sweep in files you didn't mean to commit. Stage by name, check, then commit. One concern per commit.

```bash
git status
git add path/to/File.java path/to/FileTest.java
git diff --staged --stat            # only the files you meant?
git diff --staged                   # the actual lines
git commit -m "Feature: short summary" -m "- optional detail lines"
git log --oneline -5
git push -u origin feature/my-thing # first push of a new branch
```

Checking before you commit. Any of these is fine; what matters is looking:

| Command | Shows |
|---|---|
| `git status` | **which** files are staged ("Changes to be committed") and which aren't |
| `git diff --staged --stat` | staged files **plus how many lines** each one changes. A surprise number = wrong file or editor reformatting |
| `git diff --staged` | the actual staged **lines** |

- `git diff <file>`: what changed in a file and isn't staged yet (red `-` = old lines). Handy to recover what a file looked like before your edits.
- `git add -p <file>`: stage only *parts* of a file, hunk by hunk. Useful when one file contains two concerns.

Commit prefixes used in this repo: `Feature:`, `Fix:`, `Chore:`.

## Docker

```bash
docker compose up -d                # start Postgres + Kafka in the background
docker compose ps                   # what's running
docker compose logs -f kafka        # follow a container's logs (Ctrl+C to stop)
docker compose stop                 # stop containers, keep data
docker compose down                 # remove containers, keep data (volumes)
```

> ⚠️ `docker compose down -v` also deletes the **volumes**, i.e. all Postgres data and Kafka messages.

`docker compose exec <service> <command>` runs a command **inside** a running container. `<service>` is the name from `docker-compose.yml` (`postgres`, `kafka`).

**Docker Desktop alternative:** Containers → expand `world-view` → click a container name → **Exec** tab gives a shell inside it (then skip the `docker compose exec <service>` part of the commands below). **Logs** and **Files** tabs sit next to it.

## Postgres

Open an interactive `psql` session:

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

The single quotes make the container's shell fill in `$POSTGRES_USER` / `$POSTGRES_DB`. Inside `psql`:

| Command | Does |
|---|---|
| `\dt` | list tables |
| `\d rss_entry` | describe a table (columns, types, nullable, indexes) |
| `SELECT ... ;` | run SQL. Don't forget the `;` or psql waits for more |
| `\q` | quit |

Useful queries:

```sql
SELECT count(*) FROM rss_entry;

SELECT id, left(title, 50) AS title, left(summary, 60) AS summary
FROM rss_entry ORDER BY published_at DESC LIMIT 5;
```

One-off command without the interactive session: add `-c "..."`:

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\d rss_entry"'
```

Wipe the dev table (Hibernate recreates it from the entity on next start, `ddl-auto: update`):

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DROP TABLE rss_entry;"'
```

Notes:
- Gaps in `id` are normal: the sequence uses `allocationSize = 50`, so each app start reserves a new block of ids.
- `TEXT` in Postgres has no length limit and costs the same as `VARCHAR`.

## Kafka

Kafka's CLI tools live in `/opt/kafka/bin/` inside the container.

```bash
# list topics
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# consumer group: committed offsets and lag ("how far has the app read?")
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group world-view-consumer-group

# read messages: the last 2 in the topic
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic rss-entries --from-beginning --timeout-ms 5000 | tail -n 2
```

- The console consumer uses its own temporary group, so it **doesn't move** the app's offsets.
- It ends with `Processed a total of N messages` and a `TimeoutException`. Both are normal (`--timeout-ms` = stop after 5 s of silence).
- **Offsets:** the app's consumer group remembers how far it has read. On restart it continues after that point, so old messages are not re-read unless the group id changes or offsets are reset.

## Maven (backend)

Run from `backend/world-view`. Always `./mvnw` (project-pinned, checksum-verified), never a global `mvn`.

```bash
./mvnw test                               # all tests
./mvnw test -Dtest=RssEntryMapperTest     # one test class
./mvnw test -Dtest='RssEntryDtoTest,RssPollingSchedulerTest'  # several
./mvnw clean test                         # wipe target/ first, then compile + test
./mvnw clean test-compile                 # just check that everything compiles
```

- `-Dtest` only filters which tests **run**. Maven still **compiles every** test file, so one broken test file blocks all of them.
- Use `clean` when a result looks suspiciously good (e.g. "Nothing to compile" right after changing a constructor): VS Code compiles in the background and its class files can make Maven skip compiling.
- `WorldViewApplicationTests` starts the real app with the `test` profile: needs Postgres up (and `.env`, so use `./run_test.sh`), no Kafka.
- Reading a test log: the thread name in `[...]` tells you who logged. `[main]` = the test itself (e.g. unit tests calling `checkForNewEntries()` with mocks); `[scheduling-1]` = the real `@Scheduled` scheduler. Background threads can log **after** a test finishes, so their lines show up in the terminal but not in `target/surefire-reports/`.

```bash
./run_test.sh 2>&1 | grep -E 'profile is active|scheduling-1'   # check the context test stays quiet
```

## Reading test failures

`expected: <A> but was: <B>`: **A** is what the test asserted, **B** is what the code returned. In `assertEquals(expected, actual)` the expected value always comes first.

Mockito, strict stubs (`MockitoExtension` default): a `when(...)` / `doThrow(...)` that the code never uses fails the test with `UnnecessaryStubbingException`. Usually a copy-paste leftover, or the test is wrong about the code path.
