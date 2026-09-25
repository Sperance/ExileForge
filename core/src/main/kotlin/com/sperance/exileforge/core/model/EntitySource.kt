package com.sperance.exileforge.core.model

import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validateCode
import kotlinx.serialization.json.JsonObject

/**
 * Whitelist of server collections that can be selected as foreign keys.
 *
 * The path is the route segment the server derives from the entity's serial name, lower-cased.
 * [byCode] says what a reference to the collection holds: a modifier is named by its stable code
 * since server 0.56.0, everything else by its `_id`.
 */
enum class EntitySource(val path: String, val byCode: Boolean = false) {
    USER("user"), CHARACTER("character"), EQUIPMENT("equipment"), ITEM("items"),
    RECIPE("recipe"), REDEMPTION("redemptioncodes"), MODIFIER("modifierdefinition", byCode = true), INVENTORY("characterequipment"),
    CHARACTER_CLASS("characterclass"), SKILL_NODE("skilltreenode"), EXPERIENCE_LEVEL("experiencelevel"),
    AUCTION("auctionlot"), POOL("pool");

    /** What a reference to [record] holds: its code or its id. */
    fun reference(record: JsonObject): String = if (byCode) record.text("code") else record.entityId

    /** Refuses a value no record of this collection could be referenced by. */
    fun requireReference(value: String) = if (byCode) validateCode(value) else requireId(value)
}
