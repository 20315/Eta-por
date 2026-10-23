package io.github.mangi.eta.ui.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

internal enum class ConversationPaneAnchor {
    Closed,
    Open,
    /** 向左拖到底：右侧工作区面板占据主视觉。 */
    Workspace,
}

/** 右侧工作区面板的锚点：Closed = 停在屏外，Open = 完全展开。 */
internal enum class WorkspaceAnchor {
    Closed,
    Open,
}

internal fun conversationPaneSettledValues(
    state: AnchoredDraggableState<ConversationPaneAnchor>,
    isDragging: () -> Boolean,
): Flow<ConversationPaneAnchor> = snapshotFlow {
    val anchor = state.settledValue
    val position = state.anchors.positionOf(anchor)
    if (!isDragging() && !state.isAnimationRunning && abs(state.offset - position) < 0.5f) {
        anchor
    } else {
        null
    }
    // 必须先让 snapshotFlow 观察到运动中的 null，再过滤；回到原锚点也要重新同步。
}.filterNotNull()
