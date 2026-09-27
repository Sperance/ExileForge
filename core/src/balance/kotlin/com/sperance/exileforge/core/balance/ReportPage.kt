package com.sperance.exileforge.core.balance

import java.util.Locale

/**
 * The report as one self-contained page: the flags first, then build power by level, progress, loot,
 * and the worth of nodes and skills. Charts are inline SVG; nothing loads from anywhere.
 */
object ReportPage {
    fun render(data: BalanceData): String = buildString {
        append(HEAD)
        append("<h1>Баланс ExileForge</h1><p class=\"lead\">Прогон «${data.config}»: ${data.runs} автопробег(а) зоны своего уровня на сборку и уровень, ")
        append("арена на одних и тех же стаях. Сборки: 7 классов × ${Archetype.entries.size} архетипов (${Archetype.entries.joinToString { it.title }}), редкая экипировка лучших баз уровня. ")
        append("Считалось ${fmt(data.seconds, 0)} с. Отчёт ничего не меняет.</p>")
        flags(data)
        builds(data)
        progress(data)
        loot(data)
        nodes(data)
        skills(data)
        append("</main></body></html>")
    }

    private fun StringBuilder.flags(data: BalanceData) {
        val flagged = data.builds.filter { it.flag.isNotEmpty() && it.flag != "гибнет" }
        append("<h2>Выбросы</h2>")
        data.builds.groupBy { it.level }.forEach { (level, rows) ->
            val beaten = rows.count { it.bossWinRate > 0 }
            val dying = rows.count { it.flag == "гибнет" }
            if (dying > 0) append("<p class=\"warn\">Уровень $level: ${dying} из ${rows.size} сборок гибнут в ≥50% автопробегов зоны своего уровня (в среднем ${pct(rows.map { it.ownDeathRate }.average())}); фармят в среднем зону ур. ${fmt(rows.map { it.farmZoneLevel.toDouble() }.average(), 0)}.</p>")
            if (beaten < rows.size) append("<p class=\"warn\">Уровень $level: стража зоны ${rows.first().zone} побеждают ${beaten} из ${rows.size} сборок (в среднем ${pct(rows.map { it.bossWinRate }.average())} боёв).</p>")
        }
        if (flagged.isEmpty()) { append("<p>Выбросов нет: все сборки в пределах полутора медиан своего уровня.</p>"); return }
        append("<p>Сборка «слабая» — на арене в 1,5 раза медленнее медианы уровня, «сильная» — в 1,5 раза быстрее (гибнущие в своей зоне здесь не повторяются).</p><div class=\"chips\">")
        flagged.sortedWith(compareBy({ it.flag }, { it.level })).forEach {
            append("<span class=\"chip ${cls(it.flag)}\">${it.heroClass} · ${title(it.archetype)} · ур. ${it.level} — ${it.flag}</span>")
        }
        append("</div>")
    }

