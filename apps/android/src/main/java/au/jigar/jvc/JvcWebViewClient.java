package au.jigar.jvc;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeWebViewClient;

/** Keeps only the Capacitor app origin and the HTTPS chat origin in the app window. */
public final class JvcWebViewClient extends BridgeWebViewClient {
    private final Bridge bridge;

    public JvcWebViewClient(Bridge bridge) {
        super(bridge);
        this.bridge = bridge;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (!request.isForMainFrame()) {
            return false;
        }
        return route(view, request.getUrl());
    }

    @Deprecated
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return route(view, Uri.parse(url));
    }

    private boolean route(WebView view, Uri uri) {
        if (isAppOrigin(uri) || isChatOrigin(uri)) {
            return false;
        }

        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)
            || "http".equalsIgnoreCase(scheme)
            || "mailto".equalsIgnoreCase(scheme)
            || "tel".equalsIgnoreCase(scheme)) {
            try {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException ignored) {
                // No installed handler. Keep the unsupported URL out of the WebView.
            }
        }
        return true;
    }

    private boolean isAppOrigin(Uri uri) {
        return uri != null
            && bridge.getScheme().equalsIgnoreCase(uri.getScheme())
            && bridge.getHost().equalsIgnoreCase(uri.getHost())
            && hasDefaultPort(uri)
            && uri.getUserInfo() == null;
    }

    private boolean isChatOrigin(Uri uri) {
        return uri != null
            && "https".equalsIgnoreCase(uri.getScheme())
            && "chat.jigar.au".equalsIgnoreCase(uri.getHost())
            && hasDefaultPort(uri)
            && uri.getUserInfo() == null;
    }

    private boolean hasDefaultPort(Uri uri) {
        int port = uri.getPort();
        if (port == -1) {
            return true;
        }
        return ("https".equalsIgnoreCase(uri.getScheme()) && port == 443)
            || ("http".equalsIgnoreCase(uri.getScheme()) && port == 80);
    }
}
