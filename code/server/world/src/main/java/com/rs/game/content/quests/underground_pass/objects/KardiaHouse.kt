package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.dialogue.HeadE.FRUSTRATED
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.underground_pass.STAGE_FOUND_DWARVES
import com.rs.game.content.quests.underground_pass.advanceUndergroundPass
import com.rs.game.content.quests.underground_pass.giveUniqueQuestItem
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import kotlin.math.ceil

private const val LOOTED_KARDIA_CHEST_ATTR = "lootedKardiaChest"

private fun returnCatToKardia(player: Player) {
    player.inventory.deleteItem(Obj.cavewitchcat, 1)
    player.vars.saveVarBit(Varbit.upass_gave_cat, 1)
    player.sendMessage("You knock on the door and hide around the corner...", true)
    player.sendMessage("The Witch takes the cat inside.", true)
}

@ServerStartupEvent
fun mapKardiaHouse() {
    onItemOnObject(arrayOf(Loc.cavewitch_door), arrayOf(Obj.cavewitchcat)) { (player) ->
        returnCatToKardia(player)
    }

    onObjectClick(Loc.cavewitch_door) { (player, obj, option) ->
        when (option) {
            "Knock" -> {
                player.sendMessage("You knock on the door...", true)
                if (player.vars.getVarBit(Varbit.upass_gave_cat) == 1) {
                    player.sendMessage("There is no reply.", true)
                } else if (player.inventory.containsItem(Obj.cavewitchcat)) {
                    player.sendMessage("...You place the cat by the door.", true)
                    returnCatToKardia(player)
                } else {
                    player.sendMessage("There is no reply.", true)
                }
            }

            "Open" -> {
                if (player.vars.getVarBit(Varbit.upass_gave_cat) == 1) {
                    player.sendMessage("You open the door...", true)
                    player.sendMessage("The Witch is busy talking to the cat.", true)
                    Doors.handleDoor(player, obj)
                    return@onObjectClick
                }

                player.startConversation {
                    exec { player.sendMessage("You reach to open the door...", true) }
                    npc(Npc.cavewitch, FRUSTRATED, "Get away... Far away from here!")
                    exec { player.sendMessage("The witch raises her hands above her...", true) }
                    exec {
                        World.getNPCsInChunkRange(player.chunkId, 1)
                            .firstOrNull { it.id == Npc.cavewitch }
                            ?.forceTalk("Ha ha... die mortal!")
                        val damage = ceil(player.hitpoints * 0.25).toInt().coerceAtLeast(1)
                        player.applyHit(Hit(damage, Hit.HitLook.TRUE_DAMAGE))
                    }
                }
            }
        }
    }

    onObjectClick(Loc.upass_witchwindow) { (player) ->
        if (player.vars.getVarBit(Varbit.upass_gave_cat) == 1) {
            player.sendMessage("The witch is busy playing with her cat...", true)
        } else {
            player.sendMessage("Inside you see a witch, she appears to be looking for something.", true)
            World.getNPCsInChunkRange(player.chunkId, 1)
                .firstOrNull { it.id == Npc.cavewitch }
                ?.forceTalk("Here kitty, kitty!")
        }
    }

    onObjectClick(Loc.cavewitchchest) { (player) ->
        if (player.vars.getVarBit(Varbit.upass_gave_cat) == 0) {
            player.sendMessage("Kardia stops you from opening the chest.")
            return@onObjectClick
        }
        if (player.isQuestComplete(Quest.UNDERGROUND_PASS)) {
            player.sendMessage("You find nothing else of use in the chest.")
            return@onObjectClick
        }

        val attributes = player.questManager.getAttribs(Quest.UNDERGROUND_PASS)
        if (!attributes.getB(LOOTED_KARDIA_CHEST_ATTR)) {
            player.giveUniqueQuestItem(Obj.obj_1492)
            player.giveUniqueQuestItem(Obj.obj_1494)
            player.inventory.addItemDrop(Obj._3dosestatrestore, 1)
            player.inventory.addItemDrop(Obj._3dose2attack, 1)
            attributes.setB(LOOTED_KARDIA_CHEST_ATTR, true)
            player.advanceUndergroundPass(STAGE_FOUND_DWARVES)
            player.sendMessage("You search the chest and take Kardia's supplies.")
            return@onObjectClick
        }

        if (!player.inventory.containsItem(Obj.obj_1492) && !player.bank.containsItem(Obj.obj_1492)) {
            player.giveUniqueQuestItem(Obj.obj_1492)
            player.sendMessage("You find another Doll of Iban in the chest.")
        } else {
            player.sendMessage("You find nothing else of use in the chest.")
        }
    }
}
