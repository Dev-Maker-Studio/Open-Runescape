package com.rs.game.content.quests.deserttreasure

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.pathfinder.RouteEvent
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.model.entity.Entity
import org.darkan.core.map.pathfinder.Direction
import com.rs.game.content.Effect
import com.rs.game.content.skills.agility.Agility
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.interactions.PlayerCombatInteraction
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.kts.onChunkEnter
import org.darkan.core.Constants
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

private val TROLL_CHILD_TILE = Tile.of(2835, 3740, 0)

private val FATHER_ICE_TILE = Tile.of(2825, 3808, 2)
private val MOTHER_ICE_TILE = Tile.of(2825, 3810, 2)

private val SWEET_FOOD = intArrayOf(Obj.cake, Obj.partial_cake, Obj.cake_slice, Obj.chocolate_cake, Obj.partial_chocolate_cake, Obj.chocolate_slice)

private const val CHILD_CHEERED = "dtIceChildCheered"
private const val ICE_DEAL_ACCEPTED = "dtIceDealAccepted"
private const val ICE_FATHER_FREED = "dtIceFatherFreed"
private const val ICE_MOTHER_FREED = "dtIceMotherFreed"
private const val ICE_DIAMOND_CLAIMED = "dtIceDiamondClaimed"
private const val ICE_FATHER_THANKED = "dtIceFatherThanked"
private const val ICE_MOTHER_THANKED = "dtIceMotherThanked"
private const val ICE_PARENTS_REUNITED = "dtIceParentsReunited"
private const val CHILD_TOLD_ABOUT_GATE = "dtIceChildToldAboutGate"
private const val KAMIL_SPAWNED = "dtIceKamilSpawned"

@ServerStartupEvent
fun mapIceDiamondDesertTreasure() {
    instantiateNpc(Npc.fourdiamonds_troll_child_crying) { id, tile ->
        object : NPC(id, tile, false) {
            override fun withinDistance(player: Player, distance: Int): Boolean =
                !hasHappyTrollChild(player) && super.withinDistance(player, distance)
        }
    }
    onNpcClick(Npc.fourdiamonds_troll_child_crying, Npc.fourdiamonds_troll_child_okay, options = arrayOf("Talk-to")) { (player) ->
        if (isSearchingForIceDiamond(player) || hasHappyTrollChild(player)) trollChildDialogue(player)
    }
    onItemOnNpc(Npc.fourdiamonds_troll_child_crying, Npc.fourdiamonds_troll_child_okay) { (player, item) ->
        if (!isSearchingForIceDiamond(player)) return@onItemOnNpc
        if (hasHappyTrollChild(player)) {
            trollChildDialogue(player)
            return@onItemOnNpc
        }
        if (item.id in SWEET_FOOD && !player.getBool(CHILD_CHEERED)) {
            player.inventory.deleteItem(item.id, 1)
            player.set(CHILD_CHEERED, true)
            spawnHappyTrollChild(player)
            player.startConversation {
                player(CALM_TALK, "Hey there little troll... Take this and dry those tears...")
                npc(Npc.fourdiamonds_troll_child_crying, SAD, "(sniff)")
            }
        } else {
            player.startConversation {
                npc(Npc.fourdiamonds_troll_child_crying, SAD, "WAAAAAAAA!")
                simple("It doesn't look like that will cheer him up at all...")
            }
        }
    }
    onNpcClick(Npc.fd_troll_dad) { (player) -> if (isSearchingForIceDiamond(player) || player.getBool(ICE_PARENTS_REUNITED)) trollParentDialogue(player, true) }
    onNpcClick(Npc.fd_troll_mum) { (player) -> if (isSearchingForIceDiamond(player)) trollParentDialogue(player, false) }
}

