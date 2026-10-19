package com.rs.game.content.quests.the_grand_tree

import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils

internal val GRAND_TREE_TWIGS = intArrayOf(Obj.grandtree_twigt, Obj.grandtree_twigu, Obj.grandtree_twigz, Obj.grandtree_twigo)

internal const val ATTR_ARRESTED = "grandTreeArrested"

internal const val ATTR_SHIPYARD_ACCESS = "shipyardAccess"
internal const val ATTR_CONFRONTED_GLOUGH_ABOUT_FLEET = "confrontedGloughAboutFleet"
internal const val ATTR_WARNED_KING_ABOUT_FLEET = "warnedKingAboutFleet"
internal const val ATTR_CHARLIE_REVEALED_KEY = "charlieRevealedKey"
internal const val ATTR_TWIG_T = "twigT"
internal const val ATTR_TWIG_U = "twigU"
internal const val ATTR_TWIG_Z = "twigZ"
internal const val ATTR_TWIG_O = "twigO"
internal const val ATTR_RECEIVED_TWIGS = "receivedTwigs"

private const val ATTR_DACONIA_ROOT = "daconiaRoot"

internal val Player.grandTreeStage: Int
    get() = questManager.getStage(Quest.GRAND_TREE)

internal fun Player.setGrandTreeStage(stage: Int) = questManager.setStage(Quest.GRAND_TREE, stage)

internal fun Player.grandTreeAttribs() = questManager.getAttribs(Quest.GRAND_TREE)

internal fun Player.isInGrandTreeDungeon() = plane == 0 && x in 2432..2495 && y in 9856..9910

internal fun Player.replaceMissingTwigs(): Boolean {
    val attribs = grandTreeAttribs()
    val missing = GRAND_TREE_TWIGS.filterNot { itemId ->
        inventory.containsOneItem(itemId) || when (itemId) {
            Obj.grandtree_twigt -> attribs.getB(ATTR_TWIG_T)
            Obj.grandtree_twigu -> attribs.getB(ATTR_TWIG_U)
            Obj.grandtree_twigz -> attribs.getB(ATTR_TWIG_Z)
            Obj.grandtree_twigo -> attribs.getB(ATTR_TWIG_O)
            else -> false
        }
    }
    if (inventory.freeSlots < missing.size)
        return false
    missing.forEach { inventory.addItem(it) }
    return true
}

internal val DACONIA_ROOTS = listOf(
    Tile.of(2443, 9878, 0), Tile.of(2452, 9893, 0), Tile.of(2456, 9886, 0),
    Tile.of(2467, 9896, 0), Tile.of(2473, 9897, 0), Tile.of(2485, 9885, 0),
    Tile.of(2490, 9889, 0), Tile.of(2439, 9881, 0), Tile.of(2444, 9893, 0),
    Tile.of(2455, 9874, 0), Tile.of(2457, 9881, 0), Tile.of(2465, 9891, 0),
    Tile.of(2467, 9872, 0), Tile.of(2468, 9890, 0), Tile.of(2481, 9904, 0)
)

internal fun Player.ensureDaconiaRoot(): Int {
    val attribs = grandTreeAttribs()
    var index = attribs.getI(ATTR_DACONIA_ROOT, -1)
    if (index !in DACONIA_ROOTS.indices) {
        index = Utils.random(DACONIA_ROOTS.size)
        attribs.setI(ATTR_DACONIA_ROOT, index)
    }
    return index
}

internal fun Player.hasPlacedAllTwigs(): Boolean {
    val attribs = grandTreeAttribs()
    return attribs.getB(ATTR_TWIG_T) && attribs.getB(ATTR_TWIG_U) &&
            attribs.getB(ATTR_TWIG_Z) && attribs.getB(ATTR_TWIG_O)
}
