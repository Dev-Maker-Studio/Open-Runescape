package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.scene.SceneGraph

/**
 *
 * @author Steven - 24/09/2023
 *
 */
class CutsceneAnimateObject(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val objectIndex: Int = buffer.readUShort()
    @JvmField val sequenceId: Int = buffer.readBigSmart()

    override fun perform() {
        val `object` = CutsceneTypeList.CUTSCENE_OBJECTS!![this.objectIndex]
        SceneGraph.animateObject(`object`.plane, `object`.x, `object`.y, `object`.locShape.group, `object`.locShape.shape, `object`.rotation, this.sequenceId)
    }

    override fun isFullyCached(): Boolean {
        val seqType = TypeLists.SEQ!!.list(this.sequenceId)
        return seqType.ready()
    }
}
