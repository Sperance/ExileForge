package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.DevourerRule
import com.sperance.exileforge.rules.content.LordRule
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.WardenRule

/** Дар героя, как его видит бой (3.96.0): код и строки листа, уже выросшие по ступени. */
data class RiftBoonLines(val code: String, val lines: List<StatLine>)

/**
 * Бой Разлома недели (3.96.0): механики стража акта - [warden], [devourer] или [lord] (числа - их правил) - и правила забега.
 * Без него ([Battle.rift] = null) бой тот же, что был, на тех же костях.
 */
data class RiftCombat(
    val warden: WardenRule? = null,
    val devourer: DevourerRule? = null,
    /** Непотраченное Эхо забега: им сыт Поглотитель. */
    val echo: Int = 0,
    /** Дары героя: что может украсть Поглотитель. */
    val boons: List<RiftBoonLines> = emptyList(),
    val lord: LordRule? = null,
    /** Закон, отменённый контрактом Владыки. */
    val forbidden: RiftLaw? = null,
    /** Засада: столько секунд герой и питомец вступают в бой позже. */
    val ambush: Double = 0.0,
    /** Идол Стража - тотем правил [WardenRule.idols], найденный среди тотемов кампании; null - Страж без идолов. */
    val idol: FoeTotem? = null,
) {
    /** Слоты вокруг стража, нужные Разлому: идолам Стража. Тени Поглотителя свои слоты приносят сами ([withShades]). */
    internal val slots: Int get() = if (warden != null && idol != null) warden.idolCount else 0
}

/**
 * След боя для Испытания чемпиона (3.96.0): сколько флаконов выпито (кроме выпитых самими собой в начале боя), самая низкая
 * доля здоровья героя (0..1) и сколько раз игрок переводил цель на другого живого врага, пока прежняя цель ещё стояла.
 */
class RiftTrace internal constructor() {
    var flasks: Int = 0
        internal set
    var lowestLife: Double = 1.0
        internal set
    var focusChanges: Int = 0
        internal set
}

/** Кого Разлом зовёт в HUD: страж с печатями, с дарами или с Законами. */
enum class RiftGuard { WARDEN, DEVOURER, LORD }

/** Состояние механик Разлома в бою (3.96.0): печати, замки, украденное, Законы. */
internal class RiftFight(val rules: RiftCombat, actives: Int, flasks: Int) {
    var opened = false

    // Страж Врат
    var seals = rules.warden?.seals ?: 0
    var sealsMax = seals
    var sealHits = 0

    /** Когда вернётся следующая печать; [FOREVER] - все на месте. */
    var sealsAt = FOREVER
    var wardenPhased = false
    var lockAt = rules.warden?.lockEvery ?: FOREVER
    val skillLocks = DoubleArray(actives)
    val flaskLocks = DoubleArray(flasks)

    // Поглотитель эха
    val devoured: Double = rules.devourer?.let { minOf(rules.echo * it.perEcho, it.maxPower) } ?: 0.0
    val stolen = mutableListOf<String>()
    var stoleAt = 0.0

    // Владыка Разлома
    var laws: List<RiftLaw> = emptyList()
    var element: DamageType? = null
    var next: List<RiftLaw> = emptyList()
    var nextElement: DamageType? = null
    var lawAt = rules.lord?.every ?: FOREVER

    /** Урон по Владыке под «Временем вспять»: когда и сколько, и его здоровье на прошлом срезе. */
    val losses = ArrayDeque<Pair<Double, Double>>()
    var lastLife = -1.0

    fun inForce(law: RiftLaw): Boolean = law in laws

    val guard: RiftGuard? get() = when {
        rules.warden != null -> RiftGuard.WARDEN
        rules.devourer != null -> RiftGuard.DEVOURER
        rules.lord != null -> RiftGuard.LORD
        else -> null
    }
}

/** Страж боя: враг с фазами или тотемами либо редкости босса - его ищут и бой, и [withShades]. */
internal val Foe.guards: Boolean get() = phases.isNotEmpty() || totems.isNotEmpty() || rarity == MonsterRarity.UNIQUE

/**
 * Тени Поглотителя (3.96.0): к стае [foes] добавлены [count] копий [shade] - свита стража, что ждёт зова вне поля; бой не
 * добавляет врагов посреди себя, так что украденный дар зовёт уже лежащую тень. Страж получает слоты под них.
 */
fun PhaseFoes.withShades(foes: List<Foe>, shade: Foe, count: Int): List<Foe> {
    val boss = foes.indexOfFirst { it.guards }
    if (boss < 0 || count <= 0) return foes
    val grown = foes.mapIndexed { i, foe -> if (i == boss) foe.copy(slots = maxOf(foe.slots, count)) else foe }
    return grown + List(count) { shade.copy(summonOf = boss) }
}

/** Строки, что снимают [line] с листа: прибавка - вычетом, увеличение - уменьшением, «больше» - обратным «меньше»; SET не снять. */
internal fun inverse(line: StatLine): StatLine? = when (line.op) {
    Op.ADD, Op.INCREASED -> line.copy(value = -line.value)
    Op.MORE -> line.takeIf { it.value > -100 }?.copy(value = -100 * line.value / (100 + line.value))
    Op.SET -> null
}
