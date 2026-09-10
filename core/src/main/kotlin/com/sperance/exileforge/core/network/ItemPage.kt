package com.sperance.exileforge.core.network

import kotlinx.serialization.json.*
import okhttp3.*

data class ItemPage(val items: List<JsonObject>, val page: Int, val totalPages: Int, val totalItems: Long)
