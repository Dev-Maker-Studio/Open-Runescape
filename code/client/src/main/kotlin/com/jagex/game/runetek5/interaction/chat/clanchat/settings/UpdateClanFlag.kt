package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

class UpdateClanFlag(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField var index: Int = -1
    @JvmField var flag: Int = 0
    @JvmField var shift: Int = 0
    @JvmField var bitmaskSize: Int = 0

    override fun decode(buffer: Packet) {
        this.index = buffer.readUShort()
        this.flag = buffer.readInt()
        this.shift = buffer.readUByte()
        this.bitmaskSize = buffer.readUByte()
    }

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.updateClanFlag(this.index, this.flag, this.shift, this.bitmaskSize)
    }
}
