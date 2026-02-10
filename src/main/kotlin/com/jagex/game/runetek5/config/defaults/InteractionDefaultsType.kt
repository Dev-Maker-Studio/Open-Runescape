package com.jagex.game.runetek5.config.defaults

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.DefaultsFile
import com.jagex.game.runetek5.config.inputsubscriber.InputSubscriber
import com.jagex.game.runetek5.config.inputsubscriber.KeyHoldInputSubscriber
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Interaction defaults configuration, including key bindings and tooltip colors.
 *
 * @author Brainpower - 19/04/2022
 * before: Defaults7Loader
 */
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

    /**
     * Decodes interaction defaults from the packet buffer.
     *
     * @param buffer The packet buffer to decode from
     */
    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            when (opcode) {
                1 -> primaryClickSubscriber = InputSubscriber.decode(buffer) // minimenu
                2 -> secondaryClickSubscriber = InputSubscriber.decode(buffer) // minimenu
                3 -> contextMenuSubscriber = InputSubscriber.decode(buffer) // minimenu
                4 -> openMinimenuInputSubscriber = InputSubscriber.decode(buffer)
                5 -> minimenuKey = KeyHoldInputSubscriber.decode(buffer) // minimenu
                6 -> toggleRunKey = KeyHoldInputSubscriber.decode(buffer)
                7 -> teleportKey = KeyHoldInputSubscriber.decode(buffer)
                8 -> InputSubscriber.decode(buffer) // decoded but not stored
                9 -> InputSubscriber.decode(buffer) // decoded but not stored
                10 -> InputSubscriber.decode(buffer) // decoded but not stored
                11 -> minimenuCursor = true
                12 -> tooltipColorP2p = buffer.readInt()
                13 -> tooltipColorF2p = buffer.readInt()
            }
        }
    }
}
