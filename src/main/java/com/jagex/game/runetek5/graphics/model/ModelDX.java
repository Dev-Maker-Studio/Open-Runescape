package com.jagex.game.runetek5.graphics.model;

import sun.misc.Unsafe;

import java.nio.ByteBuffer;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.buffer.NormalsBufferDX;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.graphics.model.buffer.ModelBufferDX;
import com.jagex.game.runetek5.graphics.model.normal.FaceNormalDX;
import com.jagex.game.runetek5.graphics.model.normal.ModelNormalsDX;
import com.jagex.game.runetek5.graphics.model.normal.VertexNormalDX;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardSurfaceDX;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardEffectDX;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader;
import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeWaterfall;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.graphics.shadow.ShadowDX;
import com.jagex.game.runetek5.graphics.texture.TextureTransformationMatrix;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.utils.ArrayUtils;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.MathUtils;

public class ModelDX extends Model {

    public static final float[] UV_MAP = new float[2];
    public static int[] anIntArray8928 = new int[1];
    public static int[] anIntArray8958 = new int[1];
    public static final int[] PROJ_X = new int[8];
    public static final int[] PROJ_Y = new int[8];
    public static final int[] PROJ_Z = new int[8];
    public static long[] HASHED_NORMALS;
    public static int originX;
    public static int originY;
    public static int originZ;
    public static boolean withinOrigin;
    public boolean textureAnimating;
    public int anInt8906;
    public int activeFunctionMask;
    public int preferenceFunctionMask;
    public short ambient;
    public short contast;
    public int numVertices;
    public int maxDepth;
    public int numFaces;
    public int anInt8963;
    public int numBillboards;
    public boolean accountForPriorities;
    public int[] verticesX;
    public int[] verticesY;
    public int[] verticesZ;
    public ModelBufferDX verticeManager;
    public short[] facesColor;
    public byte[] facesAlpha;
    public ModelBufferDX aClass638913;
    public ModelNormalsDX aClass3558962;
    public short[] normalsXs;
    public short[] normalsYs;
    public short[] normalsZs;
    public byte[] normalsMagnitudes;
    public ModelBufferDX lightManager;
    public float[] activeUvU;
    public float[] activeUvV;
    public ModelBufferDX aClass638930;
    public short[] normalsFaceA;
    public short[] normalsFaceB;
    public short[] normalsFaceC;
    public NormalsBufferDX normalsBuffer;
    public short[] facesMaterial;
    public BillboardSurfaceDX[] billboardBackgrounds;
    public BillboardEffectDX[] billboardEffects;
    public boolean boundsUpdated;
    public int size3d;
    public int size2d;
    public int minX;
    public int maxX;
    public int minY;
    public int maxY;
    public int minZ;
    public int maxZ;
    public int[][] verticesByLabel;
    public int[][] facesByLabel;
    public int[][] billboardsByLabel;
    public short[] aShortArray8948;
    public int[] totalFaceCounts;
    public short[] vertexIndices;
    public short[] alphaIndices;
    public short[] verticesSubmeshes;
    public short[] facesSubmeshes;
    public int[] anIntArray8944;
    public int[] anIntArray8946;
    public int[] anIntArray8952;
    public EmissiveTriangle[] aClass87Array8949;
    public EffectiveVertex[] aClass172Array8950;
    public boolean aBool8919;
    public boolean aBool8904;
    public final AbstractNativeToolkit toolkit;

    public ModelDX(AbstractNativeToolkit nativeToolkit, Mesh mesh, int flags, int setAmbient, int setContrast, int i_6) {
        this(nativeToolkit, flags, i_6, true, false);
        MaterialProvider materialProvider = nativeToolkit.materialProvider;
        int[] faceIndices = new int[mesh.numFaces];
        this.totalFaceCounts = new int[mesh.maxDepth + 1];

        for (int index = 0; index < mesh.numFaces; index++) {
            if (mesh.faceRenderType == null || mesh.faceRenderType[index] != 2) {
                if (mesh.facesMaterial != null && mesh.facesMaterial[index] != -1) {
                    MaterialType material = materialProvider.getMaterial(mesh.facesMaterial[index] & 0xffff);
                    if (((this.preferenceFunctionMask & 0x40) == 0 || !material.detailsOnly) && material.skipTriangles) {
                        continue;
                    }
                }

                faceIndices[this.numFaces++] = index;
                this.totalFaceCounts[mesh.facesA[index]]++;
                this.totalFaceCounts[mesh.facesB[index]]++;
                this.totalFaceCounts[mesh.facesC[index]]++;
            }
        }

        this.anInt8963 = this.numFaces;
        long[] billboardUids = new long[this.numFaces];
        boolean hasPriorities = (this.activeFunctionMask & 0x100) != 0;

        for (int index = 0; index < this.numFaces; index++) {
            int face = faceIndices[index];
            MaterialType material = null;
            int billboardLayer = 0;
            byte b_67 = 0;
            byte materialEffectId = 0;
            byte materialEffectParam1 = 0;
            if (mesh.billboards != null) {
                boolean hasUid = false;

                for (int texture = 0; texture < mesh.billboards.length; texture++) {
                    Billboard billboard = mesh.billboards[texture];
                    if (face == billboard.appliesOnFace) {
                        BillboardType billboardType = BillboardTypeList.list(billboard.billboardId);
                        if (billboardType.hasUid) {
                            hasUid = true;
                        }

                        if (billboardType.materialId != -1) {
                        	MaterialType billboardMaterial = materialProvider.getMaterial(billboardType.materialId);
                            if (billboardMaterial.effectCombiner == 2) {
                                this.accountForPriorities = true;
                            }
                        }
                    }
                }

                if (hasUid) {
                    billboardUids[index] = Long.MAX_VALUE;
                    this.anInt8963--;
                    continue;
                }
            }

            int materialId = -1;
            if (mesh.facesMaterial != null) {
                materialId = mesh.facesMaterial[face];
                if (materialId != -1) {
                    material = materialProvider.getMaterial(materialId & 0xffff);
                    if ((this.preferenceFunctionMask & 0x40) != 0 && material.detailsOnly) {
                        materialId = -1;
                        material = null;
                    } else {
                        materialEffectId = material.effectId;
                        materialEffectParam1 = material.effectParam1;
                    }
                }
            }

            boolean considerLayers = mesh.facesAlpha != null && mesh.facesAlpha[face] != 0 || material != null && material.effectCombiner != 0;
            if ((hasPriorities || considerLayers) && mesh.facesLayer != null) {
                billboardLayer += mesh.facesLayer[face] << 17;
            }

            if (considerLayers) {
                billboardLayer += 65536;
            }

            billboardLayer += (materialEffectId & 0xff) << 8;
            billboardLayer += materialEffectParam1 & 0xff;
            int billboardFace = b_67 + ((materialId & 0xffff) << 16);
            billboardFace += index & 0xffff;
            billboardUids[index] = ((long) billboardLayer << 32) + billboardFace;
            this.accountForPriorities |= considerLayers;
            this.textureAnimating |= material != null && (material.textureSpeedU != 0 || material.textureSpeedV != 0);
        }

        Model.addBillboard(billboardUids, faceIndices);
        this.numVertices = mesh.numVertices;
        this.maxDepth = mesh.maxDepth;
        this.verticesX = mesh.verticesX;
        this.verticesY = mesh.verticesY;
        this.verticesZ = mesh.verticesZ;
        this.verticesSubmeshes = mesh.verticesSubmeshes;
        VertexNormalDX[] vertexNormals = new VertexNormalDX[this.maxDepth];
        this.aClass87Array8949 = mesh.particleEmitters;
        this.aClass172Array8950 = mesh.particleEffectors;
        if (mesh.billboards != null) {
            this.numBillboards = mesh.billboards.length;
            this.billboardEffects = new BillboardEffectDX[this.numBillboards];
            this.billboardBackgrounds = new BillboardSurfaceDX[this.numBillboards];

            for (int index = 0; index < this.numBillboards; index++) {
                Billboard billboard = mesh.billboards[index];
                BillboardType billboardType = BillboardTypeList.list(billboard.billboardId);
                int face = -1;

                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    if (billboard.appliesOnFace == faceIndices[faceIndex]) {
                        face = faceIndex;
                        break;
                    }
                }

                if (face == -1) {
                    throw new RuntimeException();
                }

                int rgbOnFace = ColorUtil.getHslToRgb()[mesh.facesColor[billboard.appliesOnFace] & 0xffff] & 0xffffff;
                rgbOnFace |= 255 - (mesh.facesAlpha != null ? mesh.facesAlpha[billboard.appliesOnFace] : 0) << 24;
                this.billboardEffects[index] = new BillboardEffectDX(face, mesh.facesA[billboard.appliesOnFace], mesh.facesB[billboard.appliesOnFace], mesh.facesC[billboard.appliesOnFace], billboardType.size2d, billboardType.size3d, billboardType.materialId, billboardType.stationary, billboard.distance);
                this.billboardBackgrounds[index] = new BillboardSurfaceDX(rgbOnFace);
            }
        }

        int i_12 = this.numFaces * 3;
        this.vertexIndices = new short[i_12];
        this.alphaIndices = new short[i_12];
        this.normalsXs = new short[i_12];
        this.normalsYs = new short[i_12];
        this.normalsZs = new short[i_12];
        this.normalsMagnitudes = new byte[i_12];
        this.activeUvU = new float[i_12];
        this.activeUvV = new float[i_12];
        this.facesColor = new short[this.numFaces];
        this.facesAlpha = new byte[this.numFaces];
        this.normalsFaceA = new short[this.numFaces];
        this.normalsFaceB = new short[this.numFaces];
        this.normalsFaceC = new short[this.numFaces];
        this.facesMaterial = new short[this.numFaces];
        if (mesh.facesSubmeshes != null) {
            this.facesSubmeshes = new short[this.numFaces];
        }

        this.ambient = (short) setAmbient;
        this.contast = (short) setContrast;
        this.aShortArray8948 = new short[i_12];
        HASHED_NORMALS = new long[i_12];
        int faceCount = 0;

        for (int index = 0; index < mesh.maxDepth; index++) {
            int count = this.totalFaceCounts[index];
            this.totalFaceCounts[index] = faceCount;
            faceCount += count;
            vertexNormals[index] = new VertexNormalDX();
        }

        this.totalFaceCounts[mesh.maxDepth] = faceCount;
        TextureTransformationMatrix textureTransformMatrix = generateTextureMatrix(mesh, faceIndices, this.numFaces);
        FaceNormalDX[] faceNormals = new FaceNormalDX[mesh.numFaces];

        for (int index = 0; index < mesh.numFaces; index++) {
            short a = mesh.facesA[index];
            short b = mesh.facesB[index];
            short c = mesh.facesC[index];
            int xBa = this.verticesX[b] - this.verticesX[a];
            int yBa = this.verticesY[b] - this.verticesY[a];
            int zBa = this.verticesZ[b] - this.verticesZ[a];
            int xCa = this.verticesX[c] - this.verticesX[a];
            int yCa = this.verticesY[c] - this.verticesY[a];
            int zCa = this.verticesZ[c] - this.verticesZ[a];
            int deltaX = yBa * zCa - yCa * zBa;
            int deltaY = zBa * xCa - zCa * xBa;

            int deltaZ;
            for (deltaZ = xBa * yCa - xCa * yBa; deltaX > 8192 || deltaY > 8192 || deltaZ > 8192 || deltaX < -8192 || deltaY < -8192 || deltaZ < -8192; deltaZ >>= 1) {
                deltaX >>= 1;
                deltaY >>= 1;
            }

            int i_29 = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (i_29 <= 0) {
                i_29 = 1;
            }

            deltaX = deltaX * 256 / i_29;
            deltaY = deltaY * 256 / i_29;
            deltaZ = deltaZ * 256 / i_29;
            byte renderType = mesh.faceRenderType == null ? 0 : mesh.faceRenderType[index];
            if (renderType == 0) {
                VertexNormalDX vertexNormal = vertexNormals[a];
                vertexNormal.x += deltaX;
                vertexNormal.y += deltaY;
                vertexNormal.z += deltaZ;
                vertexNormal.magnitude++;
                vertexNormal = vertexNormals[b];
                vertexNormal.x += deltaX;
                vertexNormal.y += deltaY;
                vertexNormal.z += deltaZ;
                vertexNormal.magnitude++;
                vertexNormal = vertexNormals[c];
                vertexNormal.x += deltaX;
                vertexNormal.y += deltaY;
                vertexNormal.z += deltaZ;
                vertexNormal.magnitude++;
            } else if (renderType == 1) {
                FaceNormalDX faceNormal = faceNormals[index] = new FaceNormalDX();
                faceNormal.x = deltaX;
                faceNormal.y = deltaY;
                faceNormal.z = deltaZ;
            }
        }

        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            int index = faceIndices[faceIndex];
            int faceColor = mesh.facesColor[index] & 0xffff;
            
            int texture;
            if (mesh.facesTexture == null) {
                texture = -1;
            } else {
                texture = mesh.facesTexture[index];
            }

            int alpha;
            if (mesh.facesAlpha == null) {
                alpha = 0;
            } else {
                alpha = mesh.facesAlpha[index] & 0xff;
            }

            short materialId = mesh.facesMaterial == null ? -1 : mesh.facesMaterial[index];
            if (materialId != -1 && (this.preferenceFunctionMask & 0x40) != 0) {
            	MaterialType material = materialProvider.getMaterial(materialId & 0xffff);
                if (material.detailsOnly) {
                    materialId = -1;
                }
            }

            float uA = 0.0F;
            float vA = 0.0F;
            float uB = 0.0F;
            float vB = 0.0F;
            float uC = 0.0F;
            float vC = 0.0F;
            byte offB = 0;
            byte offC = 0;
            int textureType = 0;
            byte renderType;
            if (materialId != -1) {
                if (texture == -1) {
                    uA = 0.0F;
                    vA = 1.0F;
                    uB = 1.0F;
                    vB = 1.0F;
                    offB = 1;
                    uC = 0.0F;
                    vC = 0.0F;
                    offC = 2;
                } else {
                    texture &= 0xff;
                    renderType = mesh.texturesMapping[texture];
                    if (renderType == 0) {
                        short faceA = mesh.facesA[index];
                        short faceB = mesh.facesB[index];
                        short faceC = mesh.facesC[index];
                        short texturedFaceA = mesh.textureFaceX[texture];
                        short texturedFaceB = mesh.textureFaceY[texture];
                        short texturedFaceC = mesh.textureFaceZ[texture];
                        float vertTexXa = mesh.verticesX[texturedFaceA];
                        float vertTexYa = mesh.verticesY[texturedFaceA];
                        float vertTexZa = mesh.verticesZ[texturedFaceA];
                        float vertTexXBa = mesh.verticesX[texturedFaceB] - vertTexXa;
                        float vertTexYBa = mesh.verticesY[texturedFaceB] - vertTexYa;
                        float vertTexZBa = mesh.verticesZ[texturedFaceB] - vertTexZa;
                        float f_44 = mesh.verticesX[texturedFaceC] - vertTexXa;
                        float f_45 = mesh.verticesY[texturedFaceC] - vertTexYa;
                        float f_46 = mesh.verticesZ[texturedFaceC] - vertTexZa;
                        float f_47 = mesh.verticesX[faceA] - vertTexXa;
                        float f_48 = mesh.verticesY[faceA] - vertTexYa;
                        float f_49 = mesh.verticesZ[faceA] - vertTexZa;
                        float f_50 = mesh.verticesX[faceB] - vertTexXa;
                        float f_51 = mesh.verticesY[faceB] - vertTexYa;
                        float f_52 = mesh.verticesZ[faceB] - vertTexZa;
                        float f_53 = mesh.verticesX[faceC] - vertTexXa;
                        float f_54 = mesh.verticesY[faceC] - vertTexYa;
                        float f_55 = mesh.verticesZ[faceC] - vertTexZa;
                        float f_56 = vertTexYBa * f_46 - vertTexZBa * f_45;
                        float f_57 = vertTexZBa * f_44 - vertTexXBa * f_46;
                        float f_58 = vertTexXBa * f_45 - vertTexYBa * f_44;
                        float f_59 = f_45 * f_58 - f_46 * f_57;
                        float f_60 = f_46 * f_56 - f_44 * f_58;
                        float f_61 = f_44 * f_57 - f_45 * f_56;
                        float f_62 = 1.0F / (f_59 * vertTexXBa + f_60 * vertTexYBa + f_61 * vertTexZBa);
                        uA = (f_59 * f_47 + f_60 * f_48 + f_61 * f_49) * f_62;
                        uB = (f_59 * f_50 + f_60 * f_51 + f_61 * f_52) * f_62;
                        uC = (f_59 * f_53 + f_60 * f_54 + f_61 * f_55) * f_62;
                        f_59 = vertTexYBa * f_58 - vertTexZBa * f_57;
                        f_60 = vertTexZBa * f_56 - vertTexXBa * f_58;
                        f_61 = vertTexXBa * f_57 - vertTexYBa * f_56;
                        f_62 = 1.0F / (f_59 * f_44 + f_60 * f_45 + f_61 * f_46);
                        vA = (f_59 * f_47 + f_60 * f_48 + f_61 * f_49) * f_62;
                        vB = (f_59 * f_50 + f_60 * f_51 + f_61 * f_52) * f_62;
                        vC = (f_59 * f_53 + f_60 * f_54 + f_61 * f_55) * f_62;
                    } else {
                        short a = mesh.facesA[index];
                        short b = mesh.facesB[index];
                        short c = mesh.facesC[index];
                        int centerX = textureTransformMatrix.centerX[texture];
                        int centerY = textureTransformMatrix.centerY[texture];
                        int centerZ = textureTransformMatrix.centerZ[texture];
                        float[] matrix = textureTransformMatrix.matrix[texture];
                        byte textureDirection = mesh.texturesDirection[texture];
                        float textureSpeed = mesh.texturesSpeed[texture] / 256.0F;
                        if (renderType == 1) {
                        	float scaleZ = mesh.texturesScaleZ[texture] / 1024.0F;
                            mapCylindericalUv(mesh.verticesX[a], mesh.verticesY[a], mesh.verticesZ[a], centerX, centerY, centerZ, matrix, scaleZ, textureDirection, textureSpeed, UV_MAP);
                            uA = UV_MAP[0];
                            vA = UV_MAP[1];
                            mapCylindericalUv(mesh.verticesX[b], mesh.verticesY[b], mesh.verticesZ[b], centerX, centerY, centerZ, matrix, scaleZ, textureDirection, textureSpeed, UV_MAP);
                            uB = UV_MAP[0];
                            vB = UV_MAP[1];
                            mapCylindericalUv(mesh.verticesX[c], mesh.verticesY[c], mesh.verticesZ[c], centerX, centerY, centerZ, matrix, scaleZ, textureDirection, textureSpeed, UV_MAP);
                            uC = UV_MAP[0];
                            vC = UV_MAP[1];
                            float halfScaleZ = scaleZ / 2.0F;
                            if ((textureDirection & 0x1) == 0) {
                                if (uB - uA > halfScaleZ) {
                                    uB -= scaleZ;
                                    offB = 1;
                                } else if (uA - uB > halfScaleZ) {
                                    uB += scaleZ;
                                    offB = 2;
                                }

                                if (uC - uA > halfScaleZ) {
                                    uC -= scaleZ;
                                    offC = 1;
                                } else if (uA - uC > halfScaleZ) {
                                    uC += scaleZ;
                                    offC = 2;
                                }
                            } else {
                                if (vB - vA > halfScaleZ) {
                                    vB -= scaleZ;
                                    offB = 1;
                                } else if (vA - vB > halfScaleZ) {
                                    vB += scaleZ;
                                    offB = 2;
                                }

                                if (vC - vA > halfScaleZ) {
                                    vC -= scaleZ;
                                    offC = 1;
                                } else if (vA - vC > halfScaleZ) {
                                    vC += scaleZ;
                                    offC = 2;
                                }
                            }
                        } else if (renderType == 2) {
                        	float transU = mesh.texturesUtrans[texture] / 256.0F;
                        	float transV = mesh.texturesVtrans[texture] / 256.0F;
                            int deltaXBa = mesh.verticesX[b] - mesh.verticesX[a];
                            int deltaYBa = mesh.verticesY[b] - mesh.verticesY[a];
                            int deltaZBa = mesh.verticesZ[b] - mesh.verticesZ[a];
                            int deltaXCa = mesh.verticesX[c] - mesh.verticesX[a];
                            int deltaYCa = mesh.verticesY[c] - mesh.verticesY[a];
                            int deltaZCa = mesh.verticesZ[c] - mesh.verticesZ[a];
                            int multiplierA = deltaYBa * deltaZCa - deltaYCa * deltaZBa;
                            int multiplierB = deltaZBa * deltaXCa - deltaZCa * deltaXBa;
                            int multiplierC = deltaXBa * deltaYCa - deltaXCa * deltaYBa;
                            float scaleX = 64.0F / mesh.texturesScaleX[texture];
                            float scaleY = 64.0F / mesh.texturesScaleY[texture];
                            float scaleZ = 64.0F / mesh.texturesScaleZ[texture];
                            float x = (multiplierA * matrix[0] + multiplierB * matrix[1] + multiplierC * matrix[2]) / scaleX;
                            float y = (multiplierA * matrix[3] + multiplierB * matrix[4] + multiplierC * matrix[5]) / scaleY;
                            float z = (multiplierA * matrix[6] + multiplierB * matrix[7] + multiplierC * matrix[8]) / scaleZ;
                            textureType = getCubeFace(x, y, z);
                            mapRectangularUv(mesh.verticesX[a], mesh.verticesY[a], mesh.verticesZ[a], centerX, centerY, centerZ, textureType, matrix, textureDirection, textureSpeed, transU, transV, UV_MAP);
                            uA = UV_MAP[0];
                            vA = UV_MAP[1];
                            mapRectangularUv(mesh.verticesX[b], mesh.verticesY[b], mesh.verticesZ[b], centerX, centerY, centerZ, textureType, matrix, textureDirection, textureSpeed, transU, transV, UV_MAP);
                            uB = UV_MAP[0];
                            vB = UV_MAP[1];
                            mapRectangularUv(mesh.verticesX[c], mesh.verticesY[c], mesh.verticesZ[c], centerX, centerY, centerZ, textureType, matrix, textureDirection, textureSpeed, transU, transV, UV_MAP);
                            uC = UV_MAP[0];
                            vC = UV_MAP[1];
                        } else if (renderType == 3) {
                            mapSphericalUv(mesh.verticesX[a], mesh.verticesY[a], mesh.verticesZ[a], centerX, centerY, centerZ, matrix, textureDirection, textureSpeed, UV_MAP);
                            uA = UV_MAP[0];
                            vA = UV_MAP[1];
                            mapSphericalUv(mesh.verticesX[b], mesh.verticesY[b], mesh.verticesZ[b], centerX, centerY, centerZ, matrix, textureDirection, textureSpeed, UV_MAP);
                            uB = UV_MAP[0];
                            vB = UV_MAP[1];
                            mapSphericalUv(mesh.verticesX[c], mesh.verticesY[c], mesh.verticesZ[c], centerX, centerY, centerZ, matrix, textureDirection, textureSpeed, UV_MAP);
                            uC = UV_MAP[0];
                            vC = UV_MAP[1];
                            if ((textureDirection & 0x1) == 0) {
                                if (uB - uA > 0.5F) {
                                    --uB;
                                    offB = 1;
                                } else if (uA - uB > 0.5F) {
                                    ++uB;
                                    offB = 2;
                                }

                                if (uC - uA > 0.5F) {
                                    --uC;
                                    offC = 1;
                                } else if (uA - uC > 0.5F) {
                                    ++uC;
                                    offC = 2;
                                }
                            } else {
                                if (vB - vA > 0.5F) {
                                    --vB;
                                    offB = 1;
                                } else if (vA - vB > 0.5F) {
                                    ++vB;
                                    offB = 2;
                                }

                                if (vC - vA > 0.5F) {
                                    --vC;
                                    offC = 1;
                                } else if (vA - vC > 0.5F) {
                                    ++vC;
                                    offC = 2;
                                }
                            }
                        }
                    }
                }
            }

