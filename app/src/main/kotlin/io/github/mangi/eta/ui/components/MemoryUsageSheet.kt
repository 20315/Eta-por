package io.github.mangi.eta.ui.components

import android.content.pm.ApplicationInfo
import android.os.Process
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.device.MemoryProcessProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 内存面板：概览 + 按应用聚合的占用排行 + Eta 自己起的后台任务。
 *
 * 弹出方式跟入口对齐：从输入框右上角那个「内存 N%」的位置**放大**出现，
 * 不再从屏幕底部滑上来——入口在上方、面板从下方冒出会显得脱节。
 * 关闭时反向缩回同一角，视线始终落在原处。
 */
@Composable
internal fun MemoryUsageSheet(show: Boolean, onDismiss: () -> Unit) {
    if (!show) return
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(MemorySheetInitialScale) }
    val scrimAlpha = remember { Animatable(0f) }
    // 卡片自己也要能淡入淡出：0.12 倍时内容糊成一团，先透明再显示更干净。
    val cardAlpha = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    /**
     * 关闭：卡片缩回原处、遮罩同步淡掉，动画播完才真正销毁 Dialog。
     * 之前是父级置 show=false 直接砍掉，退场等于硬切，才显得「从哪来回哪去」不成立。
     */
    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            launch {
                scrimAlpha.animateTo(0f, tween(MemorySheetExitMs, easing = FastOutLinearInEasing))
            }
            launch {
                // 透明度先收工：缩到最小时已经看不见，不会在那儿「停一下」。
                cardAlpha.animateTo(0f, tween(MemorySheetCardFadeOutMs, easing = LinearOutSlowInEasing))
            }
            // 退场先慢后快，最后的收缩干净利落。
            scale.animateTo(
                MemorySheetInitialScale,
                tween(MemorySheetExitMs, easing = FastOutLinearInEasing),
            )
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = true,
        ),
    ) {
        val density = LocalDensity.current
        val container = LocalWindowInfo.current.containerSize
        val sheetWidth = with(density) { (container.width * 0.92f).toDp() }
        val sheetHeight = with(density) { (container.height * 0.62f).toDp() }

        // 打开：遮罩垫底，卡片先在前 80ms 淡进来（避开 0.12 倍时糊成一团的那一下），
        // 再以先快后慢的曲线放大落定。
        LaunchedEffect(Unit) {
            launch {
                scrimAlpha.animateTo(1f, tween(MemorySheetEnterMs, easing = LinearOutSlowInEasing))
            }
            launch {
                cardAlpha.animateTo(1f, tween(MemorySheetCardFadeInMs, easing = LinearOutSlowInEasing))
            }
            scale.animateTo(1f, tween(MemorySheetEnterMs, easing = LinearOutSlowInEasing))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha.value }
                    .background(Color.Black.copy(alpha = MemorySheetScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { close() },
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = MemorySheetBottomPadding)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = cardAlpha.value
                        // 缩放的锚点就是卡片右下角，也就是输入框右上角那个入口的位置。
                        transformOrigin = TransformOrigin(1f, 1f)
                    },
            ) {
                MemorySheetCard(
                    width = sheetWidth,
                    height = sheetHeight,
                    onDismiss = { close() },
                )
            }
        }
    }
}

/** 起点就是「内存 N%」那行小字的尺寸（卡片约 378dp 宽 × 0.12 ≈ 45dp），缩放锚点也在它那一角。 */
private const val MemorySheetInitialScale = 0.12f
private const val MemorySheetEnterMs = 340
private const val MemorySheetExitMs = 340
/** 前 80ms 只做淡入，避免 0.12 倍时内容糊成一团被看见。 */
private const val MemorySheetCardFadeInMs = 80
/** 与缩放同时收工；配合「先快后慢」的曲线，末尾缩小时已经接近透明，看不到停顿。 */
private const val MemorySheetCardFadeOutMs = 340
private const val MemorySheetScrimAlpha = 0.32f

/** 卡片距屏幕底部留出的高度：大致等于输入框卡片高度，让面板贴着入口那一角。 */
private val MemorySheetBottomPadding = 104.dp

