#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use tauri::{
    utils::config::WebviewUrl,
    webview::{NewWindowResponse, PermissionKind, PermissionResponse, WebviewWindowBuilder},
};
use tauri_plugin_opener::OpenerExt;

fn open_in_default_browser(app: &tauri::AppHandle, url: &tauri::Url) {
    match url.scheme() {
        "https" | "http" | "mailto" | "tel" => {
            // Use the operating system's registered default application.
            let _ = app.opener().open_url(url.as_str(), None::<&str>);
        }
        _ => {
            // Never hand javascript:, file:, data:, or unknown schemes to the OS.
        }
    }
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_camera_kessdev::init())
        .plugin(tauri_plugin_opener::init())
        .setup(|app| {
            let app_handle = app.handle().clone();
            let main_url = WebviewUrl::External("https://chat.jigar.au".parse()?);

            WebviewWindowBuilder::new(app, "main", main_url)
                .title("JVC")
                .inner_size(1280.0, 820.0)
                .min_inner_size(760.0, 560.0)
                .center()
                .resizable(true)
                .on_permission_request(|_, permission| match permission {
                    PermissionKind::Camera | PermissionKind::Microphone => {
                        PermissionResponse::Default
                    }
                    _ => PermissionResponse::Deny,
                })
                .on_navigation({
                    let app_handle = app_handle.clone();
                    move |url| {
                        // Keep JVC itself inside the app. Every other web link
                        // goes straight to the user's default browser.
                        if url.scheme() == "https"
                            && url.host_str() == Some("chat.jigar.au")
                        {
                            return true;
                        }

                        open_in_default_browser(&app_handle, url);
                        false
                    }
                })
                .on_new_window(move |url, _features| {
                    // Links using target=_blank and window.open follow the same
                    // default-browser rule instead of creating JVC popup windows.
                    open_in_default_browser(&app_handle, &url);
                    NewWindowResponse::Deny
                })
                .build()?;

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("failed to start JVC");
}
