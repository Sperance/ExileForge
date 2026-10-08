package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EquipSlots
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.CombatProfile
import kotlin.math.abs

/**
 * Лучше или хуже (3.89.0): что смена вещи даст герою. С 4.2.0 - два вида: [SheetVerdict] (урон и защита листа) для
 * снаряжения и [FlaskVerdict] (строки листа фляги) для фляг, что лист героя не меняют.
 */
sealed interface GearVerdict {
    /** Насколько смена лучше в целом: по нему улучшения идут первыми. */
    val score: Double

    /** Куда сдвинулась величина, когда сдвиг достаточно велик, чтобы о нём говорить. */
    enum class Shift {
        UP,
        DOWN,
        EVEN,
        ;

        companion object {
            /** Меньше полупроцента - шум округления, а не лучшая или худшая вещь. */
            private const val THRESHOLD = 0.005

            fun of(change: Double): Shift = when {
                abs(change) < THRESHOLD -> EVEN
                change > 0 -> UP
                else -> DOWN
            }
        }
    }
}

/**
 * Вердикт снаряжения (3.89.0): [offence] и [defence] - доли изменения урона в секунду и эффективного запаса здоровья
 * (0.12 - на 12% больше). Считается правилами: лист с вещью против листа без неё.
 */
data class SheetVerdict(val offence: Double, val defence: Double) : GearVerdict {
    override val score: Double get() = offence + defence

    companion object {
        /** Вердикт листа [after] против [before] героя уровня [level]: оба листа меряются одним и тем же ударом. */
        fun of(index: ContentIndex, level: Int, before: Map<String, Double>, after: Map<String, Double>): SheetVerdict {
            val rules = index.campaign.combat
            val was = Combatant(before, level, rules)
            val hit = Toughness.referenceHit(was)
            return SheetVerdict(
                change(CombatProfile.of(index, before).best, CombatProfile.of(index, after).best),
                change(Toughness.of(was, hit), Toughness.of(Combatant(after, level, rules), hit)),
            )
        }

        private fun change(before: Double, after: Double): Double = if (before > 0) after / before - 1 else 0.0
    }
}

/**
 * Эффективный запас здоровья (3.89.0): здоровье и щит, делённые на долю удара, что до них доходит. Удар - атака монстра
 * уровня героя, поровну каждого типа урона: уклонение по меткости правил, блок, затем броня и снижение для физического и
 * сопротивления для прочих, с «получаемым уроном» поверх - теми же формулами, что бой.
 */
internal object Toughness {
    /** Удар, против которого судится броня: доля запаса героя до смены, один и тот же для обоих листов. */
    private const val HIT_SHARE = 0.1

    /** Наименьшая доля доходящего удара: лист в потолках всё равно сравнивается числом. */
    private const val FLOOR = 1e-3

    fun pool(body: Combatant): Double = body.maxLife + body.maxShield

    fun referenceHit(body: Combatant): Double = pool(body) * HIT_SHARE

    fun of(body: Combatant, hit: Double): Double {
        val rules = body.rules
        val foe = Combatant(emptyMap(), body.level, rules)
        val evaded = rules.accuracy.evaded(foe.accuracy(rules.accuracy), body.evasion).coerceAtMost(body.evasionCap)
        val landed = (1 - evaded) * (1 - body.block)
        val taken = DamageType.entries.map { type ->
            val mitigated = if (type == DamageType.PHYSICAL) body.physicalMitigation(hit, rules.armour.factor) else body.resist(type)
            body.damageTaken(type) * (1 - mitigated)
        }.average()
        return pool(body) / (landed * taken).coerceAtLeast(FLOOR)
    }
}

/**
 * Место, куда можно надеть вещь (3.90.3): [slot] - место на теле (у колец и фляг их несколько), [replaced] - что там
 * надето сейчас, [verdict] - урон и защита, если надеть вещь именно сюда (null - не по требованиям).
 */
data class WearPlace(val slot: Slot, val replaced: ItemInstance?, val verdict: GearVerdict?) {
    companion object {
        /** Место по умолчанию: первое пустое, а когда пустых нет - то, где замена даёт больший прирост. */
        fun best(places: List<WearPlace>): WearPlace? = places.firstOrNull { it.replaced == null } ?: places.maxByOrNull { it.verdict?.score ?: Double.NEGATIVE_INFINITY }
    }
}

/**
 * Вердикты вещей одного героя (3.89.0): лист с вещью складывается один раз на вещь и место, пока герой и контент те же, -
 * длинный тайник и лут не пересчитывают его при каждой прокрутке. Новый герой (новый лист) или контент сбрасывают память.
 * Потокобезопасна: считают её вне главного потока. С 3.90.3 вещь слота с несколькими местами (кольцо, фляга) меряется
 * на каждом, и её вердикт - вердикт лучшего места ([WearPlace.best]).
 */
class GearVerdicts {
    private var index: ContentIndex? = null
    private var hero: HeroView? = null
    private val known = HashMap<ItemInstance, List<WearPlace>>()

    /**
     * Вердикт [item] для [hero] на лучшем месте, или null: вещь не надевается (карта, самоцвет, инструмент), уже надета или
     * вставлена, требования не выполнены, контента или героя ещё нет.
     */
    fun of(index: ContentIndex?, hero: HeroView?, item: ItemInstance): GearVerdict? = WearPlace.best(places(index, hero, item))?.verdict

    /** Места [item] у [hero], каждое с тем, что там надето, и вердиктом; пусто - вещь не надеть или ещё нечего мерить. */
    fun places(index: ContentIndex?, hero: HeroView?, item: ItemInstance): List<WearPlace> {
        index ?: return emptyList()
        hero ?: return emptyList()
        synchronized(this) {
            if (this.index !== index || this.hero !== hero) {
                this.index = index
                this.hero = hero
                known.clear()
            }
            known[item]?.let { return it }
        }
        val places = weigh(index, hero, item)
        synchronized(this) { if (this.hero === hero && this.index === index) known[item] = places }
        return places
    }

    private fun weigh(index: ContentIndex, hero: HeroView, item: ItemInstance): List<WearPlace> {
        if (item.equipped || item.socketed) return emptyList()
        val slot = index.template(item.template)?.slot ?: return emptyList()
        if (slot.isJewelLike || slot.isTool) return emptyList()
        val wearable = Sheets.unmet(index, item.template, hero.level, hero.stats).isEmpty()
        val worn = hero.equipped
        return EquipSlots.places(slot).map { place ->
            val verdict = when {
                !wearable -> null

                // Фляга листа героя не меняет (4.2.0): её меряют строками листа фляги против надетой в этом гнезде.
                slot.isFlask -> FlaskVerdict.of(index, hero.stats, item, worn[place])

                else -> Sheets.verdict(index, item, hero.level, hero.heroClass, hero.tree, hero.items, hero.stats, hero.pets.active, place)
            }
            WearPlace(place, worn[place], verdict)
        }
    }
}
