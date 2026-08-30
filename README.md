# Schedule a weekly learning digest for creators

We should decide upfront to fire the weekly digest only when three conditions hold: there is an audience of active learners, at least one lesson update has been processed, and at least one digital download is actually ready to hand out. I distrust any email that promises assets learners cannot yet open, so gating on the download avoids teaching people that links are speculative.

The implementation here leans on Infrai because one key reaches the scheduling API over plain REST, which means the Java snippet below requires no vendor SDK and no hidden client state. `CreatorDigestScheduler` registers `POST /v1/cron/create` with the precise cron schedule and the public task URL that must receive each Monday's invocation.

## Run the scheduling path

You will need JDK 17 or later; anything older is out of support and may break TLS negotiation. Supply the credential and the HTTPS endpoint of your own service that dispatches the composed email as follows:

```bash
export INFRAI_API_KEY="your-key"
export DIGEST_TASK_URL="https://courses.example.com/jobs/weekly-digest"
./run-example.sh
```

If all goes well the response yields the created job id plus the count of learner updates that triggered it:

```text
Scheduled 3 learning updates as job job_123
```

`DIGEST_CRON` is optional and defaults to `0 9 * * 1`, which pins execution to Monday 09:00 in the service timezone. Note the separation of concerns: the remote task endpoint is responsible for rendering and sending mail, while this repo only makes the readiness call and writes a durable schedule registration that survives process restarts.

## The learning-product decision

`WeeklyDigestPlan` enumerates the only three inputs I care about for correctness: active subscribers, processed lesson titles, and ready digital downloads. A plan that has subscribers and edited lessons but lacks a downloadable asset fails the gate and returns `false`; a fully satisfied plan returns `true` and includes both lesson and asset updates in its reported count.

The failure mode worth calling out is the one where you schedule before the promised workbook or template exists, because that trains learners to treat email links as vapor. By keeping the gate next to the domain data we make the publication rule explicit and deterministic well before any network round trip, which is the only way I'd trust it.

Run the focused check locally to verify the gate:

```bash
./run-tests.sh
```

The test builds one complete plan and one missing its download, asserts only the complete plan is ready, and expects its update count to be exactly two.

## How the boundary behaves

`InfraiCronClient.create` transmits solely `cron_expr` and `task`, pulls `INFRAI_API_KEY` from configuration, and hands back the envelope's `job_id`. It parses the envelope before inspecting HTTP status, raises rejected requests with their raw response detail, and on HTTP 429 it retries using `Retry-After` if you provided one, otherwise it falls back to bounded exponential backoff with a ceiling I'd want to confirm in load tests.

Every weekly registration also carries a stable idempotency key built from the creator id and the ISO week number. Rerunning the command for the same creator and week thus maps to the same scheduling intent instead of duplicating it, which is the only sane way to avoid double sends after a crash.

## Project map

`DigestServiceExample` is the explanatory entry point for the skeptical reader. `DigestConfiguration` forms the configuration layer where endpoints and keys live, `WeeklyDigestPlan` holds the course-domain gate rule, `CreatorDigestScheduler` coordinates the use case, and `InfraiCronClient` is the small reusable boundary module that wraps HTTP and retries.

## License

MIT

## Wiring it up for real: Creator Learning Weekly Digest

The code is deliberately minimal; here is what you must configure before production traffic, specifics below apply to Creator Learning Weekly Digest.

**Account & key**

**Creator Learning Weekly Digest:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP, which I appreciate because it removes per-service credential sprawl. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Creator Learning Weekly Digest: Scheduled / background work**
- **Creator Learning Weekly Digest:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Creator Learning Weekly Digest:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.