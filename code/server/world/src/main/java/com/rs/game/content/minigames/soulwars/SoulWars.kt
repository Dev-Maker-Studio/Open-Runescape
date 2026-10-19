package com.rs.game.content.minigames.soulwars

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.content.minigames.checkAndDeleteFoodAndPotions
import com.rs.game.content.minigames.giveFoodAndPotions
import com.rs.game.content.minigames.isMinigameSupply
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemEquip
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.Ticks
import com.rs.utils.equipSlot
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import org.darkan.core.util.Area
import org.darkan.core.util.Structure
import world.gregs.voidps.type.Tile
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sign

private const val ZEAL_COUNTER = "Soul Wars Zeal earned"
private const val PLAYED_COUNTER = "Soul Wars games played"
private const val WON_COUNTER = "Soul Wars games won"
private const val LOST_COUNTER = "Soul Wars games lost"
private const val DRAWN_COUNTER = "Soul Wars games drawn"
private const val KILLS_COUNTER = "Soul Wars players killed"
private const val DEATHS_COUNTER = "Soul Wars deaths"
private const val BONES_COUNTER = "Soul Wars bones buried"
private const val FRAGMENTS_COUNTER = "Soul Wars soul fragments offered"
private const val CAPTURES_COUNTER = "Soul Wars capture points taken"
private const val PLAYER_MINIMUM = 4
private const val CLIENT_TEAM_CAPACITY = 10
private val TICKS_PER_MINUTE = Ticks.fromMinutes(1)
private val LOBBY_COUNTDOWN = Ticks.fromMinutes(3)
private val GAME_DURATION = Ticks.fromMinutes(20)
private const val CAPTURE_INTERVAL = 5
private const val CONTROL_MAX = 30
private const val CONTROL_NEUTRAL = 15
private const val RED_CONTROL = 25
private const val BLUE_CONTROL = 5
private const val MAX_AVATAR_LEVEL = 100
private const val MIN_AVATAR_LEVEL = 80
private const val AVATAR_DAMAGE_CAP = 700
private const val MAX_ACTIVITY = 1000
private const val DEATH_JINGLE = 90
private const val BANDAGE_HEAL_PERCENT = 10
private const val BANDAGE_RUN_ENERGY = 50.0
private const val BANDAGE_DELAY = 3

private val WAITING_ROOM_ENTRY = Tile.of(1879, 3162, 0)
private val WAITING_ROOM_EXIT = Tile.of(1880, 3162, 0)
private val GREEN_PORTAL_ARRIVAL = Tile.of(1875, 3162, 0)
private val GAME_EXIT = Tile.of(1886, 3172, 0)
private val ARENA = Area(Structure.REGION, 28, 50, 3, 1)

internal enum class Team(
    val colouredName: String,
    val cape: Int,
    val avatarNpc: Int,
    val avatarTile: Tile,
    val avatarDangerChunks: Set<Int>,
    val spawnArea: Area,
    val exitArea: Area,
    val sizeVarc: Int,
    val avatarHealthVarc: Int,
    val avatarLevelVarc: Int,
    val avatarDeathsVarc: Int,
) {
    RED(
        "<col=A31818>red</col>", Obj.snp_red_cape, Npc.snp_red_boss, Tile.of(1965, 3249, 0),
        setOf(504215, 502167, 502166, 504214, 504213, 502165),
        Area(Structure.TILE, 1952, 3236, 5, 6), Area(Structure.TILE, 1893, 3166, 3, 7),
        Varc.varc_638, Varc.varc_640, Varc.varc_642, Varc.varc_644,
    ),
    BLUE(
        "<col=3232D1>blue</col>", Obj.snp_blue_cape, Npc.snp_blue_boss, Tile.of(1805, 3208, 0),
        setOf(463248, 461200, 461201, 463249, 463250, 461202),
        Area(Structure.TILE, 1817, 3222, 5, 6), Area(Structure.TILE, 1885, 3166, 3, 7),
        Varc.varc_637, Varc.varc_639, Varc.varc_641, Varc.varc_643,
    );

    val opponent: Team get() = if (this == RED) BLUE else RED
}

