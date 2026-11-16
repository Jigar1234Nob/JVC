# Android build

From repository root:

```bash
npm ci
npm run build
npx cap add android
npx cap sync android
npx cap open android
```

Build a debug APK using Android Studio. For public release, configure app signing and produce a signed APK/AAB.

**Before publishing:** implement and test Android WebView `onPermissionRequest` delegation with an explicit origin allowlist for `https://chat.jigar.au` and only the required camera/microphone resources. Do not auto-grant arbitrary WebView permission requests. Confirm chat.jigar.au permits embedding in a WebView.
