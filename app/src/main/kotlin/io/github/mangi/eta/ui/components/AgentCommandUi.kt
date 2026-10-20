package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.model.AgentCommand
import io.github.mangi.eta.ui.model.CommandConfirmUi
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 命令面板：输入 `/` 后浮在输入框上方，只列命中的命令。
 *
 * 手机上没有上下键，交互收敛成「点一条」：无参数命令点了直接执行，
 * 有参数命令点了补全成 `/name ` 继续输入。
 */
@Composable
internal fun AgentCommandPalette(
    matches: List<AgentCommand>,
    onPick: (AgentCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(vertical = 6.dp),
    ) {
        matches.forEach { command ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(command) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "/" + command.name,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.primary,
                    maxLines = 1,
                )
                command.argHint?.let { hint ->
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = hint,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = command.summary,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 命令确认卡：未知命令（要不要按原样发出去）与带副作用的命令（压缩上下文、停止任务）共用。
 * 位置就在输入框上方，跟命令面板同一块区域，视线不用来回跳。
 */
@Composable
internal fun AgentCommandConfirmCard(
    confirm: CommandConfirmUi,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Text(
            text = confirm.title,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = confirm.detail,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = stringResource(R.string.agent_command_cancel),
                onClick = onDismiss,
                minWidth = 0.dp,
                minHeight = 34.dp,
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.textButtonColors(),
            )
            TextButton(
                text = stringResource(
                    if (confirm.commandName == null) {
                        R.string.agent_command_send_as_text
                    } else {
                        R.string.agent_command_confirm_action
                    },
                ),
                onClick = onAccept,
                minWidth = 0.dp,
                minHeight = 34.dp,
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
