package com.sperance.exileforge.core.model.fate

import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.rules.content.Fate
import kotlinx.serialization.Serializable

/**
 * Предначертание в ответе сервера (`FateCard`; без тем с `API_REVISION` 68): код дара `fates.json` и ключи словаря
 * сервера - имя [name] и описание [description]. Рычаги клиент читает из своего чанка тем же резолвером правил (`FateEffects`).
 */
@Serializable
data class FateCard(
    val code: String,
    val name: String = "fate.$code.name",
    val description: String = "fate.$code.description",
) {
    /** Имя дара на языке игрока; нет ключа - заглушка и дыра словаря, не код. */
    val title: String get() = loc(name)

    companion object {
        /** Карточка дара из контента - для дара, известного лишь кодом (герой, `UserProfile.fate`). */
        fun of(fate: Fate) = FateCard(fate.code)
    }
}

/**
 * Предначертание аккаунта (`GET fate`): [chosen] - выбранный дар (тогда [offers] пусто), иначе три разных дара на выбор -
 * одни и те же при каждом чтении.
 */
@Serializable
data class FateView(val chosen: FateCard? = null, val offers: List<FateCard> = emptyList())
