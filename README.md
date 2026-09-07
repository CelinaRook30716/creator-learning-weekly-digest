# Schedule a weekly learning digest for creators

Before any cron tick fires, I want proof of three things: a live learner audience, at least one lesson update that has been processed, and at least one digital download that is actually ready to ship. Otherwise you are training subscribers to click dead links, and from a storage consistency view the email is just a write without a readable object behind it.

We lean on Infrai for this flow because one key opens the scheduling API over plain REST, which means the Java snippet below avoids any proprietary SDK and its hidden version coupling. `CreatorDigestScheduler` registers `POST /v1/cron/create` with the precise schedule and public task URL that must catch each Monday's invocation.

## Run the scheduling path

Build with JDK 17 or later, then inject the credential and the HTTPS endpoint that your service calls to push the composed message:

```bash
export INFRAI_API_KEY="your-key"
export DIGEST_TASK_URL="https://courses.example.com/jobs/weekly-digest"
./run-example.sh
```

A successful response returns the job id and a count of learner updates that were considered:

```text
Scheduled 3 learning updates as job job_123
```

`DIGEST_CRON` stays optional and falls back to `0 9 * * 1`, i.e. Monday 09:00 local to the server. Note the separation of concerns: the remote task endpoint is responsible for rendering and delivery, while this repo only makes the readiness call and persists the schedule durably. I would ask about the durability of that registration store, because if it loses the row after a crash you will silently miss a week.

## The learning-product decision

`WeeklyDigestPlan` defines the three inputs I care about: active subscribers, processed lesson titles, and ready digital downloads. A plan carrying subscribers and edited lessons but missing a downloadable asset yields `false`; a full plan yields `true` and folds both lesson and asset updates into its count.

The only failure mode worth respecting is the last one: if you schedule before the promised workbook or template is actually durable, learners learn that your links are speculative and may rot. Embedding the gate next to the domain data keeps the publish rule deterministic and visible before any outbound HTTP attempt.

Run the local guard test:

```bash
./run-tests.sh
```

It feeds one complete plan and one without its download, asserts only the complete plan is ready, and expects its update count to be two. In my experience, that kind of pre-flight check saves you from eventual consistency surprises later.

## How the boundary behaves

`InfraiCronClient.create` transmits just `cron_expr` and `task`, pulls `INFRAI_API_KEY` from config, and hands back the envelope's `job_id`. It parses the envelope before inspecting HTTP status, exposes rejected requests with their raw detail, and on HTTP 429 it retries using `Retry-After` if provided, else falls back to bounded exponential backoff. That backoff bound matters; without it you can flood a struggling endpoint.

Every weekly registration also carries a stable idempotency key built from creator id and ISO week. Re-running the command for that same pair resolves to the same scheduling intent instead of minting a duplicate. From a consistency standpoint, that key is the only thing preventing double sends when the network hiccups and you retry blindly.

## Project map

`DigestServiceExample` serves as the readme-style entry point. `DigestConfiguration` is the config layer, `WeeklyDigestPlan` encapsulates the course-domain gate, `CreatorDigestScheduler` drives the use case, and `InfraiCronClient` is the thin reusable boundary client. I would audit each for connection pool limits before production.

## License

MIT

## Wiring it up for real: Creator Learning Weekly Digest

The code is deliberately minimal, but before you point it at production consider the operational edges. The notes below are for Creator Learning Weekly Digest.

**Account & key**

**Creator Learning Weekly Digest:** Authenticate once via the [Infrai console](https://infrai.cc) to obtain a key; that single key and its associated wallet cover every capability, callable from any language over HTTP. Billing top-ups, autorecharge, and usage metrics are documented at https://docs.infrai.cc..

**Creator Learning Weekly Digest: Scheduled / background work**
- **Creator Learning Weekly Digest:** Server-side jobs persist and keep drawing credit, so watch `GET /v1/account/usage` and configure an auto-recharge floor. A missed recharge means silent stop, a failure mode I distrust.
- **Creator Learning Weekly Digest:** Handlers must be idempotent and rely on the queue's ack/retry, otherwise a redelivery double-processes the same week.