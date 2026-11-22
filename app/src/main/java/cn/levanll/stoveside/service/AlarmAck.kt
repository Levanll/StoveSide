package cn.levanll.stoveside.service

/**
 * 「这一轮到点我已经知道了」的共享记录。
 *
 * 确认有两条入口：App 内弹窗和通知栏的「知道了」/划走通知。两条都停铃，
 * 也必须都算数——否则用户下拉通知确认完，回到 App 还会被同一个到点再问一次。
 *
 * 键里带 dueAt：只记 stepId 的话，点过「+1 分钟」后再归零就不会再提醒了。
 * 只在内存里，进程重开后过期步骤会重新提醒，因为那个步骤客观上还没处理完。
 */
object AlarmAck {

    private val acked = mutableSetOf<Pair<Long, Long>>()

    fun mark(stepId: Long, dueAt: Long) {
        if (stepId >= 0L) acked += stepId to dueAt
    }

    fun isAcked(stepId: Long, dueAt: Long): Boolean = (stepId to dueAt) in acked

    /** 「+1 分钟」把旧的一轮作废：放开记忆，下次再归零才会重新提醒 */
    fun forget(stepId: Long) {
        acked.removeAll(acked.filter { it.first == stepId }.toSet())
    }
}
