package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.model.ToolAccessUiState
import io.github.mangi.eta.ui.model.ToolToggleItem
import io.github.mangi.eta.ui.model.ToolToggleKind
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 工具与技能面板：一页里放 MCP 服务器和技能的开关。
 *
 * 结构上刻意不写死「MCP 区 + 技能区」的业务含义，而是按 [ToolToggleKind] 分组渲染
 * [ToolToggleItem]；以后加插件、内置工具组，只加枚举和一行分组标题即可。
 * 数量多了之后的搜索、折叠、常用置顶也都在这一层加，不用重做界面。
 */
@Composable
internal fun ToolAccessSheet(
    show: Boolean,
    state: ToolAccessUiState,
    onRefresh: () -> Unit,
    onToggle: (item: ToolToggleItem, enabled: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    // 每次打开都重新拉一次：MCP 列表和技能列表都可能被别处改过。
    androidx.compose.runtime.LaunchedEffect(show) {
        if (show) onRefresh()
    }
    CornerScaleDialog(
        show = show,
        onDismiss = onDismiss,
        heightFraction = 0.66f,
    ) { requestClose ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tool_access_title),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.tool_access_summary,
                        state.mcpEnabled,
                        state.mcpItems.size,
                        state.skillEnabled,
                        state.skillItems.size,
                    ),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    text = stringResource(R.string.memory_sheet_cancel),
                    onClick = requestClose,
                    minWidth = 0.dp,
                    minHeight = 30.dp,
                    insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(),
                )
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val mcpItems = state.items.filter { it.kind == ToolToggleKind.Mcp }
                val skillItems = state.items.filter { it.kind == ToolToggleKind.Skill }
                if (mcpItems.isNotEmpty()) {
                    item(key = "mcp-header") {
                        SectionHeader(stringResource(R.string.tool_access_mcp_section))
                    }
                    items(items = mcpItems, key = { "mcp-${it.id}" }) { item ->
                        ToolToggleRow(item = item, onToggle = onToggle)
                    }
                }
                if (skillItems.isNotEmpty()) {
                    item(key = "skill-header") {
                        SectionHeader(stringResource(R.string.tool_access_skill_section))
                    }
                    items(items = skillItems, key = { "skill-${it.id}" }) { item ->
                        ToolToggleRow(item = item, onToggle = onToggle)
                    }
                }
                if (state.items.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.tool_access_empty),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                item(key = "note") {
                    Text(
                        text = stringResource(R.string.tool_access_effect_note),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToolToggleRow(item: ToolToggleItem, onToggle: (ToolToggleItem, Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        EtaSwitch(
            checked = item.enabled,
            onCheckedChange = { checked -> onToggle(item, checked) },
        )
    }
}
