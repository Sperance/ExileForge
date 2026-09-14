package com.sperance.exileforge.presentation.state

import kotlinx.serialization.Serializable
import com.sperance.exileforge.core.model.passives.PassiveCommand

@Serializable data class PendingPassiveWrite(val characterId: String, val command: PassiveCommand)
