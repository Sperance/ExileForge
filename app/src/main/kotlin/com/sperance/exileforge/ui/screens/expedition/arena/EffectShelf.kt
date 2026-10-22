package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.combat.fateSourced
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.FlowTone
import com.sperance.exileforge.core.display.Rate
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.effectText
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.flowFigure
import com.sperance.exileforge.core.display.flowParts
import com.sperance.exileforge.core.display.flowRate
import com.sperance.exileforge.core.display.guildLineTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.totemTitle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Знак плитки: вред тому, на ком лежит, - кровавая рамка и уголок, польза - золотые. Одна классификация на все плитки - из данных
 * плитки ([Shelved.bearing]), цвет самого эффекта остаётся заливкой и значком.
 */
internal enum class Bearing(val tint: Color) {
    HARM(LifeRed),
    BENEFIT(Boon),
    ;

    companion object {
        fun of(harm: Boolean): Bearing = if (harm) HARM else BENEFIT
    }
}

/**
 * Плитка полки эффектов: что лежит на бойце - плиткой на полке и строкой листа эффектов, со знаком вреда или пользы ([bearing]).
 * Новый вид состояния - новая реализация здесь, полка и лист берут его сами.
 */
internal sealed interface Shelved {
    val bearing: Bearing

    /** Оглушение (не заморозка): золотая звезда. */
    data object Stun : Shelved {
        override val bearing: Bearing get() = Bearing.HARM
    }

    /** Недуг со стаками: всегда во вред тому, на ком лежит. */
    data class Sick(val view: AilmentView) : Shelved {
        override val bearing: Bearing get() = Bearing.HARM
    }

    /** Бафф, проклятие, помеха, глоток флакона, пассивное умение: вред - по виду эффекта ([EffectKind.harmful]). */
    data class Effect(val view: EffectView) : Shelved {
        override val bearing: Bearing get() = Bearing.of(view.kind.harmful)
    }

    /** Заряды героя одного вида. */
    data class Charge(val view: ChargeView) : Shelved {
        override val bearing: Bearing get() = Bearing.BENEFIT
    }

    /** Действующий рычаг Предначертания ✦. */
    data class Fate(val view: FateGlow) : Shelved {
        override val bearing: Bearing get() = Bearing.BENEFIT
    }

    /** Строка карты, что меняет героя: вред - по её знаку ([HeroMapMod.harm]). */
    data class Mod(val mod: HeroMapMod) : Shelved {
        override val bearing: Bearing get() = Bearing.of(mod.harm)
    }

    /**
     * Сила уникального предмета героя: постоянная - в листе «на весь бой», сила-событие - на временной полке; [holders] - имена вещей,
     * что её несут ([PowerView.holders]).
     */
    data class Power(val view: PowerView, val holders: List<String> = emptyList()) : Shelved {
        override val bearing: Bearing get() = Bearing.BENEFIT
    }
}

/**
 * Строка листа эффектов: имя цветом эффекта, откуда он, что делает и сколько ему осталось ([seconds] null - весь бой); [state] - что
 * писать вместо срока (глоток прерван).
 */
internal data class ShelfRow(val title: String, val tint: Color, val source: String, val lines: List<String>, val seconds: Double?, val state: String? = null)

/** Оглушение героя или врага: держится и это не заморозка. */
internal fun stunned(held: Boolean, ailments: List<AilmentView>): Boolean = held && ailments.none { it.ailment == Ailment.FROZEN }

/**
 * Временные эффекты бойца для листа эффектов: оглушение, недуги, эффекты с таймером, заряды, силы-события героя - последними;
 * полка под карточкой берёт из них только [onShelf]. [holder] - имя надетой вещи по id (носитель силы), не найдена - null.
 */
internal fun timedShelf(
    held: Boolean,
    ailments: List<AilmentView>,
    effects: List<EffectView>,
    charges: List<ChargeView> = emptyList(),
    powers: List<PowerView> = emptyList(),
    holder: (String) -> String? = { null },
): List<Shelved> = buildList {
    if (stunned(held, ailments)) add(Shelved.Stun)
    ailments.forEach { add(Shelved.Sick(it)) }
    effects.forEach { if (!it.lasting) add(Shelved.Effect(it)) }
    charges.forEach { add(Shelved.Charge(it)) }
    powers.forEach { if (!it.standing) add(Shelved.Power(it, it.holders.mapNotNull(holder))) }
}

