package io.github.mangi.eta.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 顶栏内存胶囊：显示物理内存已用百分比，点一下看已用 / 可用 / 总量。
 * 只读 ActivityManager.MemoryInfo，不需要 Root，也不改动任何系统设置。
 */
@Composable
internal fun MemoryUsageCapsule(compact: Boolean = false) {
    val context = LocalContext.current
    var usage by remember { mutableStateOf(readMemoryUsage(context)) }
    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        while (true) {
            usage = readMemoryUsage(context)
            delay(MEMORY_REFRESH_MILLIS)
        }
    }
    Box {
        StatusCapsule(
            // 顶栏窄的时候只留百分比数字，省下的宽度优先给上下文用量。
            text = if (compact) {
                stringResource(R.string.memory_capsule_compact, usage.usedPercent)
            } else {
                stringResource(R.string.memory_capsule, usage.usedPercent)
            },
            modifier = Modifier.clickable { showMenu = true },
            description = stringResource(R.string.memory_capsule_description, usage.usedPercent),
        )
        MemoryUsageSheet(show = showMenu, onDismiss = { showMenu = false })
    }
}

internal data class MemoryUsage(
    val usedPercent: Int,
    val usedBytes: Long,
    val availableBytes: Long,
    val totalBytes: Long,
    /** 内存拓展（ZRAM/Swap）。系统设置页把它算进"总计"，我们只作参考展示。 */
    val swapTotalBytes: Long = 0L,
    val swapUsedBytes: Long = 0L,
)

internal fun readMemoryUsage(context: Context): MemoryUsage {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val info = ActivityManager.MemoryInfo()
    runCatching { manager?.getMemoryInfo(info) }
    val total = info.totalMem.takeIf { it > 0 } ?: 1L
    val available = info.availMem.coerceAtLeast(0L)
    val used = (total - available).coerceAtLeast(0L)
    val percent = ((used.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    val (swapTotal, swapUsed) = readSwapBytes()
    return MemoryUsage(percent, used, available, total, swapTotal, swapUsed)
}

/**
 * 内存拓展（ZRAM）：系统设置里的「运行内存」把它和物理内存加在一起算总量，
 * 所以那边显示"总计 16.0 + 12.0 GB、可用 17.5 GB"。这里只做展示，不参与百分比。
 */
private fun readSwapBytes(): Pair<Long, Long> = runCatching {
    var total = 0L
    var free = 0L
    java.io.File("/proc/meminfo").useLines { lines ->
        lines.forEach { line ->
            when {
                line.startsWith("SwapTotal:") -> total = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
                line.startsWith("SwapFree:") -> free = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
            }
        }
    }
    total * 1024L to (total - free).coerceAtLeast(0L) * 1024L
}.getOrDefault(0L to 0L)

internal fun formatGigabytes(bytes: Long): String =
    String.format(Locale.US, "%.1f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))

internal fun formatMegabytes(bytes: Long): String =
    String.format(Locale.US, "%.0f MB", bytes.toDouble() / (1024.0 * 1024.0))

private const val MEMORY_REFRESH_MILLIS = 5000L
