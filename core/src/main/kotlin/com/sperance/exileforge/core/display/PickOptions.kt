package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.rules.content.AdditiveEffect
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreAttribute
import com.sperance.exileforge.rules.content.GuildRank
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SmithCategory
import com.sperance.exileforge.rules.content.SmithChoice
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
     * Узлы древа [nodes] по порядку, ключ - сам узел: поиск находит узел по имени и по видимому тексту его строк и вариантов выбора с
     * числами ([lineText]); кодов узлов и имён характеристик в словах поиска нет. Список собирается на язык интерфейса: экран держит
     * его на язык, а не на каждый запрос.
     */
    fun nodes(index: ContentIndex, nodes: List<TreeNode> = index.content.tree.nodes): List<PickOption<TreeNode>> = nodes.map { node ->
        PickOption(node, nodeTitle(node.code), terms = (node.lines + node.options.flatten()).map { lineText(index, it) })
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

    /**
     * Варианты работы с выбором [work] профессии [profession] - [choices], ключ - выбор варианта (`JobView.choice`). Книги - группами
     * «Класс · <класс>» и «Общие», прочие работы - без групп; в группе - по уровню открытия, закрытые для профессии
     * ([ProfessionView.opens]: уровень и открытость варианта по ответу сервера) - в конце с замком и «с ур. N» (уровень не тот) или
     * «закрыто». Имя - что выйдет: умение книги и ритуала, стопка, база инструмента; справа - сколько этого в сумке ([bag]; у
     * ритуала - книг его умения), у базы инструмента - её уровень.
     */
    fun jobChoices(work: JobView, choices: List<JobView>, profession: ProfessionView, index: ContentIndex?, bag: (String) -> Long): List<PickOption<String>> {
        val common = { option: JobView -> work.kind == JobKind.BOOK && index?.skills?.byBook(option.choice)?.common == true }
        return choices.sortedWith(compareBy<JobView>({ common(it) }, { !profession.opens(it) }, { it.level }))
            .map { option -> jobChoice(work.kind, option, lockOf(profession, option), index, bag) }
    }

    /** Почему вариант [option] закрыт для профессии [profession]: уровень не тот - «с ур. N», иначе «закрыто»; открыт - null. */
    private fun lockOf(profession: ProfessionView, option: JobView): String? = when {
        profession.opens(option) -> null
        option.level > profession.level -> ui("picker.from_level", option.level)
        else -> ui("picker.closed")
    }

    /** Вариант [option] работы вида [kind]; [locked] - почему он закрыт, открыт - null. */
    private fun jobChoice(kind: JobKind, option: JobView, locked: String?, index: ContentIndex?, bag: (String) -> Long): PickOption<String> {
        val open = locked == null
        val code = option.choice
        val skill = when (kind) {
            JobKind.BOOK -> index?.skills?.byBook(code)
            JobKind.EVOLVE -> index?.skills?.byCode?.get(code)
            else -> null
        }
        val template = index?.template(code)?.takeIf { kind == JobKind.TOOL }
        val item = index?.item(ItemCode(code))
        val stack = skill?.book ?: code.takeIf { item != null }
        val held = stack?.let(bag)
        val kindLine = skill?.let { ui("skills.type.${it.type.name}") }
        return PickOption(
            key = code,
            title = skill?.let { SkillText.title(it.code) } ?: choiceTitle(code),
            subtitle = listOfNotNull(kindLine, template?.let { rarityTitle(it.rarity) }, ui("picker.from_level", option.level).takeIf { open }).joinToString(" · ").ifEmpty { null },
            brief = listOfNotNull(kindLine, held?.let { ui("picker.in_bag_short", it) }).joinToString(" · ").ifEmpty { null },
            glyph = item?.let { PickGlyph.Emblem(bagVisualKind(it)) } ?: template?.let { PickGlyph.Emblem(itemVisualKind(it)) } ?: PickGlyph.Sign(Glyph.LEVEL),
            accent = template?.let { PickAccent.OfRarity(it.rarity) } ?: skill?.takeIf { kind == JobKind.BOOK }?.let { PickAccent.Band(if (it.common) 1 else 0) },
            data = template?.let { PickData(it.level.toString(), ui("picker.level")) } ?: held?.let { PickData(it.toString(), ui("picker.in_bag"), lacking = it == 0L) },
            group = skill?.takeIf { kind == JobKind.BOOK }?.let { if (it.common) ui("crafts.books_common") else ui("crafts.books_class", classTitle(it.heroClass)) },
            locked = locked,
        )
    }

    /**
     * Выбор кузнеца, ключ - имя [SmithChoice]: группы «Оружие», «Броня» и «Другое» (украшения и случайная ковка), подназвание - какие базы
     * выйдут, справа - сколько самоцвета выбора в сумке ([bag]; у украшений - того, которого меньше всего), нет - ноль красным.
     */
    fun smiths(bag: (String) -> Long): List<PickOption<String>> = SmithChoice.entries.map { choice ->
        val category = choice.category
        val gem = choice.gems.minByOrNull(bag)
        PickOption(
            key = choice.name,
            title = choiceTitle(choice.name),
            subtitle = choice.attribute?.let { ui("crafts.smith_hint", attributeTitle(it)) } ?: ui(if (category != null) "crafts.smith_hint_jewellery" else "crafts.random_hint"),
            brief = gem?.let { "${itemTitle(it)} · ${ui("picker.in_bag_short", bag(it))}" },
            glyph = when (category) {
                SmithCategory.WEAPON -> PickGlyph.Emblem(ItemVisualKind.SWORD)
                SmithCategory.ARMOUR -> PickGlyph.Emblem(ItemVisualKind.ARMOR)
                SmithCategory.JEWELLERY -> PickGlyph.Emblem(ItemVisualKind.RING)
                null -> PickGlyph.Sign(Glyph.CRAFT)
            },
            accent = choice.attribute?.let { PickAccent.OfAttribute(it) },
            data = gem?.let { PickData(bag(it).toString(), itemTitle(it), lacking = bag(it) == 0L) },
            group = category?.takeIf { it.attributed }?.let { ui("merchant.group.${it.name}") } ?: ui("crafts.smith_group_other"),
        )
    }

    /**
     * Вещи [views] - цель работы над вещью, инструменты профессии; ключ - id копии, полоса - цвет редкости, справа - уровень. Вещь с
     * невыполненными требованиями [unmet] (причины правил, как у строки вещи) закрыта первой из них и стоит в конце.
     */
    fun copies(views: List<ItemView>, unmet: (ItemView) -> List<String> = { emptyList() }): List<PickOption<String>> = views.map { it to unmet(it) }.sortedBy { it.second.isNotEmpty() }.map { (view, needs) ->
        PickOption(
            key = view.id,
            title = view.title,
            subtitle = "${rarityTitle(view.rarity)} · ${slotTitle(view.slot)}",
            brief = ui("common.level_short", view.level),
            glyph = PickGlyph.Emblem(view.visualKind),
            accent = PickAccent.OfRarity(view.rarity),
            data = PickData(view.level.toString(), ui("picker.level")),
            locked = needs.firstOrNull()?.let(::requirementReason),
        )
    }

    /**
     * Добавки работы [offered] (код - эффект), ключ - код стопки: подназвание - что она даст, справа - сколько в сумке ([bag]); которой
     * нет - закрыта «нет в сумке» и стоит в конце.
     */
    fun additives(offered: Map<String, AdditiveEffect>, index: ContentIndex?, bag: (String) -> Long): List<PickOption<String>> = offered.entries
        .sortedBy { bag(it.key) == 0L }
        .map { (code, effect) ->
            val held = bag(code)
            PickOption(
                key = code,
                title = itemTitle(code),
                subtitle = additiveTitle(effect, index),
                glyph = index?.item(ItemCode(code))?.let { PickGlyph.Emblem(bagVisualKind(it)) },
                data = PickData(held.toString(), ui("picker.in_bag")).takeIf { held > 0 },
                locked = ui("picker.none_in_bag").takeIf { held == 0L },
            )
        }

    /** Имя атрибута - главного требования базы. */
    private fun attributeTitle(attribute: CoreAttribute): String = when (attribute) {
        CoreAttribute.STRENGTH -> ui("req.strength")
        CoreAttribute.DEXTERITY -> ui("req.dexterity")
        CoreAttribute.INTELLIGENCE -> ui("req.intelligence")
    }
}
