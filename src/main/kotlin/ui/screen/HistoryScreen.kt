package ui.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import data.model.StockData
import util.color
import viewmodel.StockViewModel
import java.time.format.DateTimeFormatter

/**
 * 历史记录弹窗
 * @param showHistoryPopup 控制弹窗可见性的状态
 * @param stockViewModel 提供历史数据的 ViewModel
 * @param historyWindowState 弹窗的状态，用于控制位置和大小
 */
@Composable
fun HistoryScreen(
    showHistoryPopup: MutableState<Boolean>,
    stockViewModel: StockViewModel,
    historyWindowState: DialogState
) {
    // 仅当 showHistoryPopup 为 true 时显示弹窗
    if (showHistoryPopup.value) {
        DialogWindow(
            onCloseRequest = { showHistoryPopup.value = false },
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = historyWindowState
        ) {
            // 记住列表滚动状态
            val listState = rememberLazyListState()
            // 记住上一次列表的大小，用于判断是否需要滚动
            var oldSize by remember { mutableStateOf(0) }

            // 当历史记录数量变化时，自动滚动到最新的条目
            LaunchedEffect(stockViewModel.history.size) {
                val newSize = stockViewModel.history.size
                if (newSize == 0) return@LaunchedEffect
                val layoutInfo = listState.layoutInfo
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                // 判断是否应该滚动：如果是第一次加载，或者用户停留在列表末尾
                val shouldScroll = lastVisibleItemIndex == null || (oldSize > 0 && lastVisibleItemIndex >= oldSize - 1)
                if (shouldScroll) {
                    // 平滑滚动到列表末尾
                    listState.animateScrollToItem(newSize - 1)
                }
                oldSize = newSize
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    // 添加点击手势，点击面板任何地方都会关闭它
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            showHistoryPopup.value = false
                            println("历史面板被点击，已关闭")
                        })
                    }
            ) {
                // 使用 LazyColumn 高效显示列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                ) {
                    items(stockViewModel.history) { data ->
                        HistoryRow(data)
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

/**
 * 历史记录中的单行数据展示
 * @param data 单条股票数据
 */
@Composable
private fun HistoryRow(data: StockData) {
    // 记住时间格式化器以提高性能
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = data.timestamp.format(timeFormatter),
            modifier = Modifier.width(50.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Text(
            text = "%.2f".format(data.price),
            modifier = Modifier.width(46.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        Text(
            text = "%.2f%%".format(data.changePercent),
            modifier = Modifier.width(46.dp),
            color = data.changePercent.color(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        Text(
            text = "%.2f%%".format(data.rise),
            modifier = Modifier.width(41.dp),
            color = data.rise.color(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        Text(
            text = "%.2f%%".format(data.indexPercent),
            modifier = Modifier.width(41.dp),
            color = data.indexPercent.color(),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}
