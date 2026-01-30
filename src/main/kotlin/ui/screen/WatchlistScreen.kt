package ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
                            // 核心修改：为每一行数据绑定点击事件
                            WatchlistRow(
                                data = data,
                                onClick = {
                                    // 1. 调用ViewModel切换主窗口的股票
                                    stockViewModel.switchTargetStock(data.code)
                                    // 2. 关闭自选股弹窗
                                    showWatchlistPopup.value = false
                                }
                            )
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WatchlistRow(data: StockData, onClick: () -> Unit) {
    // 1. 创建一个状态来追踪鼠标是否悬停在当前行上
    var isHovered by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 0.dp)
            // 2. 根据 isHovered 状态动态改变背景色
            //    原理: 当 isHovered 为 true 时，应用一个半透明的灰色背景，否则背景透明。
            .background(if (isHovered) Color.Gray.copy(alpha = 0.3f) else Color.Transparent)
            // 3. 添加点击事件处理器
            //    原理: clickable 修饰符使整个 Row 区域都可以响应点击，并执行传入的 onClick lambda。
            .clickable { onClick() }
            // 4. 使用 onPointerEvent 监听鼠标的进入和退出事件
            //    原理: PointerEventType.Enter 事件在鼠标光标进入组件区域时触发，我们将 isHovered 设为 true。
            //          PointerEventType.Exit 事件在鼠标光标离开时触发，我们将 isHovered 设为 false。
            //          这是一种实现Hover效果的高效方式。
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .fillMaxWidth()
    ) {
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
