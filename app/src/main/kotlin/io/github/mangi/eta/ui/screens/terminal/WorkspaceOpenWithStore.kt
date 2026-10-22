package io.github.mangi.eta.ui.screens.terminal

import io.github.mangi.eta.config.Prefs

/**
 * 工作区文件浏览器的「打开方式」设置。
 *
 * 存一个包名；空值表示「自动」——优先 MT2 管理器，没装就交给系统默认
 * （系统自带的文件管理器或选择器）。这样没装 MT2 的设备点文件不会得到「打不开」。
 */
internal object WorkspaceOpenWithStore {
    private const val KEY = "workspace_open_with_package"

    /** 用户显式选的包名；null 表示自动。 */
    fun current(): String? =
        runCatching { Prefs.localAgentPreferences()?.getString(KEY, null) }
            .getOrNull()?.takeIf { it.isNotBlank() }

    fun save(packageName: String) {
        runCatching { Prefs.localAgentPreferences()?.edit()?.putString(KEY, packageName)?.apply() }
    }

    fun clear() {
        runCatching { Prefs.localAgentPreferences()?.edit()?.remove(KEY)?.apply() }
    }
}
