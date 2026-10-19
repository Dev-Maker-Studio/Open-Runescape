package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Loc
import com.rs.engine.quest.Quest
import org.darkan.core.gameval.gen.Varbit
import com.rs.game.World
import com.rs.game.content.quests.underground_pass.*
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick

private const val TOMB_SEARCH_DAMAGE = 100
private const val CAGE_SEARCH_DAMAGE = 100
private const val UNPROTECTED_CAGE_EXTRA_DAMAGE = 20
private const val TOMB_BURN_TASK = "UndergroundPassIbanTombBurn"
private const val IBAN_DEFEAT_TASK = "UndergroundPassIbanDefeat"
private val DEMON_AMULETS = intArrayOf(Obj.obj_1497, Obj.obj_1498, Obj.obj_1499)
private val DWARF_BREW_BARREL_TILE = Tile.of(2327, 9799, 0)
private val IBAN_TOMB_TILE = Tile.of(2356, 9800, 0)
private val DOVE_CAGE_TILE = Tile.of(2134, 4702, 1)
private val IBAN_DEFEAT_EXIT_TILE = Tile.of(2479, 9607, 0)
private val IBAN_TEMPLE_EXIT_TILE = Tile.of(2145, 4648, 1)
private val WELL_OF_VOYAGE_MAP_TILE = Tile.of(2014, 4712, 1)

internal fun Player.isDisguisedAsIbanDisciple(): Boolean {
    if (equipment.getId(Equipment.CHEST) != Obj.zamrobetop ||
        equipment.getId(Equipment.LEGS) != Obj.zamrobebottom
    ) return false

    return (0 until Equipment.SIZE).all { slot ->
        val itemId = equipment.getId(slot)
        itemId == -1 || itemId == Obj.zamrobetop || itemId == Obj.zamrobebottom
    }
}

private fun Player.hasTakenIbanAshes(): Boolean =
    vars.getVarBit(Varbit.upass_iban_flesh) == 1 ||
        inventory.containsItem(Obj.ibans_ashes) ||
        bank.containsItem(Obj.ibans_ashes)

private fun Player.takeIbanAshes() {
    when {
        hasTakenIbanAshes() ->
            itemDialogue(Obj.ibans_ashes, "You have already taken a sample of Iban's ashes.")

        !inventory.hasFreeSlots() ->
            itemDialogue(
                Obj.ibans_ashes,
                "You find the charred ashes of Iban's corpse,<br> but you don't have room to take any."
            )

        else -> {
            inventory.addItem(Obj.ibans_ashes, 1)
            itemDialogue(Obj.ibans_ashes, "You find the charred ashes of Iban's corpse.")
        }
    }
}

private fun Player.igniteIbanTomb(obj: GameObject) {
    when {
        vars.getVarBit(Varbit.upass_ibantombappearance) == 3 -> {
            sendMessage("The tomb has already been scorched.")
            return
        }

        vars.getVarBit(Varbit.upass_ibantombappearance) == 2 -> {
            sendMessage("The tomb is already on fire.")
            return
        }

        vars.getVarBit(Varbit.upass_tomb_brew) == 0 -> {
            sendMessage("There is nothing on the tomb to light.")
            return
        }
    }

    faceObject(obj)
    anim(Seq.human_createfire)
    lock(6)
    vars.saveVarBit(Varbit.upass_ignitedtomb, 1)
    vars.saveVarBit(Varbit.upass_ibantombappearance, 2)
    itemDialogue(Obj.upassdwarfbrew, "The brew ignites!")
    schedule(TOMB_BURN_TASK) {
        wait(5)
        vars.saveVarBit(Varbit.upass_ibantombappearance, 3)
        unlock()
        takeIbanAshes()
    }
}

private fun Player.searchIbanTomb(obj: GameObject) {
    when (vars.getVarBit(Varbit.upass_ibantombappearance)) {
        3 -> takeIbanAshes()
        2 -> sendMessage("The tomb is too hot to search.")
        else -> {
            lock(4)
            faceObject(obj)
            anim(Seq.human_pickuptable)
            sendMessage("You try opening the tomb.", true)
            schedule {
                wait(1)
                sendMessage("The lid won't open.", true)
                packets.sendObjectMessage(0, 0xFFFFFF, obj, "Leave me be!")
                wait(1)
                forceTalk("Aaarrgghhh")
                applyHit(Hit(TOMB_SEARCH_DAMAGE, Hit.HitLook.TRUE_DAMAGE))
            }
        }
    }
}

