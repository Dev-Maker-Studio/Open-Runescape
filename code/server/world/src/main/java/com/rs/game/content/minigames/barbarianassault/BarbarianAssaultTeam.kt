package com.rs.game.content.minigames.barbarianassault

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj

const val TEAM_SIZE = 5
const val HIGHEST_WAVE = "baHighestWave"

private val LEADER_NAMES = intArrayOf(
    Component.interface_492.component_492_15, Component.interface_492.component_492_16, Component.interface_492.component_492_17,
    Component.interface_492.component_492_18, Component.interface_492.component_492_19,
)
private val LEADER_ROLES = intArrayOf(
    Component.interface_492.component_492_20, Component.interface_492.component_492_21, Component.interface_492.component_492_22,
    Component.interface_492.component_492_23, Component.interface_492.component_492_24,
)
private val LEADER_WAVES = intArrayOf(
    Component.interface_492.component_492_26, Component.interface_492.component_492_31, Component.interface_492.component_492_32,
    Component.interface_492.component_492_33, Component.interface_492.component_492_34,
)
private val LEADER_REMOVE = intArrayOf(
    Component.interface_492.component_492_47, Component.interface_492.component_492_27, Component.interface_492.component_492_28, Component.interface_492.component_492_29,
)
private val FOLLOWER_NAMES = intArrayOf(
    Component.interface_493.component_493_27, Component.interface_493.component_493_28, Component.interface_493.component_493_29,
    Component.interface_493.component_493_30, Component.interface_493.component_493_31,
)
private val FOLLOWER_ROLES = intArrayOf(
    Component.interface_493.component_493_32, Component.interface_493.component_493_33, Component.interface_493.component_493_34,
    Component.interface_493.component_493_35, Component.interface_493.component_493_36,
)
private val FOLLOWER_WAVES = intArrayOf(
    Component.interface_493.component_493_38, Component.interface_493.component_493_39, Component.interface_493.component_493_40,
    Component.interface_493.component_493_41, Component.interface_493.component_493_42,
)
private val FOLLOWER_ROLE_BUTTONS = mapOf(
    Component.interface_493.component_493_7 to Role.ATTACKER, Component.interface_493.component_493_8 to Role.ATTACKER,
    Component.interface_493.component_493_9 to Role.DEFENDER, Component.interface_493.component_493_10 to Role.DEFENDER,
    Component.interface_493.component_493_11 to Role.COLLECTOR, Component.interface_493.component_493_12 to Role.COLLECTOR,
    Component.interface_493.component_493_13 to Role.HEALER, Component.interface_493.component_493_14 to Role.HEALER,
)
private val FOLLOWER_ROLE_HIGHLIGHTS = mapOf(
    Role.ATTACKER to Component.interface_493.component_493_19, Role.DEFENDER to Component.interface_493.component_493_54,
    Role.COLLECTOR to Component.interface_493.component_493_53, Role.HEALER to Component.interface_493.component_493_52,
)
private val FOLLOWER_ACCEPT = intArrayOf(Component.interface_493.component_493_15, Component.interface_493.component_493_16)
private val FOLLOWER_DECLINE = intArrayOf(Component.interface_493.component_493_6, Component.interface_493.component_493_17)

val Player.highestBarbarianAssaultWave get() = getI(HIGHEST_WAVE, 0).coerceAtLeast(0)

class BarbarianAssaultTeam(leader: Player, var room: WaitingRoom) {
    val members = arrayListOf(leader)
    val roles = HashMap<Player, Role>()
    var game: BarbarianAssaultGame? = null
    var nextWave: Wave? = null

    val leader: Player get() = members.first()
    val full get() = members.size >= TEAM_SIZE
    val missingRoles get() = Role.entries.filter { it !in roles.values }

    fun roleOf(player: Player) = roles[player]

    fun canTake(role: Role, joining: Boolean): Boolean {
        val slotsAfter = TEAM_SIZE - members.size - if (joining) 1 else 0
        val missingAfter = Role.entries.count { it != role && it !in roles.values }
        return missingAfter <= slotsAfter
    }

    fun ready(): String? = when {
        members.size < TEAM_SIZE -> "You need a team of $TEAM_SIZE to enter the arena."
        members.any { it !in roles } -> "Everyone on your team needs a role before you can start."
        missingRoles.isNotEmpty() -> "Your team needs an ${missingRoles.joinToString { it.displayName }} before you can start."
        members.any { !room.contains(it.tile) || it.hasFinished() } -> "Your whole team needs to be in this room before you can start."
        else -> null
    }

    fun sendTeam(message: String) = members.forEach { it.sendMessage(message) }
}

