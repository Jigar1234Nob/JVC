# JVC

**JVC (Jigar Virtual Client)** is a Discord-inspired client shell for `https://chat.jigar.au`, with shared React UI and a permission centre. It uses the supplied J. brand mark (vector recreation in `apps/web/public/jvc-logo.svg`).

## Platforms
- **Windows:** Electron installer (NSIS) and portable package.
- **macOS:** Electron DMG and ZIP.
- **Android:** Capacitor Android project, debug APK in CI; signed release APK/AAB requires signing configuration.
- **Web:** Vite preview for development.

## Development
Requires Node.js 22+ and npm.

```bash
npm install
npm run dev
```

## Desktop packaging
```bash
npm install
npm run build
npm run package --workspace @jvc/desktop
```
Run the Windows build on Windows and macOS build on macOS. GitHub Actions does this automatically on pushes and pull requests. Artifacts are attached to the workflow run.

## Android packaging
```bash
npm install
npm run build
npx cap add android
npx cap sync android
npx cap open android
```
Build a debug APK in Android Studio. CI also creates a debug APK. A signed release needs a private keystore and signing configuration; never commit signing credentials.

## Security and embedding
Electron enables context isolation, sandboxing, and disables Node integration. External navigation is sent to the system browser. Desktop permission prompts are allowlisted to the chat origin and selected permission types.

The chat website must permit embedding. If it sends `X-Frame-Options: DENY` or a restrictive CSP `frame-ancestors`, the embedded view will be blocked. Configure the chat service to permit the intended JVC app origin; JVC does not bypass those browser protections.

Android's WebView permission delegation must be implemented and tested with a strict origin/resource allowlist before production release. The CI Android APK is a development build, not a signed store release.

## Release checklist
- [ ] Verify chat.jigar.au embedding headers and login flow.
- [ ] Implement/test Android WebView camera and microphone permission delegation.
- [ ] Test file upload, notifications, clipboard, deep links and screen sharing on real devices.
- [ ] Configure Android signing and store privacy disclosures.
- [ ] Configure Windows/macOS signing and update distribution.
- [ ] Test packaged apps on clean OS installations.