            if (mesh.faceRenderType == null) {
                renderType = 0;
            } else {
                renderType = mesh.faceRenderType[index];
            }

            if (renderType == 0) {
                long hash = ((long) texture << 2) + ((long) ((long) textureType << 24) + ((long) faceColor << 8) + alpha << 32);
                short a = mesh.facesA[index];
                short b = mesh.facesB[index];
                short c = mesh.facesC[index];
                VertexNormalDX vertexNormal = vertexNormals[a];
                this.normalsFaceA[faceIndex] = addNormal(a, faceIndex, hash, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, uA, vA);
                vertexNormal = vertexNormals[b];
                this.normalsFaceB[faceIndex] = addNormal(b, faceIndex, hash + offB, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, uB, vB);
                vertexNormal = vertexNormals[c];
                this.normalsFaceC[faceIndex] = addNormal(c, faceIndex, hash + offC, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, uC, vC);
            } else if (renderType == 1) {
                FaceNormalDX faceNormal = faceNormals[index];
                long hash = (((long) texture << 2) + (faceNormal.x > 0 ? 1024 : 2048) + ((long) faceNormal.y + 256 << 12) + ((long) faceNormal.z + 256 << 22)) + ((long) ((long) textureType << 24) + ((long) faceColor << 8) + alpha << 32);
                this.normalsFaceA[faceIndex] = addNormal(mesh.facesA[index], faceIndex, hash, faceNormal.x, faceNormal.y, faceNormal.z, 0, uA, vA);
                this.normalsFaceB[faceIndex] = addNormal(mesh.facesB[index], faceIndex, hash + offB, faceNormal.x, faceNormal.y, faceNormal.z, 0, uB, vB);
                this.normalsFaceC[faceIndex] = addNormal(mesh.facesC[index], faceIndex, hash + offC, faceNormal.x, faceNormal.y, faceNormal.z, 0, uC, vC);
            }

            if (mesh.facesAlpha != null) {
                this.facesAlpha[faceIndex] = mesh.facesAlpha[index];
            }

            if (mesh.facesSubmeshes != null) {
                this.facesSubmeshes[faceIndex] = mesh.facesSubmeshes[index];
            }

