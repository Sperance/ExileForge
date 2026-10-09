package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.Refusal
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.RuneDefinition
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.TierRule

/**
 * Ритуал следующего тира умения (4.4.0): правило тира [rule], работа чар [job] в профессии [profession], вся цена [cost] (вход работы,
 * книги умения и примесь его стихии) и отказ правил [refusal] - тот же, что бросит сервер.
 */
data class RitualView(val rule: TierRule, val profession: String, val job: Job.Evolve, val cost: List<JobInput>, val refusal: Refusal?)

/**
 * Рост умения, как его показывает гримуар (4.4.0) - всё из правил ([com.sperance.exileforge.rules.content.SkillGrowth]), клиент
 * своего не считает: уровень и опыт к следующему ([toNext], null - дальше уровней нет), потолок тира [cap], полна ли полоса в
 * ожидании требования ([waiting]), сколько опыта даст книга ([bookXp]), тир в бою [tier] и свой [owned], гнёзда открытые
 * [sockets] из всех [maxSockets], руны по гнёздам и ритуал следующего тира.
 */
data class SkillGrowthView(
    val level: Int,
    val xp: Double,
    val toNext: Double?,
    val cap: Int,
    val boostedCap: Int,
    val boostedTier: Int,
    val waiting: Boolean,
    val bookXp: Double,
    val tier: Int,
    val owned: Int,
    val sockets: Int,
    val maxSockets: Int,
    /** Тир, что открывает гнездо по его номеру; null - гнездо открыто или его не открыть тиром. */
    val opensAt: List<Int?>,
    val runes: List<RuneDefinition?>,
    val ritual: RitualView?,
) {
    /** На потолке тира: опыт и книги больше не ведут. */
    val capped: Boolean get() = level >= cap || toNext == null

    /** Доля полосы опыта: полная на потолке и в ожидании требования. */
    val share: Float get() = if (capped) 1f else toNext?.takeIf { it > 0 }?.let { (xp / it).toFloat().coerceIn(0f, 1f) } ?: 1f

    companion object {
        fun of(index: ContentIndex, skill: SkillDefinition, skills: HeroSkills, heroLevel: Int, stats: Map<String, Double>): SkillGrowthView {
            val growth = index.skillGrowth
            val progress = index.skills.rules.progress
            val level = skills.level(skill.code)
            val tier = growth.tier(skill, skills, stats)
            val sockets = growth.sockets(skill, skills, stats)
            val extra = (skill.kind.sockets?.let { stats[it.code] } ?: 0.0).toInt().coerceAtLeast(0)
            val maxSockets = maxOf(sockets, progress.tiers.maxOf { it.sockets } + extra)
            val opensAt = (0 until maxSockets).map { socket -> if (socket < sockets) null else progress.tiers.firstOrNull { it.sockets + extra > socket }?.tier }
            val runes = skills.runes(skill.code)
            return SkillGrowthView(
                level = level,
                xp = skills.xp(skill.code),
                toNext = growth.toNext(level),
                cap = growth.levelCap(skills.tier(skill.code)),
                boostedCap = index.skills.rules.boostedMaxLevel,
                boostedTier = progress.boostedTier,
                waiting = level > 0 && growth.waiting(skills, skill, heroLevel, stats),
                bookXp = progress.bookShare * (growth.toNext(level) ?: 0.0),
                tier = tier,
                owned = skills.tier(skill.code),
                sockets = sockets,
                maxSockets = maxSockets,
                opensAt = opensAt,
                runes = (0 until sockets).map { socket -> runes.getOrNull(socket)?.let(growth::rune) },
                ritual = ritual(index, skill, skills),
            )
        }

        /** Ритуал следующего тира: работа чар этого тира и её цена; null - тир последний или работы нет в контенте. */
        private fun ritual(index: ContentIndex, skill: SkillDefinition, skills: HeroSkills): RitualView? {
            val next = skills.tier(skill.code) + 1
            val rule = index.skills.rules.progress.tier(next) ?: return null
            val (profession, job) = index.professions.professions.firstNotNullOfOrNull { profession ->
                profession.jobs.filterIsInstance<Job.Evolve>().firstOrNull { it.tier == next }?.let { profession.code to it }
            } ?: return null
            val cost = job.inputs + index.skillGrowth.ritual(skill, next)
            return RitualView(rule, profession, job, cost, index.skillGrowth.evolveRefusal(skills, skill.code, next))
        }
    }
}
