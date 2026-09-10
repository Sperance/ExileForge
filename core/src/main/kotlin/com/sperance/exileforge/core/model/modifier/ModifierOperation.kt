package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

@Serializable
enum class ModifierOperation {

    FLAT,

    INCREASED,

    REDUCED,

    MORE,

    LESS,

    SET,

    MIN,

    MAX
}