private fun Player.searchIbanCage(obj: GameObject) {
    faceObject(obj)
    anim(Seq.human_pickuptable)

    val wearingKlanksGauntlets = equipment.getId(Equipment.HANDS) == Obj.klanks_gauntlets
    if (wearingKlanksGauntlets) {
        sendMessage("The soulless being bites into your arm, but Klank's gauntlets protect you.", true)
    } else {
        sendMessage("The soulless being bites into your unprotected arm.", true)
        forceTalk("Aaarrgghh!")
        applyHit(Hit(CAGE_SEARCH_DAMAGE + UNPROTECTED_CAGE_EXTRA_DAMAGE, Hit.HitLook.TRUE_DAMAGE))
    }

    if (obj.id != Loc.upass_cage_dummy || obj.tile != DOVE_CAGE_TILE) {
        sendMessage("You search through the bottom of the cage but find nothing.", true)
        return
    }

    val alreadyHasDove = vars.getVarBit(Varbit.upass_iban_conscience) == 1 ||
        inventory.containsItem(Obj.ibansdove) || bank.containsItem(Obj.ibansdove)
    if (alreadyHasDove) {
        sendMessage("You search through the bottom of the cage but find nothing else.", true)
        return
    }

    giveUniqueQuestItem(Obj.ibansdove)
    sendMessage("You search through the bottom of the cage and find Iban's dove.", true)
}

private fun Player.destroyIban(item: Item) {
    val iban = World.getNPCsInChunkRange(chunkId, 1).firstOrNull {
        it.id == Npc.iban && !it.isDead && !it.hasFinished()
    }
    inventory.deleteItem(item)
    vars.setVarBit(Varbit.upass_seen_temple, 1)
    advanceUndergroundPass(STAGE_RETURN_TO_LATHAS)
    lock()
    schedule(IBAN_DEFEAT_TASK) {
        iban?.forceTalk("What's happening? It's dark here... so dark!")
        wait(2)
        iban?.forceTalk("I'm falling into the dark, what have you done?")
        sendMessage("Iban falls to his knees, clutching his throat...")
        wait(2)
        iban?.forceTalk("Noooooooo!")
        wait(2)
        sendMessage("Iban slumps motionless to the floor...")
        sendMessage("A roar comes from the pit of the damned.")
        sendMessage("The infamous Iban has finally gone to rest.")
        giveUniqueQuestItem(Obj.ibanstaff)
        inventory.addItemDrop(Obj.deathrune, 15)
        inventory.addItemDrop(Obj.firerune, 30)
        sendMessage("Amongst Iban's remains you find his staff and some runes.")

        repeat(3) { wave ->
            IBAN_TEMPLE_EFFECT_TILES
                .filterIndexed { index, _ -> index % 3 == wave }
                .forEach { World.sendSpotAnim(it, Spotanim.rockfall) }
            wait(1)
        }
        sendMessage("Suddenly rocks crash to the floor as the ground begins to shake...")
        wait(2)
        sendMessage("The temple walls begin to collapse in...")
        wait(2)
        sendMessage("You are thrown from the temple platform.")
        fadeScreen {
            tele(IBAN_DEFEAT_EXIT_TILE)
            unlock()
        }
    }
}

