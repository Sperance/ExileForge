package com.sperance.exileforge.core.model.passives

import kotlinx.serialization.Serializable

@Serializable enum class PassiveOperation { FLAT, INCREASED, REDUCED, MORE, LESS }
@Serializable data class PassiveEffect(val stat: String, val operation: PassiveOperation, val value: Double)
