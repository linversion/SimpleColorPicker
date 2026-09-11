package com.linversion.simplecolorpicker.ui.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linversion.simplecolorpicker.picker.PaletteSwatch

@Composable
fun PaletteSheetContent(swatches: List<PaletteSwatch>) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // M2 ModalBottomSheet 的嵌套滚动协调是「sheet 优先」：向上拖时它外层的
    // ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection 会先把位移喂给
    // sheet 的 AnchoredDraggable，sheet 未完全展开时（半屏态、show 动画中、
    // 色板异步到达导致锚点变化）列表会滚不动或与 sheet 同时移动。
    // 在 pre-scroll 阶段把列表自己能消费的位移直接滚掉并消费，让列表优先；
    // 列表滚到底后的剩余量仍交给 sheet。
    val listFirstScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (source == NestedScrollSource.UserInput && delta < 0f && listState.canScrollForward) {
                    return Offset(0f, listState.dispatchRawDelta(delta))
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "图片色板", fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        if (swatches.isEmpty()) {
            Text(text = "暂无主色", color = Color.Gray, fontSize = 14.sp)
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .nestedScroll(listFirstScroll)
            ) {
                items(swatches) { swatch ->
                    PaletteRow(swatch) {
                        copyHex(context, swatch.hex)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "点击色块复制 Hex", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun PaletteRow(swatch: PaletteSwatch, onClick: () -> Unit) {
    val bg = Color(swatch.rgb or 0xFF000000.toInt())
    val textColor = if (isLight(swatch.rgb)) Color.Black else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = swatch.hex, color = textColor, fontSize = 16.sp)
        Text(text = "复制", color = textColor.copy(alpha = 0.8f), fontSize = 13.sp)
    }
}

private fun isLight(rgb: Int): Boolean {
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    return r * 0.299 + g * 0.587 + b * 0.114 >= 186
}

private fun copyHex(context: Context, hex: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("color", hex))
    Toast.makeText(context, "已复制 $hex", Toast.LENGTH_SHORT).show()
}
