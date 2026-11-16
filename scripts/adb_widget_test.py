#!/usr/bin/env python3
"""Non-destructive Pixel widget evidence collection. See docs/ADB_TESTING.md."""
import argparse
import datetime as dt
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "app.expressiveclock.debug"


def find_adb():
    if os.environ.get("ADB"):
        return os.environ["ADB"]
    props = ROOT / "local.properties"
    if props.exists():
        match = re.search(r"^sdk.dir=(.+)$", props.read_text(), re.M)
        if match:
            candidate = Path(match[1]) / "platform-tools/adb"
            if candidate.exists():
                return str(candidate)
    return shutil.which("adb") or str(Path.home() / "Library/Android/sdk/platform-tools/adb")


def digital_status(clocks, before, after):
    expected_hours = {before[:2], after[:2],
                      f"{int(before[:2]) % 12 or 12:02}",
                      f"{int(after[:2]) % 12 or 12:02}"}
    expected_minutes = {before[3:5], after[3:5]}
    hours = [n["text"] for n in clocks if n["resource-id"].endswith("/live_hours")]
    minutes = [n["text"] for n in clocks if n["resource-id"].endswith("/live_minutes")]
    valid = (all(h in expected_hours for h in hours)
             and all(m[:2] in expected_minutes for m in minutes))
    return ("PASS" if valid else "FAIL") if hours and minutes else "NOT_VISIBLE"


