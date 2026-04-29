import React, { useState } from "react";
import { StockData } from "../data/model/StockData";
import { getColor, formatPrice, formatPercent } from "../utils/color";
import { invoke } from "@tauri-apps/api/core";

interface WatchlistScreenProps {
  visible: boolean;
  watchlistData: StockData[];
  watchlistCodes: string[];
  lastUpdateTime: string;
  position: { x: number; y: number };
  size: { width: number; height: number };
  // onClose: () => void;
  onAddStock: (code: string) => void;
  onRemoveStock: (code: string) => void;
  onReorder: (from: number, to: number) => void;
  onSwitchTarget: (code: string) => void;
}

/**
 * 自选股列表弹窗
 */
export const WatchlistScreen: React.FC<WatchlistScreenProps> = (
  {
    visible,
    watchlistData,
    watchlistCodes,
    lastUpdateTime,
    position,
    size,
    // onClose,
    onAddStock,
    onRemoveStock,
    onReorder,
    onSwitchTarget,
  }
) => {
    const [showAddBar, setShowAddBar] = useState(false);
    const [inputCode, setInputCode] = useState("");
    const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
    const [inputKey, setInputKey] = useState(0);

    const setInputFocusMode = async (focusable: boolean) => {
      try {
        await invoke("set_watchlist_focusable", { focusable });
      } catch (error) {
        console.error(error);
      }
    };

    // 处理拖拽开始
    const handleDragStart = (index: number) => {
      setDraggedIndex(index);
    };

    // 处理拖拽移动
    const handleDragOver = (e: React.DragEvent, index: number) => {
      e.preventDefault();
      if (draggedIndex === null || draggedIndex === index) return;

      const rect = (e.target as HTMLElement).getBoundingClientRect();
      const midY = rect.top + rect.height / 2;

      if (e.clientY < midY) {
        onReorder(draggedIndex, index);
        setDraggedIndex(index);
      } else {
        onReorder(draggedIndex, index + 1 > watchlistCodes.length ? watchlistCodes.length - 1 : index + 1);
        setDraggedIndex(index + 1 > watchlistCodes.length ? watchlistCodes.length - 1 : index + 1);
      }
    };

    // 处理添加确认
    const handleAddConfirm = () => {
      if (inputCode.trim()) {
        onAddStock(inputCode.trim().toLowerCase());
        setInputCode("");
        setShowAddBar(false);
        setInputFocusMode(false);
      }
    };

    // 处理按键
    const handleKeyDown = (e: React.KeyboardEvent) => {
      if (e.key === "Enter") {
        handleAddConfirm();
      } else if (e.key === "Escape") {
        setShowAddBar(false);
        setInputCode("");
        setInputFocusMode(false);
      }
    };

    if (!visible) return null;

    const containerStyle: React.CSSProperties = {
      position: "fixed",
      left: position.x,
      top: position.y,
      width: size.width,
      height: size.height,
      backgroundColor: "rgba(0, 0, 0, 0.7)",
      borderRadius: "4px",
      overflow: "hidden",
      zIndex: 9998,
      display: "flex",
      flexDirection: "column",
    };

    const headerStyle: React.CSSProperties = {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      padding: "2px 4px",
      minHeight: "16px",
    };

    const timeStyle: React.CSSProperties = {
      color: "#AAA",
      fontSize: "7px",
      fontFamily: "var(--number-font)",
    };

    const addButtonStyle: React.CSSProperties = {
      background: "none",
      border: "none",
      color: "#FFF",
      cursor: "pointer",
      padding: "0",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
    };

    const addBarStyle: React.CSSProperties = {
      display: "flex",
      alignItems: "center",
      backgroundColor: "rgba(0, 0, 0, 0.5)",
      borderRadius: "4px",
      padding: "0 2px",
    };

    const inputStyle: React.CSSProperties = {
      width: "35px",
      background: "none",
      border: "none",
      color: "#FFF",
      fontSize: "6px",
      lineHeight: "6px",
      fontWeight: "bold",
      padding: "0 2px",
      outline: "none",
    };

    const iconButtonStyle: React.CSSProperties = {
      background: "none",
      border: "none",
      cursor: "pointer",
      padding: "0",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
    };

    const listStyle: React.CSSProperties = {
      flex: 1,
      overflowY: "auto",
      padding: "0 4px",
    };

    const rowStyle: React.CSSProperties = {
      display: "flex",
      alignItems: "center",
      padding: "0",
      cursor: "pointer",
    };

    const nameStyle: React.CSSProperties = {
      flex: 1,
      color: "#FFF",
      fontSize: "8px",
      textAlign: "left",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap",
    };

    const codeStyle: React.CSSProperties = {
      flex: 1,
      color: "#AAA",
      fontSize: "10px",
      textAlign: "left",
    };

    const priceStyle: React.CSSProperties = {
      flex: 1.1,
      color: "#FFF",
      fontSize: "10px",
      fontFamily: "var(--number-font)",
      textAlign: "right",
    };

    const changeStyle: React.CSSProperties = {
      flex: 1.1,
      fontSize: "10px",
      fontFamily: "var(--number-font)",
      textAlign: "right",
    };

    const riseStyle: React.CSSProperties = {
      flex: 1,
      fontSize: "10px",
      fontFamily: "var(--number-font)",
      textAlign: "right",
    };

    const removeButtonStyle: React.CSSProperties = {
      background: "none",
      border: "none",
      color: "#d81e06",
      cursor: "pointer",
      padding: "0 2px",
      fontSize: "10px",
      opacity: 0,
      transition: "opacity 0.2s",
    };

    return (
      <div style={containerStyle}>
        {/* 顶部操作栏 */}
        <div style={headerStyle}>
          <div style={timeStyle}>{lastUpdateTime}</div>

          {showAddBar ? (
            <div style={addBarStyle}>
              <input
                key={inputKey}
                style={inputStyle}
                type="text"
                value={inputCode}
                onChange={(e) => setInputCode(e.target.value.toUpperCase())}
                onKeyDown={handleKeyDown}
                autoFocus
                maxLength={8}
              />
              <button
                style={{ ...iconButtonStyle, color: "#1aad19" }}
                onClick={handleAddConfirm}
                disabled={!inputCode.trim()}
              >
                <svg width="8px" height="8px" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                </svg>
              </button>
              <button
                style={{ ...iconButtonStyle, color: "#d81e06" }}
                onClick={() => {
                  setShowAddBar(false);
                  setInputCode("");
                  setInputFocusMode(false);
                }}
              >
                <svg width="8px" height="8px" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
                </svg>
              </button>
            </div>
          ) : (
            <button
              style={addButtonStyle}
              onClick={async () => {
                await setInputFocusMode(true);
                setInputKey((prev) => prev + 1);
                setShowAddBar(true);
              }}
            >
              <svg width="8px" height="8px" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
              </svg>
            </button>
          )}
        </div>

        {/* 自选股列表 */}
        <div style={listStyle}>
          {watchlistData.length === 0 ? null : watchlistData.map((data, index) => (
            <div
              key={data.code}
              draggable
              onDragStart={() => handleDragStart(index)}
              onDragOver={(e) => handleDragOver(e, index)}
              style={{
                ...rowStyle,
                backgroundColor: draggedIndex === index ? "rgba(255, 255, 255, 0.1)" : "transparent",
              }}
              onMouseEnter={(e) => {
                const removeBtn = e.currentTarget.querySelector(".remove-btn");
                if (removeBtn) {
                  (removeBtn as HTMLElement).style.opacity = "1";
                }
              }}
              onMouseLeave={(e) => {
                const removeBtn = e.currentTarget.querySelector(".remove-btn");
                if (removeBtn) {
                  (removeBtn as HTMLElement).style.opacity = "0";
                }
              }}
              onClick={() => onSwitchTarget(data.code)}
            >
              <div style={nameStyle}>{data.name}</div>
              <div style={codeStyle}>{data.code.replace(/^sh|^sz/, "")}</div>
              <div style={{ ...priceStyle, color: getColor(data.changePercent) }}>
                {formatPrice(data.price)}
              </div>
              <div style={{ ...changeStyle, color: getColor(data.changePercent) }}>
                {formatPercent(data.changePercent)}
              </div>
              <div style={{ ...riseStyle, color: getColor(data.rise) }}>
                {formatPercent(data.rise)}
              </div>
              <button
                className="remove-btn"
                style={removeButtonStyle}
                onClick={(e) => {
                  e.stopPropagation();
                  onRemoveStock(data.code);
                }}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      </div>
    );
  };
