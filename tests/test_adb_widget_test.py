"""Tests for device evidence assertions; no device or ADB commands are executed."""
import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location(
    'adb_widget_test', Path(__file__).resolve().parents[1] / 'scripts/adb_widget_test.py')
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


def clocks(hour, minute):
    return [{'resource-id': 'app.expressiveclock.debug:id/live_hours', 'text': hour},
            {'resource-id': 'app.expressiveclock.debug:id/live_minutes', 'text': minute}]


class DigitalEvidenceTest(unittest.TestCase):
    def test_frozen_minute_fails(self):
        self.assertEqual('FAIL', MODULE.digital_status(clocks('11', '05'), '11:06:10', '11:06:12'))

    def test_missing_widget_is_not_a_pass(self):
        self.assertEqual('NOT_VISIBLE', MODULE.digital_status([], '11:06:10', '11:06:12'))

    def test_capture_can_cross_midnight_and_use_twelve_hour_format(self):
        self.assertEqual('PASS', MODULE.digital_status(clocks('12', '00'), '23:59:59', '00:00:02'))

    def test_seconds_field_is_accepted_without_claiming_second_accuracy(self):
        self.assertEqual('PASS', MODULE.digital_status(clocks('11', '06:11'), '11:06:10', '11:06:12'))


class FoldDisplayTest(unittest.TestCase):
    def test_uses_active_default_panel_not_first_panel(self):
        dump = ("DisplayViewport{isActive=false, displayId=1, uniqueId='local:111'} "
                "DisplayViewport{isActive=true, displayId=0, uniqueId='local:222'}")
        self.assertEqual('222', MODULE.active_display_id(dump))

    def test_missing_active_panel_is_not_guessed(self):
        self.assertIsNone(MODULE.active_display_id("DisplayViewport{isActive=false, displayId=0}"))
