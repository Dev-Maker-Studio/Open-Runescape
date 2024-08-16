package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.graphics.sprite.INativeSpriteGenerator;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.utils.Utils;

import jagtheora.ogg.OggPacket;
import jagtheora.ogg.OggStreamState;
import jagtheora.theora.*;

/**
 * 
 * @author Brainpower - 12/01/2022
 *
 */
public class JagTheoraTheoraNode extends Node_Sub41 {

    public boolean unusedBooleanV2;
    public long packetDecodeTime;
    public boolean headerDecoded;
    public double granuleTime;
    public DecoderContext decoderContext;
    public GranulePos granulePos;
    public Frame frame;
    public int maxPostProcessingLevel;
    public boolean frameDecoded;
    public boolean unusedBoolean;
    public int currentPostProcessingLevel;
    public NativeSprite frameSprite;
    public final SetupInfo setupInfo = new SetupInfo();
    public final TheoraInfo info = new TheoraInfo();
    public final TheoraComment comment = new TheoraComment();

    public JagTheoraTheoraNode(OggStreamState streamState) {
        super(streamState);
    }

    @Override
    void decode(OggPacket packet) {
        if (!this.headerDecoded) {
            int header = this.setupInfo.decodeHeader(this.info, this.comment, packet);
            if (header == 0) {
                this.headerDecoded = true;
                if (this.info.frameWidth > 2048 || this.info.frameHeight > 1024) {
                    throw new IllegalStateException();
                }

                this.decoderContext = new DecoderContext(this.info, this.setupInfo);
                this.granulePos = new GranulePos();
                this.frame = new Frame(this.info.frameWidth, this.info.frameHeight);
                this.maxPostProcessingLevel = this.decoderContext.getMaxPostProcessingLevel();
                setPostProcessingLevel(this.currentPostProcessingLevel);
            } else if (header < 0) {
                throw new IllegalStateException("" + header);
            }
        } else {
            this.packetDecodeTime = Utils.time();
            int packetIn = this.decoderContext.decodePacketIn(packet, this.granulePos);
            if (packetIn < 0) {
                throw new IllegalStateException("" + packetIn);
            }

            this.decoderContext.granuleFrame(this.granulePos);
            this.granuleTime = this.decoderContext.granuleTime(this.granulePos);
            if (this.unusedBoolean) {
                boolean keyframe = packet.isKeyFrame() == 1;
                if (!keyframe) {
                    return;
                }

                this.unusedBoolean = false;
            }

            if (!this.unusedBooleanV2 || packet.isKeyFrame() == 1) {
                if (this.decoderContext.decodeFrame(this.frame) != 0) {
                    throw new IllegalStateException("" + packetIn);
                }

                this.frameDecoded = true;
            }
        }

    }

    public void setPostProcessingLevel(int processingLevel) {
        this.currentPostProcessingLevel = processingLevel;
        if (this.headerDecoded) {
            if (this.currentPostProcessingLevel > this.maxPostProcessingLevel) {
            	this.currentPostProcessingLevel = this.maxPostProcessingLevel;
            }

            if (this.currentPostProcessingLevel < 0) {
            	this.currentPostProcessingLevel = 0;
            }

            this.decoderContext.setPostProcessingLevel(this.currentPostProcessingLevel);
        }
    }

    public float getFps() {
        return this.headerDecoded && !this.info.peerPresent() ? (float) this.info.fpsNumerator / this.info.fpsDenominator : 0.0F;
    }

    public double getGranuleTime() {
        return this.granuleTime;
    }

    public long getPacketDecodeTime() {
        return this.packetDecodeTime;
    }

    public boolean isHeaderDecoded() {
        return this.headerDecoded;
    }

    @Override
    public void reset() {
        if (this.frame != null) {
            this.frame.resetPeer();
        }

        if (this.decoderContext != null) {
            this.decoderContext.resetPeer();
            this.decoderContext = null;
        }

        if (this.granulePos != null) {
            this.granulePos.resetPeer();
            this.granulePos = null;
        }

        this.info.resetPeer();
        this.comment.resetPeer();
        this.setupInfo.resetPeer();
    }

    public NativeSprite getFrameSprite(INativeSpriteGenerator spriteGenerator) {
        if (this.frame == null) {
            return null;
        } else if (!this.frameDecoded && this.frameSprite != null) {
            return this.frameSprite;
        } else {
            this.frameSprite = spriteGenerator.createSprite(this.frame.pixels, this.frame.width, this.frame.width, this.frame.height);
            this.frameDecoded = false;
            return this.frameSprite;
        }
    }
}
