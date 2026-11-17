package au.jigar.jvc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.webkit.PermissionRequest;
import androidx.core.app.ActivityCompat;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import java.util.ArrayList;
import java.util.List;

/** Native entry point with origin- and resource-scoped camera/microphone delegation. */
public class MainActivity extends BridgeActivity {
    private static final int MEDIA_PERMISSION_REQUEST = 7421;
    private static final String CHAT_HOST = "chat.jigar.au";

    private PermissionRequest pendingRequest;
    private String[] pendingWebResources;
    private String[] pendingAndroidPermissions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bridge bridge = getBridge();
        if (bridge != null && bridge.getWebView() != null) {
            bridge.getWebView().getSettings().setSupportMultipleWindows(true);
            bridge.getWebView().getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
            bridge.getWebView().setWebChromeClient(
                new JvcWebChromeClient(bridge, new JvcWebChromeClient.PermissionRequestDelegate() {
                    @Override
                    public void onPermissionRequest(PermissionRequest request) {
                        handleMediaPermissionRequest(request);
                    }

                    @Override
                    public void onPermissionRequestCanceled(PermissionRequest request) {
                        cancelPendingRequest(request);
                    }
                })
            );
            bridge.getWebView().setWebViewClient(new JvcWebViewClient(bridge));
        }
    }

    private void handleMediaPermissionRequest(PermissionRequest request) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> handleMediaPermissionRequest(request));
            return;
        }

        if (!isTrustedChatOrigin(request.getOrigin())) {
            request.deny();
            return;
        }

        String[] requested = request.getResources();
        if (requested == null || requested.length == 0) {
            request.deny();
            return;
        }

        List<String> grantResources = new ArrayList<>();
        List<String> requiredPermissions = new ArrayList<>();

        for (String resource : requested) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                grantResources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
                addRuntimePermissionIfNeeded(requiredPermissions, Manifest.permission.RECORD_AUDIO);
            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                grantResources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                addRuntimePermissionIfNeeded(requiredPermissions, Manifest.permission.CAMERA);
            } else {
                // Deny unknown/future WebView resources instead of granting the whole request.
                request.deny();
                return;
            }
        }

        if (grantResources.isEmpty() || pendingRequest != null) {
            request.deny();
            return;
        }

        if (requiredPermissions.isEmpty()) {
            request.grant(grantResources.toArray(new String[0]));
            return;
        }

        pendingRequest = request;
        pendingWebResources = grantResources.toArray(new String[0]);
        pendingAndroidPermissions = requiredPermissions.toArray(new String[0]);
        ActivityCompat.requestPermissions(
            this,
            pendingAndroidPermissions,
            MEDIA_PERMISSION_REQUEST
        );
    }

    private void addRuntimePermissionIfNeeded(List<String> permissions, String permission) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            && !permissions.contains(permission)) {
            permissions.add(permission);
        }
    }

    private boolean isTrustedChatOrigin(Uri origin) {
        if (origin == null || origin.getUserInfo() != null) {
            return false;
        }
        return "https".equalsIgnoreCase(origin.getScheme())
            && CHAT_HOST.equalsIgnoreCase(origin.getHost())
            && (origin.getPort() == -1 || origin.getPort() == 443);
    }

    private void cancelPendingRequest(PermissionRequest request) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> cancelPendingRequest(request));
            return;
        }
        if (request == pendingRequest) {
            clearPendingRequest();
        }
    }

    private void clearPendingRequest() {
        pendingRequest = null;
        pendingWebResources = null;
        pendingAndroidPermissions = null;
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        if (requestCode == MEDIA_PERMISSION_REQUEST) {
            PermissionRequest request = pendingRequest;
            String[] resources = pendingWebResources;
            String[] requestedPermissions = pendingAndroidPermissions;
            clearPendingRequest();

            boolean allGranted = request != null
                && requestedPermissions != null
                && grantResults.length == requestedPermissions.length;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                }
            }

            if (request != null) {
                if (allGranted) {
                    request.grant(resources);
                } else {
                    request.deny();
                }
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroy() {
        if (pendingRequest != null) {
            pendingRequest.deny();
            clearPendingRequest();
        }
        super.onDestroy();
    }
}
