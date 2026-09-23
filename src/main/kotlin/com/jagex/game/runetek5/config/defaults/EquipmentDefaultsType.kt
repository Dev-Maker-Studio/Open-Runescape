package com.jagex.game.runetek5.config.defaults

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.DefaultsFile
import com.jagex.game.runetek5.config.UnknownOpcodes
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class EquipmentDefaultsType(defaultsResource: Js5ResourceProvider) {

    @JvmField var customizableObjSlots: IntArray? = null
    @JvmField var shieldSlot: Int = -1
    @JvmField var weaponSlot: Int = -1
    @JvmField var hiddenAnimationShieldSlots: IntArray? = null
    @JvmField var hiddenAnimationWeaponSlots: IntArray? = null

    init {
        val data = defaultsResource.getFile(DefaultsFile.EQUIPMENT.fileId)
        decode(Packet(data!!))
        if (this.customizableObjSlots == null) {
            throw RuntimeException("")
        }
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            when (opcode) {
                1 -> {
                    val count = buffer.readUByte()
                    this.customizableObjSlots = IntArray(count)
                    for (i in 0 until this.customizableObjSlots!!.size) {
                        this.customizableObjSlots!![i] = buffer.readUByte()
                    }
                }
                3 -> this.shieldSlot = buffer.readUByte()
                4 -> this.weaponSlot = buffer.readUByte()
                5 -> {
                    this.hiddenAnimationShieldSlots = IntArray(buffer.readUByte())
                    for (i in this.hiddenAnimationShieldSlots!!.indices) {
                        this.hiddenAnimationShieldSlots!![i] = buffer.readUByte()
                    }
                }
                6 -> {
                    this.hiddenAnimationWeaponSlots = IntArray(buffer.readUByte())
                    for (i in this.hiddenAnimationWeaponSlots!!.indices) {
                        this.hiddenAnimationWeaponSlots!![i] = buffer.readUByte()
                    }
                }
                else -> UnknownOpcodes.record(opcode)
            }
        }
    }
}
