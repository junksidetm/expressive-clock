#!/usr/bin/env python3
"""Run debug-only behavioural alarm probes on an explicitly selected, unlocked device.
Adds/removes one temporary alarm and one app-owned audio file, and briefly plays
sound at existing alarm volume. Never clears data or changes device settings.
"""
import argparse,json,subprocess,tempfile
from pathlib import Path
from adb_widget_test import find_adb
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--deferred',action='store_true',help='Wait 1–2 minutes for real upcoming-notification callback')
a=p.parse_args(); out=Path(tempfile.mkdtemp(prefix='expressive-alarm-probe-'))
state=subprocess.run([find_adb(),'-s',a.serial,'shell','dumpsys','trust'],capture_output=True,text=True,timeout=20).stdout
if any('(current)' in line and 'deviceLocked=1' in line for line in state.splitlines()):
    (out/'report.json').write_text(json.dumps({'serial':a.serial,'skipped':'securely locked'},indent=2))
    print(out); print('SKIP: unlock requires owner interaction'); raise SystemExit(2)
r=subprocess.run([find_adb(),'-s',a.serial,'shell','am','instrument','-w',
                  *(['-e','deferred','true'] if a.deferred else []),
                  'app.expressiveclock.debug/app.expressiveclock.AlarmProbe'],
                 capture_output=True,text=True,timeout=240)
text=r.stdout+r.stderr
(out/'instrumentation.txt').write_text(text)
ok=r.returncode==0 and 'PASS:' in text and 'failure:' not in text and 'INSTRUMENTATION_FAILED' not in text
(out/'report.json').write_text(json.dumps({'serial':a.serial,'passed':ok,'output':text},indent=2))
print(out);print(text)
raise SystemExit(0 if ok else 1)
