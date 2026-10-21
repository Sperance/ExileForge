package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.GenericDamage
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.sheet.StatOperation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** Рост урона листа, сложенный на клиенте, и тело по строкам боя из памяти - те же числа, что у свода правил и прямого свода. */
class SheetGrowthTest {
    private val index get() = TestContent.index
    private val base = mapOf(CoreStat.HEALTH.code to 1000.0, CoreStat.ATTACK_PHYSICAL.code to 100.0, CoreStat.ATTACK_FIRE.code to 50.0, CoreStat.ATTACK_SPEED.code to 1.0)
    private val ops = listOf(
        StatOperation(CoreStat.DAMAGE.code, Op.INCREASED, 20.0),
        StatOperation(CoreStat.ATTACK_FIRE.code, Op.INCREASED, 15.0),
        StatOperation(CoreStat.ATTACK_FIRE.code, Op.MORE, 10.0),
        StatOperation(CoreStat.ATTACK_COLD.code, Op.MORE, 7.5, perStat = CoreStat.ATTACK_PHYSICAL.code, perAmount = 30.0),
        StatOperation(CoreStat.ATTACK_PHYSICAL.code, Op.INCREASED, 40.0, condition = Condition.LOW_LIFE),
    )
    private val lines = listOf(
        StatLine(CoreStat.DAMAGE.code, Op.INCREASED, 12.3),
        StatLine(CoreStat.ATTACK_FIRE.code, Op.MORE, 4.0),
        StatLine(CoreStat.ATTACK_COLD.code, Op.INCREASED, 0.1),
        StatLine(CoreStat.ATTACK_FIRE.code, Op.INCREASED, 0.2),
    )

    @Test
    fun client_growth_matches_the_rules_fold() {
        val model = SheetModel(base, ops, index)
        listOf(emptyList(), lines, lines.reversed()).forEach { laid ->
            val own = model.growth(laid)
            val rules = model.fold.growth(laid)
            GenericDamage.HITS.forEach { type ->
                assertEquals(rules.increased(type), own.increased(type), "increased $type")
                assertEquals(rules.more(type), own.more(type), "more $type")
            }
        }
    }

    @Test
    fun a_body_is_remembered_by_its_lines() {
        val model = SheetModel(base, ops, index)
        val build = HeroBuild(HeroGear(model.plain, 40, model, kit = Loadout(), percent = index.stats.percent), emptyMap(), index.campaign.combat)
        val first = build.body(lines)
        assertSame(first, build.body(lines.toList()))
        assertEquals(model.with(build.passiveLines + lines), first.stats)
        assertEquals(model.with(build.passiveLines + lines.reversed()), build.body(lines.reversed()).stats)
    }
}
