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
 */
class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlarmService.stop(context)
    }
}
