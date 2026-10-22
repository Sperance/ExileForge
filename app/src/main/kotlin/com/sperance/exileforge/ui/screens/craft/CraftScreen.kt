package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.ForgeSection
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.QualityForecast
import com.sperance.exileforge.core.display.Term
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemDetails
import com.sperance.exileforge.core.display.itemRule
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.rangedLine
import com.sperance.exileforge.core.display.tierOf
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.forge.ForgeTarget
import com.sperance.exileforge.core.forge.OrbChoice
import com.sperance.exileforge.core.forge.QualityBatch
import com.sperance.exileforge.core.forge.Smithy
import com.sperance.exileforge.core.forge.SmithyModel
import com.sperance.exileforge.core.forge.forgeTarget
import com.sperance.exileforge.core.forge.hiddenOrbs
import com.sperance.exileforge.core.forge.orbChoices
import com.sperance.exileforge.core.i18n.ruleRefusal
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** A bench "line" that is not a recipe: taking the crafted modifier back off. */
internal const val UNCRAFT = "-"

/**
 * The forge as an anvil: a rail of the picker's sections beside the anvil («Тип» общего
 * фильтра выбора вещи), which holds the target, the orb, essence or bench line laid on it and, for an orb, its omen — and shows the
 * target as it stands. Under it the section's tray (знамения, оттиски, эссенции) or the bench's lines. Полоса над навигацией - рецепт
 * связки, ряд сфер и удерживаемая кнопка, because an orb is spent the moment it is used.
 *
 * Цель - [ForgeTarget] (зеркало правил [OrbTarget]). Every rule is the server's; what a target takes is the rules' own [OrbApplier.accepts].
 *
 * Сферы и материалы читаются из сумки - кузня открывается на свежем герое. Ряд сфер полосы - только сферы, что лягут на цель сами или со знамением
 * сумки; переставшая ложиться выбранная сфера снимается. Печати Атласа - только на карту. Эссенции, что не лягут, - серыми с отказом правил
 * словами игрока. Сфера качества говорит, что поднимет и сколько качества другого вида сбросится; сфера удачи - какие уникалки может дать
 * вещь.
 */
