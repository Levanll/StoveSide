package cn.levanll.stoveside

import cn.levanll.stoveside.domain.TimeText
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 「加一分钟」的回归测试。
 *
 * 背景：旧实现用 dueAt + 60s 当新到期时间。因为本 App 到点只提醒、不改状态，
 * dueAt 归零后会停在过去并越走越远，加上的 60 秒被已过期时长吃掉——
 * 归零 40 秒后点 +1 只剩 20 秒，归零 60 秒后点完全没反应。
 */
class TimeTextTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `extend before due keeps original deadline plus one minute`() {
        // 还剩 5 分钟时点 +1：必须在原到期时刻上加，而不是重置成 60 秒
        val due = now + 5 * 60_000L
        assertEquals(now + 6 * 60_000L, TimeText.extendDue(due, now))
    }

    @Test
    fun `extend exactly at due gives full minute`() {
        assertEquals(now + 60_000L, TimeText.extendDue(now, now))
    }

    @Test
    fun `extend 40s overdue gives full minute not 20s`() {
        // 这是用户实际遇到的场景：归零 40 秒后点 +1
        val due = now - 40_000L
        assertEquals(now + 60_000L, TimeText.extendDue(due, now))
    }

    @Test
    fun `extend 60s overdue still gives full minute`() {
        // 旧实现这里等于没反应
        val due = now - 60_000L
        assertEquals(now + 60_000L, TimeText.extendDue(due, now))
    }

    @Test
    fun `extend long overdue gives full minute`() {
        val due = now - 30 * 60_000L
        assertEquals(now + 60_000L, TimeText.extendDue(due, now))
    }

    @Test
    fun `repeated extend after overdue accumulates`() {
        var due = now - 90_000L
        repeat(3) { due = TimeText.extendDue(due, now) }
        assertEquals(now + 3 * 60_000L, due)
    }
}
