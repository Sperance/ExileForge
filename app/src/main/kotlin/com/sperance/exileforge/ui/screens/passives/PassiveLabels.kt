package com.sperance.exileforge.ui.screens.passives

import com.sperance.exileforge.core.model.passives.*

internal fun passiveStatName(stat: String): String = when(stat) {
    "maximum_life" -> "Максимум здоровья"; "maximum_mana" -> "Максимум маны"
    "strength" -> "Сила"; "dexterity" -> "Ловкость"; "intelligence" -> "Интеллект"
    "armour" -> "Броня"; "evasion" -> "Уклонение"; "energy_shield" -> "Энергетический щит"
    "accuracy" -> "Меткость"; "life_regeneration" -> "Восстановление здоровья / ход"; "mana_regeneration" -> "Восстановление маны / ход"
    "fire_resistance" -> "Сопротивление огню"; "cold_resistance" -> "Сопротивление холоду"
    "lightning_resistance" -> "Сопротивление молнии"; "chaos_resistance" -> "Сопротивление хаосу"
    "attack_damage_multiplier" -> "Урон атак"; "attack_speed_multiplier" -> "Скорость атак"; "critical_chance_multiplier" -> "Шанс критического удара"
    else -> stat
}
internal fun passiveEffectText(effect: PassiveEffect): String {
    val percent = effect.operation != PassiveOperation.FLAT
    val value = if(percent) effect.value * 100 else effect.value
    val number = if(value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)
    val prefix = when(effect.operation) {
        PassiveOperation.FLAT -> "+"; PassiveOperation.INCREASED -> "+"; PassiveOperation.REDUCED -> "−"
        PassiveOperation.MORE -> "× больше на "; PassiveOperation.LESS -> "× меньше на "
    }
    return "$prefix$number${if(percent) "%" else ""} · ${passiveStatName(effect.stat)}"
}
internal fun passiveKind(kind: PassiveNodeKind): String = when(kind) {
    PassiveNodeKind.ORIGIN -> "Начало пути"; PassiveNodeKind.SMALL -> "Малый узел"
    PassiveNodeKind.NOTABLE -> "Крупный узел"; PassiveNodeKind.KEYSTONE -> "Ключевой узел"
}
