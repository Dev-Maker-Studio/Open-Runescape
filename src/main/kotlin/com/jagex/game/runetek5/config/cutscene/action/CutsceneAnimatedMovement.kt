package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import java.util.Arrays

class CutsceneAnimatedMovement(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val entityIndex: Int

    @JvmField val movementAnimations: IntArray

    @JvmField val movementAnimationId: Int

    @JvmField val seqFlag: Int

    init {
        this.entityIndex = buffer.readUShort()
        this.movementAnimations = IntArray(MovementType.values().size)
        this.movementAnimationId = buffer.readBigSmart()
        Arrays.fill(this.movementAnimations, 0, this.movementAnimations.size, this.movementAnimationId)
        this.seqFlag = buffer.readInt()
    }

    override fun perform() {
        val pathingEntity = CutsceneTypeList.CUTSCENE_ENTITIES[this.entityIndex].getEntity()
        if (this.seqFlag == 0) {
            PlayerUpdate.animate(pathingEntity, this.movementAnimations, 0, false)
        } else {
            PlayerUpdate.animate(pathingEntity, intArrayOf(this.movementAnimationId), IntArray(1), intArrayOf(this.seqFlag))
        }
    }

    override fun isFullyCached(): Boolean {
        val seqType = TypeLists.SEQ!!.list(this.movementAnimationId)
        return seqType.ready()
    }
}