@ServerStartupEvent
fun mapIbanTemple() {
    onLogin { event ->
        if (
            event.player.vars.getVarBit(Varbit.upass_ibantombappearance) == 2 &&
            event.player.vars.getVarBit(Varbit.upass_ignitedtomb) == 1
        ) {
            event.player.vars.saveVarBit(Varbit.upass_ibantombappearance, 3)
        }
    }

    onObjectClick(Loc.upass_templedoor_closed_right, Loc.upass_templedoor_closed_left) { (player, obj, option) ->
        if (option != "Open")
            return@onObjectClick

        if (player.isQuestComplete(Quest.UNDERGROUND_PASS)) {
            if (Quest.REGICIDE.isImplemented && !player.isQuestStarted(Quest.REGICIDE)) {
                player.sendMessage("You must speak to King Lathas and start Regicide before entering Iban's temple again.")
                return@onObjectClick
            }
            player.useStairs(
                if (player.x < obj.x) IBAN_TEMPLE_EXIT_TILE else WELL_OF_VOYAGE_MAP_TILE
            )
            return@onObjectClick
        }

        val enteringTemple = player.x >= obj.x
        if (enteringTemple && !player.isDisguisedAsIbanDisciple()) {
            player.sendMessage("You must wear only a Zamorak robe top and bottom to enter Iban's temple.")
            return@onObjectClick
        }

        Doors.handleDoor(player, obj)
    }

    onObjectClick(Loc.upassshutchest1, Loc.iban_temple_throne) { (player, obj) ->
        when (obj.id) {
            Loc.upassshutchest1 -> {
                if (player.vars.getVarBit(Varbit.upass_iban_shadow) == 0 &&
                    !player.inventory.containsItem(Obj.ibansshadow) &&
                    !player.bank.containsItem(Obj.ibansshadow) &&
                    player.questManager.getAttribs(Quest.UNDERGROUND_PASS).getB("ibanShadowReleased")
                ) {
                    player.giveUniqueQuestItem(Obj.ibansshadow)
                    player.sendMessage("A trace of Iban's shadow gathers in the chest once more.")
                    return@onObjectClick
                }
                if (!DEMON_AMULETS.all { player.inventory.containsItem(it) }) {
                    player.sendMessage("Three amulet-shaped hollows are set into the chest.")
                    return@onObjectClick
                }
                DEMON_AMULETS.forEach { player.inventory.deleteItem(it, 1) }
                player.questManager.getAttribs(Quest.UNDERGROUND_PASS).setB("ibanShadowReleased", true)
                player.giveUniqueQuestItem(Obj.ibansshadow)
                player.sendMessage("The amulets fuse together and the chest releases Iban's shadow.")
            }

            Loc.iban_temple_throne -> player.sendMessage("The throne radiates the last traces of Iban's dark magic.")
        }
    }

    onObjectClick(Loc.upass_cage_dummy, Loc.upass_cage_dummy_dummy) { (player, obj, option) ->
        if (option == "Search")
            player.searchIbanCage(obj)
    }

    onItemOnObject(
        arrayOf(Loc.upassdwarfbrewbarrel),
        arrayOf(Obj.bucket_empty),
        tiles = arrayOf(DWARF_BREW_BARREL_TILE)
    ) { (player, _, item) ->
        player.inventory.replace(item, Item(Obj.upassdwarfbrew))
        player.sendMessage("You fill the bucket with dwarf brew.", true)
    }

    onItemOnObject(
        arrayOf(Loc.loc_3353),
        arrayOf(Obj.upassdwarfbrew, Obj.tinderbox),
        tiles = arrayOf(IBAN_TOMB_TILE)
    ) { (player, obj, item) ->
        when (item.id) {
            Obj.upassdwarfbrew -> {
                if (player.vars.getVarBit(Varbit.upass_tomb_brew) == 1) {
                    player.sendMessage("The tomb is already soaked in dwarf brew.")
                    return@onItemOnObject
                }
                player.inventory.replace(item, Item(Obj.bucket_empty))
                player.vars.setVarBit(Varbit.upass_tomb_brew, 1)
                player.itemDialogue(Obj.upassdwarfbrew, "You pour the strong alcohol over the tomb.")
            }

            Obj.tinderbox -> player.igniteIbanTomb(obj)
        }
    }

    onObjectClick(Loc.loc_3353, tiles = arrayOf(IBAN_TOMB_TILE)) { (player, obj, option) ->
        when (option) {
            "Light" -> player.igniteIbanTomb(obj)
            "Search" -> player.searchIbanTomb(obj)
        }
    }

    onItemOnObject(arrayOf(Loc.cave_temple_altar), arrayOf(Obj.obj_1492)) { (player, _, item) ->
        player.updateIbanDollStage()
        val stage = player.getQuestStage(Quest.UNDERGROUND_PASS)
        if (stage < STAGE_PREPARED_DOLL) {
            player.sendMessage("The doll is incomplete. It has no power over Iban yet.")
            return@onItemOnObject
        }
        if (stage >= STAGE_RETURN_TO_LATHAS) {
            player.sendMessage("Iban has already been destroyed.")
            return@onItemOnObject
        }
        player.sendMessage("You cast the doll into the Well of Voyage.")
        player.destroyIban(item)
    }
}
