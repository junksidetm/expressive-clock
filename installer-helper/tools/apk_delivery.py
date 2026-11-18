"""Private Actions delivery for a verified, immutable LOCAL build snapshot; no releases/pushes."""
import delivery_preflight
from contextlib import ExitStack
import ephemeral_apk_runner
import hashlib
import json as _config_json
import io
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time
import zipfile

REPO = "samuelbeckman/expressive-clock"
WORKFLOW = "local-debug-apk.yml"
SPOOL = Path.home() / ".local/state/expressive-clock/apk-delivery"


def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def validate_receipt(path, pass_id):
    path = Path(path).resolve()
    data = json.loads(path.read_text())
    if not re.fullmatch(r"[a-f0-9]{32}", data["build_id"]) or not re.fullmatch(r"[a-f0-9]{64}", data["sha256"]):
        raise ValueError("Invalid build identity")
    if data["pass_id"] != pass_id or data["build"] != "success" or data["test_failures"] != 0:
        raise ValueError("Build evidence does not match successful pass")
    if time.time() - data["built_epoch"] > 3600 or data["built_epoch"] > time.time() + 60:
        raise ValueError("Build receipt is stale")
    allowed=trust()
    if data.get('package_name')!=allowed['package'] or data.get('certificate_sha256')!=allowed['certificate']:
        raise ValueError('Missing or untrusted package/signature evidence')
    staged = path.parent / "ExpressiveClock-debug.apk"
    if staged.is_symlink() or sha(staged) != data["sha256"]:
        raise ValueError("Staged APK changed")
    return data, staged


def gh_path():
    found=shutil.which('gh')
    fallback=Path.home()/'.local/bin/gh'
    return found or (str(fallback) if fallback.is_file() else None)


def gh(*args, raw=False):
    # gh reads its existing Keychain/config auth. Never request/print a token or stderr.
    result = subprocess.run([gh_path(), "api", "--hostname", "github.com", *args], capture_output=True, timeout=45)
    if result.returncode: raise RuntimeError("GitHub API request failed")
    return result.stdout if raw else json.loads(result.stdout or b"null")


def downloaded_apk(blob):
    # Raw APK is itself ZIP-based. Only unwrap a transport ZIP containing exactly our APK.
    with zipfile.ZipFile(io.BytesIO(blob)) as archive:
        names = archive.namelist()
        if "AndroidManifest.xml" in names: return blob
        if names == ["ExpressiveClock-debug.apk"]: return archive.read(names[0])
    raise ValueError("Unexpected download content")


def upload_receipt(path, pass_id):
    data, _ = validate_receipt(path, pass_id)
    config_path=Path.home()/'.local/state/expressive-clock/delivery-config.json'
    config=_config_json.loads(config_path.read_text()) if config_path.is_file() else {}
    if config.get('enabled') is not True or config.get('mode') != 'ephemeral':
        raise RuntimeError('Delivery inactive; activation approval required')
    if not gh_path(): raise RuntimeError("GitHub CLI unavailable; activation pending")
    repository, commit = delivery_preflight.inspect(gh, Path(__file__).resolve().parents[2])
    cache = Path(path).with_name('remote.json')
    if cache.exists():
        # Reuse only after fresh API/byte verification, never trust a local cached URL blindly.
        previous = json.loads(cache.read_text())
        run = gh(f"repos/{REPO}/actions/runs/{previous['run_id']}")
        delivery_preflight.check_run(run, repository, commit, data['build_id'])
        if run.get('status') != 'completed' or run.get('conclusion') != 'success':
            raise ValueError('Previously uploaded run is no longer successful')
        return _verified(data, run)
    result = _dispatch(data, repository, commit)
    pending=cache.with_suffix('.tmp')
    pending.write_text(json.dumps(result));pending.chmod(0o600);pending.replace(cache)
    return result


