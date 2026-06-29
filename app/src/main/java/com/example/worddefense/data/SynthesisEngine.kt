package com.example.worddefense.data

object SynthesisEngine {
    // List of all true generals and their recipes
    val generalsList = listOf(
        General("赵云", listOf("赵", "云"), "七进七出", "物理群攻：闪烁对路径敌人造成多段刺击", 45, 2.5f, 1.8f),
        General("关羽", listOf("关", "羽"), "单刀赴会", "物理爆发：重劈前方扇形敌人并震退", 65, 2.0f, 0.9f),
        General("张飞", listOf("张", "飞"), "喝断狂澜", "控制坦克：狂吼造成大范围眩晕与嘲讽", 50, 2.2f, 1.0f),
        General("刘备", listOf("刘", "备"), "惟贤惟德", "团队辅助：治疗周围友方并提升其攻速", 30, 3.0f, 1.2f),
        General("诸葛亮", listOf("诸葛", "亮"), "东风雷霆", "法术轰炸：召唤雷云和风墙，造成大范围落雷与减速", 75, 4.0f, 0.8f),
        General("曹操", listOf("曹", "操"), "枭雄之志", "团队增益：提升周围所有字塔的暴击和攻击力", 35, 3.0f, 1.1f),
        General("孙权", listOf("孙", "权"), "制衡天下", "金币增益：每击杀一名敌人额外获得桃子", 25, 2.5f, 1.3f),
        General("周瑜", listOf("周", "瑜"), "火烧赤壁", "持续法伤：使大片路面起火，造成百分比灼烧伤害", 55, 3.0f, 1.0f),
        General("司马懿", listOf("司马", "懿"), "陨石天降", "暗影伤害：召唤陨石砸击敌人并吸取生命值", 70, 3.5f, 0.7f),
        General("黄忠", listOf("黄", "忠"), "百步穿杨", "超远单体：对最远敌人进行爆头射击，伤害随距离递增", 80, 5.0f, 0.6f),
        General("马超", listOf("马", "超"), "神威天将军", "直线突刺：对一条直线上的所有敌人造成穿透击退", 60, 3.5f, 1.2f),
        General("魏延", listOf("魏", "延"), "狂骨吸血", "近战狂暴：攻击造成吸血效果，血量越低攻速越快", 55, 1.8f, 1.5f),
        General("姜维", listOf("姜", "维"), "九伐中原", "攻守兼备：物理与法术双修，对护甲敌人造成额外伤害", 50, 2.5f, 1.4f),
        General("吕布", listOf("吕", "布"), "神鬼乱舞", "绝强战神：近战大范围极速斩击，伤害最高", 90, 2.0f, 2.0f),
        General("貂蝉", listOf("貂", "蝉"), "闭月羞花", "魅惑控制：使路径上的敌人互殴或向反方向走", 20, 2.8f, 1.0f)
    )

    fun getGeneralByName(name: String): General? {
        return generalsList.find { it.name == name }
    }

    /**
     * Attempts to merge a character in hand (charB) onto a placed tower (towerA).
     * Returns the synthesized character string, or null if synthesis is not possible.
     */
    fun checkSynthesis(charA: String, starA: Int, charB: String, starB: Int): String? {
        // Rule 1: Identity synthesis (star level up)
        if (charA == charB && starA == starB && starA < 3) {
            return charA // same character, caller handles star level up
        }

        // Rule 2: Basic unit synthesis
        val basicPair = setOf(charA, charB)
        if (basicPair.contains("兵")) {
            if (basicPair.contains("马")) return "骑"
            if (basicPair.contains("弓")) return "弓"
            if (basicPair.contains("刀")) return "刀"
            if (basicPair.contains("枪")) return "枪"
            if (basicPair.contains("盾")) return "盾"
        }

        return null
    }

    /**
     * Checks if placing first and second adjacent in reading order forms a valid general.
     * Returns the name of the general/sub-general/wildcard general, or null.
     */
    fun checkSpelling(first: String, second: String): String? {
        // True General spelling (e.g. 赵 + 云 -> 赵云)
        for (gen in generalsList) {
            if (gen.characters.size == 2) {
                if (gen.characters[0] == first && gen.characters[1] == second) {
                    return gen.name
                }
            }
        }

        // Sub-general spelling (e.g. 关 + 平 -> 关平)
        val surnames = generalsList.map { it.characters[0] }.toSet()
        if (second == "平" && surnames.contains(first)) {
            return first + "平"
        }

        // Wildcard spelling (e.g. 关 + 通 -> 伪·关羽)
        if (second == "通" && surnames.contains(first)) {
            val matchedGen = generalsList.find { it.characters[0] == first }
            if (matchedGen != null) {
                return "伪·${matchedGen.name}"
            }
        }

        return null
    }

    /**
     * Returns the base attributes of a character name.
     * Maps basic characters, merged characters, and generals to their damage, range, and attack interval.
     */
    fun getCharacterAttributes(name: String, star: Int): Triple<Int, Float, Long> {
        val multiplier = when (star) {
            1 -> 1.0f
            2 -> 1.8f
            3 -> 3.2f
            else -> 1.0f
        }

        // Check if it is a true general
        val genName = if (name.startsWith("伪·")) name.substring(2) else name
        val gen = getGeneralByName(genName)
        if (gen != null) {
            var damage = (gen.baseDamage * multiplier).toInt()
            var range = gen.baseRange
            var interval = (1000 / gen.baseAtkSpeed).toLong()

            // Wildcard reduction
            if (name.startsWith("伪·")) {
                damage = (damage * 0.7f).toInt()
                range *= 0.9f
                interval = (interval * 1.2f).toLong()
            }
            return Triple(damage, range, interval)
        }

        // Sub-generals (e.g. 关平, 赵平)
        if (name.endsWith("平") && name.length > 1) {
            val surname = name.substring(0, name.length - 1)
            val trueGen = generalsList.find { it.characters[0] == surname }
            val baseDmg = trueGen?.baseDamage ?: 30
            val baseRng = trueGen?.baseRange ?: 2.2f
            val baseSpd = trueGen?.baseAtkSpeed ?: 1.0f

            val damage = (baseDmg * multiplier * 0.5f).toInt()
            val range = baseRng * 0.8f
            val interval = (1000 / (baseSpd * 0.8f)).toLong()
            return Triple(damage, range, interval)
        }

        // Basic troops and components
        val (baseDmg, baseRng, baseAtkSpeed) = when (name) {
            "兵" -> Triple(10, 1.2f, 1.0f)
            "马" -> Triple(5, 1.0f, 1.5f)
            "弓" -> Triple(15, 3.2f, 1.1f)
            "刀" -> Triple(20, 1.5f, 1.2f)
            "枪" -> Triple(18, 2.0f, 0.9f)
            "盾" -> Triple(2, 1.0f, 0.5f) // blocks/slows down, low attack
            "骑" -> Triple(25, 1.8f, 1.6f)
            "平" -> Triple(8, 1.2f, 1.0f)
            else -> Triple(10, 1.5f, 1.0f)
        }

        val damage = (baseDmg * multiplier).toInt()
        val range = baseRng
        val interval = (1000 / baseAtkSpeed).toLong()

        return Triple(damage, range, interval)
    }
}
