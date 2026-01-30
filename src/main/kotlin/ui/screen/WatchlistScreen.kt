package ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import data.model.StockData
import util.color
import viewmodel.StockViewModel
import kotlin.math.abs

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
        var showAddBar by remember { mutableStateOf(false) }

        DialogWindow(
            onCloseRequest = { showWatchlistPopup.value = false },
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = watchlistWindowState
        ) {
            // 动态切换窗口的可聚焦状态，以便输入框可以工作
            LaunchedEffect(window, showAddBar) {
                window.setFocusableWindowState(showAddBar)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                // 顶部操作栏
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧的更新时间
                    Text(
                        text = stockViewModel.lastUpdateTime.value,
                        color = Color.LightGray,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    // 使用带 weight 的 Spacer 将右侧内容推到最右边
                    Spacer(Modifier.weight(1f))

                    // 右侧根据状态决定显示 "+" 按钮还是输入栏
                    if (showAddBar) {
                        AddStockBar(
                            onConfirm = { code ->
                                stockViewModel.addWatchlistCode(code)
                                showAddBar = false
                            },
                            onDismiss = { showAddBar = false }
                        )
                    } else {
                        // 核心修复：使用 Icon 替换 Text
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加",
                            tint = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showAddBar = true }
                                .padding(0.dp) // 为图标提供一些内边距，方便点击
                                .size(8.dp) // 控制图标大小
                        )
                    }
                }

                // 可拖拽排序的自选股列表
                DraggableWatchlist(stockViewModel, showWatchlistPopup)
            }
        }
    }
}

/**
 * 极简风格的顶部股票代码输入栏，使用 Material Design 图标
 */
@Composable
private fun AddStockBar(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    val textStyle = TextStyle(color = Color.White, fontSize = 6.sp, lineHeight = 6.sp, fontWeight = FontWeight.Bold)
    val focusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
//            .height(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 0.dp)
    ) {
        BasicTextField(
            value = code,
            // 限制输入长度不超过8位
            onValueChange = { if (it.length <= 8) code = it },
            // 使用固定宽度替换 weight，以缩短输入框
            modifier = Modifier.width(35.dp).padding(start = 2.dp).focusRequester(focusRequester),
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (code.isEmpty()) {
                        Text("", style = textStyle.copy(color = Color.Gray))
                    }
                    innerTextField()
                }
            }
        )
        // 使用 Icon 替换 Text
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "确定",
            tint = if (code.isNotBlank()) Color(0xFF1aad19) else Color.Gray,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(8.dp)
                .clickable(enabled = code.isNotBlank()) { onConfirm(code) }
        )
        // 使用 Icon 替换 Text
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "取消",
            tint = Color(0xFFd81e06),
            modifier = Modifier
                .padding(start = 4.dp)
                .size(8.dp)
                .clickable { onDismiss() }
        )
    }

    // 当输入栏显示时，立即请求焦点
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}


@Composable
private fun DraggableWatchlist(
    stockViewModel: StockViewModel,
    showWatchlistPopup: MutableState<Boolean>
) {
    val listState = rememberLazyListState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragAmount by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            itemsIndexed(stockViewModel.watchlistData, key = { _, data -> data.code }) { index, data ->
                val isBeingDragged = index == draggedItemIndex

                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedItemIndex = index
                                },
                                onDragEnd = {
                                    draggedItemIndex?.let { fromIndex ->
                                        val fromItem =
                                            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == fromIndex }
                                        if (fromItem != null) {
                                            val finalDraggedItemCenter =
                                                fromItem.offset + fromItem.size / 2 + dragAmount
                                            val toItem = listState.layoutInfo.visibleItemsInfo
                                                .filterNot { it.index == fromIndex }
                                                .minByOrNull { abs((it.offset + it.size / 2) - finalDraggedItemCenter) }

                                            if (toItem != null) {
                                                stockViewModel.reorderWatchlist(fromIndex, toItem.index)
                                            }
                                        }
                                    }
                                    draggedItemIndex = null
                                    dragAmount = 0f
                                },
                                onDragCancel = {
                                    draggedItemIndex = null
                                    dragAmount = 0f
                                },
                                onDrag = { change, dragDelta ->
                                    change.consume()
                                    dragAmount += dragDelta.y
                                }
                            )
                        }
                        .graphicsLayer {
                            translationY = if (isBeingDragged) dragAmount else 0f
                            shadowElevation = if (isBeingDragged) 8f else 0f
                        }
                        .fillMaxWidth()
                ) {
                    WatchlistRow(
                        data = data,
                        onClick = {
                            if (draggedItemIndex == null) {
                                stockViewModel.switchTargetStock(data.code)
                                showWatchlistPopup.value = false
                            }
                        }
                    )
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = listState)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WatchlistRow(data: StockData, onClick: () -> Unit) {
    var isHovered by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(if (isHovered) Color.Gray.copy(alpha = 0.3f) else Color.Transparent)
            .clickable(onClick = onClick)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .fillMaxWidth()
            .padding(vertical = 0.dp)
    ) {
        Text(data.name, Modifier.weight(1f), Color.White, 8.sp, textAlign = TextAlign.Start, maxLines = 1)
        Text(
            data.code.removePrefix("sh").removePrefix("sz"),
            Modifier.weight(1f),
            Color.LightGray,
            10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start
        )
        Text(
            "%.2f".format(data.price),
            Modifier.weight(1.1f),
            Color.White,
            10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        Text(
            "%.2f%%".format(data.changePercent),
            Modifier.weight(1.1f),
            data.changePercent.color(),
            10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        Text(
            "%.2f%%".format(data.rise),
            Modifier.weight(1f),
            data.rise.color(),
            10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}
