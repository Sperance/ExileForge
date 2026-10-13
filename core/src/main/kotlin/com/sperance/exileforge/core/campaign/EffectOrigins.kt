package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatSource

/**
 * Откуда эффекты захода (4.10.x): сервер складывает строки атласа, древа гильдии и выпитого зелья в один `RunContext.atlas`, а
 * их доли называет отдельно (`RunContext.guild`, `RunContext.potion`). По ним сдвиг листа подписан своим источником -
 * «Древо гильдии», «Зелье», а не строка карты; остаток строки - атлас или карта по коду ([fallback]).
 */
class EffectOrigins(
    private val guild: Map<String, Double> = emptyMap(),
    private val potion: String? = null,
    private val potionLines: Map<String, Double> = emptyMap(),
    /** Узлы древа гильдии с этой строкой, через запятую: ref источника [SourceKind.GUILD] - по нему названа доля. */
    private val guildNodes: Map<String, String> = emptyMap(),
) {
    /** Доли строки [code] силой [value] по источникам: гильдия, зелье и остаток; нулевые доли не входят. */
    fun parts(code: String, value: Double): List<Pair<StatSource, Double>> {
        val guilded = guild[code] ?: 0.0
        val brewed = potionLines[code] ?: 0.0
        return listOfNotNull(
            guilded.takeIf { it != 0.0 }?.let { StatSource(SourceKind.GUILD, guildNodes[code] ?: code) to it },
            potion?.let { brewed.takeIf { it != 0.0 }?.let { StatSource(SourceKind.POTION, potion) to it } },
            (value - guilded - brewed).takeIf { it != 0.0 }?.let { fallback(code) to it },
        )
    }

    companion object {
        /** Без долей: вся строка - атлас или карта. */
        val NONE = EffectOrigins()

        /** Доли захода [context] со строками зелья из правил [index]. */
        fun of(context: RunContext, index: ContentIndex): EffectOrigins = EffectOrigins(
            context.guild,
            context.potion,
            context.potion?.let(index.rules.brews::potion).orEmpty(),
            index.guilds.tree.nodes.groupBy({ it.effect }, { it.code }).mapValues { (_, codes) -> codes.joinToString(GUILD_NODES) },
        )

        /** Разделитель узлов гильдии в ref источника. */
        const val GUILD_NODES = ","

        /** Источник строки без доли: дар атласа или строка карты. */
        fun fallback(code: String): StatSource = StatSource(if (code in AtlasEffects.hero.keys || code.startsWith("ATLAS_")) SourceKind.ATLAS else SourceKind.MAP, code)
    }
}
