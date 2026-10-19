package com.rs.net.decoders.handlers

import com.rs.Settings
import com.rs.engine.quest.Quest
import com.rs.game.content.Effect
import com.rs.game.content.death.GraveStone
import com.rs.game.content.pets.Pet
import com.rs.game.content.quests.piratestreasure.CustomsOfficerPiratesTreasureD
import com.rs.game.content.quests.piratestreasure.PiratesTreasure
import com.rs.game.content.skills.slayer.npcs.ConditionalDeath
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.content.skills.thieving.PickPocketAction
import com.rs.game.content.transportation.BoatingD
import com.rs.game.content.transportation.TravelMethods
import com.rs.game.content.transportation.TravelMethods.Carrier
import com.rs.game.model.entity.interactions.StandardEntityInteraction
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.NPCClickEvent
import com.rs.plugin.events.NPCInteractionDistanceEvent
import com.rs.utils.NPCExamines
import com.rs.utils.Ticks
import org.darkan.core.Logger
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Rights

private const val EXAMINE_COLOUR = 0xFFFFFF
private const val CURE_SPECIAL_COST = 2
private val DROP_SAMPLES = intArrayOf(1000, 5000, 10000)

object NPCHandler {
    @JvmStatic
    fun handleExamine(player: Player, npc: NPC) {
        if (player.hasRights(Rights.DEVELOPER)) {
            player.sendMessage("NPC - [id=${npc.id}, loc=[${npc.x}, ${npc.y}, ${npc.plane}]]. respawnDir=${npc.definitions.respawnDirection}")
            player.sendMessage("HP: ${npc.maxHitpoints}")
            player.sendMessage("Spawn tile [${npc.respawnTile.x}, ${npc.respawnTile.y}, ${npc.respawnTile.plane}]]. ")
        }
        player.packets.sendNPCMessage(0, EXAMINE_COLOUR, npc, "${NPCExamines.getExamine(npc, player)} (${npc.id})")
        if (Settings.getConfig().isDebug && (npc.definitions.hasAttackOption() || npc.definitions.hasOption("Investigate")))
            player.sendOptionDialogue("Would you like to check the drops on this monster?") { ops ->
                for (kills in DROP_SAMPLES) ops.add("Show drops (${"%,d".format(kills)} kills)") { NPC.displayDropsFor(player, npc.id, kills) }
                ops.add("Nevermind")
            }
        Logger.debug(NPCHandler::class.java, "handleExamine", "Examined NPC: index: ${npc.index}, id: ${npc.id}")
    }

    @JvmStatic
    fun handleOption1(player: Player, npc: NPC) {
        if (!begin(player, npc, 1)) return
        approach(player, npc) {
            if (!player.controllerManager.processNPCClick1(npc)) return@approach
            npc.resetWalkSteps()
            player.faceEntityTile(npc)
            npc.faceInteracting(player)
            val click = NPCClickEvent(player, npc, 1, true)
            when {
                click.intercepted() -> return@approach
                BoatingD.getBoatForShip(player, npc.id) != null -> player.startConversation(BoatingD(player, npc.id))
                npc is GraveStone -> {
                    npc.sendGraveInscription(player)
                    npc.resetDirection()
                }
                PluginManager.handle(click) -> return@approach
                npc is Pet ->
                    if (npc != player.pet) player.sendMessage("This isn't your pet.")
                    else {
                        player.anim(Seq.seq_827)
                        npc.pickup()
                    }
                else -> nothingInteresting(player, npc, "handleOption1")
            }
        }
    }

    @JvmStatic
    fun handleOption2(player: Player, npc: NPC) {
        if (!begin(player, npc, 2)) return
        player.interactionManager.setInteraction(StandardEntityInteraction(npc, 0) {
            val click = NPCClickEvent(player, npc, 2, true)
            if (!click.intercepted()) PluginManager.handle(click)
        })
    }

    @JvmStatic
    fun handleOption3(player: Player, npc: NPC) {
        if (player.isLocked && player.actionManager.action is PickPocketAction) return
        if (!begin(player, npc, 3)) return
        approach(player, npc) {
            if (!player.controllerManager.processNPCClick2(npc)) return@approach
            player.faceEntityTile(npc)
            npc.faceInteracting(player)
            val click = NPCClickEvent(player, npc, 3, true)
            if (click.intercepted()) return@approach
            npc.resetWalkSteps()
            val ship = BoatingD.getBoatForShip(player, npc.id)
            when {
                ship != null ->
                    if (npc.id == Npc.customs_officer && player.questManager.getStage(Quest.PIRATES_TREASURE) == PiratesTreasure.SMUGGLE_RUM) player.startConversation(CustomsOfficerPiratesTreasureD(player).start)
                    else TravelMethods.sendCarrier(player, ship[0] as Carrier, ship[1] as Boolean)
                npc is Familiar && npc.definitions.hasOption("cure") -> when {
                    player.familiar != npc -> player.sendMessage("That isn't your familiar.")
                    !player.poison.isPoisoned -> player.sendMessage("Your aren't poisoned or diseased.")
                    else -> {
                        npc.drainSpec(CURE_SPECIAL_COST)
                        player.addEffect(Effect.ANTIPOISON, Ticks.fromMinutes(2).toLong())
                    }
                }
                npc is GraveStone -> npc.repair(player, false)
                npc.id == Npc.npc_14849 && npc is ConditionalDeath -> npc.useHammer(player)
                PluginManager.handle(click) -> return@approach
                else -> nothingInteresting(player, npc, "handleOption3")
            }
        }
    }

    @JvmStatic
    fun handleOption4(player: Player, npc: NPC) = graveOption(player, npc, 4) { it.repair(player, true) }

    @JvmStatic
    fun handleOption5(player: Player, npc: NPC) = graveOption(player, npc, 5) { it.demolish(player) }

    private fun graveOption(player: Player, npc: NPC, opNum: Int, onGrave: (GraveStone) -> Unit) {
        if (!begin(player, npc, opNum)) return
        approach(player, npc) {
            if (!player.controllerManager.processNPCClick3(npc)) return@approach
            npc.resetWalkSteps()
            player.faceEntityTile(npc)
            npc.faceInteracting(player)
            val click = NPCClickEvent(player, npc, opNum, true)
            when {
                click.intercepted() -> return@approach
                npc is GraveStone -> {
                    onGrave(npc)
                    npc.resetDirection()
                }
                PluginManager.handle(click) -> return@approach
                else -> nothingInteresting(player, npc, "handleOption$opNum")
            }
        }
    }

    private fun begin(player: Player, npc: NPC, opNum: Int): Boolean {
        player.stopAll(true)
        return !PluginManager.handle(NPCClickEvent(player, npc, opNum, false))
    }

    private fun approach(player: Player, npc: NPC, onReached: () -> Unit) {
        val distance = PluginManager.getObj(NPCInteractionDistanceEvent(player, npc)) as? Int ?: 0
        player.interactionManager.setInteraction(StandardEntityInteraction(npc, distance, onReached))
    }

    private fun nothingInteresting(player: Player, npc: NPC, source: String) {
        player.sendMessage("Nothing interesting happens.")
        Logger.debug(NPCHandler::class.java, source, "NPC: ${npc.id}, (${npc.x}, ${npc.y}, ${npc.plane})")
    }
}
