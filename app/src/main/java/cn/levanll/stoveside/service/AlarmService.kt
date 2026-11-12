package cn.levanll.stoveside.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat
import cn.levanll.stoveside.R
import cn.levanll.stoveside.i18n.I18n

/**
 * 到点后的持续提醒。用户点「知道了」才停，没有超时。
 *
 * 为什么必须单独一个前台服务：
 * 普通通知的响铃/震动由系统一次性播放，最长一两秒，之后不会自己再响。
 * 用户离得远（做饭时常见）就完全错过。「持续响铃」在 Android 上没有捷径，
 * 只能由应用自己持有 MediaPlayer + Vibrator，并用前台服务保证进程不被回收。
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Notifier.ACTION_DISMISS_ALARM) {
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: I18n.t("到点了", "Timer done")
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: ""

        // 多个步骤同时到点时只保留一个响铃实例：重复 start 会让
        // MediaPlayer 层层叠加，用户点一次「知道了」只停掉最上面一层。
        if (ringing) {
            NotificationManagerCompat.from(this)
                .notify(Notifier.ID_ALARM, buildNotification(title, text))
            return START_STICKY
        }

        startForeground(Notifier.ID_ALARM, buildNotification(title, text))

        // 息屏后 CPU 会休眠，MediaPlayer 与 Vibrator 都需要保持唤醒才能持续
        acquireWakeLock()
        startRinging()
        ringing = true
        return START_STICKY
    }

    private fun buildNotification(title: String, text: String): Notification {
        val channel = when (AlarmPrefs.mode) {
            AlarmPrefs.Mode.SOUND_AND_VIBRATE -> Notifier.CHANNEL_DUE_LOUD
            AlarmPrefs.Mode.VIBRATE_ONLY -> Notifier.CHANNEL_DUE_VIBRATE
            AlarmPrefs.Mode.NOTIFICATION_ONLY -> Notifier.CHANNEL_DUE
        }
        return Notifier.buildDue(this, title, text, channel, dismissible = false)
    }

    private fun startRinging() {
        if (AlarmPrefs.mode == AlarmPrefs.Mode.NOTIFICATION_ONLY) return

        if (AlarmPrefs.mode == AlarmPrefs.Mode.SOUND_AND_VIBRATE) {
            runCatching {
                val uri = Notifier.alarmSoundUri() ?: return@runCatching
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }

        runCatching {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator = v
            val pattern = longArrayOf(0, 500, 300, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        }
    }

    private fun acquireWakeLock() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StoveSide:Alarm").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseAll() {
        ringing = false
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        releaseAll()
        NotificationManagerCompat.from(this).cancel(Notifier.ID_ALARM)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"

        fun start(ctx: Context, title: String, text: String) {
            val i = Intent(ctx, AlarmService::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AlarmService::class.java))
        }
    }
}
