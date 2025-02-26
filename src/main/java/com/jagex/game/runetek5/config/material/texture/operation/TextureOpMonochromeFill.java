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
public class TextureOpMonochromeFill extends TextureOperation {

    public int anInt9860;

    public TextureOpMonochromeFill() {
        super(0, true);
        this.anInt9860 = 4096;
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, this.anInt9860);
        }
        return ints_3;
    }

    @Override
    public void decodeOpcode(int opcode, Packet stream) {
        switch (opcode) {
            case 0:
                this.anInt9860 = (stream.readUByte() << 12) / 255;
            default:
        }
    }
}
