package com.sperance.exileforge.core.model



/** Whitelist of server collections that can be selected as foreign keys. */
enum class EntitySource(val path: String) {
    USER("user"), CHARACTER("character"), EQUIPMENT("equipment"), ITEM("items"), RECIPE("recipe"), REDEMPTION("redemptioncodes")
}
