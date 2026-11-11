package cn.levanll.stoveside.data

import cn.levanll.stoveside.data.dao.StoveDao
import cn.levanll.stoveside.data.entity.Cook
import cn.levanll.stoveside.data.entity.CookStep
import cn.levanll.stoveside.data.entity.Recipe
import cn.levanll.stoveside.data.entity.Step
import kotlinx.coroutines.flow.Flow

class StoveRepository(private val dao: StoveDao) {

    fun recipesFlow(): Flow<List<Recipe>> = dao.observeRecipes()
    fun cookStepsFlow(cookId: Long): Flow<List<CookStep>> = dao.observeCookSteps(cookId)

    suspend fun recipes(): List<Recipe> = dao.recipes()
    suspend fun allSteps(): List<Step> = dao.allSteps()
    suspend fun stepsOf(recipeId: Long): List<Step> = dao.steps(recipeId)
    suspend fun cooks(): List<Cook> = dao.allCooks()
    suspend fun cook(id: Long): Cook? = dao.cook(id)
    suspend fun allCookSteps(): List<CookStep> = dao.allCookSteps()
    suspend fun cookStepsOf(cookId: Long): List<CookStep> = dao.cookSteps(cookId)
    suspend fun runningSteps(): List<CookStep> = dao.runningSteps()

    suspend fun saveRecipe(recipe: Recipe, steps: List<Step>): Long {
        val now = System.currentTimeMillis()
        val id = if (recipe.id == 0L) {
            dao.insertRecipe(recipe.copy(createdAt = now, updatedAt = now))
        } else {
            dao.updateRecipe(recipe.copy(updatedAt = now))
            dao.deleteStepsOf(recipe.id)
            recipe.id
        }
        dao.insertSteps(steps.mapIndexed { i, s -> s.copy(id = 0, recipeId = id, orderNo = i) })
        return id
    }

    suspend fun deleteRecipe(id: Long) = dao.deleteRecipe(id)

    /**
     * 开一份新的烹饪实例：把模板步骤整体拷贝一份。
     * 拷贝而非引用，是为了让"这次烹饪"独立于模板之后的任何修改。
     */
    suspend fun startCook(recipe: Recipe): Long {
        val steps = dao.steps(recipe.id)
        val seq = dao.activeCookCount(recipe.id) + 1
        val cookId = dao.insertCook(Cook(recipeId = recipe.id, label = "#$seq"))
        dao.insertCookSteps(
            steps.map { s ->
                CookStep(
                    cookId = cookId,
                    stepId = s.id,
                    orderNo = s.orderNo,
                    title = s.title,
                    durationSec = s.durationSec,
                    note = s.note,
                )
            }
        )
        return cookId
    }

    suspend fun deleteCook(id: Long) = dao.deleteCook(id)
    suspend fun updateCook(cook: Cook) = dao.updateCook(cook)
    suspend fun updateCookStep(step: CookStep) = dao.updateCookStep(step)
    suspend fun updateCookSteps(steps: List<CookStep>) = dao.updateCookSteps(steps)
}
