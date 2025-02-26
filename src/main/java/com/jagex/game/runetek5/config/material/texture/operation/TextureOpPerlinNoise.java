package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.graphics.texture.water.WaterTextureGenerator;
import com.jagex.game.runetek5.utils.ArrayUtils;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpPerlinNoise extends TextureOperation {

    public byte[] noiseArray = new byte[512];
    public int numNoiseSteps = 4;
    public int noiseAmplitude = 1638;
    public int texCoordScaleY = 4;
    public int texCoordScaleX = 4;
    public int randomSeed;
    public short[] stepMultipliers;
    public boolean useFogEffect = true;
    public short[] amplitudeMultipliers;

    public TextureOpPerlinNoise() {
        super(0, true);
    }

    public static int[][] generateNoiseMap() {
        int[][] noiseMap = new int[256][64];
        TextureOpPerlinNoise flickeringTexture = new TextureOpPerlinNoise();
        flickeringTexture.useFogEffect = false;
        flickeringTexture.texCoordScaleX = 4;
        flickeringTexture.texCoordScaleY = 4;
        flickeringTexture.numNoiseSteps = 3;
        flickeringTexture.noiseAmplitude = (int) (4096.0F * (float) 0.4);
        flickeringTexture.postDecode();
        TextureRaster.setDimensions(64, 256);
        for (int i = 0; i < 256; i++) {
            flickeringTexture.computeNoiseStep(i, noiseMap[i]);
        }
        return noiseMap;
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            computeNoiseStep(i_1, ints_3);
        }
        return ints_3;
    }

    public void computeNoiseStep(int i_1, int[] ints_2) {
        int i_4 = TextureRaster.verticalTable[i_1] * this.texCoordScaleY;
        short s_5;
        int i_6;
        int i_7;
        int i_8;
        int i_9;
        int i_10;
        int i_11;
        int i_12;
        int i_13;
        int i_14;
        int i_15;
        int i_16;
        int i_17;
        if (this.numNoiseSteps == 1) {
            s_5 = this.stepMultipliers[0];
            i_6 = this.amplitudeMultipliers[0] << 12;
            i_7 = i_6 * this.texCoordScaleY >> 12;
            i_8 = i_6 * this.texCoordScaleX >> 12;
            i_9 = i_6 * i_4 >> 12;
            i_10 = i_9 >> 12;
            i_11 = i_10 + 1;
            if (i_11 >= i_7) {
                i_11 = 0;
            }
            i_9 &= 0xfff;
            i_12 = WaterTextureGenerator.SINE_WAVE[i_9];
            i_13 = this.noiseArray[i_10 & 0xff] & 0xff;
            i_14 = this.noiseArray[i_11 & 0xff] & 0xff;
            if (this.useFogEffect) {
                for (i_15 = 0; i_15 < TextureRaster.currentWidth; i_15++) {
                    i_16 = TextureRaster.horizontalTable[i_15] * this.texCoordScaleX;
                    i_17 = computeNoise(i_6 * i_16 >> 12, i_9, i_13, i_14, i_12, i_8);
                    i_17 = s_5 * i_17 >> 12;
                    ints_2[i_15] = (i_17 >> 1) + 2048;
                }
            } else {
                for (i_15 = 0; i_15 < TextureRaster.currentWidth; i_15++) {
                    i_16 = TextureRaster.horizontalTable[i_15] * this.texCoordScaleX;
                    i_17 = computeNoise(i_6 * i_16 >> 12, i_9, i_13, i_14, i_12, i_8);
                    ints_2[i_15] = s_5 * i_17 >> 12;
                }
            }
        } else {
            s_5 = this.stepMultipliers[0];
            if (s_5 > 8 || s_5 < -8) {
                i_6 = this.amplitudeMultipliers[0] << 12;
                i_7 = i_6 * this.texCoordScaleY >> 12;
                i_8 = i_6 * this.texCoordScaleX >> 12;
                i_9 = i_4 * i_6 >> 12;
                i_10 = i_9 >> 12;
                i_11 = i_10 + 1;
                if (i_11 >= i_7) {
                    i_11 = 0;
                }
                i_9 &= 0xfff;
                i_12 = WaterTextureGenerator.SINE_WAVE[i_9];
                i_13 = this.noiseArray[i_10 & 0xff] & 0xff;
                i_14 = this.noiseArray[i_11 & 0xff] & 0xff;
                for (i_15 = 0; i_15 < TextureRaster.currentWidth; i_15++) {
                    i_16 = TextureRaster.horizontalTable[i_15] * this.texCoordScaleX;
                    i_17 = computeNoise(i_6 * i_16 >> 12, i_9, i_13, i_14, i_12, i_8);
                    ints_2[i_15] = i_17 * s_5 >> 12;
                }
            }
            for (i_6 = 1; i_6 < this.numNoiseSteps; i_6++) {
                s_5 = this.stepMultipliers[i_6];
                if (s_5 > 8 || s_5 < -8) {
                    i_7 = this.amplitudeMultipliers[i_6] << 12;
                    i_8 = i_7 * this.texCoordScaleY >> 12;
                    i_9 = i_7 * this.texCoordScaleX >> 12;
                    i_10 = i_4 * i_7 >> 12;
                    i_11 = i_10 >> 12;
                    i_12 = i_11 + 1;
                    if (i_12 >= i_8) {
                        i_12 = 0;
                    }
                    i_10 &= 0xfff;
                    i_13 = WaterTextureGenerator.SINE_WAVE[i_10];
                    i_14 = this.noiseArray[i_11 & 0xff] & 0xff;
                    i_15 = this.noiseArray[i_12 & 0xff] & 0xff;
                    int i_18;
                    if (this.useFogEffect && i_6 == this.numNoiseSteps - 1) {
                        for (i_16 = 0; i_16 < TextureRaster.currentWidth; i_16++) {
                            i_17 = TextureRaster.horizontalTable[i_16] * this.texCoordScaleX;
                            i_18 = computeNoise(i_17 * i_7 >> 12, i_10, i_14, i_15, i_13, i_9);
                            i_18 = ints_2[i_16] + (s_5 * i_18 >> 12);
                            ints_2[i_16] = (i_18 >> 1) + 2048;
                        }
                    } else {
                        for (i_16 = 0; i_16 < TextureRaster.currentWidth; i_16++) {
                            i_17 = TextureRaster.horizontalTable[i_16] * this.texCoordScaleX;
                            i_18 = computeNoise(i_7 * i_17 >> 12, i_10, i_14, i_15, i_13, i_9);
                            ints_2[i_16] += i_18 * s_5 >> 12;
                        }
                    }
                }
            }
        }
    }

    public int computeNoise(int coordX, int coordY, int i_3, int i_4, int i_5, int i_6) {
        int posX = coordX >> 12;
        int nextPosX = posX + 1;
        if (nextPosX >= i_6) {
            nextPosX = 0;
        }
        coordX &= 0xfff;
        posX &= 0xff;
        nextPosX &= 0xff;
        int negPosX = coordX - 4096;
        int negPosY = coordY - 4096;
        int noise1 = WaterTextureGenerator.SINE_WAVE[coordX];
        int noiseType1 = this.noiseArray[i_3 + posX] & 0x3;
        int i_14;
        if (noiseType1 <= 1) {
            i_14 = noiseType1 == 0 ? coordY + coordX : coordY - coordX;
        } else {
            i_14 = noiseType1 == 2 ? coordX - coordY : -coordX - coordY;
        }
        noiseType1 = this.noiseArray[i_3 + nextPosX] & 0x3;
        int i_15;
        if (noiseType1 <= 1) {
            i_15 = noiseType1 == 0 ? negPosX + coordY : coordY - negPosX;
        } else {
            i_15 = noiseType1 == 2 ? negPosX - coordY : -negPosX - coordY;
        }
        int i_16 = i_14 + (noise1 * (i_15 - i_14) >> 12);
        noiseType1 = this.noiseArray[posX + i_4] & 0x3;
        if (noiseType1 <= 1) {
            i_14 = noiseType1 == 0 ? negPosY + coordX : negPosY - coordX;
        } else {
            i_14 = noiseType1 == 2 ? coordX - negPosY : -coordX - negPosY;
        }
        noiseType1 = this.noiseArray[nextPosX + i_4] & 0x3;
        if (noiseType1 <= 1) {
            i_15 = noiseType1 == 0 ? negPosX + negPosY : negPosY - negPosX;
        } else {
            i_15 = noiseType1 == 2 ? negPosX - negPosY : -negPosX - negPosY;
        }
        int i_17 = i_14 + (noise1 * (i_15 - i_14) >> 12);
        return i_16 + (i_5 * (i_17 - i_16) >> 12);
    }

    @Override
    public void postDecode() {
        this.noiseArray = ArrayUtils.generateRandomlySeededBytearray(this.randomSeed);
        computeNoiseStep();
        for (int i = this.numNoiseSteps - 1; i >= 1; --i) {
            short stepMultiplier = this.stepMultipliers[i];
            if (stepMultiplier > 8 || stepMultiplier < -8) {
                break;
            }
            --this.numNoiseSteps;
        }
    }

    @Override
    public void decodeOpcode(int opcode, Packet buffer) {
        switch (opcode) {
            case 0:
                this.useFogEffect = buffer.readUByte() == 1;
                break;
            case 1:
                this.numNoiseSteps = buffer.readUByte();
                break;
            case 2:
                this.noiseAmplitude = buffer.readShort();
                if (this.noiseAmplitude < 0) {
                    this.stepMultipliers = new short[this.numNoiseSteps];
                    for (int i_4 = 0; i_4 < this.numNoiseSteps; i_4++) {
                        this.stepMultipliers[i_4] = (short) buffer.readShort();
                    }
                }
                break;
            case 3:
                this.texCoordScaleX = this.texCoordScaleY = buffer.readUByte();
                break;
            case 4:
                this.randomSeed = buffer.readUByte();
                break;
            case 5:
                this.texCoordScaleX = buffer.readUByte();
                break;
            case 6:
                this.texCoordScaleY = buffer.readUByte();
        }
    }

    public void computeNoiseStep() {
        if (this.noiseAmplitude > 0) {
            this.stepMultipliers = new short[this.numNoiseSteps];
            this.amplitudeMultipliers = new short[this.numNoiseSteps];
            for (int i = 0; i < this.numNoiseSteps; i++) {
                this.stepMultipliers[i] = (short) ((int) (Math.pow(this.noiseAmplitude / 4096.0F, i) * 4096.0D));
                this.amplitudeMultipliers[i] = (short) ((int) Math.pow(2.0D, i));
            }
        } else if (this.stepMultipliers != null && this.stepMultipliers.length == this.numNoiseSteps) {
            this.amplitudeMultipliers = new short[this.numNoiseSteps];
            for (int i = 0; i < this.numNoiseSteps; i++) {
                this.amplitudeMultipliers[i] = (short) ((int) Math.pow(2.0D, i));
            }
        }
    }

	public static int[] generateFlickeringEffect() {
	    int[] flickeringEffect = new int[2048];
	    TextureOpPerlinNoise flickeringTexture = new TextureOpPerlinNoise();
	    flickeringTexture.randomSeed = 35;
	    flickeringTexture.useFogEffect = true;
	    flickeringTexture.texCoordScaleX = 8;
	    flickeringTexture.texCoordScaleY = 8;
	    flickeringTexture.numNoiseSteps = 4;
	    flickeringTexture.noiseAmplitude = (int) (4096.0F * (float) 0.4);
	    flickeringTexture.postDecode();
	    TextureRaster.setDimensions(2048, 1);
	    flickeringTexture.computeNoiseStep(0, flickeringEffect);
	    return flickeringEffect;
	}
}
