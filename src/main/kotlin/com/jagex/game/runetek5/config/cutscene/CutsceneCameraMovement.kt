package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client

/**
 * Stores camera movement keyframes for cutscenes.
 * Contains position and rotation data for camera paths.
 */
class CutsceneCameraMovement(packet: Packet) {

    /** X positions for camera keyframes */
    @JvmField val xPositions: IntArray

    /** Y positions (height) for camera keyframes */
    @JvmField val yPositions: IntArray

    /** Z positions for camera keyframes */
    @JvmField val zPositions: IntArray

    /** Timestamps for each keyframe */
    @JvmField val timestamps: IntArray

    /** Target X positions for camera to look at */
    @JvmField val targetXPositions: IntArray

    /** Target Y positions for camera to look at */
    @JvmField val targetYPositions: IntArray

    /** Target Z positions for camera to look at */
    @JvmField val targetZPositions: IntArray

    init {
        val keyframeCount = packet.readUSmart()
        xPositions = IntArray(keyframeCount)
        yPositions = IntArray(keyframeCount)
        zPositions = IntArray(keyframeCount)
        timestamps = IntArray(keyframeCount)
        targetXPositions = IntArray(keyframeCount)
        targetYPositions = IntArray(keyframeCount)
        targetZPositions = IntArray(keyframeCount)

        for (i in 0 until keyframeCount) {
            xPositions[i] = packet.readUShort() - 5120
            zPositions[i] = packet.readUShort() - 5120
            yPositions[i] = packet.readShort()
            targetXPositions[i] = packet.readUShort() - 5120
            targetZPositions[i] = packet.readUShort() - 5120
            targetYPositions[i] = packet.readShort()
            timestamps[i] = packet.readShort()
        }
    }

    /**
     * Loads camera movement data into the client's camera path array
     * @param pathIndex The index of the camera path to load into
     */
    fun loadIntoClientCameraPath(pathIndex: Int) {
        val keyframeData = Array(xPositions.size shl 1) { IntArray(4) }

        for (i in xPositions.indices) {
            // Camera position keyframe
            keyframeData[i * 2][0] = xPositions[i]
            keyframeData[i * 2][1] = yPositions[i]
            keyframeData[i * 2][2] = zPositions[i]
            keyframeData[i * 2][3] = timestamps[i]

            // Camera target/look-at keyframe
            keyframeData[i * 2 + 1][0] = targetXPositions[i]
            keyframeData[i * 2 + 1][1] = targetYPositions[i]
            keyframeData[i * 2 + 1][2] = targetZPositions[i]
            keyframeData[i * 2 + 1][3] = timestamps[i]
        }

        @Suppress("UNCHECKED_CAST")
        Client.cameraSplinePaths!![pathIndex] = keyframeData as Array<IntArray?>
    }
}
