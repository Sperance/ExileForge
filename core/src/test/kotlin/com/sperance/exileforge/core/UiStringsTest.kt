package com.sperance.exileforge.core

import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.TargetRule
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.bodyPlaces
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.StatDetail
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
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.reward.RedemptionKind
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.trade.LotKind
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
     * An orb's English trade name is one in every language, so it is written once, in the
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

    /**
     * Каждая характеристика героя с разделом сводки описана подробно (`stat.detail.<код>`) на всех языках, и каждая подстановка
     * правил `{r:…}` в описании находит своё число в контенте: число механики не пишется руками и не теряется.
     */
    @Test
    fun every_sheet_stat_has_a_detailed_description_with_numbers() {
        val index = TestContent.index
        val shown = index.stats.stats.filter { index.stats.section(it.code) != null }
        assertTrue(shown.isNotEmpty(), "нет характеристик с разделом - тест ничего не проверил")
        languages.forEach { lang ->
            val table = UiStrings.table(lang)
            val missing = shown.filter { table[StatDetail.key(it.code)].isNullOrBlank() }.map { it.code }
            assertEquals(emptyList(), missing, "${lang.code}: нет подробного описания")
            val unfilled = shown.mapNotNull { stat -> table[StatDetail.key(stat.code)]?.takeIf { index.textNumbers.text(it) == null }?.let { stat.code to it } }
            assertEquals(emptyList(), unfilled, "${lang.code}: подстановка без числа в контенте")
        }
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

    /**
     * Обе формы: `ui("key", ...)` и `ui(lang, "key", ...)`. Ключ с шаблоном (`"enum.slot.$slot"`) чтением не проверить - `$` его исключает,
     * перечисления его кодов обходит тест ниже.
     */
    @Test
    fun every_key_the_sources_ask_for_exists() {
        val sources = listOf(File("src/main/kotlin"), File("../app/src/main/kotlin"))
            .filter { it.isDirectory }
        assertTrue(sources.isNotEmpty(), "исходники не найдены - тест ничего не проверил")

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
     * enums live in `rules`, so a value the server grows without its strings is caught here.
     */
    @Test
    fun every_code_the_client_enumerates_is_named() {
        val expected = buildSet {
            LotKind.entries.forEach { add("enum.lot.${it.name}") }
            com.sperance.exileforge.core.model.auction.LotReturn.entries.forEach { add("auction.returned.${it.name}") }
            Orb.entries.forEach {
                add("enum.orb.${it.name}")
                add("enum.orb.${it.name}.rule")
            }
            RedemptionKind.entries.forEach { add("enum.reward.${it.name}") }
            com.sperance.exileforge.core.display.LoreTerm.entries.forEach {
                add("term.lore.${it.name}")
                add("term.lore.${it.name}.text")
            }
            listOf(com.sperance.exileforge.core.character.GearVerdict.Shift.UP, com.sperance.exileforge.core.character.GearVerdict.Shift.DOWN).forEach { add("verdict.tool.${it.name}") }
            com.sperance.exileforge.core.campaign.ReadingPart.entries.forEach { add("stat.flow.${it.name}") }
            com.sperance.exileforge.core.display.Term.entries.forEach {
                add("term.${it.name}")
                add("term.${it.name}.text")
            }
            MonsterRarity.entries.forEach { add("enum.monster_rarity.${it.name}") }
            Rarity.entries.forEach { add("enum.rarity.${it.name}") }
            com.sperance.exileforge.core.network.SanctionCategory.entries.forEach { add("moderation.category.${it.name}") }
            com.sperance.exileforge.core.model.feedback.ReportTopic.entries.forEach { add("bug.topic.${it.name}") }
            com.sperance.exileforge.core.network.SanctionKind.entries.forEach {
                add("moderation.kind.${it.name}")
                add("moderation.imposed.${it.name}")
                add("moderation.lifted.${it.name}")
            }
            com.sperance.exileforge.core.network.ModerationAction.entries.forEach { add("moderation.action.${it.name}") }
            com.sperance.exileforge.core.network.ModerationSort.entries.forEach {
                add("moderation.sort.${it.name}")
                add("moderation.sort_short.${it.name}")
            }
            com.sperance.exileforge.rules.content.GuildRole.entries.forEach { add("guild.role.${it.name}") }
            com.sperance.exileforge.rules.content.QuestKind.entries.forEach { add("summary.quest_kind.${it.name}") }
            com.sperance.exileforge.rules.roll.WorkMiss.entries.forEach { add("crafts.miss.${it.name}") }
            com.sperance.exileforge.core.forge.ScouredTrait.entries.forEach { add("forge.scoured.${it.name}") }
            TestContent.index.professions.professions.flatMap { it.jobs }.forEach { add("crafts.group.${com.sperance.exileforge.core.crafts.JobGroups.key(it, TestContent.index)}") }
            listOf(Rarity.COMMON, Rarity.MAGIC, Rarity.RARE).forEach { add("feature.blight.quality.${it.name}") }
            listOf(com.sperance.exileforge.rules.run.FeatureKind.SECRET, com.sperance.exileforge.rules.run.FeatureKind.VAULT).forEach { add("feature.room.${it.name}") }
            listOf(com.sperance.exileforge.rules.run.FeatureKind.SECRET, com.sperance.exileforge.rules.run.FeatureKind.VAULT, com.sperance.exileforge.rules.run.FeatureKind.NODE).forEach { add("feature.action.${it.name}") }
            com.sperance.exileforge.rules.run.MapMechanic.entries.forEach { add("tester.mechanic.${it.name}") }
            Slot.entries.forEach { add("enum.slot.${it.name}") }
            bodyPlaces.forEach { add("enum.slot.${it.code}") }
            WeaponType.entries.forEach { add("enum.weapon.${it.name}") }
            SkillNodeType.entries.forEach { add("enum.node.${it.name}") }
            Ailment.entries.forEach {
                add("enum.ailment.${it.name}")
                add("fight.effect.${it.name}")
            }
            Ailment.entries.filter { it.hurts }.forEach { add("fight.flow.stalled.${it.name}") }
            DamageType.entries.forEach { add("enum.damage.${it.name}") }
            AffixKind.entries.forEach { add("mod.kind.${it.name}") }
            AffixKind.entries.filter { it.special }.forEach { add("mod.seal.${it.name}") }
            StatSection.entries.filter { it != StatSection.NONE }.forEach { add("enum.stat_group.${it.name}") }
            TestContent.index.stats.stats.filter { TestContent.index.stats.section(it.code) != null }.forEach { add("stat.desc.${it.code}") }
            TargetRule.entries.forEach { add("fight.rule.${it.name}") }
            com.sperance.exileforge.core.campaign.run.MapSection.entries.forEach { section ->
                add(section.title)
                section.empty?.let(::add)
            }
            com.sperance.exileforge.core.campaign.BuffSource.entries.forEach { add("fight.part_${it.name.lowercase()}") }
            Outcome.entries.forEach { add("expedition.outcome_${it.name.lowercase()}") }
            com.sperance.exileforge.core.campaign.run.CastState.entries.forEach { add("boss.cast.${it.name}") }
            AtlasNodeKind.entries.forEach { add("atlas.kind.${it.name}") }
            AtlasBranch.entries.forEach { add("atlas.branch.${it.name}") }
            AtlasPoints.KINDS.forEach { add("expedition.key_$it") }
            com.sperance.exileforge.core.campaign.run.LootKind.entries.forEach { add("expedition.bag.from.${it.name}") }
            AtlasPoints.ALL.forEach { add("atlas.earned.$it") }
            com.sperance.exileforge.core.campaign.BoostSource.entries.forEach { add("summary.source.${it.name}") }
            SlotCondition.entries.forEach { add("skills.condition.${it.name}") }
            SkillType.entries.forEach { add("skills.type.${it.name}") }
            SkillEvent.entries.forEach { add("skill.on.${it.name}") }
            (DamageType.entries.map { it.name } + "RANDOM").forEach { add("skill.element.$it") }
            (Ailment.entries.map { it.word } + "ELEMENT").forEach { add("skill.ailment.$it") }
            listOf(EssenceBook.VAAL_UPGRADE, EssenceBook.VAAL_SPECIAL, EssenceBook.VAAL_STRONGER).forEach { add("crystal.outcome.$it") }
            listOf("requiredLevel", "requiredStrength", "requiredDexterity", "requiredIntelligence").forEach { add("req.short.$it") }
            listOf("level", "strength", "dexterity", "intelligence").forEach { add("req.need.$it") }
            (listOf("corrupted", "mirrored", "fractured", "crafted", "equipped", "socketed") + com.sperance.exileforge.rules.content.Influence.entries.map { it.name.lowercase() }).forEach { add("state.$it") }
            com.sperance.exileforge.rules.content.Influence.entries.forEach { add("state.${it.name.lowercase()}_hint.map") }
            com.sperance.exileforge.core.display.UniqueAlbum(TestContent.index).pools.forEach { add("history.pool.${it.tag}") }
            TestContent.index.campaign.zones.map { it.biome }.filter { it.isNotBlank() }.forEach { add("expedition.biome.$it") }
            com.sperance.exileforge.rules.content.Counter.SECTIONS.forEach { (section, counters) ->
                add("chronicle.section.$section")
                counters.forEach { add("chronicle.counter.$it") }
            }
            add("chronicle.section.FIGHT")
            com.sperance.exileforge.rules.content.Stat.COMBAT.forEach { add("stats.$it") }
            com.sperance.exileforge.rules.content.Stat.GROUPS.forEach { add("stats.group.$it") }
            com.sperance.exileforge.rules.content.QuestCondition.entries.forEach { add("quest.condition.${it.name}") }
            TestContent.index.campaign.features?.traps?.kinds?.forEach { add("trap.${it.code}") }
            com.sperance.exileforge.core.campaign.combat.FateBeat.entries.forEach { add("expedition.fate_beat.${it.name}") }
            com.sperance.exileforge.core.campaign.LogKind.entries.forEach { add("fight.log_shelf.${it.name}") }
            com.sperance.exileforge.core.campaign.NoteKind.entries.forEach { add("trace.note.value.${it.name}") }
            com.sperance.exileforge.core.campaign.FactorKey.entries.forEach {
                add("trace.factor.${it.name}")
                add("trace.factor.${it.name}.about")
            }
            com.sperance.exileforge.rules.content.BuffKind.entries.forEach { add("fight.buff.${it.name}") }
            com.sperance.exileforge.rules.content.ChargeKind.entries.forEach { add("fight.charge.${it.name}") }
            com.sperance.exileforge.core.character.FlaskMeasure.entries.filter { it != com.sperance.exileforge.core.character.FlaskMeasure.LINE && it != com.sperance.exileforge.core.character.FlaskMeasure.MECHANIC }.forEach { add("flask.measure.${it.name}") }
            com.sperance.exileforge.core.network.SanctionTarget.entries.forEach { add("moderation.target.${it.name}") }
            com.sperance.exileforge.rules.content.Catalyst.entries.forEach { add("enum.catalyst.${it.name}") }
            com.sperance.exileforge.rules.rift.RiftRest.entries.forEach { add("rift.rest.${it.name}") }
            Rarity.entries.forEach {
                add("quest.rarity.${it.name}")
                add("stash.rarity_many.${it.name}")
            }
            listOf("MISSING_LIFE_DAMAGE", "AVENGE_CRIT", "HIT_CAP", "ALTERNATE_SKILL", "MANA_OVERFLOW", "STORE_TAKEN", "FULL_LIFE_GUARD", "OVERHEAL_BARRIER", "WIN_STREAK", "KILL_STACKS")
                .forEach { add("fight.fate_state.$it") }
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

    /**
     * Узел дерева, дар и рычаг дара, что называет окно разбора, названы в каждом словаре сервера: кода на их месте
     * клиент не пишет, дыра была бы заглушкой - её ловит этот тест, а не игрок.
     */
    @Test
    fun every_tree_node_and_fate_the_breakdown_names_is_named() {
        val index = TestContent.index
        val server = TestContent.serverLocales()
        assertTrue(server.isNotEmpty(), "server locale files not found")
        val keys = index.tree.byCode.keys.map(LocaleKey::skillNodeName) +
            index.fates.all.map { LocaleKey.fateName(it.code) } +
            com.sperance.exileforge.rules.content.FateLever.entries.map { LocaleKey.fateLever(it.name) }
        val holes = server.flatMap { (lang, table) -> keys.filterNot(table::containsKey).map { "$lang: $it" } }
        assertEquals(emptyList(), holes, "names missing from the server dictionaries")
    }

    /**
     * Имя характеристики у клиента - то же, что у сервера: своя подпись `enum.stat.*` - лишь
     * копия серверной с одной меткой «%» единицы (у сервера она уже может быть - тогда вторая не ставится), а не второе имя той же
     * характеристики.
     */
    @Test
    fun the_client_names_a_stat_as_the_server_does() {
        val server = TestContent.serverLocales()
        val mark = Regex("""[,\s]*%\s*$""")
        var compared = 0
        val drift = languages.flatMap { lang ->
            val table = server[lang.code] ?: return@flatMap listOf("${lang.code}: no server dictionary")
            UiStrings.keys(lang).filter { it.startsWith("enum.stat.") }.mapNotNull { key ->
                val label = table["enum.EnumStatStock.${key.removePrefix("enum.stat.")}"] ?: return@mapNotNull null
                compared++
                val own = ui(lang, key).replace(mark, "")
                "${lang.code} $key: «$own», у сервера «$label»".takeIf { own != label.replace(mark, "") }
            }
        }
        assertTrue(compared > 0, "no stat name compared: the check proves nothing")
        assertEquals(emptyList(), drift, "client stat names differ from the server's")
    }

    /** Окно загрузки: подсказок и лора хватает на выбор, и следующая строка никогда не повторяет прошлую. */
    @Test
    fun launch_lines_never_repeat() {
        val keys = com.sperance.exileforge.core.i18n.LaunchLines.keys(Lang.RU)
        assertTrue(keys.size >= 2, "launch lines: $keys")
        keys.forEach { last -> repeat(10) { seed -> assertTrue(com.sperance.exileforge.core.i18n.LaunchLines.next(keys, last, kotlin.random.Random(seed)) != last) } }
    }
}
