import React, { useEffect, useRef } from "react";
import { StockData } from "../data/model/StockData";
import { getColor, formatPrice, formatPercent } from "../utils/color";

interface HistoryScreenProps {
  visible: boolean;
  history: StockData[];
  position: { x: number; y: number };
  size: { width: number; height: number };
  onClose: () => void;
}

/**
 * 历史记录弹窗
 */
export const HistoryScreen: React.FC<HistoryScreenProps> = (
  { visible, history, position, size, onClose }
) => {
    const listRef = useRef<HTMLDivElement>(null);
    const prevLengthRef = useRef<number>(0);

    // 自动滚动到最新
    useEffect(() => {
      if (!listRef.current || history.length === 0) return;

      const shouldScroll =
        prevLengthRef.current === 0 ||
        listRef.current.scrollTop + listRef.current.clientHeight >=
          listRef.current.scrollHeight - 20;

      if (shouldScroll) {
        listRef.current.scrollTop = listRef.current.scrollHeight;
      }

      prevLengthRef.current = history.length;
    }, [history.length]);

    if (!visible) return null;

    const containerStyle: React.CSSProperties = {
      position: "fixed",
      left: position.x,
      top: position.y,
      width: size.width,
      height: size.height,
      backgroundColor: "rgba(0, 0, 0, 0.4)",
      borderRadius: "4px",
      overflow: "hidden",
      zIndex: 9998,
      cursor: "pointer",
    };

    const listStyle: React.CSSProperties = {
      width: "100%",
      height: "100%",
      overflowY: "auto",
      padding: "0 4px",
      boxSizing: "border-box",
    };

    const rowStyle: React.CSSProperties = {
      display: "flex",
      alignItems: "center",
      padding: "1px 0",
    };

    const timeStyle: React.CSSProperties = {
      width: "50px",
      color: "#FFF",
      fontSize: "11px",
      fontFamily: "monospace",
      textAlign: "center",
    };

    const priceStyle: React.CSSProperties = {
      width: "46px",
      color: "#FFF",
      fontSize: "11px",
      fontFamily: "monospace",
      textAlign: "right",
    };

    const percentStyle: React.CSSProperties = {
      width: "46px",
      fontSize: "11px",
      fontFamily: "monospace",
      textAlign: "right",
    };

    const riseStyle: React.CSSProperties = {
      width: "41px",
      fontSize: "11px",
      fontFamily: "monospace",
      textAlign: "right",
    };

    const indexStyle: React.CSSProperties = {
      width: "41px",
      fontSize: "11px",
      fontFamily: "monospace",
      textAlign: "right",
    };

    const formatTime = (date: Date): string => {
      return date.toLocaleTimeString("zh-CN", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
    };

    return (
      <div style={containerStyle} onClick={onClose}>
        <div ref={listRef} style={listStyle}>
          {history.map((data, index) => (
            <div key={`${data.timestamp.getTime()}-${index}`} style={rowStyle}>
              <div style={timeStyle}>{formatTime(data.timestamp)}</div>
              <div style={priceStyle}>{formatPrice(data.price)}</div>
              <div style={{ ...percentStyle, color: getColor(data.changePercent) }}>
                {formatPercent(data.changePercent)}
              </div>
              <div style={{ ...riseStyle, color: getColor(data.rise) }}>
                {formatPercent(data.rise)}
              </div>
              <div style={{ ...indexStyle, color: getColor(data.indexPercent) }}>
                {formatPercent(data.indexPercent)}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };
