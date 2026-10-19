package com.rs.game.content.world.areas.ardougne

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.engine.dialogue.startConversation
import com.rs.engine.pathfinder.RouteEvent
import com.rs.engine.pathfinder.routeEntityToTile
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.achievements.AchievementSystemD
import com.rs.game.content.achievements.SetReward
import com.rs.game.content.quests.monksfriend.dialogues.BrotherCedricMonksFriendD
import com.rs.game.content.quests.monksfriend.dialogues.BrotherOmadMonksFriendD
import com.rs.game.content.skills.agility.Agility
import com.rs.game.content.skills.thieving.Thieving
import com.rs.game.content.world.areas.global.AgilityShortcuts
import com.rs.game.content.world.doors.Doors
import com.rs.game.ge.GE
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

private const val OPEN_ZMI_COLLECT_ATTR = "open_zmi_collect"
private const val SCORPIUS_TALKED_TO_ATTR = "spiritOfScorpiusTalkedTo"

private val ZMI_RUNE_BUTTONS = mapOf(
    Component.interface_619.component_619_28 to Obj.airrune,
    Component.interface_619.component_619_29 to Obj.mindrune,
    Component.interface_619.component_619_30 to Obj.waterrune,
    Component.interface_619.component_619_31 to Obj.earthrune,
    Component.interface_619.component_619_32 to Obj.firerune,
    Component.interface_619.component_619_33 to Obj.bodyrune,
    Component.interface_619.component_619_34 to Obj.cosmicrune,
    Component.interface_619.component_619_35 to Obj.chaosrune,
    Component.interface_619.component_619_36 to Obj.astralrune,
    Component.interface_619.component_619_37 to Obj.lawrune,
    Component.interface_619.component_619_38 to Obj.deathrune,
    Component.interface_619.component_619_39 to Obj.bloodrune,
    Component.interface_619.component_619_40 to Obj.naturerune,
    Component.interface_619.component_619_41 to Obj.soulrune
)

private val CLOCK_TOWER_DUNGEON_ENTRANCES = arrayOf(
    Tile.of(2566, 3242, 0), Tile.of(2566, 3231, 0), Tile.of(2569, 3231, 0), Tile.of(2566, 3227, 0),
    Tile.of(2569, 3227, 0), Tile.of(2572, 3227, 0), Tile.of(2621, 3261, 0)
)

private val CLOCK_TOWER_DUNGEON_EXITS = arrayOf(
    Tile.of(2566, 9642, 0), Tile.of(2572, 9631, 0), Tile.of(2566, 9631, 0), Tile.of(2566, 9627, 0),
    Tile.of(2569, 9627, 0), Tile.of(2572, 9627, 0), Tile.of(2576, 9655, 0), Tile.of(2621, 9661, 0)
)

@ServerStartupEvent
fun mapArdougne() {
    mapArdougneNpcs()
    mapArdougneDungeons()
    mapArdougneBuildings()
    mapArdougneStairs()
}

