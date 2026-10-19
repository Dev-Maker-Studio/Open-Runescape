package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.Settings
import com.rs.engine.command.Commands
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.engine.quest.SeasonalQuests
import com.rs.game.World
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.model.ObjectShape
import org.darkan.core.model.Rights
import world.gregs.voidps.type.Tile

private const val ENABLED = true

var halloween2010Open = false
    private set

val OUTSIDE_PORTAL: Tile = Tile.of(3211, 3425, 0)
val OUTSIDE_ARRIVAL: Tile = Tile.of(3212, 3424, 0)
val OUTSIDE_PETE: Tile = Tile.of(3210, 3425, 0)

private class Decoration(val loc: Int, x: Int, y: Int, val shape: ObjectShape = ObjectShape.SCENERY_INTERACT, val rotation: Int = 0, val blocks: Boolean = true) {
    val tile: Tile = Tile.of(x, y, 0)
}

private val VARROCK_DECORATIONS = listOf(
    Decoration(Loc.hw10_portal, OUTSIDE_PORTAL.x, OUTSIDE_PORTAL.y),
    Decoration(Loc.hw11_pumpkin_torch, 3215, 3421, ObjectShape.STRAIGHT_OUSIDE_WALL_DEC, 3),
    Decoration(Loc.hw11_pumpkin_torch, 3219, 3421, ObjectShape.STRAIGHT_OUSIDE_WALL_DEC, 3),
    Decoration(Loc.hw10_pumpkin_03, 3215, 3421, rotation = 2),
    Decoration(Loc.hw10_pumpkin_04, 3219, 3421, rotation = 2),
    Decoration(Loc.hw10_pumpkin_02, 3215, 3426, ObjectShape.GROUND_INTERACT, 3),
    Decoration(Loc.hw10_pumpkin_01, 3215, 3431, ObjectShape.GROUND_INTERACT, 2),
    Decoration(Loc.hw10_pumpkin_02, 3210, 3431, ObjectShape.GROUND_INTERACT, 1),
    Decoration(Loc.hw11_pumpkin_torch, 3213, 3419, ObjectShape.STRAIGHT_OUSIDE_WALL_DEC, 2),
    Decoration(Loc.hw11_pumpkin_torch, 3209, 3416, ObjectShape.STRAIGHT_OUSIDE_WALL_DEC, 0),
    Decoration(Loc.hw10_decorations_end_01_mirror, 3200, 3430, rotation = 1, blocks = false),
    Decoration(Loc.hw10_decorations_end_02, 3203, 3430, rotation = 1, blocks = false),
    Decoration(Loc.hw10_decorations_single_02, 3216, 3438, rotation = 1, blocks = false),
    Decoration(Loc.hw10_pumpkin_03, 3219, 3438),
    Decoration(Loc.hw10_pumpkin_04, 3207, 3422, rotation = 3),
    Decoration(Loc.hw10_pumpkin_05, 3207, 3425, rotation = 3),
    Decoration(Loc.hw11_wood_gravestone1, 3223, 3437, rotation = 1),
    Decoration(Loc.hw11_plain_gravestone1, 3225, 3437, rotation = 1),
    Decoration(Loc.hw11_ornate_gravestone1, 3225, 3435, rotation = 1),
    Decoration(Loc.hw11_flag_gravestone1, 3225, 3433, rotation = 1),
    Decoration(Loc.hw11_stele_gravestone1, 3223, 3432, rotation = 1),
    Decoration(Loc.hw10_pumpkin_04, 3224, 3432),
    Decoration(Loc.hw11_angel_gravestone1, 3224, 3433, rotation = 1),
    Decoration(Loc.hw11_saradomin_gravestone1, 3224, 3437, rotation = 1),
    Decoration(Loc.hettycauldron_normal, 3224, 3431),
    Decoration(Loc.hw11_dwarf_gravestone1, 3222, 3426, rotation = 1),
    Decoration(Loc.hw11_font_gravestone1, 3224, 3426, rotation = 1),
    Decoration(Loc.hw11_wood_gravestone1, 3223, 3427, rotation = 1),
    Decoration(Loc.hw11_zamorak_gravestone1, 3225, 3425, rotation = 1),
    Decoration(Loc.hw11_guthix_gravestone1, 3222, 3421, rotation = 1),
    Decoration(Loc.hw11_armadyl_gravestone1, 3223, 3422, rotation = 1),
    Decoration(Loc.hw11_zaros_gravestone1, 3224, 3421, rotation = 1),
    Decoration(Loc.hw11_bandos_gravestone1, 3225, 3423, rotation = 1),
    Decoration(Loc.hw11_stele_gravestone1, 3200, 3427, rotation = 3),
    Decoration(Loc.hw11_flag_gravestone1, 3200, 3421, rotation = 3),
    Decoration(Loc.hettycauldron_normal, 3207, 3426),
)

