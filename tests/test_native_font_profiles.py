"""Cross-process resource contracts for production native font profiles."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'app/src'
A = '{http://schemas.android.com/apk/res/android}'


class NativeFontResourceTest(unittest.TestCase):
    def test_every_native_profile_uses_system_family_and_live_views(self):
        layouts = list((ROOT / 'main/res/layout').glob('widget_live_digital*.xml'))
        self.assertEqual(36, len(layouts))
        for path in layouts:
            tree = ET.parse(path)
            clocks = list(tree.iter('TextClock'))
            self.assertEqual(3, len(clocks), str(path))
            self.assertTrue(any(e.get(A+'id') == '@+id/background_shape' for e in tree.iter()))
            for clock in clocks:
                self.assertIn(clock.get(A+'fontFamily'), ['google-sans-flex', 'sans-serif-condensed',
                              'sans-serif', 'serif', 'monospace', 'cursive'], str(path))

    def test_initial_layout_is_working_clock_and_no_debug_override_can_reintroduce_gate(self):
        info = ET.parse(ROOT / 'main/res/xml/digital_clock_widget_info.xml').getroot()
        self.assertEqual('@layout/widget_digital_initial', info.get(A+'initialLayout'))
        self.assertEqual('TextClock', ET.parse(ROOT / 'main/res/layout/widget_digital_initial.xml').getroot().tag)
        self.assertFalse((ROOT / 'main/res/layout/widget_native_font_unavailable.xml').exists())
        self.assertFalse(list((ROOT / 'debug/res').glob('**/widget_live_digital*.xml')))
