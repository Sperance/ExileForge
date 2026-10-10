package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.StatLines
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.DevourerRule
import com.sperance.exileforge.rules.content.LordRule
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.WardenRule
import kotlin.math.max
import kotlin.math.min

// ==================== Стражи Разлома (3.96.0) ====================

/** Источник строк украденного дара [code] - на герое (снятые) и на его тени (данные). */
internal fun stolenSource(code: String) = "$STOLEN$code"

private const val STOLEN = "STOLEN#"

/** Источник силы Поглотителя, сытого Эхом. */
private const val DEVOURED = "$STOLEN#ECHO"

/** Строки Разлома лежат до конца боя и видны на полосе стража, а не плитками состояний. */
internal fun riftSourced(source: String): Boolean = source.startsWith(STOLEN)

/** Страж боя, пока он стоит; null - стража нет или он пал. */
private fun Battle.riftBoss(): Fighter? = guardian?.let(foeFighters::get)

/** [target] - страж боя. */
private fun Battle.isGuardian(target: Fighter): Boolean = target.side == Side.MONSTER && target.index == guardian

/**
 * Начало боя (3.96.0): Засада откладывает первые удары героя и питомца, Поглотитель встаёт сильнее на сытое им Эхо. Бросков
 * здесь нет: идолы встают на первом срезе.
 */
internal fun Battle.riftOpen(fight: RiftFight) {
    val ambush = fight.rules.ambush
    if (ambush > 0) {
        heroFighter.nextAttack += ambush
        pets.forEach { it.nextAttack += ambush }
    }
    val boss = riftBoss() ?: return
    if (fight.devoured > 0) {
        val lines = (listOf(CoreStat.HEALTH.code, StatLines.DAMAGE)).map { StatLine(it, Op.MORE, fight.devoured) }
        lay(boss, TimedEffect(EffectKind.BUFF, DEVOURED, lines, FOREVER, FOREVER))
    }
    fight.lastLife = boss.life
}

/** Герой и питомец уже в бою: Засада прошла. */
internal fun Battle.joined(): Boolean = time >= (rift?.ambush ?: 0.0)

/** Срез Разлома: печати, замки и идолы Стража, кражи Поглотителя, Законы Владыки. Зовётся в шаге после ярости стража. */
internal fun Battle.riftTick() {
    val fight = riftFight ?: return
    val boss = riftBoss()?.takeIf { it.alive } ?: return
    if (!fight.opened) {
        fight.opened = true
        rift?.idol?.let { idol -> repeat(rift.warden?.idolCount ?: 0) { raiseTotem(boss, listOf(idol)) } }
    }
    fight.rules.warden?.let { warden(fight, it, boss) }
    fight.rules.devourer?.let { devour(fight, it, boss) }
    fight.rules.lord?.let { lord(fight, it, boss) }
}

// ==================== Страж Врат ====================

private fun Battle.warden(fight: RiftFight, rule: WardenRule, boss: Fighter) {
    if (!fight.wardenPhased && boss.life < boss.body.maxLife * rule.phaseAt / 100) {
        fight.wardenPhased = true
        fight.sealsMax = rule.phaseSeals
        if (fight.seals < fight.sealsMax) {
            fight.seals = fight.sealsMax
            note(boss, NoteKind.SEALS_RETURNED, "", fight.seals.toDouble())
        }
        fight.sealsAt = FOREVER
    }
    if (fight.seals < fight.sealsMax && time >= fight.sealsAt - 1e-9) {
        fight.seals++
        fight.sealsAt = if (fight.seals < fight.sealsMax) time + regrowEvery(fight, rule) else FOREVER
        note(boss, NoteKind.SEALS_RETURNED, "", fight.seals.toDouble())
    }
    if (time >= fight.lockAt - 1e-9) {
        fight.lockAt = time + rule.lockEvery
        lock(fight, rule, boss)
    }
}

/** Через сколько секунд возвращается следующая печать: в фазе быстрее, при идолах - ещё быстрее. */
private fun Battle.regrowEvery(fight: RiftFight, rule: WardenRule): Double {
    val phase = if (fight.wardenPhased) rule.phaseRegrow else 1.0
    val idols = if (standingTotems.any { it.totem.totem.code == rule.idols }) rule.idolRate else 1.0
    return rule.every / phase / idols
}

/** Множитель урона по [target]: пока стоит хоть одна печать Стража - его [WardenRule.taken]; 1 - вне Разлома. */
internal fun Battle.sealed(target: Fighter): Double {
    val fight = riftFight ?: return 1.0
    val rule = fight.rules.warden ?: return 1.0
    return if (fight.seals > 0 && isGuardian(target)) max(0.0, 1 + rule.taken / 100) else 1.0
}

