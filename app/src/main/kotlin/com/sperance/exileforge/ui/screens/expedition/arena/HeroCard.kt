package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.screens.hero.PetIcon
import com.sperance.exileforge.ui.screens.hero.PetLines
import com.sperance.exileforge.ui.screens.hero.petName
import com.sperance.exileforge.ui.screens.hero.petSubtitle
import com.sperance.exileforge.ui.theme.*
import kotlin.math.sin

/**
 * The hero's card at the foot: portrait, name and class, life with the shield over it, the swing,
 * every state on them, and whom the next blow goes to — the player's pick or the class's rule.
 * Lifted and lit in gold while they swing; ringed in blood while struck.
 */
@Composable internal fun HeroCard(
    game: GameUi,
    hud: RunHud,
    fight: FightHud,
    time: Float,
    names: Map<Int, String>,
    stance: HeroStance,
    modifier: Modifier,
    large: Boolean,
    onCommand: (RunCommand) -> Unit = {},
) {
    val hero = game.heroInfo
    var petOpen by remember { mutableStateOf(false) }
    // The pet's blows and the blows at it are its own card's (3.70.0).
    val lunge = fight.lunge?.takeIf { !it.pet }
    val acting = reach(lunge, Side.HERO, null)
    val hit = struck(lunge, Side.MONSTER, null)
    val shape = RoundedCornerShape(10.dp)
    val wash = fight.heroAilments.map { it.ailment }.maxByOrNull(::washAmount)
    Row(
        modifier.fillMaxWidth().graphicsLayer {
            translationY = -acting * 10.dp.toPx()
            translationX = if (hit) sin(time * 60f) * 2.dp.toPx() else 0f
        }
            .background(if (acting > 0f) PanelRaised else Panel.copy(alpha = .94f), shape)
            .then(if (fight.heroTaunt && acting == 0f && !hit) Modifier.tauntAura(shape, time) else Modifier)
            .border(
                if (acting > 0f || hit) 2.dp else 1.dp,
                when {
                    acting > 0f -> GoldBright
                    hit -> LifeRed
                    else -> Gold.copy(alpha = .6f)
                },
                shape,
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(64.dp).aspectRatio(.75f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0B0E13))) {
            Canvas(Modifier.fillMaxSize()) {
                Portraits.hero(this, game.heroClass?.code, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash(lunge, Side.HERO, null))
            }
            CardHits(fight.hits.filter { it.target == Side.HERO && (!it.pet || it.mend) })
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    listOfNotNull(hero?.name?.takeIf { it.isNotBlank() }, ui("expedition.hero_line", hero?.heroClass?.let(::classTitle).orEmpty(), game.heroLevel)).joinToString(" · "),
                    color = GoldBright,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // The hero's own mark (2.72.0): the taunt's seal, opening its window on a tap.
                if (fight.heroTaunt) TauntSeal(time, Modifier.size(22.dp)) { tauntTip(true) }
            }
            // The pools as bars of their own (3.24.0): the shield over life, mana under it, each with its figures and share.
            if (hud.heroMaxShield > 0) VitalBar(fight.heroShield, hud.heroMaxShield, ShieldCyan, Modifier.fillMaxWidth().height(16.dp))
            VitalBar(fight.heroLife, hud.heroMaxLife, LifeRed, Modifier.fillMaxWidth().height(18.dp), ring = GoldBright.takeIf { fight.heroBarrier > 0 })
            // A pool the auras hold whole is still drawn: a full hatched bar.
            if (fight.heroMaxMana + fight.heroReserved.coerceAtLeast(0) > 0) VitalBar(fight.heroMana, fight.heroMaxMana, ManaBlue, Modifier.fillMaxWidth().height(16.dp), reserved = fight.heroReserved)
            // The hero's own buildups (3.78.0): the foes stun and freeze by the same bars.
            fight.heroBuildup?.let { BuildupBar(it, Modifier.fillMaxWidth()) }
            SwingBar(fight.heroSwing, fight.heroHeld, Modifier.fillMaxWidth())
            StateTiles(fight.heroAilments, fight.heroHeld, fight.heroEffects, fight.heroCharges)
            val target = fight.target?.let(names::get)
            if (target != null && fight.outcome == null) {
                Text(
                    ui("fight.target_line", target, if (fight.focus != null) ui("fight.target_yours") else ui("fight.rule.${stance.rule.name}")),
                    color = if (fight.focus != null) GoldBright else Parchment,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // The combat pet (3.79.0, variant B): a round badge on the hero's card, its life a ring around it.
        fight.ally?.let { PetBadge(game, it, fight.lunge?.takeIf { lunge -> lunge.pet }, fight.hits.filter { hit -> hit.pet }, time) { petOpen = true } }
    }
    // A tap on the pet's badge (3.81.0): what it is, its life and blow, and every line it rolled; the fight holds while it is read.
    if (petOpen) fight.ally?.let { FightPetSheet(game, it, onCommand) { petOpen = false } }
}

/** The combat pet's window over the fight (3.81.0): the hero's working pet of the ally's species, as the menagerie tells it. */
@Composable private fun FightPetSheet(game: GameUi, ally: AllyView, onCommand: (RunCommand) -> Unit, onDismiss: () -> Unit) {
    DisposableEffect(Unit) {
        onCommand(RunCommand.Hold(true))
        onDispose { onCommand(RunCommand.Hold(false)) }
    }
    val index = game.index ?: return
    val pets = game.hero?.pets ?: return
    val menagerie = remember(index) { Menagerie(index) }
    val pet = pets.pets.filter { it.species == ally.species }.let { same -> same.firstOrNull { pets.isActive(it.id) } ?: same.firstOrNull() } ?: return
    val kind = menagerie.species(pet.species)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PetIcon(game, pet.species, 40)
                Column(Modifier.weight(1f)) {
                    Text(petName(pet.species), color = rarityColor(pet.rarity.name), style = MaterialTheme.typography.titleLarge)
                    kind?.let { MutedText(petSubtitle(it, pet)) }
                }
                Text(ui("pets.level", pet.level), color = GoldBright, style = MaterialTheme.typography.labelLarge)
            }
            Text(ui("fight.pet_life", ally.life, ally.maxLife), color = if (ally.alive) Vital else Muted, style = MaterialTheme.typography.bodyMedium)
            kind?.let { species ->
                val sheet = menagerie.sheet(pet)
                MutedText(
                    ui(
                        "pets.sheet",
                        number(sheet[CoreStat.HEALTH.code] ?: 0.0),
                        number(listOfNotNull(species.element ?: "PHYSICAL", species.element2).sumOf { sheet["STOCK_ATTACK_$it"] ?: 0.0 }),
                    ),
                )
            }
            if (pet.quality > 0) Text(ui("pets.quality", pet.quality), color = GoldBright, style = MaterialTheme.typography.labelSmall)
            if (pet.corrupted) Text(ui("pets.corrupted"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
            PetLines(index, menagerie, pet)
        }
    }
}

