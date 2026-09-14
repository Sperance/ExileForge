package com.sperance.exileforge.core.model.passives

import kotlinx.serialization.Serializable
import com.sperance.exileforge.core.model.command.CalculatedStats

@Serializable data class PassiveState(val characterVersion: Long, val treeRevision: Int, val allocated: Set<String>,
    val totalPoints: Int, val spentPoints: Int, val availablePoints: Int, val allocatable: Set<String>,
    val refundable: Set<String>, val stats: CalculatedStats, val lockedReason: String? = null)
