package com.jagex.game.runetek5.graphics.texture.water;

class Class370_Sub1 extends WaterTextureGenerator {

    public byte[] aByteArray7752;
    public int intensity;
    public int displacement;
    public int attenuation;
    public int velocity;
    public int turbulence;
    public final int initialIntensity;
    public final int attenuationFactor;
    public final int displacementFactor;

    Class370_Sub1() {
        super(8, 8, 8);
        this.initialIntensity = (int) ((float) 3.0 * 4096.0F);
        this.attenuationFactor = (int) ((float) 0.55 * 4096.0F);
        this.velocity = this.displacementFactor = (int) (Math.pow(0.5D, -0.1f) * 4096.0D);
    }

    @Override
    void update(int i_1, int i_2) {
        if (i_1 != 0) {
            this.intensity = this.displacement * this.initialIntensity >> 12;
            if (this.intensity < 0) {
                this.intensity = 0;
            } else if (this.intensity > 4096) {
                this.intensity = 4096;
            }

            this.displacement = this.attenuationFactor - (i_2 < 0 ? -i_2 : i_2);
            this.displacement = this.displacement * this.displacement >> 12;
            this.displacement = this.displacement * this.intensity >> 12;
            this.attenuation += this.displacement * this.velocity >> 12;
            this.velocity = this.velocity * this.displacementFactor >> 12;
        } else {
            this.displacement = this.attenuationFactor - (i_2 < 0 ? -i_2 : i_2);
            this.displacement = this.displacement * this.displacement >> 12;
            this.intensity = 4096;
            this.attenuation = this.displacement;
        }

    }

    void set(int i_1, byte b_2) {
        this.aByteArray7752[i_1] = b_2;
    }

    @Override
    void reset() {
        this.velocity = this.displacementFactor;
        this.attenuation >>= 4;
        if (this.attenuation < 0) {
            this.attenuation = 0;
        } else if (this.attenuation > 255) {
            this.attenuation = 255;
        }

        set(this.turbulence++, (byte) this.attenuation);
        this.attenuation = 0;
    }

    @Override
    void clear() {
        this.turbulence = 0;
        this.attenuation = 0;
    }
}
