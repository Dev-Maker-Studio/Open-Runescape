package com.jagex.game.runetek5.config.flu;

import com.jagex.game.runetek5.buffer.Packet;

public class FluType {

    public int saturation;
    public int lightness;
    public int divisor;
    public int hue;
    public int texture = -1;
    public int scale = 512;
    public boolean shadowed = true;
    public boolean occlude = true;
    public int rgb;
    
    public void decode(Packet packet, int opcode) {
        if (opcode == 1) {
            this.rgb = packet.read24BitUnsignedInteger();
            calculateHsl16(this.rgb);
        } else if (opcode == 2) {
            this.texture = packet.readUnsignedShort();
            if (this.texture == 65535) {
                this.texture = -1;
            }
        } else if (opcode == 3) {
            this.scale = packet.readUnsignedShort() << 2;
        } else if (opcode == 4) {
            this.shadowed = false;
        } else if (opcode == 5) {
            this.occlude = false;
        }
    }

    void decode(Packet packet) {
        while (true) {
            int opcode = packet.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            decode(packet, opcode);
        }
    }

    public void calculateHsl16(int rgb) {
        double red = (rgb >> 16 & 0xff) / 256.0D;
        double green = (rgb >> 8 & 0xff) / 256.0D;
        double blue = (rgb & 0xff) / 256.0D;
        double d_9 = red;
        if (green < red) {
            d_9 = green;
        }

        if (blue < d_9) {
            d_9 = blue;
        }

        double d_11 = red;
        if (green > red) {
            d_11 = green;
        }

        if (blue > d_11) {
            d_11 = blue;
        }

        double d_13 = 0.0D;
        double d_15 = 0.0D;
        double d_17 = (d_11 + d_9) / 2.0D;
        if (d_11 != d_9) {
            if (d_17 < 0.5D) {
                d_15 = (d_11 - d_9) / (d_11 + d_9);
            }

            if (d_17 >= 0.5D) {
                d_15 = (d_11 - d_9) / (2.0D - d_11 - d_9);
            }

            if (red == d_11) {
                d_13 = (green - blue) / (d_11 - d_9);
            } else if (d_11 == green) {
                d_13 = 2.0D + (blue - red) / (d_11 - d_9);
            } else if (d_11 == blue) {
                d_13 = (red - green) / (d_11 - d_9) + 4.0D;
            }
        }

        d_13 /= 6.0D;
        this.saturation = (int) (d_15 * 256.0D);
        this.lightness = (int) (256.0D * d_17);
        if (this.saturation < 0) {
            this.saturation = 0;
        } else if (this.saturation > 255) {
            this.saturation = 255;
        }

        if (this.lightness < 0) {
            this.lightness = 0;
        } else if (this.lightness > 255) {
            this.lightness = 255;
        }

        if (d_17 > 0.5D) {
            this.divisor = (int) (d_15 * (1.0D - d_17) * 512.0D);
        } else {
            this.divisor = (int) (d_15 * d_17 * 512.0D);
        }

        if (this.divisor < 1) {
            this.divisor = 1;
        }

        this.hue = (int) (d_13 * this.divisor);
    }

}