internal enum class CapturePoint(
    val label: String,
    val zone: Area,
    val barChunks: Set<Int>,
    val bar: Int,
    val varc: Int,
    val locTile: Tile,
    val locShape: ObjectShape,
    val redLoc: Int,
    val blueLoc: Int,
    val neutralLoc: Int,
    val respawnArea: Area?,
) {
    OBELISK(
        "the soul obelisk", Area(Structure.TILE, 1879, 3224, 15, 15), setOf(481684, 479636, 479635, 481683, 483731, 483732),
        Component.interface_836.component_836_47, Varc.snp_obelisk_control_is_client, Tile.of(1886, 3231, 0), ObjectShape.SCENERY_INTERACT,
        Loc.snp_central_obelisk_red, Loc.snp_central_obelisk_blue, Loc.snp_central_obelisk_neutral, null,
    ),
    EAST_GRAVEYARD(
        "the eastern graveyard", Area(Structure.TILE, 1928, 3240, 10, 10), setOf(493973, 493974, 496022, 496021),
        Component.interface_836.component_836_49, Varc.snp_east_control_is_client, Tile.of(1933, 3243, 0), ObjectShape.WALL_STRAIGHT,
        Loc.snp_red_eastern_barrier, Loc.snp_blue_eastern_barrier, Loc.snp_neutral_eastern_barrier, Area(Structure.TILE, 1932, 3244, 2, 1),
    ),
    WEST_GRAVEYARD(
        "the western graveyard", Area(Structure.TILE, 1837, 3213, 10, 10), setOf(471442, 469394, 469393, 471441),
        Component.interface_836.component_836_50, Varc.snp_west_control_is_client, Tile.of(1842, 3220, 0), ObjectShape.WALL_STRAIGHT,
        Loc.snp_red_western_barrier, Loc.snp_blue_western_barrier, Loc.snp_neutral_western_barrier, Area(Structure.TILE, 1841, 3218, 2, 1),
    );

    val locs = setOf(redLoc, blueLoc, neutralLoc)

    fun show(owner: Team?) {
        val loc = World.getObjectWithType(locTile, locShape) ?: return
        val id = when (owner) {
            Team.RED -> redLoc
            Team.BLUE -> blueLoc
            null -> neutralLoc
        }
        if (loc.id != id)
            loc.changeId(id)
    }

    companion object {
        fun forChunk(chunkId: Int) = entries.firstOrNull { chunkId in it.barChunks }
    }
}

private class VarcFeed(vararg readers: Pair<Int, () -> Int>) {
    private val varcs = readers.map { it.first }.toIntArray()
    private val values = readers.map { it.second }

    fun newSentValues() = IntArray(varcs.size) { Int.MIN_VALUE }

    fun sync(player: Player, sent: IntArray) {
        for (i in varcs.indices) {
            val value = values[i]()
            if (sent[i] != value) {
                sent[i] = value
                player.packets.sendVarc(varcs[i], value)
            }
        }
    }
}

private object SoulWarsLobby {
    val players = LinkedHashSet<Player>()
    var game: SoulWars? = null
    var countdown = 0

    val playersNeeded get() = (PLAYER_MINIMUM - players.size).coerceAtLeast(0)

    fun tick() {
        game?.let { return it.tick() }
        if (++countdown < LOBBY_COUNTDOWN)
            return
        countdown = 0
        if (players.size >= PLAYER_MINIMUM)
            SoulWars().let {
                game = it
                it.start(players.toList())
            }
    }

    fun gameEnded() {
        game = null
        countdown = 0
    }
}

