package com.rs.game.content.quests.insearchofthemyreque

import com.rs.game.World
import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.npcCombat
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapMyrequeHellhound() {
    npcCombat(Npc.skeleton_hellhound) { npc, target ->
        npc.anim(Seq.dog_update_skeleton_hellhound_attack)
        CombatScript.delayHit(npc, 0, target, Hit.melee(npc, CombatScript.getMaxHit(npc, 120, CombatStyle.MELEE, target)))
        4
    }
}

internal fun spawnMyrequeHellhound(player: Player) {
    if (player.myrequeStage != HELLHOUND || !player.inMyrequeRoom() || player.tempAttribs.getB("myrequeAmbush") || player.isDead) return
    val old = player.tempAttribs.getO<MyrequeHellhound>("myrequeHound")
    if (old != null && !old.hasFinished()) return
    val destination = myrequeWalkable(Tile.of(3507, 9835, 0), 2) ?: return
    player.tempAttribs.setO<MyrequeHellhound>("myrequeHound", MyrequeHellhound(player, destination).apply { setCombatTarget(player) })
}

internal class MyrequeHellhound(owner: Player, tile: Tile) : OwnedNPC(owner, Npc.skeleton_hellhound, tile, true) {
    private var credited = false
    init {
        setRandomWalk(false)
    }
    override fun canBeAttackedBy(player: Player): Boolean = player === owner && owner.myrequeStage == HELLHOUND && super.canBeAttackedBy(player)
    override fun drop() { }
    override fun processNPC() {
        if (owner.isDead || owner.hasFinished() || (!isDead && (owner.myrequeStage != HELLHOUND || !owner.inMyrequeRoom()))) { finish(); return }
        super.processNPC()
    }
    override fun sendDeath(source: Entity) {
        if (!credited && owner.myrequeStage == HELLHOUND && !owner.hasFinished() && !owner.isDead) {
            credited = true
            owner.advanceMyreque(VICTORY)
            World.addGroundItem(Item(Obj.uncut_ruby, 2), tile, owner)
            World.addGroundItem(Item(Obj.big_bones, 4), tile, owner)
        }
        super.sendDeath(source)
    }
}