/**
 * Попадание стороны героя по [target] - удар героя, удар питомца, тик недуга героя: каждые [WardenRule.hits] снимают печать,
 * крит - сразу.
 */
internal fun Battle.sealStruck(target: Fighter, crit: Boolean) {
    val fight = riftFight ?: return
    val rule = fight.rules.warden ?: return
    if (fight.seals <= 0 || !isGuardian(target) || !target.alive) return
    if (!crit) {
        if (++fight.sealHits < rule.hits) return
        fight.sealHits = 0
    }
    fight.seals--
    fight.sealsAt = time + rule.regrow
    note(target, NoteKind.SEAL_BROKEN, "", fight.seals.toDouble())
}

/** «Запечатывание»: случайное умение или флакон героя заперт на [WardenRule.lock] секунд. */
private fun Battle.lock(fight: RiftFight, rule: WardenRule, boss: Fighter) {
    val skills = kit.actives.indices.filter { kit.actives[it] != null }
    val flasks = kit.flasks.indices.filter { kit.flasks[it] != null }
    val total = skills.size + flasks.size
    if (total == 0) return
    val pick = random.nextInt(total)
    val until = time + rule.lock
    if (pick < skills.size) {
        val slot = skills[pick]
        fight.skillLocks[slot] = until
        note(boss, NoteKind.SEAL_LOCK_SKILL, kit.actives[slot]!!.skill.code, rule.lock)
    } else {
        val slot = flasks[pick - skills.size]
        fight.flaskLocks[slot] = until
        note(boss, NoteKind.SEAL_LOCK_FLASK, kit.flasks[slot]!!.sheet.code, rule.lock)
    }
}

/** Сколько ещё секунд заперт слот умения [slot]; 0 - свободен. */
internal fun Battle.skillLock(slot: Int): Double = riftFight?.let { (it.skillLocks.getOrElse(slot) { 0.0 } - time).coerceAtLeast(0.0) } ?: 0.0

/** Сколько ещё секунд заперт флакон [slot]; 0 - свободен. */
internal fun Battle.flaskLock(slot: Int): Double = riftFight?.let { (it.flaskLocks.getOrElse(slot) { 0.0 } - time).coerceAtLeast(0.0) } ?: 0.0

// ==================== Поглотитель эха ====================

/** Раз в [DevourerRule.stealEvery] секунд (ниже порога - чаще) крадёт дар, и тень встаёт с ним, пока теней хватает. */
private fun Battle.devour(fight: RiftFight, rule: DevourerRule, boss: Fighter) {
    val rate = if (boss.life < boss.body.maxLife * rule.phaseAt / 100) rule.phaseRate else 1.0
    if (time < fight.stoleAt + rule.stealEvery / rate - 1e-9) return
    fight.stoleAt = time
    val left = fight.rules.boons.filter { it.code !in fight.stolen }
    if (left.isEmpty()) return
    val boon = left[random.nextInt(left.size)]
    fight.stolen += boon.code
    lay(heroFighter, TimedEffect(EffectKind.BUFF, stolenSource(boon.code), boon.lines.mapNotNull(::inverse), FOREVER, FOREVER))
    note(boss, NoteKind.BOON_STOLEN, boon.code)
    if (fight.stolen.size > rule.shades) return
    summon(boss, 1).forEach { shade -> buff(foeFighters[shade], stolenSource(boon.code), boon.lines, FOREVER) }
}

// ==================== Владыка Разлома ====================

private fun Battle.lord(fight: RiftFight, rule: LordRule, boss: Fighter) {
    val lost = fight.lastLife - boss.life
    if (lost > 0 && fight.inForce(RiftLaw.REWIND)) fight.losses.addLast(time to lost)
    while (fight.losses.firstOrNull()?.let { time - it.first > rule.rewind } == true) fight.losses.removeFirst()
    fight.lastLife = boss.life
    if (fight.next.isEmpty() && time >= fight.lawAt - rule.warn - 1e-9) announce(fight, rule, boss)
    if (time < fight.lawAt - 1e-9) return
    rewind(fight, rule, boss)
    fight.laws = fight.next
    fight.element = fight.nextElement
    fight.next = emptyList()
    fight.nextElement = null
    fight.lawAt = time + rule.every
    fight.laws.forEach { note(boss, NoteKind.LAW, it.name) }
    if (RiftLaw.HEAVY_BLOWS in fight.laws) heroFighter.nextAttack = max(heroFighter.nextAttack, time + heroFighter.attackInterval * rule.heavyEvery)
}

