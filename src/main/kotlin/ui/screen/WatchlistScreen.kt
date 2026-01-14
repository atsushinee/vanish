package ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import data.model.StockData
import viewmodel.StockViewModel

/**
 * 自选股列表弹窗
 * @param showWatchlistPopup 控制弹窗可见性的状态
 * @param stockViewModel 提供自选股数据的 ViewModel
 * @param watchlistWindowState 弹窗的状态，用于控制位置和大小
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchlistScreen(
    showWatchlistPopup: MutableState<Boolean>,
    stockViewModel: StockViewModel,
    watchlistWindowState: DialogState
) {
    // 仅当 showWatchlistPopup 为 true 时显示弹窗
    if (showWatchlistPopup.value) {
        DialogWindow(
            onCloseRequest = { showWatchlistPopup.value = false },
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = watchlistWindowState
        ) {
            // 记住列表滚动状态
            val listState = rememberLazyListState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    // 允许双击关闭弹窗
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = { showWatchlistPopup.value = false }
                    )
            ) {
                // 显示最后更新时间
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stockViewModel.lastUpdateTime.value,
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // 使用 LazyColumn 高效显示列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                            .height(watchlistWindowState.size.height) // 根据窗口高度限制列表高度
                    ) {
                        items(stockViewModel.watchlistData) { data ->
                            WatchlistRow(data)
                        }
                    }
                    // 添加垂直滚动条
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                }
            }
        }
    }
}

/**
 * 自选股列表中的单行数据展示
 * @param data 单条股票数据
 */
@Composable
private fun WatchlistRow(data: StockData) {
    // 使用 Row 布局来水平排列各个数据项
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 0.dp)) {
        // 股票名称，使用 weight 实现弹性布局，自动填充可用空间
        Text(
            text = data.name,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 8.sp,
            textAlign = TextAlign.Start,
            maxLines = 1 // 确保名称只显示一行
        )
        // 股票代码，移除 'sh' 或 'sz' 前缀
        Text(
            text = data.code.removePrefix("sh").removePrefix("sz"),
            modifier = Modifier.weight(1f),
            color = Color.LightGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start
        )
        // 当前价格
        Text(
            text = "%.2f".format(data.price),
            modifier = Modifier.weight(1.1f),
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        // 根据涨跌幅的正负决定颜色
        val changeColor = if (data.changePercent >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text(
            text = "%.2f%%".format(data.changePercent),
            modifier = Modifier.weight(1f),
            color = changeColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        // 根据涨速的正负决定颜色
        val riseColor = if (data.rise >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text(
            text = "%.2f%%".format(data.rise),
            modifier = Modifier.weight(1f),
            color = riseColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}