/**
 * Место ли плитке на временной полке под карточкой: силе - только на перезарядке или со счётом «каждое N-е», глотку - пока он
 * не прерван; сила «готова», потраченная на бой и прерванный глоток без срока остаются лишь в листе эффектов.
 */
internal val Shelved.onShelf: Boolean get() = when (this) {
    is Shelved.Effect -> !view.halted
    is Shelved.Power -> !view.spent && (view.ready > 0 || view.nth != null)
    else -> true
}

/**
 * Эффекты героя «на весь бой» - лист за кнопкой «≡ N» у портрета ([LastingSheet]): пассивные умения, постоянные силы уникалок
 * (с именем вещи - [holder]), носимые с начала баффы, строки карты и дары Предначертания.
 */
internal fun lastingShelf(fight: FightHud, mods: List<HeroMapMod>, holder: (String) -> String? = { null }): List<Shelved> = buildList {
    fight.heroPassives.forEach { add(Shelved.Effect(it)) }
    fight.heroPowers.forEach { if (it.standing) add(Shelved.Power(it, it.holders.mapNotNull(holder))) }
    fight.heroEffects.forEach { if (it.lasting) add(Shelved.Effect(it)) }
    mods.forEach { add(Shelved.Mod(it)) }
    fight.heroFate.forEach { add(Shelved.Fate(it)) }
}

/** Группа листа «на весь бой» по источнику эффекта: она же - чип-фильтр листа. */
internal enum class LastingGroup { SKILLS, ITEMS, WORLD, FATE }

/** Группа эффекта «на весь бой»: умения, предметы (силы уникалок и носимые баффы), карта с гильдией и зельем, дары Судьбы. */
internal val Shelved.lastingGroup: LastingGroup get() = when (this) {
    is Shelved.Effect -> when {
        view.passive -> LastingGroup.SKILLS
        fateSourced(view.source) -> LastingGroup.FATE
        else -> LastingGroup.ITEMS
    }

    is Shelved.Mod -> LastingGroup.WORLD

    is Shelved.Fate -> LastingGroup.FATE

    else -> LastingGroup.ITEMS
}

@Composable internal fun ShelfTile(item: Shelved, side: Dp) {
    val index = LocalContentIndex.current
    val tip = { item.tip(index) }
    when (item) {
        Shelved.Stun -> StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned"), item.bearing, side, tip)
        is Shelved.Sick -> StateTile(item.view.ailment, ailmentTint(item.view.ailment), item.view.left, item.view.stacks, ailmentLabel(item.view), item.bearing, side, tip)
        is Shelved.Effect -> EffectTile(item.view, item.bearing, side, tip)
        is Shelved.Charge -> ChargeTile(item.view, item.bearing, side, tip)
        is Shelved.Fate -> FateTile(item.view, item.bearing, side, tip)
        is Shelved.Mod -> MapModTile(item.mod, item.bearing, side, tip)
        is Shelved.Power -> PowerTile(item.view, item.bearing, side, tip)
    }
}

/**
 * Подробная выноска плитки: имя, что делает - те же строки, что в листе эффектов ([row]), откуда, сколько осталось и
 * стаки; у недуга - ещё его сила.
 */
internal fun Shelved.tip(index: ContentIndex?): Tip {
    val row = row(index)
    val own = (this as? Shelved.Sick)?.let { ailmentTip(it.view) }
    return Tip(
        row.title,
        (own?.body?.let(::listOf) ?: row.lines).joinToString("\n"),
        row.tint,
        listOf(ui("fight.fact_source") to row.source) + (own?.facts ?: listOfNotNull(row.seconds?.takeIf { it > 0 }?.let { ui("fight.fact_left") to ui("fight.fact_seconds", fineNumber(it)) })),
    )
}

