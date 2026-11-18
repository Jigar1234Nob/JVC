# JVC release and deployment guide

## Current delivery model

JVC is a shared React shell around the existing chat service at https://chat.jigar.au.

- Windows and macOS desktop clients use Tauri 2 and the operating system WebView. JVC is not an Electron app, so Electron permission hooks do not apply; the equivalent controls live in the Tauri Rust window builder.
- Android uses Capacitor 7 and Android System WebView.
- The startup screen checks the latest non-draft GitHub Release with an 8-second timeout and offers the matching platform package. The check fails open so an unavailable GitHub API never prevents chat access. Android users can skip an offered update; desktop users can continue on the installed version.
- Release assets are distributed from https://github.com/Jigar1234Nob/JVC/releases.

The 2.0.1 build candidate produces unsigned Windows/macOS installers, a debug-signed Android APK for sideload QA, and an unsigned Android AAB for the later Play signing step. These are build outputs, not production-signed packages. Tag builds now create a **draft GitHub Release** so unsigned packages are not accidentally exposed as the latest stable update.

## Embedding and site headers

JVC loads https://chat.jigar.au as a **top-level page** in the native WebView. It does not use an HTML iframe. The earlier iframe approach was not reliable, so the shared shell redirects the WebView directly to the chat origin.

This distinction matters:

- `X-Frame-Options` and CSP `frame-ancestors` control whether a page may be displayed inside a frame. They do not prevent a normal top-level navigation to the same URL.
- No site-side framing-header change is required for the current direct-navigation architecture.
- A top-level WebView still needs the site to be reachable over valid HTTPS and not blocked by unrelated access controls. The current site response and media behavior have **not** been certified.

**Live-header verification attempt, 17 November 2026:** the build container's `curl -sSIL https://chat.jigar.au/` failed with `Could not resolve host`, and the available browser fetch also reported the site as inaccessible. Therefore the actual redirect chain and response-header values could not be observed. Do not treat the site-side check as passed.

When network access is available, run:

~~~sh
curl -sSIL --max-time 20 https://chat.jigar.au/
~~~

Inspect every redirect and the final response for:

- `X-Frame-Options`
- `Content-Security-Policy`, especially `frame-ancestors`
- `Permissions-Policy`, especially `camera` and `microphone`
- HTTPS certificate and redirect behavior

If JVC is deliberately changed back to iframe embedding, the chat response must allow the exact parent origin(s) observed from the actual Tauri and Capacitor shells in `frame-ancestors`. Do not use a wildcard. Remove or avoid a conflicting `X-Frame-Options: DENY` or `SAMEORIGIN`; `ALLOW-FROM` is obsolete. Keep the site's normal anti-clickjacking policy for every other route.

For the current top-level page, an explicit site `Permissions-Policy` must not disable the chat origin's own camera and microphone access. A suitable starting point is `camera=(self), microphone=(self)`. If the chat page embeds a third-party calling provider, that provider needs a matching policy allowlist and iframe `allow="camera; microphone"`; Android JVC will still deny media requests from that third-party origin until the native allowlist is separately reviewed and changed.

The local Tauri CSP is for JVC's startup shell and does not replace the remote chat site's own response policy. Tauri automatically merges `src-tauri/Info.plist` into the generated macOS bundle; CI now verifies that both camera and microphone usage strings survive bundling. See https://v2.tauri.app/distribute/macos-application-bundle/.

## Desktop WebView permissions and navigation

The desktop app uses **Tauri 2**, not Electron. There is no Electron main process, BrowserWindow, session permission handler or Electron navigation layer in the current repository, so Electron-specific hooks do not apply. The equivalent native controls are implemented in `src-tauri/src/main.rs`. The archived Electron prototype from the supplied source snapshot was audited separately; see [LEGACY-ELECTRON-AUDIT.md](LEGACY-ELECTRON-AUDIT.md).

- Only the exact HTTPS chat origin and Tauri's known local app origins remain inside the app. The development server exception is limited to `http://localhost:5173` in debug builds.
- External HTTP(S), `mailto:` and `tel:` navigations are handed to the system default app. Other schemes are blocked.
- New-window requests are opened externally and denied inside JVC.
- Camera and microphone requests are passed to the native WebView permission UI only while the top-level page is exactly `https://chat.jigar.au`.
- On Windows, Tauri's supported `Notifications` and `ClipboardRead` permission kinds are also passed to the native WebView prompt for the trusted chat page. All other permission kinds are denied.