@Composable
private fun MemorySheetCard(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selfPid = remember { Process.myPid() }
    val selfPackage = remember { context.packageName }
    var procs by remember { mutableStateOf<List<MemoryProcessProbe.Proc>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var armedPackage by remember { mutableStateOf<String?>(null) }
    val usage = remember(loaded) { readMemoryUsage(context) }

    suspend fun reload() {
        withContext(Dispatchers.IO) { MemoryProcessProbe.snapshot() }.let {
            procs = it
            loaded = true
        }
    }

    LaunchedEffect(Unit) { reload() }

    val children = remember(procs, selfPid) {
        MemoryProcessProbe.descendants(procs, selfPid)
    }
    val apps = remember(procs, context, selfPackage) {
        resolveAppRows(context, MemoryProcessProbe.appUsage(procs, MemoryRankLimit), selfPackage)
    }

    fun terminate(pids: List<Int>) {
        if (pids.isEmpty() || busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) { MemoryProcessProbe.terminate(pids) }
            // 结束要一会儿才反映到 ps，稍等一下再抓，避免列表回到旧状态。
            delay(400)
            reload()
            busy = false
        }
    }

    fun release(packageName: String, label: String) {
        if (busy) return
        busy = true
        armedPackage = null
        scope.launch {
            val ok = withContext(Dispatchers.IO) { MemoryProcessProbe.release(packageName) }
            delay(400)
            reload()
            busy = false
            Toast.makeText(
                context,
                if (ok) {
                    context.getString(R.string.memory_sheet_release_done, label)
                } else {
                    context.getString(R.string.memory_sheet_release_failed, label)
                },
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(MiuixTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.memory_sheet_title),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                text = stringResource(R.string.memory_sheet_cancel),
                onClick = onDismiss,
                minWidth = 0.dp,
                minHeight = 30.dp,
                insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(),
            )
        }
        Spacer(Modifier.height(6.dp))
        UsageSummary(usage)
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item(key = "rank-title") {
                SectionTitle(
                    stringResource(R.string.memory_sheet_rank_title),
                    trailing = stringResource(R.string.memory_sheet_release_hint),
                )
            }
            items(items = apps, key = { "a-${it.packageName}" }) { row ->
                AppUsageRow(
                    row = row,
                    armed = armedPackage == row.packageName,
                    onArm = { armedPackage = row.packageName },
                    onCancel = { armedPackage = null },
                    onConfirm = { release(row.packageName, row.label) },
                )
            }
            item(key = "task-title") {
                SectionTitle(
                    stringResource(R.string.memory_sheet_own_tasks_title),
                    trailing = if (children.isEmpty()) null else stringResource(
                        R.string.memory_sheet_own_tasks_count,
                        children.size,
                    ),
                )
            }
            if (children.isEmpty()) {
                item(key = "task-empty") {
                    Text(
                        text = stringResource(R.string.memory_sheet_own_tasks_empty),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(items = children, key = { "c-${it.pid}" }) { proc ->
                TaskRow(
                    proc = proc,
                    onKill = { terminate(listOf(proc.pid)) },
                )
            }
        }
        if (children.isNotEmpty()) {
            TextButton(
                text = stringResource(R.string.memory_sheet_kill_all, children.size),
                onClick = { terminate(children.map { it.pid }) },
                enabled = !busy,
                minWidth = 0.dp,
                minHeight = 36.dp,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )
        }
    }
}

private const val MemoryRankLimit = 8

/** 排行里的一行：解析成应用名，并判断能不能释放。 */
private data class AppUsageRow(
    val packageName: String,
    val label: String,
    val rssBytes: Long,
    val releasable: Boolean,
)

private fun resolveAppRows(
    context: android.content.Context,
    usage: List<MemoryProcessProbe.AppUsage>,
    selfPackage: String,
): List<AppUsageRow> {
    val pm = context.packageManager
    return usage.map { item ->
        val info = runCatching {
            pm.getPackageInfo(item.packageName, 0).applicationInfo
        }.getOrNull()
        val label = info
            ?.let { app -> runCatching { pm.getApplicationLabel(app).toString() }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
            ?: item.packageName
        // 系统应用（含系统应用更新后的版本）不给释放入口；查不到包信息的进程同样只读。
        val systemApp = info == null ||
            (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        AppUsageRow(
            packageName = item.packageName,
            label = label,
            rssBytes = item.rssBytes,
            releasable = !systemApp && item.packageName != selfPackage,
        )
    }
}

@Composable
private fun UsageSummary(usage: MemoryUsage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.memory_sheet_used,
                    formatGigabytes(usage.usedBytes),
                    formatGigabytes(usage.totalBytes),
                ),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.memory_sheet_available, formatGigabytes(usage.availableBytes)),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usage.usedPercent / 100f)
                    .height(6.dp)
                    .background(MiuixTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
            )
        }
        if (usage.swapTotalBytes > 0L) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.memory_sheet_swap_line,
                    formatGigabytes(usage.swapUsedBytes),
                    formatGigabytes(usage.swapTotalBytes),
                ),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.memory_sheet_scope_note),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = trailing,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun AppUsageRow(
    row: AppUsageRow,
    armed: Boolean,
    onArm: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 应用名和包名不同才补一行，方便对号和排查。
            if (row.label != row.packageName) {
                Text(
                    text = row.packageName,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatMegabytes(row.rssBytes),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (row.releasable) {
            Spacer(Modifier.width(2.dp))
            if (armed) {
                TextButton(
                    text = stringResource(R.string.memory_sheet_cancel),
                    onClick = onCancel,
                    minWidth = 0.dp,
                    minHeight = 30.dp,
                    insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(),
                )
                TextButton(
                    text = stringResource(R.string.memory_sheet_confirm),
                    onClick = onConfirm,
                    minWidth = 0.dp,
                    minHeight = 30.dp,
                    insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            } else {
                TextButton(
                    text = stringResource(R.string.memory_sheet_release),
                    onClick = onArm,
                    minWidth = 0.dp,
                    minHeight = 30.dp,
                    insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@Composable
private fun TaskRow(proc: MemoryProcessProbe.Proc, onKill: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = proc.name,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatMegabytes(proc.rssBytes),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.width(4.dp))
        TextButton(
            text = stringResource(R.string.memory_sheet_kill),
            onClick = onKill,
            minWidth = 0.dp,
            minHeight = 30.dp,
            insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