@Composable fun CraftScreen() {
    val vm = koinViewModel<SmithyViewModel>().model
    val game by vm.game.collectAsStateWithLifecycle()
    val smithy by vm.smithy.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) { vm.ensure() }
    val hero = game.hero
    val index = game.index
    val target = remember(hero, index, game.holding.selectedEquipment) { game.forgeTarget() }
    val enabled = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    var picking by remember { mutableStateOf(false) }
    val cards = rememberForgeCards(game)
    val pickFilter = rememberItemFilter(ItemShelf.FORGE)
    var benchLine by remember(target?.id) { mutableStateOf("") }
    var engraving by remember(target?.id) { mutableStateOf(false) }
    val omens: List<Omen> = remember(index, hero?.bag) { if (hero == null || index == null) emptyList() else heldOmens(index, hero) }
    val choices = remember(target, index, omens, hero?.bag) {
        if (index == null || hero == null || target == null) emptyList() else target.orbChoices(index, game.orbs.map { it.code.value }, omens, hero::count)
    }
    val hidden = remember(target, index, omens, hero?.bag) {
        if (index == null || hero == null || target == null) emptyList() else target.hiddenOrbs(index, game.orbs.map { it.code.value }, omens, hero::count)
    }
    LaunchedEffect(target?.id, choices, smithy.orb) {
        if (target != null && smithy.orb.isNotBlank() && choices.none { it.code == smithy.orb }) vm.selectOrb("")
    }
    val seals = remember(target, index, omens, hero?.bag) {
        if (index == null || hero == null || target == null) emptyList() else target.seals(index, omens, hero::count)
    }
    LaunchedEffect(target?.id, seals, smithy.seal) {
        if (target != null && smithy.seal.isNotBlank() && seals.none { it.code == smithy.seal }) vm.selectSeal("")
    }
    val gear = target as? ForgeTarget.Gear
    val essenceRefusals = remember(gear, index, hero?.bag) {
        if (index == null || gear == null || hero == null) emptyMap() else essenceRefusals(index, hero, gear)
    }
    val quality = remember(target, index, smithy.omen) { index?.let { target?.quality(it, Omen.of(smithy.omen)) } }
    val batch = remember(target, index, smithy.orb, smithy.omen, hero?.bag, hero?.money) {
        val orb = Orb.of(smithy.orb)
        if (index == null || hero == null || target == null || orb == null) null else QualityBatch.of(index, target, orb, Omen.of(smithy.omen), hero::count, hero.money)
    }
    val chanceUniques = remember(target, index) { index?.let { target?.chanceUniques(it) }.orEmpty() }
    val sections = target?.sections ?: listOf(ForgeSection.ORBS)
    val section = smithy.section.takeIf { it in sections } ?: ForgeSection.ORBS
    val chosen = choices.firstOrNull { it.code == smithy.orb }
    val sealChosen = seals.firstOrNull { it.code == smithy.seal }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hero == null || index == null) {
                InfoCard(ui("tree.no_hero"), ui("craft.hero_first"))
                return@Column
            }
            if (sections.size > 1) PillTabs(sections.map { ui(it.title) }, sections.indexOf(section), { vm.section(sections[it]) }, segmented = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetRail(pickFilter) { picking = true }
                Anvil(
                    game,
                    cards,
                    target,
                    toolSocket(game, smithy, section, index, benchLine, if (section == ForgeSection.SEALS) sealChosen else chosen, essenceRefusals),
                    omenSocket(game, smithy, section, vm::selectOmen),
                    onPick = { picking = true },
                    modifier = Modifier.weight(1f),
                )
            }
            when (section) {
                ForgeSection.ORBS -> target?.let {
                    LineChoices(game, it, enabled, vm)
                    OmenLedger(game, smithy.orb, smithy.omen, it.orbTarget, omens, vm::selectOmen)
                }

                ForgeSection.SEALS -> gear?.let {
                    LineChoices(game, it, enabled, vm)
                    OmenTray(game, ui("forge.tray_seal_omens"), it.sealOmens(index, smithy.seal, omens), smithy.omen, vm::selectOmen)
                    SealTray(game, smithy, seals, vm::selectSeal)
                }

                ForgeSection.BENCH -> gear?.let { BenchLedger(game, index, hero, it.view, benchLine) { line -> benchLine = line } }

                ForgeSection.ESSENCES -> EssenceTray(game, smithy.essence, essenceRefusals, vm::selectEssence)
            }
        }
        if (hero != null && index != null && target != null) {
            val recipe = remember(section, target, smithy, choices, seals, quality, chanceUniques, essenceRefusals, benchLine, hero.bag, game.bench) {
                when (section) {
                    ForgeSection.ORBS -> orbRecipe(game, index, smithy, target, chosen, choices.isEmpty(), quality, chanceUniques)
                    ForgeSection.SEALS -> sealRecipe(game, index, smithy, sealChosen, seals.isEmpty())
                    ForgeSection.ESSENCES -> gear?.let { essenceRecipe(game, index, hero, it, smithy.essence, essenceRefusals) } ?: Recipe(null, null)
                    ForgeSection.BENCH -> benchRecipe(game, benchLine)
                }
            }
            when (section) {
                ForgeSection.ORBS -> OrbBar(
                    game,
                    smithy,
                    target,
                    recipe,
                    choices,
                    hidden,
                    enabled,
                    quality?.lost ?: 0,
                    batch,
                    onSelect = { code ->
                        vm.selectOrb(code)
                        engraving = code == Orb.ENGRAVERS_ORB.name
                    },
                    onPickLine = { engraving = true },
                ) { code, count -> vm.applyOrb(target, code, count = count) }

                ForgeSection.SEALS -> gear?.let { SealBar(game, smithy, it, recipe, sealChosen, seals.isEmpty(), enabled) { code -> vm.applySeal(it.id, code) } }

                ForgeSection.BENCH -> gear?.let { BenchBar(game, vm, recipe, it.view, benchLine, enabled) }

                ForgeSection.ESSENCES -> gear?.let { EssenceBar(game, recipe, smithy.essence, it.view, enabled, { code -> code !in essenceRefusals }, vm::applyEssence) }
            }
        }
    }
    if (engraving && target != null && smithy.orb == Orb.ENGRAVERS_ORB.name) {
        EngraveSheet(game, target, enabled, onDismiss = { engraving = false }) { line ->
            vm.applyOrb(target, Orb.ENGRAVERS_ORB.name, line)
            engraving = false
        }
    }
    if (picking) {
        TargetPicker(game, cards, pickFilter, target, onDismiss = { picking = false }) {
            vm.select(it)
            picking = false
        }
    }
}

/** Почему каждая эссенция сумки не ляжет на вещь [gear]: отказ правил словами игрока; эссенции, что ляжет, в карте нет. */
private fun essenceRefusals(index: ContentIndex, hero: HeroView, gear: ForgeTarget.Gear): Map<String, String> {
    val applier = OrbApplier(index)
    val view = gear.view
    return index.essences.essences.values.filter { hero.count(it.code) > 0 && !applier.accepts(it, view.item, view.template) }.associate { essence ->
        essence.code to (ruleRefusal { applier.applyEssence(essence, view.item.copy(), view.template, Dice(0)) } ?: ui("forge.essence_refused"))
    }
}

