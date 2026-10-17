package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.FateTrace
import com.sperance.exileforge.rules.content.FateLever

/** Чем назван предмет срабатывания [FateBeat]: ничем, недугом (`Ailment`) или умением героя (код). */
enum class FateRef { NONE, AILMENT, SKILL }

/**
 * Срабатывание рычага или правила строки дара в журнале боя: строка словаря клиента `expedition.fate_beat.<имя>`, её числа
 * - [FateTrace.args] по порядку `{0}`, `{1}`…, предмет [ref] - последним после чисел. [folds] - частое срабатывание: в журнал
 * ложится первое за бой, прочие копятся и в конце боя встают одной строкой свёртки (`expedition.log_fate_fold`): первые [kept]
 * чисел - как у последнего (доля, порог), прочие - сумма. Каждый отклик пишет своё срабатывание сам, через [Battle.fateBeat].
 */
enum class FateBeat(val folds: Boolean = false, val kept: Int = 0, val ref: FateRef = FateRef.NONE) {
    /** Кровь предков: ступень урона сменилась - `{0}`% урона, `{1}`% недостающего здоровья. */
    RAGE,

    /** Кровь предков: здоровье ниже `{1}`% - скорость атаки +`{0}`% ([com.sperance.exileforge.rules.content.FateKnob.LOW_LIFE_SPEED]). */
    RAGE_SPEED,

    /** Мститель: месть заряжена - удар снял `{0}`. */
    AVENGE_READY,

    /** Мститель: удар мести - крит на `{0}`. */
    AVENGE,

    /** Мститель: крит по герою погашен заслоном, ещё `{0}` с. */
    CRIT_WARD(folds = true, kept = 1),

    /** Кровавая жатва: `{0}` недугов павшего - следующему врагу. */
    SPREAD(folds = true),

    /** Стихийная триада: каждый `{0}`-й удар - недуг (предмет), `{1}` урона доли удара. */
    AILMENT(folds = true, kept = 1, ref = FateRef.AILMENT),

    /** Раскол души: павший взорвался - `{0}` урона, врагов `{1}`. */
    BURST(folds = true),

    /** Палач: враг добит ниже `{0}`% здоровья. */
    EXECUTE(folds = true, kept = 1),

    /** Кровь вожака: убийство - стопок `{0}`: +`{1}`% к скорости атаки и сотворения, +`{2}`% к вампиризму здоровья. */
    KILL_STACKS(folds = true, kept = 3),

    /** Ледяное сердце: заморозка стала замедлением `{0}`% на `{1}` с. */
    THAW(folds = true, kept = 1),

    /** Каменная кожа: удар срезан до `{0}`% здоровья - `{1}` → `{2}`. */
    CAP(folds = true, kept = 1),

    /** Каменная кожа: секунда недугов срезана до `{0}`% здоровья - `{1}` → `{2}`. */
    CAP_DOT(folds = true, kept = 1),

    /** Резонанс: умение (предмет) вслед иному - +`{0}`% урона, разных подряд `{1}`. */
    RESONANCE(folds = true, kept = 2, ref = FateRef.SKILL),

    /** Переполнение: цену умения заплатил перелив - `{0}` маны. */
    OVERFLOW_PAY(folds = true),

    /** Переполнение: перелив принял `{0}` урона. */
    OVERFLOW_SOAK(folds = true),

    /** Зеркало урона: запас ушёл ударом - +`{0}` урона. */
    MIRROR(folds = true),

    /** Зеркало урона: запас с прошлого боя захода - `{0}`. */
    MIRROR_KEPT,

    /** Живая крепость: удар по полному здоровью - −`{0}`% урона, `{1}` → `{2}`. */
    GUARD(folds = true, kept = 1),

    /** Живая крепость: первый удар по полному здоровью возвращён - `{0}` урона. */
    REFLECT,

    /** Кровь за кровь: лечение сверх полного - +`{0}` к барьеру. */
    BARRIER(folds = true),