private fun trollChildDialogue(player: Player) {
    if (hasPendingIceDiamondReunion(player)) {
        reuniteTrollParents(player)
        return
    }
    val childId = if (hasHappyTrollChild(player)) Npc.fourdiamonds_troll_child_okay else Npc.fourdiamonds_troll_child_crying
    player.startConversation {
        when {
            isSearchingForIceDiamond(player) && player.getBool(ICE_DIAMOND_CLAIMED) && !player.containsItem(Obj.fd_icediamond) -> {
                player(CALM_TALK, "I lost that diamond of Ice you gave me...")
                item(Obj.fd_icediamond, "You receive the Ice Diamond.") {
                    DesertTreasureDiamonds.giveIceDiamond(player)
                }
                npc(childId, HAPPY_TALKING, "That's okay, it blew back on an icy wind... It's almost like it wants to stay here! Here, take it back, you've earned it.")
            }
            (player.getBool(ICE_FATHER_FREED) && player.getBool(ICE_MOTHER_FREED)) || player.isQuestComplete(Quest.DESERT_TREASURE) -> {
                npc(childId, HAPPY_TALKING, "Thanks for all your help! I'm surprised you managed to survive the blizzard, being a thin skinned fleshy and all!")
                player(CALM_TALK, "What can I say? I'm a lot tougher than I look.")
            }
            !player.getBool(CHILD_CHEERED) -> {
                player(CALM_TALK, "Hello there.")
                npc(childId, SAD, "Waaaaaaa!")
                simple("This troll seems very upset about something. Maybe some sweet food would take his mind off things?")
            }
            !player.getBool(ICE_DEAL_ACCEPTED) -> childDeal(player)
            else -> {
                npc(childId, SAD, "You didn't free my mommy and daddy yet?")
                player(CALM_TALK, "Not yet...")
                npc(childId, SAD, "Please try harder! I love my mommy and daddy!")
            }
        }
    }
}

private fun DialogueBuilder.childDeal(player: Player) {
    player(CALM_TALK, "Hello there.")
    npc(Npc.fourdiamonds_troll_child_crying, SAD, "-sniff- H-hello there.")
    player(CONFUSED, "Why so sad, little troll?")
    npc(Npc.fourdiamonds_troll_child_crying, SAD, "It was the bad man! He hurt my mommy and daddy! He made them all freezey!")
    player(CONFUSED, "Bad man...?")
    npc(Npc.fourdiamonds_troll_child_crying, SAD, "He said it was because they stole his diamond! But they never did! They found it, and didn't know who it belonged to!")
    npc(Npc.fourdiamonds_troll_child_crying, SAD, "My mommy always told me stealing is wrong, they would never steal from someone!")
    npc(Npc.fourdiamonds_troll_child_crying, SAD, "Then he did some wavey hand thing and my mommy and daddy got frozified!")
    player(CONFUSED, "A diamond you say? Listen, I think I might be able to help your parents, but I need that Diamond in return.")
    npc(Npc.fourdiamonds_troll_child_crying, SAD, "-sniff- I don't think they really wanted it anyway, they would have given it back to the bad man if he'd asked before freezifying them...")
    npc(Npc.fourdiamonds_troll_child_crying, SAD, "I give you my promise ${player.genderTerm("mister", "lady")} that if you unfreeze my mommy and daddy, you can have the stupid diamond.")
    npc(Npc.fourdiamonds_troll_child_crying, CONFUSED, "Do we have a deal?")
    options {
        op("Yes") {
            player(CALM_TALK, "Absolutely. Don't worry kid, I'll get your parents back to you safe and sound.")
            exec { player.set(ICE_DEAL_ACCEPTED, true) }
        }
        op("No") {
            player(SAD, "Sorry, I can't make promises any promises about that, and I don't think I have the time to waste trying to defrost some stupid ice trolls.")
            npc(Npc.fourdiamonds_troll_child_crying, SAD, "Waaaaaaa!")
        }
    }
}

private fun trollParentDialogue(player: Player, father: Boolean) {
    if (hasPendingIceDiamondReunion(player)) {
        reuniteTrollParents(player)
        return
    }
    val npcId = if (father) Npc.fd_troll_dad else Npc.fd_troll_mum
    val freed = if (father) player.getBool(ICE_FATHER_FREED) else player.getBool(ICE_MOTHER_FREED)
    val thankedFlag = if (father) ICE_FATHER_THANKED else ICE_MOTHER_THANKED
    player.startConversation {
        if (freed && player.getBool(ICE_FATHER_FREED) && player.getBool(ICE_MOTHER_FREED)) {
            if (!player.getBool(thankedFlag)) {
                npc(
                    npcId,
                    HAPPY_TALKING,
                    if (father)
                        "Thanks again for freeing me from that ice block! I might be a troll, but it was real uncomfortable in there!"
                    else
                        "Thanks again for freeing me from that ice block! I don't know what my little snookums would have done without us!"
                )
                exec { player.set(thankedFlag, true) }
            } else if (father) {
                player(CONFUSED, "Why did you come back up here?")
                npc(Npc.fd_troll_dad, HAPPY_TALKING, "I like the view!")
            } else {
                player(CONFUSED, "What are you doing up here?")
                npc(Npc.fd_troll_mum, HAPPY_TALKING, "I think I lost an ear ring up here earlier!")
            }
        } else {
            simple("There is a thick layer of ice covering this troll. You will have to find some way of shattering it.")
        }
    }
}

