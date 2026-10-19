package com.rs.game.content.quests.creatureoffenkenstrain

import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import org.darkan.core.model.Animation
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private data class Grave(val tile: Tile, val name: String, val part: Int? = null)
private val GRAVES = listOf(
    Grave(HEAD_GRAVE, "Ed Lestwit", Obj.fenk_head_empty),
    Grave(Tile.of(3593, 3509, 0), "Elena Frey"),
    Grave(Tile.of(3594, 3491, 0), "Isla Skye"),
    Grave(Tile.of(3596, 3479, 0), "Marabella Kludge"),
    Grave(Tile.of(3502, 3576, 0), "Rolomere, 14th Lord of the North Coast", Obj.fenk_torso),
    Grave(Tile.of(3504, 3577, 0), "Rolovanne, 13th Lord of the North Coast", Obj.fenk_arms),
    Grave(Tile.of(3506, 3576, 0), "Rologray, 12th Lord of the North Coast", Obj.fenk_legs)
)
private val ENTRANCE_MEMORIAL = Tile.of(3578, 3527, 0)
private val MAUSOLEUM_MEMORIAL = Tile.of(3505, 3571, 0)
private val MEMORIALS = arrayOf(MAUSOLEUM_MEMORIAL, Tile.of(3574, 3526, 0), ENTRANCE_MEMORIAL)