Tauri's permission callback exposes the WebView's current top-level URL, not a separate requesting-frame origin. The desktop media gate therefore cannot distinguish a third-party iframe from the trusted top-level chat page. Do not embed third-party media frames in desktop until their permission delegation has been reviewed. The Android gate is stricter because Android's `PermissionRequest` exposes the requesting origin.

Tauri documents notification permission support on Windows and Linux, but not macOS/Android/iOS; clipboard-read permission callbacks are supported on Windows only. Clipboard writes generally depend on the web engine's user-gesture rules. These are backend capability limits, not proof that the chat website's notification or clipboard UX works on a particular OS. See the platform matrix at https://docs.rs/tauri/latest/tauri/webview/enum.PermissionKind.html.

## Android WebView security

apps/android/apply-native-overlay.sh copies the checked-in Capacitor MainActivity, WebChromeClient and WebViewClient overrides into the generated Android project.

The media permission gate:

- accepts only https://chat.jigar.au (default HTTPS port only, no user-info);
- grants only AUDIO_CAPTURE and/or VIDEO_CAPTURE;
- denies unknown or future WebView resources;
- requests Android RECORD_AUDIO and CAMERA runtime permissions only when needed;
- denies overlapping requests while a native permission prompt is pending;
- clears canceled requests and denies pending access when the activity is destroyed.

The navigation client keeps only the Capacitor app origin and the exact HTTPS chat origin inside the WebView. HTTP(S), mailto: and tel: links outside those origins are handed to Android's default app. Unsupported schemes are blocked.

Android PermissionRequest.grant() must receive only the specific resources being approved. The Android framework explicitly warns against granting the entire unfiltered resource array because WebView may add new resource types in future versions: https://developer.android.com/reference/android/webkit/PermissionRequest

### Notifications and clipboard

Android WebView is not a full browser and does not guarantee browser-only features that need browser UI, including web push notifications. JVC does not implement a Firebase Cloud Messaging (FCM) bridge, native push registration, a notification service, or a POST_NOTIFICATIONS flow. Treat web notifications and background push as unsupported in the Android app. Reliable notifications require a separately designed native FCM integration and server-side device registration. References: https://web.dev/articles/web-on-android?hl=en and https://issues.chromium.org/issues/40388442

JVC does not add a native clipboard bridge or broad clipboard permission. Basic selection and the Android WebView context-menu copy/paste path are platform behavior. Chromium's Android WebView implementation explicitly has no CLIPBOARD_READ_WRITE permission grant flow, and the Web Permissions API is not implemented in WebView; clipboard paste is instead gated on recent user interaction. Do not rely on programmatic `navigator.clipboard.readText()` in Android JVC without testing the exact WebView provider/version on a physical device. References: https://chromium.googlesource.com/chromium/src/+/50dd53168a8bbd38f80af2c3b5645c940b785e15/android_webview/browser/aw_content_browser_client.cc and https://developer.android.com/develop/ui/views/touch-and-input/copy-paste

The custom `JvcWebChromeClient` preserves Capacitor's dialogs and file chooser while replacing its permissive media callback with JVC's strict origin/resource gate. It does not add notification or clipboard support.

Desktop capability matrix from the current Tauri backend: Windows can pass `Notifications` and `ClipboardRead` requests to WebView2 for the trusted chat page. Tauri's current macOS backend does not support those permission kinds, and JVC denies them there. Android uses Capacitor rather than Tauri and has no native notification/clipboard bridge. See https://docs.rs/tauri/latest/tauri/webview/enum.PermissionKind.html.

Required real-device QA:
- deny camera and microphone, then verify calls remain blocked;
- grant microphone only and verify video capture remains blocked;
- grant camera only and verify audio capture remains blocked;
- verify a non-chat origin cannot obtain either resource;
- verify unknown WebView resources are denied;
- verify app background/foreground transitions and request cancellation;
- test Android selection, context-menu copy/paste and user-activated clipboard reads, including a graceful fallback when `readText()` is denied;
- test Windows notification and clipboard-read prompts; confirm macOS and Android are not advertised as supporting web notifications;
- test external links, target blank, mailto and tel links.