private fun mapArdougneNpcs() {
    onNpcClick(Npc.spirit_of_scorpius) { (player) -> talkToSpiritOfScorpius(player) }

    onNpcClick(Npc.brother_omad) { (player) -> player.startConversation(BrotherOmadMonksFriendD(player).start) }

    onNpcClick(Npc.brother_cedric) { (player) -> player.startConversation(BrotherCedricMonksFriendD(player).start) }

    onNpcClick(Npc.pmod_town_crier_east_ardoyne) { (player, npc) ->
        player.startConversation {
            npc(npc.id, HeadE.CHEERFUL, "Hello, what are you after?")
            options("What would you like to say?") {
                opExec("About the Achievement System...") { AchievementSystemD(player, npc.id, SetReward.ARDOUGNE_CLOAK) }
            }
        }
    }

    onNpcClick(Npc.rc_zmi_shylock) { (player, npc, option) ->
        when (option) {
            "Bank" -> openZmiBank(player, collect = false)
            "Collect" -> openZmiBank(player, collect = true)
            "Talk-to" -> player.startConversation {
                npc(npc.id, HeadE.CALM_TALK, "Well met, fellow adventurer! How can I help you?")
                player(HeadE.HAPPY_TALKING, "Who are you?")
                npc(npc.id, HeadE.CALM_TALK, "How frightfully rude of me, my dear " + player.getPronoun("chap", "lady") + ". My name is Eniola and I work for that most excellent enterprise, the Bank of Gielinor.")
                player(HeadE.HAPPY_TALKING, "If you work for the bank, what are you doing here?")
                npc(npc.id, HeadE.CALM_TALK, "My presence here is the start of a new enterprise of travelling banks.")
                npc(npc.id, HeadE.CALM_TALK, "I can give you bank services for a price...")
                player(HeadE.HAPPY_TALKING, "What price?")
                npc(npc.id, HeadE.CALM_TALK, "20 of any rune...")
                npc(npc.id, HeadE.CALM_TALK, "Would you like to bank or collect?")
                options("Choose an option:") {
                    opExec("Bank") {
                        player.closeInterfaces()
                        openZmiBank(player, collect = false)
                    }
                    opExec("Collect") {
                        player.closeInterfaces()
                        openZmiBank(player, collect = true)
                    }
                    op("Nevermind")
                }
            }
        }
    }

    onButtonClick(Interface.interface_619) { e ->
        ZMI_RUNE_BUTTONS[e.component]?.let { rune -> payZmiRunes(e.player, rune) }
    }

    onNpcClick(Npc.upassmage) { (player) ->
        player.sendOptionsDialogue("Buy an Iban's staff for 100,000 gold?") {
            opExec("Yes, I'll pay 100,000 gold for a staff.") {
                if (player.inventory.hasCoins(100000)) {
                    player.inventory.removeCoins(100000)
                    player.inventory.addItem(Obj.ibanstaff, 1)
                } else
                    player.sendMessage("You don't have enough money.")
            }
            op("No, that's too much.")
        }
    }

    onNpcClick(Npc.silk_merchant) { e ->
        if (e.opNum != 3)
            return@onNpcClick
        val player = e.player
        player.sendOptionsDialogue("Exchange your silk for 20gp each?") {
            opExec("Yes") {
                val number = player.inventory.getAmountOf(Obj.silk)
                player.inventory.deleteItem(Obj.silk, number)
                player.inventory.addCoins(20 * number)
            }
            op("No")
        }
    }
}

