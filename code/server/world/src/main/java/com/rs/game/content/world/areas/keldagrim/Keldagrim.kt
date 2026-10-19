package com.rs.game.content.world.areas.keldagrim

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.shop.ShopsHandler

@ServerStartupEvent
fun mapKeldagrim() {
    onNpcClick(4558) { e ->
        when (e.option) {
            "Talk-to" -> e.player.startConversation {
                npc(e.npcId, HeadE.HAPPY_TALKING, "Can I help you at all?")
                exec { ShopsHandler.openShop(e.player, "keldagrim_crossbow_shop") }
            }
            "Trade" -> ShopsHandler.openShop(e.player, "keldagrim_crossbow_shop")
        }
    }

    onNpcClick(2158) { e ->
        when (e.option) {
            "Talk-to" -> e.player.startConversation {
                npc(e.npcId, HeadE.HAPPY_TALKING, "Can I help you at all?")
                exec { ShopsHandler.openShop(e.player, "carefree_crafting_stall") }
            }
            "Trade" -> ShopsHandler.openShop(e.player, "carefree_crafting_stall")
        }
    }

    val stairMap = mapOf(
        5973 to Tile.of(2838, 10124, 0),
        5998 to Tile.of(2780, 10161, 0),
        9084 to Tile.of(1939, 4958, 0),
        9138 to Tile.of(2931, 10196, 0),
        45060 to Tile.of(1520, 4704, 0),
        45008 to Tile.of(2817, 10155, 0),
        45005 to Tile.of(2871, 10176, 1),
        45006 to Tile.of(2871, 10173, 0),
        45007 to Tile.of(2873, 10173, 2),

        )

    fun handleStairs(objectId: Int, player: Player) {
        stairMap[objectId]?.let { player.useStairs(it) }
    }

    stairMap.keys.forEach { objectId ->
        onObjectClick(objectId) { e -> handleStairs(e.objectId, e.player) }
    }

    onObjectClick(6085, 6086) { e ->
        val xOffset = when(e.getObj().rotation) {
            0 -> 0
            1 -> 3
            2 -> 0
            3 -> -3
            else -> 0
        }
        val yOffset = when(e.getObj().rotation) {
            0 -> 3
            1 -> 0
            2 -> -3
            3 -> 0
            else -> 0
        }
        if (e.objectId == 6085)
            e.player.tele(e.player.transform(xOffset, yOffset, 1))
        else if (e.objectId == 6086)
            e.player.tele(e.player.transform(-xOffset, -yOffset, -1))
    }

    onObjectClick(6089, 6090) { e ->
        val xOffset = when(e.getObj().rotation) {
            0 -> 0
            1 -> 3
            2 -> 0
            3 -> -3
            else -> 0
        }
        val yOffset = when(e.getObj().rotation) {
            0 -> 3
            1 -> 0
            2 -> -3
            3 -> 0
            else -> 0
        }
        if (e.objectId == 6089)
            e.player.tele(e.player.transform(xOffset, yOffset, 1))
        else if (e.objectId == 6090)
            e.player.tele(e.player.transform(-xOffset, -yOffset, -1))
    }
    onObjectClick(6087, 6088) { e ->
        val xOffset = when(e.getObj().rotation) {
            0 -> -0
            1 -> 3
            2 -> 0
            3 -> -3
            else -> 0
        }
        val yOffset = when(e.getObj().rotation) {
            0 -> 0
            1 -> 0
            2 -> -3
            3 -> 0
            else -> 0
        }
        if (e.objectId == 6087)
            e.player.tele(e.player.transform(xOffset, yOffset, 1))
        else if (e.objectId == 6088)
            e.player.tele(e.player.transform(-xOffset, -yOffset, -1))
    }

    onObjectClick(34040, 34041) { e ->
        val xOffset = when(e.getObj().rotation) {
            0 -> 0
            1 -> 3
            2 -> 0
            3 -> -3
            else -> 0
        }
        val yOffset = when(e.getObj().rotation) {
            0 -> 3
            1 -> 0
            2 -> -3
            3 -> 0
            else -> 0
        }
        if (e.objectId == 34040)
            e.player.tele(e.player.transform(xOffset, yOffset, 1))
        else if (e.objectId == 34041)
            e.player.tele(e.player.transform(-xOffset, -yOffset, -1))
    }
}

