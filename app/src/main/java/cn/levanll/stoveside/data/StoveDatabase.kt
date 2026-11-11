package cn.levanll.stoveside.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cn.levanll.stoveside.data.dao.StoveDao
import cn.levanll.stoveside.data.entity.Cook
import cn.levanll.stoveside.data.entity.CookStep
import cn.levanll.stoveside.data.entity.Recipe
import cn.levanll.stoveside.data.entity.Step

@Database(
    entities = [Recipe::class, Step::class, Cook::class, CookStep::class],
    version = 1,
    exportSchema = true,
)
abstract class StoveDatabase : RoomDatabase() {
    abstract fun dao(): StoveDao

    companion object {
        @Volatile
        private var instance: StoveDatabase? = null

        fun get(context: Context): StoveDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                StoveDatabase::class.java,
                "stoveside.db",
            )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { instance = it }
        }
    }
}
