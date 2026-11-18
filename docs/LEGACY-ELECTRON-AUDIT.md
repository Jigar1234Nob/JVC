# Legacy Electron prototype security audit

**Review date:** 18 November 2026  
**Status:** Reviewed, archived, not used by the current release pipeline.

## Scope and architecture decision

The supplied JVC source archive contained an earlier Electron prototype under `apps/desktop/main.cjs` and `apps/desktop/preload.cjs`. The current `main` branch is a different implementation: Tauri 2 for Windows/macOS and Capacitor 7 for Android. The current desktop entry point is `src-tauri/src/main.rs`; the archived Electron main/preload files are not in the current repository tree and are not packaged by the current workflow.

This audit records why the old Electron implementation must not be copied back into the shipping application without changes. The current project does not need Electron-specific hooks; its active navigation and permission policies are described in [RELEASE.md](RELEASE.md).

## Controls that were already good

The archived Electron window set `contextIsolation: true`, `nodeIntegration: false`, `sandbox: true`, and disabled the `webview` tag. Its preload exposed only a small frozen metadata object. Those are useful baseline controls and should be retained if an Electron build is ever revived.

## Findings in the archived prototype

### 1. Permission origin checks used string prefixes

The permission request handler trusted `requestingUrl.startsWith(origin)`, and the permission-check handler used `requestingOrigin.startsWith(...)`. Prefix checks are not origin checks. A value such as `https://chat.jigar.au.attacker.example` begins with `https://chat.jigar.au` but is a different origin.

**Required correction:** parse the URL and compare its normalized `origin` to the exact `https://chat.jigar.au` origin. Require HTTPS and the default port. Reject parse failures, credentials, unexpected schemes, and unexpected ports. Use the requesting frame's URL/origin from Electron's permission details, not only the top-level `webContents.getURL()`.

### 2. Every `file://` URL was trusted for permissions and navigation

The prototype included `file://` in the permission allowlist and allowed any `file://` destination through `will-navigate`. That is broader than the single packaged startup document and could expose local-file content to permissions or renderer state.

**Required correction:** never treat the `file:` scheme as a general trusted origin. For startup navigation, allow only the exact resolved packaged `index.html` file URL. The local shell must not receive camera, microphone, notification, or clipboard grants.

### 3. Media permission grants were not resource-scoped

The prototype allowed the generic `media` permission without checking `details.mediaTypes`. That does not distinguish audio from video and does not establish that the request came from the trusted requesting frame. It also allowed notifications, clipboard read/write, and fullscreen through the same broad origin test.

**Required correction:** handle `media` only when `details.mediaTypes` is a non-empty subset of the explicitly approved `audio` and `video` resources. Deny unknown/empty media types. Treat `display-capture` separately and deny it unless screen sharing is intentionally implemented. Keep notifications, clipboard, fullscreen, geolocation, USB, HID, and every unneeded permission denied by default.

### 4. Permission checks were not frame-aware

The old permission-check handler did not use Electron's `requestingOrigin` as a parsed exact origin, and it ignored the additional frame/security metadata. Electron documents that a permission check can originate from a subframe and that `webContents` can be null for checks such as service-worker notifications. The request and check handlers both need explicit, consistent policy.

**Required correction:** implement both `setPermissionRequestHandler` and `setPermissionCheckHandler`; compare the exact requesting origin in each. For media requests, inspect `securityOrigin`, `requestingUrl`, `mediaTypes`, and frame context where supplied. For non-document checks, rely on the exact supplied requesting origin and still default-deny unsupported permissions.

### 5. Navigation and external protocol handling were too broad

The old `will-navigate` handler exempted every `file://` URL and sent every other non-trusted URL to `shell.openExternal()` without an explicit scheme allowlist. Its `setWindowOpenHandler` allowed chat-origin popups to create new in-app windows. The localhost development origin was accepted by the shared trust helper even in packaged builds; the `DEV` flag only changed the initial URL.

**Required correction:** allow only the exact chat origin, the exact packaged entry document, and the localhost dev origin only in development. Open only user-initiated `https:`, `http:`, `mailto:`, and `tel:` links externally. Deny custom schemes and all other protocols. Deny unneeded in-app popups; do not let a remote page create an unconfigured BrowserWindow.

### 6. The old iframe delegated more browser capabilities than necessary

The archived React shell used an iframe with a broad `allow` list including camera, microphone, clipboard, display capture, fullscreen, autoplay, and notifications. The iframe route was also vulnerable to the chat site's `X-Frame-Options` or CSP `frame-ancestors` policy. The current app avoids that framing dependency by navigating the native WebView directly to the HTTPS chat page.

**Required correction if iframe embedding is ever reintroduced:** use a narrow Permissions Policy and a precise `allow` attribute, and configure the chat site's CSP `frame-ancestors` for the exact parent origins. Never use a wildcard to work around a frame refusal.

## Release decision

Do not ship the archived Electron prototype. Continue using the current Tauri 2 desktop and Capacitor 7 Android implementation. If Electron is deliberately reintroduced later, implement and test every correction above before producing release artifacts.

Official references:

- [Electron session permission handlers](https://www.electronjs.org/docs/latest/api/session)
- [Electron security recommendations](https://www.electronjs.org/docs/latest/tutorial/security)
- [Android WebView permission request guidance](https://developer.android.com/reference/android/webkit/PermissionRequest)
