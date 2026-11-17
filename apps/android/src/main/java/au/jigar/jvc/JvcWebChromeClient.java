package au.jigar.jvc;

import android.webkit.PermissionRequest;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeWebChromeClient;

/** Keeps Capacitor's dialogs and file picker while delegating media access to JVC's gate. */
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
}
