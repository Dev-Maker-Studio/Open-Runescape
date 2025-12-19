package com.jagex.game.runetek5.client.window

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.rendering.MessageBox
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.Utils

/**
 * Display mode details and profiling utilities.
 */
class DisplayModeDetails {

    @JvmField var width: Int = 0
    @JvmField var bitDepth: Int = 0
    @JvmField var height: Int = 0
    @JvmField var refreshRate: Int = 0

    companion object {
        @JvmStatic
        fun forWindowMode(windowMode: WindowMode): Array<DisplayModeDetails> {
            val displayMode = windowMode.getDisplayModes()
            val displayModeDetails = Array(displayMode.size shr 2) { DisplayModeDetails() }
            for (index in displayModeDetails.indices) {
                val instance = displayModeDetails[index]
                instance.width = displayMode[index shl 2]
                instance.height = displayMode[(index shl 2) + 1]
                instance.bitDepth = displayMode[(index shl 2) + 2]
                instance.refreshRate = displayMode[(index shl 2) + 3]
            }
            return displayModeDetails
        }

        @JvmStatic
        fun getProfilingPerformance(toolkitValue: Int, profilingTime: Int): Int {
            if (client.ENTITY_DEFAULTS.profilingMesh == -1) {
                return 1
            }
            if (toolkitValue != PreferenceManager.preferences!!.activeToolkit!!.getValue()) {
                client.loadAssets(toolkitValue, LocalizedText.PROFILING.translate(client.CURRENT_LANGUAGE), true)
                if (PreferenceManager.preferences!!.activeToolkit!!.getValue() != toolkitValue) {
                    return -1
                }
            }
            try {
                val dimension = GameShell.gameCanvas!!.size
                MessageBox.draw(
                    LocalizedText.PROFILING.translate(client.CURRENT_LANGUAGE),
                    true,
                    Renderers.activeToolkit!!,
                    FontManager.debugOverlayFont,
                    FontManager.messageboxFont
                )
                val profilingMesh = Mesh.fromResource(Resource.MODEL, client.ENTITY_DEFAULTS.profilingMesh)
                val startTime = Utils.time()
                Renderers.activeToolkit!!.resetClipping()
                client.SUN_MATRIX.setViewPosition(0.0f, 256.0f, 0.0f)
                Renderers.activeToolkit!!.setSunPosition(client.SUN_MATRIX)
                val renderMatrix = Renderers.activeToolkit!!.method8449()
                renderMatrix.method6531(
                    (dimension.width / 2f), (dimension.height / 2f), 512.0f, 512.0f,
                    Resource.mapRegion!!.getViewRangeStart().toFloat(), Resource.mapRegion!!.getViewRangeEnd().toFloat(),
                    dimension.width.toFloat(), dimension.height.toFloat()
                )
                Renderers.activeToolkit!!.setModelMatrix(renderMatrix)
                Renderers.activeToolkit!!.updateSun(1.0f)
                Renderers.activeToolkit!!.applyLighting(16777215, 0.5f, 0.5f, 20.0f, -50.0f, 30.0f)
                val profilingModel = Renderers.activeToolkit!!.createModel(profilingMesh, 2048, 64, 64, 768)
                var modelsDrawn = 0
                Draw@ for (loops in 0 until 500) {
                    Renderers.activeToolkit!!.applyClearFlags(3, 0)
                    for (i_12 in 15 downTo 0) {
                        for (i_13 in 0..i_12) {
                            client.INTERFACE_MODEL_MATRIX.setViewPosition(
                                512.0f * (i_13 - i_12 / 2.0f),
                                0.0f,
                                ((i_12 + 1) * 512).toFloat()
                            )
                            profilingModel!!.drawMatrixThreaded(
                                client.INTERFACE_MODEL_MATRIX,
                                null,
                                ModelFunctionMaskGL.EMPTY
                            )
                            modelsDrawn++
                            if (Utils.time() - startTime >= profilingTime) {
                                break@Draw
                            }
                        }
                    }
                }
                Renderers.activeToolkit!!.awaitRenderCompletion()
                val performanceIndicator = (modelsDrawn * 1000L) / (Utils.time() - startTime)
                Renderers.activeToolkit!!.applyClearFlags(3, 0)
                return performanceIndicator.toInt()
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                return -1
            }
        }
    }
}