@ServerStartupEvent
fun mapFenkenstrainObjects() {
    onObjectClick(Loc.ladder, Loc.laddertop, Loc.ladder_from_cellar_directional) { (player, _, option) ->
        when (option.lowercase()) {
            "climb-up", "climb up" -> if (player.plane < 3) player.useStairs(Seq.human_reachforladder, player.transform(0, 0, 1))
            "climb-down", "climb down" -> if (player.plane > 0) player.useStairs(Seq.human_reachforladder, player.transform(0, 0, -1))
            "climb" -> if (player.plane in 1..2) player.promptUpDown(Seq.human_reachforladder, "Climb up the ladder.", player.transform(0, 0, 1), "Climb down the ladder.", player.transform(0, 0, -1))
        }
    }
    onObjectClick(Loc.fenk_signpost) { (player) ->
        player.startConversation {
            if (player.fenkStage == UNSTARTED) questStart(FENKENSTRAIN, accepted = {
                if (player.canStartFenkenstrain()) {
                    simple("The signpost has a note pinned onto it. The note says:", "'----Braindead Butler Wanted----<br>Gravedigging skills essential - Hunchback advantageous<br>See Dr Fenkenstrain at the castle NE of Canifis'")
                    fenkExec(player) { if (player.fenkStage == UNSTARTED) player.setQuestStage(FENKENSTRAIN, ADVERT) }
                } else simple("You need Priest in Peril, The Restless Ghost started, 20 Crafting and 25 Thieving.")
            }, declined = {})
            else simple("The signpost has a note pinned onto it. The note says:", "'----Braindead Butler Wanted----<br>Gravedigging skills essential - Hunchback advantageous<br>See Dr Fenkenstrain at the castle NE of Canifis'")
        }
    }
    onObjectClick(Loc.fenk_bookcase) { (player, obj) ->
        if (obj.plane != 1 || !player.fenkActive()) return@onObjectClick
        val west = obj.tile.x < 3548
        val books = if (west) listOf("1001 Ways To Eat Fried Gizzards", "Practical Gardening For The Headless", "Human Taxidermy for Nincompoops", "The Joy of Gravedigging")
            else listOf("Chimney Sweeping on a Budget", "Handy Maggot Avoidance Techniques", "My Family and Other Zombies")
        player.startConversation {
            options("Which book would you like to read?") {
                books.forEachIndexed { index, book ->
                    op(book) {
                        if (index == if (west) 3 else 1) {
                            simple("As you pull the book a hidden latch springs into place, and the bookcase swings open, revealing a secret compartment.")
                            fenkExec(player) {
                                val amulet = if (west) Obj.fenk_marble_amulet else Obj.fenk_obsidian_amulet
                                if (player.fenk.getB("coffin") || player.containsAnyItems(amulet, Obj.fenk_star_amulet)) player.simpleDialogue("The secret compartment is empty.")
                                else player.fenkGive(amulet, "You find ${if (west) "a marble" else "an obsidian"} amulet in the secret compartment.")
                            }
                        } else simple("You find nothing useful in this book.")
                    }
                }
            }
        }
    }
    onObjectClick(Loc.fenk_grave, Loc.fenk_grave_poor) { (player, obj, option) ->
        when (option) {
            "Read", "Search" -> player.simpleDialogue("The grave says:", "'Here lies ${GRAVES.firstOrNull { it.tile == obj.tile }?.name ?: "Unknown"} - REST IN PEACE'")
            "Dig" -> player.digFenkenstrainGrave(obj)
        }
    }
    onItemOnObject(arrayOf(Loc.fenk_grave, Loc.fenk_grave_poor), arrayOf(Obj.spade)) { (player, obj) -> player.digFenkenstrainGrave(obj) }
    onObjectClick(Loc.fenk_coffin, tiles = MEMORIALS) { (player, obj, option) ->
        val entrance = obj.tile == ENTRANCE_MEMORIAL
        when (option) {
            "Search" -> player.simpleDialogue(if (entrance) "You find a depression in the memorial stone in the shape of a six-pointed star." else "You find nothing remarkable about the memorial stone.")
            "Push" -> when {
                entrance && player.fenkStage >= BODY_PARTS && (player.fenkStage >= SEWING || player.fenk.getB("coffin")) -> {
                    player.packets.sendObjectAnimation(obj, Animation(Seq.fenk_capstone_moves))
                    player.useStairs(-1, Tile.of(3577, 9927, 0), 1, 2)
                }
                obj.tile == MAUSOLEUM_MEMORIAL && player.fenkStage >= BODY_PARTS && (player.fenkStage >= SEWING || player.fenk.getB("cavern")) -> {
                    player.packets.sendObjectAnimation(obj, Animation(Seq.fenk_capstone_moves))
                    player.useStairs(-1, Tile.of(3504, 9971, 0), 1, 2)
                }
                else -> player.simpleDialogue("The coffin is incredible heavy, and does not budge.")
            }
        }
    }
    onItemOnObject(arrayOf(Loc.fenk_coffin), arrayOf(Obj.fenk_star_amulet), tiles = arrayOf(ENTRANCE_MEMORIAL)) { (player) ->
        if (!player.fenkActive() || !player.inventory.containsItem(Obj.fenk_star_amulet)) return@onItemOnObject
        if (!(player.fenkStage >= SEWING || player.fenk.getB("coffin"))) {
            player.inventory.deleteItem(Obj.fenk_star_amulet, 1)
            player.fenk.setB("coffin", true)
            player.questManager.sendQuestStage(FENKENSTRAIN)
        }
        player.simpleDialogue("The star amulet fits exactly into the depression on the coffin lid.")
    }
    onObjectClick(Loc.fenk_mausoleum_door, Loc.fenk_mausoleum_door_open) { (player, obj) ->
        if (player.fenkStage >= BODY_PARTS && player.inventory.containsItem(Obj.fenk_mausoleum_key)) {
            player.inventory.deleteItem(Obj.fenk_mausoleum_key, 1)
            player.fenk.setB("cavern", true)
            player.questManager.sendQuestStage(FENKENSTRAIN)
            player.crossFenkDoor(obj)
        } else player.simpleDialogue("The door is locked. You need the cavern key.")
    }

    onObjectClick(Loc.ladder_from_cellar_directional, tiles = arrayOf(Tile.of(3578, 9927, 0), Tile.of(3504, 9970, 0))) { (player, obj) ->
        if (obj.x < 3520 && !player.fenk.getB("cavern") && player.fenkStage != COMPLETE) player.simpleDialogue("You must unlock the cavern entrance first.")
        else player.useLadder(if (obj.x < 3520) Tile.of(3504, 3572, 0) else Tile.of(3577, 3527, 0))
    }
    onObjectClick(Loc.fenk_chest_closed2, Loc.fenk_chest_open) { (player, obj, option) ->
        if (player.fenkStage < BODY_PARTS) return@onObjectClick
        if (option == "Open") {
            obj.setIdTemporary(Loc.fenk_chest_open, 10)
            return@onObjectClick
        }
        if (option == "Close") {
            obj.setIdTemporary(Loc.fenk_chest_closed2, 10)
            return@onObjectClick
        }
        if (player.inventory.containsItem(Obj.fenk_mausoleum_key)) player.simpleDialogue("The chest is empty.")
        else if (!player.inventory.hasFreeSlots()) player.simpleDialogue("You need a free inventory space.")
        else {
            player.inventory.addItem(Obj.fenk_mausoleum_key, 1)
            player.itemDialogue(Obj.fenk_mausoleum_key, "You take a key out of the chest.")
            obj.setIdTemporary(Loc.fenk_chest_open, 3)
        }
    }
    onObjectClick(Loc.fenk_tower_door, Loc.fenk_tower_door_open, Loc.fenk_shed_door, Loc.fenk_shed_door_open) { (player, obj) ->
        val tower = obj.id in setOf(Loc.fenk_tower_door, Loc.fenk_tower_door_open)
        val authorised = if (tower) player.fenkStage >= TOWER && (player.fenkStage == COMPLETE || player.fenk.getB("towerUnlocked") || player.inventory.containsItem(Obj.fenk_tower_key))
            else player.fenkStage >= CONDUCTOR && (player.fenkStage >= REVIVED || player.fenk.getB("shed") || player.inventory.containsItem(Obj.fenk_shed_key))
        if (!authorised) player.simpleDialogue("The door is locked.")
        else {
            if (tower) {
                if (!player.fenk.getB("towerUnlocked")) player.inventory.deleteItem(Obj.fenk_tower_key, 1)
                player.fenk.setB("towerUnlocked", true)
            } else {
                if (player.fenkStage == CONDUCTOR && !player.fenk.getB("shed")) player.inventory.deleteItem(Obj.fenk_shed_key, 1)
                player.fenk.setB("shed", true)
            }
            player.questManager.sendQuestStage(FENKENSTRAIN)
            player.crossFenkDoor(obj)
        }
    }
    onObjectClick(Loc.fenk_broomcupboard, Loc.fenk_broomcupboard_open) { (player, obj, option) ->
        if (player.fenkStage < CONDUCTOR || player.fenkStage == CONDUCTOR && !player.fenk.getB("shed")) return@onObjectClick
        when (option) {
            "Open" -> obj.setIdTemporary(Loc.fenk_broomcupboard_open, 10)
            "Search" -> if (player.containsAnyItems(Obj.fenk_brush0, Obj.fenk_brush1, Obj.fenk_brush2, Obj.fenk_brush3)) player.simpleDialogue("You search the cupboard but find nothing.")
                else player.fenkGive(Obj.fenk_brush0, "You find a garden brush in the cupboard.")
            "Shut" -> obj.setIdTemporary(Loc.fenk_broomcupboard, 10)
        }
    }
    onObjectClick(Loc.fenk_canepile) { (player) ->
        if (player.fenkStage < CONDUCTOR) player.simpleDialogue("You have no reason to take a cane at the moment.")
        else if (player.inventory.hasFreeSlots()) player.inventory.addItem(Obj.fenk_cane, 1)
        else player.simpleDialogue("You need a free inventory space to take a cane.")
    }
    onItemOnObject(arrayOf(Loc.fenk_fireplace), arrayOf(Obj.fenk_brush1, Obj.fenk_brush2, Obj.fenk_brush3)) { (player, obj, item) ->
        if (obj.tile == Tile.of(3544, 3555, 1) && item.id == Obj.fenk_brush3 && player.fenkStage == CONDUCTOR && !player.containsItem(Obj.fenk_lightning_mould))
            player.fenkGive(Obj.fenk_lightning_mould, "A lightning conductor mould falls down out of the chimney.")
        else player.simpleDialogue("You give the chimney a jolly good clean out.")
    }
    onObjectClick(Loc.fenk_conductor_broken, Loc.fenk_conductor_repaired) { (player, obj) -> player.repairFenkConductor(obj) }
    onItemOnObject(arrayOf(Loc.fenk_conductor_broken), arrayOf(Obj.fenk_conductor)) { (player, obj) -> player.repairFenkConductor(obj) }
    onObjectClick(Loc.fenk_clock) { (player) ->
        if (player.fenkStage >= BODY_PARTS && !player.containsItem(Obj.fenk_letter)) player.fenkGive(Obj.fenk_letter, "As you wind the old clock a letter falls out. Judging by the thick covering of dust it must have been here for some time.")
        else player.simpleDialogue("You wind the old clock.")
    }
    onObjectClick(Loc.ladder, tiles = arrayOf(Tile.of(3539, 3543, 0), Tile.of(3548, 3539, 1), Tile.of(3548, 3554, 1))) { (player, obj) ->
        if (obj.y == 3554 && (player.fenkStage < TOWER || player.fenkStage != COMPLETE && !player.fenk.getB("towerUnlocked"))) player.simpleDialogue("The tower is locked.")
        else player.useFenkLadder(obj, 1)
    }
    onObjectClick(Loc.laddertop, tiles = arrayOf(Tile.of(3539, 3543, 1), Tile.of(3548, 3539, 2), Tile.of(3548, 3554, 2))) { (player, obj) ->
        player.useFenkLadder(obj, -1)
    }
}

