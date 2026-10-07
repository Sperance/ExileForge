package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.run.BossOdds
import com.sperance.exileforge.core.campaign.run.ChallengeView
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.bossOdds
import com.sperance.exileforge.core.campaign.traitViews
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.phaseTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.BossRecord
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.arena.rarityTint
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Насколько опасен бой по доле побед прогона (3.92.0): ключ словаря и цвет. */
private fun verdict(share: Double): Pair<String, Color> = when {
    share >= 0.8 -> "challenge.easy" to Vital
    share >= 0.5 -> "challenge.even" to Color(0xFFE8C25A)
    share >= 0.2 -> "challenge.hard" to Ember
    else -> "challenge.deadly" to LifeRed
}

/** Экран-вызов перед стражем карты (3.92.0): досье стража, шанс прогоном боёв вашим героем, «В бой» - бой. */
@Composable internal fun BossChallenge(game: GameUi, model: ExpeditionViewModel, run: ExpeditionRun, view: ChallengeView, onCommand: (RunCommand) -> Unit) {
    BossDossier(
        game,
        view.boss,
        view.level,
        view.maxLife,
        view.phase,
        view.marks,
        ui(if (view.vaal) "challenge.vaal_lair" else "challenge.lair", mapTitle(run.zone.code)),
        run.hero,
        run.rules,
        odds = { run.bossOdds() },
        record = { model.bossRecord(view.boss.code.value) },
    ) { onCommand(RunCommand.Accept) }
}

/**
 * Досье босса перед боем (3.92.0, макет B): портрет, уровень и здоровье, шанс победы прогоном боёв [odds] (тяжёлый - считается
 * вне главного потока), доля героев сервера [record], победивших его, умения, свойства и фазы - всё открывает свой лист;
 * «Снаряжение», «Разведка» и «В бой». Отойти нельзя: подошёл к логову - бой.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BossDossier(
    game: GameUi,
    boss: RolledMonster,
    level: Int,
    maxLife: Double,
    phase: String?,
    marks: List<Double>,
    caption: String,
    hero: Combatant?,
    rules: CombatRules,
    odds: () -> BossOdds?,
    record: suspend () -> BossRecord?,
    onFight: () -> Unit,
) {
    val index = game.index ?: return
    val name = monsterTitle(boss.code)
    val body = remember(boss, level) { Combatant(boss.stats, level, rules) }
    val chance by produceState<BossOdds?>(null, boss) { value = withContext(Dispatchers.Default) { odds() } }
    val server by produceState<BossRecord?>(null, boss.code) { value = record() }
    val traits = remember(boss, index) { traitViews(boss, index) }
    val lore = LocalLore.current
    var gear by remember { mutableStateOf(false) }
    var scout by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(caption.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        val shape = RoundedCornerShape(14.dp)
        Canvas(Modifier.fillMaxWidth().height(170.dp).clip(shape).background(Color(0xFF1A0F0E)).border(1.dp, Color(0xFF4A2A24), shape)) {
            Portraits.monster(this, boss.code.value, boss.form, rarityTint(boss.rarity), 0f)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = rarityTint(boss.rarity), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            MutedText(ui("challenge.subtitle", level, number(maxLife)))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val current = chance
            Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                if (current == null) {
                    CircularProgressIndicator(color = Gold, modifier = Modifier.fillMaxSize(), strokeWidth = 6.dp)
                } else {
                    CircularProgressIndicator(progress = { current.share.toFloat() }, color = verdict(current.share).second, trackColor = PanelRaised, modifier = Modifier.fillMaxSize(), strokeWidth = 6.dp)
                    Text("${(current.share * 100).roundToInt()}%", color = GoldBright, style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (current == null) {
                    MutedText(ui("challenge.weighing"))
                } else {
                    val (key, tint) = verdict(current.share)
                    Text(ui(key), color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    MutedText(ui("challenge.odds_note", current.fights, current.seconds.roundToInt()), style = MaterialTheme.typography.labelSmall)
                }
                val known = server
                val share = known?.share
                Text(
                    when {
                        known == null -> ui("challenge.server_loading")
                        share == null -> ui("challenge.server_none")
                        else -> ui("challenge.server_share", (share * 100).roundToInt(), known.fought)
                    },
                    color = Parchment,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (boss.skills.isNotEmpty()) {
            Caption(ui("challenge.skills"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                boss.skills.forEach { code ->
                    val curse = index.skills.monsterByCode[code]?.curse != null
                    LoreChip(SkillText.title(code), if (curse) Color(0xFFF2A3A0) else Rune) { lore?.invoke(Lore.Skill(code, name, body, hero)) }
                }
            }
        }
        if (traits.isNotEmpty() || phase != null) {
            Caption(ui("challenge.traits"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                traits.forEach { trait -> LoreChip(trait.title, Color(0xFFE8B06A)) { lore?.invoke(Lore.Trait(trait, name)) } }
                phase?.let { code ->
                    val steps = marks.distinct().joinToString("/") { "${it.roundToInt()}" }
                    LoreChip(ui("challenge.phase", phaseTitle(code), steps), Elder) { lore?.invoke(Lore.Phase(code, name)) }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeOutlinedButton(onClick = { gear = true }, modifier = Modifier.weight(1f)) { Text(ui("challenge.gear")) }
            ForgeOutlinedButton(onClick = { scout = true }, modifier = Modifier.weight(1f)) { Text(ui("challenge.scout")) }
        }
        ForgeButton(onClick = onFight, modifier = Modifier.fillMaxWidth()) { Text(ui("challenge.fight")) }
    }
    if (gear) GearSheet(game) { gear = false }
    if (scout) ScoutSheet(name, body) { scout = false }
}

/** Разведка стража (3.92.0): здоровье, щит, урон по типам, броня, уклонение и сопротивления его листом. */
@Composable private fun ScoutSheet(name: String, body: Combatant, onDismiss: () -> Unit) {
    val facts = buildList {
        add(ui("challenge.scout_life") to number(body.maxLife))
        if (body.maxShield > 0) add(ui("challenge.scout_shield") to number(body.maxShield))
        body.damage.filterValues { it > 0 }.forEach { (type, value) -> add(ui("lore.damage_of", ui("skill.element.${type.name}")) to number(value * body.damageMore)) }
        add(ui("challenge.scout_speed") to "%.2f".format(java.util.Locale.ROOT, body.attackSpeed))
        add(ui("challenge.scout_armour") to number(body.armour))
        add(ui("challenge.scout_evasion") to number(body.evasion))
        DamageType.entries.filter { it != DamageType.PHYSICAL }.forEach { type ->
            add(ui("challenge.scout_resist", ui("skill.element.${type.name}")) to "${(body.resist(type) * 100).roundToInt()}%")
        }
    }
    LoreSheet(name, ui("challenge.scout"), GoldBright, emptyList(), facts, onDismiss = onDismiss)
}
