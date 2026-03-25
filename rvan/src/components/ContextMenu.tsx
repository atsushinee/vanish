import React from "react";

interface ContextMenuProps {
  visible: boolean;
  onWatchlist: () => void;
  onTimeShare: () => void;
  onCloseApp: () => void;
  onDismiss: () => void;
}

export const ContextMenu: React.FC<ContextMenuProps> = ({
  visible,
  onWatchlist,
  onTimeShare,
  onCloseApp,
  onDismiss,
}) => {
  if (!visible) return null;

  const handleAction = (fn: () => void) => {
    fn();
    onDismiss();
  };

  const iconButtonStyle: React.CSSProperties = {
    width: "18px",
    height: "18px",
    border: "none",
    borderRadius: "4px",
    background: "transparent",
    color: "#fff",
    padding: 0,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer",
  };

  const closeButtonStyle: React.CSSProperties = {
    ...iconButtonStyle,
    width: "9px",
    height: "9px",
  };

  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 10000,
      }}
      onContextMenu={(e) => {
        e.preventDefault();
        const target = e.target as HTMLElement;
        if (!target.closest("button")) {
          onDismiss();
        }
      }}
      onDoubleClick={(e) => {
        const target = e.target as HTMLElement;
        if (!target.closest("button")) {
          onDismiss();
        }
      }}
    >
      <div
        style={{
          width: "100%",
          height: "100%",
          borderRadius: "8px",
          backgroundColor: "rgba(0, 0, 0, 0.5)",
          position: "relative",
          boxSizing: "border-box",
        }}
      >
        <div
          style={{
            position: "absolute",
            left: "50%",
            top: "50%",
            transform: "translate(-50%, -50%)",
            display: "flex",
            alignItems: "center",
            gap: "10px",
          }}
        >
          <button style={iconButtonStyle} onClick={() => handleAction(onWatchlist)} title="Watchlist">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 13h2v-2H3v2zm0 6h2v-2H3v2zm0-12h2V5H3v2zm4 12h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z" />
            </svg>
          </button>
          <button style={iconButtonStyle} onClick={() => handleAction(onTimeShare)} title="TimeShare">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 17h2.59L9 13.59l4 4L20.59 10H23v2h-1.59L13 20.41l-4-4L7 18.41V21H3z" />
            </svg>
          </button>
        </div>
        <button
          style={{
            ...closeButtonStyle,
            position: "absolute",
            right: "8px",
            top: "50%",
            transform: "translateY(-50%)",
          }}
          onClick={() => handleAction(onCloseApp)}
          title="Close"
        >
          <svg width="6" height="6" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
          </svg>
        </button>
      </div>
    </div>
  );
};
