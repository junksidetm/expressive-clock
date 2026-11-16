#!/usr/bin/env python3
"""Assisted visual font matrix for an existing widget; restores the supplied original font.
Pass the actual original font and weight. No system settings are changed.
"""
import argparse
import datetime as dt
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from adb_widget_test import find_adb, ROOT


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--widget-id', required=True, type=int)
    parser.add_argument('--restore-font', required=True,
                        choices=['Condensed', 'Flex', 'Serif', 'Monospace'])
    parser.add_argument('--restore-weight', required=True, choices=['Thin', 'Regular', 'Medium', 'Bold'])
    parser.add_argument('--fonts', nargs='+', default=['Condensed', 'Flex', 'Serif', 'Monospace'],
                        choices=['Condensed', 'Flex', 'Serif', 'Monospace'])
    parser.add_argument('--weights', nargs='+', choices=['Thin', 'Regular', 'Medium', 'Bold'])
    parser.add_argument('--layouts', nargs='+', choices=['Stacked', 'Horizontal', 'Compact'])
    parser.add_argument('--restore-layout', choices=['Stacked', 'Horizontal', 'Compact'])
    parser.add_argument('--home-component', help='Explicit test host; does not change the default launcher')
    args = parser.parse_args()
    adb = find_adb()
    out = Path(tempfile.gettempdir()) / 'expressive-clock-fonts' / dt.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    out.mkdir(parents=True)
    print(out, flush=True)
    state = {"widget_id": args.widget_id, "restore_font": args.restore_font,
             "restore_weight": args.restore_weight, "restoration": "pending", "captures": []}
    def record():
        (out / 'scenario.json').write_text(json.dumps(state, indent=2))
    record()
    def device(*parts):
        return subprocess.run([adb, '-s', args.serial, *parts], check=True,
                              text=True, capture_output=True, timeout=45).stdout
    def go_home():
        if args.home_component:
            device('shell', 'am', 'start', '-n', args.home_component,
                   '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
        else:
            device('shell', 'input', 'keyevent', 'KEYCODE_HOME')
    def nodes():
        remote = '/data/local/tmp/expressive-clock-font-matrix.xml'
        try:
            device('shell', 'uiautomator', 'dump', remote)
            return list(ET.fromstring(device('shell', 'cat', remote)).iter('node'))
        finally:
            device('shell', 'rm', '-f', remote)
    def tap(text):
        for attempt in range(5):
            candidates = nodes()
            node = next((n for n in candidates if n.get('text') == text
                         and n.get('package') == 'app.expressiveclock.debug'), None)
            if node is not None:
                x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
                device('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
                return
            scroll = next((n for n in candidates if n.get('scrollable') == 'true'
                           and n.get('package') == 'app.expressiveclock.debug'), None)
            if scroll is None:
                break
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', scroll.get('bounds')))
            x = (x1 + x2) // 2
            device('shell', 'input', 'swipe', str(x), str(y2 - (y2-y1)//5),
                   str(x), str(y1 + (y2-y1)//5), '400')
        raise RuntimeError('Cannot locate configuration control: ' + text)
    def open_config():
        device('shell', 'am', 'start', '-n',
               'app.expressiveclock.debug/app.expressiveclock.widget.ClockWidgetConfigActivity',
               '--ei', 'appWidgetId', str(args.widget_id))
        # Configuration activities can reopen with a remembered scroll offset.
        # Start each case at Layout before selecting font and weight below it.
        for attempt in range(5):
            current = nodes()
            if any(n.get('text') == 'Stacked' for n in current):
                return
            scrolls = [n for n in current if n.get('scrollable') == 'true'
                       and n.get('package') == 'app.expressiveclock.debug']
            if not scrolls:
                continue
            def rect(n):
                return list(map(int, re.findall(r'\d+', n.get('bounds'))))
            x1, y1, x2, y2 = max((rect(n) for n in scrolls), key=lambda r: r[3]-r[1])
            x = (x1+x2)//2
            device('shell', 'input', 'swipe', str(x), str(y1+(y2-y1)//5),
                   str(x), str(y2-(y2-y1)//5), '300')
        raise RuntimeError('Configuration Layout controls did not become visible')
    def capture(label):
        subprocess.run([sys.executable, str(ROOT / 'scripts/adb_widget_test.py'),
                        'capture', '--serial', args.serial, '--label', label,
                        '--output', str(out / label)], check=True)
        result = json.loads((out / label / 'report.json').read_text())
        state['captures'].append(label)
        record()
        if label.endswith('host') and (not result['samples'] or
                result['samples'][-1]['digital_time'] != 'PASS'):
            raise RuntimeError('Widget not visible or time mismatch; inspect ' + str(out / label))
    try:
        for font in args.fonts:
            for layout in args.layouts or [None]:
                for weight in args.weights or [None]:
                    open_config()
                    if layout:
                        tap(layout)
                    tap(font)
                    if weight:
                        tap(weight)
                    label = '-'.join(x.lower() for x in [font, layout, weight] if x)
                    capture(label + '-preview')
                    tap('Save')
                    go_home()
                    capture(label + '-host')
    except Exception as error:
        state['error'] = str(error)
        record()
        raise
    finally:
        try:
            open_config()
            if args.restore_layout:
                tap(args.restore_layout)
            tap(args.restore_font)
            tap(args.restore_weight)
            capture('restored-preview')
            tap('Save')
            go_home()
            capture('restored-host')
            state['restoration'] = 'actions-completed; inspect restored-preview and restored-host'
        except Exception as error:
            state['restoration'] = 'FAILED: ' + str(error)
            print('RESTORATION NOT VERIFIED: reconnect/unlock and restore ' +
                  args.restore_font + '/' + args.restore_weight, file=sys.stderr)
            raise
        finally:
            record()
    print('Restoration actions completed; review screenshots: ' + str(out), flush=True)


if __name__ == '__main__':
    main()