private fun mapArdougneDungeons() {
    onObjectClick(Loc.loc_26844, Loc.loc_26845) { (player, obj) ->
        if (obj.id == Loc.loc_26844)
            player.tele(Tile.of(3312, 4817, 0))
        else
            player.tele(Tile.of(3308, 4819, 0))
    }

    onObjectClick(Loc.slug2_ruin_entrance) { it.player.tele(Tile.of(2696, 9683, 0)) }

    onObjectClick(Loc.di2_slug2_dongeon_ruin_exit) { it.player.tele(Tile.of(2697, 3283, 0)) }

    onObjectClick(Loc.ladder_cellar, Loc.ladder_cellar_directional, tiles = CLOCK_TOWER_DUNGEON_ENTRANCES) { (player, obj) ->
        player.useLadder(Tile.of(obj.x, obj.y + 6399, 0))
    }

    onObjectClick(Loc.di2_ladder_base, tiles = CLOCK_TOWER_DUNGEON_EXITS) { (player, obj) ->
        player.useLadder(Tile.of(obj.x, obj.y - 6399, 0))
    }

    onObjectClick(Loc.ladder_from_cellar, Loc.ladder_cellar_directional) { (player, obj) ->
        player.ladder(Tile.of(player.x, player.y + if (obj.id == Loc.ladder_cellar_directional) 6400 else -6400, 0))
    }

    onObjectClick(Loc.manholeclosed, Loc.manholeopen) { (player, obj, option) ->
        if (option.equals("Open", ignoreCase = true)) {
            val openedHole = GameObject(obj.id + 1, obj.type, obj.rotation, obj.x, obj.y, obj.plane)
            player.faceObject(openedHole)
            World.spawnObjectTemporary(openedHole, Ticks.fromMinutes(1))
        }
        if (option.equals("Climb-Down", ignoreCase = true) && obj.tile.matches(Tile.of(2632, 3294, 0)))
            player.useStairs(Seq.human_reachforladdertop, Tile.of(2631, 9694, 0), 1, 2)
    }

    onObjectClick(Loc.di2_ladder_base) { (player, obj) ->
        if (obj.tile.matches(Tile.of(2632, 9694, 0)))
            player.ladder(Tile.of(2633, 3294, 0))
    }

    onObjectClick(Loc.ardougne_log_balance_left_sc, Loc.ardougne_log_balance_right_sc) { (player, obj) ->
        if (!Agility.hasLevel(player, 33))
            return@onObjectClick
        AgilityShortcuts.walkLog(player, player.transform(if (obj.id == Loc.ardougne_log_balance_right_sc) -4 else 4, 0, 0), 3)
    }


    onObjectClick(Loc.ladder_cellar, tiles = arrayOf(Tile.of(2677, 3405, 0))) { (player) ->
        player.useStairs(Seq.seq_827, player.transform(0, 6400, 0), 1, 2)
    }

    onObjectClick(Loc.di2_ladder_base, tiles = arrayOf(Tile.of(2677, 9805, 0))) { (player) ->
        player.useStairs(Seq.human_reachforladder, player.transform(0, -6400, 0), 1, 2)
    }

    onObjectClick(Loc.di2_ikov_darkstairsdown, tiles = arrayOf(Tile.of(2650, 9804, 0))) { (player) ->
        player.useStairs(-1, player.transform(-8, -41, 0), 1, 2)
    }

    onObjectClick(Loc.ikov_darkstairs, tiles = arrayOf(Tile.of(2638, 9763, 0))) { (player) ->
        player.useStairs(-1, player.transform(8, 41, 0), 1, 2)
    }

    onObjectClick(Loc.fai_lg_descending_stairs2) { (player) -> player.tele(player.transform(-3, 6400, 0)) }

    onObjectClick(Loc.di2_stairs_from_cellar, tiles = arrayOf(Tile.of(2717, 9773, 0))) { (player) ->
        player.tele(player.transform(3, -6400, 0))
    }

    onObjectClick(Loc.loc_8783) { (player) ->
        if (player.isQuestComplete(Quest.BIOHAZARD))
            player.ladder(Tile.of(2044, 4649, 0))
        else
            player.sendMessage("The trapdoor is bolted on the other side.")
    }

    onObjectClick(Loc.mourner_hideout_ladder1) { it.player.ladder(Tile.of(2543, 3327, 0)) }
}

