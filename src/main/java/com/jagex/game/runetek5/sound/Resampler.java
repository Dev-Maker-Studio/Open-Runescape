package com.jagex.game.runetek5.sound;

/**
 * https://www.rune-server.ee/runescape-development/rs2-client/downloads/673441-167-osrs-sound-effects-dumped-wav-format-2.html
 * @author CrazyPanda
 * 
 * @author Brainpower
 *
 */
public class Resampler {

    public int inputSampleRate;

    public int outputSampleRate;

    public int[][] resampleTable;

    public Resampler(int inputSampleRate, int outputSampleRate) {
        if (outputSampleRate != inputSampleRate) {
            int i_3 = Resampler.step(inputSampleRate, outputSampleRate);
            inputSampleRate /= i_3;
            outputSampleRate /= i_3;
            this.inputSampleRate = inputSampleRate;
            this.outputSampleRate = outputSampleRate;
            this.resampleTable = new int[inputSampleRate][14];
            for (int i_4 = 0; i_4 < inputSampleRate; i_4++) {
                int[] ints_5 = this.resampleTable[i_4];
                double d_6 = (double) i_4 / inputSampleRate + 6.0D;
                int i_8 = (int) Math.floor(d_6 - 7.0D + 1.0D);
                if (i_8 < 0) {
                    i_8 = 0;
                }
                int i_9 = (int) Math.ceil(7.0D + d_6);
                if (i_9 > 14) {
                    i_9 = 14;
                }
                for (double d_10 = (double) outputSampleRate / inputSampleRate; i_8 < i_9; i_8++) {
                    double d_12 = (i_8 - d_6) * Math.PI;
                    double d_14 = d_10;
                    if (d_12 < -1.0E-4 || d_12 > 1.0E-4D) {
                        d_14 = d_10 * (Math.sin(d_12) / d_12);
                    }
                    d_14 *= 0.54D + 0.46D * Math.cos(0.2243994752564138D * (i_8 - d_6));
                    ints_5[i_8] = (int) Math.floor(d_14 * 65536.0D + 0.5D);
                }
            }
        }
    }
    
	public static int step(int inputSampleRate, int outputSampleRate) {
	    if (outputSampleRate > inputSampleRate) {
	        int tmp = inputSampleRate;
	        inputSampleRate = outputSampleRate;
	        outputSampleRate = tmp;
	    }	
	    while (outputSampleRate != 0) {
	        int tmp = inputSampleRate % outputSampleRate;
	        inputSampleRate = outputSampleRate;
	        outputSampleRate = tmp;
	    }	
	    return inputSampleRate;
	}

    public byte[] resample(byte[] inputSignal) {
        byte[] outputSignal = inputSignal;
        if (this.resampleTable != null) {
            int outputLength = (int) ((long) this.outputSampleRate * outputSignal.length / this.inputSampleRate) + 14;
            int[] resampledData = new int[outputLength];
            int i = 0;
            int j = 0;
            for (int k = 0; k < outputSignal.length; k++) {
                byte inputSample = outputSignal[k];
                int[] resampleCoefficients = this.resampleTable[j];
                for (int l = 0; l < 14; l++) {
                    resampledData[i + l] += inputSample * resampleCoefficients[l];
                }
                j += this.outputSampleRate;
                int numInputSamples = j / this.inputSampleRate;
                i += numInputSamples;
                j -= numInputSamples * this.inputSampleRate;
            }
            outputSignal = new byte[outputLength];
            for (int k = 0; k < outputLength; k++) {
                int i_11 = resampledData[k] + 32768 >> 16;
                if (i_11 < -128) {
                    outputSignal[k] = -128;
                } else if (i_11 > 127) {
                    outputSignal[k] = 127;
                } else {
                    outputSignal[k] = (byte) i_11;
                }
            }
        }
        return outputSignal;
    }

    public short[] resample(short[] inputSignal) {
        short[] outputSignal = inputSignal;
        if (this.resampleTable != null) {
            int outputLength = (int) ((long) outputSignal.length * this.outputSampleRate / this.inputSampleRate) + 14;
            int[] resampledData = new int[outputLength];
            int i = 0;
            int j = 0;
            for (int k = 0; k < outputSignal.length; k++) {
                short inputSample = outputSignal[k];
                int[] resampleCoefficients = this.resampleTable[j];
                for (int l = 0; l < 14; l++) {
                    resampledData[l + i] += resampleCoefficients[l] * inputSample >> 2;
                }
                j += this.outputSampleRate;
                int numInputSamples = j / this.inputSampleRate;
                i += numInputSamples;
                j -= numInputSamples * this.inputSampleRate;
            }
            outputSignal = new short[outputLength];
            for (int k = 0; k < outputLength; k++) {
                int resampledSample = resampledData[k] + 8192 >> 14;
                if (resampledSample < -32768) {
                    outputSignal[k] = -32768;
                } else if (resampledSample > 32767) {
                    outputSignal[k] = 32767;
                } else {
                    outputSignal[k] = (short) resampledSample;
                }
            }
        }
        return outputSignal;
    }

    public int scaleRate(int sampleRate) {
        if (this.resampleTable != null) {
            sampleRate = (int) ((long) this.outputSampleRate * sampleRate / this.inputSampleRate);
        }
        return sampleRate;
    }

    public int scalePosition(int position) {
        if (this.resampleTable != null) {
            position = (int) ((long) this.outputSampleRate * position / this.inputSampleRate) + 6;
        }
        return position;
    }
}