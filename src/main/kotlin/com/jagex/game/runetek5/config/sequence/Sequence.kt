package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.graphics.model.Model

open class Sequence protected constructor(tween: Boolean) {

    @JvmField
    var type: SeqType? = null

    @JvmField
    var resetAnimation: Boolean = false

    @JvmField
    var tweening: Boolean = false

    @JvmField
    var speed: Int = 0

    @JvmField
    var currentLoopCount: Int = 0

    @JvmField
    var currentFrameId: Int = 0

    @JvmField
    var nextFrameId: Int = 0

    @JvmField
    var currentFrameDuration: Int = 0

    @JvmField
    var replayMode: Int = 0

    @JvmField
    var hardReset: Boolean = false

    @JvmField
    val currentMask: SequenceMask

    @JvmField
    val nextMask: SequenceMask?

    init {
        this.tweening = tween // could be incorrect
        this.currentMask = SequenceMask()
        if (this.tweening) {
            this.nextMask = SequenceMask()
        } else {
            this.nextMask = null
        }
    }

    fun isMaskCalculated(): Boolean {
        if (this.type != null) {
            val maskCalculated = this.currentMask.isCalculated(SeqTypeList.seqTypeList!!, this.type!!, this.currentFrameId, this.nextFrameId, this.type!!.frames!!)
            if (maskCalculated && this.tweening && this.type!!.interfaceFrames != null) {
                this.nextMask!!.isCalculated(SeqTypeList.seqTypeList!!, this.type!!, this.currentFrameId, this.nextFrameId, this.type!!.interfaceFrames!!)
            }
            return maskCalculated
        }
        return false
    }

    fun copy(from: Sequence) {
        this.type = from.type
        this.resetAnimation = from.resetAnimation
        this.tweening = from.tweening
        this.speed = from.speed
        this.currentLoopCount = from.currentLoopCount
        this.currentFrameId = from.currentFrameId
        this.nextFrameId = from.nextFrameId
        this.currentFrameDuration = from.currentFrameDuration
        resetMasks()
    }

    fun hasType(): Boolean {
        return this.type != null
    }

    fun getType(): SeqType? {
        return this.type
    }

    fun update(sequenceId: Int) {
        animateSequence(sequenceId, 0, 0, false)
    }

    fun resetSequence() {
        animateSequence(-1, 0, 0, false)
    }

    fun animate(sequenceId: Int) {
        animate(sequenceId, 0, 0, false, true)
    }

    fun animateSequence(sequenceId: Int, speed: Int, replayMode: Int, replay: Boolean) {
        animate(sequenceId, speed, replayMode, replay, false)
    }

    fun animate(sequenceId: Int, speed: Int, replayMode: Int, replay: Boolean, hardReset: Boolean) {
        if (sequenceId != getId()) {
            if (sequenceId == -1) {
                this.type = null
            } else {
                if (this.type != null && this.type!!.id == sequenceId) {
                    if (this.type!!.loopType == 0) {
                        return
                    }
                } else {
                    this.type = SeqTypeList.seqTypeList!!.list(sequenceId)
                }
                this.currentLoopCount = 0
                this.speed = speed
                this.replayMode = replayMode
                this.hardReset = hardReset
                if (replay) {
                    this.currentFrameId = (Math.random() * this.type!!.frames!!.size).toInt()
                    this.currentFrameDuration = (Math.random() * this.type!!.durations!![this.currentFrameId]).toInt()
                } else {
                    this.currentFrameId = 0
                    this.currentFrameDuration = 0
                }
                this.nextFrameId = this.currentFrameId + 1
                if (this.nextFrameId >= this.type!!.frames!!.size) {
                    this.nextFrameId = -1
                }
                if (this.speed == 0) {
                    playSound(this.type!!, this.currentFrameId)
                }
                this.resetAnimation = false
            }
            resetMasks()
        }
    }

    fun hasSpeed(): Boolean {
        return this.speed != 0
    }

    fun getSpeed(): Int {
        return this.speed
    }

