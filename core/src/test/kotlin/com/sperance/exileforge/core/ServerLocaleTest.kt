package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.documentDescription
import com.sperance.exileforge.core.display.documentTitle
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.i18n.*
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionLotKind
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.OrbOutcome
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test

/**
 * Localisation as the server serves it since 0.14.0.
 *
 * No document in Mongo carries text any more: an entity stores a code and the string lives in a
 * static dictionary keyed by that code. These tests pin the key shape, because the client has to
 * compute the very same key the server wrote — a drift shows up as a raw key on screen.
 */
class ServerLocaleTest {
    private val id = "0123456789abcdef01234567"

    private val document = """{
        "equipment.IRON_SKULLCAP.name": "Железный шишак",
        "equipment.IRON_SKULLCAP.description": "Шлем наёмника",
        "item.CHAOS_ORB.name": "Сфера хаоса",
        "item.ORB_OF_FUSING.name": "Сфера соединения",
        "item.ORB_OF_FUSING.description": "Меняет связи гнёзд",
        "modifier.LIFE_AND_MANA.name": "+{0} к здоровью и +{1} к мане",
        "skilltree.STR_LIFE_1.name": "Крепость",
        "skilltree.STR_LIFE_1.description": "Больше здоровья",
        "class.MARAUDER.name": "Мародёр",
        "class.MARAUDER.description": "Сила и броня",
        "error.AU_006": "Нельзя купить собственный лот",
        "error.AU_002": "Уровень {0} слишком мал для аукциона",
        "system.success": "Успешно"
    }"""

    private fun load() { serverLocale = LocaleBundle.parse("ru", "sha-1", document) }
    @After fun forget() { serverLocale = LocaleBundle() }

    @Test fun `a key is section, code and field, exactly as the server writes it`() {
        assertEquals("equipment.IRON_SKULLCAP.name", LocaleKey.equipmentName("IRON_SKULLCAP"))
        assertEquals("equipment.IRON_SKULLCAP.description", LocaleKey.equipmentDescription("IRON_SKULLCAP"))
        assertEquals("item.CHAOS_ORB.name", LocaleKey.itemName("CHAOS_ORB"))
        assertEquals("modifier.LIFE_AND_MANA.name", LocaleKey.modifierName("LIFE_AND_MANA"))
        assertEquals("skilltree.STR_LIFE_1.description", LocaleKey.skillNodeDescription("STR_LIFE_1"))
        assertEquals("class.MARAUDER.name", LocaleKey.className("MARAUDER"))
        assertEquals("error.AU_006", LocaleKey.error("AU_006"))
    }

    @Test fun `a missing key comes back as itself so a hole is visible`() {
        assertTrue(LocaleBundle().isEmpty)
        assertEquals("equipment.NOTHING.name", loc(LocaleKey.equipmentName("NOTHING")))
        load()
        assertEquals(13, serverLocale.size)
        assertEquals("Железный шишак", loc("equipment.IRON_SKULLCAP.name"))
        assertEquals("equipment.NOTHING.name", loc("equipment.NOTHING.name"))
        assertEquals("—", locOr("equipment.NOTHING.name", "—"))
    }

    @Test fun `the manifest names a language by its own label and a fingerprint`() {
        val manifest = WireJson.decodeFromString(LocaleManifest.serializer(),
            """{"default":"en","languages":[{"code":"en","label":"English","hash":"a1"},{"code":"ru","label":"Русский","hash":"b2"}]}""")
        assertEquals("en", manifest.default)
        assertEquals("Русский", manifest.language("ru")?.label)
        assertEquals("b2", manifest.language("ru")?.hash)
        assertNull(manifest.language("de"))
    }

    @Test fun `an argument is itself a key, because that is how the server passes one`() {
        load()
        val outcome = WireJson.decodeFromString(OrbOutcome.serializer(),
            """{"messageKey":"modifier.LIFE_AND_MANA.name","messageArgs":["12","equipment.IRON_SKULLCAP.name"],
                "item":{"_id":"$id","characterId":"$id","equipmentId":"$id","rarity":"RARE"}}""")
        assertEquals("+12 к здоровью и +Железный шишак к мане", outcome.message)
        // A plain number is not a key and comes through untouched.
        assertEquals("+12 к здоровью и +30 к мане",
            loc("modifier.LIFE_AND_MANA.name", listOf("12", "30")))
    }

