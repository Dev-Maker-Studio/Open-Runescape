package com.jagex.game.runetek5.config.item

import com.jagex.game.runetek5.buffer.Packet

/**
 * Item appearance customization for player equipment.
 *
 * @author Steven - 22/11/2023
 */
class ItemTypeCustomisation(objType: ItemType) {

    @JvmField val maleBody: IntArray = IntArray(3)
    @JvmField val femaleBody: IntArray = IntArray(3)
    @JvmField val maleHead: IntArray = IntArray(2)
    @JvmField val femaleHead: IntArray = IntArray(2)
    @JvmField var recolDst: ShortArray? = null
    @JvmField var retexDst: ShortArray? = null

    init {
        maleBody[0] = objType.maleEquip1
        maleBody[1] = objType.maleEquip2
        maleBody[2] = objType.maleEquip3
        femaleBody[0] = objType.femaleEquip1
        femaleBody[1] = objType.femaleEquip2
        femaleBody[2] = objType.femaleEquip3
        maleHead[0] = objType.maleHead1
        maleHead[1] = objType.maleHead2
        femaleHead[0] = objType.femaleHead1
        femaleHead[1] = objType.femaleHead2
        if (objType.recolorDst != null) {
            recolDst = ShortArray(objType.recolorDst!!.size)
            System.arraycopy(objType.recolorDst, 0, recolDst!!, 0, recolDst!!.size)
        }
        if (objType.retexDst != null) {
            retexDst = ShortArray(objType.retexDst!!.size)
            System.arraycopy(objType.retexDst, 0, retexDst!!, 0, retexDst!!.size)
        }
    }

    companion object {
        @JvmStatic
        fun decodeItemEffects(objType: ItemType, stream: Packet): ItemTypeCustomisation {
            val customisation = ItemTypeCustomisation(objType)
            val flags = stream.readUByte()
            val modifiesBody = (flags and 0x1) != 0
            val modifiesHead = (flags and 0x2) != 0
            val modifiesColors = (flags and 0x4) != 0
            val modifiesTextures = (flags and 0x8) != 0
            if (modifiesBody) {
                customisation.maleBody[0] = stream.readBigSmart()
                customisation.femaleBody[0] = stream.readBigSmart()
                if (objType.maleEquip2 != -1 || objType.femaleEquip2 != -1) {
                    customisation.maleBody[1] = stream.readBigSmart()
                    customisation.femaleBody[1] = stream.readBigSmart()
                }
                if (objType.maleEquip3 != -1 || objType.femaleEquip3 != -1) {
                    customisation.maleBody[2] = stream.readBigSmart()
                    customisation.femaleBody[2] = stream.readBigSmart()
                }
            }
            if (modifiesHead) {
                customisation.maleHead[0] = stream.readBigSmart()
                customisation.femaleHead[0] = stream.readBigSmart()
                if (objType.maleHead2 != -1 || objType.femaleHead2 != -1) {
                    customisation.maleHead[1] = stream.readBigSmart()
                    customisation.femaleHead[1] = stream.readBigSmart()
                }
            }
            if (modifiesColors) {
                val bitpackedSlots = stream.readUShort()
                val slots = intArrayOf(
                    bitpackedSlots and 0xf,
                    bitpackedSlots shr 4 and 0xf,
                    bitpackedSlots shr 8 and 0xf,
                    bitpackedSlots shr 12 and 0xf
                )
                for (i in 0 until 4) {
                    if (slots[i] != 15) {
                        customisation.recolDst!![slots[i]] = stream.readUShort().toShort()
                    }
                }
            }
            if (modifiesTextures) {
                val bitpackedSlots = stream.readUByte()
                val slots = intArrayOf(bitpackedSlots and 0xf, bitpackedSlots shr 4 and 0xf)
                for (i in 0 until 2) {
                    if (slots[i] != 15) {
                        customisation.retexDst!![slots[i]] = stream.readUShort().toShort()
                    }
                }
            }
            return customisation
        }
    }
}
