package com.jagex.game.runetek5.config.inputsubscriber

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.DefaultsFile
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class CutsceneDefaultsType(defaultsResource: Js5ResourceProvider) {
    @JvmField var exitCutscene: InputSubscriber? = null

    init {
        val data = defaultsResource.getFile(DefaultsFile.CUTSCENE.fileId)
        decode(Packet(data!!))
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            if (opcode == 1) {
                exitCutscene = InputSubscriber.decode(buffer)
            }
        }
    }
}
