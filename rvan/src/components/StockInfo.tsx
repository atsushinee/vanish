import React from "react";
import { StockData } from "../data/model/StockData";
import { getColor, formatPrice, formatPercent } from "../utils/color";

interface StockInfoProps {
  stockData: StockData | null;
}

/**
 * 股票信息显示组件
 */
export const StockInfo: React.FC<StockInfoProps> = ({ stockData }) => {
  const textOpacity = 0.4;

  if (!stockData) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          height: "100%",
          color: "#888",
          fontSize: "10px",
          opacity: textOpacity,
        }}
      >
        加载中...
      </div>
    );
  }

  const priceColor = getColor(stockData.changePercent);

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "2px 6px",
        height: "100%",
        boxSizing: "border-box",
        backgroundColor: "rgba(0, 0, 0, 0)",
        borderRadius: "8px",
      }}
    >
      <div
        style={{
          flex: 1.2,
          color: priceColor,
          fontSize: "11px",
          fontFamily: "var(--number-font)",
          textAlign: "center",
          fontWeight: "bold",
          opacity: textOpacity,
        }}
      >
        {formatPrice(stockData.price)}
      </div>

      {/* 涨跌幅 */}
      <div
        style={{
          flex: 1.1,
          color: priceColor,
          fontSize: "10px",
          fontFamily: "var(--number-font)",
          textAlign: "center",
          opacity: textOpacity,
        }}
      >
        {formatPercent(stockData.changePercent)}
      </div>

      <div
        style={{
          flex: 1,
          color: getColor(stockData.indexPercent),
          fontSize: "10px",
          fontFamily: "var(--number-font)",
          textAlign: "center",
          opacity: textOpacity,
        }}
      >
        {formatPercent(stockData.indexPercent)}
      </div>
    </div>
  );
};
