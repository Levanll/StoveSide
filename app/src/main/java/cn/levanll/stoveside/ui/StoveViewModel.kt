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
import cn.levanll.stoveside.domain.TimeText
import cn.levanll.stoveside.i18n.I18n
import cn.levanll.stoveside.service.AlarmService
import cn.levanll.stoveside.service.TimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 单步运行态：数据来自 Room，倒计时由 ticker 派生，不落库 */
data class StepView(val step: CookStep, val remainingMs: Long) {
    /**
     * 已过期多久（毫秒）。0 表示还没到点。
     * 归零后 dueAt 停在过去不会自动清理，界面需要靠它区分
     * 「刚归零」和「已经过去三分钟」，否则两者都是红色的 00:00。
     */
    val overdueMs: Long get() = if (step.status == "running" && remainingMs < 0) -remainingMs else 0L
    val isOverdue: Boolean get() = overdueMs > 0L
}

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

/**
 * 到点弹窗的内容。试听时 [step] 为 null：试听不关联任何步骤，也就不该写库。
 * 一次只弹一条，[extraCount] 是同一轮里还有几个也到点了。
 */
data class AlarmPrompt(
    val title: String,
    val subtitle: String,
    val overdueMs: Long,
    val extraCount: Int,
    val step: CookStep?,
)

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

    /** 到点弹窗；null 表示没有待确认的到点 */
    private val _alarmPrompt = MutableStateFlow<AlarmPrompt?>(null)
    val alarmPrompt: StateFlow<AlarmPrompt?> = _alarmPrompt.asStateFlow()

    /**
     * 已确认过的 (stepId, dueAt)，只在内存里。进程重开后过期步骤会再次提醒——
     * 那个步骤客观上还没处理完。键里带 dueAt，所以「+1 分钟」后重新到点会再弹。
     */
    private val ackedOverdue = mutableSetOf<Pair<Long, Long>>()

    /** 试听不关联步骤，用一个独立开关驱动同一个弹窗 */
    private var previewing = false

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
        deriveAlarmPrompt(views)
    }

    private fun deriveAlarmPrompt(views: List<CookView>) {
        if (previewing) {
            _alarmPrompt.value = AlarmPrompt(
                title = I18n.t("试听：到点了", "Test: timer done"),
                subtitle = I18n.t("这是一次试听，点「知道了」停止", "This is a test alarm; tap \"Got it\" to stop"),
                overdueMs = 0L,
                extraCount = 0,
                step = null,
            )
            return
        }
        val waiting = views.flatMap { cook ->
            cook.steps.mapNotNull { sv ->
                val due = sv.step.dueAt ?: return@mapNotNull null
                if (!sv.isOverdue || (sv.step.id to due) in ackedOverdue) null else sv to cook.recipeName
            }
        }
        val head = waiting.firstOrNull()
        _alarmPrompt.value = if (head == null) {
            null
        } else {
            AlarmPrompt(
                title = head.first.step.title,
                subtitle = head.second,
                overdueMs = head.first.overdueMs,
                extraCount = waiting.size - 1,
                step = head.first.step,
            )
        }
    }

    /** 「知道了」：停铃、记下这一轮到点，步骤状态不动——它客观上还在跑 */
    fun ackAlarm() {
        val prompt = _alarmPrompt.value ?: return
        val step = prompt.step
        if (step != null) step.dueAt?.let { ackedOverdue += step.id to it }
        previewing = false
        AlarmService.stop(getApplication())
        _alarmPrompt.value = null
        // 同一轮里若还有别的到点步骤，立刻重算，让下一条接上
        viewModelScope.launch { refresh() }
    }

    /** 试听：走和真实到点同一条弹窗链路，只是没有步骤可写 */
    fun tryAlarm() {
        AlarmService.start(
            getApplication(),
            I18n.t("试听：到点了", "Test: timer done"),
            I18n.t("这是一次试听，点「知道了」停止", "This is a test alarm; tap \"Got it\" to stop"),
        )
        previewing = true
        viewModelScope.launch { refresh() }
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

    fun deleteCook(cook: Cook) = launchAction {
        repo.deleteCook(cook.id)
        // 删掉实例等于结束这锅，铃声不该继续响
        AlarmService.stop(getApplication())
    }

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
        TimerService.start(getApplication())
        repo.updateCookSteps(list)
        val allDone = list.all { it.status == "done" || it.status == "skipped" }
        if (allDone) {
            repo.cook(cookId)?.let { repo.updateCook(it.copy(status = "done", finishedAt = now)) }
        }
        // 用户已经把这步处理完了，铃声没有继续响的理由。
        // 不在这里停的话，只能靠点「+1 分钟」或点通知才停得掉。
        AlarmService.stop(getApplication())
    }

    fun startStep(step: CookStep) = launchAction {
        repo.updateCookStep(activate(step, System.currentTimeMillis()))
        TimerService.start(getApplication())
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
        // 暂停也是「我已经知道了」，同样要停铃
        AlarmService.stop(getApplication())
    }

    /**
     * 加一分钟。
     *
     * 基准必须是 max(now, due)：到点后 dueAt 会停在过去（本 App 到点只提醒、不改状态），
     * 若继续以旧 dueAt 为基准，加上的 60 秒会被「已过期时长」吃掉——
     * 归零 40 秒后点一次只加 20 秒，归零 60 秒后点等于没反应。
     * 反过来，未到点时若直接用 now 当基准，会把「还剩 5 分钟」重置成 60 秒，
     * 所以取两者较晚的一个，两种场景才对。
     */
    fun addOneMinute(step: CookStep) = launchAction {
        val due = step.dueAt
        if (due == null) {
            repo.updateCookStep(step.copy(durationSec = step.durationSec + 60))
            return@launchAction
        }
        val newDue = TimeText.extendDue(dueAt = due, now = System.currentTimeMillis())
        repo.updateCookStep(
            step.copy(dueAt = newDue, durationSec = step.durationSec + 60)
        )
        // 用户跑到这一步点「+1 分钟」，等于已经知道到点了，铃声不该继续在耳边叫
        AlarmService.stop(getApplication())
        // 延期后旧的一轮到点作废：放开记忆，下次再归零才会重新弹窗
        ackedOverdue.removeAll(ackedOverdue.filter { it.first == step.id }.toSet())
        _alarmPrompt.value = null
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