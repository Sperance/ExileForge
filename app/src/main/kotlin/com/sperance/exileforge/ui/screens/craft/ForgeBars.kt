package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.presentation.forge.Smithy
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Полосы кузницы над навигацией (3.80.24): выбранная сфера, эссенция или строка верстака с удерживаемой кнопкой. */
/** The chosen essence over the navigation, with the held button: a common item becomes rare, a rare one is rolled anew. */
@Composable internal fun EssenceBar(game: GameUi, code: String, instance: ItemInstance, enabled: Boolean, accepted: (String) -> Boolean, onApply: (String, String) -> Unit) {
    val owned = game.bagAmount(code) ?: 0L
    val essence = game.index?.essence(code)?.takeIf { owned > 0 && accepted(code) }
    ForgeBar {
        if (essence == null) {
            Text(ui("forge.pick_essence"), color = Muted)
            return@ForgeBar
        }
        BarTitle(ForgeGlyphs.Shard, Elder, itemTitle(code), stock(code, owned, 1))
        HoldButton(ui("confirm.hold", ui("forge.apply_essence")), Elder, enabled = enabled && !instance.corrupted, rearm = true) {
            onApply(instance.id, code)
        }
    }
}

/** The chosen orb over the navigation: what it does, what the bag keeps, and the button that is held. */
@Composable internal fun OrbBar(
    game: GameUi,
    smithy: Smithy,
    instance: ItemInstance,
    enabled: Boolean,
    accepted: (String) -> Boolean,
    needsOmen: (String) -> Boolean,
    lost: Int,
    onApply: (String, String) -> Unit,
) {
    val code = smithy.orb
    // Качество другого вида (4.2.1): сфера качества сбросит его - сначала подтверждение с тем, сколько пропадёт.
    var confirming by remember(instance.id, code, smithy.omen) { mutableStateOf(false) }
    val owned = game.bagAmount(code) ?: 0L
    val orb = game.orbs.firstOrNull { it.code.value == code && owned > 0 && accepted(code) }
    ForgeBar {
        if (orb == null) {
            Text(ui("forge.pick_orb"), color = Muted)
            return@ForgeBar
        }
        // An orb the item takes only under an omen (a catalyst's Orb of Quality) waits for one: alone the server would refuse it.
        val waiting = needsOmen(orb.code.value) && smithy.omen.isBlank()
        BarTitle(
            ForgeGlyphs.Orb,
            Gold,
            itemTitle(orb.code.value) + smithy.omen.takeIf { it.isNotBlank() }?.let { " + ${itemTitle(it)}" }.orEmpty(),
            if (waiting) ui("forge.needs_omen") to true else stock(code, owned, 1),
            orb = Orb.of(orb.code.value),
        )
        val resets = orb.code.value == Orb.QUALITY_ORB.name && lost > 0
        if (resets) Text(ui("forge.quality_reset", lost), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        HoldButton(ui("confirm.hold", ui("forge.apply_orb")), Gold, enabled = enabled && !instance.corrupted && !waiting, rearm = true) {
            if (resets) confirming = true else onApply(instance.id, orb.code.value)
        }
    }
    if (confirming && orb != null) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = PanelRaised,
            shape = DialogShape,
            tonalElevation = 0.dp,
            title = { Text(ui("forge.quality_reset_title"), color = Gold) },
            text = { Text(ui("forge.quality_reset_note", lost), color = Parchment) },
            confirmButton = {
                ForgeTextButton(enabled = enabled, onClick = {
                    confirming = false
                    onApply(instance.id, orb.code.value)
                }) { Text(ui("forge.quality_reset_do")) }
            },
            dismissButton = { ForgeTextButton(onClick = { confirming = false }) { Text(ui("common.cancel")) } },
        )
    }
}

/** The chosen bench line over the navigation, priced, with the same held button; рецепт, что не встанет (4.2.0), не предлагается. */
@Composable internal fun BenchBar(game: GameUi, vm: SmithyViewModel, item: ItemView, chosen: String, enabled: Boolean) {
    val index = game.index ?: return
    val hero = game.hero ?: return
    val instance = item.item
    val refusals = benchRefusals(index, hero, item)
    val recipe = game.bench.firstOrNull { it.code == chosen && it.code !in refusals }
    ForgeBar {
        when {
            chosen == UNCRAFT -> {
                val scouring = index.rules.bench.uncraftOrb
                val owned = game.bagAmount(scouring.name) ?: 0L
                BarTitle(ForgeGlyphs.Anvil, Crafted, ui("bench.remove"), stock(scouring.name, owned, 1))
                HoldButton(ui("confirm.hold", ui("forge.remove_bench")), Crafted, enabled = enabled && owned >= 1, rearm = true) { vm.uncraft(instance.id) }
            }

            recipe != null -> {
                val owned = game.bagAmount(recipe.orb.name) ?: 0L
                BarTitle(ForgeGlyphs.Anvil, Crafted, recipeText(index, recipe), stock(recipe.orb.name, owned, recipe.amount))
                HoldButton(ui("confirm.hold", ui("forge.apply_bench")), Crafted, enabled = enabled && owned >= recipe.amount, rearm = true) { vm.craft(instance.id, recipe.code) }
            }

            else -> Text(ui("forge.pick_line"), color = Muted)
        }
    }
}

/**
 * What the bag keeps after one use. It is printed only when the bag can pay; when it cannot, the
 * bar says so in red and the button stays off (2.46.0).
 * Нехватка с 3.89.0 - общей строкой «Не хватает: …».
 */
internal fun stock(code: String, have: Long, need: Long): Pair<String, Boolean> = Cost.item(code, need).shortfall({ have }, 0).text()?.let { it to true } ?: (ui("forge.orb_left", have, have - need) to false)

@Composable internal fun ForgeBar(content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = Bronze)
    Column(
        Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable internal fun BarTitle(icon: ImageVector, accent: Color, title: String, stock: Pair<String, Boolean>, orb: Orb? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (orb != null) {
            OrbGlyph(orb, Modifier.size(34.dp))
        } else {
            Icon(icon, null, tint = accent, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stock.first, color = if (stock.second) LifeRed else Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}