private fun mapArdougneBuildings() {
    onObjectClick(Loc.area_ardougne_door2_locked) { e ->
        if (e.opNum == 1)
            e.player.sendMessage("This door is securely locked.")
        else
            e.player.handleOneWayDoor(e.obj, 1, 3)
    }

    onObjectClick(Loc.picklock4) { e ->
        if (e.opNum == 1)
            e.player.sendMessage("This door is securely locked.")
        else
            Doors.handleDoor(e.player, e.obj)
    }

    onObjectClick("Legends Guild door") { it.player.handleOneWayDoor(it.obj, 1, 3) }

    onObjectClick(Loc.ranging_guild_door, checkDistance = false) { (player, obj) ->
        if (player.skills.getLevelForXp(Constants.RANGE) <= 40) {
            player.sendMessage("You require 40 Ranged to enter the Ranging Guild.")
            return@onObjectClick
        }
        val southEast = Tile.of(2659, 3437, 0)
        val northWest = Tile.of(2657, 3439, 0)
        val route = routeEntityToTile(player, southEast)
        val (approach, exit) = if (route.success && !route.alternative) southEast to northWest else northWest to southEast
        player.setRouteEvent(RouteEvent(approach) {
            Doors.handleDoor(player, obj)
            player.addWalkSteps(exit, 5, false)
        })
    }

    onObjectClick(Loc.loc_2567, Loc.loc_2568) { e ->
        if (e.opNum == 2)
            Thieving.checkTrapsChest(e.player, e.obj, Loc.loc_2574, 28, 21, 25.0, Item(Obj.coins, 3), Item(Obj.naturerune, 1))
    }

    onObjectClick(Loc.loc_2569) { e ->
        if (e.opNum == 2)
            Thieving.checkTrapsChest(e.player, e.obj, Loc.loc_2574, 59, 210, 250.0, Item(Obj.coins, 500), Item(Obj.bloodrune, 2))
    }

    onObjectClick(Loc.loc_2566) { e ->
        if (e.opNum == 2)
            Thieving.checkTrapsChest(e.player, e.obj, Loc.loc_2574, 43, 50, 125.0, Item(Obj.coins, 50))
    }

    onObjectClick(Loc.chestclosed) { e ->
        if (e.opNum != 1)
            return@onObjectClick
        val obj = e.obj
        val openedChest = GameObject(Loc.chestopen, obj.type, obj.rotation, obj.x, obj.y, obj.plane)
        e.player.faceObject(openedChest)
        e.player.anim(Seq.human_openchest)
        e.player.lock(2)
        World.spawnObjectTemporary(openedChest, Ticks.fromSeconds(30))
    }

    onObjectClick(Loc.chestopen) { e ->
        when (e.opNum) {
            2 -> {
                e.player.sendMessage("You search the chest but find nothing.")
                e.player.faceObject(e.obj)
            }
            3 -> {
                e.player.faceObject(e.obj)
                e.player.anim(Seq.human_openchest)
                e.player.lock(2)
                World.removeObject(e.obj)
            }
        }
    }

    onObjectClick(Loc.loc_19171) { (player) ->
        if (!Agility.hasLevel(player, 20))
            return@onObjectClick
        player.useStairs(-1, Tile.of(if (player.x >= 2523) 2522 else 2523, 3375, 0), 1, 2, "You easily squeeze through the railing.")
    }
}

