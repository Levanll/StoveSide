package cn.levanll.stoveside.backup

/**
 * 导出文件结构。schemaVersion 是唯一兼容性判断依据：
 * 导入时按它分支迁移，未知的高版本直接拒绝而不是"尽力而为"。
 */
data class BackupFile(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val app: String = "stoveside",
    val exportedAt: Long = System.currentTimeMillis(),
    val recipes: List<BackupRecipe> = emptyList(),
    val cooks: List<BackupCook> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
) {
    companion object {
        /** v1：首版，Recipe/Step/Cook/CookStep 四表全量 */
        const val CURRENT_SCHEMA = 1
    }
}

data class BackupRecipe(
    val id: Long,
    val name: String,
    val note: String = "",
    val createdAt: Long = 0,
    val steps: List<BackupStep> = emptyList(),
)

data class BackupStep(
    val id: Long,
    val orderNo: Int,
    val title: String,
    val durationSec: Int = 0,
    val note: String = "",
)

data class BackupCook(
    val id: Long,
    val recipeId: Long,
    val label: String,
    val status: String = "running",
    val createdAt: Long = 0,
    val finishedAt: Long? = null,
    val steps: List<BackupCookStep> = emptyList(),
)

data class BackupCookStep(
    val stepId: Long,
    val orderNo: Int,
    val title: String,
    val durationSec: Int = 0,
    val note: String = "",
    val status: String = "pending",
    val startedAt: Long? = null,
    val dueAt: Long? = null,
    val finishedAt: Long? = null,
)

data class BackupSettings(
    val lang: String = "zh",
)
