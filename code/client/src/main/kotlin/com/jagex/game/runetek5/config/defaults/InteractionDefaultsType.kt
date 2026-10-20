package com.jagex.game.runetek5.config.defaults

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.DefaultsFile
import com.jagex.game.runetek5.config.inputsubscriber.InputSubscriber
import com.jagex.game.runetek5.config.inputsubscriber.KeyHoldInputSubscriber
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class InteractionDefaultsType(resource: Js5ResourceProvider) {

    @JvmField var primaryClickSubscriber: InputSubscriber? = null
    @JvmField var secondaryClickSubscriber: InputSubscriber? = null
    @JvmField var contextMenuSubscriber: InputSubscriber? = null
    @JvmField var openMinimenuInputSubscriber: InputSubscriber? = null
    @JvmField var minimenuKey: KeyHoldInputSubscriber? = null
    @JvmField var toggleRunKey: KeyHoldInputSubscriber? = null
    @JvmField var teleportKey: KeyHoldInputSubscriber? = null
    @JvmField var minimenuCursor: Boolean = false
    @JvmField var tooltipColorP2p: Int = 0
    @JvmField var tooltipColorF2p: Int = 0

    init {
        val data = resource.getFile(DefaultsFile.INTERACTIONS.fileId)
        decode(Packet(data!!))
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            when (opcode) {
                1 -> primaryClickSubscriber = InputSubscriber.decode(buffer)
                2 -> secondaryClickSubscriber = InputSubscriber.decode(buffer)
                3 -> contextMenuSubscriber = InputSubscriber.decode(buffer)
                4 -> openMinimenuInputSubscriber = InputSubscriber.decode(buffer)
                5 -> minimenuKey = KeyHoldInputSubscriber.decode(buffer)
                6 -> toggleRunKey = KeyHoldInputSubscriber.decode(buffer)
                7 -> teleportKey = KeyHoldInputSubscriber.decode(buffer)
                8 -> InputSubscriber.decode(buffer)
                9 -> InputSubscriber.decode(buffer)
                10 -> InputSubscriber.decode(buffer)
                11 -> minimenuCursor = true
                12 -> tooltipColorP2p = buffer.readInt()
                13 -> tooltipColorF2p = buffer.readInt()
            }
        }
    }
}
