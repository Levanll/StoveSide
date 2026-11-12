package cn.levanll.stoveside

import android.app.Application
import cn.levanll.stoveside.data.StoveDatabase
import cn.levanll.stoveside.data.StoveRepository
import cn.levanll.stoveside.i18n.I18n
import cn.levanll.stoveside.service.AlarmPrefs

class StoveApp : Application() {

    lateinit var repository: StoveRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        I18n.init(this)
        AlarmPrefs.init(this)
        repository = StoveRepository(StoveDatabase.get(this).dao())
    }

    companion object {
        lateinit var instance: StoveApp
            private set
    }
}
