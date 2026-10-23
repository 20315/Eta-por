package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mangi.eta.R
import io.github.mangi.eta.data.db.ConversationCategoryEntity
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分类色条：按顺序取色，和侧栏分类标题的竖条同一套视觉语言。 */
private val CategoryBarColors = listOf(
    Color(0xFF5B8DEF), Color(0xFF3FB98A), Color(0xFFE0A33E), Color(0xFFB96BD8),
    Color(0xFFE2685F), Color(0xFF3FA9C9), Color(0xFF8C8CC7), Color(0xFF9AA23E),
)

internal fun categoryBarColor(index: Int): Color = CategoryBarColors[index % CategoryBarColors.size]

/**
 * 「移动到分类」底部弹层：可滚动列表 + 当前项打勾 + 末尾新建分类。
 * 分类多于 8 个时才出现搜索框，避免小列表也被塞一个输入框。
 */
@Composable
internal fun ConversationMoveCategorySheet(
    conversationTitle: String?,
    categories: List<ConversationCategoryEntity>,
    currentCategoryId: String?,
    onPick: (String?) -> Unit,
    onCreateAndPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(
        show = conversationTitle != null,
        title = stringResource(R.string.conversation_category_move),
        onDismissRequest = onDismiss,
        insideMargin = DpSize(0.dp, 0.dp),
    ) {
        if (conversationTitle != null) {
            var query by remember { mutableStateOf("") }
            var creating by remember { mutableStateOf(false) }
            var newName by remember { mutableStateOf("") }
            val ordered = remember(categories) { categories.sortedWith(compareBy({ it.sortOrder }, { it.createdAt })) }
            val filtered = remember(ordered, query) {
                val keyword = query.trim()
                if (keyword.isEmpty()) ordered else ordered.filter { it.name.contains(keyword, ignoreCase = true) }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = conversationTitle,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                )
                if (ordered.size > 8) {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        label = stringResource(R.string.conversation_move_category_search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 6.dp),
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .fillMaxWidth(),
                ) {
                    item(key = "uncategorized") {
                        CategoryPickRow(
                            label = stringResource(R.string.conversation_category_uncategorized),
                            barColor = null,
                            selected = currentCategoryId == null,
                            onClick = { onPick(null); onDismiss() },
                        )
                    }
                    items(ordered, key = { it.id }) { category ->
                        CategoryPickRow(
                            label = category.name,
                            barColor = categoryBarColor(ordered.indexOf(category)),
                            selected = currentCategoryId == category.id,
                            onClick = { onPick(category.id); onDismiss() },
                        )
                    }
                    item(key = "create") {
                        CreateCategoryRow(
                            creating = creating,
                            newName = newName,
                            onNameChange = { newName = it },
                            onStart = { creating = true },
                            onCancel = { creating = false; newName = "" },
                            onConfirm = {
                                val name = newName.trim()
                                if (name.isNotEmpty()) {
                                    onCreateAndPick(name)
                                    onDismiss()
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CategoryPickRow(
    label: String,
    barColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (barColor != null) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 16.dp)
                    .background(barColor, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(15.dp))
        }
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            fontSize = 16.sp,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            MiuixIcon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CreateCategoryRow(
    creating: Boolean,
    newName: String,
    onNameChange: (String) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!creating) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStart)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixIcon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.conversation_move_category_create),
                style = MiuixTheme.textStyles.body1,
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            InputField(
                query = newName,
                onQueryChange = onNameChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                label = stringResource(R.string.conversation_move_category_name),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onCancel,
                    minWidth = 0.dp,
                    modifier = Modifier.padding(end = 8.dp),
                )
                TextButton(
                    text = stringResource(R.string.action_confirm),
                    onClick = onConfirm,
                    minWidth = 0.dp,
                )
            }
        }
    }
}
