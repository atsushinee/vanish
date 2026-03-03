# rvan

> **Vanish 项目的 Tauri + React + TypeScript 重构版本**
> 跨平台、极简主义的 AI 交易助手

## 项目简介

rvan 是原 vanish 项目的重构版本，使用现代化的技术栈实现：

- **前端**: React + TypeScript
- **后端**: Tauri (Rust)
- **UI**: 原生 CSS，保持与原项目一致的外观和交互

## 功能特性

- **实时行情数据**: 从新浪财经获取实时股票报价
- **置顶透明窗口**: 透明、可拖拽的窗口，始终置顶显示
- **智能右键菜单**: 右键点击访问历史记录、自选股、分时图
- **双击交互**: 双击快速切换自选股列表
- **自定义自选股**: 管理喜欢的股票，支持拖拽排序
- **系统托盘集成**: 最小化到托盘，单击恢复
- **轻量高效**: 专为 24/7 运行设计

## 技术架构

### 目录结构

```
rvan/
├── src/                      # 前端源代码
│   ├── components/           # React 组件
│   │   ├── StockInfo.tsx     # 股票信息显示
│   │   └── ContextMenu.tsx   # 右键菜单
│   ├── screens/              # 弹窗屏幕组件
│   │   ├── HistoryScreen.tsx     # 历史记录
│   │   ├── WatchlistScreen.tsx   # 自选股列表
│   │   └── TimeShareScreen.tsx   # 分时图
│   ├── viewmodels/           # MVVM 模式的状态管理
│   │   ├── StockViewModel.ts     # 股票数据管理
│   │   └── TimeShareViewModel.ts # 分时图数据管理
│   ├── data/
│   │   ├── model/            # 数据模型
│   │   └── remote/           # API 客户端
│   ├── config/               # 配置管理
│   └── utils/                # 工具函数
├── src-tauri/                # Tauri 后端 (Rust)
│   ├── src/
│   │   └── lib.rs            # 主后端逻辑
│   ├── capabilities/         # 权限配置
│   └── tauri.conf.json       # Tauri 配置
└── package.json              # 前端依赖
```

## 开发指南

### 环境要求

- Node.js 18+
- Rust 1.70+
- npm 或 yarn

### 安装依赖

```bash
cd rvan
npm install
```

### 开发模式

```bash
npm run tauri dev
```

### 构建应用

```bash
npm run tauri build
```

## 配置说明

配置文件 `config.properties` 位于应用运行目录：

```properties
# 窗口位置
ui.window.x=18.0
ui.window.y=811.0

# 目标股票
stock.target=sh600593

# 大盘指数
stock.index=sh000001

# 自选股列表
stock.watchlist=sh000001,sz002413,sz002639,sh603273
```

## 交互说明

### 主窗口
- **左键拖拽**: 移动窗口位置
- **右键点击**: 显示上下文菜单
- **双击**: 切换自选股列表
- **悬停**: 显示窗口（透明度变化）

### 系统托盘
- **左键点击**: 显示/隐藏主窗口
- **右键点击**: 显示托盘菜单（退出）

### 上下文菜单
- **历史**: 查看历史数据
- **自选列表**: 管理自选股
- **分时图**: 查看分钟级价格走势
- **关闭**: 退出应用

## 与原 vanish 项目的对比

| 特性 | vanish (原项目) | rvan (重构版) |
|------|----------------|---------------|
| 语言 | Kotlin | TypeScript + Rust |
| UI 框架 | Compose Multiplatform | React |
| 网络 | Ktor | reqwest (Rust) |
| 状态管理 | Compose State + ViewModel | MobX |
| 打包 | 原生分发 | Tauri 打包 |
| 资源占用 | 较低 | 更低 |

## 许可证

与原 vanish 项目保持一致