private val ICE_GATE_IDS = arrayOf(Loc.icegate_left, Loc.icegate_right)
private val ICE_GATE_TILES = arrayOf(Tile.of(2838, 3736, 0))
private val ICE_GATE_EXTERIOR_TILE = Tile.of(2837, 3738, 0)
private val ICE_GATE_INTERIOR_TILE = Tile.of(2839, 3738, 0)

private val ICE_CAVE_IDS = arrayOf(Loc.loc_6440, Loc.trollrescue_troll_cave_entrance_multi, Loc.trollrescue_troll_cave_entrance1, Loc.trollrescue_troll_cave_entrance2, Loc.trollrescue_troll_cave_entrance3, Loc.trollrescue_troll_cave_entrance4, Loc.trollrescue_troll_cave_entrance5)
private val ICE_CAVE_ENTRANCE_TILES = arrayOf(Tile.of(2868, 3718, 0))
private val ICE_CAVE_EXIT_OBJECT_TILE = Tile.of(2872, 3719, 0)
private val ICE_CAVE_EXIT_TILE = Tile.of(2874, 3720, 0)
private val KAMIL_AMBUSH_TILES = (2858..2874).flatMap { x -> (3748..3762).map { y -> Tile.of(x, y, 0) } }.toTypedArray()
private val KAMIL_SPAWN_TILE = Tile.of(2868, 3755, 0)
private val ICE_LEDGE_TILES = arrayOf(Tile.of(2837, 3804, 0), Tile.of(2838, 3804, 0), Tile.of(2837, 3804, 1), Tile.of(2838, 3804, 1))
private val ICE_LEDGE_APPROACH_TILE = Tile.of(2838, 3805, 0)
private val ICE_LEDGE_PATH_TILE = Tile.of(2838, 3803, 1)

private const val ICE_TROLLS_KILLED = "dtIceTrollsKilled"
private const val ICE_TROLLS_REQUIRED = 5
private const val KAMIL_DEFEATED = "dtIceKamilDefeated"

private fun syncIceTrollCave(player: Player) {
    val cleared = when {
        player.isQuestComplete(Quest.DESERT_TREASURE) -> ICE_TROLLS_REQUIRED
        player.getQuestStage(Quest.DESERT_TREASURE) < STAGE_DIAMONDS_OF_AZZANADRA -> 0
        else -> player.getI(ICE_TROLLS_KILLED, 0).coerceIn(0, ICE_TROLLS_REQUIRED)
    }
    player.vars.setVarBit(Varbit.fd_icewarrior_trollskilled, cleared)
}

