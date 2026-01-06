package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneDirectCameraTransition(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val anInt9299: Int

    @JvmField val anInt9300: Int

    @JvmField val anInt9301: Int

    @JvmField val anInt9302: Int

    @JvmField val anInt9303: Int

    @JvmField val anInt9304: Int

    init {
        this.anInt9299 = buffer.readUShort()
        this.anInt9300 = buffer.readUShort()
        this.anInt9301 = buffer.readUShort()
        this.anInt9302 = buffer.readUShort()
        this.anInt9303 = buffer.readUShort()
        this.anInt9304 = buffer.readUShort()
    }

    override fun perform() {
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS!![this.anInt9299].loadIntoClientCameraPath(0)
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS!![this.anInt9300].loadIntoClientCameraPath(1)
        Client.anInt7277 = 0
        Client.anInt7279 = this.anInt9301
        Client.anInt7256 = 0
        Client.anInt7327 = this.anInt9303
        Client.anInt7182 = this.anInt9304
        Client.anInt7448 = 1
        Client.anInt7280 = this.anInt9302
        Camera.cameraCalculationType = 3
        Camera.calculateCutsceneCameraPosition()
        Client.aBool7321 = true
    }
}
