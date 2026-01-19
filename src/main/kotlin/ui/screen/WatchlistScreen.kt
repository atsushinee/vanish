package ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import util.color
import viewmodel.StockViewModel
import java.awt.Dialog

/**
 * 自选股列表弹窗
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchlistScreen(
    showWatchlistPopup: MutableState<Boolean>,
    stockViewModel: StockViewModel,
    watchlistWindowState: DialogState
) {
    if (showWatchlistPopup.value) {
        DialogWindow(
            onCloseRequest = { showWatchlistPopup.value = false },
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = watchlistWindowState
        ) {
            // 核心修正：采用“组合拳”方案，彻底阻止窗口出现在任务切换器中
            LaunchedEffect(window) {
                window.modalExclusionType = Dialog.ModalExclusionType.APPLICATION_EXCLUDE
                window.focusableWindowState = false
            }

            val listState = rememberLazyListState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = { showWatchlistPopup.value = false }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stockViewModel.lastUpdateTime.value,
                        color = Color.LightGray,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                            .height(watchlistWindowState.size.height)
                    ) {
                        items(stockViewModel.watchlistData) { data ->
                            WatchlistRow(data)
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
}

/**
 * 自选股列表中的单行数据展示
 */
@Composable
private fun WatchlistRow(data: StockData) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 0.dp)) {
        Text(
            text = data.name,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 8.sp,
            textAlign = TextAlign.Start,
            maxLines = 1
        )
        Text(
            text = data.code.removePrefix("sh").removePrefix("sz"),
            modifier = Modifier.weight(1f),
            color = Color.LightGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start
        )
        Text(
            text = "%.2f".format(data.price),
            modifier = Modifier.weight(1.1f),
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        Text(
            text = "%.2f%%".format(data.changePercent),
            modifier = Modifier.weight(1.1f),
            color = data.changePercent.color(),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        Text(
            text = "%.2f%%".format(data.rise),
            modifier = Modifier.weight(1f),
            color = data.rise.color(),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}
