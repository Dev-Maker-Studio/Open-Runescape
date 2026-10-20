package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

class UpdateBitpackedVariable(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField var key: Int = 0
    @JvmField var value: Int = 0
    @JvmField var shift: Int = 0
    @JvmField var bitmaskSize: Int = 0

    override fun decode(buffer: Packet) {
        this.key = buffer.readInt()
        this.value = buffer.readInt()
        this.shift = buffer.readUByte()
        this.bitmaskSize = buffer.readUByte()
    }

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.updateBitpackedVariable(this.key, this.value, this.shift, this.bitmaskSize)
    }
}