private fun mapArdougneStairs() {
    onObjectClick(Loc.fai_lg_stairs_bottom, Loc.fai_lg_stairs_top) { (player, obj) ->
        if (obj.id == Loc.fai_lg_stairs_bottom)
            player.tele(player.transform(0, if (obj.rotation == 0) 4 else 0, 1))
        else
            player.tele(player.transform(0, when (obj.rotation) { 0 -> -4; 3 -> 3; else -> 0 }, -1))
    }

    onObjectClick(Loc.tol_stairs01, Loc.tol_gapfill01) { (player, obj) ->
        val dx = if (obj.rotation <= 1) -2 else 0
        val dy = when (obj.rotation) { 0 -> -2; 1 -> 2; else -> 0 }
        if (obj.id == Loc.tol_stairs01)
            player.tele(player.transform(dx, dy, 1))
        else
            player.tele(player.transform(-dx, -dy, -1))
    }

    onObjectClick(Loc.area_sanguine_ghetto_ladder_up, Loc.area_sanguine_ghetto_ladder_down) { (player, obj) ->
        val dx = when (obj.rotation) { 1 -> 2; 3 -> -2; else -> 0 }
        val dy = when (obj.rotation) { 2 -> -2; 0 -> 2; else -> 0 }
        if (obj.id == Loc.area_sanguine_ghetto_ladder_up)
            player.tele(player.transform(dx, dy, 1))
        else
            player.tele(player.transform(-dx, -dy, -1))
    }

    onObjectClick(Loc.carni_mansion_stairs_bottom) { it.player.tele(Tile.of(2569, 3267, 1)) }
    onObjectClick(Loc.carni_mansion_stairs_top) { it.player.tele(Tile.of(2568, 3269, 0)) }
    onObjectClick(Loc.area_ardougne_trapdoor_ladder_base) { it.player.tele(Tile.of(2569, 3268, 0)) }
    onObjectClick(Loc.area_ardougne_trapdoor_open) { it.player.tele(Tile.of(2569, 9668, 0)) }

    onObjectClick(Loc.luc2_fa_stairs, Loc.luc2_fa_stairs_top) { (player, obj) ->
        climbStraightStairs(player, obj, Loc.luc2_fa_stairs)
    }

    onObjectClick(Loc.area_ardougne_run_down_spiralstairs, Loc.area_ardougne_run_down_spiralstairs_top) { (player, obj) ->
        val dx = when (obj.rotation) { 0 -> 3; 2 -> -3; else -> 0 }
        val dy = when (obj.rotation) { 3 -> 3; 1 -> -3; else -> 0 }
        if (obj.id == Loc.area_ardougne_run_down_spiralstairs)
            player.tele(player.transform(dx, dy, 1))
        else
            player.tele(player.transform(-dx, -dy, -1))
    }

    onObjectClick(Loc.area_ardougne_run_down_stairs, Loc.area_ardougne_run_down_stairs_top) { (player, obj) ->
        climbStraightStairs(player, obj, Loc.area_ardougne_run_down_stairs)
    }

    onObjectClick(Loc.spiralstairs, Loc.spiralstairstop) { (player, obj) ->
        player.tele(player.transform(0, 0, if (obj.id == Loc.spiralstairs) 1 else -1))
    }

    onObjectClick(Loc.outdoorstairs_wooden_bottom_gloffset) { it.player.tele(Tile.of(2517, 3426, 1)) }
    onObjectClick(Loc.outdoorstairs_wooden_top_gloffset) { it.player.tele(Tile.of(2516, 3423, 0)) }

    onObjectClick(Loc.area_ardougne_posh_spiralstairs) { (player, obj) ->
        player.useStairs(-1, player.tile.ahead(obj.rotation + 1, 3, 1), 1, 1)
    }

    onObjectClick(Loc.area_ardougne_posh_spiralstairs_top) { (player, obj) ->
        player.useStairs(-1, player.tile.ahead(obj.rotation + 1, -3, -1), 1, 1)
    }

    onObjectClick(Loc.area_ardougne_poor_stairs) { (player, obj) ->
        player.useStairs(-1, player.transform(if (obj.rotation == 3) -3 else 3, 0, 1), 1, 1)
    }

    onObjectClick(Loc.area_ardougne_poor_stairs_top) { (player, obj) ->
        player.useStairs(-1, player.transform(if (obj.rotation == 3) 3 else -3, 0, -1), 1, 1)
    }

    onObjectClick(Loc.area_ardougne_basic_stairs) { (player, obj) ->
        player.useStairs(-1, player.tile.ahead(obj.rotation, 3, 1), 1, 1)
    }

    onObjectClick(Loc.area_ardougne_basic_stairs_top) { (player, obj) ->
        player.useStairs(-1, player.tile.ahead(obj.rotation, -3, -1), 1, 1)
    }
}

