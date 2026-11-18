"""Fail closed before exposing a temporary Mac runner to a repository's job queue."""
import base64
import json
from pathlib import Path

REPO = 'samuelbeckman/expressive-clock'
WORKFLOW = '.github/workflows/local-debug-apk.yml'


def inspect(api, root):
    repo = api(f'repos/{REPO}')
    if repo.get('owner', {}).get('login') != 'samuelbeckman':
        raise ValueError('Unexpected repository identity or visibility')
    if repo.get('private') is not True:
        config_path=Path.home()/'.local/state/expressive-clock/delivery-config.json'
        config=json.loads(config_path.read_text()) if config_path.is_file() else {}
        if config.get('allow_public_artifacts') is not True:
            raise ValueError('Public repository artifacts require explicit exposure approval')
    # Personal repository runners have no workflow-scoped runner groups. Only proceed in this
    # owner-only repo with exactly the reviewed dispatch-only workflow; do not silently relax it.
    collaborators = api(f'repos/{REPO}/collaborators?per_page=100')
    if len(collaborators) >= 100 or any(c.get('login') != 'samuelbeckman' for c in collaborators):
        raise ValueError('Runner activation requires review of repository collaborators')
    workflows = api(f'repos/{REPO}/actions/workflows?per_page=100')
    active = [w for w in workflows.get('workflows', []) if w.get('state') == 'active']
    if workflows.get('total_count', 0) > 100 or len(active) != 1 or active[0].get('path') != WORKFLOW:
        raise ValueError('Unexpected active workflows; temporary runner will not start')
    branch = api(f"repos/{REPO}/branches/{repo['default_branch']}")
    commit = branch['commit']['sha']
    source = api(f'repos/{REPO}/contents/{WORKFLOW}?ref={commit}')
    if source.get('encoding') != 'base64' or base64.b64decode(source['content']) != (Path(root)/WORKFLOW).read_bytes():
        raise ValueError('Remote upload workflow differs from the reviewed local workflow')
    no_other_runs(api)
    return repo, commit


def no_other_runs(api, allowed=None):
    # Query each live status explicitly: an old queued run must not hide behind 100 newer runs.
    for status in ('queued', 'in_progress', 'waiting', 'pending', 'requested'):
        runs = api(f'repos/{REPO}/actions/runs?status={status}&per_page=100')
        if any(r.get('id') != allowed for r in runs.get('workflow_runs', [])):
            raise ValueError('Another Actions run is active; temporary runner will not start')


def check_run(run, repo, commit, build_id):
    if (run.get('event') != 'workflow_dispatch' or run.get('path') != WORKFLOW
        or run.get('head_branch') != repo['default_branch'] or run.get('head_sha') != commit
        or run.get('head_repository', {}).get('full_name') != REPO
        or run.get('display_title') != 'Local APK '+build_id):
        raise ValueError('Unexpected dispatched run; temporary runner will not start')
