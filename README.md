# 🫥 Vanish

> **A cross-platform, minimalist AI trading companion.**  
> Seamlessly floats on your desktop—always there, never in the way.

Built with **Kotlin** and **Compose Multiplatform**, *Vanish* delivers real-time stock insights with elegance and efficiency across **Windows, macOS, and Linux**.

## ✨ Features

- **Real-Time Market Data**: Fetches live quotes from reliable financial APIs (e.g., Sina Finance).
- **Always-On-Top Overlay**: Transparent, draggable window that stays visible without obstructing your workflow.
- **Smart Context Menu**: Right-click for quick access to history, watchlist, or exit.
- **Double-Click Interaction**: Toggle detailed views (e.g., historical trends) with a simple double-tap.
- **Customizable Watchlist**: Manage your favorite stocks directly from the UI; changes persist across sessions.
- **System Tray Integration**: Minimize to tray and restore with a single click—perfect for long-term monitoring.
- **Lightweight & Efficient**: Minimal resource usage, designed for 24/7 operation.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Desktop)
- **Networking**: Ktor Client
- **State Management**: Compose State + ViewModel pattern
- **Configuration**: Type-safe, persistent config via `AppConfig`
- **Logging**: Built-in rotating file logger for diagnostics

## 📦 Configuration

All settings are managed through a clean, type-safe `AppConfig` system:

- Target stock
- Index benchmark (`sh000001`)
- Custom watchlist
- Window position

No more magic strings—everything is centralized and persisted in `config.properties`.
