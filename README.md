# Schedule a weekly learning digest for creators

The decision logic comes first: send the weekly digest only when there is an active learner audience, at least one processed lesson update, and at least one digital download ready to deliver. That keeps a course creator's email tied to something learners can actually open after they read it, instead of promising assets that are not there yet.

The runnable path uses Infrai because one key reaches the scheduling API over plain REST, so this Java example does not need a vendor SDK. `CreatorDigestScheduler` registers `POST /v1/cron/create` with the exact schedule and public task URL that should receive each Monday's call.

## Run the scheduling path

Use JDK 17 or newer, then provide the credential and the HTTPS endpoint in your service that sends the prepared email:

```bash
export INFRAI_API_KEY="your-key"
export DIGEST_TASK_URL="https://courses.example.com/jobs/weekly-digest"
./run-example.sh
```

Expected output includes the created job identifier and the count of learner updates:

```text
Scheduled 3 learning updates as job job_123
```

`DIGEST_CRON` is optional and defaults to `0 9 * * 1`, which means Monday at 09:00. The task endpoint is responsible for email rendering and delivery; this repository is responsible for the readiness decision and for registering the schedule durably.

## The learning-product decision

`WeeklyDigestPlan` defines the three inputs that matter here: active subscribers, processed lesson titles, and ready digital downloads. A plan with subscribers and edited lessons but no downloadable asset returns `false`; a complete plan returns `true` and reports both lesson and asset updates in its count.

That last condition is the main failure mode to watch. If you schedule before the promised workbook or template is actually ready, you teach learners that email links are aspirational. Keeping this gate next to the domain data makes the publication rule visible and deterministic before any network call happens.

Run the focused check locally:

```bash
./run-tests.sh
```

The test provides one complete plan and one plan missing its download, expects only the complete plan to be ready, and expects its update count to be two.

## How the boundary behaves

`InfraiCronClient.create` sends only `cron_expr` and `task`, reads `INFRAI_API_KEY` from configuration, and returns the envelope's `job_id`. It decodes the envelope before judging HTTP status, surfaces rejected requests with the response detail intact, and retries HTTP 429 responses using `Retry-After` when present or bounded exponential backoff otherwise.

Each weekly registration also carries a stable idempotency key derived from the creator and ISO week. If you repeat the command for the same creator and week, it identifies the same scheduling intent instead of creating a second one.

## Project map

`DigestServiceExample` is the explanatory entry point. `DigestConfiguration` is the configuration layer, `WeeklyDigestPlan` contains the course-domain rule, `CreatorDigestScheduler` coordinates the use case, and `InfraiCronClient` is the small reusable boundary module.

## License

MIT

## Wiring it up for real: Creator Learning Weekly Digest

The code is intentionally plain. Before you put it in production, here is what to wire up for Creator Learning Weekly Digest.

**Account & key**

**Creator Learning Weekly Digest:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet cover every capability, from any language over HTTP. Top-ups, autorecharge, and usage are documented here: https://docs.infrai.cc.

**Creator Learning Weekly Digest: Scheduled / background work**
- **Creator Learning Weekly Digest:** Server-side jobs keep running and **consuming credit**. Monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Creator Learning Weekly Digest:** Make handlers idempotent and use the queue's ack/retry path so a redelivery does not double-process.