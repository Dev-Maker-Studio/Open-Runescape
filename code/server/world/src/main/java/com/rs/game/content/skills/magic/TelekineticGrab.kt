package com.rs.game.content.skills.magic

import com.rs.game.World
import com.rs.game.content.Effect
import com.rs.game.content.minigames.treasuretrails.TreasureTrailsManager
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.PickupItemEvent
import com.rs.plugin.kts.onInterfaceOnGroundItem
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile

private const val LEVEL = 33
private const val XP = 43.0
private const val MAX_DISTANCE = 8

@ServerStartupEvent
fun mapTelekineticGrab() {
    onInterfaceOnGroundItem(fromComponents = intArrayOf(Component.interface_192.component_192_44)) { e ->
        e.player.actionManager.setAction(TelekineticGrabAction(e.item.id, e.item.tile))
    }
}

private class TelekineticGrabAction(private val itemId: Int, private val tile: Tile) : PlayerAction() {
    override fun start(player: Player): Boolean = true

    override fun process(player: Player): Boolean {
        if (player.isDead || player.hasFinished()) return false
        val item = groundItem(player) ?: return false
        if (player.plane != tile.level) return false
        if (player.hasEffect(Effect.FREEZE)) return true
        if (!player.lineOfSightTo(tile, false) || Utils.getDistance(player.tile, tile) > MAX_DISTANCE) {
            if (player.hasWalkSteps()) player.resetWalkSteps()
            player.calcFollow(tile, 25, true)
            return true
        }
        if (TreasureTrailsManager.isScroll(item.id) && player.treasureTrailsManager.hasClueScrollItem()) {
            player.sendMessage("You should finish the clue you are currently doing first.")
            return false
        }
        if (player.canCastSpell() && Magic.checkMagicAndRunes(player, LEVEL, true, RuneSet(Rune.AIR, 1, Rune.LAW, 1))) cast(player)
        return false
    }

    private fun cast(player: Player) {
        setActionDelay(player, 3)
        player.addSpellDelay(2)
        player.resetWalkSteps()
        player.faceTile(tile)
        player.anim(Seq.human_caststrike)
        player.skills.addXp(Constants.MAGIC, XP)
        player.spotAnim(SpotAnim(Spotanim.telegrab_casting, 2, 50, Utils.getAngleTo(tile.x - player.x, tile.y - player.y)))
        World.sendProjectile(player, tile, Spotanim.telegrab_travel, 35 to 0, 60, 10, 0, 0) {
            val item = groundItem(player)
            if (item == null) {
                player.sendMessage("Too late. It's gone!")
                return@sendProjectile
            }
            World.sendSpotAnim(tile, SpotAnim(Spotanim.telegrab_impact))
            val pickup = PickupItemEvent(player, item, true)
            PluginManager.handle(pickup)
            if (!pickup.isCancelPickup) World.removeGroundItem(player, item, true)
        }
    }

    private fun groundItem(player: Player) = ChunkManager.getChunk(tile.getChunkId()).getGroundItem(itemId, tile, player)

    override fun processWithDelay(player: Player): Int = 0

    override fun stop(player: Player) {}
}
