package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.rules.sheet.FlaskSheet

/** Бонус героя к флягам: характеристика [stat], её итог [total] (к лучшему ли - [better]) и источники строками разбора [sources]. */
data class FlaskBonus(val stat: String, val total: String, val better: Boolean?, val sources: List<ShareRow>)

/**
 * Бонусы героя к флягам для блока «У вас» карточки фляги: каждая характеристика, что лист фляги читает у героя
 * ([FlaskSheet.HERO_BONUSES]), с итогом не ноль - тем же разбором по источникам, что окно цифры ([StatExplainer]).
 * Считается при первом чтении: карточка не фляги его не трогает.
 */
class FlaskBonuses(private val game: GameUi) {
    val bonuses: List<FlaskBonus> by lazy {
        val combat = game.hero?.combat ?: return@lazy emptyList()
        val explainer = StatExplainer(game)
        FlaskSheet.HERO_BONUSES.mapNotNull { bonus ->
            val breakdown = combat.explain(bonus.code)
            if (breakdown.total == 0.0) return@mapNotNull null
            val view = explainer.explain(breakdown, emptyList(), combat::holders)
            FlaskBonus(bonus.code, view.total, StatPolarity.better(game.index, bonus.code, breakdown.total), view.cards.flatMap { it.rows })
        }
    }
}
