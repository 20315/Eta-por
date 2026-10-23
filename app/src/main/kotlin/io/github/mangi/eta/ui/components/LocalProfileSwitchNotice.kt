package io.github.mangi.eta.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 模式切换的两种结果。 */
internal enum class LocalToolProfileSwitch {
    Tools,
    Chat,
}

/**
 * 模式切换提示：一个极简的全局状态，胶囊写完，输入框上方那条提示条读它。
 *
 * 用单例而不是状态流，是因为它只服务一个瞬时提示，没必要把整条状态链串起来；
 * [revision] 让同一种模式连续切换两次也能重新计时。
 */
internal object LocalProfileSwitchNotice {
    var pending by mutableStateOf<LocalToolProfileSwitch?>(null)
        private set

    private var revision by mutableLongStateOf(0L)

    fun publish(kind: LocalToolProfileSwitch) {
        pending = kind
        revision += 1
    }

    fun revisionToken(): Long = revision

    fun consume(token: Long) {
        if (token == revision) pending = null
    }
}

/**
 * 输入框上方的切换提示：切完模式告诉你这轮到底带不带工具，2.4 秒后自己淡出。
 * 位置就在对话流末尾与输入框之间，不挡消息也不改历史。
 */
@Composable
internal fun LocalProfileSwitchToast(modifier: Modifier = Modifier) {
    val kind = LocalProfileSwitchNotice.pending
    val token = LocalProfileSwitchNotice.revisionToken()
    // 退场动画期间 pending 已经清空，保留最后一次内容避免闪空。
    var shown by remember { mutableStateOf<LocalToolProfileSwitch?>(null) }
    if (kind != null) shown = kind
    LaunchedEffect(token, kind) {
        if (kind != null) {
            delay(2_400)
            LocalProfileSwitchNotice.consume(token)
        }
    }
    AnimatedVisibility(
        visible = kind != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val current = shown ?: LocalToolProfileSwitch.Tools
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(
                    color = MiuixTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (current == LocalToolProfileSwitch.Chat) {
                    Icons.AutoMirrored.Rounded.Chat
                } else {
                    Icons.Rounded.Widgets
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (current == LocalToolProfileSwitch.Chat) {
                        R.string.local_profile_switched_chat
                    } else {
                        R.string.local_profile_switched_tools
                    },
                ),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
