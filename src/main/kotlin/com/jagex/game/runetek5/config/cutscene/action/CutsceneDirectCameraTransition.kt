package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneDirectCameraTransition(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val positionMovementIndex: Int

    @JvmField val lookAtMovementIndex: Int

    @JvmField val positionKeyframe: Int

    @JvmField val lookAtKeyframe: Int

    @JvmField val splineSpeedStart: Int

    @JvmField val splineSpeedEnd: Int

    init {
        this.positionMovementIndex = buffer.readUShort()
        this.lookAtMovementIndex = buffer.readUShort()
        this.positionKeyframe = buffer.readUShort()
        this.lookAtKeyframe = buffer.readUShort()
        this.splineSpeedStart = buffer.readUShort()
        this.splineSpeedEnd = buffer.readUShort()
    }

    override fun perform() {
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS!![this.positionMovementIndex].loadIntoClientCameraPath(0)
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS!![this.lookAtMovementIndex].loadIntoClientCameraPath(1)
        Client.cameraPositionPathIndex = 0
        Client.cameraPositionKeyframe = this.positionKeyframe
        Client.cameraSplineProgress = 0
        Client.cameraSplineSpeedStart = this.splineSpeedStart
        Client.cameraSplineSpeedEnd = this.splineSpeedEnd
        Client.cameraLookAtPathIndex = 1
        Client.cameraLookAtKeyframe = this.lookAtKeyframe
        Camera.cameraCalculationType = 3
        Camera.calculateCutsceneCameraPosition()
        Client.cutsceneCameraActive = true
    }
}
