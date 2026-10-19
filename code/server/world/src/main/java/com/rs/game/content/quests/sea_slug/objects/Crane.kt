package com.rs.game.content.quests.sea_slug.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import world.gregs.voidps.type.Tile
import org.darkan.core.model.Animation
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val CRANE_TILE = Tile.of(2770, 3287, 1)
private val KENNITH_WALL_APPROACH = Tile.of(2767, 3289, 1)
private val KENNITH_WALL_EXIT = Tile.of(2769, 3289, 1)

@ServerStartupEvent
fun mapSeaSlugCrane() {
    onObjectClick(Loc.seaslug_crane, Loc.seaslug_crane2, tiles = arrayOf(CRANE_TILE)) { (player, obj) ->
        when (player.getQuestStage(Quest.SEA_SLUG)) {
            STAGE_NEEDS_CRANE -> player.rescueKennithWithCrane(obj)
            else -> player.sendMessage("Maybe I shouldn't be playing with that...")
        }
    }
}

private fun Player.rescueKennithWithCrane(obj: GameObject) {
    if (isLocked) return
    if (!inventory.containsItem(Obj.torch_lit)) {
        sendMessage("You need your lit torch to keep the fishermen away.")
        return
    }
    val kennith = World.getNPCsInChunkRange(chunkId, 2)
        .firstOrNull { it.id == Npc.kennith_platform && isSeaSlugOwnedNpc(it) }
    if (kennith == null) {
        sendMessage("Kennith is not ready to use the crane. Speak to him again.")
        refreshSeaSlugState()
        return
    }
    resetWalkSteps()
    lock(27)
    schedule("sea_slug_crane_rescue") {
        try {
            faceObject(obj)
            packets.sendCameraPos(CRANE_TILE.transform(6, -6, 0), 2000)
            packets.sendCameraLook(CRANE_TILE, 500)
            kennith.setRandomWalk(false)
            kennith.setNextFaceEntity(null)
            kennith.resetWalkSteps()
            kennith.setRun(false)
            // Walk inside the room before using the leap to cross the broken wall.
            kennith.addWalkSteps(2767, 3288, 3, true)
            kennith.addWalkSteps(KENNITH_WALL_APPROACH, 1, true)
            var approachTicks = 0
            while (kennith.tile != KENNITH_WALL_APPROACH && approachTicks < 6) {
                wait(1)
                approachTicks++
                if (kennith.hasFinished() || !isAtFishingPlatform() ||
                    getQuestStage(Quest.SEA_SLUG) != STAGE_NEEDS_CRANE)
                    return@schedule
            }
            if (kennith.tile != KENNITH_WALL_APPROACH) {
                sendMessage("Kennith couldn't reach the broken wall. Try rotating the crane again.")
                return@schedule
            }
            kennith.resetWalkSteps()
            kennith.faceTile(KENNITH_WALL_EXIT)
            kennith.forceMove(KENNITH_WALL_EXIT, Seq.seaslug_ken_leap, 0, 40)
            sendMessage("Kennith scrambles through the broken wall...")
            wait(2)
            if (kennith.hasFinished() || !isAtFishingPlatform() || getQuestStage(Quest.SEA_SLUG) != STAGE_NEEDS_CRANE)
                return@schedule
            // crane2 contains Kennith's model; retire the NPC as that animation starts.
            kennith.finish()
            anim(Seq.seaslug_controls_left)
            val animatedCrane = GameObject(Loc.seaslug_crane2, obj.type, obj.rotation, CRANE_TILE)
            packets.sendAddObject(animatedCrane)
            packets.sendObjectAnimation(animatedCrane, Animation(Seq.seaslug_crane_left))
            soundEffect(Sound.sound_3020, false)
            wait(15)
            if (!isAtFishingPlatform() || getQuestStage(Quest.SEA_SLUG) != STAGE_NEEDS_CRANE)
                return@schedule
            sendMessage("Down below, you see Holgart collect the boy from the crane and lead him away to safety.")
            setQuestStage(Quest.SEA_SLUG, STAGE_KENNITH_RESCUED)
        } finally {
            anim(-1)
            packets.sendResetCamera()
            packets.sendAddObject(GameObject(Loc.seaslug_crane, obj.type, obj.rotation, CRANE_TILE))
            refreshSeaSlugState()
            unlock()
        }
    }
}
