package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.model.LocalToolProfile
import io.github.mangi.eta.agent.model.LocalToolProfileStore
import io.github.mangi.eta.ui.model.AgentModelOptionUi
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏的模式开关：只有「全部工具」和「纯聊天」两种，图标分段式，点一下就切。
 *
 * 轻度 / 开发 / 极简 / 标准仍留在 [LocalToolProfile] 里，但只作为上下文超限时的自动降档阶梯，
 * 不再让人手选——它们长期没人碰，摆在界面上只是噪音。
 * 模式只决定发给模型的工具清单，不影响工具自身的执行能力。
 */
private val profileOptions = listOf(LocalToolProfile.FULL, LocalToolProfile.CHAT)

/** 旧配置里的中间档一律归到「全部工具」：存储值不动，等用户主动切一次才被改写。 */
internal fun profileOption(profile: LocalToolProfile): LocalToolProfile =
    if (profile == LocalToolProfile.CHAT) LocalToolProfile.CHAT else LocalToolProfile.FULL

@Composable
internal fun LocalProfileTopBarCapsule(model: AgentModelOptionUi?, compact: Boolean = false) {
    val profileKey = model?.let { it.providerId to it.modelId }
    var profile by remember(profileKey) {
        mutableStateOf(LocalToolProfileStore.current(profileKey?.first, profileKey?.second))
    }
    val current = profileOption(profile)
    val haptic = LocalHapticFeedback.current
    val selectedBrush = Brush.linearGradient(
        colors = listOf(
            MiuixTheme.colorScheme.primary,
            MiuixTheme.colorScheme.primary.copy(alpha = 0.72f),
        ),
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        profileOptions.forEach { option ->
            val selected = option == current
            val label = stringResource(profileLabel(option))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (selected) Modifier.background(selectedBrush) else Modifier)
                    .clickable(enabled = !selected) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        LocalToolProfileStore.save(option, profileKey?.first, profileKey?.second)
                        profile = option
                        LocalProfileSwitchNotice.publish(
                            if (option == LocalToolProfile.CHAT) {
                                LocalToolProfileSwitch.Chat
                            } else {
                                LocalToolProfileSwitch.Tools
                            },
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (option == LocalToolProfile.CHAT) {
                        Icons.AutoMirrored.Rounded.Chat
                    } else {
                        Icons.Rounded.Widgets
                    },
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = if (selected) {
                        MiuixTheme.colorScheme.onPrimary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                )
            }
        }
    }
}

internal fun profileLabel(profile: LocalToolProfile): Int = when (profile) {
    LocalToolProfile.FULL -> R.string.local_profile_full
    LocalToolProfile.DEV -> R.string.local_profile_dev
    LocalToolProfile.CHAT -> R.string.local_profile_chat
    LocalToolProfile.LIGHT -> R.string.local_profile_light
    LocalToolProfile.STANDARD -> R.string.local_profile_standard
    LocalToolProfile.MINIMAL -> R.string.local_profile_minimal
}
