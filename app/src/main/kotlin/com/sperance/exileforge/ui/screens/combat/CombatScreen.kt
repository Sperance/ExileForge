package com.sperance.exileforge.ui.screens.combat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.EntitySpinner
import com.sperance.exileforge.ui.components.Spinner
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted

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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Поход", style = MaterialTheme.typography.headlineLarge, color = Gold)
            Text("Пошаговые сражения · добыча · боссы", color = Muted)
            TextButton(enabled = !s.busy && s.signedIn, onClick = { vm.tab(7) }) { Text("Древо навыков") }
            EntitySpinner("Персонаж", s.characterId, EntitySource.CHARACTER, !s.busy && s.pending == null, vm::characterId)
            OutlinedButton(enabled = s.signedIn && !s.busy && s.characterId.isNotBlank(), onClick = vm::loadCombat) { Text("Загрузить / продолжить бой") }
            if(!s.signedIn) Text("Войдите во вкладке «Аккаунт».")
            if(s.battlePending != null) {
                Text("Ответ на действие не подтверждён. Повтор отправит тот же запрос без повторной награды.", color = Gold)
                Button(enabled = !s.busy && s.signedIn, onClick = vm::retryBattle) { Text("Подтвердить действие") }
            }
        }
        if(ready && !active) item {
            Spinner("Зона", zoneId, s.combatCatalog?.zones.orEmpty().associate { it.id to "${it.name} · ур. ${it.level}" }, controls, { zoneId = it })
            zone?.let { z ->
                Text(z.description)
                Text("Противники: " + z.monsters.joinToString { it.name }, color = Muted)
                val kills = s.battleView?.zoneKills?.get(z.id) ?: 0
                Text("Босс: ${z.boss.name} · победы ${minOf(kills, z.killsForBoss)}/${z.killsForBoss}", color = Gold)
                val allowed = controls && (s.hero?.level?.toInt() ?: 0) >= z.level
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = allowed, onClick = { vm.startBattle(z.id, false) }) { Text("Искать противника") }
                    OutlinedButton(enabled = allowed && kills >= z.killsForBoss, onClick = { vm.startBattle(z.id, true) }) { Text("Вызвать босса") }
                }
                if(!allowed && controls) Text("Нужен персонаж уровня ${z.level}.")
            }
            Text("В начале боя здоровье и флаконы восстанавливаются. Экипировка фиксируется на весь бой.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        if(battle != null) {
            item {
                Text(if(battle.monster.boss) "БОСС · ${battle.monster.name}" else battle.monster.name, style = MaterialTheme.typography.titleLarge, color = Gold)
                CombatantCard(battle.enemy, false)
                Spacer(Modifier.height(8.dp))
                CombatantCard(battle.hero, true)
                Text("Ход ${battle.turn}/80 · флаконы ${battle.potions}/2", color = Muted)
                if(battle.unsupportedStats.isNotEmpty()) Text("Не участвуют в расчёте: " + battle.unsupportedStats.joinToString(), style = MaterialTheme.typography.bodySmall)
            }
            if(active) item {
                BattleActions(battle, controls, vm::battleAction) { confirmFlee = true }
                Text("Защита снижает входящий урон на 65%. Флакон и защита тратят ход.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            if(!active) item {
                Text(when(battle.status) { BattleStatus.VICTORY -> "Победа — награды уже в инвентаре"; BattleStatus.DEFEAT -> "Поражение — возвращение в лагерь"; else -> "Отступление без наград" }, color = Gold)
                battle.rewards.forEach { Text("+ ${it.amount} · ${it.name}") }
                OutlinedButton(onClick = { vm.tab(4); vm.loadInventory() }, enabled = !s.busy) { Text("Открыть арсенал") }
            }
            item { Text("Журнал боя", style = MaterialTheme.typography.titleMedium) }
            items(battle.log.asReversed()) { line -> Card(Modifier.fillMaxWidth()) { Text(line, Modifier.padding(10.dp)) } }
        }
        if(s.combatCatalog != null) {
            item { TextButton(onClick = { showLoot = !showLoot }) { Text(if(showLoot) "Скрыть таблицы лута" else "Показать таблицы лута") } }
            if(showLoot) items(s.combatCatalog.lootTables) { table ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${table.id} · бросков: ${table.rolls}", color = Gold)
                        val total = table.entries.sumOf { it.weight }.toDouble()
                        table.entries.forEach { entry -> Text("${lootName(entry.kind)} ×${entry.amount} · ${"%.1f".format(entry.weight / total * 100)}% за бросок") }
                    }
                }
            }
        }
    }
    if(confirmFlee) AlertDialog(onDismissRequest = { confirmFlee = false }, title = { Text("Отступить?") },
        text = { Text("Бой завершится без опыта, золота и добычи.") },
        confirmButton = { TextButton(enabled = controls, onClick = { confirmFlee = false; vm.battleAction(BattleAction.FLEE) }) { Text("Отступить") } },
        dismissButton = { TextButton(onClick = { confirmFlee = false }) { Text("Остаться") } })
}

@Composable internal fun CombatantCard(unit: Combatant, player: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(unit.name, style = MaterialTheme.typography.titleMedium)
            Text("HP ${unit.life.toInt()} / ${unit.maxLife.toInt()}", color = Gold)
            LinearProgressIndicator(progress = { (unit.life / unit.maxLife.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            if(player) Text("MP ${unit.mana.toInt()} / ${unit.maxMana.toInt()} · щит ${unit.shield.toInt()}")
            Text("Урон ${unit.damage.toInt()} · броня ${unit.armour.toInt()} · уклонение ${unit.evasion.toInt()}", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
private fun lootName(kind: String) = when(kind) {
    "NONE" -> "Без предмета"; "NORMAL" -> "Обычный предмет"; "MAGIC" -> "Магический предмет"
    "RARE" -> "Редкий предмет"; "UNIQUE" -> "Уникальный (или редкий, если нет подходящей базы)"
    "TRANSMUTATION" -> "Сфера превращения"; "ALTERATION" -> "Сфера перемен"
    "ALCHEMY" -> "Сфера алхимии"; "CHAOS" -> "Сфера хаоса"; else -> kind
}
