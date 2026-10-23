package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 对话页内容层的容器。
 *
 * 挂在 [io.github.mangi.eta.ui.app.AgentAppShell] 的 content 上，而不是整个 NavDisplay：
 * 顶栏在 Scaffold 的 topBar 里、不在这棵子树，所以那一行保持不动。
 *
 * 内容不做位移——工作区面板从右侧滑入覆盖，不需要把对话页推走；
 * 早先「左移露出边缘」那版会让人以为那条边能点，实际点不到，所以改成纯覆盖。
 *
 * 滑动手势不在这里：它和侧栏共用 [ConversationSidePaneScaffold] 里那个
 * anchoredDraggable 容器，左右两个方向由同一处分发，避免两套手势互相抢事件。
 */
@Composable
internal fun WorkspaceShiftHost(
    shifted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
    }
}
