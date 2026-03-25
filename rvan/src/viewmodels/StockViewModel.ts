import { makeAutoObservable, runInAction } from "mobx";
import { StockData } from "../data/model/StockData";
import { loadConfig, saveConfig } from "../config/AppConfig";
import { fetchSinaRealtimeData } from "../data/remote/ApiClient";

/**
 * 股票 ViewModel - 管理股票数据和自选股
 */
export class StockViewModel {
  // 当前股票数据
  stockData: StockData | null = null;

  // 历史数据
  history: StockData[] = [];

  // 自选股代码列表
  watchlistCodes: string[] = [];

  // 自选股实时数据
  watchlistData: StockData[] = [];

  // 最后更新时间
  lastUpdateTime: string = "";

  // 上次价格（用于计算涨速）
  private lastPrice: number = 0;

  // 自选股上次价格映射
  private watchlistLastPrices: Map<string, number> = new Map();

  // 监控定时器
  private monitorInterval: ReturnType<typeof setInterval> | null = null;
  private watchlistInterval: ReturnType<typeof setInterval> | null = null;

  constructor() {
    makeAutoObservable(this);
    this.init();
  }

  /**
   * 初始化
   */
  async init(): Promise<void> {
    const config = await loadConfig();
    runInAction(() => {
      this.watchlistCodes = [...config.watchlist];
    });
    this.startRealtimeMonitor();
  }

  /**
   * 启动实时监控
   */
  startRealtimeMonitor(): void {
    if (this.monitorInterval) {
      clearInterval(this.monitorInterval);
    }

    this.fetchRealtimeData();
    this.monitorInterval = setInterval(() => {
      this.fetchRealtimeData();
    }, 1000);
  }

  /**
   * 停止实时监控
   */
  stopRealtimeMonitor(): void {
    if (this.monitorInterval) {
      clearInterval(this.monitorInterval);
      this.monitorInterval = null;
    }
  }

  /**
   * 启动自选股监控
   */
  startWatchlistMonitor(): void {
    if (this.watchlistInterval) {
      clearInterval(this.watchlistInterval);
    }

    this.fetchWatchlistData();
    this.watchlistInterval = setInterval(() => {
      this.fetchWatchlistData();
    }, 1000);
  }

  /**
   * 停止自选股监控
   */
  stopWatchlistMonitor(): void {
    if (this.watchlistInterval) {
      clearInterval(this.watchlistInterval);
      this.watchlistInterval = null;
    }
    runInAction(() => {
      this.watchlistData = [];
      this.watchlistLastPrices.clear();
      this.lastUpdateTime = "";
    });
  }

  /**
   * 获取实时数据
   */
  private async fetchRealtimeData(): Promise<void> {
    try {
      const config = await loadConfig();
      const targetStock = config.target_stock;
      const indexCode = config.index_code;

      const codes = [targetStock, indexCode].join(",");
      const rawResponse = await fetchSinaRealtimeData(codes);

      const quotesMap = this.parseSinaResponse(rawResponse);

      const stockQuote = quotesMap.get(targetStock);
      const indexQuote = quotesMap.get(indexCode);

      if (stockQuote && indexQuote) {
        const { price: price1, preClose: preClose1 } = stockQuote;
        const { price: price2, preClose: preClose2 } = indexQuote;

        const changePct = preClose1 > 0 ? (price1 / preClose1 - 1) * 100 : 0;
        const rise =
          this.lastPrice > 0 ? (price1 - this.lastPrice) / this.lastPrice * 100 : 0;
        this.lastPrice = price1;

        const indexPct = preClose2 > 0 ? (price2 / preClose2 - 1) * 100 : 0;

        const newStockData: StockData = {
          code: targetStock,
          name: "",
          price: price1,
          changePercent: changePct,
          rise,
          indexPercent: indexPct,
          timestamp: new Date(),
        };

        runInAction(() => {
          this.stockData = newStockData;
          this.history.push(newStockData);
          if (this.history.length > 500) {
            this.history.shift();
          }
        });
      }
    } catch (error) {
      console.error("获取实时数据失败:", error);
    }
  }

  /**
   * 获取自选股数据
   */
  private async fetchWatchlistData(): Promise<void> {
    if (this.watchlistCodes.length === 0) {
      runInAction(() => {
        this.watchlistData = [];
      });
      return;
    }

    try {
      const codes = this.watchlistCodes.join(",");
      const rawResponse = await fetchSinaRealtimeData(codes);

      const newDataMap = this.parseSinaResponseForWatchlist(rawResponse);

      // 按照 watchlistCodes 的顺序构建列表
      const updatedList: StockData[] = [];
      for (const code of this.watchlistCodes) {
        const data = newDataMap.get(code);
        if (data) {
          updatedList.push(data);
        }
      }

      runInAction(() => {
        this.watchlistData = updatedList;
        const now = new Date();
        this.lastUpdateTime = now.toLocaleString("zh-CN", {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        });
      });
    } catch (error) {
      console.error("获取自选股数据失败:", error);
    }
  }

