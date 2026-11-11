package cn.levanll.stoveside.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 应用内语言。
 *
 * 为什么不用系统 Locale：用户明确要求"切了以后下次启动还是这个语言"，
 * 与手机系统语言解耦才可预测；SharedPreferences 存一个枚举值即可。
 */
object I18n {
    enum class Lang { ZH, EN }

    private const val PREF = "stoveside_prefs"
    private const val KEY_LANG = "lang"

    private var prefs: android.content.SharedPreferences? = null

    var lang by mutableStateOf(Lang.ZH)
        private set

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs = p
        lang = if (p.getString(KEY_LANG, "zh") == "en") Lang.EN else Lang.ZH
    }

    fun set(target: Lang) {
        lang = target
        prefs?.edit()?.putString(KEY_LANG, if (target == Lang.EN) "en" else "zh")?.apply()
    }

    fun toggle(): Lang {
        set(if (lang == Lang.ZH) Lang.EN else Lang.ZH)
        return lang
    }

    /** t("中文字面量", "English literal")，代码里就地双语，避免两套资源文件错位。 */
    fun t(zh: String, en: String): String = if (lang == Lang.ZH) zh else en
}
