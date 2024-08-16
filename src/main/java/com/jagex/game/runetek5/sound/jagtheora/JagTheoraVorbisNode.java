package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.sound.Resampler;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;

import jagtheora.ogg.OggPacket;
import jagtheora.ogg.OggStreamState;
import jagtheora.vorbis.DSPState;
import jagtheora.vorbis.VorbisBlock;
import jagtheora.vorbis.VorbisComment;
import jagtheora.vorbis.VorbisInfo;

public class JagTheoraVorbisNode extends Node_Sub41 {

    public final VorbisInfo aVorbisInfo9605 = new VorbisInfo();
    public final VorbisComment aVorbisComment9599 = new VorbisComment();
    public double aDouble9604;
    public int anInt9602;
    public DSPState aDSPState9600;
    public VorbisBlock aVorbisBlock9601;
    public Resampler aClass3449598;
    public Node_Sub15_Sub1 aNodeSub15Sub19603;

    public JagTheoraVorbisNode(OggStreamState oggstreamstate1) {
        super(oggstreamstate1);
    }

    public static short[][] method12805(float[][] floats_0, short[][] shorts_1) {
        for (int i_3 = 0; i_3 < floats_0.length; i_3++) {
            for (int i_4 = 0; i_4 < shorts_1[i_3].length; i_4++) {
                shorts_1[i_3][i_4] = (short) ((int) (16383.0F * floats_0[i_3][i_4]));
            }
        }
        return shorts_1;
    }

    @Override
    void decode(OggPacket oggpacket1) {
        if (super.anInt8031 < 3) {
            int i_3 = this.aVorbisInfo9605.headerIn(this.aVorbisComment9599, oggpacket1);
            if (i_3 < 0) {
                throw new IllegalStateException("" + i_3);
            }
            if (super.anInt8031 == 2) {
                if (this.aVorbisInfo9605.channels > 2 || this.aVorbisInfo9605.channels < 1) {
                    throw new RuntimeException("" + this.aVorbisInfo9605.channels);
                }
                this.aDSPState9600 = new DSPState(this.aVorbisInfo9605);
                this.aVorbisBlock9601 = new VorbisBlock(this.aDSPState9600);
                this.aClass3449598 = new Resampler(this.aVorbisInfo9605.rate, PcmPlayerManager.sampleRate);
                this.aNodeSub15Sub19603 = new Node_Sub15_Sub1(this.aVorbisInfo9605.channels);
            }
        } else {
            if (this.aVorbisBlock9601.synthesis(oggpacket1) == 0) {
                this.aDSPState9600.blockIn(this.aVorbisBlock9601);
            }
            float[][] floats_6 = this.aDSPState9600.pcmOut(this.aVorbisInfo9605.channels);
            this.aDouble9604 = this.aDSPState9600.granuleTime();
            if (this.aDouble9604 == -1.0) {
                this.aDouble9604 = (float) this.anInt9602 / this.aVorbisInfo9605.rate;
            }
            this.aDSPState9600.read(floats_6[0].length);
            this.anInt9602 += floats_6[0].length;
            MonoSterioData class282Sub394 = this.aNodeSub15Sub19603.method14814(floats_6[0].length, this.aDouble9604);
            method12805(floats_6, class282Sub394.aShortArrayArray8003);
            for (int i_5 = 0; i_5 < this.aVorbisInfo9605.channels; i_5++) {
                class282Sub394.aShortArrayArray8003[i_5] = this.aClass3449598.resample(class282Sub394.aShortArrayArray8003[i_5]);
            }
            this.aNodeSub15Sub19603.method14815(class282Sub394);
        }
    }

    public Node_Sub15_Sub1 method14845() {
        return this.aNodeSub15Sub19603;
    }


    public int method14847() {
        return this.aNodeSub15Sub19603 == null ? 0 : this.aNodeSub15Sub19603.method14811();
    }

    public double method14848() {
        double d_2 = this.aDouble9604;
        if (this.aNodeSub15Sub19603 != null) {
            d_2 = this.aNodeSub15Sub19603.method14819();
            if (d_2 < 0.0D) {
                d_2 = this.aDouble9604;
            }
        }
        return d_2 - (256.0F / PcmPlayerManager.sampleRate);
    }

    @Override
    public void reset() {
        if (this.aVorbisBlock9601 != null) {
            this.aVorbisBlock9601.resetPeer();
        }
        if (this.aDSPState9600 != null) {
            this.aDSPState9600.resetPeer();
        }
        this.aVorbisComment9599.resetPeer();
        this.aVorbisInfo9605.resetPeer();
        if (this.aNodeSub15Sub19603 != null) {
            this.aNodeSub15Sub19603.method14808();
        }
    }
}
