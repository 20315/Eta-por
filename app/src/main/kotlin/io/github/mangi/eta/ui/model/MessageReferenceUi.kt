package io.github.mangi.eta.ui.model

import androidx.compose.runtime.Immutable

/**
 * 一条被 @ 引用的历史消息。
 *
 * 只存 id 与展示信息；正文在发送时按 id 回到消息列表里取，
 * 这样即使会话被压缩、消息被编辑，引用也不会指错内容。
 */
@Immutable
data class MessageReferenceUi(
    val messageId: String,
    /** 在消息流里的序号（1 基），只用于 chip 上显示 #3 这种标记。 */
    val sequence: Int,
    val fromUser: Boolean,
    /** chip 与面板里的预览，取正文前 60 字。 */
    val preview: String,
    /** 全文匹配用的正文（截断到 4000 字），不显示。 */
    val searchText: String = "",
)