  /**
   * 解析新浪响应（单只股票）
   */
  private parseSinaResponse(response: string): Map<string, { price: number; preClose: number }> {
    const result = new Map<string, { price: number; preClose: number }>();

    const lines = response.split("\n").filter((line) => line.trim());
    for (const line of lines) {
      try {
        const code = line.substring(line.indexOf("var hq_str_") + 11, line.indexOf("="));
        const dataString = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));

        if (!dataString) continue;

        const parts = dataString.split(",");
        if (parts.length < 4) continue;

        const price = parseFloat(parts[3]) || 0;
        const preClose = parseFloat(parts[2]) || 0;

        if (price > 0 && preClose > 0) {
          result.set(code, { price, preClose });
        }
      } catch (e) {
        console.warn("解析行数据失败:", line, e);
      }
    }

    return result;
  }

  /**
   * 解析新浪响应（自选股列表）
   */
  private parseSinaResponseForWatchlist(response: string): Map<string, StockData> {
    const result = new Map<string, StockData>();

    const lines = response.split("\n").filter((line) => line.trim());
    for (const line of lines) {
      try {
        const code = line.substring(line.indexOf("var hq_str_") + 11, line.indexOf("="));
        const dataString = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));

        if (!dataString) continue;

        const parts = dataString.split(",");
        if (parts.length < 4) continue;

        const name = parts[0];
        const price = parseFloat(parts[3]) || 0;
        const preClose = parseFloat(parts[2]) || 0;

        if (price === 0 || preClose === 0) continue;

        const changePct = (price / preClose - 1) * 100;

        const lastPrice = this.watchlistLastPrices.get(code) || 0;
        const rise = lastPrice > 0 ? (price - lastPrice) / lastPrice * 100 : 0;
        this.watchlistLastPrices.set(code, price);

        result.set(code, {
          code,
          name,
          price,
          changePercent: changePct,
          rise,
          indexPercent: 0,
          timestamp: new Date(),
        });
      } catch (e) {
        console.warn("解析行数据失败:", line, e);
      }
    }

    return result;
  }

  /**
   * 判断是否为交易时间
   */
  private isTradingTime(): boolean {
    const now = new Date();
    const hours = now.getHours();
    const minutes = now.getMinutes();
    const totalMinutes = hours * 60 + minutes;

    // 上午 9:25-11:31
    const amStart = 9 * 60 + 15;
    const amEnd = 11 * 60 + 31;
    // 下午 13:00-15:01
    const pmStart = 13 * 60;
    const pmEnd = 15 * 60 + 1;

    return (totalMinutes >= amStart && totalMinutes <= amEnd) ||
      (totalMinutes >= pmStart && totalMinutes <= pmEnd);
  }

  /**
   * 添加自选股
   */
  async addWatchlistCode(code: string): Promise<void> {
    if (!code.trim() || this.watchlistCodes.includes(code)) {
      return;
    }

    runInAction(() => {
      this.watchlistCodes.push(code);
    });

    const config = await loadConfig();
    config.watchlist = [...this.watchlistCodes];
    await saveConfig(config);

    this.fetchWatchlistData();
  }

  /**
   * 移除自选股
   */
  async removeWatchlistCode(code: string): Promise<void> {
    const index = this.watchlistCodes.indexOf(code);
    if (index === -1) return;

    runInAction(() => {
      this.watchlistCodes.splice(index, 1);
    });

    const config = await loadConfig();
    config.watchlist = [...this.watchlistCodes];
    await saveConfig(config);

    this.fetchWatchlistData();
  }

  /**
   * 切换目标股票
   */
  async switchTargetStock(newCode: string): Promise<void> {
    const config = await loadConfig();
    config.target_stock = newCode;
    await saveConfig(config);

    this.lastPrice = 0;
    this.fetchRealtimeData();
  }

  /**
   * 重新排序自选股
   */
  async reorderWatchlist(from: number, to: number): Promise<void> {
    if (
      from < 0 ||
      from >= this.watchlistCodes.length ||
      to < 0 ||
      to >= this.watchlistCodes.length ||
      from === to
    ) {
      return;
    }

    // 交换代码
    const codes = [...this.watchlistCodes];
    const temp = codes[from];
    codes[from] = codes[to];
    codes[to] = temp;

    // 交换数据
    const data = [...this.watchlistData];
    if (from < data.length && to < data.length) {
      const tempData = data[from];
      data[from] = data[to];
      data[to] = tempData;
    }

    runInAction(() => {
      this.watchlistCodes = codes;
      this.watchlistData = data;
    });

    const config = await loadConfig();
    config.watchlist = codes;
    await saveConfig(config);
  }

  /**
   * 刷新数据
   */
  refresh(): void {
    this.fetchRealtimeData();
  }
}
