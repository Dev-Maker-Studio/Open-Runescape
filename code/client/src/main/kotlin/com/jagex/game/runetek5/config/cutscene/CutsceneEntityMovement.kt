package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity

class CutsceneEntityMovement(buffer: Packet) {

    @JvmField val movementTypes: IntArray
    @JvmField val bitpackedPositions: IntArray

    init {
        val stepCount = buffer.readUSmart()
        this.movementTypes = IntArray(stepCount)
        this.bitpackedPositions = IntArray(stepCount)
        for (index in 0 until stepCount) {
            val movementType = buffer.readUByte()
            this.movementTypes[index] = movementType
            val x = buffer.readUShort()
            val y = buffer.readUShort()
            this.bitpackedPositions[index] = y + (x shl 16)
        }
    }

    fun move(entity: CutsceneEntity, plane: Int) {
        val bitpackedStartposition = this.bitpackedPositions[0]
        entity.move(plane, bitpackedStartposition ushr 16, bitpackedStartposition and 0xffff)
        val pathingEntity = entity.getEntity()
        pathingEntity.movementStepsLeft = 0
        for (index in this.movementTypes.size - 1 downTo 0) {
            val type = this.movementTypes[index]
            val position = this.bitpackedPositions[index]
            pathingEntity.movementBaseX[pathingEntity.movementStepsLeft] = position shr 16
            pathingEntity.movementBaseY[pathingEntity.movementStepsLeft] = position and 0xffff
            var moveType = MovementType.WALKING.id
            when (type) {
                0 -> moveType = MovementType.HALF_WALK.id
                2 -> moveType = MovementType.RUNNING.id
            }
            pathingEntity.movementTypes[pathingEntity.movementStepsLeft] = moveType
            pathingEntity.movementStepsLeft++
        }
    }
}