private val LOBBY_VARCS = VarcFeed(
    Varc.snp_game_in_progress_client to { if (SoulWarsLobby.game == null) 0 else 1 },
    Varc.varc_633 to { CLIENT_TEAM_CAPACITY - SoulWarsLobby.playersNeeded },
    Varc.varc_634 to { CLIENT_TEAM_CAPACITY - SoulWarsLobby.playersNeeded },
    Varc.varc_635 to { SoulWarsLobby.countdown / TICKS_PER_MINUTE },
    Varc.snp_game_duration_client to { (SoulWarsLobby.game?.ticks ?: 0) / TICKS_PER_MINUTE },
    *Team.entries.map { team -> team.sizeVarc to { SoulWarsLobby.game?.members(team)?.size ?: 0 } }.toTypedArray(),
)

private val GAME_VARCS = VarcFeed(
    Varc.snp_game_duration_client to { (SoulWarsLobby.game?.ticks ?: 0) / TICKS_PER_MINUTE },
    *Team.entries.flatMap { team ->
        listOf(
            team.avatarHealthVarc to { SoulWarsLobby.game?.avatar(team)?.healthPercent() ?: 0 },
            team.avatarLevelVarc to { SoulWarsLobby.game?.avatar(team)?.level ?: 0 },
            team.avatarDeathsVarc to { SoulWarsLobby.game?.avatarDeaths(team) ?: 0 },
        )
    }.toTypedArray(),
    *CapturePoint.entries.map { point -> point.varc to { SoulWarsLobby.game?.control(point) ?: CONTROL_NEUTRAL } }.toTypedArray(),
)

@ServerStartupEvent
fun mapSoulWars() {
    onObjectClick(Loc.snp_edgeville_portal_leafy) { (player) -> player.useStairs(-1, Tile.of(1886, 3178, 0), 0, 1) }

    onObjectClick(Loc.snp_leave_soulwars_portal) { (player) -> player.useStairs(-1, Tile.of(3082, 3475, 0), 0, 1) }

    onObjectClick(Loc.snp_blue_waitingroom_barrier) { (player) ->
        when {
            player.x < WAITING_ROOM_EXIT.x -> {
                player.passThrough(WAITING_ROOM_EXIT)
                player.controllerManager.forceStop()
            }
            hasCape(player) -> player.sendMessage("You cannot enter the lobby with a cape.")
            else -> {
                player.passThrough(WAITING_ROOM_ENTRY)
                player.controllerManager.startController(SoulWarsLobbyController())
            }
        }
    }

    onObjectClick(Loc.snp_green_portal) { (player) ->
        if (hasCape(player))
            return@onObjectClick player.sendMessage("You cannot enter the lobby with a cape.")
        player.tele(GREEN_PORTAL_ARRIVAL)
        player.controllerManager.startController(SoulWarsLobbyController())
    }

    onItemClick(Obj.snp_bones, options = arrayOf("Bury")) { (player, item) ->
        val controller = player.controllerManager.getController(SoulWarsGameController::class.java) ?: return@onItemClick
        if (controller.game?.bury(player, controller.team) != true)
            return@onItemClick
        controller.activity = min(controller.activity + 100, MAX_ACTIVITY)
        player.inventory.deleteItem(item)
        player.anim(Seq.seq_827)
    }

    onItemEquip(Obj.snp_red_cape, Obj.snp_blue_cape) { e ->
        if (!e.dequip())
            return@onItemEquip
        if (!e.player.controllerManager.isIn(SoulWarsGameController::class.java))
            removeCape(e.player)
        e.cancel()
    }

    onChunkEnter { (entity, chunkId) ->
        val player = entity as? Player ?: return@onChunkEnter
        if (!player.controllerManager.isIn(SoulWarsGameController::class.java))
            return@onChunkEnter
        val shown = CapturePoint.forChunk(chunkId)
        for (point in CapturePoint.entries)
            player.packets.setIFHidden(Interface.interface_836, Utils.componentIdFromHash(point.bar), point != shown)
    }

    WorldTasks.scheduleTimer(0, 0) {
        SoulWarsLobby.tick()
        true
    }
}

private fun hasCape(player: Player) =
    player.equipment.get(Equipment.CAPE) != null || player.inventory.items.array().any { it?.definitions?.equipSlot == Equipment.CAPE }