    @Test fun `a modifier is one whole sentence, not a label with numbers bolted on`() {
        val definition = WireJson.decodeFromString(ModifierDefinition.serializer(),
            """{"_id":"$id","code":"LIFE_AND_MANA","source":"PREFIX",
                "effects":[{"stat":"STOCK_HEALTH","operation":"ADD"},{"stat":"STOCK_MANA","operation":"ADD"}]}""")
        val rolled = buildJsonObject { put("modifierId", id); put("values", buildJsonArray { add(46.0); add(11.5) }) }
        // Without a dictionary the numbers are still printed: a roll never vanishes with a translation.
        assertEquals("46 Здоровье · 12 Мана", modifierText(rolled, listOf(definition)))
        load()
        assertEquals("+46 к здоровью и +12 к мане", modifierText(rolled, listOf(definition)))
        // A definition the client has not read still shows what the server rolled.
        assertEquals("46 · 12", modifierText(rolled, emptyList()))
    }

    @Test fun `an entity is named by its code through the dictionary`() {
        load()
        val helmet = buildJsonObject { put("_id", id); put("code", "IRON_SKULLCAP"); put("slot", "HELMET") }
        assertEquals("Железный шишак", documentTitle(helmet))
        assertEquals("Шлем наёмника", documentDescription(helmet))
        val orb = buildJsonObject { put("_id", id); put("code", "CHAOS_ORB"); put("category", "CURRENCY") }
        assertEquals("Сфера хаоса", documentTitle(orb))
        // A character is the one document whose name its player wrote, so it is not in the dictionary.
        val exile = buildJsonObject { put("_id", id); put("userId", id); put("name", "Изгнанник") }
        assertEquals("Изгнанник", documentTitle(exile))
        // A code the dictionary does not know is shown readably rather than as a raw key.
        assertEquals("UNKNOWN HAT", documentTitle(buildJsonObject { put("code", "UNKNOWN_HAT"); put("slot", "HELMET") }))
    }

    @Test fun `a class, a tree node and a lot all read the same dictionary`() {
        load()
        val marauder = WireJson.decodeFromString(CharacterClass.serializer(), """{"_id":"$id","code":"MARAUDER"}""")
        assertEquals("Мародёр", marauder.title)
        assertEquals("Сила и броня", marauder.details)
        val node = WireJson.decodeFromString(SkillTreeNode.serializer(), """{"_id":"$id","code":"STR_LIFE_1","type":"SMALL"}""")
        assertEquals("Крепость", node.title)
        assertEquals("Больше здоровья", node.details)
        // Which section a lot's code belongs to is decided by what kind of lot it is.
        assertEquals("Железный шишак", AuctionLot(kind = AuctionLotKind.EQUIPMENT, itemCode = "IRON_SKULLCAP").title)
        assertEquals("Сфера хаоса", AuctionLot(kind = AuctionLotKind.ITEM, itemCode = "CHAOS_ORB").title)
    }

    @Test fun `an orb the client has no table for is still named by the server`() {
        load()
        val unknown = CurrencyItem(id, "ORB_OF_FUSING", "ORB_OF_FUSING", 5)
        assertNull(unknown.orb)
        assertEquals("Сфера соединения", unknown.title(Lang.EN))
        assertEquals("Меняет связи гнёзд", unknown.details(Lang.EN))
        // One the client does know keeps its own rule when the dictionary has no description for it.
        val chaos = CurrencyItem(id, "CHAOS_ORB", "CHAOS_ORB", 1)
        assertEquals("Сфера хаоса", chaos.title(Lang.RU))
        assertEquals("Перекатывает аффиксы редкого предмета", chaos.details(Lang.RU))
    }

    @Test fun `a refusal is translated only when the envelope carried everything it needs`() {
        load()
        // The error envelope has a message and a code but never the arguments that filled the
        // template, so a template with a hole in it keeps the sentence the server already built.
        assertEquals("Character level 3 is too low for the auction",
            locError("AU_002", "Character level 3 is too low for the auction"))
        assertEquals("Нельзя купить собственный лот", locError("AU_006", "You cannot buy your own lot"))
        // No code, or a code outside the dictionary: the server's own sentence stands.
        assertEquals("Boom", locError(null, "Boom"))
        assertEquals("Boom", locError("XX_999", "Boom"))
    }

    @Test fun `the catalogue narrows itself by name through the dictionary`() {
        load()
        assertEquals(setOf("IRON_SKULLCAP"), serverLocale.codesMatching(LocaleKey.EQUIPMENT, "шишак"))
        assertEquals(setOf("CHAOS_ORB"), serverLocale.codesMatching(LocaleKey.ITEM, "хаос"))
        // A description is not a name, and a blank needle is not a filter.
        assertTrue(serverLocale.codesMatching(LocaleKey.EQUIPMENT, "наёмника").isEmpty())
        assertTrue(serverLocale.codesMatching(LocaleKey.EQUIPMENT, "  ").isEmpty())
    }
}
