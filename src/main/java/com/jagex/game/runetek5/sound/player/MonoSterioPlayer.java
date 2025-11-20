package com.jagex.game.runetek5.sound.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import com.jagex.game.runetek5.utils.MathUtils;

public class MonoSterioPlayer extends PcmPlayer {

    public AudioFormat audioFormat;
    public byte[] buffer;
    public boolean hasSoundmax;
    public SourceDataLine sourceDataLine;
    public int bufferLength;

    @Override
    public void initialize() {
        Info[] mixerInfos = AudioSystem.getMixerInfo();
        if (mixerInfos != null) {
            for (Info mixerInfo : mixerInfos) {
                if (mixerInfo != null) {
                    String mixerInfoName = mixerInfo.getName();
                    if (mixerInfoName != null && mixerInfoName.toLowerCase().indexOf("soundmax") >= 0) {
                        this.hasSoundmax = true;
                    }
                }
            }
        }

        this.audioFormat = new AudioFormat(PcmPlayerManager.sampleRate, 16, PcmPlayerManager.stereo ? 2 : 1, true, false);
        this.buffer = new byte[256 << (PcmPlayerManager.stereo ? 2 : 1)];
    }

    @Override
    public void play(int bufferLength) throws LineUnavailableException {
        try {
        	DataLine.Info dataline = new DataLine.Info(SourceDataLine.class, this.audioFormat, bufferLength << (PcmPlayerManager.stereo ? 2 : 1));
            this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataline);
            this.sourceDataLine.open();
            this.sourceDataLine.start();
            this.bufferLength = bufferLength;
        } catch (LineUnavailableException lineUnavailableException) {
            if (invalid(bufferLength) == 1) {
            	this.sourceDataLine = null;
                throw lineUnavailableException;
            }

            play(MathUtils.nextPowerOfTwo(bufferLength));
        }
    }

    @Override
    public void writeToBuffer() {
        int byteCount = 256;
        if (PcmPlayerManager.stereo) {
            byteCount <<= 1;
        }

        for (int index = 0; index < byteCount; index++) {
            int data = super.bitpackedSoundData[index];
            if ((data + 8388608 & ~0xffffff) != 0) {
                data = 0x7fffff ^ data >> 31;
            }

            this.buffer[index * 2] = (byte) (data >> 8);
            this.buffer[index * 2 + 1] = (byte) (data >> 16);
        }

        this.sourceDataLine.write(this.buffer, 0, byteCount << 1);
    }

    @Override
    public void close() {
        if (this.sourceDataLine != null) {
            this.sourceDataLine.close();
            this.sourceDataLine = null;
        }
    }

    @Override
    public void open() throws LineUnavailableException {
        this.sourceDataLine.flush();
        if (this.hasSoundmax) {
            this.sourceDataLine.close();
            this.sourceDataLine = null;
            DataLine.Info dataline = new DataLine.Info(SourceDataLine.class, this.audioFormat, this.bufferLength << (PcmPlayerManager.stereo ? 2 : 1));
            this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataline);
            this.sourceDataLine.open();
            this.sourceDataLine.start();
        }
    }

    @Override
    public int dataLeft() {
        return this.bufferLength - (this.sourceDataLine.available() >> (PcmPlayerManager.stereo ? 2 : 1));
    }

	public static int invalid(int bufferLength) {
	    bufferLength = (bufferLength & 0x55555555) + (bufferLength >>> 1 & 0x55555555);
	    bufferLength = (bufferLength >>> 2 & 0x33333333) + (bufferLength & 0x33333333);
	    bufferLength = bufferLength + (bufferLength >>> 4) & 0xf0f0f0f;
	    bufferLength += bufferLength >>> 8;
	    bufferLength += bufferLength >>> 16;
	    return bufferLength & 0xff;
	}

}
