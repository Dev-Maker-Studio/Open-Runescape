package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.graphics.model.Model;

public class Sequence {

    public SeqType type;
    public boolean resetAnimation;
    public boolean tweening;
    public int speed;
    public int currentLoopCount;
    public int currentFrameId;
    public int nextFrameId;
    public int currentFrameDuration;
    public int replayMode;
    public boolean hardReset;
    public final SequenceMask currentMask;
    public final SequenceMask nextMask;

    protected Sequence(boolean tween) {
        this.tweening = tween; // could be incorrect
        this.currentMask = new SequenceMask();
        if (this.tweening) {
            this.nextMask = new SequenceMask();
        } else {
            this.nextMask = null;
        }
    }

    public boolean isMaskCalculated() {
        if (this.type != null) {
            boolean maskCalculated = this.currentMask.isCalculated(SeqTypeList.seqTypeList, this.type, this.currentFrameId, this.nextFrameId, this.type.frames);
            if (maskCalculated && this.tweening && this.type.interfaceFrames != null) {
                this.nextMask.isCalculated(SeqTypeList.seqTypeList, this.type, this.currentFrameId, this.nextFrameId, this.type.interfaceFrames);
            }
            return maskCalculated;
        }
        return false;
    }

    public void copy(Sequence from) {
        this.type = from.type;
        this.resetAnimation = from.resetAnimation;
        this.tweening = from.tweening;
        this.speed = from.speed;
        this.currentLoopCount = from.currentLoopCount;
        this.currentFrameId = from.currentFrameId;
        this.nextFrameId = from.nextFrameId;
        this.currentFrameDuration = from.currentFrameDuration;
        resetMasks();
    }

    public boolean hasType() {
        return this.type != null;
    }

    public SeqType getType() {
        return this.type;
    }

    public void update(int sequenceId) {
        animateSequence(sequenceId, 0, 0, false);
    }

    public void resetSequence() {
        animateSequence(-1, 0, 0, false);
    }

    public void animate(int sequenceId) {
        animate(sequenceId, 0, 0, false, true);
    }

    public void animateSequence(int sequenceId, int speed, int replayMode, boolean replay) {
        animate(sequenceId, speed, replayMode, replay, false);
    }

    public void animate(int sequenceId, int speed, int replayMode, boolean replay, boolean hardReset) {
        if (sequenceId != getId()) {
            if (sequenceId == -1) {
                this.type = null;
            } else {
                if (this.type != null && this.type.id == sequenceId) {
                    if (this.type.loopType == 0) {
                        return;
                    }
                } else {
                    this.type = SeqTypeList.seqTypeList.list(sequenceId);
                }
                this.currentLoopCount = 0;
                this.speed = speed;
                this.replayMode = replayMode;
                this.hardReset = hardReset;
                if (replay) {
                    this.currentFrameId = (int) (Math.random() * this.type.frames.length);
                    this.currentFrameDuration = (int) (Math.random() * this.type.durations[this.currentFrameId]);
                } else {
                    this.currentFrameId = 0;
                    this.currentFrameDuration = 0;
                }
                this.nextFrameId = this.currentFrameId + 1;
                if (this.nextFrameId >= this.type.frames.length) {
                    this.nextFrameId = -1;
                }
                if (this.speed == 0) {
                    playSound(this.type, this.currentFrameId);
                }
                this.resetAnimation = false;
            }
            resetMasks();
        }
    }

    public boolean hasSpeed() {
        return this.speed != 0;
    }