def _dispatch(data, repository, commit):
    # An opt-in one-job same-Mac runner reads only this immutable snapshot. No public fetch URL or local server.
    gh(f"repos/{REPO}/actions/workflows/{WORKFLOW}/dispatches", "-X", "POST",
       "-f", f"ref={repository['default_branch']}", "-f", f"inputs[build_id]={data['build_id']}",
       "-f", f"inputs[sha256]={data['sha256']}")
    run = None
    try:
        with ExitStack() as lifetime:
            started_runner = False
            deadline = time.monotonic() + 180
            while time.monotonic() < deadline:
                if run is None:
                    runs = gh(f"repos/{REPO}/actions/workflows/{WORKFLOW}/runs?event=workflow_dispatch&per_page=100")
                    matches = [r for r in runs.get("workflow_runs", []) if r.get("display_title") == f"Local APK {data['build_id']}"]
                    if len(matches) > 1: raise ValueError("Ambiguous upload run")
                    if matches: run = matches[0]
                else:
                    run = gh(f"repos/{REPO}/actions/runs/{run['id']}")
                if run:
                    delivery_preflight.check_run(run, repository, commit, data['build_id'])
                    if not started_runner and run['status'] != 'completed':
                        delivery_preflight.no_other_runs(gh, run['id'])
                        lifetime.enter_context(ephemeral_apk_runner.runner(data['build_id'], gh))
                        started_runner = True
                if run and run["status"] == "completed":
                    if run["conclusion"] != "success": raise RuntimeError("Upload workflow failed")
                    return _verified(data, run)
                time.sleep(3)
            raise RuntimeError("Upload queued or timed out; remote unavailable")
    except BaseException:
        if run and run.get('status') != 'completed':
            try: gh(f"repos/{REPO}/actions/runs/{run['id']}/cancel", "-X", "POST")
            except Exception: pass
        raise


def _verified(data, run):
    artifacts = gh(f"repos/{REPO}/actions/runs/{run['id']}/artifacts")
    matches = [a for a in artifacts.get("artifacts", []) if a["name"] == "ExpressiveClock-debug.apk" and not a["expired"]]
    if len(matches) != 1: raise ValueError("Expected artifact unavailable")
    artifact = matches[0]
    manifests = [a for a in artifacts.get("artifacts", []) if a["name"] == "build-manifest.json" and not a["expired"]]
    if len(manifests) != 1: raise ValueError("Trusted build manifest unavailable")
    manifest_artifact = manifests[0]
    manifest = json.loads(download_manifest(gh(f"repos/{REPO}/actions/artifacts/{manifest_artifact['id']}/zip", raw=True)))
    expected = {"schema":1,"build_id":data["build_id"],"sha256":data["sha256"],"built_at":data["built_at"],
        "run_id":run['id'],"artifact_id":artifact['id'],"package_name":data['package_name'],"certificate_sha256":data['certificate_sha256'],"version_name":data['version_name'],"version_code":data['version_code'],"apk_bytes":data['apk_bytes']}
    if manifest != expected: raise ValueError("Remote build manifest mismatch")
    blob = gh(f"repos/{REPO}/actions/artifacts/{artifact['id']}/zip", raw=True)
    if hashlib.sha256(downloaded_apk(blob)).hexdigest() != data["sha256"]:
        raise ValueError("Remote APK hash mismatch")
    url = f"https://github.com/{REPO}/actions/runs/{run['id']}/artifacts/{artifact['id']}"
    return {"url": url, "sha256": data["sha256"], "built_at": data["built_at"], "artifact_id": artifact["id"], "run_id": run["id"], "head_sha": run["head_sha"], "install_url":
        f"expressive-clock-install://artifact?run={run['id']}&artifact={artifact['id']}&manifest={manifest_artifact['id']}&build={data['build_id']}&sha={data['sha256']}"}


def download_manifest(blob):
    if blob.startswith(b'{'): return blob
    with zipfile.ZipFile(io.BytesIO(blob)) as archive:
        if archive.namelist() == ['build-manifest.json'] and archive.getinfo('build-manifest.json').file_size <= 65536:
            return archive.read('build-manifest.json')
    raise ValueError('Unexpected manifest transport')


def trust():
    values = {}
    for line in (Path(__file__).resolve().parents[2]/'installer-helper/trust.properties').read_text().splitlines():
        if line and not line.startswith('#'):
            key,value=line.split('=',1);values[key]=value
    return values


def verify_apk(path):
    root=Path(__file__).resolve().parents[2]
    sdk=Path.home()/'Library/Android/sdk'
    # Read only the SDK location, never signing configuration or environment secrets.
    local=root/'local.properties'
    if local.is_file():
        for line in local.read_text().splitlines():
            if line.startswith('sdk.dir='): sdk=Path(line.split('=',1)[1])
    binaries=sdk/'build-tools/36.0.0'
    cert=subprocess.run([str(binaries/'apksigner'),'verify','--print-certs',str(path)],capture_output=True,text=True,timeout=30)
    package=subprocess.run([str(binaries/'aapt2'),'dump','badging',str(path)],capture_output=True,text=True,timeout=30)
    pins=re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-f0-9]{64})',cert.stdout)
    name=re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",package.stdout)
    allowed=trust()
    if cert.returncode or package.returncode or pins != [allowed['certificate']] or not name or name[1]!=allowed['package']:
        raise ValueError('APK package/signature does not match helper trust policy')
    return {'package_name':name[1], 'certificate_sha256':pins[0], 'version_code':int(name[2]), 'version_name':name[3], 'apk_bytes':Path(path).stat().st_size}