/** Что сказать о плитке в листе эффектов: те же слова, что её подсказка, и строки, что она кладёт; числа правил строк - из контента [index]. */
internal fun Shelved.row(index: ContentIndex?): ShelfRow = when (this) {
    Shelved.Stun -> ShelfRow(ui("expedition.stunned"), GoldBright, ui("fight.src.stun"), listOf(ui("fight.effect.STUN")), null)

    is Shelved.Sick -> ailmentTip(view).let { tip ->
        ShelfRow(ailmentLabel(view), tip.tint, ui("fight.src.ailment"), listOf(tip.body) + tip.facts.filter { it.first != ui("fight.fact_left") }.map { (k, v) -> "$k: $v" }, view.seconds)
    }

    is Shelved.Effect -> ShelfRow(
        effectTitle(view),
        effectTint(view),
        ui(
            when {
                view.passive -> "fight.src.passive"
                view.flask != null -> "fight.src.flask"
                view.source == INTIMIDATED -> "fight.src.intimidate"
                view.kind == EffectKind.CURSE -> "fight.src.curse"
                view.kind == EffectKind.HINDER -> "fight.src.hinder"
                view.kind == EffectKind.MARK -> "fight.src.mark"
                view.lasting -> "fight.src.worn"
                else -> "fight.src.buff"
            },
        ),
        when {
            view.halted -> listOf(ui("fight.flask_halted"))
            view.source == INTIMIDATED -> listOf(ui("fight.intimidated_hint"))
            view.kind == EffectKind.MARK -> listOf(ui("fight.mark_hint"))
            else -> view.lines.map { SkillText.statLine(it.stat, it.op, it.value, index) }
        },
        view.seconds.takeUnless { view.lasting || view.halted },
        ui("fight.flask_halted_short").takeIf { view.halted },
    )

    is Shelved.Charge -> ShelfRow(ui("fight.charge.${view.kind.name}"), Gold, ui("fight.src.charge"), listOf(ui("fight.charge_count", view.count, view.max)), view.seconds)

    is Shelved.Fate -> ShelfRow(
        locOr("fate.${view.fate}.name", view.fate),
        FateInk.Gold,
        ui("fight.src.fate"),
        listOf(ui("fight.fate_state.${view.lever.name}", fineNumber(view.value), view.count)),
        null,
    )

    is Shelved.Mod -> ShelfRow(
        modTitle(mod),
        modTint(mod),
        ui(modSource(mod)),
        mod.shifts.map { (stat, delta) -> effectText(stat, Op.ADD, delta) },
        null,
    )

    is Shelved.Power -> ShelfRow(
        statTitle(view.stat),
        UniqueTint,
        holders.distinct().joinToString(", ").ifEmpty { ui("fight.src.power") },
        listOf(SkillText.statLine(view.stat, Op.ADD, view.value, index)) + powerFacts(view),
        null,
        powerState(view),
    )
}

/** Цвет сил уникалок: цвет уникальной редкости. */
private val UniqueTint: Color get() = rarityColor(Rarity.UNIQUE.name)

/** Счёт силы-события строками листа: сколько раз сработала, счёт «каждое N-е», перезарядка. */
private fun powerFacts(view: PowerView): List<String> = if (view.standing) {
    emptyList()
} else {
    listOfNotNull(
        if (view.fired > 0) ui("fight.power.fired", view.fired) else null,
        view.nth?.let { (count, every) -> ui("fight.power.every", count, every) },
        if (view.cooldown > 0) ui("fight.power.cooldown", fineNumber(view.cooldown)) else null,
    )
}

/**
 * Что писать вместо срока у силы-события, по её счёту: раз за бой - «истрачена» или «раз за бой», идущая перезарядка - секунды до
 * готовности, «каждое N-е» - сколько ещё до срабатывания, вышедшая перезарядка - «наготове»; сила по событию без перезарядки и счёта -
 * ничего (пустая строка). Постоянная - «весь бой», как у прочих строк без срока.
 */
private fun powerState(view: PowerView): String {
    val nth = view.nth
    return when {
        view.standing -> ui("fight.shelf.whole")
        view.spent -> ui("fight.power.spent")
        view.once -> ui("fight.power.once")
        view.ready > 0 -> ui("fight.fact_seconds", fineNumber(view.ready))
        nth != null -> ui("fight.power.left", nth.second - nth.first)
        view.cooldown > 0 -> ui("fight.power.ready")
        else -> ""
    }
}

/** Метка в углу плитки силы-события: счёт «каждое N-е» или сколько раз сработала; нечего считать - null. */
private fun powerBadge(view: PowerView): String? = if (view.standing) {
    null
} else {
    view.nth?.let { (count, every) -> ui("fight.power.every_badge", count, every) } ?: view.fired.takeIf { it > 0 }?.toString()
}

/**
 * Сила уникального предмета: значок силы цветом уникальной редкости. У силы-события затемнение - оставшаяся перезарядка
 * ([PowerView.cooling]), срабатывание - вспышка плитки ([PowerView.flash], отклик боя - всегда), в углу - [powerBadge].
 */