object BarbarianAssaultTeams {
    private val teams = HashMap<Player, BarbarianAssaultTeam>()
    private val invites = HashMap<Player, BarbarianAssaultTeam>()
    private val pendingRoles = HashMap<Player, Role>()

    fun of(player: Player) = teams[player]

    fun create(leader: Player, room: WaitingRoom): BarbarianAssaultTeam = BarbarianAssaultTeam(leader, room).also { teams[leader] = it }

    fun writeRole(player: Player, room: WaitingRoom) {
        val team = teams[player]
        if (team != null && team.leader != player) {
            player.sendMessage("Only your team leader can use the scroll to set roles.")
            return
        }
        player.sendOptionsDialogue("Which role would you like to play?") {
            for (role in Role.entries) opExec(role.displayName) { setLeaderRole(player, room, role) }
        }
    }

    fun setLeaderRole(player: Player, room: WaitingRoom, role: Role) {
        val team = teams[player] ?: create(player, room)
        if (team.leader != player || team.game != null) return
        val previous = team.roles.remove(player)
        if (!team.canTake(role, false)) {
            previous?.let { team.roles[player] = it }
            player.sendMessage("Your team still needs every role covered; pick one of ${team.missingRoles.joinToString { it.displayName }}.")
            return
        }
        team.roles[player] = role
        showRole(player, role)
        player.sendMessage("You will play as the ${role.displayName}.")
        refresh(team)
    }

    fun invite(leader: Player, target: Player) {
        val room = WaitingRoom.containing(leader.tile)
        val team = teams[leader] ?: room?.let { create(leader, it) }
        val refusal = when {
            room == null || team == null -> "You can only recruit players inside a waiting room."
            team.leader != leader -> "Only your team leader can recruit players."
            team.game != null -> "Your team is busy fighting."
            team.full -> "Your team is already full."
            target == leader -> "You can't recruit yourself."
            !room.contains(target.tile) -> "${target.displayName} needs to be in this room to be recruited."
            teams.containsKey(target) -> "${target.displayName} is already on a team."
            else -> null
        }
        if (refusal != null) {
            leader.sendMessage(refusal)
            return
        }
        invites[target] = team!!
        pendingRoles.remove(target)
        leader.sendMessage("You invite ${target.displayName} to join your team.")
        target.sendMessage("${leader.displayName} has invited you to join their Barbarian Assault team.")
        target.interfaceManager.sendInterface(Interface.interface_493)
        refreshInvite(target, team)
    }

    fun handleFollowerButton(player: Player, component: Int) {
        val team = invites[player] ?: run {
            player.closeInterfaces()
            return
        }
        when (component) {
            in FOLLOWER_ROLE_BUTTONS -> {
                pendingRoles[player] = FOLLOWER_ROLE_BUTTONS.getValue(component)
                refreshInvite(player, team)
            }
            in FOLLOWER_ACCEPT -> accept(player, team)
            in FOLLOWER_DECLINE -> {
                invites.remove(player)
                pendingRoles.remove(player)
                player.closeInterfaces()
                team.leader.sendMessage("${player.displayName} declined your invitation.")
            }
        }
    }

    private fun accept(player: Player, team: BarbarianAssaultTeam) {
        val role = pendingRoles[player]
        val refusal = when {
            role == null -> "Please select a role first."
            team.game != null -> "That team is already fighting."
            team.full -> "That team is already full."
            !team.room.contains(player.tile) -> "You need to be in the team's waiting room to join it."
            !team.canTake(role, true) -> "That team still needs ${team.missingRoles.joinToString { it.displayName }}; pick one of those."
            teams.containsKey(player) -> "You are already on a team."
            else -> null
        }
        if (refusal != null) {
            player.sendMessage(refusal)
            return
        }
        invites.remove(player)
        pendingRoles.remove(player)
        team.members += player
        team.roles[player] = role!!
        teams[player] = team
        showRole(player, role)
        player.closeInterfaces()
        team.sendTeam("${player.displayName} has joined the team as the ${role.displayName}.")
        player.sendMessage("Your team: ${team.members.joinToString { "${it.displayName} (${team.roleOf(it)?.displayName})" }}.")
        refresh(team)
    }

    fun handleLeaderButton(player: Player, component: Int) {
        val team = teams[player] ?: return
        val slot = LEADER_REMOVE.indexOf(component)
        if (slot < 0) {
            player.closeInterfaces()
            return
        }
        if (team.leader != player || team.game != null) return
        team.members.getOrNull(slot + 1)?.let { kick(team, it) }
    }

    fun kick(team: BarbarianAssaultTeam, member: Player) {
        if (member == team.leader) return
        team.sendTeam("${member.displayName} has been removed from the team.")
        leave(member)
    }

