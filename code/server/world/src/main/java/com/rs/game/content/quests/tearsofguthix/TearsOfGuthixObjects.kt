package com.rs.game.content.quests.tearsofguthix

import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.containsTool
import com.rs.game.content.skills.mining.Pickaxe
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private val LANTERNS = mapOf(Obj.bullseye_lantern_nolens to Obj.tog_sapphire_lantern_empty,
    Obj.bullseye_lantern_empty to Obj.tog_sapphire_lantern_empty,
    Obj.bullseye_lantern_unlit to Obj.tog_sapphire_lantern_unlit)
private val MAGIC_ROCKS = arrayOf<Any>(Loc.tog_blue_stone_rocks1, Loc.tog_blue_stone_rocks2, Loc.tog_blue_stone_rocks3)

@ServerStartupEvent
fun mapTearsOfGuthixObjects() {
    onItemOnItem(intArrayOf(Obj.sapphire), LANTERNS.keys.toIntArray()) { event ->
        val lantern = event.getUsedWith(Obj.sapphire)
        val player = event.player
        if (!player.inventory.containsItem(Obj.sapphire, 1) || !player.inventory.containsItem(lantern.id, 1)) return@onItemOnItem
        player.inventory.deleteItem(Obj.sapphire, 1)
        player.inventory.replaceItem(LANTERNS.getValue(lantern.id), 1, lantern.slot)
        if (lantern.id != Obj.bullseye_lantern_nolens) player.inventory.addItem(Obj.bullseye_lantern_lens, 1)
        player.sendMessage("You swap the lantern's lens for a sapphire.")
    }
    onItemClick(Obj.tog_stone, options = arrayOf("Craft")) { (player) -> player.craftMagicStone() }
    onItemOnItem(intArrayOf(Obj.tog_stone), intArrayOf(Obj.chisel)) { it.player.craftMagicStone() }
    onObjectClick(*MAGIC_ROCKS) { (player, _, option) ->
        if (option == "Prospect") player.simpleDialogue("The stone is infused with the power of Guthix.")
        else player.mineMagicStone()
    }
    onItemOnObject(MAGIC_ROCKS) { (player, _, item) ->
        if (Pickaxe.entries.any { it.itemId == item.id }) player.mineMagicStone()
    }
    onItemOnNpc(Npc.tog_light_creature, Npc.tog_light_creature_carrying, checkDistance = false) { event ->
        val player = event.player
        if (event.isAtNPC || player.isLocked || !player.inventory.containsItem(event.item.id, 1)) return@onItemOnNpc
        if (event.item.id != Obj.tog_sapphire_lantern_lit) { player.sendMessage("The creature appears to ignore you."); return@onItemOnNpc }
        if (event.npc.id == Npc.tog_light_creature_carrying) {
            if (player.plane == 0 && player.tile.within(event.npc.tile, 16) && player.isQuestComplete(Quest.WHILE_GUTHIX_SLEEPS)) player.flyWithLight(Tile.of(3219, 9527, 2))
            return@onItemOnNpc
        }
        if (player.plane != 2 || player.x !in 3200..3248 || player.y !in 9504..9535 || !player.tile.within(event.npc.tile, 16)) return@onItemOnNpc
        if (player.tearsStage < MAKE_BOWL) { player.sendMessage("You have no reason to cross the chasm yet."); return@onItemOnNpc }
        if (player.isQuestComplete(Quest.WHILE_GUTHIX_SLEEPS)) player.sendOptionDialogue("Where would you like the light creature to take you?") { options ->
            options.add("Across to the magic stone.") { player.flyWithLight(Tile.of(3226, 9496, 2)) }
            options.add("Down into the chasm.") { player.flyWithLight(Tile.of(2520, 5884, 0)) }
            options.add("Nowhere.")
        } else player.flyWithLight(Tile.of(3226, 9496, 2))
    }
    onObjectClick(Loc.tog_climbing_rocks_down) { (player, obj) ->
        if (player.isLocked) return@onObjectClick
        val goingUp = player.x > obj.x
        if (goingUp && player.tearsStage < MINED_STONE && !player.tears.getB("usedLightCreature")) {
            player.simpleDialogue("The rocks are too steep to climb. You will need to attract a light creature to reach the magic stone.")
            return@onObjectClick
        }
        val landing = Tile.of(if (goingUp) 3238 else 3241, 9498, 2)
        if (!World.floorAndWallsFree(landing, 1)) {
            player.sendMessage("There is no safe place to climb to.")
            return@onObjectClick
        }
        player.useStairs(if (goingUp) Seq.tog_climb_slope_up else Seq.tog_climb_slope_down, landing, 2, 3)
    }
    onObjectClick(Loc.tog_climbing_rocks_up) { (player, obj) ->
        val goingEast = player.x < obj.x
        player.useStairs(if (goingEast) Seq.tog_climb_slope_up else Seq.tog_climb_slope_down,
            Tile.of(if (goingEast) 3241 else 3239, obj.y, 2), 2, 3)
    }
    onChunkEnter { event ->
        event.player?.let { player ->
            if (player.inTearsCavern() && !player.inTearsRoom()) {
                player.musicsManager.unlockMusic(Midi.midi_397)
                player.musicsManager.playSpecificAmbientSong(Midi.midi_397, true)
            }
        }
    }
    onLogin { (player) ->
        player.schedule {
            wait(2)
            if (!player.hasFinished() && player.inTearsCavern()) player.musicsManager.playSpecificAmbientSong(Midi.midi_397, true)
        }
    }
}

