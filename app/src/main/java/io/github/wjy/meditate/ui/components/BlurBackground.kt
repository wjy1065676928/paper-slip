package io.github.wjy.meditate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 统一背景模糊组件（双层容器方案）
 *
 * 当 [isOverlayVisible] 为 true 且 [blurEnabled] 为 true 时，
 * 对 [content] 应用平滑的模糊动画，[overlay] 则保持在顶层不受模糊影响。
 *
 * 布局结构：
 * ```
 * Box(fillMaxSize)
 *   ├── Box(fillMaxSize, blur)     ← 层 1：背景（可模糊）
 *   │     └── content
 *   └── Box(fillMaxSize)           ← 层 2：前景（始终清晰）
 *         ├── 半透明遮罩
 *         └── overlay
 * ```
 *
 * 使用 Compose [Modifier.blur]：
 * - Android 12+：通过 RenderEffect 硬件加速
 * - Android 7-11：Compose 自动降级无模糊
 */
@Composable
fun BlurBackground(
    isOverlayVisible: Boolean,
    blurEnabled: Boolean,
    blurIntensity: Float,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val blurRadius by animateFloatAsState(
        targetValue = if (isOverlayVisible && blurEnabled) blurIntensity else 0f,
        animationSpec = tween(300),
        label = "blurRadius"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 层 1：背景（应用硬件模糊）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius.dp)
        ) {
            content()
        }

        // 层 2：前景（始终清晰，在顶层）
        Box(modifier = Modifier.fillMaxSize()) {
            // 半透明遮罩层
            if (isOverlayVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f))
                )
            }
            // 弹窗等叠加内容
            overlay()
        }
    }
}
