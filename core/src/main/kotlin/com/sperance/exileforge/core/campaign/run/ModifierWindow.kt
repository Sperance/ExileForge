package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.roll.EffectSource
import com.sperance.exileforge.rules.sheet.SourceKind

/**
 * Откуда доля строки окна модификаторов карты; порядок значений - порядок источников в окне: строки Ваал, затем карта, штраф акта,
 * сделки алтаря, Атлас и прочее. [key] - ключ локали имени источника.
 */
enum class ModifierSource(val key: String) {
    VAAL("run.mods.source.VAAL"),
    MAP("run.mods.source.MAP"),
    PORTAL_MAP("run.mods.source.PORTAL_MAP"),
    PORTAL_ZONE("run.mods.source.PORTAL_ZONE"),
    ACT_PENALTY("run.mods.source.ACT_PENALTY"),
    PACTS("run.mods.source.PACTS"),
    ATLAS("run.mods.source.ATLAS"),
    ATLAS_COST("run.mods.source.ATLAS_COST"),
    SCARAB("run.mods.source.SCARAB"),
    INFLUENCE("run.mods.source.INFLUENCE"),
    GUILD("run.mods.source.GUILD"),
    POTION("run.mods.source.POTION"),
    ;

    companion object {
        /** Источник строк карты-предмета [source]; строки самой карты называются [map] - картой, а в зоне Ваал - тем, что под порталом. */
        fun of(source: EffectSource, map: ModifierSource): ModifierSource = when (source) {
            EffectSource.MAP -> map
            EffectSource.ATLAS -> ATLAS
            EffectSource.SCARAB -> SCARAB
            EffectSource.INFLUENCE -> INFLUENCE
            EffectSource.ATLAS_COST -> ATLAS_COST
        }

        /** Источник доли атласа захода с видом [kind]: гильдия и зелье - свои, остаток - Атлас. */
        fun of(kind: SourceKind): ModifierSource = when (kind) {
            SourceKind.GUILD -> GUILD
            SourceKind.POTION -> POTION
            else -> ATLAS
        }
    }
}

/** Доля [value] источника [source] в сумме характеристики. */
data class ModifierShare(val source: ModifierSource, val value: Double)

/** Сумма [total] характеристики [stat] окна модификаторов и её доли [shares] по источникам, в порядке источников. */
data class ModifierSum(val stat: String, val total: Double, val shares: List<ModifierShare>)

/**
 * Окно модификаторов карты или зоны Ваал: один список сумм по характеристикам ([sums]); [vaalAtlasEffect] - «Сила модификаторов карт»
 * Атласа на строках Ваал (множитель, 1 - без неё), сноска доли «Зона Ваал».
 */
data class ModifierWindow(val sums: List<ModifierSum>, val vaalAtlasEffect: Double = 1.0) {
    companion object {
        /** Пустое окно. */
        val EMPTY = ModifierWindow(emptyList())

        /**
         * Окно из долей [parts] (источник - строки по характеристикам): одинаковые источники складываются, характеристика встаёт по первому
         * источнику, что её назвал (строки Ваал - в порядке броска), нулевые доли и суммы не входят.
         */
        fun of(parts: List<Pair<ModifierSource, Map<String, Double>>>, vaalAtlasEffect: Double = 1.0): ModifierWindow {
            val byStat = LinkedHashMap<String, MutableMap<ModifierSource, Double>>()
            parts.sortedBy { it.first.ordinal }.forEach { (source, lines) ->
                lines.forEach { (stat, value) -> byStat.getOrPut(stat) { sortedMapOf() }.merge(source, value, Double::plus) }
            }
            val sums = byStat.mapNotNull { (stat, shares) ->
                val kept = shares.filterValues { it != 0.0 }.map { (source, value) -> ModifierShare(source, value) }
                kept.sumOf { it.value }.takeIf { it != 0.0 }?.let { ModifierSum(stat, it, kept) }
            }
            return ModifierWindow(sums, vaalAtlasEffect)
        }
    }
}
