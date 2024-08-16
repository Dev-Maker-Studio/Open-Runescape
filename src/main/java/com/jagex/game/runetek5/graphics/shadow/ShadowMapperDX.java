package com.jagex.game.runetek5.graphics.shadow;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader;
import com.jagex.game.runetek5.scene.ground.GroundDX;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;

public class ShadowMapperDX {

    public ShadowTileDX[][] tiles;
    public final AbstractNativeToolkit toolkit;
    public final GroundDX groundDx;
    public final int width;
    public final int length;
    public final byte[] shadowMapperData;
    public final int sizeShift;
    public final int sizeX;
    public final int sizeY;

    public ShadowMapperDX(AbstractNativeToolkit toolkit, GroundDX ground) {
        this.toolkit = toolkit;
        this.groundDx = ground;
        this.width = (this.groundDx.width * this.groundDx.tileUnits >> toolkit.shadowSizeShift) + 2;
        this.length = (this.groundDx.length * this.groundDx.tileUnits >> toolkit.shadowSizeShift) + 2;
        this.shadowMapperData = new byte[this.width * this.length];
        this.sizeShift = toolkit.shadowSizeShift + 7 - this.groundDx.tileScale;
        this.sizeX = this.groundDx.width >> this.sizeShift;
        this.sizeY = this.groundDx.length >> this.sizeShift;
    }

    public void method1312() {
        this.tiles = new ShadowTileDX[this.sizeX][this.sizeY];

        for (int z = 0; z < this.sizeY; z++) {
            for (int x = 0; x < this.sizeX; x++) {
                this.tiles[x][z] = new ShadowTileDX(this.toolkit, this, this.groundDx, x, z, this.sizeShift, x * 128 + 1, z * 128 + 1);
                if (this.tiles[x][z].anInt716 == 0) {
                    this.tiles[x][z] = null;
                }
            }
        }

    }

