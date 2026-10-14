package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.run.AgentMode
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.gathered
import com.sperance.exileforge.rules.content.BehaviourRule
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.test.Test
import kotlin.test.assertEquals

/** Свалка карты: путь ногами, а не прямая; видящая героя стая - в пределах взгляда, потерявшая и страж - нет. */
class GatherTest {
    // Коридор-подкова: верхний ряд y=1 и нижний y=5 сходятся только столбцом x=10
    private val width = 12
    private val height = 7
    private val tiles = Array(width * height) { i ->
        val x = i % width
        val y = i / width
        if (x in 1..10 && (y == 1 || y == 5) || x == 10 && y in 1..5) Tile.FLOOR else Tile.WALL
    }
    private val spawns = listOf(Cell(1, 1), Cell(3, 1), Cell(6, 1), Cell(8, 1), Cell(1, 5))
    private val map = ExpeditionMap(width, height, tiles, IntArray(width * height), start = Cell(10, 3), exit = Cell(2, 1), spawns = spawns)
    private val behaviour = BehaviourRule("WANDER", wanderSpeed = 0.0, chaseSpeed = 1.0, sight = 6.0, giveUp = 5.0)
    private fun monster() = RolledMonster(MonsterCode("M"), "form", MonsterRarity.entries.first(), emptyList(), emptyMap(), behaviour)

    @Test
    fun the_brawl_gathers_by_walking_path_and_sight() {
        val world = ExpeditionWorld(TestContent.index.campaign.expedition.copy(gatherRadius = 3.0), map, spawns.map { listOf(monster()) }, heroSpeed = 1.0, seed = 1L, bossMonster = monster())
        val (fighting, near, chasing, hunting, behindWall) = world.agents
        chasing.mode = AgentMode.CHASING
        hunting.mode = AgentMode.HUNTING
        behindWall.mode = AgentMode.CHASING
        world.boss!!.mode = AgentMode.CHASING
        assertEquals(listOf(fighting, near, chasing), world.gathered(fighting))
    }
}
