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
import java.awt.Dialog
import java.time.format.DateTimeFormatter

/**
 * 历史记录弹窗
 */
@Composable
fun HistoryScreen(
    showHistoryPopup: MutableState<Boolean>,
    stockViewModel: StockViewModel,
    historyWindowState: DialogState
) {
    if (showHistoryPopup.value) {
        DialogWindow(
            onCloseRequest = { showHistoryPopup.value = false },
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = historyWindowState
        ) {
            // 核心修正：采用“组合拳”方案，彻底阻止窗口出现在任务切换器中
            LaunchedEffect(window) {
                window.modalExclusionType = Dialog.ModalExclusionType.APPLICATION_EXCLUDE
                window.focusableWindowState = false
            }

            val listState = rememberLazyListState()
            var oldSize by remember { mutableStateOf(0) }

            LaunchedEffect(stockViewModel.history.size) {
                val newSize = stockViewModel.history.size
                if (newSize == 0) return@LaunchedEffect
                val layoutInfo = listState.layoutInfo
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val shouldScroll = lastVisibleItemIndex == null || (oldSize > 0 && lastVisibleItemIndex >= oldSize - 1)
                if (shouldScroll) {
                    listState.animateScrollToItem(newSize - 1)
                }
                oldSize = newSize
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            showHistoryPopup.value = false
                            println("历史面板被点击，已关闭")
                        })
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                ) {
                    items(stockViewModel.history) { data ->
                        HistoryRow(data)
                    }
                }
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
 */
@Composable
private fun HistoryRow(data: StockData) {
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
