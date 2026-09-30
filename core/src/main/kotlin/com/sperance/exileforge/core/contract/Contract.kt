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
const val SERVER_COMMIT = "41f52a39423f88e36231eeddfd02688eca400c5b"
const val SERVER_BRANCH = "claude/tender-pasteur-a36kj2"
const val SERVER_VERSION = "1.39.0"

/** A Mongo id: 24 hex digits. Content is named by codes, only players' things carry ids. */
fun requireId(id: String) { require(Regex("[0-9a-fA-F]{24}").matches(id)) { ui("contract.bad_id") } }

/**
 * The id of an item copy: an ObjectId for what the server made, but a run's loot keeps the id its seed
 * rolled on both sides (`r<seed>-k0-1`) — a 24-digit check refused to wear what a map dropped (3.2.0).
 */
fun requireItemId(id: String) { require(ITEM_ID.matches(id)) { ui("contract.bad_id") } }

private val ITEM_ID = Regex("[0-9A-Za-z-]{1,64}")