@ServerStartupEvent
fun mapIceTrollCaveAndKamil() {
    onChunkEnter { (entity, _) ->
        val player = entity as? Player ?: return@onChunkEnter
        syncIceTrollCave(player)
        spawnHappyTrollChild(player)
        startIcePathCold(player)
        if (player.plane == 2 && isOnIcePath(player)) spawnIceParentBlocks(player)
    }
    onLogin { (player) ->
        syncIceTrollCave(player)
        if (player.getBool(ICE_COLD_RUN_BLOCK)) {
            player.unblockRun()
            player.set(ICE_COLD_RUN_BLOCK, false)
        }
        spawnHappyTrollChild(player)
        startIcePathCold(player)
        if (player.plane == 2 && isOnIcePath(player)) spawnIceParentBlocks(player)
        if (player.getBool(KAMIL_SPAWNED) && !player.getBool(KAMIL_DEFEATED)) {
            player.set(KAMIL_SPAWNED, false)
        }
    }

    onObjectClick(*ICE_GATE_IDS, tiles = ICE_GATE_TILES) { (player, obj) ->
        val inside = player.x > obj.x
        if (!inside) {
            if (!isSearchingForIceDiamond(player)) return@onObjectClick
            if (!player.getBool(ICE_DEAL_ACCEPTED)) {
                player.sendMessage("The bars are frozen tightly shut and a sturdy layer of ice prevents you from slipping through.")
                return@onObjectClick
            }
            if (!player.getBool(CHILD_TOLD_ABOUT_GATE)) {
                player.set(CHILD_TOLD_ABOUT_GATE, true)
                player.startConversation {
                    npc(Npc.fourdiamonds_troll_child_crying, SAD, "Oh, I'm sorry. My mommy says, that we ice trolls are so cold we can freeze the air around us when we are upset or angry.")
                    npc(Npc.fourdiamonds_troll_child_crying, SAD, "I'm afraid I must have frozen those gates too much when I was crying, but since you told me you are going to help rescue my mommy and daddy you should be able to squeeze through the bars.")
                    npc(Npc.fourdiamonds_troll_child_crying, SAD, "You are much smaller than most trolls, it should be easy for you!")
                }
                return@onObjectClick
            }
        }
        player.sendMessage("You squeeze through the large icy bars of the gate.")
        player.forceMove(if (inside) ICE_GATE_EXTERIOR_TILE else ICE_GATE_INTERIOR_TILE, Seq.human_doublepipesqueeze, 10, 90) {
            if (!inside) startIcePathCold(player)
        }
    }

    onObjectClick(Loc.trollrescue_troll_cave_exit, tiles = arrayOf(ICE_CAVE_EXIT_OBJECT_TILE)) { (player) ->
        if (!isSearchingForIceDiamond(player) || !player.getBool(ICE_DEAL_ACCEPTED)) return@onObjectClick
        player.tele(Tile.of(2867, 3718, 0))
    }

    onNpcClick(Npc.i_troll_guard) { (player) ->
        if (!isSearchingForIceDiamond(player) || !player.getBool(ICE_DEAL_ACCEPTED)) return@onNpcClick
        player.startConversation {
            npc(Npc.i_troll_guard, ANGRY, "Hur hur hur! Well look here, a puny fleshy human!")
            npc(Npc.i_troll_guard, ANGRY, "You should beware of the icy wind that runs through this valley, it will bring a fleshy like you to a cold end indeed!")
        }
    }

    onObjectClick(*ICE_CAVE_IDS, tiles = ICE_CAVE_ENTRANCE_TILES) { (player) ->
        if (!isSearchingForIceDiamond(player) || !player.getBool(ICE_DEAL_ACCEPTED)) return@onObjectClick
        val trollsKilled = player.getI(ICE_TROLLS_KILLED, 0)
        if (trollsKilled < ICE_TROLLS_REQUIRED) {
            player.sendMessage("The entrance to the cave is covered in too much ice to get through.")
            return@onObjectClick
        }
        player.tele(ICE_CAVE_EXIT_TILE)
    }

    onNpcDeath(
        Npc.i_troll_guard,
        Npc.trollrescue_icetroll_melee1,
        Npc.trollrescue_icetroll_melee2,
        Npc.trollrescue_icetroll_melee3,
        Npc.trollrescue_icetroll_melee4,
        Npc.trollrescue_icetroll_melee5,
        Npc.trollrescue_icetroll_melee6,
        Npc.trollrescue_icetroll_melee7
    ) { (killer, npc) ->
        val player = killer as? Player ?: return@onNpcDeath
        if (!isSearchingForIceDiamond(player) || !player.getBool(ICE_DEAL_ACCEPTED)) return@onNpcDeath
        val trollsKilled = player.getI(ICE_TROLLS_KILLED, 0)
        if (trollsKilled >= ICE_TROLLS_REQUIRED) return@onNpcDeath
        val newCount = trollsKilled + 1
        player.set(ICE_TROLLS_KILLED, newCount)
        syncIceTrollCave(player)
        player.sendMessage("A chunk of ice falls away from the cave entrance...")
        if (newCount == ICE_TROLLS_REQUIRED)
            player.sendMessage("The last chunk of ice falls away, clearing the cave entrance.")
    }

    onPlayerStep(KAMIL_AMBUSH_TILES) { (player) ->
        if (!isSearchingForIceDiamond(player) || !player.getBool(ICE_DEAL_ACCEPTED) || player.getI(ICE_TROLLS_KILLED, 0) < ICE_TROLLS_REQUIRED) return@onPlayerStep
        if (player.getBool(KAMIL_DEFEATED)) {
            spawnIceParentBlocks(player)
            return@onPlayerStep
        }
        if (!player.getBool(KAMIL_SPAWNED)) {
            player.set(KAMIL_SPAWNED, true)
            player.sendMessage("You can feel an evil presence nearby...")
            Kamil(player, KAMIL_SPAWN_TILE).apply {
                setAutoDespawnAtDistance(false)
                forceTalk("Sallamakar Ro!")
            }
        }
    }


    onObjectClick(Loc.trollrescue_blankmodel, tiles = ICE_LEDGE_TILES, checkDistance = false) { (player, obj) ->
        if (player.isLocked) return@onObjectClick
        if (player.plane !in 0..1) return@onObjectClick
        val climbing = player.plane == 0
        if (climbing && !player.getBool(KAMIL_DEFEATED) && !player.isQuestComplete(Quest.DESERT_TREASURE)) {
            player.sendMessage("You must defeat Kamil before climbing the icy ledge.")
            return@onObjectClick
        }
        if (climbing && !player.equipment.containsOneItem(Obj.death_spikedboots)) {
            player.sendMessage("You need to be wearing spiked boots to climb the icy ledge.")
            return@onObjectClick
        }
        crossIceLedge(
            player,
            if (climbing) ICE_LEDGE_APPROACH_TILE else ICE_LEDGE_PATH_TILE,
            if (climbing) ICE_LEDGE_PATH_TILE else ICE_LEDGE_APPROACH_TILE,
            if (climbing) Seq.human_climbing else Seq.human_climbing_down
        ) {
            player.sendMessage(if (climbing) "You use the spikes on your boots to climb the icy ledge." else "You climb down the icy ledge.")
        }
    }

    onObjectClick(Loc.icegate_left_small, Loc.icegate_right_small, tiles = arrayOf(Tile.of(2853, 3808, 1), Tile.of(2853, 3810, 1), Tile.of(2853, 3808, 2), Tile.of(2853, 3810, 2)), checkDistance = false) { (player, obj) ->
        if (player.isLocked) return@onObjectClick
        if (player.plane !in 1..2) return@onObjectClick
        val leaving = player.plane == 2
        if (!leaving && !player.getBool(KAMIL_DEFEATED) && !player.isQuestComplete(Quest.DESERT_TREASURE)) {
            player.sendMessage("You must defeat Kamil before crossing onto the ice bridge.")
            return@onObjectClick
        }
        if (!leaving && !player.equipment.containsOneItem(Obj.death_spikedboots)) {
            player.sendMessage("You need to be wearing spiked boots to climb onto the ice bridge.")
            return@onObjectClick
        }
        val bridgeY = if (obj.y == 3808) 3809 else 3810
        crossIceLedge(
            player,
            Tile.of(if (leaving || player.x <= 2852) 2851 else 2855, bridgeY, player.plane),
            Tile.of(if (leaving) 2855 else 2850, bridgeY, if (leaving) 1 else 2),
            Seq.human_doublepipesqueeze
        ) {
            if (!leaving) spawnIceParentBlocks(player)
        }
    }

    onObjectClick(Loc.trollrescue_blankmodel2, tiles = arrayOf(Tile.of(2852, 3809, 2), Tile.of(2852, 3810, 2)), checkDistance = false) { (player, obj) ->
        if (player.isLocked || player.plane != 2) return@onObjectClick
        crossIceLedge(
            player,
            Tile.of(2851, obj.y, 2),
            Tile.of(2855, obj.y, 1),
            Seq.human_climbing_down
        )
    }

    onNpcDeath(Npc.icediamond_icewarrior) { e ->
        val kamil = e.npc as? OwnedNPC ?: return@onNpcDeath
        val player = kamil.owner
        player.set(KAMIL_DEFEATED, true)
        spawnIceParentBlocks(player)
        player.startConversation {
            player(CALM_TALK, "Well, that must have been the 'bad man' that the troll kid was on about... His parents must be up ahead somewhere.")
        }
    }
}