/** Строки на выбор, что ждут решения над вещью: скрытая строка ([REVEALS] - аффикс или судьбоносная) и знамение выбора. */
@Composable private fun LineChoices(game: GameUi, target: ForgeTarget, enabled: Boolean, vm: SmithyModel) {
    when (target) {
        is ForgeTarget.Gear -> {
            val item = target.view.item
            REVEALS.firstOrNull { it.pending(item) }?.Choice(game, item, enabled, vm::unveil)
            LineChoice(game, item, item.offer, "forge.choice_title", "forge.choice_hint", enabled, vm::choose)
        }
    }
}

internal val ForgeSection.title get() = when (this) {
    ForgeSection.ORBS -> "forge.section_orbs"
    ForgeSection.SEALS -> "forge.section_seals"
    ForgeSection.BENCH -> "forge.section_bench"
    ForgeSection.ESSENCES -> "forge.section_essences"
}

/**
 * The anvil's tool socket: the orb or the Atlas seal ([chosen] из лотка своего раздела), the essence or the bench line the section lays
 * on the target, when one is chosen and fits.
 */
internal fun toolSocket(game: GameUi, smithy: Smithy, section: ForgeSection, index: ContentIndex, benchLine: String, chosen: OrbChoice?, essenceRefusals: Map<String, String>): Socket = when (section) {
    ForgeSection.ORBS -> {
        val glyph: (@Composable () -> Unit)? = chosen?.let { { BagIcon(it.code, Modifier.fillMaxSize(), kind = ItemVisualKind.CURRENCY) } }
        Socket(Gold, glyph)
    }

    ForgeSection.SEALS -> {
        val glyph: (@Composable () -> Unit)? = chosen?.let { { BagIcon(it.code, Modifier.fillMaxSize(), kind = ItemVisualKind.CURRENCY) } }
        Socket(Gold, glyph)
    }

    ForgeSection.ESSENCES -> {
        val code = smithy.essence.takeIf { index.essence(it) != null && (game.bagAmount(it) ?: 0L) > 0 && it !in essenceRefusals }
        val glyph: (@Composable () -> Unit)? = if (code == null) {
            null
        } else {
            { BagIcon(code, Modifier.fillMaxSize(), tint = Elder) }
        }
        Socket(Elder, glyph)
    }

    ForgeSection.BENCH -> {
        val glyph: (@Composable () -> Unit)? = if (benchLine.isBlank()) {
            null
        } else {
            { Icon(ForgeGlyphs.Anvil, null, tint = Crafted, modifier = Modifier.fillMaxSize()) }
        }
        Socket(Crafted, glyph)
    }
}

/** The omen's socket beside an orb or a seal: the omen laid with it, a tap taking it off; none for the bench or an essence. */
internal fun omenSocket(game: GameUi, smithy: Smithy, section: ForgeSection, onSelect: (String) -> Unit): Socket? {
    if (!section.omens) return null
    val omen = smithy.omen.takeIf { it.isNotBlank() && (game.bagAmount(it) ?: 0L) > 0 }
    val glyph: (@Composable () -> Unit)? = if (omen == null) {
        null
    } else {
        { Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.fillMaxSize()) }
    }
    return Socket(
        Rune,
        glyph,
        if (omen == null) {
            null
        } else {
            { onSelect("") }
        },
    )
}

/** Знамение [code], что лежит рядом со сферой или оттиском: выбрано и есть в сумке, иначе null. */
private fun laidOmen(game: GameUi, code: String): String? = code.takeIf { it.isNotBlank() && (game.bagAmount(it) ?: 0L) > 0 }

/**
 * Описание правил для подсказки итога: [parts] без пустых и без того, что уже напечатано итогом ([effect]); у знамения-катализатора
 * ([omen]) - ещё общее правило катализаторов термином.
 */
private fun recipeDetail(index: ContentIndex, effect: String?, omen: String?, vararg parts: String?): String {
    val catalyst = Omen.of(omen.orEmpty())?.catalyst?.let { "${Term.CATALYST.title}: ${Term.CATALYST.text(index.campaign.combat)}" }
    val omenDetails = omen?.let { itemDetails(it, index) }
    return (parts.toList() + omenDetails + catalyst).filterNotNull().filter { it.isNotBlank() && it != effect }.distinct().joinToString("\n\n")
}

/**
 * Рецепт сферы: имя сферы, знамение и итог - ждёт знамения; у сферы качества - что она поднимет на вещи (или отказ правил);
 * со знамением - что оно меняет; иначе действие сферы одной фразой. В подсказке - полное описание сферы и, у сферы удачи, какие
 * уникалки может дать вещь; красным - что сбросит качество и что снимет Очищение ([ForgeTarget.scoured]).
 */
