# world-view

## 1. What this project is

An ad-free, paywall-free news dashboard. RSS news, weather alerts, and trending topics, plus an audio/radio player, all in one place.

## 2. Tech stack

| Layer      | Tech                                                     |
|------------|----------------------------------------------------------|
| Backend    | Spring Boot 4.1, Java 17, Maven                          |
| RSS parse  | Rome 2.1.0                                               |
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
      │
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

## 4. Repository layout

```
world-view/
├── README.md                  # this file
├── docker-compose.yml         # postgres + kafka
├── .env                       # NOT committed — POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB
├── kafka/
│   ├── server.properties      # KRaft broker+controller, PLAINTEXT on 9092, controller on 9093,
│   │                          #   internal topic replication factor 1 (single broker)
│   └── kafka-entrypoint.sh    # formats storage on first boot, then starts the broker
├── docs/
│   └── conventions.md         # test naming convention
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
│       │   ├── mapper/RssEntryMapper.java    # SyndEntry → RssEntryDto (published → updated → fetch time)
│       │   ├── service/
│       │   │   ├── RssFeedService.java       # fetch + parse feed with Rome
│       │   │   ├── RssPollingScheduler.java  # @Scheduled every 15 min: fetch, map, send; one failed send doesn't stop the rest
│       │   │   ├── RssEntryProducer.java     # sends RssEntryDto to rss-entries, key = link
│       │   │   └── RssEntryConsumer.java     # listens on rss-entries, saves new entries, skips duplicate links
│       │   ├── dto/RssEntryDto.java          # record: link, publishedAt, sourceName
│       │   ├── entity/RssEntry.java          # JPA entity, table rss_entry
│       │   └── repository/RssEntryRepository.java  # JpaRepository<RssEntry, Long> + existsByLink
│       ├── main/resources/application.yaml
│       └── test/
│           ├── java/.../WorldViewApplicationTests.java   # contextLoads (needs live DB + Kafka)
│           ├── java/.../mapper/RssEntryMapperTest.java
│           ├── java/.../service/RssEntryConsumerTest.java
│           ├── java/.../service/RssEntryProducerTest.java
│           ├── java/.../service/RssFeedServiceTest.java
│           ├── java/.../service/RssPollingSchedulerTest.java
│           └── resources/sample-feed-only-2.xml          # 2-entry NPR fixture
└── frontend/                  # bare `ng new` scaffold — no features yet
```

## 5. Running locally

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
   On startup it polls NPR right away. Look for `Sent N entries to Kafka` and `Processing new entry with link ...` in the log.
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

### Checking the data

```bash
# rows saved in Postgres (interactive: drop the -c "..." part, \q to quit)
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*) from rss_entry;"'

# Kafka: list topics, consumer group lag, raw messages
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group world-view-consumer-group
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic rss-entries --from-beginning --timeout-ms 5000
```

If port 8080 is already in use, find the process with `lsof -nP -iTCP:8080 -sTCP:LISTEN`.

## 6. Configuration (`application.yaml`)

| Key | Value | Notes |
|-----|-------|-------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/${POSTGRES_DB}` | creds from `.env` |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate creates/updates `rss_entry` automatically |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | |
| `spring.kafka.consumer.group-id` | `world-view-consumer-group` | |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` | new consumer group starts from the oldest message |
| `spring.kafka.consumer.value-deserializer` | `JacksonJsonDeserializer` | turns JSON messages into `RssEntryDto` |
| `spring.json.value.default.type` | `net.thinhpham.world_view.dto.RssEntryDto` | fallback type for messages without a type header |
| `spring.json.trusted.packages` | `net.thinhpham.world_view.dto` | only classes in this package may be deserialized |
| `spring.kafka.producer.value-serializer` | `JacksonJsonSerializer` | turns `RssEntryDto` into JSON before sending |
| `npr.feed.url` | `https://feeds.npr.org/1014/rss.xml` | NPR Politics feed |

## 7. Roadmap / next steps

Rough order. Each item maps to one feature branch.

1. ✅ **`feature/kafka-consumer`**: listen on `rss-entries`, save new entries to Postgres, skip duplicate links.
2. ✅ **`feature/kafka-producer`**: `RssEntryMapper` maps `SyndEntry` → `RssEntryDto` (date falls back from published → updated → fetch time, with a warning log). `RssEntryProducer` sends it to `rss-entries` with the link as key. JSON serializer configured, topic name in `KafkaTopics`.
3. ✅ **End-to-end check**: rows land in `rss_entry`. Required setting the Kafka internal-topic replication factor to 1, because the default of 3 can't be met by a single broker and consumer groups fail with `FIND_COORDINATOR` timeouts. Still to observe: `already exists` logs on a repeat poll.
4. **Expand the entity**: title, description/summary, maybe an image URL. The current DTO has no title, which the dashboard will need.
5. **REST API**: e.g. `GET /api/entries?page=…`, newest first.
6. **Frontend**: news list page in Angular, calling the API.
7. **More sources**: more RSS feeds (make `NprFeedProperties` a list of feeds), then weather alerts, trending topics, and the audio/radio player.
