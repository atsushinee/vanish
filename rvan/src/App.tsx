import React, { useEffect, useState, useCallback } from "react";
import { Observer } from "mobx-react-lite";
import { StockViewModel } from "./viewmodels/StockViewModel";
import { TimeShareViewModel } from "./viewmodels/TimeShareViewModel";
import { StockInfo } from "./components/StockInfo";
import { ContextMenu } from "./components/ContextMenu";
import { WatchlistScreen } from "./screens/WatchlistScreen";
import { TimeShareScreen } from "./screens/TimeShareScreen";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { loadConfig } from "./config/AppConfig";

/**
 * 主应用组件
 */
export const App: React.FC = () => {
  const mainWindow = getCurrentWindow();
  const isMainWindow = mainWindow.label === "main";
  const isWatchlistWindow = mainWindow.label === "watchlist";
  const isTimeShareWindow = mainWindow.label === "timeshare";
  const watchlistDialogSize = { width: 200, height: 70 };
  const timeShareWindowSize = { width: 200, height: 70 };

  // ViewModel
  const [stockViewModel] = useState(() => new StockViewModel());
  const [timeShareViewModel, setTimeShareViewModel] = useState<TimeShareViewModel | null>(null);
  const [timeShareCode, setTimeShareCode] = useState<string | null>(null);

  // UI 状态
  const [isWindowVisible, setIsWindowVisible] = useState(true);
  const [showContextMenu, setShowContextMenu] = useState(false);

  // 窗口位置（像素）— 实际由 Rust 管理

  // 窗口尺寸
  // 弹窗位置和尺寸
  const watchlistPosition = { x: 0, y: 0 };
  const watchlistSize = { width: watchlistDialogSize.width, height: watchlistDialogSize.height };

  const timeSharePosition = { x: 0, y: 0 };
  const timeShareSize = { width: timeShareWindowSize.width, height: timeShareWindowSize.height };


  // 处理双击
  const handleDoubleClick = useCallback(async () => {
    if (!isMainWindow) return;

    try {
      await invoke("toggle_watchlist_near_main");
    } catch (error) {
      console.error("toggle watchlist dialog failed", error);
    }
  }, [isMainWindow]);

  // 处理关闭应用
  const handleCloseApp = useCallback(async () => {
    try {
      await invoke("exit_app");
    } catch (error) {
      console.error("退出应用失败:", error);
    }
  }, []);

  const handleShowWatchlist = useCallback(async () => {
    await invoke("toggle_watchlist_near_main");
  }, []);

  const handleToggleTimeShare = useCallback(async () => {
    await invoke("toggle_timeshare_near_main");
  }, []);

  // 切换主窗口可见性
  const toggleMainWindow = useCallback(async () => {
    const visible = await invoke<boolean>("toggle_window", { label: "main" });
    setIsWindowVisible(visible);
    if (!visible) {
      setShowContextMenu(false);
      invoke("hide_window", { label: "watchlist" }).catch(console.error);
      invoke("hide_window", { label: "timeshare" }).catch(console.error);
    }
  }, []);

  // 监听托盘点击事件
  useEffect(() => {
    const unlisten = listen("tauri://tray-icon-click", () => {
      toggleMainWindow();
    });

    return () => {
      unlisten.then((fn) => fn());
    };
  }, [toggleMainWindow]);

  // 处理自选股监控启动/停止
  useEffect(() => {
    if (!isWatchlistWindow) return;
    stockViewModel.startWatchlistMonitor();
    return () => {
      stockViewModel.stopWatchlistMonitor();
    };
  }, [isWatchlistWindow, stockViewModel]);

  // 初始化 TimeShareViewModel
  useEffect(() => {
    if (!isTimeShareWindow) return;
    loadConfig().then((config) => setTimeShareCode(config.target_stock));
  }, [isTimeShareWindow]);

  // 加载窗口位置
  useEffect(() => {
    if (!isTimeShareWindow) return;
    const code = stockViewModel.stockData?.code;
    if (code) {
      setTimeShareCode(code);
    }
  }, [isTimeShareWindow, stockViewModel.stockData?.code]);

  // 更新时间线 ViewModel
  useEffect(() => {
    if (!isTimeShareWindow || !timeShareCode) return;
    if (!timeShareViewModel) {
      setTimeShareViewModel(new TimeShareViewModel(timeShareCode));
      return;
    }
    timeShareViewModel.updateCode(timeShareCode);
  }, [isTimeShareWindow, timeShareCode, timeShareViewModel]);

  // 定时刷新分时图
  useEffect(() => {
    if (!isTimeShareWindow || !timeShareViewModel) return;

    const interval = setInterval(() => {
      timeShareViewModel.loadTimeShareData();
    }, 5000);

    return () => clearInterval(interval);
  }, [isTimeShareWindow, timeShareViewModel]);

  // 处理添加自选股
  const handleAddStock = useCallback(
    async (code: string) => {
      await stockViewModel.addWatchlistCode(code);
    },
    [stockViewModel]
  );

  // 处理移除自选股
  const handleRemoveStock = useCallback(
    async (code: string) => {
      await stockViewModel.removeWatchlistCode(code);
    },
    [stockViewModel]
  );

  // 处理 reorder
  const handleReorder = useCallback(
    async (from: number, to: number) => {
      await stockViewModel.reorderWatchlist(from, to);
    },
    [stockViewModel]
  );

  // 处理切换目标股票
  const handleSwitchTarget = useCallback(
    async (code: string) => {
      await stockViewModel.switchTargetStock(code);
    },
    [stockViewModel]
  );

  return (
    <Observer>
      {() => (
        <div
          style={{
            width: "100vw",
            height: "100vh",
            overflow: "hidden",
          }}
          onContextMenu={(e) => {
            e.preventDefault();
            if (isMainWindow) {
              setShowContextMenu(true);
            }
          }}
        >
          {/* 主窗口内容 */}
          {isMainWindow && isWindowVisible && (
            <div
              style={{
                position: "fixed",
                inset: 0,
                backgroundColor: "rgba(0, 0, 0, 0)",
                borderRadius: "8px",
                userSelect: "none",
                pointerEvents: "none", // 关键：点击穿透到原生窗口
              }}
            >
              <div
                data-tauri-drag-region
                onDoubleClick={handleDoubleClick}
                style={{
                  position: "absolute",
                  inset: 0,
                  pointerEvents: "auto",
                  backgroundColor: "transparent",
                }}
              />
              <StockInfo stockData={stockViewModel.stockData} />
            </div>
          )}


          {/* 历史记录弹窗 */}
          {isMainWindow && (
            <ContextMenu
              visible={showContextMenu}
              onWatchlist={handleShowWatchlist}
              onTimeShare={handleToggleTimeShare}
              onCloseApp={handleCloseApp}
              onDismiss={() => setShowContextMenu(false)}
            />
          )}

          {/* 自选股列表弹窗 */}
          <WatchlistScreen
            visible={isWatchlistWindow}
            watchlistData={stockViewModel.watchlistData}
            watchlistCodes={stockViewModel.watchlistCodes}
            lastUpdateTime={stockViewModel.lastUpdateTime}
            position={watchlistPosition}
            size={watchlistSize}
            onAddStock={handleAddStock}
            onRemoveStock={handleRemoveStock}
            onReorder={handleReorder}
            onSwitchTarget={handleSwitchTarget}
          />

          {/* 分时图弹窗 */}
          <TimeShareScreen
            visible={isTimeShareWindow}
            position={timeSharePosition}
            size={timeShareSize}
            points={
              timeShareViewModel?.uiState.type === "success"
                ? timeShareViewModel.uiState.points
                : []
            }
            preClosePrice={
              timeShareViewModel?.uiState.type === "success"
                ? timeShareViewModel.uiState.preClosePrice
                : 0
            }
            onClose={() => {
              invoke("hide_window", { label: "timeshare" }).catch(console.error);
            }}
          />
        </div>
      )}
    </Observer>
  );
};

export default App;
