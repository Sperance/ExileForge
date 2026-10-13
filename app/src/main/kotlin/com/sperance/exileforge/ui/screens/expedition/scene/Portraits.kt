package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sperance.exileforge.core.display.Portrait
import com.sperance.exileforge.core.display.PortraitBundle
import com.sperance.exileforge.core.display.PortraitKey
import com.sperance.exileforge.core.display.classPortrait
import com.sperance.exileforge.core.display.serverPortraits
import com.sperance.exileforge.ui.icons.drawPortrait
import kotlin.math.sin

/**
 * The busts in the arena's frames (2.28.0): the hero and every monster form, head and shoulders, in whatever box the frame
 * gives - всегда портрет сервера. Пока портреты не пришли, жетон пуст: своих фигур клиент не рисует.
 *
 * A [wash] colours the whole portrait (an ailment on the fighter), [flash] whitens it (a blow just landed) and [time] lets it
 * breathe.
 */
object Portraits {

    /** The hero's bust: the server's portrait of the class, breathing a little; без портрета - пустой жетон. */
    fun hero(scope: DrawScope, classCode: String?, time: Float, wash: Color? = null, washAmount: Float = 0f, flash: Float = 0f) = with(scope) {
        val portrait = PortraitLookup.hero(classCode)
        if (portrait != null) drawPortrait(portrait, sin(time * 1.5f) * size.height * .006f) else drawRect(Blank)
        finish(scope, wash, washAmount, flash)
    }

    /**
     * A monster's bust: its own portrait, then its form's; без портрета - пустой жетон.
     * [phase] - стадия боя для живого портрета (у Матери Скверны - порванные пуповины, см. [BlightPortraits.mother]).
     */
    fun monster(scope: DrawScope, code: String, form: String, time: Float, wash: Color? = null, washAmount: Float = 0f, flash: Float = 0f, phase: Int = 0) = with(scope) {
        val portrait = PortraitLookup.monster(code, form)
        if (portrait != null) {
            val lift = sin(time * 1.2f + 1f) * size.height * .006f
            // живые портреты (Разлом, Скверна): дыхание фигуры и слои анимации поверх портрета
            LivingPortraits.breathing(scope, code, time) {
                drawPortrait(portrait, lift)
                LivingPortraits.draw(this, code, time, lift, phase)
            }
        } else {
            drawRect(Blank)
        }
        finish(scope, wash, washAmount, flash)
    }

    private fun finish(scope: DrawScope, wash: Color?, washAmount: Float, flash: Float) = with(scope) {
        if (wash != null && washAmount > 0) drawRect(wash.copy(alpha = washAmount.coerceIn(0f, .7f)))
        if (flash > 0) drawRect(Color.White.copy(alpha = flash.coerceIn(0f, .85f)))
        // Vignette: the frame's dark closes in on the bust. Кисть одна - круг единичного радиуса, растянутый переносом
        val radius = size.maxDimension * .75f
        withTransform({
            translate(size.width / 2, size.height / 2)
            scale(radius, radius, Offset.Zero)
        }) { drawRect(Vignette, Offset(-size.width / 2 / radius, -size.height / 2 / radius), Size(size.width / radius, size.height / radius)) }
    }

    /** Пустой жетон: фон рамки, пока портретов нет. */
    private val Blank = Color(0xFF0B0E13)

    private val Vignette = Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xCC000000)), Offset.Zero, 1f)
}

/**
 * Портреты сервера по коду без сборки ключа на каждый кадр: ответ запоминается до смены набора портретов ([serverPortraits]).
 * Только главный поток.
 */
internal object PortraitLookup {
    private var bundle: PortraitBundle? = null
    private val classes = HashMap<String, Any>()
    private val monsters = HashMap<String, Any>()
    private val forms = HashMap<String, Any>()
    private val none = Any()

    fun hero(code: String?): Portrait? {
        if (code == null) return null
        fresh()
        return classes.getOrPut(code) { classPortrait(code) ?: none } as? Portrait
    }

    fun monster(code: String, form: String): Portrait? {
        fresh()
        (monsters.getOrPut(code) { serverPortraits[PortraitKey.monster(code)] ?: none } as? Portrait)?.let { return it }
        return forms.getOrPut(form) { serverPortraits[PortraitKey.form(form)] ?: none } as? Portrait
    }

    private fun fresh() {
        val now = serverPortraits
        if (now === bundle) return
        bundle = now
        classes.clear()
        monsters.clear()
        forms.clear()
    }
}
