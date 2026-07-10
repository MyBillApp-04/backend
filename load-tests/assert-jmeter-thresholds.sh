#!/usr/bin/env bash
set -euo pipefail

results="${1:-results.jtl}"
max_p95_ms="${MAX_P95_MS:-500}"
max_error_percent="${MAX_ERROR_PERCENT:-1}"

if [[ ! -s "$results" ]]; then
  echo "JMeter results are missing: $results" >&2
  exit 1
fi

mapfile -t elapsed < <(tail -n +2 "$results" | awk -F, '$8 == "true" {print $2}' | sort -n)
total=$(($(wc -l < "$results") - 1))
success=${#elapsed[@]}
errors=$((total - success))

if (( total <= 0 )); then
  echo "JMeter produced no samples" >&2
  exit 1
fi

p95_index=$(((success * 95 + 99) / 100 - 1))
p95=${elapsed[$p95_index]:-999999}
error_percent=$(awk -v errors="$errors" -v total="$total" 'BEGIN { printf "%.2f", errors * 100 / total }')

echo "samples=$total successes=$success errors=$errors error_percent=$error_percent p95_ms=$p95"

awk -v actual="$error_percent" -v limit="$max_error_percent" 'BEGIN { exit !(actual > limit) }' &&
  { echo "Error rate ${error_percent}% exceeds ${max_error_percent}%" >&2; exit 1; } || true

if (( p95 > max_p95_ms )); then
  echo "p95 ${p95}ms exceeds ${max_p95_ms}ms" >&2
  exit 1
fi
