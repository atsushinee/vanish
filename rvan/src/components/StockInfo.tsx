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
      }}
    >
      {/* 股票名称 */}
      <div
        style={{
          flex: 1.5,
          color: "#FFF",
          fontSize: "10px",
          fontWeight: "bold",
          overflow: "hidden",
          textOverflow: "ellipsis",
          whiteSpace: "nowrap",
          textAlign: "left",
        }}
      >
        {stockData.name || stockData.code}
      </div>

      {/* 价格 */}
      <div
        style={{
          flex: 1.2,
          color: priceColor,
          fontSize: "11px",
          fontFamily: "monospace",
          textAlign: "right",
          fontWeight: "bold",
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
          fontFamily: "monospace",
          textAlign: "right",
        }}
      >
        {formatPercent(stockData.changePercent)}
      </div>

      {/* 涨速 */}
      <div
        style={{
          flex: 1,
          color: getColor(stockData.rise),
          fontSize: "10px",
          fontFamily: "monospace",
          textAlign: "right",
        }}
      >
        {formatPercent(stockData.rise)}
      </div>

      {/* 指数 */}
      <div
        style={{
          flex: 1,
          color: getColor(stockData.indexPercent),
          fontSize: "10px",
          fontFamily: "monospace",
          textAlign: "right",
        }}
      >
        {formatPercent(stockData.indexPercent)}
      </div>
    </div>
  );
};
