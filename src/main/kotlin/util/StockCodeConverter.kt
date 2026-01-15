package util

/**
 * 将多种格式的股票代码统一转换为东方财富API要求的格式。
 * @param code 股票代码，支持 "sh600000", "sz000001", "600000.SH", "000001.SZ", "600000" 等格式。
 * @return 东方财富API格式的代码 (例如 "1.600000" 或 "0.000001")。
 */
fun toEastMoneySymbol(code: String): String {
    val lowerCaseCode = code.lowercase()
    if (lowerCaseCode.startsWith("sh")) {
        return "1.${lowerCaseCode.removePrefix("sh")}"
    }
    if (lowerCaseCode.startsWith("sz")) {
        return "0.${lowerCaseCode.removePrefix("sz")}"
    }

    if (code.contains('.')) {
        val parts = code.split('.')
        val market = parts[1].uppercase()
        val stockCode = parts[0]
        return when (market) {
            "SH" -> "1.$stockCode"
            "SZ" -> "0.$stockCode"
            else -> code
        }
    }

    return when {
        code.startsWith("6") -> "1.$code"
        code.startsWith("0") || code.startsWith("3") -> "0.$code"
        else -> code
    }
}

/**
 * 将内部代码（如 600000.SH）转换为新浪行情接口要求的格式（如 sh600000）。
 * @param code 内部标准代码。
 * @return 新浪API格式的代码。
 */
fun toSinaSymbol(code: String): String {
    return if (code.contains('.')) {
        val parts = code.split('.')
        parts[1].lowercase() + parts[0]
    } else {
        code
    }
}
