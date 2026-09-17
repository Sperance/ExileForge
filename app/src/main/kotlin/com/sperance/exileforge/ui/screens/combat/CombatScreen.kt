package com.sperance.exileforge.ui.screens.combat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun CombatScreen(s: ForgeState, vm: ForgeViewModel) {
    var zoneId by rememberSaveable(s.characterId) { mutableStateOf("coast") }
    var showLoot by rememberSaveable { mutableStateOf(false) }
    var confirmFlee by remember { mutableStateOf(false) }
    val ready = s.battleCharacterId == s.characterId && s.battleView != null
    val battle = if(ready) s.battleView?.battle else null
    val active = battle?.status == BattleStatus.ACTIVE
    val controls = ready && s.signedIn && !s.busy && s.battlePending == null
    val zone = s.combatCatalog?.zones?.firstOrNull { it.id == zoneId }
    val icons = LocalForgeIcons.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(tr("Поход", "Expedition"), tr("Пошаговые сражения · добыча · боссы", "Turn-based battles · loot · bosses"), ForgeGlyphs.Swords)
            TextButton(enabled = !s.busy && s.signedIn, onClick = { vm.tab(7) }) { Text(tr("Древо навыков", "Passive tree")) }
            EntitySpinner(tr("Персонаж", "Character"), s.characterId, EntitySource.CHARACTER, !s.busy && s.pending == null, vm::characterId)
            OutlinedButton(enabled = s.signedIn && !s.busy && s.characterId.isNotBlank(), onClick = vm::loadCombat, modifier = Modifier.fillMaxWidth()) { Text(tr("Загрузить / продолжить бой", "Load / resume the battle")) }
            if(!s.signedIn) Text(tr("Войдите во вкладке «Аккаунт».", "Sign in on the Account tab."))
            if(s.battlePending != null) {
                Text(tr("Ответ на действие не подтверждён. Повтор отправит тот же запрос без повторной награды.", "The action was not confirmed. A retry sends the identical request without a second reward."), color = Gold)
                Button(enabled = !s.busy && s.signedIn, onClick = vm::retryBattle) { Text(tr("Подтвердить действие", "Confirm the action")) }
            }
        }
        if(ready && !active) item {
            ForgePanel {
                Engraved(tr("Зоны", "Zones"))
                Spinner(tr("Зона", "Zone"), zoneId, s.combatCatalog?.zones.orEmpty().associate { it.id to tr("${it.name} · ур. ${it.level}", "${it.name} · lvl ${it.level}") }, controls, { zoneId = it })
                zone?.let { z ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ForgeIcon(z.icon ?: "combat-zone", Modifier.size(48.dp), description = z.name)
                        Text(z.description, modifier = Modifier.weight(1f))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        z.monsters.forEach { monster ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ForgeIcon(icons.forMonster(monster), Modifier.size(28.dp), description = monster.name)
                                Text(monster.name, color = Muted, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    val kills = s.battleView?.zoneKills?.get(z.id) ?: 0
                    Text(tr("Босс: ${z.boss.name} · победы ${minOf(kills, z.killsForBoss)}/${z.killsForBoss}", "Boss: ${z.boss.name} · kills ${minOf(kills, z.killsForBoss)}/${z.killsForBoss}"), color = Gold)
                    val allowed = controls && (s.hero?.level?.toInt() ?: 0) >= z.level
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = allowed, onClick = { vm.startBattle(z.id, false) }) { Text(tr("Искать противника", "Seek an enemy")) }
                        OutlinedButton(enabled = allowed && kills >= z.killsForBoss, onClick = { vm.startBattle(z.id, true) }) { Icon(ForgeGlyphs.Skull, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Вызвать босса", "Summon the boss")) }
                    }
                    if(!allowed && controls) Text(tr("Нужен персонаж уровня ${z.level}.", "A character of level ${z.level} is required."))
                }
                Text(tr("В начале боя здоровье и флаконы восстанавливаются. Экипировка фиксируется на весь бой.", "Life and flasks are restored when a battle starts. Equipment is locked for its duration."), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if(battle != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForgeIcon(icons.forMonster(battle.monster), Modifier.size(44.dp), description = battle.monster.name)
                    Text(if(battle.monster.boss) tr("БОСС", "BOSS") + " · ${battle.monster.name}" else battle.monster.name, style = MaterialTheme.typography.titleLarge, color = if(battle.monster.boss) Blood else Gold)
                }
                OrnateDivider(if(battle.monster.boss) Blood else Gold)
                CombatantCard(battle.enemy, false)
                Spacer(Modifier.height(8.dp))
                CombatantCard(battle.hero, true)
                Text(tr("Ход ${battle.turn}/80 · флаконы ${battle.potions}/2", "Turn ${battle.turn}/80 · flasks ${battle.potions}/2"), color = Muted)
                if(battle.unsupportedStats.isNotEmpty()) Text(tr("Не участвуют в расчёте: ", "Not used in the calculation: ") + battle.unsupportedStats.joinToString(), style = MaterialTheme.typography.bodySmall)
            }
            if(active) item {
                BattleActions(battle, controls, vm::battleAction) { confirmFlee = true }
                Text(tr("Защита снижает входящий урон на 65%. Флакон и защита тратят ход.", "Guard reduces incoming damage by 65%. Flask and guard both cost a turn."), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            if(!active) item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForgeIcon(icons.forBattleStatus(battle.status.name), Modifier.size(48.dp))
                    Text(when(battle.status) {
                        BattleStatus.VICTORY -> tr("Победа — награды уже в инвентаре", "Victory — the rewards are already in your stash")
                        BattleStatus.DEFEAT -> tr("Поражение — возвращение в лагерь", "Defeat — back to camp")
                        else -> tr("Отступление без наград", "Retreat without rewards")
                    }, color = Gold, style = MaterialTheme.typography.titleMedium)
                }
                battle.rewards.forEach { PropertyRow(it.name, "+${it.amount}", it.name) }
                OutlinedButton(onClick = { vm.tab(4); vm.loadInventory() }, enabled = !s.busy) { Text(tr("Открыть арсенал", "Open the stash")) }
            }
            item { Text(tr("Журнал боя", "Battle log").uppercase(), style = MaterialTheme.typography.titleMedium, color = Gold) }
            items(battle.log.asReversed()) { line -> ForgePanel { Text(line, style = MaterialTheme.typography.bodyMedium) } }
        }
        if(s.combatCatalog != null) {
            item { TextButton(onClick = { showLoot = !showLoot }) { Text(if(showLoot) tr("Скрыть таблицы лута", "Hide the loot tables") else tr("Показать таблицы лута", "Show the loot tables")) } }
            if(showLoot) items(s.combatCatalog.lootTables) { table ->
                ForgePanel {
                    Text(tr("${table.id} · бросков: ${table.rolls}", "${table.id} · rolls: ${table.rolls}"), color = Gold, style = MaterialTheme.typography.labelLarge)
                    val total = table.entries.sumOf { it.weight }.toDouble()
                    table.entries.forEach { entry -> PropertyRow("${lootName(entry.kind)} ×${entry.amount}", tr("${"%.1f".format(entry.weight / total * 100)}% за бросок", "${"%.1f".format(entry.weight / total * 100)}% per roll"), entry.kind) }
                }
            }
        }
    }
    if(confirmFlee) AlertDialog(onDismissRequest = { confirmFlee = false }, containerColor = Panel, titleContentColor = Gold,
        title = { Text(tr("Отступить?", "Retreat?")) },
        text = { Text(tr("Бой завершится без опыта, золота и добычи.", "The battle ends with no experience, gold or loot.")) },
        confirmButton = { TextButton(enabled = controls, onClick = { confirmFlee = false; vm.battleAction(BattleAction.FLEE) }) { Text(tr("Отступить", "Retreat")) } },
        dismissButton = { TextButton(onClick = { confirmFlee = false }) { Text(tr("Остаться", "Stay")) } })
}

/** Combatant plate: name, life bar and the numbers the server rolled with. */
@Composable internal fun CombatantCard(unit: Combatant, player: Boolean) {
    val accent = if(player) Gold else Blood
    ForgePanel(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if(player) ForgeGlyphs.Exile else ForgeGlyphs.Skull, null, tint = accent, modifier = Modifier.size(20.dp))
            Text(unit.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("HP ${unit.life.toInt()} / ${unit.maxLife.toInt()}", color = GoldBright, style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(progress = { (unit.life / unit.maxLife.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp), color = LifeRed, trackColor = Abyss)
        if(player) Text("MP ${unit.mana.toInt()} / ${unit.maxMana.toInt()} · " + tr("щит", "shield") + " ${unit.shield.toInt()}", color = ManaBlue, style = MaterialTheme.typography.labelMedium)
        Text(tr("Урон ${unit.damage.toInt()} · броня ${unit.armour.toInt()} · уклонение ${unit.evasion.toInt()}", "Damage ${unit.damage.toInt()} · armour ${unit.armour.toInt()} · evasion ${unit.evasion.toInt()}"), color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
private fun lootName(kind: String) = when(kind) {
    "NONE" -> tr("Без предмета", "No item"); "NORMAL" -> tr("Обычный предмет", "Normal item"); "MAGIC" -> tr("Магический предмет", "Magic item")
    "RARE" -> tr("Редкий предмет", "Rare item"); "UNIQUE" -> tr("Уникальный (или редкий, если нет подходящей базы)", "Unique (or rare when no base fits)")
    "TRANSMUTATION" -> tr("Сфера превращения", "Orb of Transmutation"); "ALTERATION" -> tr("Сфера перемен", "Orb of Alteration")
    "ALCHEMY" -> tr("Сфера алхимии", "Orb of Alchemy"); "CHAOS" -> tr("Сфера хаоса", "Chaos Orb"); else -> kind
}