private fun crossIceLedge(player: Player, approach: Tile, destination: Tile, animation: Int, afterCrossing: () -> Unit = {}) {
    player.stopAll()
    val cross = Runnable {
        if (player.isLocked || player.plane != approach.plane || !player.withinDistance(approach, 1)) return@Runnable
        player.forceMove(destination.transform(0, 0, player.plane - destination.plane), animation, 10, 90) {
            player.tele(destination)
            player.anim(-1)
            afterCrossing()
        }
    }
    if (player.withinDistance(approach, 1)) cross.run()
    else player.setRouteEvent(RouteEvent(approach, cross))
}

@ServerStartupEvent
fun mapIceTrollParentFreeing() {
    onNpcClick(Npc.fd_trollblock1, Npc.fd_trollblock2, options = arrayOf("Smash-ice"), checkDistance = false) { e ->
        val block = e.npc as? OwnedNPC ?: return@onNpcClick
        if (block.owner != e.player || !e.player.getBool(KAMIL_DEFEATED) || e.player.isLocked) return@onNpcClick
        e.player.stopAll(true)
        e.player.interactionManager.setInteraction(PlayerCombatInteraction(e.player, block))
    }
    onNpcClick(Npc.fd_trollblock1, Npc.fd_trollblock2, options = arrayOf("Talk-to")) { e ->
        val block = e.npc as? OwnedNPC ?: return@onNpcClick
        if (block.owner != e.player) return@onNpcClick
        e.player.sendMessage("The troll is trapped in the ice. You'll have to smash it to free them.")
    }
    onNpcDeath(Npc.fd_trollblock1, Npc.fd_trollblock2) { e ->
        val block = e.npc as? OwnedNPC ?: return@onNpcDeath
        val player = block.owner
        if (!player.getBool(KAMIL_DEFEATED)) return@onNpcDeath

        when (block.id) {
            Npc.fd_trollblock1 -> freeTrollParent(player, true, block.tile)
            Npc.fd_trollblock2 -> freeTrollParent(player, false, block.tile)
        }
    }
}

