"""Resource-contract regression checks; run with python3 -m unittest discover -s tests."""
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "app/src/main"
A = "{http://schemas.android.com/apk/res/android}"

class LiveWidgetsTest(unittest.TestCase):
    def test_every_font_and_orientation_uses_live_clocks(self):
        layouts = list((ROOT / "res/layout").glob("widget_live_digital*.xml"))
        self.assertEqual(36, len(layouts))
        for layout in layouts:
            elements = {e.get(A + "id"): e for e in ET.parse(layout).iter()}
            for name in ("live_hours", "live_minutes", "live_date"):
                self.assertEqual("TextClock", elements["@+id/" + name].tag, str(layout))

    def test_analog_hands_remain_native(self):
        self.assertTrue(any(e.tag == "AnalogClock"
                            for e in ET.parse(ROOT / "res/layout/widget_clock.xml").iter()))

    def test_both_providers_receive_clock_and_restart_events(self):
        manifest = ET.parse(ROOT / "AndroidManifest.xml")
        for name in ("ClockWidgetProvider", "DigitalClockWidgetProvider"):
            receiver = next(e for e in manifest.iter("receiver")
                            if e.get(A + "name") == ".widget." + name)
            actions = {e.get(A + "name") for e in receiver.iter("action")}
            for action in ("BOOT_COMPLETED", "TIME_SET", "TIMEZONE_CHANGED",
                           "LOCALE_CHANGED", "MY_PACKAGE_REPLACED"):
                self.assertIn("android.intent.action." + action, actions)
