package cn.levanll.stoveside.domain

/**
 * 把用户粘贴的自然语言食谱拆成带时长、带备注的步骤。
 *
 * 设计取向：宁可少解析一次时间，也不要解析错。
 *  - 时间单位认「秒/分钟/小时/min/sec/h」，中文数字与阿拉伯数字都支持；
 *  - 「一个半小时」「1个半小时」「半小时」这类口语写法单独处理；
 *  - 一行里有多个时间时取第一个作为该步时长，其余并入备注；
 *  - 完全解析不出的文本不会被丢弃，整行作为标题或备注保留。
 */
object RecipeParser {

    data class ParsedStep(val title: String, val durationSec: Int, val note: String)

    data class ParsedRecipe(val name: String, val steps: List<ParsedStep>)

    private val bulletPrefix = Regex("^\\s*(?:[0-9]{1,2}[.、)]|[-*•·]|第[一二三四五六七八九十]+步)\\s*")

    /** 「X 个半小时」/「半小时」：必须排在通用规则之前，否则「半」会被当成 0.5 小时。 */
    private val halfHourPattern = Regex(
        "([0-9]{1,3}|[一二两三四五六七八九十]+)?\\s*个?\\s*半\\s*个?\\s*(?:小时|hours?|hrs?)",
        RegexOption.IGNORE_CASE,
    )

    private val timePattern = Regex(
        "([0-9]{1,3}(?:\\.[0-9])?|[一二两三四五六七八九十半]+)\\s*个?\\s*" +
            "(小时半|个小时|小时|分钟|分|秒钟|秒|hours?|hrs?|minutes?|mins?|seconds?|secs?|h(?![a-z]))",
        RegexOption.IGNORE_CASE,
    )

    private val noiseLine = Regex("^\\s*(?:小贴士|提示|注意|tips?:?|note:?)\\s*$", RegexOption.IGNORE_CASE)

    private val cnDigits = mapOf(
        '零' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
    )

    fun parse(raw: String, fallbackName: String): ParsedRecipe {
        val lines = raw.replace("\r\n", "\n").split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ParsedRecipe(fallbackName, listOf(ParsedStep(fallbackName, 0, raw.trim())))
        }

        // 第一行若没有时间也没有分步编号，视为食谱名
        var startIndex = 0
        var name = fallbackName
        val head = lines.first()
        if (!hasTime(head) && !bulletPrefix.containsMatchIn(head) && head.length <= 24) {
            name = head
            startIndex = 1
        }

        val steps = mutableListOf<ParsedStep>()
        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (noiseLine.matches(line)) continue
            val body = line.replace(bulletPrefix, "").trim()
            if (body.isEmpty()) continue
            steps += parseLine(body)
        }

        if (steps.isEmpty()) steps += ParsedStep(fallbackName, 0, raw.trim())
        return ParsedRecipe(name, steps)
    }

    private fun hasTime(text: String): Boolean =
        halfHourPattern.containsMatchIn(text) || timePattern.containsMatchIn(text)

    private fun parseLine(body: String): ParsedStep {
        // 先试「X 个半小时」，避免被通用规则拆成「半 + 小时」
        halfHourPattern.find(body)?.let { m ->
            val base = m.groupValues[1].ifBlank { "0" }
            val baseValue = parseNumber(base) ?: 0.0
            return build(body, m.range.first, m.range.last + 1, (baseValue * 3600 + 1800).toInt())
        }
        val m = timePattern.find(body) ?: return ParsedStep(body.take(60), 0, "")
        return build(body, m.range.first, m.range.last + 1, toSeconds(m.groupValues[1], m.groupValues[2]))
    }

    private fun build(body: String, start: Int, end: Int, seconds: Int): ParsedStep {
        val before = body.substring(0, start).trim().trimEnd('，', ',', '、', '。', '：', ':')
        val after = body.substring(end).trim().trimStart('，', ',', '、', '。', '：', ':')
        return ParsedStep(before.ifBlank { body.take(60) }, seconds, after)
    }

    /** 「1.5 小时」「十五分钟」「半小时」都要能算对；无法解析返回 0（= 不计时）。 */
    fun toSeconds(amount: String, unit: String): Int {
        val value = parseNumber(amount) ?: return 0
        val seconds = when (unit.lowercase()) {
            "小时", "个小时", "hour", "hours", "hr", "hrs", "h" -> value * 3600
            "小时半" -> value * 3600 + 1800
            "分钟", "分", "minute", "minutes", "min", "mins" -> value * 60
            "秒钟", "秒", "second", "seconds", "sec", "secs" -> value
            else -> 0.0
        }
        // 单步超过 12 小时基本是误判，直接丢弃时长，避免出现荒谬的倒计时
        return if (seconds > 43200) 0 else seconds.toInt()
    }

    fun parseNumber(text: String): Double? {
        text.toDoubleOrNull()?.let { return it }
        if (text == "半") return 0.5
        if (text.isEmpty()) return null
        if (text.any { it !in cnDigits && it != '十' && it != '半' }) return null

        var total = 0.0
        var section = 0.0
        var hasValue = false
        for (c in text) {
            when {
                c == '十' -> {
                    section = if (section == 0.0) 10.0 else section * 10
                    hasValue = true
                }
                c == '半' -> {
                    section += 0.5
                    hasValue = true
                }
                else -> {
                    section += (cnDigits[c] ?: 0).toDouble()
                    hasValue = true
                }
            }
        }
        total += section
        return if (hasValue) total else null
    }
}