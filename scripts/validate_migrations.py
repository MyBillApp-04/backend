"""Fail CI when Flyway migration versions are duplicated or malformed."""

from pathlib import Path
import re
import sys

MIGRATION_DIR = Path("src/main/resources/db/migration")
PATTERN = re.compile(r"^V(\d+)__.+\.sql$")

versions: dict[int, Path] = {}
errors: list[str] = []

for migration in sorted(MIGRATION_DIR.glob("*.sql")):
    match = PATTERN.match(migration.name)
    if not match:
        errors.append(f"Invalid Flyway filename: {migration.name}")
        continue

    version = int(match.group(1))
    if version in versions:
        errors.append(
            f"Duplicate Flyway version V{version}: "
            f"{versions[version].name}, {migration.name}"
        )
    versions[version] = migration

if not versions:
    errors.append(f"No migrations found in {MIGRATION_DIR}")

if errors:
    print("\n".join(errors), file=sys.stderr)
    raise SystemExit(1)

print(f"Validated {len(versions)} unique Flyway migrations "
      f"(V{min(versions)}..V{max(versions)}).")
