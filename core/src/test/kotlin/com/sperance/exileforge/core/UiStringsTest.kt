package com.sperance.exileforge.core

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.campaign.Ailment
import com.sperance.exileforge.core.campaign.DamageType
import com.sperance.exileforge.core.campaign.Outcome
import com.sperance.exileforge.core.campaign.TargetRule
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.StatGroup
import com.sperance.exileforge.core.display.bodyPlaces
import com.sperance.exileforge.core.model.auction.LotKind
import com.sperance.exileforge.core.model.command.RedemptionKind
import com.sperance.exileforge.rules.content.AtlasNodeKind
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillEvent
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.text.LocaleKey
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The client's own dictionaries must not drift apart.
 *
 * This mirrors the server's `LocalizationTest`: the languages hold the same keys, nothing is left
 * empty, and a numbered hole never disappears in translation. The last one matters most - a label
 * that lost its `{0}` silently drops a number the player was meant to read.
 *
 * The final test is the one the server cannot have: it reads the sources and checks that every key
 * a `ui("...")` call names actually exists. A key is a string, so nothing but this catches a typo.
 */
class UiStringsTest {

    private val languages = Lang.entries

    @Test
    fun every_language_has_a_table() {
        languages.forEach {
            assertTrue(UiStrings.keys(it).isNotEmpty(), "${it.code}: словарь пуст или не прочитан")
        }
    }

    @Test
    fun the_languages_have_exactly_the_same_keys() {
        val reference = UiStrings.keys(Lang.RU)

        languages.forEach {
            val keys = UiStrings.keys(it)
            assertEquals(emptySet(), reference - keys, "${it.code}: нет ключей")
            assertEquals(emptySet(), keys - reference, "${it.code}: лишние ключи")
        }
    }

    @Test
    fun nothing_is_left_empty() {
        languages.forEach { lang ->
            UiStrings.table(lang).forEach { (key, text) ->
                assertTrue(text.isNotBlank(), "${lang.code}: пустая строка у $key")
            }
        }
    }

    @Test
    fun a_placeholder_never_disappears_in_translation() {
        val hole = Regex("\\{\\d+}")
        val reference = UiStrings.table(Lang.RU)

        languages.filterNot { it == Lang.RU }.forEach { lang ->
            val table = UiStrings.table(lang)
            reference.forEach { (key, text) ->
                val expected = hole.findAll(text).map { it.value }.toSet()
                val actual = hole.findAll(table.getValue(key)).map { it.value }.toSet()
                assertEquals(expected, actual, "$key: в ru $expected, в ${lang.code} $actual")
            }
        }
    }

    /**
     * An orb's English trade name (2.51.0) is one in every language, so it is written once, in the
     * common file, and never again in a language file — the same line the server draws with
     * `locale/common.json`. The title itself is translated; in English it is the trade name.
     */
    @Test
    fun what_every_language_shares_is_written_once() {
        val common = UiStrings.common()
        assertTrue(common.isNotEmpty(), "ui_common.json пуст или не прочитан")
        val orbTitle = Regex("^enum\\.orb\\.[A-Z_]+\\.trade$")
        assertEquals(emptyList(), common.keys.filterNot(orbTitle::matches), "в общем словаре переводимое")
        languages.forEach {
            val own = UiStrings.own(it)
            assertEquals(emptySet(), own.keys intersect common.keys, "${it.code}: повторяет ui_common.json")
            assertEquals(emptyList(), own.keys.filter(orbTitle::matches), "${it.code}: торговое имя сферы вне общего словаря")
        }
        common.forEach { (key, trade) -> assertEquals(trade, UiStrings.own(Lang.EN)[key.removeSuffix(".trade")], "en: $key") }
    }

    @Test
    fun a_missing_key_is_shown_as_itself() {
        assertEquals("nothing.like.this", ui("nothing.like.this"))
    }

    @Test
    fun arguments_fill_the_holes() {
        assertEquals("Уровень 7", ui(Lang.RU, "hero.level", 7))
        assertEquals("Level 7", ui(Lang.EN, "hero.level", 7))
    }

    @Test
    fun a_counted_noun_follows_its_language() {
        assertEquals("tree.node.one", pluralKey("tree.node", 1, Lang.RU))
        assertEquals("tree.node.few", pluralKey("tree.node", 3, Lang.RU))
        assertEquals("tree.node.many", pluralKey("tree.node", 11, Lang.RU))
        assertEquals("tree.node.many", pluralKey("tree.node", 5, Lang.EN))
        assertEquals("узла", plural("tree.node", 3, Lang.RU))
    }

