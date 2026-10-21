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

## 3. Planned data flow

```
 NPR RSS feed
      │  (every 15 min)
      ▼
 RssPollingScheduler ──► RssFeedService.fetchLatestEntries()     ✅ done
      │
      ▼
 [producer] SyndEntry → RssEntryDto → Kafka topic "rss-entries"  ❌ not started
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
│   ├── server.properties      # KRaft broker+controller, PLAINTEXT on 9092, controller on 9093
│   └── kafka-entrypoint.sh    # formats storage on first boot, then starts the broker
├── docs/
│   ├── conventions.md         # test naming convention
│   └── runbook.md             # end-to-end run + test commands
├── backend/world-view/
│   ├── pom.xml
│   ├── run_backend.sh         # loads ../../.env then `mvn spring-boot:run`
│   ├── run_test.sh            # loads ../../.env then `mvn test`
│   └── src/
│       ├── main/java/net/thinhpham/world_view/
│       │   ├── WorldViewApplication.java
│       │   ├── config/
│       │   │   ├── NprFeedProperties.java    # binds npr.feed.url
│       │   │   └── SchedulingConfig.java     # @EnableScheduling
│       │   ├── service/
│       │   │   ├── RssFeedService.java       # fetch + parse feed with Rome
│       │   │   ├── RssPollingScheduler.java  # @Scheduled every 15 min, logs count
│       │   │   └── RssEntryConsumer.java     # listens on rss-entries, saves new entries, skips duplicate links
│       │   ├── dto/RssEntryDto.java          # record: link, publishedAt, sourceName
│       │   ├── entity/RssEntry.java          # JPA entity, table rss_entry
│       │   └── repository/RssEntryRepository.java  # JpaRepository<RssEntry, Long> + existsByLink
│       ├── main/resources/application.yaml
│       └── test/
│           ├── java/.../WorldViewApplicationTests.java   # contextLoads (needs live DB + Kafka)
│           ├── java/.../service/RssEntryConsumerTest.java
│           ├── java/.../service/RssFeedServiceTest.java
│           ├── java/.../service/RssPollingSchedulerTest.java
│           └── resources/sample-feed-only-2.xml          # 2-entry NPR fixture
└── frontend/                  # bare `ng new` scaffold — no features yet
```

## 5. Running locally

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
4. Run the backend tests. Docker must be up for `contextLoads`.
   ```bash
   cd backend/world-view
   ./run_test.sh
   ```
5. Start the frontend:
   ```bash
   cd frontend
   npm install
   npm start    # http://localhost:4200
   ```

## 6. Configuration (`application.yaml`)

| Key | Value | Notes |
|-----|-------|-------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/${POSTGRES_DB}` | creds from `.env` |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate creates/updates `rss_entry` automatically |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | |
| `spring.kafka.consumer.group-id` | `world-view-consumer-group` | |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` | new consumer group starts from the oldest message |
| `spring.kafka.consumer.value-deserializer` | `JacksonJsonDeserializer` | turns JSON messages into `RssEntryDto` |
| `spring.json.value.default.type` | `net.thinhpham.world_view.dto.RssEntryDto` | needed because messages carry no type headers |
| `spring.json.trusted.packages` | `net.thinhpham.world_view.dto` | only classes in this package may be deserialized |
| `npr.feed.url` | `https://feeds.npr.org/1014/rss.xml` | NPR Politics feed |

## 7. Roadmap / next steps

Rough order. Each item maps to one feature branch.

1. ✅ **`feature/kafka-consumer`**: listen on `rss-entries`, save new entries to Postgres, skip duplicate links.
2. **`feature/kafka-producer`**: in `RssPollingScheduler`, map `SyndEntry` → `RssEntryDto` (`getLink()`, `getPublishedDate().toInstant()`, a source name like `"NPR"`) and send it with `KafkaTemplate` to `rss-entries`. Configure the JSON serializer.
3. **End-to-end check**: run with Docker, confirm rows land in `rss_entry`, and confirm no duplicate errors after a second poll.
4. **Expand the entity**: title, description/summary, maybe an image URL. The current DTO has no title, which the dashboard will need.
5. **REST API**: e.g. `GET /api/entries?page=…`, newest first.
6. **Frontend**: news list page in Angular, calling the API.
7. **More sources**: more RSS feeds (make `NprFeedProperties` a list of feeds), then weather alerts, trending topics, and the audio/radio player.
