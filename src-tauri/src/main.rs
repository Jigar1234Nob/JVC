#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use tauri::{
    utils::config::WebviewUrl,
    webview::{NewWindowResponse, PermissionKind, PermissionResponse, WebviewWindowBuilder},
};
use tauri_plugin_opener::OpenerExt;

fn is_chat_origin(url: &tauri::Url) -> bool {
    url.scheme() == "https"
        && url.host_str() == Some("chat.jigar.au")
        && url.port_or_known_default() == Some(443)
        && url.username().is_empty()
        && url.password().is_none()
}

fn is_internal_app_origin(url: &tauri::Url) -> bool {
    if !url.username().is_empty() || url.password().is_some() {
        return false;
    }

    match (url.scheme(), url.host_str(), url.port()) {
        ("tauri", Some("localhost"), None) => true,
        ("http", Some("tauri.localhost"), None | Some(80)) => true,
        ("https", Some("tauri.localhost"), None | Some(443)) => true,
        #[cfg(debug_assertions)]
        ("http", Some("localhost"), Some(5173)) => true,
        _ => false,
    }
}

fn open_in_default_browser(app: &tauri::AppHandle, url: &tauri::Url) {
    match url.scheme() {
        "https" | "http" | "mailto" | "tel" => {
            let _ = app.opener().open_url(url.as_str(), None::<&str>);
        }
        _ => {}
    }
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .setup(|app| {
            let app_handle = app.handle().clone();
            WebviewWindowBuilder::new(app, "main", WebviewUrl::App("index.html".into()))
                .title("JVC")
                .inner_size(1280.0, 820.0)
                .min_inner_size(760.0, 560.0)
                .center()
                .resizable(true)
                .on_permission_request(|webview, permission| {
                    let trusted_chat = webview
                        .url()
                        .map(|url| is_chat_origin(&url))
                        .unwrap_or(false);

                    if !trusted_chat {
                        return PermissionResponse::Deny;
                    }

                    match permission {
                        PermissionKind::Camera | PermissionKind::Microphone => {
                            PermissionResponse::Default
                        }
                        #[cfg(target_os = "windows")]
                        PermissionKind::Notifications | PermissionKind::ClipboardRead => {
                            PermissionResponse::Default
                        }
                        _ => PermissionResponse::Deny,
                    }
                })
                .on_navigation({
                    let app_handle = app_handle.clone();
                    move |url| {
                        if is_internal_app_origin(url) || is_chat_origin(url) {
                            return true;
                        }
                        open_in_default_browser(&app_handle, url);
                        false
                    }
                })
                .on_new_window(move |url, _features| {
                    open_in_default_browser(&app_handle, &url);
                    NewWindowResponse::Deny
                })
                .build()?;
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("failed to start JVC");
}
