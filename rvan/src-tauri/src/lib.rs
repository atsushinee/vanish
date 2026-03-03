use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use std::sync::Mutex;
use tauri::{
    image::Image,
    menu::{Menu, MenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    AppHandle, Manager,
};

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppConfig {
    pub window_x: f64,
    pub window_y: f64,
    pub target_stock: String,
    pub index_code: String,
    pub watchlist: Vec<String>,
    pub gemini_api_key: String,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            window_x: 18.0,
            window_y: 811.0,
            target_stock: "sh600593".to_string(),
            index_code: "sh000001".to_string(),
            watchlist: vec![
                "sh000001".to_string(),
                "sz002413".to_string(),
                "sz002639".to_string(),
                "sh603273".to_string(),
            ],
            gemini_api_key: String::new(),
        }
    }
}

impl AppConfig {
    fn get_config_path() -> PathBuf {
        let mut path = std::env::current_dir().unwrap_or_default();
        path.push("config.properties");
        path
    }

    pub fn load() -> Self {
        let path = Self::get_config_path();
        if !path.exists() {
            return Self::default();
        }

        let content = match fs::read_to_string(&path) {
            Ok(c) => c,
            Err(_) => return Self::default(),
        };

        let mut config = Self::default();
        for line in content.lines() {
            let line = line.trim();
            if line.is_empty() || line.starts_with('#') {
                continue;
            }
            if let Some((key, value)) = line.split_once('=') {
                let key = key.trim();
                let value = value.trim();
                match key {
                    "ui.window.x" => config.window_x = value.parse().unwrap_or(18.0),
                    "ui.window.y" => config.window_y = value.parse().unwrap_or(811.0),
                    "stock.target" => config.target_stock = value.to_string(),
                    "stock.index" => config.index_code = value.to_string(),
                    "stock.watchlist" => {
                        config.watchlist =
                            value.split(',').map(|s| s.trim().to_string()).collect()
                    }
                    "ai.gemini.apiKey" => config.gemini_api_key = value.to_string(),
                    _ => {}
                }
            }
        }
        config
    }

    pub fn save(&self) -> Result<()> {
        let path = Self::get_config_path();
        let mut content = String::from("#Vanish App Configuration\n");
        content.push_str(&format!("ui.window.x={}\n", self.window_x));
        content.push_str(&format!("ui.window.y={}\n", self.window_y));
        content.push_str(&format!("stock.target={}\n", self.target_stock));
        content.push_str(&format!("stock.index={}\n", self.index_code));
        content.push_str(&format!(
            "stock.watchlist={}\n",
            self.watchlist.join(",")
        ));
        if !self.gemini_api_key.is_empty() {
            content.push_str(&format!("ai.gemini.apiKey={}\n", self.gemini_api_key));
        }
        fs::write(&path, content)?;
        Ok(())
    }
}

#[tauri::command]
fn get_config(state: tauri::State<Mutex<AppConfig>>) -> AppConfig {
    state.lock().unwrap().clone()
}

#[tauri::command]
fn save_config(config: AppConfig, state: tauri::State<Mutex<AppConfig>>) -> Result<(), String> {
    let mut state = state.lock().map_err(|e| e.to_string())?;
    *state = config.clone();
    config.save().map_err(|e| e.to_string())
}

