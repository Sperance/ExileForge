package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.rules.text.ClassText
import com.sperance.exileforge.rules.text.LocaleKey

/**
 * A class as the choosing screen shows it (3.13.0): who it is in the dictionary's words, what a level-one hero of it
 * starts with by the rules' own sheet, the skills it learns and where on the tree it stands.
 */
data class ClassGuide(
    val code: String,
    val difficulty: Int,
    val attributes: List<Pair<String, Double>>,
    val vitals: List<Pair<String, Double>>,
    val weapon: String,
    val skills: List<SkillDefinition>,
    val start: TreeNode?,
    /** The tree nodes nearer this class's start than any other: its own branch. */
    val region: Set<String>,
    val keystones: List<TreeNode>,
) {
    val role: String get() = text(ClassText.ROLE)
    val lore: String get() = text(ClassText.LORE)
    val style: String get() = text(ClassText.STYLE)
    val pros: List<String> get() = lines(ClassText.PROS)
    val cons: List<String> get() = lines(ClassText.CONS)
    val builds: List<String> get() = text(ClassText.BUILDS).split(',').map(String::trim).filter(String::isNotEmpty)

    private fun text(field: ClassText) = locOr(LocaleKey.classText(code, field), "")
    private fun lines(field: ClassText) = text(field).lines().map(String::trim).filter(String::isNotEmpty)

    companion object {
        const val MAX_DIFFICULTY = 3
        val ATTRIBUTES = listOf("STOCK_STRENGTH", "STOCK_AGILITY", "STOCK_INTELLECT")
        private val VITALS = listOf("STOCK_HEALTH", "STOCK_MANA", "STOCK_ENERGY_SHIELD", "STOCK_EVASION", "STOCK_ARMOR")

        fun of(index: ContentIndex, code: String): ClassGuide? {
            val heroClass = index.heroClass(code) ?: return null
            val stats = Sheets.calculate(index, 1, code, emptyList(), emptyList()).stats
            val region = ClassRegions(index).of(heroClass.startNode)
            return ClassGuide(
                code = code,
                difficulty = heroClass.difficulty.coerceIn(0, MAX_DIFFICULTY),
                attributes = ATTRIBUTES.map { it to (stats[it] ?: 0.0) },
                vitals = VITALS.mapNotNull { stat -> stats[stat]?.takeIf { it > 0 }?.let { stat to it } },
                weapon = heroClass.weapon,
                skills = index.skills.ofClass(code).filter { it.type.kind == SkillKind.ACTIVE }.sortedBy { it.unlock },
                start = index.tree.node(heroClass.startNode),
                region = region,
                keystones = region.mapNotNull(index.tree::node).filter { it.type == SkillNodeType.KEYSTONE }.sortedBy { it.code },
            )
        }
    }
}

/** The tree split among the classes: every node belongs to the start fewest steps away along the tree's own paths. */
class ClassRegions(private val index: ContentIndex) {
    private val owner: Map<String, String> by lazy {
        val owner = HashMap<String, String>()
        val queue = ArrayDeque<String>()
        index.tree.byCode.values.filter { it.type == SkillNodeType.START }.forEach { owner[it.code] = it.code; queue += it.code }
        while (queue.isNotEmpty()) {
            val code = queue.removeFirst()
            index.tree.neighbours(code).filter { it !in owner }.forEach { owner[it] = owner.getValue(code); queue += it }
        }
        owner
    }

    fun of(startNode: String): Set<String> = owner.filterValues { it == startNode }.keys
}
