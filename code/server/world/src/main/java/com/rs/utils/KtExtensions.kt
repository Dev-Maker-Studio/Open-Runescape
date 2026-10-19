package com.rs.utils

import com.rs.game.model.entity.Entity
import org.darkan.core.cache.AnimationDefinitions
import org.darkan.core.gameval.gen.Param
import org.darkan.core.model.Animation
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.cache.definition.data.AnimationDefinition
import world.gregs.voidps.cache.definition.data.EnumDefinition
import world.gregs.voidps.cache.definition.data.ItemDefinition
import world.gregs.voidps.cache.definition.data.BASDefinition
import world.gregs.voidps.cache.definition.data.NPCDefinition
import world.gregs.voidps.cache.definition.data.ObjectDefinition
import world.gregs.voidps.cache.definition.data.StructDefinition
import world.gregs.voidps.type.Tile
import org.darkan.core.model.Bonus
import org.darkan.core.model.Item
import org.darkan.core.Utils.getDistance
import org.darkan.core.model.MoveType

fun Set<Entity>.closestOrNull(tile: Tile): Entity? = this.minByOrNull { entity -> getDistance(entity.tile, tile) }

/**
 * Checks if this tile is at the given x,y coordinates (ignoring plane).
 */
fun Tile.isAt(x: Int, y: Int): Boolean = this.x == x && this.y == y

/**
 * Checks if this tile is at the given x,y,plane coordinates.
 */
fun Tile.isAt(x: Int, y: Int, plane: Int): Boolean = this.x == x && this.y == y && this.plane == plane

/**
 * Checks if this tile is within the rectangular area defined by two corners (x1,y1) to (x2,y2).
 */
fun Tile.withinArea(x1: Int, y1: Int, x2: Int, y2: Int): Boolean =
    this.x >= x1 && this.x <= x2 && this.y >= y1 && this.y <= y2

/**
 * Gets the chunk X coordinate (x / 8).
 */
val Tile.chunkX: Int get() = x shr 3

/**
 * Gets the chunk Y coordinate (y / 8).
 */
val Tile.chunkY: Int get() = y shr 3

/**
 * Gets the local hash within the chunk (encodes localX, localY, and plane).
 * Format: localX | (localY << 4) | (plane << 8)
 */
val Tile.chunkLocalHash: Int get() = (x and 7) or ((y and 7) shl 4) or (plane shl 8)

// hasAttackOption() and hasOption() are now member functions on NPCDefinition

/**
 * Gets the option at the given index (1-based) from the object definition.
 * Returns null if the index is out of bounds or options are null.
 */

val ObjectDefinition.clipType: Int get() = solid

fun ObjectDefinition.blocks(): Boolean = (block and ObjectDefinition.PROJECTILE) != 0

/**
 * Whether this object should be ignored by the pathfinder.
 */
val ObjectDefinition.ignoresPathfinder: Boolean get() = ignoreOnRoute

/**
 * Gets the object shape types from the model types as an array of ObjectShape.
 * Legacy compatibility for the old `types` property.
 */
val ObjectDefinition.types: Array<ObjectShape> get() =
    modelTypes?.map { ObjectShape.forId(it.toInt()) ?: ObjectShape.SCENERY_INTERACT }?.toTypedArray()
        ?: arrayOf(ObjectShape.SCENERY_INTERACT)

/**
 * Gets the AnimationDefinition for this Animation's primary ID.
 */
val Animation.defs: AnimationDefinition get() = AnimationDefinitions.getDefs(primaryId)

/**
 * Total emote duration in game ticks. One duration unit is one client cycle (20ms),
 * and one game tick is 30 client cycles.
 */
val AnimationDefinition.emoteGameTicks: Int get() = (durations?.sum() ?: 0) / 30

// Most ItemDefinition extensions have been migrated to member functions on the class.
// Remaining ones that are only needed from Kotlin:

/**
 * Legacy alias for wearPos - the equipment slot this item occupies.
 */
val ItemDefinition.equipSlot: Int get() = wearPos

/**
 * Legacy alias for primaryMaleModel - the primary male equipped model.
 */
val ItemDefinition.maleEquip1: Int get() = primaryMaleModel

/**
 * Legacy alias for secondaryMaleModel - the secondary male equipped model.
 */
val ItemDefinition.maleEquip2: Int get() = secondaryMaleModel

/**
 * Gets a parameter value as Int from the params map.
 * Legacy replacement for getParamVal(key).
 */
