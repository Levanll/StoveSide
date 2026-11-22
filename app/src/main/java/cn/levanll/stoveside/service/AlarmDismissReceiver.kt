package cn.levanll.stoveside.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 通知栏「知道了」按钮的接收器。
 *
 * 用广播而不是 PendingIntent.getService：点击通知动作时 App 可能在后台，
 * 直接拉起前台服务会撞上 Android 12+ 的后台启动限制；
 * stopService 没有这个限制，且能同时停掉已在运行的服务。
 *
 * 除了停铃，还要把「这一轮已经知道了」写进 [AlarmAck]：
 * 否则用户下拉通知确认完，回到 App 还会被同一个到点再问一次。
 */
class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val stepId = intent?.getLongExtra(AlarmService.EXTRA_STEP_ID, -1L) ?: -1L
        val dueAt = intent?.getLongExtra(AlarmService.EXTRA_DUE_AT, 0L) ?: 0L
        if (stepId >= 0L) AlarmAck.mark(stepId, dueAt)
        AlarmService.stop(context)
    }
}
