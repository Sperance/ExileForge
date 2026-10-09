package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.content.PetSpecies
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.hybridOf
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** A species as the dictionary names it. */
fun petName(species: String): String = locOr("pet.$species", species)

/** A species' sprite (3.70.0): the egg of its biome, framed as a stack of the bag is; the menagerie's glyph for one the content does not know. */
@Composable fun PetIcon(game: GameUi, species: String, size: Int) {
    // A hybrid (3.79.0) has a portrait of its own, not its biome's egg.
    if (SpriteIcon(com.sperance.exileforge.core.display.icon("pet.$species"), Gold, Modifier.size(size.dp), halo = false)) return
    val egg = game.index?.pets?.let { pets -> pets.species.firstOrNull { it.code == species }?.let { pets.eggs[it.biome]?.get(Rarity.MAGIC) } }
    if (egg != null) {
        StackIcon(game, egg, size)
    } else {
        Icon(ForgeGlyphs.Exile, null, tint = Gold, modifier = Modifier.size(size.dp))
    }
}

/** Who is shown in the menagerie (3.81.0, mockup A): rarities (none - all), the role and whether at work. */
private enum class PetRoleFilter(val key: String) { ALL("pets.filter_all"), COMBAT("pets.filter_combat"), HELPER("pets.filter_helper") }
private enum class PetStatusFilter(val key: String) { ALL("pets.filter_all"), WORKING("pets.filter_working"), RESTING("pets.filter_resting") }

/**
 * The menagerie (3.5.0, server 1.5.0; mockup A «Компактный список», 3.81.0): the incubator and the breeding folded into a row each
 * over the list, the filters by rarity, role and status, and a compact row per pet — its icon, name in its rarity's colour, what it
 * is, its level and every line it rolled, always in sight. A tap unfolds the row's actions: to work or back, to the forge (the pets'
 * orbs are spent there since 3.81.0), or let go for gold.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenagerieSection(game: GameUi, vm: HeroViewModel) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val pets = hero.pets
    val menagerie = remember(index) { Menagerie(index) }
    var incubator by rememberSaveable { mutableStateOf(false) }
    var breeding by rememberSaveable { mutableStateOf(false) }
    var rarities by remember { mutableStateOf(emptySet<Rarity>()) }
    var role by rememberSaveable { mutableStateOf(PetRoleFilter.ALL) }
    var status by rememberSaveable { mutableStateOf(PetStatusFilter.ALL) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Engraved(ui("pets.title", pets.pets.size))
        MutedText(ui("pets.hint"))
        val busy = pets.incubator.entries.count { it.busy }
        Fold(ui("incubator.title", busy, pets.incubator.slots), incubator, { incubator = !incubator }) { IncubatorPanel(game, vm, titled = false) }
        Fold(ui("pets.breed_title"), breeding, { breeding = !breeding }) { BreedingPanel(game, vm) }
        if (pets.pets.isEmpty()) {
            InfoCard(ui("pets.empty"), ui("pets.empty_hint"))
            return@Column
        }
        val present = pets.pets.map { it.rarity }.distinct().sortedBy { it.ordinal }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            present.forEach { r ->
                FilterChip(
                    selected = r in rarities,
                    onClick = { rarities = if (r in rarities) rarities - r else rarities + r },
                    label = { Text(ui("enum.rarity.${r.name}"), color = rarityColor(r.name)) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PetRoleFilter.entries.forEach { f -> FilterChip(selected = role == f, onClick = { role = f }, label = { Text(ui(f.key)) }) }
            Spacer(Modifier.width(6.dp))
            PetStatusFilter.entries.drop(1).forEach { f ->
                FilterChip(selected = status == f, onClick = { status = if (status == f) PetStatusFilter.ALL else f }, label = { Text(ui(f.key)) })
            }
        }
        val shown = pets.pets.filter { pet ->
            val kind = menagerie.species(pet.species)?.kind
            val active = pets.isActive(pet.id)
            (rarities.isEmpty() || pet.rarity in rarities) &&
                when (role) {
                    PetRoleFilter.ALL -> true
                    PetRoleFilter.COMBAT -> kind == PetKind.COMBAT
                    PetRoleFilter.HELPER -> kind == PetKind.HELPER
                } &&
                when (status) {
                    PetStatusFilter.ALL -> true
                    PetStatusFilter.WORKING -> active
                    PetStatusFilter.RESTING -> !active
                }
        }.sortedWith(compareBy({ !pets.isActive(it.id) }, { -it.rarity.ordinal }, { -it.level }))
        if (shown.isEmpty()) MutedText(ui("pets.filter_none"))
        shown.forEach { pet -> PetRow(game, vm, pet, open == pet.id) { open = if (open == pet.id) null else pet.id } }
    }
}

/** A folded part of the menagerie: one row with its title and a chevron; a tap opens what is under it. */
@Composable private fun Fold(title: String, open: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.fillMaxWidth().depthPanel(shape)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Gold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(if (open) "▾" else "▸", color = Gold)
        }
        if (open) Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** What a pet is, in one line: its kind, then a fighter's role and elements or a helper's craft. */
