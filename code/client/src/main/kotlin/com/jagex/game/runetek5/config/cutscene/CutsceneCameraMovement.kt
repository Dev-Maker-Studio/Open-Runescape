package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client

class CutsceneCameraMovement(packet: Packet) {

    @JvmField val xPositions: IntArray

    @JvmField val yPositions: IntArray

    @JvmField val zPositions: IntArray

    @JvmField val timestamps: IntArray

    @JvmField val targetXPositions: IntArray

    @JvmField val targetYPositions: IntArray

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

    fun loadIntoClientCameraPath(pathIndex: Int) {
        val keyframeData = Array(xPositions.size shl 1) { IntArray(4) }

        for (i in xPositions.indices) {
            keyframeData[i * 2][0] = xPositions[i]
            keyframeData[i * 2][1] = yPositions[i]
            keyframeData[i * 2][2] = zPositions[i]
            keyframeData[i * 2][3] = timestamps[i]

            keyframeData[i * 2 + 1][0] = targetXPositions[i]
            keyframeData[i * 2 + 1][1] = targetYPositions[i]
            keyframeData[i * 2 + 1][2] = targetZPositions[i]
            keyframeData[i * 2 + 1][3] = timestamps[i]
        }

        @Suppress("UNCHECKED_CAST")
        Client.cameraSplinePaths!![pathIndex] = keyframeData as Array<IntArray?>
    }
}
