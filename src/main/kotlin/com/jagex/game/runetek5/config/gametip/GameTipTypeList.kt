package com.jagex.game.runetek5.config.gametip

import java.util.Random

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.staticmethods.StaticMethods

/**
 *
 * @author Brainpower - 28/03/2022
 *
 */
class GameTipTypeList(resourceProvider: Js5ResourceProvider) {

    @JvmField val tipsResource: Js5ResourceProvider
    @JvmField val tipStages: Array<GameTipStage>
    @JvmField val tipsFileId: Int

    init {
        tipsResource = resourceProvider
        val buffer = Packet(tipsResource.getFile(0, 0)!!)
        val size = if (buffer.buffer.size >= 1) buffer.readUByte() else -1
        if (size < 4) {
            tipStages = emptyArray()
            tipsFileId = -1
        } else {
            val length = buffer.readUByte()
            val tipRenderDetails = TipRenderDetails.values()
            var hasStages = true
            if (tipRenderDetails.size == length) {
                for (tipRenderDetail in tipRenderDetails) {
                    val transformType = buffer.readUByte()
                    if (transformType != tipRenderDetail.transformType) {
                        hasStages = false
                        break
                    }
                }
            } else {
                hasStages = false
            }

            if (hasStages) {
                val definedStageCount = buffer.readUByte()
                val totalStageCount = buffer.readUByte()
                tipsFileId = buffer.readShort()
                val displayDurationMs = buffer.readUMedium()
                val timeBetweenUpdatesMs = buffer.readUShort()

                tipStages = Array(totalStageCount + 1) { decodeDefaultStage(displayDurationMs, timeBetweenUpdatesMs) }

                for (stageIndex in 0 until definedStageCount) {
                    val stage = buffer.readUByte()
                    val hasUpdates = buffer.readUByte() == 1
                    val updateDetailCount = buffer.readUShort()
                    val updateDetails: Array<GameTipUpdateDetails>
                    if (tipsFileId != -1) {
                        updateDetails = Array(updateDetailCount + 1) { GameTipUpdateDetails(0, 0, 0) }
                        updateDetails[0] = GameTipUpdateDetails(tipsFileId, displayDurationMs, timeBetweenUpdatesMs)

                        for (index in 0 until updateDetailCount) {
                            val file = buffer.readUShort()
                            val duration = buffer.readUMedium()
                            val delay = buffer.readUShort()
                            updateDetails[index + 1] = GameTipUpdateDetails(file, duration, delay)
                        }
                    } else {
                        updateDetails = Array(updateDetailCount) { GameTipUpdateDetails(0, 0, 0) }

                        for (index in 0 until updateDetailCount) {
                            val file = buffer.readUShort()
                            val duration = buffer.readUMedium()
                            val delay = buffer.readUShort()
                            updateDetails[index] = GameTipUpdateDetails(file, duration, delay)
                        }

                        tipStages[stage] = GameTipStage(hasUpdates, updateDetails)
                    }

                    tipStages[stage] = GameTipStage(hasUpdates, updateDetails)
                }

                for (index in 0 until totalStageCount + 1) {
                    if (tipStages[index] == null) {
                        tipStages[index] = decodeDefaultStage(displayDurationMs, timeBetweenUpdatesMs)
                    }
                }
            } else {
                tipStages = emptyArray()
                tipsFileId = -1
            }
        }
    }

    fun decodeDefaultStage(displayDurationMs: Int, timeBetweenUpdatesMs: Int): GameTipStage {
        return if (tipsFileId == -1) {
            GameTipStage(false, emptyArray())
        } else {
            val updateDetails = GameTipUpdateDetails(tipsFileId, displayDurationMs, timeBetweenUpdatesMs)
            GameTipStage(false, arrayOf(updateDetails))
        }
    }

    fun list(fileId: Int): GameTipType {
        val data = tipsResource.getFile(fileId, 0)
        val tipsType = GameTipType()
        tipsType.decode(Packet(data!!))
        return tipsType
    }

    fun getUpdateDetails(startStagePreference: Int): Array<GameTipUpdateDetails> {
        if (startStagePreference >= 0 && startStagePreference < tipStages.size) {
            val stage = tipStages[startStagePreference]
            if (stage.hasUpdates && stage.gameTipUpdateDetails.size > 1) {
                val start = if (tipsFileId == -1) 0 else 1
                val random = Random()
                val updateDetailsCache = Array(stage.gameTipUpdateDetails.size) { GameTipUpdateDetails(0, 0, 0) }
                System.arraycopy(stage.gameTipUpdateDetails, 0, updateDetailsCache, 0, updateDetailsCache.size)
                for (src in start until updateDetailsCache.size) {
                    val dst = StaticMethods.boundedRandom(random, updateDetailsCache.size - start) + start
                    val updateDetails = stage.gameTipUpdateDetails[src]
                    updateDetailsCache[src] = updateDetailsCache[dst]
                    updateDetailsCache[dst] = updateDetails
                }
                return updateDetailsCache
            }
            return stage.gameTipUpdateDetails
        }
        return decodeDefaultStage(0, 0).gameTipUpdateDetails
    }

    fun hasTipsFile(): Boolean {
        return tipsFileId != -1
    }
}
