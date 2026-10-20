package io.github.mangi.eta.ui.model

import androidx.compose.runtime.Immutable

/** 会话导航条目对应的说话方，仅用于列表左侧标签。 */
internal enum class ConversationNavRole {
    User,
    Assistant,
    System,
}

/**
 * 会话导航面板的一行。
 *
 * @property turn 该消息所属的轮次（用户第几次提问，从 1 开始）；0 表示提问之前的系统提示。
 * @property detail 目录模式下补一行该轮回复的摘要，方便扫读；搜索模式下为 null。
 */
@Immutable
internal data class ConversationNavEntry(
    val messageId: String,
    val turn: Int,
    val role: ConversationNavRole,
    val text: String,
    val detail: String? = null,
)

/** 目录模式下摘要的长度：手机上一行放不下整段，太长反而看不出区别。 */
private const val NavOutlineSnippetChars = 120

/** 搜索模式下命中片段的长度：命中点前后各留一点上下文。 */
private const val NavSearchSnippetChars = 140

/**
 * 构造会话导航条目。
 *
 * - [query] 为空时只列每轮的用户提问，干的就是「目录」这件事；
 * - [query] 非空时列出所有命中消息（与侧栏搜索同一套匹配口径），点一条就跳过去。
 */
internal fun AgentChatHomeUiState.navEntries(
    query: String,
    noticeText: (SystemNoticeCode) -> String,
): List<ConversationNavEntry> {
    val trimmed = query.trim()
    val entries = ArrayList<ConversationNavEntry>()
    var turn = 0
    messages.forEachIndexed { index, message ->
        if (message is UserMessageUi) turn += 1
        val texts = message.searchableTexts(noticeText)
        if (trimmed.isEmpty()) {
            if (message is UserMessageUi) {
                entries += ConversationNavEntry(
                    messageId = message.id,
                    turn = turn,
                    role = ConversationNavRole.User,
                    text = navSnippet(texts.firstOrNull().orEmpty(), query = null, limit = NavOutlineSnippetChars),
                    detail = navReplyPreview(messages, index, noticeText),
                )
            }
            return@forEachIndexed
        }
        val hit = texts.firstOrNull { text -> text.contains(trimmed, ignoreCase = true) }
            ?: return@forEachIndexed
        entries += ConversationNavEntry(
            messageId = message.id,
            turn = turn,
            role = message.navRole(),
            text = navSnippet(hit, query = trimmed, limit = NavSearchSnippetChars),
        )
    }
    return entries
}

/** 用户提问下面的那一行：这一轮助手最先说了什么。 */
private fun navReplyPreview(
    messages: List<AgentChatMessageUi>,
    fromIndex: Int,
    noticeText: (SystemNoticeCode) -> String,
): String? {
    for (index in fromIndex + 1 until messages.size) {
        val next = messages[index]
        if (next is UserMessageUi) return null
        if (next is AgentMessageUi) {
            val preview = navSnippet(next.content, query = null, limit = NavOutlineSnippetChars)
            if (preview.isNotBlank()) return preview
        }
        if (next is SystemNoticeMessageUi) {
            val preview = navSnippet(noticeText(next.code), query = null, limit = NavOutlineSnippetChars)
            if (preview.isNotBlank()) return preview
        }
    }
    return null
}

private fun AgentChatMessageUi.navRole(): ConversationNavRole = when (this) {
    is UserMessageUi -> ConversationNavRole.User
    is SystemNoticeMessageUi -> ConversationNavRole.System
    else -> ConversationNavRole.Assistant
}

/** 压平换行与多余空白，命中时以命中点为中心截一段。 */
private fun navSnippet(text: String, query: String?, limit: Int): String {
    val flat = text.replace('\n', ' ').replace("\r", " ").replace(Regex("\\s{2,}"), " ").trim()
    if (flat.isEmpty()) return ""
    val needle = query?.takeIf { it.isNotBlank() } ?: return flat.take(limit)
    val hit = flat.indexOf(needle, ignoreCase = true)
    if (hit < 0) return flat.take(limit)
    val start = (hit - limit / 4).coerceAtLeast(0)
    val end = (hit + needle.length + limit / 2).coerceAtMost(flat.length)
    val head = if (start > 0) "…" else ""
    val tail = if (end < flat.length) "…" else ""
    return head + flat.substring(start, end) + tail
}