private fun spawnIceParentBlocks(player: Player) {
    if (!player.getBool(KAMIL_DEFEATED)) return

    if (!player.getBool(ICE_FATHER_FREED) && !player.ownsActiveNpc(Npc.fd_trollblock1)) {
        object : OwnedNPC(player, Npc.fd_trollblock1, FATHER_ICE_TILE, true) {
            init {
                setAutoDespawnAtDistance(false)
                setRandomWalk(false)
                faceDir(Direction.EAST)
            }

            override fun setCombatTarget(entity: Entity?) = Unit

            override fun getRespawnDirection() = Direction.EAST.angle
        }
    } else if (player.getBool(ICE_FATHER_FREED) && !player.getBool(ICE_PARENTS_REUNITED) && !player.ownsActiveNpc(Npc.fd_troll_dad)) {
        OwnedNPC(player, Npc.fd_troll_dad, FATHER_ICE_TILE, true).apply {
            setAutoDespawnAtDistance(false)
        }
    }

    if (!player.getBool(ICE_MOTHER_FREED) && !player.ownsActiveNpc(Npc.fd_trollblock2)) {
        object : OwnedNPC(player, Npc.fd_trollblock2, MOTHER_ICE_TILE, true) {
            init {
                setAutoDespawnAtDistance(false)
                setRandomWalk(false)
                faceDir(Direction.EAST)
            }

            override fun setCombatTarget(entity: Entity?) = Unit

            override fun getRespawnDirection() = Direction.EAST.angle
        }
    } else if (player.getBool(ICE_MOTHER_FREED) && !player.getBool(ICE_PARENTS_REUNITED) && !player.ownsActiveNpc(Npc.fd_troll_mum)) {
        OwnedNPC(player, Npc.fd_troll_mum, MOTHER_ICE_TILE, true).apply {
            setAutoDespawnAtDistance(false)
        }
    }
}

private fun freeTrollParent(player: Player, father: Boolean, tile: Tile) {
    val freedFlag = if (father) ICE_FATHER_FREED else ICE_MOTHER_FREED
    if (player.getBool(freedFlag)) return

    player.set(freedFlag, true)

    player.tasks.schedule(6) {
        val parentId = if (father) Npc.fd_troll_dad else Npc.fd_troll_mum
        if (!player.ownsActiveNpc(parentId)) {
            OwnedNPC(player, parentId, tile, true).apply {
                setAutoDespawnAtDistance(false)
            }
        }

        if (player.getBool(ICE_FATHER_FREED) && player.getBool(ICE_MOTHER_FREED)) {
            reuniteTrollParents(player)
            return@schedule
        }

        player.startConversation {
            if (father) {
                npc(Npc.fd_troll_dad, HAPPY_TALKING, "Oh thank you! It was really cold in there! But please, you must free my wife as well! Our son is depending on us!")
            } else {
                npc(Npc.fd_troll_mum, HAPPY_TALKING, "Wow, thanks for breaking me out of that ice! But please, my husband is still trapped in there!")
            }
        }
    }
}

private fun hasPendingIceDiamondReunion(player: Player): Boolean =
    isSearchingForIceDiamond(player) && player.getBool(ICE_FATHER_FREED) &&
        player.getBool(ICE_MOTHER_FREED) && !player.getBool(ICE_DIAMOND_CLAIMED)