#[tauri::command]
async fn fetch_sina_realtime_data(codes: String) -> Result<String, String> {
    let client = reqwest::Client::new();
    let url = format!(
        "http://hq.sinajs.cn/rn={}&list={}",
        std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis(),
        codes
    );

    let response = client
        .get(&url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Referer", "https://finance.sina.com.cn/")
        .send()
        .await
        .map_err(|e| e.to_string())?;

    let bytes = response.bytes().await.map_err(|e| e.to_string())?;
    // 使用 GBK 解码
    let (text, _, _) = encoding_rs::GBK.decode(&bytes);
    Ok(text.into_owned())
}

#[tauri::command]
async fn fetch_eastmoney_timeshare(code: String) -> Result<String, String> {
    let secid = to_eastmoney_symbol(&code);
    let client = reqwest::Client::new();
    let url = format!(
        "http://push2his.eastmoney.com/api/qt/stock/trends2/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58&secid={}&ndays=1",
        secid
    );

    let response = client
        .get(&url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        )
        .header("Referer", "https://quote.eastmoney.com/")
        .send()
        .await
        .map_err(|e| e.to_string())?;

    response.text().await.map_err(|e| e.to_string())
}

fn to_eastmoney_symbol(code: &str) -> String {
    let lower_code = code.to_lowercase();
    if lower_code.starts_with("sh") {
        return format!("1.{}", lower_code.strip_prefix("sh").unwrap());
    }
    if lower_code.starts_with("sz") {
        return format!("0.{}", lower_code.strip_prefix("sz").unwrap());
    }

    if code.contains('.') {
        let parts: Vec<&str> = code.split('.').collect();
        if parts.len() == 2 {
            let market = parts[1].to_uppercase();
            let stock_code = parts[0];
            return match market.as_str() {
                "SH" => format!("1.{}", stock_code),
                "SZ" => format!("0.{}", stock_code),
                _ => code.to_string(),
            };
        }
    }

    if code.starts_with('6') {
        format!("1.{}", code)
    } else if code.starts_with('0') || code.starts_with('3') {
        format!("0.{}", code)
    } else {
        code.to_string()
    }
}

#[tauri::command]
fn update_main_window_position(app_handle: AppHandle, x: f64, y: f64) -> Result<(), String> {
    if let Some(window) = app_handle.get_webview_window("main") {
        let physical_x = (x * 2.0) as i32; // 转换为物理像素
        let physical_y = (y * 2.0) as i32;
        window
            .set_position(tauri::Position::Physical(tauri::PhysicalPosition::new(
                physical_x, physical_y,
            )))
            .map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
fn show_window(app_handle: AppHandle, label: String) -> Result<(), String> {
    if let Some(window) = app_handle.get_webview_window(&label) {
        window.show().map_err(|e| e.to_string())?;
        window.set_focus().map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
fn hide_window(app_handle: AppHandle, label: String) -> Result<(), String> {
    if let Some(window) = app_handle.get_webview_window(&label) {
        window.hide().map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
fn toggle_window(app_handle: AppHandle, label: String) -> Result<bool, String> {
    if let Some(window) = app_handle.get_webview_window(&label) {
        let is_visible = window.is_visible().map_err(|e| e.to_string())?;
        if is_visible {
            window.hide().map_err(|e| e.to_string())?;
            Ok(false)
        } else {
            window.show().map_err(|e| e.to_string())?;
            window.set_focus().map_err(|e| e.to_string())?;
            Ok(true)
        }
    } else {
        Ok(false)
    }
}

#[tauri::command]
fn exit_app(app_handle: AppHandle) {
    app_handle.exit(0);
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .manage(Mutex::new(AppConfig::load()))
        .invoke_handler(tauri::generate_handler![
            get_config,
            save_config,
            fetch_sina_realtime_data,
            fetch_eastmoney_timeshare,
            update_main_window_position,
            show_window,
            hide_window,
            toggle_window,
            exit_app
        ])
        .setup(|app| {
            // 创建托盘菜单
            let quit_i = MenuItem::with_id(app, "quit", "退出", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&quit_i])?;

            // 加载图标
            let icon_bytes = include_bytes!("../icons/icon.ico");
            let icon = Image::from_bytes(icon_bytes)?;

            let _tray = TrayIconBuilder::new()
                .icon(icon)
                .tooltip("rvan - AI 交易助手")
                .menu(&menu)
                .on_menu_event(|app, event| match event.id.as_ref() {
                    "quit" => {
                        // 保存配置
                        let config = app.state::<Mutex<AppConfig>>();
                        if let Ok(current_config) = config.lock() {
                            let mut updated_config = current_config.clone();
                            // 更新窗口位置等配置
                            if let Some(window) = app.get_webview_window("main") {
                                if let Ok(pos) = window.outer_position() {
                                    updated_config.window_x = pos.x as f64 / 2.0;
                                    updated_config.window_y = pos.y as f64 / 2.0;
                                }
                            }
                            let _ = updated_config.save();
                        }
                        app.exit(0);
                    }
                    _ => {}
                })
                .on_tray_icon_event(|tray, event| {
                    if let TrayIconEvent::Click {
                        button: MouseButton::Left,
                        button_state: MouseButtonState::Up,
                        ..
                    } = event
                    {
                        let app = tray.app_handle();
                        if let Some(window) = app.get_webview_window("main") {
                            let _ = toggle_window(app.clone(), "main".to_string());
                        }
                    }
                })
                .build(app)?;

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
