import contextlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock,patch
import ephemeral_apk_runner as runner
import apk_delivery

class EphemeralRunnerTest(unittest.TestCase):
    def test_inactive_by_default_and_no_registration(self):
        with tempfile.TemporaryDirectory() as d,patch.object(runner,'CONFIG',Path(d)/'missing'):
            api=Mock()
            with self.assertRaises(RuntimeError):
                with runner.runner('a'*32,api): pass
            api.assert_not_called()
    def config(self,root):
        dist=root/'distribution';dist.mkdir()
        (dist/'config.sh').write_text('');(dist/'run.sh').write_text('')
        config=root/'config.json';config.write_text(json.dumps(dict(enabled=True,mode='ephemeral',runner_distribution=str(dist))))
        return config,dist
    def test_rejects_existing_registered_distribution(self):
        with tempfile.TemporaryDirectory() as d:
            config,dist=self.config(Path(d));(dist/'.credentials').write_text('not-a-real-secret')
            with patch.object(runner,'CONFIG',config):
                api=Mock()
                with self.assertRaises(ValueError):
                    with runner.runner('a'*32,api):pass
                api.assert_not_called()
    def test_one_job_registration_uses_environment_not_token_argument(self):
        with tempfile.TemporaryDirectory() as d:
            config,_=self.config(Path(d));name='ec-apk-'+'a'*32
            api=Mock(side_effect=[{'token':'synthetic-test-token'},{'runners':[{'name':name,'id':7}]},None])
            proc=Mock();proc.poll.return_value=None
            with patch.object(runner,'CONFIG',config),patch.object(runner.subprocess,'run',return_value=Mock(returncode=0)) as configure,patch.object(runner.subprocess,'Popen',return_value=proc) as launch,patch.object(runner.os,'killpg'),patch.dict(runner.os.environ,{'GH_TOKEN':'synthetic-never-forward'}):
                with runner.runner('a'*32,api):pass
            args=configure.call_args.args[0]
            self.assertIn('--ephemeral',args);self.assertNotIn('--token',args);self.assertNotIn('synthetic-test-token',args)
            self.assertEqual('DELETE',api.call_args.args[-1])
            proc.wait.assert_called_once()
            self.assertNotIn('GH_TOKEN',launch.call_args.kwargs['env'])
            self.assertNotIn('ACTIONS_RUNNER_INPUT_TOKEN',launch.call_args.kwargs['env'])
    def test_failed_configuration_cleans_registration(self):
        with tempfile.TemporaryDirectory() as d:
            config,_=self.config(Path(d));api=Mock(side_effect=[{'token':'synthetic'},{'runners':[]}])
            with patch.object(runner,'CONFIG',config),patch.object(runner.subprocess,'run',return_value=Mock(returncode=1)):
                with self.assertRaises(RuntimeError):
                    with runner.runner('a'*32,api):pass
            self.assertEqual(2,api.call_count)
    def test_manifest_zip_rejects_other_files(self):
        import io,zipfile
        b=io.BytesIO()
        with zipfile.ZipFile(b,'w') as z:z.writestr('wrong.json','{}')
        with self.assertRaises(ValueError):apk_delivery.download_manifest(b.getvalue())
