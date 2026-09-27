package com.sperance.exileforge.core

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ContentLoader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** The content of the pinned server (`backend/` submodule), read straight from its files: what the client downloads chunk by chunk. */
object TestContent {
    private val root = listOf(File("../backend/src/main/resources"), File("backend/src/main/resources")).first { it.isDirectory }
    val index: ContentIndex by lazy { ContentLoader.load { File(root, "content/$it").readText() } }

    /** Server locale tables by language code, as `/locale/{code}.json` serves them. */
    fun serverLocales(): Map<String, Map<String, String>> = File(root, "locale").listFiles { f -> f.extension == "json" && f.name != "index.json" && f.name != "common.json" }
        .orEmpty().associate { file -> file.nameWithoutExtension to Json.parseToJsonElement(file.readText()).jsonObject.mapValues { it.value.jsonPrimitive.content } }
}
