package cn.levanll.stoveside.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.levanll.stoveside.MainActivity
import cn.levanll.stoveside.R
import cn.levanll.stoveside.i18n.I18n

object Notifier {

    const val CHANNEL_ONGOING = "ongoing"

    /** 旧channel：只发通知不发声，保留给「仅通知」档并兼容已存在的用户设置。 */
    const val CHANNEL_DUE = "due"

    /** 响铃+震动。channel 建好后属性不可改，所以响铃方式必须用不同 channel id。 */
    const val CHANNEL_DUE_LOUD = "due_loud"

    /** 只震动不发声。 */
    const val CHANNEL_DUE_VIBRATE = "due_vibrate"

    const val ACTION_DISMISS_ALARM = "cn.levanll.stoveside.DISMISS_ALARM"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ctx.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                I18n.t("进行中的计时", "Running timers"),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DUE,
                I18n.t("到点提醒", "Timer finished"),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
                setShowBadge(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DUE_LOUD,
                I18n.t("到点提醒（响铃）", "Timer finished (alarm)"),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                setShowBadge(true)
                // 走闹钟音频流：做饭时手机常连着音箱或放得远，
                // 通知流会被压得很低，闹钟流才是灶台该用的。
                val uri = alarmSoundUri()
                if (uri != null) {
                    setSound(
                        uri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DUE_VIBRATE,
                I18n.t("到点提醒（仅震动）", "Timer finished (vibrate only)"),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                setShowBadge(true)
                setSound(null, null)
            }
        )
    }

    /** 系统闹钟铃声；取不到就退回通知音，再取不到返回 null 由 MediaPlayer 兜底静音。 */
    fun alarmSoundUri(): android.net.Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun contentIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 「知道了」：唯一能真正停止响铃的入口，因此必须始终可见。 */
    private fun dismissIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, AlarmDismissReceiver::class.java).setAction(ACTION_DISMISS_ALARM)
        return PendingIntent.getBroadcast(
            ctx, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun ongoing(ctx: Context, title: String, text: String) {
        if (!hasPermission(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(ctx))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID_ONGOING, n)
    }

    /**
     * 到点通知。「仅通知」档直接发；响铃/震动档由 AlarmService 以前台通知发出，
     * 两者共用 [buildDue] 保证文案与按钮一致。
     */
    fun due(ctx: Context, id: Int, title: String, text: String) {
        if (!hasPermission(ctx)) return
        NotificationManagerCompat.from(ctx).notify(id, buildDue(ctx, title, text, CHANNEL_DUE))
    }

    fun buildDue(
        ctx: Context,
        title: String,
        text: String,
        channel: String,
        dismissible: Boolean = true,
    ): android.app.Notification {
        val builder = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(dismissible)
            .setOngoing(!dismissible)
            .setContentIntent(contentIntent(ctx))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, I18n.t("知道了", "Got it"), dismissIntent(ctx))
        if (!isLoudChannel(channel)) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        }
        return builder.build()
    }

    private fun isLoudChannel(channel: String): Boolean =
        channel == CHANNEL_DUE_LOUD || channel == CHANNEL_DUE_VIBRATE

    fun cancel(ctx: Context, id: Int) {
        NotificationManagerCompat.from(ctx).cancel(id)
    }

    private fun hasPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    const val ID_ONGOING = 1001
    const val ID_DUE_BASE = 2000
    // 远离 ID_DUE_BASE + stepId 的区间，避免和普通到点通知撞号
    const val ID_ALARM = 9100
}
