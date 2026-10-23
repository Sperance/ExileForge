package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.campaign.run.FountainView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex

/**
 * Предложение фонтана списком: «Восстанавливает: здоровье 60%, мана 60%, энергощит 100%, заряды фляг 40%» - числа правила фонтанов
 * подставляет контент [index] (`{r:fountains.…}`); строки щита нет, когда у героя нет энергощита ([FountainView.shielded]).
 */
fun fountainOffer(fountain: FountainView, index: ContentIndex?): String {
    val parts = listOfNotNull(
        "fountain.part.life",
        "fountain.part.mana",
        "fountain.part.shield".takeIf { fountain.shielded },
        "fountain.part.flasks",
    ).map { key -> ui(key).let { index.numberedText(it) ?: it } }
    return ui("fountain.offer", parts.joinToString(", "))
}
