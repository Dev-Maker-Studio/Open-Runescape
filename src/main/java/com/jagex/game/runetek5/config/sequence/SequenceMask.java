package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.config.sequence.frameset.FrameSet;
import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class SequenceMask {

    public int currentFrame;

    public FrameSet currentFrameset;

    public int functionMask;

    public int nextFrame;

    public FrameSet nextFrameset;

    public boolean functionmaskCalculated;

    public boolean isCalculated(SeqTypeList seqtypelist, SeqType seqtype, int currentFrameId, int nextFrameId, int[] frames) {
    	if (this.functionmaskCalculated)
    		return true;
    	
        if (currentFrameId >= frames.length) {
            return false;
        } 
        this.currentFrame = frames[currentFrameId];
        this.currentFrameset = seqtypelist.listFrameset(this.currentFrame >> 16);
        this.currentFrame &= 0xffff;
        
        if (this.currentFrameset == null)
        	return false;
        
        if (seqtype.tweened && nextFrameId != -1 && nextFrameId < frames.length) {
            this.nextFrame = frames[nextFrameId];
            this.nextFrameset = seqtypelist.listFrameset(this.nextFrame >> 16);
            this.nextFrame &= 0xffff;
        }
        if (seqtype.lights) {
            this.functionMask |= ALTERS_LIGHT;
        }
        if (this.currentFrameset.modifiesColors(this.currentFrame)) {
            this.functionMask |= ALTERS_COLOR;
        }
        if (this.currentFrameset.modifiesAlpha(this.currentFrame)) {
            this.functionMask |= ALTERS_ALPHA;
        }
        if (this.currentFrameset.modifiesBillboards(this.currentFrame)) {
            this.functionMask |= ALTERS_BILLBOARDS;
        }
        if (this.nextFrameset != null) {
            if (this.nextFrameset.modifiesColors(this.nextFrame)) {
                this.functionMask |= ALTERS_COLOR;
            }
            if (this.nextFrameset.modifiesAlpha(this.nextFrame)) {
                this.functionMask |= ALTERS_ALPHA;
            }
            if (this.nextFrameset.modifiesBillboards(this.nextFrame)) {
                this.functionMask |= ALTERS_BILLBOARDS;
            }
        }
        this.functionMask |= ALTERS_VERTICES;
        this.functionmaskCalculated = true;
        return true; 
    }

    void reset() {
        this.functionmaskCalculated = false;
        this.functionMask = 0;
        this.nextFrameset = null;
        this.currentFrameset = null;
    }
}
