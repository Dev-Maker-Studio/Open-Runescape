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
        Client.cameraPositionPathIndex = 0
        Client.cameraPositionKeyframe = this.anInt9301
        Client.cameraSplineProgress = 0
        Client.cameraSplineSpeedStart = this.anInt9303
        Client.cameraSplineSpeedEnd = this.anInt9304
        Client.cameraLookAtPathIndex = 1
        Client.cameraLookAtKeyframe = this.anInt9302
        Camera.cameraCalculationType = 3
        Camera.calculateCutsceneCameraPosition()
        Client.cutsceneCameraActive = true
    }
}
