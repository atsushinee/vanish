import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.model.StockData

@Composable
fun StockInfo(stockData: StockData?) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stockData?.price?.let { "%.2f".format(it) } ?: "--.--",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stockData?.changePercent?.let { "%.2f%%".format(it) } ?: "--.--%",
            color = when {
                stockData == null -> Color.White
                stockData.changePercent >= 0 -> Color(0xFFd81e06)
                else -> Color(0xFF1aad19)
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
//        Text(
//            text = stockData?.rise?.let { "%.2f%%".format(it) } ?: "--.--%",
//            color = when {
//                stockData == null -> Color.White
//                stockData.rise > 0.1 -> Color(0xFFd81e06)
//                stockData.rise < -0.1 -> Color(0xFF1aad19)
//                else -> Color.White
//            },
//            fontSize = 8.sp,
//            fontFamily = FontFamily.Monospace
//        )
        Text(
            text = stockData?.indexPercent?.let { "%.2f%%".format(it) } ?: "--.--%",
            color = when {
                stockData == null -> Color.White
                stockData.indexPercent >= 0 -> Color(0xFFd81e06)
                else -> Color(0xFF1aad19)
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

