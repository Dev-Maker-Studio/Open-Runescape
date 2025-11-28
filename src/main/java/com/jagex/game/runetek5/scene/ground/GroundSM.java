package com.jagex.game.runetek5.scene.ground;

import static com.jagex.game.runetek5.scene.tile.TileFunctionMask.*;

import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.scene.tile.PlainTile;
import com.jagex.game.runetek5.scene.tile.PlainTileBlended;
import com.jagex.game.runetek5.scene.tile.ShapedTile;
import com.jagex.game.runetek5.scene.tile.ShapedTileBlended;
import com.jagex.game.runetek5.scene.tile.TextureBlendedTile;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.toolkit.safemode.RasterizerSM;
import com.jagex.game.runetek5.toolkit.safemode.ThreadResourceSM;
import com.jagex.game.runetek5.utils.ColorUtil;

public class GroundSM extends Ground {

	/**
	 * Tiles that render as squares [MIN]
	 */
    public PlainTile[][] scenePlainTiles;
    public ShapedTile[][] sceneShapedTiles;
    
    /**
     * Tiles that blend colors from overlay / underlay together [LOW / MED]
     */
    public PlainTileBlended[][] plainBlendedTiles;
    public ShapedTileBlended[][] shapedBlendedTiles;
    
    /**
     * Tiles that blend colors and textures from overlay / underlay together [HIGH]
     */
    public TextureBlendedTile[][] blendedTexturedTiles;
    
    /**
     * Color adjustment
     */
    public byte[][] directionalTileLight;
    public byte[][] tileShadowing;
    
    /**
     * Projection matrix
     */
    public float vertexXA;
    public float vertexYA;
    public float vertexZNearA;
    public float vertexZFarA;
    public float vertexXB;
    public float vertexYB;
    public float vertexZNearB;
    public float vertexZFarB;
    public float vertexXC;
    public float vertexYC;
    public float vertexZNearC;
    public float vertexZFarC;
    public float vertexXD;
    public float vertexYD;
    public float vertexZNearD;
    public float vertexZFarD;
    
    /**
     * The rendering toolkit
     */
    public final SafeModeToolkit toolkit;
    
    /**
     * Floor attributes
     */
    public final int groundFunctionMask;

    /**
     * Setup the variables such as light, shadows, floor attributes
     */
    public GroundSM(SafeModeToolkit toolkit, int groundFunctionMask, int regionSizeX, int regionSizeY, int[][] otherTileHeights, int[][] tileHeights) {
        super(regionSizeX, regionSizeY, 512, otherTileHeights);
        this.toolkit = toolkit;
        this.groundFunctionMask = groundFunctionMask;
        this.directionalTileLight = new byte[regionSizeX + 1][regionSizeY + 1];
        int baseLight = this.toolkit.lightIntensity >> 9;
        for (int y = 1; y < regionSizeY; y++) {
            for (int x = 1; x < regionSizeX; x++) {
                int heightDifferenceX = tileHeights[x + 1][y] - tileHeights[x - 1][y];
                int heightDifferenceY = tileHeights[x][y + 1] - tileHeights[x][y - 1];
                int normalLength = (int) Math.sqrt(512 * 512 + heightDifferenceX * heightDifferenceX + heightDifferenceY * heightDifferenceY);
                int normalizedNormalX = (heightDifferenceX << 8) / normalLength;
                int normalizedNormalY = 512 * -512 / normalLength;
                int normalizedNormalZ = (heightDifferenceY << 8) / normalLength;
                int directionalLightIntensity = baseLight + (this.toolkit.lightX * normalizedNormalX + this.toolkit.lightY * normalizedNormalY + this.toolkit.lightZ * normalizedNormalZ >> 17);
                directionalLightIntensity >>= 1;
                if (directionalLightIntensity < 2) {
                    directionalLightIntensity = 2;
                } else if (directionalLightIntensity > 126) {
                    directionalLightIntensity = 126;
                }
                this.directionalTileLight[x][y] = (byte) directionalLightIntensity;
            }
        }
        this.tileShadowing = new byte[regionSizeX + 1][regionSizeY + 1];
    }

    @Override
    public void setTileShadow(int x, int y, int light) {
        if (this.tileShadowing[x][y] < light) {
            this.tileShadowing[x][y] = (byte) light;
        }
    }

