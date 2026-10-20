package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.model.AgentModelOptionUi
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏状态胶囊组：内存占用 + 工具档。
 *
 * 上下文用量不在这里——输入栏右侧已经有圆环按钮（同一份数字），顶栏再放一遍是重复，
 * 省下来的宽度正好留给工具档，工具档才有机会显示全称。
 *
 * 宽度仍然自己算：顶栏 actions 行给不给可信约束不一定，按「屏宽 − 其余顶栏控件占位」兜底，
 * 再与父级约束取较小值，预算内逐级压缩文字，所以胶囊组永远不会超出可用宽度。
 */

/** 顶栏里除胶囊组以外必须占住的位置：导航图标、标题、朗读、新建、⋯ 与两侧留白。 */
private val TopBarCapsuleReserve = 216.dp

/** 父级给不出可信约束时的兜底下限。 */
private val TopBarCapsuleFloor = 56.dp

/** 各胶囊的估算占位；估偏大只会让胶囊早一点变短标。 */
private val FullProfileCapsuleWidth = 100.dp
private val CompactProfileCapsuleWidth = 100.dp

/** 顶栏所有胶囊统一高度：模式开关放得下图标，内存数字也不会挤成一条。 */
private val StatusCapsuleHeight = 33.dp
private val CapsuleSpacing = 4.dp

/**
 * 预算档位。工具档优先：它是能点的入口，只要放得下就写全称；
 * 内存是只读诊断信息，宽度不够时先变短标、再整枚收起。
 */
private val ProfileFullBudget = FullProfileCapsuleWidth

@Composable
internal fun ConversationStatusCapsules(
    model: AgentModelOptionUi?,
    modifier: Modifier = Modifier,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    BoxWithConstraints(modifier) {
        val budget = minOf(maxWidth, (screenWidth - TopBarCapsuleReserve).coerceAtLeast(TopBarCapsuleFloor))
        val showProfile = budget >= CompactProfileCapsuleWidth
        val profileFull = budget >= ProfileFullBudget
        Row(
            horizontalArrangement = Arrangement.spacedBy(CapsuleSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 内存百分比搬到输入框右上角了，顶栏这里只留模式开关。
            if (showProfile) LocalProfileTopBarCapsule(model, compact = !profileFull)
        }
    }
}

@Composable
internal fun StatusCapsule(text: String, modifier: Modifier, description: String = text) {
    Box(
        modifier = modifier
            .height(StatusCapsuleHeight)
            .background(MiuixTheme.colorScheme.surfaceContainer, CircleShape)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
