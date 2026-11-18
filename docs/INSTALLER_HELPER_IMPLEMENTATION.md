# Expressive Clock Installer Helper — implementation report

## Delivered locally

- APK: [ExpressiveClock-Installer-debug.apk](/Users/sambeckman/Downloads/ExpressiveClock-Installer-debug.apk), SHA-256 `148022ef0e33523702420741abf1ff2457d198346407bf3731921b9e0ad11cf5`. APK signature verified with apksigner; package `app.expressiveclock.installer`, version `0.1-local` (code 1). Installed alongside Clock on both devices.
- Source/build/setup: [installer-helper](../installer-helper/README.md), independent Gradle root. No inclusion in main settings or production APK/AAB.
- Workflow: [.github/workflows/local-debug-apk.yml](../.github/workflows/local-debug-apk.yml), new manual-dispatch-only seven-day APK/manifest upload. Not committed, pushed, dispatched or activated.
- Evidence: [ExpressiveClock-Installer-evidence-20261117-193648](/Users/sambeckman/Downloads/ExpressiveClock-Installer-evidence-20261117-193648). Light/dark screenshots are isolated Compose fixture states, not proof of real GitHub authorization or downloading.

## Reuse and changes

Read Expressive Island's installer-helper Java controller/security code, Compose UI/theme/motion/progress, trust/build configuration, documentation, local-debug-apk workflow and apk_delivery/delivery_preflight/ephemeral runner/notification/completion tooling. Reused and adapted those components locally. Island files and configuration were read only and remain unchanged.

Clock-specific app ID, repository pin, target package, certificate, URI scheme, Keystore alias, runner labels/spool/config paths and ntfy topic validation isolate the apps. No Island public client ID or activated configuration is inherited. A dedicated Clock GitHub App remains to be registered/approved by the owner.

Retained streamed bounded downloads, checksum/manifest/run/artifact validation, Keystore AES-GCM credential storage, token refresh, generation-based cancellation, cache revalidation, seven-day expiry and original expressive visual design. Added apksig cryptographic APK signature verification, signed package/version/size agreement, installed signer matching and downgrade rejection. Metadata includes actual version name/code, build timestamp, run and APK size. Existing app version remains 0.1.4-debug/code 7 as configured; no version bump was made.

Deliberate change from Island's latest optional external APK handler: standard Android PackageInstaller sessions with explicit confirmation and a non-exported result receiver. This allows actual success/cancel/failure reporting. It does not infer installation from an Activity resume. No uninstall/clear-data/downgrade workaround exists. Different installed signers and older builds get explicit rejection messages. Same-code different APKs with timestamps older than the installed last-update time are conservatively rejected.

## Verification

| Check | Result |
|---|---|
| Helper testDebugUnitTest | 57 passed, zero failures/errors |
| Helper assembleDebug / lintDebug / assembleDebugAndroidTest | Successful; lint has nonfatal SDK/dependency/style suggestions |
| Python delivery/preflight/runner/notification tests | 21 passed, mocked network/runner operations |
| Pixel 11 Pro Fold instrumentation | 18 passed: 12 Compose UI, 1 launcher smoke, 5 real verification/Keystore tests |
| OPPO Find N6 instrumentation | 18 passed, same suite |
| Final security-message build verification | Five verification tests repeated successfully on each phone |
| Main assembleDebug / testDebugUnitTest | Successful, 55 passed |
| Canonical local-only finish_debug_pass command | Fresh main tests/build, signer/package/hash verified, unique Downloads copy and immutable local receipt; no upload/notification |
| Workflow YAML and embedded Python | Parsed/compiled locally |
| git diff --check | Passed |

Device verification accepted each phone's actual installed Clock debug APK without installing it; rejected the helper APK, changed bytes and tampered APK even with a recomputed advertised hash. Keystore round trip and revocation against late saves passed. UI fixtures exercised download progress, Cancel, Retry, phase transitions, large fonts, dark/light contrast and animations. Inspected rendered light/dark screenshots on both phones. Isolated fixture states show a simulated connection, never actual tokens.

Before/after ADB package inspection confirmed Clock's installation paths, version codes and last-update timestamps unchanged on both phones. No Clock or Island app was replaced/uninstalled/cleared. Only the new helper and its instrumentation APK were installed. Device posture, animation and theme settings were not changed for this task.

## Pending activation / limitations

1. Owner approves/registers a Clock GitHub App (device flow, expiring user tokens, Actions read, selected repository access), enters its public client ID and authorizes on each phone. No GitHub credentials were created/replaced.
2. Owner reviews and separately approves workflow commit/push, runner registration/admin privileges and external delivery activation. No standing runner/service is installed. Preflight intentionally stops for unexpected workflows/collaborators/runs.
3. **Repository is public. Actions artifacts are not confidential.** Approve that exposure via external `allow_public_artifacts` configuration before delivery, or request a separate private distribution design. Visibility and Releases were not changed.
4. Owner selects/subscribes to a dedicated Clock ntfy topic and approves external notification configuration. No messages were published. Canonical local build is the default; `--deliver --notify` is explicit and configuration-gated.
5. Live OAuth/refresh/expiry, artifact discovery/download cancellation on real network traffic, unavailable remote artifacts, ntfy action routing and actual Android installation confirmation/result remain untested until activation. Rotation/background/process-death behaviour needs extended live testing. Tests do not substitute for a successful authenticated end-to-end update.

See README for exact setup/config/commands. Official references reviewed: [GitHub device flow](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-user-access-token-for-a-github-app), [Actions artifacts](https://docs.github.com/en/rest/actions/artifacts), [Android PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller).

## Repository safety / file inventory

Created the installer-helper tree (app source/resources/tests, independent Gradle tooling, public trust.properties, adapted Python tools and README files), the dedicated workflow, and this report. No pre-existing source/config/documentation was modified or deleted. SHA-256 comparison against the pre-task source snapshot found zero changes to existing files. All prior uncommitted v0.1.5 functionality remains intact.

Final branch: **main**. Working tree remains dirty with all prior modified/untracked work plus new `.github/`, `installer-helper/` and this report. Nothing staged, committed, pushed, merged, released or published. No signing credentials/keystores/access tokens added to source.
