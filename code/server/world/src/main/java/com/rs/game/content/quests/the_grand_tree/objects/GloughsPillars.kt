package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import com.rs.game.World
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import org.darkan.core.model.GroundItem
import org.darkan.core.model.GroundItemType
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onPickupItem

private data class PillarTwig(val pillar: Int, val twig: Int, val attribute: String, val tile: Tile)

private const val ATTR_PILLAR_TWIG = "grandTreePillarTwig"

private val PILLAR_TWIGS = listOf(
    PillarTwig(Loc.grandtree_pillart, Obj.grandtree_twigt, ATTR_TWIG_T, Tile.of(2485, 3467, 2)),
    PillarTwig(Loc.grandtree_pillaru, Obj.grandtree_twigu, ATTR_TWIG_U, Tile.of(2486, 3467, 2)),
    PillarTwig(Loc.grandtree_pillarz, Obj.grandtree_twigz, ATTR_TWIG_Z, Tile.of(2487, 3467, 2)),
    PillarTwig(Loc.grandtree_pillaro, Obj.grandtree_twigo, ATTR_TWIG_O, Tile.of(2488, 3467, 2))
)

private val PILLAR_TILES = PILLAR_TWIGS.map { it.tile }.toTypedArray()
private val PILLAR_CHUNK = PILLAR_TWIGS.first().tile.getChunkId()

@ServerStartupEvent
fun mapGloughsPillars() {
    onItemOnObject(PILLAR_TWIGS.map { it.pillar as Any }.toTypedArray(), GRAND_TREE_TWIGS.map { it as Any }.toTypedArray()) { (player, obj, item) ->
        val match = PILLAR_TWIGS.firstOrNull { it.pillar == obj.id }
        if (player.grandTreeStage != STAGE_INVASION_PLANS || !player.grandTreeAttribs().getB(ATTR_RECEIVED_TWIGS)) {
            player.sendMessage("Nothing interesting happens.")
            return@onItemOnObject
        }
        if (match == null || match.twig != item.id) {
            player.sendMessage("The twig doesn't seem to fit this pillar.")
            return@onItemOnObject
        }
        if (player.grandTreeAttribs().getB(match.attribute)) {
            player.sendMessage("That twig is already in place.")
            return@onItemOnObject
        }
        player.inventory.deleteItem(item.id, 1)
        player.grandTreeAttribs().setB(match.attribute, true)
        showPlacedTwig(player, match)
        player.sendMessage("You place the twig on the pillar.")
        if (player.hasPlacedAllTwigs()) {
            player.sendMessage("You can hear the grinding of an ancient pulley system.")
            player.setGrandTreeStage(STAGE_OPENED_TRAPDOOR)
        }
    }

    onPickupItem(*GRAND_TREE_TWIGS.map { it as Any }.toTypedArray(), tiles = PILLAR_TILES) { event ->
        val match = PILLAR_TWIGS.firstOrNull {
            it.twig == event.item.id && it.tile.matches(event.item.tile)
        } ?: return@onPickupItem
        if (event.item.getMetaData(ATTR_PILLAR_TWIG) != true)
            return@onPickupItem
        if (event.player.grandTreeStage != STAGE_INVASION_PLANS) {
            event.cancelPickup()
            event.player.sendMessage("The twigs are locked in place.")
            return@onPickupItem
        }
        if (!event.player.inventory.hasRoomFor(event.item.item)) {
            event.cancelPickup()
            event.player.sendMessage("Not enough space in your inventory.")
            return@onPickupItem
        }
        event.cancelPickup()
        event.item.item.deleteMetaData()
        if (World.removeGroundItem(event.player, event.item, true))
            event.player.grandTreeAttribs().setB(match.attribute, false)
        else
            event.item.addMetaData(ATTR_PILLAR_TWIG, true)
    }

    onChunkEnter { (entity, chunkId) ->
        val player = entity as? Player ?: return@onChunkEnter
        if (chunkId == PILLAR_CHUNK && player.grandTreeStage in STAGE_INVASION_PLANS..STAGE_OPENED_TRAPDOOR)
            showPlacedTwigs(player)
        else
            removePlacedTwigs(player)
    }
}

private fun showPlacedTwigs(player: Player) {
    val attributes = player.grandTreeAttribs()
    PILLAR_TWIGS.filter { attributes.getB(it.attribute) }.forEach { showPlacedTwig(player, it) }
}

private fun showPlacedTwig(player: Player, pillarTwig: PillarTwig) {
    val chunk = ChunkManager.getChunk(pillarTwig.tile.getChunkId())
    val existing = chunk.getGroundItem(pillarTwig.twig, pillarTwig.tile, player)
    if (existing?.getMetaData(ATTR_PILLAR_TWIG) == true)
        return
    val groundItem = GroundItem(
        Item(pillarTwig.twig).addMetaData(ATTR_PILLAR_TWIG, true),
        pillarTwig.tile,
        player.username,
        GroundItemType.INVISIBLE
    )
    chunk.addGroundItem(groundItem)
}

private fun removePlacedTwigs(player: Player) {
    val chunk = ChunkManager.getChunk(PILLAR_CHUNK)
    PILLAR_TWIGS.forEach {
        val groundItem = chunk.getGroundItem(it.twig, it.tile, player)
        if (groundItem?.getMetaData(ATTR_PILLAR_TWIG) == true)
            World.removeGroundItem(groundItem)
    }
}