private fun reuniteTrollParents(player: Player) {
    if (!hasPendingIceDiamondReunion(player)) return

    player.startConversation {
        npc(Npc.fd_troll_dad, HAPPY_TALKING, "Phew! Am I glad to be out of that big ice cube! Are you okay too darling?")
        npc(Npc.fd_troll_mum, HAPPY_TALKING, "Yes, I thought we were done for! Why ever did that nasty Kamil freeze us up there anyway?")
        player(CONFUSED, "He must have been trying to protect his Diamond...")
        npc(Npc.fd_troll_dad, CONFUSED, "You mean that diamond I found the other day belonged to him? But why didn't he just ask for it back? It's not like I really want it or anything! And how did you know we had that diamond anyway, fleshy?")
        player(CALM_TALK, "Your son told me. That's why I rescued you, it is very important that I have that diamond...")
        npc(Npc.fd_troll_mum, SAD, "Ooohhhhh, my poor baby! He must have been so worried about us...")
        npc(Npc.fd_troll_dad, HAPPY_TALKING, "Yes, but he certainly inherited his Dad's smarts! If he'd told this fleshy that he had the Diamond and not us, we might never have been rescued!")
        player(CONFUSED, "Wait... what? That stupid little troll kid had the diamond all along?")
        npc(Npc.fd_troll_mum, ANGRY, "Don't you talk about my baby like that!")
        npc(Npc.fd_troll_dad, HAPPY_TALKING, "Now, now dear, all's well that ends well. We've been freed and this fleshy has certainly earned ${player.genderTerm("himself", "herself")} that diamond. Let's get out of this terrible place and see our son!")
        simple("You are transported back to the Ice gate with the trolls.", extraFunctionality = Runnable {
            returnTrollParentsToChild(player)
        })
        npc(Npc.fourdiamonds_troll_child_okay, HAPPY_TALKING, "Mommy! Daddy! You're free!")
        npc(Npc.fd_troll_dad, HAPPY_TALKING, "That's right son, and it's all thanks to this brave adventurer here. Now, make sure you hand over that diamond ${player.genderTerm("he", "she")} was looking for.")
        npc(Npc.fd_troll_dad, HAPPY_TALKING, "It has been nothing but trouble for us, let's just get back to our cave and have dinner.")
        npc(Npc.fd_troll_mum, HAPPY_TALKING, "That's right son, it's your favorite tonight too! A big plate of raw mackerel!")
        npc(Npc.fourdiamonds_troll_child_okay, HAPPY_TALKING, "RAW MACKEREL! YUMMY!")
        item(Obj.fd_icediamond, "You receive the Ice Diamond.") {
            if (hasPendingIceDiamondReunion(player)) {
                if (!player.containsItem(Obj.fd_icediamond)) DesertTreasureDiamonds.giveIceDiamond(player)
                player.set(ICE_DIAMOND_CLAIMED, true)
            }
        }
        npc(Npc.fourdiamonds_troll_child_okay, HAPPY_TALKING, "Here ya go ${player.genderTerm("mister", "lady")}! Thanks for getting my mom and dad away from the bad man!")
        player(CALM_TALK, "Don't worry about it, just as long as I don't have to go back into that blizzard.")
    }
}

private fun returnTrollParentsToChild(player: Player) {
    player.set(ICE_PARENTS_REUNITED, true)
    spawnHappyTrollChild(player, false)
    player.resetWalkSteps()
    player.resetCombat()
    player.anim(-1)
    player.lock(2)
    player.tele(ICE_GATE_EXTERIOR_TILE)

    val parents = listOf(
        Npc.fd_troll_dad to ICE_GATE_EXTERIOR_TILE.transform(-4, 0, 0),
        Npc.fd_troll_mum to ICE_GATE_EXTERIOR_TILE.transform(-4, 2, 0)
    )
    World.NPCs.filter {
        it is OwnedNPC && it.owner === player &&
            it.id in intArrayOf(Npc.fd_trollblock1, Npc.fd_trollblock2, Npc.fd_troll_dad, Npc.fd_troll_mum)
    }.toList().forEach { it.finish() }
    parents.forEach { (id, tile) ->
        OwnedNPC(player, id, tile, true).apply {
            setRandomWalk(false)
            faceDir(Direction.EAST)
        }
    }
}

