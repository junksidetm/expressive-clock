#!/usr/bin/env python3
"""One nonfatal, privacy-conscious ntfy attempt per explicit development pass ID."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
import urllib.request
import apk_delivery

CONFIG = Path.home()/".local/state/expressive-clock/notification-config.json"
ROOT = Path(__file__).resolve().parents[2]
STATE = Path.home() / ".local/state/expressive-clock/ntfy"


def message(args):
    if args.status == "success" and (args.build != "success" or args.apk != "verified"):
        raise ValueError("Success requires a successful build and verified APK copy")
    if args.apk == "verified":
        source = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
        receipt=json.loads(Path(args.receipt).read_text())
        target = Path(receipt['downloads_apk'])
        if not source.is_file() or not target.is_file() or source.stat().st_size == 0:
            raise ValueError("Verified APK files unavailable")
        digest = lambda p: hashlib.sha256(p.read_bytes()).digest()
        if digest(source) != digest(target):
            raise ValueError("Downloads APK does not match build output")
    summary = " ".join(args.summary.split())
    if not summary or len(summary) > 200:
        raise ValueError("Use a brief, manually sanitized summary (1–200 characters)")
    build = {"success": "Build successful", "failed": "Build failed", "not-run": "Build not run"}[args.build]
    tests = f", {args.tests} tests passed" if args.tests is not None else ", tests not run"
    apk = {"verified": "APK ready in Downloads", "not-copied": "APK not refreshed", "unchanged": "Downloads APK unchanged", "not-required": "APK not required"}[args.apk]
    return f"{summary.rstrip('.')} . {build}{tests}. {apk}.".replace(" .", ".")


def send(args, state=STATE, opener=urllib.request.urlopen):
    try:
        config=json.loads(CONFIG.read_text()) if CONFIG.is_file() else {}
        topic=config.get('topic','')
        if config.get('enabled') is not True or not __import__('re').fullmatch(r'expressive-clock-[A-Za-z0-9_-]{8,120}',topic):
            raise ValueError('Clock notification topic is not activated')
        body = message(args)
        state.mkdir(parents=True, exist_ok=True, mode=0o700)
        marker = state / (hashlib.sha256(args.pass_id.encode()).hexdigest() + ".json")
        try:
            fd = os.open(marker, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        except FileExistsError:
            print("ntfy: duplicate pass suppressed")
            return
        # Claim BEFORE HTTP. No automatic retries: a timeout may follow an accepted POST.
        # At-most-once attempts avoid duplicate pushes, at the cost of no guaranteed delivery.
        with os.fdopen(fd, "w") as f:
            json.dump({"outcome": "attempted"}, f)
        payload = {
            "topic": topic,
            "title": "Expressive Clock " + ("⚠️" if args.status == "failure" else "✓" if args.status == "success" else "Test" if args.status == "test" else "Update"),
            "message": body, "priority": 3,
        }
        if args.status == "success":
            try:
                receipt = getattr(args, "receipt", None)
                if not receipt:
                    raise ValueError("No fresh pass receipt")
                evidence, _ = apk_delivery.validate_receipt(receipt, args.pass_id)
                if evidence["tests"] != args.tests:
                    raise ValueError("Test evidence mismatch")
                payload["message"] += f" Built: {evidence['built_at']}."
                remote = apk_delivery.upload_receipt(receipt, args.pass_id)
                payload["title"] = "Expressive Clock APK Ready 📱"
                payload["actions"] = [{"action": "view", "label": "Install APK", "url": remote["install_url"]}]
                payload["click"] = remote["install_url"]
                payload["message"] += " APK: ExpressiveClock-debug.apk. Clock Installer required; Actions build available 7 days."
                marker.with_suffix(".delivery.json").write_text(json.dumps(remote))
            except Exception as error:
                payload.pop("actions", None)
                payload.pop("click", None)
                payload["title"] = "Expressive Clock — Local APK Ready"
                payload["message"] += " Remote download unavailable; GitHub setup/upload verification incomplete."
                print(f"APK upload unavailable ({type(error).__name__}); sending local-only completion", file=sys.stderr)
        request = urllib.request.Request("https://ntfy.sh", data=json.dumps(payload).encode(),
                                         headers={"Content-Type": "application/json", **({"Authorization":"Bearer "+config['publish_token']} if config.get('publish_token') else {})}, method="POST")
        try:
            with opener(request, timeout=10) as response:
                status = response.status
                result = json.loads(response.read(65536))
            if not 200 <= status < 300 or result.get("event") != "message" or not result.get("id"):
                raise ValueError("Unexpected publish response")
            marker.write_text(json.dumps({"outcome": "accepted", "http": status, "id": result["id"]}))
            print(f"ntfy: HTTP {status}; accepted message id={result['id']}")
        except Exception as error:
            marker.write_text(json.dumps({"outcome": "delivery-unconfirmed", "error": type(error).__name__}))
            print(f"ntfy: delivery unconfirmed ({type(error).__name__}); development result unchanged", file=sys.stderr)
    except Exception as error:
        print(f"ntfy: skipped ({type(error).__name__}); check setup/status arguments", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pass-id", required=True)
    parser.add_argument("--status", required=True, choices=["success", "failure", "test", "info"])
    parser.add_argument("--receipt", help="Fresh immutable build receipt from finish_debug_pass.py")
    parser.add_argument("--summary", required=True, help="Manually sanitized feature summary; never raw logs/content")
    parser.add_argument("--build", required=True, choices=["success", "failed", "not-run"])
    parser.add_argument("--tests", type=int)
    parser.add_argument("--apk", required=True, choices=["verified", "not-copied", "unchanged", "not-required"])
    args = parser.parse_args()
    if args.tests is not None and args.tests < 0:
        parser.error("tests cannot be negative")
    send(args)


if __name__ == "__main__":
    main()
