package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;

public abstract class AbstractCutsceneSpotAnimation extends CutsceneAction {

	public final int gfxId;

	public final int rotation;

	public final int displayHeight;

	public AbstractCutsceneSpotAnimation(Packet buffer) {
        super(buffer);
        this.gfxId = buffer.readUnsignedShort();
        this.rotation = buffer.readUnsignedShort();
        this.displayHeight = buffer.readUnsignedByte();
    }

    @Override
    public boolean isFullyCached() {
        SpotAnimType gfxType = TypeLists.SPOT_ANIM.list(this.gfxId);
        boolean ready = gfxType.isModelLoaded();
        SeqType seqType = TypeLists.SEQ.list(gfxType.sequenceId);
        ready &= seqType.ready();
        return ready;
    }
}