private fun Player.digFenkenstrainGrave(obj: GameObject) {
    if (!inventory.containsItem(Obj.spade)) { simpleDialogue("You need a spade to dig here."); return }
    val grave = GRAVES.firstOrNull { it.tile == obj.tile }
    anim(Seq.seq_830)
    sendMessage("You start digging...")
    val part = grave?.part
    if (part == null || fenkStage < BODY_PARTS) { simpleDialogue("...but the grave is empty."); return }
    val delivered = if (part == Obj.fenk_head_empty) Obj.fenk_head_full else part
    if (fenkStage >= SEWING || fenk.getB("part$delivered") || containsItem(part) || part == Obj.fenk_head_empty && containsItem(Obj.fenk_head_full)) {
        simpleDialogue("...but the grave is empty.")
        return
    }
    fenkGive(part, when (part) {
        Obj.fenk_head_empty -> "... and you unearth a decapitated head."
        Obj.fenk_torso -> "... and you unearth a torso."
        Obj.fenk_arms -> "... and you unearth a pair of arms."
        else -> "... and you unearth a pair of legs."
    })
    if (part == Obj.fenk_head_empty && inventory.containsItem(part)) stopGardenerEscort(this)
}

private fun Player.crossFenkDoor(obj: GameObject) {
    val destination = when (obj.rotation) {
        0 -> obj.tile.transform(if (x < obj.x) 0 else -1, 0, 0)
        1 -> obj.tile.transform(0, if (y > obj.y) 0 else 1, 0)
        2 -> obj.tile.transform(if (x > obj.x) 0 else 1, 0, 0)
        else -> obj.tile.transform(0, if (y < obj.y) 0 else -1, 0)
    }
    val openId = when (obj.id) {
        Loc.fenk_tower_door, Loc.fenk_tower_door_open -> Loc.fenk_tower_door_open
        Loc.fenk_shed_door, Loc.fenk_shed_door_open -> Loc.fenk_shed_door_open
        else -> Loc.fenk_mausoleum_door_open
    }
    val openTile = when (obj.rotation) {
        0 -> obj.tile.transform(-1, 0, 0)
        1 -> obj.tile.transform(0, 1, 0)
        2 -> obj.tile.transform(1, 0, 0)
        else -> obj.tile.transform(0, -1, 0)
    }
    val open = GameObject(openId, obj.type, obj.getRotation(-1), openTile)
    resetWalkSteps()
    lock(3)
    packets.sendRemoveObject(obj)
    packets.sendAddObject(open)
    addWalkSteps(destination, 3, false)
    schedule {
        wait(2)
        packets.sendRemoveObject(open)
        packets.sendAddObject(obj)
        unlock()
    }
}

