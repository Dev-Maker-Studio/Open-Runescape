package com.rs.game.content.quests.observatoryquest.dialogues

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.npc.NPC
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile

private val CHATTER_START_DELAY = Ticks.fromSeconds(15)
private val CHATTER_PERIOD = Ticks.fromSeconds(25)

private class ObservatoryGoblin(id: Int, tile: Tile) : NPC(id, tile) {
    private var chatterTimer = CHATTER_START_DELAY
    init {
        setKeepsOwnFacing(true)
        configure()
    }

    override fun getRespawnDirection(): Int = when (id) {
        Npc.qip_obs_cooking_goblin -> Direction.WEST.angle
        Npc.qip_obs_drunken_goblin -> Direction.EAST.angle
        else -> Direction.SOUTH.angle
    }

    override fun processNPC() {
        super.processNPC()
        if (isDead || hasFinished() || --chatterTimer > 0) return
        chatterTimer = CHATTER_PERIOD
        configure()
        chatter()?.let(::forceTalk)
    }

    private fun configure() {
        setRandomWalk(false)
        when (id) {
            Npc.qip_obs_cooking_goblin -> faceDir(Direction.WEST)
            Npc.qip_obs_drunken_goblin -> faceDir(Direction.EAST)
            Npc.qip_obs_goblin_setting_a_fire -> faceDir(Direction.SOUTH)
        }
    }

    private fun chatter(): String? = when (id) {
        Npc.qip_obs_goblin_setting_a_fire -> if (Utils.random(2) == 0) "Come on! Please light!" else "Was that a spark?"
        Npc.qip_obs_drunken_goblin -> if (Utils.random(2) == 0) "La la la. Do di dum dii!" else "Doh ray meeee laa doh faaa!"
        Npc.qip_obs_cooking_goblin -> when (Utils.random(3)) {
            0 -> "This is gonna taste sooo good"
            1 -> "Cook, cook, cook!"
            else -> "I'm so hungry!"
        }
        else -> null
    }
}

@ServerStartupEvent
fun mapObservatoryGoblinDialogues() {
    instantiateNpc(Npc.qip_obs_cooking_goblin, Npc.qip_obs_drunken_goblin, Npc.qip_obs_goblin_setting_a_fire) { id, tile -> ObservatoryGoblin(id, tile) }
    onNpcClick(Npc.qip_obs_goblin_setting_a_fire, options = arrayOf("Talk-to")) { (player) ->
        player.startConversation {
            player(HeadE.CALM_TALK, "Where did you get that lens?")
            npc(Npc.qip_obs_goblin_setting_a_fire, HeadE.CALM_TALK, "From that strange metal thing up on the hill.")
            player(HeadE.CALM_TALK, "You should give that back!")
            npc(Npc.qip_obs_goblin_setting_a_fire, HeadE.CALM_TALK, "Even if it's cracked?")
            player(HeadE.CALM_TALK, "Ah, well, I suppose it's of no use. But, still.")
        }
    }

    onNpcClick(Npc.qip_obs_drunken_goblin, options = arrayOf("Talk-to")) { (player) ->
        player.startConversation {
            player(HeadE.CALM_TALK, "Hi there.")
            npc(Npc.qip_obs_drunken_goblin, HeadE.CALM_TALK, "Hey, ids me matesh!")
            player(HeadE.CALM_TALK, "Sorry, have we met?")
            npc(Npc.qip_obs_drunken_goblin, HeadE.CALM_TALK, "Yeah! you wazsh wiv me in dat pub overy by hill!")
            player(HeadE.CALM_TALK, "I have no idea what you're going on about.")
            npc(Npc.qip_obs_drunken_goblin, HeadE.CALM_TALK, "Glad yeeash remembers.")
        }
    }

    onNpcClick(Npc.qip_obs_cooking_goblin, options = arrayOf("Talk-to")) { (player) ->
        player.startConversation {
            player(HeadE.CALM_TALK, "Hello.")
            npc(Npc.qip_obs_cooking_goblin, HeadE.CALM_TALK, "Shush! I'm concentrating.")
            player(HeadE.CALM_TALK, "Oh, sorry.")
        }
    }
}
