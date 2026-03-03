import React, { useEffect, useRef } from "react";

interface ContextMenuProps {
  visible: boolean;
  position: { x: number; y: number };
  onClose: () => void;
  onShowHistory: () => void;
  onShowWatchlist: () => void;
  onShowTimeShare: () => void;
  onCloseApp: () => void;
}

/**
 * 右键上下文菜单
 */
export const ContextMenu: React.FC<ContextMenuProps> = ({
  visible,
  position,
  onClose,
  onShowHistory,
  onShowWatchlist,
  onShowTimeShare,
  onCloseApp,
}) => {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!visible) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [visible, onClose]);

  if (!visible) return null;

  const menuStyle: React.CSSProperties = {
    position: "fixed",
    left: position.x,
    top: position.y,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    width: "108px",
    height: "24px",
    backgroundColor: "rgba(0, 0, 0, 0.75)",
    borderRadius: "8px",
    padding: "0 4px",
    boxSizing: "border-box",
    zIndex: 9999,
  };

  const buttonStyle: React.CSSProperties = {
    background: "none",
    border: "none",
    cursor: "pointer",
    padding: "2px",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    color: "#FFF",
  };

  const iconStyle: React.CSSProperties = {
    width: "12px",
    height: "12px",
  };

  const handleButtonClick = (callback: () => void) => {
    callback();
    onClose();
  };

  return (
    <div ref={menuRef} style={menuStyle}>
      <div style={{ display: "flex", gap: "2px", flex: 12, justifyContent: "space-evenly" }}>
        {/* 历史按钮 */}
        <button
          style={buttonStyle}
          onClick={() => handleButtonClick(onShowHistory)}
          title="历史"
        >
          <svg style={iconStyle} viewBox="0 0 24 24" fill="currentColor">
            <path d="M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z" />
          </svg>
        </button>

        {/* 自选列表按钮 */}
        <button
          style={buttonStyle}
          onClick={() => handleButtonClick(onShowWatchlist)}
          title="自选列表"
        >
          <svg style={iconStyle} viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z" />
          </svg>
        </button>

        {/* 分时图按钮 */}
        <button
          style={buttonStyle}
          onClick={() => handleButtonClick(onShowTimeShare)}
          title="分时图"
        >
          <svg style={iconStyle} viewBox="0 0 24 24" fill="currentColor">
            <path d="M13 2.05v3.03c3.39.49 6 3.39 6 6.92 0 .9-.18 1.75-.48 2.54l2.6 1.53c.56-1.24.88-2.62.88-4.07 0-5.18-3.95-9.45-9-9.95zM12 19c-3.87 0-7-3.13-7-7 0-3.53 2.61-6.43 6-6.92V2.05c-5.06.5-9 4.76-9 9.95 0 5.52 4.47 10 9.99 10 3.31 0 6.24-1.61 8.06-4.09l-2.6-1.53C16.17 17.98 14.21 19 12 19z" />
          </svg>
        </button>
      </div>

      <div style={{ display: "flex", flex: 1, justifyContent: "flex-end" }}>
        {/* 关闭按钮 */}
        <button
          style={{ ...buttonStyle, marginRight: "2px" }}
          onClick={onCloseApp}
          title="关闭"
        >
          <svg style={{ width: "6px", height: "6px" }} viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
          </svg>
        </button>
      </div>
    </div>
  );
};
