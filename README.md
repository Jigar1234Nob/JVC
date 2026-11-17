# JVC

**JVC (Jigar Virtual Client)** is a lightweight client shell for [chat.jigar.au](https://chat.jigar.au), using the supplied J. logo and a shared React startup/update screen.

## Architecture

- **Windows and macOS:** Tauri 2 with the operating system's native WebView. JVC is not an Electron application.
- **Android:** Capacitor 7 with Android System WebView and checked-in native permission/navigation overrides.
- **Chat:** loaded as a top-level HTTPS page at https://chat.jigar.au. The app does not rely on an iframe.

The desktop shell keeps only the local app origin and the exact HTTPS chat origin in-app. External HTTP(S), mailto and tel links are handed to the system default app. Android uses the same allowlist and applies an explicit origin/resource gate to camera and microphone requests.

## Development

Requires Node.js 22+, Rust stable, and the native prerequisites for the target OS.

~~~bash
npm install
npm run dev
~~~

## Desktop builds

~~~bash
npm install
npm run desktop:dev
npm run desktop:build
~~~

Tauri builds must run on the target operating system. GitHub Actions builds Windows x64 and a universal macOS package for Intel and Apple Silicon.

## Android

~~~bash
npm install
npm run build
npx cap add android
npx cap sync android
bash apps/android/apply-native-overlay.sh
npx cap open android
~~~

CI creates a debug-signed APK for sideload QA and an unsigned release AAB. A Play Store release requires a private upload keystore and a signed AAB.

## CI and releases

Pull requests and pushes to main build platform artifacts using shared npm and Cargo lockfiles. A matching version tag creates a draft GitHub Release after all three platform jobs pass. Sign the packages, replace the draft assets, complete the release checklist, and publish the draft manually. The startup checker reads only the latest non-draft GitHub Release and offers the matching installer.

The current packages are unsigned desktop builds and a debug-signed Android APK. Do not treat them as production-signed installers.

## Security, site configuration and privacy

The live chat service must be checked for HTTPS redirects, Permissions-Policy and framing headers before release. JVC currently uses direct top-level navigation, so X-Frame-Options and CSP frame-ancestors do not block the current navigation flow. If the site is changed to iframe embedding, it must explicitly allow the actual parent origin in CSP.

Android media requests are restricted to https://chat.jigar.au and the specific microphone/camera WebView resources. Android notifications and clipboard behavior still require physical-device verification; reliable background push requires native integration.

See [docs/RELEASE.md](docs/RELEASE.md) for build, signing, privacy, update-distribution and release checklists.