private fun removeCape(player: Player) {
    player.equipment.setNoPluginTrigger(Equipment.CAPE, null)
    player.equipment.refresh(Equipment.CAPE)
    player.appearance.generateAppearanceData()
}

internal class SoulAvatar(val team: Team, private val game: SoulWars) : NPC(team.avatarNpc, team.avatarTile) {
    var level = MAX_AVATAR_LEVEL

    init {
        capDamage = AVATAR_DAMAGE_CAP
    }

    override fun sendDeath(source: Entity?) {
        super.sendDeath(source)
        level = MAX_AVATAR_LEVEL
        game.avatarKilled(team)
    }

    override fun handlePreHit(hit: Hit) {
        val attacker = (hit.source as? Familiar)?.owner ?: hit.source as? Player
        if (attacker != null && !attackableBy(attacker))
            hit.damage = 0
        super.handlePreHit(hit)
    }

    override fun canBeAttackedBy(player: Player): Boolean {
        if (game.teamOf(player) != team.opponent)
            return false
        if (!meetsSlayerRequirement(player)) {
            player.sendMessage("You need a Slayer level of $level to attack this avatar.")
            return false
        }
        return true
    }

    override fun getPossibleTargets(): MutableList<Entity> =
        game.members(team.opponent).filter { !it.isDead && it.getChunkId() in team.avatarDangerChunks }.toMutableList()

    fun attackableBy(player: Player) = game.teamOf(player) == team.opponent && meetsSlayerRequirement(player)

    private fun meetsSlayerRequirement(player: Player) = player.skills.getLevelForXp(Skills.SLAYER) >= level

    fun remainingHitpoints() = if (isDead || hasFinished()) 0 else hitpoints

    fun healthPercent() = ceil(remainingHitpoints() * 100.0 / maxHitpoints).toInt()
}

internal class SoulWars {
    private val teams = Team.entries.associateWith { LinkedHashSet<Player>() }
    private val avatars = Team.entries.associateWith { SoulAvatar(it, this) }
    private val avatarDeaths = Team.entries.associateWith { 0 }.toMutableMap()
    private val control = CapturePoint.entries.associateWith { CONTROL_NEUTRAL }.toMutableMap()
    private val owners = CapturePoint.entries.associateWith<CapturePoint, Team?> { null }.toMutableMap()
    var ticks = 0
        private set
    var ended = false
        private set

    fun members(team: Team): Set<Player> = teams.getValue(team)
    fun teamOf(player: Player) = Team.entries.firstOrNull { player in teams.getValue(it) }
    fun avatar(team: Team) = avatars.getValue(team)
    fun avatarDeaths(team: Team) = avatarDeaths.getValue(team)
    fun control(point: CapturePoint) = control.getValue(point)

    fun start(entrants: List<Player>) {
        CapturePoint.entries.forEach { it.show(null) }
        val combatTotals = Team.entries.associateWith { 0 }.toMutableMap()
        for (player in entrants.sortedByDescending { it.skills.combatLevel }) {
            val team = Team.entries.minWith(compareBy<Team>({ teams.getValue(it).size }, { combatTotals.getValue(it) }))
            teams.getValue(team).add(player)
            combatTotals[team] = combatTotals.getValue(team) + player.skills.combatLevel
            player.controllerManager.startController(SoulWarsGameController(team == Team.RED, this))
        }
    }

    fun tick() {
        if (++ticks >= GAME_DURATION)
            return end()
        if (ticks % CAPTURE_INTERVAL == 0)
            updateCapturePoints()
    }

    private fun updateCapturePoints() {
        for (point in CapturePoint.entries) {
            val balance = capturers(Team.RED, point) - capturers(Team.BLUE, point)
            val value = (control.getValue(point) + balance.sign).coerceIn(0, CONTROL_MAX)
            control[point] = value
            val owner = when {
                value >= RED_CONTROL -> Team.RED
                value <= BLUE_CONTROL -> Team.BLUE
                else -> null
            }
            if (owner == owners[point])
                continue
            owners[point] = owner
            point.show(owner)
            if (owner != null) {
                teams.getValue(owner).filter { it.isCanPvp && !it.isDead && point.zone.within(it.tile) }.forEach { it.incrementCount(CAPTURES_COUNTER) }
                broadcast("The ${owner.colouredName} team has taken control of ${point.label}.")
            }
        }
    }

