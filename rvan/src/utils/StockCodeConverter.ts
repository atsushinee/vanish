/**
 * 将股票代码转换为新浪 API 格式
 * @param code 股票代码，如 "sh600000" 或 "600000.SH"
 */
export function toSinaSymbol(code: string): string {
  if (code.includes(".")) {
    const parts = code.split(".");
    return parts[1].toLowerCase() + parts[0];
  }
  return code;
}

/**
 * 将股票代码转换为东方财富 API 格式
 * @param code 股票代码，如 "sh600000" 或 "600000.SH"
 */
export function toEastMoneySymbol(code: string): string {
  const lowerCode = code.toLowerCase();
  if (lowerCode.startsWith("sh")) {
    return `1.${lowerCode.replace("sh", "")}`;
  }
  if (lowerCode.startsWith("sz")) {
    return `0.${lowerCode.replace("sz", "")}`;
  }

  if (code.includes(".")) {
    const parts = code.split(".");
    const market = parts[1].toUpperCase();
    const stockCode = parts[0];
    switch (market) {
      case "SH":
        return `1.${stockCode}`;
      case "SZ":
        return `0.${stockCode}`;
      default:
        return code;
    }
  }

  if (code.startsWith("6")) {
    return `1.${code}`;
  } else if (code.startsWith("0") || code.startsWith("3")) {
    return `0.${code}`;
  }
  return code;
}
