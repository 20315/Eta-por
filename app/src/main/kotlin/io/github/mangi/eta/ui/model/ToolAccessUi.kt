package io.github.mangi.eta.ui.model

import androidx.compose.runtime.Immutable

/** 开关的来源。以后再加插件、内置工具组，只加一个枚举值和一个数据源，界面不用改结构。 */
internal enum class ToolToggleKind {
    Mcp,
    Skill,
}

/**
 * 面板里的一行开关。
 *
 * 面板不认具体是 MCP 还是技能，只认这一层；分组靠 [kind]。
 */
@Immutable
internal data class ToolToggleItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val enabled: Boolean,
    val kind: ToolToggleKind,
)

@Immutable
internal data class ToolAccessUiState(
    val items: List<ToolToggleItem> = emptyList(),
) {
    val mcpItems: List<ToolToggleItem> get() = items.filter { it.kind == ToolToggleKind.Mcp }
    val skillItems: List<ToolToggleItem> get() = items.filter { it.kind == ToolToggleKind.Skill }
    val mcpEnabled: Int get() = mcpItems.count { it.enabled }
    val skillEnabled: Int get() = skillItems.count { it.enabled }
}
