package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

/**
 *
 * @author Brainpower - 12/01/2022
 *
 */
class CutsceneMoveEntity(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val targetIndex: Int
    @JvmField val x: Int
    @JvmField val y: Int
    @JvmField val plane: Int
    @JvmField val direction: Int

    init {
        this.targetIndex = buffer.readUShort()
        val bitpacked = buffer.readInt()
        this.x = bitpacked ushr 16
        this.y = bitpacked and 0xffff
        this.plane = buffer.readUByte()
        this.direction = buffer.readSmart()
    }

    override fun perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES[this.targetIndex].move(this.x, this.y, this.plane, this.direction)
    }
}
