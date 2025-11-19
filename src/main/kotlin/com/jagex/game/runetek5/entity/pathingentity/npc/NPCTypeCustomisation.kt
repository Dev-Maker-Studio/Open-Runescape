package com.jagex.game.runetek5.entity.pathingentity.npc

import com.jagex.game.runetek5.config.npc.NPCType

/**
 * [J]
 * @author Steven - 11/10/2023
 *
 */
class NPCTypeCustomisation {

    @JvmField var uid: Long = 0

    @JvmField val modelIds: IntArray

    @JvmField var recolorDst: ShortArray? = null

    @JvmField var retextureDst: ShortArray? = null

    constructor(uid: Long, models: IntArray, recolorDst: ShortArray?, retextureDst: ShortArray?) {
        this.uid = uid
        this.modelIds = models
        this.recolorDst = recolorDst
        this.retextureDst = retextureDst
    }

    constructor(npcType: NPCType, body: Boolean) {
        if (body) {
            this.modelIds = IntArray(npcType.modelIds.size)
            System.arraycopy(npcType.modelIds, 0, this.modelIds, 0, this.modelIds.size)
        } else {
            this.modelIds = IntArray(npcType.headModels.size)
            System.arraycopy(npcType.headModels, 0, this.modelIds, 0, this.modelIds.size)
        }
        if (npcType.recolorDst != null) {
            this.recolorDst = ShortArray(npcType.recolorDst!!.size)
            System.arraycopy(npcType.recolorDst, 0, this.recolorDst, 0, this.recolorDst!!.size)
        }
        if (npcType.retextureDst != null) {
            this.retextureDst = ShortArray(npcType.retextureDst!!.size)
            System.arraycopy(npcType.retextureDst, 0, this.retextureDst, 0, this.retextureDst!!.size)
        }
    }
}