fun ItemDefinition.getParamVal(key: Int): Int = params?.get(key) as? Int ?: 0

// --- NPCDefinition extensions ---

// combatLevel is now a member property on NPCDefinition

/**
 * Gets compatible animations for this NPC definition by looking up its BAS (renderEmote).
 * Returns the BASDefinition associated with the NPC's render emote, or null if none.
 */
val NPCDefinition.compatibleAnimations: BASDefinition? get() =
    if (renderEmote != -1) org.darkan.core.cache.BASDefinitions.getDefs(renderEmote) else null

/**
 * Gets the modified colors (recolored) of the NPC definition.
 * Legacy alias for modifiedColours.
 */
val NPCDefinition.modifiedColors: ShortArray? get() = modifiedColours

/**
 * Gets the modified textures of the NPC definition.
 * Legacy alias for modifiedTextureColours.
 */
val NPCDefinition.modifiedTextures: ShortArray? get() = modifiedTextureColours

val NPCDefinition.isUndead: Boolean get() = (params?.get(Param.undead) as? Int) == 1

// --- EnumDefinition extensions ---

/**
 * Legacy alias for `map` property - the key-value map in enum definitions.
 */
val EnumDefinition.values: Map<Int, Any> get() = map ?: emptyMap()

// getStringValue() and getIntValue() are now member functions on EnumDefinition

// getStringValue() and getIntValue() are now member functions on StructDefinition

// isDestroyItem, canExchange, getWearingSkillRequiriments, getWieldQuestReq, value/getValue
// are now member functions on ItemDefinition

/**
 * Legacy alias for `cost` - the item's value/price. Kotlin-only extension.
 */
val ItemDefinition.value: Int get() = cost

// --- NPCDefinition extensions (additional) ---

// getBonus() is now a member function on NPCDefinition

// isEquipType, containsOption, isWearItem, getEquipmentOption, getSellPrice, getHighAlchPrice
// are now member functions on ItemDefinition

// getInventoryOption is now a member function on ItemDefinition


// --- Additional NPCDefinition extensions for Appearance compat ---


/**
 * Legacy field: NPC walking animation. Maps to renderEmote lookup via BAS.
 */
val NPCDefinition.walkingAnimation: Int get() {
    val bas = compatibleAnimations ?: return -1
    return bas.walkAnimation
}

/**
 * Legacy field: NPC rotate 180 animation. Maps to renderEmote lookup via BAS.
 */
val NPCDefinition.rotate180Animation: Int get() {
    val bas = compatibleAnimations ?: return -1
    return bas.walkDir1
}

/**
 * Legacy field: NPC rotate 90 right animation. Maps to renderEmote lookup via BAS.
 */
val NPCDefinition.rotate90RightAnimation: Int get() {
    val bas = compatibleAnimations ?: return -1
    return bas.walkDir2
}

/**
 * Legacy field: NPC rotate 90 left animation. Maps to renderEmote lookup via BAS.
 */
val NPCDefinition.rotate90LeftAnimation: Int get() {
    val bas = compatibleAnimations ?: return -1
    return bas.walkDir3
}

/**
 * Legacy field: special byte for transformed NPC appearance. Maps to size.
 */
val NPCDefinition.specialByte: Int get() = size

/**
 * Legacy field: NPC BAS ID. Maps to renderEmote.
 */
val NPCDefinition.basId: Int get() = renderEmote

/**
 * Gets the NPC's name with var-based transformation support.
 * Legacy overload that accepts a VarManager/vars parameter.
 */
fun NPCDefinition.getName(vars: Any?): String = name

/**
 * Gets the NPC's attack delay from params (param 14).
 */
fun NPCDefinition.getAttackDelay(): Int {
    return params?.get(14) as? Int ?: 4
}

val NPCDefinition.crawls: Boolean get() = moveSpeed == MoveType.CRAWL.id

// getSize(), getKeyForValue(), getValues() are now member functions on EnumDefinition

// getIntValue(key, default) is now a member function on StructDefinition

// --- Item utilities for Java interop ---

/**
 * A copy of this item with its own metadata map, and [Item.slot] left at its default.
 *
 * This is legacy `new Item(Item)` (`com.rs.lib.game.Item:42-44`), which built a fresh item through
 * the clamping constructor and so never carried the source's transient slot over.
 * Callable from Java as KtExtensionsKt.copyItem(item).
 */
fun Item.copyItem(): Item = clone()