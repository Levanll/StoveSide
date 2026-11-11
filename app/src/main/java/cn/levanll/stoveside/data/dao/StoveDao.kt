package cn.levanll.stoveside.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.levanll.stoveside.data.entity.Cook
import cn.levanll.stoveside.data.entity.CookStep
import cn.levanll.stoveside.data.entity.CookWithRecipe
import cn.levanll.stoveside.data.entity.Recipe
import cn.levanll.stoveside.data.entity.Step
import kotlinx.coroutines.flow.Flow

@Dao
interface StoveDao {

    // ---- 模板 ----

    @Query("SELECT * FROM recipes ORDER BY updated_at DESC")
    fun observeRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes ORDER BY updated_at DESC")
    suspend fun recipes(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun recipe(id: Long): Recipe?

    @Insert
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Query("SELECT * FROM steps WHERE recipe_id = :recipeId ORDER BY order_no")
    fun observeSteps(recipeId: Long): Flow<List<Step>>

    @Query("SELECT * FROM steps WHERE recipe_id = :recipeId ORDER BY order_no")
    suspend fun steps(recipeId: Long): List<Step>

    @Query("SELECT * FROM steps ORDER BY recipe_id, order_no")
    suspend fun allSteps(): List<Step>

    @Insert
    suspend fun insertSteps(steps: List<Step>): List<Long>

    @Query("DELETE FROM steps WHERE recipe_id = :recipeId")
    suspend fun deleteStepsOf(recipeId: Long)

    // ---- 实例 ----

    @Query(
        """
        SELECT c.*, r.name AS recipeName FROM cooks c
        JOIN recipes r ON r.id = c.recipe_id
        WHERE c.status != 'archived'
        ORDER BY c.created_at DESC
        """
    )
    fun observeCooks(): Flow<List<Cook>>

    @Query("SELECT * FROM cooks WHERE id = :id")
    suspend fun cook(id: Long): Cook?

    @Query("SELECT * FROM cooks WHERE status != 'archived' ORDER BY created_at")
    suspend fun activeCooks(): List<Cook>

    @Query("SELECT * FROM cooks ORDER BY created_at")
    suspend fun allCooks(): List<Cook>

    @Query("SELECT COUNT(*) FROM cooks WHERE recipe_id = :recipeId AND status != 'archived'")
    suspend fun activeCookCount(recipeId: Long): Int

    @Insert
    suspend fun insertCook(cook: Cook): Long

    @Update
    suspend fun updateCook(cook: Cook)

    @Query("DELETE FROM cooks WHERE id = :id")
    suspend fun deleteCook(id: Long)

    @Query("SELECT * FROM cook_steps WHERE cook_id = :cookId ORDER BY order_no")
    fun observeCookSteps(cookId: Long): Flow<List<CookStep>>

    @Query("SELECT * FROM cook_steps WHERE cook_id = :cookId ORDER BY order_no")
    suspend fun cookSteps(cookId: Long): List<CookStep>

    @Query("SELECT * FROM cook_steps ORDER BY cook_id, order_no")
    suspend fun allCookSteps(): List<CookStep>

    @Query("SELECT * FROM cook_steps WHERE status = 'running'")
    suspend fun runningSteps(): List<CookStep>

    @Insert
    suspend fun insertCookSteps(steps: List<CookStep>): List<Long>

    @Update
    suspend fun updateCookStep(step: CookStep)

    @Update
    suspend fun updateCookSteps(steps: List<CookStep>)

    // ---- 导入导出 ----

    @Query("DELETE FROM cooks")
    suspend fun clearCooks()

    @Query("DELETE FROM cook_steps")
    suspend fun clearCookSteps()

    @Query("DELETE FROM steps")
    suspend fun clearSteps()

    @Query("DELETE FROM recipes")
    suspend fun clearRecipes()

    @Insert
    suspend fun insertRecipes(recipes: List<Recipe>): List<Long>

    @Insert
    suspend fun insertCooks(cooks: List<Cook>): List<Long>

    @Transaction
    suspend fun replaceAll(
        recipes: List<Recipe>,
        steps: List<Step>,
        cooks: List<Cook>,
        cookSteps: List<CookStep>,
    ) {
        clearCookSteps()
        clearCooks()
        clearSteps()
        clearRecipes()
        insertRecipes(recipes)
        insertSteps(steps)
        insertCooks(cooks)
        insertCookSteps(cookSteps)
    }
}
