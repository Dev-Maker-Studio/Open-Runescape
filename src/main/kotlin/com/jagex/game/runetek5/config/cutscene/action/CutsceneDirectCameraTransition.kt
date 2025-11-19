package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
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
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS[this.anInt9299].method1363(0)
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS[this.anInt9300].method1363(1)
        client.anInt7277 = 0
        client.anInt7279 = this.anInt9301
        client.anInt7256 = 0
        client.anInt7327 = this.anInt9303
        client.anInt7182 = this.anInt9304
        client.anInt7448 = 1
        client.anInt7280 = this.anInt9302
        Camera.cameraCalculationType = 3
        Camera.calculateCutsceneCameraPosition()
        client.aBool7321 = true
    }
}
