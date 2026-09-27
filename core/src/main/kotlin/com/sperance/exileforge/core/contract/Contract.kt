package com.sperance.exileforge.core.contract

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.RulesJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** The wire's JSON is the rules' own: compact (no defaults, no nulls), lenient on unknown keys, so both sides read one shape. */
val WireJson: Json = RulesJson

fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()

/** The server this client is built against: the submodule `backend/` is pinned to this commit, and `SERVER_VERSION` is its `Constants.kt`. */
const val SERVER_COMMIT = "e093211339e68ed1fe2ad0fdc40758bcf017c017"
const val SERVER_BRANCH = "claude/tender-pasteur-a36kj2"
const val SERVER_VERSION = "1.1.0"

/** A Mongo id: 24 hex digits. Content is named by codes, only players' things carry ids. */
fun requireId(id: String) { require(Regex("[0-9a-fA-F]{24}").matches(id)) { ui("contract.bad_id") } }
