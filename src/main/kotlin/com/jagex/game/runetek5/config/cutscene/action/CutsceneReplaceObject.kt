package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

/**
 *
 * @author Brainpower - 01/02/2023
 *
 */
class CutsceneReplaceObject(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val locIndex: Int
    @JvmField val x: Int
    @JvmField val y: Int
    @JvmField val plane: Int
    @JvmField val rotation: Int

    init {
        this.locIndex = buffer.readUShort()
        val bitpackedPosition = buffer.readInt()
        this.x = bitpackedPosition ushr 16
        this.y = bitpackedPosition and 0xffff
        this.plane = buffer.readUByte()
        this.rotation = buffer.readUByte()
    }

    override fun perform() {
        CutsceneTypeList.CUTSCENE_OBJECTS!![this.locIndex].replace(this.plane, this.x, this.y, this.rotation)
    }
}
