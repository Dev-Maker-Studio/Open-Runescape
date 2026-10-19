package com.rs.game.content.world.areas.taverley

import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.wolfwhistle.WolfWhistle
import com.rs.game.content.skills.summoning.Summoning
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.Hit
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPickupItem
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapTaverley() {
    onObjectClick(Loc.omge_taverley_stairs_lower, Loc.omge_taverley_stairs_upper) { (player, obj) ->
        val tile = obj.tile
        val destination = if (obj.id == Loc.omge_taverley_stairs_lower) {
            if (tile.isAt(2928, 3445, 0)
                && player.questManager.getStage(Quest.WOLF_WHISTLE) == WolfWhistle.WOLPERTINGER_MATERIALS
                && !player.inventory.containsItem(Obj.wolf_embroidered_pouch)
                && !player.bank.containsItem(Obj.wolf_embroidered_pouch, 1)
            )
                player.forceTalk("Okay, that pouch has to be here somewhere...")
            when (obj.rotation) {
                0 -> tile.transform(0, 2, 1)
                1 -> tile.transform(2, 0, 1)
                2 -> tile.transform(0, -1, 1)
                else -> tile.transform(-1, 0, 1)
            }
        } else when (obj.rotation) {
            0 -> tile.transform(-1, -1, -1)
            1 -> tile.transform(-1, 1, -1)
            2 -> tile.transform(1, 2, -1)
            else -> tile.transform(2, -1, -1)
        }
        player.useStairs(-1, destination, 0, 0)
    }

    onObjectClick(Loc.loc_67498) { (player) ->
        if (player.questManager.getStage(Quest.WOLF_WHISTLE) == WolfWhistle.NOT_STARTED)
            player.startConversation { simple("I'm sure there is nothing down there. It's just an old, dry well that is making funny echoes.") }
    }

    onObjectClick(Loc.omge_skill_summoning_stone) { (player, obj, option) ->
        when (option) {
            "Infuse-pouch" -> {
                if (player.questManager.getStage(Quest.WOLF_WHISTLE) == WolfWhistle.WOLPERTINGER_CREATION && WolfWhistle.wolfWhistleObeliskReadyToInfusePouch(player))
                    WolfWhistle.doWolpertingerPouchCreation(player, obj)
                else
                    Summoning.openInfusionInterface(player, false)
            }
            "Renew-points" -> {
                val summonLevel = player.skills.getLevelForXp(Constants.SUMMONING)
                if (player.skills.getLevel(Constants.SUMMONING) < summonLevel) {
                    player.lock(3)
                    player.anim(Seq.wolf_player_recharge)
                    player.skills.set(Constants.SUMMONING, summonLevel)
                    player.sendMessage("You have recharged your Summoning points.", true)
                } else
                    player.sendMessage("You already have full Summoning points.")
            }
        }
    }

    onObjectClick(Loc.scorpionwall) { Doors.handleDoor(it.player, it.obj, -1) }

    onObjectClick(Loc.loc_67043, Loc.omge_slayer_cave_wall_exit) { (player, obj) ->
        if (obj.id == Loc.loc_67043 && obj.tile.isAt(2923, 3404, 0))
            player.tele(Tile.of(2220, 4532, 0))
        else if (obj.id == Loc.omge_slayer_cave_wall_exit && obj.tile.isAt(2220, 4531, 0))
            player.tele(Tile.of(2927, 3406, 0))
    }

    onPickupItem(Obj.obj_245, tiles = arrayOf(Tile.of(2946, 3474, 0), Tile.of(2946, 3473, 0))) { e ->
        if (e.isTelegrabbed)
            return@onPickupItem
        val player = e.player
        player.applyHit(Hit(player, 50, Hit.HitLook.TRUE_DAMAGE))
        for (monk in player.queryNearbyNPCsByTileRange(7) { it.id == Npc.chaosmonk2 }) {
            if (monk.isDead)
                continue
            monk.forceTalk("Hands off Zamorak's wine!")
            monk.setCombatTarget(player)
        }
        e.cancelPickup()
    }

    onObjectClick(Loc.loc_24672, Loc.loc_24673) { (player, obj) ->
        if (obj.id == Loc.loc_24672)
            player.tele(player.transform(0, if (obj.rotation == 0) 4 else 0, 1))
        else
            player.tele(player.transform(0, if (obj.rotation == 0) -4 else 0, -1))
    }
}