    fun setSpeed(speed: Int) {
        this.speed = speed
    }

    fun animateRotatedModel(model: Model, rotation: Int) {
        if (this.type != null && this.type!!.frames != null && isMaskCalculated()) {
            model.animateModelFrame(this.currentMask.currentFrameset, this.currentMask.currentFrame, this.currentMask.nextFrameset, this.currentMask.nextFrame, this.currentFrameDuration, this.type!!.durations!![this.currentFrameId], rotation, this.type!!.lights)
            if (this.tweening && this.type!!.interfaceFrames != null && this.nextMask!!.functionmaskCalculated) {
                model.animateModelFrame(this.nextMask.currentFrameset, this.nextMask.currentFrame, this.nextMask.nextFrameset, this.nextMask.nextFrame, this.currentFrameDuration, this.type!!.durations!![this.currentFrameId], rotation, this.type!!.lights)
            }
        }
    }

    fun animateUnrotatedModel(model: Model, submesh: Int) {
        if (this.type!!.frames != null && isMaskCalculated()) {
            model.animateUnrotatedSubmesh(this.currentMask.currentFrameset, this.currentMask.currentFrame, this.currentMask.nextFrameset, this.currentMask.nextFrame, this.currentFrameDuration, this.type!!.durations!![this.currentFrameId], submesh, this.type!!.lights, null)
            if (this.tweening && this.type!!.interfaceFrames != null && this.nextMask!!.functionmaskCalculated) {
                model.animateUnrotatedSubmesh(this.nextMask.currentFrameset, this.nextMask.currentFrame, this.nextMask.nextFrameset, this.nextMask.nextFrame, this.currentFrameDuration, this.type!!.durations!![this.currentFrameId], submesh, this.type!!.lights, null)
            }
        }
    }

    fun animateShadow(model: Model) {
        if (this.type!!.frames != null && isMaskCalculated()) {
            model.animateShadowFrame(this.currentMask.currentFrameset, this.currentMask.currentFrame)
            if (this.tweening && this.type!!.interfaceFrames != null && this.nextMask!!.functionmaskCalculated) {
                model.animateShadowFrame(this.nextMask.currentFrameset, this.nextMask.currentFrame)
            }
        }
    }

    fun needsReset(): Boolean {
        return this.resetAnimation
    }

    fun reset() {
        reset(0)
    }

    fun reset(newSpeed: Int) {
        this.currentFrameId = 0
        this.nextFrameId = if (this.type!!.frames!!.size > 1) 1 else -1
        this.currentFrameDuration = 0
        this.resetAnimation = false
        this.speed = newSpeed
        this.currentLoopCount = 0
        playSound(this.type!!, this.currentFrameId)
        resetMasks()
    }

    fun resetLoops() {
        this.currentLoopCount = 0
    }

    fun method7585(speed: Int): Boolean {
        var speedMod = speed
        return !((this.type == null) or ((speedMod - this.speed).also { speedMod = it } <= 0)) && (this.type!!.tweened or (this.currentFrameDuration + speedMod > this.type!!.durations!![this.currentFrameId]))
    }

    open fun playSound(seqtype: SeqType, frame: Int) {
        // Implemented in override
    }

    fun resetMasks() {
        this.currentMask.reset()
        if (this.tweening) {
            this.nextMask!!.reset()
        }
    }

    fun getId(): Int {
        return if (this.type != null) this.type!!.id else -1
    }

    fun animateSequence(sequenceId: Int, speed: Int) {
        animateSequence(sequenceId, speed, 0, false)
    }

