package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.screens.expedition.arena.AllEffects
import com.sperance.exileforge.ui.screens.expedition.arena.EffectShelf
import com.sperance.exileforge.ui.screens.expedition.arena.EffectsSheet
import com.sperance.exileforge.ui.screens.expedition.arena.timedShelf

/**
 * Что герой несёт на карте (макет «Эффекты героя на карте», вариант C): недуги - из боя и от ловушек, баффы и проклятия, заряды -
 * плитками той же полки, что в бою ([EffectShelf]: стаки и секунды, касание плитки - её выноска). Слева «≡ N» - лист всех
 * эффектов ([EffectsSheet]) с итогом здоровья в секунду; его же открывает «+N». Ничего не несёт - полки нет.
 */
@Composable internal fun RoadEffects(hud: RunHud) {
    val timed = remember(hud.heroAilments, hud.heroEffects, hud.heroCharges) { timedShelf(false, hud.heroAilments, hud.heroEffects, hud.heroCharges) }
    var open by remember { mutableStateOf(false) }
    if (timed.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().height(TILE), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            AllEffects(timed.size, Modifier.width(TILE).fillMaxHeight()) { open = true }
            EffectShelf(timed, TILE, { open = true }, Modifier.weight(1f))
        }
    }
    if (open) EffectsSheet(ui("fight.shelf.hero"), timed, emptyList(), hud.lifeFlow) { open = false }
}

/** Сторона плитки полки на карте. */
private val TILE = 26.dp
