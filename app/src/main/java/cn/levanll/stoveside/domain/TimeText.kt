package cn.levanll.stoveside.domain

import cn.levanll.stoveside.i18n.I18n

object TimeText {

    /**
     * 「加一分钟」之后的新到期时刻。
     *
     * 基准取 max(now, dueAt)，不是直接 dueAt + 60s：
     * 本 App 到点只提醒、不改状态，dueAt 归零后会停在过去并越走越远，
     * 以它为基准时加上的 60 秒会被「已过期时长」吃掉——
     * 归零 40 秒后点一次只剩 20 秒，归零 60 秒后点看起来毫无反应。
     * 反过来若直接用 now 当基准，未到点（还剩 5 分钟）时点 +1
     * 会把剩余重置成 60 秒，所以必须取两者较晚的一个。
     */
    fun extendDue(dueAt: Long, now: Long, addMs: Long = 60_000L): Long =
        maxOf(now, dueAt) + addMs

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
