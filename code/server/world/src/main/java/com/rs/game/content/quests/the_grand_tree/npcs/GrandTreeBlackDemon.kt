package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.content.skills.summoning.Familiar
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin

private const val TEMP_BLACK_DEMON = "grandTreeBlackDemon"

private val BLACK_DEMON_SPAWN = Tile.of(2474, 9862, 0)

internal class GrandTreeBlackDemon(owner: Player) : OwnedNPC(owner, Npc.grandtree_blackdemon, BLACK_DEMON_SPAWN, true) {
    init {
        isAutoDespawnAtDistance = false
    }

    override fun processNPC() {
        if (!isDead && (owner.isDead || owner.grandTreeStage != STAGE_OPENED_TRAPDOOR || !owner.isInGrandTreeDungeon())) {
            owner.tempAttribs.removeO<GrandTreeBlackDemon>(TEMP_BLACK_DEMON)
            finish()
            return
        }
        super.processNPC()
    }

    override fun sendDeath(source: Entity?) {
        if ((source === owner || source is Familiar && source.owner === owner) && owner.grandTreeStage == STAGE_OPENED_TRAPDOOR) {
            owner.setGrandTreeStage(STAGE_DEMON_DEFEATED)
            owner.tempAttribs.removeO<GrandTreeBlackDemon>(TEMP_BLACK_DEMON)
            owner.startConversation {
                npc(Npc.grandtree_glough, HeadE.SCARED, "Mummy!")
                simple("Glough's run off!")
            }
            spawnUndergroundNarnode(owner)
        }
        super.sendDeath(source)
    }
}

internal fun spawnGrandTreeBlackDemon(player: Player): GrandTreeBlackDemon {
    val existing = player.tempAttribs.getO<GrandTreeBlackDemon>(TEMP_BLACK_DEMON)
    if (existing != null && !existing.hasFinished())
        return existing
    return GrandTreeBlackDemon(player).also {
        player.tempAttribs.setO<GrandTreeBlackDemon>(TEMP_BLACK_DEMON, it)
        it.combatTarget = player
    }
}

@ServerStartupEvent
fun mapGrandTreeBlackDemonLogin() {
    onLogin { (player) ->
        if (player.isInGrandTreeDungeon() && player.grandTreeStage == STAGE_OPENED_TRAPDOOR)
            spawnGrandTreeBlackDemon(player)
    }
}
