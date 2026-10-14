package io.github.mangi.eta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 可选中的胶囊标签：用于筛选与少量互斥/多选项，选中态只用主题色浅底与文字表达。 */
@Composable
internal fun EtaChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val container by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MiuixTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 160),
        label = "choice_chip_container",
    )
    val content by animateColorAsState(
        targetValue = when {
            !enabled -> MiuixTheme.colorScheme.disabledOnSecondaryVariant
            selected -> MiuixTheme.colorScheme.primary
            else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
        },
        animationSpec = tween(durationMillis = 160),
        label = "choice_chip_content",
    )
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = content,
        maxLines = 1,
        modifier = modifier
            .semantics { this.selected = selected }
            .squircleSurface(color = container, cornerRadius = 16.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