@Composable private fun PowerTile(view: PowerView, bearing: Bearing, side: Dp, tip: () -> Tip) {
    val title = statTitle(view.stat)
    Tipped(tip, Modifier.shelfTile(UniqueTint, bearing, side).semantics { contentDescription = title }) {
        StatIcon(view.stat, UniqueTint, Modifier.fillMaxSize().padding(side / 6))
        Box(Modifier.fillMaxWidth().fillMaxHeight(view.cooling).background(Color.Black.copy(alpha = .55f)))
        if (view.flash > 0f) Box(Modifier.matchParentSize().background(GoldBright.copy(alpha = FLASH_ALPHA * view.flash)))
        powerBadge(view)?.let { badge ->
            Text(badge, color = Parchment, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp))
        }
    }
}

/** Яркость вспышки сработавшей силы в её начале. */
private const val FLASH_ALPHA = .5f

/** Имя эффекта: бафф правил - по виду, бафф дара - даром, глоток - флаконом, тотем и ярость - своим именем, прочее - умением. */
internal fun effectTitle(view: EffectView): String = when {
    view.buff != null -> ui("fight.buff.${view.buff!!.name}")
    view.source == INTIMIDATED -> ui("fight.intimidated")
    fateSourced(view.source) -> ui("fight.fate_buff")
    view.flask != null -> equipmentTitle(view.source)
    else -> view.origin?.let(::originTitle) ?: SkillText.title(view.source)
}

/** Имя эффекта без своего умения: тотема - из словаря тотемов, ярости - словом боя, свиты на боссе - именем монстра. */
internal fun originTitle(origin: EffectOrigin): String = when (origin) {
    is EffectOrigin.Totem -> totemTitle(origin.code)
    EffectOrigin.Rage -> ui("fight.rage_buff")
    is EffectOrigin.Ward -> monsterTitle(origin.monster)
}

/** Цвет эффекта: вред - кровью, глоток - цветом флакона, прочее - золотом. */
internal fun effectTint(view: EffectView): Color = when {
    view.kind.harmful -> LifeRed
    view.flask != null -> flaskTint(view.flask!!)
    else -> Gold
}

private fun modTint(mod: HeroMapMod): Color = Bearing.of(mod.harm).tint

/** Имя плитки строки на герое: строка карты - своим кодом, доля гильдии и зелья - своим источником. */
private fun modTitle(mod: HeroMapMod): String = when (mod.source.kind) {
    SourceKind.GUILD -> guildLineTitle(mod.source.ref)
    SourceKind.POTION -> itemTitle(mod.source.ref)
    else -> statTitle(mod.source.ref)
}

/** Откуда плитка строки на герое: карта, древо гильдии или зелье. */
private fun modSource(mod: HeroMapMod): String = when (mod.source.kind) {
    SourceKind.GUILD -> "fight.src.guild"
    SourceKind.POTION -> "fight.src.potion"
    else -> "fight.src.map"
}

/** Строка карты на герое: пунктирная плитка - значок первого сдвинутого стата цветом вреда или пользы ([bearing]). */
@Composable private fun MapModTile(mod: HeroMapMod, bearing: Bearing, side: Dp, tip: () -> Tip) {
    val tint = bearing.tint
    val shape = RoundedCornerShape(4.dp)
    val title = modTitle(mod)
    Tipped(
        tip,
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .1f)).dashedBorder(tint, shape).bearingCorner(bearing, side)
            .semantics { contentDescription = title },
    ) {
        mod.shifts.firstOrNull()?.let { (stat, _) -> StatIcon(stat, tint, Modifier.fillMaxSize().padding(side / 6)) }
    }
}

