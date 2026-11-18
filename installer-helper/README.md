# Expressive Clock Installer Helper

Standalone Android companion: **Expressive Clock Installer Helper**, `app.expressiveclock.installer`.
Source is an adaptation of `~/Development/Expressive_Island/installer-helper` and its private
Actions delivery tooling. Island's source, credentials, notification topic and app are unchanged.
This independent Gradle root is NOT included in Clock's production APK/AAB or root settings.

## Design and trust model

Same responsive Compose presentation, Google Sans Flex, charcoal status card, light/dark Clock
teal palette, mechanical progress, press feedback, connection controls, Cancel and Retry.
Verified build version/name, UTC date, run ID and size appear in the status card. Launching from
a notification discovers only its exact reviewed run/artifact pair; this is not a general APK browser.

`expressive-clock-install://artifact?run=<id>&artifact=<id>&manifest=<id>&build=<32hex>&sha=<64hex>`

Only `samuelbeckman/expressive-clock`, default-branch successful `workflow_dispatch` runs titled
`Local APK <build>` and `.github/workflows/local-debug-apk.yml` are accepted. Both unexpired artifact
IDs must occur in that run with exact names `ExpressiveClock-debug.apk` and `build-manifest.json`.
Authenticated manifest evidence binds run/build/artifact/hash/time/package/certificate/version/size.
Seven-day validity, bounded HTTPS responses, bounded ZIP expansion and streamed private cache files
are inherited. GitHub bearer credentials go only to api.github.com, never storage redirect hosts.
A notification is untrusted; no notification-supplied URL, package, signer or alternate repo is used.
SHA-256 provides integrity, not authenticity on its own. Repo writers and reviewed workflow editors
are trusted. APK signatures are additionally verified cryptographically with Android apksig.

Public trust anchors in `trust.properties` were extracted with apksigner from the verified current
Mac Clock debug APK. Target is **app.expressiveclock.debug**, never production `app.expressiveclock`
or either Island package. Certificate SHA-256:
`1469909d722636651745b20f58b12343a26802d1428e50fb8e3fa667d3cf1952`.
Only public fingerprints are stored. No keystore/private key is copied into the helper or Actions.
A different developer's debug key requires a deliberate reviewed trust update, never a link override.

Before installation, check the candidate AND installed package/signature, version code and build
age. Lower version codes are rejected. Same-version different APKs older than the installed app's
last-update time are conservatively rejected (including uncertain older local builds). Exact installed
APK retries are allowed. No downgrade strategy is implemented; no uninstall or clear-data exists.
Android rechecks installation compatibility and retains data for compatible updates.

One serial worker owns authentication/download/verification. Generation checks invalidate cancelled
callbacks; late refreshes cannot restore disconnected tokens. Rotation replaces the UI observer.
A new link does not interrupt an ongoing operation; Cancel then reopen/retry. Process death loses an
in-flight download safely; reopen the notification for fresh evidence and verified-cache reuse.

Unlike Island's optional external installer preference, Clock uses standard **PackageInstaller
sessions**, with USER_ACTION_REQUIRED on API 31+, Android's unknown-source permission and an explicit
non-exported result receiver. This reports actual Android success/cancel/failure rather than guessing
from return to the app. No silent install, root, Shizuku, arbitrary APK viewer or FileProvider sharing.
If backgrounded before commit, staging is abandoned and the user taps Install on return. A pending
session/result is correlated in private preferences across process recreation. Android permission
approval does not itself approve an install: return and tap Install to see the confirmation.

## Build locally

```
ANDROID_HOME="$HOME/Library/Android/sdk" bash installer-helper/gradlew -p installer-helper \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
python3 -m unittest discover -s installer-helper/tools -p 'test_*.py'
```

APK: `installer-helper/app/build/outputs/apk/debug/app-debug.apk`. Uses Island's existing toolchain
versions, min SDK 26, target 36, compile 37. Install once with Android's normal installer or ADB.
Both helper apps coexist. Clock production signing, dependencies and version remain untouched.

## GitHub authentication setup — pending owner action