    private fun StringBuilder.builds(data: BalanceData) {
        append("<h2>Сила сборок</h2><p>Арена: секунд на стаю зоны (поражение = +${Bench.LOSS_SECONDS.toInt()} с), по архетипам; ячейка краснее — медленнее. Справа — средние по классу: доля погибших автопробегов в зоне своего уровня; зона, где сборка фармит (гибель ≤25%, шаг вниз — 4 уровня), и её темп, опыт и золото; бой со стражем своей зоны один на один (до 5 минут).</p>")
        data.levels.forEach { level ->
            val rows = data.builds.filter { it.level == level }
            val max = rows.maxOf { cost(it) }.coerceAtLeast(1.0)
            append("<h3>Уровень $level · зона ${rows.first().zone} (${rows.first().zoneLevel})</h3><div class=\"scroll\"><table><tr><th>Класс</th>")
            Archetype.entries.forEach { append("<th>${it.title}</th>") }
            append("<th>Здоровье</th><th>Сопр.</th><th>Гибель в своей</th><th>Фарм-зона</th><th>Мин/зона</th><th>Опыт/ч</th><th>Золото/ч</th><th>Страж: победы</th><th>Страж: с</th></tr>")
            rows.groupBy { it.heroClass }.forEach { (heroClass, own) ->
                append("<tr><td>$heroClass</td>")
                Archetype.entries.forEach { arch ->
                    val b = own.first { it.archetype == arch.name }
                    val share = cost(b) / max
                    append("<td class=\"heat\" style=\"--h:${fmt(share, 2)}\" title=\"потери ${pct(b.arenaLossRate)}\">${fmt(b.arenaSecondsPerPack, 1)}${if (b.arenaLossRate > 0) " <small>✕${pct(b.arenaLossRate)}</small>" else ""}</td>")
                }
                val avg = { f: (BuildResult) -> Double -> own.map(f).average() }
                append("<td>${fmt(avg { it.life }, 0)}</td><td>${fmt(avg { it.resistance }, 0)}%</td><td class=\"${if (avg { it.ownDeathRate } >= 0.5) "bad" else ""}\">${pct(avg { it.ownDeathRate })}</td>")
                append("<td>ур. ${fmt(avg { it.farmZoneLevel.toDouble() }, 0)}</td><td>${fmt(avg { it.clearMinutes }, 1)}</td><td>${big(avg { it.experiencePerHour })}</td><td>${big(avg { it.goldPerHour })}</td>")
                append("<td class=\"${if (avg { it.bossWinRate } < 0.5) "bad" else ""}\">${pct(avg { it.bossWinRate })}</td><td>${fmt(avg { it.bossSeconds }, 0)}</td></tr>")
            }
            append("</table></div>")
        }
    }

    private fun StringBuilder.progress(data: BalanceData) {
        append("<h2>Скорость прогресса</h2><p>Часов автопробегов в фарм-зонах до уровня (опыт в час между измеренными уровнями — по прямой; гибель стоит забега; штраф опыта за гибель не учтён). После 70 зоны кончаются.</p>")
        val milestones = data.progress.flatMap { it.hoursTo.keys }.distinct().sorted()
        val series = data.progress.groupBy { it.heroClass }.map { (heroClass, rows) -> heroClass to milestones.map { m -> rows.mapNotNull { it.hoursTo[m] }.average() } }
        append(lineChart(milestones, series))
        append("<div class=\"scroll\"><table><tr><th>Класс</th><th>Архетип</th>")
        milestones.forEach { append("<th>до $it</th>") }
        append("</tr>")
        data.progress.forEach { p ->
            append("<tr><td>${p.heroClass}</td><td>${title(p.archetype)}</td>")
            milestones.forEach { append("<td>${p.hoursTo[it]?.let { h -> fmt(h, 1) } ?: "—"}</td>") }
            append("</tr>")
        }
        append("</table></div>")
    }

    private fun StringBuilder.loot(data: BalanceData) {
        append("<h2>Добыча в час</h2><p>Среднее по всем сборкам уровня, в их фарм-зонах. «Ценность» — стопки по цене торговца.</p><div class=\"scroll\"><table><tr><th>Уровень</th><th>Золото/ч</th><th>Ценность стопок/ч</th><th>Редкие/ч</th><th>Уникальные/ч</th><th>Самое частое</th></tr>")
        data.loot.forEach { l ->
            val top = l.itemsPerHour.entries.sortedByDescending { it.value }.take(6).joinToString(", ") { "${it.key} ${fmt(it.value, 1)}" }
            append("<tr><td>${l.level}</td><td>${big(l.goldPerHour)}</td><td>${big(l.itemValuePerHour)}</td><td>${fmt(l.rarePerHour, 1)}</td><td>${fmt(l.uniquePerHour, 2)}</td><td class=\"wide\">$top</td></tr>")
        }
        append("</table></div><h3>Сферы в час по уровням</h3>")
        val orbs = data.loot.flatMap { it.itemsPerHour.keys }.filter { it.contains("ORB") || it.contains("MIRROR") || it.contains("SCROLL") }.distinct().sorted()
        append("<div class=\"scroll\"><table><tr><th>Сфера</th>")
        data.loot.forEach { append("<th>${it.level}</th>") }
        append("</tr>")
        orbs.forEach { orb ->
            append("<tr><td>$orb</td>")
            data.loot.forEach { append("<td>${it.itemsPerHour[orb]?.let { v -> fmt(v, 2) } ?: "—"}</td>") }
            append("</tr>")
        }
        append("</table></div>")
    }