    @Override
    public void calculateBlockTiles(int x, int y, int[] offsetsX, int[] heightOffsets, int[] offsetsY, int[] unused, int[] trianglesA, int[] trianglesB, int[] trianglesC, int[] primaryColors, int[] secondaryColors, int[] primaryMaterialIds, int[] secondaryMaterialIds, WaterTile class9014) {
        if (this.scenePlainTiles == null) {
            this.scenePlainTiles = new PlainTile[super.width][super.length];
            this.sceneShapedTiles = new ShapedTile[super.width][super.length];
        } else if (this.plainBlendedTiles != null || this.blendedTexturedTiles != null) {
            throw new IllegalStateException();
        }
        boolean offsetMatchesTileunits = false;
        if (primaryColors.length == 2 && trianglesA.length == 2 && (primaryColors[0] == primaryColors[1] || primaryMaterialIds[0] != -1 && primaryMaterialIds[1] == primaryMaterialIds[0])) {
            offsetMatchesTileunits = true;

            for (int triangleIndex = 1; triangleIndex < 2; triangleIndex++) {
            	int offX = offsetsX[trianglesA[triangleIndex]];
            	int offY = offsetsY[trianglesA[triangleIndex]];
                if (offX != 0 && offX != super.tileUnits || offY != 0 && offY != super.tileUnits) {
                    offsetMatchesTileunits = false;
                    break;
                }
            }
        }
        if (!offsetMatchesTileunits) {
            ShapedTile shapedTile = new ShapedTile();
            short offsetCount = (short) offsetsX.length;
            short colorCount = (short) primaryColors.length;
            shapedTile.count = offsetCount;
            shapedTile.light = new short[offsetCount];
            shapedTile.offsetX = new short[offsetCount];
            shapedTile.offsetZ = new short[offsetCount];
            shapedTile.offsetY = new short[offsetCount];
            for (int index = 0; index < offsetCount; index++) {
                int offsetX = offsetsX[index];
                int offsetY = offsetsY[index];
                if (offsetX == 0 && offsetY == 0) {
                    shapedTile.light[index] = (short) (this.directionalTileLight[x][y] - this.tileShadowing[x][y]);
                } else if (offsetX == 0 && offsetY == super.tileUnits) {
                    shapedTile.light[index] = (short) (this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]);
                } else if (offsetX == super.tileUnits && offsetY == super.tileUnits) {
                    shapedTile.light[index] = (short) (this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]);
                } else if (offsetX == super.tileUnits && offsetY == 0) {
                    shapedTile.light[index] = (short) (this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]);
                } else {
                	int lightA = (this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]) * offsetX + (this.directionalTileLight[x][y] - this.tileShadowing[x][y]) * (super.tileUnits - offsetX);
                	int lightB = (this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]) * (super.tileUnits - offsetX) + (this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]) * offsetX;
                    shapedTile.light[index] = (short) (lightA * (super.tileUnits - offsetY) + lightB * offsetY >> super.tileScale * 2);
                }
                int sceneX = offsetX + (x << super.tileScale);
                int sceneY = offsetY + (y << super.tileScale);
                shapedTile.offsetX[index] = (short) offsetX;
                shapedTile.offsetY[index] = (short) offsetY;
                shapedTile.offsetZ[index] = (short) (getAverageHeight(sceneX, sceneY) + (heightOffsets != null ? heightOffsets[index] : 0));
                if (shapedTile.light[index] < 2) {
                    shapedTile.light[index] = 2;
                }
            }
            boolean texturedFloor = false;
            int triangleCount = 0;
            for (int index = 0; index < colorCount; index++) {
                if (primaryColors[index] >= 0 || secondaryColors != null && secondaryColors[index] >= 0) {
                    triangleCount++;
                }
                int materialId = primaryMaterialIds[index];
                if (materialId != -1) {
                    MaterialType material = this.toolkit.materialProvider.getMaterial(materialId);
                    if (!material.detailsOnly) {
                        texturedFloor = true;
                        if (flagMatch(material.effectId) || material.textureSpeedU != 0 || material.textureSpeedV != 0) {
                            shapedTile.functionMask |= TEXTURE_MOVEMENT;
                        }
                    }
                }
            }
            shapedTile.primaryColors = new int[triangleCount];
            if (secondaryColors != null) {
                shapedTile.secondaryColors = new int[triangleCount];
            }
            shapedTile.triangleA = new short[triangleCount];
            shapedTile.triangleB = new short[triangleCount];
            shapedTile.triangleC = new short[triangleCount];
            if (texturedFloor) {
                shapedTile.primaryMaterialIds = new short[triangleCount];
                shapedTile.secondaryMaterialIds = new short[triangleCount];
            }
            for (int triangleIndex = 0; triangleIndex < colorCount; triangleIndex++) {
                if (primaryColors[triangleIndex] >= 0 || secondaryColors != null && secondaryColors[triangleIndex] >= 0) {
                    if (primaryColors[triangleIndex] >= 0) {
                        shapedTile.primaryColors[shapedTile.triangleCount] = ColorUtil.adjustHsv(primaryColors[triangleIndex]);
                    } else {
                        shapedTile.primaryColors[shapedTile.triangleCount] = -1;
                    }
                    if (secondaryColors != null) {
                        if (secondaryColors[triangleIndex] != -1) {
                            shapedTile.secondaryColors[shapedTile.triangleCount] = ColorUtil.adjustHsv(secondaryColors[triangleIndex]);
                        } else {
                            shapedTile.secondaryColors[shapedTile.triangleCount] = -1;
                        }
                    }
                    shapedTile.triangleA[shapedTile.triangleCount] = (short) trianglesA[triangleIndex];
                    shapedTile.triangleB[shapedTile.triangleCount] = (short) trianglesB[triangleIndex];
                    shapedTile.triangleC[shapedTile.triangleCount] = (short) trianglesC[triangleIndex];
                    if (texturedFloor) {
                        if (primaryMaterialIds[triangleIndex] != -1 && !this.toolkit.materialProvider.getMaterial(primaryMaterialIds[triangleIndex]).detailsOnly) {
                            shapedTile.primaryMaterialIds[shapedTile.triangleCount] = (short) primaryMaterialIds[triangleIndex];
                            shapedTile.secondaryMaterialIds[shapedTile.triangleCount] = (short) secondaryMaterialIds[triangleIndex];
                        } else {
                            shapedTile.primaryMaterialIds[shapedTile.triangleCount] = -1;
                        }
                    }
                    shapedTile.triangleCount++;
                }
            }
            this.sceneShapedTiles[x][y] = shapedTile;
        } else if (primaryColors[0] >= 0 || secondaryColors != null && secondaryColors[0] >= 0) {
            PlainTile plainTile = new PlainTile();
            int color = primaryColors[0];
            int materialId = primaryMaterialIds[0];
            if (secondaryColors != null) {
                plainTile.color = ColorUtil.repackHsl(ColorUtil.adjustHsv(secondaryColors[0]), this.directionalTileLight[x][y] - this.tileShadowing[x][y]);
                if (color == -1) {
                    plainTile.functionMask |= HD_LIGHT;
                }
            }
            if (super.tileHeights[x][y] == super.tileHeights[x + 1][y] && super.tileHeights[x][y] == super.tileHeights[x + 1][y + 1] && super.tileHeights[x][y + 1] == super.tileHeights[x][y]) {
                plainTile.functionMask |= COMPLETELY_FLAT;
            }
            MaterialType material = null;
            if (materialId != -1) {
                material = this.toolkit.materialProvider.getMaterial(materialId);
            }
            if (material != null && (plainTile.functionMask & HD_LIGHT) == 0 && !material.detailsOnly) {
                plainTile.colorSouthWest = (short) (this.directionalTileLight[x][y] - this.tileShadowing[x][y]);
                plainTile.colorSouthEast = (short) (this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]);
                plainTile.colorNorthEast = (short) (this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]);
                plainTile.colorNorthWest = (short) (this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]);
                plainTile.materialId = (short) materialId;
                if (flagMatch(material.effectId) || material.textureSpeedU != 0 || material.textureSpeedV != 0) {
                    plainTile.functionMask |= TEXTURE_MOVEMENT;
                }
            } else {
                short hsl = ColorUtil.adjustHsv(color);
                plainTile.colorSouthWest = (short) ColorUtil.repackHsl(hsl, this.directionalTileLight[x][y] - this.tileShadowing[x][y]);
                plainTile.colorSouthEast = (short) ColorUtil.repackHsl(hsl, this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]);
                plainTile.colorNorthEast = (short) ColorUtil.repackHsl(hsl, this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]);
                plainTile.colorNorthWest = (short) ColorUtil.repackHsl(hsl, this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]);
                plainTile.materialId = -1;
            }

            this.scenePlainTiles[x][y] = plainTile;
        }
    }

    @Override
    public void addBlendedTile(int x, int y, int[] verticesX, int[] heightOffsets, int[] verticesY, int[] faceAlphas, int[] primaryColors, int[] materialColors, int[] materialIds, int[] ints_10, WaterTile waterTile, boolean bool12) {
        boolean hasTextures = (this.groundFunctionMask & DISABLE_TEXTURES) == 0;
        if (this.plainBlendedTiles == null && !hasTextures) {
            this.plainBlendedTiles = new PlainTileBlended[super.width][super.length];
            this.shapedBlendedTiles = new ShapedTileBlended[super.width][super.length];
        } else if (this.blendedTexturedTiles == null && hasTextures) {
            this.blendedTexturedTiles = new TextureBlendedTile[super.width][super.length];
        } else if (this.scenePlainTiles != null) {
            throw new IllegalStateException();
        }

        if (verticesX != null && verticesX.length != 0) {
            for (int index = 0; index < primaryColors.length; index++) {
                if (primaryColors[index] == -1) {
                    primaryColors[index] = 0;
                } else {
                    primaryColors[index] = ColorUtil.getHsvToRgbTable()[ColorUtil.adjustHsv(primaryColors[index]) & 0xffff] << 8 | 0xff;
                }
            }

            if (materialColors != null) {
                for (int index = 0; index < materialColors.length; index++) {
                    if (materialColors[index] == -1) {
                        materialColors[index] = 0;
                    } else {
                        materialColors[index] = ColorUtil.getHsvToRgbTable()[ColorUtil.adjustHsv(materialColors[index]) & 0xffff] << 8 | 0xff;
                    }
                }
            }

            if (hasTextures) {
                TextureBlendedTile blendedTexturedTile = new TextureBlendedTile();
                blendedTexturedTile.verticeCount = (short) verticesX.length;
                blendedTexturedTile.faceCount = (short) (verticesX.length / 3);
                blendedTexturedTile.positionX = new short[blendedTexturedTile.verticeCount];
                blendedTexturedTile.tileDrawHeight = new short[blendedTexturedTile.verticeCount];
                blendedTexturedTile.positionZ = new short[blendedTexturedTile.verticeCount];
                blendedTexturedTile.colors = new int[blendedTexturedTile.verticeCount];
                blendedTexturedTile.materialId = new short[blendedTexturedTile.verticeCount];
                blendedTexturedTile.aShortArray2254 = new short[blendedTexturedTile.verticeCount];
                blendedTexturedTile.lightIntensity = new byte[blendedTexturedTile.verticeCount];
                if (faceAlphas != null) {
                    blendedTexturedTile.alpha = new short[blendedTexturedTile.verticeCount];
                }

                for (int vertice = 0; vertice < blendedTexturedTile.verticeCount; vertice++) {
                	int verticeX = verticesX[vertice];
                	int verticeY = verticesY[vertice];
                	int lightIntensity;
                    if (verticeX == 0 && verticeY == 0) {
                        lightIntensity = this.directionalTileLight[x][y] - this.tileShadowing[x][y];
                    } else if (verticeX == 0 && verticeY == super.tileUnits) {
                        lightIntensity = this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1];
                    } else if (verticeX == super.tileUnits && verticeY == super.tileUnits) {
                        lightIntensity = this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1];
                    } else if (verticeX == super.tileUnits && verticeY == 0) {
                        lightIntensity = this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y];
                    } else {
                        int lightNorth = (this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]) * verticeX + (this.directionalTileLight[x][y] - this.tileShadowing[x][y]) * (super.tileUnits - verticeX);
                        int lightSouth = (this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]) * (super.tileUnits - verticeX) + (this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]) * verticeX;
                        lightIntensity = lightNorth * (super.tileUnits - verticeY) + lightSouth * verticeY >> super.tileScale * 2;
                    }

                    int sceneX = verticeX + (x << super.tileScale);
                    int sceneY = verticeY + (y << super.tileScale);
                    blendedTexturedTile.positionX[vertice] = (short) verticeX;
                    blendedTexturedTile.positionZ[vertice] = (short) verticeY;
                    blendedTexturedTile.tileDrawHeight[vertice] = (short) (getAverageHeight(sceneX, sceneY) + (heightOffsets != null ? heightOffsets[vertice] : 0));
                    if (lightIntensity < 0) {
                        lightIntensity = 0;
                    }

                    if (primaryColors[vertice] == 0) {
                        blendedTexturedTile.colors[vertice] = 0;
                    } else {
                    	int alpha = 0;
                        if (faceAlphas != null) {
                            short s_23 = blendedTexturedTile.alpha[vertice] = (short) faceAlphas[vertice];
                            if (waterTile.scale != 0) {
                                alpha = s_23 * 255 / waterTile.scale;
                                if (alpha < 0) {
                                    alpha = 0;
                                } else if (alpha > 255) {
                                    alpha = 255;
                                }
                            }
                        }

                        int baseColor = ~0xffffff;
                        if (materialIds[vertice] != -1 && flagMatch(this.toolkit.materialProvider.getMaterial(materialIds[vertice]).effectId)) {
                            baseColor = -1694498816;
                        }

                        blendedTexturedTile.colors[vertice] = baseColor | ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[vertice] >> 8, lightIntensity), waterTile.color, alpha);
                    }
                    if (materialColors != null) {
                        blendedTexturedTile.lightIntensity[vertice] = (byte) lightIntensity;
                    }

                    blendedTexturedTile.materialId[vertice] = (short) materialIds[vertice];
                    blendedTexturedTile.aShortArray2254[vertice] = (short) ints_10[vertice];
                }

                if (materialColors != null) {
                    blendedTexturedTile.materialColors = new int[blendedTexturedTile.faceCount];
                }

                for (int face = 0; face < blendedTexturedTile.faceCount; face++) {
                	int vertice = face * 3;
                    if (materialColors != null && materialColors[vertice] != 0) {
                        blendedTexturedTile.materialColors[face] = ~0xffffff | materialColors[vertice] >> 8;
                    }
                }

                this.blendedTexturedTiles[x][y] = blendedTexturedTile;
            } else {
                boolean fullColorMatch = true;
                int indexSouthWest = -1;
                int indexSouthEast = -1;
                int indexNorthEast = -1;
                int indexNorthWest = -1;
                if (verticesX.length != 6) {
                    fullColorMatch = false;
                } else {
                    for (int index = 0; index < 6; index++) {
                        if (verticesX[index] == 0 && verticesY[index] == 0) {
                            if (indexSouthWest != -1 && primaryColors[indexSouthWest] != primaryColors[index]) {
                                fullColorMatch = false;
                                break;
                            }
                            indexSouthWest = index;
                        } else if (verticesX[index] == super.tileUnits && verticesY[index] == 0) {
                            if (indexSouthEast != -1 && primaryColors[indexSouthEast] != primaryColors[index]) {
                                fullColorMatch = false;
                                break;
                            }
                            indexSouthEast = index;
                        } else if (verticesX[index] == super.tileUnits && verticesY[index] == super.tileUnits) {
                            if (indexNorthEast != -1 && primaryColors[indexNorthEast] != primaryColors[index]) {
                                fullColorMatch = false;
                                break;
                            }
                            indexNorthEast = index;
                        } else if (verticesX[index] == 0 && verticesY[index] == super.tileUnits) {
                            if (indexNorthWest != -1 && primaryColors[indexNorthWest] != primaryColors[index]) {
                                fullColorMatch = false;
                                break;
                            }
                            indexNorthWest = index;
                        }
                    }
                    if (indexSouthWest == -1 || indexSouthEast == -1 || indexNorthEast == -1 || indexNorthWest == -1) {
                        fullColorMatch = false;
                    }
                    if (fullColorMatch) {
                        if (heightOffsets != null) {
                            for (int z = 0; z < 4; z++) {
                                if (heightOffsets[z] != 0) {
                                    fullColorMatch = false;
                                    break;
                                }
                            }
                        }
                        if (fullColorMatch) {
                            for (int z = 1; z < 4; z++) {
                                if (verticesX[z] != verticesX[0] && verticesX[z] != verticesX[0] + super.tileUnits && verticesX[0] - super.tileUnits != verticesX[z]) {
                                    fullColorMatch = false;
                                    break;
                                }

                                if (verticesY[z] != verticesY[0] && verticesY[z] != verticesY[0] + super.tileUnits && verticesY[z] != verticesY[0] - super.tileUnits) {
                                    fullColorMatch = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (fullColorMatch) {
                    PlainTileBlended blendedTile = new PlainTileBlended();
                    int primaryColor = primaryColors[0];
                    int materialId = materialIds[0];
                    if (materialColors != null) {
                        blendedTile.materialColor = materialColors[0] >> 8;
                        if (primaryColor == 0) {
                            blendedTile.functionMask |= HD_LIGHT;
                        }
                    } else if (primaryColor == 0) {
                        return;
                    }

                    if (super.tileHeights[x][y] == super.tileHeights[x + 1][y] && super.tileHeights[x][y] == super.tileHeights[x + 1][y + 1] && super.tileHeights[x][y + 1] == super.tileHeights[x][y]) {
                        blendedTile.functionMask |= COMPLETELY_FLAT;
                    }

                    if (materialId != -1 && (blendedTile.functionMask & HD_LIGHT) == 0 && !this.toolkit.materialProvider.getMaterial(materialId).detailsOnly) {
                    	int alpha;
                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexSouthWest] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorSouthWest = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexSouthWest] >> 8, this.directionalTileLight[x][y] - this.tileShadowing[x][y]), waterTile.color, alpha);
                        if (blendedTile.materialColor != 0) {
                            blendedTile.colorSouthWest |= 255 - (this.directionalTileLight[x][y] - this.tileShadowing[x][y]) << 25;
                        }

                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexSouthEast] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorSouthEast = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexSouthEast] >> 8, this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]), waterTile.color, alpha);
                        if (blendedTile.materialColor != 0) {
                            blendedTile.colorSouthEast |= 255 - (this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]) << 25;
                        }

                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexNorthEast] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorNorthEast = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexNorthEast] >> 8, this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]), waterTile.color, alpha);
                        if (blendedTile.materialColor != 0) {
                            blendedTile.colorNorthEast |= 255 - (this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]) << 25;
                        }

                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexNorthWest] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorNorthWest = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexNorthWest] >> 8, this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]), waterTile.color, alpha);
                        blendedTile.materialId = (short) materialId;
                    } else {
                    	int alpha;
                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexSouthWest] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorSouthWest = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexSouthWest] >> 8, this.directionalTileLight[x][y] - this.tileShadowing[x][y]), waterTile.color, alpha);
                        if (blendedTile.materialColor != 0) {
                            blendedTile.colorSouthWest |= 255 - (this.directionalTileLight[x][y] - this.tileShadowing[x][y]) << 25;
                        }

                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexSouthEast] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorSouthEast = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexSouthEast] >> 8, this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]), waterTile.color, alpha);
                        if (blendedTile.materialColor != 0) {
                            blendedTile.colorSouthEast |= 255 - (this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]) << 25;
                        }

                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexNorthEast] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorNorthEast = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexNorthEast] >> 8, this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]), waterTile.color, alpha);
                        if (blendedTile.materialColor != 0) {
                            blendedTile.colorNorthEast |= 255 - (this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]) << 25;
                        }

                        if (faceAlphas != null && waterTile.scale != 0) {
                            alpha = faceAlphas[indexNorthWest] * 255 / waterTile.scale;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }
                        } else {
                            alpha = 0;
                        }

                        blendedTile.colorNorthWest = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[indexNorthWest] >> 8, this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]), waterTile.color, alpha);
                        if (blendedTile.materialColor != 0) {
                            blendedTile.colorNorthWest |= 255 - (this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]) << 25;
                        }

                        blendedTile.materialId = -1;
                    }

                    if (faceAlphas != null) {
                        blendedTile.alphaNorthEast = (short) faceAlphas[indexNorthEast];
                        blendedTile.alphaNorthWest = (short) faceAlphas[indexNorthWest];
                        blendedTile.alphaSouthEast = (short) faceAlphas[indexSouthEast];
                        blendedTile.alphaSouthWest = (short) faceAlphas[indexSouthWest];
                    }

                    this.plainBlendedTiles[x][y] = blendedTile;
                } else {
                    ShapedTileBlended shapedBlendedTile = new ShapedTileBlended();
                    shapedBlendedTile.verticeCount = (short) verticesX.length;
                    shapedBlendedTile.faceCount = (short) (verticesX.length / 3);
                    shapedBlendedTile.positionX = new short[shapedBlendedTile.verticeCount];
                    shapedBlendedTile.drawHeight = new short[shapedBlendedTile.verticeCount];
                    shapedBlendedTile.positionY = new short[shapedBlendedTile.verticeCount];
                    shapedBlendedTile.colors = new int[shapedBlendedTile.verticeCount];
                    if (faceAlphas != null) {
                        shapedBlendedTile.alpha = new short[shapedBlendedTile.verticeCount];
                    }
                    for (int index = 0; index < shapedBlendedTile.verticeCount; index++) {
                    	int verticeX = verticesX[index];
                    	int verticeY = verticesY[index];
                        int lightShadowAdjustment;
                        int sceneX;
                        int sceneY;
                        if (verticeX == 0 && verticeY == 0) {
                            lightShadowAdjustment = this.directionalTileLight[x][y] - this.tileShadowing[x][y];
                        } else if (verticeX == 0 && verticeY == super.tileUnits) {
                            lightShadowAdjustment = this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1];
                        } else if (verticeX == super.tileUnits && verticeY == super.tileUnits) {
                            lightShadowAdjustment = this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1];
                        } else if (verticeX == super.tileUnits && verticeY == 0) {
                            lightShadowAdjustment = this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y];
                        } else {
                            sceneX = (this.directionalTileLight[x + 1][y] - this.tileShadowing[x + 1][y]) * verticeX + (this.directionalTileLight[x][y] - this.tileShadowing[x][y]) * (super.tileUnits - verticeX);
                            sceneY = (this.directionalTileLight[x][y + 1] - this.tileShadowing[x][y + 1]) * (super.tileUnits - verticeX) + (this.directionalTileLight[x + 1][y + 1] - this.tileShadowing[x + 1][y + 1]) * verticeX;
                            lightShadowAdjustment = sceneX * (super.tileUnits - verticeY) + sceneY * verticeY >> super.tileScale * 2;
                        }
                        sceneX = verticeX + (x << super.tileScale);
                        sceneY = verticeY + (y << super.tileScale);
                        shapedBlendedTile.positionX[index] = (short) verticeX;
                        shapedBlendedTile.positionY[index] = (short) verticeY;
                        shapedBlendedTile.drawHeight[index] = (short) (getAverageHeight(sceneX, sceneY) + (heightOffsets != null ? heightOffsets[index] : 0));
                        if (lightShadowAdjustment < 0) {
                            lightShadowAdjustment = 0;
                        }

                        if (primaryColors[index] == 0) {
                            if (materialColors != null) {
                                shapedBlendedTile.colors[index] = lightShadowAdjustment << 25;
                            } else {
                                shapedBlendedTile.colors[index] = 0;
                            }
                        } else {
                        	int alpha = 0;
                            if (faceAlphas != null) {
                                short alphaPercent = shapedBlendedTile.alpha[index] = (short) faceAlphas[index];
                                if (waterTile.scale != 0) {
                                    alpha = alphaPercent * 255 / waterTile.scale;
                                    if (alpha < 0) {
                                        alpha = 0;
                                    } else if (alpha > 255) {
                                        alpha = 255;
                                    }
                                }
                            }
                            shapedBlendedTile.colors[index] = ColorUtil.blendColorsWithAlpha(ColorUtil.brighten(primaryColors[index] >> 8, lightShadowAdjustment), waterTile.color, alpha);
                            if (materialColors != null) {
                                shapedBlendedTile.colors[index] |= lightShadowAdjustment << 25;
                            }
                        }
                    }

                    boolean textured = false;
                    for (int face = 0; face < shapedBlendedTile.faceCount; face++) {
                        if (materialIds[face * 3] != -1 && !this.toolkit.materialProvider.getMaterial(materialIds[face * 3]).detailsOnly) {
                            textured = true;
                        }
                    }

                    if (materialColors != null) {
                        shapedBlendedTile.materialColors = new int[shapedBlendedTile.faceCount];
                    }

                    if (textured) {
                        shapedBlendedTile.materialIds = new short[shapedBlendedTile.faceCount];
                        shapedBlendedTile.aShortArray2422 = new short[shapedBlendedTile.faceCount];
                    }

                    for (int face = 0; face < shapedBlendedTile.faceCount; face++) {
                    	int verticeA = face * 3;
                        if (materialColors != null && materialColors[verticeA] != 0) {
                            shapedBlendedTile.materialColors[face] = materialColors[verticeA] >> 8;
                        }

                        if (textured) {
                        	int verticeB = verticeA + 1;
                        	int verticeC = verticeB + 1;
                            boolean texturesEnabled = false;
                            boolean detailsOnly = true;
                            int materialId = materialIds[verticeA];
                            if (materialId != -1 && !this.toolkit.materialProvider.getMaterial(materialId).detailsOnly) {
                                texturesEnabled = true;
                            } else {
                                detailsOnly = false;
                            }

                            materialId = materialIds[verticeB];
                            if (materialId != -1 && !this.toolkit.materialProvider.getMaterial(materialId).detailsOnly) {
                                texturesEnabled = true;
                            } else {
                                detailsOnly = false;
                            }

                            materialId = materialIds[verticeC];
                            if (materialId != -1 && !this.toolkit.materialProvider.getMaterial(materialId).detailsOnly) {
                                texturesEnabled = true;
                            } else {
                                detailsOnly = false;
                            }

                            if (detailsOnly) {
                                shapedBlendedTile.materialIds[face] = (short) materialId;
                                shapedBlendedTile.aShortArray2422[face] = (short) ints_10[verticeA];
                            } else {
                                if (texturesEnabled) {
                                    materialId = materialIds[verticeA];
                                    if (materialId != -1 && !this.toolkit.materialProvider.getMaterial(materialId).detailsOnly) {
                                        shapedBlendedTile.colors[verticeA] = ColorUtil.getHsvToRgbTable()[ColorUtil.adjustHsv(this.toolkit.materialProvider.getMaterial(materialId).colorHsl & 0xffff) & 0xffff];
                                    }

                                    materialId = materialIds[verticeB];
                                    if (materialId != -1 && !this.toolkit.materialProvider.getMaterial(materialId).detailsOnly) {
                                        shapedBlendedTile.colors[verticeB] = ColorUtil.getHsvToRgbTable()[ColorUtil.adjustHsv(this.toolkit.materialProvider.getMaterial(materialId).colorHsl & 0xffff) & 0xffff];
                                    }

                                    materialId = materialIds[verticeC];
                                    if (materialId != -1 && !this.toolkit.materialProvider.getMaterial(materialId).detailsOnly) {
                                        shapedBlendedTile.colors[verticeC] = ColorUtil.getHsvToRgbTable()[ColorUtil.adjustHsv(this.toolkit.materialProvider.getMaterial(materialId).colorHsl & 0xffff) & 0xffff];
                                    }
                                }
                                shapedBlendedTile.materialIds[face] = -1;
                            }
                        }
                    }
                    this.shapedBlendedTiles[x][y] = shapedBlendedTile;
                }
            }
        }
    }

    @Override
    public void resetLight() {
        this.directionalTileLight = null;
        this.tileShadowing = null;
    }

    public void renderBlendedUntexturedTile(int i_1, int i_2, boolean bool3, ThreadResourceSM class1854, RasterizerSM rasterizer, float[] floats_6, float[] floats_7, float[] floats_8, float[] floats_9, float[] floats_10, int i_11) {
        PlainTileBlended class17712 = this.plainBlendedTiles[i_1][i_2];
        int i_14;
        int i_15;
        int i_16;
        float f_18;
        float f_19;
        float f_20;
        float f_21;
        float f_22;
        float f_23;
        float f_24;
        if (class17712 != null) {
            if ((class17712.functionMask & HD_LIGHT) == 0) {
                if (i_11 != 0) {
                    if ((class17712.functionMask & TEXTURE_MOVEMENT) != 0) {
                        if ((i_11 & COMPLETELY_FLAT) != 0) {
                            return;
                        }
                    } else if ((i_11 & HD_LIGHT) != 0) {
                        return;
                    }
                }

                int i_13 = super.tileUnits * i_1;
                i_14 = i_13 + super.tileUnits;
                i_15 = i_2 * super.tileUnits;
                i_16 = i_15 + super.tileUnits;
                float f_17 = 0.0F;
                f_18 = 0.0F;
                f_19 = 0.0F;
                f_20 = 0.0F;
                float f_25;
                float f_26;
                float f_27;
                float f_28;
                float f_29;
                float f_30;
                float f_31;
                float f_32;
                float f_33;
                float f_34;
                float f_35;
                float f_36;
                int i_37;
                int i_39;
                float f_41;
                float f_42;
                float f_43;
                float f_44;
                float f_45;
                float f_47;
                float f_48;
                float f_49;
                float f_50;
                float f_51;
                float f_52;
                float f_60;
                float f_63;
                if ((class17712.functionMask & COMPLETELY_FLAT) != 0 && !bool3) {
                    i_37 = super.tileHeights[i_1][i_2];
                    f_60 = this.vertexZNearB * i_37;
                    float f_61 = this.vertexZFarB * i_37;
                    float f_62 = this.vertexZNearD + this.vertexZNearA * i_13 + f_60 + this.vertexZNearC * i_15;
                    f_21 = this.vertexZFarD + this.vertexZFarA * i_13 + f_61 + this.vertexZFarC * i_15;
                    if (f_62 < -f_21) {
                        return;
                    }

                    f_41 = this.vertexZNearD + this.vertexZNearA * i_14 + f_60 + this.vertexZNearC * i_15;
                    f_22 = this.vertexZFarD + this.vertexZFarA * i_14 + f_61 + this.vertexZFarC * i_15;
                    if (f_41 < -f_22) {
                        return;
                    }

                    f_42 = this.vertexZNearD + this.vertexZNearA * i_14 + f_60 + this.vertexZNearC * i_16;
                    f_23 = this.vertexZFarD + this.vertexZFarA * i_14 + f_61 + this.vertexZFarC * i_16;
                    if (f_42 < -f_23) {
                        return;
                    }

                    f_43 = this.vertexZNearD + this.vertexZNearA * i_13 + f_60 + this.vertexZNearC * i_16;
                    f_24 = this.vertexZFarD + this.vertexZFarA * i_13 + f_61 + this.vertexZFarC * i_16;
                    if (f_43 < -f_24) {
                        return;
                    }

                    f_25 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_62 / f_21;
                    f_26 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_41 / f_22;
                    f_27 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_42 / f_23;
                    f_28 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_43 / f_24;
                    if (class1854.aBool2304) {
                        f_44 = f_62 - class1854.aFloat2305;
                        if (f_44 > 0.0F) {
                            f_17 = f_44 / class1854.aFloat2303;
                            if (f_17 > 1.0F) {
                                f_17 = 1.0F;
                            }
                        }

                        f_44 = f_41 - class1854.aFloat2305;
                        if (f_44 > 0.0F) {
                            f_18 = f_44 / class1854.aFloat2303;
                            if (f_18 > 1.0F) {
                                f_18 = 1.0F;
                            }
                        }

                        f_44 = f_42 - class1854.aFloat2305;
                        if (f_44 > 0.0F) {
                            f_19 = f_44 / class1854.aFloat2303;
                            if (f_19 > 1.0F) {
                                f_19 = 1.0F;
                            }
                        }

                        f_44 = f_43 - class1854.aFloat2305;
                        if (f_44 > 0.0F) {
                            f_20 = f_44 / class1854.aFloat2303;
                            if (f_20 > 1.0F) {
                                f_20 = 1.0F;
                            }
                        }
                    }

                    f_44 = this.vertexXB * i_37;
                    f_45 = this.vertexYB * i_37;
                    f_63 = this.vertexXD + this.vertexXA * i_13 + f_44 + this.vertexXC * i_15;
                    f_29 = rasterizer.centerX + rasterizer.offsetX * f_63 / f_21;
                    f_47 = this.vertexYD + this.vertexYA * i_13 + f_45 + this.vertexYC * i_15;
                    f_30 = rasterizer.centerY + rasterizer.offsetY * f_47 / f_21;
                    f_48 = this.vertexXD + this.vertexXA * i_14 + f_44 + this.vertexXC * i_15;
                    f_31 = rasterizer.centerX + rasterizer.offsetX * f_48 / f_22;
                    f_49 = this.vertexYD + this.vertexYA * i_14 + f_45 + this.vertexYC * i_15;
                    f_32 = rasterizer.centerY + rasterizer.offsetY * f_49 / f_22;
                    f_50 = this.vertexXD + this.vertexXA * i_14 + f_44 + this.vertexXC * i_16;
                    f_33 = rasterizer.centerX + rasterizer.offsetX * f_50 / f_23;
                    f_51 = this.vertexYD + this.vertexYA * i_14 + f_45 + this.vertexYC * i_16;
                    f_34 = rasterizer.centerY + rasterizer.offsetY * f_51 / f_23;
                    f_52 = this.vertexXD + this.vertexXA * i_13 + f_44 + this.vertexXC * i_16;
                    f_35 = rasterizer.centerX + rasterizer.offsetX * f_52 / f_24;
                    float f_53 = this.vertexYD + this.vertexYA * i_13 + f_45 + this.vertexYC * i_16;
                    f_36 = rasterizer.centerY + rasterizer.offsetY * f_53 / f_24;
                } else {
                    i_37 = super.tileHeights[i_1][i_2];
                    int i_38 = super.tileHeights[i_1 + 1][i_2];
                    i_39 = super.tileHeights[i_1 + 1][i_2 + 1];
                    int i_40 = super.tileHeights[i_1][i_2 + 1];
                    f_41 = this.vertexZNearD + this.vertexZNearA * i_13 + this.vertexZNearB * i_37 + this.vertexZNearC * i_15;
                    f_21 = this.vertexZFarD + this.vertexZFarA * i_13 + this.vertexZFarB * i_37 + this.vertexZFarC * i_15;
                    if (f_41 < -f_21) {
                        return;
                    }

                    f_42 = this.vertexZNearD + this.vertexZNearA * i_14 + this.vertexZNearB * i_38 + this.vertexZNearC * i_15;
                    f_22 = this.vertexZFarD + this.vertexZFarA * i_14 + this.vertexZFarB * i_38 + this.vertexZFarC * i_15;
                    if (f_42 < -f_22) {
                        return;
                    }

                    f_43 = this.vertexZNearD + this.vertexZNearA * i_14 + this.vertexZNearB * i_39 + this.vertexZNearC * i_16;
                    f_23 = this.vertexZFarD + this.vertexZFarA * i_14 + this.vertexZFarB * i_39 + this.vertexZFarC * i_16;
                    if (f_43 < -f_23) {
                        return;
                    }

                    f_44 = this.vertexZNearD + this.vertexZNearA * i_13 + this.vertexZNearB * i_40 + this.vertexZNearC * i_16;
                    f_24 = this.vertexZFarD + this.vertexZFarA * i_13 + this.vertexZFarB * i_40 + this.vertexZFarC * i_16;
                    if (f_44 < -f_24) {
                        return;
                    }

                    f_25 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_41 / f_21;
                    f_26 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_42 / f_22;
                    f_27 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_43 / f_23;
                    f_28 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_44 / f_24;
                    if (bool3) {
                        f_45 = f_41 - class1854.aFloat2305;
                        int i_46;
                        if (f_45 > 0.0F) {
                            f_45 /= class1854.aFloat2303;
                            if (f_45 > 1.0F) {
                                f_45 = 1.0F;
                            }

                            f_17 = f_45;
                            i_46 = (int) (class17712.alphaSouthWest * f_45);
                            if (i_46 > 0) {
                                i_37 -= i_46;
                            }
                        }

                        f_45 = f_42 - class1854.aFloat2305;
                        if (f_45 > 0.0F) {
                            f_45 /= class1854.aFloat2303;
                            if (f_45 > 1.0F) {
                                f_45 = 1.0F;
                            }

                            f_18 = f_45;
                            i_46 = (int) (class17712.alphaSouthEast * f_45);
                            if (i_46 > 0) {
                                i_38 -= i_46;
                            }
                        }

                        f_45 = f_43 - class1854.aFloat2305;
                        if (f_45 > 0.0F) {
                            f_45 /= class1854.aFloat2303;
                            if (f_45 > 1.0F) {
                                f_45 = 1.0F;
                            }

                            f_19 = f_45;
                            i_46 = (int) (class17712.alphaNorthEast * f_45);
                            if (i_46 > 0) {
                                i_39 -= i_46;
                            }
                        }

                        f_45 = f_44 - class1854.aFloat2305;
                        if (f_45 > 0.0F) {
                            f_45 /= class1854.aFloat2303;
                            if (f_45 > 1.0F) {
                                f_45 = 1.0F;
                            }

                            f_20 = f_45;
                            i_46 = (int) (class17712.alphaNorthWest * f_45);
                            if (i_46 > 0) {
                                i_40 -= i_46;
                            }
                        }
                    } else if (class1854.aBool2304) {
                        f_45 = f_41 - class1854.aFloat2305;
                        if (f_45 > 0.0F) {
                            f_17 = f_45 / class1854.aFloat2303;
                            if (f_17 > 1.0F) {
                                f_17 = 1.0F;
                            }
                        }

                        f_45 = f_42 - class1854.aFloat2305;
                        if (f_45 > 0.0F) {
                            f_18 = f_45 / class1854.aFloat2303;
                            if (f_18 > 1.0F) {
                                f_18 = 1.0F;
                            }
                        }

                        f_45 = f_43 - class1854.aFloat2305;
                        if (f_45 > 0.0F) {
                            f_19 = f_45 / class1854.aFloat2303;
                            if (f_19 > 1.0F) {
                                f_19 = 1.0F;
                            }
                        }

                        f_45 = f_44 - class1854.aFloat2305;
                        if (f_45 > 0.0F) {
                            f_20 = f_45 / class1854.aFloat2303;
                            if (f_20 > 1.0F) {
                                f_20 = 1.0F;
                            }
                        }
                    }

                    f_45 = this.vertexXD + this.vertexXA * i_13 + this.vertexXB * i_37 + this.vertexXC * i_15;
                    f_29 = rasterizer.centerX + rasterizer.offsetX * f_45 / f_21;
                    f_63 = this.vertexYD + this.vertexYA * i_13 + this.vertexYB * i_37 + this.vertexYC * i_15;
                    f_30 = rasterizer.centerY + rasterizer.offsetY * f_63 / f_21;
                    f_47 = this.vertexXD + this.vertexXA * i_14 + this.vertexXB * i_38 + this.vertexXC * i_15;
                    f_31 = rasterizer.centerX + rasterizer.offsetX * f_47 / f_22;
                    f_48 = this.vertexYD + this.vertexYA * i_14 + this.vertexYB * i_38 + this.vertexYC * i_15;
                    f_32 = rasterizer.centerY + rasterizer.offsetY * f_48 / f_22;
                    f_49 = this.vertexXD + this.vertexXA * i_14 + this.vertexXB * i_39 + this.vertexXC * i_16;
                    f_33 = rasterizer.centerX + rasterizer.offsetX * f_49 / f_23;
                    f_50 = this.vertexYD + this.vertexYA * i_14 + this.vertexYB * i_39 + this.vertexYC * i_16;
                    f_34 = rasterizer.centerY + rasterizer.offsetY * f_50 / f_23;
                    f_51 = this.vertexXD + this.vertexXA * i_13 + this.vertexXB * i_40 + this.vertexXC * i_16;
                    f_35 = rasterizer.centerX + rasterizer.offsetX * f_51 / f_24;
                    f_52 = this.vertexYD + this.vertexYA * i_13 + this.vertexYB * i_40 + this.vertexYC * i_16;
                    f_36 = rasterizer.centerY + rasterizer.offsetY * f_52 / f_24;
                }

                boolean bool59 = class17712.materialId != -1 && flagMatch(this.toolkit.materialProvider.getMaterial(class17712.materialId).effectId);
                f_60 = f_18 + f_19 + f_20;
                if ((f_33 - f_35) * (f_32 - f_36) - (f_34 - f_36) * (f_31 - f_35) > 0.0F) {
                    rasterizer.clipEdges = f_33 < 0.0F || f_35 < 0.0F || f_31 < 0.0F || f_33 > rasterizer.clipWidth || f_35 > rasterizer.clipWidth || f_31 > rasterizer.clipWidth;
                    if (f_60 < 3.0F) {
                        if (f_60 > 0.0F) {
                            if (class17712.materialId >= 0) {
                                i_39 = ~0xffffff;
                                if (bool59) {
                                    i_39 = -1694498816;
                                }

                                rasterizer.drawMaterializedTriangle(true, true, false, f_34, f_36, f_32, f_33, f_35, f_31, f_27, f_28, f_26, f_23, f_24, f_22, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, i_39 | class17712.colorNorthEast & 0xffffff, i_39 | class17712.colorNorthWest & 0xffffff, i_39 | class17712.colorSouthEast & 0xffffff, class1854.fogColor, f_19 * 255.0F, f_20 * 255.0F, f_18 * 255.0F, class17712.materialId);
                            } else {
                                if (bool59) {
                                    rasterizer.blendingAlpha = 100;
                                }

                                rasterizer.drawTriangle(true, true, false, f_34, f_36, f_32, f_33, f_35, f_31, f_27, f_28, f_26, ColorUtil.blendMaterialColor(class17712.colorNorthEast, (int) (f_19 * 255.0F) << 24 | class1854.fogColor), ColorUtil.blendMaterialColor(class17712.colorNorthWest, (int) (f_20 * 255.0F) << 24 | class1854.fogColor), ColorUtil.blendMaterialColor(class17712.colorSouthEast, (int) (f_18 * 255.0F) << 24 | class1854.fogColor));
                                rasterizer.blendingAlpha = 0;
                            }
                        } else if (class17712.materialId >= 0) {
                            i_39 = ~0xffffff;
                            if (bool59) {
                                i_39 = -1694498816;
                            }

                            rasterizer.drawMaterializedTriangle(true, true, false, f_34, f_36, f_32, f_33, f_35, f_31, f_27, f_28, f_26, f_23, f_24, f_22, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, i_39 | class17712.colorNorthEast & 0xffffff, i_39 | class17712.colorNorthWest & 0xffffff, i_39 | class17712.colorSouthEast & 0xffffff, 0, 0.0F, 0.0F, 0.0F, class17712.materialId);
                        } else {
                            if (bool59) {
                                rasterizer.blendingAlpha = 100;
                            }

                            rasterizer.drawTriangle(true, true, false, f_34, f_36, f_32, f_33, f_35, f_31, f_27, f_28, f_26, class17712.colorNorthEast, class17712.colorNorthWest, class17712.colorSouthEast);
                            rasterizer.blendingAlpha = 0;
                        }
                    } else {
                        rasterizer.drawFlatTriangle(true, true, false, f_34, f_36, f_32, f_33, f_35, f_31, f_27, f_28, f_26, class1854.fogColor);
                    }
                }

                f_60 = f_17 + f_18 + f_20;
                if ((f_29 - f_31) * (f_36 - f_32) - (f_30 - f_32) * (f_35 - f_31) > 0.0F) {
                    rasterizer.clipEdges = f_29 < 0.0F || f_31 < 0.0F || f_35 < 0.0F || f_29 > rasterizer.clipWidth || f_31 > rasterizer.clipWidth || f_35 > rasterizer.clipWidth;
                    if (f_60 < 3.0F) {
                        if (bool59) {
                            rasterizer.blendingAlpha = -1694498816;
                        }

                        if (f_60 > 0.0F) {
                            if (class17712.materialId >= 0) {
                                i_39 = ~0xffffff;
                                if (bool59) {
                                    i_39 = -1694498816;
                                }

                                rasterizer.drawMaterializedTriangle(true, true, false, f_30, f_32, f_36, f_29, f_31, f_35, f_25, f_26, f_28, f_21, f_22, f_24, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, i_39 | class17712.colorSouthWest & 0xffffff, i_39 | class17712.colorSouthEast & 0xffffff, i_39 | class17712.colorNorthWest & 0xffffff, class1854.fogColor, f_17 * 255.0F, f_18 * 255.0F, f_20 * 255.0F, class17712.materialId);
                            } else {
                                if (bool59) {
                                    rasterizer.blendingAlpha = 100;
                                }

                                rasterizer.drawTriangle(true, true, false, f_30, f_32, f_36, f_29, f_31, f_35, f_25, f_26, f_28, ColorUtil.blendMaterialColor(class17712.colorSouthWest, (int) (f_17 * 255.0F) << 24 | class1854.fogColor), ColorUtil.blendMaterialColor(class17712.colorSouthEast, (int) (f_18 * 255.0F) << 24 | class1854.fogColor), ColorUtil.blendMaterialColor(class17712.colorNorthWest, (int) (f_20 * 255.0F) << 24 | class1854.fogColor));
                                rasterizer.blendingAlpha = 0;
                            }
                        } else if (class17712.materialId >= 0) {
                            i_39 = ~0xffffff;
                            if (bool59) {
                                i_39 = -1694498816;
                            }

                            rasterizer.drawMaterializedTriangle(true, true, false, f_30, f_32, f_36, f_29, f_31, f_35, f_25, f_26, f_28, f_21, f_22, f_24, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, i_39 | class17712.colorSouthWest & 0xffffff, i_39 | class17712.colorSouthEast & 0xffffff, i_39 | class17712.colorNorthWest & 0xffffff, 0, 0.0F, 0.0F, 0.0F, class17712.materialId);
                        } else {
                            if (bool59) {
                                rasterizer.blendingAlpha = 100;
                            }

                            rasterizer.drawTriangle(true, true, false, f_30, f_32, f_36, f_29, f_31, f_35, f_25, f_26, f_28, class17712.colorSouthWest, class17712.colorSouthEast, class17712.colorNorthWest);
                            rasterizer.blendingAlpha = 0;
                        }
                    } else {
                        rasterizer.drawFlatTriangle(true, true, false, f_30, f_32, f_36, f_29, f_31, f_35, f_25, f_26, f_28, class1854.fogColor);
                    }
                }
            }
        } else {
            ShapedTileBlended class19654 = this.shapedBlendedTiles[i_1][i_2];
            if (class19654 != null) {
                if (i_11 != 0) {
                    if ((class19654.functionMask & TEXTURE_MOVEMENT) != 0) {
                        if ((i_11 & COMPLETELY_FLAT) != 0) {
                            return;
                        }
                    } else if ((i_11 & HD_LIGHT) != 0) {
                        return;
                    }
                }

                int i_55;
                for (i_14 = 0; i_14 < class19654.verticeCount; i_14++) {
                    i_15 = class19654.positionX[i_14] + (i_1 << super.tileScale);
                    i_16 = class19654.drawHeight[i_14];
                    i_55 = class19654.positionY[i_14] + (i_2 << super.tileScale);
                    f_18 = this.vertexZNearD + this.vertexZNearA * i_15 + this.vertexZNearB * i_16 + this.vertexZNearC * i_55;
                    f_19 = this.vertexZFarD + this.vertexZFarA * i_15 + this.vertexZFarB * i_16 + this.vertexZFarC * i_55;
                    if (f_18 < -f_19) {
                        return;
                    }

                    f_20 = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_18 / f_19;
                    floats_10[i_14] = 0.0F;
                    if (bool3) {
                        f_21 = f_18 - class1854.aFloat2305;
                        if (f_21 > 0.0F) {
                            f_21 /= class1854.aFloat2303;
                            if (f_21 > 1.0F) {
                                f_21 = 1.0F;
                            }

                            floats_10[i_14] = f_21;
                            int i_56 = (int) (class19654.alpha[i_14] * f_21);
                            if (i_56 > 0) {
                                i_16 -= i_56;
                            }
                        }
                    } else if (class1854.aBool2304) {
                        f_21 = f_18 - class1854.aFloat2305;
                        if (f_21 > 0.0F) {
                            floats_10[i_14] = f_21 / class1854.aFloat2303;
                            if (floats_10[i_14] > 1.0F) {
                                floats_10[i_14] = 1.0F;
                            }
                        }
                    }

                    f_21 = this.vertexXD + this.vertexXA * i_15 + this.vertexXB * i_16 + this.vertexXC * i_55;
                    f_22 = this.vertexYD + this.vertexYA * i_15 + this.vertexYB * i_16 + this.vertexYC * i_55;
                    floats_6[i_14] = rasterizer.centerX + rasterizer.offsetX * f_21 / f_19;
                    floats_7[i_14] = rasterizer.centerY + rasterizer.offsetY * f_22 / f_19;
                    floats_8[i_14] = f_20;
                    floats_9[i_14] = f_19;
                }

                if (class19654.materialIds != null) {
                    for (i_14 = 0; i_14 < class19654.faceCount; i_14++) {
                        i_15 = i_14 * 3;
                        i_16 = i_15 + 1;
                        i_55 = i_16 + 1;
                        f_18 = floats_6[i_15];
                        f_19 = floats_6[i_16];
                        f_20 = floats_6[i_55];
                        f_21 = floats_7[i_15];
                        f_22 = floats_7[i_16];
                        f_23 = floats_7[i_55];
                        f_24 = floats_10[i_15] + floats_10[i_16] + floats_10[i_55];
                        if ((f_18 - f_19) * (f_23 - f_22) - (f_21 - f_22) * (f_20 - f_19) > 0.0F) {
                            rasterizer.clipEdges = f_18 < 0.0F || f_19 < 0.0F || f_20 < 0.0F || f_18 > rasterizer.clipWidth || f_19 > rasterizer.clipWidth || f_20 > rasterizer.clipWidth;
                            short materialId = class19654.materialIds[i_14];
                            if (f_24 < 3.0F) {
                                int i_58;
                                if (f_24 > 0.0F) {
                                    if (materialId != -1) {
                                        i_58 = ~0xffffff;
                                        if (flagMatch(this.toolkit.materialProvider.getMaterial(materialId).effectId)) {
                                            i_58 = -1694498816;
                                        }

                                        rasterizer.drawMaterializedTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], floats_9[i_15], floats_9[i_16], floats_9[i_55], (float) class19654.positionX[i_15] / super.tileUnits, (float) class19654.positionX[i_16] / super.tileUnits, (float) class19654.positionX[i_55] / super.tileUnits, (float) class19654.positionY[i_15] / super.tileUnits, (float) class19654.positionY[i_16] / super.tileUnits, (float) class19654.positionY[i_55] / super.tileUnits, i_58 | class19654.colors[i_15] & 0xffffff, i_58 | class19654.colors[i_16] & 0xffffff, i_58 | class19654.colors[i_55] & 0xffffff, class1854.fogColor, floats_10[i_15] * 255.0F, floats_10[i_16] * 255.0F, floats_10[i_55] * 255.0F, materialId);
                                    } else if ((class19654.colors[i_15] & 0xffffff) != 0) {
                                        rasterizer.drawTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], ColorUtil.blendMaterialColor(class19654.colors[i_15], (int) (floats_10[i_15] * 255.0F) << 24 | class1854.fogColor), ColorUtil.blendMaterialColor(class19654.colors[i_16], (int) (floats_10[i_16] * 255.0F) << 24 | class1854.fogColor), ColorUtil.blendMaterialColor(class19654.colors[i_55], (int) (floats_10[i_55] * 255.0F) << 24 | class1854.fogColor));
                                        rasterizer.blendingAlpha = 0;
                                    }
                                } else if (materialId != -1) {
                                    i_58 = ~0xffffff;
                                    if (flagMatch(this.toolkit.materialProvider.getMaterial(materialId).effectId)) {
                                        i_58 = -1694498816;
                                    }

                                    rasterizer.drawMaterializedTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], floats_9[i_15], floats_9[i_16], floats_9[i_55], (float) class19654.positionX[i_15] / super.tileUnits, (float) class19654.positionX[i_16] / super.tileUnits, (float) class19654.positionX[i_55] / super.tileUnits, (float) class19654.positionY[i_15] / super.tileUnits, (float) class19654.positionY[i_16] / super.tileUnits, (float) class19654.positionY[i_55] / super.tileUnits, i_58 | class19654.colors[i_15] & 0xffffff, i_58 | class19654.colors[i_16] & 0xffffff, i_58 | class19654.colors[i_55] & 0xffffff, 0, 0.0F, 0.0F, 0.0F, materialId);
                                } else if ((class19654.colors[i_15] & 0xffffff) != 0) {

                                    rasterizer.drawTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], class19654.colors[i_15], class19654.colors[i_16], class19654.colors[i_55]);
                                    rasterizer.blendingAlpha = 0;
                                }
                            } else {
                                rasterizer.drawFlatTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], class1854.fogColor);
                            }
                        }
                    }
                } else {
                    for (i_14 = 0; i_14 < class19654.faceCount; i_14++) {
                        i_15 = i_14 * 3;
                        i_16 = i_15 + 1;
                        i_55 = i_16 + 1;
                        f_18 = floats_6[i_15];
                        f_19 = floats_6[i_16];
                        f_20 = floats_6[i_55];
                        f_21 = floats_7[i_15];
                        f_22 = floats_7[i_16];
                        f_23 = floats_7[i_55];
                        f_24 = floats_10[i_15] + floats_10[i_16] + floats_10[i_55];
                        if ((f_18 - f_19) * (f_23 - f_22) - (f_21 - f_22) * (f_20 - f_19) > 0.0F) {
                            rasterizer.clipEdges = f_18 < 0.0F || f_19 < 0.0F || f_20 < 0.0F || f_18 > rasterizer.clipWidth || f_19 > rasterizer.clipWidth || f_20 > rasterizer.clipWidth;
                            if (f_24 < 3.0F) {
                                if (f_24 > 0.0F) {
                                    if ((class19654.colors[i_15] & 0xffffff) != 0) {
                                        rasterizer.drawTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], ColorUtil.mixColorsWithTransparency(class19654.colors[i_15], class1854.fogColor, floats_10[i_15] * 255.0F), ColorUtil.mixColorsWithTransparency(class19654.colors[i_16], class1854.fogColor, floats_10[i_16] * 255.0F), ColorUtil.mixColorsWithTransparency(class19654.colors[i_55], class1854.fogColor, floats_10[i_55] * 255.0F));
                                    }
                                } else if ((class19654.colors[i_15] & 0xffffff) != 0) {
                                    rasterizer.drawTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], class19654.colors[i_15], class19654.colors[i_16], class19654.colors[i_55]);
                                }
                            } else {
                                rasterizer.drawFlatTriangle(true, true, false, f_21, f_22, f_23, f_18, f_19, f_20, floats_8[i_15], floats_8[i_16], floats_8[i_55], class1854.fogColor);
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void renderFloor(int x, int y, int visibleTiles, boolean[][] onscreen, boolean dummy) {
    	if (this.toolkit.colourBuffer == null || this.toolkit.depthBuffer == null) {
    		throw new IllegalStateException("");
    	}
        Matrix44 floorMatrix = this.toolkit.floorMatrix;
        this.vertexXA = floorMatrix.getFaceList()[0];
        this.vertexYA = floorMatrix.getFaceList()[1];
        this.vertexZNearA = floorMatrix.getFaceList()[2];
        this.vertexZFarA = floorMatrix.getFaceList()[3];
        this.vertexXB = floorMatrix.getFaceList()[4];
        this.vertexYB = floorMatrix.getFaceList()[5];
        this.vertexZNearB = floorMatrix.getFaceList()[6];
        this.vertexZFarB = floorMatrix.getFaceList()[7];
        this.vertexXC = floorMatrix.getFaceList()[8];
        this.vertexYC = floorMatrix.getFaceList()[9];
        this.vertexZNearC = floorMatrix.getFaceList()[10];
        this.vertexZFarC = floorMatrix.getFaceList()[11];
        this.vertexXD = floorMatrix.getFaceList()[12];
        this.vertexYD = floorMatrix.getFaceList()[13];
        this.vertexZNearD = floorMatrix.getFaceList()[14];
        this.vertexZFarD = floorMatrix.getFaceList()[15];
        for (int startX = 0; startX < visibleTiles + visibleTiles; startX++) {
            for (int startY = 0; startY < visibleTiles + visibleTiles; startY++) {
                if (onscreen[startX][startY]) {
                    int sceneX = startX + (x - visibleTiles);
                    int sceneY = startY + (y - visibleTiles);
                    if (sceneX >= 0 && sceneX < super.width && sceneY >= 0 && sceneY < super.length) {
                        render(sceneX, sceneY);
                    }
                }
            }
        }
    }

    public void renderHighDetailMinimap(int offX, int offY, int startX, int startY, int endX, int endY, boolean[][] onscreen, ThreadResourceSM class1859, RasterizerSM rasterizer, float[] projectedVerticesX, float[] projectedVerticesY) {
    	int stepY = 1024 * (endY - startY) / 256;
        int baseOffset = 1024 >> 8;
        boolean depthBuffering = class1859.zbuffering;
        this.toolkit.setDepthBuffering(false);
        rasterizer.smoothEdges = false;
        rasterizer.blending = false;
        int offsetX = offX;
        int offsetY = stepY + offY;
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                if (onscreen[x - startX][y - startY]) {
                    if (this.plainBlendedTiles != null) {
                        if (this.plainBlendedTiles[x][y] != null) {
                            PlainTileBlended plainTileBlended = this.plainBlendedTiles[x][y];
                            if (plainTileBlended.materialId != -1 && (plainTileBlended.functionMask & HD_LIGHT) == 0 && plainTileBlended.materialColor == 0) {
                            	int materialColor = this.toolkit.getMaterialColor(plainTileBlended.materialId);
                                rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset), (offsetY - baseOffset), offsetY, (offsetX + baseOffset), offsetX, (offsetX + baseOffset), 100.0F, 100.0F, 100.0F, ColorUtil.repackHsl(materialColor, plainTileBlended.colorNorthEast), ColorUtil.repackHsl(materialColor, plainTileBlended.colorNorthWest), ColorUtil.repackHsl(materialColor, plainTileBlended.colorSouthEast));
                                rasterizer.drawShadedTriangle(true, true, false, offsetY, offsetY, (offsetY - baseOffset), offsetX, (offsetX + baseOffset), offsetX, 100.0F, 100.0F, 100.0F, ColorUtil.repackHsl(materialColor, plainTileBlended.colorSouthWest), ColorUtil.repackHsl(materialColor, plainTileBlended.colorSouthEast), ColorUtil.repackHsl(materialColor, plainTileBlended.colorNorthWest));
                            } else if (plainTileBlended.materialColor == 0) {
                                rasterizer.drawTriangle(true, true, false, (offsetY - baseOffset), (offsetY - baseOffset), offsetY, (offsetX + baseOffset), offsetX, (offsetX + baseOffset), 100.0F, 100.0F, 100.0F, plainTileBlended.colorNorthEast, plainTileBlended.colorNorthWest, plainTileBlended.colorSouthEast);
                                rasterizer.drawTriangle(true, true, false, offsetY, offsetY, (offsetY - baseOffset), offsetX, (offsetX + baseOffset), offsetX, 100.0F, 100.0F, 100.0F, plainTileBlended.colorSouthWest, plainTileBlended.colorSouthEast, plainTileBlended.colorNorthWest);
                            } else {
                            	int materialColor = plainTileBlended.materialColor;
                                rasterizer.drawTriangle(true, true, false, (offsetY - baseOffset), (offsetY - baseOffset), offsetY, (offsetX + baseOffset), offsetX, (offsetX + baseOffset), 100.0F, 100.0F, 100.0F, ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorNorthEast & ~0xffffff), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorNorthWest & ~0xffffff), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorSouthEast & ~0xffffff));
                                rasterizer.drawTriangle(true, true, false, offsetY, offsetY, (offsetY - baseOffset), offsetX, (offsetX + baseOffset), offsetX, 100.0F, 100.0F, 100.0F, ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorSouthWest & ~0xffffff), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorSouthEast & ~0xffffff), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorNorthWest & ~0xffffff));
                            }
                        } else if (this.shapedBlendedTiles[x][y] != null) {
                            ShapedTileBlended shapedBlendedTile = this.shapedBlendedTiles[x][y];
                            for (int vertice = 0; vertice < shapedBlendedTile.verticeCount; vertice++) {
                                projectedVerticesX[vertice] = (offsetX + shapedBlendedTile.positionX[vertice] * baseOffset / super.tileUnits);
                                projectedVerticesY[vertice] = (offsetY - shapedBlendedTile.positionY[vertice] * baseOffset / super.tileUnits);
                            }
                            for (int face = 0; face < shapedBlendedTile.faceCount; face++) {
                            	int a = face * 3;
                            	int b = a + 1;
                            	int c = b + 1;
                            	float xA = projectedVerticesX[a];
                            	float xB = projectedVerticesX[b];
                            	float xC = projectedVerticesX[c];
                            	float yA = projectedVerticesY[a];
                            	float yB = projectedVerticesY[b];
                            	float yC = projectedVerticesY[c];
                                if (shapedBlendedTile.materialColors == null || shapedBlendedTile.materialColors[face] == 0 || shapedBlendedTile.materialIds != null && shapedBlendedTile.materialIds[face] != -1) {
                                    if (shapedBlendedTile.materialIds != null && shapedBlendedTile.materialIds[face] != -1) {
                                        int materialColor = this.toolkit.getMaterialColor(shapedBlendedTile.materialIds[face]);
                                        rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, materialColor, materialColor, materialColor);
                                    } else {
                                        rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, shapedBlendedTile.colors[a], shapedBlendedTile.colors[b], shapedBlendedTile.colors[c]);
                                    }
                                } else {
                                	int materialColor = shapedBlendedTile.materialColors[face];
                                    rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, ColorUtil.blendMaterialColor(materialColor, ~0xffffff - (shapedBlendedTile.colors[a] & ~0xffffff)), ColorUtil.blendMaterialColor(materialColor, ~0xffffff - (shapedBlendedTile.colors[b] & ~0xffffff)), ColorUtil.blendMaterialColor(materialColor, ~0xffffff - (shapedBlendedTile.colors[c] & ~0xffffff)));
                                }
                            }
                        }
                    } else if (this.blendedTexturedTiles[x][y] != null) {
                        TextureBlendedTile blendedTexturedTile = this.blendedTexturedTiles[x][y];
                        for (int vertice = 0; vertice < blendedTexturedTile.verticeCount; vertice++) {
                            projectedVerticesX[vertice] = (offsetX + blendedTexturedTile.positionX[vertice] * baseOffset / super.tileUnits);
                            projectedVerticesY[vertice] = (offsetY - blendedTexturedTile.positionZ[vertice] * baseOffset / super.tileUnits);
                        }
                        for (int face = 0; face < blendedTexturedTile.faceCount; face++) {
                        	int a = face * 3;
                        	int b = a + 1;
                        	int c = b + 1;
                        	float xA = projectedVerticesX[a];
                        	float xB = projectedVerticesX[b];
                        	float xC = projectedVerticesX[c];
                        	float yA = projectedVerticesY[a];
                        	float yB = projectedVerticesY[b];
                        	float yC = projectedVerticesY[c];
                            if (blendedTexturedTile.materialColors != null && blendedTexturedTile.materialColors[face] != 0) {
                            	int materialColor = blendedTexturedTile.materialColors[face];
                                rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, materialColor, materialColor, materialColor);
                            } else {
                                rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, blendedTexturedTile.colors[a], blendedTexturedTile.colors[b], blendedTexturedTile.colors[c]);
                            }
                        }
                    }
                }
                offsetY -= baseOffset;
            }
            offsetY = stepY + offY;
            offsetX += baseOffset;
        }
        rasterizer.smoothEdges = true;
        this.toolkit.setDepthBuffering(depthBuffering);
    }
    
    
    public void renderBlendedTexturesTile(int x, int y, boolean bool3, ThreadResourceSM thread, RasterizerSM rasterizer, float[] perspectiveX, float[] perspectiveY, float[] perspectiveZ, float[] perspectiveZB, float[] floats_10, int tileFlags) {
        TextureBlendedTile blendedTexturedTile = this.blendedTexturedTiles[x][y];
        if ((tileFlags == 0 || (tileFlags & HD_LIGHT) == 0) && blendedTexturedTile != null) {
            for (int verticeIndex = 0; verticeIndex < blendedTexturedTile.verticeCount; verticeIndex++) {
                int i_14 = blendedTexturedTile.positionX[verticeIndex] + (x << super.tileScale);
                int i_15 = blendedTexturedTile.tileDrawHeight[verticeIndex];
                int i_16 = blendedTexturedTile.positionZ[verticeIndex] + (y << super.tileScale);
                float f_17 = this.vertexZNearD + this.vertexZNearA * i_14 + this.vertexZNearB * i_15 + this.vertexZNearC * i_16;
                float zB = this.vertexZFarD + this.vertexZFarA * i_14 + this.vertexZFarB * i_15 + this.vertexZFarC * i_16;
                if (f_17 < -zB) {
                    return;
                }

                floats_10[verticeIndex] = 0.0F;
                if (bool3) {
                    float f_19 = f_17 - thread.aFloat2305;
                    if (f_19 > 0.0F) {
                        f_19 /= thread.aFloat2303;
                        if (f_19 > 1.0F) {
                            f_19 = 1.0F;
                        }

                        floats_10[verticeIndex] = f_19;
                        int alpha = (int) (blendedTexturedTile.alpha[verticeIndex] * f_19);
                        if (alpha > 0) {
                            i_15 -= alpha;
                        }
                    }
                } else if (thread.aBool2304) {
                    float f_19 = f_17 - thread.aFloat2305;
                    if (f_19 > 0.0F) {
                        floats_10[verticeIndex] = f_19 / thread.aFloat2303;
                        if (floats_10[verticeIndex] > 1.0F) {
                            floats_10[verticeIndex] = 1.0F;
                        }
                    }
                }

                float f_19 = this.vertexXD + this.vertexXA * i_14 + this.vertexXB * i_15 + this.vertexXC * i_16;
                float f_28 = this.vertexYD + this.vertexYA * i_14 + this.vertexYB * i_15 + this.vertexYC * i_16;
                perspectiveX[verticeIndex] = rasterizer.centerX + rasterizer.offsetX * f_19 / zB;
                perspectiveY[verticeIndex] = rasterizer.centerY + rasterizer.offsetY * f_28 / zB;
                perspectiveZ[verticeIndex] = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_17 / zB;
                perspectiveZB[verticeIndex] = zB;
            }

            float f_26 = super.tileUnits;

            for (int face = 0; face < blendedTexturedTile.faceCount; face++) {
            	int a = face * 3;
            	int b = a + 1;
                int c = b + 1;
                float xA = perspectiveX[a];
                float xB = perspectiveX[b];
                float xC = perspectiveX[c];
                float yA = perspectiveY[a];
                float yB = perspectiveY[b];
                float yC = perspectiveY[c];
                if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0F) {
                    rasterizer.clipEdges = xA < 0.0F || xB < 0.0F || xC < 0.0F || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth;
                    if (floats_10[a] + floats_10[b] + floats_10[c] < 3.0F) {
                        int scaledX = x << super.tileScale;
                        int scaledY = y << super.tileScale;
                        if ((blendedTexturedTile.colors[a] & 0xffffff) != 0) {
                            if (blendedTexturedTile.materialId[a] == blendedTexturedTile.materialId[b] && blendedTexturedTile.materialId[a] == blendedTexturedTile.materialId[c] && blendedTexturedTile.aShortArray2254[a] == blendedTexturedTile.aShortArray2254[b] && blendedTexturedTile.aShortArray2254[a] == blendedTexturedTile.aShortArray2254[c]) {
                                rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZ[a], perspectiveZ[b], perspectiveZ[c], perspectiveZB[a], perspectiveZB[b], perspectiveZB[c], (float) (blendedTexturedTile.positionX[a] + scaledX) / blendedTexturedTile.aShortArray2254[a], (float) (blendedTexturedTile.positionX[b] + scaledX) / blendedTexturedTile.aShortArray2254[b], (float) (blendedTexturedTile.positionX[c] + scaledX) / blendedTexturedTile.aShortArray2254[c], (float) (blendedTexturedTile.positionZ[a] + scaledY) / blendedTexturedTile.aShortArray2254[a], (float) (blendedTexturedTile.positionZ[b] + scaledY) / blendedTexturedTile.aShortArray2254[b], (float) (blendedTexturedTile.positionZ[c] + scaledY) / blendedTexturedTile.aShortArray2254[c], blendedTexturedTile.colors[a], blendedTexturedTile.colors[b], blendedTexturedTile.colors[c], thread.fogColor, floats_10[a] * 255.0F, floats_10[b] * 255.0F, floats_10[c] * 255.0F, blendedTexturedTile.materialId[a]);
                            } else {
                                rasterizer.drawMultiMaterializedTriangle(yA, yB, yC, xA, xB, xC, perspectiveZ[a], perspectiveZ[b], perspectiveZ[c], perspectiveZB[a], perspectiveZB[b], perspectiveZB[c], (blendedTexturedTile.positionX[a] + scaledX) / f_26, (blendedTexturedTile.positionX[b] + scaledX) / f_26, (blendedTexturedTile.positionX[c] + scaledX) / f_26, (blendedTexturedTile.positionZ[a] + scaledY) / f_26, (blendedTexturedTile.positionZ[b] + scaledY) / f_26, (blendedTexturedTile.positionZ[c] + scaledY) / f_26, blendedTexturedTile.colors[a], blendedTexturedTile.colors[b], blendedTexturedTile.colors[c], thread.fogColor, floats_10[a] * 255.0F, floats_10[b] * 255.0F, floats_10[c] * 255.0F, blendedTexturedTile.materialId[a], f_26 / blendedTexturedTile.aShortArray2254[a], blendedTexturedTile.materialId[b], f_26 / blendedTexturedTile.aShortArray2254[b], blendedTexturedTile.materialId[c], f_26 / blendedTexturedTile.aShortArray2254[c]);
                            }
                        }
                    } else {
                        rasterizer.drawFlatTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZ[a], perspectiveZ[b], perspectiveZ[c], thread.fogColor);
                    }
                }
            }
        }
    }

    public boolean flagMatch(int effectId) {
        return (this.groundFunctionMask & 0x8) != 0 && (effectId == 4 || (effectId == 8 || effectId == 9));
    }

    

    public void renderLowDetailMinimap(int offX, int offY, int startX, int startY, int endX, int endY, boolean[][] onScreen, ThreadResourceSM class1859, RasterizerSM rasterizer, float[] renderX, float[] renderY) {
    	int stepY = 1024 * (endY - startY) / 256;
        int baseOffset = 1024 >> 8;
        boolean enableZbuffer = class1859.zbuffering;
        this.toolkit.setDepthBuffering(false);
        rasterizer.smoothEdges = false;
        rasterizer.blending = false;
        int offsetX = offX;
        int offsetY = stepY + offY;
        for (int tileX = startX; tileX < endX; tileX++) {
            for (int tileY = startY; tileY < endY; tileY++) {
                if (onScreen[tileX - startX][tileY - startY]) {
                    if (this.scenePlainTiles[tileX][tileY] != null) {
                        PlainTile plain = this.scenePlainTiles[tileX][tileY];
                        if (plain.materialId != -1 && (plain.functionMask & HD_LIGHT) == 0 && plain.color == -1) {
                        	int materialColor = this.toolkit.getMaterialColor(plain.materialId);
                            rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset), (offsetY - baseOffset), offsetY, (offsetX + baseOffset), offsetX, (offsetX + baseOffset), 100.0F, 100.0F, 100.0F, ColorUtil.repackHsl(materialColor, plain.colorNorthEast & 0xffff), ColorUtil.repackHsl(materialColor, plain.colorNorthWest & 0xffff), ColorUtil.repackHsl(materialColor, plain.colorSouthEast & 0xffff));
                            rasterizer.drawShadedTriangle(true, true, false, offsetY, offsetY, (offsetY - baseOffset), offsetX, (offsetX + baseOffset), offsetX, 100.0F, 100.0F, 100.0F, ColorUtil.repackHsl(materialColor, plain.colorSouthWest & 0xffff), ColorUtil.repackHsl(materialColor, plain.colorSouthEast & 0xffff), ColorUtil.repackHsl(materialColor, plain.colorNorthWest & 0xffff));
                        } else if (plain.color == -1) {
                            rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset), (offsetY - baseOffset), offsetY, (offsetX + baseOffset), offsetX, (offsetX + baseOffset), 100.0F, 100.0F, 100.0F, (plain.colorNorthEast & 0xffff), (plain.colorNorthWest & 0xffff), (plain.colorSouthEast & 0xffff));
                            rasterizer.drawShadedTriangle(true, true, false, offsetY, offsetY, (offsetY - baseOffset), offsetX, (offsetX + baseOffset), offsetX, 100.0F, 100.0F, 100.0F, (plain.colorSouthWest & 0xffff), (plain.colorSouthEast & 0xffff), (plain.colorNorthWest & 0xffff));
                        } else {
                        	int tileColor = plain.color;
                            rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset), (offsetY - baseOffset), offsetY, (offsetX + baseOffset), offsetX, (offsetX + baseOffset), 100.0F, 100.0F, 100.0F, tileColor, tileColor, tileColor);
                            rasterizer.drawShadedTriangle(true, true, false, offsetY, offsetY, (offsetY - baseOffset), offsetX, (offsetX + baseOffset), offsetX, 100.0F, 100.0F, 100.0F, tileColor, tileColor, tileColor);
                        }
                    } else if (this.sceneShapedTiles[tileX][tileY] != null) {
                        ShapedTile tile = this.sceneShapedTiles[tileX][tileY];
                        for (int i_21 = 0; i_21 < tile.count; i_21++) {
                            renderX[i_21] = (offsetX + tile.offsetX[i_21] * baseOffset / super.tileUnits);
                            renderY[i_21] = (offsetY - tile.offsetY[i_21] * baseOffset / super.tileUnits);
                        }
                        for (int triangleIndex = 0; triangleIndex < tile.triangleCount; triangleIndex++) {
                            short a = tile.triangleA[triangleIndex];
                            short b = tile.triangleB[triangleIndex];
                            short c = tile.triangleC[triangleIndex];
                            float xA = renderX[a];
                            float xB = renderX[b];
                            float xC = renderX[c];
                            float yA = renderY[a];
                            float yB = renderY[b];
                            float yC = renderY[c];
                            if (tile.secondaryColors != null && tile.secondaryColors[triangleIndex] != -1) {
                                int tileColor = tile.secondaryColors[triangleIndex];
                                rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, ColorUtil.repackHsl(tileColor, tile.light[a]), ColorUtil.repackHsl(tileColor, tile.light[b]), ColorUtil.repackHsl(tileColor, tile.light[c]));
                            } else if (tile.primaryMaterialIds != null && tile.primaryMaterialIds[triangleIndex] != -1) {
                                int materialColor = this.toolkit.getMaterialColor(tile.primaryMaterialIds[triangleIndex]);
                                rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, ColorUtil.repackHsl(materialColor, tile.light[a]), ColorUtil.repackHsl(materialColor, tile.light[b]), ColorUtil.repackHsl(materialColor, tile.light[c]));
                            } else {
                                int tileColor = tile.primaryColors[triangleIndex];
                                rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, ColorUtil.repackHsl(tileColor, tile.light[a]), ColorUtil.repackHsl(tileColor, tile.light[b]), ColorUtil.repackHsl(tileColor, tile.light[c]));
                            }
                        }
                    }
                }
                offsetY -= baseOffset;
            }
            offsetY = stepY + offY;
            offsetX += baseOffset;
        }
        rasterizer.smoothEdges = true;
        this.toolkit.setDepthBuffering(enableZbuffer);
    }


    @Override
    public Shadow createShadowAt(int i_1, int i_2, Shadow shadow) {
        return null;
    }

    @Override
    public void addShadow(Shadow shadow, int i_2, int i_3, int i_4) {
    	// NOOP
    }

    @Override
    public boolean hasRenderingShadows(Shadow shadow, int i_2, int i_3, int i_4) {
        return false;
    }

 



    /**
     * Only starts rendering HD after loading the next region
     */
    @Override
    public void renderMinimapFloor(int i_1, int i_2, int tilesStartX, int tilesStartY, int tilesEndX, int tilesEndY, boolean[][] tilesOnScreen) {
        ThreadResourceSM thread = this.toolkit.getThreadResource(Thread.currentThread());
        RasterizerSM rasterizer = thread.rasterizer;
        rasterizer.blendingAlpha = 0;
        rasterizer.clipEdges = true;
        this.toolkit.clearDepth();
        if (this.plainBlendedTiles == null && this.blendedTexturedTiles == null) {
            if (this.scenePlainTiles != null) {
                renderLowDetailMinimap(i_1, i_2, tilesStartX, tilesStartY, tilesEndX, tilesEndY, tilesOnScreen, thread, rasterizer, thread.perspectiveX, thread.perspectiveY);
            }
        } else {
            renderHighDetailMinimap(i_1, i_2, tilesStartX, tilesStartY, tilesEndX, tilesEndY, tilesOnScreen, thread, rasterizer, thread.perspectiveX, thread.perspectiveY);
        }
    }

    @Override
    public void removeShadow(Shadow class282Sub50Sub171, int i_2, int i_3, int i_4) {
    }


    //draw_tiles
    public void renderBlockTile(int x, int y, RasterizerSM rasterizer, float[] perspectiveX, float[] perspectiveY, float[] perspectiveZNear, float[] perspectiveZFar, int tileFlags) {
        PlainTile plainTile = this.scenePlainTiles[x][y];
        // cant move down
        float farZB;
        float farZC;
        float farZD;
        float XA;
        float YA;
        float ZA;
        if (plainTile != null) {
            if ((plainTile.functionMask & HD_LIGHT) == 0) {
                if (tileFlags != 0) {
                    if ((plainTile.functionMask & TEXTURE_MOVEMENT) != 0) {
                        if ((tileFlags & COMPLETELY_FLAT) != 0) {
                            return;
                        }
                    } else if ((tileFlags & HD_LIGHT) != 0) {
                        return;
                    }
                }

                int tileunitsStartX = super.tileUnits * x;
                int tileunitsEndX = tileunitsStartX + super.tileUnits;
                int tileunitsStartY = y * super.tileUnits;
                int tileunitsEndY = tileunitsStartY + super.tileUnits;
                float farZA;
                float XB;
                float YB;
                float ZB;
                float xA;
                float yA;
                float zA;
                float XC;
                float YC;
                float ZC;
                if ((plainTile.functionMask & COMPLETELY_FLAT) != 0) {
                    float tileHeight = super.tileHeights[x][y];
                    float nearB = this.vertexZNearB * tileHeight;
                    float farB = this.vertexZFarB * tileHeight;
                    
                    float nearZ = this.vertexZNearD + this.vertexZNearA * tileunitsStartX + nearB + this.vertexZNearC * tileunitsStartY;
                    farZA = this.vertexZFarD + this.vertexZFarA * tileunitsStartX + farB + this.vertexZFarC * tileunitsStartY;
                    if (nearZ < -farZA) {
                        return;
                    }

                    float f_35 = this.vertexZNearD + this.vertexZNearA * tileunitsEndX + nearB + this.vertexZNearC * tileunitsStartY;
                    farZB = this.vertexZFarD + this.vertexZFarA * tileunitsEndX + farB + this.vertexZFarC * tileunitsStartY;
                    if (f_35 < -farZB) {
                        return;
                    }

                    float f_36 = this.vertexZNearD + this.vertexZNearA * tileunitsEndX + nearB + this.vertexZNearC * tileunitsEndY;
                    farZC = this.vertexZFarD + this.vertexZFarA * tileunitsEndX + farB + this.vertexZFarC * tileunitsEndY;
                    if (f_36 < -farZC) {
                        return;
                    }

                    float f_37 = this.vertexZNearD + this.vertexZNearA * tileunitsStartX + nearB + this.vertexZNearC * tileunitsEndY;
                    farZD = this.vertexZFarD + this.vertexZFarA * tileunitsStartX + farB + this.vertexZFarC * tileunitsEndY;
                    if (f_37 < -farZD) {
                        return;
                    }

                    float heightXB = this.vertexXB * tileHeight;
                    float heightYB = this.vertexYB * tileHeight;
                    
                    float f_40 = this.vertexXD + this.vertexXA * tileunitsStartX + heightXB + this.vertexXC * tileunitsStartY;
                    XA = rasterizer.centerX + rasterizer.offsetX * f_40 / farZA;
                    float f_41 = this.vertexYD + this.vertexYA * tileunitsStartX + heightYB + this.vertexYC * tileunitsStartY;
                    YA = rasterizer.centerY + rasterizer.offsetY * f_41 / farZA;
                    ZA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZ / farZA;
                    
                    float f_42 = this.vertexXD + this.vertexXA * tileunitsEndX + heightXB + this.vertexXC * tileunitsStartY;
                    XB = rasterizer.centerX + rasterizer.offsetX * f_42 / farZB;
                    float f_43 = this.vertexYD + this.vertexYA * tileunitsEndX + heightYB + this.vertexYC * tileunitsStartY;
                    YB = rasterizer.centerY + rasterizer.offsetY * f_43 / farZB;
                    ZB = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_35 / farZB;
                    
                    float f_44 = this.vertexXD + this.vertexXA * tileunitsEndX + heightXB + this.vertexXC * tileunitsEndY;
                    xA = rasterizer.centerX + rasterizer.offsetX * f_44 / farZC;
                    float f_45 = this.vertexYD + this.vertexYA * tileunitsEndX + heightYB + this.vertexYC * tileunitsEndY;
                    yA = rasterizer.centerY + rasterizer.offsetY * f_45 / farZC;
                    zA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_36 / farZC;
                    
                    float f_46 = this.vertexXD + this.vertexXA * tileunitsStartX + heightXB + this.vertexXC * tileunitsEndY;
                    XC = rasterizer.centerX + rasterizer.offsetX * f_46 / farZD;
                    float f_47 = this.vertexYD + this.vertexYA * tileunitsStartX + heightYB + this.vertexYC * tileunitsEndY;
                    YC = rasterizer.centerY + rasterizer.offsetY * f_47 / farZD;
                    ZC = rasterizer.aFloat1682 + rasterizer.aFloat1683 * f_37 / farZD;
                    
                } else {
                    
                	int heightNorthWest = super.tileHeights[x][y];
                    int heightNorthEast = super.tileHeights[x + 1][y];
                    int heightSouthEast = super.tileHeights[x + 1][y + 1];
                    int heightSouthWest = super.tileHeights[x][y + 1];
                    
                    float nearZA = this.vertexZNearD + this.vertexZNearA * tileunitsStartX + this.vertexZNearB * heightNorthWest + this.vertexZNearC * tileunitsStartY;
                    farZA = this.vertexZFarD + this.vertexZFarA * tileunitsStartX + this.vertexZFarB * heightNorthWest + this.vertexZFarC * tileunitsStartY;
                    if (nearZA < -farZA) {
                        return;
                    }

                    float nearZB = this.vertexZNearD + this.vertexZNearA * tileunitsEndX + this.vertexZNearB * heightNorthEast + this.vertexZNearC * tileunitsStartY;
                    farZB = this.vertexZFarD + this.vertexZFarA * tileunitsEndX + this.vertexZFarB * heightNorthEast + this.vertexZFarC * tileunitsStartY;
                    if (nearZB < -farZB) {
                        return;
                    }

                    float nearZC = this.vertexZNearD + this.vertexZNearA * tileunitsEndX + this.vertexZNearB * heightSouthEast + this.vertexZNearC * tileunitsEndY;
                    farZC = this.vertexZFarD + this.vertexZFarA * tileunitsEndX + this.vertexZFarB * heightSouthEast + this.vertexZFarC * tileunitsEndY;
                    if (nearZC < -farZC) {
                        return;
                    }

                    float nearZD = this.vertexZNearD + this.vertexZNearA * tileunitsStartX + this.vertexZNearB * heightSouthWest + this.vertexZNearC * tileunitsEndY;
                    farZD = this.vertexZFarD + this.vertexZFarA * tileunitsStartX + this.vertexZFarB * heightSouthWest + this.vertexZFarC * tileunitsEndY;
                    if (nearZD < -farZD) {
                        return;
                    }

                    float f_39 = this.vertexXD + this.vertexXA * tileunitsStartX + this.vertexXB * heightNorthWest + this.vertexXC * tileunitsStartY;
                    XA = rasterizer.centerX + rasterizer.offsetX * f_39 / farZA;
                    float f_40 = this.vertexYD + this.vertexYA * tileunitsStartX + this.vertexYB * heightNorthWest + this.vertexYC * tileunitsStartY;
                    YA = rasterizer.centerY + rasterizer.offsetY * f_40 / farZA;
                    ZA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZA / farZA;
                    
                    float f_41 = this.vertexXD + this.vertexXA * tileunitsEndX + this.vertexXB * heightNorthEast + this.vertexXC * tileunitsStartY;
                    XB = rasterizer.centerX + rasterizer.offsetX * f_41 / farZB;
                    float f_42 = this.vertexYD + this.vertexYA * tileunitsEndX + this.vertexYB * heightNorthEast + this.vertexYC * tileunitsStartY;
                    YB = rasterizer.centerY + rasterizer.offsetY * f_42 / farZB;
                    ZB = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZB / farZB;
                    
                    float f_43 = this.vertexXD + this.vertexXA * tileunitsEndX + this.vertexXB * heightSouthEast + this.vertexXC * tileunitsEndY;
                    xA = rasterizer.centerX + rasterizer.offsetX * f_43 / farZC;
                    float f_44 = this.vertexYD + this.vertexYA * tileunitsEndX + this.vertexYB * heightSouthEast + this.vertexYC * tileunitsEndY;
                    yA = rasterizer.centerY + rasterizer.offsetY * f_44 / farZC;
                    zA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZC / farZC;
                    
                    float f_45 = this.vertexXD + this.vertexXA * tileunitsStartX + this.vertexXB * heightSouthWest + this.vertexXC * tileunitsEndY;
                    XC = rasterizer.centerX + rasterizer.offsetX * f_45 / farZD;
                    float f_46 = this.vertexYD + this.vertexYA * tileunitsStartX + this.vertexYB * heightSouthWest + this.vertexYC * tileunitsEndY;
                    YC = rasterizer.centerY + rasterizer.offsetY * f_46 / farZD;
                    ZC = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZD / farZD;
                }

                if ((xA - XC) * (YB - YC) - (yA - YC) * (XB - XC) > 0.0F) {
                    rasterizer.clipEdges = xA < 0.0F || XC < 0.0F || XB < 0.0F || xA > rasterizer.clipWidth || XC > rasterizer.clipWidth || XB > rasterizer.clipWidth;
                    if (plainTile.materialId >= 0) {
                        rasterizer.drawMaterializedTriangle(true, true, false, yA, YC, YB, xA, XC, XB, zA, ZC, ZB, farZC, farZD, farZB, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, ~0xffffff | ColorUtil.getHsvToRgbTable()[plainTile.colorNorthEast & 0xffff] & 0xffffff, ~0xffffff | ColorUtil.getHsvToRgbTable()[plainTile.colorNorthWest & 0xffff] & 0xffffff, ~0xffffff | ColorUtil.getHsvToRgbTable()[plainTile.colorSouthEast & 0xffff] & 0xffffff, 0, 0.0F, 0.0F, 0.0F, plainTile.materialId);
                    } else {
                        rasterizer.drawShadedTriangle(true, true, false, yA, YC, YB, xA, XC, XB, zA, ZC, ZB, (plainTile.colorNorthEast & 0xffff), (plainTile.colorNorthWest & 0xffff), (plainTile.colorSouthEast & 0xffff));
                    }
                }

                if ((XA - XB) * (YC - YB) - (YA - YB) * (XC - XB) > 0.0F) {
                    rasterizer.clipEdges = XA < 0.0F || XB < 0.0F || XC < 0.0F || XA > rasterizer.clipWidth || XB > rasterizer.clipWidth || XC > rasterizer.clipWidth;
                    if (plainTile.materialId >= 0) {
                        rasterizer.drawMaterializedTriangle(true, true, false, YA, YB, YC, XA, XB, XC, ZA, ZB, ZC, farZA, farZB, farZD, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, ~0xffffff | ColorUtil.getHsvToRgbTable()[plainTile.colorSouthWest & 0xffff] & 0xffffff, ~0xffffff | ColorUtil.getHsvToRgbTable()[plainTile.colorSouthEast & 0xffff] & 0xffffff, ~0xffffff | ColorUtil.getHsvToRgbTable()[plainTile.colorNorthWest & 0xffff] & 0xffffff, 0, 0.0F, 0.0F, 0.0F, plainTile.materialId);
                    } else {
                        rasterizer.drawShadedTriangle(true, true, false, YA, YB, YC, XA, XB, XC, ZA, ZB, ZC, (plainTile.colorSouthWest & 0xffff), (plainTile.colorSouthEast & 0xffff), (plainTile.colorNorthWest & 0xffff));
                    }
                }
            }
        } else {
            ShapedTile shapedTile = this.sceneShapedTiles[x][y];
            if (shapedTile != null) {
                if (tileFlags != 0) {
                    if ((shapedTile.functionMask & TEXTURE_MOVEMENT) != 0) {
                        if ((tileFlags & COMPLETELY_FLAT) != 0) {
                            return;
                        }
                    } else if ((tileFlags & HD_LIGHT) != 0) {
                        return;
                    }
                }

                for (int triangleIndex = 0; triangleIndex < shapedTile.count; triangleIndex++) {
                    int offsetX = shapedTile.offsetX[triangleIndex] + (x << super.tileScale);
                    int offsetY = shapedTile.offsetY[triangleIndex] + (y << super.tileScale);
                    short offsetZ = shapedTile.offsetZ[triangleIndex];
                    float nearZ = this.vertexZNearD + this.vertexZNearA * offsetX + this.vertexZNearB * offsetZ + this.vertexZNearC * offsetY;
                    float farZ = this.vertexZFarD + this.vertexZFarA * offsetX + this.vertexZFarB * offsetZ + this.vertexZFarC * offsetY;
                    if (nearZ < -farZ) {
                        return;
                    }

                    float viewX = this.vertexXD + this.vertexXA * offsetX + this.vertexXB * offsetZ + this.vertexXC * offsetY;
                    float viewY = this.vertexYD + this.vertexYA * offsetX + this.vertexYB * offsetZ + this.vertexYC * offsetY;
                    perspectiveX[triangleIndex] = rasterizer.centerX + rasterizer.offsetX * viewX / farZ;
                    perspectiveY[triangleIndex] = rasterizer.centerY + rasterizer.offsetY * viewY / farZ;
                    perspectiveZNear[triangleIndex] = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZ / farZ;
                    perspectiveZFar[triangleIndex] = farZ;
                }

                if (shapedTile.primaryMaterialIds != null) {
                    for (int triangleIndex = 0; triangleIndex < shapedTile.triangleCount; triangleIndex++) {
                        short a = shapedTile.triangleA[triangleIndex];
                        short b = shapedTile.triangleB[triangleIndex];
                        short c = shapedTile.triangleC[triangleIndex];
                        float xA = perspectiveX[a];
                        float xB = perspectiveX[b];
                        float xC = perspectiveX[c];
                        float yA = perspectiveY[a];
                        float yB = perspectiveY[b];
                        float yC = perspectiveY[c];
                        if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0F) {
                            rasterizer.clipEdges = xA < 0.0F || xB < 0.0F || xC < 0.0F || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth;
                            short materialA = shapedTile.primaryMaterialIds[triangleIndex];
                            if (materialA != -1) {
                                rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZNear[a], perspectiveZNear[b], perspectiveZNear[c], perspectiveZFar[a], perspectiveZFar[b], perspectiveZFar[c], (float) shapedTile.offsetX[a] / super.tileUnits, (float) shapedTile.offsetX[b] / super.tileUnits, (float) shapedTile.offsetX[c] / super.tileUnits, (float) shapedTile.offsetY[a] / super.tileUnits, (float) shapedTile.offsetY[b] / super.tileUnits, (float) shapedTile.offsetY[c] / super.tileUnits, ~0xffffff | ColorUtil.getHsvToRgbTable()[shapedTile.light[a] & 0xffff] & 0xffffff, ~0xffffff | ColorUtil.getHsvToRgbTable()[shapedTile.light[b] & 0xffff] & 0xffffff, ~0xffffff | ColorUtil.getHsvToRgbTable()[shapedTile.light[c] & 0xffff] & 0xffffff, 0, 0.0F, 0.0F, 0.0F, materialA);
                            } else {
                                int primaryColor = shapedTile.primaryColors[triangleIndex];
                                if (primaryColor != -1) {
                                    rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZNear[a], perspectiveZNear[b], perspectiveZNear[c], ColorUtil.repackHsl(primaryColor, shapedTile.light[a]), ColorUtil.repackHsl(primaryColor, shapedTile.light[b]), ColorUtil.repackHsl(primaryColor, shapedTile.light[c]));
                                }
                            }
                        }
                    }
                } else {
                    for (int triangleIndex = 0; triangleIndex < shapedTile.triangleCount; triangleIndex++) {
                        short a = shapedTile.triangleA[triangleIndex];
                        short b = shapedTile.triangleB[triangleIndex];
                        short c = shapedTile.triangleC[triangleIndex];
                        float xA = perspectiveX[a];
                        float xB = perspectiveX[b];
                        float xC = perspectiveX[c];
                        float yA = perspectiveY[a];
                        float yB = perspectiveY[b];
                        float yC = perspectiveY[c];
                        if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0F) {
                            int primaryColor = shapedTile.primaryColors[triangleIndex];
                            if (primaryColor != -1) {
                                rasterizer.clipEdges = xA < 0.0F || xB < 0.0F || xC < 0.0F || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth;
                                rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZNear[a], perspectiveZNear[b], perspectiveZNear[c], ColorUtil.repackHsl(primaryColor, shapedTile.light[a]), ColorUtil.repackHsl(primaryColor, shapedTile.light[b]), ColorUtil.repackHsl(primaryColor, shapedTile.light[c]));
                            }
                        }
                    }
                }
            }
        }

    }


    public void render(int x, int y) {
        ThreadResourceSM thread = this.toolkit.getThreadResource(Thread.currentThread());
        thread.rasterizer.blendingAlpha = 0;
        if (this.plainBlendedTiles != null) {
            renderBlendedUntexturedTile(x, y, thread.hasWaterUpdate, thread, thread.rasterizer, thread.perspectiveX, thread.perspectiveY, thread.perspectiveZNear, thread.perspectiveZFar, thread.aFloatArray2345, 0);
        } else if (this.scenePlainTiles != null) {
        	renderBlockTile(x, y, thread.rasterizer, thread.perspectiveX, thread.perspectiveY, thread.perspectiveZNear, thread.perspectiveZFar, 0);
        } else if (this.blendedTexturedTiles != null) {
        	renderBlendedTexturesTile(x, y, thread.hasWaterUpdate, thread, thread.rasterizer, thread.perspectiveX, thread.perspectiveY, thread.perspectiveZNear, thread.perspectiveZFar, thread.aFloatArray2345, 0);
        }

    }

    @Override
    public void addLight(PointLight light, int[] ints_2) {
    }

}
