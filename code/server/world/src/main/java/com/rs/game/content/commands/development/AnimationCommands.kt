package com.rs.game.content.commands.development

import com.rs.Settings
import org.darkan.core.cache.AnimationDefinitions
import org.darkan.core.cache.BASDefinitions
import org.darkan.core.cache.NPCDefinitions
import org.darkan.core.model.ObjectShape
import com.rs.engine.command.Commands
import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.async.schedule
import com.rs.game.tasks.Task
import com.rs.game.tasks.WorldTasks
import org.darkan.core.model.Animation
import org.darkan.core.model.Rights
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile
import org.darkan.core.Logger
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.utils.ObjAnimList

@ServerStartupEvent
fun loadAnimationCommands() {
    val animRights = if (Settings.getConfig().isDebug) Rights.PLAYER else Rights.DEVELOPER

    Commands.add(Rights.DEVELOPER, "spotanim,gfx [id height]", "Creates a spot animation on top of the player.") { p, args ->
        val height = if (args.size == 1) 0 else args[1].toInt()
        p.spotAnim(args[0].toInt(), 0, height)
    }

    Commands.add(Rights.DEVELOPER, "tilespotanim,tilegfx [id height]", "Creates a spot animation on the tile south of the player.") { p, args ->
        val height = if (args.size == 1) 0 else args[1].toInt()
        World.sendSpotAnim(p.tile.transform(0, -1), SpotAnim(args[0].toInt(), 0, height))
    }

    Commands.add(animRights, "anim,emote [id]", "Animates the player with specified ID.") { p, args ->
        val animId = args[0].toInt()
        if (animId <= Utils.getAnimationDefinitionsSize())
            p.anim(animId)
    }

    Commands.add(animRights, "sync,animgfx [animId gfxId]", "Animates the player with specified ID and plays a SpotAnim at the same time.") { p, args ->
        val animId = args[0].toInt()
        val gfxId = args[1].toInt()

        if (animId <= Utils.getAnimationDefinitionsSize() && gfxId <= Utils.getSpotAnimDefinitionsSize())
            p.sync(animId, gfxId)
    }

    Commands.add(animRights, "bas,render [id]", "Sets the BAS of the player to specified ID.") { p, args ->
        p.appearance.setBAS(args[0].toInt())
    }

    Commands.add(animRights, "companim [npcId]", "Prints out animations compatible with the npc id.") { p, args ->
        val npcId = args[0].toInt()
        if (npcId in 0 until Utils.getNPCDefinitionsSize()) {
            val compatAnims = compatibleAnimations(NPCDefinitions.getDefs(npcId).renderEmote).toString()
            p.packets.sendDevConsoleMessage("$npcId: $compatAnims")
            p.sendMessage("$npcId: $compatAnims")
            Logger.debug(Commands::class.java, "companim", compatAnims)
        }
    }

    Commands.add(Rights.DEVELOPER, "setlook [slot id]", "Appearance setting") { p, args ->
        p.appearance.setLook(args[0].toInt(), args[1].toInt())
        p.appearance.generateAppearanceData()
    }

    Commands.add(Rights.DEVELOPER, "objectanim,oanim [x y animId (objectType)]", "Makes an object play an animation.") { p, args ->
        val x = args[0].toInt()
        val y = args[1].toInt()
        val animId = if (args.size == 3) args[2].toInt() else args[3].toInt()

        val obj = if (args.size == 3) {
            World.getObject(Tile.of(x, y, p.plane))
        } else {
            World.getObject(Tile.of(x, y, p.plane), ObjectShape.forId(args[2].toInt()))
        }

        if (obj == null) {
            p.packets.sendDevConsoleMessage("No object was found.")
            return@add
        }

        p.packets.sendObjectAnimation(obj, Animation(animId))
    }

    Commands.add(Rights.DEVELOPER, "objectanimloop,oanimloop [x y (startId) (endId)]", "Loops through object animations.") { p, args ->
        val x = args[0].toInt()
        val y = args[1].toInt()
        val tile = Tile.of(x, y, p.plane)
        val obj = ChunkManager.getChunk(tile.getChunkId()).getSpawnedObject(tile)

        if (obj == null) {
            p.packets.sendDevConsoleMessage("Could not find object at [x=$x, y=$y, z=${p.plane}].")
            return@add
        }

        if (!ObjAnimList.inited()) ObjAnimList.init()

        val start = if (args.size > 2) args[2].toInt() else 10
        val end = if (args.size > 3) args[3].toInt() else 20000

        p.schedule("loopAnim") {
            var current = start
            while (current < end) {
                World.sendObjectAnimation(obj, Animation(current))
                p.sendMessage("Current animation: $current")
                wait(2)
                for (i in (current + 1) until end) {
                    if (!ObjAnimList.isUsed(i)) {
                        current = i
                        break
                    }
                }
            }
        }
    }

    Commands.add(animRights, "tonpc,pnpc,npcme [npcId]", "Transforms the player into an NPC.") { p, args ->
        val npcId = args[0].toInt()
        if (npcId <= Utils.getNPCDefinitionsSize()) {
            p.appearance.transformIntoNPC(npcId)
        }
    }

    Commands.add(Rights.DEVELOPER, "testnpc,possessnpc [npcId]", "Spawns a real NPC for combat/animation testing.") { p, args ->
        val npcId = args[0].toInt()
        if (npcId !in 0 until Utils.getNPCDefinitionsSize())
            return@add
        getTestNpc(p)?.finish()
        val npc = NPC(npcId, p.tile.transform(1, 0))
        npc.setRandomWalk(false)
        p.tempAttribs.setO<NPC>(TEST_NPC_ATTR, npc)
        p.sendMessage("Test NPC $npcId spawned. Use ::testnpcattack, ::npcanim, or ::unpossess.")
    }

    Commands.add(Rights.DEVELOPER, "testnpcattack,npcattack", "Makes the possessed test NPC attack you using real NPC combat.") { p, _ ->
        val npc = getTestNpc(p)
        if (npc == null) {
            p.sendMessage("You do not have an active test NPC. Use ::possessnpc [npcId] first.")
            return@add
        }
        npc.setCombatTarget(p)
        p.sendMessage("Test NPC ${npc.id} is now attacking you.")
    }

    Commands.add(Rights.DEVELOPER, "npcanim [animId]", "Forces an animation on the possessed test NPC.") { p, args ->
        val npc = getTestNpc(p)
        if (npc == null) {
            p.sendMessage("You do not have an active test NPC. Use ::possessnpc [npcId] first.")
            return@add
        }
        npc.anim(args[0].toInt())
    }

    Commands.add(Rights.DEVELOPER, "testnpchere,npchere", "Moves the possessed test NPC beside you.") { p, _ ->
        val npc = getTestNpc(p)
        if (npc == null) {
            p.sendMessage("You do not have an active test NPC. Use ::possessnpc [npcId] first.")
            return@add
        }
        npc.removeCombatTarget()
        npc.tele(p.tile.transform(1, 0))
    }

    Commands.add(Rights.DEVELOPER, "unpossess,removetestnpc", "Removes the possessed test NPC.") { p, _ ->
        val npc = getTestNpc(p)
        if (npc == null) {
            p.sendMessage("You do not have an active test NPC.")
            return@add
        }
        npc.finish()
        p.tempAttribs.removeO<NPC>(TEST_NPC_ATTR)
        p.sendMessage("Test NPC removed.")
    }

    Commands.add(Rights.DEVELOPER, "headicon", "Set custom headicon.") { p, args ->
        p.tempAttribs.setI("customHeadIcon", args[0].toInt())
        p.appearance.generateAppearanceData()
    }

    Commands.add(Rights.DEVELOPER, "skull", "Set custom skull.") { p, args ->
        p.setSkullInfiniteDelay(args[0].toInt())
        p.appearance.generateAppearanceData()
    }

    Commands.add(Rights.DEVELOPER, "getapp", "Prints out appearance data") { p, _ ->
        p.appearance.printDebug()
    }
}