    private fun StringBuilder.nodes(data: BalanceData) {
        append("<h2>Полезность узлов</h2><p>Каждый взятый значимый узел, ключевой и мастерство сборок уровня ${data.levels.firstOrNull { it >= 70 } ?: 70} снимается по одному: на сколько процентов герой слабее без него: сила против стражей зоны (сколько стражей он переживёт — время жизни против бессмертного стража к времени убийства безобидного), у «Фармера» — ценность добычи по листу. Отрицательное — без узла сильнее.</p>")
        append("<div class=\"cols\"><div><h3>Самые ценные</h3>").append(nodeTable(data.nodes.take(15))).append("</div>")
        append("<div><h3>Пустые и вредные</h3>").append(nodeTable(data.nodes.filter { it.loss <= 0.5 }.sortedBy { it.loss }.take(20))).append("</div></div>")
        append("<p>Ни одна сборка не взяла: ${data.untakenNodes.entries.joinToString { "${it.key} — ${it.value}" }.ifEmpty { "таких нет" }}.</p>")
    }

    private fun nodeTable(rows: List<NodeResult>) = buildString {
        append("<table><tr><th>Узел</th><th>Тип</th><th>Берут</th><th>Потеря</th></tr>")
        rows.forEach { append("<tr><td>${it.code}</td><td>${it.type}</td><td>${it.takenBy}</td><td>${fmt(it.loss, 1)}%</td></tr>") }
        append("</table>")
    }

    private fun StringBuilder.skills(data: BalanceData) {
        append("<h2>Полезность умений</h2><p>Каждое умение класса — одно в лучшей сборке класса; отношение времени арены к бою без умений (меньше 1 — умение помогает).</p><div class=\"cols\">")
        data.skills.groupBy { it.heroClass }.forEach { (heroClass, rows) ->
            append("<div><h3>$heroClass</h3><table><tr><th>Умение</th><th>Тип</th><th>×</th></tr>")
            rows.forEach { append("<tr><td>${it.code}</td><td>${it.type}</td><td class=\"${if (it.relative >= 0.99) "bad" else ""}\">${fmt(it.relative, 2)}</td></tr>") }
            append("</table></div>")
        }
        append("</div>")
    }

    private fun lineChart(xs: List<Int>, series: List<Pair<String, List<Double>>>): String {
        if (xs.isEmpty()) return ""
        val w = 720.0; val h = 260.0; val pad = 44.0
        val max = series.flatMap { it.second }.filter { it.isFinite() }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        fun x(i: Int) = pad + (w - pad * 2) * i / (xs.size - 1).coerceAtLeast(1)
        fun y(v: Double) = h - pad - (h - pad * 2) * v / max
        return buildString {
            append("<div class=\"scroll\"><svg viewBox=\"0 0 $w $h\" class=\"chart\" role=\"img\" aria-label=\"Часы до уровня по классам\">")
            (0..4).forEach { k -> val v = max * k / 4; append("<line x1=\"$pad\" x2=\"${w - pad}\" y1=\"${y(v)}\" y2=\"${y(v)}\" class=\"grid\"/><text x=\"${pad - 6}\" y=\"${y(v) + 4}\" class=\"axis\" text-anchor=\"end\">${fmt(v, 0)}</text>") }
            xs.forEachIndexed { i, lv -> append("<text x=\"${x(i)}\" y=\"${h - pad + 18}\" class=\"axis\" text-anchor=\"middle\">$lv</text>") }
            series.forEachIndexed { s, (name, values) ->
                val points = values.mapIndexed { i, v -> "${x(i)},${y(v)}" }.joinToString(" ")
                append("<polyline points=\"$points\" class=\"line s$s\"/><text x=\"${w - pad + 4}\" y=\"${y(values.last())}\" class=\"label s$s\">$name</text>")
            }
            append("</svg></div>")
        }
    }

