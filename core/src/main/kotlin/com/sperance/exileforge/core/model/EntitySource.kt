package com.sperance.exileforge.core.model

/**
 * Whitelist of server collections that can be selected as foreign keys.
 *
 * The path is the route segment the server derives from the entity's serial name, lower-cased.
 */
enum class EntitySource(val path: String) {
    USER("user"), CHARACTER("character"), EQUIPMENT("equipment"), ITEM("items"),
    RECIPE("recipe"), REDEMPTION("redemptioncodes"), MODIFIER("modifierdefinition"), INVENTORY("characterequipment"),
    CHARACTER_CLASS("characterclass"), SKILL_NODE("skilltreenode"), EXPERIENCE_LEVEL("experiencelevel"),
    AUCTION("auctionlot")
}
