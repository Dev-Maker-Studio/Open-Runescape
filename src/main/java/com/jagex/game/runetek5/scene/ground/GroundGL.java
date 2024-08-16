package com.jagex.game.runetek5.scene.ground;

import com.jagex.game.runetek5.cache.node.HashTable;
import static com.jagex.game.runetek5.scene.ground.GroundFunctionMask.*;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ShortNode;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.graphics.light.LightGL;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.graphics.shadow.ShadowGL;
import com.jagex.game.runetek5.graphics.shadow.ShadowMapperGL;
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer;
import com.jagex.game.runetek5.imageproducer.AbstractDisplay;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GoundMaterialGL;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer;
import com.jagex.game.runetek5.utils.ArrayUtils;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.MathUtils;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;

import jaclib.memory.Stream;
import jaclib.memory.heap.NativeHeapBuffer;
import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class GroundGL extends Ground {

    public static final float[] GROUND_MATRIX = new float[16];
    public ShadowMapperGL shadowMapper;
    public int[][][] tileOffsetsZ;
    public int[][][] tileOffsetsX;
    public int[][][] tileColors;
    public int[][][] anIntArrayArrayArray8662;
    public int[][][] tileBlendColors;
    public int[][][] anIntArrayArrayArray8649;
    public GoundMaterialGL[][][] materials;
    public byte[][] shadowing;
    public HashTable materialTable;
    public float[][] lightingZ;
    public float[][] lightingY;
    public float[][] lightingX;
    public Node[] goundMaterialNodes;
    public ArrayBufferGL dataArrayBuffer;
    public OpenGlArrayDrawBuffer vertexBuffer;
    public OpenGlArrayDrawBuffer colorBuffer;
    public OpenGlArrayDrawBuffer textureBuffer;
    public OpenGlArrayDrawBuffer normalsBuffer;
    public int amountOfTexturedFaces;
    public int totalMaterializedTiles;
    public int maxColorCount;
    public int currentColorCount;
    public final LinkedList activeLights = new LinkedList();
    public final OpenGlToolkit activeToolkit;
    public final int quarterTileScale;
    public final int quarterTileUnits;
    public final int floorFunctionMask;
    public final int waterFunctionMask;
    public final short[][] materialIndices;
    public final byte[][] shadowFunctionMasks;

    public GroundGL(OpenGlToolkit toolkit, int floorFunctionMask, int waterFunctionMask, int regionSizeX, int regionSizeY, int[][] heightsCurrentLvl, int[][] heightsNextLvl) {
        super(regionSizeX, regionSizeY, 512, heightsCurrentLvl);
        this.activeToolkit = toolkit;
        this.quarterTileScale = this.tileScale - 2;
        this.quarterTileUnits = 1 << this.quarterTileScale;
        this.floorFunctionMask = floorFunctionMask;
        this.waterFunctionMask = waterFunctionMask;
        this.anIntArrayArrayArray8649 = new int[regionSizeX][regionSizeY][];
        this.materials = new GoundMaterialGL[regionSizeX][regionSizeY][];
        this.tileOffsetsX = new int[regionSizeX][regionSizeY][];
        this.tileOffsetsZ = new int[regionSizeX][regionSizeY][];
        this.tileColors = new int[regionSizeX][regionSizeY][];
        this.tileBlendColors = new int[regionSizeX][regionSizeY][];
        this.materialIndices = new short[regionSizeY * regionSizeX][];
        this.shadowFunctionMasks = new byte[regionSizeX][regionSizeY];
        this.shadowing = new byte[regionSizeX + 1][regionSizeY + 1];
        this.lightingX = new float[this.width + 1][this.length + 1];
        this.lightingY = new float[this.width + 1][this.length + 1];
        this.lightingZ = new float[this.width + 1][this.length + 1];
        for (int tileY = 1; tileY < this.length; tileY++) {
            for (int tileX = 1; tileX < this.width; tileX++) {
                int heightDeltaHorizontal = heightsNextLvl[tileX + 1][tileY] - heightsNextLvl[tileX - 1][tileY];
                int hiehgtDeltaVertical = heightsNextLvl[tileX][tileY + 1] - heightsNextLvl[tileX][tileY - 1];
                float intensity = (float) (1.0D / Math.sqrt(512 * 512 * 4 + heightDeltaHorizontal * heightDeltaHorizontal + hiehgtDeltaVertical * hiehgtDeltaVertical));
                this.lightingX[tileX][tileY] = heightDeltaHorizontal * intensity;
                this.lightingY[tileX][tileY] = (-512 * 2) * intensity;
                this.lightingZ[tileX][tileY] = hiehgtDeltaVertical * intensity;
            }
        }
        this.materialTable = new HashTable(128);
        if ((this.waterFunctionMask & HD_SHADOWS) != 0) {
        	this.shadowMapper = new ShadowMapperGL(this.activeToolkit, this);
        }
    }

    public void method13879(ShadowGL shadowGl, int offsetsXCount, int offsetsYCount) {
        int[] offsetsX = this.tileOffsetsX[offsetsXCount][offsetsYCount];
        int[] offsetsZ = this.tileOffsetsZ[offsetsXCount][offsetsYCount];
        int totalOffsets = offsetsX.length;
        if (this.activeToolkit.projectedVerticesX.length < totalOffsets) {
            this.activeToolkit.projectedVerticesX = new int[totalOffsets];
            this.activeToolkit.projectedVerticesY = new int[totalOffsets];
        }
        int[] sceneShadowOffX = this.activeToolkit.projectedVerticesX;
        int[] sceneShadowOffZ = this.activeToolkit.projectedVerticesY;
        for (int index = 0; index < totalOffsets; index++) {
            sceneShadowOffX[index] = offsetsX[index] >> this.activeToolkit.shadowScale;
            sceneShadowOffZ[index] = offsetsZ[index] >> this.activeToolkit.shadowScale;
        }
        int count = 0;
        while (count < totalOffsets) {
            int xA = sceneShadowOffX[count];
            int zA = sceneShadowOffZ[count++];
            int xB = sceneShadowOffX[count];
            int zB = sceneShadowOffZ[count++];
            int xC = sceneShadowOffX[count];
            int zC = sceneShadowOffZ[count++];
            if ((xA - xB) * (zB - zC) - (zB - zA) * (xC - xB) > 0) {
                shadowGl.markShadedTriangle(zA, zB, zC, xA, xB, xC);
            }
        }
    }

    @Override
    public void setTileShadow(int x, int y, int shadow) {
        if ((this.shadowing[x][y] & 0xff) < shadow) {
            this.shadowing[x][y] = (byte) shadow;
        }
    }

    @Override
    public void calculateBlockTiles(int i_1, int i_2, int[] ints_3, int[] ints_4, int[] ints_5, int[] ints_6, int[] ints_7, int[] ints_8, int[] ints_9, int[] ints_10, int[] ints_11, int[] ints_12, int[] ints_13, WaterTile class9014) {
        int i_16 = ints_10.length;
        int[] ints_17 = new int[i_16 * 3];
        int[] ints_18 = new int[i_16 * 3];
        int[] ints_19 = new int[i_16 * 3];
        int[] ints_20 = ints_11 != null ? new int[i_16 * 3] : null;
        int[] ints_21 = new int[i_16 * 3];
        int[] ints_22 = new int[i_16 * 3];
        int[] ints_23 = ints_4 != null ? new int[i_16 * 3] : null;
        int[] ints_24 = ints_6 != null ? new int[i_16 * 3] : null;
        int i_25 = 0;

        for (int i_26 = 0; i_26 < i_16; i_26++) {
            int i_27 = ints_7[i_26];
            int i_28 = ints_8[i_26];
            int i_29 = ints_9[i_26];
            ints_17[i_25] = ints_3[i_27];
            ints_18[i_25] = ints_5[i_27];
            ints_19[i_25] = ints_10[i_26];
            ints_21[i_25] = ints_12[i_26];
            ints_22[i_25] = ints_13[i_26];
            if (ints_11 != null) {
                ints_20[i_25] = ints_11[i_26];
            }

            if (ints_4 != null) {
                ints_23[i_25] = ints_4[i_27];
            }

            if (ints_6 != null) {
                ints_24[i_25] = ints_6[i_27];
            }

            ++i_25;
            ints_17[i_25] = ints_3[i_28];
            ints_18[i_25] = ints_5[i_28];
            ints_19[i_25] = ints_10[i_26];
            ints_21[i_25] = ints_12[i_26];
            ints_22[i_25] = ints_13[i_26];
            if (ints_11 != null) {
                ints_20[i_25] = ints_11[i_26];
            }

            if (ints_4 != null) {
                ints_23[i_25] = ints_4[i_28];
            }

            if (ints_6 != null) {
                ints_24[i_25] = ints_6[i_28];
            }

            ++i_25;
            ints_17[i_25] = ints_3[i_29];
            ints_18[i_25] = ints_5[i_29];
            ints_19[i_25] = ints_10[i_26];
            ints_21[i_25] = ints_12[i_26];
            ints_22[i_25] = ints_13[i_26];
            if (ints_11 != null) {
                ints_20[i_25] = ints_11[i_26];
            }

            if (ints_4 != null) {
                ints_23[i_25] = ints_4[i_29];
            }

            if (ints_6 != null) {
                ints_24[i_25] = ints_6[i_29];
            }

            ++i_25;
        }

        addBlendedTile(i_1, i_2, ints_17, ints_23, ints_18, ints_24, ints_19, ints_20, ints_21, ints_22, class9014, false);
    }

    public void method13881(int i_1, int i_2, int i_3, boolean[][] bools_4, boolean bool5) {
        if (this.goundMaterialNodes != null) {
            int i_7 = i_3 + i_3 + 1;
            i_7 *= i_7;
            if (this.activeToolkit.anIntArray8502.length < i_7) {
                this.activeToolkit.anIntArray8502 = new int[i_7];
            }

            if (this.activeToolkit.textureBuffer.buffer.length < this.amountOfTexturedFaces * 2) {
                this.activeToolkit.textureBuffer = new GlBitBuffer(this.amountOfTexturedFaces * 2);
            }

            int i_8 = i_1 - i_3;
            int i_9 = i_8;
            if (i_8 < 0) {
                i_8 = 0;
            }

            int i_10 = i_2 - i_3;
            int i_11 = i_10;
            if (i_10 < 0) {
                i_10 = 0;
            }

            int i_12 = i_3 + i_1;
            if (i_12 > super.width - 1) {
                i_12 = super.width - 1;
            }

            int i_13 = i_3 + i_2;
            if (i_13 > super.length - 1) {
                i_13 = super.length - 1;
            }

            int i_14 = 0;
            int[] ints_15 = this.activeToolkit.anIntArray8502;

            for (int i_16 = i_8; i_16 <= i_12; i_16++) {
                boolean[] bools_20 = bools_4[i_16 - i_9];

                for (int i_21 = i_10; i_21 <= i_13; i_21++) {
                    if (bools_20[i_21 - i_11]) {
                        ints_15[i_14++] = i_16 + i_21 * super.width;
                    }
                }
            }

            this.activeToolkit.setupMatrixRendering();
            this.activeToolkit.setLightingEnabled((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) != 0);

            for (int i_16 = 0; i_16 < this.goundMaterialNodes.length; i_16++) {
                ((GoundMaterialGL) this.goundMaterialNodes[i_16]).method12085(ints_15, i_14);
            }

            if (!this.activeLights.isEmpty()) {
                int fogColor = this.activeToolkit.fogColor;
                int fogDensity = this.activeToolkit.fogDensity;
                this.activeToolkit.calculateFog(0, fogDensity, this.activeToolkit.anInt8378);
                this.activeToolkit.setLightingEnabled(false);
                this.activeToolkit.setDepthMask(false);
                this.activeToolkit.setBlendMode(128);
                this.activeToolkit.renderTexture(-2);
                this.activeToolkit.setActiveTexture(this.activeToolkit.texture2d);
                this.activeToolkit.loadTextureCombineMode(GL_MODULATE, GL_REPLACE);
                this.activeToolkit.setTexture(0, GL_CONSTANT, 770);
                this.activeToolkit.setTextureTransparency(0, 34167);

                for (Node light = this.activeLights.getTail(); light != null; light = this.activeLights.getPrevious()) {
                    LightGL glLight = (LightGL) light;
                    glLight.method12311(i_1, i_2, i_3, bools_4);
                }

                this.activeToolkit.setTexture(0, GL_TEXTURE, 768);
                this.activeToolkit.setTextureTransparency(0, GL_TEXTURE);
                this.activeToolkit.setActiveTexture(null);
                this.activeToolkit.calculateFog(fogColor, fogDensity, this.activeToolkit.anInt8378);
            }

            if (this.shadowMapper != null) {
                OpenGL.glPushMatrix();
                OpenGL.glTranslatef(0.0F, -1.0f, 0.0F);
                this.activeToolkit.toggleDrawingClientstates(this.vertexBuffer, null, null, this.textureBuffer);
                this.shadowMapper.method2290(i_1, i_2, i_3, bools_4, bool5);
                OpenGL.glPopMatrix();
            }
        }

    }

    @Override
    public void resetLight() {
        if (this.currentColorCount > 0) {
            byte[][] intensity = new byte[super.width + 1][super.length + 1];
            for (int x = 1; x < super.width; x++) {
                for (int y = 1; y < super.length; y++) {
                    intensity[x][y] = (byte) (
                    		(this.shadowing[x][y - 1] >> 2) +	// south of center
                    		(this.shadowing[x][y] >> 1) + 		// center
                    		(this.shadowing[x - 1][y] >> 2) + 	// west of center
                    		(this.shadowing[x][y + 1] >> 3) + 	// north of center
                    		(this.shadowing[x + 1][y] >> 3));	// east of center
                }
            }
            this.goundMaterialNodes = new Node[this.materialTable.nodeCount()];
            this.materialTable.values(this.goundMaterialNodes);
            for (int nodeIndex = 0; nodeIndex < this.goundMaterialNodes.length; nodeIndex++) {
                ((GoundMaterialGL) this.goundMaterialNodes[nodeIndex]).generateNativeBuffer(this.currentColorCount);
            }
            int bufferEntrySize = 24;
            if (this.anIntArrayArrayArray8662 != null) {
                bufferEntrySize += 4;
            }
            if ((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) != 0) {
                bufferEntrySize += 12;
            }
            NativeHeapBuffer nativeHeapBuffer = this.activeToolkit.nativeHeap.createBuffer(bufferEntrySize * this.currentColorCount, false);
            Stream nativeStream = new Stream(nativeHeapBuffer);
            GoundMaterialGL[] materialsByVertice = new GoundMaterialGL[this.currentColorCount];
            int furthestBitValue = MathUtils.furtestBitValue(this.currentColorCount / 4);
            if (furthestBitValue < 1) {
                furthestBitValue = 1;
            }

            HashTable hashTable = new HashTable(furthestBitValue);
            GoundMaterialGL[] someMaterialArray = new GoundMaterialGL[this.maxColorCount];
            for (int x = 0; x < super.width; x++) {
                for (int y = 0; y < super.length; y++) {
                    if (this.tileColors[x][y] != null) {
                        GoundMaterialGL[] arr_65 = this.materials[x][y];
                        int[] offsetsX = this.tileOffsetsX[x][y];
                        int[] offsetsZ = this.tileOffsetsZ[x][y];
                        int[] blendingColors = this.tileBlendColors[x][y];
                        int[] mainColors = this.tileColors[x][y];
                        int[] floorOffsets = this.anIntArrayArrayArray8649 != null ? this.anIntArrayArrayArray8649[x][y] : null;
                        int[] ints_17 = this.anIntArrayArrayArray8662 != null ? this.anIntArrayArrayArray8662[x][y] : null;
                        if (blendingColors == null) {
                            blendingColors = mainColors;
                        }
                        float lightingXSouthWest = this.lightingX[x][y];
                        float lightingYSouthWest = this.lightingY[x][y];
                        float lightingZSouthWest = this.lightingZ[x][y];
                        float lightingXNorthWest = this.lightingX[x][y + 1];
                        float lightingYNorthWest = this.lightingY[x][y + 1];
                        float lightingZNorthWest = this.lightingZ[x][y + 1];
                        float lightingXNorthEast = this.lightingX[x + 1][y + 1];
                        float lightingYNorthEast = this.lightingY[x + 1][y + 1];
                        float lightingZNorthEast = this.lightingZ[x + 1][y + 1];
                        float lightingXSouthEast = this.lightingX[x + 1][y];
                        float lightingYSouthEast = this.lightingY[x + 1][y];
                        float lightingZSouthEast = this.lightingZ[x + 1][y];
                        int lightStrengthSouthWest = intensity[x][y] & 0xff;
                        int lightStrengthNorthWest = intensity[x][y + 1] & 0xff;
                        int lightStrengthNorthEast = intensity[x + 1][y + 1] & 0xff;
                        int lightStrengthSouthEast = intensity[x + 1][y] & 0xff;
                        int i_34 = 0;

                        label439:
                        for (int colorIndex = 0; colorIndex < mainColors.length; colorIndex++) {
                            GoundMaterialGL currentMaterialNode = arr_65[colorIndex];
                            for (int index = 0; index < i_34; index++) {
                                if (currentMaterialNode == someMaterialArray[index]) {
                                    continue label439;
                                }
                            }
                            someMaterialArray[i_34++] = currentMaterialNode;
                        }

                        short[] shorts_74 = this.materialIndices[x + y * super.width] = new short[mainColors.length];

                        for (int loop = 0; loop < mainColors.length; loop++) {
                            int sceneX = (x << super.tileScale) + offsetsX[loop];
                            int sceneY = (y << super.tileScale) + offsetsZ[loop];
                            int downscaledX = sceneX >> this.quarterTileScale;
                            int downscaledY = sceneY >> this.quarterTileScale;
                            int mainColor = mainColors[loop];
                            int blendColor = blendingColors[loop];
                            int floorHeightOffset = floorOffsets != null ? floorOffsets[loop] : 0;
                            long uid = (long) blendColor << 48 | (long) mainColor << 32 | (downscaledX << 16) | downscaledY;
                            int offsetX = offsetsX[loop];
                            int offsetZ = offsetsZ[loop];
                            byte baseStrength = 74;
                            int directColor = 0;
                            float f_50 = 1.0F;
                            float lightX;
                            float lightY;
                            float lightZ;
                            int lightStrength;
                            if (offsetX == 0 && offsetZ == 0) {
                                lightX = lightingXSouthWest;
                                lightY = lightingYSouthWest;
                                lightZ = lightingZSouthWest;
                                lightStrength = baseStrength - lightStrengthSouthWest;
                            } else if (offsetX == 0 && offsetZ == super.tileUnits) {
                                lightX = lightingXNorthWest;
                                lightY = lightingYNorthWest;
                                lightZ = lightingZNorthWest;
                                lightStrength = baseStrength - lightStrengthNorthWest;
                            } else if (offsetX == super.tileUnits && offsetZ == super.tileUnits) {
                                lightX = lightingXNorthEast;
                                lightY = lightingYNorthEast;
                                lightZ = lightingZNorthEast;
                                lightStrength = baseStrength - lightStrengthNorthEast;
                            } else if (offsetX == super.tileUnits && offsetZ == 0) {
                                lightX = lightingXSouthEast;
                                lightY = lightingYSouthEast;
                                lightZ = lightingZSouthEast;
                                lightStrength = baseStrength - lightStrengthSouthEast;
                            } else {
                                float partualOffsetX = (float) offsetX / super.tileUnits;
                                float partualOffsetZ = (float) offsetZ / super.tileUnits;
                                float lightDeltaXSouth = lightingXSouthWest + (lightingXSouthEast - lightingXSouthWest) * partualOffsetX;
                                float lightDeltaYSouth = lightingYSouthWest + (lightingYSouthEast - lightingYSouthWest) * partualOffsetX;
                                float lightDeltaZSouth = lightingZSouthWest + (lightingZSouthEast - lightingZSouthWest) * partualOffsetX;
                                float lightDeltaXNorth = lightingXNorthWest + (lightingXNorthEast - lightingXNorthWest) * partualOffsetX;
                                float lightDeltaYNorth = lightingYNorthWest + (lightingYNorthEast - lightingYNorthWest) * partualOffsetX;
                                float lightDeltaZNorth = lightingZNorthWest + (lightingZNorthEast - lightingZNorthWest) * partualOffsetX;
                                lightX = lightDeltaXSouth + (lightDeltaXNorth - lightDeltaXSouth) * partualOffsetZ;
                                lightY = lightDeltaYSouth + (lightDeltaYNorth - lightDeltaYSouth) * partualOffsetZ;
                                lightZ = lightDeltaZSouth + (lightDeltaZNorth - lightDeltaZSouth) * partualOffsetZ;
                                int mainLight = lightStrengthSouthWest + (offsetX * (lightStrengthSouthEast - lightStrengthSouthWest) >> super.tileScale);
                                int adjustmentLight = lightStrengthNorthWest + (offsetX * (lightStrengthNorthEast - lightStrengthNorthWest) >> super.tileScale);
                                lightStrength = baseStrength - (mainLight + (offsetZ * (adjustmentLight - mainLight) >> super.tileScale));
                            }

                            if (mainColor != -1) {
                                int lightness = lightStrength * (mainColor & 0x7f) >> 7;
                                if (lightness < 2) {
                                    lightness = 2;
                                } else if (lightness > 126) {
                                    lightness = 126;
                                }

                                directColor = ColorUtil.hslToRgb[mainColor & 0xff80 | lightness];
                                if ((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) == 0) {
                                    f_50 = this.activeToolkit.sunDirection[0] * lightX + this.activeToolkit.sunDirection[1] * lightY + this.activeToolkit.sunDirection[2] * lightZ;
                                    f_50 = this.activeToolkit.sunBrightness + f_50 * (f_50 > 0.0F ? this.activeToolkit.aFloat8433 : this.activeToolkit.aFloat8434);
                                }
                            }

                            Node blendedTile = null;
                            if ((sceneX & this.quarterTileUnits - 1) == 0 && (sceneY & this.quarterTileUnits - 1) == 0) {
                                blendedTile = hashTable.get(uid);
                            }

                            int i_84;
                            int colorRgb;
                            if (blendedTile == null) {
                                if (blendColor != mainColor) {
                                    int lightness = lightStrength * (blendColor & 0x7f) >> 7;
                                    if (lightness < 2) {
                                        lightness = 2;
                                    } else if (lightness > 126) {
                                        lightness = 126;
                                    }

                                    colorRgb = ColorUtil.hslToRgb[blendColor & 0xff80 | lightness];
                                    if ((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) == 0) {
                                        float f_10000 = this.activeToolkit.sunDirection[0] * lightX + this.activeToolkit.sunDirection[1] * lightY + this.activeToolkit.sunDirection[2] * lightZ;
                                        float lightStrengthMultiplier = this.activeToolkit.sunBrightness + f_50 * (f_50 > 0.0F ? this.activeToolkit.aFloat8433 : this.activeToolkit.aFloat8434);
                                        int red = colorRgb >> 16 & 0xff;
                                        int green = colorRgb >> 8 & 0xff;
                                        int blue = colorRgb & 0xff;
                                        red = (int) (red * lightStrengthMultiplier);
                                        if (red < 0) {
                                            red = 0;
                                        } else if (red > 255) {
                                            red = 255;
                                        }
                                        green = (int) (green * lightStrengthMultiplier);
                                        if (green < 0) {
                                            green = 0;
                                        } else if (green > 255) {
                                            green = 255;
                                        }
                                        blue = (int) (blue * lightStrengthMultiplier);
                                        if (blue < 0) {
                                            blue = 0;
                                        } else if (blue > 255) {
                                            blue = 255;
                                        }
                                        colorRgb = red << 16 | green << 8 | blue;
                                    }
                                } else {
                                    colorRgb = directColor;
                                }
                                if (this.activeToolkit.bigEndianess) {
                                    nativeStream.putFloatBigEndian(sceneX);
                                    nativeStream.putFloatBigEndian((getAverageHeight(sceneX, sceneY) + floorHeightOffset));
                                    nativeStream.putFloatBigEndian(sceneY);
                                    nativeStream.putByte((byte) (colorRgb >> 16));
                                    nativeStream.putByte((byte) (colorRgb >> 8));
                                    nativeStream.putByte((byte) colorRgb);
                                    nativeStream.putByte(-1);
                                    nativeStream.putFloatBigEndian(sceneX);
                                    nativeStream.putFloatBigEndian(sceneY);
                                    if (this.anIntArrayArrayArray8662 != null) {
                                        nativeStream.putFloatBigEndian(ints_17 != null ? (ints_17[loop] - 1) : 0.0F);
                                    }
                                    if ((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) != 0) {
                                        nativeStream.putFloatBigEndian(lightX);
                                        nativeStream.putFloatBigEndian(lightY);
                                        nativeStream.putFloatBigEndian(lightZ);
                                    }
                                } else {
                                    nativeStream.putFloatLittleEndian(sceneX);
                                    nativeStream.putFloatLittleEndian((getAverageHeight(sceneX, sceneY) + floorHeightOffset));
                                    nativeStream.putFloatLittleEndian(sceneY);
                                    nativeStream.putByte((byte) (colorRgb >> 16));
                                    nativeStream.putByte((byte) (colorRgb >> 8));
                                    nativeStream.putByte((byte) colorRgb);
                                    nativeStream.putByte(-1);
                                    nativeStream.putFloatLittleEndian(sceneX);
                                    nativeStream.putFloatLittleEndian(sceneY);
                                    if (this.anIntArrayArrayArray8662 != null) {
                                        nativeStream.putFloatLittleEndian(ints_17 != null ? (ints_17[loop] - 1) : 0.0F);
                                    }
                                    if ((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) != 0) {
                                        nativeStream.putFloatLittleEndian(lightX);
                                        nativeStream.putFloatLittleEndian(lightY);
                                        nativeStream.putFloatLittleEndian(lightZ);
                                    }
                                }
                                i_84 = this.totalMaterializedTiles++;
                                shorts_74[loop] = (short) i_84;
                                if (mainColor != -1) {
                                    materialsByVertice[i_84] = arr_65[loop];
                                }
                                hashTable.put(new ShortNode(shorts_74[loop]), uid);
                            } else {
                                shorts_74[loop] = ((ShortNode) blendedTile).aShort8067;
                                i_84 = shorts_74[loop] & 0xffff;
                                if (mainColor != -1 && arr_65[loop].pointer < materialsByVertice[i_84].pointer) {
                                    materialsByVertice[i_84] = arr_65[loop];
                                }
                            }

                            for (int materialIndex = 0; materialIndex < i_34; materialIndex++) {
                                someMaterialArray[materialIndex].method12083(i_84, directColor, lightStrength, f_50);
                            }

                            this.amountOfTexturedFaces++;
                        }
                    }
                }
            }

            for (int i_9 = 0; i_9 < this.totalMaterializedTiles; i_9++) {
                GoundMaterialGL class282Sub310 = materialsByVertice[i_9];
                if (class282Sub310 != null) {
                    class282Sub310.appendToNativeStream(i_9);
                }
            }

            for (int x = 0; x < super.width; x++) {
                for (int y = 0; y < super.length; y++) {
                    short[] vertices = this.materialIndices[x + y * super.width];
                    if (vertices != null) {
                        int faceCount = 0;
                        for (int verticeCount = 0; verticeCount < vertices.length; faceCount++) {
                            int faceA = vertices[verticeCount++] & 0xffff;
                            int faceB = vertices[verticeCount++] & 0xffff;
                            int faceC = vertices[verticeCount++] & 0xffff;
                            GoundMaterialGL texturedFaceA = materialsByVertice[faceA];
                            GoundMaterialGL texturedFaceB = materialsByVertice[faceB];
                            GoundMaterialGL texturedFaceC = materialsByVertice[faceC];
                            GoundMaterialGL texturedFaceD = null;
                            if (texturedFaceA != null) {
                                texturedFaceA.generateFunctionMask(x, y, faceCount);
                                texturedFaceD = texturedFaceA;
                            }
                            if (texturedFaceB != null) {
                                texturedFaceB.generateFunctionMask(x, y, faceCount);
                                if (texturedFaceD == null || texturedFaceB.pointer < texturedFaceD.pointer) {
                                    texturedFaceD = texturedFaceB;
                                }
                            }
                            if (texturedFaceC != null) {
                                texturedFaceC.generateFunctionMask(x, y, faceCount);
                                if (texturedFaceD == null || texturedFaceC.pointer < texturedFaceD.pointer) {
                                    texturedFaceD = texturedFaceC;
                                }
                            }
                            if (texturedFaceD != null) {
                                if (texturedFaceA != null) {
                                    texturedFaceD.appendToNativeStream(faceA);
                                }
                                if (texturedFaceB != null) {
                                    texturedFaceD.appendToNativeStream(faceB);
                                }
                                if (texturedFaceC != null) {
                                    texturedFaceD.appendToNativeStream(faceC);
                                }
                                texturedFaceD.generateFunctionMask(x, y, faceCount);
                            }
                        }
                    }
                }
            }
            nativeStream.method2925();
            this.dataArrayBuffer = this.activeToolkit.createDataArrayBuffer(bufferEntrySize, nativeHeapBuffer, nativeStream.written());
            if (this.dataArrayBuffer instanceof GlVboArrayBuffer) {
                nativeHeapBuffer.deallocateBuffer();
            }
            this.vertexBuffer = new OpenGlArrayDrawBuffer(this.dataArrayBuffer, GL_FLOAT, 3, 0);
            this.colorBuffer = new OpenGlArrayDrawBuffer(this.dataArrayBuffer, GL_UNSIGNED_BYTE, 4, 12);
            byte pointerOffset;
            if (this.anIntArrayArrayArray8662 != null) {
                this.textureBuffer = new OpenGlArrayDrawBuffer(this.dataArrayBuffer, GL_FLOAT, 3, 16);
                pointerOffset = 28;
            } else {
                this.textureBuffer = new OpenGlArrayDrawBuffer(this.dataArrayBuffer, GL_FLOAT, 2, 16);
                pointerOffset = 24;
            }

            if ((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) != 0) {
                this.normalsBuffer = new OpenGlArrayDrawBuffer(this.dataArrayBuffer, GL_FLOAT, 3, pointerOffset);
            }

            long[] pointers = new long[this.goundMaterialNodes.length];

            for (int nodeIndex = 0; nodeIndex < this.goundMaterialNodes.length; nodeIndex++) {
                GoundMaterialGL materialNode = (GoundMaterialGL) this.goundMaterialNodes[nodeIndex];
                pointers[nodeIndex] = materialNode.pointer;
                materialNode.createDataArrayBuffer(this.totalMaterializedTiles);
            }

            ArrayUtils.quicksort(pointers, this.goundMaterialNodes);
            if (this.shadowMapper != null) {
                this.shadowMapper.method2309();
            }
        } else {
            this.shadowMapper = null;
        }

        if ((this.floorFunctionMask & GroundFunctionMask.HIDE) == 0) {
            this.tileOffsetsZ = null;
            this.tileOffsetsX = null;
            this.tileColors = null;
        }

        this.anIntArrayArrayArray8662 = null;
        this.tileBlendColors = null;
        this.anIntArrayArrayArray8649 = null;
        this.materials = null;
        this.shadowing = null;
        this.materialTable = null;
        this.lightingZ = null;
        this.lightingY = null;
        this.lightingX = null;
    }

    @Override
    public void renderMinimapFloor(int i_1, int i_2, int startX, int startY, int endX, int endY, boolean[][] materialized) {
        AbstractDisplay activeDisplay = this.activeToolkit.getDisplay();
        if (this.currentColorCount > 0 && activeDisplay != null) {
            this.activeToolkit.method13586();
            this.activeToolkit.toggleFog(false);
            this.activeToolkit.setLightingEnabled(false);
            this.activeToolkit.toggleZbuffer(false);
            this.activeToolkit.setDepthMask(false);
            this.activeToolkit.setBlendMode(0);
            this.activeToolkit.renderTexture(-2);
            this.activeToolkit.setActiveTexture(null);
            GROUND_MATRIX[0] = 1024 / (128.0F * super.tileUnits * activeDisplay.getWidth());
            GROUND_MATRIX[1] = 0.0F;
            GROUND_MATRIX[2] = 0.0F;
            GROUND_MATRIX[3] = 0.0F;
            GROUND_MATRIX[4] = 0.0F;
            GROUND_MATRIX[5] = 1024 / (128.0F * super.tileUnits * activeDisplay.getHeight());
            GROUND_MATRIX[6] = 0.0F;
            GROUND_MATRIX[7] = 0.0F;
            GROUND_MATRIX[8] = 0.0F;
            GROUND_MATRIX[9] = 0.0F;
            GROUND_MATRIX[10] = 0.0F;
            GROUND_MATRIX[11] = 0.0F;
            GROUND_MATRIX[12] = -1.0f - ((1024 * startX) / 128.0F - (i_1 * 2)) / activeDisplay.getWidth();
            GROUND_MATRIX[13] = 1.0F - ((i_2 * 2) + (1024 * endY) / 128.0F) / activeDisplay.getHeight();
            GROUND_MATRIX[14] = 0.0F;
            GROUND_MATRIX[15] = 1.0F;
            OpenGL.glMatrixMode(5889);
            OpenGL.glLoadMatrixf(GROUND_MATRIX, 0);
            GROUND_MATRIX[0] = 1.0F;
            GROUND_MATRIX[1] = 0.0F;
            GROUND_MATRIX[2] = 0.0F;
            GROUND_MATRIX[3] = 0.0F;
            GROUND_MATRIX[4] = 0.0F;
            GROUND_MATRIX[5] = 0.0F;
            GROUND_MATRIX[6] = 1.0F;
            GROUND_MATRIX[7] = 0.0F;
            GROUND_MATRIX[8] = 0.0F;
            GROUND_MATRIX[9] = 1.0F;
            GROUND_MATRIX[10] = 0.0F;
            GROUND_MATRIX[11] = 0.0F;
            GROUND_MATRIX[12] = 0.0F;
            GROUND_MATRIX[13] = 0.0F;
            GROUND_MATRIX[14] = 0.0F;
            GROUND_MATRIX[15] = 1.0F;
            OpenGL.glMatrixMode(5888);
            OpenGL.glLoadMatrixf(GROUND_MATRIX, 0);
            if ((this.waterFunctionMask & GroundFunctionMask.HD_LIGHT) != 0) {
                this.activeToolkit.setLightingEnabled(true);
                this.activeToolkit.setLightingSourceParams();
            } else {
                this.activeToolkit.setLightingEnabled(false);
            }
            
            this.activeToolkit.toggleDrawingClientstates(this.vertexBuffer, this.normalsBuffer, this.colorBuffer, this.textureBuffer);
            if (this.activeToolkit.textureBuffer.buffer.length < this.amountOfTexturedFaces * 2) {
                this.activeToolkit.textureBuffer = new GlBitBuffer(this.amountOfTexturedFaces * 2);
            } else {
                this.activeToolkit.textureBuffer.currentPosition = 0;
            }
            
            GlBitBuffer textureBuffer = this.activeToolkit.textureBuffer;
            int texturesLoaded = 0;
            int materialIndex;
            short[] materials;
            if (this.activeToolkit.bigEndianess) {
                for (int y = startY; y < endY; y++) {
                    materialIndex = y * super.width + startX;
                    for (int x = startX; x < endX; x++) {
                        if (materialized[x - startX][y - startY]) {
                            materials = this.materialIndices[materialIndex];
                            if (materials != null) {
                                for (int index = 0; index < materials.length; index++) {
                                    textureBuffer.writeShort(materials[index] & 0xffff);
                                    texturesLoaded++;
                                }
                            }
                        }
                        materialIndex++;
                    }
                }
            } else {
                for (int y = startY; y < endY; y++) {
                    materialIndex = y * super.width + startX;
                    for (int x = startX; x < endX; x++) {
                        if (materialized[x - startX][y - startY]) {
                            materials = this.materialIndices[materialIndex];
                            if (materials != null) {
                                for (int index = 0; index < materials.length; index++) {
                                    textureBuffer.writeLEShort(materials[index] & 0xffff);
                                    texturesLoaded++;
                                }
                            }
                        }
                        materialIndex++;
                    }
                }
            }
            if (texturesLoaded > 0) {
                GlElementsArrayBuffer texture2d = new GlElementsArrayBuffer(this.activeToolkit, textureBuffer.buffer, textureBuffer.currentPosition);
                this.activeToolkit.drawElements(texture2d, 0, texturesLoaded);
            }
        }
    }

    @Override
    public void addShadow(Shadow class282Sub50Sub171, int i_2, int i_3, int i_4) {
        if (this.shadowMapper != null && class282Sub50Sub171 != null) {
            int i_7 = i_2 - (i_3 * this.activeToolkit.anInt8438 >> 8) >> this.activeToolkit.shadowScale;
            int i_8 = i_4 - (i_3 * this.activeToolkit.anInt8439 >> 8) >> this.activeToolkit.shadowScale;
            this.shadowMapper.method2314(class282Sub50Sub171, i_7, i_8);
        }
    }

    @Override
    public boolean hasRenderingShadows(Shadow shadow, int i_2, int i_3, int i_4) {
        if (this.shadowMapper != null && shadow != null) {
            int i_7 = i_2 - (i_3 * this.activeToolkit.anInt8438 >> 8) >> this.activeToolkit.shadowScale;
            int i_8 = i_4 - (i_3 * this.activeToolkit.anInt8439 >> 8) >> this.activeToolkit.shadowScale;
            return this.shadowMapper.method2293(shadow, i_7, i_8);
        } 
        return false;
    }

    @Override
    public void removeShadow(Shadow shadow, int i_2, int i_3, int i_4) {
        if (this.shadowMapper != null && shadow != null) {
            int i_7 = i_2 - (i_3 * this.activeToolkit.anInt8438 >> 8) >> this.activeToolkit.shadowScale;
            int i_8 = i_4 - (i_3 * this.activeToolkit.anInt8439 >> 8) >> this.activeToolkit.shadowScale;
            this.shadowMapper.addShadow(shadow, i_7, i_8);
        }

    }

    @Override
    public void addLight(PointLight lightNode, int[] ints_2) {
        this.activeLights.insertTail(new LightGL(this.activeToolkit, this, lightNode, ints_2));
    }
    
    
    @Override
    public void addBlendedTile(int tileX, int tileY, int[] offsetsX, int[] lightGroundOffsetsA, int[] offsetsZ, int[] lightGroundOffsetsB, int[] mainColors, int[] blendColors, int[] waterMaterials, int[] waterScaling, WaterTile waterTile, boolean hasShadows) {
        MaterialProvider materialProvider = this.activeToolkit.materialProvider;
        if (lightGroundOffsetsB != null && this.anIntArrayArrayArray8662 == null) {
            this.anIntArrayArrayArray8662 = new int[super.width][super.length][];
        }
        if (lightGroundOffsetsA != null && this.anIntArrayArrayArray8649 == null) {
            this.anIntArrayArrayArray8649 = new int[super.width][super.length][];
        }
        this.tileOffsetsX[tileX][tileY] = offsetsX;
        this.tileOffsetsZ[tileX][tileY] = offsetsZ;
        this.tileColors[tileX][tileY] = mainColors;
        this.tileBlendColors[tileX][tileY] = blendColors;
        if (this.anIntArrayArrayArray8662 != null) {
            this.anIntArrayArrayArray8662[tileX][tileY] = lightGroundOffsetsB;
        }
        if (this.anIntArrayArrayArray8649 != null) {
            this.anIntArrayArrayArray8649[tileX][tileY] = lightGroundOffsetsA;
        }
        GoundMaterialGL[] groundMaterial = this.materials[tileX][tileY] = new GoundMaterialGL[mainColors.length];
        for (int index = 0; index < mainColors.length; index++) {
            int materialId = waterMaterials[index];
            int scaling = waterScaling[index];
            if ((this.waterFunctionMask & 0x20) != 0 && materialId != -1 && materialProvider.getMaterial(materialId).detailsOnly) {
                scaling = 128;
                materialId = -1;
            }
            long uid = (long) waterTile.intensity << 48 | (long) waterTile.scale << 42 | (long) waterTile.color << 28 | ((long) scaling << 14) | materialId;
            Node activeGroundMaterial;
            for (activeGroundMaterial = this.materialTable.get(uid); activeGroundMaterial != null; activeGroundMaterial = this.materialTable.nextInBucket()) {
                GoundMaterialGL openglGroundMaterial = (GoundMaterialGL) activeGroundMaterial;
                if (materialId == openglGroundMaterial.materialId && openglGroundMaterial.textureScale == scaling && openglGroundMaterial.waterTile.equals(waterTile)) {
                    break;
                }
            }
            if (activeGroundMaterial == null) {
                groundMaterial[index] = new GoundMaterialGL(this, materialId, scaling, waterTile);
                this.materialTable.put(groundMaterial[index], uid);
            } else {
                groundMaterial[index] = (GoundMaterialGL) activeGroundMaterial;
            }
        }
        if (hasShadows) {
            this.shadowFunctionMasks[tileX][tileY] |= GroundFunctionMask.CONTAINS_SHADOW;
        }
        if (mainColors.length > this.maxColorCount) {
            this.maxColorCount = mainColors.length;
        }
        this.currentColorCount += mainColors.length;
    }

    @Override
    public void renderFloor(int i_1, int i_2, int i_3, boolean[][] bools_4, boolean bool5) {
        method13881(i_1, i_2, i_3, bools_4, bool5);
    }


    @Override
    public Shadow createShadowAt(int tileX, int tileY, Shadow shadow) {
        if ((this.shadowFunctionMasks[tileX][tileY] & GroundFunctionMask.CONTAINS_SHADOW) == 0) {
            return null;
        } 
        int dimension = super.tileUnits >> this.activeToolkit.shadowScale;
        ShadowGL openglShadow = (ShadowGL) shadow;
        ShadowGL generatedOpenglShadow;
        if (openglShadow != null && openglShadow.fitsDimension(dimension, dimension)) {
            generatedOpenglShadow = openglShadow;
            openglShadow.reset();
        } else {
            generatedOpenglShadow = new ShadowGL(dimension, dimension);
        }

        generatedOpenglShadow.setDimension(0, 0, dimension, dimension);
        method13879(generatedOpenglShadow, tileX, tileY);
        return generatedOpenglShadow;
    }
}