/**
 * Every animation id that shares a skeleton with the given BAS's stand animation, ascending.
 *
 * Ported from legacy `NPCDefinitions#getCompatibleAnimations` (core
 * src/main/java/com/rs/cache/loaders/NPCDefinitions.java:255-276), which `companim` printed. It
 * had been reduced to printing the raw render emote id because the animation table scan needs
 * `Utils.getAnimationDefinitionsSize()`, which used to return 0.
 *
 * Legacy read the skeleton out of a separate `frameSetIds` array; the modern decoder packs the
 * same value into the high half of each entry of [AnimationDefinition.frames]
 * (`frames[i] = (frameSetId shl 16) + frameId`), so `frames[0] ushr 16` is legacy's
 * `frameSetIds[0]`, and 0xFFFF is its "unset" -1.
 */
private fun compatibleAnimations(basId: Int): List<Int> {
    if (basId == -1)
        return emptyList()
    val standAnimation = BASDefinitions.getDefs(basId).standAnimation
    if (standAnimation == -1)
        return emptyList()
    val standFrames = AnimationDefinitions.getDefs(standAnimation).frames
    if (standFrames == null || standFrames.isEmpty())
        return emptyList()
    val skeleton = standFrames[0] ushr 16

    val matches = ArrayList<Int>()
    for (i in 0 until Utils.getAnimationDefinitionsSize()) {
        val frames = AnimationDefinitions.getDefs(i).frames
        if (frames == null || frames.isEmpty())
            continue
        val frameSet = frames[0] ushr 16
        if (frameSet == 0xFFFF)
            continue
        if (frameSet == skeleton)
            matches.add(i)
    }
    return matches
}

private const val TEST_NPC_ATTR = "developerTestNpc"

private fun getTestNpc(player: Player): NPC? {
    val npc = player.tempAttribs.getO<NPC>(TEST_NPC_ATTR)
    if (npc == null || npc.hasFinished()) {
        player.tempAttribs.removeO<NPC>(TEST_NPC_ATTR)
        return null
    }
    return npc
}