    /** Кровь за кровь: барьер с прошлого боя захода - `{0}`. */
    BARRIER_KEPT,

    /** Удача рода: проверка с удачей, шанс `{0}`%. */
    LUCKY(folds = true, kept = 1),

    /** Обет крови: умение (предмет) оплачено здоровьем - `{0}`. */
    BLOOD(folds = true, ref = FateRef.SKILL),

    /** Призрачный клинок: призрак повторил удар - `{0}` урона. */
    PHANTOM(folds = true),

    /** Разгон: `{0}` побед в заходе - +`{1}`% урона. */
    STREAK,

    /** Разгон: `{0}` с неуязвимости за `{1}` побед. */
    STREAK_WARD,

    /** Равновесие: +`{0}`% урона за сопротивления сверх потолка. */
    OVERCAP,

    /** Проклятый металл: урон недуга (предмет) меньше на `{0}`%. */
    ARMOUR_AILMENT(folds = true, kept = 1, ref = FateRef.AILMENT),

    /** Рунный круг: умения с занятыми гнёздами - +`{0}`% урона. */
    FULL_SOCKETS,
}

/** Свёртка частого срабатывания [beat] за бой: сколько раз ещё ([repeats]) и итог его чисел ([args], по [FateBeat.kept]). */
internal class FateTally(private val beat: FateBeat) {
    var repeats = 0
        private set
    var args: List<Double> = emptyList()
        private set
    var ref: String = ""
        private set

    fun add(next: List<Double>, with: String) {
        args = if (repeats == 0) next else next.mapIndexed { i, v -> if (i < beat.kept) v else v + (args.getOrNull(i) ?: 0.0) }
        ref = with
        repeats++
    }
}

/**
 * Значок действующего рычага у героя в бою: ✦ среди состояний, пока рычаг [lever] дара [fate] активен; [value] и [count] -
 * числа подсказки `fight.fate_state.<рычаг>` (доля, запас, стаки), [left] - доля оставшегося срока, если он есть.
 */
data class FateGlow(val fate: String, val lever: FateLever, val value: Double, val count: Int = 0, val left: Float = 1f)

/**
 * Срабатывание [beat] Предначертания героя с числами [args] и предметом [ref] - строка журнала с [FateTrace]; частое ([FateBeat.folds])
 * после первого за бой копится в свёртку ([fateFolds]). Кость не тянется - запись боя не меняет.
 */
internal fun Battle.fateBeat(beat: FateBeat, vararg args: Double, ref: String = "") {
    val fate = kit.fate.effects.fate?.code ?: return
    if (beat.folds) {
        val tallies = fateFight.tallies
        tallies[beat]?.let {
            it.add(args.toList(), ref)
            return
        }
        tallies[beat] = FateTally(beat)
    }
    fateLine(fate, beat, args.toList(), ref, 0)
}

/** Свёртки частых срабатываний боя - по строке на каждое, что повторялось; в конце боя. */
internal fun Battle.fateFolds() {
    val fate = kit.fate.effects.fate?.code ?: return
    fateFight.tallies.forEach { (beat, tally) -> if (tally.repeats > 0) fateLine(fate, beat, tally.args, tally.ref, tally.repeats) }
    fateFight.tallies.clear()
}

private fun Battle.fateLine(fate: String, beat: FateBeat, args: List<Double>, ref: String, repeats: Int) = record(
    Side.HERO, Action.NOTE, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, target()?.index ?: 0, onSelf = true,
    trace = FateTrace(fate, beat, args, ref, repeats, shot(heroFighter), origin),
)

/** Действующие рычаги героя - значками среди его состояний. */
fun Battle.fateGlows(): List<FateGlow> {
    val fate = kit.fate.effects.fate?.code ?: return emptyList()
    return if (fated.isEmpty()) emptyList() else fated.mapNotNull { it.glow(this, fate) }
}
