import { invoke } from "@tauri-apps/api/core";

/**
 * 获取新浪实时行情数据
 * @param codes 逗号分隔的股票代码列表
 */
export async function fetchSinaRealtimeData(codes: string): Promise<string> {
  return await invoke<string>("fetch_sina_realtime_data", { codes });
}

/**
 * 获取东方财富分时图数据
 * @param code 股票代码
 */
export async function fetchEastMoneyTimeShare(code: string): Promise<string> {
  return await invoke<string>("fetch_eastmoney_timeshare", { code });
}