private fun hasHappyTrollChild(player: Player): Boolean =
    player.getBool(CHILD_CHEERED) || player.getBool(ICE_PARENTS_REUNITED) ||
        player.getBool(ICE_DIAMOND_CLAIMED) || player.isQuestComplete(Quest.DESERT_TREASURE)

private fun spawnHappyTrollChild(player: Player, requireNearby: Boolean = true) {
    if (!hasHappyTrollChild(player) || (requireNearby && !player.withinDistance(TROLL_CHILD_TILE, 32)) ||
        player.ownsActiveNpc(Npc.fourdiamonds_troll_child_okay)) return
    OwnedNPC(player, Npc.fourdiamonds_troll_child_okay, TROLL_CHILD_TILE, true).apply {
        setAutoDespawnAtDistance(false)
        setRandomWalk(false)
        faceDir(Direction.EAST)
    }
}

private fun isSearchingForIceDiamond(player: Player): Boolean =
    player.getQuestStage(Quest.DESERT_TREASURE) == STAGE_DIAMONDS_OF_AZZANADRA

private const val ICE_COLD_INTERVAL = 10
private const val ICE_COLD_DAMAGE = 20
private const val ICE_FALL_DAMAGE = 20

private const val ICE_COLD_ACTIVE = "dtIceColdActive"
private const val ICE_COLD_RUN_BLOCK = "dtIceColdRunBlock"

private fun isOnIcePath(player: Player): Boolean =
    player.getBool(ICE_DEAL_ACCEPTED) && player.x in 2816..2879 && player.y in 3710..3839 &&
        (player.plane > 0 || player.y >= 3744 || player.x >= 2839)

private fun startIcePathCold(player: Player) {
    if (!isOnIcePath(player) || player.tempAttribs.getB(ICE_COLD_ACTIVE)) return
    player.tempAttribs.setB(ICE_COLD_ACTIVE, true)
    var lastTile = Tile.of(player.tile)
    WorldTasks.scheduleTimer { tick ->
        if (player.hasFinished() || player.isDead || !isOnIcePath(player)) {
            if (player.getBool(ICE_COLD_RUN_BLOCK)) player.unblockRun()
            player.set(ICE_COLD_RUN_BLOCK, false)
            player.tempAttribs.setB(ICE_COLD_ACTIVE, false)
            return@scheduleTimer false
        }
        if (player.hasEffect(Effect.OOG_SALTWATER_POOL)) {
            if (player.getBool(ICE_COLD_RUN_BLOCK)) player.unblockRun()
            player.set(ICE_COLD_RUN_BLOCK, false)
        } else {
            if (!player.isRunBlocked) {
                player.blockRun()
                player.set(ICE_COLD_RUN_BLOCK, true)
            }
            if (player.getRun()) player.setRun(false)
        }
        val currentTile = Tile.of(player.tile)
        val previousTile = lastTile
        lastTile = currentTile
        if (currentTile != previousTile && currentTile.plane == previousTile.plane &&
            currentTile.withinDistance(previousTile, 1) && !player.isLocked && isOnIcyShelf(player)) {
            if (!player.equipment.containsOneItem(Obj.death_spikedboots)) {
                player.resetWalkSteps()
                player.tele(previousTile)
                lastTile = previousTile
                player.sendMessage("You need to be wearing spiked boots to walk on the ice.")
            } else if (!Agility.rollSuccess(player, 230, 250)) {
                player.stopAll()
                player.lock(2)
                player.anim(Seq.human_walk_logbalance_stumble_hold)
                player.tasks.schedule(2) {
                    if (!player.hasFinished() && !player.isDead) player.anim(-1)
                }
                player.applyHit(Hit(player, ICE_FALL_DAMAGE, Hit.HitLook.TRUE_DAMAGE))
            }
        }
        if (tick > 0 && tick % ICE_COLD_INTERVAL == 0) {
            for (skill in Constants.SKILL_NAME.indices)
                if (skill != Constants.HITPOINTS && skill != Constants.PRAYER)
                    player.skills.lowerStat(skill, 1)
            val damage = minOf(ICE_COLD_DAMAGE, (player.hitpoints - 1).coerceAtLeast(0))
            if (damage > 0) {
                player.hitpoints -= damage
                player.refreshHitPoints()
                player.fakeHit(Hit(player, damage, Hit.HitLook.TRUE_DAMAGE))
            }
        }
        true
    }
}

private fun isOnIcyShelf(player: Player): Boolean =
    player.plane in 1..2 && player.y in 3790..3820
