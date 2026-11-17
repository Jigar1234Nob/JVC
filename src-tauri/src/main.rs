#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use tauri::{
    utils::config::WebviewUrl,
    webview::{NewWindowResponse, WebviewWindowBuilder},
    Manager,
};

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_camera_kessdev::init())
        .setup(|app| {
            let app_handle = app.handle().clone();
            let main_url = WebviewUrl::External("https://chat.jigar.au".parse()?);

            WebviewWindowBuilder::new(app, "main", main_url)
                .title("JVC")
                .inner_size(1280.0, 820.0)
                .min_inner_size(760.0, 560.0)
                .center()
                .resizable(true)
                .on_new_window(move |url, features| {
                    // Only allow secure web popups. Never create javascript:, data:,
                    // file:, or other unexpected windows from remote page content.
                    if url.scheme() != "https" {
                        return NewWindowResponse::Deny;
                    }

                    let label = format!(
                        "popup-{}",
                        std::time::SystemTime::now()
                            .duration_since(std::time::UNIX_EPOCH)
                            .unwrap_or_default()
                            .as_millis()
                    );

                    match WebviewWindowBuilder::new(
                        &app_handle,
                        label,
                        WebviewUrl::External(url.clone()),
                    )
                    .title("JVC")
                    .inner_size(900.0, 700.0)
                    .min_inner_size(480.0, 360.0)
                    .center()
                    .resizable(true)
                    .window_features(features)
                    .on_document_title_changed(|window, _title| {
                        // Keep JVC branding in the native title bar instead of
                        // exposing arbitrary webpage titles as browser chrome.
                        let _ = window.set_title("JVC");
                    })
                    .build()
                    {
                        Ok(window) => NewWindowResponse::Create { window },
                        Err(_) => NewWindowResponse::Deny,
                    }
                })
                .build()?;

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("failed to start JVC");
}
