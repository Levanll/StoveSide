package cn.levanll.stoveside.service

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 到点提醒的响铃方式。用户可在设置页切换，选择立即持久化。
 *
 * 单独建一个偏好对象而不是塞进 I18n：两者的生命周期与语义都不一样，
 * 且提醒方式需要在 Service 里被读取，不应依赖 Compose 的 I18n 状态。
 */
object AlarmPrefs {

    enum class Mode { SOUND_AND_VIBRATE, VIBRATE_ONLY, NOTIFICATION_ONLY }

    private const val PREF = "stoveside_prefs"
    private const val KEY_MODE = "alarm_mode"

    private var prefs: android.content.SharedPreferences? = null

    /** 默认「响铃 + 震动」：灶台场景最常见的诉求是「我离得远也要听见」。 */
    var mode by mutableStateOf(Mode.SOUND_AND_VIBRATE)
        private set

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs = p
        mode = when (p.getString(KEY_MODE, null)) {
            "vibrate" -> Mode.VIBRATE_ONLY
            "notification" -> Mode.NOTIFICATION_ONLY
            else -> Mode.SOUND_AND_VIBRATE
        }
    }

    fun set(target: Mode) {
        mode = target
        val raw = when (target) {
            Mode.SOUND_AND_VIBRATE -> "sound_vibrate"
            Mode.VIBRATE_ONLY -> "vibrate"
            Mode.NOTIFICATION_ONLY -> "notification"
        }
        prefs?.edit()?.putString(KEY_MODE, raw)?.apply()
    }
}
