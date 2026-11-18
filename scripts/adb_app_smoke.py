#!/usr/bin/env python3
"""Non-destructive app layout checkpoints; explicit serial required. Never saves editor drafts."""
import argparse, datetime, json, re, subprocess, tempfile
from pathlib import Path
import xml.etree.ElementTree as ET
from adb_widget_test import find_adb

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--editor',action='store_true',help='Open/cancel a new unsaved alarm and inspect keyboard')
a=p.parse_args()
out=Path(tempfile.mkdtemp(prefix='expressive-app-smoke-'))
print(out,flush=True)
adb=find_adb()
def run(*args):
    return subprocess.run([adb,'-s',a.serial,*args],check=True,capture_output=True,text=True,timeout=50).stdout

def nodes():
    remote='/data/local/tmp/expressive-app-smoke.xml'
    try:
        run('shell','uiautomator','dump',remote)
        return ET.fromstring(run('shell','cat',remote))
    finally: run('shell','rm','-f',remote)

def capture(label):
    tree=nodes()
    (out/(label+'.xml')).write_bytes(ET.tostring(tree))
    remote='/data/local/tmp/expressive-app-smoke.png'
    try:
        # Default panel is evidence only; foldables can report stale physical display IDs.
        run('shell','screencap','-p',remote)
        run('pull',remote,str(out/(label+'.png')))
    finally: run('shell','rm','-f',remote)
    return tree

def tap(target):
    node=next((n for n in nodes().iter('node') if n.get('package')=='app.expressiveclock.debug'
               and target in (n.get('text'),n.get('content-desc'))),None)
    if node is None: raise RuntimeError('Control unavailable: '+target)
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))

report={'serial':a.serial,'screens':[],'visual_review':'Required: captures are not pixel/overlap assertions'}
try:
    for tab in ['alarms','world','timers','stopwatch']:
        run('shell','am','start','-W','-n','app.expressiveclock.debug/app.expressiveclock.MainActivity','--es','tab',tab)
        tree=capture(tab)
        if not any(n.get('package')=='app.expressiveclock.debug' for n in tree.iter('node')):
            raise RuntimeError('App UI unavailable; device may be locked')
        report['screens'].append(tab)
    if a.editor:
        run('shell','am','start','-W','-n','app.expressiveclock.debug/app.expressiveclock.MainActivity','--es','tab','alarms')
        tap('Add alarm')
        try:
            capture('new-alarm')
            tap('Alarm')
            run('shell','input','text','ADB_layout_probe')
            capture('alarm-keyboard')
            run('shell','input','keyevent','KEYCODE_BACK')
        finally:
            tap('Cancel')
        report['editor']='Draft opened, text entered and cancelled; inspect images'
    report['completed']=True
except Exception as e:
    report['error']=str(e)
    report['completed']=False
finally:
    (out/'report.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report),flush=True)

raise SystemExit(0 if report.get('completed') else 1)