/** A support pet's healing of the hero: no blow, only life given back. */
private val FloatingHit.mend: Boolean get() = pet && target == Side.HERO && amount == 0 && healed > 0

/**
 * The combat pet's badge (3.79.0, variant B «Значок на карточке героя»): its sprite in a circle, its life a ring around it.
 * The ring flashes gold as it strikes, red as it is struck, green as it mends the hero; the blows it takes float over it.
 * Its own blows rise green on the foes' cards and its lines are green in the log. Down, it greys.
 */
@Composable private fun PetBadge(game: GameUi, ally: AllyView, lunge: LungeView?, hits: List<FloatingHit>, time: Float, onTap: () -> Unit) {
    val acting = reach(lunge, Side.HERO, null)
    val hit = struck(lunge, Side.HERO, null)
    val mending = hits.any { it.mend && it.age < PET_PULSE }
    val ring = when {
        !ally.alive -> Muted
        acting > 0f -> GoldBright
        hit -> LifeRed
        else -> Vital
    }
    val share = if (ally.maxLife > 0) (ally.life.toFloat() / ally.maxLife).coerceIn(0f, 1f) else 0f
    Box(
        Modifier.size(48.dp).graphicsLayer {
            translationY = -acting * 4.dp.toPx()
            translationX = if (hit) sin(time * 60f) * 2.dp.toPx() else 0f
            alpha = if (ally.alive) 1f else .55f
        }.clip(CircleShape).clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val at = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            drawCircle(Abyss)
            drawArc(PanelRaised, 0f, 360f, false, at, inset, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawArc(if (mending) Vital else ring, -90f, 360f * share, false, at, inset, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        }
        PetIcon(game, ally.species, 30)
        CardHits(hits.filter { it.target == Side.HERO && !it.mend })
    }
}

/** How long, in seconds, the pet's card glows after it mends the hero. */
private const val PET_PULSE = .5
