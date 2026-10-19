package com.rs.game.content.skills.prayer

import com.rs.game.World
import com.rs.game.content.skills.prayer.Burying.Bone
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Constants
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim

private enum class Altar(val loc: Int, val xpMul: Float) {
    OAK(Loc.poh_altar_saradomin_1, 2.0f),
    TEAK(Loc.poh_altar_saradomin_2, 2.1f),
    CLOTH(Loc.poh_altar_saradomin_3, 2.25f),
    MAHOGANY(Loc.poh_altar_saradomin_4, 2.5f),
    LIMESTONE(Loc.poh_altar_saradomin_5, 2.75f),
    MARBLE(Loc.poh_altar_saradomin_6, 3.0f),
    GILDED(Loc.poh_altar_saradomin_7, 3.5f);

    companion object {
        val byLoc = entries.associateBy { it.loc }
    }
}

@ServerStartupEvent
fun mapBoneAltar() {
    onItemOnObject(Altar.entries.map { it.loc }.toTypedArray<Any>(), Bone.entries.map { it.id }.toTypedArray<Any>()) { (player, obj, item) ->
        val altar = Altar.byLoc[obj.id] ?: return@onItemOnObject
        val bone = Bone.forId(item.id) ?: return@onItemOnObject
        player.actionManager.setAction(BoneAction(altar, bone, obj))
    }

    onObjectClick("Altar", "Chaos altar", "Altar of guthix") { (player, obj) ->
        if (obj.id == Loc.dt_zaros_altar)
            return@onObjectClick
        val defs = obj.getDefinitions(player)
        if (defs.hasOption(1, "Pray") || defs.hasOption(1, "Pray-at") || defs.hasOption(1, "Recharge"))
            player.prayer.worshipAltar()
    }
}

private class BoneAction(private val altar: Altar, private val bone: Bone, private val obj: GameObject) : PlayerAction() {
    override fun start(player: Player) = player.inventory.containsItem(bone.id, 1)

    override fun process(player: Player) = player.inventory.containsItem(bone.id, 1)

    override fun processWithDelay(player: Player): Int {
        if (player.inventory.containsItem(bone.id, 1)) {
            player.incrementCount(ItemDefinitions.getDefs(bone.id).name + " offered at altar")
            player.inventory.deleteItem(bone.id, 1)
            player.skills.addXp(Constants.PRAYER, bone.experience * altar.xpMul)
            player.anim(Seq.human_bone_sacrifice)
            World.sendSpotAnim(obj.tile, Spotanim.bone_sacrifice_magic)
        }
        return 2
    }

    override fun stop(player: Player) {}
}
