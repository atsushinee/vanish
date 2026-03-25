import React, { useEffect, useState, useCallback } from "react";
import { Observer } from "mobx-react-lite";
import { StockViewModel } from "./viewmodels/StockViewModel";
import { TimeShareViewModel } from "./viewmodels/TimeShareViewModel";
import { StockInfo } from "./components/StockInfo";
import { HistoryScreen } from "./screens/HistoryScreen";
import { WatchlistScreen } from "./screens/WatchlistScreen";
import { TimeShareScreen } from "./screens/TimeShareScreen";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { loadConfig } from "./config/AppConfig";

/**
 * 主应用组件
 */
export const App: React.FC = () => {
  // ViewModel
  const [stockViewModel] = useState(() => new StockViewModel());
  const [timeShareViewModel, setTimeShareViewModel] = useState<TimeShareViewModel | null>(null);

  // UI 状态
  const [isWindowVisible, setIsWindowVisible] = useState(true);
  const [showHistory, setShowHistory] = useState(false);
  const [showWatchlist, setShowWatchlist] = useState(false);
  const [showTimeShare, setShowTimeShare] = useState(false);

  // 窗口位置（像素）— 实际由 Rust 管理
  const [windowPosition, setWindowPosition] = useState({ x: 18, y: 811 });
  // const windowPosition = { x: 18, y: 811 };

  // 窗口尺寸
  const windowWidth = 108;
  const windowHeight = 24;

  // 弹窗位置和尺寸
  const historyPosition = { x: windowPosition.x - 66, y: windowPosition.y - 45 };
  const historySize = { width: 240, height: 45 };

  const watchlistPosition = { x: windowPosition.x - 46, y: windowPosition.y - 70 };
  const watchlistSize = { width: 200, height: 70 };

  const timeSharePosition = { x: windowPosition.x - 46, y: windowPosition.y - 70 };
  const timeShareSize = { width: 200, height: 70 };


  // 处理双击
  const handleDoubleClick = useCallback(() => {
    setShowWatchlist((prev) => !prev);
    setShowTimeShare(false);
  }, []);

  // 处理关闭应用
  const handleCloseApp = useCallback(async () => {
    try {
      await invoke("exit_app");
    } catch (error) {
      console.error("退出应用失败:", error);
    }
  }, []);

  // 切换主窗口可见性
  const toggleMainWindow = useCallback(async () => {
    const visible = await invoke<boolean>("toggle_window", { label: "main" });
    setIsWindowVisible(visible);
    if (!visible) {
      setShowHistory(false);
      setShowWatchlist(false);
      setShowTimeShare(false);
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
    if (showWatchlist) {
      stockViewModel.startWatchlistMonitor();
    } else {
      stockViewModel.stopWatchlistMonitor();
    }
  }, [showWatchlist, stockViewModel]);

  // 初始化 TimeShareViewModel
  useEffect(() => {
    if (stockViewModel.stockData && showTimeShare && !timeShareViewModel) {
      const vm = new TimeShareViewModel(stockViewModel.stockData.code);
      setTimeShareViewModel(vm);
    }
  }, [stockViewModel.stockData, showTimeShare, timeShareViewModel]);

  // 加载窗口位置
  useEffect(() => {
    loadConfig().then((config) => {
      setWindowPosition({ x: config.window_x, y: config.window_y });
    });
  }, []);

  // 更新时间线 ViewModel
  useEffect(() => {
    if (timeShareViewModel && stockViewModel.stockData) {
      timeShareViewModel.updateCode(stockViewModel.stockData.code);
    }
  }, [stockViewModel.stockData?.code, timeShareViewModel]);

  // 定时刷新分时图
  useEffect(() => {
    if (!showTimeShare || !timeShareViewModel) return;

    const interval = setInterval(() => {
      timeShareViewModel.loadTimeShareData();
    }, 5000);

    return () => clearInterval(interval);
  }, [showTimeShare, timeShareViewModel]);

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
      setShowWatchlist(false);
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
          onContextMenu={()=>{}}
          onDoubleClick={handleDoubleClick}
        >
          {/* 主窗口内容 */}
          {isWindowVisible && (
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
              <StockInfo stockData={stockViewModel.stockData} />
            </div>
          )}


          {/* 历史记录弹窗 */}
          <HistoryScreen
            visible={showHistory}
            history={stockViewModel.history}
            position={historyPosition}
            size={historySize}
            onClose={() => setShowHistory(false)}
          />

          {/* 自选股列表弹窗 */}
          <WatchlistScreen
            visible={showWatchlist}
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
            visible={showTimeShare}
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
            onClose={() => setShowTimeShare(false)}
          />
        </div>
      )}
    </Observer>
  );
};

export default App;
