package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.PartySocket
import com.sperance.exileforge.core.party.MateCard
import com.sperance.exileforge.core.party.PartyMessage
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.party.PartyFrame
import com.sperance.exileforge.rules.party.PartyLaunch
import com.sperance.exileforge.rules.party.PartyStatus
import com.sperance.exileforge.rules.party.PartyView
import kotlinx.coroutines.launch

/**
 * Co-op (3.25.0): the lobby before a zone and the link that carries the run between its heroes.
 *
 * The host gathers the lobby from a zone's card; guests come in by its code or from the guild's list. While the
 * hero is in a lobby the socket stays open: the roster comes over it, the start reaches the guests over it, and
 * then the run's own talk — the host's world and events to the guests, their cards and moves to the host — which
 * [ExpeditionViewModel] makes and takes. Leaving the run leaves the lobby; the host's leaving ends it for all.
 */
class PartyViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {
    private var socket: PartySocket? = null
    /** Whose socket it is: another hero entered means another link. */
    private var linked: String? = null

    val view: PartyView? get() = state.value.play.party.view
    val isHost: Boolean get() = view?.isHost(heroId) == true

    /** The lobby the hero is in, as the server holds it: read on the world map, and the link opened for it. */
    fun refresh() { with(runtime) {
        val id = heroId.takeIf { it.isNotBlank() } ?: return
        read("party") {
            val current = api.party.current(id)
            if (!onScreen(id)) return@read
            show(current)
            if (current != null) link(id) else unlink()
            show(guild = api.party.guild(id))
        }
    } }

    fun loadGuild() { with(runtime) {
        val id = heroId.takeIf { it.isNotBlank() } ?: return
        read("partyGuild") { api.party.guild(id).let { if (onScreen(id)) show(guild = it) } }
    } }

    /** The host gathers a lobby for [mapCode], to set out with the picked map. */
    fun create(mapCode: String, itemId: String?) = command { id -> api().party.create(id, mapCode, itemId).also { link(id) } }

    fun join(code: String) = command { id -> api().party.join(id, code).also { link(id) } }

    fun kick(memberId: String) = command { id -> api().party.kick(id, memberId); view }

    /** Out of the lobby — for a guest in a run, the run in the host's zone is closed with it. */
    fun leave() { with(runtime) {
        val id = heroId.takeIf { it.isNotBlank() } ?: return
        task(writing = true) { quit(id) }
    } }

    internal suspend fun quit(id: String) {
        runCatching { runtime.api.party.leave(id) }
        unlink()
        show(null)
    }

    /** The host sets out: the server enters every hero; the host's run begins here, the guests' by the socket. */
    internal suspend fun start(id: String): PartyLaunch = runtime.api.party.start(id)

    /** Sends [message] over the lobby's link: from a guest to the host, from the host to guest [to] or to all. */
    fun send(message: PartyMessage, to: String? = null) { socket?.send(message, to) }

    /** The hero's card for the host's fights: the finished sheet, the skills, the belt, the weapon and the pet. */
    fun sendCard() {
        val hero = state.value.hero?.takeIf { it.id == heroId } ?: return
        if (isHost || view?.status != PartyStatus.RUNNING) return
        val weapon = hero.equipped[Slot.WEAPON_1H] ?: hero.equipped[Slot.WEAPON_2H]
        send(PartyMessage.Card(MateCard(hero.id, hero.info.name, hero.heroClass, hero.level, hero.stats, hero.skills,
            Slot.FLASKS.map { hero.equipped[it] }, weapon?.template, hero.pets.pet(hero.pets.combat))))
    }

    private fun link(id: String) {
        if (linked == id && socket != null) return
        unlink()
        linked = id
        socket = runtime.api.party.socket(id, runtime.scope, { incoming -> runtime.scope.launch { take(id, incoming) } }) { online ->
            runtime.scope.launch { update { it.copy(play = it.play.copy(party = it.play.party.copy(online = online))) } }
        }
    }

    private fun unlink() {
        socket?.close()
        socket = null
        linked = null
    }

    private fun take(id: String, frame: PartyFrame) {
        if (!onScreen(id)) return
        when (frame) {
            is PartyFrame.Roster -> roster(frame.party)
            is PartyFrame.Launched -> runtime.expeditionViewModel.follow(id, frame.launch)
            is PartyFrame.Relay -> PartyMessage.decode(frame.payload)?.let { message ->
                if (isHost) runtime.expeditionViewModel.fromGuest(frame.from, message)
                else if (message is PartyMessage.Hello) sendCard() else runtime.expeditionViewModel.fromHost(message)
            }
            is PartyFrame.Closed -> {
                unlink()
                show(null)
                runtime.toast(ui("party.closed.${frame.reason}"))
                runtime.expeditionViewModel.partyGone()
            }
            is PartyFrame.Send -> Unit
        }
    }

    /** A new roster: the host lets go of the guests who left, and asks the ones back for their cards. */
    private fun roster(party: PartyView) {
        val before = view
        show(party)
        if (party.isHost(heroId)) {
            val gone = before?.members.orEmpty().map { it.heroId } - party.members.map { it.heroId }.toSet()
            gone.forEach { runtime.expeditionViewModel.guestGone(it) }
            if (party.status == PartyStatus.RUNNING) send(PartyMessage.Hello)
        } else if (party.status == PartyStatus.RUNNING) sendCard()
    }

    private fun show(party: PartyView?) = update { it.copy(play = it.play.copy(party = it.play.party.copy(view = party))) }
    private fun show(guild: List<PartyView>) = update { it.copy(play = it.play.copy(party = it.play.party.copy(guild = guild))) }

    private fun api() = runtime.api

    private fun command(call: suspend ForgeRuntime.(String) -> PartyView?) { with(runtime) {
        val id = heroId.takeIf { it.isNotBlank() } ?: return
        task(writing = true) { runtime.call(id).let { if (onScreen(id)) show(it) } }
    } }

    /** Another hero on screen, or signed out: the old one's link goes. */
    fun drop() { unlink(); show(null) }
}
