package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.utils.ArrayUtils;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpFlip extends TextureOperation {

    public boolean mirrorHorizontally = true;
    public boolean mirrorVertically = true;

    public TextureOpFlip() {
        super(1, false);
    }

    @Override
    public int[] getMonochromeOutput(int index) {
        int[] palette = super.monochromeImageCache.getPalette(index);
        if (super.monochromeImageCache.dirty) {
            int[] mirroredPalette = getChildMonochromeOutput(0, this.mirrorVertically ? TextureRaster.columnEnd - index : index);
            if (this.mirrorHorizontally) {
                for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                    palette[i_5] = mirroredPalette[TextureRaster.rowEnd - i_5];
                }
            } else {
                ArrayUtils.arrayCopy(mirroredPalette, 0, palette, 0, TextureRaster.currentWidth);
            }
        }
        return palette;
    }

    @Override
    public int[][] getColorOutput(int index) {
        int[][] palette = super.colorImageCache.getPalette(index);
        if (super.colorImageCache.dirty) {
            int[][] mirroredPalette = getChildColorOutput(0, this.mirrorVertically ? TextureRaster.columnEnd - index : index);
            int[] mirroredRed = mirroredPalette[0];
            int[] mirroredGreen = mirroredPalette[1];
            int[] mirroredBlue = mirroredPalette[2];
            int[] paletteRed = palette[0];
            int[] paletteGreen = palette[1];
            int[] paletteBlue = palette[2];
            if (this.mirrorHorizontally) {
                for (int i = 0; i < TextureRaster.currentWidth; i++) {
                    paletteRed[i] = mirroredRed[TextureRaster.rowEnd - i];
                    paletteGreen[i] = mirroredGreen[TextureRaster.rowEnd - i];
                    paletteBlue[i] = mirroredBlue[TextureRaster.rowEnd - i];
                }
            } else {
                for (int i = 0; i < TextureRaster.currentWidth; i++) {
                    paletteRed[i] = mirroredRed[i];
                    paletteGreen[i] = mirroredGreen[i];
                    paletteBlue[i] = mirroredBlue[i];
                }
            }
        }
        return palette;
    }

    @Override
    public void decodeOpcode(int opcode, Packet buffer) {
        switch (opcode) {
            case 0:
                this.mirrorHorizontally = buffer.readUnsignedByte() == 1;
                break;
            case 1:
                this.mirrorVertically = buffer.readUnsignedByte() == 1;
                break;
            case 2:
                super.monochrome = buffer.readUnsignedByte() == 1;
        }
    }
}