## Build matrix and artifacts

The GitHub Actions workflow builds:

| Platform | Runner | Package |
|---|---|---|
| Windows x64 | windows-2025 | NSIS `.exe` and MSI |
| macOS universal | macos-15 Apple Silicon runner | Universal `.dmg` and `.app` for Intel + Apple Silicon |
| Android | ubuntu-24.04 | Debug-signed `.apk` + unsigned release `.aab` |

The workflow commits and shares both the root `package-lock.json` and `src-tauri/Cargo.lock`. If either is absent on a push to `main`, the resolver creates it and commits it with the restricted `GITHUB_TOKEN`; every platform job downloads the same lockfile artifact. JavaScript dependencies are installed with `npm ci`, Rust lock consistency is checked with `cargo metadata --locked`, and the Rust compiler is pinned to 1.100.0. Android's SDK platform and build-tools versions are pinned in the workflow. A change to Rust dependencies must update and commit `src-tauri/Cargo.lock` rather than silently resolving a new graph in each platform job.

Pull requests and main-branch pushes build artifacts but do not publish a release. Pushing a matching version tag such as `v2.0.1` creates a **draft** GitHub Release containing the Windows installers, universal macOS DMG, debug-signed Android APK and unsigned Android AAB. The tag must match the checked-in package version. Sign the desktop packages and AAB, replace the draft assets, finish device/site/privacy checks, then publish the draft manually. The in-app update checker reads only the latest non-draft release, so it will not offer a draft candidate.

### Latest automated validation record

