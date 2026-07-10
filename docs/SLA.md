# Service levels and performance objectives

These objectives define the engineering target for the MyBill API. They are
not a contractual customer SLA unless incorporated into a separate agreement.

## Availability and latency

| Signal | Objective | Measurement |
|---|---:|---|
| Monthly API availability | 99.9% | Successful, non-maintenance requests / total requests |
| Read API latency | p95 ≤ 500 ms, p99 ≤ 1 s | `http.server.requests` Prometheus histogram |
| Write API latency | p95 ≤ 1 s, p99 ≤ 2 s | `http.server.requests` Prometheus histogram |
| Server error rate | < 1% over 5 minutes | HTTP 5xx / all HTTP responses |
| Sync completion | p95 ≤ 2 s for a 100-change page | `mybill.sync.duration` |

Scheduled maintenance announced at least 24 hours ahead is excluded from the
availability calculation. Client errors (4xx), caller cancellations, and
upstream outages outside MyBill control are reported but excluded.

## Alerting and response

| Severity | Example | Acknowledge | Restore target |
|---|---|---:|---:|
| SEV-1 | Authentication unavailable, data corruption risk | 15 min | 1 hour |
| SEV-2 | Major API or sync degradation | 30 min | 4 hours |
| SEV-3 | Limited feature degradation | 1 business day | 3 business days |

Alert on two consecutive five-minute windows below the availability objective,
above 1% 5xx, or above the latency objective. Use `X-Request-ID` to join an
incident report to JSON application logs.

## Error budget and review

A 99.9% monthly target permits about 43 minutes of unplanned unavailability in
a 30-day month. When half the monthly budget is consumed, pause risky releases
and prioritize reliability work. Review objectives quarterly and after every
SEV-1 incident.

The CI JMeter gate is deliberately narrower than production monitoring: it
checks `/ping` at p95 ≤ 500 ms and errors ≤ 1% to catch gross regressions.
