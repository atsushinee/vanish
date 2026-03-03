import { makeAutoObservable, runInAction } from "mobx";
import { TimeSharePoint } from "../data/model/StockData";
import { fetchEastMoneyTimeShare } from "../data/remote/ApiClient";

/**
 * UI 状态类型
 */
export type TimeShareUiState =
  | { type: "loading" }
  | { type: "no-data" }
  | { type: "error"; message: string }
  | { type: "success"; points: TimeSharePoint[]; preClosePrice: number };

/**
 * 分时图 ViewModel
 */
export class TimeShareViewModel {
  private code: string;
  uiState: TimeShareUiState = { type: "loading" };

  constructor(code: string) {
    this.code = code;
    makeAutoObservable(this);
    this.loadTimeShareData();
  }

  /**
   * 加载分时数据
   */
  async loadTimeShareData(): Promise<void> {
    try {
      const response = await fetchEastMoneyTimeShare(this.code);
      const json = JSON.parse(response);

      if (!json.data || !json.data.trends || json.data.trends.length === 0) {
        runInAction(() => {
          this.uiState = { type: "no-data" };
        });
        return;
      }

      const points = this.parseTrends(json.data.trends);
      const preClosePrice = json.data.preClose || 0;

      if (points.length === 0) {
        runInAction(() => {
          this.uiState = { type: "no-data" };
        });
      } else {
        runInAction(() => {
          this.uiState = {
            type: "success",
            points,
            preClosePrice,
          };
        });
      }
    } catch (error) {
      runInAction(() => {
        this.uiState = {
          type: "error",
          message: `数据加载失败：${error}`,
        };
      });
    }
  }

  /**
   * 解析分时数据
   */
  private parseTrends(trends: string[]): TimeSharePoint[] {
    return trends
      .map((trendString: string) => {
        const parts = trendString.split(",");
        try {
          const timeStr = parts[0].split(" ")[1];
          const price = parseFloat(parts[1]);
          return { time: timeStr, price };
        } catch (e) {
          console.warn("解析分时数据点失败:", trendString, e);
          return null;
        }
      })
      .filter((p: TimeSharePoint | null): p is TimeSharePoint => p !== null);
  }

  /**
   * 更新代码并重新加载
   */
  updateCode(code: string): void {
    this.code = code;
    this.uiState = { type: "loading" };
    this.loadTimeShareData();
  }
}
