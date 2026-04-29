import React, { useEffect, useRef } from "react";
import { TimeSharePoint } from "../data/model/StockData";

interface TimeShareScreenProps {
  visible: boolean;
  position: { x: number; y: number };
  size: { width: number; height: number };
  points: TimeSharePoint[];
  preClosePrice: number;
  onClose: () => void;
}

/**
 * 分时图弹窗
 */
export const TimeShareScreen: React.FC<TimeShareScreenProps> = (
  { visible, position, size, points, preClosePrice, onClose }
) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);

    useEffect(() => {
      if (!visible || !canvasRef.current || points.length === 0) return;

      const canvas = canvasRef.current;
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      // 设置画布尺寸（考虑 DPI）
      const dpr = window.devicePixelRatio || 1;
      const rect = canvas.getBoundingClientRect();
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      ctx.scale(dpr, dpr);

      const width = rect.width;
      const height = rect.height;
      const padding = { top: 4, right: 8, bottom: 4, left: 8 };

      // 清空画布
      ctx.clearRect(0, 0, width, height);

      // 计算价格范围
      const maxDiff = Math.max(
        ...points.map((p) => Math.max(p.price - preClosePrice, preClosePrice - p.price))
      );
      const priceRange = Math.max(maxDiff * 2, 0.01);
      const minPrice = preClosePrice - maxDiff;
      const yScale = (height - padding.top - padding.bottom) / priceRange;

      // 计算 X 轴缩放（255 分钟：9:15-15:00）
      const totalMinutes = 255;
      const xScale = (width - padding.left - padding.right) / totalMinutes;

      // 绘制背景网格
      ctx.strokeStyle = "gray";
      ctx.setLineDash([4, 4]);
      ctx.lineWidth = 1;

      // 水平中线（昨收价）
      const midY = padding.top + (height - padding.top - padding.bottom) / 2;
      ctx.beginPath();
      ctx.moveTo(padding.left, midY);
      ctx.lineTo(width - padding.right, midY);
      ctx.stroke();

      // 垂直时间线
      const timeMarkers = [0, 15, 75, 135, 195, 255]; // 9:15, 9:30, 10:30, 11:30/13:00, 14:00, 15:00
      timeMarkers.forEach((minute) => {
        const x = padding.left + minute * xScale;
        ctx.beginPath();
        ctx.moveTo(x, padding.top);
        ctx.lineTo(x, height - padding.bottom);
        ctx.stroke();
      });

      // 绘制价格线
      ctx.setLineDash([]);
      ctx.lineWidth = 1.5;

      if (points.length >= 2) {
        ctx.beginPath();

        for (let i = 0; i < points.length; i++) {
          const p = points[i];
          const x = padding.left + i * xScale;
          const y = height - padding.bottom - (p.price - minPrice) * yScale;

          if (i === 0) {
            ctx.moveTo(x, y);
          } else {
            ctx.lineTo(x, y);
          }
        }

        // 根据价格设置颜色
        const lastPrice = points[points.length - 1].price;
        ctx.strokeStyle = lastPrice >= preClosePrice ? "#d81e06" : "#1aad19";
        ctx.stroke();
      }
    }, [visible, points, preClosePrice]);

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
      cursor: "pointer",
    };

    const canvasStyle: React.CSSProperties = {
      width: "100%",
      height: "100%",
    };

    return (
      <div style={containerStyle} onDoubleClick={onClose}>
        <canvas ref={canvasRef} style={canvasStyle} />
      </div>
    );
  };
