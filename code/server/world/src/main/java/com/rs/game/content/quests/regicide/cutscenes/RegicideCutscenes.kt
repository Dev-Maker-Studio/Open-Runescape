package com.rs.game.content.quests.regicide.cutscenes

import com.rs.game.content.quests.regicide.items.showIorwerthMessage
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Midi
import com.rs.engine.cutscenekt.Cutscene
import com.rs.game.World
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.cutscenekt.cutscene
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.*
import com.rs.game.content.quests.regicide.npcs.GUARD_EATING_UNTIL
import com.rs.game.content.quests.regicide.objects.safeRegicideTile
import com.rs.game.content.quests.regicide.objects.ISAFDAR_ENTRY
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

fun Player.playIsafdarArrival() {
    if (isLocked || getQuestStage(Quest.REGICIDE) != STAGE_ENTER_ISAFDAR)
        return
    cutscene {
        endTile = ISAFDAR_ENTRY
        try {
            fadeInAndWait()
            dynamicRegion(endTile, 288, 400, 4, 4, copyNpcs = false)
            teleRegicidePlayer(8, 16)
            player.musicsManager.forcePlayMusic(Midi.midi_252)
            hideMinimap()
            val idris = createRegicideActor(Npc.regicide_good_elf1, 5, 11)
            camPos(14, 12, 900)
            camLook(8, 16, 400)
            fadeOutAndWait()
            walkRegicideActor(idris, 7, 13)
            wait(3)
            idris.faceEntityTile(player)
            dialogue {
                npc(idris, CALM_TALK, "Halt human! What are you doing here?")
                npc(idris, CALM_TALK, "Shhhh.... What was that!?! What the......")
            }
            waitForDialogue()
            val essyllt = createRegicideActor(Npc.regicide_evil_elf1, 5, 13)
            val morvran = createRegicideActor(Npc.regicide_evil_elf2, 6, 11)
            walkRegicideActor(essyllt, 6, 13)
            walkRegicideActor(morvran, 7, 12)
            wait(2)
            essyllt.faceEntityTile(idris)
            morvran.faceEntityTile(idris)
            essyllt.anim(Seq.seq_422)
            wait(1)
            idris.anim(Seq.human_death)
            wait(3)
            idris.finish()
            essyllt.faceEntityTile(player)
            morvran.faceEntityTile(player)
            dialogue {
                npc(morvran, CALM_TALK, "Are you the human by the name of ${player.displayName}?")
                player(CALM_TALK, "Yes, that's me.")
                npc(essyllt, CALM_TALK, "Good... We have been expecting you. King Lathas sent word of your coming. You should speak with Lord Iorwerth. He will help you track down that brigand Tyras. You'll find his camp in the north west of the forest.")
            }
            waitForDialogue()
            walkRegicideActor(essyllt, 3, 14)
            walkRegicideActor(morvran, 3, 13)
            wait(3)
            player.advanceRegicide(STAGE_MEET_IORWERTH)
            fadeInAndWait()
        } finally {
            stop()
            player.musicsManager.reset()
            fadeOutQuickly()
        }
    }
}

fun Player.playArianwynRevelation() {
    if (isLocked || getQuestStage(Quest.REGICIDE) != STAGE_RETURN_TO_LATHAS || !inventory.containsItem(Obj.regicide_iorwerth_message))
        return
    cutscene {
        endTile = player.tile
        try {
            fadeInAndWait()
            dynamicRegion(endTile, 320, 410, 4, 4, copyNpcs = false)
            teleRegicidePlayer(17, 16)
            player.musicsManager.forcePlayMusic(Midi.knightly)
            hideMinimap()
            val arianwyn = createRegicideActor(Npc.npc_1202, 15, 16)
            arianwyn.faceEntityTile(player)
            player.faceEntityTile(arianwyn)
            camPos(22, 12, 850)
            camLook(16, 16, 400)
            fadeOutAndWait()
            dialogue {
                npc(arianwyn, CALM_TALK, "Are you the human by the name ${player.displayName}?")
                player(CALM_TALK, "Yes, that's me?!?")
                npc(arianwyn, CALM_TALK, "Thank Seren, maybe all is not yet lost.")
                player(CALM_TALK, "What do you mean?")
                npc(arianwyn, CALM_TALK, "It was you that murdered King Tyras, do not bother to deny it, I was watching. There is much to explain and little time. You carry a message from Lord Iorwerth do you not.")
                player(CALM_TALK, "Well you seem to know everything else so I'll not lie. Yes.")
                npc(arianwyn, CALM_TALK, "Good, we understand each other well. Now hand it here and find the truth of who your real employer is.")
                simple("You show the message to the elf, he moves his lips silently and the seal on the message is parted.")
            }
            waitForDialogue()
            player.showIorwerthMessage()
            wait { !player.interfaceManager.topOpen(Interface.interface_174) }
            dialogue {
                player(CALM_TALK, "I had no idea!")
                npc(arianwyn, CALM_TALK, "There is still a chance to stop this. But we must be careful, or they will push their plans forward and act before we're ready. Give the message to King Lathas, He will not see my little spell. Act like nothing unusual happened. You're going to be our man on the inside.")
                player(CALM_TALK, "You want me to spy?!? OK what's in it for me?")
                npc(arianwyn, CALM_TALK, "How can I put this... This isn't a struggle for land or title. It is a war for life, all life. If the gate to Zamoraks world is opened we all die. Deliver your message, act normal. We'll be in touch.")
            }
            waitForDialogue()
            player.advanceRegicide(STAGE_DELIVER_MESSAGE)
            fadeInAndWait()
        } finally {
            stop()
            player.musicsManager.reset()
            fadeOutQuickly()
        }
    }
}

