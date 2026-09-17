package com.sperance.exileforge.core.model.passives

import kotlinx.serialization.Serializable

@Serializable enum class PassiveNodeKind { ORIGIN, SMALL, NOTABLE, KEYSTONE }
@Serializable data class PassiveNode(val id: String, val name: String, val description: String, val kind: PassiveNodeKind,
    val x: Double, val y: Double, val effects: List<PassiveEffect>, val cost: Int = 1,
    /** Icon of the server set; the tree fills it on the way out and never stores it. */
    val icon: String? = null)
