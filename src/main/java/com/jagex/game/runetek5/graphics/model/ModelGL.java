package com.jagex.game.runetek5.graphics.model;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;
import com.jagex.game.runetek5.buffer.ElementsBufferGL;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardSurfaceGL;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardEffectGL;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.graphics.model.buffer.ModelElementsBufferGL;
import com.jagex.game.runetek5.graphics.model.normal.FaceNormalGL;
import com.jagex.game.runetek5.graphics.model.normal.ModelNormalsGL;
import com.jagex.game.runetek5.graphics.model.normal.VertexNormalGL;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.graphics.shadow.ShadowGL;
import com.jagex.game.runetek5.graphics.texture.TextureTransformationMatrix;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer;
import com.jagex.game.runetek5.utils.ArrayUtils;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.MathUtils;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaclib.memory.Stream;
import jaggl.OpenGL;

public class ModelGL extends Model {

    public static final float[] uvTypes = new float[2];
    public static final int[] matrixX = new int[8];
    public static final int[] matrixY = new int[8];
    public static final int[] matrixZ = new int[8];
    public static long[] normalsUid;
    public static int originX;
    public static int originY;
    public static int originZ;
    public static boolean withinOrigin;
    public boolean boundsCalculated;
    public boolean hasAlphaTransition;
    public boolean textureAnimating;
    public int numVertices;
    public int maxDepth;
    public int numNormals;
    public int numFaces;
    public int anInt8302;
    public boolean updateRequired = true;
    public final OpenGlToolkit toolkit;
    public int functionMask;
    public int preferenceFlags;
    public OpenGlArrayDrawBuffer vertexBuffer;
    public OpenGlArrayDrawBuffer textureCoordinatesBuffer;
    public OpenGlArrayDrawBuffer normalsBuffer;
    public OpenGlArrayDrawBuffer colorsBuffer;
    public ModelElementsBufferGL modelElementsBuffer;
    public int[] triangles;
    public int[] verticesX;
    public int[] verticesY;
    public int[] verticesZ;
    public short[] verticesSubmeshes;
    public EmissiveTriangle[] emissiveTriangles;
    public EffectiveVertex[] effectiveVertices;
    public int billboardCount;
    public BillboardEffectGL[] billboardEffects;
    public BillboardSurfaceGL[] billboardSurfaces;
    public short[] normalsX;
    public short[] normalsY;
    public short[] normalsZ;
    public byte[] magnitudes;
    public float[] textureU;
    public float[] textureV;
    public short[] facesColor;
    public byte[] facesAlpha;
    public short[] facesA;
    public short[] facesB;
    public short[] facesC;
    public short[] facesMaterial;
    public short[] facesSubmeshes;
    public short ambient;
    public short contrast;
    public short[] aShortArray8288;
    public int[] anIntArray8289;
    public int[][] vertexGroups;
    public int[][] billboardGroups;
    public int[][] faceGroups;
    public ModelNormalsGL aClass1668259;
    public byte renderState;
    public int size3d;
    public int size2d;
    public short minX;
    public short maxX;
    public short minY;
    public short maxY;
    public short minZ;
    public short maxZ;
    public ElementsBufferGL anInterface158277;
    public ArrayBufferGL anInterface148305;

    public ModelGL(OpenGlToolkit renderer, Mesh mesh, int rasterizerFlags, int i_4, int i_5, int otherFlags) {
        this.toolkit = renderer;
        this.functionMask = rasterizerFlags;
        this.preferenceFlags = otherFlags;
        if (ModelFunctionMaskGL.hasVertices(rasterizerFlags)) {
            this.vertexBuffer = new OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0);
        }

        if (ModelFunctionMaskGL.hasTextureCoordinates(rasterizerFlags)) {
            this.textureCoordinatesBuffer = new OpenGlArrayDrawBuffer(null, GL_FLOAT, 2, 0);
        }

