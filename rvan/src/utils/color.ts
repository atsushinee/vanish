/**
 * 根据数值返回对应的颜色
 * @param value 数值
 * @param upColor 上涨颜色（红色）
 * @param downColor 下跌颜色（绿色）
 * @param flatColor 平盘颜色（白色）
 * @param epsilon 判断为 0 的误差范围
 */
export function getColor(
  value: number | null | undefined,
  upColor: string = "#d81e06",
  downColor: string = "#1aad19",
  flatColor: string = "#FFFFFF",
  epsilon: number = 1e-6
): string {
  if (value === null || value === undefined) {
    return flatColor;
  }
  if (value > epsilon) {
    return upColor;
  }
  if (value < -epsilon) {
    return downColor;
  }
  return flatColor;
}

/**
 * 格式化价格为字符串
 */
export function formatPrice(price: number): string {
  return price.toFixed(2);
}

/**
 * 格式化百分比为字符串
 */
export function formatPercent(value: number): string {
  return `${value.toFixed(2)}%`;
}
