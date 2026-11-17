# JVC release and deployment guide

## Current delivery model

JVC is a shared React shell around the existing chat service at https://chat.jigar.au.

- Windows and macOS desktop clients use Tauri 2 and the operating system WebView. JVC is not an Electron app, so Electron permission hooks do not apply; the equivalent controls live in the Tauri Rust window builder.
- Android uses Capacitor 7 and Android System WebView.
- The startup screen checks the latest non-draft GitHub Release and offers the platform package. The check must fail open so an unavailable GitHub API never prevents chat access.
- Release assets are distributed from https://github.com/Jigar1234Nob/JVC/releases.

The current CI packages are unsigned desktop builds and a debug-signed Android APK. They are useful for QA and sideload testing, not yet store-ready production packages.

## Embedding and site headers

JVC currently navigates the main WebView directly to https://chat.jigar.au. It does not use an iframe. Top-level WebView navigation is not blocked by X-Frame-Options or CSP frame-ancestors; those headers control framed documents.

The live response headers could not be inspected from the build environment during this release preparation. Before shipping, run:

~~~sh
curl -sSIL https://chat.jigar.au/
~~~

Inspect every redirect and the final response for:

- X-Frame-Options
- Content-Security-Policy, especially frame-ancestors
- Permissions-Policy, especially camera and microphone
- HTTPS certificate and redirect behavior

If the site is later placed inside an iframe, configure the site's CSP to allow only the actual parent origin(s), for example:

~~~http
Content-Security-Policy: frame-ancestors 'self' https://your-approved-parent.example
~~~

Do not send X-Frame-Options: DENY or SAMEORIGIN for a cross-origin iframe. ALLOW-FROM is obsolete; use CSP frame-ancestors. Do not weaken these headers globally. The native JVC clients should continue to use direct top-level navigation unless a verified site-side iframe requirement exists.

For media capture, the site should serve HTTPS and its Permissions-Policy must not disable camera or microphone for the top-level chat origin. If the chat page embeds a third-party call frame, that frame needs its own explicit allow="camera; microphone" delegation and a compatible policy. JVC's Android native gate intentionally denies media requests from any origin other than exactly https://chat.jigar.au.

## Desktop WebView permissions and navigation

The Tauri window permits only the exact HTTPS chat origin and its own internal app origin. Other top-level HTTP(S), mailto and tel navigations are opened with the operating system's default app; unsupported schemes are blocked. New-window requests are sent to the default app and denied inside JVC.

Camera and microphone requests are passed to the native WebView permission UI only while the top-level page is exactly https://chat.jigar.au. Every other permission kind is denied. The community camera plugin previously included in this project installed its own Windows/Linux permission handler that allowed camera and microphone before the application handler could apply its origin check. That plugin was removed so the JVC gate remains authoritative.

Tauri's permission callback exposes the WebView's current top-level URL, not a separate requesting-frame origin. The Android implementation below has a stricter frame-origin check because Android's PermissionRequest API provides the requesting origin directly. Do not embed third-party media frames in desktop until their permissions and frame policy have been reviewed.

Tauri's documented platform support is not uniform: web notification permission requests are supported on Windows and Linux but not macOS/Android/iOS; clipboard-read permission callbacks are supported on Windows but not macOS/Linux/Android/iOS. These are callback/backend limits, not proof that every website clipboard or notification feature works. Verify actual chat-site behavior on each OS before advertising it.

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

The wrapper does not currently provide a native push-notification bridge. Do not promise background push delivery from the Android WebView. If the chat service requires reliable background notifications, integrate Firebase Cloud Messaging or a Capacitor native notifications plugin with server-side registration and a user-visible opt-in flow.

Clipboard behavior is supplied by Android WebView and the chat website. Clipboard writes should be initiated by a user action. Clipboard reads may require a secure context, browser permission and user activation; they must be tested on supported Android System WebView versions. No broad clipboard permission is granted by the native wrapper. See https://developer.mozilla.org/en-US/docs/Web/API/Clipboard/readText

Required real-device QA:
- deny camera and microphone, then verify calls remain blocked;
- grant microphone only and verify video capture remains blocked;
- grant camera only and verify audio capture remains blocked;
- verify a non-chat origin cannot obtain either resource;
- verify unknown WebView resources are denied;
- verify app background/foreground transitions and request cancellation;
- test copy, paste and clipboard read from a user gesture;
- test notification permission prompts and background behavior, and record unsupported cases;
- test external links, target blank, mailto and tel links.

## Build matrix and artifacts

The GitHub Actions workflow builds:

| Platform | Runner | Package |
|---|---|---|
| Windows x64 | windows-2025 | NSIS .exe and MSI |
| macOS universal | macos-15 Apple Silicon runner | Universal .dmg and .app for Intel + Apple Silicon |
| Android | ubuntu-24.04 | Debug .apk |

The workflow resolves one npm lock snapshot per run and shares it with all platform jobs. For fully repeatable builds across separate runs, commit the generated package-lock.json and use it as the source-of-truth lockfile before cutting a production release.

Pull requests and main-branch pushes build artifacts but do not publish a release. Publishing is tag-driven: push a version tag such as v2.1.0 after the release checklist is complete. The workflow then attaches the platform packages to that GitHub Release. The in-app update checker reads the latest non-draft release, so only publish tags intended for end users.

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

The current APK is signed with the CI debug key. For a production APK/AAB, create a private upload keystore, keep it outside the repository, and configure the Android Gradle release signing block using encrypted GitHub Actions secrets. Keep the same upload key for subsequent Play Store updates and back it up securely. Never publish a debug-key APK as a production update.

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
- [ ] Test notifications and clipboard on supported OS/WebView versions.
- [ ] Test the Windows installer on a clean Windows VM.
- [ ] Test the universal macOS DMG on Intel and Apple Silicon Macs.
- [ ] Sign and notarize Windows/macOS packages; sign Android release APK/AAB.
- [ ] Publish the privacy policy and complete store disclosures.
- [ ] Create and push the version tag only after all checks pass.
- [ ] Verify all assets appear on the GitHub Release and the startup checker selects the right package.
