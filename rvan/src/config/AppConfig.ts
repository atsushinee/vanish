/**
 * 应用配置接口
 */
export interface AppConfigType {
  window_x: number;
  window_y: number;
  target_stock: string;
  index_code: string;
  watchlist: string[];
  gemini_api_key: string;
}

/**
 * 默认配置
 */
export const DEFAULT_CONFIG: AppConfigType = {
  window_x: 18.0,
  window_y: 811.0,
  target_stock: "sh600593",
  index_code: "sh000001",
  watchlist: ["sh000001", "sz002413", "sz002639", "sh603273"],
  gemini_api_key: "",
};

/**
 * 从后端加载配置
 */
export async function loadConfig(): Promise<AppConfigType> {
  try {
    const { invoke } = await import("@tauri-apps/api/core");
    return await invoke<AppConfigType>("get_config");
  } catch (error) {
    console.error("加载配置失败:", error);
    return DEFAULT_CONFIG;
  }
}

/**
 * 保存配置到后端
 */
export async function saveConfig(config: AppConfigType): Promise<void> {
  try {
    const { invoke } = await import("@tauri-apps/api/core");
    await invoke("save_config", { config });
  } catch (error) {
    console.error("保存配置失败:", error);
    throw error;
  }
}
