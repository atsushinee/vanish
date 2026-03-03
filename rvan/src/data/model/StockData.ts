/**
 * 股票数据模型
 */
export interface StockData {
  code: string;
  name: string;
  price: number;
  changePercent: number;
  rise: number;
  indexPercent: number;
  timestamp: Date;
}

/**
 * 新浪行情原始数据
 */
export interface SinaQuote {
  price: number;
  preClose: number;
}

/**
 * 东方财富分时数据响应
 */
export interface EastMoneyTimeShareResponse {
  data?: TimeShareDataContainer;
}

/**
 * 分时数据容器
 */
export interface TimeShareDataContainer {
  code?: string;
  preClose: number;
  trends: string[];
}

/**
 * 分时图数据点
 */
export interface TimeSharePoint {
  time: string; // HH:mm 格式
  price: number;
}
