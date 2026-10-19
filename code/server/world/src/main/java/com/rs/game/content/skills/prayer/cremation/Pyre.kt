package com.rs.game.content.skills.prayer.cremation

import org.darkan.core.cache.ItemDefinitions
import com.rs.game.content.quests.shadesofmortton.onMorttonShadeCremated
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import com.rs.game.World.addGroundItem
import com.rs.game.World.getClosestObjectByObjectId
import com.rs.game.World.sendSpotAnim
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.model.gameobject.OwnedObject
import org.darkan.core.Constants
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

class Pyre(player: Player?, obj: GameObject?, val log: PyreLog, val isShadePyre: Boolean) : OwnedObject(player, GameObject(obj, if (isShadePyre) log.shadeNoCorpse else log.vyreNoCorpse)) {
    private var corpse: Corpse? = null
    private var life = 50
    private var lit = false

    override fun tick(owner: Player) {
        if (life-- <= 0) destroy()
    }

    fun setCorpse(corpse: Corpse): Boolean {
        if (lit || this.corpse != null || !log.validCorpse(corpse) || (corpse == Corpse.VYRE && isShadePyre)) return false
        this.corpse = corpse
        changeId(if (isShadePyre) log.shadeCorpse else log.vyreCorpse)
        life = 50
        return true
    }

    override fun onDestroy() {
        if (lit) return
        addGroundItem(Item(log.itemId), coordFace, owner)
        if (corpse != null) addGroundItem(Item(corpse!!.itemIds[0]), coordFace, owner)
    }

    fun light(player: Player): Boolean {
        if (!ownedBy(player)) {
            player.sendMessage("That's not your pyre!")
            return false
        }
        if (corpse == null || lit) return false
        if (!player.inventory.containsItem(Obj.tinderbox, 1)) {
            player.sendMessage("You need a tinderbox to do that.")
            return false
        }
        if (player.skills.getLevel(Constants.FIREMAKING) < log.level) {
            player.sendMessage("You need a Firemaking level of ${log.level} to burn these logs.")
            return false
        }
        player.sendMessage("You attempt to light the funeral pyre.")
        life = 50
        lit = true
        player.lock()
        player.schedule {
            player.anim(Seq.bonfire_player_lighting_fire_2012_update)
            wait(1)
            sendSpotAnim(coordFace, Spotanim.burn_firewarrior)
            ReleasedSpirit(player, coordFace, isShadePyre)
            player.skills.addXp(Constants.FIREMAKING, log.xp)
            player.skills.addXp(Constants.PRAYER, corpse!!.xp)
            if (isShadePyre && corpse == Corpse.LOAR) player.onMorttonShadeCremated()
            wait(2)
            this@Pyre.destroy()
            wait(1)
            player.incrementCount(ItemDefinitions.getDefs(corpse!!.itemIds[0]).name + " cremated")
            player.unlock()
            val stand: GameObject = getClosestObjectByObjectId(if (isShadePyre) Loc.pyre_stand else Loc.myq4_pyre_stand, coordFace) ?: return@schedule
            sendSpotAnim(stand.tile, Spotanim.my4q_stone_stand_smoke_puff)
            val rewards = corpse!!.getKeyDrop(player, log).filterNotNull()
            for (item in rewards) addGroundItem(item, stand.tile, player)
            if (isShadePyre && rewards.isNotEmpty()) player.sendMessage("A reward appears on the stand")
        }
        return true
    }

    private class ReleasedSpirit(owner: Player?, tile: Tile?, shade: Boolean) : OwnedNPC(owner, if (shade) Npc.shade_heaven else Npc.npc_7687, tile, false) {
        private var life = if (shade) 6 else 12

        override fun processNPC() {
            if (life-- <= 0) finish()
        }
    }
}