No Clock GitHub App has been registered or credentials generated/replaced by this task.
Create a dedicated GitHub App for Clock (or explicitly approve extending the existing App's selected
repository access). Enable device flow and expiring user tokens, disable unnecessary webhooks, grant
only repository **Actions: read** and implicit Metadata read, install only on the intended repository.
Enter its PUBLIC client ID in Connection settings; Connect GitHub → Open GitHub authorization → enter
the code and approve in the browser. Do not enter a PAT/client secret. The helper starts with no
inherited Island client ID or tokens. App permissions and repo installation, not an OAuth scope
string in the phone, limit access. AES-GCM tokens are protected by a non-exportable Android Keystore
key, in internal preferences; backups/transfers exclude all app data. Disconnect removes local tokens;
revoke GitHub App authorization on GitHub separately if needed.

References: [GitHub device flow](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-user-access-token-for-a-github-app),
[Actions artifact API](https://docs.github.com/en/rest/actions/artifacts),
[Android PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller).

## Actions activation and signing — NOT activated

Local workflow: `../.github/workflows/local-debug-apk.yml`. Manual dispatch ONLY; no push, PR,
release or production-build trigger. Seven-day APK/manifest artifacts, no GitHub Release. It uploads
an immutable, fresh, verified **local debug APK**, not a GitHub-hosted rebuild signed with a new key.
This delivers uncommitted development changes without uploading source or signing credentials.
An opt-in one-job Mac runner uses a unique build label, no standing service and no checkout. Local
preflight rejects changed remote workflow bytes, unexpected collaborators/other active workflows
or runs, and wrong dispatch commit. A verified, unconfigured official runner distribution must be
approved/provided by the owner. Mac-only existing gh login needs Actions dispatch and runner admin;
the PHONE app only needs Actions read. Nothing registers a runner on import or local-only builds.

**Clock's repository is PUBLIC. Actions artifacts are not confidential merely because API downloads
require login. Anyone with repository access can obtain builds.** No visibility changes were made.
Do not approve uploads if that exposure is unacceptable. Use an approved private distribution repo
and deliberately review/rebuild trust policy instead; the current helper pins the requested repo.

After reviewing/approving the workflow commit/push separately, activate external files (not in Git):
`~/.local/state/expressive-clock/delivery-config.json` (chmod 600):

```json
{"enabled": true, "mode": "ephemeral", "allow_public_artifacts": true,
 "runner_distribution": "/absolute/path/to/verified-unconfigured-actions-runner"}
```

No activation file is created automatically. Public exposure requires that explicit flag; no
workflow dispatch occurs unless both `--deliver` and approved config are present.

## ntfy setup — NOT activated

Subscribe in ntfy to a dedicated, owner-chosen `expressive-clock-<at-least-8-random-characters>` topic.
Keep it separate from Island. Topic names are not authentication; prefer access-controlled topics.
`~/.local/state/expressive-clock/notification-config.json` (chmod 600):

```json
{"enabled": true, "topic": "expressive-clock-REPLACE_WITH_RANDOM_SUFFIX"}
```

For a protected ntfy.sh topic optionally add `publish_token` in that external file; never commit it.
The adapted sender posts to HTTPS ntfy.sh only. It adds **Install APK** and click actions only after
fresh remote manifest AND APK hash verification. The link carries IDs/hash only, no credentials or
signed storage URL. Custom schemes can be claimed by other apps; choose Clock Installer Helper in
Android's chooser. No notifications were sent during implementation. No automatic recurring job exists.

Local-only build/test/stage (never uploads/notifies):

```
python3 installer-helper/tools/finish_debug_pass.py --pass-id '<unique-id>' --summary 'Brief sanitized changes'
```

After approval and activation, add `--deliver --notify`. `--deliver` alone uploads without notifying.
The command tests/builds the MAIN Clock app, verifies identity/signature, makes a uniquely named
Downloads copy and read-only spool snapshot/receipt outside Git, then optionally delivers/notifies.
A pass ID is claimed once: inspect evidence after interrupted runs rather than blindly repeating.
Receipts expire for upload after one hour; remote artifacts/manifests after seven days. Expired
links report unavailable; build a fresh pass. No permanent services or automatic cleanup of local
spool evidence. Owner can remove completed spool folders after seven days when no job is pending.

## Verification and remaining limits

See [implementation report](../docs/INSTALLER_HELPER_IMPLEMENTATION.md) for actual test results and
APK delivery path. Live Clock OAuth, authenticated artifact download, ntfy and final installation
remain pending activation; do not mistake fixture/UI tests for a live end-to-end test. Generic errors
never display response bodies/tokens/signed URLs. Rotation, expiry and cancellation should also be
exercised against a real artifact after activation. Do not approve an incompatible update or uninstall
the main app to make a test pass.
