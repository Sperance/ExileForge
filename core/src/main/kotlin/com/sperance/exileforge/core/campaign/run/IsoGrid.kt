package com.sperance.exileforge.core.campaign.run

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Изометрия карты похода - одна на клиент: x клеток бежит по экрану вниз-вправо, y - вниз-влево, клетка вдвое шире,
 * чем выше. Сдвиги экрана - в полуширинах клетки по обеим осям. Её читают камера сцены, мини-карта и полная карта (`SceneCamera`,
 * `MapProjection` приложения) и стик ([pace]) - своей копии матрицы ни у кого нет.
 */
object IsoGrid {
    /**
     * Во сколько раз шаг вверх-вниз по экрану медленнее шага вбок при той же силе стика: честная изометрия сжимает
     * вертикаль вдвое, это казалось ходьбой вполсилы; частичная компенсация - 0.75, будто камера круче. Скорость шага в
     * клетках по вертикали - в 1.5 раза выше, чем вбок на экране; монстры ходят в клетках.
     */
    const val VERTICAL_PACE = .75

    /** Сдвиг точки карты ([x], [y]) по экрану вправо, в полуширинах клетки. */
    fun across(x: Double, y: Double): Double = x - y

    /** Сдвиг точки карты ([x], [y]) по экрану вниз, в полуширинах клетки. */
    fun down(x: Double, y: Double): Double = (x + y) / 2

    /** Сдвиг экрана [across] вправо и [down] вниз (в полуширинах клетки) - сдвиг по карте; обратная к [across] и [down]. */
    fun inverse(across: Double, down: Double): Pair<Double, Double> = (across + 2 * down) / 2 to (2 * down - across) / 2

    /**
     * Стик ([screenX] вправо, [screenY] вниз, длина до 1) - шаг по карте в долях скорости героя: направление на экране то же, что
     * у стика, а экранная скорость - на эллипсе с полуосями 1 вбок и [VERTICAL_PACE] вверх-вниз, вбок она как у честной
     * изометрии (шаг в одну скорость героя). Длина шага бывает больше 1 - по вертикали до 1.5 скорости героя в клетках.
     *
     * Радиус эллипса скоростей - в направлении стика; вбок шаг в одну скорость.
     */
    fun pace(screenX: Double, screenY: Double): Pair<Double, Double> {
        val length = hypot(screenX, screenY)
        if (length < 1e-9) return 0.0 to 0.0
        val push = length.coerceAtMost(1.0)
        val (dx, dy) = screenX / length to screenY / length
        val radius = 1 / sqrt(dx * dx + dy * dy / (VERTICAL_PACE * VERTICAL_PACE))
        val screen = SIDEWAYS * radius * push
        return inverse(dx * screen, dy * screen)
    }

    /** Экранная скорость шага вбок в одну скорость героя, в полуширинах клетки: шаг (1, −1)/√2 по карте. */
    private val SIDEWAYS = sqrt(2.0)
}
