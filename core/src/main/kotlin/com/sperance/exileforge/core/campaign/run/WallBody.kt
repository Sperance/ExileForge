package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Shoreline
import com.sperance.exileforge.core.campaign.WallField
import kotlin.math.floor
import kotlin.math.hypot

/** Куда встало тело после шага ([WallBody.slide]): одно на мир, шаг пишет в него - без пары на каждый такт. */
internal class Footing {
    var x = 0.0
        private set
    var y = 0.0
        private set

    fun at(x: Double, y: Double): Footing {
        this.x = x
        this.y = y
        return this
    }
}

/**
 * Тела в скале карты: герой и монстр - круг, скала - нарисованное подножие массива [walls] (то же поле, что режет сцена, с шумом
 * края), пропасть и вода - их нарисованный контур [shoreline] (тот же, что заливает сцена): тело подходит к самой кромке, а не к
 * краю клетки, и не цепляется за невидимый угол. Центр тела всегда стоит на проходимой клетке - сетка путей, света и стай не
 * меняется.
 */
internal class WallBody(private val map: ExpeditionMap, private val walls: () -> WallField, private val shoreline: () -> Shoreline) {
    /** Помещается ли круг радиуса [radius] с центром в ([x], [y]). */
    fun free(x: Double, y: Double, radius: Double): Boolean {
        if (!map.walkable(floor(x).toInt(), floor(y).toInt())) return false
        if (shoreline().inside(x, y) || walls().inside(x, y)) return false
        return nearest(x, y, radius)?.let { it.distance >= radius } ?: true
    }

    /**
     * Шаг ([dx], [dy]) из ([x], [y]): целиком, если свободно; иначе - вдоль кромки (составляющая шага по касательной), иначе - по
     * осям порознь. Так круг обтекает выступ скалы, а не упирается в него.
     */
    fun slide(x: Double, y: Double, dx: Double, dy: Double, radius: Double, into: Footing): Footing {
        if (free(x + dx, y + dy, radius)) return into.at(x + dx, y + dy)
        nearest(x + dx, y + dy, radius)?.let { hit ->
            val along = dx * hit.nx + dy * hit.ny
            val tx = dx - along * hit.nx
            val ty = dy - along * hit.ny
            if (hypot(tx, ty) > EPSILON && free(x + tx, y + ty, radius)) return into.at(x + tx, y + ty)
        }
        val nx = if (free(x + dx, y, radius)) x + dx else x
        val ny = if (free(nx, y + dy, radius)) y + dy else y
        return into.at(nx, ny)
    }

    /** Ближайшая к точке кромка в пределах [reach]: подножие скалы или контур глади - что ближе. */
    private fun nearest(x: Double, y: Double, reach: Double): Shoreline.Edge? = listOfNotNull(shoreline().nearest(x, y, reach), walls().nearest(x, y, reach)).minByOrNull { it.distance }

    private companion object {
        const val EPSILON = 1e-9
    }
}