internal fun orbRecipe(
    game: GameUi,
    index: ContentIndex,
    smithy: Smithy,
    target: ForgeTarget?,
    chosen: OrbChoice?,
    empty: Boolean,
    quality: QualityForecast?,
    chanceUniques: List<String>,
): Recipe {
    if (target == null) return Recipe(null, null)
    if (chosen == null) return Recipe(null, ui(if (empty) target.noOrbsKey else "forge.pick_orb"))
    val code = chosen.code
    val omen = laidOmen(game, smithy.omen)
    val forecast = quality?.takeIf { code == Orb.QUALITY_ORB.name }
    val (effect, tone) = when {
        chosen.needsOmen && smithy.omen.isBlank() -> ui("forge.needs_omen") to Rune

        else -> forecast?.refusal?.let { it to LifeRed }
            ?: forecast?.raises?.takeIf { it.isNotBlank() }?.let { it to Vital }
            ?: (omen?.let { itemDescription(it, index) } ?: itemRule(code)) to Vital
    }
    val uniques = ui("forge.chance_uniques", chanceUniques.joinToString(", ") { equipmentTitle(it) }).takeIf { code == Orb.ORB_OF_CHANCE.name && chanceUniques.isNotEmpty() }
    val scoured = Orb.of(code)?.let(target::scoured).orEmpty()
    return Recipe(
        tool = itemTitle(code),
        effect = effect,
        tone = tone,
        omen = omen?.let(::itemTitle),
        detail = recipeDetail(index, effect, omen, itemDescription(code, index), itemDetails(code, index), uniques),
        warnings = listOfNotNull(
            forecast?.lost?.takeIf { it > 0 }?.let { ui("forge.quality_reset", it) },
            scoured.takeIf { it.isNotEmpty() }?.let { strips -> ui("forge.scour_strips", strips.joinToString(", ") { ui("forge.scoured.${it.name}") }) },
        ),
    )
}

/** Рецепт оттиска Атласа: имя, знамение и итог - ждёт знамения, что меняет знамение или действие оттиска одной фразой. */
internal fun sealRecipe(game: GameUi, index: ContentIndex, smithy: Smithy, chosen: OrbChoice?, empty: Boolean): Recipe {
    if (chosen == null) return Recipe(null, ui(if (empty) "forge.no_seals" else "forge.pick_seal"))
    val omen = laidOmen(game, smithy.omen)
    val (effect, tone) = when {
        chosen.needsOmen && smithy.omen.isBlank() -> ui("forge.needs_omen") to Rune
        else -> (omen?.let { itemDescription(it, index) } ?: itemRule(chosen.code)) to Vital
    }
    return Recipe(itemTitle(chosen.code), effect, tone, omen?.let(::itemTitle), recipeDetail(index, effect, omen, itemDescription(chosen.code, index), itemDetails(chosen.code, index)))
}

/**
 * Рецепт эссенции: имя и что гарантия даст именно этой вещи - «Даст T5 из 6: +(12–18)% к урону огнём» (`OrbApplier.essenceGuarantee`);
 * не ляжет - отказ правил красным. В подсказке - описание эссенции и общее правило эссенций.
 */
internal fun essenceRecipe(game: GameUi, index: ContentIndex, hero: HeroView, gear: ForgeTarget.Gear, code: String, refusals: Map<String, String>): Recipe {
    val essence = index.essence(code)?.takeIf { (game.bagAmount(code) ?: 0L) > 0 }
    if (essence == null) {
        val any = index.itemsByCategory[Item.ESSENCE].orEmpty().any { hero.count(it.code.value) > 0 }
        return Recipe(null, ui(if (any) "forge.pick_essence" else "forge.no_essences"))
    }
    val refusal = refusals[code]
    val view = gear.view
    val gives = if (refusal != null) {
        null
    } else {
        OrbApplier(index).essenceGuarantee(essence, view.item, view.template)?.let { roll ->
            index.modifier(roll.code)?.let { def -> def.tier(roll.tier)?.let { tier -> ui("forge.essence_gives", tierOf(roll.tier, def.tiers.size), rangedLine(index, def.code, tier.values)) } }
        }
    }
    val (effect, tone) = refusal?.let { it to LifeRed } ?: ((gives ?: itemDescription(code, index)) to Elder)
    return Recipe(itemTitle(code), effect, tone, detail = recipeDetail(index, effect, null, itemDescription(code, index), itemDetails(code, index), ui("essence.note")))
}

/**
 * Рецепт верстака: строку называет сам выбранный ряд верстака, поэтому рецепт лишь просит выбрать; снятие ремесленного
 * модификатора называет себя.
 */
internal fun benchRecipe(game: GameUi, benchLine: String): Recipe = when {
    benchLine == UNCRAFT -> Recipe(ui("bench.remove"), null)
    benchLine.isNotBlank() && game.bench.any { it.code == benchLine } -> Recipe(null, null)
    else -> Recipe(null, ui("forge.pick_line"))
}
