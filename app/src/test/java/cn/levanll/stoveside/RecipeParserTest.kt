package cn.levanll.stoveside

import cn.levanll.stoveside.domain.RecipeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeParserTest {

    @Test
    fun `parses chinese minutes`() {
        val r = RecipeParser.parse("红烧肉\n1. 五花肉切块，冷水下锅焯5分钟\n2. 炒糖色，小火10分钟", "fallback")
        assertEquals("红烧肉", r.name)
        assertEquals(2, r.steps.size)
        assertEquals(5 * 60, r.steps[0].durationSec)
        assertEquals(10 * 60, r.steps[1].durationSec)
        assertTrue(r.steps[0].title.contains("五花肉"))
    }

    @Test
    fun `parses english units`() {
        val r = RecipeParser.parse("Pancakes\n- Whisk the batter 2 mins\n- Fry on medium heat for 90 seconds", "p")
        assertEquals(2, r.steps.size)
        assertEquals(120, r.steps[0].durationSec)
        assertEquals(90, r.steps[1].durationSec)
    }

    @Test
    fun `chinese numerals`() {
        assertEquals(15.0, RecipeParser.parseNumber("十五")!!, 0.001)
        assertEquals(20.0, RecipeParser.parseNumber("二十")!!, 0.001)
        assertEquals(0.5, RecipeParser.parseNumber("半")!!, 0.001)
        assertEquals(1800, RecipeParser.toSeconds("半", "小时"))
    }

    @Test
    fun `unparseable text is kept as a note`() {
        val r = RecipeParser.parse("随便写点东西没有时间", "菜")
        assertEquals(1, r.steps.size)
        assertEquals(0, r.steps[0].durationSec)
        assertTrue(r.steps[0].title.isNotBlank())
    }

    @Test
    fun `absurd duration dropped`() {
        assertEquals(0, RecipeParser.toSeconds("99", "小时"))
    }
    @Test
    fun `version comparison`() {
        assertEquals(1, cn.levanll.stoveside.update.UpdateChecker.compareVersions("1.0.1", "1.0.0"))
        assertEquals(-1, cn.levanll.stoveside.update.UpdateChecker.compareVersions("1.0.0", "1.1.0"))
        assertEquals(0, cn.levanll.stoveside.update.UpdateChecker.compareVersions("v1.0.0", "1.0.0"))
        assertEquals(1, cn.levanll.stoveside.update.UpdateChecker.compareVersions("1.2", "1.1.9"))
    }
    @Test
    fun `half hour with counter`() {
        // 「一个半小时」= 90 分钟，不是 30 分钟
        assertEquals(5400, RecipeParser.parse("炖一个半小时", "x").steps[0].durationSec)
        assertEquals(5400, RecipeParser.parse("1个半小时", "x").steps[0].durationSec)
        assertEquals(1800, RecipeParser.parse("半小时", "x").steps[0].durationSec)
        // 「两小时半」= 2.5 小时 = 9000 秒，与「一个半小时」不是一回事
        assertEquals(9000, RecipeParser.parse("两小时半", "x").steps[0].durationSec)
    }

    @Test
    fun `english single h unit`() {
        assertEquals(3600, RecipeParser.parse("bake 1h", "x").steps[0].durationSec)
        assertEquals(5400, RecipeParser.parse("simmer 1.5h", "x").steps[0].durationSec)
    }
}