    fun leave(player: Player) {
        invites.remove(player)
        pendingRoles.remove(player)
        val team = teams.remove(player) ?: return
        hideRole(player)
        val leaderBefore = team.leader
        team.members.remove(player)
        team.roles.remove(player)
        if (player.interfaceManager.topOpen(Interface.interface_492)) player.closeInterfaces()
        if (team.members.isEmpty()) {
            invites.values.removeIf { it == team }
            return
        }
        val wasLeader = leaderBefore == player
        if (wasLeader && team.game == null) {
            team.sendTeam("${player.displayName}, your team leader, has left.")
            disband(team)
            return
        }
        team.game?.leave(player)
        team.sendTeam("${player.displayName} has left the team.")
        if (wasLeader) team.leader.sendMessage("You are now the leader of your team.")
        refresh(team)
    }

    fun disband(team: BarbarianAssaultTeam) {
        for (member in team.members.toList()) {
            teams.remove(member)
            hideRole(member)
            member.sendMessage("Your Barbarian Assault team has been disbanded.")
        }
        team.members.clear()
        team.roles.clear()
        invites.values.removeIf { it == team }
    }

    fun readScroll(player: Player) {
        val team = teams[player]
        if (team == null) {
            player.sendMessage("You aren't on a team. Choose a role with the scroll, then use it on other players in this room to recruit them.")
            return
        }
        player.interfaceManager.sendInterface(Interface.interface_492)
        refreshLeader(player, team)
    }

    fun refresh(team: BarbarianAssaultTeam) {
        for (member in team.members) if (member.interfaceManager.topOpen(Interface.interface_492)) refreshLeader(member, team)
        for ((invitee, invited) in invites) if (invited == team) refreshInvite(invitee, team)
        team.members.forEach { it.baWaitingController?.refreshHud() }
    }

    private fun refreshLeader(player: Player, team: BarbarianAssaultTeam) {
        fillRoster(player, team, LEADER_NAMES, LEADER_ROLES, LEADER_WAVES)
        player.packets.setIFText(Component.interface_492.component_492_1, "${team.leader.displayName}'s team")
        player.packets.setIFText(Component.interface_492.component_492_5, "Missing roles:")
        player.packets.setIFText(Component.interface_492.component_492_6, team.missingRoles.joinToString { it.displayName }.ifEmpty { "None" })
        player.packets.setIFText(Component.interface_492.component_492_45, "Your role level:")
        player.packets.setIFText(Component.interface_492.component_492_46, team.roleOf(player)?.level(player)?.toString() ?: "-")
    }

    private fun refreshInvite(player: Player, team: BarbarianAssaultTeam) {
        fillRoster(player, team, FOLLOWER_NAMES, FOLLOWER_ROLES, FOLLOWER_WAVES)
        val chosen = pendingRoles[player]
        player.packets.setIFText(Component.interface_493.component_493_5, "${team.leader.displayName} is recruiting")
        player.packets.setIFText(Component.interface_493.component_493_18, chosen?.displayName ?: "None")
        player.packets.setIFText(Component.interface_493.component_493_21, "Needed: ${team.missingRoles.joinToString { it.displayName }.ifEmpty { "any role" }}")
        for ((role, highlight) in FOLLOWER_ROLE_HIGHLIGHTS) player.setHidden(highlight, role != chosen)
    }

    private fun fillRoster(player: Player, team: BarbarianAssaultTeam, names: IntArray, roles: IntArray, waves: IntArray) {
        for (slot in 0 until TEAM_SIZE) {
            val member = team.members.getOrNull(slot)
            player.packets.setIFText(names[slot], member?.displayName ?: "none set")
            player.packets.setIFText(roles[slot], member?.let { team.roleOf(it)?.displayName } ?: "none set")
            player.packets.setIFText(waves[slot], member?.highestBarbarianAssaultWave?.toString() ?: "-")
        }
    }

    private fun showRole(player: Player, role: Role) = player.appearance.overrideWorn(Equipment.AURA, role.icon(player))

    private fun hideRole(player: Player) = player.appearance.clearWornOverride(Equipment.AURA)

    fun ensureScroll(player: Player) {
        if (player.inventory.containsItem(Obj.barbassault_scroll)) {
            player.sendMessage("You already have a recruitment scroll.")
            return
        }
        if (!player.inventory.hasFreeSlots()) {
            player.sendMessage("You need a free inventory space to take a scroll.")
            return
        }
        player.inventory.addItem(Obj.barbassault_scroll, 1)
    }
}

fun Player.setHidden(component: Int, hidden: Boolean) =
    packets.setIFHidden(Utils.interfaceIdFromHash(component), Utils.componentIdFromHash(component), hidden)
