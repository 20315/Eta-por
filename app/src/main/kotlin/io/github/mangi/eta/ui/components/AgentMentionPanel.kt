package io.github.mangi.eta.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.model.MessageReferenceUi
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * `@` 引用面板：列出可引用的历史消息，按关键词过滤。
 *
 * 与命令面板同一块位置、同一套视觉，区别是这里选出来的是"引用"而不是"执行"。
 */
@Composable
internal fun AgentMentionPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    candidates: List<MessageReferenceUi>,
    selectedIds: Set<String>,
    onPick: (MessageReferenceUi) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = MiuixTheme.textStyles.footnote1.fontSize,
            ),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_mention_search_hint),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                inner()
            },
        )
            Text(
                text = "×",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
            )
        }
        Spacer(Modifier.padding(top = 6.dp))
        Column(
            modifier = Modifier
                .heightIn(max = MentionPanelMaxHeight)
                .verticalScroll(rememberScrollState()),
        ) {
        candidates.forEach { candidate ->
            val selected = candidate.messageId in selectedIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !selected) { onPick(candidate) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "#" + candidate.sequence,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(
                        if (candidate.fromUser) R.string.chat_mention_role_user else R.string.chat_mention_role_agent,
                    ),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = if (selected) stringResource(R.string.chat_mention_selected) else candidate.preview,
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (selected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (candidates.isEmpty()) {
            Text(
                text = stringResource(R.string.chat_mention_empty),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        }
    }
}

/**
 * 面板最高 168dp（约 4 行）：再高就会把输入卡片挤出键盘上方的可用区域，
 * 表现为输入框只剩顶上一行。超出部分在这里滚动。
 */
private val MentionPanelMaxHeight = 168.dp
