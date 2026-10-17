package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.RunEvent

/** Откуда порция добычи захода: вид события; подпись - `expedition.bag.from.<вид>`. */
enum class LootKind { MONSTER, BOSS, CHEST, FEATURE, CRYSTAL, ABYSS, VAAL, OTHER }

/** Источник порции: вид и у монстра - его код (имя - словарём сервера). */
data class LootFrom(val kind: LootKind, val monster: MonsterCode? = null) {
    companion object {
        /** Источник события [event] захода [run]: убийство - монстр своей стаи. */
        fun of(event: RunEvent, run: com.sperance.exileforge.rules.run.Run): LootFrom = when (event) {
            is RunEvent.Kill -> LootFrom(LootKind.MONSTER, run.spawn(event.i, event.vaal).pack.getOrNull(event.m)?.code)
            is RunEvent.Boss -> LootFrom(LootKind.BOSS)
            is RunEvent.Chest -> LootFrom(LootKind.CHEST)
            is RunEvent.Feature -> LootFrom(LootKind.FEATURE)
            is RunEvent.Crystal, is RunEvent.CrystalVaal -> LootFrom(LootKind.CRYSTAL)
            is RunEvent.AbyssClaim -> LootFrom(LootKind.ABYSS)
            else -> LootFrom(LootKind.OTHER)
        }
    }
}

/**
 * Порция добычи захода для «Сумки захода»: событие [n], откуда [from], когда записано
 * ([at], мс часов захода) и что сервер за него дал; null - ответа ещё нет. Добычу катит только сервер: здесь она лишь раскладывается.
 */
data class BagEntry(val n: Int, val from: LootFrom, val at: Long, val reward: Reward?) {
    /** В сумке есть что показать: вещи или стопки (золото и опыт - итогом над списком). */
    val shown: Boolean get() = reward?.let { it.equipment.isNotEmpty() || it.items.isNotEmpty() } == true
}