fun Player.fireRegicideCatapult() {
    if (isLocked || plane != 0 || x !in 2170..2200 || y !in 3170..3195)
        return
    if (getQuestStage(Quest.REGICIDE) != STAGE_DESTROY_TENT) {
        sendMessage(when (getQuestStage(Quest.REGICIDE)) {
            STAGE_PREPARE_EXPLOSIVE -> "You should show the Big Book of Bangs to the Chemist before using the catapult."
            in STAGE_REPORT_TYRAS_DEATH..STAGE_COMPLETE -> "You have already destroyed Tyras's tent."
            else -> "You have no reason to use the catapult."
        })
        return
    }
    if (!inventory.containsItem(Obj.regicide_barrel_lid_fused)) {
        sendMessage("You need a barrel bomb with a cloth fuse.")
        return
    }
    if (System.currentTimeMillis() >= questManager.getAttribs(Quest.REGICIDE).getL(GUARD_EATING_UNTIL)) {
        npcDialogue(Npc.regicide_tyras_lazy_guard, CALM_TALK, "Oi! Don't mess with that.")
        return
    }
    if (!inventory.containsItem(Obj.tinderbox)) {
        sendMessage("You need a tinderbox.")
        return
    }
    cutscene {
        endTile = player.tile
        val catapultState = player.vars.getVarBit(Varbit.catcon_catapult_state)
        try {
            fadeInAndWait()
            dynamicRegion(endTile, 272, 392, 4, 8, copyNpcs = false)
            player.vars.setVarBit(Varbit.catcon_catapult_state, 1)
            teleRegicidePlayer(6, 47)
            player.musicsManager.forcePlayMusic(Midi.meridian)
            hideMinimap()
            camPos(13, 42, 1100)
            camLook(6, 47, 450)
            fadeOutAndWait()
            player.faceTile(tileFromLocal(7, 47))
            player.anim(Seq.wind_catapult)
            World.sendObjectAnimation(World.getObject(tileFromLocal(9, 47)), Seq.winddown_catapult)
            wait(3)
            player.anim(Seq.human_pickuptable)
            wait(2)
            walkRegicideActor(player, 9, 47)
            wait(2)
            player.anim(Seq.regicide_catapault_trigger)
            World.sendObjectAnimation(World.getObject(tileFromLocal(9, 47)), Seq.fire_catapult)
            projectile(Tile.of(9, 47, 0), Tile.of(10, 6, 0), Spotanim.castlewars_catapult_projanim, 45, 0, 0, 2, 45, null)
            wait(3)
            camPos(13, 12, 1100)
            camLook(10, 6, 450)
            spotAnim(Spotanim.castlewars_catapult_impact, 10, 6, 0)
            val tent = (0..3).flatMap { plane ->
                World.getAllObjectsInChunkRange(Tile.of(getX(10), getY(6), plane).getChunkId(), 1)
            }.filter {
                it.x in getX(7)..getX(13) && it.y in getY(4)..getY(8) &&
                    it.id in intArrayOf(Loc.regicide_tent_human, Loc.regicide_tent_door, Loc.regicide_tentroof1_human, Loc.regicde_tentroofcentre1)
            }
            for (obj in tent) {
                when (obj.id) {
                    Loc.regicide_tent_human -> World.spawnObject(GameObject(Loc.regicide_tent_human_fire, obj.type, obj.rotation, obj.tile))
                    Loc.regicide_tent_door -> World.spawnObject(GameObject(Loc.regicide_tent_door_for_fire, obj.type, obj.rotation, obj.tile))
                    else -> World.removeObject(obj)
                }
            }
            wait(6)
            if (player.inventory.containsItem(Obj.regicide_barrel_lid_fused)) {
                player.inventory.deleteItem(Obj.regicide_barrel_lid_fused, 1)
                player.advanceRegicide(STAGE_REPORT_TYRAS_DEATH)
            }
            fadeInAndWait()
        } finally {
            player.vars.setVarBit(Varbit.catcon_catapult_state, catapultState)
            stop()
            player.musicsManager.reset()
            fadeOutQuickly()
        }
    }
}

private fun Cutscene.createRegicideActor(id: Int, localX: Int, localY: Int): NPC {
    val tile = requireNotNull(safeRegicideTile(tileFromLocal(localX, localY)))
    return npcCreate(id, tile.x - getX(0), tile.y - getY(0), tile.plane)
}

private fun Cutscene.walkRegicideActor(entity: Entity, localX: Int, localY: Int) {
    val tile = safeRegicideTile(tileFromLocal(localX, localY)) ?: return
    entity.run = false
    entity.addWalkSteps(tile.x, tile.y, 25, true)
}

private fun Cutscene.teleRegicidePlayer(localX: Int, localY: Int) {
    val tile = requireNotNull(safeRegicideTile(Tile.of(getX(localX), getY(localY), 0)))
    player.tele(tile)
}
