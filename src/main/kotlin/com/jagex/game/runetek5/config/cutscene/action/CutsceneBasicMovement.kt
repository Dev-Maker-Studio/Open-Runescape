package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneEntity
import com.jagex.game.runetek5.config.cutscene.CutsceneEntityMovement
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneBasicMovement(buffer: Packet) : CutsceneAction(buffer) {
    @JvmField val entityIndex: Int = buffer.readUShort()
    @JvmField val movementIndex: Int = buffer.readUShort()
    @JvmField val plane: Int = buffer.readUByte()

    override fun perform() {
        val entity: CutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES[this.entityIndex]
        val movement: CutsceneEntityMovement = CutsceneTypeList.CUTSCENE_ENTITY_MOVEMENTS[this.movementIndex]
        movement.move(entity, this.plane)
    }

    companion object {
        @JvmField var anInt9440: Int = 0
    }
}