    @Test
    fun every_key_the_sources_ask_for_exists() {
        val sources = listOf(File("src/main/kotlin"), File("../app/src/main/kotlin"))
            .filter { it.isDirectory }
        assertTrue(sources.isNotEmpty(), "исходники не найдены - тест ничего не проверил")

        // Both shapes: ui("key", ...) and ui(lang, "key", ...). A key built with a template -
        // "enum.slot.$slot" - cannot be checked by reading, so '$' keeps it out; the enums whose
        // codes it is built from are walked by the test below instead.
        val call = Regex("\\bui(?:Or)?\\((?:[A-Za-z][A-Za-z0-9_.]*\\s*,\\s*)?\"([^\"\\\\$]+)\"")
        val known = UiStrings.keys(Lang.RU)
        val missing = sources.asSequence()
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> call.findAll(file.readText()).map { file.name to it.groupValues[1] } }
            .filterNot { (_, key) -> key in known }
            .toList()

        assertTrue(missing.isEmpty(), "нет в словаре: ${missing.take(10)}")
    }

    /**
     * Every code the client enumerates is named.
     *
     * These are the keys built from a code at runtime, which the source scan above cannot see. The
     * enums now live in `rules`, so a value the server grows without its strings is caught here.
     */
    @Test
    fun every_code_the_client_enumerates_is_named() {
        val expected = buildSet {
            LotKind.entries.forEach { add("enum.lot.${it.name}") }
            Orb.entries.forEach { add("enum.orb.${it.name}"); add("enum.orb.${it.name}.rule") }
            RedemptionKind.entries.forEach { add("enum.reward.${it.name}") }
            MonsterRarity.entries.forEach { add("enum.monster_rarity.${it.name}") }
            Rarity.entries.forEach { add("enum.rarity.${it.name}") }
            Slot.entries.forEach { add("enum.slot.${it.name}") }
            bodyPlaces.forEach { add("enum.slot.${it.code}") }
            WeaponType.entries.forEach { add("enum.weapon.${it.name}") }
            SkillNodeType.entries.forEach { add("enum.node.${it.name}") }
            Ailment.entries.forEach { add("enum.ailment.${it.name}"); add("fight.effect.${it.name}") }
            DamageType.entries.forEach { add("enum.damage.${it.name}") }
            AffixKind.entries.forEach { add("mod.kind.${it.name}") }
            StatGroup.entries.forEach { add("enum.stat_group.${it.name}") }
            TargetRule.entries.forEach { add("fight.rule.${it.name}") }
            Outcome.entries.forEach { add("expedition.outcome_${it.name.lowercase()}") }
            AtlasNodeKind.entries.forEach { add("atlas.kind.${it.name}") }
            AtlasBranch.entries.forEach { add("atlas.branch.${it.name}") }
            AtlasPoints.KINDS.forEach { add("expedition.key_$it") }
            SlotCondition.entries.forEach { add("skills.condition.${it.name}") }
            SkillType.entries.forEach { add("skills.type.${it.name}") }
            SkillEvent.entries.forEach { add("skill.on.${it.name}") }
            (DamageType.entries.map { it.name } + "RANDOM").forEach { add("skill.element.$it") }
            (Ailment.entries.map { it.word } + "ELEMENT").forEach { add("skill.ailment.$it") }
            listOf(EssenceBook.VAAL_UPGRADE, EssenceBook.VAAL_SPECIAL, EssenceBook.VAAL_STRONGER).forEach { add("crystal.outcome.$it") }
            listOf("requiredLevel", "requiredStrength", "requiredDexterity", "requiredIntelligence").forEach { add("req.short.$it") }
            listOf("corrupted", "mirrored", "equipped", "socketed").forEach { add("state.$it") }
        }

        val known = UiStrings.keys(Lang.RU)
        assertEquals(emptySet(), expected - known, "коды без строк в словаре")
    }

    /**
     * Every stat of the content registry has a title the player can read: the client's own
     * `enum.stat.*` or the server's label in each of its languages (`statTitle` falls back to it).
     */
    @Test
    fun every_stat_of_the_registry_is_named() {
        val index = TestContent.index
        val own = UiStrings.keys(Lang.RU)
        val server = TestContent.serverLocales()
        assertTrue(server.isNotEmpty(), "server locale files not found")
        val unnamed = index.stats.stats.filter { stat ->
            "enum.stat.${stat.code}" !in own && server.any { (_, table) -> LocaleKey.statLabel(stat) !in table }
        }
        assertEquals(emptyList(), unnamed.map { it.code }, "stats without a title")
    }
}
