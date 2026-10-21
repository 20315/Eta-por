package io.github.mangi.eta.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 从右下角（输入框右上角）缩放出来的通用弹层。
 *
 * 内存面板、工具与技能面板共用这一套：起点很小、先淡入再放大，
 * 关闭时缩回同一角、透明度与缩放同步收工，做到「从哪来回哪去」。
 */
@Composable
internal fun CornerScaleDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    widthFraction: Float = 0.92f,
    heightFraction: Float = 0.62f,
    bottomPadding: Dp = CornerDialogBottomPadding,
    content: @Composable (requestClose: () -> Unit) -> Unit,
) {
    if (!show) return
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(CornerDialogInitialScale) }
    val scrimAlpha = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            launch {
                scrimAlpha.animateTo(0f, tween(CornerDialogExitMs, easing = FastOutLinearInEasing))
            }
            launch {
                cardAlpha.animateTo(0f, tween(CornerDialogExitMs, easing = LinearOutSlowInEasing))
            }
            scale.animateTo(
                CornerDialogInitialScale,
                tween(CornerDialogExitMs, easing = FastOutLinearInEasing),
            )
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = true,
        ),
    ) {
        val density = LocalDensity.current
        val container = LocalWindowInfo.current.containerSize
        val width = with(density) { (container.width * widthFraction).toDp() }
        val height = with(density) { (container.height * heightFraction).toDp() }

        LaunchedEffect(Unit) {
            launch {
                scrimAlpha.animateTo(1f, tween(CornerDialogEnterMs, easing = LinearOutSlowInEasing))
            }
            launch {
                cardAlpha.animateTo(1f, tween(CornerDialogCardFadeInMs, easing = LinearOutSlowInEasing))
            }
            scale.animateTo(1f, tween(CornerDialogEnterMs, easing = LinearOutSlowInEasing))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha.value }
                    .background(Color.Black.copy(alpha = CornerDialogScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { close() },
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = bottomPadding)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = cardAlpha.value
                        transformOrigin = TransformOrigin(1f, 1f)
                    },
            ) {
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .background(MiuixTheme.colorScheme.surface, RoundedCornerShape(24.dp)),
                ) {
                    content { close() }
                }
            }
        }
    }
}

private const val CornerDialogInitialScale = 0.12f
private const val CornerDialogEnterMs = 340
private const val CornerDialogExitMs = 340
private const val CornerDialogCardFadeInMs = 80
private const val CornerDialogScrimAlpha = 0.32f

/** 卡片距屏幕底部留出的高度：大致等于输入框高度，让面板贴着入口那一角。 */
internal val CornerDialogBottomPadding = 104.dp

/** 面板内容区统一高度：由调用方决定，这里只给个默认值参考。 */
internal val CornerDialogDefaultHeightFraction = 0.62f
