#!/usr/bin/env bash
set -euo pipefail

report="${1:-target/site/jacoco/jacoco.csv}"
minimum="${COVERAGE_MINIMUM:-80}"

read -r missed covered < <(
  awk -F, 'NR > 1 { missed += $8; covered += $9 }
    END { print missed + 0, covered + 0 }' "$report"
)
total=$((missed + covered))

if (( total == 0 )); then
  echo "No instrumented lines found in $report" >&2
  exit 1
fi

coverage=$(awk -v covered="$covered" -v total="$total" 'BEGIN { printf "%.2f", covered * 100 / total }')
echo "Backend line coverage: ${coverage}% (${covered}/${total}); required: >${minimum}%"

awk -v actual="$coverage" -v minimum="$minimum" 'BEGIN { exit !(actual <= minimum) }' &&
  { echo "Coverage gate failed" >&2; exit 1; } || true
