package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.RunPhase
import com.sperance.exileforge.core.campaign.run.engage
import com.sperance.exileforge.core.campaign.run.exit
import com.sperance.exileforge.rules.run.RunEvent

/**
 * Проход захода без игрока - для сторожей, симуляций и нагрузки, в игре его нет: стаи по порядку номеров, затем сундуки, страж и
 * выход. Каждый шаг - то, что заход делает сам, когда герой подходит к объекту; итог выигранного боя закрывается сразу, ворота
 * зоны Ваал остаются закрытыми, объекты карты (алтари, Бездна, узлы, ловушки) проход не трогает.
 */
class Walkthrough(private val run: ExpeditionRun) {
    /** Один кадр в [dt] секунд; false - заход кончен: гибель, выход или уход. */
    fun step(dt: Double): Boolean {
        run.update(dt)
        when (run.hud.value.phase) {
            RunPhase.DEAD, RunPhase.CLEARED, RunPhase.LEFT -> return false
            RunPhase.GATE -> run.send(RunCommand.ShutGate(false))
            RunPhase.LOOT -> run.send(RunCommand.Continue)
            RunPhase.MAP -> next()
            else -> Unit
        }
        return true
    }

    /** Следующий шаг на карте: живая стая, нераскрытый сундук, страж на посту, иначе выход. */
    private fun next() = with(run) {
        val boss = world.boss
        world.agents.filter { it !== boss && it.alive && it.standing.isNotEmpty() }.minByOrNull { it.id }?.let {
            engage(it)
            started = true
            return@with
        }
        world.chests.firstOrNull { !it.opened }?.let { chest ->
            chest.opened = true
            rewarding(record { RunEvent.Chest(it, chest.id) })
            return@with
        }
        boss?.takeIf { it.alive && it.standing.isNotEmpty() }?.let {
            engage(it)
            started = true
            return@with
        }
        exit()
    }
}