    public int getSpeed() {
        return this.speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void animateRotatedModel(Model model, int rotation) {
        if (this.type != null && this.type.frames != null && isMaskCalculated()) {
            model.animateModelFrame(this.currentMask.currentFrameset, this.currentMask.currentFrame, this.currentMask.nextFrameset, this.currentMask.nextFrame, this.currentFrameDuration, this.type.durations[this.currentFrameId], rotation, this.type.lights);
            if (this.tweening && this.type.interfaceFrames != null && this.nextMask.functionmaskCalculated) {
                model.animateModelFrame(this.nextMask.currentFrameset, this.nextMask.currentFrame, this.nextMask.nextFrameset, this.nextMask.nextFrame, this.currentFrameDuration, this.type.durations[this.currentFrameId], rotation, this.type.lights);
            }
        }
    }

    public void animateUnrotatedModel(Model model, int submesh) {
        if (this.type.frames != null && isMaskCalculated()) {
            model.animateUnrotatedSubmesh(this.currentMask.currentFrameset, this.currentMask.currentFrame, this.currentMask.nextFrameset, this.currentMask.nextFrame, this.currentFrameDuration, this.type.durations[this.currentFrameId], submesh, this.type.lights, null);
            if (this.tweening && this.type.interfaceFrames != null && this.nextMask.functionmaskCalculated) {
                model.animateUnrotatedSubmesh(this.nextMask.currentFrameset, this.nextMask.currentFrame, this.nextMask.nextFrameset, this.nextMask.nextFrame, this.currentFrameDuration, this.type.durations[this.currentFrameId], submesh, this.type.lights, null);
            }
        }
    }

    public void animateShadow(Model model) {
        if (this.type.frames != null && isMaskCalculated()) {
            model.animateShadowFrame(this.currentMask.currentFrameset, this.currentMask.currentFrame);
            if (this.tweening && this.type.interfaceFrames != null && this.nextMask.functionmaskCalculated) {
                model.animateShadowFrame(this.nextMask.currentFrameset, this.nextMask.currentFrame);
            }
        }
    }

    public boolean needsReset() {
        return this.resetAnimation;
    }

    public void reset() {
        reset(0);
    }

    public void reset(int newSpeed) {
        this.currentFrameId = 0;
        this.nextFrameId = this.type.frames.length > 1 ? 1 : -1;
        this.currentFrameDuration = 0;
        this.resetAnimation = false;
        this.speed = newSpeed;
        this.currentLoopCount = 0;
        playSound(this.type, this.currentFrameId);
        resetMasks();
    }

    public void resetLoops() {
        this.currentLoopCount = 0;
    }

    public boolean method7585(int speed) {
        return !(this.type == null | (speed -= this.speed) <= 0) && this.type.tweened | this.currentFrameDuration + speed > this.type.durations[this.currentFrameId];
    }

    public void playSound(SeqType seqtype, int frame) {
    	// Implemented in override
    }

    public void resetMasks() {
        this.currentMask.reset();
        if (this.tweening) {
            this.nextMask.reset();
        }
    }

    public int getId() {
        return this.type != null ? this.type.id : -1;
    }

    public void animateSequence(int sequenceId, int speed) {
        animateSequence(sequenceId, speed, 0, false);
    }

    public boolean progressCycle(int seqDuration) {
        if (this.type != null && seqDuration != 0) {
            if (this.speed > 0) {
                if (this.speed >= seqDuration) {
                    this.speed -= seqDuration;
                    return false;
                }
                seqDuration -= this.speed;
                this.speed = 0;
                playSound(this.type, this.currentFrameId);
            }
            seqDuration += this.currentFrameDuration;
            boolean tween = this.type.tweened | SeqTypeList.forceTween;
            if (seqDuration > 100 && this.type.replayInterval > 0) {
                int fromFrame;
                for (fromFrame = this.type.frames.length - this.type.replayInterval; this.currentFrameId < fromFrame && seqDuration > this.type.durations[this.currentFrameId]; this.currentFrameId++) {
                    seqDuration -= this.type.durations[this.currentFrameId];
                }
                if (this.currentFrameId >= fromFrame) {
                    int maxDuration = 0;
                    for (int frameIndex = fromFrame; frameIndex < this.type.frames.length; frameIndex++) {
                        maxDuration += this.type.durations[frameIndex];
                    }
                    if (this.replayMode == 0) {
                        this.currentLoopCount += seqDuration / maxDuration;
                    }
                    seqDuration %= maxDuration;
                }
                this.nextFrameId = this.currentFrameId + 1;
                if (this.nextFrameId >= this.type.frames.length) {
                    this.nextFrameId -= this.type.replayInterval;
                    if (this.nextFrameId < 0 || this.nextFrameId >= this.type.frames.length) {
                        this.nextFrameId = -1;
                    }
                }
                tween = true;
            }
            while (seqDuration > this.type.durations[this.currentFrameId]) {
                tween = true;
                seqDuration -= this.type.durations[++this.currentFrameId - 1];
                if (this.currentFrameId >= this.type.frames.length) {
                    if (this.type.replayInterval != -1 && this.replayMode != 2) {
                        this.currentFrameId -= this.type.replayInterval;
                        if (this.replayMode == 0) {
                            this.currentLoopCount++;
                        }
                    }
                    if (this.currentLoopCount >= this.type.replayCount || this.currentFrameId < 0 || this.currentFrameId >= this.type.frames.length) {
                        this.resetAnimation = true;
                        break;
                    }
                }
                playSound(this.type, this.currentFrameId);
                this.nextFrameId = this.currentFrameId + 1;
                if (this.nextFrameId >= this.type.frames.length) {
                    if (this.type.replayInterval == -1 && this.hardReset) {
                        this.nextFrameId = 0;
                    } else {
                        this.nextFrameId -= this.type.replayInterval;
                    }
                    if (this.nextFrameId < 0 || this.nextFrameId >= this.type.frames.length) {
                        this.nextFrameId = -1;
                    }
                }
            }
            this.currentFrameDuration = seqDuration;
            if (tween) {
                resetMasks();
            }
            return tween;
        }
        return false;        
    }

    public int getFunctionMask() {
        if (isMaskCalculated()) {
            int flags = 0;
            if (isMaskCalculated()) {
                flags |= this.currentMask.functionMask;
                if (this.tweening && this.type.interfaceFrames != null) {
                    flags |= this.nextMask.functionMask;
                }
            }
            return flags;
        } 
        return 0;
    }

	public static void doubleAnimation(Model model, Sequence sequenceA, Sequence sequenceB) {
	    if (sequenceA.isMaskCalculated() && sequenceB.isMaskCalculated()) {
	        SeqType typeA = sequenceA.type;
	        SeqType typeB = sequenceB.type;
	        model.doubleAnimation(sequenceA.currentMask.currentFrameset, sequenceA.currentMask.currentFrame, sequenceA.currentMask.nextFrameset, sequenceA.currentMask.nextFrame, sequenceA.currentFrameDuration, typeA.durations[sequenceA.currentFrameId], sequenceB.currentMask.currentFrameset, sequenceB.currentMask.currentFrame, sequenceB.currentMask.nextFrameset, sequenceB.currentMask.nextFrame, sequenceB.currentFrameDuration, typeB.durations[sequenceB.currentFrameId], typeA.interLeaveOrder, typeA.lights | typeB.lights);
	    }
	}
}
