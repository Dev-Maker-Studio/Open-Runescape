package com.rs.game.content.quests.watchtower

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.model.Item
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnNpc
import com.rs.game.tasks.WorldTasks
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit

val WATCHTOWER_SHAMAN_VARBITS = mapOf(
    Npc.npc_5175 to Varbit.watchtower_shaman_1,
    Npc.npc_5180 to Varbit.watchtower_shaman_2,
    Npc.npc_5183 to Varbit.watchtower_shaman_3,
    Npc.npc_5186 to Varbit.watchtower_shaman_4,
    Npc.npc_5189 to Varbit.watchtower_shaman_5,
    Npc.npc_5192 to Varbit.watchtower_shaman_6,
    Npc.qip_watchtower_ogre_shaman_01_normal to Varbit.watchtower_shaman_1,
    Npc.qip_watchtower_ogre_shaman_02_normal to Varbit.watchtower_shaman_2,
    Npc.qip_watchtower_ogre_shaman_03_normal to Varbit.watchtower_shaman_3,
    Npc.qip_watchtower_ogre_shaman_04_normal to Varbit.watchtower_shaman_4,
    Npc.qip_watchtower_ogre_shaman_05_normal to Varbit.watchtower_shaman_5,
    Npc.qip_watchtower_ogre_shaman_06_normal to Varbit.watchtower_shaman_6
)

private val SHAMAN_DESTROYED_LINES = listOf(
    "That's one destroyed...",
    "That's the second one gone...",
    "That's the third one dealt with...",
    "There goes the fourth...",
    "That's five! Only one more left now..."
)