On 17 November 2026, GitHub Actions run [#44052296994](https://github.com/Jigar1234Nob/JVC/actions/runs/44052296994) passed for main commit `9dec6bd38c86c6af2a31af89048ac848f92dd736`. The dependency-lock resolver, Windows x64, universal macOS and Android jobs all completed successfully. The tag-only draft-release job was skipped because this was a push to `main`.

Checks completed in that run:

- `cargo metadata --locked` and the app-version alignment checks passed.
- The shared React/TypeScript/Vite production build passed on all platform jobs.
- Windows produced `JVC_2.0.1_x64-setup.exe` (2,046,345 bytes) and `JVC_2.0.1_x64_en-US.msi` (3,059,712 bytes); CI verified both installer types exist.
- macOS produced `JVC_2.0.1_universal.dmg` (5.65 MiB) and a 20.08 MiB app bundle. CI confirmed the executable contains both `x86_64` and `arm64` slices and checked that camera and microphone usage descriptions survived bundling.
- Android produced package `au.jigar.jvc`, versionName `2.0.1`, versionCode `20001`. CI verified the debug APK signature and manifest permissions for CAMERA, RECORD_AUDIO and MODIFY_AUDIO_SETTINGS. The APK uses Android's debug certificate. The unsigned release AAB was also built and passed ZIP archive integrity validation.

GitHub Actions artifacts from that run (temporary; 14-day retention, expiring 1 December 2026):

- [Windows x64 installers](https://github.com/Jigar1234Nob/JVC/actions/runs/44052296994/artifacts/14314651023) — archive 4,875,382 bytes; SHA-256 `4a23f3eef7aa4834af23692d5290375a093a760214891aa72818562a4b9f5684`
- [Universal macOS package](https://github.com/Jigar1234Nob/JVC/actions/runs/44052296994/artifacts/14315245024) — archive 11,985,154 bytes; SHA-256 `73acf7f355797b2d9211d72e3b2bde055cd47d8130b989e6c02475e390e6c8b0`
- [Android debug APK](https://github.com/Jigar1234Nob/JVC/actions/runs/44052296994/artifacts/14314840612) — archive 3,703,551 bytes; SHA-256 `71365a71e83a0845a465b658a93bb0a45aa6636025f39dbc838ca44b4aa85b8f`
- [Android unsigned release AAB](https://github.com/Jigar1234Nob/JVC/actions/runs/44052296994/artifacts/14314651122) — archive 2,826,646 bytes; SHA-256 `6674d65b5439564495a9da0537fe27a8eeef3ba76332073fc6c890ccd7d690d5`

These results prove CI compilation, packaging and the listed static checks. They do not prove interactive installation or runtime behavior. No Windows VM, Intel Mac, Apple Silicon Mac or physical Android device was available. Camera/microphone prompts, real calls, login, upload, notifications and clipboard UX still need device QA.

The public [`v2.0.0` release](https://github.com/Jigar1234Nob/JVC/releases/tag/v2.0.0) predates the current 2.0.1 source and is not a 2.0.1 build. The 2.0.1 artifacts above are not attached to that release. No `v2.0.1` tag or public release has been created. The startup checker reads only the latest non-draft release, so the 2.0.0 release is not offered as an update to an installed 2.0.1 build. Pushing a matching `v2.0.1` tag will create a draft candidate; it must be signed, checked and manually published.
## Signing and packaging

Never commit certificates, keystores, passwords, provisioning profiles or API keys.

### Windows

The current NSIS and MSI packages are unsigned. For public distribution, obtain an Authenticode code-signing certificate, import it securely on the Windows runner, sign the final .exe and .msi with SignTool, and timestamp the signatures. Verify with:

~~~powershell
signtool verify /pa /v JVC-Setup.exe
~~~

Without a trusted certificate, Windows SmartScreen may show an unknown-publisher warning.

### macOS

The current DMG and app bundle are unsigned and not notarized. For distribution outside the Mac App Store, obtain an Apple Developer ID Application certificate, sign the .app with hardened runtime and required entitlements, submit it to Apple's notary service, staple the ticket, then package the signed app into the DMG. Verify with:

~~~sh
codesign --verify --deep --strict --verbose=2 JVC.app
spctl --assess --type execute --verbose JVC.app
xcrun stapler validate JVC.app
~~~

A universal binary must contain both arm64 and x86_64 slices. App Store distribution additionally requires the appropriate App Store provisioning and installer package process.

### Android

CI builds a debug-signed APK for sideload QA and an unsigned release AAB for the Play release path. For a production APK/AAB, create a private upload keystore, keep it outside the repository, and configure the Android Gradle release signing block using encrypted GitHub Actions secrets. Keep the same upload key for subsequent Play Store updates and back it up securely. Never publish a debug-key APK as a production update.

Before Play distribution, complete Play Console app-content declarations, Data safety answers, target API requirements, privacy policy URL, and notification/camera/microphone disclosures. Use the actual behavior of chat.jigar.au when answering data collection and sharing questions.

## Privacy disclosure checklist

Publish a public privacy policy before broad distribution. It should identify:

- JVC's publisher/contact details and support channel;
- that JVC loads the remote chat service and the service's own privacy policy governs account, message, voice and uploaded content;
- that the startup checker contacts GitHub's Releases API and GitHub may receive standard request metadata;
- camera and microphone are used only for user-initiated chat/call features and are gated by OS permissions;
- clipboard access is controlled by WebView/browser behavior and user interaction;
- whether notifications are supported, what service sends them, and how users opt in/out;
- retention, analytics, crash reporting and third-party processors, if any;
- account/data deletion and privacy contact procedures.

Do not claim JVC itself stores or transmits chat data independently unless that is verified from the chat site's implementation and privacy policy.

## Release checklist

- [ ] Verify live chat.jigar.au response headers and redirect chain.
- [ ] Verify login, voice/video calls, file upload and logout on desktop and Android.
- [ ] Run the Android origin/resource permission matrix above on a physical device.
- [ ] Test Windows WebView2 notification/clipboard-read prompts; confirm macOS and Android web notifications are treated as unsupported; test Android context-menu copy/paste and the programmatic-read fallback.
- [ ] Test the Windows installer on a clean Windows VM.
- [ ] Test the universal macOS DMG on Intel and Apple Silicon Macs.
- [ ] Sign and notarize Windows/macOS packages; sign Android release APK/AAB.
- [ ] Publish the privacy policy and complete store disclosures.
- [ ] Confirm the tag exactly matches the versions in `package.json`, `apps/web/package.json`, `src-tauri/Cargo.toml`, and `src-tauri/tauri.conf.json`.
- [ ] Create and push the version tag only after all checks pass; CI creates a draft release.
- [ ] Replace draft assets with signed production packages, then publish the draft.
- [ ] Verify the published release assets and confirm the startup checker selects the correct platform package.
