"""Opt-in, one-job Actions runner. Importing this module never registers anything."""
from contextlib import contextmanager
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import tempfile

CONFIG = Path.home()/'.local/state/expressive-clock/delivery-config.json'

@contextmanager
def runner(build_id, api):
    # This file is intentionally NOT created by setup. Explicit user approval must precede it.
    config=json.loads(CONFIG.read_text()) if CONFIG.is_file() else {}
    if config.get('enabled') is not True or config.get('mode') != 'ephemeral':
        raise RuntimeError('Private delivery inactive; activation approval required')
    distribution=Path(config['runner_distribution']).expanduser().resolve()
    if not (distribution/'config.sh').is_file() or not (distribution/'run.sh').is_file():
        raise RuntimeError('Approved runner distribution unavailable')
    if any((distribution/x).exists() for x in ('.runner','.credentials','.credentials_rsaparams')):
        raise ValueError('Use an unconfigured, verified runner distribution')
    name='ec-apk-'+build_id
    # Unique label matches only this dispatch; no standing listener/service or shared runner label.
    with tempfile.TemporaryDirectory(prefix='ec-runner-') as temp:
        folder=Path(temp)/'runner';shutil.copytree(distribution,folder)
        process=None
        try:
            token=api('repos/samuelbeckman/expressive-clock/actions/runners/registration-token','-X','POST')['token']
            # Do not forward the coding session's GitHub/API credentials into Actions jobs.
            safe_env={key:os.environ[key] for key in ('PATH','HOME','USER','LOGNAME','SHELL','LANG','LC_ALL','TMPDIR') if key in os.environ}
            env=safe_env.copy();env['ACTIONS_RUNNER_INPUT_TOKEN']=token
            result=subprocess.run(['./config.sh','--unattended','--ephemeral','--disableupdate',
                '--url','https://github.com/samuelbeckman/expressive-clock','--name',name,
                '--no-default-labels','--labels',name,'--work','_work'],cwd=folder,env=env,
                stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=60)
            del env['ACTIONS_RUNNER_INPUT_TOKEN'];del token
            if result.returncode:raise RuntimeError('Ephemeral runner configuration failed')
            # Retain bounded diagnostics in private temp only, removed with all runner credentials.
            with (folder/'local-run.log').open('wb') as log:
                process=subprocess.Popen(['./run.sh'],cwd=folder,env=safe_env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
                yield
        finally:
            if process and process.poll() is None:
                try:
                    os.killpg(process.pid,signal.SIGTERM);process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
                except ProcessLookupError:pass
            # Successful ephemeral runners deregister themselves. Also remove idle/failed registrations.
            try:
                registrations=api('repos/samuelbeckman/expressive-clock/actions/runners?per_page=100')
                for item in registrations.get('runners',[]):
                    if item.get('name')==name:
                        api(f"repos/samuelbeckman/expressive-clock/actions/runners/{item['id']}",'-X','DELETE')
            except Exception:
                print('Runner cleanup could not be confirmed; check the repository Actions runner list')