def active_display_id(display_dump):
    for viewport in re.findall(r"DisplayViewport\{[^}]+\}", display_dump):
        if "isActive=true" in viewport and "displayId=0," in viewport:
            match = re.search(r"uniqueId='local:(\d+)'", viewport)
            if match:
                return match[1]
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=["check", "capture", "run"])
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
    parser.add_argument("--install", action="store_true", help="Build then adb install -r; keep data")
    parser.add_argument("--duration", type=int, default=180)
    parser.add_argument("--interval", type=int, default=30)
    parser.add_argument("--label", default="widgets")
    parser.add_argument("--output", type=Path)
    parser.add_argument('--home-component', help='Explicit test host without changing launcher defaults')
    parser.add_argument("--display-id", help="Explicit SurfaceFlinger physical display ID after verifying the captured panel")
    args = parser.parse_args()
    if args.duration < 0 or args.interval < 1:
        parser.error("duration must be nonnegative; interval must be positive")
    if args.install and args.mode != "run":
        parser.error("--install is only valid for run")
    adb = find_adb()

    def command(*parts, timeout=30):
        result = subprocess.run([adb, *parts], capture_output=True, text=True, timeout=timeout)
        if result.returncode:
            raise RuntimeError(result.stderr.strip() or result.stdout.strip())
        return result.stdout.strip()

    devices = command("devices", "-l")
    print(devices, flush=True)
    states = dict(re.findall(r"^(\S+)\s+(device|unauthorized|offline)\b", devices, re.M))
    serial = args.serial
    if not serial:
        ready = [s for s, state in states.items() if state == "device"]
        if len(ready) != 1:
            raise RuntimeError("Connect/unlock and authorize one device, or pass --serial.")
        serial = ready[0]
    if states.get(serial) != "device":
        raise RuntimeError(f"{serial}: {states.get(serial, 'not connected')}; authorize USB debugging.")
    prefix = ["-s", serial]

    def device(*parts, **kwargs):
        return command(*prefix, *parts, **kwargs)

    stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    out = args.output or Path(tempfile.gettempdir()) / "expressive-clock-adb" / stamp
    out.mkdir(parents=True, exist_ok=False)
    print(f"Evidence: {out}", flush=True)
    info = {
        "serial": serial, "adb": command("version"),
        "model": device("shell", "getprop", "ro.product.model"),
        "android": device("shell", "getprop", "ro.build.version.release"),
        "sdk": device("shell", "getprop", "ro.build.version.sdk"),
        "size": device("shell", "wm", "size"),
        "fold": device("shell", "cmd", "device_state", "print-state"),
        "displays": device("shell", "dumpsys", "SurfaceFlinger", "--display-id"),
    }
    (out / "device.json").write_text(json.dumps(info, indent=2))
    report = {"device": info, "samples": [], "automated": {}, "manual": [
        "Analog hand movement and exact pivot/angle: inspect clock crops across samples.",
        "Typography versus previews; verify each typeface/weight/layout separately.",
        "Launcher resizing, physical rotation and fold/unfold: capture labeled checkpoints.",
        "Android 12–16 and other launchers need separate device runs.",
    ]}
    log = None
    log_file = None
    completed = False

    def capture(index):
        label = re.sub(r"[^a-zA-Z0-9_-]", "_", args.label)
        base = f"{index:03d}-{label}"
        # Capture to a remote file, then pull. Foldables may print a warning to stdout,
        # corrupting an otherwise convenient exec-out screencap PNG stream.
        remote = f"/data/local/tmp/expressive-clock-{stamp}"
        png = out / f"{base}.png"
        xml = out / f"{base}.xml"
        before = device("shell", "date", "+%H:%M:%S")
        display_id = args.display_id or active_display_id(device("shell", "dumpsys", "display"))
        if not display_id:
            raise RuntimeError("Cannot identify active physical display; stop rather than capture an arbitrary panel")
        try:
            device("shell", "uiautomator", "dump", remote + ".xml", timeout=45)
            device("pull", remote + ".xml", str(xml))
            device("shell", "screencap", "-d", display_id, "-p", remote + ".png")
            device("pull", remote + ".png", str(png))
            if not png.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
                raise RuntimeError("Invalid screenshot PNG")
            nodes = list(ET.parse(xml).iter("node"))
            clocks = [{k: n.get(k, "") for k in
                       ("resource-id", "class", "text", "content-desc", "bounds")}
                      for n in nodes if PACKAGE + ":id/live_" in n.get("resource-id", "")]
            analog = [{k: n.get(k, "") for k in ("class", "content-desc", "bounds")}
                      for n in nodes if n.get("class") == "android.widget.AnalogClock"
                      or n.get("resource-id") == PACKAGE + ":id/hands"]
        finally:
            # Only delete temporary files created by this run.
            device("shell", "rm", "-f", remote + ".png", remote + ".xml")
        after = device("shell", "date", "+%H:%M:%S")
        status = digital_status(clocks, before, after)
        sample = {"png": png.name, "xml": xml.name, "device_time_before": before,
                  "device_time_after": after, "digital_time": status,
                  "digital": clocks, "analog": analog,
                  "display_id": display_id, "size": device("shell", "wm", "size"),
                  "fold": device("shell", "cmd", "device_state", "print-state")}
        report["samples"].append(sample)
        (out / "report.json").write_text(json.dumps(report, indent=2))
        print(f"{base}: digital={status}, time={before}, analog_views={len(analog)}", flush=True)

    try:
        if args.mode == "run":
            log_file = (out / "logcat.txt").open("w")
            # Include the launcher process, where RemoteViews inflation failures happen.
            log = subprocess.Popen([adb, *prefix, "logcat", "-v", "threadtime", "-T", "1"],
                                   stdout=log_file, stderr=subprocess.STDOUT)
        if args.install:
            subprocess.run([str(ROOT / "gradlew"), "assembleDebug", "testDebugUnitTest"],
                           cwd=ROOT, check=True)
            print(device("install", "-r",
                         str(ROOT / "app/build/outputs/apk/debug/app-debug.apk"), timeout=120),
                  flush=True)
            report["automated"]["install"] = "PASS (install -r, no data clear)"
        if args.mode == "run":
            if args.home_component:
                device("shell", "am", "start", "-n", args.home_component,
                       "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
            else:
                device("shell", "input", "keyevent", "KEYCODE_HOME")
        start = time.monotonic()
        capture(0)
        if args.mode == "run":
            index = 1
            while time.monotonic() - start < args.duration:
                remaining = args.duration - (time.monotonic() - start)
                time.sleep(min(args.interval, remaining))
                capture(index)
                index += 1
        completed = True
    except Exception as error:
        report["error"] = str(error)
        raise
    finally:
        if log:
            log.terminate()
            try:
                log.wait(timeout=5)
            except subprocess.TimeoutExpired:
                log.kill()
                log.wait()
        if log_file:
            log_file.close()
        if (out / "logcat.txt").exists():
            lines = (out / "logcat.txt").read_text(errors="replace").splitlines()
            matches = [line for line in lines if re.search(
                r"FATAL EXCEPTION|AndroidRuntime|RemoteViews|InflateException|expressiveclock",
                line, re.I)]
            (out / "relevant-logcat.txt").write_text("\n".join(matches))
            report["automated"]["logcat"] = "Captured; inspect relevant-logcat.txt (matches are not automatically failures)"
        samples = report["samples"]
        stamps = {s["device_time_before"][:5] for s in samples}
        report["automated"]["digital_live"] = (
            "PASS" if completed and len(stamps) >= 3 and all(s["digital_time"] == "PASS" for s in samples)
            else "FAIL" if any(s["digital_time"] == "FAIL" for s in samples)
            else "INCONCLUSIVE: need visible digital clocks across at least three distinct minutes")
        report["completed"] = completed
        (out / "report.json").write_text(json.dumps(report, indent=2))
        print(f"Report: {out / 'report.json'}", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.SubprocessError, OSError) as error:
        raise SystemExit(f"ADB test stopped: {error}")
