package io.github.mangi.eta.ui.model

import androidx.compose.runtime.Immutable

/** 命令来源：内置（固定逻辑）与技能（注入 SKILL.md 后交给模型）。 */
internal enum class AgentCommandKind {
    BuiltIn,
    Skill,
}

@Immutable
internal data class AgentCommand(
    /** 不带斜杠的命令名，例如 compact。 */
    val name: String,
    val summary: String,
    val aliases: List<String> = emptyList(),
    /** 参数提示，例如 "[模型名]"。有参数的命令被选中后不立即执行。 */
    val argHint: String? = null,
    val kind: AgentCommandKind = AgentCommandKind.BuiltIn,
    /** 有副作用、执行前要用户确认一下的命令。 */
    val requiresConfirmation: Boolean = false,
    /** 技能命令对应的技能 id；内置命令为 null。 */
    val skillId: String? = null,
) {
    fun matches(token: String): Boolean =
        name.equals(token, ignoreCase = true) ||
            aliases.any { alias -> alias.equals(token, ignoreCase = true) }
}

/**
 * 一次输入的解析结果。
 *
 * - [Text]：普通消息，含 `//` 转义后的字面量斜杠；
 * - [Run]：命中的命令 + 余下全部文本作为参数；
 * - [Unknown]：看起来是命令但没找到，交给界面问一句「按原样发送？」。
 */
@Immutable
internal sealed interface AgentCommandInput {
    data class Text(val text: String) : AgentCommandInput

    data class Run(val command: AgentCommand, val args: String) : AgentCommandInput

    data class Unknown(val name: String, val raw: String) : AgentCommandInput
}

/**
 * 解析规则与主流 Agent 保持一致：命令只在消息开头被识别，命令名后面的一切都是参数。
 *
 * - `//` 是转义：`//foo` 发送为 `/foo`；
 * - 单个 `/` 或不带空白的全角 `／` 当作普通文本，中文输入法误触也不会吃掉消息；
 * - 大小写不敏感，别名同样命中。
 */
internal fun parseAgentCommand(
    raw: String,
    commands: List<AgentCommand>,
): AgentCommandInput {
    if (!raw.startsWith("/")) return AgentCommandInput.Text(raw)
    if (raw.startsWith("//")) return AgentCommandInput.Text(raw.substring(1))
    val body = raw.substring(1)
    val name = body.takeWhile { !it.isWhitespace() }
    if (name.isBlank()) return AgentCommandInput.Text(raw)
    val args = body.drop(name.length).trim()
    val command = commands.firstOrNull { it.matches(name) }
        ?: return AgentCommandInput.Unknown(name, raw)
    return AgentCommandInput.Run(command, args)
}

/**
 * 命令面板的候选列表：输入 `/<前缀>` 时按名字前缀过滤，其次是包含匹配。
 * 已经输入空格（开始写参数）后返回空列表，面板随之收起。
 */
internal fun filterAgentCommands(
    raw: String,
    commands: List<AgentCommand>,
): List<AgentCommand> {
    if (!raw.startsWith("/") || raw.startsWith("//")) return emptyList()
    val token = raw.substring(1)
    if (token.any { it.isWhitespace() }) return emptyList()
    if (token.isEmpty()) return commands.sortedBy { it.name.lowercase() }
    val prefix = commands.filter { command ->
        command.name.startsWith(token, ignoreCase = true) ||
            command.aliases.any { alias -> alias.startsWith(token, ignoreCase = true) }
    }
    val contains = commands.filter { command ->
        command !in prefix && (
            command.name.contains(token, ignoreCase = true) ||
                command.aliases.any { alias -> alias.contains(token, ignoreCase = true) }
            )
    }
    return prefix.sortedBy { it.name.lowercase() } + contains.sortedBy { it.name.lowercase() }
}