fun petSubtitle(kind: PetSpecies, pet: Pet): String {
    val what = when (kind.kind) {
        PetKind.COMBAT -> listOfNotNull(
            (pet.role ?: kind.role)?.let { locOr("pet.role.$it", it.name) },
            listOfNotNull(kind.element, kind.element2).joinToString(" + ") { locOr("pet.element.$it", it) }.ifBlank { null },
        )

        PetKind.HELPER -> listOfNotNull(kind.focus?.let { locOr("pet.focus.$it", it.name) })
    }
    return (listOf(locOr("pet.kind.${kind.kind}", kind.kind.name)) + what).joinToString(" · ")
}

/** Every line a pet rolled, one by one so a fractured one (server 1.65.0) is told apart: it stays through every orb. */
@Composable fun PetLines(index: ContentIndex, menagerie: Menagerie, pet: Pet) {
    if (pet.lines.isEmpty()) MutedText(ui("pets.no_lines"), style = MaterialTheme.typography.bodySmall)
    pet.lines.forEach { line ->
        menagerie.lines(pet.copy(lines = listOf(line))).firstOrNull()?.let {
            val text = lineText(index, it)
            Text(if (line.fractured) "$text · ${ui("pets.fractured")}" else text, color = if (line.fractured) Gold else ModBlue, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Карточка питомца в стиле Зверинца: значок, имя в цвете редкости, кто он, уровень и «В деле», метки, все строки и у бойца
 * здоровье с уроном; [extra] - что лежит под ней (действия Зверинца). Её же рисует снаряжение героя ([PetSlots], 3.90.2).
 */
@Composable
internal fun PetCard(
    game: GameUi,
    index: ContentIndex,
    menagerie: Menagerie,
    kind: PetSpecies,
    pet: Pet,
    active: Boolean,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    /** Долгое нажатие (4.4.x): в выборе цели кузницы оно открывает карточку, а нажатие выбирает. */
    onLongClick: (() -> Unit)? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(8.dp)
    val tint = rarityColor(pet.rarity.name)
    Column(
        Modifier.fillMaxWidth().depthPanel(shape).border(1.dp, if (selected) GoldBright else tint.copy(alpha = .45f), shape)
            .then(if (onClick != null || onLongClick != null) Modifier.combinedClickable(onLongClick = onLongClick) { onClick?.invoke() } else Modifier).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetIcon(game, pet.species, 36)
            Column(Modifier.weight(1f)) {
                Text(petName(pet.species), color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                MutedText(petSubtitle(kind, pet), style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(ui("pets.level", pet.level), color = GoldBright, style = MaterialTheme.typography.labelMedium)
                if (active) Text(ui("pets.at_work"), color = Vital, style = MaterialTheme.typography.labelSmall)
            }
        }
        val marks = listOfNotNull(
            ui("pets.hybrid").takeIf { kind.element2 != null }?.let { it to GoldBright },
            ui("pets.tired").takeIf { pet.tiredUntil > System.currentTimeMillis() }?.let { it to Muted },
            ui("pets.corrupted").takeIf { pet.corrupted }?.let { it to LifeRed },
            ui("pets.offer_waiting").takeIf { pet.offer.isNotEmpty() }?.let { it to Rune },
        )
        marks.forEach { (text, color) -> Text(text, color = color, style = MaterialTheme.typography.labelSmall) }
        PetLines(index, menagerie, pet)
        if (kind.kind == PetKind.COMBAT) {
            val sheet = menagerie.sheet(pet)
            MutedText(ui("pets.sheet", number(sheet[CoreStat.HEALTH.code] ?: 0.0), number(listOfNotNull(kind.element, kind.element2).sumOf { sheet["STOCK_ATTACK_$it"] ?: 0.0 })), style = MaterialTheme.typography.labelSmall)
        }
        extra()
    }
}

/**
 * Карточка питомца в шторке (4.4.x): кузница открывает её касанием питомца на наковальне и долгим нажатием в выборе цели;
 * [action] - главная кнопка места (сменить питомца), как [InspectAction] в карточке предмета.
 */
@Composable
fun PetCardSheet(game: GameUi, pet: Pet, action: InspectAction? = null, onDismiss: () -> Unit) {
    val index = game.index ?: return
    val menagerie = remember(index) { Menagerie(index) }
    val kind = menagerie.species(pet.species) ?: return
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PetCard(game, index, menagerie, kind, pet, game.hero?.pets?.isActive(pet.id) == true)
            action?.let {
                ForgeButton(onClick = {
                    onDismiss()
                    it.run()
                }, modifier = Modifier.fillMaxWidth(), enabled = it.enabled) { Text(it.label) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PetRow(game: GameUi, vm: HeroViewModel, pet: Pet, open: Boolean, onToggle: () -> Unit) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val menagerie = remember(index) { Menagerie(index) }
    val kind = menagerie.species(pet.species) ?: return
    val active = hero.pets.isActive(pet.id)
    val smithy = koinViewModel<SmithyViewModel>()
    val shell = koinViewModel<ShellViewModel>()
    var selling by remember(pet.id) { mutableStateOf(false) }
    var listing by remember(pet.id) { mutableStateOf(false) }
    var hiring by remember(pet.id) { mutableStateOf(false) }
    // A helper does not fight (3.70.0): the hiring dialog repeats its lines before it goes to work.
    val helps = if (kind.kind == PetKind.HELPER) menagerie.lines(pet).joinToString(", ") { lineText(index, it) }.ifBlank { "—" } else null
    PetCard(game, index, menagerie, kind, pet, active, selected = open, onClick = onToggle) {
        if (open) {
            FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ForgeButton(onClick = { if (helps != null && !active) hiring = true else vm.activatePet(pet.id) }, enabled = !game.busy) {
                    Text(ui(if (active) "pets.rest" else "pets.work"))
                }
                ForgeOutlinedButton(onClick = {
                    smithy.openPet(pet.id)
                    shell.tab(TAB_CRAFT)
                }) { Text(ui("pets.to_forge")) }
                // Работающего питомца не продать (CH_039): сначала отдых.
                if (!active) {
                    ForgeOutlinedButton(onClick = { listing = true }, enabled = !game.busy) { Text(ui("hero.action_auction")) }
                    ForgeTextButton(onClick = { selling = true }, enabled = !game.busy) { Text(ui("pets.sell"), color = LifeRed) }
                }
            }
        }
    }
    if (hiring) {
        ConfirmSheet(
            title = ui("pets.work_q"),
            confirm = ui("pets.work"),
            subtitle = petName(pet.species),
            note = helps,
            onDismiss = { hiring = false },
        ) {
            hiring = false
            vm.activatePet(pet.id)
        }
    }
    if (selling) {
        ConfirmSheet(
            title = ui("pets.sell_q"),
            confirm = ui("pets.sell"),
            danger = true,
            subtitle = petName(pet.species),
            ledger = listOf(LedgerLine(ui("pets.sell_gold"), number(index.rules.pets.releasePrice(pet).toDouble()), Tone.GAIN)),
            onDismiss = { selling = false },
        ) {
            selling = false
            vm.sellPet(pet.id)
        }
    }
    if (listing) {
        ListingSheet(game, petName(pet.species), onDismiss = { listing = false }) { orb, price, _ ->
            listing = false
            vm.sellPetLot(pet.id, orb, price)
        }
    }
}

/**
 * Breeding (3.79.0, server 1.74.0): two combat pets of the rules' level, rested, and an Orb of Breeding; a hybrid of their two
 * elements is born with the rules' chance, an egg of a parent's kind otherwise. Both parents rest for the rules' hours.
 */
@Composable private fun BreedingPanel(game: GameUi, vm: HeroViewModel) {
    val index = game.index ?: return
    val hero = game.hero ?: return
    val rule = index.pets.breeding
    val menagerie = remember(index) { Menagerie(index) }
    val now = System.currentTimeMillis()
    val fit = hero.pets.pets.filter { menagerie.species(it.species)?.kind == PetKind.COMBAT && it.level >= rule.minLevel && it.tiredUntil <= now }
    var first by remember { mutableStateOf<String?>(null) }
    var second by remember { mutableStateOf<String?>(null) }
    val orbs = game.bagAmount(rule.orb) ?: 0L
    MutedText(ui("pets.breed_hint", rule.minLevel, (rule.hybridChance * 100).toInt(), rule.restHours))
    if (fit.size < 2) {
        MutedText(ui("pets.breed_none", rule.minLevel))
        return
    }
    val options = fit.associate { it.id to "${petName(it.species)} · ${ui("pets.level", it.level)}" }
    Spinner(ui("pets.breed_first"), first.orEmpty(), options, !game.busy) { first = it }
    Spinner(ui("pets.breed_second"), second.orEmpty(), options - first.orEmpty(), !game.busy) { second = it }
    val pair = listOfNotNull(first, second).mapNotNull { id -> fit.firstOrNull { it.id == id }?.let { menagerie.species(it.species) } }
    if (pair.size == 2) {
        val hybrid = index.pets.species.hybridOf(pair[0].element.orEmpty(), pair[1].element.orEmpty())
        if (hybrid != null) {
            Text(ui("pets.breed_may", petName(hybrid.code)), color = GoldBright, style = MaterialTheme.typography.bodySmall)
        } else {
            MutedText(ui("pets.breed_same"))
        }
    }
    ForgeButton(
        enabled = !game.busy && first != null && second != null && first != second && orbs > 0,
        onClick = {
            vm.breedPets(first!!, second!!)
            first = null
            second = null
        },
    ) {
        Text(ui("pets.breed_go", itemTitle(rule.orb), orbs))
    }
}
