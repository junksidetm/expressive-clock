import contextlib
import hashlib
import io
import json
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import patch
import zipfile
import apk_delivery as delivery
import notify_completion as notify

class DeliveryTests(unittest.TestCase):
    def receipt(self, root, **changes):
        blob=b'local-test-bytes'
        (root/'ExpressiveClock-debug.apk').write_bytes(blob)
        data=dict(build_id='a'*32,pass_id='pass',sha256=hashlib.sha256(blob).hexdigest(),build='success',test_failures=0,tests=3,built_epoch=time.time(),built_at='2026-11-17T00:00:00Z',package_name=delivery.trust()['package'],certificate_sha256=delivery.trust()['certificate'],version_code=7,version_name='0.1.4-debug',apk_bytes=len(blob))|changes
        p=root/'receipt.json';p.write_text(json.dumps(data));return p
    def test_receipt_and_changed_bytes(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);p=self.receipt(root);delivery.validate_receipt(p,'pass')
            (root/'ExpressiveClock-debug.apk').write_bytes(b'wrong')
            with self.assertRaises(ValueError):delivery.validate_receipt(p,'pass')
    def test_stale_wrong_pass_failed_tests_and_wrong_identity(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            for changes in [dict(built_epoch=time.time()-4000),dict(pass_id='other'),dict(test_failures=1),dict(package_name='app.expressiveisland.debug'),dict(certificate_sha256='0'*64)]:
                with self.assertRaises(ValueError):delivery.validate_receipt(self.receipt(root,**changes),'pass')
    def test_transport_accepts_only_expected_apk(self):
        def archive(name):
            out=io.BytesIO()
            with zipfile.ZipFile(out,'w') as z:z.writestr(name,b'bytes')
            return out.getvalue()
        self.assertEqual(b'bytes',delivery.downloaded_apk(archive('ExpressiveClock-debug.apk')))
        with self.assertRaises(ValueError):delivery.downloaded_apk(archive('ExpressiveIsland-debug.apk'))
    def test_no_dispatch_without_activation(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);p=self.receipt(root)
            with patch.object(Path,'home',return_value=root),patch.object(delivery,'gh') as api:
                with self.assertRaises(RuntimeError):delivery.upload_receipt(p,'pass')
                api.assert_not_called()
    def test_notification_without_config_does_not_publish(self):
        from argparse import Namespace
        with tempfile.TemporaryDirectory() as d,patch.object(notify,'CONFIG',Path(d)/'missing'),contextlib.redirect_stderr(io.StringIO()):
            with patch.object(notify.urllib.request,'urlopen') as opener:
                notify.send(Namespace(),state=Path(d)/'state',opener=opener)
                opener.assert_not_called()
    def test_other_helper_notification_topic_is_rejected(self):
        from argparse import Namespace
        with tempfile.TemporaryDirectory() as d,contextlib.redirect_stderr(io.StringIO()):
            p=Path(d)/'config';p.write_text(json.dumps(dict(enabled=True,topic='expressive-island-1234')))
            with patch.object(notify,'CONFIG',p),patch.object(notify.urllib.request,'urlopen') as opener:
                notify.send(Namespace(),state=Path(d)/'state',opener=opener)
                opener.assert_not_called()