@ServerStartupEvent
fun mapWatchtowerPotion() {
    onItemOnItem(intArrayOf(Obj.pestle_and_mortar), intArrayOf(Obj.bat_bones)) { e ->
        val player = e.player
        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_MAKE_OGRE_POTION) return@onItemOnItem
        player.inventory.deleteItem(Obj.bat_bones, 1)
        player.inventory.addItem(Obj.ground_bat_bones, 1)
        player.anim(Seq.human_herbing_grind)
        player.sendMessage("You grind the bat bones.")
    }

    onItemOnItem(intArrayOf(Obj.jangerberries), intArrayOf(Obj.obj_91, Obj.vial_water, Obj.jangervial, Obj.guamjangervial)) { e ->
        val player = e.player
        if (player.getQuestStage(Quest.WATCHTOWER) !in STAGE_MAKE_OGRE_POTION..STAGE_DEFEAT_SHAMANS) return@onItemOnItem
        if (!canMixPotion(player)) return@onItemOnItem
        val vial = e.getUsedWith(Obj.jangerberries) ?: return@onItemOnItem
        if (vial.id != Obj.obj_91 && vial.id != Obj.vial_water) {
            explodePotion(player, Obj.jangerberries, vial)
            return@onItemOnItem
        }
        player.inventory.deleteItem(Obj.jangerberries, 1)
        vial.id = if (vial.id == Obj.obj_91) Obj.guamjangervial else Obj.jangervial
        player.inventory.refresh()
        player.anim(Seq.human_herbing_vial)
    }

    onItemOnItem(intArrayOf(Obj.ground_bat_bones), intArrayOf(Obj.obj_91, Obj.vial_water, Obj.jangervial, Obj.guamjangervial)) { e ->
        val player = e.player
        if (player.getQuestStage(Quest.WATCHTOWER) !in STAGE_MAKE_OGRE_POTION..STAGE_DEFEAT_SHAMANS) return@onItemOnItem
        if (!canMixPotion(player)) return@onItemOnItem
        val vial = e.getUsedWith(Obj.ground_bat_bones) ?: return@onItemOnItem
        if (vial.id != Obj.guamjangervial) {
            explodePotion(player, Obj.ground_bat_bones, vial)
            return@onItemOnItem
        }
        player.inventory.deleteItem(Obj.ground_bat_bones, 1)
        vial.id = Obj.ogre_potion
        player.inventory.refresh()
        player.anim(Seq.human_herbing_vial)
    }

    onItemOnItem(intArrayOf(Obj.guam_leaf), intArrayOf(Obj.jangervial, Obj.guamjangervial)) { e ->
        if (e.player.getQuestStage(Quest.WATCHTOWER) !in STAGE_MAKE_OGRE_POTION..STAGE_DEFEAT_SHAMANS) return@onItemOnItem
        val vial = e.getUsedWith(Obj.guam_leaf) ?: return@onItemOnItem
        explodePotion(e.player, Obj.guam_leaf, vial)
    }

    onItemOnNpc(*WATCHTOWER_SHAMAN_VARBITS.keys.toTypedArray<Any>()) { (player, item, npc) ->
        if (item.id != Obj.magic_ogre_potion || player.getQuestStage(Quest.WATCHTOWER) != STAGE_DEFEAT_SHAMANS) return@onItemOnNpc
        if (player.skills.getLevel(Skills.MAGIC) < REQUIRED_MAGIC_LEVEL) {
            player.sendMessage("You need a Magic level of $REQUIRED_MAGIC_LEVEL to use this potion.")
            return@onItemOnNpc
        }
        val varbit = WATCHTOWER_SHAMAN_VARBITS[npc.id] ?: return@onItemOnNpc
        if (player.vars.getVarBit(varbit) != 0) return@onItemOnNpc
        player.faceEntityTile(npc)
        player.anim(Seq.qip_watchtower_pour_vial)
        player.lock(7)
        val meltingShaman = OwnedNPC(player, Npc.watchtower_cutscene_ogre_shaman, npc.tile, true)
        meltingShaman.setRandomWalk(false)
        meltingShaman.lock(5)
        meltingShaman.faceEntityTile(player)
        meltingShaman.anim(Seq.chieftan_ogre_melting)
        WorldTasks.schedule(5) { meltingShaman.finish() }
        player.sendMessage("You pour some of the potion onto the shaman.")
        player.vars.saveVarBit(varbit, 1)
        val destroyed = WATCHTOWER_SHAMAN_VARBITS.values.distinct().count { player.vars.getVarBit(it) != 0 }
        if (destroyed == WATCHTOWER_SHAMAN_VARBITS.values.distinct().size) {
            finishShamans(player)
            return@onItemOnNpc
        }
        SHAMAN_DESTROYED_LINES.getOrNull(destroyed - 1)?.let { player.startConversation { player(HeadE.HAPPY_TALKING, it) } }
    }
}

private fun canMixPotion(player: Player): Boolean {
    if (player.skills.getLevel(Skills.HERBLORE) >= REQUIRED_HERBLORE_LEVEL) return true
    player.sendMessage("You need a Herblore level of $REQUIRED_HERBLORE_LEVEL to mix this potion.")
    return false
}

private fun explodePotion(player: Player, wrongIngredient: Int, vial: Item) {
    player.startConversation { player(HeadE.SCARED, "OW! That hurt!") }
    player.inventory.deleteItem(wrongIngredient, 1)
    vial.id = Obj.vial_empty
    player.inventory.refresh()
    player.applyHit(Hit(player, 100, Hit.HitLook.TRUE_DAMAGE))
}

private fun finishShamans(player: Player) {
    player.inventory.deleteItem(Obj.magic_ogre_potion, 1)
    player.inventory.addItem(Obj.vial_empty, 1)
    if (!player.inventory.containsItem(Obj.powering_crystal3, 1)) player.inventory.addItemDrop(Obj.powering_crystal3, 1)
    player.sendMessage("A crystal drops from the hands of the disappearing ogre. You snatch it up quickly.")
    player.setQuestStage(Quest.WATCHTOWER, STAGE_RECOVER_CRYSTALS)
}
