# Observability runbook

Production logs are newline-delimited JSON on stdout. Ship stdout with the
platform log agent to Loki, Elasticsearch, CloudWatch, or another JSON-aware
backend. Preserve these fields: `@timestamp`, `level`, `logger_name`,
`message`, `service`, and `requestId`.

Every response includes `X-Request-ID`. Clients should send their own stable
ID when retrying a request and include it in support reports. Unsafe or
oversized IDs are replaced server-side.

Useful searches:

```text
requestId:"<reported-id>"
level:ERROR AND service:"mybill-backend"
message:"http_request" AND message:"status=5*"
```

For a downloaded JSON log:

```powershell
./scripts/analyze-logs.ps1 -Path ./application.log
```

Prometheus metrics are exposed at `/actuator/prometheus`. Dashboard and alert
on `http_server_requests_seconds`, `mybill_sync_duration_seconds`,
`auth_success_total`, `auth_failure_total`, `jvm_memory_used_bytes`, and Hikari
connection metrics. Authentication counters carry `flow="login|refresh"` and
an `outcome` label, for example:

```promql
sum(rate(auth_failure_total{flow="refresh"}[5m]))
/
clamp_min(sum(rate(auth_success_total{flow="refresh"}[5m])
  + rate(auth_failure_total{flow="refresh"}[5m])), 0.001)
```

The logger-management endpoint is exposed at `/actuator/loggers` and remains
authentication-protected by Spring Security. CI archives `metrics.prom` with
the JMeter results so metric output can be inspected after each run. Never
index bearer tokens, request bodies, passwords, or payment credentials.
