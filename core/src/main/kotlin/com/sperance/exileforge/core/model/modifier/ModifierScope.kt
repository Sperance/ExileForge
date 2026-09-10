package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

@Serializable
enum class ModifierScope {

    ITEM,

    CHARACTER,

    SKILL,

    ATTACK,

    SPELL,

    HIT,

    TARGET,

    AREA,

    PARTY
}
