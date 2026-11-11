package cn.levanll.stoveside.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 预留：系统闹钟触发的到点提醒。
 * 当前版本所有到期判断都在 TimerService 的 tick 里完成，
 * 这里只负责把被系统杀掉的计时服务重新拉起。
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        TimerService.start(context)
    }
}