    private fun capturers(team: Team, point: CapturePoint) =
        teams.getValue(team).count { it.isCanPvp && !it.isDead && point.zone.within(it.tile) }

    private fun broadcast(message: String) = teams.values.forEach { team -> team.forEach { it.sendMessage(message) } }

    fun avatarKilled(team: Team) {
        avatarDeaths[team] = avatarDeaths.getValue(team) + 1
    }

    fun respawnTile(team: Team, tile: Tile): Tile {
        val graveyards = CapturePoint.entries.filter { owners[it] == team }.mapNotNull { it.respawnArea?.getRandomTile() }
        return (graveyards + team.spawnArea.getRandomTile()).minBy { Utils.getDistance(it, tile) }
    }

    fun bury(player: Player, team: Team): Boolean {
        val avatar = avatars.getValue(team)
        if (avatar.isDead || avatar.hasFinished())
            return false
        if (avatar.level >= MAX_AVATAR_LEVEL) {
            player.sendMessage("Your avatar is already a high enough level.")
            return false
        }
        avatar.level++
        player.incrementCount(BONES_COUNTER)
        return true
    }

    fun offerFragments(player: Player, team: Team, fragments: Item) {
        if (owners[CapturePoint.OBELISK] != team)
            return player.sendMessage("The obelisk is unresponsive as your team is not in control of it.")
        val avatar = avatars.getValue(team.opponent)
        val used = min(fragments.amount, avatar.level - MIN_AVATAR_LEVEL)
        if (used <= 0)
            return player.sendMessage("Your opponent's avatar cannot be weakened any further.")
        player.inventory.deleteItem(Obj.snp_tut_soul_fragment, used)
        player.incrementCount(FRAGMENTS_COUNTER, used)
        avatar.level -= used
    }

    fun left(player: Player) {
        teams.values.forEach { it.remove(player) }
        if (!ended && teams.values.all { it.isEmpty() })
            end()
    }

    private fun winner(): Team? {
        val redDeaths = avatarDeaths.getValue(Team.RED)
        val blueDeaths = avatarDeaths.getValue(Team.BLUE)
        if (redDeaths != blueDeaths)
            return if (redDeaths > blueDeaths) Team.BLUE else Team.RED
        val redHitpoints = avatars.getValue(Team.RED).remainingHitpoints()
        val blueHitpoints = avatars.getValue(Team.BLUE).remainingHitpoints()
        return when {
            redHitpoints > blueHitpoints -> Team.RED
            blueHitpoints > redHitpoints -> Team.BLUE
            else -> null
        }
    }

    private fun end() {
        ended = true
        val winner = winner()
        avatars.values.forEach {
            it.finish()
            it.cancelRespawnTask()
        }
        CapturePoint.entries.forEach { it.show(null) }
        for (team in Team.entries) {
            for (player in teams.getValue(team).toList()) {
                player.tele(team.exitArea.getRandomTile())
                player.controllerManager.forceStop()
                award(player, team, winner)
            }
        }
        clearArenaGroundItems()
        SoulWarsLobby.gameEnded()
    }

    private fun award(player: Player, team: Team, winner: Team?) {
        val zeal = if (winner == team) 3 else 1
        player.soulWarsZeal += zeal
        player.incrementCount(ZEAL_COUNTER, zeal)
        player.incrementCount(PLAYED_COUNTER)
        player.incrementCount(when (winner) {
            team -> WON_COUNTER
            null -> DRAWN_COUNTER
            else -> LOST_COUNTER
        })
        val result = when (winner) {
            team -> "Your team won"
            null -> "The game was a draw"
            else -> "Your team lost"
        }
        player.sendMessage("$result. You are awarded $zeal Zeal. You now have ${Utils.formatNumber(player.soulWarsZeal)} Zeal.")
    }

