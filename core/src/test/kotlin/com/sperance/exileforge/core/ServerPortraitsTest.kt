package com.sperance.exileforge.core

import com.sperance.exileforge.core.display.*
import kotlin.test.*
import org.junit.After
import org.junit.Test

/** Portraits as the server serves them since 0.29.0: SVG in the subset the client draws itself. */
class ServerPortraitsTest {
    private val svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 400">
        <defs>
            <linearGradient id="bg" gradientUnits="userSpaceOnUse" x1="150" y1="0" x2="150" y2="400">
                <stop offset="0" stop-color="#161a22"/><stop offset="1" stop-color="#07090d" stop-opacity="0.5"/>
            </linearGradient>
            <radialGradient id="glow" gradientUnits="userSpaceOnUse" cx="150" cy="170" r="210"><stop offset="0" stop-color="#fff"/></radialGradient>
        </defs>
        <rect x="0" y="0" width="300" height="400" fill="url(#bg)"/>
        <g opacity="0.5"><circle cx="150" cy="165" r="40" fill="url(#glow)" fill-opacity="0.5"/></g>
        <ellipse cx="10" cy="20" rx="4" ry="2" fill="#ff0000"/>
        <path d="M0 0 L10 10" fill="none" stroke="#00ff00" stroke-width="3" stroke-linecap="round"/>
        <text>ignored</text>
    </svg>"""

    @After fun forget() { serverPortraits = PortraitBundle() }

    @Test fun `every element becomes path data with its paint`() {
        val portrait = PortraitSvg.parse(svg)
        assertEquals(300f, portrait.width)
        assertEquals(400f, portrait.height)
        assertEquals(4, portrait.shapes.size)
        val (rect, circle, ellipse, line) = portrait.shapes
        assertEquals("M0.0 0.0 h300.0 v400.0 h-300.0 Z", rect.d)
        val linear = assertIs<PortraitPaint.Linear>(rect.fill)
        assertEquals(listOf(0xFF161A22, 0x7F07090D), linear.stops.map { it.argb })
        assertIs<PortraitPaint.Radial>(circle.fill)
        assertEquals(.25f, circle.fillAlpha)
        assertTrue(circle.d.startsWith("M110.0 165.0 A40.0 40.0"))
        assertEquals(PortraitPaint.Solid(0xFFFF0000), ellipse.fill)
        assertNull(line.fill)
        assertEquals(PortraitPaint.Solid(0xFF00FF00), line.stroke)
        assertEquals(3f, line.strokeWidth)
        assertTrue(line.round)
    }

    @Test fun `a colour is hex, short or long, and anything else is no paint`() {
        assertEquals(0xFFFFFFFF, PortraitSvg.colour("#fff", 1f))
        assertEquals(0x80123456, PortraitSvg.colour("#123456", .5f + .002f))
        assertNull(PortraitSvg.colour("none", 1f))
        assertNull(PortraitSvg.colour("red", 1f))
    }

    @Test fun `a monster's own portrait wins over its form's, and none means the client's drawing`() {
        val bust = PortraitSvg.parse(svg)
        val own = bust.copy(shapes = emptyList())
        assertNull(monsterPortrait("FALLEN", "HUMANOID"))
        serverPortraits = PortraitBundle(mapOf("form.HUMANOID" to bust, "class.WITCH" to bust))
        assertSame(bust, monsterPortrait("FALLEN", "HUMANOID"))
        serverPortraits = PortraitBundle(mapOf("form.HUMANOID" to bust, "monster.FALLEN" to own))
        assertSame(own, monsterPortrait("FALLEN", "HUMANOID"))
        assertNull(classPortrait("WITCH"))
        assertNull(classPortrait(null))
        assertEquals("portraits/class/WITCH.svg", PortraitKey.path(PortraitKey.characterClass("WITCH")))
    }
}