/** Знамение: Законы, что вступят через [LordRule.warn] секунд, - новые, кроме отменённого контрактом; ниже порога два разом. */
private fun Battle.announce(fight: RiftFight, rule: LordRule, boss: Fighter) {
    val pool = rule.laws.distinct().filter { it != fight.rules.forbidden }
    val bag = pool.filter { it !in fight.laws }.ifEmpty { pool }.toMutableList()
    val count = if (boss.life < boss.body.maxLife * rule.lastAt / 100) rule.lastLaws else 1
    fight.next = List(min(count, bag.size)) { bag.removeAt(random.nextInt(bag.size)) }
    fight.nextElement = if (RiftLaw.ONE_ELEMENT in fight.next) DamageType.ELEMENTS[random.nextInt(DamageType.ELEMENTS.size)] else null
}

/** «Время вспять» кончается: урон последних [LordRule.rewind] секунд, не дотянувший до [LordRule.rewindBreak]%, возвращается. */
private fun Battle.rewind(fight: RiftFight, rule: LordRule, boss: Fighter) {
    if (!fight.inForce(RiftLaw.REWIND)) return
    val window = fight.losses.sumOf { it.second }
    fight.losses.clear()
    if (window <= 0 || window >= boss.body.maxLife * rule.rewindBreak / 100) return
    val before = boss.life
    boss.life = min(boss.body.maxLife, boss.life + window)
    fight.lastLife = boss.life
    note(boss, NoteKind.LAW_REWOUND, RiftLaw.REWIND.name, boss.life - before)
}

/** Закон [law] сейчас в силе. */
internal fun Battle.inForce(law: RiftLaw): Boolean = riftFight?.inForce(law) == true

/** Множитель урона типа [type] по [target]: под «Одной стихией» Владыку ранит лишь её стихия; 1 - вне Разлома. */
internal fun Battle.lawTaken(target: Fighter, type: DamageType?): Double {
    val fight = riftFight ?: return 1.0
    val element = fight.element ?: return 1.0
    return if (fight.inForce(RiftLaw.ONE_ELEMENT) && isGuardian(target) && type != element) 0.0 else 1.0
}

/** Во сколько раз реже бьёт [me]: под «Тяжкими ударами» - герой. */
internal fun Battle.cadence(me: Fighter): Double = if (me === heroFighter && inForce(RiftLaw.HEAVY_BLOWS)) rift?.lord?.heavyEvery ?: 1.0 else 1.0

/** Во сколько раз сильнее удар оружия [me] по [blow]: под «Тяжкими ударами» - героя. */
internal fun Battle.heavy(me: Fighter, blow: Blow): Double {
    val rule = rift?.lord ?: return 1.0
    return if (me === heroFighter && blow.action == Action.ATTACK && inForce(RiftLaw.HEAVY_BLOWS)) rule.heavy else 1.0
}

/** Крит [me] гаснет: под «Без критов» сторона героя бьёт обычным ударом - броски те же. */
internal fun Battle.critBanned(me: Fighter): Boolean = me.side == Side.HERO && inForce(RiftLaw.NO_CRITS)

/** Щит [me] не держит и не заряжается: «Без щита» для героя. */
internal fun Battle.shieldless(me: Fighter): Boolean = me === heroFighter && inForce(RiftLaw.NO_SHIELD)

/** Мана [me] не течёт сама: «Без маны» для героя. */
internal fun Battle.manaless(me: Fighter): Boolean = me === heroFighter && inForce(RiftLaw.NO_MANA)

/** «Цена крови»: удар героя, дошедший до цели, стоит доли его здоровья - но не последней единицы. */
internal fun Battle.bloodPrice(me: Fighter) {
    if (me !== heroFighter || !inForce(RiftLaw.BLOOD_PRICE)) return
    val price = me.body.maxLife * rift!!.lord!!.bloodPrice / 100
    me.life = max(min(me.life, 1.0), me.life - price)
}

/**
 * Жизнь [amount], данная [me] (3.96.0): все пути лечения героя идут через неё. Под «Лечение ранит» герой теряет её, но не
 * последнюю единицу; вне Закона - прежняя формула. Сколько вылечено. [overheal] (4.6.1) - лечение, излишек которого сверх
 * полного здоровья героя берёт Предначертание (похищение, за удар и убийство, фляги).
 */
internal fun Battle.lifeBack(me: Fighter, amount: Double, overheal: Boolean = false): Double {
    val before = me.life
    if (amount > 0 && me === heroFighter && inForce(RiftLaw.HEALING_HURTS)) {
        me.life = max(min(before, 1.0), before - amount)
        return 0.0
    }
    me.life = min(me.body.maxLife, me.life + amount)
    if (overheal && me === heroFighter) fateOverheal(before + amount - me.life)
    return me.life - before
}
