package com.sperance.exileforge.ui.screens.passives

import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.passives.*

internal fun passiveStatName(stat: String): String = when(stat) {
    "maximum_life" -> tr("Максимум здоровья", "Maximum life"); "maximum_mana" -> tr("Максимум маны", "Maximum mana")
    "strength" -> tr("Сила", "Strength"); "dexterity" -> tr("Ловкость", "Dexterity"); "intelligence" -> tr("Интеллект", "Intelligence")
    "armour" -> tr("Броня", "Armour"); "evasion" -> tr("Уклонение", "Evasion"); "energy_shield" -> tr("Энергетический щит", "Energy shield")
    "accuracy" -> tr("Меткость", "Accuracy")
    "life_regeneration" -> tr("Восстановление здоровья / ход", "Life regeneration / turn"); "mana_regeneration" -> tr("Восстановление маны / ход", "Mana regeneration / turn")
    "fire_resistance" -> tr("Сопротивление огню", "Fire resistance"); "cold_resistance" -> tr("Сопротивление холоду", "Cold resistance")
    "lightning_resistance" -> tr("Сопротивление молнии", "Lightning resistance"); "chaos_resistance" -> tr("Сопротивление хаосу", "Chaos resistance")
    "attack_damage_multiplier" -> tr("Урон атак", "Attack damage"); "attack_speed_multiplier" -> tr("Скорость атак", "Attack speed")
    "critical_chance_multiplier" -> tr("Шанс критического удара", "Critical strike chance")
    else -> stat
}
internal fun passiveEffectText(effect: PassiveEffect): String {
    val percent = effect.operation != PassiveOperation.FLAT
    val value = if(percent) effect.value * 100 else effect.value
    val number = if(value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)
    val prefix = when(effect.operation) {
        PassiveOperation.FLAT -> "+"; PassiveOperation.INCREASED -> "+"; PassiveOperation.REDUCED -> "−"
        PassiveOperation.MORE -> tr("× больше на ", "× more by "); PassiveOperation.LESS -> tr("× меньше на ", "× less by ")
    }
    return "$prefix$number${if(percent) "%" else ""} · ${passiveStatName(effect.stat)}"
}
internal fun passiveKind(kind: PassiveNodeKind): String = when(kind) {
    PassiveNodeKind.ORIGIN -> tr("Начало пути", "Starting point"); PassiveNodeKind.SMALL -> tr("Малый узел", "Small node")
    PassiveNodeKind.NOTABLE -> tr("Крупный узел", "Notable node"); PassiveNodeKind.KEYSTONE -> tr("Ключевой узел", "Keystone")
}
