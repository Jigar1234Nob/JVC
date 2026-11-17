package au.jigar.jvc;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.webkit.PermissionRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeWebChromeClient;

/** Keeps Capacitor dialogs and file picking while applying JVC media and popup policies. */
public final class JvcWebChromeClient extends BridgeWebChromeClient {
    public interface PermissionRequestDelegate {
        void onPermissionRequest(PermissionRequest request);
        void onPermissionRequestCanceled(PermissionRequest request);
    }

    private final PermissionRequestDelegate delegate;

    public JvcWebChromeClient(Bridge bridge, PermissionRequestDelegate delegate) {
        super(bridge);
        this.delegate = delegate;
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        delegate.onPermissionRequest(request);
    }

    @Override
    public void onPermissionRequestCanceled(PermissionRequest request) {
        delegate.onPermissionRequestCanceled(request);
    }

    /**
     * Route user-opened target=_blank/window.open destinations to Android's default app.
     * The temporary WebView exists only to receive the destination URL; it is never shown.
     */
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        if (!isUserGesture || resultMsg == null
            || !(resultMsg.obj instanceof WebView.WebViewTransport)) {
            return false;
        }

        WebView popup = new WebView(view.getContext());
        popup.getSettings().setJavaScriptEnabled(view.getSettings().getJavaScriptEnabled());
        popup.setWebViewClient(new WebViewClient() {
            private boolean routed;

            @Override
            public boolean shouldOverrideUrlLoading(WebView child, WebResourceRequest request) {
                return routePopupDestination(child, request.getUrl());
            }

            @Deprecated
            @Override
            public boolean shouldOverrideUrlLoading(WebView child, String url) {
                return routePopupDestination(child, Uri.parse(url));
            }

            private boolean routePopupDestination(WebView child, Uri uri) {
                if (uri == null || "about".equalsIgnoreCase(uri.getScheme())) {
                    return false;
                }
                if (routed) {
                    return true;
                }
                routed = true;

                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme)
                    || "http".equalsIgnoreCase(scheme)
                    || "mailto".equalsIgnoreCase(scheme)
                    || "tel".equalsIgnoreCase(scheme)) {
                    try {
                        child.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException ignored) {
                        // No installed handler. Keep the destination out of the app WebView.
                    }
                }

                child.destroy();
                return true;
            }
        });

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(popup);
        resultMsg.sendToTarget();
        return true;
    }
}
