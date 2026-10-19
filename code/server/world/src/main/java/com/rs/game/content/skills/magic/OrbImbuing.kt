package com.rs.game.content.skills.magic

import com.rs.game.content.quests.legendsquest.DRY_POOL
import com.rs.game.content.quests.legendsquest.legends
import com.rs.game.content.quests.legendsquest.legendsStage
import com.rs.game.content.quests.legendsquest.passOrbGate
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onInterfaceOnObject
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile

private enum class Orb(val loc: Int, val component: Int, val level: Int, val xp: Double, val product: Int, val graphic: Int, val rune: Rune) {
    WATER(Loc.obelisk_water, Component.interface_192.component_192_60, 56, 66.0, Obj.water_orb, Spotanim.spotanim_149, Rune.WATER),
    EARTH(Loc.obelisk_earth, Component.interface_192.component_192_64, 60, 70.0, Obj.earth_orb, Spotanim.spotanim_151, Rune.EARTH),
    FIRE(Loc.obelisk_fire, Component.interface_192.component_192_71, 63, 73.0, Obj.fire_orb, Spotanim.spotanim_152, Rune.FIRE),
    AIR(Loc.obelisk_air, Component.interface_192.component_192_74, 66, 76.0, Obj.air_orb, Spotanim.spotanim_150, Rune.AIR);

    fun canCast(player: Player): Boolean {
        if (player.skills.getLevel(Skills.MAGIC) < level) {
            player.sendMessage("You need a magic level of $level to imbue ${name.lowercase()} orbs.")
            return false
        }
        return true
    }
    fun consume(player: Player): Boolean {
        if (!player.inventory.containsItem(Obj.stafforb)) { player.sendMessage("You've run out of orbs to imbue."); return false }
        if (!player.inventory.containsItem(Obj.cosmicrune, 3) || !Magic.checkRunes(player, false, RuneSet(rune, 30))) { player.sendMessage("You have run out of runes."); return false }
        Magic.checkRunes(player, true, RuneSet(rune, 30))
        player.inventory.deleteItem(Obj.cosmicrune, 3)
        player.inventory.deleteItem(Obj.stafforb, 1)
        return true
    }
    fun animate(player: Player, tile: Tile) {
        player.spotAnim(SpotAnim(graphic, 0, 100))
        player.anim(Seq.seq_726)
        player.faceTile(tile)
        player.skills.addXp(Skills.MAGIC, xp)
    }
}

private class OrbChargingAction(private val orb: Orb, private val tile: Tile) : PlayerAction() {
    override fun start(player: Player) = orb.canCast(player)
    override fun process(player: Player) = orb.canCast(player)
    override fun processWithDelay(player: Player): Int {
        if (!orb.consume(player)) return -1
        player.inventory.addItem(orb.product, 1)
        orb.animate(player, tile)
        return 3
    }
    override fun stop(player: Player) {}
}

@ServerStartupEvent
fun mapOrbImbuing() {
    onInterfaceOnObject(fromInterfaceIds = intArrayOf(Interface.interface_192), fromComponentIds = Orb.entries.map { Utils.componentIdFromHash(it.component) }.toIntArray()) { event ->
        val orb = Orb.entries.firstOrNull { Utils.componentIdFromHash(it.component) == event.componentId } ?: return@onInterfaceOnObject
        val player = event.player
        if (event.objectId in intArrayOf(Loc.di2_cave_door_lgmagictrialgatel, Loc.di2_cave_door_lgmagictrialgater)) {
            if (player.legendsStage < DRY_POOL || !orb.canCast(player) || !orb.consume(player)) return@onInterfaceOnObject
            orb.animate(player, event.`object`.tile)
            player.legends.setB("orbGate", true)
            player.sendMessage("The orb attached to the door glows brightly as you charge it. The illusory doors glow, then fade away.")
            passOrbGate(player)
        } else if (event.objectId == orb.loc) player.actionManager.setAction(OrbChargingAction(orb, event.`object`.tile))
        else player.sendMessage("Try using this spell on the correct obelisk.")
    }
}
