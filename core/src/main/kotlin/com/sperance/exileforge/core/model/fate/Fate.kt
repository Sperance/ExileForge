package com.sperance.exileforge.core.model.fate

import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.rules.content.Fate
import kotlinx.serialization.Serializable

/**
 * Предначертание в ответе сервера (4.6.0, `FateCard`; без тем с `API_REVISION` 68): код дара `fates.json` и ключи словаря
 * сервера - имя [name] и описание [description]. Рычаги клиент читает из своего чанка тем же резолвером правил (`FateEffects`).
 */
@Serializable
data class FateCard(
    val code: String,
    val name: String = "fate.$code.name",
    val description: String = "fate.$code.description",
) {
    /** Имя дара на языке игрока; нет ключа - код. */
    val title: String get() = locOr(name, code)

    /** Что дар делает, словами сервера. */
    val text: String get() = locOr(description, "")

    companion object {
        /** Карточка дара из контента - для дара, известного лишь кодом (герой, `UserProfile.fate`). */
        fun of(fate: Fate) = FateCard(fate.code)
    }
}

/**
 * Предначертание аккаунта (4.6.0, `GET fate`): [chosen] - выбранный дар (тогда [offers] пусто), иначе три разных дара на выбор -
 * одни и те же при каждом чтении.
 */
@Serializable
data class FateView(val chosen: FateCard? = null, val offers: List<FateCard> = emptyList())
