package com.rs.game.content.minigames.pest

import com.rs.game.World
import com.rs.game.model.entity.Entity
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

enum class FortificationPart(val intact: IntArray, val intactShape: ObjectShape, val gate: Boolean) {
    GATE(intArrayOf(Loc.pest_wall_gate_fixed, Loc.pest_wall_gate_damaged_1, Loc.pest_wall_gate_damaged_2), ObjectShape.WALL_STRAIGHT, true),
    GATE_MIRROR(intArrayOf(Loc.pest_wall_gate_fixed_mirror, Loc.pest_wall_gate_damaged_1_mirror, Loc.pest_wall_gate_damaged_2_mirror), ObjectShape.WALL_STRAIGHT, true),
    BARRICADE(intArrayOf(Loc.pest_barricade_fixed, Loc.pest_barricade_damaged), ObjectShape.WALL_STRAIGHT, false),
    BARRICADE_BOTTOM(intArrayOf(Loc.pest_barricade_fixed_bottom, Loc.pest_barricade_damaged_bottom), ObjectShape.WALL_INTERACT, false),
    BARRICADE_TOP(intArrayOf(Loc.pest_barricade_fixed_top, Loc.pest_barricade_damaged_top), ObjectShape.WALL_INTERACT, false);

    val destroyedLevel get() = intact.size

    fun destroyed(rotation: Int): Pair<Int, Int> = when (this) {
        GATE -> Loc.pest_wall_gate_destroyed to (rotation + 2 and 3)
        GATE_MIRROR -> Loc.pest_wall_gate_destroyed_mirror to (rotation + 2 and 3)
        BARRICADE -> Loc.pest_barricade_destroyed to (rotation + 2 and 3)
        BARRICADE_BOTTOM -> Loc.pest_barricade_destroyed_top to (rotation + 3 and 3)
        BARRICADE_TOP -> Loc.pest_barricade_destroyed_bottom to (rotation + 1 and 3)
    }

    companion object {
        val gateLocs = entries.filter { it.gate }.flatMap { it.intact.toList() }.toSet() + setOf(
            Loc.pest_wall_gate_destroyed, Loc.pest_wall_gate_destroyed_mirror, Loc.pest_wall_gate_fixed_open, Loc.pest_wall_gate_fixed_mirror_open,
            Loc.pest_wall_gate_damaged_1_open, Loc.pest_wall_gate_damaged_1_mirror_open, Loc.pest_wall_gate_damaged_2_open, Loc.pest_wall_gate_damaged_2_mirror_open,
        )

        val locs = entries.flatMap { it.intact.toList() }.toSet() + gateLocs +
            setOf(Loc.pest_barricade_destroyed, Loc.pest_barricade_destroyed_bottom, Loc.pest_barricade_destroyed_top)

        private val intactLevels = entries.flatMap { part -> part.intact.mapIndexed { level, loc -> loc to (part to level) } }.toMap()
        private val destroyedParts = mapOf(
            Loc.pest_wall_gate_destroyed to (GATE to 2),
            Loc.pest_wall_gate_destroyed_mirror to (GATE_MIRROR to 2),
            Loc.pest_barricade_destroyed to (BARRICADE to 2),
            Loc.pest_barricade_destroyed_top to (BARRICADE_BOTTOM to 1),
            Loc.pest_barricade_destroyed_bottom to (BARRICADE_TOP to 3),
        )

        fun template(obj: GameObject): Pair<FortificationPiece, Int>? {
            intactLevels[obj.id]?.let { (part, level) -> return FortificationPiece(part, obj.tile, obj.rotation) to level }
            val (part, unrotate) = destroyedParts[obj.id] ?: return null
            return FortificationPiece(part, obj.tile, obj.rotation + unrotate and 3) to part.destroyedLevel
        }
    }
}

class FortificationPiece(val part: FortificationPart, val tile: Tile, val rotation: Int) {
    fun objectAt(level: Int): GameObject =
        if (level >= part.destroyedLevel) part.destroyed(rotation).let { (id, rot) -> GameObject(id, ObjectShape.GROUND_DECORATION, rot, tile) }
        else GameObject(part.intact[level], part.intactShape, rotation, tile)

    fun clear() {
        for (shape in listOf(part.intactShape, ObjectShape.GROUND_DECORATION)) {
            while (true) {
                val obj = World.getObject(tile, shape)?.takeIf { it.id in FortificationPart.locs } ?: break
                World.removeObject(obj)
            }
        }
    }
}

class Fortification(val pieces: List<FortificationPiece>, level: Int) {
    var level = level
        private set

    private val destroyedLevel = pieces.first().part.destroyedLevel
    val gate = pieces.first().part.gate
    val destroyed get() = level >= destroyedLevel
    val damaged get() = level > 0
    val closed get() = pieces.all { World.getObjectWithId(it.tile, it.objectAt(level).id) != null }
    val standing get() = !destroyed && closed

    fun contains(tile: Tile) = pieces.any { it.tile.matches(tile) }

    fun closestPiece(entity: Entity): Tile = pieces.minBy { entity.tile.distanceTo(it.tile) }.tile

    fun damage(): Boolean {
        if (!standing) return false
        setLevel(level + 1)
        return true
    }

    fun repair(): Boolean {
        if (!damaged || !closed) return false
        setLevel(level - 1)
        return true
    }

    fun removeRevealedTemplate() {
        if (!damaged || destroyed) return
        for (piece in pieces) World.getObjectWithId(piece.tile, piece.objectAt(0).id)?.let(World::removeObject)
    }

    fun rebuild() {
        if (damaged && closed) setLevel(0)
    }

    private fun setLevel(newLevel: Int) {
        for (piece in pieces) {
            piece.clear()
            World.spawnObject(piece.objectAt(newLevel))
        }
        level = newLevel
    }
}

class Fortifications(private val all: List<Fortification>) {

    fun at(tile: Tile) = all.firstOrNull { it.contains(tile) }

    fun nearestStanding(entity: Entity, range: Int) = all.filter { it.standing }
        .map { it to entity.tile.distanceTo(it.closestPiece(entity)) }
        .filter { it.second <= range }
        .minByOrNull { it.second }?.first

    fun damageNear(tile: Tile, radius: Int) = all.filter { fortification -> fortification.standing && fortification.pieces.any { it.tile.withinDistance(tile, radius) } }
        .forEach { it.damage() }

    fun rebuildAll() = all.forEach { it.rebuild() }

    companion object {
        fun scan(objects: List<GameObject>): Fortifications {
            val placed = objects.mapNotNull { FortificationPart.template(it) }
            val groups = ArrayList<MutableList<Pair<FortificationPiece, Int>>>()
            for (entry in placed) {
                val touching = groups.filter { group -> group.any { it.first.part.gate == entry.first.part.gate && it.first.tile.withinDistance(entry.first.tile, 1) } }
                val merged = touching.flatten().toMutableList().apply { add(entry) }
                groups.removeAll(touching.toSet())
                groups += merged
            }
            return Fortifications(groups.map { group -> Fortification(group.map { it.first }, group.maxOf { it.second }) })
        }
    }
}
