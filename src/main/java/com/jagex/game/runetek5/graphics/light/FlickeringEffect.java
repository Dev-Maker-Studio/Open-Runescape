package com.jagex.game.runetek5.graphics.light;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpPerlinNoise;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.MathUtils;

/**
 * @author Brainpower
 *
 */
public class FlickeringEffect {

    public int level;
    public final int type;
    public PointLight pointLight;
    public final boolean growsUpwards;
    public final boolean growsDownwards;
    public final short[] ranges;
    public final int rotationOffset; // scalar > zaros
    public int surrounding;
    public int effect;
    public int ticker;
    public int duration;
	public static int[] flickeringTexture;

    public FlickeringEffect(AbstractToolkit toolkit, int sizeShift, Packet stream) {
        if (FlickeringEffect.flickeringTexture == null) {
            FlickeringEffect.createFlickeringTexture();
        }
        this.level = stream.readUByte();
        this.growsUpwards = (this.level & 0x8) != 0;
        this.growsDownwards = (this.level & 0x10) != 0;
        this.level &= 0x7;
        int positionX = stream.readUShort() << 2;
        int positionZ = stream.readUShort() << 2;
        int positionY = stream.readUShort() << 2;
        int size2d = stream.readUByte();
        int size3d = size2d * 2 + 1;
        this.ranges = new short[size3d];
        for (int index = 0; index < this.ranges.length; index++) {
            short bitpacked = (short) stream.readUShort();
            int height = bitpacked >>> 8;
            int lowestHeight = bitpacked & 0xff;
            if (height >= size3d) {
                height = size3d - 1;
            }
            if (lowestHeight > size3d - height) {
                lowestHeight = size3d - height;
            }
            this.ranges[index] = (short) (height << 8 | lowestHeight);
        }
        size2d = (size2d << sizeShift) + (1 << sizeShift >> 1);
        int color = ColorUtil.getHslToRgb() != null ? ColorUtil.getHslToRgb()[stream.readUShort()] : ColorUtil.getHsvToRgbTable()[ColorUtil.adjustHsv(stream.readUShort()) & 0xffff];
        int bitpacked = stream.readUByte();
        this.type = bitpacked & 0x1f;
        this.rotationOffset = (bitpacked & 0xe0) << 3;
        if (this.type != 31) {
            updateConstants();
        }
        generateLight(toolkit, positionX, positionY, positionZ, size2d, color);
    }

    public void generateLight(AbstractToolkit toolkit, int x, int y, int z, int size2d, int color) {
        this.pointLight = toolkit.createPointLight(x, y, z, size2d, color);
    }

    public void updateConstants() {
        switch (this.type) {
            case 2:
                this.effect = 1;
                this.surrounding = 0;
                this.ticker = 2048;
                this.duration = 2048;
                break;
            case 3:
                this.effect = 1;
                this.surrounding = 0;
                this.ticker = 2048;
                this.duration = 4096;
                break;
            case 4:
                this.effect = 4;
                this.surrounding = 0;
                this.ticker = 2048;
                this.duration = 2048;
                break;
            case 5:
                this.effect = 4;
                this.surrounding = 0;
                this.ticker = 2048;
                this.duration = 8192;
                break;
            case 6:
                this.effect = 3;
                this.surrounding = 1280;
                this.ticker = 768;
                this.duration = 2048;
                break;
            case 7:
                this.effect = 3;
                this.surrounding = 1280;
                this.ticker = 768;
                this.duration = 4096;
                break;
            case 8:
                this.effect = 3;
                this.surrounding = 1024;
                this.ticker = 1024;
                this.duration = 2048;
                break;
            case 9:
                this.effect = 3;
                this.surrounding = 1024;
                this.ticker = 1024;
                this.duration = 4096;
                break;
            case 10:
                this.effect = 3;
                this.surrounding = 1536;
                this.ticker = 512;
                this.duration = 2048;
                break;
            case 11:
                this.effect = 3;
                this.surrounding = 1536;
                this.ticker = 512;
                this.duration = 4096;
                break;
            case 12:
                this.effect = 2;
                this.surrounding = 0;
                this.ticker = 2048;
                this.duration = 2048;
                break;
            case 13:
                this.effect = 2;
                this.surrounding = 0;
                this.ticker = 2048;
                this.duration = 8192;
                break;
            case 14:
                this.effect = 1;
                this.surrounding = 1280;
                this.ticker = 768;
                this.duration = 2048;
                break;
            case 15:
                this.effect = 1;
                this.surrounding = 1536;
                this.ticker = 512;
                this.duration = 4096;
                break;
            case 16:
                this.effect = 1;
                this.surrounding = 1792;
                this.ticker = 256;
                this.duration = 8192;
                break;
            default:
                this.surrounding = 0;
                this.effect = 0;
                this.ticker = 2048;
                this.duration = 2048;
        }
    }

    public void applyEffect(int baseRotation, boolean disableFlickingEffects) {
        int xan;
        if (!disableFlickingEffects) {
            int rotation = this.duration * baseRotation / 50 + this.rotationOffset & 0x7ff;
            switch (this.effect) {
                case 1:
                    xan = (MathUtils.SINE[rotation << 3] >> 4) + 1024;
                    break;
                case 2:
                    xan = rotation;
                    break;
                case 3:
                    xan = FlickeringEffect.flickeringTexture[rotation] >> 1;
                    break;
                case 4:
                    xan = rotation >> 10 << 11;
                    break;
                case 5:
                    xan = (rotation < 1024 ? rotation : 2048 - rotation) << 1;
                    break;
                default:
                    xan = 2048;
            }
        } else {
            xan = 2048;
        }
        this.pointLight.setTransparency(((xan * this.ticker >> 11) + this.surrounding) / 2048.0F);
    }

    public void setType(int effect, int duration, int ticker, int surrounding) {
        this.effect = effect;
        this.surrounding = surrounding;
        this.ticker = ticker;
        this.duration = duration;
    }

	public static void createFlickeringTexture() {
	    FlickeringEffect.flickeringTexture = TextureOpPerlinNoise.generateFlickeringEffect();
	}
}
