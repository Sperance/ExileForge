package com.sperance.exileforge.core.editor.conflict

import com.sperance.exileforge.core.contract.protectedFields
import kotlinx.serialization.json.*

data class FieldConflict(val field: String, val original: JsonElement?, val local: JsonElement?, val remote: JsonElement?)
data class MergeReview(val merged: JsonObject, val conflicts: List<FieldConflict>)
object ThreeWayMerge {
    /** Arrays are one atomic field: never merge modifiers by index. */
    fun review(original: JsonObject, local: JsonObject, remote: JsonObject): MergeReview {
        val merged = remote.toMutableMap()
        val conflicts = mutableListOf<FieldConflict>()
        (original.keys + local.keys).filterNot { it in protectedFields }.forEach { key ->
            if(local[key] != original[key]) {
                if(remote[key] != original[key] && remote[key] != local[key]) conflicts += FieldConflict(key, original[key], local[key], remote[key])
                else local[key]?.let { merged[key] = it } ?: merged.remove(key)
            }
        }
        return MergeReview(JsonObject(merged), conflicts)
    }
    fun resolve(review: MergeReview, keepLocal: Map<String, Boolean>): JsonObject {
        require(review.conflicts.all { it.field in keepLocal }) { "Выберите значение для каждого конфликта" }
        val fields = review.merged.toMutableMap()
        review.conflicts.forEach { c -> (if(keepLocal.getValue(c.field)) c.local else c.remote)?.let { fields[c.field] = it } ?: fields.remove(c.field) }
        return JsonObject(fields)
    }
}