/** Плитка переполнения «+N»: сколько ещё эффектов не влезло; касание - лист эффектов [onOpen]. */
@Composable private fun MoreTile(count: Int, side: Dp, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(Muted.copy(alpha = .08f)).border(1.dp, Muted, shape).clickable(onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Text(ui("fight.shelf.more", count), color = Parchment, fontSize = (side.value * .36f).coerceIn(7f, 11f).sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * Полка эффектов: плитки [items] стороной [side] в ряд или столбец ([vertical]), сколько влезает; не влезшие - плиткой
 * «+N» последней. Касание плитки - её подробная выноска ([tip]), касание «+N» - лист эффектов ([onOpen]).
 * Подкомпоновка по месту - число «+N» зависит от размера полки; интринсиков у полки нет, её размер задаёт родитель.
 */
@Composable internal fun EffectShelf(items: List<Shelved>, side: Dp, onOpen: () -> Unit, modifier: Modifier = Modifier, vertical: Boolean = false, gap: Dp = 4.dp) {
    SubcomposeLayout(modifier) { constraints ->
        val sidePx = side.roundToPx()
        val gapPx = gap.roundToPx()
        val room = if (vertical) constraints.maxHeight else constraints.maxWidth
        val fit = if (room == Constraints.Infinity) items.size else ((room + gapPx) / (sidePx + gapPx)).coerceAtLeast(1)
        val over = items.size > fit
        val shown = if (over) fit - 1 else items.size
        val tile = Constraints.fixed(sidePx, sidePx)
        val tiles = (0 until shown).map { i -> subcompose(i) { ShelfTile(items[i], side) }.first().measure(tile) } +
            (if (over) listOf(subcompose(MORE) { MoreTile(items.size - shown, side, onOpen) }.first().measure(tile)) else emptyList())
        val length = if (tiles.isEmpty()) 0 else tiles.size * sidePx + (tiles.size - 1) * gapPx
        val width = (if (vertical) sidePx else length).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (if (vertical) length else sidePx).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            tiles.forEachIndexed { i, placeable -> if (vertical) placeable.place(0, i * (sidePx + gapPx)) else placeable.place(i * (sidePx + gapPx), 0) }
        }
    }
}

private const val MORE = "more"

/** Цвет итога здоровья по его тону ([FlowTone]): убыль - красным, прибыль - зелёным, покой - приглушённо. */
internal fun flowTint(net: Double): Color = when (FlowTone.of(net)) {
    FlowTone.LOSS -> Color(0xFFE5635C)
    FlowTone.GAIN -> Vital
    FlowTone.STILL -> Muted
}

/**
 * Разбор здоровья в секунду на карте - касанием метки на полосе жизни: регенерация, отдых, деген, недуги и что принял
 * щит строками с итогом; под ранящим недугом - чем он глушит восстановление. Читает живой снимок: числа идут.
 */
@Composable internal fun LifeFlowSheet(flow: () -> LifeFlow?, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        val now = flow() ?: return@ForgeSheet
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowSummary(now)
            FlowPart(ui("fight.flow.part.regen"), now.regen - now.rest)
            FlowPart(ui("fight.flow.part.rest"), now.rest, now.restIn.takeIf { it > 0 }?.let { ui("fight.flow.rest_in", ceil(it).toInt()) })
            if (now.degen > 0) FlowPart(ui("fight.flow.part.degen"), -now.degen)
            if (now.ailments >= .5) FlowPart(ui("fight.flow.part.ailments"), -now.ailments)
            if (now.shielded >= .5) FlowPart(ui("fight.flow.part.shielded"), now.shielded)
            now.stalledBy?.let { ailment ->
                MutedText(
                    ui("fight.flow.stalled_note", ui(ailment.key()), flowRate(now.stalled)),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Строка разбора: имя части и её вклад в секунду со знаком; [waiting] - часть ещё не началась, вместо числа - когда начнётся. */
@Composable private fun FlowPart(title: String, value: Double, waiting: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (waiting != null) {
            Text(waiting, color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
            Text(flowRate(value), color = flowTint(value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Плитка итога здоровья: «−12/с» красным, «+8/с» зелёным, сверху рельса героя; [stacked] - «/с» строкой ниже. */
@Composable internal fun FlowTile(flow: LifeFlow, modifier: Modifier, stacked: Boolean = true) {
    val tint = flowTint(flow.net)
    val shape = RoundedCornerShape(4.dp)
    val rate = flowRate(flow.net)
    Box(
        modifier.clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .14f)).border(1.dp, tint, shape)
            .semantics { contentDescription = ui("fight.flow.title") + " " + rate },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (stacked) flowFigure(flow.net) + "\n" + Rate.unit else rate,
            color = tint,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/**
 * Лист эффектов: всё, что лежит на бойце, - временные и на весь бой; у героя сверху - итог здоровья в секунду и его
 * разбор (один раз - своей строкой в группе он не повторяется). Эффекты героя на весь бой - свой лист [LastingSheet].
 * Читается вживую: таймеры идут, бой не стоит.
 */
@Composable internal fun EffectsSheet(title: String, timed: List<Shelved>, lasting: List<Shelved>, flow: LifeFlow?, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium)
            flow?.let { FlowSummary(it) }
            if (timed.isEmpty() && lasting.isEmpty() && flow == null) MutedText(ui("fight.no_states"))
            if (timed.isNotEmpty()) {
                ShelfGroup(ui("fight.shelf.timed", timed.size))
                timed.forEach { SheetRow(it) }
            }
            if (lasting.isNotEmpty()) {
                ShelfGroup(ui("fight.shelf.lasting", lasting.size))
                lasting.forEach { SheetRow(it) }
            }
        }
    }
}

@Composable private fun ShelfGroup(text: String) = Text(
    text.uppercase(),
    color = Muted,
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
)

/** Сводка сверху листа героя: итог здоровья в секунду крупно и из чего он. */
@Composable private fun FlowSummary(flow: LifeFlow) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).background(PanelRaised, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(ui("fight.flow.title"), color = Parchment, style = MaterialTheme.typography.bodySmall)
            MutedText(flowParts(flow).ifEmpty { ui("fight.flow.quiet") }, style = MaterialTheme.typography.labelSmall)
        }
        Text(flowRate(flow.net), color = flowTint(flow.net), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * Лист эффектов героя «на весь бой» (кнопка «≡ N» у портрета): «На весь бой · N», чипы выбора источника со счётом (больше одной
 * группы) и группы [LastingGroup] строками с полными текстами - у силы уникалки источник - имя вещи. Читается вживую, бой не стоит.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LastingSheet(items: List<Shelved>, onDismiss: () -> Unit) {
    val groups = remember(items) { items.groupBy { it.lastingGroup }.toSortedMap() }
    var only by remember { mutableStateOf<LastingGroup?>(null) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(ui("fight.shelf.lasting", items.size), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            if (groups.size > 1) {
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForgeChip(ui("fight.lasting.all"), only == null, { only = null }, mode = ChipMode.Choice, count = items.size)
                    groups.forEach { (group, list) -> ForgeChip(ui("fight.lasting.group.${group.name}"), only == group, { only = group }, mode = ChipMode.Choice, count = list.size) }
                }
            }
            if (items.isEmpty()) MutedText(ui("fight.no_states"))
            groups.filterKeys { only == null || it == only }.forEach { (group, list) ->
                ShelfGroup(ui("fight.lasting.group.${group.name}"))
                list.forEach { SheetRow(it) }
            }
        }
    }
}

@Composable private fun SheetRow(item: Shelved) {
    val row = item.row(LocalContentIndex.current)
    EffectRow({ ShelfTile(item, 30.dp) }, row, row.state ?: row.seconds?.let { ui("fight.fact_seconds", fineNumber(it)) } ?: ui("fight.shelf.whole"))
}

@Composable private fun EffectRow(tile: @Composable () -> Unit, row: ShelfRow, time: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        tile()
        Column(Modifier.weight(1f)) {
            Text(row.title, color = row.tint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            MutedText(row.source, style = MaterialTheme.typography.labelSmall)
            row.lines.filter { it.isNotBlank() }.forEach { Text(it, color = Parchment.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall) }
        }
        Text(time, color = if (row.seconds != null) Parchment else Muted, style = MaterialTheme.typography.labelMedium, fontWeight = if (row.seconds != null) FontWeight.Bold else FontWeight.Normal)
    }
    HorizontalDivider(color = Bronze.copy(alpha = .5f))
}

/**
 * Рамка плитки эффекта: тёмная подложка, заливка цветом эффекта [tint], рамка 1dp и уголок знака [bearing] - кровью у вреда,
 * золотом у пользы.
 */
internal fun Modifier.shelfTile(tint: Color, bearing: Bearing, side: Dp, shape: Shape = RoundedCornerShape(4.dp)): Modifier = size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, bearing.tint, shape).bearingCorner(bearing, side)

/** Уголок знака плитки стороной [side]: треугольник цвета [bearing] в левом верхнем углу, поверх затемнения срока. */
internal fun Modifier.bearingCorner(bearing: Bearing, side: Dp): Modifier = drawWithCache {
    val leg = side.toPx() * CORNER_SHARE
    val corner = Path().apply {
        moveTo(0f, 0f)
        lineTo(leg, 0f)
        lineTo(0f, leg)
        close()
    }
    onDrawWithContent {
        drawContent()
        drawPath(corner, bearing.tint)
    }
}

/** Катет уголка знака долей стороны плитки. */
private const val CORNER_SHARE = .32f

/** Пунктирная рамка 1dp: строка карты - не эффект боя, а условие захода. */
private fun Modifier.dashedBorder(tint: Color, shape: Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
    onDrawWithContent {
        drawContent()
        drawOutline(outline, tint, style = stroke)
    }
}
