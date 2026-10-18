package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.ChargeView
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.core.campaign.run.AilmentView
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.screens.expedition.arena.AllEffects
import com.sperance.exileforge.ui.screens.expedition.arena.EffectShelf
import com.sperance.exileforge.ui.screens.expedition.arena.EffectsSheet
import com.sperance.exileforge.ui.screens.expedition.arena.timedShelf

/**
 * Что герой несёт на карте: недуги - из боя и от ловушек, баффы и проклятия, заряды -
 * плитками той же полки, что в бою ([EffectShelf]: стаки и секунды, касание плитки - её выноска). Слева «≡ N» - лист всех
 * эффектов ([EffectsSheet]) с итогом здоровья в секунду; его же открывает «+N». Ничего не несёт - полки нет, а её высота стоит: низ
 * панели карты не прыгает, когда недуг лёг или сошёл.
 *
 * Полка читает живой снимок (тело экрана несомого не видит) и перекомпонуется сама, когда несомое сменилось.
 */
@Composable internal fun RoadEffects(live: State<RunHud>, index: ContentIndex) {
    val hud by remember(live) { derivedStateOf { live.value.let { RoadView(it.heroAilments, it.heroEffects, it.heroCharges, it.lifeFlow) } } }
    val timed = remember(hud) { timedShelf(false, hud.ailments, hud.effects, hud.charges) }
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(TILE), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (timed.isNotEmpty()) {
            AllEffects(timed.size, Modifier.width(TILE).fillMaxHeight()) { open = true }
            EffectShelf(timed, index, TILE, { open = true }, Modifier.weight(1f))
        }
    }
    if (open) EffectsSheet(ui("fight.shelf.hero"), timed, emptyList(), hud.flow, index) { open = false }
}

/** Несомое героя одним значением. */
private data class RoadView(val ailments: List<AilmentView>, val effects: List<EffectView>, val charges: List<ChargeView>, val flow: LifeFlow?)

/** Сторона плитки полки на карте. */
private val TILE = 26.dp
