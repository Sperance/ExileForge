package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.GuildRank
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.rules.reward.RedemptionKind

/**
 * Варианты общего выбора из списка по смыслу: подписи, данные, группы и цвета собираются здесь, экраны лишь передают их выбору.
 * Слова - на языке интерфейса.
 */
object PickOptions {
    /**
     * Ранги гильдии [ranks] по порядку правил, ключ - номер ранга: подназвание - порог вклада («С вступления» у нулевого), справа -
     * сколько членов гильдии [members] сейчас в ранге; состава нет - без числа.
     */
    fun ranks(ranks: List<GuildRank>, members: List<GuildMember>?): List<PickOption<Int>> = ranks.mapIndexed { step, rank ->
        val from = number(rank.from.toDouble())
        PickOption(
            key = step,
            title = GuildText.rank(rank.code),
            subtitle = if (rank.from > 0) ui("picker.rank_from", from) else ui("picker.rank_open"),
            brief = if (rank.from > 0) ui("picker.rank_from_short", from) else null,
            glyph = PickGlyph.Sign(Glyph.CHARACTER),
            accent = PickAccent.Step(step, ranks.size),
            data = members?.let { all -> PickData(all.count { it.rank == rank.code }.toString(), ui("picker.in_rank")) },
        )
    }

    /** Зоны карт по регионам кампании (группа - регион), в регионе - по уровню: подназвание - босс зоны, справа - уровень. */
    fun zones(index: ContentIndex): List<PickOption<String>> = index.campaign.regions.flatMapIndexed { band, region ->
        val group = regionTitle(region.code)
        region.zones.sortedBy { it.level }.map { zone ->
            PickOption(
                key = zone.code.value,
                title = mapTitle(zone.code),
                subtitle = ui("picker.boss", monsterTitle(zone.boss)),
                brief = ui("common.level_short", zone.level),
                glyph = PickGlyph.Sign(Glyph.MAP),
                accent = PickAccent.Band(band),
                data = PickData(zone.level.toString(), ui("picker.level")),
                group = group,
            )
        }
    }

    /** Стопки контента по полкам сумки (группа - полка), на полке - по имени: подназвание - действие сферы или оттиска, если есть. */
    fun items(index: ContentIndex): List<PickOption<String>> = index.items.values
        .map { item -> Triple(item, BagCategory.of(item), itemTitle(item.code.value)) }
        .sortedWith(compareBy({ it.second.ordinal }, { it.third }))
        .map { (item, shelf, title) ->
            val group = ui(shelf.key)
            PickOption(
                key = item.code.value,
                title = title,
                subtitle = itemRule(item.code.value).ifBlank { null },
                brief = group,
                glyph = PickGlyph.Emblem(bagVisualKind(item)),
                accent = PickAccent.Band(shelf.ordinal),
                group = group,
            )
        }

    /**
     * Шаблоны снаряжения по месту (группа - место), в месте - по уровню и имени: подназвание - редкость и код шаблона (поиск по коду),
     * на чипе - редкость и уровень, справа - уровень, полоса - цвет редкости.
     */
    fun templates(index: ContentIndex): List<PickOption<String>> = index.templates.values
        .sortedWith(compareBy({ it.slot.ordinal }, { it.level }, { equipmentTitle(it.code) }))
        .map { template ->
            val rarity = rarityTitle(template.rarity)
            PickOption(
                key = template.code,
                title = equipmentTitle(template.code),
                subtitle = "$rarity · ${template.code}",
                brief = "$rarity · ${ui("common.level_short", template.level)}",
                glyph = PickGlyph.Emblem(itemVisualKind(template)),
                accent = PickAccent.OfRarity(template.rarity),
                data = PickData(template.level.toString(), ui("picker.level")),
                group = slotTitle(template.slot),
            )
        }

    /** Редкости по порядку правил, полоса - цвет редкости; [any] - первым вариант «любая» (ключ null). */
    fun rarities(any: String? = null): List<PickOption<Rarity>> = listOfNotNull(any?.let { PickOption<Rarity>(null, it, glyph = PickGlyph.Sign(Glyph.RARITY)) }) +
        Rarity.entries.map { PickOption(it, rarityTitle(it), glyph = PickGlyph.Sign(Glyph.RARITY), accent = PickAccent.OfRarity(it)) }

    /** Места вещей; [any] - первым вариант «любое» (ключ null). */
    fun slots(any: String? = null): List<PickOption<Slot>> = listOfNotNull(any?.let { PickOption<Slot>(null, it, glyph = PickGlyph.Sign(Glyph.ITEM)) }) +
        Slot.entries.map { PickOption(it, slotTitle(it), glyph = PickGlyph.Sign(Glyph.ITEM)) }

    /** Профессии контента, ключ - код, подназвание - их инструмент; [all] - первым вариант «все» (ключ null). */
    fun professions(index: ContentIndex, all: String): List<PickOption<String>> = listOf(PickOption<String>(null, all, glyph = PickGlyph.Sign(Glyph.CRAFT))) +
        index.professions.professions.map { PickOption(it.code, professionTitle(it.code), subtitle = slotTitle(it.tool), glyph = PickGlyph.Sign(Glyph.CRAFT)) }

    /**
     * Что может назвать награда кода вида [kind] из контента [index]: стопки по полкам сумки или шаблоны по месту и уровню, ключ - код.
     * Пусто у видов, что ничего не называют, и пока контента нет.
     */
    fun rewards(index: ContentIndex?, kind: RedemptionKind): List<PickOption<String>> = when {
        index == null -> emptyList()
        kind == RedemptionKind.ITEM -> items(index)
        kind == RedemptionKind.EQUIPMENT -> templates(index)
        else -> emptyList()
    }

    /**
     * Узлы древа [nodes] по порядку, ключ - код: поиск находит узел по имени и по видимому тексту его строк и вариантов выбора с
     * числами ([lineText]); кодов узлов и имён характеристик в словах поиска нет. Список собирается на язык интерфейса: экран держит
     * его на язык, а не на каждый запрос.
     */
    fun nodes(index: ContentIndex, nodes: List<TreeNode> = index.content.tree.nodes): List<PickOption<String>> = nodes.map { node ->
        PickOption(node.code, nodeTitle(node.code), terms = (node.lines + node.options.flatten()).map { lineText(index, it) })
    }

    /** Виды награды кода по порядку правил. */
    fun redemptionKinds(): List<PickOption<RedemptionKind>> = RedemptionKind.entries.map { kind ->
        val glyph = when (kind) {
            RedemptionKind.ITEM -> Glyph.ITEM
            RedemptionKind.EQUIPMENT -> Glyph.DEFENCE
            RedemptionKind.EXPERIENCE -> Glyph.LEVEL
            RedemptionKind.GOLD -> Glyph.CURRENCY
        }
        PickOption(kind, ui("enum.reward.${kind.name}"), glyph = PickGlyph.Sign(glyph))
    }
}
