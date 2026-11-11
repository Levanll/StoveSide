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
}