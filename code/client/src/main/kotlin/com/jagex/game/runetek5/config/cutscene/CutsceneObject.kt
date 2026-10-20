package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.scene.LocAction

class CutsceneObject(buffer: Packet) {

    @JvmField val locId: Int
    @JvmField val locShape: ObjectShapes
    @JvmField var plane: Int = 0
    @JvmField var x: Int = 0
    @JvmField var y: Int = 0
    @JvmField var rotation: Int = 0

    init {
        locId = buffer.readBigSmart()
        locShape = Identifiable.identify(ObjectShapes.values(), buffer.readUByte()) as ObjectShapes
    }

    fun isFullyCached(): Boolean {
        if (locId < 0) {
            return true
        }
        val locType: ObjectType = TypeLists.LOC!!.list(locId)
        var cached = locType.isTypeCached(locShape.shape)
        val seqIds = locType.getAnimations()
        if (seqIds != null) {
            for (seqId in seqIds) {
                cached = cached and TypeLists.SEQ!!.list(seqId).ready()
            }
        }
        return cached
    }

    fun destroy() {
        LocAction.destroyObject(plane, x, y, locShape.group, -1, locShape.shape, rotation)
    }

    fun replace(plane: Int, x: Int, y: Int, rotation: Int) {
        LocAction.destroyObject(plane, x, y, locShape.group, locId, locShape.shape, rotation)
        this.plane = plane
        this.x = x
        this.y = y
        this.rotation = rotation
    }
}