    fun progressCycle(seqDuration: Int): Boolean {
        if (this.type != null && seqDuration != 0) {
            var seqDurationMod = seqDuration
            if (this.speed > 0) {
                if (this.speed >= seqDurationMod) {
                    this.speed -= seqDurationMod
                    return false
                }
                seqDurationMod -= this.speed
                this.speed = 0
                playSound(this.type!!, this.currentFrameId)
            }
            seqDurationMod += this.currentFrameDuration
            var tween = this.type!!.tweened or SeqTypeList.forceTween
            if (seqDurationMod > 100 && this.type!!.replayInterval > 0) {
                val fromFrame = this.type!!.frames!!.size - this.type!!.replayInterval
                while (this.currentFrameId < fromFrame && seqDurationMod > this.type!!.durations!![this.currentFrameId]) {
                    seqDurationMod -= this.type!!.durations!![this.currentFrameId]
                    this.currentFrameId++
                }
                if (this.currentFrameId >= fromFrame) {
                    var maxDuration = 0
                    for (frameIndex in fromFrame until this.type!!.frames!!.size) {
                        maxDuration += this.type!!.durations!![frameIndex]
                    }
                    if (this.replayMode == 0) {
                        this.currentLoopCount += seqDurationMod / maxDuration
                    }
                    seqDurationMod %= maxDuration
                }
                this.nextFrameId = this.currentFrameId + 1
                if (this.nextFrameId >= this.type!!.frames!!.size) {
                    this.nextFrameId -= this.type!!.replayInterval
                    if (this.nextFrameId < 0 || this.nextFrameId >= this.type!!.frames!!.size) {
                        this.nextFrameId = -1
                    }
                }
                tween = true
            }
            while (seqDurationMod > this.type!!.durations!![this.currentFrameId]) {
                tween = true
                seqDurationMod -= this.type!!.durations!![++this.currentFrameId - 1]
                if (this.currentFrameId >= this.type!!.frames!!.size) {
                    if (this.type!!.replayInterval != -1 && this.replayMode != 2) {
                        this.currentFrameId -= this.type!!.replayInterval
                        if (this.replayMode == 0) {
                            this.currentLoopCount++
                        }
                    }
                    if (this.currentLoopCount >= this.type!!.replayCount || this.currentFrameId < 0 || this.currentFrameId >= this.type!!.frames!!.size) {
                        this.resetAnimation = true
                        break
                    }
                }
                playSound(this.type!!, this.currentFrameId)
                this.nextFrameId = this.currentFrameId + 1
                if (this.nextFrameId >= this.type!!.frames!!.size) {
                    if (this.type!!.replayInterval == -1 && this.hardReset) {
                        this.nextFrameId = 0
                    } else {
                        this.nextFrameId -= this.type!!.replayInterval
                    }
                    if (this.nextFrameId < 0 || this.nextFrameId >= this.type!!.frames!!.size) {
                        this.nextFrameId = -1
                    }
                }
            }
            this.currentFrameDuration = seqDurationMod
            if (tween) {
                resetMasks()
            }
            return tween
        }
        return false
    }

    fun getFunctionMask(): Int {
        if (isMaskCalculated()) {
            var flags = 0
            if (isMaskCalculated()) {
                flags = flags or this.currentMask.functionMask
                if (this.tweening && this.type!!.interfaceFrames != null) {
                    flags = flags or this.nextMask!!.functionMask
                }
            }
            return flags
        }
        return 0
    }

    companion object {
        @JvmStatic
        fun doubleAnimation(model: Model, sequenceA: Sequence, sequenceB: Sequence) {
            if (sequenceA.isMaskCalculated() && sequenceB.isMaskCalculated()) {
                val typeA = sequenceA.type
                val typeB = sequenceB.type
                model.doubleAnimation(sequenceA.currentMask.currentFrameset, sequenceA.currentMask.currentFrame, sequenceA.currentMask.nextFrameset, sequenceA.currentMask.nextFrame, sequenceA.currentFrameDuration, typeA!!.durations!![sequenceA.currentFrameId], sequenceB.currentMask.currentFrameset, sequenceB.currentMask.currentFrame, sequenceB.currentMask.nextFrameset, sequenceB.currentMask.nextFrame, sequenceB.currentFrameDuration, typeB!!.durations!![sequenceB.currentFrameId], typeA.interLeaveOrder, typeA.lights or typeB.lights)
            }
        }
    }
}
