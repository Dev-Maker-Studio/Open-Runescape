package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.config.sequence.frameset.FrameSet
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask

/**
 * Sequence mask for calculating animation function masks.
 */
class SequenceMask {

    @JvmField var currentFrame: Int = 0
    @JvmField var currentFrameset: FrameSet? = null
    @JvmField var functionMask: Int = 0
    @JvmField var nextFrame: Int = 0
    @JvmField var nextFrameset: FrameSet? = null
    @JvmField var functionmaskCalculated: Boolean = false

    /**
     * Calculates the function mask for the current sequence state.
     *
     * @param seqtypelist The sequence type list
     * @param seqtype The sequence type
     * @param currentFrameId The current frame ID
     * @param nextFrameId The next frame ID for tweening
     * @param frames The frame array
     * @return True if calculation succeeded
     */
    fun isCalculated(
        seqtypelist: SeqTypeList,
        seqtype: SeqType,
        currentFrameId: Int,
        nextFrameId: Int,
        frames: IntArray
    ): Boolean {
        if (functionmaskCalculated) {
            return true
        }

        if (currentFrameId >= frames.size) {
            return false
        }
        currentFrame = frames[currentFrameId]
        currentFrameset = seqtypelist.listFrameset(currentFrame shr 16)
        currentFrame = currentFrame and 0xffff

        if (currentFrameset == null) {
            return false
        }

        if (seqtype.tweened && nextFrameId != -1 && nextFrameId < frames.size) {
            nextFrame = frames[nextFrameId]
            nextFrameset = seqtypelist.listFrameset(nextFrame shr 16)
            nextFrame = nextFrame and 0xffff
        }

        if (seqtype.lights) {
            functionMask = functionMask or ModelFunctionMask.ALTERS_LIGHT
        }
        if (currentFrameset!!.modifiesColors(currentFrame)) {
            functionMask = functionMask or ModelFunctionMask.ALTERS_COLOR
        }
        if (currentFrameset!!.modifiesAlpha(currentFrame)) {
            functionMask = functionMask or ModelFunctionMask.ALTERS_ALPHA
        }
        if (currentFrameset!!.modifiesBillboards(currentFrame)) {
            functionMask = functionMask or ModelFunctionMask.ALTERS_BILLBOARDS
        }
        if (nextFrameset != null) {
            if (nextFrameset!!.modifiesColors(nextFrame)) {
                functionMask = functionMask or ModelFunctionMask.ALTERS_COLOR
            }
            if (nextFrameset!!.modifiesAlpha(nextFrame)) {
                functionMask = functionMask or ModelFunctionMask.ALTERS_ALPHA
            }
            if (nextFrameset!!.modifiesBillboards(nextFrame)) {
                functionMask = functionMask or ModelFunctionMask.ALTERS_BILLBOARDS
            }
        }
        functionMask = functionMask or ModelFunctionMask.ALTERS_VERTICES
        functionmaskCalculated = true
        return true
    }

    /**
     * Resets the sequence mask to initial state.
     */
    fun reset() {
        functionmaskCalculated = false
        functionMask = 0
        nextFrameset = null
        currentFrameset = null
    }
}