private fun talkToSpiritOfScorpius(player: Player) {
    val spirit = Npc.spirit_of_scorpius
    val observatoryDone = player.isQuestComplete(Quest.OBSERVATORY_QUEST, "to speak to Scorpius")
    player.startConversation {
        if (player.equipment.amuletId != Obj.amulet_of_ghostspeak)
            simple("This powerful spirit seems capable of speaking to you even though you are not wearing an Amulet of Ghostspeak.")
        npc(spirit, HeadE.OLD_CHAT, if (observatoryDone) "Who treads upon my grave?" else "How dare you disturb me!")
        if (observatoryDone) options {
            if (!player.getBool(SCORPIUS_TALKED_TO_ATTR)) {
                op("I seek your wisdom.") {
                    player(HeadE.CALM_TALK, "I seek your wisdom.")
                    npc(spirit, HeadE.OLD_CHAT, "Indeed, I feel you have beheld the far places in the heavens.")
                    npc(spirit, HeadE.OLD_CHAT, "My Lord instructs me to help you.")
                    npc(spirit, HeadE.OLD_CHAT, "Here is a mould to make a token for our Lord;a mould for the unholy symbol of Zamorak.")
                    item(Obj.unholy_symbol_mould, "The ghost gives you a casting mould.") {
                        player.set(SCORPIUS_TALKED_TO_ATTR, true)
                        player.inventory.addItemDrop(Obj.unholy_symbol_mould, 1)
                    }
                }
            } else {
                op("I have come to seek a blessing.") {
                    if (!player.inventory.containsItem(Obj.stringsnake)) {
                        npc(spirit, HeadE.OLD_CHAT, "No blessings will be given to those who have no symbol of our Lord's love.")
                    } else {
                        npc(spirit, HeadE.OLD_CHAT, "I see you have the unholy symbol of our Lord. I will bless it for you.")
                        simple("The ghost mutters in a strange voice.")
                        simple("The unholy symbol throbs with power.") { player.inventory.replace(Obj.stringsnake, Obj.blessedsnake) }
                        npc(spirit, HeadE.OLD_CHAT, "The symbol of our Lord has been blessed with power!")
                        npc(spirit, HeadE.OLD_CHAT, "My master calls.")
                    }
                }
                op("I need another unholy symbol mould.") {
                    if (player.inventory.containsItem(Obj.unholy_symbol_mould)) {
                        npc(spirit, HeadE.OLD_CHAT, "One you already have, another is not needed.")
                    } else {
                        npc(spirit, HeadE.OLD_CHAT, "To lose an object is easy to replace. To lose the affections of our Lord is impossible to forgive.")
                        item(Obj.unholy_symbol_mould, "The ghost hands you another mould.") { player.inventory.addItemDrop(Obj.unholy_symbol_mould, 1) }
                    }
                }
            }
            op("I have come to kill you.") {
                player(HeadE.ANGRY, "I have come to kill you.")
                npc(spirit, HeadE.OLD_CHAT, "The might of mortals, to me, is as the dust to the sea.")
            }
        }
    }
}

private fun openZmiBank(player: Player, collect: Boolean) {
    if (!player.bank.checkPin())
        return
    player.tempAttribs.setB(OPEN_ZMI_COLLECT_ATTR, collect)
    player.interfaceManager.sendInterface(Interface.interface_619)
}

private fun payZmiRunes(player: Player, rune: Int) {
    val runes = Item(rune, 20)
    if (!player.inventory.containsItems(runes)) {
        player.sendMessage("You don't have enough " + runes.name + "s!")
        player.closeInterfaces()
        return
    }
    player.inventory.removeItems(runes)
    if (player.tempAttribs.getB(OPEN_ZMI_COLLECT_ATTR))
        GE.openCollection(player)
    else
        player.bank.open()
}

private fun climbStraightStairs(player: Player, obj: GameObject, bottom: Int) {
    val dx = when (obj.rotation) { 1 -> 3; 3 -> -3; else -> 0 }
    val dy = if (obj.rotation == 2) -3 else 0
    if (obj.id == bottom)
        player.tele(player.transform(dx, dy, 1))
    else
        player.tele(player.transform(-dx, -dy, -1))
}

private fun Tile.ahead(rotation: Int, distance: Int, planeOffset: Int): Tile = when (rotation and 0x3) {
    0 -> transform(0, distance, planeOffset)
    1 -> transform(distance, 0, planeOffset)
    2 -> transform(0, -distance, planeOffset)
    else -> transform(-distance, 0, planeOffset)
}
