package cn.levanll.stoveside.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cn.levanll.stoveside.R
import cn.levanll.stoveside.StoveApp
import cn.levanll.stoveside.data.entity.CookStep
import cn.levanll.stoveside.domain.TimeText
import cn.levanll.stoveside.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 前台服务：只在 App 退到后台时承担"到点提醒"。
 * 到期时间全部来自数据库里的墙钟时间，服务重启也不会丢；
 * 已提醒过的步骤用内存集合去重，避免每秒刷屏。
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val notified = mutableSetOf<Pair<Long, Long?>>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildForeground(null, null))
        if (job == null) job = scope.launch { tickLoop() }
        return START_STICKY
    }

    private suspend fun tickLoop() {
        val repo = StoveApp.instance.repository
        while (true) {
            val now = System.currentTimeMillis()
            val running = runCatching { repo.runningSteps() }.getOrDefault(emptyList())
            // 到点只提醒，不改状态：改回 pending 会让用户丢掉"这锅还在跑"的事实，
            // 也让「+1 分钟」无处可点。去重键用 stepId+dueAt，同一轮到期只响一次。
            val due = running.filter { it.dueAt != null && it.dueAt <= now }
                .filter { (it.id to it.dueAt) !in notified }
            for (step in due) {
                notified += step.id to step.dueAt
                Notifier.due(
                    this,
                    (Notifier.ID_DUE_BASE + step.id).toInt(),
                    I18n.t("到点了", "Timer done"),
                    step.title,
                )
            }
            val next = running.filter { it.dueAt != null }
                .minByOrNull { it.dueAt!! }
            updateForeground(next, running.size, now)
            delay(1000)
        }
    }

    private fun buildForeground(next: CookStep?, count: Int?) = NotificationCompat.Builder(this, Notifier.CHANNEL_ONGOING)
        .setSmallIcon(R.drawable.ic_stat_timer)
        .setContentTitle(I18n.t("灶暖计时运行中", "StoveSide timers running"))
        .setContentText(
            when {
                next == null -> I18n.t("没有进行中的计时", "No active timers")
                else -> "${next.title} · ${TimeText.clock(next.dueAt!! - System.currentTimeMillis())}"
            }
        )
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun updateForeground(next: CookStep?, count: Int, now: Long) {
        val n = buildForeground(next, count)
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, n)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val ACTION_STOP = "cn.levanll.stoveside.STOP_TIMERS"

        fun start(ctx: Context) {
            val i = Intent(ctx, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, TimerService::class.java))
        }
    }
}