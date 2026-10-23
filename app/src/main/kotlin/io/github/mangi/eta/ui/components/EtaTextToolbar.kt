package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.mangi.eta.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 文本选择菜单的模型：Compose 只会把当前可用的动作传进来（输入框有粘贴/剪切，静态文本只有复制/全选）。
 * 系统自带的浮动工具栏在本机部分路径下只回「全选」，所以这里自己画一条，保证复制始终可用。
 */
internal data class EtaTextSelectionMenu(
    val anchor: Rect,
    val onCopy: (() -> Unit)? = null,
    val onPaste: (() -> Unit)? = null,
    val onCut: (() -> Unit)? = null,
    val onSelectAll: (() -> Unit)? = null,
)

private class EtaTextToolbarState {
    var menu by mutableStateOf<EtaTextSelectionMenu?>(null)
}

private class EtaTextToolbar(private val state: EtaTextToolbarState) : TextToolbar {
    override val status: TextToolbarStatus
        get() = if (state.menu != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun hide() {
        state.menu = null
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        state.menu = EtaTextSelectionMenu(
            anchor = rect,
            onCopy = onCopyRequested,
            onPaste = onPasteRequested,
            onCut = onCutRequested,
            onSelectAll = onSelectAllRequested,
        )
    }
}

/** 装在 App 根部：把文本长按菜单换成自带的一条（复制 / 剪切 / 粘贴 / 全选）。 */
@Composable
internal fun ProvideEtaTextToolbar(content: @Composable () -> Unit) {
    val state = remember { EtaTextToolbarState() }
    val toolbar = remember(state) { EtaTextToolbar(state) }
    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        content()
        state.menu?.let { menu ->
            EtaTextSelectionMenuPopup(menu) { state.menu = null }
        }
    }
}

@Composable
private fun EtaTextSelectionMenuPopup(menu: EtaTextSelectionMenu, onDismiss: () -> Unit) {
    // 用 Dialog（独立窗口）承载菜单：Popup 依附宿主 view tree，在 Dialog 内长按会触发
    // "layouts are not part of the same hierarchy" 的跨窗口崩溃。
    val density = LocalDensity.current
    val menuHeightPx = with(density) { 44.dp.toPx() }
    val offsetX = with(density) { menu.anchor.left.toInt() }.coerceAtLeast(0)
    val offsetY = (with(density) { menu.anchor.top.toInt() } - menuHeightPx.toInt()).coerceAtLeast(0)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 透明点击层：点外面收起菜单
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null) { onDismiss() },
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { androidx.compose.ui.unit.IntOffset(offsetX, offsetY) }
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                menu.onCut?.let { action ->
                    SelectionMenuItem(stringResource(R.string.text_action_cut)) {
                        action(); onDismiss()
                    }
                }
                menu.onCopy?.let { action ->
                    SelectionMenuItem(stringResource(R.string.ui_copy_4edd1d)) {
                        action(); onDismiss()
                    }
                }
                menu.onPaste?.let { action ->
                    SelectionMenuItem(stringResource(R.string.terminal_key_paste)) {
                        action(); onDismiss()
                    }
                }
                menu.onSelectAll?.let { action ->
                    SelectionMenuItem(stringResource(R.string.page_select_all_3e44b2)) {
                        action(); onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionMenuItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MiuixTheme.textStyles.button,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
