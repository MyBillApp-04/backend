#!/usr/bin/env python3
"""Fail CI if the PostgreSQL Testcontainers integration test did not execute."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET


report = Path("target/surefire-reports/TEST-com.mybill.MyBill_Backend.PostgresMigrationIT.xml")
if not report.is_file():
    raise SystemExit(f"Missing integration-test report: {report}")

suite = ET.parse(report).getroot()
tests = int(suite.attrib.get("tests", "0"))
failures = int(suite.attrib.get("failures", "0"))
errors = int(suite.attrib.get("errors", "0"))
skipped = int(suite.attrib.get("skipped", "0"))

print(
    "PostgreSQL integration result: "
    f"tests={tests}, failures={failures}, errors={errors}, skipped={skipped}"
)
if tests < 1 or failures or errors or skipped:
    sys.exit("PostgreSQL integration test must run successfully in CI")
