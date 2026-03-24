package io.github.wjy.meditate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.wjy.meditate.ui.theme.AppStyle
import io.github.wjy.meditate.ui.theme.LocalAppStyle
import io.github.wjy.meditate.ui.theme.LocalCustomColors

@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalCustomColors.current.cardBackground,
    content: @Composable () -> Unit
) {
    val style = LocalAppStyle.current
    val radius = LocalCustomColors.current.cardRadius

    Box(
        modifier = modifier
            // 使用 graphicsLayer 进行裁切，性能优于 .clip()
            .graphicsLayer {
                shape = RoundedCornerShape(radius)
                clip = true
            }
            .background(backgroundColor)
            .padding(if (style == AppStyle.MIUI) 16.dp else 12.dp)
    ) {
        content()
    }
}
