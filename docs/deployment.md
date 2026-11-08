# Deployment and cost

Nothing is deployed yet. This page collects what we know so the **cost review** (roadmap item 9) can be a quick decision. All prices are rough estimates. Check current pricing before deciding.

## Budget and goal

- **Budget: about $20–30 per year** for production.
- Production only has to serve news to the dashboard and the voice assistant.
- Kafka is for learning and can stay **local** in Docker on the dev machine.

## What costs money

- **Storage is cheap.** A story is about 1–2 KB. One feed adds roughly 50–100 new stories a day, so about 70 MB per feed per year. Disk storage is in the cents.
- **The always-on server is the real cost.** The poller runs every 15 minutes, so the machine runs 24/7. RAM is what drives the price:

| Setup | RAM needed |
|---|---|
| Spring Boot + Postgres (no Kafka) | ~1 GB |
| Spring Boot + Postgres + Kafka (single broker, small heap) | ~2–4 GB |

- Managed services (AWS RDS, AWS MSK) cost far more than running the same software in Docker on one machine. MSK alone is hundreds of dollars per month: out of the question.
- Optional extras: a domain name (~$10–15/year, not needed if only you and the voice assistant use it); reaching the server away from home can be free with a VPN like Tailscale.

## Options

| Option | Without Kafka (~1 GB) | With Kafka (~2–4 GB) | Notes |
|---|---|---|---|
| **Home server** (old laptop, mini PC, Raspberry Pi 5) | ~$5–15/year electricity | about the same | one-time hardware cost (or $0 with an old laptop); sits next to the voice assistant |
| **Free tier** (e.g. Oracle Cloud Always Free ARM) | $0 | $0 if available | instances are often sold out |
| **Budget VPS** | ~$15–30/year with yearly deals | ~$50–120/year | deals come and go |
| **AWS EC2** | t3.micro ~$90/year after free tier | t3.small/medium ~$180–360/year | over budget with Kafka |

**At home, Kafka is nearly free. In the cloud, it multiplies the cost roughly 3–10×.**

## The deciding question

**Is there a home machine with 4–8 GB of RAM?**

- **Yes** → keep Kafka in production, at home.
- **No** → **Hybrid** (current favorite).

## Hybrid

Production runs in `direct` mode on the smallest/cheapest machine; Kafka runs only locally in Docker for learning (`kafka` mode). Same code, a flag decides the path.

| | Production | Local (dev machine) |
|---|---|---|
| Flag | `direct` | `kafka` |
| Kafka | not installed | in Docker, when wanted |
| Postgres | production database | separate local database |
| Cost | smallest server | free |

Rules:
- **Keep the databases separate.** The dev machine never writes to production.
- **Test both paths.** Production never runs the Kafka path, so tests must keep it working.

## If we drop Kafka in production: refactor plan

Only `RssEntryProducer` and `RssEntryConsumer` know about Kafka; the scheduler hands entries off in one line. Estimated at about half a day including tests:

1. New interface, e.g. `RssEntryPublisher` with `publish(RssEntryDto)`. `RssEntryProducer` implements it.
2. `RssPollingScheduler` depends on the interface instead of `RssEntryProducer`.
3. Move "skip if incomplete/duplicate, else save" from `RssEntryConsumer` into a service, e.g. `RssEntryService.saveIfNew(dto)`. The consumer calls it.
4. New `DirectRssEntryPublisher` implementing the interface by calling `saveIfNew` directly.
5. The flag: a property like `world-view.messaging: kafka | direct` plus `@ConditionalOnProperty` on the producer, consumer and direct publisher. In `direct` mode, also exclude Kafka auto-configuration so Spring doesn't try to reach a broker.
6. A production Docker Compose file (or compose profile) without the Kafka service.

Unchanged: feed service, mapper, DTO, entity, repository, REST API, frontend.

## Later

- **Retention rule:** delete stories older than N days (30/90) to cap storage. Old news isn't useful to the voice assistant anyway.
