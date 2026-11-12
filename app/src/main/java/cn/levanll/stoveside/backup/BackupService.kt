package cn.levanll.stoveside.backup

import android.content.Context
import android.net.Uri
import cn.levanll.stoveside.data.StoveRepository
import cn.levanll.stoveside.data.entity.Cook
import cn.levanll.stoveside.data.entity.CookStep
import cn.levanll.stoveside.data.entity.Recipe
import cn.levanll.stoveside.data.entity.Step
import cn.levanll.stoveside.data.StoveDatabase
import cn.levanll.stoveside.i18n.I18n
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ImportResult {
    data class Success(val recipes: Int, val cooks: Int) : ImportResult()
    data class Failure(val reason: String) : ImportResult()
}

/**
 * 导入导出的两条硬规则：
 *  1. 导入前先把当前库全量导出到 backups/ 自动快照，失败即还原；
 *  2. 只接受 schemaVersion <= CURRENT_SCHEMA，更高版本明确拒绝，不做"尽力解析"。
 */
class BackupService(
    private val context: Context,
    private val repo: StoveRepository,
    private val dao: cn.levanll.stoveside.data.dao.StoveDao,
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun backupDir(): File = File(context.filesDir, "backups").apply { mkdirs() }

    suspend fun exportTo(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload()
            val json = gson.toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("cannot open output stream")
            payload.recipes.size
        }
    }

    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull() ?: return@withContext ImportResult.Failure("unreadable")
        val parsed = runCatching { gson.fromJson(text, BackupFile::class.java) }
            .getOrElse { e ->
                return@withContext ImportResult.Failure(
                    if (e is JsonSyntaxException) "malformed" else (e.message ?: "parse error")
                )
            }
        if (parsed.schemaVersion > BackupFile.CURRENT_SCHEMA) {
            return@withContext ImportResult.Failure("schema_too_new")
        }
        // 高危：先快照，任何异常都还原
        val snapshot = runCatching { autoSnapshot() }.getOrElse {
            return@withContext ImportResult.Failure("snapshot_failed")
        }
        runCatching {
            dao.replaceAll(
                recipes = parsed.recipes.map { Recipe(it.id, it.name, it.note, false, it.createdAt, it.createdAt) },
                steps = parsed.recipes.flatMap { r ->
                    r.steps.map { Step(it.id, r.id, it.orderNo, it.title, it.durationSec, it.note) }
                },
                cooks = parsed.cooks.map { Cook(it.id, it.recipeId, it.label, it.status, it.createdAt, it.finishedAt) },
                cookSteps = parsed.cooks.flatMap { c ->
                    c.steps.map {
                        CookStep(
                            id = 0, cookId = c.id, stepId = it.stepId, orderNo = it.orderNo,
                            title = it.title, durationSec = it.durationSec, note = it.note,
                            status = it.status, startedAt = it.startedAt, dueAt = it.dueAt,
                            finishedAt = it.finishedAt,
                        )
                    }
                },
            )
        }.onFailure {
            runCatching { restoreFromSnapshot(snapshot) }
            return@withContext ImportResult.Failure(it.message ?: "write_failed")
        }
        // 语言设置也要恢复：导出时写了 settings.lang，不读回来就是个死字段
        runCatching {
            I18n.set(if (parsed.settings.lang == "en") I18n.Lang.EN else I18n.Lang.ZH)
        }
        ImportResult.Success(parsed.recipes.size, parsed.cooks.size)
    }

    /** 覆盖写库前自动留档，文件名带时间戳，永不互相覆盖。 */
    suspend fun autoSnapshot(): File = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(backupDir(), "auto-$stamp.json")
        file.writeText(gson.toJson(buildPayload()), Charsets.UTF_8)
        pruneSnapshots(keep = 20)
        file
    }

    suspend fun restoreFromSnapshot(file: File): ImportResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { gson.fromJson(file.readText(Charsets.UTF_8), BackupFile::class.java) }
            .getOrNull() ?: return@withContext ImportResult.Failure("snapshot_unreadable")
        runCatching {
            dao.replaceAll(
                recipes = parsed.recipes.map { Recipe(it.id, it.name, it.note, false, it.createdAt, it.createdAt) },
                steps = parsed.recipes.flatMap { r ->
                    r.steps.map { Step(it.id, r.id, it.orderNo, it.title, it.durationSec, it.note) }
                },
                cooks = parsed.cooks.map { Cook(it.id, it.recipeId, it.label, it.status, it.createdAt, it.finishedAt) },
                cookSteps = parsed.cooks.flatMap { c ->
                    c.steps.map {
                        CookStep(
                            id = 0, cookId = c.id, stepId = it.stepId, orderNo = it.orderNo,
                            title = it.title, durationSec = it.durationSec, note = it.note,
                            status = it.status, startedAt = it.startedAt, dueAt = it.dueAt,
                            finishedAt = it.finishedAt,
                        )
                    }
                },
            )
        }.getOrElse { return@withContext ImportResult.Failure(it.message ?: "restore_failed") }
        runCatching {
            I18n.set(if (parsed.settings.lang == "en") I18n.Lang.EN else I18n.Lang.ZH)
        }
        ImportResult.Success(parsed.recipes.size, parsed.cooks.size)
    }

    fun listSnapshots(): List<File> =
        backupDir().listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    private fun pruneSnapshots(keep: Int) {
        listSnapshots().drop(keep).forEach { runCatching { it.delete() } }
    }

    private suspend fun buildPayload(): BackupFile {
        val recipes = repo.recipes()
        val steps = repo.allSteps()
        val cooks = repo.cooks()
        val cookSteps = repo.allCookSteps()
        return BackupFile(
            recipes = recipes.map { r ->
                BackupRecipe(
                    id = r.id, name = r.name, note = r.note, createdAt = r.createdAt,
                    steps = steps.filter { it.recipeId == r.id }.map {
                        BackupStep(it.id, it.orderNo, it.title, it.durationSec, it.note)
                    },
                )
            },
            cooks = cooks.map { c ->
                BackupCook(
                    id = c.id, recipeId = c.recipeId, label = c.label, status = c.status,
                    createdAt = c.createdAt, finishedAt = c.finishedAt,
                    steps = cookSteps.filter { it.cookId == c.id }.map {
                        BackupCookStep(
                            it.stepId, it.orderNo, it.title, it.durationSec, it.note,
                            it.status, it.startedAt, it.dueAt, it.finishedAt,
                        )
                    },
                )
            },
            settings = BackupSettings(lang = if (I18n.lang == I18n.Lang.EN) "en" else "zh"),
        )
    }
}
