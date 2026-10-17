package com.sperance.exileforge.core.sim

import com.sperance.exileforge.rules.content.ContentIndex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.security.MessageDigest

/**
 * Ключ дискового кэша эталонов ([HeroPool]): эталон из кэша годится, только пока не сдвинулось ничего, что читает постройка героя
 * ([MobKit.hero], цена листа, подбор набора [LoadoutTuner] по манекенам зон). Ключ - отпечаток кода (`code`: Gradle отдаёт отпечаток
 * классов ядра, симуляций и правил), всех файлов контента и чисел эталонного врага Мощи по уровням; из `campaign.json` в отпечаток
 * не входят разделы, которых постройка не читает ([IDLE]: боссы, режимы, карты, объекты), и записи боссов в `monsters` - подгонка
 * силы стражей и режимов кэш не сбрасывает, а любая правка вещей, умений, дерева или обычных монстров - сбрасывает.
 */
object HeroCacheKey {
    /** Папка кэша внутри [root] для кода [code], контента из папки [content] и его индекса [index]. */
    fun dir(root: File, code: String, content: File, index: ContentIndex): File = File(root, of(code, content, index))

    /** Отпечаток кода [code], контента из папки [content] и эталонного врага индекса [index]. */
    fun of(code: String, content: File, index: ContentIndex): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(code.toByteArray())
        content.listFiles { file -> file.extension == "json" }.orEmpty().sortedBy { it.name }.forEach { file ->
            digest.update(file.name.toByteArray())
            digest.update(if (file.name == CAMPAIGN) building(file.readText()).toByteArray() else file.readBytes())
        }
        val foe = index.referenceFoe
        (1..index.classes.maxLevel).forEach { level -> digest.update("$level:${foe.hit(level)}:${foe.accuracy(level)}:${foe.evasion(level)}".toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }.take(KEY)
    }

    /** Часть `campaign.json`, что читает постройка героя: без разделов [IDLE] и без записей боссов. */
    private fun building(text: String): String {
        val campaign = Json.parseToJsonElement(text).jsonObject
        val kept = campaign.filterKeys { it !in IDLE }.mapValues { (key, value) ->
            if (key == MONSTERS && value is JsonArray) JsonArray(value.filterNot { (it as? JsonObject)?.get(BOSS) == JsonPrimitive(true) }) else value
        }
        return JsonObject(kept).toString()
    }

    private const val CAMPAIGN = "campaign.json"
    private const val MONSTERS = "monsters"
    private const val BOSS = "boss"

    /** Длина ключа папки, знаков отпечатка. */
    private const val KEY = 24

    /** Разделы `campaign.json`, которых постройка эталона не читает: боссы и их фазы, режимы, карты, объекты и услуги. */
    private val IDLE = setOf("bosses", "services", "fountains", "features", "corruption", "vaal", "maps", "abyss", "trials", "lootChests", "chests")
}