private fun Player.repairFenkConductor(obj: GameObject) {
    if (fenkStage >= REVIVED) { simpleDialogue("The lightning conductor is now beyond repair."); return }
    if (fenkStage != CONDUCTOR || !inventory.containsItem(Obj.fenk_conductor)) { simpleDialogue("You need a new silver conductor to repair this."); return }
    inventory.deleteItem(Obj.fenk_conductor, 1)
    lock(4)
    anim(Seq.fenk_human_poke)
    packets.sendObjectAnimation(obj, Animation(Seq.fenk_lightning))
    setQuestStage(FENKENSTRAIN, REVIVED)
    schedule {
        wait(3)
        unlock()
        simpleDialogue("You repair the lightning conductor not one moment too soon - a tremendous bolt of lightning melts the new lightning conductor, and power blazes throughout the castle, if only briefly.")
    }
}

private fun Player.useFenkLadder(obj: GameObject, planeOffset: Int) {
    val anchor = obj.tile.transform(0, 0, planeOffset)
    val destinations = when (obj.y) {
        3539 -> listOf(anchor.transform(0, -1, 0), anchor.transform(0, 1, 0))
        3554 -> listOf(anchor.transform(0, 1, 0), anchor.transform(0, -1, 0))
        else -> listOf(anchor.transform(-1, 0, 0), anchor.transform(1, 0, 0))
    }
    val destination = destinations.firstOrNull { World.floorAndWallsFree(it, 1) }
    if (destination == null) simpleDialogue("The way is blocked. Please try again in a moment.")
    else useLadder(destination)
}
