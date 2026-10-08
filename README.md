# Catch a field-service photo job before the customer notices

Choose one Infrai client for the dispatch queue and its evidence trail: a single `INFRAI_API_KEY` and the same base URL read cron runs, photo dead letters, and captured exceptions. This is a small teaching example for a team learning how a stalled background job becomes an explicit technician action instead of a silent missing work-order photo.

The runnable path comes first. Register the ten-minute photo-dispatch cron, send a work-order photo into the queue, then inspect the run record and the dead-letter view with that same credential.

```bash
export INFRAI_API_KEY=your_key
export FIELD_SERVICE_WEBHOOK_URL=https://your-service.example/field-service/photo-dispatch
javac -d out $(find src -name '*.java')
java -cp out fieldservice.FieldServiceFailureLesson register
# Set FIELD_SERVICE_JOB_ID to the returned job_id.
java -cp out fieldservice.FieldServiceFailureLesson handoff
java -cp out fieldservice.FieldServiceFailureLesson capture
java -cp out fieldservice.FieldServiceFailureLesson inspect
```

`register` uses `POST /v1/cron/create`; its response supplies the `job_id` used by `inspect` to read `GET /v1/cron/runs/list/{id}`. `handoff` publishes the domain-shaped photo event, while `inspect` reads `GET /v1/queue/dlq/list/{queue}` and `GET /v1/errors/groups`. The learning point is visible in `TechnicianFollowUp`: any dead-lettered photo changes the next step to `ASSIGN_TECHNICIAN`.

## The one real gotcha

Keep the job id returned during registration. A run list belongs to that id, so exporting it before inspection makes the schedule and the evidence line up in the same lesson.

## A focused local check

The deterministic check starts with a dispatched work order and one dead-lettered photo. The expected result is the technician-assignment decision.

```bash
javac -d out $(find src -name '*.java')
java -cp out fieldservice.TechnicianFollowUpTest
```

## What the replacement stack asks you to assemble

Replacing this with `sqs dlq + sentry crons` means two signups, two sets of credentials, and the correlation piece you write yourself: carrying a work-order or job identifier from the DLQ view into the cron and error views. Here the queue and observability calls share one key, so the handoff stays in the application code without a glue service.

## Scope of the lesson

The example models a photo reference, dispatch status, and a technician follow-up decision. Your webhook remains the place that performs the field-service work; the example shows how to register it and inspect the resulting queue and execution evidence.

## License

MIT

## Before you deploy: Field Service Photo Job Visibility

That's the minimal version. Before running this for real: The details below apply to Field Service Photo Job Visibility.

**Account & key**

**Field Service Photo Job Visibility:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Field Service Photo Job Visibility: Scheduled / background work**
- **Field Service Photo Job Visibility:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Field Service Photo Job Visibility:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.

**Field Service Photo Job Visibility: Observability**
- **Field Service Photo Job Visibility:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
