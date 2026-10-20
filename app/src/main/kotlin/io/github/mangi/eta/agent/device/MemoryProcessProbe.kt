package io.github.mangi.eta.agent.device

import io.github.mangi.eta.core.AndroidAgentLogger

/**
 * 内存面板的数据源：进程 RSS 排行，以及「Eta 自己起的后台任务」。
 *
 * 只读 `ps`，结束操作只针对自己的后代进程（终端会话、构建进程、守护进程），
 * 不碰系统进程，也不需要猜包名。
 */
internal object MemoryProcessProbe {
    data class Proc(
        val pid: Int,
        val ppid: Int,
        val rssBytes: Long,
        val name: String,
    )

    private val executor by lazy {
        BoundedRootCommandExecutor(AndroidAgentLogger, rootAvailable = { RootAccess.isGranted })
    }

    private val lineSplit = Regex("\\s+")

    /** 一次 `ps` 抓全量快照；调用方自己决定排序与截断。 */
    fun snapshot(): List<Proc> {
        val result = executor.execute(
            "ps -A -o PID,PPID,RSS,NAME",
            timeoutMillis = 8_000L,
            maxOutputBytes = 512 * 1024,
        )
        if (!result.ok) return emptyList()
        return result.stdout.lineSequence().mapNotNull { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("PID")) return@mapNotNull null
            val parts = line.split(lineSplit, limit = 4)
            if (parts.size != 4) return@mapNotNull null
            val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
            val ppid = parts[1].toIntOrNull() ?: return@mapNotNull null
            val rssKb = parts[2].toLongOrNull() ?: return@mapNotNull null
            Proc(pid = pid, ppid = ppid, rssBytes = rssKb * 1024L, name = parts[3])
        }.toList()
    }

    /** 一行的占用：同名多进程（:remote 之类）合并到同一个包名上。 */
    data class AppUsage(
        val packageName: String,
        val rssBytes: Long,
    )

    /**
     * 按应用聚合的内存排行。进程名 `com.foo:remote` 归到 `com.foo`；
     * `system_server`、`ps`、`[kthreadd]` 这类不是包名，原样保留，界面上查不到应用信息就只读展示。
     */
    fun appUsage(procs: List<Proc>, limit: Int): List<AppUsage> =
        procs.asSequence()
            .filter { it.pid > 0 && it.rssBytes > 0 && it.name.isNotBlank() }
            .groupBy { proc -> proc.name.substringBefore(':').trim() }
            .map { (name, group) -> AppUsage(name, group.sumOf(Proc::rssBytes)) }
            .sortedByDescending(AppUsage::rssBytes)
            .take(limit)
            .toList()

    /**
     * 释放一个用户应用：`am force-stop` 让系统把它整个停掉。
     * 只接受形如包名的参数，调用方保证不是系统应用、也不是 Eta 自己。
     */
    fun release(packageName: String): Boolean {
        if (!PACKAGE_NAME.matches(packageName)) return false
        val result = executor.execute(
            "am force-stop $packageName",
            timeoutMillis = 8_000L,
            maxOutputBytes = 16 * 1024,
        )
        return result.ok
    }

    private val PACKAGE_NAME = Regex("[A-Za-z0-9_][A-Za-z0-9_.]*")

    /**
     * 当前进程的全部后代。Eta 起的终端会话、构建进程都是它的子进程，
     * 顺着 ppid 链找比按名字猜安全得多。
     */
    fun descendants(procs: List<Proc>, rootPid: Int): List<Proc> {
        val childrenByParent = procs.groupBy(Proc::ppid)
        val collected = mutableListOf<Proc>()
        val queue = ArrayDeque<Proc>()
        childrenByParent[rootPid]?.let(queue::addAll)
        val seen = mutableSetOf<Int>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.pid == rootPid || !seen.add(current.pid)) continue
            collected += current
            childrenByParent[current.pid]?.let(queue::addAll)
        }
        return collected.sortedByDescending(Proc::rssBytes)
    }

    /** 结束指定进程；调用方保证这些 pid 都是自己的后代。返回是否发出成功。 */
    fun terminate(pids: List<Int>): Boolean {
        val targets = pids.filter { it > 1 }
        if (targets.isEmpty()) return false
        val result = executor.execute(
            "kill -TERM " + targets.joinToString(" "),
            timeoutMillis = 5_000L,
            maxOutputBytes = 16 * 1024,
        )
        return result.ok
    }
}
