package cn.levanll.stoveside.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.levanll.stoveside.StoveApp
import cn.levanll.stoveside.data.StoveRepository
import cn.levanll.stoveside.data.entity.Cook
import cn.levanll.stoveside.data.entity.CookStep
import cn.levanll.stoveside.data.entity.Recipe
import cn.levanll.stoveside.data.entity.Step
import cn.levanll.stoveside.domain.RecipeParser
import cn.levanll.stoveside.i18n.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 单步运行态：数据来自 Room，倒计时由 ticker 派生，不落库 */
data class StepView(val step: CookStep, val remainingMs: Long)

data class CookView(
    val cook: Cook,
    val recipeName: String,
    val steps: List<StepView>,
) {
    val doneCount: Int get() = steps.count { it.step.status == "done" || it.step.status == "skipped" }
    val totalCount: Int get() = steps.size
    val runningCount: Int get() = steps.count { it.step.status == "running" }
    /** 最短剩余时间，用于首页"最紧急的排最前" */
    val soonest: Long
        get() = steps.filter { it.step.status == "running" && it.step.dueAt != null }
            .minOfOrNull { it.remainingMs } ?: Long.MAX_VALUE
}

class StoveViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: StoveRepository = (app as StoveApp).repository

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _cooks = MutableStateFlow<List<CookView>>(emptyList())
    val cooks: StateFlow<List<CookView>> = _cooks.asStateFlow()

    private val _draft = MutableStateFlow<List<Step>>(emptyList())
    val draft: StateFlow<List<Step>> = _draft.asStateFlow()

    private val _parsePreview = MutableStateFlow<RecipeParser.ParsedRecipe?>(null)
    val parsePreview: StateFlow<RecipeParser.ParsedRecipe?> = _parsePreview.asStateFlow()

    init {
        viewModelScope.launch {
            repo.recipesFlow().collect { list -> _recipes.value = list }
        }
        // 单一刷新循环：数据量很小（几十条），用快照重建比层层 Flow 组合更好读，
        // 也保证"数据库是唯一真相"——UI 不持有任何独立的计时状态。
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(if (hasRunning()) 500L else 2000L)
            }
        }
    }

    private var lastSnapshot: List<CookView> = emptyList()

    private fun hasRunning(): Boolean = lastSnapshot.any { it.runningCount > 0 }

    private suspend fun refresh() = withContext(Dispatchers.IO) {
        val cooks = repo.cooks().filter { it.status != "archived" }
        val names = repo.recipes().associate { it.id to it.name }
        val now = System.currentTimeMillis()
        val views = cooks.map { cook ->
            CookView(
                cook = cook,
                recipeName = names[cook.recipeId] ?: "",
                steps = repo.cookStepsOf(cook.id).map { s ->
                    StepView(s, (s.dueAt ?: 0L) - now)
                },
            )
        }.sortedWith(
            compareBy<CookView> { if (it.runningCount > 0) 0 else 1 }
                .thenBy { it.soonest }
                .thenByDescending { it.cook.createdAt }
        )
        lastSnapshot = views
        _cooks.value = views
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            refresh()
        }
    }

    // ---------- 模板 ----------

    fun addDraftStep(step: Step) { _draft.value = _draft.value + step }

    fun removeDraftStep(index: Int) {
        _draft.value = _draft.value.toMutableList().also { if (index in it.indices) it.removeAt(index) }
    }

    fun moveDraftStep(from: Int, to: Int) {
        val list = _draft.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _draft.value = list
    }

    fun replaceDraft(list: List<Step>) { _draft.value = list }

    fun clearDraft() {
        _draft.value = emptyList()
        _parsePreview.value = null
    }

    fun previewParse(text: String, fallbackName: String) {
        _parsePreview.value = RecipeParser.parse(text, fallbackName)
    }

    fun applyPreview() {
        val preview = _parsePreview.value ?: return
        _draft.value = preview.steps.mapIndexed { i, p ->
            Step(recipeId = 0, orderNo = i, title = p.title, durationSec = p.durationSec, note = p.note)
        }
        _parsePreview.value = null
    }

    fun saveRecipe(recipe: Recipe, onDone: () -> Unit = {}) {
        val steps = _draft.value
        launchAction {
            repo.saveRecipe(recipe, steps)
            _draft.value = emptyList()
            onDone()
        }
    }

    fun deleteRecipe(recipe: Recipe) = launchAction { repo.deleteRecipe(recipe.id) }

    // ---------- 实例 ----------

    fun startCook(recipe: Recipe) = launchAction { repo.startCook(recipe) }

    fun deleteCook(cook: Cook) = launchAction { repo.deleteCook(cook.id) }

    /** 勾选完成：当前步 done，并自动点亮下一步 */
    fun completeStep(cookId: Long, step: CookStep) = launchAction {
        val now = System.currentTimeMillis()
        val list = repo.cookStepsOf(cookId).toMutableList()
        val idx = list.indexOfFirst { it.id == step.id }
        if (idx < 0) return@launchAction
        list[idx] = list[idx].copy(status = "done", finishedAt = now)
        list.getOrNull(idx + 1)?.let { next ->
            if (next.status == "pending") list[idx + 1] = activate(next, now)
        }
        repo.updateCookSteps(list)
        val allDone = list.all { it.status == "done" || it.status == "skipped" }
        if (allDone) {
            repo.cook(cookId)?.let { repo.updateCook(it.copy(status = "done", finishedAt = now)) }
        }
    }

    fun startStep(step: CookStep) = launchAction {
        repo.updateCookStep(activate(step, System.currentTimeMillis()))
    }

    fun pauseStep(step: CookStep) = launchAction {
        val remain = ((step.dueAt ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
        val sec = ((remain + 999) / 1000).toInt()
        repo.updateCookStep(
            step.copy(
                status = "pending",
                dueAt = null,
                startedAt = null,
                durationSec = if (step.durationSec > 0) maxOf(sec, 1) else 0,
            )
        )
    }

    fun addOneMinute(step: CookStep) = launchAction {
        val due = step.dueAt
        repo.updateCookStep(
            if (due == null) {
                step.copy(durationSec = step.durationSec + 60)
            } else {
                step.copy(dueAt = due + 60_000L, durationSec = step.durationSec + 60)
            }
        )
    }

    fun updateNote(step: CookStep, note: String) = launchAction {
        repo.updateCookStep(step.copy(note = note))
    }

    private fun activate(step: CookStep, now: Long): CookStep {
        val due = if (step.durationSec > 0) now + step.durationSec * 1000L else null
        return step.copy(status = "running", startedAt = now, dueAt = due)
    }

    fun toggleLang(): I18n.Lang = I18n.toggle()
}