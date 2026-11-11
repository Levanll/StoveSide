package cn.levanll.stoveside.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 食谱模板。模板与实例分离：同一份食谱可以开多个 Cook，互不影响。
 * 改名/改步骤只影响模板，不追溯已开始的实例。
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String = "",
    /** 内置示例食谱标记，卸载重装不重复生成 */
    @ColumnInfo(name = "is_builtin") val isBuiltin: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

/** 模板节点。时长以秒为单位，0 表示"不计时，仅记录"。 */
@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("recipe_id"), Index(value = ["recipe_id", "order_no"], unique = true)],
)
data class Step(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "order_no") val orderNo: Int,
    val title: String,
    /** 该步默认时长（秒）；0 = 不计时 */
    @ColumnInfo(name = "duration_sec") val durationSec: Int = 0,
    /** 模板默认备注，实例可覆盖且不回写 */
    val note: String = "",
)

/**
 * 一次烹饪实例（同一食谱的第 N 份）。
 * label 用于首页区分「红烧肉 #1 / #2」，默认按创建顺序编号。
 */
@Entity(
    tableName = "cooks",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("recipe_id"), Index("status")],
)
data class Cook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val label: String,
    /** running / done / archived */
    val status: String = "running",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
)

/**
 * 实例节点。标题/时长/备注在开火时从模板拷贝一份，
 * 之后模板怎么改都不影响这次烹饪——这是"可追溯"的前提。
 */
@Entity(
    tableName = "cook_steps",
    foreignKeys = [
        ForeignKey(
            entity = Cook::class,
            parentColumns = ["id"],
            childColumns = ["cook_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("cook_id"), Index(value = ["cook_id", "order_no"], unique = true)],
)
data class CookStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cook_id") val cookId: Long,
    @ColumnInfo(name = "step_id") val stepId: Long,
    @ColumnInfo(name = "order_no") val orderNo: Int,
    val title: String,
    @ColumnInfo(name = "duration_sec") val durationSec: Int,
    /** 实例备注：用户在这次烹饪里改的，不回写模板 */
    val note: String = "",
    /** pending / running / done / skipped */
    val status: String = "pending",
    @ColumnInfo(name = "started_at") val startedAt: Long? = null,
    /** 到期墙钟时间；0 或 null 表示不计时的步骤 */
    @ColumnInfo(name = "due_at") val dueAt: Long? = null,
    /** 实际勾选完成时间，用于记录与导出 */
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
)

/** 带模板信息的实例视图，首页列表用 */
data class CookWithRecipe(
    val cook: Cook,
    val recipe: Recipe,
    val totalSteps: Int,
    val doneSteps: Int,
)
