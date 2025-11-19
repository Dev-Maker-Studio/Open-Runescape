package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.cutscene.CutsceneAction

/**
 *
 * @author Brainpower - 06/04/2022
 *
 */
class CutsceneUncenteredCameraMovement(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val localX: Int = buffer.readUShort()
    @JvmField val moveZ: Int = buffer.readUShort()
    @JvmField val localY: Int = buffer.readUShort()
    @JvmField val angleX: Int = buffer.readUShort()
    @JvmField val angleY: Int = buffer.readUShort()

    override fun perform() {
        Camera.moveCamera(this.localX, this.localY, this.moveZ, 100, 100, false)
        Camera.setAngle(this.angleX, this.angleY, 0)
        client.aBool7321 = true
    }
}
