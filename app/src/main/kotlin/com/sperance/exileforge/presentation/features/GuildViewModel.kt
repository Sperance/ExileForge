package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.GuildState
import com.sperance.exileforge.presentation.state.GuildTab
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.phrase
import com.sperance.exileforge.rules.content.GuildMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

/**
 * Guilds (3.22.0, server 1.20.0). The server keeps every rule; the client names a guild or a member, prints the refusal
 * and draws what the command answered — the hero's whole guild picture, or the guild as it now stands. A command that
 * moves the hero (joining, leaving, giving) reads the hero after it, since the gold rides on it.
 */
class GuildViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun tab(tab: GuildTab?) = guild { it.copy(tab = tab) }
    fun query(text: String) = guild { it.copy(query = text) }

    /** Narrows the list to one faction, or to none with a blank code, and reads it again from the first page. */
    fun filterFaction(code: String) {
        guild { it.copy(faction = code) }
        search(0)
    }

    /** The hero's guild; the content comes first, since `guilds.json` is what the screen reads its numbers from. */
    fun load() {
        with(runtime) {
            read(Reads.GUILD) {
                val id = heroId
                check(id.isNotBlank()) { ui("auction.choose_character") }
                ensureContent()
                val mine = runtime.api.guild.mine(id)
                if (onScreen(id)) guild { it.copy(mine = mine) }
            }
        }
    }

    fun search(page: Int = 0) {
        with(runtime) {
            read(Reads.GUILD_SEARCH, restart = true) {
                val id = heroId
                val found = state.value.guild.let { runtime.api.guild.search(id, it.query, page, it.faction) }
                if (onScreen(id)) guild { it.copy(search = found) }
            }
        }
    }

    fun create(name: String, tag: String, faction: String, emblem: String, color: String, mode: GuildMode, minLevel: Int) = moving(ui("guild.toast.created", name.trim())) { runtime.api.guild.create(it, name, tag, faction, emblem, color, mode, minLevel) }

    /** An OPEN guild takes the hero at once; any other is asked. */
    fun join(card: GuildCard) = if (card.mode == GuildMode.OPEN) {
        moving(ui("guild.toast.joined", card.name)) { runtime.api.guild.join(it, card.id) }
    } else {
        moving(ui("guild.toast.applied", card.name)) { runtime.api.guild.apply(it, card.id) }
    }

    fun acceptInvite(guildId: String) = moving(ui("guild.toast.joined", inviteName(guildId))) { runtime.api.guild.acceptInvite(it, guildId) }
    fun declineInvite(guildId: String) = moving(ui("guild.toast.declined")) { runtime.api.guild.declineInvite(it, guildId) }
    fun leave() = moving(ui("guild.toast.left")) { runtime.api.guild.leave(it) }
    fun disband() = moving(ui("guild.toast.disbanded")) { runtime.api.guild.disband(it) }

    fun acceptApplicant(applicantId: String) = inside(ui("guild.toast.accepted")) { runtime.api.guild.acceptApplicant(it, applicantId) }
    fun declineApplicant(applicantId: String) = inside(ui("guild.toast.declined")) { runtime.api.guild.declineApplicant(it, applicantId) }
    fun invite(name: String) = inside(ui("guild.toast.invited", name.trim())) { runtime.api.guild.invite(it, name) }
    fun member(command: MemberCommand, memberId: String) = inside(ui("guild.toast.done")) { runtime.api.guild.member(it, command, memberId) }

    fun settings(mode: GuildMode, minLevel: Int, emblem: String, color: String, announcement: String) = inside(ui("guild.toast.saved")) { runtime.api.guild.settings(it, mode, minLevel, emblem, color, announcement) }

    /** The bonus tree (3.79.0): the leader takes a rank or resets it all. */
    fun takeNode(node: String) = moving(ui("guild.toast.node")) { runtime.api.guild.takeNode(it, node) }
    fun resetTree() = moving(ui("guild.toast.tree_reset")) { runtime.api.guild.resetTree(it) }

    /** The guild stash (3.79.0): read, put in, take out, a tab's rank; every write answers with the stash as it stands. */
    fun loadStash() {
        with(runtime) {
            read(Reads.GUILD) {
                val id = heroId
                val stash = runtime.api.guild.stash(id)
                if (onScreen(id)) guild { it.copy(stash = stash) }
            }
        }
    }
    fun deposit(tab: Int, itemId: String?, code: String?, amount: Long) = stashCommand(ui("guild.toast.deposited")) { runtime.api.guild.deposit(it, tab, itemId, code, amount) }
    fun take(entryId: String) = stashCommand(ui("guild.toast.taken")) { runtime.api.guild.take(it, entryId) }
    fun tabRank(tab: Int, minRank: Int) = stashCommand(ui("guild.toast.saved")) { runtime.api.guild.tabRank(it, tab, minRank) }

    private fun stashCommand(done: String, block: suspend (String) -> com.sperance.exileforge.core.model.guild.GuildStashView) = command(done) { id ->
        val stash = block(id)
        guild { it.copy(stash = stash) }
        after { runtime.heroViewModel.readHero() }
    }

    /** Gold or an orb into the treasury: the answer carries the guild, the hero's row and the gold left, so nothing is read again but the hero. */
    fun contribute(item: String, amount: Long) {
        with(runtime) {
            task(writing = true, touches = setOf(Reads.GUILD, Reads.HERO)) {
                val id = heroId
                val given = runtime.api.guild.contribute(id, item, amount)
                if (!onScreen(id)) return@task
                guild { it.copy(mine = it.mine?.copy(guild = given.guild, me = given.me)) }
                update { s -> s.copy(play = s.play.copy(hero = s.play.hero?.let { it.copy(info = it.info.copy(money = given.money)) })) }
                toast(ui("guild.toast.contributed"))
                after { heroViewModel.readHero() }
            }
        }
    }

    /** The journal from its newest page, or the next page after the ones shown; an empty page is the end. */
    fun loadLog(more: Boolean = false) {
        with(runtime) {
            read(Reads.GUILD_LOG, restart = !more) {
                val id = heroId
                val page = if (more) state.value.guild.logPage + 1 else 0
                val entries = runtime.api.guild.log(id, page)
                if (onScreen(id)) guild { it.copy(log = if (more) it.log + entries else entries, logPage = page, logEnd = entries.isEmpty()) }
            }
        }
    }

    private fun inviteName(guildId: String): String = state.value.guild.mine?.invites?.firstOrNull { it.guild.id == guildId }?.guild?.name.orEmpty()

    /**
     * A command that moves the hero in or out of a guild — or answers an invitation — and answers the whole [GuildMine].
     * The hero is read after it: the gold rides on the snapshot, a 304 when the command brought it.
     */
    private fun moving(done: String, block: suspend (String) -> GuildMine) = command(done) { id ->
        val mine = block(id)
        guild { it.copy(mine = mine, tab = if (mine.guild == null) null else it.tab) }
        after { runtime.heroViewModel.readHero() }
    }

    /** A command inside the guild, answered by the guild as it now stands. */
    private fun inside(done: String, block: suspend (String) -> GuildView) = command(done) { id ->
        val view = block(id)
        guild { it.copy(mine = it.mine?.let { mine -> mine.copy(guild = view, me = view.member(id) ?: mine.me) }) }
    }

    /** One guild command: one at a time — the hero cannot be swapped under it — and never retried. */
    private fun command(done: String, block: suspend (String) -> Unit) {
        with(runtime) {
            task(writing = true, touches = setOf(Reads.GUILD, Reads.HERO)) {
                val id = heroId
                check(id.isNotBlank()) { ui("auction.choose_character") }
                block(id)
                toast(done)
            }
        }
    }

    /** The reads after a command the server already made: their failure is not the command's. */
    private suspend fun after(block: suspend () -> Unit) {
        with(runtime) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                runtime.commands.refuse(phrase("guild.done_refresh"))
            }
        }
    }

    private fun guild(transform: (GuildState) -> GuildState) = update { it.copy(guild = transform(it.guild)) }
}
