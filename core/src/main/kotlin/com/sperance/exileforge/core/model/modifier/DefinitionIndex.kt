package com.sperance.exileforge.core.model.modifier

/**
 * The definition by code (server 0.56.0; by id before), through an index built once per list (2.56.0).
 *
 * The world's definitions are one list held in state, so every card and every recomposition
 * used to walk it; the index follows the list by identity and is rebuilt only when the world does.
 */
fun List<ModifierDefinition>.definition(code: String): ModifierDefinition? {
    val held = index
    val ready = if (held != null && held.first === this) held.second else associateBy { it.code }.also { index = this to it }
    return ready[code]
}

@Volatile private var index: Pair<List<ModifierDefinition>, Map<String, ModifierDefinition>>? = null
