package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

@Serializable
data class ModifierDefinition(

    val id: String,

    val name: String,

    val source: ModifierSource,

    val scope: ModifierScope = ModifierScope.ITEM,

    val affixType: AffixType? = null,

    val tiers: List<ModifierTier> = emptyList(),

    val tags: Set<ModifierTag> = emptySet(),

    val conditions: List<ModifierCondition> = emptyList(),

    val effects: List<ModifierEffect> = emptyList(),

    val priority: Int = 0,

    val rollable: Boolean = true,

    val stackable: Boolean = false,

    val revision: Int = 1,
    val enabled: Boolean = true,
    val _id: String? = null
)
