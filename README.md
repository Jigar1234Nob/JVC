# JVC

**JVC (Jigar Virtual Client)** is a lightweight Discord-inspired client shell for [chat.jigar.au](https://chat.jigar.au), with a shared React interface and a permission centre.

## Desktop: Rust + Tauri 2

Windows and macOS use **Tauri 2**, with a small Rust application and the operating system's native WebView (WebView2 on Windows, WKWebView on macOS). Unlike Electron, JVC does not bundle a separate Chromium runtime.

Expected benefits:
- Smaller installers and less bundled runtime overhead.
- Lower idle memory use.
- Faster cold startup.
- Native OS web rendering and window integration.

Actual performance depends on the host OS and the chat website. The chat itself still loads over the network.

## Platforms

- **Windows:** Tauri NSIS and MSI installers.
- **macOS:** Tauri application bundle and DMG.
- **Android:** Capacitor debug APK in CI. A signed store release needs signing configuration.
- **Web:** Vite development preview.

## Development

Requires Node.js 22+, Rust stable, and the platform's Tauri prerequisites.

```bash
npm install
npm run dev
```

## Desktop development and builds

```bash
npm install
npm run desktop:dev
npm run desktop:build
```

Tauri builds must run on the target operating system. GitHub Actions builds Windows and macOS artifacts separately and attaches them to the workflow run.

Platform prerequisites: https://v2.tauri.app/start/prerequisites/

## Android

```bash
npm install
npm run build
npx cap add android
npx cap sync android
```

Open the generated Android project in Android Studio. CI also creates a debug APK. A signed release requires a private keystore and signing configuration; never commit signing credentials.

## Security and embedding

The chat website must permit embedding. If it sends `X-Frame-Options: DENY` or a restrictive CSP `frame-ancestors`, the embedded view will be blocked. Configure the chat service to permit the intended JVC app origin; JVC does not bypass browser protections.

Android WebView camera and microphone permission delegation must be implemented and tested with a strict origin/resource allowlist before production release. The CI Android APK is a development build, not a signed store release.

## Release checklist

- [ ] Verify chat.jigar.au embedding headers and login flow.
- [ ] Test Tauri camera, microphone, notifications and clipboard permissions on Windows and macOS.
- [ ] Implement/test Android WebView camera and microphone permission delegation.
- [ ] Test file upload, notifications, clipboard, deep links and screen sharing on real devices.
- [ ] Configure Android signing and store privacy disclosures.
- [ ] Configure Windows/macOS signing and update distribution.
- [ ] Test packaged apps on clean OS installations.
