package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

@Serializable
enum class ModifierSource {

    BASE_ITEM,

    PREFIX,

    SUFFIX,

    UNIQUE,

    ENCHANTMENT,

    CORRUPTION,

    PASSIVE,

    SKILL,

    AURA,

    FLASK,

    JEWEL,

    MAP,

    MONSTER,

    TEMPORARY,

    SYSTEM
}