    public void method1314(AbstractEnvironmentShader shader, int camX, int camZ, int maxDistance, boolean[][] visibleTiles, boolean fullRender) {
        float invTileUnits = 1.0F / (this.toolkit.activeFlags * 128);

        if (fullRender) {
            for (int z = 0; z < this.sizeY; z++) {
                int startZ = z << this.sizeShift;
                int endZ = z + 1 << this.sizeShift;

                label147:
                for (int x = 0; x < this.sizeX; x++) {
                    if (this.tiles[x][z] != null) {
                        int startX = x << this.sizeShift;
                        int endX = x + 1 << this.sizeShift;

                        for (int currX = startX; currX < endX; currX++) {
                            if (currX - camX >= -maxDistance && currX - camX <= maxDistance) {
                                for (int currZ = startZ; currZ < endZ; currZ++) {
                                    if (currZ - camZ >= -maxDistance && currZ - camZ <= maxDistance && visibleTiles[currX - camX + maxDistance][maxDistance + (currZ - camZ)]) {
                                        shader.textureCoordinateMatrix.method6525(invTileUnits, invTileUnits, 1.0F, 1.0F);
                                        shader.textureCoordinateMatrix.faceList[12] = (-x);
                                        shader.textureCoordinateMatrix.faceList[13] = (-z);
                                        this.tiles[x][z].method1288(shader);
                                        continue label147;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            int[] ints_24 = new int[this.sizeY * this.sizeX];
            ByteBuffer bytebuffer9 = this.toolkit.directBuffer;
            bytebuffer9.clear();
            int i_10 = 0;

            for (int z = 0; z < this.sizeY; z++) {
            	int startZ = z << this.sizeShift;
            	int endZ = z + 1 << this.sizeShift;

                for (int x = 0; x < this.sizeX; x++) {
                    ShadowTileDX tile = this.tiles[x][z];
                    int i_16 = 0;
                    if (tile != null) {
                        int startX = x << this.sizeShift;
                        int endX = x + 1 << this.sizeShift;

                        for (int currZ = startZ; currZ < endZ; currZ++) {
                            if (currZ - camZ >= -maxDistance && currZ - camZ <= maxDistance) {
                                int i_20 = startX + this.groundDx.width * currZ;

                                for (int currX = startX; currX < endX; currX++) {
                                    if (currX - camX >= -maxDistance && currX - camX <= maxDistance && visibleTiles[currX - camX + maxDistance][maxDistance + (currZ - camZ)]) {
                                        short[] shorts_22 = this.groundDx.tileVertices[i_20];
                                        if (shorts_22 != null) {
                                            for (int i_23 = 0; i_23 < shorts_22.length; i_23++) {
                                                bytebuffer9.putShort(shorts_22[i_23]);
                                                ++i_16;
                                            }
                                        }
                                    }

                                    ++i_20;
                                }
                            }
                        }
                    }

                    ints_24[i_10] = i_16;
                    ++i_10;
                }
            }

            if (bytebuffer9.position() != 0) {
            	int avar12 = bytebuffer9.position();
                DirectXBuffer interface3212 = this.toolkit.method13911(avar12 / 2);
                interface3212.upload(0, avar12, this.toolkit.directBufferAddress);
                int i_13 = 0;
                i_10 = 0;

                for (int z = 0; z < this.sizeY; z++) {
                    for (int x = 0; x < this.sizeX; x++) {
                        if (ints_24[i_10] != 0) {
                            shader.textureCoordinateMatrix.method6525(invTileUnits, invTileUnits, 1.0F, 1.0F);
                            shader.textureCoordinateMatrix.faceList[12] = (-x);
                            shader.textureCoordinateMatrix.faceList[13] = (-z);
                            this.tiles[x][z].method1290(shader, interface3212, i_13, ints_24[i_10] / 3);
                            i_13 += ints_24[i_10];
                        }

                        ++i_10;
                    }
                }
            }
        }

    }

    public void remove(Shadow shadow, int x, int z) {
        ShadowDX shadowDx = (ShadowDX) shadow;
        x += shadowDx.minX + 1;
        z += shadowDx.minZ + 1;
        int i_5 = x + z * this.width;
        int i_6 = 0;
        int shadowSizeZ = shadowDx.sizeZ;
        int shadowSizeX = shadowDx.sizeX;
        int i_9 = this.width - shadowSizeX;
        int i_10 = 0;
        int i_11;
        if (z <= 0) {
            i_11 = 1 - z;
            shadowSizeZ -= i_11;
            i_6 += i_11 * shadowSizeX;
            i_5 += i_11 * this.width;
            z = 1;
        }

        if (z + shadowSizeZ >= this.length) {
            i_11 = z + shadowSizeZ + 1 - this.length;
            shadowSizeZ -= i_11;
        }

        if (x <= 0) {
            i_11 = 1 - x;
            shadowSizeX -= i_11;
            i_6 += i_11;
            i_5 += i_11;
            i_10 += i_11;
            i_9 += i_11;
            x = 1;
        }

        if (shadowSizeX + x >= this.width) {
            i_11 = shadowSizeX + x + 1 - this.width;
            shadowSizeX -= i_11;
            i_10 += i_11;
            i_9 += i_11;
        }

        if (shadowSizeX > 0 && shadowSizeZ > 0) {
            method1330(this.shadowMapperData, shadowDx.shadowRaster, i_6, i_5, shadowSizeX, shadowSizeZ, i_9, i_10);
            requestZoneUpdate(x, z, shadowSizeX, shadowSizeZ);
        }

    }

    public boolean hasShadow(Shadow shadow, int x, int z) {
        ShadowDX shadowDx = (ShadowDX) shadow;
        x += shadowDx.minX + 1;
        z += shadowDx.minZ + 1;
        int i_5 = x + z * this.width;
        int i_6 = shadowDx.sizeZ;
        int i_7 = shadowDx.sizeX;
        int i_8 = this.width - i_7;
        int i_9;
        if (z <= 0) {
            i_9 = 1 - z;
            i_6 -= i_9;
            i_5 += i_9 * this.width;
            z = 1;
        }

        if (z + i_6 >= this.length) {
            i_9 = z + i_6 + 1 - this.length;
            i_6 -= i_9;
        }

        if (x <= 0) {
            i_9 = 1 - x;
            i_7 -= i_9;
            i_5 += i_9;
            i_8 += i_9;
            x = 1;
        }

        if (i_7 + x >= this.width) {
            i_9 = i_7 + x + 1 - this.width;
            i_7 -= i_9;
            i_8 += i_9;
        }

        if (i_7 > 0 && i_6 > 0) {
            byte b_10 = 8;
            i_8 += (b_10 - 1) * this.width;
            return method1328(this.shadowMapperData, i_5, i_7, i_6, i_8, b_10);
        }
        return false;
    }

    public void add(Shadow shadow, int x, int y) {
        ShadowDX shadowDx = (ShadowDX) shadow;
        x += shadowDx.minX + 1;
        y += shadowDx.minZ + 1;
        int i_5 = x + y * this.width;
        int i_6 = 0;
        int shadowSizeZ = shadowDx.sizeZ;
        int shadowSizeX = shadowDx.sizeX;
        int i_9 = this.width - shadowSizeX;
        int i_10 = 0;
        int i_11;
        if (y <= 0) {
            i_11 = 1 - y;
            shadowSizeZ -= i_11;
            i_6 += i_11 * shadowSizeX;
            i_5 += i_11 * this.width;
            y = 1;
        }

        if (y + shadowSizeZ >= this.length) {
            i_11 = y + shadowSizeZ + 1 - this.length;
            shadowSizeZ -= i_11;
        }

        if (x <= 0) {
            i_11 = 1 - x;
            shadowSizeX -= i_11;
            i_6 += i_11;
            i_5 += i_11;
            i_10 += i_11;
            i_9 += i_11;
            x = 1;
        }

        if (shadowSizeX + x >= this.width) {
            i_11 = shadowSizeX + x + 1 - this.width;
            shadowSizeX -= i_11;
            i_10 += i_11;
            i_9 += i_11;
        }

        if (shadowSizeX > 0 && shadowSizeZ > 0) {
            method1318(this.shadowMapperData, shadowDx.shadowRaster, i_6, i_5, shadowSizeX, shadowSizeZ, i_9, i_10);
            requestZoneUpdate(x, y, shadowSizeX, shadowSizeZ);
        }

    }

    public void requestZoneUpdate(int startAbsX, int startAbsZ, int sizeX, int sizeZ) {
        if (this.tiles != null) {
            int startX = startAbsX - 1 >> 7;
            int endX = sizeX + (startAbsX - 1) - 1 >> 7;
            int startZ = startAbsZ - 1 >> 7;
            int endZ = startAbsZ - 1 + sizeZ - 1 >> 7;

            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {
                    if (this.tiles[x][z] != null) {
                    	this.tiles[x][z].needsUpdate = true;
                    }
                }
            }
        }
    }
    
    public static void method1318(byte[] shadowMapperData, byte[] bytes_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        int i_8 = -(i_4 >> 2);
        i_4 = -(i_4 & 0x3);

        for (int i_9 = -i_5; i_9 < 0; i_9++) {
            for (int i_10 = i_8; i_10 < 0; i_10++) {
                shadowMapperData[++i_3] += bytes_1[i_2++];
                shadowMapperData[++i_3] += bytes_1[i_2++];
                shadowMapperData[++i_3] += bytes_1[i_2++];
                shadowMapperData[++i_3] += bytes_1[i_2++];
            }

            for (int i_10 = i_4; i_10 < 0; i_10++) {
                shadowMapperData[++i_3] += bytes_1[i_2++];
            }

            i_3 += i_6;
            i_2 += i_7;
        }

    }

    public static boolean method1328(byte[] shadowMapperData, int i_1, int i_2, int i_3, int i_4, int i_5) {
        int i_6 = i_2 % i_5;
        int i_7;
        if (i_6 != 0) {
            i_7 = i_5 - i_6;
        } else {
            i_7 = 0;
        }

        int i_8 = -((i_3 + i_5 - 1) / i_5);
        int i_9 = -((i_5 + i_2 - 1) / i_5);

        for (int i_10 = i_8; i_10 < 0; i_10++) {
            for (int i_11 = i_9; i_11 < 0; i_11++) {
                if (shadowMapperData[i_1] == 0) {
                    return true;
                }

                i_1 += i_5;
            }

            i_1 -= i_7;
            if (shadowMapperData[i_1 - 1] == 0) {
                return true;
            }

            i_1 += i_4;
        }

        return false;
    }

    public static void method1330(byte[] shadowMapperData, byte[] bytes_1, int src, int dst, int i_4, int i_5, int dstOffsetStep, int srcOffsetStep) {
        int i_8 = -(i_4 >> 2);
        i_4 = -(i_4 & 0x3);

        for (int i_9 = -i_5; i_9 < 0; i_9++) {
            for (int i_10 = i_8; i_10 < 0; i_10++) {
                shadowMapperData[++dst] -= bytes_1[src++];
                shadowMapperData[++dst] -= bytes_1[src++];
                shadowMapperData[++dst] -= bytes_1[src++];
                shadowMapperData[++dst] -= bytes_1[src++];
            }

            for (int i_10 = i_4; i_10 < 0; i_10++) {
                shadowMapperData[++dst] -= bytes_1[src++];
            }

            dst += dstOffsetStep;
            src += srcOffsetStep;
        }

    }

}