private fun Player.flyWithLight(destination: Tile) {
    if (isLocked || !inventory.containsItem(Obj.tog_sapphire_lantern_lit, 1)) return
    stopAll()
    lock(14)
    sendMessage("The light-creature is attracted to your beam and comes towards you...")
    schedule {
        try {
            anim(Seq.tog_player_float_up)
            wait(3)
            val landing = if (destination.plane != plane) destination else listOf(destination, destination.transform(1, 0), destination.transform(0, 1), destination.transform(-1, 0))
                .firstOrNull { World.floorAndWallsFree(it, 1) }
            if (landing == null) { sendMessage("The light creature cannot find a safe place to land."); return@schedule }
            if (plane == landing.plane && tile.within(landing, 64)) {
                forceMove(landing, Seq.tog_player_float_across, 0, 180, false)
                lock(10)
            }
            else { tele(landing); anim(Seq.tog_player_float_across) }
            if (destination == Tile.of(3226, 9496, 2) && tearsStage in MAKE_BOWL..MADE_BOWL)
                tears.setB("usedLightCreature", true)
            wait(6)
            anim(Seq.tog_player_float_down)
            wait(3)
        } finally { anim(-1); unlock() }
    }
}

private fun Player.mineMagicStone() {
    if (isLocked || plane != 2 || x !in 3210..3238 || y !in 9486..9502) return
    if (tearsStage !in MAKE_BOWL..MADE_BOWL) { simpleDialogue("You have no need to mine any magic stone."); return }
    if (containsItem(Obj.tog_stone) || containsItem(Obj.tog_bowl)) { simpleDialogue("You have already mined some stone. You don't need any more."); return }
    if (tearsStage == MAKE_BOWL && !tears.getB("usedLightCreature")) {
        simpleDialogue("You must use a lit sapphire lantern to attract a light creature and reach the magic stone first.")
        return
    }
    if (skills.getLevel(Constants.MINING) < 20) { sendMessage("You need a Mining level of 20 to mine this stone."); return }
    val pickaxe = Pickaxe.getBest(this) ?: Pickaxe.entries.lastOrNull {
        containsTool(it.itemId) && skills.getLevel(Constants.MINING) >= it.level
    }
    if (pickaxe == null) { sendMessage("You need a pickaxe to mine the stone."); return }
    if (!inventory.hasFreeSlots()) { simpleDialogue("Your inventory is too full to hold any stone."); return }
    stopAll()
    lock(4)
    schedule {
        try {
            anim(pickaxe.animId)
            wait(3)
            if (tearsStage !in MAKE_BOWL..MADE_BOWL || containsItem(Obj.tog_stone) || containsItem(Obj.tog_bowl)) return@schedule
            if (!inventory.hasFreeSlots()) { sendMessage("Your inventory is too full to hold any stone."); return@schedule }
            inventory.addItem(Obj.tog_stone, 1)
            setQuestStage(TOG, maxOf(tearsStage, MINED_STONE))
            sendMessage("You manage to mine some stone.")
            itemDialogue(Obj.tog_stone, "You manage to mine some stone.")
        } finally { anim(-1); unlock() }
    }
}

private fun Player.craftMagicStone() {
    if (isLocked || tearsStage !in MAKE_BOWL..MADE_BOWL || !inventory.containsItem(Obj.tog_stone, 1)) return
    if (skills.getLevel(Constants.CRAFTING) < 20) { sendMessage("You need a Crafting level of 20 to make the bowl."); return }
    if (!inventory.containsItem(Obj.chisel, 1) && !containsTool(Obj.chisel)) { sendMessage("You need a chisel to make the bowl."); return }
    stopAll()
    lock(4)
    schedule {
        try {
            anim(Seq.enakhra_chisel_cut_stone)
            wait(3)
            if (tearsStage !in MAKE_BOWL..MADE_BOWL || !inventory.containsItem(Obj.tog_stone, 1)) return@schedule
            val stone = inventory.items.lookup(Obj.tog_stone) ?: return@schedule
            inventory.replaceItem(Obj.tog_bowl, 1, inventory.items.getThisItemSlot(stone))
            setQuestStage(TOG, MADE_BOWL)
            sendMessage("You make a stone bowl.")
            itemDialogue(Obj.tog_bowl, "You make a stone bowl.")
        } finally { anim(-1); unlock() }
    }
}

internal fun Player.inTearsCavern() = plane == 2 && x in 3200..3263 && y in 9472..9535