    private fun clearArenaGroundItems() {
        for (x in ARENA.mapX until ARENA.mapX + ARENA.mapWidth step 8) {
            for (y in ARENA.mapY until ARENA.mapY + ARENA.mapHeight step 8) {
                ChunkManager.getChunk(Tile.of(x, y, 0).getChunkId(), false).allGroundItems
                    .filter { it.id == Obj.snp_bones || it.id == Obj.snp_tut_soul_fragment || isMinigameSupply(it.id) }
                    .forEach { World.removeGroundItem(it) }
            }
        }
    }
}

class SoulWarsLobbyController : Controller() {
    @Transient
    private var sentVarcs: IntArray? = null

    override fun start() {
        player.interfaceManager.sendOverlay(Interface.interface_837)
        SoulWarsLobby.players.add(player)
        syncVarcs()
    }

    override fun process() = syncVarcs()

    private fun syncVarcs() {
        val sent = sentVarcs ?: LOBBY_VARCS.newSentValues().also { sentVarcs = it }
        LOBBY_VARCS.sync(player, sent)
    }

    override fun login(): Boolean {
        player.tele(WAITING_ROOM_EXIT)
        return true
    }

    override fun processTeleport(tele: Teleport) = false

    override fun logout(): Boolean {
        SoulWarsLobby.players.remove(player)
        player.tile = WAITING_ROOM_EXIT
        return false
    }

    override fun onRemove() {
        SoulWarsLobby.players.remove(player)
        player.interfaceManager.removeOverlay()
    }
}

class SoulWarsGameController internal constructor(val redTeam: Boolean, @Transient internal val game: SoulWars?) : Controller() {
    @Transient
    internal var activity: Int = MAX_ACTIVITY

    @Transient
    private var sentVarcs: IntArray? = null

    internal val team get() = if (redTeam) Team.RED else Team.BLUE

    override fun start() {
        player.interfaceManager.sendOverlay(Interface.interface_836)
        syncVarcs()
        player.tele(team.spawnArea.getRandomTile())
        player.equipment.setNoPluginTrigger(Equipment.CAPE, Item(team.cape, 1))
        player.equipment.refresh(Equipment.CAPE)
        player.appearance.generateAppearanceData()
        player.isCanPvp = false
        player.startConversation {
            npc(Npc.nom_nomad, HeadE.EVIL_LAUGH, "The time is now! Crush their souls!")
        }
    }

    private fun syncVarcs() {
        val sent = sentVarcs ?: GAME_VARCS.newSentValues().also { sentVarcs = it }
        GAME_VARCS.sync(player, sent)
    }

    override fun processObjectClick1(obj: GameObject): Boolean {
        when (obj.id) {
            Loc.snp_blue_exit_portal, Loc.snp_red_exit_portal -> player.sendOptionDialogue {
                it.add("Yes, leave. (You won't receive any rewards for doing so)") { player.controllerManager.forceStop() }
                it.add("Nevermind.")
            }
            Loc.snp_counter_bandages_blue, Loc.snp_counter_bandages_red -> giveFoodAndPotions(player)
            Loc.snp_blue_graveyard_barrier -> leaveThrough(obj, player.x < obj.x, -1, 0)
            Loc.snp_red_graveyard_barrier -> leaveThrough(obj, player.x >= obj.x, 0, 0)
            in CapturePoint.EAST_GRAVEYARD.locs -> leaveThrough(obj, player.y <= obj.y, 0, 0)
            in CapturePoint.WEST_GRAVEYARD.locs -> leaveThrough(obj, player.y >= obj.y, 0, 0)
        }
        return false
    }

    private fun leaveThrough(obj: GameObject, alreadyOutside: Boolean, deltaX: Int, deltaY: Int) {
        if (alreadyOutside)
            return player.sendMessage("Onward! You have no reason to go back inside.")
        player.passThrough(obj.tile.transform(deltaX, deltaY, 0))
        player.isCanPvp = true
    }

