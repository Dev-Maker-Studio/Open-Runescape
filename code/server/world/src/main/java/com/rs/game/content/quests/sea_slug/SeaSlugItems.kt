package com.rs.game.content.quests.sea_slug

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.content.world.LightSource
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.*
import kotlin.random.Random

@ServerStartupEvent
fun mapSeaSlugItems() {
    onPickupItem(Obj.seasluginv) { e ->
        val player = e.player
        if (!player.isAtFishingPlatform()) return@onPickupItem
        e.cancelPickup()
        if (e.isTelegrabbed) return@onPickupItem
        player.applyHit(Hit(player, 30, Hit.HitLook.TRUE_DAMAGE))
        player.startConversation {
            simple("You pick up the sea slug.<br><br>It sinks its teeth deep into your hand.")
            player(HeadE.SCARED, "Ouch!")
        }
    }

    onItemOnItem(
        intArrayOf(Obj.swamp_tar, Obj.pot_flour),
        intArrayOf(Obj.swamp_tar, Obj.pot_flour)
    ) { e ->
        val ids = setOf(e.item1.id, e.item2.id)
        if (ids != setOf(Obj.swamp_tar, Obj.pot_flour)) return@onItemOnItem
        e.player.inventory.deleteItem(Obj.swamp_tar, 1)
        e.player.inventory.deleteItem(Obj.pot_flour, 1)
        e.player.inventory.addItem(Obj.pot_empty, 1)
        e.player.inventory.addItem(Obj.rawswamppaste, 1)
        e.player.skills.addXp(Skills.COOKING, 1.0)
        e.player.itemDialogue(Obj.rawswamppaste, "You mix the flour and swamp tar into raw swamp paste.")
    }

    onItemOnObject(
        objectNamesOrIds = arrayOf("Fire"),
        itemNamesOrIds = arrayOf(Obj.rawswamppaste)
    ) { (player) ->
        if (!player.inventory.containsItem(Obj.rawswamppaste)) return@onItemOnObject
        player.anim(Seq.human_firecooking)
        player.inventory.deleteItem(Obj.rawswamppaste, 1)
        player.inventory.addItem(Obj.swamppaste, 1)
        player.skills.addXp(Skills.COOKING, 2.0)
        player.itemDialogue(Obj.swamppaste, "You heat the raw paste over the fire. It thickens into swamp paste.")
    }

    onItemOnItem(
        intArrayOf(Obj.broken_glass, Obj.damp_sticks),
        intArrayOf(Obj.broken_glass, Obj.damp_sticks)
    ) { e ->
        val ids = setOf(e.item1.id, e.item2.id)
        if (ids != setOf(Obj.broken_glass, Obj.damp_sticks)) return@onItemOnItem
        val player = e.player
        val slot = e.getUsedWith(Obj.broken_glass).slot
        player.lock(19)
        player.schedule("sea_slug_dry_sticks") {
            try {
                player.anim(Seq.seaslug_player_drying_sticks)
                wait(17)
                if (!player.inventory.containsItem(Obj.broken_glass) || player.inventory.getItem(slot)?.id != Obj.damp_sticks)
                    return@schedule
                player.inventory.replaceItem(Obj.dry_sticks, 1, slot)
                player.itemDialogue(Obj.dry_sticks, "You use the broken glass to scrape the damp bark from the sticks.")
            } finally {
                player.unlock()
            }
        }
    }

    onItemClick(Obj.dry_sticks, options = arrayOf("Rub-together")) { (player) ->
        player.lightSeaSlugTorch()
    }

    onItemOnItem(intArrayOf(Obj.dry_sticks), intArrayOf(Obj.torch_unlit)) { e ->
        e.player.lightSeaSlugTorch()
    }

    onItemClick(Obj.torch_unlit, options = arrayOf("Light")) { e ->
        if (e.player.inventory.containsItem(Obj.dry_sticks)) e.player.lightSeaSlugTorch()
        else LightSource.lightSource(e.player, e.slotId)
    }

    onDestroyItem(Obj.torch_unlit, Obj.torch_lit, Obj.damp_sticks, Obj.dry_sticks, Obj.broken_glass, Obj.seasluginv) { (player, item) ->
        when (item.id) {
            Obj.torch_unlit, Obj.torch_lit -> player.sendMessage("Bailey may be able to replace the torch if you still need one.")
            Obj.damp_sticks, Obj.broken_glass -> player.sendMessage("You can find another on the Fishing Platform.")
            Obj.dry_sticks -> player.sendMessage("You can scrape another set of damp sticks with broken glass.")
            Obj.seasluginv -> player.sendMessage("That is probably for the best.")
        }
    }
}

private fun Player.lightSeaSlugTorch() {
    if (isLocked || !inventory.containsItem(Obj.dry_sticks)) return
    if (skills.getLevel(Skills.FIREMAKING) < 30) {
        sendMessage("You need a Firemaking level of 30 to light the torch with dry sticks.")
        return
    }
    lock(4)
    schedule("sea_slug_light_torch") {
        try {
            anim(Seq.human_createfire)
            sendMessage("You rub together the dry sticks.")
            wait(2)
            if (!inventory.containsItem(Obj.dry_sticks)) return@schedule
            val slot = inventory.items.array().indexOfFirst { it?.id == Obj.torch_unlit }
            if (slot == -1 || Random.nextBoolean()) {
                sendMessage("The sticks smoke momentarily then die out.")
                return@schedule
            }
            sendMessage("The sticks catch alight.")
            inventory.replaceItem(Obj.torch_lit, 1, slot)
            sendMessage("You place the smouldering twigs to your torch.")
            itemDialogue(Obj.torch_lit, "Your torch lights.")
        } finally {
            unlock()
        }
    }
}

fun Player.preventSeaSlugTinderboxLighting(itemId: Int): Boolean {
    if (itemId != Obj.torch_unlit || !isAtFishingPlatform()) return false
    sendMessage("Your tinderbox is damp from the sea crossing. It won't light here.")
    return true
}
