package com.rs.game.content.quests.deserttreasure.objects

import com.rs.engine.quest.Quest
import com.rs.game.content.quests.deserttreasure.STAGE_IFTAH_YA_SIMSIM
import com.rs.game.content.quests.deserttreasure.STAGE_RETURN_TO_JALDRAOCHT
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onLogin
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

private const val MAGIC_REQUIREMENT = 50

private const val BLOOD_PLACED = "dtBloodDiamondPlaced"
private const val SMOKE_PLACED = "dtSmokeDiamondPlaced"
private const val ICE_PLACED = "dtIceDiamondPlaced"
private const val SHADOW_PLACED = "dtShadowDiamondPlaced"

private data class Pillar(val objectId: Int, val x: Int, val y: Int, val diamondId: Int, val varbit: Int, val placedKey: String)

private val pillars = arrayOf(
    Pillar(Loc.loc_6482, 3220, 2909, Obj.fd_blood_diamond, Varbit.fd_column_blood, BLOOD_PLACED),
    Pillar(Loc.loc_6485, 3244, 2909, Obj.fd_diamond_fire, Varbit.fd_column_fire, SMOKE_PLACED),
    Pillar(Loc.loc_6488, 3244, 2885, Obj.fd_icediamond, Varbit.fd_column_ice, ICE_PLACED),
    Pillar(Loc.loc_6491, 3220, 2885, Obj.fd_dark_diamond, Varbit.fd_column_shadow, SHADOW_PLACED)
)

@ServerStartupEvent
fun mapDesertTreasurePillars() {
    onItemOnObject(
        objectNamesOrIds = pillars.map { it.objectId }.toTypedArray(),
        itemNamesOrIds = pillars.map { it.diamondId }.toTypedArray()
    ) { (player, obj, item) ->
        val pillar = pillars.firstOrNull {
            it.objectId == obj.id &&
                it.x == obj.x &&
                it.y == obj.y &&
                obj.plane == 0
        } ?: return@onItemOnObject

        placeDiamond(player, pillar, item.id)
    }

    onLogin { (player) ->
        syncPillarVarbits(player)
    }
}

private fun placeDiamond(player: Player, pillar: Pillar, itemId: Int) {
    if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_IFTAH_YA_SIMSIM) {
        return
    }

    if (player.skills.getLevel(Skills.MAGIC) < MAGIC_REQUIREMENT) {
        player.sendMessage("You are not a powerful enough mage to breach the protective aura.")
        player.sendMessage("You need a magic level of at least 50 to enter the Pyramid.")
        return
    }

    if (itemId != pillar.diamondId) {
        player.sendMessage("That doesn't appear to be the correct diamond...")
        return
    }

    if (player.getBool(pillar.placedKey)) {
        return
    }

    if (!player.containsItem(pillar.diamondId)) {
        return
    }

    player.inventory.deleteItem(pillar.diamondId, 1)
    player.set(pillar.placedKey, true)

    player.vars.setVarBit(pillar.varbit, 1)

    player.sendMessage("The diamond is absorbed into the pillar...")

    if (allDiamondsPlaced(player)) {
        player.sendMessage("The force preventing access to the Pyramid has now vanished.")
        player.setQuestStage(Quest.DESERT_TREASURE, STAGE_RETURN_TO_JALDRAOCHT)
    }
}

private fun syncPillarVarbits(player: Player) {
    pillars.forEach { pillar ->
        player.vars.setVarBit(pillar.varbit, if (player.getBool(pillar.placedKey)) 1 else 0)
    }
}

private fun allDiamondsPlaced(player: Player): Boolean =
    player.getBool(BLOOD_PLACED) &&
        player.getBool(SMOKE_PLACED) &&
        player.getBool(ICE_PLACED) &&
        player.getBool(SHADOW_PLACED)
