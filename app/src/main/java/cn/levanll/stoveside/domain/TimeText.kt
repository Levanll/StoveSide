package cn.levanll.stoveside.domain

import cn.levanll.stoveside.i18n.I18n

object TimeText {

    /** 剩余毫秒 → 「12:05」或「1:02:03」，负数按 0 处理 */
    fun clock(remainingMs: Long): String {
        val total = (remainingMs / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** 步骤时长 → 「1 分 30 秒」 */
    fun duration(seconds: Int): String {
        if (seconds <= 0) return I18n.t("不计时", "No timer")
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        val parts = mutableListOf<String>()
        if (h > 0) parts += I18n.t("${h} 小时", "${h}h")
        if (m > 0) parts += I18n.t("${m} 分", "${m}m")
        if (s > 0) parts += I18n.t("${s} 秒", "${s}s")
        return parts.joinToString(" ")
    }
}
