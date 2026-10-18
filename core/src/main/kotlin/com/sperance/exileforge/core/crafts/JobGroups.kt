package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Job

/**
 * Группы работ профессии для отбора в её окне - по тому, что работа даёт: стопка - категория её предмета (материал - с подкатегорией:
 * примеси, зелья, скарабеи, сырьё), прочие - вид работы. [counts] - группы с открытыми работами ([ProfessionView.opens]) в порядке
 * первой такой работы и сколько их в группе, [total] - сколько открытых всего; группа без открытых работ в отбор не входит.
 * Подпись группы - ключ словаря `crafts.group.<код>`.
 */
class JobGroups private constructor(val counts: List<Pair<String, Int>>, private val byJob: Map<String, String>) {
    val size: Int get() = counts.size

    /** Сколько открытых работ во всех группах. */
    val total: Int get() = counts.sumOf { it.second }

    /** Есть ли в отборе группа [group]. */
    operator fun contains(group: String): Boolean = counts.any { it.first == group }

    /** Группа работы [job] - и закрытой. */
    fun groupOf(job: JobView): String = byJob[job.code].orEmpty()

    companion object {
        fun of(profession: ProfessionView, index: ContentIndex?): JobGroups {
            val byJob = profession.jobs.associate { it.code to key(it.job, index) }
            val counts = profession.jobs.filter(profession::opens).groupingBy { byJob.getValue(it.code) }.eachCount().toList()
            return JobGroups(counts, byJob)
        }

        /** Код группы работы [job]: категория предмета стопки (у материала - с подкатегорией через точку) или вид работы. */
        fun key(job: Job, index: ContentIndex?): String {
            val item = (job as? Job.Item)?.let { index?.item(ItemCode(it.output)) } ?: return job.kind.name
            return if (item.category == Item.MATERIAL) "${item.category}.${item.subCategory}" else item.category
        }
    }
}
