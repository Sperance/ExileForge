package com.sperance.exileforge.core.model.atlas

import kotlinx.serialization.Serializable

/** One hero's atlas: nodes taken (the start among them), achievements earned and the point balance. */
@Serializable data class AtlasState(
    val allocated: List<String> = emptyList(),
    val earned: List<String> = emptyList(),
    val points: Int = 0,
    val available: Int = 0,
)