    override fun processButtonClick(interfaceId: Int, componentId: Int, slotId: Int, slotId2: Int, opNum: Int): Boolean {
        if (interfaceId != Interface.inventory || opNum != 1) return true
        val bandages = player.inventory.getItem(slotId)?.takeIf { it.id == Obj.castlewars_bandages } ?: return true
        if (!player.canEat()) return false
        player.inventory.deleteItem(slotId, bandages)
        player.anim(Seq.human_eat)
        player.addFoodDelay(BANDAGE_DELAY)
        player.heal(player.maxHitpoints * BANDAGE_HEAL_PERCENT / 100)
        player.restoreRunEnergy(BANDAGE_RUN_ENERGY)
        player.poison.reset()
        return false
    }

    override fun processItemOnObject(obj: GameObject, item: Item): Boolean {
        if (item.id == Obj.snp_tut_soul_fragment && obj.id in CapturePoint.OBELISK.locs)
            game?.offerFragments(player, team, item)
        return false
    }

    override fun sendDeath(): Boolean {
        player.lock(8)
        player.stopAll()
        WorldTasks.scheduleTimer { loop ->
            when (loop) {
                0 -> player.anim(Seq.human_death)
                1 -> player.sendMessage("Oh dear, you have died.")
                4 -> {
                    player.mostDamageReceivedSourcePlayer?.let { killer ->
                        killer.removeDamage(player)
                        killer.increaseKillCount(player)
                        killer.incrementCount(KILLS_COUNTER)
                    }
                    player.incrementCount(DEATHS_COUNTER)
                    val activeGame = game?.takeIf { !it.ended && player.controllerManager.controller === this }
                    if (activeGame == null) {
                        player.anim(-1)
                        return@scheduleTimer false
                    }
                    player.inventory.getItemById(Obj.snp_tut_soul_fragment)?.let { fragments ->
                        World.addGroundItem(fragments, player.tile)
                        player.inventory.deleteItem(fragments)
                    }
                    World.addGroundItem(Item(Obj.snp_bones, 1), player.tile)
                    player.reset()
                    player.tele(activeGame.respawnTile(team, player.tile))
                    player.isCanPvp = false
                    player.anim(-1)
                    activity = MAX_ACTIVITY
                }
                5 -> {
                    player.jingle(DEATH_JINGLE)
                    return@scheduleTimer false
                }
            }
            true
        }
        return false
    }

    override fun process() {
        activity = if (player.inCombat()) min(activity + 10, MAX_ACTIVITY) else activity - 2
        player.vars.setVar(Varp.varp_1380, activity.coerceAtLeast(0))
        if (activity <= 0) {
            player.tele(team.exitArea.getRandomTile())
            player.controllerManager.forceStop()
            return
        }
        syncVarcs()
    }

    override fun canAttack(target: Entity) = when (target) {
        is SoulAvatar -> target.team == team.opponent
        is Player -> game?.teamOf(target) == team.opponent
        else -> true
    }

    override fun canHit(entity: Entity) = when (entity) {
        is SoulAvatar -> entity.attackableBy(player)
        is Player -> game?.teamOf(entity) == team.opponent
        else -> true
    }

    override fun login(): Boolean {
        player.tele(GAME_EXIT)
        return true
    }

    override fun canDepositItem(item: Item?): Boolean {
        player.sendMessage("You can't bank items here.")
        return false
    }

    override fun processTeleport(tele: Teleport) = false

    override fun logout(): Boolean {
        clearMinigameValues(player)
        game?.left(player)
        player.tile = GAME_EXIT
        return false
    }

    override fun onRemove() {
        clearMinigameValues(player)
        player.interfaceManager.removeOverlay()
        game?.left(player)
    }
}

private fun clearMinigameValues(player: Player) {
    player.isCanPvp = false
    checkAndDeleteFoodAndPotions(player)
    player.inventory.deleteItem(Obj.snp_bones, player.inventory.getNumberOf(Obj.snp_bones))
    player.inventory.deleteItem(Obj.snp_tut_soul_fragment, Int.MAX_VALUE)
    removeCape(player)
    player.reset()
}
