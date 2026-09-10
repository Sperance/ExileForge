package com.sperance.exileforge.presentation.state

import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

data class PendingInventoryAction(val characterId: String, val operation: String, val payload: JsonObject)
