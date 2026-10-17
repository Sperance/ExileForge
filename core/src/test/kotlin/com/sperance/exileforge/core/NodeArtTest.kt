package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.run.NodeArt
import com.sperance.exileforge.rules.content.Job
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Облики узлов сбора: у каждой работы-сбора закреплённого сервера есть облик, и реестр клиента рисует его целым и собранным. */
class NodeArtTest {
    @Test
    fun everyGatheringWorkHasALookTheRegistryDraws() {
        val gathering = TestContent.index.professions.professions.flatMap { it.jobs }.filterIsInstance<Job.Item>().filter { it.gathers }
        assertTrue(gathering.isNotEmpty())
        gathering.forEach { job ->
            val look = assertNotNull(job.look, "${job.code} has no node look")
            val sketch = NodeArt.of(look)
            assertTrue(sketch.full.isNotEmpty() && sketch.gathered.isNotEmpty(), "$look is not drawn in both states")
        }
    }
}
