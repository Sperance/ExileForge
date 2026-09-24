package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.modifier.ModifierDefinition

/**
 * What a small icon stands for, apart from how it is drawn.
 *
 * Icons used to be picked by searching the text beside them for Russian and English fragments —
 * "брон", "armor", "сил" — which broke the moment a label was reworded or shown in another
 * language, and could not be tested without Compose. Now a caller names a meaning, the domain
 * codes the server sends are mapped to one here, and `:app` holds a single exhaustive table from a
 * meaning to its drawing: a new glyph does not compile until it has one.
 */
enum class Glyph {
    LIFE, MANA, SHIELD, FIRE, COLD, LIGHTNING, CHAOS, ATTACK, DEFENCE, EVASION, SPEED, CRITICAL,
    ATTRIBUTE, LEECH, LEVEL, CURRENCY, CHARACTER, ITEM, RARITY, STATE, TREE, TEXT, RULE, REFERENCE,
    IDENTITY, SERVER, CRAFT, MAP, GEM, FLASK, IMAGE, INFO, ALERT;

    companion object {
        /**
         * The glyph of a characteristic, by the server's own enum name.
         *
         * The names are the server's stable English codes, so their words are a vocabulary rather
         * than prose: an element wins over the kind of stat it is — a fire resistance is fire first —
         * which is why the table is ordered and read top down. A name nothing here knows keeps the
         * neutral glyph instead of a guess.
         */
        fun ofStat(stat: String): Glyph = statWords.firstOrNull { (word, _) -> word in stat.uppercase() }?.second ?: INFO

        /** The glyph of a document field — an editor row, a form label — by its exact key. */
        fun ofField(key: String): Glyph = fields[key] ?: stat(key) ?: INFO

        /** A modifier is drawn as the characteristic its first effect changes. */
        fun ofModifier(modifierId: String, definitions: List<ModifierDefinition>): Glyph =
            definitions.firstOrNull { it.id == modifierId }?.effects?.firstOrNull()?.stat?.let(::ofStat) ?: INFO

        fun of(catalog: Catalog): Glyph = when (catalog) {
            Catalog.ITEMS -> CURRENCY
            Catalog.EQUIPMENT -> ITEM
            Catalog.CHARACTERS -> CHARACTER
        }

        private fun stat(key: String) = if (key.startsWith("STOCK_") || key.startsWith("BATTLE_")) ofStat(key) else null

        private val statWords = listOf(
            "LIGHT_RADIUS" to MAP, "FIRE" to FIRE, "COLD" to COLD, "WATER" to COLD, "LIGHTNING" to LIGHTNING, "ELECTRIC" to LIGHTNING,
            "CHAOS" to CHAOS, "DARK" to CHAOS, "ENERGY_SHIELD" to SHIELD, "LEECH" to LEECH, "VAMPIRE" to LEECH,
            "HEALTH" to LIFE, "MANA" to MANA, "ENERGY" to MANA, "INTELLECT" to MANA, "CRITICAL" to CRITICAL,
            "SPEED" to SPEED, "EVASION" to EVASION, "AGILITY" to EVASION, "ARMOR" to DEFENCE, "RESIST" to DEFENCE,
            "BLOCK" to DEFENCE, "STUN" to DEFENCE, "ATTACK" to ATTACK, "COMBAT" to ATTACK, "STRENGTH" to ATTRIBUTE,
            "CONSTITUTION" to ATTRIBUTE, "GOLD" to CURRENCY, "RARITY" to RARITY, "QUANTITY" to RARITY,
            "EXPERIENCE" to LEVEL, "INVENTORY" to ITEM, "MAGIC" to MANA,
            "IGNITE" to FIRE, "BURNING" to FIRE, "CHILL" to COLD, "FREEZE" to COLD, "SHOCK" to LIGHTNING, "POISON" to CHAOS,
            "BLEED" to ATTACK, "FLASK" to LIFE, "REDUCTION" to DEFENCE,
        )

        private val fields = mapOf(
            "name" to TEXT, "description" to TEXT, "code" to IDENTITY, "tags" to TEXT,
            "level" to LEVEL, "itemLevel" to LEVEL, "requiredLevel" to LEVEL, "experience" to LEVEL,
            "requiredStrength" to ATTRIBUTE, "requiredDexterity" to EVASION, "requiredIntelligence" to MANA,
            "rarity" to RARITY, "slot" to ITEM, "weaponType" to ATTACK, "category" to ITEM, "subCategory" to ITEM,
            "type" to ITEM, "durability" to DEFENCE, "price" to CURRENCY,
            "modifierId" to REFERENCE, "modifierIds" to REFERENCE, "baseParams" to RULE, "effects" to RULE,
            "stat" to RULE, "perStat" to RULE, "perAmount" to RULE, "operation" to RULE, "value" to RULE, "values" to RULE,
            "source" to RULE, "isLocal" to RULE,
            "battleSkills" to ATTACK, "professionSkills" to CRAFT, "boolSkills" to STATE,
            "userId" to CHARACTER, "classId" to CHARACTER,
        )
    }
}
