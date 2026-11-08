# Configuration (`application.yaml`)

File: `backend/world-view/src/main/resources/application.yaml`. Database credentials come from `.env` at the repo root (not committed), loaded by `run_backend.sh` / `run_test.sh`.

| Key | Value | Notes |
|-----|-------|-------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/${POSTGRES_DB}` | creds from `.env` |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate creates/updates `rss_entry` from the entity on startup. It adds columns but won't add a `NOT NULL` column to a table that already has rows |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | |
| `spring.kafka.consumer.group-id` | `world-view-consumer-group` | committed offsets are stored per group |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` | a **new** consumer group starts from the oldest message; an existing group continues after its committed offset |
| `spring.kafka.consumer.value-deserializer` | `JacksonJsonDeserializer` | turns JSON messages into `RssEntryDto` |
| `spring.json.value.default.type` | `net.thinhpham.world_view.dto.RssEntryDto` | fallback type for messages without a type header |
| `spring.json.trusted.packages` | `net.thinhpham.world_view.dto` | only classes in this package may be deserialized |
| `spring.kafka.producer.value-serializer` | `JacksonJsonSerializer` | turns `RssEntryDto` into JSON before sending |
| `npr.feed.url` | `https://feeds.npr.org/1014/rss.xml` | NPR Politics feed |

Kafka broker settings live in `kafka/server.properties` (KRaft, PLAINTEXT on 9092, controller on 9093, internal-topic replication factor 1). See [decisions.md](decisions.md#1-single-kafka-broker-internal-topic-replication-factor-1).