        if (ModelFunctionMaskGL.hasNormals(rasterizerFlags, otherFlags)) {
            this.normalsBuffer = new OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0);
        }

        if (ModelFunctionMaskGL.hasColors(rasterizerFlags)) {
            this.colorsBuffer = new OpenGlArrayDrawBuffer(null, GL_UNSIGNED_BYTE, 4, 0);
        }

        if (ModelFunctionMaskGL.hasElementsBuffer(rasterizerFlags)) {
            this.modelElementsBuffer = new ModelElementsBufferGL();
        }

        MaterialProvider materialProvider = renderer.materialProvider;
        int[] faces = new int[mesh.numFaces];
        this.triangles = new int[mesh.maxDepth + 1];

        for (int faceIndex = 0; faceIndex < mesh.numFaces; faceIndex++) {
            if (mesh.faceRenderType == null || mesh.faceRenderType[faceIndex] != 2) {
                if (mesh.facesMaterial != null && mesh.facesMaterial[faceIndex] != -1) {
                    MaterialType meshMaterial = materialProvider.getMaterial(mesh.facesMaterial[faceIndex] & 0xffff);
                    if (((this.preferenceFlags & 0x40) == 0 || !meshMaterial.detailsOnly) && meshMaterial.skipTriangles) {
                        continue;
                    }
                }

                faces[this.numFaces++] = faceIndex;
                this.triangles[mesh.facesA[faceIndex]]++;
                this.triangles[mesh.facesB[faceIndex]]++;
                this.triangles[mesh.facesC[faceIndex]]++;
            }
        }

        this.anInt8302 = this.numFaces;
        long[] billboardUids = new long[this.numFaces];
        boolean hasTransitions = (this.functionMask & 0x100) != 0;

        int currentFace;
        int billboardMainUid;
        MaterialType material;
        short billboardMaterialId;
        int appliesOnFace;
        for (int triangleIndex = 0; triangleIndex < this.numFaces; triangleIndex++) {
            currentFace = faces[triangleIndex];
            MaterialType billboardMaterial = null;
            billboardMainUid = 0;
            byte b_67 = 0;
            byte effectId = 0;
            byte effectParam1 = 0;
            if (mesh.billboards != null) {
                boolean hasUid = false;

                for (int texturedFace = 0; texturedFace < mesh.billboards.length; texturedFace++) {
                    Billboard vertexNormal = mesh.billboards[texturedFace];
                    if (currentFace == vertexNormal.appliesOnFace) {
                        BillboardType billboardType = BillboardTypeList.list(vertexNormal.billboardId);
                        if (billboardType.hasUid) {
                            hasUid = true;
                        }

                        if (billboardType.materialId != -1) {
                            material = materialProvider.getMaterial(billboardType.materialId);
                            if (material.effectCombiner == 2) {
                                this.hasAlphaTransition = true;
                            }
                        }
                    }
                }

                if (hasUid) {
                    billboardUids[triangleIndex] = Long.MAX_VALUE;
                    this.anInt8302--;
                    continue;
                }
            }

            billboardMaterialId = -1;
            if (mesh.facesMaterial != null) {
                billboardMaterialId = mesh.facesMaterial[currentFace];
                if (billboardMaterialId != -1) {
                    billboardMaterial = materialProvider.getMaterial(billboardMaterialId & 0xffff);
                    if ((this.preferenceFlags & 0x40) != 0 && billboardMaterial.detailsOnly) {
                        billboardMaterialId = -1;
                        billboardMaterial = null;
                    } else {
                        effectId = billboardMaterial.effectId;
                        effectParam1 = billboardMaterial.effectParam1;
                        if (billboardMaterial.textureSpeedU != 0 || billboardMaterial.textureSpeedV != 0) {
                            this.textureAnimating = true;
                        }
                    }
                }
            }

            boolean alphaTransition = mesh.facesAlpha != null && mesh.facesAlpha[currentFace] != 0 || billboardMaterial != null && billboardMaterial.effectCombiner != 0;
            if ((hasTransitions || alphaTransition) && mesh.facesLayer != null) {
                billboardMainUid += mesh.facesLayer[currentFace] << 17;
            }

            if (alphaTransition) {
                billboardMainUid += 65536;
            }

            billboardMainUid += (effectId & 0xff) << 8;
            billboardMainUid += effectParam1 & 0xff;
            appliesOnFace = b_67 + ((billboardMaterialId & 0xffff) << 16);
            appliesOnFace += triangleIndex & 0xffff;
            billboardUids[triangleIndex] = ((long) billboardMainUid << 32) + appliesOnFace;
            this.hasAlphaTransition |= alphaTransition;
            this.textureAnimating |= billboardMaterial != null && (billboardMaterial.textureSpeedU != 0 || billboardMaterial.textureSpeedV != 0);
        }

        Model.addBillboard(billboardUids, faces);
        this.numVertices = mesh.numVertices;
        this.maxDepth = mesh.maxDepth;
        this.verticesX = mesh.verticesX;
        this.verticesY = mesh.verticesY;
        this.verticesZ = mesh.verticesZ;
        this.verticesSubmeshes = mesh.verticesSubmeshes;
        VertexNormalGL[] vertexNormals = new VertexNormalGL[this.maxDepth];
        this.emissiveTriangles = mesh.particleEmitters;
        this.effectiveVertices = mesh.particleEffectors;
        int triangleIndex;
        if (mesh.billboards != null) {
            this.billboardCount = mesh.billboards.length;
            this.billboardEffects = new BillboardEffectGL[this.billboardCount];
            this.billboardSurfaces = new BillboardSurfaceGL[this.billboardCount];

            for (currentFace = 0; currentFace < this.billboardCount; currentFace++) {
                Billboard billboard = mesh.billboards[currentFace];
                BillboardType billboardType = BillboardTypeList.list(billboard.billboardId);
                appliesOnFace = -1;

                for (triangleIndex = 0; triangleIndex < this.numFaces; triangleIndex++) {
                    if (billboard.appliesOnFace == faces[triangleIndex]) {
                        appliesOnFace = triangleIndex;
                        break;
                    }
                }

                if (appliesOnFace == -1) {
                    throw new RuntimeException();
                }

                triangleIndex = ColorUtil.hslToRgb[mesh.facesColor[billboard.appliesOnFace] & 0xffff] & 0xffffff;
                triangleIndex |= 255 - (mesh.facesAlpha != null ? mesh.facesAlpha[billboard.appliesOnFace] : 0) << 24;
                this.billboardEffects[currentFace] = new BillboardEffectGL(appliesOnFace, mesh.facesA[billboard.appliesOnFace], mesh.facesB[billboard.appliesOnFace], mesh.facesC[billboard.appliesOnFace], billboardType.size2d, billboardType.size3d, billboardType.materialId, billboardType.shape, billboardType.stationary, billboard.distance);
                this.billboardSurfaces[currentFace] = new BillboardSurfaceGL(triangleIndex);
            }
        }

        currentFace = this.numFaces * 3;
        this.normalsX = new short[currentFace];
        this.normalsY = new short[currentFace];
        this.normalsZ = new short[currentFace];
        this.magnitudes = new byte[currentFace];
        this.textureU = new float[currentFace];
        this.textureV = new float[currentFace];
        this.facesColor = new short[this.numFaces];
        this.facesAlpha = new byte[this.numFaces];
        this.facesA = new short[this.numFaces];
        this.facesB = new short[this.numFaces];
        this.facesC = new short[this.numFaces];
        this.facesMaterial = new short[this.numFaces];
        if (mesh.facesSubmeshes != null) {
            this.facesSubmeshes = new short[this.numFaces];
        }

        this.ambient = (short) i_4;
        this.contrast = (short) i_5;
        this.aShortArray8288 = new short[currentFace];
        normalsUid = new long[currentFace];
        int i_13 = 0;

        for (billboardMainUid = 0; billboardMainUid < mesh.maxDepth; billboardMainUid++) {
            appliesOnFace = this.triangles[billboardMainUid];
            this.triangles[billboardMainUid] = i_13;
            i_13 += appliesOnFace;
            vertexNormals[billboardMainUid] = new VertexNormalGL();
        }

        this.triangles[mesh.maxDepth] = i_13;
        TextureTransformationMatrix transformMatrix = generateTextureMatrix(mesh, faces, this.numFaces);
        FaceNormalGL[] faceNormals = new FaceNormalGL[mesh.numFaces];

        int alphaAmt;
        int i_21;
        short lastMaterial;
        for (triangleIndex = 0; triangleIndex < mesh.numFaces; triangleIndex++) {
            lastMaterial = mesh.facesA[triangleIndex];
            billboardMaterialId = mesh.facesB[triangleIndex];
            short materialId = mesh.facesC[triangleIndex];
            alphaAmt = this.verticesX[billboardMaterialId] - this.verticesX[lastMaterial];
            i_21 = this.verticesY[billboardMaterialId] - this.verticesY[lastMaterial];
            int i_22 = this.verticesZ[billboardMaterialId] - this.verticesZ[lastMaterial];
            int i_23 = this.verticesX[materialId] - this.verticesX[lastMaterial];
            int i_24 = this.verticesY[materialId] - this.verticesY[lastMaterial];
            int i_25 = this.verticesZ[materialId] - this.verticesZ[lastMaterial];
            int x = i_21 * i_25 - i_24 * i_22;
            int y = i_22 * i_23 - i_25 * alphaAmt;

            int z;
            for (z = alphaAmt * i_24 - i_23 * i_21; x > 8192 || y > 8192 || z > 8192 || x < -8192 || y < -8192 || z < -8192; z >>= 1) {
                x >>= 1;
                y >>= 1;
            }

            int i_29 = (int) Math.sqrt(x * x + y * y + z * z);
            if (i_29 <= 0) {
                i_29 = 1;
            }

            x = x * 256 / i_29;
            y = y * 256 / i_29;
            z = z * 256 / i_29;
            byte b_30 = mesh.faceRenderType == null ? 0 : mesh.faceRenderType[triangleIndex];
            if (b_30 == 0) {
                VertexNormalGL vertexNormal = vertexNormals[lastMaterial];
                vertexNormal.x += x;
                vertexNormal.y += y;
                vertexNormal.z += z;
                vertexNormal.magnitude++;
                vertexNormal = vertexNormals[billboardMaterialId];
                vertexNormal.x += x;
                vertexNormal.y += y;
                vertexNormal.z += z;
                vertexNormal.magnitude++;
                vertexNormal = vertexNormals[materialId];
                vertexNormal.x += x;
                vertexNormal.y += y;
                vertexNormal.z += z;
                vertexNormal.magnitude++;
            } else if (b_30 == 1) {
                FaceNormalGL faceNormal = faceNormals[triangleIndex] = new FaceNormalGL();
                faceNormal.x = x;
                faceNormal.y = y;
                faceNormal.z = z;
            }
        }

        for (triangleIndex = 0; triangleIndex < this.numFaces; triangleIndex++) {
        	int texturedFace;
            int faceIndex = faces[triangleIndex];
            int color = mesh.facesColor[faceIndex] & 0xffff;
            if (mesh.facesTexture == null) {
                texturedFace = -1;
            } else {
                texturedFace = mesh.facesTexture[faceIndex];
            }

            if (mesh.facesAlpha == null) {
                alphaAmt = 0;
            } else {
                alphaAmt = mesh.facesAlpha[faceIndex] & 0xff;
            }

            short materialId = mesh.facesMaterial == null ? -1 : mesh.facesMaterial[faceIndex];
            if (materialId != -1 && (this.preferenceFlags & 0x40) != 0) {
                material = materialProvider.getMaterial(materialId & 0xffff);
                if (material.detailsOnly) {
                    materialId = -1;
                }
            }

            float aU = 0.0F;
            float aV = 0.0F;
            float bU = 0.0F;
            float bV = 0.0F;
            float cU = 0.0F;
            float cV = 0.0F;
            byte uidOffsetB = 0;
            byte uidOffsetC = 0;
            int i_95 = 0;
            byte renderType;
            if (materialId != -1) {
                if (texturedFace == -1) {
                    aU = 0.0F;
                    aV = 1.0F;
                    bU = 1.0F;
                    bV = 1.0F;
                    uidOffsetB = 1;
                    cU = 0.0F;
                    cV = 0.0F;
                    uidOffsetC = 2;
                } else {
                    texturedFace &= 0xff;
                    renderType = mesh.texturesMapping[texturedFace];
                    float f_40;
                    float f_41;
                    float f_42;
                    float f_52;
                    float f_53;
                    float f_54;
                    float f_55;
                    float f_56;
                    float f_57;
                    if (renderType == 0) {
                        short s_75 = mesh.facesA[faceIndex];
                        short s_76 = mesh.facesB[faceIndex];
                        short s_34 = mesh.facesC[faceIndex];
                        short s_35 = mesh.textureFaceX[texturedFace];
                        short s_36 = mesh.textureFaceY[texturedFace];
                        short s_77 = mesh.textureFaceZ[texturedFace];
                        float f_38 = mesh.verticesX[s_35];
                        float f_39 = mesh.verticesY[s_35];
                        f_40 = mesh.verticesZ[s_35];
                        f_41 = mesh.verticesX[s_36] - f_38;
                        f_42 = mesh.verticesY[s_36] - f_39;
                        float f_43 = mesh.verticesZ[s_36] - f_40;
                        float f_44 = mesh.verticesX[s_77] - f_38;
                        float f_45 = mesh.verticesY[s_77] - f_39;
                        float f_46 = mesh.verticesZ[s_77] - f_40;
                        float f_47 = mesh.verticesX[s_75] - f_38;
                        float f_48 = mesh.verticesY[s_75] - f_39;
                        float f_49 = mesh.verticesZ[s_75] - f_40;
                        float f_50 = mesh.verticesX[s_76] - f_38;
                        float f_51 = mesh.verticesY[s_76] - f_39;
                        f_52 = mesh.verticesZ[s_76] - f_40;
                        f_53 = mesh.verticesX[s_34] - f_38;
                        f_54 = mesh.verticesY[s_34] - f_39;
                        f_55 = mesh.verticesZ[s_34] - f_40;
                        f_56 = f_42 * f_46 - f_43 * f_45;
                        f_57 = f_43 * f_44 - f_41 * f_46;
                        float f_58 = f_41 * f_45 - f_42 * f_44;
                        float f_59 = f_45 * f_58 - f_46 * f_57;
                        float f_60 = f_46 * f_56 - f_44 * f_58;
                        float f_61 = f_44 * f_57 - f_45 * f_56;
                        float f_62 = 1.0F / (f_59 * f_41 + f_60 * f_42 + f_61 * f_43);
                        aU = (f_59 * f_47 + f_60 * f_48 + f_61 * f_49) * f_62;
                        bU = (f_59 * f_50 + f_60 * f_51 + f_61 * f_52) * f_62;
                        cU = (f_59 * f_53 + f_60 * f_54 + f_61 * f_55) * f_62;
                        f_59 = f_42 * f_58 - f_43 * f_57;
                        f_60 = f_43 * f_56 - f_41 * f_58;
                        f_61 = f_41 * f_57 - f_42 * f_56;
                        f_62 = 1.0F / (f_59 * f_44 + f_60 * f_45 + f_61 * f_46);
                        aV = (f_59 * f_47 + f_60 * f_48 + f_61 * f_49) * f_62;
                        bV = (f_59 * f_50 + f_60 * f_51 + f_61 * f_52) * f_62;
                        cV = (f_59 * f_53 + f_60 * f_54 + f_61 * f_55) * f_62;
                    } else {
                    	short faceA = mesh.facesA[faceIndex];
                    	short faceB = mesh.facesB[faceIndex];
                    	short faceC = mesh.facesC[faceIndex];
                        int centerX = transformMatrix.centerX[texturedFace];
                        int centerY = transformMatrix.centerY[texturedFace];
                        int centerZ = transformMatrix.centerZ[texturedFace];
                        float[] matrix = transformMatrix.matrix[texturedFace];
                        byte direction = mesh.texturesDirection[texturedFace];
                        f_40 = mesh.texturesSpeed[texturedFace] / 256.0F;
                        if (renderType == 1) {
                            f_41 = mesh.texturesScaleZ[texturedFace] / 1024.0F;
                            mapCylindericalUv(mesh.verticesX[faceA], mesh.verticesY[faceA], mesh.verticesZ[faceA], centerX, centerY, centerZ, matrix, f_41, direction, f_40, uvTypes);
                            aU = uvTypes[0];
                            aV = uvTypes[1];
                            mapCylindericalUv(mesh.verticesX[faceB], mesh.verticesY[faceB], mesh.verticesZ[faceB], centerX, centerY, centerZ, matrix, f_41, direction, f_40, uvTypes);
                            bU = uvTypes[0];
                            bV = uvTypes[1];
                            mapCylindericalUv(mesh.verticesX[faceC], mesh.verticesY[faceC], mesh.verticesZ[faceC], centerX, centerY, centerZ, matrix, f_41, direction, f_40, uvTypes);
                            cU = uvTypes[0];
                            cV = uvTypes[1];
                            f_42 = f_41 / 2.0F;
                            if ((direction & 0x1) == 0) {
                                if (bU - aU > f_42) {
                                    bU -= f_41;
                                    uidOffsetB = 1;
                                } else if (aU - bU > f_42) {
                                    bU += f_41;
                                    uidOffsetB = 2;
                                }

                                if (cU - aU > f_42) {
                                    cU -= f_41;
                                    uidOffsetC = 1;
                                } else if (aU - cU > f_42) {
                                    cU += f_41;
                                    uidOffsetC = 2;
                                }
                            } else {
                                if (bV - aV > f_42) {
                                    bV -= f_41;
                                    uidOffsetB = 1;
                                } else if (aV - bV > f_42) {
                                    bV += f_41;
                                    uidOffsetB = 2;
                                }

                                if (cV - aV > f_42) {
                                    cV -= f_41;
                                    uidOffsetC = 1;
                                } else if (aV - cV > f_42) {
                                    cV += f_41;
                                    uidOffsetC = 2;
                                }
                            }
                        } else if (renderType == 2) {
                            f_41 = mesh.texturesUtrans[texturedFace] / 256.0F;
                            f_42 = mesh.texturesVtrans[texturedFace] / 256.0F;
                            int i_100 = mesh.verticesX[faceB] - mesh.verticesX[faceA];
                            int i_101 = mesh.verticesY[faceB] - mesh.verticesY[faceA];
                            int i_102 = mesh.verticesZ[faceB] - mesh.verticesZ[faceA];
                            int i_103 = mesh.verticesX[faceC] - mesh.verticesX[faceA];
                            int i_104 = mesh.verticesY[faceC] - mesh.verticesY[faceA];
                            int i_105 = mesh.verticesZ[faceC] - mesh.verticesZ[faceA];
                            int i_106 = i_101 * i_105 - i_104 * i_102;
                            int i_107 = i_102 * i_103 - i_105 * i_100;
                            int i_108 = i_100 * i_104 - i_103 * i_101;
                            f_52 = 64.0F / mesh.texturesScaleX[texturedFace];
                            f_53 = 64.0F / mesh.texturesScaleY[texturedFace];
                            f_54 = 64.0F / mesh.texturesScaleZ[texturedFace];
                            f_55 = (i_106 * matrix[0] + i_107 * matrix[1] + i_108 * matrix[2]) / f_52;
                            f_56 = (i_106 * matrix[3] + i_107 * matrix[4] + i_108 * matrix[5]) / f_53;
                            f_57 = (i_106 * matrix[6] + i_107 * matrix[7] + i_108 * matrix[8]) / f_54;
                            i_95 = getCubeFace(f_55, f_56, f_57);
                            mapRectangularUv(mesh.verticesX[faceA], mesh.verticesY[faceA], mesh.verticesZ[faceA], centerX, centerY, centerZ, i_95, matrix, direction, f_40, f_41, f_42, uvTypes);
                            aU = uvTypes[0];
                            aV = uvTypes[1];
                            mapRectangularUv(mesh.verticesX[faceB], mesh.verticesY[faceB], mesh.verticesZ[faceB], centerX, centerY, centerZ, i_95, matrix, direction, f_40, f_41, f_42, uvTypes);
                            bU = uvTypes[0];
                            bV = uvTypes[1];
                            mapRectangularUv(mesh.verticesX[faceC], mesh.verticesY[faceC], mesh.verticesZ[faceC], centerX, centerY, centerZ, i_95, matrix, direction, f_40, f_41, f_42, uvTypes);
                            cU = uvTypes[0];
                            cV = uvTypes[1];
                        } else if (renderType == 3) {
                            mapSphericalUv(mesh.verticesX[faceA], mesh.verticesY[faceA], mesh.verticesZ[faceA], centerX, centerY, centerZ, matrix, direction, f_40, uvTypes);
                            aU = uvTypes[0];
                            aV = uvTypes[1];
                            mapSphericalUv(mesh.verticesX[faceB], mesh.verticesY[faceB], mesh.verticesZ[faceB], centerX, centerY, centerZ, matrix, direction, f_40, uvTypes);
                            bU = uvTypes[0];
                            bV = uvTypes[1];
                            mapSphericalUv(mesh.verticesX[faceC], mesh.verticesY[faceC], mesh.verticesZ[faceC], centerX, centerY, centerZ, matrix, direction, f_40, uvTypes);
                            cU = uvTypes[0];
                            cV = uvTypes[1];
                            if ((direction & 0x1) == 0) {
                                if (bU - aU > 0.5F) {
                                    --bU;
                                    uidOffsetB = 1;
                                } else if (aU - bU > 0.5F) {
                                    ++bU;
                                    uidOffsetB = 2;
                                }

                                if (cU - aU > 0.5F) {
                                    --cU;
                                    uidOffsetC = 1;
                                } else if (aU - cU > 0.5F) {
                                    ++cU;
                                    uidOffsetC = 2;
                                }
                            } else {
                                if (bV - aV > 0.5F) {
                                    --bV;
                                    uidOffsetB = 1;
                                } else if (aV - bV > 0.5F) {
                                    ++bV;
                                    uidOffsetB = 2;
                                }

                                if (cV - aV > 0.5F) {
                                    --cV;
                                    uidOffsetC = 1;
                                } else if (aV - cV > 0.5F) {
                                    ++cV;
                                    uidOffsetC = 2;
                                }
                            }
                        }
                    }
                }
            }

            if (mesh.faceRenderType == null) {
                renderType = 0;
            } else {
                renderType = mesh.faceRenderType[faceIndex];
            }

            if (renderType == 0) {
                long uid = ((long) texturedFace << 2) + ((long) ((long) i_95 << 24) + ((long) color << 8) + alphaAmt << 32);
                short faceA = mesh.facesA[faceIndex];
                short faceB = mesh.facesB[faceIndex];
                short faceC = mesh.facesC[faceIndex];
                VertexNormalGL vertexNormal = vertexNormals[faceA];
                this.facesA[triangleIndex] = connectNormal(faceA, uid, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, aU, aV);
                vertexNormal = vertexNormals[faceB];
                this.facesB[triangleIndex] = connectNormal(faceB, uid + uidOffsetB, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, bU, bV);
                vertexNormal = vertexNormals[faceC];
                this.facesC[triangleIndex] = connectNormal(faceC, uid + uidOffsetC, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, cU, cV);
            } else if (renderType == 1) {
                FaceNormalGL faceNormal = faceNormals[faceIndex];
                long uid = (((long) texturedFace << 2) + (faceNormal.x > 0 ? 1024 : 2048) + ((long) faceNormal.y + 256 << 12) + ((long) faceNormal.z + 256 << 22)) + ((long) ((long) i_95 << 24) + ((long) color << 8) + alphaAmt << 32);
                this.facesA[triangleIndex] = connectNormal(mesh.facesA[faceIndex], uid, faceNormal.x, faceNormal.y, faceNormal.z, 0, aU, aV);
                this.facesB[triangleIndex] = connectNormal(mesh.facesB[faceIndex], uid + uidOffsetB, faceNormal.x, faceNormal.y, faceNormal.z, 0, bU, bV);
                this.facesC[triangleIndex] = connectNormal(mesh.facesC[faceIndex], uid + uidOffsetC, faceNormal.x, faceNormal.y, faceNormal.z, 0, cU, cV);
            }

            if (mesh.facesAlpha != null) {
                this.facesAlpha[triangleIndex] = mesh.facesAlpha[faceIndex];
            }

            if (mesh.facesSubmeshes != null) {
                this.facesSubmeshes[triangleIndex] = mesh.facesSubmeshes[faceIndex];
            }

            this.facesColor[triangleIndex] = mesh.facesColor[faceIndex];
            this.facesMaterial[triangleIndex] = materialId;
        }

        triangleIndex = 0;
        lastMaterial = -10000;

        for (int i_85 = 0; i_85 < this.anInt8302; i_85++) {
        	short materialId = this.facesMaterial[i_85];
            if (materialId != lastMaterial) {
                triangleIndex++;
                lastMaterial = materialId;
            }
        }

        this.anIntArray8289 = new int[triangleIndex + 1];
        triangleIndex = 0;
        lastMaterial = -10000;

        for (int face = 0; face < this.anInt8302; face++) {
        	short materialId = this.facesMaterial[face];
            if (materialId != lastMaterial) {
                this.anIntArray8289[triangleIndex++] = face;
                lastMaterial = materialId;
            }
        }

        this.anIntArray8289[triangleIndex] = this.anInt8302;
        normalsUid = null;
        this.normalsX = clone(this.normalsX, this.numNormals);
        this.normalsY = clone(this.normalsY, this.numNormals);
        this.normalsZ = clone(this.normalsZ, this.numNormals);
        this.magnitudes = clone(this.magnitudes, this.numNormals);
        this.textureU = clone(this.textureU, this.numNormals);
        this.textureV = clone(this.textureV, this.numNormals);
        if (mesh.vertexGroups != null && ModelFunctionMaskGL.altersVertices(rasterizerFlags)) {
            this.vertexGroups = mesh.createVertexGroups(false);
        }

        if (mesh.billboards != null && ModelFunctionMaskGL.altersBillboards(rasterizerFlags)) {
            this.billboardGroups = mesh.createBillboardGroups();
        }

        if (mesh.faceGroups != null && ModelFunctionMaskGL.altersFaces(rasterizerFlags)) {
            int i_85 = 0;
            int[] ints_68 = new int[256];

            for (alphaAmt = 0; alphaAmt < this.numFaces; alphaAmt++) {
                i_21 = mesh.faceGroups[faces[alphaAmt]];
                if (i_21 >= 0) {
                    ++ints_68[i_21];
                    if (i_21 > i_85) {
                        i_85 = i_21;
                    }
                }
            }

            this.faceGroups = new int[i_85 + 1][];

            for (alphaAmt = 0; alphaAmt <= i_85; alphaAmt++) {
                this.faceGroups[alphaAmt] = new int[ints_68[alphaAmt]];
                ints_68[alphaAmt] = 0;
            }

            for (alphaAmt = 0; alphaAmt < this.numFaces; alphaAmt++) {
                i_21 = mesh.faceGroups[faces[alphaAmt]];
                if (i_21 >= 0) {
                    this.faceGroups[i_21][ints_68[i_21]++] = alphaAmt;
                }
            }
        }

    }

    public ModelGL(OpenGlToolkit toolkit) {
        this.toolkit = toolkit;
        this.vertexBuffer = new OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0);
        this.textureCoordinatesBuffer = new OpenGlArrayDrawBuffer(null, GL_FLOAT, 2, 0);
        this.normalsBuffer = new OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0);
        this.colorsBuffer = new OpenGlArrayDrawBuffer(null, GL_UNSIGNED_BYTE, 4, 0);
        this.modelElementsBuffer = new ModelElementsBufferGL();
    }

    public static float[] clone(float[] src, int size) {
        float[] dst = new float[size];
        System.arraycopy(src, 0, dst, 0, size);
        return dst;
    }

    public static short[] clone(short[] src, int size) {
        short[] dst = new short[size];
        System.arraycopy(src, 0, dst, 0, size);
        return dst;
    }

    public static int method13538(int i_0, int i_1) {
        int i_11 = i_1;
        i_11 = (i_0 & 0x7f) * i_11 >> 7;
        if (i_11 < 2) {
            i_11 = 2;
        } else if (i_11 > 126) {
            i_11 = 126;
        }

        return (i_0 & 0xff80) + i_11;
    }

    public static byte[] clone(byte[] src, int size) {
        byte[] dst = new byte[size];
        System.arraycopy(src, 0, dst, 0, size);
        return dst;
    }



    public void method13522(ShadowGL shadowgl) {
        if (this.toolkit.projectedVerticesX.length < this.numNormals) {
            this.toolkit.projectedVerticesX = new int[this.numNormals];
            this.toolkit.projectedVerticesY = new int[this.numNormals];
        }

        int[] projectedVerticesX = this.toolkit.projectedVerticesX;
        int[] projectedVerticesY = this.toolkit.projectedVerticesY;

        for (int v = 0; v < this.maxDepth; v++) {
        	int x = (this.verticesX[v] - (this.toolkit.anInt8438 * this.verticesY[v] >> 8) >> this.toolkit.shadowScale) - shadowgl.startX;
        	int y = (this.verticesZ[v] - (this.toolkit.anInt8439 * this.verticesY[v] >> 8) >> this.toolkit.shadowScale) - shadowgl.startY;
        	int i_7 = this.triangles[v];
        	int i_8 = this.triangles[v + 1];

            for (int i_9 = i_7; i_9 < i_8; i_9++) {
            	int i_10 = (this.aShortArray8288[i_9] & 0xffff) - 1;
                if (i_10 == -1) {
                    break;
                }

                projectedVerticesX[i_10] = x;
                projectedVerticesY[i_10] = y;
            }
        }

        for (int face = 0; face < this.anInt8302; face++) {
        	int faceA = this.facesA[face] & 0xffff;
        	int faceB = this.facesB[face] & 0xffff;
        	int faceC = this.facesC[face] & 0xffff;
        	int xA = projectedVerticesX[faceA];
        	int xB = projectedVerticesX[faceB];
        	int xC = projectedVerticesX[faceC];
            int yA = projectedVerticesY[faceA];
            int yB = projectedVerticesY[faceB];
            int yC = projectedVerticesY[faceC];
            if ((xA - xB) * (yB - yC) - (yB - yA) * (xC - xB) > 0) {
            	shadowgl.markShadedTriangle(yA, yB, yC, xA, xB, xC);
            }
        }
    }


    @Override
    public void applyShading(int i_1) {
        this.functionMask = i_1;
        if (this.aClass1668259 != null && (this.functionMask & 0x10000) == 0) {
            this.normalsX = this.aClass1668259.normalsX;
            this.normalsY = this.aClass1668259.normalsY;
            this.normalsZ = this.aClass1668259.normalsZ;
            this.magnitudes = this.aClass1668259.magnitude;
            this.aClass1668259 = null;
        }

        this.updateRequired = true;
        method13525();
    }

    @Override
    public Model copy(byte b_1, int functionMask, boolean bool3) {
        boolean bool4 = false;
        ModelGL class528Sub15;
        ModelGL class528Sub16;
        if (b_1 > 0 && b_1 <= 7) {
            class528Sub16 = this.toolkit.copyDst[b_1 - 1];
            class528Sub15 = this.toolkit.copySrc[b_1 - 1];
            bool4 = true;
        } else {
            class528Sub15 = class528Sub16 = new ModelGL(this.toolkit);
        }

        return method13524(class528Sub15, class528Sub16, functionMask, bool4);
    }

    public Model method13524(ModelGL dst, ModelGL src, int flags, boolean bool4) {
        dst.renderState = 0;
        dst.functionMask = flags;
        dst.preferenceFlags = this.preferenceFlags;
        dst.ambient = this.ambient;
        dst.contrast = this.contrast;
        dst.numVertices = this.numVertices;
        dst.maxDepth = this.maxDepth;
        dst.numNormals = this.numNormals;
        dst.numFaces = this.numFaces;
        dst.anInt8302 = this.anInt8302;
        dst.billboardCount = this.billboardCount;
        if ((flags & 0x100) != 0) {
            dst.hasAlphaTransition = true;
        } else {
            dst.hasAlphaTransition = this.hasAlphaTransition;
        }

        dst.textureAnimating = this.textureAnimating;
        boolean copyVerticesX = ModelFunctionMaskGL.altersVerticesX(flags);
        boolean copyVerticesY = ModelFunctionMaskGL.altersVerticesY(flags);
        boolean copyVerticesZ = ModelFunctionMaskGL.altersVerticesZ(flags);
        boolean copyAnyVertices = copyVerticesX | copyVerticesY | copyVerticesZ;
        int i_10;
        if (copyAnyVertices) {
            if (copyVerticesX) {
                if (src.verticesX != null && src.verticesX.length >= this.numVertices) {
                    dst.verticesX = src.verticesX;
                } else {
                    dst.verticesX = src.verticesX = new int[this.numVertices];
                }
            } else {
                dst.verticesX = this.verticesX;
            }

            if (copyVerticesY) {
                if (src.verticesY != null && src.verticesY.length >= this.numVertices) {
                    dst.verticesY = src.verticesY;
                } else {
                    dst.verticesY = src.verticesY = new int[this.numVertices];
                }
            } else {
                dst.verticesY = this.verticesY;
            }

            if (copyVerticesZ) {
                if (src.verticesZ != null && src.verticesZ.length >= this.numVertices) {
                    dst.verticesZ = src.verticesZ;
                } else {
                    dst.verticesZ = src.verticesZ = new int[this.numVertices];
                }
            } else {
                dst.verticesZ = this.verticesZ;
            }

            for (i_10 = 0; i_10 < this.numVertices; i_10++) {
                if (copyVerticesX) {
                    dst.verticesX[i_10] = this.verticesX[i_10];
                }

                if (copyVerticesY) {
                    dst.verticesY[i_10] = this.verticesY[i_10];
                }

                if (copyVerticesZ) {
                    dst.verticesZ[i_10] = this.verticesZ[i_10];
                }
            }
        } else {
            dst.verticesX = this.verticesX;
            dst.verticesY = this.verticesY;
            dst.verticesZ = this.verticesZ;
        }

        if (ModelFunctionMaskGL.altersAnyVerticeType(flags)) {
            if (bool4) {
                dst.renderState |= 0x1;
            }

            dst.vertexBuffer = src.vertexBuffer;
            dst.vertexBuffer.offset = this.vertexBuffer.offset;
            dst.vertexBuffer.buffer = this.vertexBuffer.buffer;
        } else if (ModelFunctionMaskGL.hasVertices(flags)) {
            dst.vertexBuffer = this.vertexBuffer;
        } else {
            dst.vertexBuffer = null;
        }

        if (ModelFunctionMaskGL.altersColor(flags)) {
            if (src.facesColor != null && src.facesColor.length >= this.numFaces) {
                dst.facesColor = src.facesColor;
            } else {
                dst.facesColor = src.facesColor = new short[this.numFaces];
            }

            for (i_10 = 0; i_10 < this.numFaces; i_10++) {
                dst.facesColor[i_10] = this.facesColor[i_10];
            }
        } else {
            dst.facesColor = this.facesColor;
        }

        if (ModelFunctionMaskGL.altersAlpha(flags)) {
            if (src.facesAlpha != null && src.facesAlpha.length >= this.numFaces) {
                dst.facesAlpha = src.facesAlpha;
            } else {
                dst.facesAlpha = src.facesAlpha = new byte[this.numFaces];
            }

            for (i_10 = 0; i_10 < this.numFaces; i_10++) {
                dst.facesAlpha[i_10] = this.facesAlpha[i_10];
            }
        } else {
            dst.facesAlpha = this.facesAlpha;
        }

        if (ModelFunctionMaskGL.method2199(flags, this.preferenceFlags)) {
            if (bool4) {
                dst.renderState |= 0x2;
            }

            dst.colorsBuffer = src.colorsBuffer;
            dst.colorsBuffer.offset = this.colorsBuffer.offset;
            dst.colorsBuffer.buffer = this.colorsBuffer.buffer;
        } else if (ModelFunctionMaskGL.hasColors(flags)) {
            dst.colorsBuffer = this.colorsBuffer;
        } else {
            dst.colorsBuffer = null;
        }

        int i_11;
        if (ModelFunctionMaskGL.altersLight(flags)) {
            if (src.normalsX != null && src.normalsX.length >= this.numNormals) {
                dst.normalsX = src.normalsX;
                dst.normalsY = src.normalsY;
                dst.normalsZ = src.normalsZ;
            } else {
                i_10 = this.numNormals;
                dst.normalsX = src.normalsX = new short[i_10];
                dst.normalsY = src.normalsY = new short[i_10];
                dst.normalsZ = src.normalsZ = new short[i_10];
            }

            if (this.aClass1668259 != null) {
                if (src.aClass1668259 == null) {
                    src.aClass1668259 = new ModelNormalsGL();
                }

                ModelNormalsGL class16612 = dst.aClass1668259 = src.aClass1668259;
                if (class16612.normalsX == null || class16612.normalsX.length < this.numNormals) {
                    i_11 = this.numNormals;
                    class16612.normalsX = new short[i_11];
                    class16612.normalsY = new short[i_11];
                    class16612.normalsZ = new short[i_11];
                    class16612.magnitude = new byte[i_11];
                }

                for (i_11 = 0; i_11 < this.numNormals; i_11++) {
                    dst.normalsX[i_11] = this.normalsX[i_11];
                    dst.normalsY[i_11] = this.normalsY[i_11];
                    dst.normalsZ[i_11] = this.normalsZ[i_11];
                    class16612.normalsX[i_11] = this.aClass1668259.normalsX[i_11];
                    class16612.normalsY[i_11] = this.aClass1668259.normalsY[i_11];
                    class16612.normalsZ[i_11] = this.aClass1668259.normalsZ[i_11];
                    class16612.magnitude[i_11] = this.aClass1668259.magnitude[i_11];
                }
            } else {
                for (i_10 = 0; i_10 < this.numNormals; i_10++) {
                    dst.normalsX[i_10] = this.normalsX[i_10];
                    dst.normalsY[i_10] = this.normalsY[i_10];
                    dst.normalsZ[i_10] = this.normalsZ[i_10];
                }
            }

        } else {
            dst.aClass1668259 = this.aClass1668259;
            dst.normalsX = this.normalsX;
            dst.normalsY = this.normalsY;
            dst.normalsZ = this.normalsZ;
        }
        dst.magnitudes = this.magnitudes;

        if (ModelFunctionMaskGL.method2261(flags, this.preferenceFlags)) {
            if (bool4) {
                dst.renderState |= 0x4;
            }

            dst.normalsBuffer = src.normalsBuffer;
            dst.normalsBuffer.offset = this.normalsBuffer.offset;
            dst.normalsBuffer.buffer = this.normalsBuffer.buffer;
        } else if (ModelFunctionMaskGL.hasNormals(flags, this.preferenceFlags)) {
            dst.normalsBuffer = this.normalsBuffer;
        } else {
            dst.normalsBuffer = null;
        }

        dst.textureU = this.textureU;
        dst.textureV = this.textureV;

        if (ModelFunctionMaskGL.method2200()) {
            if (bool4) {
                dst.renderState |= 0x8;
            }

            dst.textureCoordinatesBuffer = src.textureCoordinatesBuffer;
            dst.textureCoordinatesBuffer.offset = this.textureCoordinatesBuffer.offset;
            dst.textureCoordinatesBuffer.buffer = this.textureCoordinatesBuffer.buffer;
        } else if (ModelFunctionMaskGL.hasTextureCoordinates(flags)) {
            dst.textureCoordinatesBuffer = this.textureCoordinatesBuffer;
        } else {
            dst.textureCoordinatesBuffer = null;
        }

        if (ModelFunctionMaskGL.altersFacing(flags)) {
            if (src.facesA != null && src.facesA.length >= this.numFaces) {
                dst.facesA = src.facesA;
                dst.facesB = src.facesB;
                dst.facesC = src.facesC;
            } else {
                i_10 = this.numFaces;
                dst.facesA = src.facesA = new short[i_10];
                dst.facesB = src.facesB = new short[i_10];
                dst.facesC = src.facesC = new short[i_10];
            }

            for (i_10 = 0; i_10 < this.numFaces; i_10++) {
                dst.facesA[i_10] = this.facesA[i_10];
                dst.facesB[i_10] = this.facesB[i_10];
                dst.facesC[i_10] = this.facesC[i_10];
            }
        } else {
            dst.facesA = this.facesA;
            dst.facesB = this.facesB;
            dst.facesC = this.facesC;
        }

        if (ModelFunctionMaskGL.copyElementsBuffer(flags)) {
            if (bool4) {
                dst.renderState |= 0x10;
            }

            dst.modelElementsBuffer = src.modelElementsBuffer;
            dst.modelElementsBuffer.openglElementsBuffer = this.modelElementsBuffer.openglElementsBuffer;
        } else if (ModelFunctionMaskGL.hasElementsBuffer(flags)) {
            dst.modelElementsBuffer = this.modelElementsBuffer;
        } else {
            dst.modelElementsBuffer = null;
        }

        if (ModelFunctionMaskGL.altersTextures(flags)) {
            if (src.facesMaterial != null && src.facesMaterial.length >= this.numFaces) {
                dst.facesMaterial = src.facesMaterial;
            } else {
                i_10 = this.numFaces;
                dst.facesMaterial = src.facesMaterial = new short[i_10];
            }

            for (i_10 = 0; i_10 < this.numFaces; i_10++) {
                dst.facesMaterial[i_10] = this.facesMaterial[i_10];
            }
        } else {
            dst.facesMaterial = this.facesMaterial;
        }

        if (ModelFunctionMaskGL.copyBillboards(flags)) {
            if (src.billboardSurfaces != null && src.billboardSurfaces.length >= this.billboardCount) {
                dst.billboardSurfaces = src.billboardSurfaces;

                for (i_10 = 0; i_10 < this.billboardCount; i_10++) {
                    dst.billboardSurfaces[i_10].copyFrom(this.billboardSurfaces[i_10]);
                }
            } else {
                i_10 = this.billboardCount;
                dst.billboardSurfaces = src.billboardSurfaces = new BillboardSurfaceGL[i_10];

                for (i_11 = 0; i_11 < this.billboardCount; i_11++) {
                    dst.billboardSurfaces[i_11] = this.billboardSurfaces[i_11].copy();
                }
            }
        } else {
            dst.billboardSurfaces = this.billboardSurfaces;
        }

        dst.billboardEffects = this.billboardEffects;
        if (this.boundsCalculated) {
            dst.size3d = this.size3d;
            dst.size2d = this.size2d;
            dst.minX = this.minX;
            dst.maxX = this.maxX;
            dst.minY = this.minY;
            dst.maxY = this.maxY;
            dst.minZ = this.minZ;
            dst.maxZ = this.maxZ;
            dst.boundsCalculated = true;
        } else {
            dst.boundsCalculated = false;
        }

        dst.vertexGroups = this.vertexGroups;
        dst.faceGroups = this.faceGroups;
        dst.billboardGroups = this.billboardGroups;
        dst.aShortArray8288 = this.aShortArray8288;
        dst.triangles = this.triangles;
        dst.verticesSubmeshes = this.verticesSubmeshes;
        dst.facesSubmeshes = this.facesSubmeshes;
        dst.anIntArray8289 = this.anIntArray8289;
        dst.emissiveTriangles = this.emissiveTriangles;
        dst.effectiveVertices = this.effectiveVertices;
        return dst;
    }

    public void method13525() {
        if (this.updateRequired) {
            this.updateRequired = false;
            if (this.emissiveTriangles == null && this.effectiveVertices == null && this.billboardEffects == null && !ModelFunctionMaskGL.altersVerticePosition(this.functionMask)) {
                boolean verticesXUploaded = false;
                boolean verticesYUploaded = false;
                boolean verticesZUploaded = false;
                if (this.verticesX != null && !ModelFunctionMaskGL.method2202(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer.buffer == null) {
                        this.updateRequired = true;
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds();
                        }

                        verticesXUploaded = true;
                    }
                }

                if (this.verticesY != null && !ModelFunctionMaskGL.method2229(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer.buffer == null) {
                        this.updateRequired = true;
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds();
                        }

                        verticesYUploaded = true;
                    }
                }

                if (this.verticesZ != null && !ModelFunctionMaskGL.method2252(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer.buffer == null) {
                        this.updateRequired = true;
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds();
                        }

                        verticesZUploaded = true;
                    }
                }

                if (verticesXUploaded) {
                    this.verticesX = null;
                }

                if (verticesYUploaded) {
                    this.verticesY = null;
                }

                if (verticesZUploaded) {
                    this.verticesZ = null;
                }
            }

            if (this.aShortArray8288 != null && this.verticesX == null && this.verticesY == null && this.verticesZ == null) {
                this.aShortArray8288 = null;
                this.triangles = null;
            }

            if (this.magnitudes != null && !ModelFunctionMaskGL.method2204(this.functionMask, this.preferenceFlags)) {
                if (this.normalsBuffer != null) {
                    if (this.normalsBuffer.buffer != null) {
                        this.normalsZ = null;
                        this.normalsY = null;
                        this.normalsX = null;
                        this.magnitudes = null;
                    } else {
                        this.updateRequired = true;
                    }
                } else if (this.colorsBuffer != null && this.colorsBuffer.buffer == null) {
                    this.updateRequired = true;
                } else {
                    this.normalsZ = null;
                    this.normalsY = null;
                    this.normalsX = null;
                    this.magnitudes = null;
                }
            }

            if (this.facesColor != null && !ModelFunctionMaskGL.method2205(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer.buffer == null) {
                    this.updateRequired = true;
                } else {
                    this.facesColor = null;
                }
            }

            if (this.facesAlpha != null && !ModelFunctionMaskGL.method2206(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer.buffer == null) {
                    this.updateRequired = true;
                } else {
                    this.facesAlpha = null;
                }
            }

            if (this.textureU != null && !ModelFunctionMaskGL.method2241()) {
                if (this.textureCoordinatesBuffer != null && this.textureCoordinatesBuffer.buffer == null) {
                    this.updateRequired = true;
                } else {
                    this.textureV = null;
                    this.textureU = null;
                }
            }

            if (this.facesMaterial != null && !ModelFunctionMaskGL.method2212(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer.buffer == null) {
                    this.updateRequired = true;
                } else {
                    this.facesMaterial = null;
                }
            }

            if (this.facesA != null && !ModelFunctionMaskGL.method2211(this.functionMask, this.preferenceFlags)) {
                if (this.modelElementsBuffer != null && this.modelElementsBuffer.openglElementsBuffer == null || this.colorsBuffer != null && this.colorsBuffer.buffer == null) {
                    this.updateRequired = true;
                } else {
                    this.facesC = null;
                    this.facesB = null;
                    this.facesA = null;
                }
            }

            if (this.faceGroups != null && !ModelFunctionMaskGL.altersFaces(this.functionMask)) {
                this.faceGroups = null;
                this.facesSubmeshes = null;
            }

            if (this.vertexGroups != null && !ModelFunctionMaskGL.altersVertices(this.functionMask)) {
                this.vertexGroups = null;
                this.verticesSubmeshes = null;
            }

            if (this.billboardGroups != null && !ModelFunctionMaskGL.altersBillboards(this.functionMask)) {
                this.billboardGroups = null;
            }

            if (this.anIntArray8289 != null && (this.functionMask & 0x800) == 0 && (this.functionMask & 0x40000) == 0) {
                this.anIntArray8289 = null;
            }
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

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        this.boundsCalculated = false;
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

        for (i_4 = 0; i_4 < this.numNormals; i_4++) {
            i_5 = this.normalsZ[i_4] * i_2 + this.normalsX[i_4] * i_3 >> 14;
            this.normalsZ[i_4] = (short) (this.normalsZ[i_4] * i_3 - this.normalsX[i_4] * i_2 >> 14);
            this.normalsX[i_4] = (short) i_5;
        }

        if (this.normalsBuffer == null && this.colorsBuffer != null) {
            this.colorsBuffer.buffer = null;
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer.buffer = null;
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        this.boundsCalculated = false;
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

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        this.boundsCalculated = false;
    }

    @Override
    public void mirror() {
        int i_1;
        for (i_1 = 0; i_1 < this.maxDepth; i_1++) {
            this.verticesZ[i_1] = -this.verticesZ[i_1];
        }

        for (i_1 = 0; i_1 < this.numNormals; i_1++) {
            this.normalsZ[i_1] = (short) (-this.normalsZ[i_1]);
        }

        for (i_1 = 0; i_1 < this.numFaces; i_1++) {
            short s_2 = this.facesA[i_1];
            this.facesA[i_1] = this.facesC[i_1];
            this.facesC[i_1] = s_2;
        }

        if (this.normalsBuffer == null && this.colorsBuffer != null) {
            this.colorsBuffer.buffer = null;
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer.buffer = null;
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        if (this.modelElementsBuffer != null) {
            this.modelElementsBuffer.openglElementsBuffer = null;
        }

        this.boundsCalculated = false;
    }

    @Override
    public void scale(int x, int y, int z) {
        for (int v = 0; v < this.maxDepth; v++) {
            if (x != 128) {
                this.verticesX[v] = x * this.verticesX[v] >> 7;
            }

            if (y != 128) {
                this.verticesY[v] = y * this.verticesY[v] >> 7;
            }

            if (z != 128) {
                this.verticesZ[v] = z * this.verticesZ[v] >> 7;
            }
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        this.boundsCalculated = false;
    }

    @Override
    public void contourToGround(int contourType, int contourModifier, Ground currentLevel, Ground nextLevel, int sceneX, int averageHeight, int sceneY) {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        int minTileX = sceneX + this.minX;
        int maxTileX = sceneX + this.maxX;
        int minTileY = sceneY + this.minZ;
        int maxTileY = sceneY + this.maxZ;
        if (contourType != 1 && contourType != 2 && contourType != 3 && contourType != 5 || minTileX >= 0 && maxTileX + currentLevel.tileUnits >> currentLevel.tileScale < currentLevel.width && minTileY >= 0 && maxTileY + currentLevel.tileUnits >> currentLevel.tileScale < currentLevel.length) {
            if (contourType != 4 && contourType != 5) {
                minTileX >>= currentLevel.tileScale;
                maxTileX = maxTileX + (currentLevel.tileUnits - 1) >> currentLevel.tileScale;
                minTileY >>= currentLevel.tileScale;
                maxTileY = maxTileY + (currentLevel.tileUnits - 1) >> currentLevel.tileScale;
                if (currentLevel.getHeight(minTileX, minTileY) == averageHeight && currentLevel.getHeight(maxTileX, minTileY) == averageHeight && currentLevel.getHeight(minTileX, maxTileY) == averageHeight && currentLevel.getHeight(maxTileX, maxTileY) == averageHeight) {
                    return;
                }
            } else if (nextLevel == null || minTileX < 0 || nextLevel.tileUnits + maxTileX >> nextLevel.tileScale >= nextLevel.width || minTileY < 0 || nextLevel.tileUnits + maxTileY >> nextLevel.tileScale >= nextLevel.length) {
                return;
            }

            if (contourType == 1) {
                for (int v = 0; v < this.maxDepth; v++) {
                    this.verticesY[v] = this.verticesY[v] + currentLevel.getAverageHeight(this.verticesX[v] + sceneX, this.verticesZ[v] + sceneY) - averageHeight;
                }
            } else if (contourType == 2) {
                short MinY = this.minY;
                if (MinY == 0) {
                    return;
                }

                for (int v = 0; v < this.maxDepth; v++) {
                    int i_14 = (this.verticesY[v] << 16) / MinY;
                    if (i_14 < contourModifier) {
                        this.verticesY[v] += (currentLevel.getAverageHeight(this.verticesX[v] + sceneX, this.verticesZ[v] + sceneY) - averageHeight) * (contourModifier - i_14) / contourModifier;
                    }
                }
            } else if (contourType == 3) {
                int width = (contourModifier & 0xff) * 4;
                int length = (contourModifier >> 8 & 0xff) * 4;
                int xan = (contourModifier >> 16 & 0xff) << 6;
                int zan = (contourModifier >> 24 & 0xff) << 6;
                if (sceneX - (width >> 1) < 0 || sceneX + currentLevel.tileUnits + (width >> 1) >= currentLevel.width << currentLevel.tileScale || sceneY - (length >> 1) < 0 || currentLevel.tileUnits + (length >> 1) + sceneY >= currentLevel.length << currentLevel.tileScale) {
                    return;
                }

                hillChange(currentLevel, sceneX, averageHeight, sceneY, width, length, xan, zan);
            } else if (contourType == 4) {
                int sizeY = this.maxY - this.minY;

                for (int v = 0; v < this.maxDepth; v++) {
                    this.verticesY[v] += (nextLevel.getAverageHeight(this.verticesX[v] + sceneX, this.verticesZ[v] + sceneY) - averageHeight) + sizeY;
                }
            } else if (contourType == 5) {
                int sizeY = this.maxY - this.minY;

                for (int v = 0; v < this.maxDepth; v++) {
                    int x = sceneX + this.verticesX[v];
                    int z = sceneY + this.verticesZ[v];
                    int currentAvgHeight = currentLevel.getAverageHeight(x, z);
                    int nextAvgHeight = nextLevel.getAverageHeight(x, z);
                    int delta = currentAvgHeight - nextAvgHeight - contourModifier;
                    this.verticesY[v] = (delta * ((this.verticesY[v] << 8) / sizeY) >> 8) - (averageHeight - currentAvgHeight);
                }
            }
                
            

            if (this.vertexBuffer != null) {
                this.vertexBuffer.buffer = null;
            }

            this.boundsCalculated = false;
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

        for (int v = 0; v < this.maxDepth; v++) {
            int vertexX = this.verticesX[v];
            int vertexY = this.verticesY[v];
            int vertexZ = this.verticesZ[v];
            if (vertexX < minX) {
                minX = vertexX;
            }

            if (vertexX > maxX) {
                maxX = vertexX;
            }

            if (vertexY < minY) {
                minY = vertexY;
            }

            if (vertexY > maxY) {
                maxY = vertexY;
            }

            if (vertexZ < minZ) {
                minZ = vertexZ;
            }

            if (vertexZ > maxZ) {
                maxZ = vertexZ;
            }

            int s2d = vertexX * vertexX + vertexZ * vertexZ;
            if (s2d > size2d) {
                size2d = s2d;
            }

            int s3d = vertexX * vertexX + vertexZ * vertexZ + vertexY * vertexY;
            if (s3d > size3d) {
                size3d = s3d;
            }
        }

        this.minX = (short) minX;
        this.maxX = (short) maxX;
        this.minY = (short) minY;
        this.maxY = (short) maxY;
        this.minZ = (short) minZ;
        this.maxZ = (short) maxZ;
        this.size2d = (int) (Math.sqrt(size2d) + 0.99D);
        this.size3d = (int) (Math.sqrt(size3d) + 0.99D);
        this.boundsCalculated = true;
    }

    @Override
    public int getSize2d() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.size2d;
    }

    @Override
    public int getSize3d() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.size3d;
    }

    @Override
    public int getMinX() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.minX;
    }


    @Override
    public int getMinY() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.minY;
    }

    @Override
    public int getMaxY() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.maxY;
    }

    @Override
    public int getMinZ() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.minZ;
    }

    @Override
    public void applyShading(Model model, int i_2, int i_3, int i_4, boolean bool5) {
        ModelGL openglModel = (ModelGL) model;
        if (this.numFaces != 0 && openglModel.numFaces != 0) {
            int glMaxDepth = openglModel.maxDepth;
            int[] vertX = openglModel.verticesX;
            int[] vertY = openglModel.verticesY;
            int[] vertZ = openglModel.verticesZ;
            short[] normX = openglModel.normalsX;
            short[] normY = openglModel.normalsY;
            short[] normZ = openglModel.normalsZ;
            byte[] normMag = openglModel.magnitudes;
            short[] shorts_15;
            short[] shorts_16;
            short[] shorts_17;
            byte[] bytes_18;
            if (this.aClass1668259 != null) {
                shorts_15 = this.aClass1668259.normalsX;
                shorts_16 = this.aClass1668259.normalsY;
                shorts_17 = this.aClass1668259.normalsZ;
                bytes_18 = this.aClass1668259.magnitude;
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
            if (openglModel.aClass1668259 != null) {
                shorts_19 = openglModel.aClass1668259.normalsX;
                shorts_20 = openglModel.aClass1668259.normalsY;
                shorts_21 = openglModel.aClass1668259.normalsZ;
                bytes_22 = openglModel.aClass1668259.magnitude;
            } else {
                shorts_19 = null;
                shorts_20 = null;
                shorts_21 = null;
                bytes_22 = null;
            }

            int[] ints_23 = openglModel.triangles;
            short[] shorts_24 = openglModel.aShortArray8288;
            if (!openglModel.boundsCalculated) {
                openglModel.calculateBounds();
            }

            short s_25 = openglModel.minY;
            short s_26 = openglModel.maxY;
            short s_27 = openglModel.minX;
            short s_28 = openglModel.maxX;
            short s_29 = openglModel.minZ;
            short s_30 = openglModel.maxZ;

            for (int i_31 = 0; i_31 < this.maxDepth; i_31++) {
                int i_32 = this.verticesY[i_31] - i_3;
                if (i_32 >= s_25 && i_32 <= s_26) {
                    int i_33 = this.verticesX[i_31] - i_2;
                    if (i_33 >= s_27 && i_33 <= s_28) {
                        int i_34 = this.verticesZ[i_31] - i_4;
                        if (i_34 >= s_29 && i_34 <= s_30) {
                            int i_35 = -1;
                            int i_36 = this.triangles[i_31];
                            int i_37 = this.triangles[i_31 + 1];

                            int i_38;
                            for (i_38 = i_36; i_38 < i_37; i_38++) {
                                i_35 = this.aShortArray8288[i_38] - 1;
                                if (i_35 == -1 || this.magnitudes[i_35] != 0) {
                                    break;
                                }
                            }

                            if (i_35 != -1) {
                                for (i_38 = 0; i_38 < glMaxDepth; i_38++) {
                                    if (i_33 == vertX[i_38] && i_34 == vertZ[i_38] && i_32 == vertY[i_38]) {
                                        int i_39 = -1;
                                        i_36 = ints_23[i_38];
                                        i_37 = ints_23[i_38 + 1];

                                        for (int i_40 = i_36; i_40 < i_37; i_40++) {
                                            i_39 = shorts_24[i_40] - 1;
                                            if (i_39 == -1 || normMag[i_39] != 0) {
                                                break;
                                            }
                                        }

                                        if (i_39 != -1) {
                                            if (shorts_15 == null) {
                                                this.aClass1668259 = new ModelNormalsGL();
                                                shorts_15 = this.aClass1668259.normalsX = ArrayUtils.arraycopy(this.normalsX);
                                                shorts_16 = this.aClass1668259.normalsY = ArrayUtils.arraycopy(this.normalsY);
                                                shorts_17 = this.aClass1668259.normalsZ = ArrayUtils.arraycopy(this.normalsZ);
                                                bytes_18 = this.aClass1668259.magnitude = ArrayUtils.safeFullArraycopy(this.magnitudes);
                                            }

                                            if (shorts_19 == null) {
                                                ModelNormalsGL class16646 = openglModel.aClass1668259 = new ModelNormalsGL();
                                                shorts_19 = class16646.normalsX = ArrayUtils.arraycopy(normX);
                                                shorts_20 = class16646.normalsY = ArrayUtils.arraycopy(normY);
                                                shorts_21 = class16646.normalsZ = ArrayUtils.arraycopy(normZ);
                                                bytes_22 = class16646.magnitude = ArrayUtils.safeFullArraycopy(normMag);
                                            }

                                            short s_47 = this.normalsX[i_35];
                                            short s_41 = this.normalsY[i_35];
                                            short s_42 = this.normalsZ[i_35];
                                            byte b_43 = this.magnitudes[i_35];
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
                                            b_43 = normMag[i_39];
                                            i_36 = this.triangles[i_31];
                                            i_37 = this.triangles[i_31 + 1];

                                            for (i_44 = i_36; i_44 < i_37; i_44++) {
                                                i_45 = this.aShortArray8288[i_44] - 1;
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

                                            if (this.normalsBuffer == null && this.colorsBuffer != null) {
                                                this.colorsBuffer.buffer = null;
                                            }

                                            if (this.normalsBuffer != null) {
                                                this.normalsBuffer.buffer = null;
                                            }

                                            if (openglModel.normalsBuffer == null && openglModel.colorsBuffer != null) {
                                                openglModel.colorsBuffer.buffer = null;
                                            }

                                            if (openglModel.normalsBuffer != null) {
                                                openglModel.normalsBuffer.buffer = null;
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

    }

    @Override
    public void setAmbient(int ambient) {
        this.ambient = (short) ambient;
        if (this.colorsBuffer != null) {
            this.colorsBuffer.buffer = null;
        }
    }

    @Override
    public void setContrast(int contrast) {
        this.contrast = (short) contrast;
        if (this.colorsBuffer != null) {
            this.colorsBuffer.buffer = null;
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer.buffer = null;
        }

    }

    @Override
    public int getContrast() {
        return this.contrast;
    }


    public int hslToRgb(int colorHsl, short materialId, int ambient, byte alpha) {
        int i_5 = ColorUtil.hslToRgb[method13538(colorHsl, ambient)];
        if (materialId != -1) {
            MaterialType materialType = this.toolkit.materialProvider.getMaterial(materialId & 0xffff);
            int a = materialType.alpha & 0xff;
            int i_10;
            if (a != 0) {
                int i_8;
                if (ambient < 0) {
                    i_8 = 0;
                } else if (ambient > 127) {
                    i_8 = 16777215;
                } else {
                    i_8 = ambient * 131586;
                }

                i_10 = 256 - a;
                i_5 = ((i_8 & 0xff00ff) * a + (i_5 & 0xff00ff) * i_10 & -16711936) + (a * (i_8 & 0xff00) + i_10 * (i_5 & 0xff00) & 0xff0000) >> 8;
            }

            int b = materialType.brightness & 0xff;
            if (b != 0) {
                b += 256;
                int i_9 = b * ((i_5 & 0xff0000) >> 16);
                if (i_9 > 65535) {
                    i_9 = 65535;
                }

                i_10 = b * ((i_5 & 0xff00) >> 8);
                if (i_10 > 65535) {
                    i_10 = 65535;
                }

                int i_11 = (i_5 & 0xff) * b;
                if (i_11 > 65535) {
                    i_11 = 65535;
                }

                i_5 = (i_11 >> 8) + (i_10 & 0xff00) + (i_9 << 8 & 0xff0000);
            }
        }

        return i_5 << 8 | 255 - (alpha & 0xff);
    }

    @Override
    public void applyEnvironmentAlpha(byte alpha, byte[] bytes_2) {
        if (bytes_2 == null) {
            for (int f = 0; f < this.numFaces; f++) {
                this.facesAlpha[f] = alpha;
            }
        } else {
            for (int f = 0; f < this.numFaces; f++) {
                int antiAlpha = 255 - (255 - (alpha & 0xff)) * (255 - (bytes_2[f] & 0xff)) / 255;
                this.facesAlpha[f] = (byte) antiAlpha;
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer.buffer = null;
        }
    }



    @Override
    public boolean accountForLayers() {
        return this.hasAlphaTransition;
    }



    @Override
    public void awaitLock() {
    	// NOOP
    }

    @Override
    public void unlock() {
    	// NOOP
    }

    @Override
    public boolean startAnimation() {
        if (this.vertexGroups == null) {
            return false;
        } 
        for (int v = 0; v < this.numVertices; v++) {
            this.verticesX[v] <<= 4;
            this.verticesY[v] <<= 4;
            this.verticesZ[v] <<= 4;
        }

        originX = 0;
        originY = 0;
        originZ = 0;
        return true;
    }

    @Override
    public void animateTransform(int transformationType, int[] labels, int rotationX, int rotationY, int rotationZ, boolean lights, int submesh, int[] verticesData) {
        int i_31 = rotationX;
        int i_41 = rotationY;
        int i_51 = rotationZ;
        int i_9 = labels.length;
        if (transformationType == 0) {
            i_31 <<= 4;
            i_41 <<= 4;
            i_51 <<= 4;
            int i_10 = 0;
            originX = 0;
            originY = 0;
            originZ = 0;

            for (int i_11 = 0; i_11 < i_9; i_11++) {
                int i_12 = labels[i_11];
                if (i_12 < this.vertexGroups.length) {
                    int[] ints_13 = this.vertexGroups[i_12];

                    for (int i_14 = 0; i_14 < ints_13.length; i_14++) {
                        int v = ints_13[i_14];
                        if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[v]) != 0) {
                            originX += this.verticesX[v];
                            originY += this.verticesY[v];
                            originZ += this.verticesZ[v];
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
            if (transformationType == 1) {
                if (verticesData != null) {
                    int i_10 = i_51 * verticesData[2] + verticesData[1] * i_41 + i_31 * verticesData[0] + 8192 >> 14;
                    int i_11 = i_31 * verticesData[3] + i_51 * verticesData[5] + i_41 * verticesData[4] + 8192 >> 14;
                    int i_12 = i_31 * verticesData[6] + verticesData[7] * i_41 + i_51 * verticesData[8] + 8192 >> 14;
                    i_31 = i_10;
                    i_41 = i_11;
                    i_51 = i_12;
                }

                i_31 <<= 4;
                i_41 <<= 4;
                i_51 <<= 4;

                for (int i_10 = 0; i_10 < i_9; i_10++) {
                    int i_11 = labels[i_10];
                    if (i_11 < this.vertexGroups.length) {
                    	int[] ints_47 = this.vertexGroups[i_11];

                        for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                            int i_14 = ints_47[i_48];
                            if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[i_14]) != 0) {
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
                if (transformationType == 2) {
                    if (verticesData != null) {
                        int i_10 = verticesData[9] << 4;
                        int i_11 = verticesData[10] << 4;
                        int i_12 = verticesData[11] << 4;
                        int i_48 = verticesData[12] << 4;
                        int i_14 = verticesData[13] << 4;
                        int i_15 = verticesData[14] << 4;
                        if (withinOrigin) {
                            i_16 = verticesData[0] * originX + verticesData[6] * originZ + verticesData[3] * originY + 8192 >> 14;
                            i_17 = verticesData[7] * originZ + verticesData[4] * originY + verticesData[1] * originX + 8192 >> 14;
                            i_18 = verticesData[8] * originZ + verticesData[2] * originX + verticesData[5] * originY + 8192 >> 14;
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
                                    i_34 += verticesData[i_53 + i_33 * 3] * ints_49[i_53 + i_32 * 3];
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
                                    i_38 += ints_31[i_37 + i_39 * 3] * verticesData[i_39 + i_36 * 3];
                                }

                                ints_35[i_37 + i_36 * 3] = i_38 + 8192 >> 14;
                            }
                        }

                        i_36 = i_34 * verticesData[2] + i_32 * verticesData[0] + i_33 * verticesData[1] + 8192 >> 14;
                        i_37 = i_32 * verticesData[3] + i_33 * verticesData[4] + verticesData[5] * i_34 + 8192 >> 14;
                        i_38 = i_32 * verticesData[6] + i_34 * verticesData[8] + verticesData[7] * i_33 + 8192 >> 14;
                        i_36 += i_10;
                        i_37 += i_11;
                        i_38 += i_12;

                        for (i_39 = 0; i_39 < i_9; i_39++) {
                            i_40 = labels[i_39];
                            if (i_40 < this.vertexGroups.length) {
                                int[] ints_41 = this.vertexGroups[i_40];

                                for (int i_42 = 0; i_42 < ints_41.length; i_42++) {
                                    int i_43 = ints_41[i_42];
                                    if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[i_43]) != 0) {
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
                        for (int i_10 = 0; i_10 < i_9; i_10++) {
                            int i_11 = labels[i_10];
                            if (i_11 < this.vertexGroups.length) {
                            	int[] ints_47 = this.vertexGroups[i_11];

                                for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                    int i_14 = ints_47[i_48];
                                    if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[i_14]) != 0) {
                                        this.verticesX[i_14] -= originX;
                                        this.verticesY[i_14] -= originY;
                                        this.verticesZ[i_14] -= originZ;
                                        if (i_51 != 0) {
                                        	int i_15 = MathUtils.SINE[i_51];
                                            i_16 = MathUtils.COSINE[i_51];
                                            i_17 = i_15 * this.verticesY[i_14] + i_16 * this.verticesX[i_14] + 16383 >> 14;
                                            this.verticesY[i_14] = i_16 * this.verticesY[i_14] - i_15 * this.verticesX[i_14] + 16383 >> 14;
                                            this.verticesX[i_14] = i_17;
                                        }

                                        if (i_31 != 0) {
                                        	int i_15 = MathUtils.SINE[i_31];
                                            i_16 = MathUtils.COSINE[i_31];
                                            i_17 = i_16 * this.verticesY[i_14] - i_15 * this.verticesZ[i_14] + 16383 >> 14;
                                            this.verticesZ[i_14] = i_15 * this.verticesY[i_14] + i_16 * this.verticesZ[i_14] + 16383 >> 14;
                                            this.verticesY[i_14] = i_17;
                                        }

                                        if (i_41 != 0) {
                                        	int i_15 = MathUtils.SINE[i_41];
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

                        if (lights) {
                            for (int i_10 = 0; i_10 < i_9; i_10++) {
                            	int i_11 = labels[i_10];
                                if (i_11 < this.vertexGroups.length) {
                                	int[] ints_47 = this.vertexGroups[i_11];

                                    for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                    	int i_14 = ints_47[i_48];
                                        if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[i_14]) != 0) {
                                        	int i_15 = this.triangles[i_14];
                                            i_16 = this.triangles[i_14 + 1];

                                            for (i_17 = i_15; i_17 < i_16; i_17++) {
                                                i_18 = this.aShortArray8288[i_17] - 1;
                                                if (i_18 == -1) {
                                                    break;
                                                }

                                                if (i_51 != 0) {
                                                    i_19 = MathUtils.SINE[i_51];
                                                    i_20 = MathUtils.COSINE[i_51];
                                                    i_21 = this.normalsY[i_18] * i_19 + this.normalsX[i_18] * i_20 + 16383 >> 14;
                                                    this.normalsY[i_18] = (short) (this.normalsY[i_18] * i_20 - this.normalsX[i_18] * i_19 + 16383 >> 14);
                                                    this.normalsX[i_18] = (short) i_21;
                                                }

                                                if (i_31 != 0) {
                                                    i_19 = MathUtils.SINE[i_31];
                                                    i_20 = MathUtils.COSINE[i_31];
                                                    i_21 = this.normalsY[i_18] * i_20 - this.normalsZ[i_18] * i_19 + 16383 >> 14;
                                                    this.normalsZ[i_18] = (short) (this.normalsY[i_18] * i_19 + this.normalsZ[i_18] * i_20 + 16383 >> 14);
                                                    this.normalsY[i_18] = (short) i_21;
                                                }

                                                if (i_41 != 0) {
                                                    i_19 = MathUtils.SINE[i_41];
                                                    i_20 = MathUtils.COSINE[i_41];
                                                    i_21 = this.normalsZ[i_18] * i_19 + this.normalsX[i_18] * i_20 + 16383 >> 14;
                                                    this.normalsZ[i_18] = (short) (this.normalsZ[i_18] * i_20 - this.normalsX[i_18] * i_19 + 16383 >> 14);
                                                    this.normalsX[i_18] = (short) i_21;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (this.normalsBuffer == null && this.colorsBuffer != null) {
                                this.colorsBuffer.buffer = null;
                            }

                            if (this.normalsBuffer != null) {
                                this.normalsBuffer.buffer = null;
                            }
                        }
                    }
                } else if (transformationType == 3) {
                    if (verticesData != null) {
                    	int i_10 = verticesData[9] << 4;
                    	int i_11 = verticesData[10] << 4;
                    	int i_12 = verticesData[11] << 4;
                    	int i_48 = verticesData[12] << 4;
                        int i_14 = verticesData[13] << 4;
                        int i_15 = verticesData[14] << 4;
                        if (withinOrigin) {
                            i_16 = verticesData[0] * originX + verticesData[6] * originZ + verticesData[3] * originY + 8192 >> 14;
                            i_17 = verticesData[7] * originZ + verticesData[4] * originY + verticesData[1] * originX + 8192 >> 14;
                            i_18 = verticesData[8] * originZ + verticesData[2] * originX + verticesData[5] * originY + 8192 >> 14;
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
                        int[] ints_50 = {i_16 * verticesData[0] + 8192 >> 14, i_16 * verticesData[3] + 8192 >> 14, i_16 * verticesData[6] + 8192 >> 14, i_17 * verticesData[1] + 8192 >> 14, i_17 * verticesData[4] + 8192 >> 14, verticesData[7] * i_17 + 8192 >> 14, i_18 * verticesData[2] + 8192 >> 14, verticesData[5] * i_18 + 8192 >> 14, i_18 * verticesData[8] + 8192 >> 14};
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
                                    i_32 += ints_50[i_52 + i_33 * 3] * verticesData[i_33 + i_30 * 3];
                                }

                                ints_51[i_52 + i_30 * 3] = i_32 + 8192 >> 14;
                            }
                        }

                        i_30 = i_28 * verticesData[2] + i_26 * verticesData[0] + i_27 * verticesData[1] + 8192 >> 14;
                        i_52 = i_26 * verticesData[3] + i_27 * verticesData[4] + verticesData[5] * i_28 + 8192 >> 14;
                        i_32 = i_26 * verticesData[6] + i_28 * verticesData[8] + verticesData[7] * i_27 + 8192 >> 14;
                        i_30 += i_10;
                        i_52 += i_11;
                        i_32 += i_12;

                        for (i_33 = 0; i_33 < i_9; i_33++) {
                            i_34 = labels[i_33];
                            if (i_34 < this.vertexGroups.length) {
                                ints_35 = this.vertexGroups[i_34];

                                for (i_36 = 0; i_36 < ints_35.length; i_36++) {
                                    i_37 = ints_35[i_36];
                                    if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[i_37]) != 0) {
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
                        for (int i_10 = 0; i_10 < i_9; i_10++) {
                        	int i_11 = labels[i_10];
                            if (i_11 < this.vertexGroups.length) {
                            	int[] ints_47 = this.vertexGroups[i_11];

                                for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                	int i_14 = ints_47[i_48];
                                    if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[i_14]) != 0) {
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
                    BillboardEffectGL class16254;
                    BillboardSurfaceGL class14556;
                    if (transformationType == 5) {
                        if (this.faceGroups != null) {
                            for (int i_10 = 0; i_10 < i_9; i_10++) {
                            	int i_11 = labels[i_10];
                                if (i_11 < this.faceGroups.length) {
                                	int[] ints_47 = this.faceGroups[i_11];

                                    for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                    	int i_14 = ints_47[i_48];
                                        if (this.facesSubmeshes == null || (submesh & this.facesSubmeshes[i_14]) != 0) {
                                        	int i_15 = (this.facesAlpha[i_14] & 0xff) + i_31 * 8;
                                            if (i_15 < 0) {
                                                i_15 = 0;
                                            } else if (i_15 > 255) {
                                                i_15 = 255;
                                            }

                                            this.facesAlpha[i_14] = (byte) i_15;
                                            if (this.colorsBuffer != null) {
                                                this.colorsBuffer.buffer = null;
                                            }
                                        }
                                    }
                                }
                            }

                            if (this.billboardEffects != null) {
                                for (int i_10 = 0; i_10 < this.billboardCount; i_10++) {
                                    class16254 = this.billboardEffects[i_10];
                                    class14556 = this.billboardSurfaces[i_10];
                                    class14556.diffuseColor = class14556.diffuseColor & 0xffffff | 255 - (this.facesAlpha[class16254.appliesOnFace] & 0xff) << 24;
                                }
                            }
                        }
                    } else if (transformationType == 7) {
                        if (this.faceGroups != null) {
                            for (int i_10 = 0; i_10 < i_9; i_10++) {
                            	int i_11 = labels[i_10];
                                if (i_11 < this.faceGroups.length) {
                                	int[] ints_47 = this.faceGroups[i_11];

                                    for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                    	int i_14 = ints_47[i_48];
                                        if (this.facesSubmeshes == null || (submesh & this.facesSubmeshes[i_14]) != 0) {
                                        	int i_15 = this.facesColor[i_14] & 0xffff;
                                            i_16 = i_15 >> 10 & 0x3f;
                                            i_17 = i_15 >> 7 & 0x7;
                                            i_18 = i_15 & 0x7f;
                                            i_16 = i_31 + i_16 & 0x3f;
                                            i_17 += i_41 / 4;
                                            if (i_17 < 0) {
                                                i_17 = 0;
                                            } else if (i_17 > 7) {
                                                i_17 = 7;
                                            }

                                            i_18 += i_51;
                                            if (i_18 < 0) {
                                                i_18 = 0;
                                            } else if (i_18 > 127) {
                                                i_18 = 127;
                                            }

                                            this.facesColor[i_14] = (short) (i_16 << 10 | i_17 << 7 | i_18);
                                            if (this.colorsBuffer != null) {
                                                this.colorsBuffer.buffer = null;
                                            }
                                        }
                                    }
                                }
                            }

                            if (this.billboardEffects != null) {
                                for (int i_10 = 0; i_10 < this.billboardCount; i_10++) {
                                    class16254 = this.billboardEffects[i_10];
                                    class14556 = this.billboardSurfaces[i_10];
                                    class14556.diffuseColor = class14556.diffuseColor & -16777216 | ColorUtil.hslToRgb[this.facesColor[class16254.appliesOnFace] & 0xffff] & 0xffffff;
                                }
                            }
                        }
                    } else {
                        if (transformationType == 8) {
                            if (this.billboardGroups != null) {
                                for (int i_10 = 0; i_10 < i_9; i_10++) {
                                	int i_11 = labels[i_10];
                                    if (i_11 < this.billboardGroups.length) {
                                    	int[] ints_47 = this.billboardGroups[i_11];

                                        for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                        	BillboardSurfaceGL surface = this.billboardSurfaces[ints_47[i_48]];
                                            surface.offsetX += i_31;
                                            surface.offsetY += i_41;
                                        }
                                    }
                                }
                            }
                        } else if (transformationType == 10) {
                            if (this.billboardGroups != null) {
                                for (int i_10 = 0; i_10 < i_9; i_10++) {
                                	int i_11 = labels[i_10];
                                    if (i_11 < this.billboardGroups.length) {
                                    	int[] ints_47 = this.billboardGroups[i_11];

                                        for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                        	BillboardSurfaceGL surface = this.billboardSurfaces[ints_47[i_48]];
                                            surface.scaleX = i_31 * surface.scaleX >> 7;
                                            surface.scaleY = i_41 * surface.scaleY >> 7;
                                        }
                                    }
                                }
                            }
                        } else if (transformationType == 9 && this.billboardGroups != null) {
                            for (int i_10 = 0; i_10 < i_9; i_10++) {
                            	int i_11 = labels[i_10];
                                if (i_11 < this.billboardGroups.length) {
                                	int[] ints_47 = this.billboardGroups[i_11];

                                    for (int i_48 = 0; i_48 < ints_47.length; i_48++) {
                                    	BillboardSurfaceGL surface = this.billboardSurfaces[ints_47[i_48]];
                                        surface.rotation = i_31 + surface.rotation & 0x3fff;
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
    public void animateShadowtransform(int transformationType, int x, int y, int z) {
        if (transformationType == 0) {
        	int transformCount = 0;
            originX = 0;
            originY = 0;
            originZ = 0;

            for (int v = 0; v < this.maxDepth; v++) {
                originX += this.verticesX[v];
                originY += this.verticesY[v];
                originZ += this.verticesZ[v];
                transformCount++;
            }

            if (transformCount > 0) {
                originX = x + originX / transformCount;
                originY = y + originY / transformCount;
                originZ = z + originZ / transformCount;
            } else {
                originX = x;
                originY = y;
                originZ = z;
            }
        } else if (transformationType == 1) {
            for (int v = 0; v < this.maxDepth; v++) {
                this.verticesX[v] += x;
                this.verticesY[v] += y;
                this.verticesZ[v] += z;
            }
        } else if (transformationType == 2) {
            for (int v = 0; v < this.maxDepth; v++) {
                this.verticesX[v] -= originX;
                this.verticesY[v] -= originY;
                this.verticesZ[v] -= originZ;
                if (z != 0) {
                	int xan = MathUtils.SINE[z];
                    int zan = MathUtils.COSINE[z];
                    int tmp = xan * this.verticesY[v] + zan * this.verticesX[v] + 16383 >> 14;
                    this.verticesY[v] = zan * this.verticesY[v] - xan * this.verticesX[v] + 16383 >> 14;
                    this.verticesX[v] = tmp;
                }

                if (x != 0) {
                	int xan = MathUtils.SINE[x];
                    int zan = MathUtils.COSINE[x];
                    int tmp = zan * this.verticesY[v] - xan * this.verticesZ[v] + 16383 >> 14;
                    this.verticesZ[v] = xan * this.verticesY[v] + zan * this.verticesZ[v] + 16383 >> 14;
                    this.verticesY[v] = tmp;
                }

                if (y != 0) {
                	int xan = MathUtils.SINE[y];
                    int zan = MathUtils.COSINE[y];
                    int tmp = xan * this.verticesZ[v] + zan * this.verticesX[v] + 16383 >> 14;
                    this.verticesZ[v] = zan * this.verticesZ[v] - xan * this.verticesX[v] + 16383 >> 14;
                    this.verticesX[v] = tmp;
                }

                this.verticesX[v] += originX;
                this.verticesY[v] += originY;
                this.verticesZ[v] += originZ;
            }
        } else if (transformationType == 3) {
            for (int v = 0; v < this.maxDepth; v++) {
                this.verticesX[v] -= originX;
                this.verticesY[v] -= originY;
                this.verticesZ[v] -= originZ;
                this.verticesX[v] = x * this.verticesX[v] / 128;
                this.verticesY[v] = y * this.verticesY[v] / 128;
                this.verticesZ[v] = z * this.verticesZ[v] / 128;
                this.verticesX[v] += originX;
                this.verticesY[v] += originY;
                this.verticesZ[v] += originZ;
            }
        } else if (transformationType == 5) {
            for (int f = 0; f < this.numFaces; f++) {
            	int alpha = (this.facesAlpha[f] & 0xff) + x * 8;
                if (alpha < 0) {
                    alpha = 0;
                } else if (alpha > 255) {
                    alpha = 255;
                }

                this.facesAlpha[f] = (byte) alpha;
            }

            if (this.colorsBuffer != null) {
                this.colorsBuffer.buffer = null;
            }

            if (this.billboardEffects != null) {
                for (int b = 0; b < this.billboardCount; b++) {
                	BillboardEffectGL effect = this.billboardEffects[b];
                    BillboardSurfaceGL surface = this.billboardSurfaces[b];
                    surface.diffuseColor = surface.diffuseColor & 0xffffff | 255 - (this.facesAlpha[effect.appliesOnFace] & 0xff) << 24;
                }
            }
        } else if (transformationType == 7) {
            for (int f = 0; f < this.numFaces; f++) {
            	int hsl = this.facesColor[f] & 0xffff;
                int h = hsl >> 10 & 0x3f;
                int s = hsl >> 7 & 0x7;
                int l = hsl & 0x7f;
                h = h + x & 0x3f;
                s += y / 4;
                if (s < 0) {
                    s = 0;
                } else if (s > 7) {
                    s = 7;
                }

                l += z;
                if (l < 0) {
                    l = 0;
                } else if (l > 127) {
                    l = 127;
                }

                this.facesColor[f] = (short) (h << 10 | s << 7 | l);
            }

            if (this.colorsBuffer != null) {
                this.colorsBuffer.buffer = null;
            }

            if (this.billboardEffects != null) {
                for (int b = 0; b < this.billboardCount; b++) {
                	BillboardEffectGL effect = this.billboardEffects[b];
                    BillboardSurfaceGL surface = this.billboardSurfaces[b];
                    surface.diffuseColor = surface.diffuseColor & -16777216 | ColorUtil.hslToRgb[this.facesColor[effect.appliesOnFace] & 0xffff] & 0xffffff;
                }
            }
        } else if (transformationType == 8) {
            for (int b = 0; b < this.billboardCount; b++) {
            	BillboardSurfaceGL surface = this.billboardSurfaces[b];
                surface.offsetX += x;
                surface.offsetY += y;
            }
        } else if (transformationType == 9) {
            for (int b = 0; b < this.billboardCount; b++) {
            	BillboardSurfaceGL surface = this.billboardSurfaces[b];
                surface.rotation = x + surface.rotation & 0x3fff;
            }
        } else if (transformationType == 10) {
            for (int b = 0; b < this.billboardCount; b++) {
            	BillboardSurfaceGL surface = this.billboardSurfaces[b];
                surface.scaleX = x * surface.scaleX >> 7;
                surface.scaleY = y * surface.scaleY >> 7;
            }
        }      
    }

    @Override
    public void animatePartialtransform(int transformationType, int[] labels, int x, int y, int z, int rotation, boolean lights) {
        int labelCount = labels.length;
        if (transformationType == 0) {
            x <<= 4;
            y <<= 4;
            z <<= 4;
            int translateCount = 0;
            originX = 0;
            originY = 0;
            originZ = 0;

            for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
                int group = labels[labelIndex];
                if (group < this.vertexGroups.length) {
                    int[] vertexGroup = this.vertexGroups[group];

                    for (int i = 0; i < vertexGroup.length; i++) {
                    	int v = vertexGroup[i];
                        originX += this.verticesX[v];
                        originY += this.verticesY[v];
                        originZ += this.verticesZ[v];
                        translateCount++;
                    }
                }
            }

            if (translateCount > 0) {
                originX = x + originX / translateCount;
                originY = y + originY / translateCount;
                originZ = z + originZ / translateCount;
            } else {
                originX = x;
                originY = y;
                originZ = z;
            }
        } else if (transformationType == 1) {
            x <<= 4;
            y <<= 4;
            z <<= 4;

            for (int i_9 = 0; i_9 < labelCount; i_9++) {
            	int group = labels[i_9];
                if (group < this.vertexGroups.length) {
                    int[] vertexGroup = this.vertexGroups[group];

                    for (int i = 0; i < vertexGroup.length; i++) {
                    	int v = vertexGroup[i];
                        this.verticesX[v] += x;
                        this.verticesY[v] += y;
                        this.verticesZ[v] += z;
                    }
                }
            }
        } else if (transformationType == 2) {
            for (int i_9 = 0; i_9 < labelCount; i_9++) {
            	int group = labels[i_9];
                if (group < this.vertexGroups.length) {
                	int[] vertexGroup = this.vertexGroups[group];
                    if ((rotation & 0x1) == 0) {
                        for (int i = 0; i < vertexGroup.length; i++) {
                        	int v = vertexGroup[i];
                            this.verticesX[v] -= originX;
                            this.verticesY[v] -= originY;
                            this.verticesZ[v] -= originZ;
                            if (z != 0) {
                            	int xan = MathUtils.SINE[z];
                            	int zan = MathUtils.COSINE[z];
                            	int tmp = xan * this.verticesY[v] + zan * this.verticesX[v] + 16383 >> 14;
                                this.verticesY[v] = zan * this.verticesY[v] - xan * this.verticesX[v] + 16383 >> 14;
                                this.verticesX[v] = tmp;
                            }

                            if (x != 0) {
                            	int xan = MathUtils.SINE[x];
                            	int zan = MathUtils.COSINE[x];
                            	int tmp = zan * this.verticesY[v] - xan * this.verticesZ[v] + 16383 >> 14;
                                this.verticesZ[v] = xan * this.verticesY[v] + zan * this.verticesZ[v] + 16383 >> 14;
                                this.verticesY[v] = tmp;
                            }

                            if (y != 0) {
                            	int xan = MathUtils.SINE[y];
                            	int zan = MathUtils.COSINE[y];
                            	int tmp = xan * this.verticesZ[v] + zan * this.verticesX[v] + 16383 >> 14;
                                this.verticesZ[v] = zan * this.verticesZ[v] - xan * this.verticesX[v] + 16383 >> 14;
                                this.verticesX[v] = tmp;
                            }

                            this.verticesX[v] += originX;
                            this.verticesY[v] += originY;
                            this.verticesZ[v] += originZ;
                        }
                    } else {
                        for (int i = 0; i < vertexGroup.length; i++) {
                        	int v = vertexGroup[i];
                            this.verticesX[v] -= originX;
                            this.verticesY[v] -= originY;
                            this.verticesZ[v] -= originZ;
                            if (x != 0) {
                            	int xan = MathUtils.SINE[x];
                            	int zan = MathUtils.COSINE[x];
                            	int tmp = zan * this.verticesY[v] - xan * this.verticesZ[v] + 16383 >> 14;
                                this.verticesZ[v] = xan * this.verticesY[v] + zan * this.verticesZ[v] + 16383 >> 14;
                                this.verticesY[v] = tmp;
                            }

                            if (z != 0) {
                            	int xan = MathUtils.SINE[z];
                            	int zan = MathUtils.COSINE[z];
                            	int tmp = xan * this.verticesY[v] + zan * this.verticesX[v] + 16383 >> 14;
                                this.verticesY[v] = zan * this.verticesY[v] - xan * this.verticesX[v] + 16383 >> 14;
                                this.verticesX[v] = tmp;
                            }

                            if (y != 0) {
                            	int xan = MathUtils.SINE[y];
                            	int zan = MathUtils.COSINE[y];
                            	int tmp = xan * this.verticesZ[v] + zan * this.verticesX[v] + 16383 >> 14;
                                this.verticesZ[v] = zan * this.verticesZ[v] - xan * this.verticesX[v] + 16383 >> 14;
                                this.verticesX[v] = tmp;
                            }

                            this.verticesX[v] += originX;
                            this.verticesY[v] += originY;
                            this.verticesZ[v] += originZ;
                        }
                    }
                }
            }

            if (lights) {
                for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
                	int group = labels[labelIndex];
                    if (group < this.vertexGroups.length) {
                    	int[] vertexGroup = this.vertexGroups[group];

                        for (int i = 0; i < vertexGroup.length; i++) {
                        	int v = vertexGroup[i];
                        	int i_14 = this.triangles[v];
                        	int i_15 = this.triangles[v + 1];

                            for (int i_16 = i_14; i_16 < i_15; i_16++) {
                            	int n = this.aShortArray8288[i_16] - 1;
                                if (n == -1) {
                                    break;
                                }

                                if (z != 0) {
                                    int xan = MathUtils.SINE[z];
                                    int zan = MathUtils.COSINE[z];
                                    int tmp = this.normalsY[n] * xan + this.normalsX[n] * zan + 16383 >> 14;
                                    this.normalsY[n] = (short) (this.normalsY[n] * zan - this.normalsX[n] * xan + 16383 >> 14);
                                    this.normalsX[n] = (short) tmp;
                                }

                                if (x != 0) {
                                	int xan = MathUtils.SINE[x];
                                	int zan = MathUtils.COSINE[x];
                                	int tmp = this.normalsY[n] * zan - this.normalsZ[n] * xan + 16383 >> 14;
                                    this.normalsZ[n] = (short) (this.normalsY[n] * xan + this.normalsZ[n] * zan + 16383 >> 14);
                                    this.normalsY[n] = (short) tmp;
                                }

                                if (y != 0) {
                                	int xan = MathUtils.SINE[y];
                                	int zan = MathUtils.COSINE[y];
                                	int tmp = this.normalsZ[n] * xan + this.normalsX[n] * zan + 16383 >> 14;
                                    this.normalsZ[n] = (short) (this.normalsZ[n] * zan - this.normalsX[n] * xan + 16383 >> 14);
                                    this.normalsX[n] = (short) tmp;
                                }
                            }
                        }
                    }
                }

                if (this.normalsBuffer == null && this.colorsBuffer != null) {
                    this.colorsBuffer.buffer = null;
                }

                if (this.normalsBuffer != null) {
                    this.normalsBuffer.buffer = null;
                }
            }
        } else if (transformationType == 3) {
            for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
            	int group = labels[labelIndex];
                if (group < this.vertexGroups.length) {
                	int[] vertexGroup = this.vertexGroups[group];

                    for (int i = 0; i < vertexGroup.length; i++) {
                    	int v = vertexGroup[i];
                        this.verticesX[v] -= originX;
                        this.verticesY[v] -= originY;
                        this.verticesZ[v] -= originZ;
                        this.verticesX[v] = x * this.verticesX[v] >> 7;
                        this.verticesY[v] = y * this.verticesY[v] >> 7;
                        this.verticesZ[v] = z * this.verticesZ[v] >> 7;
                        this.verticesX[v] += originX;
                        this.verticesY[v] += originY;
                        this.verticesZ[v] += originZ;
                    }
                }
            }
        } else if (transformationType == 5) {
            if (this.faceGroups != null) {
                for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
                	int group = labels[labelIndex];
                    if (group < this.faceGroups.length) {
                    	int[] faceGroup = this.faceGroups[group];

                        for (int i = 0; i < faceGroup.length; i++) {
                        	int face = faceGroup[i];
                        	int alpha = (this.facesAlpha[face] & 0xff) + x * 8;
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }

                            this.facesAlpha[face] = (byte) alpha;
                        }

                        if (faceGroup.length > 0 && this.colorsBuffer != null) {
                            this.colorsBuffer.buffer = null;
                        }
                    }
                }

                if (this.billboardEffects != null) {
                    for (int billboard = 0; billboard < this.billboardCount; billboard++) {
                    	BillboardEffectGL effect = this.billboardEffects[billboard];
                        BillboardSurfaceGL surface = this.billboardSurfaces[billboard];
                        surface.diffuseColor = surface.diffuseColor & 0xffffff | 255 - (this.facesAlpha[effect.appliesOnFace] & 0xff) << 24;
                    }
                }
            }
        } else if (transformationType == 7) {
            if (this.faceGroups != null) {
                for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
                	int group = labels[labelIndex];
                    if (group < this.faceGroups.length) {
                    	int[] faceGroup = this.faceGroups[group];

                        for (int i = 0; i < faceGroup.length; i++) {
                        	int face = faceGroup[i];
                        	int colorHsl = this.facesColor[face] & 0xffff;
                        	int h = colorHsl >> 10 & 0x3f;
                        	int s = colorHsl >> 7 & 0x7;
                			int l = colorHsl & 0x7f;
                            h = x + h & 0x3f;
                            s += y / 4;
                            if (s < 0) {
                                s = 0;
                            } else if (s > 7) {
                                s = 7;
                            }

                            l += z;
                            if (l < 0) {
                                l = 0;
                            } else if (l > 127) {
                                l = 127;
                            }

                            this.facesColor[face] = (short) (h << 10 | s << 7 | l);
                        }

                        if (faceGroup.length > 0 && this.colorsBuffer != null) {
                            this.colorsBuffer.buffer = null;
                        }
                    }
                }

                if (this.billboardEffects != null) {
                    for (int billboard = 0; billboard < this.billboardCount; billboard++) {
                    	BillboardEffectGL effect = this.billboardEffects[billboard];
                        BillboardSurfaceGL surface = this.billboardSurfaces[billboard];
                        surface.diffuseColor = surface.diffuseColor & -16777216 | ColorUtil.hslToRgb[this.facesColor[effect.appliesOnFace] & 0xffff] & 0xffffff;
                    }
                }
            }
        } else if (transformationType == 8) {
            if (this.billboardGroups != null) {
                for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
                	int group = labels[labelIndex];
                    if (group < this.billboardGroups.length) {
                    	int[] billboardGroup = this.billboardGroups[group];

                        for (int i = 0; i < billboardGroup.length; i++) {
                        	BillboardSurfaceGL surface = this.billboardSurfaces[billboardGroup[i]];
                            surface.offsetX += x;
                            surface.offsetY += y;
                        }
                    }
                }
            }
        } else if (transformationType == 10) {
            if (this.billboardGroups != null) {
                for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
                	int group = labels[labelIndex];
                    if (group < this.billboardGroups.length) {
                    	int[] billboardGroup = this.billboardGroups[group];

                        for (int i = 0; i < billboardGroup.length; i++) {
                        	BillboardSurfaceGL surface = this.billboardSurfaces[billboardGroup[i]];
                            surface.scaleX = x * surface.scaleX >> 7;
                            surface.scaleY = y * surface.scaleY >> 7;
                        }
                    }
                }
            }
        } else if (transformationType == 9 && this.billboardGroups != null) {
            for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
            	int group = labels[labelIndex];
                if (group < this.billboardGroups.length) {
                	int[] billboardGroup = this.billboardGroups[group];

                    for (int i = 0; i < billboardGroup.length; i++) {
                    	BillboardSurfaceGL surface = this.billboardSurfaces[billboardGroup[i]];
                        surface.rotation = x + surface.rotation & 0x3fff;
                    }
                }
            }
        }
    }

    @Override
    public void endAnimation() {
        for (int v = 0; v < this.numVertices; v++) {
            this.verticesX[v] = this.verticesX[v] + 7 >> 4;
            this.verticesY[v] = this.verticesY[v] + 7 >> 4;
            this.verticesZ[v] = this.verticesZ[v] + 7 >> 4;
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        this.boundsCalculated = false;
    }

    @Override
    public void offsetSubmesh(Matrix43 matrix44var1, int i_2, boolean bool3) {
        if (this.verticesSubmeshes != null) {
            Matrix43 matrix44var4 = matrix44var1;
            if (bool3) {
                matrix44var4 = this.toolkit.aClass2948405;
                matrix44var4.updateMatrix(matrix44var1);
            }

            float[] floats_5 = new float[3];

            for (int i_6 = 0; i_6 < this.maxDepth; i_6++) {
                if ((i_2 & this.verticesSubmeshes[i_6]) != 0) {
                    matrix44var4.calculatePerspective(this.verticesX[i_6], this.verticesY[i_6], this.verticesZ[i_6], floats_5);
                    this.verticesX[i_6] = (int) floats_5[0];
                    this.verticesY[i_6] = (int) floats_5[1];
                    this.verticesZ[i_6] = (int) floats_5[2];
                }
            }
        }

    }

    @Override
    public EffectiveVertex[] getEffectiveVertices() {
        return this.effectiveVertices;
    }

    @Override
    public EmissiveTriangle[] getEmissiveTriangles() {
        return this.emissiveTriangles;
    }

    @Override
    public void updateParticlePosition(Matrix43 projectionMatrix) {
        Matrix44 matrix442 = this.toolkit.aClass3848380;
        matrix442.fromMatrix(projectionMatrix);
        int i_3;
        if (this.emissiveTriangles != null) {
            for (i_3 = 0; i_3 < this.emissiveTriangles.length; i_3++) {
                EmissiveTriangle class874 = this.emissiveTriangles[i_3];
                EmissiveTriangle class875 = class874;
                if (class874.child != null) {
                    class875 = class874.child;
                }

                class875.verticeXA = (int) (matrix442.faceList[12] + matrix442.faceList[0] * this.verticesX[class874.faceA] + matrix442.faceList[4] * this.verticesY[class874.faceA] + matrix442.faceList[8] * this.verticesZ[class874.faceA]);
                class875.verticeYA = (int) (matrix442.faceList[13] + matrix442.faceList[1] * this.verticesX[class874.faceA] + matrix442.faceList[5] * this.verticesY[class874.faceA] + matrix442.faceList[9] * this.verticesZ[class874.faceA]);
                class875.verticeZA = (int) (matrix442.faceList[14] + matrix442.faceList[2] * this.verticesX[class874.faceA] + matrix442.faceList[6] * this.verticesY[class874.faceA] + matrix442.faceList[10] * this.verticesZ[class874.faceA]);
                class875.verticeXB = (int) (matrix442.faceList[12] + matrix442.faceList[0] * this.verticesX[class874.faceB] + matrix442.faceList[4] * this.verticesY[class874.faceB] + matrix442.faceList[8] * this.verticesZ[class874.faceB]);
                class875.verticeYB = (int) (matrix442.faceList[13] + matrix442.faceList[1] * this.verticesX[class874.faceB] + matrix442.faceList[5] * this.verticesY[class874.faceB] + matrix442.faceList[9] * this.verticesZ[class874.faceB]);
                class875.verticeZB = (int) (matrix442.faceList[14] + matrix442.faceList[2] * this.verticesX[class874.faceB] + matrix442.faceList[6] * this.verticesY[class874.faceB] + matrix442.faceList[10] * this.verticesZ[class874.faceB]);
                class875.verticeXC = (int) (matrix442.faceList[12] + matrix442.faceList[0] * this.verticesX[class874.faceC] + matrix442.faceList[4] * this.verticesY[class874.faceC] + matrix442.faceList[8] * this.verticesZ[class874.faceC]);
                class875.verticeYC = (int) (matrix442.faceList[13] + matrix442.faceList[1] * this.verticesX[class874.faceC] + matrix442.faceList[5] * this.verticesY[class874.faceC] + matrix442.faceList[9] * this.verticesZ[class874.faceC]);
                class875.verticeZC = (int) (matrix442.faceList[14] + matrix442.faceList[2] * this.verticesX[class874.faceC] + matrix442.faceList[6] * this.verticesY[class874.faceC] + matrix442.faceList[10] * this.verticesZ[class874.faceC]);
            }
        }

        if (this.effectiveVertices != null) {
            for (i_3 = 0; i_3 < this.effectiveVertices.length; i_3++) {
                EffectiveVertex class1726 = this.effectiveVertices[i_3];
                EffectiveVertex class1727 = class1726;
                if (class1726.child != null) {
                    class1727 = class1726.child;
                }

                if (class1726.matrix != null) {
                    class1726.matrix.copyFrom(matrix442);
                } else {
                    class1726.matrix = new Matrix44(matrix442);
                }

                class1727.verticeX = (int) (matrix442.faceList[12] + matrix442.faceList[0] * this.verticesX[class1726.appliesOnVertex] + matrix442.faceList[4] * this.verticesY[class1726.appliesOnVertex] + matrix442.faceList[8] * this.verticesZ[class1726.appliesOnVertex]);
                class1727.verticeY = (int) (matrix442.faceList[13] + matrix442.faceList[1] * this.verticesX[class1726.appliesOnVertex] + matrix442.faceList[5] * this.verticesY[class1726.appliesOnVertex] + matrix442.faceList[9] * this.verticesZ[class1726.appliesOnVertex]);
                class1727.verticeZ = (int) (matrix442.faceList[14] + matrix442.faceList[2] * this.verticesX[class1726.appliesOnVertex] + matrix442.faceList[6] * this.verticesY[class1726.appliesOnVertex] + matrix442.faceList[10] * this.verticesZ[class1726.appliesOnVertex]);
            }
        }

    }

    @Override
    public boolean calculateProjection(int i_1, int i_2, Matrix43 matrix44var3, boolean bool4, int i_5) {
        Matrix44 matrix446 = this.toolkit.aClass3848380;
        matrix446.fromMatrix(matrix44var3);
        matrix446.multiplyBy(this.toolkit.aClass3848394);
        boolean bool7 = false;
        int i_8 = Integer.MAX_VALUE;
        int i_9 = Integer.MIN_VALUE;
        int i_10 = Integer.MAX_VALUE;
        int i_11 = Integer.MIN_VALUE;
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        int centerX = this.maxX - this.minX >> 1;
        int centerY = this.maxY - this.minY >> 1;
        int centerZ = this.maxZ - this.minZ >> 1;
        int i_15 = centerX + this.minX;
        int i_16 = centerY + this.minY;
        int i_17 = centerZ + this.minZ;
        int i_18 = i_15 - (centerX << i_5);
        int i_19 = i_16 - (centerY << i_5);
        int i_20 = i_17 - (centerZ << i_5);
        int i_21 = i_15 + (centerX << i_5);
        int i_22 = i_16 + (centerY << i_5);
        int i_23 = i_17 + (centerZ << i_5);
        matrixX[0] = i_18;
        matrixY[0] = i_19;
        matrixZ[0] = i_20;
        matrixX[1] = i_21;
        matrixY[1] = i_19;
        matrixZ[1] = i_20;
        matrixX[2] = i_18;
        matrixY[2] = i_22;
        matrixZ[2] = i_20;
        matrixX[3] = i_21;
        matrixY[3] = i_22;
        matrixZ[3] = i_20;
        matrixX[4] = i_18;
        matrixY[4] = i_19;
        matrixZ[4] = i_23;
        matrixX[5] = i_21;
        matrixY[5] = i_19;
        matrixZ[5] = i_23;
        matrixX[6] = i_18;
        matrixY[6] = i_22;
        matrixZ[6] = i_23;
        matrixX[7] = i_21;
        matrixY[7] = i_22;
        matrixZ[7] = i_23;

        for (int i_24 = 0; i_24 < 8; i_24++) {
            int i_39 = matrixX[i_24];
            int i_26 = matrixY[i_24];
            int i_27 = matrixZ[i_24];
            float f_28 = matrix446.faceList[2] * i_39 + matrix446.faceList[6] * i_26 + matrix446.faceList[10] * i_27 + matrix446.faceList[14];
            float f_29 = matrix446.faceList[3] * i_39 + matrix446.faceList[7] * i_26 + matrix446.faceList[11] * i_27 + matrix446.faceList[15];
            if (f_28 >= -f_29) {
            	float f_30 = matrix446.faceList[0] * i_39 + matrix446.faceList[4] * i_26 + matrix446.faceList[8] * i_27 + matrix446.faceList[12];
            	float f_31 = matrix446.faceList[1] * i_39 + matrix446.faceList[5] * i_26 + matrix446.faceList[9] * i_27 + matrix446.faceList[13];
                int i_32 = (int) (this.toolkit.aFloat8315 + this.toolkit.imageProducerCenterX * f_30 / f_29);
                int i_33 = (int) (this.toolkit.aFloat8399 + this.toolkit.imageProducerCenterY * f_31 / f_29);
                if (i_32 < i_8) {
                    i_8 = i_32;
                }

                if (i_32 > i_9) {
                    i_9 = i_32;
                }

                if (i_33 < i_10) {
                    i_10 = i_33;
                }

                if (i_33 > i_11) {
                    i_11 = i_33;
                }

                bool7 = true;
            }
        }

        if (bool7 && i_1 > i_8 && i_1 < i_9 && i_2 > i_10 && i_2 < i_11) {
            if (bool4) {
                return true;
            }

            if (this.toolkit.projectedVerticesX.length < this.numNormals) {
                this.toolkit.projectedVerticesX = new int[this.numNormals];
                this.toolkit.projectedVerticesY = new int[this.numNormals];
            }

            int[] projectedVerticesX = this.toolkit.projectedVerticesX;
            int[] projectedVerticesY = this.toolkit.projectedVerticesY;

            for (int i_26 = 0; i_26 < this.maxDepth; i_26++) {
            	int i_27 = this.verticesX[i_26];
                int i_40 = this.verticesY[i_26];
                int i_41 = this.verticesZ[i_26];
                float f_30 = matrix446.faceList[2] * i_27 + matrix446.faceList[6] * i_40 + matrix446.faceList[10] * i_41 + matrix446.faceList[14];
                float f_31 = matrix446.faceList[3] * i_27 + matrix446.faceList[7] * i_40 + matrix446.faceList[11] * i_41 + matrix446.faceList[15];
                if (f_30 >= -f_31) {
                    float f_42 = matrix446.faceList[0] * i_27 + matrix446.faceList[4] * i_40 + matrix446.faceList[8] * i_41 + matrix446.faceList[12];
                    float f_43 = matrix446.faceList[1] * i_27 + matrix446.faceList[5] * i_40 + matrix446.faceList[9] * i_41 + matrix446.faceList[13];
                    int i_34 = this.triangles[i_26];
                    int i_35 = this.triangles[i_26 + 1];

                    for (int i_36 = i_34; i_36 < i_35; i_36++) {
                        int i_37 = this.aShortArray8288[i_36] - 1;
                        if (i_37 == -1) {
                            break;
                        }

                        projectedVerticesX[i_37] = (int) (this.toolkit.aFloat8315 + this.toolkit.imageProducerCenterX * f_42 / f_31);
                        projectedVerticesY[i_37] = (int) (this.toolkit.aFloat8399 + this.toolkit.imageProducerCenterY * f_43 / f_31);
                    }
                } else {
                	int i_32 = this.triangles[i_26];
                	int i_33 = this.triangles[i_26 + 1];

                    for (int i_34 = i_32; i_34 < i_33; i_34++) {
                    	int i_35 = this.aShortArray8288[i_34] - 1;
                        if (i_35 == -1) {
                            break;
                        }

                        projectedVerticesX[this.aShortArray8288[i_34] - 1] = -999999;
                    }
                }
            }

            for (int i_26 = 0; i_26 < this.numFaces; i_26++) {
                if (projectedVerticesX[this.facesA[i_26]] != -999999 && projectedVerticesX[this.facesB[i_26]] != -999999 && projectedVerticesX[this.facesC[i_26]] != -999999 && isHovering(i_1, i_2, projectedVerticesY[this.facesA[i_26]], projectedVerticesY[this.facesB[i_26]], projectedVerticesY[this.facesC[i_26]], projectedVerticesX[this.facesA[i_26]], projectedVerticesX[this.facesB[i_26]], projectedVerticesX[this.facesC[i_26]])) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isHovering(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8) {
        return (i_2 >= i_3 || i_2 >= i_4 || i_2 >= i_5) && ((i_2 <= i_3 || i_2 <= i_4 || i_2 <= i_5) && ((i_1 >= i_6 || i_1 >= i_7 || i_1 >= i_8) && (i_1 <= i_6 || i_1 <= i_7 || i_1 <= i_8)));
    }

 

    @Override
    public void drawMatrixThreaded(Matrix43 matrix44var1, BoundingBoxPolygon boundingBox, int i_3) {
        if ((i_3 & 0x2) != 0) {
            OpenGL.glPolygonMode(1032, 6913);
        }

        if (this.numNormals != 0) {
            Matrix44 matrix444 = this.toolkit.aClass3848442;
            Matrix44 matrix445 = this.toolkit.aClass3848380;
            Matrix44 matrix446 = this.toolkit.aClass3848407;
            matrix445.fromMatrix(matrix44var1);
            matrix446.copyFrom(matrix445);
            matrix446.multiplyBy(this.toolkit.aClass3848394);
            if (!this.boundsCalculated) {
                calculateBounds();
            }

            float[] floats_7 = this.toolkit.aFloatArray8396;
            matrix445.scale(0.0F, this.minY, 0.0F, floats_7);
            float f_8 = floats_7[0];
            float f_9 = floats_7[1];
            float f_10 = floats_7[2];
            matrix445.scale(0.0F, this.maxY, 0.0F, floats_7);
            float f_11 = floats_7[0];
            float f_12 = floats_7[1];
            float f_13 = floats_7[2];

            for (int i_14 = 0; i_14 < 6; i_14++) {
                float[] floats_33 = this.toolkit.aFloatArrayArray8395[i_14];
                float f_16 = floats_33[0] * f_8 + floats_33[1] * f_9 + floats_33[2] * f_10 + floats_33[3] + this.size2d;
                float f_17 = floats_33[0] * f_11 + floats_33[1] * f_12 + floats_33[2] * f_13 + floats_33[3] + this.size2d;
                if (f_16 < 0.0F && f_17 < 0.0F) {
                    return;
                }
            }

            if (boundingBox != null) {
                boolean bool34 = false;
                boolean bool15 = true;
                int i_35 = this.minX + this.maxX >> 1;
                int i_36 = this.minZ + this.maxZ >> 1;
                short s_19 = this.minY;
                float f_21 = matrix446.faceList[0] * i_35 + matrix446.faceList[4] * s_19 + matrix446.faceList[8] * i_36 + matrix446.faceList[12];
                float f_22 = matrix446.faceList[1] * i_35 + matrix446.faceList[5] * s_19 + matrix446.faceList[9] * i_36 + matrix446.faceList[13];
                float f_23 = matrix446.faceList[2] * i_35 + matrix446.faceList[6] * s_19 + matrix446.faceList[10] * i_36 + matrix446.faceList[14];
                float f_24 = matrix446.faceList[3] * i_35 + matrix446.faceList[7] * s_19 + matrix446.faceList[11] * i_36 + matrix446.faceList[15];
                if (f_23 >= -f_24) {
                    boundingBox.left = (int) (this.toolkit.aFloat8315 + this.toolkit.imageProducerCenterX * f_21 / f_24);
                    boundingBox.bottom = (int) (this.toolkit.aFloat8399 + this.toolkit.imageProducerCenterY * f_22 / f_24);
                } else {
                    bool34 = true;
                }

                s_19 = this.maxY;
                float f_25 = matrix446.faceList[0] * i_35 + matrix446.faceList[4] * s_19 + matrix446.faceList[8] * i_36 + matrix446.faceList[12];
                float f_26 = matrix446.faceList[1] * i_35 + matrix446.faceList[5] * s_19 + matrix446.faceList[9] * i_36 + matrix446.faceList[13];
                float f_27 = matrix446.faceList[2] * i_35 + matrix446.faceList[6] * s_19 + matrix446.faceList[10] * i_36 + matrix446.faceList[14];
                float f_28 = matrix446.faceList[3] * i_35 + matrix446.faceList[7] * s_19 + matrix446.faceList[11] * i_36 + matrix446.faceList[15];
                if (f_27 >= -f_28) {
                    boundingBox.right = (int) (this.toolkit.aFloat8315 + this.toolkit.imageProducerCenterX * f_25 / f_28);
                    boundingBox.top = (int) (this.toolkit.aFloat8399 + this.toolkit.imageProducerCenterY * f_26 / f_28);
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
                            boundingBox.left = (int) (this.toolkit.aFloat8315 + this.toolkit.imageProducerCenterX * f_30 / f_32);
                            boundingBox.bottom = (int) (this.toolkit.aFloat8399 + this.toolkit.imageProducerCenterY * f_31 / f_32);
                        } else if (f_27 < -f_28) {
                            f_29 = (f_27 + f_28) / (f_23 + f_24) - 1.0F;
                            f_30 = f_25 + f_29 * (f_21 - f_25);
                            f_31 = f_26 + f_29 * (f_22 - f_26);
                            f_32 = f_28 + f_29 * (f_24 - f_28);
                            boundingBox.right = (int) (this.toolkit.aFloat8315 + this.toolkit.imageProducerCenterX * f_30 / f_32);
                            boundingBox.top = (int) (this.toolkit.aFloat8399 + this.toolkit.imageProducerCenterY * f_31 / f_32);
                        }
                    }
                }

                if (bool15) {
                    if (f_23 / f_24 > f_27 / f_28) {
                        f_29 = f_21 + matrix444.faceList[0] * this.size2d + matrix444.faceList[12];
                        f_30 = f_24 + matrix444.faceList[3] * this.size2d + matrix444.faceList[15];
                        boundingBox.size = (int) (this.toolkit.aFloat8315 - boundingBox.left + this.toolkit.imageProducerCenterX * f_29 / f_30);
                    } else {
                        f_29 = f_25 + matrix444.faceList[0] * this.size2d + matrix444.faceList[12];
                        f_30 = f_28 + matrix444.faceList[3] * this.size2d + matrix444.faceList[15];
                        boundingBox.size = (int) (this.toolkit.aFloat8315 - boundingBox.right + this.toolkit.imageProducerCenterX * f_29 / f_30);
                    }

                    boundingBox.positionCalculated = true;
                }
            }

            this.toolkit.setupMatrixRendering();
            this.toolkit.multiMatrix(matrix445);
            method13534();
            this.toolkit.popMatrix();
            matrix445.multiplyBy(this.toolkit.aClass3848348);
            method13535(matrix445);
            if ((i_3 & 0x2) != 0) {
                OpenGL.glPolygonMode(1028, 6914);
            }
        }

    }

    public void method13534() {
        if (this.anInt8302 != 0) {
            if (this.renderState != 0) {
                upload(true);
            }

            upload(false);
            if (this.modelElementsBuffer != null) {
                if (this.modelElementsBuffer.openglElementsBuffer == null) {
                    method13536((this.renderState & 0x10) != 0);
                }

                if (this.modelElementsBuffer.openglElementsBuffer != null) {
                    this.toolkit.setLightingEnabled(this.normalsBuffer != null);
                    this.toolkit.toggleDrawingClientstates(this.vertexBuffer, this.normalsBuffer, this.colorsBuffer, this.textureCoordinatesBuffer);
                    int i_1 = this.anIntArray8289.length - 1;

                    for (int i_2 = 0; i_2 < i_1; i_2++) {
                        int i_3 = this.anIntArray8289[i_2];
                        int i_4 = this.anIntArray8289[i_2 + 1];
                        int materialId = this.facesMaterial[i_3] & 0xffff;
                        if (materialId == 65535) {
                            materialId = -1;
                        }

                        this.toolkit.renderTexture(materialId, this.normalsBuffer != null);
                        this.toolkit.drawElements(this.modelElementsBuffer.openglElementsBuffer, i_3 * 3, (i_4 - i_3) * 3);
                    }
                }
            }

            method13525();
        }

    }

    public void method13535(Matrix44 matrix441) {
        if (this.billboardEffects != null) {
            Matrix44 matrix442 = this.toolkit.aClass3848361;
            this.toolkit.pushMatrix();
            this.toolkit.setDepthBuffering(!this.hasAlphaTransition);
            this.toolkit.setLightingEnabled(false);
            this.toolkit.toggleDrawingClientstates(this.toolkit.aClass1438494, null, null, this.toolkit.aClass1438495);

            for (int b = 0; b < this.billboardCount; b++) {
                BillboardEffectGL effect = this.billboardEffects[b];
                BillboardSurfaceGL surface = this.billboardSurfaces[b];
                if (!effect.stationary || !this.toolkit.bloomEnabled()) {
                    float f_6 = (this.verticesX[effect.faceA] + this.verticesX[effect.faceB] + this.verticesX[effect.faceC]) * 0.3333333F;
                    float f_7 = (this.verticesY[effect.faceA] + this.verticesY[effect.faceB] + this.verticesY[effect.faceC]) * 0.3333333F;
                    float f_8 = (this.verticesZ[effect.faceA] + this.verticesZ[effect.faceB] + this.verticesZ[effect.faceC]) * 0.3333333F;
                    float f_9 = matrix441.faceList[0] * f_6 + matrix441.faceList[4] * f_7 + matrix441.faceList[8] * f_8 + matrix441.faceList[12];
                    float f_10 = matrix441.faceList[1] * f_6 + matrix441.faceList[5] * f_7 + matrix441.faceList[9] * f_8 + matrix441.faceList[13];
                    float f_11 = matrix441.faceList[2] * f_6 + matrix441.faceList[6] * f_7 + matrix441.faceList[10] * f_8 + matrix441.faceList[14];
                    float f_12 = (float) (1.0D / Math.sqrt(f_9 * f_9 + f_10 * f_10 + f_11 * f_11)) * effect.distance;
                    matrix442.method6549(surface.rotation, surface.scaleX * effect.sizeX >> 7, surface.scaleY * effect.sizeY >> 7, f_9 + surface.offsetX - f_9 * f_12, f_10 + surface.offsetY - f_10 * f_12, f_11 - f_11 * f_12);
                    this.toolkit.loadMatrix(matrix442);
                    int diffuseColor = surface.diffuseColor;
                    OpenGL.glColor4ub((byte) (diffuseColor >> 16), (byte) (diffuseColor >> 8), (byte) diffuseColor, (byte) (diffuseColor >> 24));
                    this.toolkit.renderTexture(effect.materialId);
                    this.toolkit.setBlendMode(effect.shape);
                    this.toolkit.drawArrays(4);
                }
            }

            this.toolkit.setDepthBuffering(true);
            this.toolkit.popMatrix();
        }

    }

    public void method13536(boolean bool1) {
        if (this.toolkit.textureBuffer.buffer.length < this.anInt8302 * 6) {
            this.toolkit.textureBuffer = new GlBitBuffer((this.anInt8302 + 100) * 6);
        } else {
            this.toolkit.textureBuffer.currentPosition = 0;
        }

        GlBitBuffer textureBuffer = this.toolkit.textureBuffer;
        int i_3;
        if (this.toolkit.bigEndianess) {
            for (i_3 = 0; i_3 < this.anInt8302; i_3++) {
                textureBuffer.writeShort(this.facesA[i_3]);
                textureBuffer.writeShort(this.facesB[i_3]);
                textureBuffer.writeShort(this.facesC[i_3]);
            }
        } else {
            for (i_3 = 0; i_3 < this.anInt8302; i_3++) {
                textureBuffer.writeShortLittle(this.facesA[i_3]);
                textureBuffer.writeShortLittle(this.facesB[i_3]);
                textureBuffer.writeShortLittle(this.facesC[i_3]);
            }
        }

        if (textureBuffer.currentPosition != 0) {
            if (bool1) {
                if (this.anInterface158277 == null) {
                    this.anInterface158277 = this.toolkit.createElementsArrayBuffer(textureBuffer.buffer, textureBuffer.currentPosition, true);
                } else {
                    this.anInterface158277.putElements(textureBuffer.buffer, textureBuffer.currentPosition);
                }

                this.modelElementsBuffer.openglElementsBuffer = this.anInterface158277;
            } else {
                this.modelElementsBuffer.openglElementsBuffer = this.toolkit.createElementsArrayBuffer(textureBuffer.buffer, textureBuffer.currentPosition, false);
            }

            if (!bool1) {
                this.updateRequired = true;
            }
        }

    }

    public void upload(boolean checkFlags) {
        boolean hasColors = this.colorsBuffer != null && this.colorsBuffer.buffer == null;
        boolean hasNormals = this.normalsBuffer != null && this.normalsBuffer.buffer == null;
        boolean hasVertices = this.vertexBuffer != null && this.vertexBuffer.buffer == null;
        boolean hasTextures = this.textureCoordinatesBuffer != null && this.textureCoordinatesBuffer.buffer == null;
        if (checkFlags) {
            hasColors &= (this.renderState & 0x2) != 0;
            hasNormals &= (this.renderState & 0x4) != 0;
            hasVertices &= (this.renderState & 0x1) != 0;
            hasTextures &= (this.renderState & 0x8) != 0;
        }

        byte entrySize = 0;
        byte verticesOffset = 0;
        byte colorsOffset = 0;
        byte normalsOffset = 0;
        byte texturesOffset = 0;
        if (hasVertices) {
            entrySize += 12;
        }

        if (hasColors) {
            colorsOffset = entrySize;
            entrySize += 4;
        }

        if (hasNormals) {
            normalsOffset = entrySize;
            entrySize += 12;
        }

        if (hasTextures) {
            texturesOffset = entrySize;
            entrySize += 8;
        }

        if (entrySize != 0) {
            if (this.toolkit.textureBuffer.buffer.length < entrySize * this.numNormals) {
                this.toolkit.textureBuffer = new GlBitBuffer((this.numNormals + 100) * entrySize);
            } else {
                this.toolkit.textureBuffer.currentPosition = 0;
            }

            GlBitBuffer bitBuffer = this.toolkit.textureBuffer;
            if (hasVertices) {
                int i_16;
                int i_17;
                if (this.toolkit.bigEndianess) {
                    for (int vertice = 0; vertice < this.maxDepth; vertice++) {
                        int vertexX = Stream.floatToRawIntBits(this.verticesX[vertice]);
                        int vertexY = Stream.floatToRawIntBits(this.verticesY[vertice]);
                        int vertexZ = Stream.floatToRawIntBits(this.verticesZ[vertice]);
                        i_16 = this.triangles[vertice];
                        i_17 = this.triangles[vertice + 1];

                        for (int i_18 = i_16; i_18 < i_17; i_18++) {
                        	int i_19 = this.aShortArray8288[i_18] - 1;
                            if (i_19 == -1) {
                                break;
                            }

                            bitBuffer.currentPosition = i_19 * entrySize;
                            bitBuffer.writeInt(vertexX);
                            bitBuffer.writeInt(vertexY);
                            bitBuffer.writeInt(vertexZ);
                        }
                    }
                } else {
                    for (int v = 0; v < this.maxDepth; v++) {
                    	int vertexX = Stream.floatToRawIntBits(this.verticesX[v]);
                    	int vertexY = Stream.floatToRawIntBits(this.verticesY[v]);
                    	int vertexZ = Stream.floatToRawIntBits(this.verticesZ[v]);
                        i_16 = this.triangles[v];
                        i_17 = this.triangles[v + 1];

                        for (int i_18 = i_16; i_18 < i_17; i_18++) {
                        	int i_19 = (this.aShortArray8288[i_18] & 0xffff) - 1;
                            if (i_19 == -1) {
                                break;
                            }

                            bitBuffer.currentPosition = i_19 * entrySize;
                            bitBuffer.writeIntLittle(vertexX);
                            bitBuffer.writeIntLittle(vertexY);
                            bitBuffer.writeIntLittle(vertexZ);
                        }
                    }
                }
            }

            float f_20;
            short[] shorts_34;
            short[] shorts_35;
            short[] shorts_36;
            byte[] bytes_37;
            float f_38;
            float f_39;
            if (hasColors) {
                if (this.normalsBuffer == null) {
                    if (this.aClass1668259 != null) {
                        shorts_34 = this.aClass1668259.normalsX;
                        shorts_35 = this.aClass1668259.normalsY;
                        shorts_36 = this.aClass1668259.normalsZ;
                        bytes_37 = this.aClass1668259.magnitude;
                    } else {
                        shorts_34 = this.normalsX;
                        shorts_35 = this.normalsY;
                        shorts_36 = this.normalsZ;
                        bytes_37 = this.magnitudes;
                    }

                    f_38 = this.toolkit.sunDirection[0];
                    f_39 = this.toolkit.sunDirection[1];
                    float f_40 = this.toolkit.sunDirection[2];
                    float f_41 = this.toolkit.sunBrightness;
                    f_20 = this.toolkit.aFloat8433 * 768.0F / this.contrast;
                    float f_21 = this.toolkit.aFloat8434 * 768.0F / this.contrast;

                    for (int i_22 = 0; i_22 < this.numFaces; i_22++) {
                        int i_23 = hslToRgb(this.facesColor[i_22], this.facesMaterial[i_22], this.ambient, this.facesAlpha[i_22]);
                        float f_24 = (i_23 >>> 24) * this.toolkit.sunColorRed;
                        float f_25 = (i_23 >> 16 & 0xff) * this.toolkit.sunColorGreen;
                        float f_26 = (i_23 >> 8 & 0xff) * this.toolkit.sunColorBlue;
                        short s_27 = this.facesA[i_22];
                        short s_28 = bytes_37[s_27];
                        float f_29;
                        if (s_28 == 0) {
                            f_29 = (f_38 * shorts_34[s_27] + f_39 * shorts_35[s_27] + f_40 * shorts_36[s_27]) * 0.0026041667F;
                        } else {
                            f_29 = (f_38 * shorts_34[s_27] + f_39 * shorts_35[s_27] + f_40 * shorts_36[s_27]) / (s_28 * 256);
                        }

                        float f_30 = f_41 + f_29 * (f_29 < 0.0F ? f_21 : f_20);
                        int i_31 = (int) (f_24 * f_30);
                        if (i_31 < 0) {
                            i_31 = 0;
                        } else if (i_31 > 255) {
                            i_31 = 255;
                        }

                        int i_32 = (int) (f_25 * f_30);
                        if (i_32 < 0) {
                            i_32 = 0;
                        } else if (i_32 > 255) {
                            i_32 = 255;
                        }

                        int i_33 = (int) (f_26 * f_30);
                        if (i_33 < 0) {
                            i_33 = 0;
                        } else if (i_33 > 255) {
                            i_33 = 255;
                        }

                        bitBuffer.currentPosition = colorsOffset + s_27 * entrySize;
                        bitBuffer.writeByte(i_31);
                        bitBuffer.writeByte(i_32);
                        bitBuffer.writeByte(i_33);
                        bitBuffer.writeByte(255 - (this.facesAlpha[i_22] & 0xff));
                        s_27 = this.facesB[i_22];
                        s_28 = bytes_37[s_27];
                        if (s_28 == 0) {
                            f_29 = (f_38 * shorts_34[s_27] + f_39 * shorts_35[s_27] + f_40 * shorts_36[s_27]) * 0.0026041667F;
                        } else {
                            f_29 = (f_38 * shorts_34[s_27] + f_39 * shorts_35[s_27] + f_40 * shorts_36[s_27]) / (s_28 * 256);
                        }

                        f_30 = f_41 + f_29 * (f_29 < 0.0F ? f_21 : f_20);
                        i_31 = (int) (f_24 * f_30);
                        if (i_31 < 0) {
                            i_31 = 0;
                        } else if (i_31 > 255) {
                            i_31 = 255;
                        }

                        i_32 = (int) (f_25 * f_30);
                        if (i_32 < 0) {
                            i_32 = 0;
                        } else if (i_32 > 255) {
                            i_32 = 255;
                        }

                        i_33 = (int) (f_26 * f_30);
                        if (i_33 < 0) {
                            i_33 = 0;
                        } else if (i_33 > 255) {
                            i_33 = 255;
                        }

                        bitBuffer.currentPosition = colorsOffset + s_27 * entrySize;
                        bitBuffer.writeByte(i_31);
                        bitBuffer.writeByte(i_32);
                        bitBuffer.writeByte(i_33);
                        bitBuffer.writeByte(255 - (this.facesAlpha[i_22] & 0xff));
                        s_27 = this.facesC[i_22];
                        s_28 = bytes_37[s_27];
                        if (s_28 == 0) {
                            f_29 = (f_38 * shorts_34[s_27] + f_39 * shorts_35[s_27] + f_40 * shorts_36[s_27]) * 0.0026041667F;
                        } else {
                            f_29 = (f_38 * shorts_34[s_27] + f_39 * shorts_35[s_27] + f_40 * shorts_36[s_27]) / (s_28 * 256);
                        }

                        f_30 = f_41 + f_29 * (f_29 < 0.0F ? f_21 : f_20);
                        i_31 = (int) (f_24 * f_30);
                        if (i_31 < 0) {
                            i_31 = 0;
                        } else if (i_31 > 255) {
                            i_31 = 255;
                        }

                        i_32 = (int) (f_25 * f_30);
                        if (i_32 < 0) {
                            i_32 = 0;
                        } else if (i_32 > 255) {
                            i_32 = 255;
                        }

                        i_33 = (int) (f_26 * f_30);
                        if (i_33 < 0) {
                            i_33 = 0;
                        } else if (i_33 > 255) {
                            i_33 = 255;
                        }

                        bitBuffer.currentPosition = colorsOffset + s_27 * entrySize;
                        bitBuffer.writeByte(i_31);
                        bitBuffer.writeByte(i_32);
                        bitBuffer.writeByte(i_33);
                        bitBuffer.writeByte(255 - (this.facesAlpha[i_22] & 0xff));
                    }
                } else {
                    for (int vertice = 0; vertice < this.numFaces; vertice++) {
                    	int vvvv = hslToRgb(this.facesColor[vertice], this.facesMaterial[vertice], this.ambient, this.facesAlpha[vertice]);
                        bitBuffer.currentPosition = colorsOffset + entrySize * (this.facesA[vertice] & 0xffff);
                        bitBuffer.writeInt(vvvv);
                        bitBuffer.currentPosition = colorsOffset + entrySize * (this.facesB[vertice] & 0xffff);
                        bitBuffer.writeInt(vvvv);
                        bitBuffer.currentPosition = colorsOffset + entrySize * (this.facesC[vertice] & 0xffff);
                        bitBuffer.writeInt(vvvv);
                    }
                }
            }

            if (hasNormals) {
                if (this.aClass1668259 != null) {
                    shorts_34 = this.aClass1668259.normalsX;
                    shorts_35 = this.aClass1668259.normalsY;
                    shorts_36 = this.aClass1668259.normalsZ;
                    bytes_37 = this.aClass1668259.magnitude;
                } else {
                    shorts_34 = this.normalsX;
                    shorts_35 = this.normalsY;
                    shorts_36 = this.normalsZ;
                    bytes_37 = this.magnitudes;
                }

                f_38 = 3.0F / this.contrast;
                f_39 = 3.0F / (this.contrast / 2 + this.contrast);
                bitBuffer.currentPosition = normalsOffset;
                if (this.toolkit.bigEndianess) {
                    for (int i_18 = 0; i_18 < this.numNormals; i_18++) {
                    	int i_19 = bytes_37[i_18] & 0xff;
                        if (i_19 == 0) {
                            bitBuffer.writeFloatBigEndian(shorts_34[i_18] * f_39);
                            bitBuffer.writeFloatBigEndian(shorts_35[i_18] * f_39);
                            bitBuffer.writeFloatBigEndian(shorts_36[i_18] * f_39);
                        } else {
                            f_20 = f_38 / i_19;
                            bitBuffer.writeFloatBigEndian(shorts_34[i_18] * f_20);
                            bitBuffer.writeFloatBigEndian(shorts_35[i_18] * f_20);
                            bitBuffer.writeFloatBigEndian(shorts_36[i_18] * f_20);
                        }

                        bitBuffer.currentPosition += entrySize - 12;
                    }
                } else {
                    for (int i_18 = 0; i_18 < this.numNormals; i_18++) {
                    	int i_19 = bytes_37[i_18] & 0xff;
                        if (i_19 == 0) {
                            bitBuffer.writeFloatLittleEndian(shorts_34[i_18] * f_39);
                            bitBuffer.writeFloatLittleEndian(shorts_35[i_18] * f_39);
                            bitBuffer.writeFloatLittleEndian(shorts_36[i_18] * f_39);
                        } else {
                            f_20 = f_38 / i_19;
                            bitBuffer.writeFloatLittleEndian(shorts_34[i_18] * f_20);
                            bitBuffer.writeFloatLittleEndian(shorts_35[i_18] * f_20);
                            bitBuffer.writeFloatLittleEndian(shorts_36[i_18] * f_20);
                        }

                        bitBuffer.currentPosition += entrySize - 12;
                    }
                }
            }

            if (hasTextures) {
                bitBuffer.currentPosition = texturesOffset;
                if (this.toolkit.bigEndianess) {
                    for (int vertice = 0; vertice < this.numNormals; vertice++) {
                        bitBuffer.writeFloatBigEndian(this.textureU[vertice]);
                        bitBuffer.writeFloatBigEndian(this.textureV[vertice]);
                        bitBuffer.currentPosition += entrySize - 8;
                    }
                } else {
                    for (int vertice = 0; vertice < this.numNormals; vertice++) {
                        bitBuffer.writeFloatLittleEndian(this.textureU[vertice]);
                        bitBuffer.writeFloatLittleEndian(this.textureV[vertice]);
                        bitBuffer.currentPosition += entrySize - 8;
                    }
                }
            }

            bitBuffer.currentPosition = entrySize * this.numNormals;
            ArrayBufferGL buffer;
            if (checkFlags) {
                if (this.anInterface148305 == null) {
                    this.anInterface148305 = this.toolkit.createArrayBuffer(entrySize, bitBuffer.buffer, bitBuffer.currentPosition, true);
                } else {
                    this.anInterface148305.putArray(entrySize, bitBuffer.buffer, bitBuffer.currentPosition);
                }

                buffer = this.anInterface148305;
                this.renderState = 0;
            } else {
                buffer = this.toolkit.createArrayBuffer(entrySize, bitBuffer.buffer, bitBuffer.currentPosition, false);
                this.updateRequired = true;
            }

            if (hasVertices) {
                this.vertexBuffer.buffer = buffer;
                this.vertexBuffer.offset = verticesOffset;
            }

            if (hasTextures) {
                this.textureCoordinatesBuffer.buffer = buffer;
                this.textureCoordinatesBuffer.offset = texturesOffset;
            }

            if (hasColors) {
                this.colorsBuffer.buffer = buffer;
                this.colorsBuffer.offset = colorsOffset;
            }

            if (hasNormals) {
                this.normalsBuffer.buffer = buffer;
                this.normalsBuffer.offset = normalsOffset;
            }
        }

    }


    @Override
    public boolean hasAnimatedTexture() {
        return this.textureAnimating;
    }

    @Override
    public byte[] getAlpha() {
        return this.facesAlpha;
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


    public short connectNormal(int face, long uid, int x, int y, int z, int magnitude, float u, float v) {
        int i_11 = this.triangles[face];
        int i_12 = this.triangles[face + 1];
        int i_13 = 0;

        for (int i_14 = i_11; i_14 < i_12; i_14++) {
            short s_15 = this.aShortArray8288[i_14];
            if (s_15 == 0) {
                i_13 = i_14;
                break;
            }

            if (normalsUid[i_14] == uid) {
                return (short) (s_15 - 1);
            }
        }

        this.aShortArray8288[i_13] = (short) (this.numNormals + 1);
        normalsUid[i_13] = uid;
        this.normalsX[this.numNormals] = (short) x;
        this.normalsY[this.numNormals] = (short) y;
        this.normalsZ[this.numNormals] = (short) z;
        this.magnitudes[this.numNormals] = (byte) magnitude;
        this.textureU[this.numNormals] = u;
        this.textureV[this.numNormals] = v;
        return (short) (this.numNormals++);
    }


    @Override
    public void translate(int x, int y, int z) {
        for (int v = 0; v < this.maxDepth; v++) {
            if (x != 0) {
                this.verticesX[v] += x;
            }

            if (y != 0) {
                this.verticesY[v] += y;
            }

            if (z != 0) {
                this.verticesZ[v] += z;
            }
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        this.boundsCalculated = false;
    }




    @Override
    public int getFunctionMask() {
        return this.functionMask;
    }


    @Override
    public void zaxisRotateWithoutNormals(int rotation) {
        int xan = MathUtils.SINE[rotation];
        int zan = MathUtils.COSINE[rotation];

        for (int vertice = 0; vertice < this.maxDepth; vertice++) {
            int tmpX = xan * this.verticesY[vertice] + zan * this.verticesX[vertice] >> 14;
            this.verticesY[vertice] = zan * this.verticesY[vertice] - xan * this.verticesX[vertice] >> 14;
            this.verticesX[vertice] = tmpX;
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.buffer = null;
        }

        this.boundsCalculated = false;
    }

    @Override
    public void tint(int hue, int saturation, int lightness, int opacity) {
        for (int i = 0; i < this.numFaces; i++) {
            int hsl = this.facesColor[i] & 0xffff;
            int h = hsl >> 10 & 0x3f;
            int s = hsl >> 7 & 0x7;
            int l = hsl & 0x7f;
            if (hue != -1) {
                h += (hue - h) * opacity >> 7;
            }

            if (saturation != -1) {
                s += opacity * (saturation - s) >> 7;
            }

            if (lightness != -1) {
                l += (lightness - l) * opacity >> 7;
            }

            this.facesColor[i] = (short) (h << 10 | s << 7 | l);
        }

        if (this.billboardEffects != null) {
            for (int billboard = 0; billboard < this.billboardCount; billboard++) {
                BillboardEffectGL effect = this.billboardEffects[billboard];
                BillboardSurfaceGL suraface = this.billboardSurfaces[billboard];
                suraface.diffuseColor = suraface.diffuseColor & -16777216 | ColorUtil.hslToRgb[this.facesColor[effect.appliesOnFace] & 0xffff] & 0xffffff;
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer.buffer = null;
        }
    }

    @Override
    public void recolor(short src, short dst) {
        for (int face = 0; face < this.numFaces; face++) {
            if (this.facesColor[face] == src) {
                this.facesColor[face] = dst;
            }
        }

        if (this.billboardEffects != null) {
            for (int billboard = 0; billboard < this.billboardCount; billboard++) {
                BillboardEffectGL effect = this.billboardEffects[billboard];
                BillboardSurfaceGL surface = this.billboardSurfaces[billboard];
                surface.diffuseColor = surface.diffuseColor & -16777216 | ColorUtil.hslToRgb[this.facesColor[effect.appliesOnFace] & 0xffff] & 0xffffff;
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer.buffer = null;
        }
    }




    @Override
    public void uploadToGpu() {
        if (this.numNormals > 0 && this.anInt8302 > 0) {
            upload(false);
            if ((this.renderState & 0x10) == 0 && this.modelElementsBuffer.openglElementsBuffer == null) {
                method13536(false);
            }

            method13525();
        }
    }

    @Override
    public void rematerial(short src, short dst) {
        MaterialProvider provider = this.toolkit.materialProvider;

        for (int face = 0; face < this.numFaces; face++) {
            if (this.facesMaterial[face] == src) {
                this.facesMaterial[face] = dst;
            }
        }

        byte srcAlpha = 0;
        byte srcBrightness = 0;
        if (src != -1) {
            MaterialType srcMaterial = provider.getMaterial(src & 0xffff);
            srcAlpha = srcMaterial.alpha;
            srcBrightness = srcMaterial.brightness;
        }

        byte dstAlpha = 0;
        byte dstBrightness = 0;
        if (dst != -1) {
            MaterialType dstMaterial = provider.getMaterial(dst & 0xffff);
            dstAlpha = dstMaterial.alpha;
            dstBrightness = dstMaterial.brightness;
            if (dstMaterial.textureSpeedU != 0 || dstMaterial.textureSpeedV != 0) {
                this.textureAnimating = true;
            }
        }

        if (srcAlpha != dstAlpha | srcBrightness != dstBrightness) {
            if (this.billboardEffects != null) {
                for (int billboard = 0; billboard < this.billboardCount; billboard++) {
                    BillboardEffectGL effect = this.billboardEffects[billboard];
                    BillboardSurfaceGL surface = this.billboardSurfaces[billboard];
                    surface.diffuseColor = surface.diffuseColor & -16777216 | ColorUtil.hslToRgb[this.facesColor[effect.appliesOnFace] & 0xffff] & 0xffffff;
                }
            }

            if (this.colorsBuffer != null) {
                this.colorsBuffer.buffer = null;
            }
        }
    }



    @Override
    public int getMaxZ() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.maxZ;
    }



    @Override
    public Shadow createShadowSprite(Shadow shadow) {
        if (this.numNormals == 0) {
            return null;
        } 
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        int x2;
        int x1;
        if (this.toolkit.anInt8438 > 0) {
            x2 = this.minX - (this.toolkit.anInt8438 * this.maxY >> 8) >> this.toolkit.shadowScale;
            x1 = this.maxX - (this.toolkit.anInt8438 * this.minY >> 8) >> this.toolkit.shadowScale;
        } else {
            x2 = this.minX - (this.toolkit.anInt8438 * this.minY >> 8) >> this.toolkit.shadowScale;
            x1 = this.maxX - (this.toolkit.anInt8438 * this.maxY >> 8) >> this.toolkit.shadowScale;
        }

        int z2;
        int z1;
        if (this.toolkit.anInt8439 > 0) {
            z2 = this.minZ - (this.toolkit.anInt8439 * this.maxY >> 8) >> this.toolkit.shadowScale;
            z1 = this.maxZ - (this.toolkit.anInt8439 * this.minY >> 8) >> this.toolkit.shadowScale;
        } else {
            z2 = this.minZ - (this.toolkit.anInt8439 * this.minY >> 8) >> this.toolkit.shadowScale;
            z1 = this.maxZ - (this.toolkit.anInt8439 * this.maxY >> 8) >> this.toolkit.shadowScale;
        }

        int sizeX = x1 - x2 + 1;
        int sizeZ = z1 - z2 + 1;
        ShadowGL class282Sub50Sub17Sub18 = (ShadowGL) shadow;
        ShadowGL class282Sub50Sub17Sub19;
        if (class282Sub50Sub17Sub18 != null && class282Sub50Sub17Sub18.fitsDimension(sizeX, sizeZ)) {
            class282Sub50Sub17Sub19 = class282Sub50Sub17Sub18;
            class282Sub50Sub17Sub18.reset();
        } else {
            class282Sub50Sub17Sub19 = new ShadowGL(sizeX, sizeZ);
        }

        class282Sub50Sub17Sub19.setDimension(x2, z2, x1, z1);
        method13522(class282Sub50Sub17Sub19);
        return class282Sub50Sub17Sub19;
    }

    @Override
    public int getAmbient() {
        return this.ambient;
    }

    @Override
    public int getMaxX() {
        if (!this.boundsCalculated) {
            calculateBounds();
        }

        return this.maxX;
    }
}