    private fun cost(b: BuildResult) = b.arenaSecondsPerPack + b.arenaLossRate * Bench.LOSS_SECONDS
    private fun title(archetype: String) = Archetype.entries.firstOrNull { it.name == archetype }?.title ?: archetype
    private fun cls(flag: String) = when (flag) { "сильный" -> "strong"; "слабый" -> "weak"; else -> "dead" }
    private fun fmt(v: Double, digits: Int) = if (v.isFinite()) String.format(Locale.US, "%.${digits}f", v) else "—"
    private fun pct(v: Double) = "${fmt(v * 100, 0)}%"
    private fun big(v: Double) = when { !v.isFinite() -> "—"; v >= 1e6 -> "${fmt(v / 1e6, 1)}M"; v >= 1e3 -> "${fmt(v / 1e3, 1)}k"; else -> fmt(v, 0) }

    private val HEAD = """
        <!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Баланс ExileForge</title><style>
        :root{color-scheme:dark;--bg:#111315;--panel:#1a1d20;--line:#2c3136;--ink:#e6e1d6;--mute:#9a958a;--accent:#3fb67a;--warm:#d9a53a;--bad:#e05a4e}
        body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,sans-serif}
        main{max-width:1100px;margin:0 auto;padding:24px 16px 64px}
        h1{font-size:28px;margin:0 0 6px}h2{font-size:21px;margin:36px 0 6px;color:var(--warm)}h3{font-size:15px;margin:18px 0 6px}
        p{color:var(--mute);max-width:75ch;margin:0 0 10px}.lead{font-size:16px}
        .scroll{overflow-x:auto}table{border-collapse:collapse;font-variant-numeric:tabular-nums;margin-bottom:8px}
        th,td{padding:4px 10px;border-bottom:1px solid var(--line);text-align:right;white-space:nowrap}th:first-child,td:first-child{text-align:left}
        th{color:var(--mute);font-weight:500}td.wide{white-space:normal;text-align:left;min-width:320px}
        td.heat{background:color-mix(in srgb,var(--bad) calc(var(--h)*70%),transparent)}td small{color:var(--mute)}
        td.bad{color:var(--bad)}.warn{color:var(--bad)}.chips{display:flex;flex-wrap:wrap;gap:6px}.chip{border-radius:99px;padding:3px 10px;font-size:13px;border:1px solid var(--line)}
        .chip.strong{border-color:var(--accent);color:var(--accent)}.chip.weak{border-color:var(--warm);color:var(--warm)}.chip.dead{border-color:var(--bad);color:var(--bad)}
        .cols{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:16px}
        .chart{width:100%;max-width:820px;min-width:560px}.grid{stroke:var(--line)}.axis{fill:var(--mute);font-size:11px}
        .line{fill:none;stroke-width:2}.label{font-size:11px}
        .s0{stroke:#e0405a;fill:#e0405a}.s1{stroke:#7fcf4a;fill:#7fcf4a}.s2{stroke:#4a7ae8;fill:#4a7ae8}.s3{stroke:#ef8a3a;fill:#ef8a3a}
        .s4{stroke:#e8c060;fill:#e8c060}.s5{stroke:#38b8b0;fill:#38b8b0}.s6{stroke:#c8ccd4;fill:#c8ccd4}.line.s0,.line.s1,.line.s2,.line.s3,.line.s4,.line.s5,.line.s6{fill:none}
        </style></head><body><main>
    """.trimIndent()
}
