#!/usr/bin/env python3
"""Canonical fresh test/build/copy/stage/upload/notify workflow, with one final notification."""
import argparse
import fcntl
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET
import apk_delivery
import notify_completion

ROOT = Path(__file__).resolve().parents[2]


def build(pass_id):
    build_id = uuid.uuid4().hex
    target = apk_delivery.SPOOL / build_id
    target.mkdir(parents=True, mode=0o700)
    output = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    started = time.time()
    with (target / "build.log").open("w") as log:
        # Force current-run test XML; never reuse old test counts as fresh verification evidence.
        result = subprocess.run(["bash", "./gradlew", ":app:testDebugUnitTest", ":app:assembleDebug", "--rerun-tasks"], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode or not output.is_file() or output.stat().st_size == 0 or output.stat().st_mtime < started:
        raise RuntimeError("Build or tests failed; Downloads not refreshed")
    tests = failures = 0
    files = list((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
    if not files: raise RuntimeError("No test evidence generated")
    for file in files:
        if file.stat().st_mtime < started: raise RuntimeError("Stale test evidence")
        attrs = ET.parse(file).getroot().attrib
        tests += int(attrs.get("tests", 0)) - int(attrs.get("skipped", 0))
        failures += int(attrs.get("failures", 0)) + int(attrs.get("errors", 0))
    if failures: raise RuntimeError("Tests failed; no upload")
    identity = apk_delivery.verify_apk(output)
    downloads = Path.home() / ("Downloads/ExpressiveClock-debug-"+build_id+".apk")
    downloads.parent.mkdir(exist_ok=True)
    with downloads.open("xb") as dest: dest.write(output.read_bytes())
    staged = target / "ExpressiveClock-debug.apk"
    shutil.copy2(output, staged)
    expected = apk_delivery.sha(output)
    if expected != apk_delivery.sha(downloads) or expected != apk_delivery.sha(staged):
        raise RuntimeError("APK verification failed")
    staged.chmod(0o400)
    now = time.time()
    evidence = dict(build_id=build_id, pass_id=pass_id, build="success", test_failures=0, tests=tests,
        **identity, downloads_apk=str(downloads), sha256=expected, built_epoch=now, built_at=datetime.fromtimestamp(now, timezone.utc).isoformat())
    receipt = target / "receipt.json"
    receipt.write_text(json.dumps(evidence, indent=2))
    print(f"Fresh build: {tests} tests passed; Downloads SHA-256 {expected}")
    return receipt, evidence


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pass-id", required=True)
    parser.add_argument("--summary", required=True)
    parser.add_argument("--deliver", action="store_true", help="Explicitly upload using approved external configuration")
    parser.add_argument("--notify", action="store_true", help="Explicitly publish to approved Clock ntfy topic (requires --deliver)")
    args = parser.parse_args()
    if args.notify and not args.deliver: parser.error("--notify requires --deliver")
    # Don't rebuild or upload twice after an interrupted/retried completion invocation.
    state = notify_completion.STATE
    state.mkdir(parents=True, exist_ok=True, mode=0o700)
    claim = state / (hashlib.sha256(args.pass_id.encode()).hexdigest() + ".build")
    try:
        with claim.open("x") as f: f.write("started")
    except FileExistsError:
        print("Pass already started; inspect its existing receipt/logs, don't rebuild with a new ID")
        return
    # Serialize canonical passes across processes: shared Gradle output/Downloads must not race.
    with (state / "debug-delivery.lock").open("a") as lock:
        fcntl.flock(lock.fileno(), fcntl.LOCK_EX)
        common = dict(pass_id=args.pass_id, summary=args.summary, receipt=None)
        try:
            receipt, evidence = build(args.pass_id)
            print("Local receipt:", receipt)
            if args.notify:
                notify_completion.send(argparse.Namespace(**(common | dict(status="success", build="success", tests=evidence['tests'], apk="verified", receipt=str(receipt)))))
            elif args.deliver:
                apk_delivery.upload_receipt(receipt,args.pass_id)
        except Exception as error:
            # Static failure summary only; don't publish raw Gradle/API exception output.
            print(f"Pass incomplete: {type(error).__name__}; inspect local build evidence", file=sys.stderr)
            raise SystemExit(1)


if __name__ == "__main__": main()