            this.facesColor[faceIndex] = mesh.facesColor[index];
            this.facesMaterial[faceIndex] = materialId;
        }

        if (this.anInt8963 > 0) {
            int count = 1;
            short a = this.facesMaterial[0];

            for (int index = 0; index < this.anInt8963; index++) {
                short c = this.facesMaterial[index];
                if (c != a) {
                    count++;
                    a = c;
                }
            }

            this.anIntArray8952 = new int[count];
            this.anIntArray8946 = new int[count];
            this.anIntArray8944 = new int[count + 1];
            this.anIntArray8944[0] = 0;
            int i_85 = this.anInt8906;
            int c = 0;
            int zzzz = 0;
            short zzzzzzzz = this.facesMaterial[0];

            for (int index = 0; index < this.anInt8963; index++) {
                short materialId = this.facesMaterial[index];
                if (materialId != zzzzzzzz) {
                    this.anIntArray8952[zzzz] = i_85;
                    this.anIntArray8946[zzzz] = c - i_85 + 1;
                    ++zzzz;
                    this.anIntArray8944[zzzz] = index;
                    i_85 = this.anInt8906;
                    c = 0;
                    zzzzzzzz = materialId;
                }

                short normal = this.normalsFaceA[index];
                if (normal < i_85) {
                    i_85 = normal;
                }

                if (normal > c) {
                    c = normal;
                }

                normal = this.normalsFaceB[index];
                if (normal < i_85) {
                    i_85 = normal;
                }

                if (normal > c) {
                    c = normal;
                }

                normal = this.normalsFaceC[index];
                if (normal < i_85) {
                    i_85 = normal;
                }

                if (normal > c) {
                    c = normal;
                }
            }

            this.anIntArray8952[zzzz] = i_85;
            this.anIntArray8946[zzzz] = c - i_85 + 1;
            zzzz++;
            this.anIntArray8944[zzzz] = this.anInt8963;
        }

        HASHED_NORMALS = null;
        this.vertexIndices = ArrayUtils.arrayCopy(this.vertexIndices, this.anInt8906);
        this.alphaIndices = ArrayUtils.arrayCopy(this.alphaIndices, this.anInt8906);
        this.normalsXs = ArrayUtils.arrayCopy(this.normalsXs, this.anInt8906);
        this.normalsYs = ArrayUtils.arrayCopy(this.normalsYs, this.anInt8906);
        this.normalsZs = ArrayUtils.arrayCopy(this.normalsZs, this.anInt8906);
        this.normalsMagnitudes = ArrayUtils.arrayCopy(this.normalsMagnitudes, this.anInt8906);
        this.activeUvU = ArrayUtils.arrayCopy(this.activeUvU, this.anInt8906);
        this.activeUvV = ArrayUtils.arrayCopy(this.activeUvV, this.anInt8906);
        if (mesh.vertexGroups != null && ModelFunctionMaskDX.is0x20(flags)) {
            this.verticesByLabel = mesh.createVertexGroups(false);
        }

        if (mesh.billboards != null && ModelFunctionMaskDX.is0x400(flags)) {
            this.billboardsByLabel = mesh.createBillboardGroups();
        }

        if (mesh.faceGroups != null && ModelFunctionMaskDX.is0x180(flags)) {
            int faceIndex = 0;
            int[] faceCountMap = new int[256];

            for (int index = 0; index < this.numFaces; index++) {
                int faceByLabel = mesh.faceGroups[faceIndices[index]];
                if (faceByLabel >= 0) {
                    faceCountMap[faceByLabel]++;
                    if (faceByLabel > faceIndex) {
                        faceIndex = faceByLabel;
                    }
                }
            }

            this.facesByLabel = new int[faceIndex + 1][];
            for (int index = 0; index <= faceIndex; index++) {
                this.facesByLabel[index] = new int[faceCountMap[index]];
                faceCountMap[index] = 0;
            }

            for (int index = 0; index < this.numFaces; index++) {
                int faceLabel = mesh.faceGroups[faceIndices[index]];
                if (faceLabel >= 0) {
                    this.facesByLabel[faceLabel][faceCountMap[faceLabel]++] = index;
                }
            }
        }

    }

    public ModelDX(AbstractNativeToolkit nativeToolkit, int functionMask, int preferenceFunctionMask, boolean initializeBuffers, boolean bool5) {
        this.numVertices = 0;
        this.maxDepth = 0;
        this.anInt8906 = 0;
        this.numFaces = 0;
        this.anInt8963 = 0;
        this.aBool8904 = false;
        this.aBool8919 = true;
        this.accountForPriorities = false;
        this.textureAnimating = false;
        this.boundsUpdated = false;
        this.toolkit = nativeToolkit;
        this.activeFunctionMask = functionMask;
        this.preferenceFunctionMask = preferenceFunctionMask;
        this.aBool8904 = bool5;
        if (initializeBuffers || ModelFunctionMaskDX.method990(this.activeFunctionMask)) {
        	this.verticeManager = new ModelBufferDX(ModelFunctionMaskDX.method995(this.activeFunctionMask));
        }

        if (initializeBuffers || ModelFunctionMaskDX.method993(this.activeFunctionMask)) {
        	this.aClass638930 = new ModelBufferDX(ModelFunctionMaskDX.method1056(this.activeFunctionMask));
        }

        if (initializeBuffers || ModelFunctionMaskDX.method1059(this.activeFunctionMask)) {
        	this.aClass638913 = new ModelBufferDX(ModelFunctionMaskDX.method1051(this.activeFunctionMask, this.preferenceFunctionMask));
        }

        if (initializeBuffers || ModelFunctionMaskDX.method983(this.activeFunctionMask, this.preferenceFunctionMask)) {
        	this.lightManager = new ModelBufferDX(ModelFunctionMaskDX.deepCloneLights(this.activeFunctionMask, this.preferenceFunctionMask));
        }

        if (initializeBuffers || ModelFunctionMaskDX.method1043(this.activeFunctionMask)) {
        	this.normalsBuffer = new NormalsBufferDX(ModelFunctionMaskDX.hasNormalsBuffer(this.activeFunctionMask));
        }

        if (initializeBuffers && bool5) {
        	this.verticeManager.anInterface4664 = this.verticeManager.streamSource = this.toolkit.createStream(true);
        	this.aClass638930.anInterface4664 = this.aClass638930.streamSource = this.toolkit.createStream(true);
        	this.aClass638913.anInterface4664 = this.aClass638913.streamSource = this.toolkit.createStream(true);
        	this.lightManager.anInterface4664 = this.lightManager.streamSource = this.toolkit.createStream(true);
        }
    }

    public short addNormal(int face, int faceIndex, long hashedNormal, int normalX, int normalY, int normalZ, int normalMagnitude, float u, float v) {
        int i_12 = this.totalFaceCounts[face];
        int i_13 = this.totalFaceCounts[face + 1];
        int i_14 = 0;

        for (int i_15 = i_12; i_15 < i_13; i_15++) {
            short s_16 = this.aShortArray8948[i_15];
            if (s_16 == 0) {
                i_14 = i_15;
                break;
            }

            if (HASHED_NORMALS[i_15] == hashedNormal) {
                return (short) (s_16 - 1);
            }
        }

        this.aShortArray8948[i_14] = (short) (this.anInt8906 + 1);
        HASHED_NORMALS[i_14] = hashedNormal;
        this.alphaIndices[this.anInt8906] = (short) faceIndex;
        this.vertexIndices[this.anInt8906] = (short) face;
        this.normalsXs[this.anInt8906] = (short) normalX;
        this.normalsYs[this.anInt8906] = (short) normalY;
        this.normalsZs[this.anInt8906] = (short) normalZ;
        this.normalsMagnitudes[this.anInt8906] = (byte) normalMagnitude;
        this.activeUvU[this.anInt8906] = u;
        this.activeUvV[this.anInt8906] = v;
        return (short) (this.anInt8906++);
    }

    @Override
    public Model copy(byte copyIndex, int flags, boolean dummy) {
        boolean unused = false;
        ModelDX copySrc;
        ModelDX copyDst;
        if (copyIndex > 0 && copyIndex <= 7) {
            copyDst = this.toolkit.copyByTypeDst[copyIndex - 1];
            copySrc = this.toolkit.copyByTypeSrc[copyIndex - 1];
            unused = true;
        } else {
            copySrc = copyDst = new ModelDX(this.toolkit, 0, 0, true, false);
        }

        return copyModel(copySrc, copyDst, flags);
    }

    public Model copyModel(ModelDX copySrc, ModelDX copyDst, int flags) {
        copySrc.activeFunctionMask = flags;
        copySrc.preferenceFunctionMask = this.preferenceFunctionMask;
        copySrc.ambient = this.ambient;
        copySrc.contast = this.contast;
        copySrc.numVertices = this.numVertices;
        copySrc.maxDepth = this.maxDepth;
        copySrc.anInt8906 = this.anInt8906;
        copySrc.numFaces = this.numFaces;
        copySrc.anInt8963 = this.anInt8963;
        copySrc.numBillboards = this.numBillboards;
        if ((flags & 0x100) != 0) {
            copySrc.accountForPriorities = true;
        } else {
            copySrc.accountForPriorities = this.accountForPriorities;
        }

        copySrc.textureAnimating = this.textureAnimating;
        boolean copyVerticesX = ModelFunctionMaskDX.copyVerticesX(flags);
        boolean copyVerticesY = ModelFunctionMaskDX.copyVerticesY(flags);
        boolean copyVerticesZ = ModelFunctionMaskDX.copyVerticesZ(flags);
        boolean copyVertices = copyVerticesX | copyVerticesY | copyVerticesZ;
        if (copyVertices) {
            if (copyVerticesX) {
                if (copyDst.verticesX != null && copyDst.verticesX.length >= this.numVertices) {
                    copySrc.verticesX = copyDst.verticesX;
                } else {
                    copySrc.verticesX = copyDst.verticesX = new int[this.numVertices];
                }
            } else {
                copySrc.verticesX = this.verticesX;
            }

            if (copyVerticesY) {
                if (copyDst.verticesY != null && copyDst.verticesY.length >= this.numVertices) {
                    copySrc.verticesY = copyDst.verticesY;
                } else {
                    copySrc.verticesY = copyDst.verticesY = new int[this.numVertices];
                }
            } else {
                copySrc.verticesY = this.verticesY;
            }

            if (copyVerticesZ) {
                if (copyDst.verticesZ != null && copyDst.verticesZ.length >= this.numVertices) {
                    copySrc.verticesZ = copyDst.verticesZ;
                } else {
                    copySrc.verticesZ = copyDst.verticesZ = new int[this.numVertices];
                }
            } else {
                copySrc.verticesZ = this.verticesZ;
            }

            for (int index = 0; index < this.numVertices; index++) {
                if (copyVerticesX) {
                    copySrc.verticesX[index] = this.verticesX[index];
                }

                if (copyVerticesY) {
                    copySrc.verticesY[index] = this.verticesY[index];
                }

                if (copyVerticesZ) {
                    copySrc.verticesZ[index] = this.verticesZ[index];
                }
            }
        } else {
            copySrc.verticesX = this.verticesX;
            copySrc.verticesY = this.verticesY;
            copySrc.verticesZ = this.verticesZ;
        }

        if (ModelFunctionMaskDX.method995(flags)) {
            copySrc.verticeManager = copyDst.verticeManager;
            copySrc.verticeManager.streamSource = this.verticeManager.streamSource;
            copySrc.verticeManager.allocated = this.verticeManager.allocated;
            copySrc.verticeManager.aBool663 = true;
        } else if (ModelFunctionMaskDX.method990(flags)) {
            copySrc.verticeManager = this.verticeManager;
        } else {
            copySrc.verticeManager = null;
        }

        if (ModelFunctionMaskDX.copyColors(flags)) {
            if (copyDst.facesColor != null && copyDst.facesColor.length >= this.numFaces) {
                copySrc.facesColor = copyDst.facesColor;
            } else {
                copySrc.facesColor = copyDst.facesColor = new short[this.numFaces];
            }

            if (this.numFaces >= 0) System.arraycopy(this.facesColor, 0, copySrc.facesColor, 0, this.numFaces);
        } else {
            copySrc.facesColor = this.facesColor;
        }

        if (ModelFunctionMaskDX.copyAlpha(flags)) {
            if (copyDst.facesAlpha != null && copyDst.facesAlpha.length >= this.numFaces) {
                copySrc.facesAlpha = copyDst.facesAlpha;
            } else {
                copySrc.facesAlpha = copyDst.facesAlpha = new byte[this.numFaces];
            }

            if (this.numFaces >= 0) System.arraycopy(this.facesAlpha, 0, copySrc.facesAlpha, 0, this.numFaces);
        } else {
            copySrc.facesAlpha = this.facesAlpha;
        }

        if (ModelFunctionMaskDX.method1051(flags, this.preferenceFunctionMask)) {
            copySrc.aClass638913 = copyDst.aClass638913;
            copySrc.aClass638913.streamSource = this.aClass638913.streamSource;
            copySrc.aClass638913.allocated = this.aClass638913.allocated;
            copySrc.aClass638913.aBool663 = true;
        } else if (ModelFunctionMaskDX.method1059(flags)) {
            copySrc.aClass638913 = this.aClass638913;
        } else {
            copySrc.aClass638913 = null;
        }

        if (ModelFunctionMaskDX.method982(flags)) {
            if (copyDst.normalsXs != null && copyDst.normalsXs.length >= this.anInt8906) {
                copySrc.normalsXs = copyDst.normalsXs;
                copySrc.normalsYs = copyDst.normalsYs;
                copySrc.normalsZs = copyDst.normalsZs;
            } else {
                int i_10 = this.anInt8906;
                copySrc.normalsXs = copyDst.normalsXs = new short[i_10];
                copySrc.normalsYs = copyDst.normalsYs = new short[i_10];
                copySrc.normalsZs = copyDst.normalsZs = new short[i_10];
            }

            if (this.aClass3558962 != null) {
                if (copyDst.aClass3558962 == null) {
                    copyDst.aClass3558962 = new ModelNormalsDX();
                }

                ModelNormalsDX class35512 = copySrc.aClass3558962 = copyDst.aClass3558962;
                if (class35512.normalsX == null || class35512.normalsX.length < this.anInt8906) {
                    class35512.normalsX = new short[this.anInt8906];
                    class35512.normalsY = new short[this.anInt8906];
                    class35512.normalsZ = new short[this.anInt8906];
                    class35512.magnitude = new byte[this.anInt8906];
                }

                for (int i_11 = 0; i_11 < this.anInt8906; i_11++) {
                    copySrc.normalsXs[i_11] = this.normalsXs[i_11];
                    copySrc.normalsYs[i_11] = this.normalsYs[i_11];
                    copySrc.normalsZs[i_11] = this.normalsZs[i_11];
                    class35512.normalsX[i_11] = this.aClass3558962.normalsX[i_11];
                    class35512.normalsY[i_11] = this.aClass3558962.normalsY[i_11];
                    class35512.normalsZ[i_11] = this.aClass3558962.normalsZ[i_11];
                    class35512.magnitude[i_11] = this.aClass3558962.magnitude[i_11];
                }
            } else {
                copySrc.aClass3558962 = null;

                for (int i_10 = 0; i_10 < this.anInt8906; i_10++) {
                    copySrc.normalsXs[i_10] = this.normalsXs[i_10];
                    copySrc.normalsYs[i_10] = this.normalsYs[i_10];
                    copySrc.normalsZs[i_10] = this.normalsZs[i_10];
                }
            }

        } else {
            copySrc.aClass3558962 = this.aClass3558962;
            copySrc.normalsXs = this.normalsXs;
            copySrc.normalsYs = this.normalsYs;
            copySrc.normalsZs = this.normalsZs;
        }
        copySrc.normalsMagnitudes = this.normalsMagnitudes;

        if (ModelFunctionMaskDX.deepCloneLights(flags, this.preferenceFunctionMask)) {
            copySrc.lightManager = copyDst.lightManager;
            copySrc.lightManager.streamSource = this.lightManager.streamSource;
            copySrc.lightManager.allocated = this.lightManager.allocated;
            copySrc.lightManager.aBool663 = true;
        } else if (ModelFunctionMaskDX.method983(flags, this.preferenceFunctionMask)) {
            copySrc.lightManager = this.lightManager;
        } else {
            copySrc.lightManager = null;
        }

        if (ModelFunctionMaskDX.deepCloneUv()) {
            if (copyDst.activeUvU != null && copyDst.activeUvU.length >= this.numFaces) {
                copySrc.activeUvU = copyDst.activeUvU;
                copySrc.activeUvV = copyDst.activeUvV;
            } else {
                int i_10 = this.anInt8906;
                copySrc.activeUvU = copyDst.activeUvU = new float[i_10];
                copySrc.activeUvV = copyDst.activeUvV = new float[i_10];
            }

            for (int i_10 = 0; i_10 < this.anInt8906; i_10++) {
                copySrc.activeUvU[i_10] = this.activeUvU[i_10];
                copySrc.activeUvV[i_10] = this.activeUvV[i_10];
            }
        } else {
            copySrc.activeUvU = this.activeUvU;
            copySrc.activeUvV = this.activeUvV;
        }

        if (ModelFunctionMaskDX.method1056(flags)) {
            copySrc.aClass638930 = copyDst.aClass638930;
            copySrc.aClass638930.streamSource = this.aClass638930.streamSource;
            copySrc.aClass638930.allocated = this.aClass638930.allocated;
            copySrc.aClass638930.aBool663 = true;
        } else if (ModelFunctionMaskDX.method993(flags)) {
            copySrc.aClass638930 = this.aClass638930;
        } else {
            copySrc.aClass638930 = null;
        }

        if (ModelFunctionMaskDX.deepCloneNormals(flags)) {
            if (copyDst.normalsFaceA != null && copyDst.normalsFaceA.length >= this.numFaces) {
                copySrc.normalsFaceA = copyDst.normalsFaceA;
                copySrc.normalsFaceB = copyDst.normalsFaceB;
                copySrc.normalsFaceC = copyDst.normalsFaceC;
            } else {
                copySrc.normalsFaceA = copyDst.normalsFaceA = new short[this.numFaces];
                copySrc.normalsFaceB = copyDst.normalsFaceB = new short[this.numFaces];
                copySrc.normalsFaceC = copyDst.normalsFaceC = new short[this.numFaces];
            }

            for (int index = 0; index < this.numFaces; index++) {
                copySrc.normalsFaceA[index] = this.normalsFaceA[index];
                copySrc.normalsFaceB[index] = this.normalsFaceB[index];
                copySrc.normalsFaceC[index] = this.normalsFaceC[index];
            }
        } else {
            copySrc.normalsFaceA = this.normalsFaceA;
            copySrc.normalsFaceB = this.normalsFaceB;
            copySrc.normalsFaceC = this.normalsFaceC;
        }

        if (ModelFunctionMaskDX.hasNormalsBuffer(flags)) {
            copySrc.normalsBuffer = copyDst.normalsBuffer;
            copySrc.normalsBuffer.anInterface32407 = this.normalsBuffer.anInterface32407;
            copySrc.normalsBuffer.allocated = this.normalsBuffer.allocated;
            copySrc.normalsBuffer.aBool406 = true;
        } else if (ModelFunctionMaskDX.method1043(flags)) {
            copySrc.normalsBuffer = this.normalsBuffer;
        } else {
            copySrc.normalsBuffer = null;
        }

        if (ModelFunctionMaskDX.deepCloneMaterials(flags)) {
            if (copyDst.facesMaterial != null && copyDst.facesMaterial.length >= this.numFaces) {
                copySrc.facesMaterial = copyDst.facesMaterial;
            } else {
                int i_10 = this.numFaces;
                copySrc.facesMaterial = copyDst.facesMaterial = new short[i_10];
            }

            if (this.numFaces >= 0) System.arraycopy(this.facesMaterial, 0, copySrc.facesMaterial, 0, this.numFaces);
        } else {
            copySrc.facesMaterial = this.facesMaterial;
        }

        if (ModelFunctionMaskDX.copyBillboardBackgrounds(flags)) {
            if (copyDst.billboardBackgrounds != null && copyDst.billboardBackgrounds.length >= this.numBillboards) {
                copySrc.billboardBackgrounds = copyDst.billboardBackgrounds;
                for (int index = 0; index < this.numBillboards; index++) {
                    copySrc.billboardBackgrounds[index].copyFrom(this.billboardBackgrounds[index]);
                }
            } else {
                copySrc.billboardBackgrounds = copyDst.billboardBackgrounds = new BillboardSurfaceDX[this.numBillboards];
                for (int index = 0; index < this.numBillboards; index++) {
                    copySrc.billboardBackgrounds[index] = this.billboardBackgrounds[index].copy();
                }
            }
        } else {
            copySrc.billboardBackgrounds = this.billboardBackgrounds;
        }

        copySrc.billboardEffects = this.billboardEffects;
        if (this.boundsUpdated) {
            copySrc.size3d = this.size3d;
            copySrc.size2d = this.size2d;
            copySrc.minX = this.minX;
            copySrc.maxX = this.maxX;
            copySrc.minY = this.minY;
            copySrc.maxY = this.maxY;
            copySrc.minZ = this.minZ;
            copySrc.maxZ = this.maxZ;
            copySrc.boundsUpdated = true;
        } else {
            copySrc.boundsUpdated = false;
        }

        copySrc.verticesByLabel = this.verticesByLabel;
        copySrc.facesByLabel = this.facesByLabel;
        copySrc.billboardsByLabel = this.billboardsByLabel;
        copySrc.aShortArray8948 = this.aShortArray8948;
        copySrc.totalFaceCounts = this.totalFaceCounts;
        copySrc.vertexIndices = this.vertexIndices;
        copySrc.alphaIndices = this.alphaIndices;
        copySrc.verticesSubmeshes = this.verticesSubmeshes;
        copySrc.facesSubmeshes = this.facesSubmeshes;
        copySrc.anIntArray8944 = this.anIntArray8944;
        copySrc.anIntArray8946 = this.anIntArray8946;
        copySrc.anIntArray8952 = this.anIntArray8952;
        copySrc.aClass87Array8949 = this.aClass87Array8949;
        copySrc.aClass172Array8950 = this.aClass172Array8950;
        return copySrc;
    }


    public void setVerticesInactive() {
        if (this.verticeManager != null) {
            this.verticeManager.allocated = false;
        }
    }

    public void method14269() {
        if (this.aClass638913 != null) {
            this.aClass638913.allocated = false;
        }
    }

    @Override
    public void offsetSubmesh(Matrix43 matrix44var1, int submeshFlags, boolean bool3) {
        if (this.verticesSubmeshes != null) {
            Matrix43 matrix = matrix44var1;
            if (bool3) {
                matrix = this.toolkit.unsure;
                matrix.updateMatrix(matrix44var1);
            }

            float[] vertices = new float[3];
            for (int index = 0; index < this.maxDepth; index++) {
                if ((submeshFlags & this.verticesSubmeshes[index]) != 0) {
                    matrix.calculatePerspective(this.verticesX[index], this.verticesY[index], this.verticesZ[index], vertices);
                    this.verticesX[index] = (int) vertices[0];
                    this.verticesY[index] = (int) vertices[1];
                    this.verticesZ[index] = (int) vertices[2];
                }
            }
        }

    }

    public void method14270() {
        if (this.normalsBuffer != null) {
            this.normalsBuffer.allocated = false;
        }
    }

    @Override
    public void yaxisRotateWithoutNormals(int i_1) {
        int i_2 = MathUtils.SINE[i_1];
        int i_3 = MathUtils.COSINE[i_1];

        for (int i_4 = 0; i_4 < this.maxDepth; i_4++) {
            int i_5 = i_2 * this.verticesZ[i_4] + i_3 * this.verticesX[i_4] >> 14;
            this.verticesZ[i_4] = i_3 * this.verticesZ[i_4] - i_2 * this.verticesX[i_4] >> 14;
            this.verticesX[i_4] = i_5;
        }

        setVerticesInactive();
        this.boundsUpdated = false;
    }


    @Override
    public void xaxisRotateWithoutNormals(int i_1) {
        int i_2 = MathUtils.SINE[i_1];
        int i_3 = MathUtils.COSINE[i_1];

        for (int i_4 = 0; i_4 < this.maxDepth; i_4++) {
            int i_5 = i_3 * this.verticesY[i_4] - i_2 * this.verticesZ[i_4] >> 14;
            this.verticesZ[i_4] = i_2 * this.verticesY[i_4] + i_3 * this.verticesZ[i_4] >> 14;
            this.verticesY[i_4] = i_5;
        }

        setVerticesInactive();
        this.boundsUpdated = false;
    }

    @Override
    public void zaxisRotateWithoutNormals(int i_1) {
        int i_2 = MathUtils.SINE[i_1];
        int i_3 = MathUtils.COSINE[i_1];

        for (int i_4 = 0; i_4 < this.maxDepth; i_4++) {
            int i_5 = i_2 * this.verticesY[i_4] + i_3 * this.verticesX[i_4] >> 14;
            this.verticesY[i_4] = i_3 * this.verticesY[i_4] - i_2 * this.verticesX[i_4] >> 14;
            this.verticesX[i_4] = i_5;
        }

        setVerticesInactive();
        this.boundsUpdated = false;
    }

    @Override
    public void translate(int translateX, int translateY, int translateZ) {
        for (int index = 0; index < this.maxDepth; index++) {
            if (translateX != 0) {
                this.verticesX[index] += translateX;
            }

            if (translateY != 0) {
                this.verticesY[index] += translateY;
            }

            if (translateZ != 0) {
                this.verticesZ[index] += translateZ;
            }
        }
        setVerticesInactive();
        this.boundsUpdated = false;
    }

    public boolean method14271() {
        if (this.normalsBuffer.allocated) {
            return true;
        } 
        if (this.normalsBuffer.anInterface32409 == null) {
            this.normalsBuffer.anInterface32409 = this.toolkit.method13993(false);
        }

        DirectXBuffer streamSource = this.normalsBuffer.anInterface32409;
        streamSource.create(this.anInt8963 * 6);
        Unsafe unsafe = this.toolkit.theUnsafe;
        if (unsafe != null) {
            int i_3 = this.anInt8963 * 6;
            long long_4 = streamSource.getDataStorePointer(0, i_3);
            if (long_4 == 0L) {
                return false;
            } 
            for (int index = 0; index < this.anInt8963; index++) {
                unsafe.putShort(long_4, this.normalsFaceA[index]);
                long_4 += 2L;
                unsafe.putShort(long_4, this.normalsFaceB[index]);
                long_4 += 2L;
                unsafe.putShort(long_4, this.normalsFaceC[index]);
                long_4 += 2L;
            }

            streamSource.unlock();
            this.normalsBuffer.anInterface32407 = streamSource;
            this.normalsBuffer.allocated = true;
            this.aBool8919 = true;
            return true;
        } else {
            ByteBuffer buffer = this.toolkit.directBuffer;
            buffer.clear();

            for (int index = 0; index < this.anInt8963; index++) {
                buffer.putShort(this.normalsFaceA[index]);
                buffer.putShort(this.normalsFaceB[index]);
                buffer.putShort(this.normalsFaceC[index]);
            }

            if (streamSource.upload(0, buffer.position(), this.toolkit.directBufferAddress)) {
                this.normalsBuffer.anInterface32407 = streamSource;
                this.normalsBuffer.allocated = true;
                this.aBool8919 = true;
                return true;
            } 
            return false;
        }
    }

    @Override
    public void scale(int scaleX, int scaleY, int scaleZ) {
        for (int index = 0; index < this.maxDepth; index++) {
            if (scaleX != 128) {
                this.verticesX[index] = this.verticesX[index] * scaleX >> 7;
            }

            if (scaleY != 128) {
                this.verticesY[index] = scaleY * this.verticesY[index] >> 7;
            }

            if (scaleZ != 128) {
                this.verticesZ[index] = scaleZ * this.verticesZ[index] >> 7;
            }
        }
        setVerticesInactive();
        this.boundsUpdated = false;
    }

    @Override
    public int getSize2d() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.size2d;
    }

    @Override
    public byte[] getAlpha() {
        return this.facesAlpha;
    }

    @Override
    public int getMinX() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.minX;
    }

    @Override
    public int getMaxX() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.maxX;
    }



    @Override
    public void applyShading(Model model, int i_2, int i_3, int i_4, boolean bool5) {
        ModelDX modelDx = (ModelDX) model;
        if (this.numFaces != 0 && modelDx.numFaces != 0) {
            int depth = modelDx.maxDepth;
            int[] vertX = modelDx.verticesX;
            int[] vertY = modelDx.verticesY;
            int[] vertZ = modelDx.verticesZ;
            short[] normX = modelDx.normalsXs;
            short[] normY = modelDx.normalsYs;
            short[] normZ = modelDx.normalsZs;
            byte[] normMagnitude = modelDx.normalsMagnitudes;
            short[] shorts_15;
            short[] shorts_16;
            short[] shorts_17;
            byte[] bytes_18;
            if (this.aClass3558962 != null) {
                shorts_15 = this.aClass3558962.normalsX;
                shorts_16 = this.aClass3558962.normalsY;
                shorts_17 = this.aClass3558962.normalsZ;
                bytes_18 = this.aClass3558962.magnitude;
            } else {
                shorts_15 = null;
                shorts_16 = null;
                shorts_17 = null;
                bytes_18 = null;
            }

            short[] shorts_19;
            short[] shorts_20;
            short[] shorts_21;
            byte[] bytes_22;
            if (modelDx.aClass3558962 != null) {
                shorts_19 = modelDx.aClass3558962.normalsX;
                shorts_20 = modelDx.aClass3558962.normalsY;
                shorts_21 = modelDx.aClass3558962.normalsZ;
                bytes_22 = modelDx.aClass3558962.magnitude;
            } else {
                shorts_19 = null;
                shorts_20 = null;
                shorts_21 = null;
                bytes_22 = null;
            }

            int[] ints_23 = modelDx.totalFaceCounts;
            short[] shorts_24 = modelDx.aShortArray8948;
            if (!modelDx.boundsUpdated) {
                modelDx.calculateBounds();
            }

            int miny = modelDx.minY;
            int maxy = modelDx.maxY;
            int minx = modelDx.minX;
            int maxx = modelDx.maxX;
            int minz = modelDx.minZ;
            int maxz = modelDx.maxZ;

            for (int i_31 = 0; i_31 < this.maxDepth; i_31++) {
                int i_32 = this.verticesY[i_31] - i_3;
                if (i_32 >= miny && i_32 <= maxy) {
                    int i_33 = this.verticesX[i_31] - i_2;
                    if (i_33 >= minx && i_33 <= maxx) {
                        int i_34 = this.verticesZ[i_31] - i_4;
                        if (i_34 >= minz && i_34 <= maxz) {
                            int i_35 = -1;
                            int i_36 = this.totalFaceCounts[i_31];
                            int i_37 = this.totalFaceCounts[i_31 + 1];

                            int i_38;
                            for (i_38 = i_36; i_38 < i_37; i_38++) {
                                i_35 = this.aShortArray8948[i_38] - 1;
                                if (i_35 == -1 || this.normalsMagnitudes[i_35] != 0) {
                                    break;
                                }
                            }

                            if (i_35 != -1) {
                                for (i_38 = 0; i_38 < depth; i_38++) {
                                    if (i_33 == vertX[i_38] && i_34 == vertZ[i_38] && i_32 == vertY[i_38]) {
                                        int i_39 = -1;
                                        i_36 = ints_23[i_38];
                                        i_37 = ints_23[i_38 + 1];

                                        for (int i_40 = i_36; i_40 < i_37; i_40++) {
                                            i_39 = shorts_24[i_40] - 1;
                                            if (i_39 == -1 || normMagnitude[i_39] != 0) {
                                                break;
                                            }
                                        }

                                        if (i_39 != -1) {
                                            if (shorts_15 == null) {
                                                this.aClass3558962 = new ModelNormalsDX();
                                                shorts_15 = this.aClass3558962.normalsX = ArrayUtils.arraycopy(this.normalsXs);
                                                shorts_16 = this.aClass3558962.normalsY = ArrayUtils.arraycopy(this.normalsYs);
                                                shorts_17 = this.aClass3558962.normalsZ = ArrayUtils.arraycopy(this.normalsZs);
                                                bytes_18 = this.aClass3558962.magnitude = ArrayUtils.safeFullArraycopy(this.normalsMagnitudes);
                                            }

                                            if (shorts_19 == null) {
                                                ModelNormalsDX class35546 = modelDx.aClass3558962 = new ModelNormalsDX();
                                                shorts_19 = class35546.normalsX = ArrayUtils.arraycopy(normX);
                                                shorts_20 = class35546.normalsY = ArrayUtils.arraycopy(normY);
                                                shorts_21 = class35546.normalsZ = ArrayUtils.arraycopy(normZ);
                                                bytes_22 = class35546.magnitude = ArrayUtils.safeFullArraycopy(normMagnitude);
                                            }

                                            short s_47 = this.normalsXs[i_35];
                                            short s_41 = this.normalsYs[i_35];
                                            short s_42 = this.normalsZs[i_35];
                                            byte b_43 = this.normalsMagnitudes[i_35];
                                            i_36 = ints_23[i_38];
                                            i_37 = ints_23[i_38 + 1];

                                            int i_44;
                                            int i_45;
                                            for (i_44 = i_36; i_44 < i_37; i_44++) {
                                                i_45 = shorts_24[i_44] - 1;
                                                if (i_45 == -1) {
                                                    break;
                                                }

                                                if (bytes_22[i_45] != 0) {
                                                    shorts_19[i_45] += s_47;
                                                    shorts_20[i_45] += s_41;
                                                    shorts_21[i_45] += s_42;
                                                    bytes_22[i_45] += b_43;
                                                }
                                            }

                                            s_47 = normX[i_39];
                                            s_41 = normY[i_39];
                                            s_42 = normZ[i_39];
                                            b_43 = normMagnitude[i_39];
                                            i_36 = this.totalFaceCounts[i_31];
                                            i_37 = this.totalFaceCounts[i_31 + 1];

                                            for (i_44 = i_36; i_44 < i_37; i_44++) {
                                                i_45 = this.aShortArray8948[i_44] - 1;
                                                if (i_45 == -1) {
                                                    break;
                                                }

                                                if (bytes_18[i_45] != 0) {
                                                    shorts_15[i_45] += s_47;
                                                    shorts_16[i_45] += s_41;
                                                    shorts_17[i_45] += s_42;
                                                    bytes_18[i_45] += b_43;
                                                }
                                            }

                                            modelDx.method14285();
                                            method14285();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void setAmbient(int i_1) {
        this.ambient = (short) i_1;
        method14269();
    }

    @Override
    public void setContrast(int i_1) {
        this.contast = (short) i_1;
        setVerticesInactive();
        method14285();
    }

    public void method14274() {
        if (this.aBool8919) {
            this.aBool8919 = false;
            if (this.aClass87Array8949 == null && this.aClass172Array8950 == null && this.billboardEffects == null && !ModelFunctionMaskDX.method981(this.activeFunctionMask)) {
                boolean bool1 = false;
                boolean bool2 = false;
                boolean bool3 = false;
                if (this.verticesX != null && !ModelFunctionMaskDX.method1000(this.activeFunctionMask)) {
                    if (this.verticeManager != null && !this.verticeManager.method1264()) {
                        this.aBool8919 = true;
                    } else {
                        if (!this.boundsUpdated) {
                            calculateBounds();
                        }

                        bool1 = true;
                    }
                }

                if (this.verticesY != null && !ModelFunctionMaskDX.method1001(this.activeFunctionMask)) {
                    if (this.verticeManager != null && !this.verticeManager.method1264()) {
                        this.aBool8919 = true;
                    } else {
                        if (!this.boundsUpdated) {
                            calculateBounds();
                        }

                        bool2 = true;
                    }
                }

                if (this.verticesZ != null && !ModelFunctionMaskDX.method1002(this.activeFunctionMask)) {
                    if (this.verticeManager != null && !this.verticeManager.method1264()) {
                        this.aBool8919 = true;
                    } else {
                        if (!this.boundsUpdated) {
                            calculateBounds();
                        }

                        bool3 = true;
                    }
                }

                if (bool1) {
                    this.verticesX = null;
                }

                if (bool2) {
                    this.verticesY = null;
                }

                if (bool3) {
                    this.verticesZ = null;
                }
            }

            if (this.aShortArray8948 != null && this.verticesX == null && this.verticesY == null && this.verticesZ == null) {
                this.aShortArray8948 = null;
                this.totalFaceCounts = null;
            }

            if (this.normalsMagnitudes != null && !ModelFunctionMaskDX.method996(this.activeFunctionMask, this.preferenceFunctionMask)) {
                label300:
                {
                    label243:
                    {
                        if ((this.preferenceFunctionMask & 0x37) != 0) {
                            if (this.lightManager == null || this.lightManager.method1264()) {
                                break label243;
                            }
                        } else if (this.aClass638913 == null || this.aClass638913.method1264()) {
                            break label243;
                        }

                        this.aBool8919 = true;
                        break label300;
                    }

                    this.normalsZs = null;
                    this.normalsYs = null;
                    this.normalsXs = null;
                    this.normalsMagnitudes = null;
                }
            }

            if (this.facesColor != null && !ModelFunctionMaskDX.method987(this.activeFunctionMask, this.preferenceFunctionMask)) {
                if (this.aClass638913 != null && !this.aClass638913.method1264()) {
                    this.aBool8919 = true;
                } else {
                    this.facesColor = null;
                }
            }

            if (this.facesAlpha != null && !ModelFunctionMaskDX.method1004(this.activeFunctionMask, this.preferenceFunctionMask)) {
                if (this.aClass638913 != null && !this.aClass638913.method1264()) {
                    this.aBool8919 = true;
                } else {
                    this.facesAlpha = null;
                }
            }

            if (this.activeUvU != null && !ModelFunctionMaskDX.method1005(this.activeFunctionMask)) {
                if (this.aClass638930 != null && !this.aClass638930.method1264()) {
                    this.aBool8919 = true;
                } else {
                    this.activeUvV = null;
                    this.activeUvU = null;
                }
            }

            if (this.facesMaterial != null && !ModelFunctionMaskDX.method1010(this.activeFunctionMask, this.preferenceFunctionMask)) {
                if (this.aClass638913 != null && !this.aClass638913.method1264()) {
                    this.aBool8919 = true;
                } else {
                    this.facesMaterial = null;
                }
            }

            if (this.normalsFaceA != null && !ModelFunctionMaskDX.method1009(this.activeFunctionMask)) {
                if ((this.normalsBuffer == null || this.normalsBuffer.method861()) && (this.aClass638913 == null || this.aClass638913.method1264())) {
                    this.normalsFaceC = null;
                    this.normalsFaceB = null;
                    this.normalsFaceA = null;
                } else {
                    this.aBool8919 = true;
                }
            }

            if (this.vertexIndices != null) {
                if (this.verticeManager != null && !this.verticeManager.method1264()) {
                    this.aBool8919 = true;
                } else {
                    this.vertexIndices = null;
                }
            }

            if (this.alphaIndices != null) {
                if (this.aClass638913 != null && !this.aClass638913.method1264()) {
                    this.aBool8919 = true;
                } else {
                    this.alphaIndices = null;
                }
            }

            if (this.facesByLabel != null && !ModelFunctionMaskDX.is0x180(this.activeFunctionMask)) {
                this.facesByLabel = null;
                this.facesSubmeshes = null;
            }

            if (this.verticesByLabel != null && !ModelFunctionMaskDX.is0x20(this.activeFunctionMask)) {
                this.verticesByLabel = null;
                this.verticesSubmeshes = null;
            }

            if (this.billboardsByLabel != null && !ModelFunctionMaskDX.is0x400(this.activeFunctionMask)) {
                this.billboardsByLabel = null;
            }

            if (this.anIntArray8944 != null && (this.activeFunctionMask & 0x800) == 0 && (this.activeFunctionMask & 0x40000) == 0) {
                this.anIntArray8944 = null;
                this.anIntArray8946 = null;
                this.anIntArray8952 = null;
            }
        }

    }

    @Override
    public void recolor(short s_1, short s_2) {
        int i_3;
        for (i_3 = 0; i_3 < this.numFaces; i_3++) {
            if (this.facesColor[i_3] == s_1) {
                this.facesColor[i_3] = s_2;
            }
        }

        if (this.billboardEffects != null) {
            for (i_3 = 0; i_3 < this.numBillboards; i_3++) {
                BillboardEffectDX class564 = this.billboardEffects[i_3];
                BillboardSurfaceDX class655 = this.billboardBackgrounds[i_3];
                class655.diffuseColor = class655.diffuseColor & ~0xffffff | ColorUtil.getHslToRgb()[this.facesColor[class564.appliesOnFace] & 0xffff] & 0xffffff;
            }
        }

        method14269();
    }

    @Override
    public void applyEnvironmentAlpha(byte b_1, byte[] bytes_2) {
        int i_3;
        if (bytes_2 == null) {
            for (i_3 = 0; i_3 < this.numFaces; i_3++) {
                this.facesAlpha[i_3] = b_1;
            }
        } else {
            for (i_3 = 0; i_3 < this.numFaces; i_3++) {
                int i_4 = 255 - (255 - (b_1 & 0xff)) * (255 - (bytes_2[i_3] & 0xff)) / 255;
                this.facesAlpha[i_3] = (byte) i_4;
            }
        }

        method14269();
    }

    @Override
    public void rematerial(short materialSrc, short materialDst) {
        MaterialProvider materialProvider = this.toolkit.materialProvider;

        for (int index = 0; index < this.numFaces; index++) {
            if (this.facesMaterial[index] == materialSrc) {
                this.facesMaterial[index] = materialDst;
            }
        }

        byte srcMaterialContast = 0;
        byte srcMaterialBrightness = 0;
        if (materialSrc != -1) {
            MaterialType srcMaterial = materialProvider.getMaterial(materialSrc & 0xffff);
            srcMaterialContast = srcMaterial.alpha;
            srcMaterialBrightness = srcMaterial.brightness;
        }

        byte dstMaterialContrast = 0;
        byte dstMaterialBrightness = 0;
        if (materialDst != -1) {
            MaterialType dstMaterial = materialProvider.getMaterial(materialDst & 0xffff);
            dstMaterialContrast = dstMaterial.alpha;
            dstMaterialBrightness = dstMaterial.brightness;
            if (dstMaterial.textureSpeedU != 0 || dstMaterial.textureSpeedV != 0) {
                this.textureAnimating = true;
            }
        }

        if (srcMaterialContast != dstMaterialContrast | srcMaterialBrightness != dstMaterialBrightness) {
            if (this.billboardEffects != null) {
                for (int index = 0; index < this.numBillboards; index++) {
                    BillboardEffectDX effect = this.billboardEffects[index];
                    BillboardSurfaceDX background = this.billboardBackgrounds[index];
                    background.diffuseColor = background.diffuseColor & ~0xffffff | ColorUtil.getHslToRgb()[this.facesColor[effect.appliesOnFace] & 0xffff] & 0xffffff;
                }
            }

            method14269();
        }
    }

    @Override
    public void uploadToGpu() {
        if (this.anInt8906 > 0 && this.anInt8963 > 0) {
            method14279();
            method14271();
            method14274();
        }

    }

    @Override
    public boolean accountForLayers() {
        return this.accountForPriorities;
    }

    @Override
    public boolean hasAnimatedTexture() {
        return this.textureAnimating;
    }


    public void method14275(Matrix44 matrix441) {
        if (this.billboardEffects != null) {
            this.toolkit.setDepthBuffering(!this.accountForPriorities);
            Matrix43 matrix44var2 = this.toolkit.unsure;
            AbstractParticleShader particleShader = this.toolkit.abstractParticleShader;

            for (int i_4 = 0; i_4 < this.numBillboards; i_4++) {
                BillboardEffectDX effect = this.billboardEffects[i_4];
                BillboardSurfaceDX background = this.billboardBackgrounds[i_4];
                if (!effect.stationary || !this.toolkit.bloomEnabled()) {
                    float f_7 = (this.verticesX[effect.faceA] + this.verticesX[effect.faceB] + this.verticesX[effect.faceC]) * 0.3333333F;
                    float f_8 = (this.verticesY[effect.faceA] + this.verticesY[effect.faceB] + this.verticesY[effect.faceC]) * 0.3333333F;
                    float f_9 = (this.verticesZ[effect.faceA] + this.verticesZ[effect.faceB] + this.verticesZ[effect.faceC]) * 0.3333333F;
                    float f_10 = matrix441.getFaceList()[0] * f_7 + matrix441.getFaceList()[4] * f_8 + matrix441.getFaceList()[8] * f_9 + matrix441.getFaceList()[12];
                    float f_11 = matrix441.getFaceList()[1] * f_7 + matrix441.getFaceList()[5] * f_8 + matrix441.getFaceList()[9] * f_9 + matrix441.getFaceList()[13];
                    float f_12 = matrix441.getFaceList()[2] * f_7 + matrix441.getFaceList()[6] * f_8 + matrix441.getFaceList()[10] * f_9 + matrix441.getFaceList()[14];
                    float f_13 = (float) (1.0D / Math.sqrt(f_10 * f_10 + f_11 * f_11 + f_12 * f_12)) * effect.distance;
                    matrix44var2.method5213(background.rotation, background.scaleX * effect.size2d >> 7, background.scaleY * effect.size3d >> 7, f_10 + background.offsetX - f_10 * f_13, f_11 + background.offsetY - f_11 * f_13, f_12 - f_12 * f_13);
                    matrix44var2.multiply(this.toolkit.aClass2948804);
                    this.toolkit.aClass3848715.fromMatrix(matrix44var2);
                    particleShader.method1788(this.toolkit.aClass3848715);
                    particleShader.textureCoordinateMatrix.identity();
                    particleShader.diffuseColor = background.diffuseColor;
                    particleShader.diffuseSampler = this.toolkit.materialCache.getTextureSampler(effect.materialId);
                    particleShader.method1790();
                }
            }

            this.toolkit.setDepthBuffering(true);
        }

    }

    @Override
    public void animateShadowtransform(int i_1, int i_2, int i_3, int i_4) {
        int i_5;
        int i_6;
        if (i_1 == 0) {
            i_5 = 0;
            originX = 0;
            originY = 0;
            originZ = 0;

            for (i_6 = 0; i_6 < this.maxDepth; i_6++) {
                originX += this.verticesX[i_6];
                originY += this.verticesY[i_6];
                originZ += this.verticesZ[i_6];
                ++i_5;
            }

            if (i_5 > 0) {
                originX = i_2 + originX / i_5;
                originY = i_3 + originY / i_5;
                originZ = i_4 + originZ / i_5;
            } else {
                originX = i_2;
                originY = i_3;
                originZ = i_4;
            }
        } else if (i_1 == 1) {
            for (i_5 = 0; i_5 < this.maxDepth; i_5++) {
                this.verticesX[i_5] += i_2;
                this.verticesY[i_5] += i_3;
                this.verticesZ[i_5] += i_4;
            }
        } else {
            int i_7;
            int i_8;
            if (i_1 == 2) {
                for (i_5 = 0; i_5 < this.maxDepth; i_5++) {
                    this.verticesX[i_5] -= originX;
                    this.verticesY[i_5] -= originY;
                    this.verticesZ[i_5] -= originZ;
                    if (i_4 != 0) {
                        i_6 = MathUtils.SINE[i_4];
                        i_7 = MathUtils.COSINE[i_4];
                        i_8 = i_6 * this.verticesY[i_5] + i_7 * this.verticesX[i_5] + 16383 >> 14;
                        this.verticesY[i_5] = i_7 * this.verticesY[i_5] - i_6 * this.verticesX[i_5] + 16383 >> 14;
                        this.verticesX[i_5] = i_8;
                    }

                    if (i_2 != 0) {
                        i_6 = MathUtils.SINE[i_2];
                        i_7 = MathUtils.COSINE[i_2];
                        i_8 = i_7 * this.verticesY[i_5] - i_6 * this.verticesZ[i_5] + 16383 >> 14;
                        this.verticesZ[i_5] = i_6 * this.verticesY[i_5] + i_7 * this.verticesZ[i_5] + 16383 >> 14;
                        this.verticesY[i_5] = i_8;
                    }

                    if (i_3 != 0) {
                        i_6 = MathUtils.SINE[i_3];
                        i_7 = MathUtils.COSINE[i_3];
                        i_8 = i_6 * this.verticesZ[i_5] + i_7 * this.verticesX[i_5] + 16383 >> 14;
                        this.verticesZ[i_5] = i_7 * this.verticesZ[i_5] - i_6 * this.verticesX[i_5] + 16383 >> 14;
                        this.verticesX[i_5] = i_8;
                    }

                    this.verticesX[i_5] += originX;
                    this.verticesY[i_5] += originY;
                    this.verticesZ[i_5] += originZ;
                }
            } else if (i_1 == 3) {
                for (i_5 = 0; i_5 < this.maxDepth; i_5++) {
                    this.verticesX[i_5] -= originX;
                    this.verticesY[i_5] -= originY;
                    this.verticesZ[i_5] -= originZ;
                    this.verticesX[i_5] = i_2 * this.verticesX[i_5] / 128;
                    this.verticesY[i_5] = i_3 * this.verticesY[i_5] / 128;
                    this.verticesZ[i_5] = i_4 * this.verticesZ[i_5] / 128;
                    this.verticesX[i_5] += originX;
                    this.verticesY[i_5] += originY;
                    this.verticesZ[i_5] += originZ;
                }
            } else {
                BillboardEffectDX class5610;
                BillboardSurfaceDX class6511;
                if (i_1 == 5) {
                    for (i_5 = 0; i_5 < this.numFaces; i_5++) {
                        i_6 = (this.facesAlpha[i_5] & 0xff) + i_2 * 8;
                        if (i_6 < 0) {
                            i_6 = 0;
                        } else if (i_6 > 255) {
                            i_6 = 255;
                        }

                        this.facesAlpha[i_5] = (byte) i_6;
                    }

                    if (this.billboardEffects != null) {
                        for (i_5 = 0; i_5 < this.numBillboards; i_5++) {
                            class5610 = this.billboardEffects[i_5];
                            class6511 = this.billboardBackgrounds[i_5];
                            class6511.diffuseColor = class6511.diffuseColor & 0xffffff | 255 - (this.facesAlpha[class5610.appliesOnFace] & 0xff) << 24;
                        }
                    }

                    method14269();
                } else if (i_1 == 7) {
                    for (i_5 = 0; i_5 < this.numFaces; i_5++) {
                        i_6 = this.facesColor[i_5] & 0xffff;
                        i_7 = i_6 >> 10 & 0x3f;
                        i_8 = i_6 >> 7 & 0x7;
                        int i_9 = i_6 & 0x7f;
                        i_7 = i_7 + i_2 & 0x3f;
                        i_8 += i_3 / 4;
                        if (i_8 < 0) {
                            i_8 = 0;
                        } else if (i_8 > 7) {
                            i_8 = 7;
                        }

                        i_9 += i_4;
                        if (i_9 < 0) {
                            i_9 = 0;
                        } else if (i_9 > 127) {
                            i_9 = 127;
                        }

                        this.facesColor[i_5] = (short) (i_7 << 10 | i_8 << 7 | i_9);
                    }

                    if (this.billboardEffects != null) {
                        for (i_5 = 0; i_5 < this.numBillboards; i_5++) {
                            class5610 = this.billboardEffects[i_5];
                            class6511 = this.billboardBackgrounds[i_5];
                            class6511.diffuseColor = class6511.diffuseColor & ~0xffffff | ColorUtil.getHslToRgb()[this.facesColor[class5610.appliesOnFace] & 0xffff] & 0xffffff;
                        }
                    }

                    method14269();
                } else {
                    BillboardSurfaceDX class6512;
                    if (i_1 == 8) {
                        for (i_5 = 0; i_5 < this.numBillboards; i_5++) {
                            class6512 = this.billboardBackgrounds[i_5];
                            class6512.offsetX += i_2;
                            class6512.offsetY += i_3;
                        }
                    } else if (i_1 == 10) {
                        for (i_5 = 0; i_5 < this.numBillboards; i_5++) {
                            class6512 = this.billboardBackgrounds[i_5];
                            class6512.scaleX = i_2 * class6512.scaleX >> 7;
                            class6512.scaleY = i_3 * class6512.scaleY >> 7;
                        }
                    } else if (i_1 == 9) {
                        for (i_5 = 0; i_5 < this.numBillboards; i_5++) {
                            class6512 = this.billboardBackgrounds[i_5];
                            class6512.rotation = i_2 + class6512.rotation & 0x3fff;
                        }
                    }
                }
            }
        }

    }

    @Override
    public void animatePartialtransform(int i_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, boolean bool7) {
        int i_8 = ints_2.length;
        int i_9;
        int i_10;
        int i_11;
        int[] ints_12;
        int i_13;
        int i_14;
        if (i_1 == 0) {
            i_3 <<= 4;
            i_4 <<= 4;
            i_5 <<= 4;
            i_9 = 0;
            originX = 0;
            originY = 0;
            originZ = 0;

            for (i_10 = 0; i_10 < i_8; i_10++) {
                i_11 = ints_2[i_10];
                if (i_11 < this.verticesByLabel.length) {
                    ints_12 = this.verticesByLabel[i_11];

                    for (i_13 = 0; i_13 < ints_12.length; i_13++) {
                        i_14 = ints_12[i_13];
                        originX += this.verticesX[i_14];
                        originY += this.verticesY[i_14];
                        originZ += this.verticesZ[i_14];
                        ++i_9;
                    }
                }
            }

            if (i_9 > 0) {
                originX = i_3 + originX / i_9;
                originY = i_4 + originY / i_9;
                originZ = i_5 + originZ / i_9;
            } else {
                originX = i_3;
                originY = i_4;
                originZ = i_5;
            }
        } else {
            int[] ints_21;
            int i_22;
            if (i_1 == 1) {
                i_3 <<= 4;
                i_4 <<= 4;
                i_5 <<= 4;

                for (i_9 = 0; i_9 < i_8; i_9++) {
                    i_10 = ints_2[i_9];
                    if (i_10 < this.verticesByLabel.length) {
                        ints_21 = this.verticesByLabel[i_10];

                        for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                            i_13 = ints_21[i_22];
                            this.verticesX[i_13] += i_3;
                            this.verticesY[i_13] += i_4;
                            this.verticesZ[i_13] += i_5;
                        }
                    }
                }
            } else {
                int i_15;
                int i_16;
                int i_17;
                int i_18;
                if (i_1 == 2) {
                    for (i_9 = 0; i_9 < i_8; i_9++) {
                        i_10 = ints_2[i_9];
                        if (i_10 < this.verticesByLabel.length) {
                            ints_21 = this.verticesByLabel[i_10];
                            if ((i_6 & 0x1) == 0) {
                                for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                                    i_13 = ints_21[i_22];
                                    this.verticesX[i_13] -= originX;
                                    this.verticesY[i_13] -= originY;
                                    this.verticesZ[i_13] -= originZ;
                                    if (i_5 != 0) {
                                        i_14 = MathUtils.SINE[i_5];
                                        i_15 = MathUtils.COSINE[i_5];
                                        i_16 = i_14 * this.verticesY[i_13] + i_15 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesY[i_13] = i_15 * this.verticesY[i_13] - i_14 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesX[i_13] = i_16;
                                    }

                                    if (i_3 != 0) {
                                        i_14 = MathUtils.SINE[i_3];
                                        i_15 = MathUtils.COSINE[i_3];
                                        i_16 = i_15 * this.verticesY[i_13] - i_14 * this.verticesZ[i_13] + 16383 >> 14;
                                        this.verticesZ[i_13] = i_14 * this.verticesY[i_13] + i_15 * this.verticesZ[i_13] + 16383 >> 14;
                                        this.verticesY[i_13] = i_16;
                                    }

                                    if (i_4 != 0) {
                                        i_14 = MathUtils.SINE[i_4];
                                        i_15 = MathUtils.COSINE[i_4];
                                        i_16 = i_14 * this.verticesZ[i_13] + i_15 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesZ[i_13] = i_15 * this.verticesZ[i_13] - i_14 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesX[i_13] = i_16;
                                    }

                                    this.verticesX[i_13] += originX;
                                    this.verticesY[i_13] += originY;
                                    this.verticesZ[i_13] += originZ;
                                }
                            } else {
                                for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                                    i_13 = ints_21[i_22];
                                    this.verticesX[i_13] -= originX;
                                    this.verticesY[i_13] -= originY;
                                    this.verticesZ[i_13] -= originZ;
                                    if (i_3 != 0) {
                                        i_14 = MathUtils.SINE[i_3];
                                        i_15 = MathUtils.COSINE[i_3];
                                        i_16 = i_15 * this.verticesY[i_13] - i_14 * this.verticesZ[i_13] + 16383 >> 14;
                                        this.verticesZ[i_13] = i_14 * this.verticesY[i_13] + i_15 * this.verticesZ[i_13] + 16383 >> 14;
                                        this.verticesY[i_13] = i_16;
                                    }

                                    if (i_5 != 0) {
                                        i_14 = MathUtils.SINE[i_5];
                                        i_15 = MathUtils.COSINE[i_5];
                                        i_16 = i_14 * this.verticesY[i_13] + i_15 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesY[i_13] = i_15 * this.verticesY[i_13] - i_14 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesX[i_13] = i_16;
                                    }

                                    if (i_4 != 0) {
                                        i_14 = MathUtils.SINE[i_4];
                                        i_15 = MathUtils.COSINE[i_4];
                                        i_16 = i_14 * this.verticesZ[i_13] + i_15 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesZ[i_13] = i_15 * this.verticesZ[i_13] - i_14 * this.verticesX[i_13] + 16383 >> 14;
                                        this.verticesX[i_13] = i_16;
                                    }

                                    this.verticesX[i_13] += originX;
                                    this.verticesY[i_13] += originY;
                                    this.verticesZ[i_13] += originZ;
                                }
                            }
                        }
                    }

                    if (bool7) {
                        for (i_9 = 0; i_9 < i_8; i_9++) {
                            i_10 = ints_2[i_9];
                            if (i_10 < this.verticesByLabel.length) {
                                ints_21 = this.verticesByLabel[i_10];

                                for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                                    i_13 = ints_21[i_22];
                                    i_14 = this.totalFaceCounts[i_13];
                                    i_15 = this.totalFaceCounts[i_13 + 1];

                                    for (i_16 = i_14; i_16 < i_15; i_16++) {
                                        i_17 = this.aShortArray8948[i_16] - 1;
                                        if (i_17 == -1) {
                                            break;
                                        }

                                        int i_19;
                                        int i_20;
                                        if (i_5 != 0) {
                                            i_18 = MathUtils.SINE[i_5];
                                            i_19 = MathUtils.COSINE[i_5];
                                            i_20 = this.normalsYs[i_17] * i_18 + this.normalsXs[i_17] * i_19 + 16383 >> 14;
                                            this.normalsYs[i_17] = (short) (this.normalsYs[i_17] * i_19 - this.normalsXs[i_17] * i_18 + 16383 >> 14);
                                            this.normalsXs[i_17] = (short) i_20;
                                        }

                                        if (i_3 != 0) {
                                            i_18 = MathUtils.SINE[i_3];
                                            i_19 = MathUtils.COSINE[i_3];
                                            i_20 = this.normalsYs[i_17] * i_19 - this.normalsZs[i_17] * i_18 + 16383 >> 14;
                                            this.normalsZs[i_17] = (short) (this.normalsYs[i_17] * i_18 + this.normalsZs[i_17] * i_19 + 16383 >> 14);
                                            this.normalsYs[i_17] = (short) i_20;
                                        }

                                        if (i_4 != 0) {
                                            i_18 = MathUtils.SINE[i_4];
                                            i_19 = MathUtils.COSINE[i_4];
                                            i_20 = this.normalsZs[i_17] * i_18 + this.normalsXs[i_17] * i_19 + 16383 >> 14;
                                            this.normalsZs[i_17] = (short) (this.normalsZs[i_17] * i_19 - this.normalsXs[i_17] * i_18 + 16383 >> 14);
                                            this.normalsXs[i_17] = (short) i_20;
                                        }
                                    }
                                }
                            }
                        }

                        method14285();
                    }
                } else if (i_1 == 3) {
                    for (i_9 = 0; i_9 < i_8; i_9++) {
                        i_10 = ints_2[i_9];
                        if (i_10 < this.verticesByLabel.length) {
                            ints_21 = this.verticesByLabel[i_10];

                            for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                                i_13 = ints_21[i_22];
                                this.verticesX[i_13] -= originX;
                                this.verticesY[i_13] -= originY;
                                this.verticesZ[i_13] -= originZ;
                                this.verticesX[i_13] = i_3 * this.verticesX[i_13] >> 7;
                                this.verticesY[i_13] = i_4 * this.verticesY[i_13] >> 7;
                                this.verticesZ[i_13] = i_5 * this.verticesZ[i_13] >> 7;
                                this.verticesX[i_13] += originX;
                                this.verticesY[i_13] += originY;
                                this.verticesZ[i_13] += originZ;
                            }
                        }
                    }
                } else {
                    boolean bool24;
                    BillboardSurfaceDX background;
                    BillboardEffectDX effect;
                    if (i_1 == 5) {
                        if (this.facesByLabel != null) {
                            bool24 = false;

                            for (i_10 = 0; i_10 < i_8; i_10++) {
                                i_11 = ints_2[i_10];
                                if (i_11 < this.facesByLabel.length) {
                                    ints_12 = this.facesByLabel[i_11];

                                    for (i_13 = 0; i_13 < ints_12.length; i_13++) {
                                        i_14 = ints_12[i_13];
                                        i_15 = (this.facesAlpha[i_14] & 0xff) + i_3 * 8;
                                        if (i_15 < 0) {
                                            i_15 = 0;
                                        } else if (i_15 > 255) {
                                            i_15 = 255;
                                        }

                                        this.facesAlpha[i_14] = (byte) i_15;
                                    }

                                    bool24 |= ints_12.length > 0;
                                }
                            }

                            if (bool24) {
                                if (this.billboardEffects != null) {
                                    for (i_10 = 0; i_10 < this.numBillboards; i_10++) {
                                        effect = this.billboardEffects[i_10];
                                        background = this.billboardBackgrounds[i_10];
                                        background.diffuseColor = background.diffuseColor & 0xffffff | 255 - (this.facesAlpha[effect.appliesOnFace] & 0xff) << 24;
                                    }
                                }

                                method14269();
                            }
                        }
                    } else if (i_1 == 7) {
                        if (this.facesByLabel != null) {
                            bool24 = false;

                            for (i_10 = 0; i_10 < i_8; i_10++) {
                                i_11 = ints_2[i_10];
                                if (i_11 < this.facesByLabel.length) {
                                    ints_12 = this.facesByLabel[i_11];

                                    for (i_13 = 0; i_13 < ints_12.length; i_13++) {
                                        i_14 = ints_12[i_13];
                                        i_15 = this.facesColor[i_14] & 0xffff;
                                        i_16 = i_15 >> 10 & 0x3f;
                                        i_17 = i_15 >> 7 & 0x7;
                                        i_18 = i_15 & 0x7f;
                                        i_16 = i_3 + i_16 & 0x3f;
                                        i_17 += i_4 / 4;
                                        if (i_17 < 0) {
                                            i_17 = 0;
                                        } else if (i_17 > 7) {
                                            i_17 = 7;
                                        }

                                        i_18 += i_5;
                                        if (i_18 < 0) {
                                            i_18 = 0;
                                        } else if (i_18 > 127) {
                                            i_18 = 127;
                                        }

                                        this.facesColor[i_14] = (short) (i_16 << 10 | i_17 << 7 | i_18);
                                    }

                                    bool24 |= ints_12.length > 0;
                                }
                            }

                            if (bool24) {
                                if (this.billboardEffects != null) {
                                    for (i_10 = 0; i_10 < this.numBillboards; i_10++) {
                                        effect = this.billboardEffects[i_10];
                                        background = this.billboardBackgrounds[i_10];
                                        background.diffuseColor = background.diffuseColor & ~0xffffff | ColorUtil.getHslToRgb()[this.facesColor[effect.appliesOnFace] & 0xffff] & 0xffffff;
                                    }
                                }

                                method14269();
                            }
                        }
                    } else {
                        BillboardSurfaceDX class6523;
                        if (i_1 == 8) {
                            if (this.billboardsByLabel != null) {
                                for (i_9 = 0; i_9 < i_8; i_9++) {
                                    i_10 = ints_2[i_9];
                                    if (i_10 < this.billboardsByLabel.length) {
                                        ints_21 = this.billboardsByLabel[i_10];

                                        for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                                            class6523 = this.billboardBackgrounds[ints_21[i_22]];
                                            class6523.offsetX += i_3;
                                            class6523.offsetY += i_4;
                                        }
                                    }
                                }
                            }
                        } else if (i_1 == 10) {
                            if (this.billboardsByLabel != null) {
                                for (i_9 = 0; i_9 < i_8; i_9++) {
                                    i_10 = ints_2[i_9];
                                    if (i_10 < this.billboardsByLabel.length) {
                                        ints_21 = this.billboardsByLabel[i_10];

                                        for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                                            class6523 = this.billboardBackgrounds[ints_21[i_22]];
                                            class6523.scaleX = i_3 * class6523.scaleX >> 7;
                                            class6523.scaleY = i_4 * class6523.scaleY >> 7;
                                        }
                                    }
                                }
                            }
                        } else if (i_1 == 9 && this.billboardsByLabel != null) {
                            for (i_9 = 0; i_9 < i_8; i_9++) {
                                i_10 = ints_2[i_9];
                                if (i_10 < this.billboardsByLabel.length) {
                                    ints_21 = this.billboardsByLabel[i_10];

                                    for (i_22 = 0; i_22 < ints_21.length; i_22++) {
                                        class6523 = this.billboardBackgrounds[ints_21[i_22]];
                                        class6523.rotation = i_3 + class6523.rotation & 0x3fff;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void awaitLock() {
    }


    @Override
    public EffectiveVertex[] getEffectiveVertices() {
        return this.aClass172Array8950;
    }

    @Override
    public EmissiveTriangle[] getEmissiveTriangles() {
        return this.aClass87Array8949;
    }

    @Override
    public void updateParticlePosition(Matrix43 projectionMatrix) {
        Matrix44 matrix442 = this.toolkit.aClass3848683;
        matrix442.fromMatrix(projectionMatrix);
        int i_3;
        if (this.aClass87Array8949 != null) {
            for (i_3 = 0; i_3 < this.aClass87Array8949.length; i_3++) {
                EmissiveTriangle class874 = this.aClass87Array8949[i_3];
                EmissiveTriangle class875 = class874;
                if (class874.child != null) {
                    class875 = class874.child;
                }

                class875.verticeXA = (int) (matrix442.getFaceList()[12] + matrix442.getFaceList()[0] * this.verticesX[class874.faceA] + matrix442.getFaceList()[4] * this.verticesY[class874.faceA] + matrix442.getFaceList()[8] * this.verticesZ[class874.faceA]);
                class875.verticeYA = (int) (matrix442.getFaceList()[13] + matrix442.getFaceList()[1] * this.verticesX[class874.faceA] + matrix442.getFaceList()[5] * this.verticesY[class874.faceA] + matrix442.getFaceList()[9] * this.verticesZ[class874.faceA]);
                class875.verticeZA = (int) (matrix442.getFaceList()[14] + matrix442.getFaceList()[2] * this.verticesX[class874.faceA] + matrix442.getFaceList()[6] * this.verticesY[class874.faceA] + matrix442.getFaceList()[10] * this.verticesZ[class874.faceA]);
                class875.verticeXB = (int) (matrix442.getFaceList()[12] + matrix442.getFaceList()[0] * this.verticesX[class874.faceB] + matrix442.getFaceList()[4] * this.verticesY[class874.faceB] + matrix442.getFaceList()[8] * this.verticesZ[class874.faceB]);
                class875.verticeYB = (int) (matrix442.getFaceList()[13] + matrix442.getFaceList()[1] * this.verticesX[class874.faceB] + matrix442.getFaceList()[5] * this.verticesY[class874.faceB] + matrix442.getFaceList()[9] * this.verticesZ[class874.faceB]);
                class875.verticeZB = (int) (matrix442.getFaceList()[14] + matrix442.getFaceList()[2] * this.verticesX[class874.faceB] + matrix442.getFaceList()[6] * this.verticesY[class874.faceB] + matrix442.getFaceList()[10] * this.verticesZ[class874.faceB]);
                class875.verticeXC = (int) (matrix442.getFaceList()[12] + matrix442.getFaceList()[0] * this.verticesX[class874.faceC] + matrix442.getFaceList()[4] * this.verticesY[class874.faceC] + matrix442.getFaceList()[8] * this.verticesZ[class874.faceC]);
                class875.verticeYC = (int) (matrix442.getFaceList()[13] + matrix442.getFaceList()[1] * this.verticesX[class874.faceC] + matrix442.getFaceList()[5] * this.verticesY[class874.faceC] + matrix442.getFaceList()[9] * this.verticesZ[class874.faceC]);
                class875.verticeZC = (int) (matrix442.getFaceList()[14] + matrix442.getFaceList()[2] * this.verticesX[class874.faceC] + matrix442.getFaceList()[6] * this.verticesY[class874.faceC] + matrix442.getFaceList()[10] * this.verticesZ[class874.faceC]);
            }
        }

        if (this.aClass172Array8950 != null) {
            for (i_3 = 0; i_3 < this.aClass172Array8950.length; i_3++) {
                EffectiveVertex class1726 = this.aClass172Array8950[i_3];
                EffectiveVertex class1727 = class1726;
                if (class1726.child != null) {
                    class1727 = class1726.child;
                }

                if (class1726.matrix != null) {
                    class1726.matrix.copyFrom(matrix442);
                } else {
                    class1726.matrix = new Matrix44(matrix442);
                }

                class1727.verticeX = (int) (matrix442.getFaceList()[12] + matrix442.getFaceList()[0] * this.verticesX[class1726.appliesOnVertex] + matrix442.getFaceList()[4] * this.verticesY[class1726.appliesOnVertex] + matrix442.getFaceList()[8] * this.verticesZ[class1726.appliesOnVertex]);
                class1727.verticeY = (int) (matrix442.getFaceList()[13] + matrix442.getFaceList()[1] * this.verticesX[class1726.appliesOnVertex] + matrix442.getFaceList()[5] * this.verticesY[class1726.appliesOnVertex] + matrix442.getFaceList()[9] * this.verticesZ[class1726.appliesOnVertex]);
                class1727.verticeZ = (int) (matrix442.getFaceList()[14] + matrix442.getFaceList()[2] * this.verticesX[class1726.appliesOnVertex] + matrix442.getFaceList()[6] * this.verticesY[class1726.appliesOnVertex] + matrix442.getFaceList()[10] * this.verticesZ[class1726.appliesOnVertex]);
            }
        }

    }

    @Override
    public boolean calculateProjection(int i_1, int i_2, Matrix43 matrix44var3, boolean bool4, int sizeShift) {
        Matrix44 matrix446 = this.toolkit.aClass3848683;
        matrix446.fromMatrix(matrix44var3);
        matrix446.multiplyBy(this.toolkit.transformMatrix);
        boolean bool7 = false;
        float f_8 = Float.MAX_VALUE;
        float f_9 = -3.4028235E38f;
        float f_10 = Float.MAX_VALUE;
        float f_11 = -3.4028235E38f;
        if (!this.boundsUpdated) {
            calculateBounds();
        }

        int modelCenterX = this.maxX - this.minX >> 1;
        int modelCenterY = this.maxY - this.minY >> 1;
        int modelCenterZ = this.maxZ - this.minZ >> 1;
        int x = modelCenterX + this.minX;
        int y = modelCenterY + this.minY;
        int z = modelCenterZ + this.minZ;
        int lowX = x - (modelCenterX << sizeShift);
        int lowY = y - (modelCenterY << sizeShift);
        int lowZ = z - (modelCenterZ << sizeShift);
        int highX = x + (modelCenterX << sizeShift);
        int highY = y + (modelCenterY << sizeShift);
        int highZ = z + (modelCenterZ << sizeShift);
        PROJ_X[0] = lowX;
        PROJ_Y[0] = lowY;
        PROJ_Z[0] = lowZ;
        PROJ_X[1] = highX;
        PROJ_Y[1] = lowY;
        PROJ_Z[1] = lowZ;
        PROJ_X[2] = lowX;
        PROJ_Y[2] = highY;
        PROJ_Z[2] = lowZ;
        PROJ_X[3] = highX;
        PROJ_Y[3] = highY;
        PROJ_Z[3] = lowZ;
        PROJ_X[4] = lowX;
        PROJ_Y[4] = lowY;
        PROJ_Z[4] = highZ;
        PROJ_X[5] = highX;
        PROJ_Y[5] = lowY;
        PROJ_Z[5] = highZ;
        PROJ_X[6] = lowX;
        PROJ_Y[6] = highY;
        PROJ_Z[6] = highZ;
        PROJ_X[7] = highX;
        PROJ_Y[7] = highY;
        PROJ_Z[7] = highZ;

        for (int index = 0; index < 8; index++) {
        	float pX = PROJ_X[index];
        	float pY = PROJ_Y[index];
        	float pZ = PROJ_Z[index];
        	float f_28 = matrix446.getFaceList()[2] * pX + matrix446.getFaceList()[6] * pY + matrix446.getFaceList()[10] * pZ + matrix446.getFaceList()[14];
        	float f_29 = matrix446.getFaceList()[3] * pX + matrix446.getFaceList()[7] * pY + matrix446.getFaceList()[11] * pZ + matrix446.getFaceList()[15];
            if (f_28 >= -f_29) {
            	float f_30 = matrix446.getFaceList()[0] * pX + matrix446.getFaceList()[4] * pY + matrix446.getFaceList()[8] * pZ + matrix446.getFaceList()[12];
            	float f_31 = matrix446.getFaceList()[1] * pX + matrix446.getFaceList()[5] * pY + matrix446.getFaceList()[9] * pZ + matrix446.getFaceList()[13];
                float f_32 = this.toolkit.aFloat8797 + this.toolkit.viewportCenterX * f_30 / f_29;
                float f_33 = this.toolkit.aFloat8741 + this.toolkit.viewportCenterY * f_31 / f_29;
                if (f_32 < f_8) {
                    f_8 = f_32;
                }

                if (f_32 > f_9) {
                    f_9 = f_32;
                }

                if (f_33 < f_10) {
                    f_10 = f_33;
                }

                if (f_33 > f_11) {
                    f_11 = f_33;
                }

                bool7 = true;
            }
        }

        if (bool7 && i_1 > f_8 && i_1 < f_9 && i_2 > f_10 && i_2 < f_11) {
            if (bool4) {
                return true;
            }

            if (anIntArray8928.length < this.anInt8906) {
                anIntArray8928 = new int[this.anInt8906];
                anIntArray8958 = new int[this.anInt8906];
            }

            for (int index = 0; index < this.maxDepth; index++) {
            	float f_25 = this.verticesX[index];
            	float f_26 = this.verticesY[index];
            	float f_27 = this.verticesZ[index];
            	float f_28 = matrix446.getFaceList()[2] * f_25 + matrix446.getFaceList()[6] * f_26 + matrix446.getFaceList()[10] * f_27 + matrix446.getFaceList()[14];
            	float f_29 = matrix446.getFaceList()[3] * f_25 + matrix446.getFaceList()[7] * f_26 + matrix446.getFaceList()[11] * f_27 + matrix446.getFaceList()[15];
                int i_38;
                int i_39;
                if (f_28 >= -f_29) {
                	float f_30 = matrix446.getFaceList()[0] * f_25 + matrix446.getFaceList()[4] * f_26 + matrix446.getFaceList()[8] * f_27 + matrix446.getFaceList()[12];
                	float f_31 = matrix446.getFaceList()[1] * f_25 + matrix446.getFaceList()[5] * f_26 + matrix446.getFaceList()[9] * f_27 + matrix446.getFaceList()[13];
                    i_38 = this.totalFaceCounts[index];
                    i_39 = this.totalFaceCounts[index + 1];

                    for (int i_34 = i_38; i_34 < i_39; i_34++) {
                        int i_35 = this.aShortArray8948[i_34] - 1;
                        if (i_35 == -1) {
                            break;
                        }

                        anIntArray8928[i_35] = (int) (this.toolkit.aFloat8797 + this.toolkit.viewportCenterX * f_30 / f_29);
                        anIntArray8958[i_35] = (int) (this.toolkit.aFloat8741 + this.toolkit.viewportCenterY * f_31 / f_29);
                    }
                } else {
                    int i_36 = this.totalFaceCounts[index];
                    int i_37 = this.totalFaceCounts[index + 1];

                    for (i_38 = i_36; i_38 < i_37; i_38++) {
                        i_39 = this.aShortArray8948[i_38] - 1;
                        if (i_39 == -1) {
                            break;
                        }

                        anIntArray8928[this.aShortArray8948[i_38] - 1] = -999999;
                    }
                }
            }

            for (int index = 0; index < this.numFaces; index++) {
                if (anIntArray8928[this.normalsFaceA[index]] != -999999 &&
                	anIntArray8928[this.normalsFaceB[index]] != -999999 &&
                	anIntArray8928[this.normalsFaceC[index]] != -999999 &&
                	method14276(i_1, i_2, 
                			anIntArray8958[this.normalsFaceA[index]],
                			anIntArray8958[this.normalsFaceB[index]],
                			anIntArray8958[this.normalsFaceC[index]],
                			anIntArray8928[this.normalsFaceA[index]],
                			anIntArray8928[this.normalsFaceB[index]],
                			anIntArray8928[this.normalsFaceC[index]])) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean method14276(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8) {
        return (i_2 >= i_3 || i_2 >= i_4 || i_2 >= i_5) && ((i_2 <= i_3 || i_2 <= i_4 || i_2 <= i_5) && ((i_1 >= i_6 || i_1 >= i_7 || i_1 >= i_8) && (i_1 <= i_6 || i_1 <= i_7 || i_1 <= i_8)));
    }

    @Override
    public Shadow createShadowSprite(Shadow shadow) {
        if (this.anInt8906 == 0) {
            return null;
        } 
        if (!this.boundsUpdated) {
            calculateBounds();
        }

        int minSx;
        int maxSx;
        if (this.toolkit.shadowOffsetX > 0) {
            minSx = this.minX - (this.toolkit.shadowOffsetX * this.maxY >> 8) >> this.toolkit.shadowSizeShift;
            maxSx = this.maxX - (this.toolkit.shadowOffsetX * this.minY >> 8) >> this.toolkit.shadowSizeShift;
        } else {
            minSx = this.minX - (this.toolkit.shadowOffsetX * this.minY >> 8) >> this.toolkit.shadowSizeShift;
            maxSx = this.maxX - (this.toolkit.shadowOffsetX * this.maxY >> 8) >> this.toolkit.shadowSizeShift;
        }

        int minSz;
        int maxSz;
        if (this.toolkit.shadowOffsetZ > 0) {
            minSz = this.minZ - (this.toolkit.shadowOffsetZ * this.maxY >> 8) >> this.toolkit.shadowSizeShift;
            maxSz = this.maxZ - (this.toolkit.shadowOffsetZ * this.minY >> 8) >> this.toolkit.shadowSizeShift;
        } else {
            minSz = this.minZ - (this.toolkit.shadowOffsetZ * this.minY >> 8) >> this.toolkit.shadowSizeShift;
            maxSz = this.maxZ - (this.toolkit.shadowOffsetZ * this.maxY >> 8) >> this.toolkit.shadowSizeShift;
        }

        int width = maxSx - minSx + 1;
        int length = maxSz - minSz + 1;
        ShadowDX directxShadow = (ShadowDX) shadow;
        ShadowDX shadow2;
        if (directxShadow != null && directxShadow.isClean(width, length)) {
            shadow2 = directxShadow;
            directxShadow.clearPixels();
        } else {
            shadow2 = new ShadowDX(width, length);
        }

        shadow2.setSize(minSx, minSz, maxSx, maxSz);
        method14277(shadow2);
        return shadow2;
    }

    public void method14277(ShadowDX shadow) {
        if (anIntArray8928.length < this.anInt8906) {
            anIntArray8928 = new int[this.anInt8906];
            anIntArray8958 = new int[this.anInt8906];
        }

        int i_2;
        int i_6;
        int i_7;
        int i_8;
        for (i_2 = 0; i_2 < this.maxDepth; i_2++) {
            int i_3 = (this.verticesX[i_2] - (this.toolkit.shadowOffsetX * this.verticesY[i_2] >> 8) >> this.toolkit.shadowSizeShift) - shadow.minX;
            int i_4 = (this.verticesZ[i_2] - (this.toolkit.shadowOffsetZ * this.verticesY[i_2] >> 8) >> this.toolkit.shadowSizeShift) - shadow.minZ;
            int i_5 = this.totalFaceCounts[i_2];
            i_6 = this.totalFaceCounts[i_2 + 1];

            for (i_7 = i_5; i_7 < i_6; i_7++) {
                i_8 = this.aShortArray8948[i_7] - 1;
                if (i_8 == -1) {
                    break;
                }

                anIntArray8928[i_8] = i_3;
                anIntArray8958[i_8] = i_4;
            }
        }

        for (i_2 = 0; i_2 < this.anInt8963; i_2++) {
            short s_12 = this.normalsFaceA[i_2];
            short s_13 = this.normalsFaceB[i_2];
            short s_14 = this.normalsFaceC[i_2];
            i_6 = anIntArray8928[s_12];
            i_7 = anIntArray8928[s_13];
            i_8 = anIntArray8928[s_14];
            int i_9 = anIntArray8958[s_12];
            int i_10 = anIntArray8958[s_13];
            int i_11 = anIntArray8958[s_14];
            if ((i_6 - i_7) * (i_10 - i_11) - (i_10 - i_9) * (i_8 - i_7) > 0) {
            	/**
            	 * Crashes multi threaded maploading,
            	 * even with the 876 shadow code.
            	 * Probably has to do with the static variables
            	 * above that are local in 876
            	 */
            	if(!Loader.MULTI_THREADED_MAPLOADING) { 
            		shadow.calculateShadowTriangle(i_9, i_10, i_11, i_6, i_7, i_8);
            	}
            }
        }

    }

    @Override
    public void drawMatrixThreaded(Matrix43 matrix44var1, BoundingBoxPolygon class275Sub52, int i_3) {
//		if (aClass34_8938 != null) {
//			aClass34_8938.method841();
//		}

        if (this.anInt8906 != 0) {
            Matrix44 matrix444 = this.toolkit.aClass3848724;
            Matrix44 matrix445 = this.toolkit.aClass3848683;
            Matrix44 matrix446 = this.toolkit.aClass3848715;
            matrix445.fromMatrix(matrix44var1);
            matrix446.copyFrom(matrix445);
            matrix446.multiplyBy(this.toolkit.transformMatrix);
            if (!this.boundsUpdated) {
                calculateBounds();
            }

            float[] floats_7 = this.toolkit.aFloatArray8717;
            matrix445.scale(0.0F, this.minY, 0.0F, floats_7);
            float f_8 = floats_7[0];
            float f_9 = floats_7[1];
            float f_10 = floats_7[2];
            matrix445.scale(0.0F, this.maxY, 0.0F, floats_7);
            float f_11 = floats_7[0];
            float f_12 = floats_7[1];
            float f_13 = floats_7[2];

            for (int i_14 = 0; i_14 < 6; i_14++) {
                float[] floats_33 = this.toolkit.aFloatArrayArray8730[i_14];
                float f_16 = floats_33[0] * f_8 + floats_33[1] * f_9 + floats_33[2] * f_10 + floats_33[3] + this.size2d;
                float f_17 = floats_33[0] * f_11 + floats_33[1] * f_12 + floats_33[2] * f_13 + floats_33[3] + this.size2d;
                if (f_16 < 0.0F && f_17 < 0.0F) {
                    return;
                }
            }

            if (class275Sub52 != null) {
                boolean bool34 = false;
                boolean bool15 = true;
                int i_35 = this.minX + this.maxX >> 1;
                int i_36 = this.minZ + this.maxZ >> 1;
                int i_19 = this.minY;
                float f_21 = matrix446.getFaceList()[0] * i_35 + matrix446.getFaceList()[4] * i_19 + matrix446.getFaceList()[8] * i_36 + matrix446.getFaceList()[12];
                float f_22 = matrix446.getFaceList()[1] * i_35 + matrix446.getFaceList()[5] * i_19 + matrix446.getFaceList()[9] * i_36 + matrix446.getFaceList()[13];
                float f_23 = matrix446.getFaceList()[2] * i_35 + matrix446.getFaceList()[6] * i_19 + matrix446.getFaceList()[10] * i_36 + matrix446.getFaceList()[14];
                float f_24 = matrix446.getFaceList()[3] * i_35 + matrix446.getFaceList()[7] * i_19 + matrix446.getFaceList()[11] * i_36 + matrix446.getFaceList()[15];
                if (f_23 >= -f_24) {
                    class275Sub52.left = (int) (this.toolkit.aFloat8797 + this.toolkit.viewportCenterX * f_21 / f_24);
                    class275Sub52.bottom = (int) (this.toolkit.aFloat8741 + this.toolkit.viewportCenterY * f_22 / f_24);
                } else {
                    bool34 = true;
                }

                i_19 = this.maxY;
                float f_25 = matrix446.getFaceList()[0] * i_35 + matrix446.getFaceList()[4] * i_19 + matrix446.getFaceList()[8] * i_36 + matrix446.getFaceList()[12];
                float f_26 = matrix446.getFaceList()[1] * i_35 + matrix446.getFaceList()[5] * i_19 + matrix446.getFaceList()[9] * i_36 + matrix446.getFaceList()[13];
                float f_27 = matrix446.getFaceList()[2] * i_35 + matrix446.getFaceList()[6] * i_19 + matrix446.getFaceList()[10] * i_36 + matrix446.getFaceList()[14];
                float f_28 = matrix446.getFaceList()[3] * i_35 + matrix446.getFaceList()[7] * i_19 + matrix446.getFaceList()[11] * i_36 + matrix446.getFaceList()[15];
                if (f_27 >= -f_28) {
                    class275Sub52.right = (int) (this.toolkit.aFloat8797 + this.toolkit.viewportCenterX * f_25 / f_28);
                    class275Sub52.top = (int) (this.toolkit.aFloat8741 + this.toolkit.viewportCenterY * f_26 / f_28);
                } else {
                    bool34 = true;
                }

                float f_29;
                float f_30;
                if (bool34) {
                    if (f_23 < -f_24 && f_27 < -f_28) {
                        bool15 = false;
                    } else {
                        float f_31;
                        float f_32;
                        if (f_23 < -f_24) {
                            f_29 = (f_23 + f_24) / (f_27 + f_28) - 1.0F;
                            f_30 = f_21 + f_29 * (f_25 - f_21);
                            f_31 = f_22 + f_29 * (f_26 - f_22);
                            f_32 = f_24 + f_29 * (f_28 - f_24);
                            class275Sub52.left = (int) (this.toolkit.aFloat8797 + this.toolkit.viewportCenterX * f_30 / f_32);
                            class275Sub52.bottom = (int) (this.toolkit.aFloat8741 + this.toolkit.viewportCenterY * f_31 / f_32);
                        } else if (f_27 < -f_28) {
                            f_29 = (f_27 + f_28) / (f_23 + f_24) - 1.0F;
                            f_30 = f_25 + f_29 * (f_21 - f_25);
                            f_31 = f_26 + f_29 * (f_22 - f_26);
                            f_32 = f_28 + f_29 * (f_24 - f_28);
                            class275Sub52.right = (int) (this.toolkit.aFloat8797 + this.toolkit.viewportCenterX * f_30 / f_32);
                            class275Sub52.top = (int) (this.toolkit.aFloat8741 + this.toolkit.viewportCenterY * f_31 / f_32);
                        }
                    }
                }

                if (bool15) {
                    if (f_23 / f_24 > f_27 / f_28) {
                        f_29 = f_21 + matrix444.getFaceList()[0] * this.size2d + matrix444.getFaceList()[12];
                        f_30 = f_24 + matrix444.getFaceList()[3] * this.size2d + matrix444.getFaceList()[15];
                        class275Sub52.size = (int) (this.toolkit.aFloat8797 - class275Sub52.left + this.toolkit.viewportCenterX * f_29 / f_30);
                    } else {
                        f_29 = f_25 + matrix444.getFaceList()[0] * this.size2d + matrix444.getFaceList()[12];
                        f_30 = f_28 + matrix444.getFaceList()[3] * this.size2d + matrix444.getFaceList()[15];
                        class275Sub52.size = (int) (this.toolkit.aFloat8797 - class275Sub52.right + this.toolkit.viewportCenterX * f_29 / f_30);
                    }

                    class275Sub52.positionCalculated = true;
                }
            }

            this.toolkit.setPolygonMode(i_3);
            method14278(matrix44var1);
            this.toolkit.setPolygonMode(0);
            matrix445.fromMatrix(matrix44var1);
            matrix445.multiplyBy(this.toolkit.particleMatrix);
            method14275(matrix445);
        }

    }

    public void method14278(Matrix43 matrix44var1) {
        if (this.anInt8963 != 0 && method14279() && method14271()) {
            this.toolkit.method14004();
            AbstractEnvironmentShader perspective = this.toolkit.environmentShader;
            this.toolkit.setStreamSource(0, this.verticeManager.streamSource);
            this.toolkit.setStreamSource(1, this.aClass638913.streamSource);
            this.toolkit.setStreamSource(2, this.aClass638930.streamSource);
            this.toolkit.method13997(this.normalsBuffer.anInterface32407);
            this.toolkit.aClass3848683.fromMatrix(matrix44var1);
            perspective.loadWorldViewProjection(this.toolkit.aClass3848683);
            perspective.textureCoordinateMatrix.identity();
            if (this.toolkit.missingWaterColor) {
                WaterTile class903 = this.toolkit.getRenderedWaterTile();
                perspective.heightFogPlane.set(0.0F, 1.0F, 0.0F, (-this.toolkit.renderedWaterColor));
                perspective.heightFogPlane.scale(3.0F / class903.scale);
                perspective.heightFogColor.set((class903.color >> 16 & 0xff) / 255.0F, (class903.color >> 8 & 0xff) / 255.0F, (class903.color & 0xff) / 255.0F);
            } else {
                perspective.heightFogPlane.set(0.0F, 0.0F, 0.0F, 0.0F);
                perspective.heightFogColor.set(0.0F, 0.0F, 0.0F);
            }

            if (this.toolkit.fogDistance > 0) {
                perspective.distanceFogPlane.set(0.0F, 0.0F, 1.0F, -this.toolkit.fogStart);
                perspective.distanceFogColor.set((this.toolkit.fogColorRgb >> 16 & 0xff) / 255.0F, (this.toolkit.fogColorRgb >> 8 & 0xff) / 255.0F, (this.toolkit.fogColorRgb & 0xff) / 255.0F);
                this.toolkit.aClass3848683.fromMatrix(matrix44var1);
                this.toolkit.aClass3848683.rotate();
                perspective.heightFogPlane.concat(this.toolkit.aClass3848683);
                this.toolkit.aClass3848683.fromMatrix(matrix44var1);
                this.toolkit.aClass3848683.multiplyBy(this.toolkit.particleMatrix);
                this.toolkit.aClass3848683.rotate();
                perspective.distanceFogPlane.concat(this.toolkit.aClass3848683);
                perspective.distanceFogPlane.scale(1.0F / (this.toolkit.fogEnd - this.toolkit.fogStart));
            } else {
                perspective.distanceFogPlane.set(0.0F, 0.0F, 0.0F, 0.0F);
                perspective.distanceFogColor.set(0.0F, 0.0F, 0.0F);
            }

            if ((this.preferenceFunctionMask & 0x37) == 0) {
                this.toolkit.setVertexDeclaration(this.toolkit.aClass708721);
                if (this.toolkit.enableLights) {
                    this.toolkit.toggleLights(false);
                }

                for (int i_12 = 0; i_12 < this.anIntArray8952.length; i_12++) {
                    int i_4 = this.anIntArray8944[i_12];
                    int i_5 = this.anIntArray8944[i_12 + 1];
                    int materialId = this.facesMaterial[i_4] & 0xffff;
                    boolean transparent = false;
                    if (materialId != 0xFFFF) {
                        perspective.textureSampler = this.toolkit.materialCache.getTextureSampler(materialId);
                        MaterialType material = this.toolkit.materialProvider.getMaterial(materialId);
                        transparent = !MaterialTypeList.isOpaqueEffect(material.effectId);
                        perspective.textureCoordinateMatrix.getFaceList()[12] = (this.toolkit.animatingCycle % 128000) / 1000.0F * material.textureSpeedU / 64.0F % 1.0F;
                        perspective.textureCoordinateMatrix.getFaceList()[13] = (this.toolkit.animatingCycle % 128000) / 1000.0F * material.textureSpeedV / 64.0F % 1.0F;
                    } else {
                        perspective.textureSampler = this.toolkit.activeTextureSampler;
                        float[] floats_18 = perspective.textureCoordinateMatrix.getFaceList();
                        perspective.textureCoordinateMatrix.getFaceList()[13] = 0.0F;
                        floats_18[12] = 0.0F;
                    }

                    perspective.anInt467 = this.anIntArray8952[i_12];
                    perspective.anInt468 = this.anIntArray8946[i_12];
                    perspective.anInt469 = i_4 * 3;
                    perspective.anInt470 = i_5 - i_4;
                    perspective.loadUnlitProgram(transparent);
                }
            } else {
                this.toolkit.setStreamSource(3, this.lightManager.streamSource);
                this.toolkit.setVertexDeclaration(this.toolkit.aClass708836);
                Matrix43 matrix = this.toolkit.unsure;
                matrix.copyFrom(matrix44var1);
                matrix.updateIdentitiyMatrix();
                int lightCount = 0;
                if (this.toolkit.enableLights) {
                    this.toolkit.toggleLights(true);
                } else {
                    perspective.sunPosition.set(this.toolkit.sunPosition[0], this.toolkit.sunPosition[1], this.toolkit.sunPosition[2]);
                    perspective.sunPosition.transform(matrix);
                    perspective.sunColor.set(this.toolkit.sunColorIntensity * this.toolkit.baseLightColorRed, this.toolkit.sunColorIntensity * this.toolkit.baseLightColorGreen, this.toolkit.sunColorIntensity * this.toolkit.baseLightColorBlue);
                    perspective.antiSunColor.set(-this.toolkit.antiSunColorIntensity * this.toolkit.baseLightColorRed, -this.toolkit.antiSunColorIntensity * this.toolkit.baseLightColorGreen, -this.toolkit.antiSunColorIntensity * this.toolkit.baseLightColorBlue);
                    perspective.lightColor.set(this.toolkit.lightIntensity * this.toolkit.baseLightColorRed, this.toolkit.lightIntensity * this.toolkit.baseLightColorGreen, this.toolkit.lightIntensity * this.toolkit.baseLightColorBlue);
                    if (this.toolkit.currentSceneLightCount > 0) {
                        lightCount = this.toolkit.currentSceneLightCount;
                        Vector3 lightVector = this.toolkit.pointLightVector;
                        for (int index = 0; index < lightCount; index++) {
                            PointLight lightNode = this.toolkit.lightSources[index];
                            int color = lightNode.getColor();
                            lightVector.copy(lightNode.position);
                            lightVector.transformWithTranslation(matrix);
                            perspective.pointLightsPosAndRadiusSq[index * 4] = lightVector.x;
                            perspective.pointLightsPosAndRadiusSq[index * 4 + 1] = lightVector.y;
                            perspective.pointLightsPosAndRadiusSq[index * 4 + 2] = lightVector.z;
                            perspective.pointLightsPosAndRadiusSq[index * 4 + 3] = (lightNode.getSize2d() * lightNode.getSize2d());
                            float alphaPercentage = lightNode.getAlpha() / 255.0F;
                            perspective.pointLightsDiffuseColor[index * 4] = (color >> 16 & 0xff) * alphaPercentage;
                            perspective.pointLightsDiffuseColor[index * 4 + 1] = (color >> 8 & 0xff) * alphaPercentage;
                            perspective.pointLightsDiffuseColor[index * 4 + 2] = (color & 0xff) * alphaPercentage;
                            perspective.pointLightsDiffuseColor[index * 4 + 3] = 1.0F;
                        }
                    }
                }

                for (int i_5 = 0; i_5 < this.anIntArray8952.length; i_5++) {
                    int index = this.anIntArray8944[i_5];
                    int i_7 = this.anIntArray8944[i_5 + 1];
                    int faceMaterial = this.facesMaterial[index] & 0xffff;
                    byte effectId = 11;
                    if (faceMaterial != 65535) {
                        perspective.textureSampler = this.toolkit.materialCache.getTextureSampler(faceMaterial);
                        MaterialType material = this.toolkit.materialProvider.getMaterial(faceMaterial);
                        effectId = material.effectId;
                        perspective.indentifyMaterialEffect(material);
                        perspective.textureCoordinateMatrix.getFaceList()[12] = (this.toolkit.animatingCycle % 128000) / 1000.0F * material.textureSpeedU / 64.0F % 1.0F;
                        perspective.textureCoordinateMatrix.getFaceList()[13] = (this.toolkit.animatingCycle % 128000) / 1000.0F * material.textureSpeedV / 64.0F % 1.0F;
                    } else {
                        perspective.textureSampler = this.toolkit.activeTextureSampler;
                        float[] floats_20 = perspective.textureCoordinateMatrix.getFaceList();
                        perspective.textureCoordinateMatrix.getFaceList()[13] = 0.0F;
                        floats_20[12] = 0.0F;
                    }

                    perspective.anInt467 = this.anIntArray8952[i_5];
                    perspective.anInt468 = this.anIntArray8946[i_5];
                    perspective.anInt469 = index * 3;
                    perspective.anInt470 = i_7 - index;
                    switch (effectId) {
                        case 1:
                            perspective.eyePos.set(this.toolkit.aClass3848814.getFaceList()[12], this.toolkit.aClass3848814.getFaceList()[13], this.toolkit.aClass3848814.getFaceList()[14]);
                            perspective.eyePos.transformWithTranslation(matrix);
                            perspective.loadSpecularPointlight(lightCount);
                            break;
                        case 2:
                        case 3:
                        case 4:
                        default:
                            perspective.loadStandartPointlight(lightCount);
                            break;
                        case 5:
                            if (!this.toolkit.enableLights) {
                                NativeWaterfall class41Sub1Sub221 = this.toolkit.nativeWaterfall;
                                MaterialType class16911 = this.toolkit.materialProvider.getMaterial(faceMaterial);
                                class41Sub1Sub221.method15550(class16911.effectParam1);
                                class41Sub1Sub221.aClass38410146.fromMatrix(matrix44var1);
                                class41Sub1Sub221.aClass38410138.fromMatrix(matrix44var1);
                                class41Sub1Sub221.aClass38410138.multiplyBy(this.toolkit.aClass3848729);
                                class41Sub1Sub221.anInt10152 = this.anIntArray8952[i_5];
                                class41Sub1Sub221.anInt10153 = this.anIntArray8946[i_5];
                                class41Sub1Sub221.anInt10142 = index * 3;
                                class41Sub1Sub221.anInt10155 = i_7 - index;
                                class41Sub1Sub221.method15549();
                            } else {
                                perspective.loadStandartPointlight(lightCount);
                            }
                            break;
                        case 6:
                            perspective.loadUnlitProgram(!MaterialTypeList.isOpaqueEffect(effectId));
                            break;
                        case 7:
                            perspective.eyePos.set(this.toolkit.aClass3848814.getFaceList()[12], this.toolkit.aClass3848814.getFaceList()[13], this.toolkit.aClass3848814.getFaceList()[14]);
                            perspective.eyePos.transformWithTranslation(matrix);
                            perspective.aClass384465.fromMatrix(matrix44var1);
                            perspective.anInterface31451 = this.toolkit.getEnvironmentMapSampler();
                            perspective.loadPointlightEnvironmentMapping(lightCount);
                    }
                }
            }
            method14274();
        }
    }

    @Override
    public boolean startAnimation() {
        if (this.verticesByLabel == null) {
            return false;
        } else {
            for (int i_1 = 0; i_1 < this.numVertices; i_1++) {
                this.verticesX[i_1] <<= 4;
                this.verticesY[i_1] <<= 4;
                this.verticesZ[i_1] <<= 4;
            }

            originX = 0;
            originY = 0;
            originZ = 0;
            return true;
        }
    }

    public boolean method14279() {
        boolean bool1 = !this.aClass638913.allocated;
        boolean bool2 = (this.preferenceFunctionMask & 0x37) != 0 && !this.lightManager.allocated;
        boolean verticesClean = !this.verticeManager.allocated;
        boolean bool4 = !this.aClass638930.allocated;
        if (!verticesClean && !bool1 && !bool2 && !bool4) {
            return true;
        } else {
            int i_8;
            long long_9;
            ByteBuffer modelBuffer;
            int i_36;
            if (verticesClean && this.vertexIndices != null) {
                if (this.verticeManager.anInterface4664 == null) {
                    this.verticeManager.anInterface4664 = this.toolkit.createStream(this.aBool8904);
                }

                NativeStream modelBufferSource = this.verticeManager.anInterface4664;
                modelBufferSource.create(this.anInt8906 * 12, 12);
                Unsafe unsafe7 = this.toolkit.theUnsafe;
                if (unsafe7 != null) {
                    i_8 = this.anInt8906 * 12;
                    long_9 = modelBufferSource.getDataStorePointer(0, i_8);

                    for (int i_11 = 0; i_11 < this.anInt8906; i_11++) {
                        short s_32 = this.vertexIndices[i_11];
                        unsafe7.putFloat(long_9, this.verticesX[s_32]);
                        long_9 += 4L;
                        unsafe7.putFloat(long_9, this.verticesY[s_32]);
                        long_9 += 4L;
                        unsafe7.putFloat(long_9, this.verticesZ[s_32]);
                        long_9 += 4L;
                    }

                    modelBufferSource.unlock();
                } else {
                    modelBuffer = this.toolkit.directBuffer;
                    modelBuffer.clear();

                    for (i_36 = 0; i_36 < this.anInt8906; i_36++) {
                        modelBuffer.putFloat(this.verticesX[this.vertexIndices[i_36]]);
                        modelBuffer.putFloat(this.verticesY[this.vertexIndices[i_36]]);
                        modelBuffer.putFloat(this.verticesZ[this.vertexIndices[i_36]]);
                    }

                    modelBufferSource.upload(0, modelBuffer.position(), this.toolkit.directBufferAddress);
                }

                this.verticeManager.streamSource = modelBufferSource;
                this.verticeManager.allocated = true;
            }

            short[] shorts_30;
            float f_46;
            float f_50;
            if (bool1) {
                if (this.aClass638913.anInterface4664 == null) {
                    this.aClass638913.anInterface4664 = this.toolkit.createStream(this.aBool8904);
                }

                NativeStream modelBufferSource = this.aClass638913.anInterface4664;
                modelBufferSource.create(this.anInt8906 * 4, 4);
                Unsafe unsafe7 = this.toolkit.theUnsafe;
                float baseIntensity;
                int b;
                short[] shorts_31;
                if (unsafe7 != null) {
                    i_8 = this.anInt8906 * 4;
                    long_9 = modelBufferSource.getDataStorePointer(0, i_8);
                    if ((this.preferenceFunctionMask & 0x37) == 0) {
                        short[] shorts_13;
                        byte[] bytes_14;
                        short[] shorts_42;
                        if (this.aClass3558962 != null) {
                            shorts_31 = this.aClass3558962.normalsX;
                            shorts_42 = this.aClass3558962.normalsY;
                            shorts_13 = this.aClass3558962.normalsZ;
                            bytes_14 = this.aClass3558962.magnitude;
                        } else {
                            shorts_31 = this.normalsXs;
                            shorts_42 = this.normalsYs;
                            shorts_13 = this.normalsZs;
                            bytes_14 = this.normalsMagnitudes;
                        }

                        float f_45 = this.toolkit.sunColorIntensity * 768.0F / this.contast;
                        f_46 = this.toolkit.antiSunColorIntensity * 768.0F / this.contast;

                        for (int index = 0; index < this.anInt8906; index++) {
                            short s_48 = this.alphaIndices[index];
                            int i_49 = getMaterialColor(this.facesColor[s_48] & 0xffff, this.facesMaterial[s_48], this.ambient);
                            float lightBlue = (i_49 >> 16 & 0xff) * this.toolkit.baseLightColorRed;
                            f_50 = (i_49 >> 8 & 0xff) * this.toolkit.baseLightColorGreen;
                            baseIntensity = (i_49 & 0xff) * this.toolkit.baseLightColorBlue;
                            byte b_51 = bytes_14[index];
                            float f_52;
                            if (b_51 == 0) {
                                f_52 = (this.toolkit.sunPosition[0] * shorts_31[index] + this.toolkit.sunPosition[1] * shorts_42[index] + this.toolkit.sunPosition[2] * shorts_13[index]) * 0.0026041667F;
                            } else {
                                f_52 = (this.toolkit.sunPosition[0] * shorts_31[index] + this.toolkit.sunPosition[1] * shorts_42[index] + this.toolkit.sunPosition[2] * shorts_13[index]) / (b_51 * 256.0F);
                            }

                            float f_53 = this.toolkit.lightIntensity + f_52 * (f_52 < 0.0F ? f_46 : f_45);
                            b = (int) (lightBlue * f_53);
                            if (b < 0) {
                                b = 0;
                            } else if (b > 255) {
                                b = 255;
                            }

                            int i_27 = (int) (f_50 * f_53);
                            if (i_27 < 0) {
                                i_27 = 0;
                            } else if (i_27 > 255) {
                                i_27 = 255;
                            }

                            int i_28 = (int) (baseIntensity * f_53);
                            if (i_28 < 0) {
                                i_28 = 0;
                            } else if (i_28 > 255) {
                                i_28 = 255;
                            }

                            unsafe7.putInt(long_9, 255 - this.facesAlpha[s_48] << 24 | b << 16 | i_27 << 8 | i_28);
                            long_9 += 4L;
                        }
                    } else {
                        for (int i_11 = 0; i_11 < this.anInt8906; i_11++) {
                            short s_32 = this.alphaIndices[i_11];
                            int i_57 = 255 - this.facesAlpha[s_32] << 24 | getMaterialColor(this.facesColor[s_32] & 0xffff, this.facesMaterial[s_32], this.ambient);
                            unsafe7.putInt(long_9, i_57);
                            long_9 += 4L;
                        }
                    }

                    modelBufferSource.unlock();
                } else {
                    modelBuffer = this.toolkit.directBuffer;
                    modelBuffer.clear();
                    if ((this.preferenceFunctionMask & 0x37) == 0) {
                        short[] shorts_10;
                        byte[] typesLight;
                        if (this.aClass3558962 != null) {
                            shorts_30 = this.aClass3558962.normalsX;
                            shorts_10 = this.aClass3558962.normalsY;
                            shorts_31 = this.aClass3558962.normalsZ;
                            typesLight = this.aClass3558962.magnitude;
                        } else {
                            shorts_30 = this.normalsXs;
                            shorts_10 = this.normalsYs;
                            shorts_31 = this.normalsZs;
                            typesLight = this.normalsMagnitudes;
                        }

                        float intensitySun = this.toolkit.sunColorIntensity * 768.0F / this.contast;
                        float intensityAntiSun = this.toolkit.antiSunColorIntensity * 768.0F / this.contast;

                        for (int index = 0; index < this.anInt8906; index++) {
                            short alphaIndex = this.alphaIndices[index];
                            int materialColorRgb = getMaterialColor(this.facesColor[alphaIndex] & 0xffff, this.facesMaterial[alphaIndex], this.ambient);
                            float lightRed = (materialColorRgb >> 16 & 0xff) * this.toolkit.baseLightColorRed;
                            float lightGreen = (materialColorRgb >> 8 & 0xff) * this.toolkit.baseLightColorGreen;
                            float lightBlue = (materialColorRgb & 0xff) * this.toolkit.baseLightColorBlue;
                            byte lightType = typesLight[index];
                            if (lightType == 0) {
                                baseIntensity = (this.toolkit.sunPosition[0] * shorts_30[index] + this.toolkit.sunPosition[1] * shorts_10[index] + this.toolkit.sunPosition[2] * shorts_31[index]) * 0.0026041667F;
                            } else {
                                baseIntensity = (this.toolkit.sunPosition[0] * shorts_30[index] + this.toolkit.sunPosition[1] * shorts_10[index] + this.toolkit.sunPosition[2] * shorts_31[index]) / (lightType * 256.0F);
                            }

                            float intensity = this.toolkit.lightIntensity + baseIntensity * (baseIntensity < 0.0F ? intensityAntiSun : intensitySun);
                            int r = (int) (lightRed * intensity);
                            if (r < 0) {
                                r = 0;
                            } else if (r > 255) {
                                r = 255;
                            }

                            int g = (int) (lightGreen * intensity);
                            if (g < 0) {
                                g = 0;
                            } else if (g > 255) {
                                g = 255;
                            }

                            b = (int) (lightBlue * intensity);
                            if (b < 0) {
                                b = 0;
                            } else if (b > 255) {
                                b = 255;
                            }

                            modelBuffer.putInt(255 - this.facesAlpha[alphaIndex] << 24 | r << 16 | g << 8 | b);
                        }
                    } else {
                        for (i_36 = 0; i_36 < this.anInt8906; i_36++) {
                            short alphaIndex = this.alphaIndices[i_36];
                            int color = 255 - this.facesAlpha[alphaIndex] << 24 | getMaterialColor(this.facesColor[alphaIndex] & 0xffff, this.facesMaterial[alphaIndex], this.ambient);
                            modelBuffer.putInt(color);
                        }
                    }

                    modelBufferSource.upload(0, modelBuffer.position(), this.toolkit.directBufferAddress);
                }

                this.aClass638913.streamSource = modelBufferSource;
                this.aClass638913.allocated = true;
            }

            if (bool2) {
                if (this.lightManager.anInterface4664 == null) {
                    this.lightManager.anInterface4664 = this.toolkit.createStream(this.aBool8904);
                }

                NativeStream modelBufferSource = this.lightManager.anInterface4664;
                modelBufferSource.create(this.anInt8906 * 12, 12);
                short[] shorts_39;
                byte[] bytes_40;
                short[] shorts_54;
                if (this.aClass3558962 != null) {
                    shorts_39 = this.aClass3558962.normalsX;
                    shorts_54 = this.aClass3558962.normalsY;
                    shorts_30 = this.aClass3558962.normalsZ;
                    bytes_40 = this.aClass3558962.magnitude;
                } else {
                    shorts_39 = this.normalsXs;
                    shorts_54 = this.normalsYs;
                    shorts_30 = this.normalsZs;
                    bytes_40 = this.normalsMagnitudes;
                }

                float f_41 = 3.0F / this.contast;
                float f_55 = 3.0F / (this.contast / 2 + this.contast);
                Unsafe buffer = this.toolkit.theUnsafe;
                if (buffer != null) {
                    int i_56 = this.anInt8906 * 12;
                    long byteIndex = modelBufferSource.getDataStorePointer(0, i_56);

                    for (int index = 0; index < this.anInt8906; index++) {
                        float lightRed = bytes_40[index] == 0 ? f_55 : f_41 / bytes_40[index];
                        float lightGreen = shorts_39[index] * lightRed;
                        float lightBlue = shorts_54[index] * lightRed;
                        f_50 = shorts_30[index] * lightRed;
                        buffer.putFloat(byteIndex, lightGreen);
                        byteIndex += 4L;
                        buffer.putFloat(byteIndex, lightBlue);
                        byteIndex += 4L;
                        buffer.putFloat(byteIndex, f_50);
                        byteIndex += 4L;
                    }

                    modelBufferSource.unlock();
                } else {
                    ByteBuffer bytebuffer44 = this.toolkit.directBuffer;
                    bytebuffer44.clear();

                    for (int i_15 = 0; i_15 < this.anInt8906; i_15++) {
                        f_46 = bytes_40[i_15] == 0 ? f_55 : f_41 / bytes_40[i_15];
                        float f_47 = shorts_39[i_15] * f_46;
                        float lightRed = shorts_54[i_15] * f_46;
                        float lightGreen = shorts_30[i_15] * f_46;
                        bytebuffer44.putFloat(f_47);
                        bytebuffer44.putFloat(lightRed);
                        bytebuffer44.putFloat(lightGreen);
                    }

                    modelBufferSource.upload(0, bytebuffer44.position(), this.toolkit.directBufferAddress);
                }

                this.lightManager.streamSource = modelBufferSource;
                this.lightManager.allocated = true;
            }

            if (bool4) {
                if (this.aClass638930.anInterface4664 == null) {
                    this.aClass638930.anInterface4664 = this.toolkit.createStream(this.aBool8904);
                }

                NativeStream modelBufferSource = this.aClass638930.anInterface4664;
                modelBufferSource.create(this.anInt8906 * 8, 8);
                Unsafe unsafe7 = this.toolkit.theUnsafe;
                if (unsafe7 != null) {
                    i_8 = this.anInt8906 * 8;
                    long_9 = modelBufferSource.getDataStorePointer(0, i_8);

                    for (int index = 0; index < this.anInt8906; index++) {
                        unsafe7.putFloat(long_9, this.activeUvU[index]);
                        long_9 += 4L;
                        unsafe7.putFloat(long_9, this.activeUvV[index]);
                        long_9 += 4L;
                    }

                    modelBufferSource.unlock();
                } else {
                    modelBuffer = this.toolkit.directBuffer;
                    modelBuffer.clear();

                    for (i_36 = 0; i_36 < this.anInt8906; i_36++) {
                        modelBuffer.putFloat(this.activeUvU[i_36]);
                        modelBuffer.putFloat(this.activeUvV[i_36]);
                    }

                    modelBufferSource.upload(0, modelBuffer.position(), this.toolkit.directBufferAddress);
                }

                this.aClass638930.streamSource = modelBufferSource;
                this.aClass638930.allocated = true;
            }

            return true;
        }
    }

    public void deallocate() {
        if (this.verticeManager != null) {
            this.verticeManager.deallocate();
        }

        if (this.aClass638930 != null) {
            this.aClass638930.deallocate();
        }

        if (this.aClass638913 != null) {
            this.aClass638913.deallocate();
        }

        if (this.lightManager != null) {
            this.lightManager.deallocate();
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer.deallocate();
        }

    }

    public void calculateBounds() {
        int minX = 32767;
        int minY = 32767;
        int minZ = 32767;
        int maxX = -32768;
        int maxY = -32768;
        int maxZ = -32768;
        int size2d = 0;
        int size3d = 0;

        for (int index = 0; index < this.maxDepth; index++) {
            int verticeX = this.verticesX[index];
            int verticeY = this.verticesY[index];
            int verticeZ = this.verticesZ[index];
            if (verticeX < minX) {
                minX = verticeX;
            }

            if (verticeX > maxX) {
                maxX = verticeX;
            }

            if (verticeY < minY) {
                minY = verticeY;
            }

            if (verticeY > maxY) {
                maxY = verticeY;
            }

            if (verticeZ < minZ) {
                minZ = verticeZ;
            }

            if (verticeZ > maxZ) {
                maxZ = verticeZ;
            }

            int size = verticeX * verticeX + verticeZ * verticeZ;
            if (size > size2d) {
                size2d = size;
            }

            size = verticeX * verticeX + verticeZ * verticeZ + verticeY * verticeY;
            if (size > size3d) {
                size3d = size;
            }
        }

        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.size2d = (int) (Math.sqrt(size2d) + 0.99D);
        this.size3d = (int) (Math.sqrt(size3d) + 0.99D);
        this.boundsUpdated = true;
    }

    public int getMaterialColor(int hsl, short materialId, int lightness) {
        int color = this.toolkit.hslToRgb[mixLightness(hsl, lightness)];
        if (materialId != -1) {
            MaterialType material = this.toolkit.materialProvider.getMaterial(materialId & 0xffff);
            int contrast = material.alpha & 0xff;
            int brightness;
            if (contrast != 0) {
                brightness = lightness * 131586;
                int transparency = 256 - contrast;
                color = ((brightness & 0xff00ff) * contrast + (color & 0xff00ff) * transparency & -16711936) + (contrast * (brightness & 0xff00) + transparency * (color & 0xff00) & 0xff0000) >> 8;
            }

            brightness = material.brightness & 0xff;
            if (brightness != 0) {
                brightness += 256;
                int r = brightness * ((color & 0xff0000) >> 16);
                if (r > 65535) {
                    r = 65535;
                }

                int g = brightness * ((color & 0xff00) >> 8);
                if (g > 65535) {
                    g = 65535;
                }

                int b = (color & 0xff) * brightness;
                if (b > 65535) {
                    b = 65535;
                }

                color = (b >> 8) + (g & 0xff00) + (r << 8 & 0xff0000);
            }
        }

        return color & 0xffffff;
    }



    @Override
    public void contourToGround(int i_1, int i_2, Ground class3903, Ground class3904, int i_5, int i_6, int i_7) {
        if (!this.boundsUpdated) {
            calculateBounds();
        }

        int i_8 = i_5 + this.minX;
        int i_9 = i_5 + this.maxX;
        int i_10 = i_7 + this.minZ;
        int i_11 = i_7 + this.maxZ;
        if (i_1 != 1 && i_1 != 2 && i_1 != 3 && i_1 != 5 || i_8 >= 0 && i_9 + class3903.tileUnits >> class3903.tileScale < class3903.width && i_10 >= 0 && i_11 + class3903.tileUnits >> class3903.tileScale < class3903.length) {
            if (i_1 != 4 && i_1 != 5) {
                i_8 >>= class3903.tileScale;
                i_9 = i_9 + (class3903.tileUnits - 1) >> class3903.tileScale;
                i_10 >>= class3903.tileScale;
                i_11 = i_11 + (class3903.tileUnits - 1) >> class3903.tileScale;
                if (class3903.getHeight(i_8, i_10) == i_6 && class3903.getHeight(i_9, i_10) == i_6 && class3903.getHeight(i_8, i_11) == i_6 && class3903.getHeight(i_9, i_11) == i_6) {
                    return;
                }
            } else if (class3904 == null || i_8 < 0 || class3904.tileUnits + i_9 >> class3904.tileScale >= class3904.width || i_10 < 0 || class3904.tileUnits + i_11 >> class3904.tileScale >= class3904.length) {
                return;
            }

            int i_12;
            if (i_1 == 1) {
                for (i_12 = 0; i_12 < this.maxDepth; i_12++) {
                    this.verticesY[i_12] = this.verticesY[i_12] + class3903.getAverageHeight(this.verticesX[i_12] + i_5, this.verticesZ[i_12] + i_7) - i_6;
                }
            } else {
                int i_13;
                int i_14;
                if (i_1 == 2) {
                    i_12 = this.minY;
                    if (i_12 == 0) {
                        return;
                    }

                    for (i_13 = 0; i_13 < this.maxDepth; i_13++) {
                        i_14 = (this.verticesY[i_13] << 16) / i_12;
                        if (i_14 < i_2) {
                            this.verticesY[i_13] += (class3903.getAverageHeight(this.verticesX[i_13] + i_5, this.verticesZ[i_13] + i_7) - i_6) * (i_2 - i_14) / i_2;
                        }
                    }
                } else {
                    int i_15;
                    if (i_1 == 3) {
                        i_12 = (i_2 & 0xff) * 4;
                        i_13 = (i_2 >> 8 & 0xff) * 4;
                        i_14 = (i_2 >> 16 & 0xff) << 6;
                        i_15 = (i_2 >> 24 & 0xff) << 6;
                        if (i_5 - (i_12 >> 1) < 0 || i_5 + class3903.tileUnits + (i_12 >> 1) >= class3903.width << class3903.tileScale || i_7 - (i_13 >> 1) < 0 || class3903.tileUnits + (i_13 >> 1) + i_7 >= class3903.length << class3903.tileScale) {
                            return;
                        }

                        hillChange(class3903, i_5, i_6, i_7, i_12, i_13, i_14, i_15);
                    } else if (i_1 == 4) {
                        i_12 = this.maxY - this.minY;

                        for (i_13 = 0; i_13 < this.maxDepth; i_13++) {
                            this.verticesY[i_13] += (class3904.getAverageHeight(this.verticesX[i_13] + i_5, this.verticesZ[i_13] + i_7) - i_6) + i_12;
                        }
                    } else if (i_1 == 5) {
                        i_12 = this.maxY - this.minY;

                        for (i_13 = 0; i_13 < this.maxDepth; i_13++) {
                            i_14 = i_5 + this.verticesX[i_13];
                            i_15 = i_7 + this.verticesZ[i_13];
                            int i_16 = class3903.getAverageHeight(i_14, i_15);
                            int i_17 = class3904.getAverageHeight(i_14, i_15);
                            int i_18 = i_16 - i_17 - i_2;
                            this.verticesY[i_13] = (i_18 * ((this.verticesY[i_13] << 8) / i_12) >> 8) - (i_6 - i_16);
                        }
                    }
                }
            }

            setVerticesInactive();
            this.boundsUpdated = false;
        }

    }

    @Override
    public void endAnimation() {
        for (int index = 0; index < this.numVertices; index++) {
            this.verticesX[index] = this.verticesX[index] + 7 >> 4;
            this.verticesY[index] = this.verticesY[index] + 7 >> 4;
            this.verticesZ[index] = this.verticesZ[index] + 7 >> 4;
        }

        setVerticesInactive();
        this.boundsUpdated = false;
    }

    public void method14285() {
        if ((this.preferenceFunctionMask & 0x37) != 0) {
            if (this.lightManager != null) {
                this.lightManager.allocated = false;
            }
        } else if (this.aClass638913 != null) {
            this.aClass638913.allocated = false;
        }
    }


    @Override
    public int getSize3d() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.size3d;
    }

    @Override
    public void applyShading(int functionMask) {
        if (this.verticeManager != null) {
            this.verticeManager.aBool663 = ModelFunctionMaskDX.method995(functionMask);
        }

        if (this.aClass638930 != null) {
            this.aClass638930.aBool663 = ModelFunctionMaskDX.method1056(functionMask);
        }

        if (this.aClass638913 != null) {
            this.aClass638913.aBool663 = ModelFunctionMaskDX.method1051(functionMask, this.preferenceFunctionMask);
        }

        if (this.lightManager != null) {
            this.lightManager.aBool663 = ModelFunctionMaskDX.deepCloneLights(functionMask, this.preferenceFunctionMask);
        }

        this.activeFunctionMask = functionMask;
        if (this.aClass3558962 != null && (this.activeFunctionMask & 0x10000) == 0) {
            this.normalsXs = this.aClass3558962.normalsX;
            this.normalsYs = this.aClass3558962.normalsY;
            this.normalsZs = this.aClass3558962.normalsZ;
            this.normalsMagnitudes = this.aClass3558962.magnitude;
            this.aClass3558962 = null;
        }

        this.aBool8919 = true;
        method14274();
    }

    @Override
    public int getMaxY() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.maxY;
    }


    @Override
    public int getMinY() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.minY;
    }


    @Override
    public int getContrast() {
        return this.contast;
    }



    @Override
    public void rotateWithNormals(int i_1) {
        int i_2 = MathUtils.SINE[i_1];
        int i_3 = MathUtils.COSINE[i_1];

        int i_4;
        int i_5;
        for (i_4 = 0; i_4 < this.maxDepth; i_4++) {
            i_5 = i_2 * this.verticesZ[i_4] + i_3 * this.verticesX[i_4] >> 14;
            this.verticesZ[i_4] = i_3 * this.verticesZ[i_4] - i_2 * this.verticesX[i_4] >> 14;
            this.verticesX[i_4] = i_5;
        }

        for (i_4 = 0; i_4 < this.anInt8906; i_4++) {
            i_5 = this.normalsZs[i_4] * i_2 + this.normalsXs[i_4] * i_3 >> 14;
            this.normalsZs[i_4] = (short) (this.normalsZs[i_4] * i_3 - this.normalsXs[i_4] * i_2 >> 14);
            this.normalsXs[i_4] = (short) i_5;
        }

        setVerticesInactive();
        method14285();
        this.boundsUpdated = false;
    }

    @Override
    public int getFunctionMask() {
        return this.activeFunctionMask;
    }




    @Override
    public int getMaxZ() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.maxZ;
    }


    @Override
    public boolean areTexturesCached() {
        if (this.facesMaterial != null) {
            for (int i_1 = 0; i_1 < this.facesMaterial.length; i_1++) {
                if (this.facesMaterial[i_1] != -1 && !this.toolkit.materialProvider.isCached(this.facesMaterial[i_1])) {
                    return false;
                }
            }

        }
        return true;
    }

    @Override
    public int getMinZ() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.minZ;
    }

    public int mixLightness(int hsl, int lightness) {
        int i_21 = lightness;
        i_21 = (hsl & 0x7f) * i_21 >> 7;
        if (i_21 < 2) {
            i_21 = 2;
        } else if (i_21 > 126) {
            i_21 = 126;
        }

        return i_21 + (hsl & 0xff80);
    }

    @Override
    public int getAmbient() {
        return this.ambient;
    }

    @Override
    public void animateTransform(int i_1, int[] ints_2, int i_3, int i_4, int i_5, boolean bool6, int i_7, int[] ints_8) {
        int i_31 = i_3;
        int i_41 = i_4;
        int i_51 = i_5;
        int i_9 = ints_2.length;
        int i_10;
        int i_11;
        int i_12;
        int[] ints_13;
        int i_14;
        int i_15;
        if (i_1 == 0) {
            i_31 <<= 4;
            i_41 <<= 4;
            i_51 <<= 4;
            i_10 = 0;
            originX = 0;
            originY = 0;
            originZ = 0;

            for (i_11 = 0; i_11 < i_9; i_11++) {
                i_12 = ints_2[i_11];
                if (i_12 < this.verticesByLabel.length) {
                    ints_13 = this.verticesByLabel[i_12];

                    for (i_14 = 0; i_14 < ints_13.length; i_14++) {
                        i_15 = ints_13[i_14];
                        if (this.verticesSubmeshes == null || (i_7 & this.verticesSubmeshes[i_15]) != 0) {
                            originX += this.verticesX[i_15];
                            originY += this.verticesY[i_15];
                            originZ += this.verticesZ[i_15];
                            ++i_10;
                        }
                    }
                }
            }

            if (i_10 > 0) {
                originX = i_31 + originX / i_10;
                originY = i_41 + originY / i_10;
                originZ = i_51 + originZ / i_10;
                withinOrigin = true;
            } else {
                originX = i_31;
                originY = i_41;
                originZ = i_51;
            }
        } else {
            int[] ints_47;
            int i_48;
            if (i_1 == 1) {
                if (ints_8 != null) {
                    i_10 = i_51 * ints_8[2] + ints_8[1] * i_41 + i_31 * ints_8[0] + 8192 >> 14;
                    i_11 = i_31 * ints_8[3] + i_51 * ints_8[5] + i_41 * ints_8[4] + 8192 >> 14;
                    i_12 = i_31 * ints_8[6] + ints_8[7] * i_41 + i_51 * ints_8[8] + 8192 >> 14;
                    i_31 = i_10;
                    i_41 = i_11;
                    i_51 = i_12;
                }

                i_31 <<= 4;
                i_41 <<= 4;
                i_51 <<= 4;

                for (i_10 = 0; i_10 < i_9; i_10++) {
                    i_11 = ints_2[i_10];
                    if (i_11 < this.verticesByLabel.length) {
                        ints_47 = this.verticesByLabel[i_11];

                        for (i_48 = 0; i_48 < ints_47.length; i_48++) {
                            i_14 = ints_47[i_48];
                            if (this.verticesSubmeshes == null || (i_7 & this.verticesSubmeshes[i_14]) != 0) {
                                this.verticesX[i_14] += i_31;
                                this.verticesY[i_14] += i_41;
                                this.verticesZ[i_14] += i_51;
                            }
                        }
                    }
                }
            } else {
                int i_16;
                int i_17;
                int i_18;
                int i_19;
                int i_20;
                int i_21;
                int i_22;
                int i_23;
                int i_24;
                int i_26;
                int i_27;
                int i_28;
                int i_30;
                int i_32;
                int i_33;
                int i_34;
                int[] ints_35;
                int i_36;
                int i_37;
                int i_38;
                int i_39;
                int i_40;
                if (i_1 == 2) {
                    if (ints_8 != null) {
                        i_10 = ints_8[9] << 4;
                        i_11 = ints_8[10] << 4;
                        i_12 = ints_8[11] << 4;
                        i_48 = ints_8[12] << 4;
                        i_14 = ints_8[13] << 4;
                        i_15 = ints_8[14] << 4;
                        if (withinOrigin) {
                            i_16 = ints_8[0] * originX + ints_8[6] * originZ + ints_8[3] * originY + 8192 >> 14;
                            i_17 = ints_8[7] * originZ + ints_8[4] * originY + ints_8[1] * originX + 8192 >> 14;
                            i_18 = ints_8[8] * originZ + ints_8[2] * originX + ints_8[5] * originY + 8192 >> 14;
                            i_16 += i_48;
                            i_17 += i_14;
                            i_18 += i_15;
                            originX = i_16;
                            originY = i_17;
                            originZ = i_18;
                            withinOrigin = false;
                        }

                        int[] ints_49 = new int[9];
                        i_17 = MathUtils.COSINE[i_31];
                        i_18 = MathUtils.SINE[i_31];
                        i_19 = MathUtils.COSINE[i_41];
                        i_20 = MathUtils.SINE[i_41];
                        i_21 = MathUtils.COSINE[i_51];
                        i_22 = MathUtils.SINE[i_51];
                        i_23 = i_18 * i_21 + 8192 >> 14;
                        i_24 = i_18 * i_22 + 8192 >> 14;
                        ints_49[0] = i_19 * i_21 + i_20 * i_24 + 8192 >> 14;
                        ints_49[1] = i_20 * i_23 + -i_19 * i_22 + 8192 >> 14;
                        ints_49[2] = i_20 * i_17 + 8192 >> 14;
                        ints_49[3] = i_17 * i_22 + 8192 >> 14;
                        ints_49[4] = i_17 * i_21 + 8192 >> 14;
                        ints_49[5] = -i_18;
                        ints_49[6] = i_19 * i_24 + -i_20 * i_21 + 8192 >> 14;
                        ints_49[7] = i_20 * i_22 + i_19 * i_23 + 8192 >> 14;
                        ints_49[8] = i_19 * i_17 + 8192 >> 14;
                        int i_25 = ints_49[1] * -originY + ints_49[0] * -originX + ints_49[2] * -originZ + 8192 >> 14;
                        i_26 = -originY * ints_49[4] + ints_49[3] * -originX + -originZ * ints_49[5] + 8192 >> 14;
                        i_27 = ints_49[7] * -originY + ints_49[6] * -originX + ints_49[8] * -originZ + 8192 >> 14;
                        i_28 = i_25 + originX;
                        int i_29 = i_26 + originY;
                        i_30 = i_27 + originZ;
                        int[] ints_31 = new int[9];

                        for (i_32 = 0; i_32 < 3; i_32++) {
                            for (i_33 = 0; i_33 < 3; i_33++) {
                                i_34 = 0;

                                for (int i_53 = 0; i_53 < 3; i_53++) {
                                    i_34 += ints_8[i_53 + i_33 * 3] * ints_49[i_53 + i_32 * 3];
                                }

                                ints_31[i_33 + i_32 * 3] = i_34 + 8192 >> 14;
                            }
                        }

                        i_32 = i_14 * ints_49[1] + i_48 * ints_49[0] + i_15 * ints_49[2] + 8192 >> 14;
                        i_33 = i_14 * ints_49[4] + i_48 * ints_49[3] + i_15 * ints_49[5] + 8192 >> 14;
                        i_34 = ints_49[6] * i_48 + i_15 * ints_49[8] + ints_49[7] * i_14 + 8192 >> 14;
                        i_32 += i_28;
                        i_33 += i_29;
                        i_34 += i_30;
                        ints_35 = new int[9];

                        for (i_36 = 0; i_36 < 3; i_36++) {
                            for (i_37 = 0; i_37 < 3; i_37++) {
                                i_38 = 0;

                                for (i_39 = 0; i_39 < 3; i_39++) {
                                    i_38 += ints_31[i_37 + i_39 * 3] * ints_8[i_39 + i_36 * 3];
                                }

                                ints_35[i_37 + i_36 * 3] = i_38 + 8192 >> 14;
                            }
                        }

                        i_36 = i_34 * ints_8[2] + i_32 * ints_8[0] + i_33 * ints_8[1] + 8192 >> 14;
                        i_37 = i_32 * ints_8[3] + i_33 * ints_8[4] + ints_8[5] * i_34 + 8192 >> 14;
                        i_38 = i_32 * ints_8[6] + i_34 * ints_8[8] + ints_8[7] * i_33 + 8192 >> 14;
                        i_36 += i_10;
                        i_37 += i_11;
                        i_38 += i_12;

                        for (i_39 = 0; i_39 < i_9; i_39++) {
                            i_40 = ints_2[i_39];
                            if (i_40 < this.verticesByLabel.length) {
                                int[] ints_41 = this.verticesByLabel[i_40];

                                for (int i_42 = 0; i_42 < ints_41.length; i_42++) {
                                    int i_43 = ints_41[i_42];
                                    if (this.verticesSubmeshes == null || (i_7 & this.verticesSubmeshes[i_43]) != 0) {
                                        int i_44 = ints_35[0] * this.verticesX[i_43] + ints_35[2] * this.verticesZ[i_43] + ints_35[1] * this.verticesY[i_43] + 8192 >> 14;
                                        int i_45 = ints_35[4] * this.verticesY[i_43] + ints_35[5] * this.verticesZ[i_43] + ints_35[3] * this.verticesX[i_43] + 8192 >> 14;
                                        int i_46 = ints_35[6] * this.verticesX[i_43] + ints_35[7] * this.verticesY[i_43] + ints_35[8] * this.verticesZ[i_43] + 8192 >> 14;
                                        i_44 += i_36;
                                        i_45 += i_37;
                                        i_46 += i_38;
                                        this.verticesX[i_43] = i_44;
                                        this.verticesY[i_43] = i_45;
                                        this.verticesZ[i_43] = i_46;
                                    }
                                }
                            }
                        }
                    } else {
                        for (i_10 = 0; i_10 < i_9; i_10++) {
                            i_11 = ints_2[i_10];
                            if (i_11 < this.verticesByLabel.length) {
                                ints_47 = this.verticesByLabel[i_11];

                                for (i_48 = 0; i_48 < ints_47.length; i_48++) {
                                    i_14 = ints_47[i_48];
                                    if (this.verticesSubmeshes == null || (i_7 & this.verticesSubmeshes[i_14]) != 0) {
                                        this.verticesX[i_14] -= originX;
                                        this.verticesY[i_14] -= originY;
                                        this.verticesZ[i_14] -= originZ;
                                        if (i_51 != 0) {
                                            i_15 = MathUtils.SINE[i_51];
                                            i_16 = MathUtils.COSINE[i_51];
                                            i_17 = i_15 * this.verticesY[i_14] + i_16 * this.verticesX[i_14] + 16383 >> 14;
                                            this.verticesY[i_14] = i_16 * this.verticesY[i_14] - i_15 * this.verticesX[i_14] + 16383 >> 14;
                                            this.verticesX[i_14] = i_17;
                                        }

                                        if (i_31 != 0) {
                                            i_15 = MathUtils.SINE[i_31];
                                            i_16 = MathUtils.COSINE[i_31];
                                            i_17 = i_16 * this.verticesY[i_14] - i_15 * this.verticesZ[i_14] + 16383 >> 14;
                                            this.verticesZ[i_14] = i_15 * this.verticesY[i_14] + i_16 * this.verticesZ[i_14] + 16383 >> 14;
                                            this.verticesY[i_14] = i_17;
                                        }

                                        if (i_41 != 0) {
                                            i_15 = MathUtils.SINE[i_41];
                                            i_16 = MathUtils.COSINE[i_41];
                                            i_17 = i_15 * this.verticesZ[i_14] + i_16 * this.verticesX[i_14] + 16383 >> 14;
                                            this.verticesZ[i_14] = i_16 * this.verticesZ[i_14] - i_15 * this.verticesX[i_14] + 16383 >> 14;
                                            this.verticesX[i_14] = i_17;
                                        }

                                        this.verticesX[i_14] += originX;
                                        this.verticesY[i_14] += originY;
                                        this.verticesZ[i_14] += originZ;
                                    }
                                }
                            }
                        }

                        if (bool6) {
                            for (i_10 = 0; i_10 < i_9; i_10++) {
                                i_11 = ints_2[i_10];
                                if (i_11 < this.verticesByLabel.length) {
                                    ints_47 = this.verticesByLabel[i_11];

                                    for (i_48 = 0; i_48 < ints_47.length; i_48++) {
                                        i_14 = ints_47[i_48];
                                        if (this.verticesSubmeshes == null || (i_7 & this.verticesSubmeshes[i_14]) != 0) {
                                            i_15 = this.totalFaceCounts[i_14];
                                            i_16 = this.totalFaceCounts[i_14 + 1];

                                            for (i_17 = i_15; i_17 < i_16; i_17++) {
                                                i_18 = this.aShortArray8948[i_17] - 1;
                                                if (i_18 == -1) {
                                                    break;
                                                }

                                                if (i_51 != 0) {
                                                    i_19 = MathUtils.SINE[i_51];
                                                    i_20 = MathUtils.COSINE[i_51];
                                                    i_21 = this.normalsYs[i_18] * i_19 + this.normalsXs[i_18] * i_20 + 16383 >> 14;
                                                    this.normalsYs[i_18] = (short) (this.normalsYs[i_18] * i_20 - this.normalsXs[i_18] * i_19 + 16383 >> 14);
                                                    this.normalsXs[i_18] = (short) i_21;
                                                }

                                                if (i_31 != 0) {
                                                    i_19 = MathUtils.SINE[i_31];
                                                    i_20 = MathUtils.COSINE[i_31];
                                                    i_21 = this.normalsYs[i_18] * i_20 - this.normalsZs[i_18] * i_19 + 16383 >> 14;
                                                    this.normalsZs[i_18] = (short) (this.normalsYs[i_18] * i_19 + this.normalsZs[i_18] * i_20 + 16383 >> 14);
                                                    this.normalsYs[i_18] = (short) i_21;
                                                }

                                                if (i_41 != 0) {
                                                    i_19 = MathUtils.SINE[i_41];
                                                    i_20 = MathUtils.COSINE[i_41];
                                                    i_21 = this.normalsZs[i_18] * i_19 + this.normalsXs[i_18] * i_20 + 16383 >> 14;
                                                    this.normalsZs[i_18] = (short) (this.normalsZs[i_18] * i_20 - this.normalsXs[i_18] * i_19 + 16383 >> 14);
                                                    this.normalsXs[i_18] = (short) i_21;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            method14285();
                        }
                    }
                } else if (i_1 == 3) {
                    if (ints_8 != null) {
                        i_10 = ints_8[9] << 4;
                        i_11 = ints_8[10] << 4;
                        i_12 = ints_8[11] << 4;
                        i_48 = ints_8[12] << 4;
                        i_14 = ints_8[13] << 4;
                        i_15 = ints_8[14] << 4;
                        if (withinOrigin) {
                            i_16 = ints_8[0] * originX + ints_8[6] * originZ + ints_8[3] * originY + 8192 >> 14;
                            i_17 = ints_8[7] * originZ + ints_8[4] * originY + ints_8[1] * originX + 8192 >> 14;
                            i_18 = ints_8[8] * originZ + ints_8[2] * originX + ints_8[5] * originY + 8192 >> 14;
                            i_16 += i_48;
                            i_17 += i_14;
                            i_18 += i_15;
                            originX = i_16;
                            originY = i_17;
                            originZ = i_18;
                            withinOrigin = false;
                        }

                        i_16 = i_31 << 15 >> 7;
                        i_17 = i_41 << 15 >> 7;
                        i_18 = i_51 << 15 >> 7;
                        i_19 = i_16 * -originX + 8192 >> 14;
                        i_20 = i_17 * -originY + 8192 >> 14;
                        i_21 = i_18 * -originZ + 8192 >> 14;
                        i_22 = i_19 + originX;
                        i_23 = i_20 + originY;
                        i_24 = i_21 + originZ;
                        int[] ints_50 = {i_16 * ints_8[0] + 8192 >> 14, i_16 * ints_8[3] + 8192 >> 14, i_16 * ints_8[6] + 8192 >> 14, i_17 * ints_8[1] + 8192 >> 14, i_17 * ints_8[4] + 8192 >> 14, ints_8[7] * i_17 + 8192 >> 14, i_18 * ints_8[2] + 8192 >> 14, ints_8[5] * i_18 + 8192 >> 14, i_18 * ints_8[8] + 8192 >> 14};
                        i_26 = i_16 * i_48 + 8192 >> 14;
                        i_27 = i_17 * i_14 + 8192 >> 14;
                        i_28 = i_18 * i_15 + 8192 >> 14;
                        i_26 += i_22;
                        i_27 += i_23;
                        i_28 += i_24;
                        int[] ints_51 = new int[9];

                        int i_52;
                        for (i_30 = 0; i_30 < 3; i_30++) {
                            for (i_52 = 0; i_52 < 3; i_52++) {
                                i_32 = 0;

                                for (i_33 = 0; i_33 < 3; i_33++) {
                                    i_32 += ints_50[i_52 + i_33 * 3] * ints_8[i_33 + i_30 * 3];
                                }

                                ints_51[i_52 + i_30 * 3] = i_32 + 8192 >> 14;
                            }
                        }

                        i_30 = i_28 * ints_8[2] + i_26 * ints_8[0] + i_27 * ints_8[1] + 8192 >> 14;
                        i_52 = i_26 * ints_8[3] + i_27 * ints_8[4] + ints_8[5] * i_28 + 8192 >> 14;
                        i_32 = i_26 * ints_8[6] + i_28 * ints_8[8] + ints_8[7] * i_27 + 8192 >> 14;
                        i_30 += i_10;
                        i_52 += i_11;
                        i_32 += i_12;

                        for (i_33 = 0; i_33 < i_9; i_33++) {
                            i_34 = ints_2[i_33];
                            if (i_34 < this.verticesByLabel.length) {
                                ints_35 = this.verticesByLabel[i_34];

                                for (i_36 = 0; i_36 < ints_35.length; i_36++) {
                                    i_37 = ints_35[i_36];
                                    if (this.verticesSubmeshes == null || (i_7 & this.verticesSubmeshes[i_37]) != 0) {
                                        i_38 = ints_51[0] * this.verticesX[i_37] + ints_51[2] * this.verticesZ[i_37] + ints_51[1] * this.verticesY[i_37] + 8192 >> 14;
                                        i_39 = ints_51[4] * this.verticesY[i_37] + ints_51[5] * this.verticesZ[i_37] + ints_51[3] * this.verticesX[i_37] + 8192 >> 14;
                                        i_40 = ints_51[6] * this.verticesX[i_37] + ints_51[7] * this.verticesY[i_37] + ints_51[8] * this.verticesZ[i_37] + 8192 >> 14;
                                        i_38 += i_30;
                                        i_39 += i_52;
                                        i_40 += i_32;
                                        this.verticesX[i_37] = i_38;
                                        this.verticesY[i_37] = i_39;
                                        this.verticesZ[i_37] = i_40;
                                    }
                                }
                            }
                        }
                    } else {
                        for (i_10 = 0; i_10 < i_9; i_10++) {
                            i_11 = ints_2[i_10];
                            if (i_11 < this.verticesByLabel.length) {
                                ints_47 = this.verticesByLabel[i_11];

                                for (i_48 = 0; i_48 < ints_47.length; i_48++) {
                                    i_14 = ints_47[i_48];
                                    if (this.verticesSubmeshes == null || (i_7 & this.verticesSubmeshes[i_14]) != 0) {
                                        this.verticesX[i_14] -= originX;
                                        this.verticesY[i_14] -= originY;
                                        this.verticesZ[i_14] -= originZ;
                                        this.verticesX[i_14] = i_31 * this.verticesX[i_14] >> 7;
                                        this.verticesY[i_14] = i_41 * this.verticesY[i_14] >> 7;
                                        this.verticesZ[i_14] = i_51 * this.verticesZ[i_14] >> 7;
                                        this.verticesX[i_14] += originX;
                                        this.verticesY[i_14] += originY;
                                        this.verticesZ[i_14] += originZ;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    BillboardSurfaceDX class6555;
                    boolean bool56;
                    BillboardEffectDX class5657;
                    if (i_1 == 5) {
                        if (this.facesByLabel != null) {
                            bool56 = false;

                            for (i_11 = 0; i_11 < i_9; i_11++) {
                                i_12 = ints_2[i_11];
                                if (i_12 < this.facesByLabel.length) {
                                    ints_13 = this.facesByLabel[i_12];

                                    for (i_14 = 0; i_14 < ints_13.length; i_14++) {
                                        i_15 = ints_13[i_14];
                                        if (this.facesSubmeshes == null || (i_7 & this.facesSubmeshes[i_15]) != 0) {
                                            i_16 = (this.facesAlpha[i_15] & 0xff) + i_31 * 8;
                                            if (i_16 < 0) {
                                                i_16 = 0;
                                            } else if (i_16 > 255) {
                                                i_16 = 255;
                                            }

                                            this.facesAlpha[i_15] = (byte) i_16;
                                        }
                                    }

                                    bool56 |= ints_13.length > 0;
                                }
                            }

                            if (bool56) {
                                if (this.billboardEffects != null) {
                                    for (i_11 = 0; i_11 < this.numBillboards; i_11++) {
                                        class5657 = this.billboardEffects[i_11];
                                        class6555 = this.billboardBackgrounds[i_11];
                                        class6555.diffuseColor = class6555.diffuseColor & 0xffffff | 255 - (this.facesAlpha[class5657.appliesOnFace] & 0xff) << 24;
                                    }
                                }

                                method14269();
                            }
                        }
                    } else if (i_1 == 7) {
                        if (this.facesByLabel != null) {
                            bool56 = false;

                            for (i_11 = 0; i_11 < i_9; i_11++) {
                                i_12 = ints_2[i_11];
                                if (i_12 < this.facesByLabel.length) {
                                    ints_13 = this.facesByLabel[i_12];

                                    for (i_14 = 0; i_14 < ints_13.length; i_14++) {
                                        i_15 = ints_13[i_14];
                                        if (this.facesSubmeshes == null || (i_7 & this.facesSubmeshes[i_15]) != 0) {
                                            i_16 = this.facesColor[i_15] & 0xffff;
                                            i_17 = i_16 >> 10 & 0x3f;
                                            i_18 = i_16 >> 7 & 0x7;
                                            i_19 = i_16 & 0x7f;
                                            i_17 = i_31 + i_17 & 0x3f;
                                            i_18 += i_41 / 4;
                                            if (i_18 < 0) {
                                                i_18 = 0;
                                            } else if (i_18 > 7) {
                                                i_18 = 7;
                                            }

                                            i_19 += i_51;
                                            if (i_19 < 0) {
                                                i_19 = 0;
                                            } else if (i_19 > 127) {
                                                i_19 = 127;
                                            }

                                            this.facesColor[i_15] = (short) (i_17 << 10 | i_18 << 7 | i_19);
                                        }
                                    }

                                    bool56 |= ints_13.length > 0;
                                }
                            }

                            if (bool56) {
                                if (this.billboardEffects != null) {
                                    for (i_11 = 0; i_11 < this.numBillboards; i_11++) {
                                        class5657 = this.billboardEffects[i_11];
                                        class6555 = this.billboardBackgrounds[i_11];
                                        class6555.diffuseColor = class6555.diffuseColor & ~0xffffff | ColorUtil.getHslToRgb()[this.facesColor[class5657.appliesOnFace] & 0xffff] & 0xffffff;
                                    }
                                }

                                method14269();
                            }
                        }
                    } else {
                        BillboardSurfaceDX class6554;
                        if (i_1 == 8) {
                            if (this.billboardsByLabel != null) {
                                for (i_10 = 0; i_10 < i_9; i_10++) {
                                    i_11 = ints_2[i_10];
                                    if (i_11 < this.billboardsByLabel.length) {
                                        ints_47 = this.billboardsByLabel[i_11];

                                        for (i_48 = 0; i_48 < ints_47.length; i_48++) {
                                            class6554 = this.billboardBackgrounds[ints_47[i_48]];
                                            class6554.offsetX += i_31;
                                            class6554.offsetY += i_41;
                                        }
                                    }
                                }
                            }
                        } else if (i_1 == 10) {
                            if (this.billboardsByLabel != null) {
                                for (i_10 = 0; i_10 < i_9; i_10++) {
                                    i_11 = ints_2[i_10];
                                    if (i_11 < this.billboardsByLabel.length) {
                                        ints_47 = this.billboardsByLabel[i_11];

                                        for (i_48 = 0; i_48 < ints_47.length; i_48++) {
                                            class6554 = this.billboardBackgrounds[ints_47[i_48]];
                                            class6554.scaleX = i_31 * class6554.scaleX >> 7;
                                            class6554.scaleY = i_41 * class6554.scaleY >> 7;
                                        }
                                    }
                                }
                            }
                        } else if (i_1 == 9 && this.billboardsByLabel != null) {
                            for (i_10 = 0; i_10 < i_9; i_10++) {
                                i_11 = ints_2[i_10];
                                if (i_11 < this.billboardsByLabel.length) {
                                    ints_47 = this.billboardsByLabel[i_11];

                                    for (i_48 = 0; i_48 < ints_47.length; i_48++) {
                                        class6554 = this.billboardBackgrounds[ints_47[i_48]];
                                        class6554.rotation = i_31 + class6554.rotation & 0x3fff;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }


    @Override
    public void mirror() {
        int i_1;
        for (i_1 = 0; i_1 < this.maxDepth; i_1++) {
            this.verticesZ[i_1] = -this.verticesZ[i_1];
        }

        for (i_1 = 0; i_1 < this.anInt8906; i_1++) {
            this.normalsZs[i_1] = (short) (-this.normalsZs[i_1]);
        }

        for (i_1 = 0; i_1 < this.numFaces; i_1++) {
            short s_2 = this.normalsFaceA[i_1];
            this.normalsFaceA[i_1] = this.normalsFaceC[i_1];
            this.normalsFaceC[i_1] = s_2;
        }

        setVerticesInactive();
        method14285();
        method14270();
        this.boundsUpdated = false;
    }

    @Override
    public void tint(int i_1, int i_2, int i_3, int i_4) {
        for (int index = 0; index < this.numFaces; index++) {
            int color = this.facesColor[index] & 0xffff;
            int h = color >> 10 & 0x3f;
            int s = color >> 7 & 0x7;
            int l = color & 0x7f;
            if (i_1 != -1) {
                h += (i_1 - h) * i_4 >> 7;
            }

            if (i_2 != -1) {
                s += i_4 * (i_2 - s) >> 7;
            }

            if (i_3 != -1) {
                l += (i_3 - l) * i_4 >> 7;
            }

            this.facesColor[index] = (short) (h << 10 | s << 7 | l);
        }

        if (this.billboardEffects != null) {
            for (int index = 0; index < this.numBillboards; index++) {
                BillboardEffectDX class5610 = this.billboardEffects[index];
                BillboardSurfaceDX class6511 = this.billboardBackgrounds[index];
                class6511.diffuseColor = class6511.diffuseColor & ~0xffffff | ColorUtil.getHslToRgb()[this.facesColor[class5610.appliesOnFace] & 0xffff] & 0xffffff;
            }
        }

        method14269();
    }

    @Override
    public void unlock() {
    }

}