private val spawnedLocs = mutableListOf<GameObject>()
private var outsidePete: NPC? = null

@ServerStartupEvent(ServerStartupEvent.Priority.POST_PROCESS)
fun openHalloween2010OnStartup() {
    if (ENABLED)
        openHalloween2010()
}

@ServerStartupEvent
fun mapHalloween2010() {
    onObjectClick(Loc.hw10_portal, tiles = arrayOf(OUTSIDE_PORTAL)) { (player) ->
        if (!halloween2010Open)
            return@onObjectClick
        if (player.hw10Stage == Hw10Stage.NOT_STARTED)
            player.startConversation { player(HeadE.CONFUSED, "It's probably best not to just run randomly through portals. I should speak to the portal owner first.") }
        else
            player.enterHw10House()
    }

    onLogin { (player) ->
        player.syncHw10Edition()
        if (player.hw10Stage != Hw10Stage.PARTY)
            return@onLogin
        if (player.inHw10House && !player.inPartyHouse)
            player.tele(PARTY_DANCE_FLOOR)
        completeHalloween2010(player)
    }

    val rights = if (Settings.getConfig().isDebug) Rights.PLAYER else Rights.ADMIN
    Commands.add(rights, "hw10 [open|close|portal|house|stage (n)|tasks|finale|reset|hardreset|teaser (all)]", "Halloween 2010 event tools.") { p, args ->
        when (args.getOrNull(0)) {
            "open" -> openHalloween2010()
            "close" -> closeHalloween2010()
            "portal" -> p.tele(OUTSIDE_ARRIVAL)
            "house" -> p.enterHw10House()
            "stage" -> p.hw10Stage = Hw10Stage.of(args.getOrNull(1)?.toIntOrNull() ?: 0)
            "tasks" -> {
                p.werewolfState = WerewolfState.MOONSHINE_DELIVERED
                p.grimState = GrimState.AUTOGRAPH_DELIVERED
                p.solveSpiderPuzzle()
                p.webState = WebState.SOLVED
            }
            "finale" -> {
                if (!p.inHw10House)
                    p.enterHw10House()
                playPartyCutscene(p)
            }
            "reset" -> p.resetHw10()
            "hardreset" -> p.sendMessage(p.hardResetHw10())
            "teaser" -> if (args.getOrNull(1) == "all") World.players.filter { !it.isLocked }.forEach { it.playHw10Teaser() } else p.playHw10Teaser()
            else -> p.sendMessage("Usage: ::hw10 open | close | portal | house | stage <0|1|2|10|20> | tasks | finale | reset | hardreset | teaser [all]")
        }
        p.sendMessage("Halloween 2010: open=$halloween2010Open, stage=${p.hw10Stage}, grim=${p.grimState}, web=${p.webState}, werewolves=${p.werewolfState}, spiders found=${p.spidersFound}")
    }
}

fun openHalloween2010() {
    if (halloween2010Open)
        return
    halloween2010Open = true
    for (decoration in VARROCK_DECORATIONS) {
        val loc = GameObject(decoration.loc, decoration.shape, decoration.rotation, decoration.tile)
        World.spawnObject(loc, decoration.blocks)
        spawnedLocs += loc
    }
    outsidePete = World.spawnNPC(Npc.hw10_pumpkin_pete, OUTSIDE_PETE, false, true).apply { setRandomWalk(false) }
    spawnTrickOrTreaters()
    SeasonalQuests.enable(Quest.HALLOWEEN_2010)
}

fun closeHalloween2010() {
    if (!halloween2010Open)
        return
    halloween2010Open = false
    spawnedLocs.forEach(World::removeObject)
    spawnedLocs.clear()
    outsidePete?.finish()
    outsidePete = null
    removeTrickOrTreaters()
    SeasonalQuests.disable(Quest.HALLOWEEN_2010)
}
