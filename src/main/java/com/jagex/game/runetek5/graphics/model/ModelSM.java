package com.jagex.game.runetek5.graphics.model;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardSurface;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardEffect;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.graphics.model.normal.FaceNormal;
import com.jagex.game.runetek5.graphics.model.normal.VertexNormal;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.graphics.texture.TextureTransformationMatrix;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.toolkit.safemode.RasterizerSM;
import com.jagex.game.runetek5.toolkit.safemode.ThreadResourceSM;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.MathUtils;

public class ModelSM extends Model {

    public static int maxModelVertices = 4096;
    public static int maxModelTriangles = 4096;
    public static int renderCycle;
    public RasterizerSM rasterizer;
    public ThreadResourceSM threadResource;
    public int[] waterRenderHeight;
    public float[] projectedVerticesX;
    public float[] projectedVerticesY;
    public float[] projectedVerticesZ;
    public float[] projectedVerticesZ2;
    public int[] matrixX;
    public int[] matrixY;
    public int[] matrixZ;
    public int[] renderCycleByVertex;
    public int[] renderCycleByDepth;
    public int[] billboardOnFace;
    public int contrast;
    public int appearanceUpdateStage;
    public ThreadResourceSM currentViewport;
    public ModelSM[] copyDstModels;
    public ModelSM[] copySrcModels;
    public VertexNormal[] vertexNormals;
    public FaceNormal[] facesNormal;
    public int[] facesColourA;
    public int[] facesColourB;
    public int[] facesColourC;
    public short[] facesColor;
    public byte[] faceRenderType;
    public VertexNormal[] vertexNormalOffsets;
    public boolean boundsUpdated;
    public short maxX;
    public short maxY;
    public short maxZ;
    public short minX;
    public short minY;
    public short minZ;
    public short size2d;
    public short size3d;
    public int ambient;
    public int numVertices;
    public int maxDepth;
    public int numFaces;
    public int numBillboards;
    public boolean accountForPriorities;
    public boolean textureAnimated;
    public int[] verticesX;
    public int[] verticesY;
    public int[] verticesZ;
    public byte[] facesAlpha;
    public short[] facesMaterial;
    public BillboardSurface[] billboardSurfaces;
    public float[][] uMap;
    public float[][] vMap;
    public int[][] vertexGroups;
    public int[][] faceGroups;
    public int[][] billboardGroups;
    public short[] verticesSubmeshes;
    public short[] facesSubmeshes;
    public byte[] facesLayer;
    public short[] facesA;
    public short[] facesB;
    public short[] facesC;
    public EmissiveTriangle[] particleEmitters;
    public EffectiveVertex[] effectors;
    public BillboardEffect[] billboardEffects;
    public short[] aShortArray8623;
    public int functionMask;
    public boolean calculateColors;
    public boolean upscaled;
    public int originX;
    public int originY;
    public int originZ;
    public boolean withinOrigin;
    public boolean renderLock;
    public final SafeModeToolkit toolkit;

    public ModelSM(SafeModeToolkit hardwarerenderer1) {
        this.toolkit = hardwarerenderer1;
    }

    public ModelSM(SafeModeToolkit safeModeToolkit, Mesh mesh, int mFlags, int mAmbient, int mContrast, int preferenceMask) {
        this.toolkit = safeModeToolkit;
        this.functionMask = mFlags;
        this.ambient = mAmbient;
        this.contrast = mContrast;
        MaterialProvider materialProvider = this.toolkit.materialProvider;
        this.numVertices = mesh.numVertices;
        this.maxDepth = mesh.maxDepth;
        this.verticesX = mesh.verticesX;
        this.verticesY = mesh.verticesY;
        this.verticesZ = mesh.verticesZ;
        this.numFaces = mesh.numFaces;
        this.facesA = mesh.facesA;
        this.facesB = mesh.facesB;
        this.facesC = mesh.facesC;
        this.facesLayer = mesh.facesLayer;
        this.facesColor = mesh.facesColor;
        this.facesAlpha = mesh.facesAlpha;
        this.facesSubmeshes = mesh.facesSubmeshes;
        this.faceRenderType = mesh.faceRenderType;
        this.particleEmitters = mesh.particleEmitters;
        this.effectors = mesh.particleEffectors;
        this.verticesSubmeshes = mesh.verticesSubmeshes;
        int[] faces = new int[this.numFaces];
        for (int faceIndex = 0; faceIndex < this.numFaces; faces[faceIndex] = faceIndex++) { }
        long[] billboardUids = new long[this.numFaces];
        boolean hasPriorities = (this.functionMask & ALTERS_ALPHA) != 0;
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            int face = faces[faceIndex];
            MaterialType material1 = null;
            int billboardLayer = 0;
            byte materialEffectId = 0;
            byte materialEffectParam1 = 0;
            if (mesh.billboards != null) {
                boolean hasUid = false;
                for (int billboardIndex = 0; billboardIndex < mesh.billboards.length; billboardIndex++) {
                    Billboard billboard = mesh.billboards[billboardIndex];
                    if (face == billboard.appliesOnFace) {
                        BillboardType billboardType = BillboardTypeList.list(billboard.billboardId);
                        if (billboardType.hasUid) {
                            hasUid = true;
                        }
                        if (billboardType.materialId != -1) {
                            MaterialType material = materialProvider.getMaterial(billboardType.materialId);
                            if (material.effectCombiner == 2) {
                                this.accountForPriorities = true;
                            }
                        }
                    }
                }
                if (hasUid) {
                    billboardUids[faceIndex] = Long.MAX_VALUE;
                }
            }
            short materialId = -1;
            if (mesh.facesMaterial != null) {
                materialId = mesh.facesMaterial[face];
                if (materialId != -1) {
                    material1 = materialProvider.getMaterial(materialId & 0xffff);
                    if ((preferenceMask & DISABLE_TEXTURES) != 0 && material1.detailsOnly) {
                        materialId = -1;
                    } else {
                        materialEffectId = material1.effectId;
                        materialEffectParam1 = material1.effectParam1;
                    }
                }
            }
            boolean considerLayers = this.facesAlpha != null && this.facesAlpha[face] != 0 || material1 != null && material1.effectCombiner == 2;
            if ((hasPriorities || considerLayers) && this.facesLayer != null) {
                billboardLayer += this.facesLayer[face] << 17;
            }
            if (considerLayers) {
                billboardLayer += 65536;
            }
            billboardLayer += (materialEffectId & 0xff) << 8;
            billboardLayer += materialEffectParam1 & 0xff;
            int billboardFace = ((materialId & 0xffff) << 16);
            billboardFace += faceIndex & 0xffff;
            billboardUids[faceIndex] = ((long) billboardLayer << 32) + billboardFace;
            this.accountForPriorities |= considerLayers;
        }
        Model.addBillboard(billboardUids, faces);
        if (mesh.billboards != null) {
            this.numBillboards = mesh.billboards.length;
            this.billboardEffects = new BillboardEffect[this.numBillboards];
            this.billboardSurfaces = new BillboardSurface[this.numBillboards];
            for (int billboardIndex = 0; billboardIndex < mesh.billboards.length; billboardIndex++) {
                Billboard billboard = mesh.billboards[billboardIndex];
                BillboardType billboardType = BillboardTypeList.list(billboard.billboardId);
                int billboardFaceRgb = ColorUtil.hsvToRgbTable[mesh.facesColor[billboard.appliesOnFace] & 0xffff] & 0xffffff;
                billboardFaceRgb |= 255 - (mesh.facesAlpha != null ? mesh.facesAlpha[billboard.appliesOnFace] & 0xff : 0) << 24;
                this.billboardEffects[billboardIndex] = new BillboardEffect(billboard.appliesOnFace, billboardType.size2d, billboardType.size3d, billboardType.materialId, billboardType.blendType, billboardType.shape, billboardType.hasUid, billboard.distance);
                this.billboardSurfaces[billboardIndex] = new BillboardSurface(billboardFaceRgb);
            }
        }
        this.uMap = new float[this.numFaces][];
        this.vMap = new float[this.numFaces][];
        TextureTransformationMatrix textureMatrix = generateTextureMatrix(mesh, faces, this.numFaces);
        ThreadResourceSM renderingThread = this.toolkit.getThreadResource(Thread.currentThread());
        float[] uvTypes = renderingThread.uvTypes;
        boolean materialized = false;
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            int face = faces[faceIndex];
            byte textureOnFace;
            if (mesh.facesTexture == null) {
                textureOnFace = -1;
            } else {
                textureOnFace = mesh.facesTexture[face];
            }
            short materialId = mesh.facesMaterial == null ? -1 : mesh.facesMaterial[face];
            if (materialId != -1 && (preferenceMask & DISABLE_TEXTURES) != 0) {
                MaterialType material = materialProvider.getMaterial(materialId & 0xffff);
                if (material.detailsOnly) {
                    materialId = -1;
                }
            }
            if (materialId != -1) {
                materialized = true;
                float[] u = this.uMap[face] = new float[3];
                float[] v = this.vMap[face] = new float[3];
                if (textureOnFace == -1) {
                    u[0] = 0.0F;
                    v[0] = 1.0F;
                    u[1] = 1.0F;
                    v[1] = 1.0F;
                    u[2] = 0.0F;
                    v[2] = 0.0F;
                } else {
                    int textureIndex = textureOnFace & 0xff;
                    byte textureMapping = mesh.texturesMapping[textureIndex];
                    short b;
                    short c;
                    float f_30;
                    float f_31;
                    float f_32;
                    float f_42;
                    float f_43;
                    float f_44;
                    float f_45;
                    float f_46;
                    float f_47;
                    short a;
                    if (textureMapping == 0) {
                        a = this.facesA[face];
                        b = this.facesB[face];
                        c = this.facesC[face];
                        short texX = mesh.textureFaceX[textureIndex];
                        short texY = mesh.textureFaceY[textureIndex];
                        short texZ = mesh.textureFaceZ[textureIndex];
                        float f_28 = this.verticesX[texX];
                        float f_29 = this.verticesY[texX];
                        f_30 = this.verticesZ[texX];
                        f_31 = this.verticesX[texY] - f_28;
                        f_32 = this.verticesY[texY] - f_29;
                        float f_33 = this.verticesZ[texY] - f_30;
                        float f_34 = this.verticesX[texZ] - f_28;
                        float f_35 = this.verticesY[texZ] - f_29;
                        float f_36 = this.verticesZ[texZ] - f_30;
                        float f_37 = this.verticesX[a] - f_28;
                        float f_38 = this.verticesY[a] - f_29;
                        float f_39 = this.verticesZ[a] - f_30;
                        float f_40 = this.verticesX[b] - f_28;
                        float f_41 = this.verticesY[b] - f_29;
                        f_42 = this.verticesZ[b] - f_30;
                        f_43 = this.verticesX[c] - f_28;
                        f_44 = this.verticesY[c] - f_29;
                        f_45 = this.verticesZ[c] - f_30;
                        f_46 = f_32 * f_36 - f_33 * f_35;
                        f_47 = f_33 * f_34 - f_31 * f_36;
                        float f_48 = f_31 * f_35 - f_32 * f_34;
                        float f_49 = f_35 * f_48 - f_36 * f_47;
                        float f_50 = f_36 * f_46 - f_34 * f_48;
                        float f_51 = f_34 * f_47 - f_35 * f_46;
                        float f_52 = 1.0F / (f_49 * f_31 + f_50 * f_32 + f_51 * f_33);
                        u[0] = (f_49 * f_37 + f_50 * f_38 + f_51 * f_39) * f_52;
                        u[1] = (f_49 * f_40 + f_50 * f_41 + f_51 * f_42) * f_52;
                        u[2] = (f_49 * f_43 + f_50 * f_44 + f_51 * f_45) * f_52;
                        f_49 = f_32 * f_48 - f_33 * f_47;
                        f_50 = f_33 * f_46 - f_31 * f_48;
                        f_51 = f_31 * f_47 - f_32 * f_46;
                        f_52 = 1.0F / (f_49 * f_34 + f_50 * f_35 + f_51 * f_36);
                        v[0] = (f_49 * f_37 + f_50 * f_38 + f_51 * f_39) * f_52;
                        v[1] = (f_49 * f_40 + f_50 * f_41 + f_51 * f_42) * f_52;
                        v[2] = (f_49 * f_43 + f_50 * f_44 + f_51 * f_45) * f_52;
                    } else {
                        a = this.facesA[face];
                        b = this.facesB[face];
                        c = this.facesC[face];
                        int centerX = textureMatrix.centerX[textureIndex];
                        int centerY = textureMatrix.centerY[textureIndex];
                        int centerZ = textureMatrix.centerZ[textureIndex];
                        float[] matrix = textureMatrix.matrix[textureIndex];
                        byte direction = mesh.texturesDirection[textureIndex];
                        f_30 = mesh.texturesSpeed[textureIndex] / 256.0F;
                        if (textureMapping == 1) {
                            f_31 = mesh.texturesScaleZ[textureIndex] / 1024.0F;
                            mapCylindericalUv(this.verticesX[a], this.verticesY[a], this.verticesZ[a], centerX, centerY, centerZ, matrix, f_31, direction, f_30, uvTypes);
                            u[0] = uvTypes[0];
                            v[0] = uvTypes[1];
                            mapCylindericalUv(this.verticesX[b], this.verticesY[b], this.verticesZ[b], centerX, centerY, centerZ, matrix, f_31, direction, f_30, uvTypes);
                            u[1] = uvTypes[0];
                            v[1] = uvTypes[1];
                            mapCylindericalUv(this.verticesX[c], this.verticesY[c], this.verticesZ[c], centerX, centerY, centerZ, matrix, f_31, direction, f_30, uvTypes);
                            u[2] = uvTypes[0];
                            v[2] = uvTypes[1];
                            f_32 = f_31 / 2.0F;
                            if ((direction & 0x1) == 0) {
                                if (u[1] - u[0] > f_32) {
                                    u[1] -= f_31;
                                } else if (u[0] - u[1] > f_32) {
                                    u[1] += f_31;
                                }

                                if (u[2] - u[0] > f_32) {
                                    u[2] -= f_31;
                                } else if (u[0] - u[2] > f_32) {
                                    u[2] += f_31;
                                }
                            } else {
                                if (v[1] - v[0] > f_32) {
                                    v[1] -= f_31;
                                } else if (v[0] - v[1] > f_32) {
                                    v[1] += f_31;
                                }

                                if (v[2] - v[0] > f_32) {
                                    v[2] -= f_31;
                                } else if (v[0] - v[2] > f_32) {
                                    v[2] += f_31;
                                }
                            }
                        } else if (textureMapping == 2) {
                            f_31 = mesh.texturesUtrans[textureIndex] / 256.0F;
                            f_32 = mesh.texturesVtrans[textureIndex] / 256.0F;
                            int i_77 = this.verticesX[b] - this.verticesX[a];
                            int i_78 = this.verticesY[b] - this.verticesY[a];
                            int i_79 = this.verticesZ[b] - this.verticesZ[a];
                            int i_80 = this.verticesX[c] - this.verticesX[a];
                            int i_81 = this.verticesY[c] - this.verticesY[a];
                            int i_82 = this.verticesZ[c] - this.verticesZ[a];
                            int i_83 = i_78 * i_82 - i_81 * i_79;
                            int i_84 = i_79 * i_80 - i_82 * i_77;
                            int i_85 = i_77 * i_81 - i_80 * i_78;
                            f_42 = 64.0F / mesh.texturesScaleX[textureIndex];
                            f_43 = 64.0F / mesh.texturesScaleY[textureIndex];
                            f_44 = 64.0F / mesh.texturesScaleZ[textureIndex];
                            f_45 = (i_83 * matrix[0] + i_84 * matrix[1] + i_85 * matrix[2]) / f_42;
                            f_46 = (i_83 * matrix[3] + i_84 * matrix[4] + i_85 * matrix[5]) / f_43;
                            f_47 = (i_83 * matrix[6] + i_84 * matrix[7] + i_85 * matrix[8]) / f_44;
                            int i_86 = getCubeFace(f_45, f_46, f_47);
                            mapRectangularUv(this.verticesX[a], this.verticesY[a], this.verticesZ[a], centerX, centerY, centerZ, i_86, matrix, direction, f_30, f_31, f_32, uvTypes);
                            u[0] = uvTypes[0];
                            v[0] = uvTypes[1];
                            mapRectangularUv(this.verticesX[b], this.verticesY[b], this.verticesZ[b], centerX, centerY, centerZ, i_86, matrix, direction, f_30, f_31, f_32, uvTypes);
                            u[1] = uvTypes[0];
                            v[1] = uvTypes[1];
                            mapRectangularUv(this.verticesX[c], this.verticesY[c], this.verticesZ[c], centerX, centerY, centerZ, i_86, matrix, direction, f_30, f_31, f_32, uvTypes);
                            u[2] = uvTypes[0];
                            v[2] = uvTypes[1];
                        } else if (textureMapping == 3) {
                            mapSphericalUv(this.verticesX[a], this.verticesY[a], this.verticesZ[a], centerX, centerY, centerZ, matrix, direction, f_30, uvTypes);
                            u[0] = uvTypes[0];
                            v[0] = uvTypes[1];
                            mapSphericalUv(this.verticesX[b], this.verticesY[b], this.verticesZ[b], centerX, centerY, centerZ, matrix, direction, f_30, uvTypes);
                            u[1] = uvTypes[0];
                            v[1] = uvTypes[1];
                            mapSphericalUv(this.verticesX[c], this.verticesY[c], this.verticesZ[c], centerX, centerY, centerZ, matrix, direction, f_30, uvTypes);
                            u[2] = uvTypes[0];
                            v[2] = uvTypes[1];
                            if ((direction & 0x1) == 0) {
                                if (u[1] - u[0] > 0.5F) {
                                    --u[1];
                                } else if (u[0] - u[1] > 0.5F) {
                                    ++u[1];
                                }

                                if (u[2] - u[0] > 0.5F) {
                                    --u[2];
                                } else if (u[0] - u[2] > 0.5F) {
                                    ++u[2];
                                }
                            } else {
                                if (v[1] - v[0] > 0.5F) {
                                    --v[1];
                                } else if (v[0] - v[1] > 0.5F) {
                                    ++v[1];
                                }

                                if (v[2] - v[0] > 0.5F) {
                                    --v[2];
                                } else if (v[0] - v[2] > 0.5F) {
                                    ++v[2];
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!materialized) {
            this.vMap = null;
            this.uMap = null;
        }
        if (mesh.vertexGroups != null && (this.functionMask & ALTERS_VERTICES) != 0) {
            this.vertexGroups = mesh.createVertexGroups(true);
        }
        if (mesh.faceGroups != null && (this.functionMask & ALTERS_FACES) != 0) {
            this.faceGroups = mesh.createFaceGroups();
        }
        if (mesh.billboards != null && (this.functionMask & ALTERS_BILLBOARDS) != 0) {
            this.billboardGroups = mesh.createBillboardGroups();
        }
        if (mesh.facesMaterial != null) {
            this.facesMaterial = new short[this.numFaces];
            boolean hasMaterial = false;
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                short materialId = mesh.facesMaterial[faceIndex];
                if (materialId != -1) {
                    MaterialType material = this.toolkit.materialProvider.getMaterial(materialId);
                    if ((preferenceMask & DISABLE_TEXTURES) != 0 && material.detailsOnly) {
                        this.facesMaterial[faceIndex] = -1;
                    } else {
                        this.facesMaterial[faceIndex] = materialId;
                        hasMaterial = true;
                        if (material.effectCombiner == 2) {
                            this.accountForPriorities = true;
                        }

                        if (material.textureSpeedU != 0 || material.textureSpeedV != 0) {
                            this.textureAnimated = true;
                        }
                    }
                } else {
                    this.facesMaterial[faceIndex] = -1;
                }
            }
            if (!hasMaterial) {
                this.facesMaterial = null;
            }
        } else {
            this.facesMaterial = null;
        }
        if (this.accountForPriorities || this.billboardEffects != null) {
            this.aShortArray8623 = new short[this.numFaces];
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                this.aShortArray8623[faceIndex] = (short) faces[faceIndex];
            }
        }
    }

    @Override
    public void drawMatrixThreaded(Matrix43 matrix44var1, BoundingBoxPolygon boundingBox, int rasterizerFunctionMask) {
        multithreadedDraw(matrix44var1, boundingBox, rasterizerFunctionMask);
    }

    public void copyDependencies(ThreadResourceSM resource) {
        this.rasterizer = resource.rasterizer;
        if (resource != this.threadResource) {
            this.threadResource = resource;
            this.waterRenderHeight = this.threadResource.waterRenderHeight;
            this.projectedVerticesX = this.threadResource.projectedVerticesX;
            this.projectedVerticesY = this.threadResource.projectedVerticesY;
            this.projectedVerticesZ = this.threadResource.projectedVerticesZ;
            this.projectedVerticesZ2 = this.threadResource.projectedVerticesZ2;
            this.matrixX = this.threadResource.matrixX;
            this.matrixY = this.threadResource.matrixY;
            this.matrixZ = this.threadResource.matrixZ;
            this.renderCycleByVertex = this.threadResource.renderCycleByVertex;
            this.renderCycleByDepth = this.threadResource.renderCycleByDepth;
            this.billboardOnFace = this.threadResource.billboardIdByFace;
        }
    }

    @Override
    public void setContrast(int i_1) {
        if ((this.functionMask & ALTERS_CONTRAST) != 8192) {
            throw new IllegalStateException();
        } else {
            this.contrast = i_1;
            this.appearanceUpdateStage = 0;
        }
    }

    public void setToolkitForThread(Thread thread) {
        ThreadResourceSM viewport = this.toolkit.getThreadResource(thread);
        if (viewport != this.currentViewport) {
            this.currentViewport = viewport;
            this.copyDstModels = this.currentViewport.copyDstModels;
            this.copySrcModels = this.currentViewport.copySrcModels;
        }

    }

    public void calculateNormals() {
        this.vertexNormals = new VertexNormal[this.maxDepth];
        for (int faceIndex = 0; faceIndex < this.maxDepth; faceIndex++) {
            this.vertexNormals[faceIndex] = new VertexNormal();
        }
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            short a = this.facesA[faceIndex];
            short b = this.facesB[faceIndex];
            short c = this.facesC[faceIndex];
            int deltaXBa = this.verticesX[b] - this.verticesX[a];
            int deltaYBa = this.verticesY[b] - this.verticesY[a];
            int deltaZBa = this.verticesZ[b] - this.verticesZ[a];
            int deltaXCa = this.verticesX[c] - this.verticesX[a];
            int deltaYCa = this.verticesY[c] - this.verticesY[a];
            int deltaZCa = this.verticesZ[c] - this.verticesZ[a];
            int deltaX = deltaYBa * deltaZCa - deltaYCa * deltaZBa;
            int deltaY = deltaZBa * deltaXCa - deltaZCa * deltaXBa;
            int deltaZ;
            for (deltaZ = deltaXBa * deltaYCa - deltaXCa * deltaYBa; deltaX > 8192 || deltaY > 8192 || deltaZ > 8192 || deltaX < -8192 || deltaY < -8192 || deltaZ < -8192; deltaZ >>= 1) {
                deltaX >>= 1;
                deltaY >>= 1;
            }
            int divider = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (divider <= 0) {
                divider = 1;
            }
            deltaX = deltaX * 256 / divider;
            deltaY = deltaY * 256 / divider;
            deltaZ = deltaZ * 256 / divider;
            byte renderType;
            if (this.faceRenderType == null) {
                renderType = 0;
            } else {
                renderType = this.faceRenderType[faceIndex];
            }
            if (renderType == 0) {
                VertexNormal normal = this.vertexNormals[a];
                normal.x += deltaX;
                normal.y += deltaY;
                normal.z += deltaZ;
                normal.magnitude++;
                normal = this.vertexNormals[b];
                normal.x += deltaX;
                normal.y += deltaY;
                normal.z += deltaZ;
                normal.magnitude++;
                normal = this.vertexNormals[c];
                normal.x += deltaX;
                normal.y += deltaY;
                normal.z += deltaZ;
                normal.magnitude++;
            } else if (renderType == 1) {
                if (this.facesNormal == null) {
                    this.facesNormal = new FaceNormal[this.numFaces];
                }
                FaceNormal faceNormal = this.facesNormal[faceIndex] = new FaceNormal();
                faceNormal.x = deltaX;
                faceNormal.y = deltaY;
                faceNormal.z = deltaZ;
            }
        }
    }



    public void appearanceUpdate(boolean resetRenderType) {
        if (this.appearanceUpdateStage == 1) {
            recalculateColorsThreaded();
        } else if (this.appearanceUpdateStage == 2) {
            if ((this.functionMask & RESET_COLOR) == 0 && this.uMap == null) {
                this.facesColor = null;
            }
            if (resetRenderType) {
                this.faceRenderType = null;
            }
        } else {
            calculateNormalsThreaded();
            int lightX = this.toolkit.lightX;
            int lightY = this.toolkit.lightY;
            int lightZ = this.toolkit.lightZ;
            int light = this.toolkit.lightIntensity >> 8;
            //System.out.println("XYZ: "+light_x+","+light_y+","+light_z+","+toolkit.lowest_contrast+","+toolkit.highest_contrast+","+toolkit.light_intensity+","+contrast+","+ambient);
            int lowContrast = 0;
            int highContrast = 0;
            if (this.contrast != 0) {
                lowContrast = this.toolkit.lowestContrast * 768 / this.contrast;
                highContrast = this.toolkit.highestContrast * 768 / this.contrast;
            }
            if (this.facesColourA == null) {
                this.facesColourA = new int[this.numFaces];
                this.facesColourB = new int[this.numFaces];
                this.facesColourC = new int[this.numFaces];
            }
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                byte renderType;
                if (this.faceRenderType == null) {
                    renderType = 0;
                } else {
                    renderType = this.faceRenderType[faceIndex];
                }
                byte alpha;
                if (this.facesAlpha == null) {
                    alpha = 0;
                } else {
                    alpha = this.facesAlpha[faceIndex];
                }
                short materialId;
                if (this.facesMaterial == null) {
                    materialId = -1;
                } else {
                    materialId = this.facesMaterial[faceIndex];
                }
                if (alpha == -2) {
                    renderType = 3;
                }
                if (alpha == -1) {
                    renderType = 2;
                }
                if (materialId == -1) {
                    if (renderType != 0) {
                        if (renderType == 1) {
                        	int colorHsl = this.facesColor[faceIndex] & 0xffff;
                        	int finalAmbient = (colorHsl & 0x7f) * this.ambient >> 7;
            				short signedHsl = ColorUtil.adjustHsv(colorHsl & -128 | finalAmbient);
                            FaceNormal faceNormal = this.facesNormal[faceIndex];
                            int lightness = lightX * faceNormal.x + lightY * faceNormal.y + lightZ * faceNormal.z >> 16;
                            int finalContrast = lightness > 256 ? lowContrast : highContrast;
                            int finalLight = (light >> 1) + (finalContrast * lightness >> 17);
                            this.facesColourA[faceIndex] = finalLight << 17 | ColorUtil.repackHsl(signedHsl, finalLight);
                            this.facesColourC[faceIndex] = -1;
                        } else if (renderType == 3) {
                            this.facesColourA[faceIndex] = 128;
                            this.facesColourC[faceIndex] = -1;
                        } else {
                            this.facesColourC[faceIndex] = -2;
                        }
                    } else {
                    	int colorHsl = this.facesColor[faceIndex] & 0xffff;
                    	int finalAmbient = (colorHsl & 0x7f) * this.ambient >> 7;
                        short signedHsl = ColorUtil.adjustHsv(colorHsl & -128 | finalAmbient);
                        VertexNormal vertexNormal;
                        if (this.vertexNormalOffsets != null && this.vertexNormalOffsets[this.facesA[faceIndex]] != null) {
                            vertexNormal = this.vertexNormalOffsets[this.facesA[faceIndex]];
                        } else {
                            vertexNormal = this.vertexNormals[this.facesA[faceIndex]];
                        }
                        int lightness = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude >> 16;
                        int finalContrast = lightness > 256 ? lowContrast : highContrast;
                        int finalLight = (light >> 1) + (finalContrast * lightness >> 17);
                        this.facesColourA[faceIndex] = finalLight << 17 | ColorUtil.repackHsl(signedHsl, finalLight);
                        if (this.vertexNormalOffsets != null && this.vertexNormalOffsets[this.facesB[faceIndex]] != null) {
                            vertexNormal = this.vertexNormalOffsets[this.facesB[faceIndex]];
                        } else {
                            vertexNormal = this.vertexNormals[this.facesB[faceIndex]];
                        }
                        lightness = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude >> 16;
                        finalContrast = lightness > 256 ? lowContrast : highContrast;
                        finalLight = (light >> 1) + (finalContrast * lightness >> 17);
                        this.facesColourB[faceIndex] = finalLight << 17 | ColorUtil.repackHsl(signedHsl, finalLight);
                        if (this.vertexNormalOffsets != null && this.vertexNormalOffsets[this.facesC[faceIndex]] != null) {
                            vertexNormal = this.vertexNormalOffsets[this.facesC[faceIndex]];
                        } else {
                            vertexNormal = this.vertexNormals[this.facesC[faceIndex]];
                        }
                        lightness = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude >> 16;
                        finalContrast = lightness > 256 ? lowContrast : highContrast;
                        finalLight = (light >> 1) + (finalContrast * lightness >> 17);
                        this.facesColourC[faceIndex] = finalLight << 17 | ColorUtil.repackHsl(signedHsl, finalLight);
                    }
                } else {
                	int colorHsl = this.facesColor[faceIndex] & 0xffff;
                    if (renderType != 0) {
                        if (renderType == 1) {
                            FaceNormal faceNormal = this.facesNormal[faceIndex];
                            int i_14 = lightX * faceNormal.x + lightY * faceNormal.y + lightZ * faceNormal.z >> 16;
                        	int i_20 = i_14 > 256 ? lowContrast : highContrast;
                            int lightness = signLight((light >> 2) + (i_20 * i_14 >> 18));
                            this.facesColourA[faceIndex] = lightness << 24 | getMaterialColor(colorHsl, materialId, lightness);
                            this.facesColourC[faceIndex] = -1;
                        } else {
                            this.facesColourC[faceIndex] = -2;
                        }
                    } else {
                        VertexNormal vertexNormal;
                        if (this.vertexNormalOffsets != null && this.vertexNormalOffsets[this.facesA[faceIndex]] != null) {
                            vertexNormal = this.vertexNormalOffsets[this.facesA[faceIndex]];
                        } else {
                            vertexNormal = this.vertexNormals[this.facesA[faceIndex]];
                        }
                        int i_14 = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude >> 16;
                        int finalContrast = i_14 > 256 ? lowContrast : highContrast;
                        int lightness = signLight((light >> 2) + (finalContrast * i_14 >> 18));
                        this.facesColourA[faceIndex] = lightness << 24 | getMaterialColor(colorHsl, materialId, lightness);
                        if (this.vertexNormalOffsets != null && this.vertexNormalOffsets[this.facesB[faceIndex]] != null) {
                            vertexNormal = this.vertexNormalOffsets[this.facesB[faceIndex]];
                        } else {
                            vertexNormal = this.vertexNormals[this.facesB[faceIndex]];
                        }
                        i_14 = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude >> 16;
                        finalContrast = i_14 > 256 ? lowContrast : highContrast;
                        lightness = signLight((light >> 2) + (finalContrast * i_14 >> 18));
                        this.facesColourB[faceIndex] = lightness << 24 | getMaterialColor(colorHsl, materialId, lightness);
                        if (this.vertexNormalOffsets != null && this.vertexNormalOffsets[this.facesC[faceIndex]] != null) {
                            vertexNormal = this.vertexNormalOffsets[this.facesC[faceIndex]];
                        } else {
                            vertexNormal = this.vertexNormals[this.facesC[faceIndex]];
                        }
                        i_14 = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude >> 16;
                        finalContrast = i_14 > 256 ? lowContrast : highContrast;
                        lightness = signLight((light >> 2) + (finalContrast * i_14 >> 18));
                        this.facesColourC[faceIndex] = lightness << 24 | getMaterialColor(colorHsl, materialId, lightness);
                    }
                }
            }
            this.vertexNormals = null;
            this.vertexNormalOffsets = null;
            this.facesNormal = null;
            if ((this.functionMask & RESET_COLOR) == 0 && this.uMap == null) {
                this.facesColor = null;
            }
            if (resetRenderType) {
                this.faceRenderType = null;
            }
            this.appearanceUpdateStage = 2;
        }
    }

    public int getMaterialColor(int hsl, short materialId, int lightness) {
        int color = ColorUtil.hslToRgb[mixLightness(hsl, lightness)];
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
        return color;
    }

    @Override
    public Model copy(byte recentuseIndex, int copyFlags, boolean recalculateBeforeCopy) {
        setToolkitForThread(Thread.currentThread());
        boolean fromCache = false;
        ModelSM dstModel;
        ModelSM srcModel;
        if (recentuseIndex > 0 && recentuseIndex <= 7) {
            srcModel = this.copySrcModels[recentuseIndex - 1];
            dstModel = this.copyDstModels[recentuseIndex - 1];
            fromCache = true;
        } else {
            dstModel = srcModel = new ModelSM(this.toolkit);
        }
        return copy(dstModel, srcModel, copyFlags, fromCache, recalculateBeforeCopy);
    }

    public int signLight(int i_1) {
        int i_11 = i_1;
        if (i_11 < 2) {
            i_11 = 2;
        } else if (i_11 > 126) {
            i_11 = 126;
        }

        return i_11;
    }

    public void recalculateColorsThreaded() {
        if (this.appearanceUpdateStage == 0) {
            appearanceUpdateThreaded(false);
        } else if (this.toolkit.executingThreadCount > 1) {
            synchronized (this) {
                recalculateColors();
            }
        } else {
            recalculateColors();
        }

    }

    public void recalculateColors() {
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            short faceMaterialId = this.facesMaterial != null ? this.facesMaterial[faceIndex] : -1;
            if (faceMaterialId == -1) {
                int color = this.facesColor[faceIndex] & 0xffff;
                int colorMask = (color & 0x7f) * this.ambient >> 7;
                short adjustedColor = ColorUtil.adjustHsv(color & -128 | colorMask);
                int lightness;
                if (this.facesColourC[faceIndex] == -1) {
                    lightness = this.facesColourA[faceIndex] & -131072;
                    this.facesColourA[faceIndex] = lightness | ColorUtil.repackHsl(adjustedColor, lightness >> 17);
                } else if (this.facesColourC[faceIndex] != -2) {
                    lightness = this.facesColourA[faceIndex] & -131072;
                    this.facesColourA[faceIndex] = lightness | ColorUtil.repackHsl(adjustedColor, lightness >> 17);
                    lightness = this.facesColourB[faceIndex] & -131072;
                    this.facesColourB[faceIndex] = lightness | ColorUtil.repackHsl(adjustedColor, lightness >> 17);
                    lightness = this.facesColourC[faceIndex] & -131072;
                    this.facesColourC[faceIndex] = lightness | ColorUtil.repackHsl(adjustedColor, lightness >> 17);
                }
            }
        }
        this.appearanceUpdateStage = 2;
    }

    public boolean hasBillboardOnFace(int face) {
        return this.billboardOnFace != null && this.billboardOnFace[face] != -1;
    }


    public Model copy(ModelSM modelDst, ModelSM modelSrc, int functionMask, boolean expand, boolean recalculateBeforeCopy) {
        modelDst.boundsUpdated = this.boundsUpdated;
        if (this.boundsUpdated) {
            modelDst.maxX = this.maxX;
            modelDst.maxY = this.maxY;
            modelDst.maxZ = this.maxZ;
            modelDst.minX = this.minX;
            modelDst.minY = this.minY;
            modelDst.minZ = this.minZ;
            modelDst.size2d = this.size2d;
            modelDst.size3d = this.size3d;
        }
        modelDst.ambient = this.ambient;
        modelDst.contrast = this.contrast;
        modelDst.numVertices = this.numVertices;
        modelDst.maxDepth = this.maxDepth;
        modelDst.numFaces = this.numFaces;
        modelDst.numBillboards = this.numBillboards;
        if ((functionMask & ALTERS_ALPHA) != 0) {
            modelDst.accountForPriorities = true;
        } else {
            modelDst.accountForPriorities = this.accountForPriorities;
        }
        modelDst.textureAnimated = this.textureAnimated;
        boolean groundAdjustment = (functionMask & FULL_DISPLACEMENT) == 7 | (functionMask & 0x20) != 0;
        boolean displacementX = groundAdjustment || (functionMask & DISPLACE_X) != 0;
        boolean displacementY = groundAdjustment || (functionMask & DISPLACE_Y) != 0;
        boolean displacementZ = groundAdjustment || (functionMask & DISPLACE_Z) != 0 || (functionMask & INVERT) != 0;
        if (!displacementX && !displacementY && !displacementZ) {
            modelDst.verticesX = this.verticesX;
            modelDst.verticesY = this.verticesY;
            modelDst.verticesZ = this.verticesZ;
        } else {
            if (!displacementX) {
                modelDst.verticesX = this.verticesX;
            } else {
                if (modelSrc.verticesX != null && modelSrc.verticesX.length >= this.numVertices) {
                    modelDst.verticesX = modelSrc.verticesX;
                } else {
                    modelDst.verticesX = modelSrc.verticesX = new int[this.numVertices];
                }

                if (this.numVertices >= 0) System.arraycopy(this.verticesX, 0, modelDst.verticesX, 0, this.numVertices);
            }
            if (!displacementY) {
                modelDst.verticesY = this.verticesY;
            } else {
                if (modelSrc.verticesY != null && modelSrc.verticesY.length >= this.numVertices) {
                    modelDst.verticesY = modelSrc.verticesY;
                } else {
                    modelDst.verticesY = modelSrc.verticesY = new int[this.numVertices];
                }

                if (this.numVertices >= 0) System.arraycopy(this.verticesY, 0, modelDst.verticesY, 0, this.numVertices);
            }
            if (displacementZ) {
                if (modelSrc.verticesZ != null && modelSrc.verticesZ.length >= this.numVertices) {
                    modelDst.verticesZ = modelSrc.verticesZ;
                } else {
                    modelDst.verticesZ = modelSrc.verticesZ = new int[this.numVertices];
                }

                if (this.numVertices >= 0) System.arraycopy(this.verticesZ, 0, modelDst.verticesZ, 0, this.numVertices);
            } else {
                modelDst.verticesZ = this.verticesZ;
            }
        }
        if ((functionMask & 0x84080) != 0) {
            if (modelSrc.facesColor != null && modelSrc.facesColor.length >= this.numFaces) {
                modelDst.facesColor = modelSrc.facesColor;
            } else {
                modelDst.facesColor = modelSrc.facesColor = new short[this.numFaces];
            }
            if (this.numFaces >= 0) System.arraycopy(this.facesColor, 0, modelDst.facesColor, 0, this.numFaces);
        } else {
            modelDst.facesColor = this.facesColor;
        }
        if ((functionMask & 0x97018) != 0) {
            modelDst.appearanceUpdateStage = 0;
            modelDst.facesColourC = null;
            modelDst.facesColourB = null;
            modelDst.facesColourA = null;
        } else if ((functionMask & 0x80) != 0) {
            if (recalculateBeforeCopy) {
                appearanceUpdateThreaded(false);
            }
            if (this.facesColourA != null) {
                if (modelSrc.facesColourA != null && modelSrc.facesColourA.length >= this.numFaces) {
                    modelDst.facesColourA = modelSrc.facesColourA;
                    modelDst.facesColourB = modelSrc.facesColourB;
                    modelDst.facesColourC = modelSrc.facesColourC;
                } else {
                    modelDst.facesColourA = modelSrc.facesColourA = new int[this.numFaces];
                    modelDst.facesColourB = modelSrc.facesColourB = new int[this.numFaces];
                    modelDst.facesColourC = modelSrc.facesColourC = new int[this.numFaces];
                }
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    modelDst.facesColourA[faceIndex] = this.facesColourA[faceIndex];
                    modelDst.facesColourB[faceIndex] = this.facesColourB[faceIndex];
                    modelDst.facesColourC[faceIndex] = this.facesColourC[faceIndex];
                }
            }
            modelDst.appearanceUpdateStage = this.appearanceUpdateStage;
        } else {
            if (recalculateBeforeCopy) {
                appearanceUpdateThreaded(false);
            }
            modelDst.facesColourA = this.facesColourA;
            modelDst.facesColourB = this.facesColourB;
            modelDst.facesColourC = this.facesColourC;
            modelDst.appearanceUpdateStage = this.appearanceUpdateStage;
        }
        if ((functionMask & ALTERS_ALPHA) != 0) {
            if (modelSrc.facesAlpha != null && modelSrc.facesAlpha.length >= this.numFaces) {
                modelDst.facesAlpha = modelSrc.facesAlpha;
            } else {
                modelDst.facesAlpha = modelSrc.facesAlpha = new byte[this.numFaces];
            }
            if (this.facesAlpha != null) {
                if (this.numFaces >= 0) System.arraycopy(this.facesAlpha, 0, modelDst.facesAlpha, 0, this.numFaces);
            } else {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    modelDst.facesAlpha[faceIndex] = 0;
                }
            }
        } else {
            modelDst.facesAlpha = this.facesAlpha;
        }
        if ((functionMask & 0x8) == 0 && (functionMask & INVERT) == 0) {
            if (recalculateBeforeCopy) {
                calculateNormalsThreaded();
            }
            modelDst.vertexNormals = this.vertexNormals;
            modelDst.facesNormal = this.facesNormal;
        } else {
            if (modelSrc.vertexNormals != null && modelSrc.vertexNormals.length >= this.maxDepth) {
                modelDst.vertexNormals = modelSrc.vertexNormals;
            } else {
                modelDst.vertexNormals = modelSrc.vertexNormals = new VertexNormal[this.maxDepth];
            }
            if (this.vertexNormals != null) {
                for (int depth = 0; depth < this.maxDepth; depth++) {
                    modelDst.vertexNormals[depth] = new VertexNormal(this.vertexNormals[depth]);
                }
            } else {
                modelDst.vertexNormals = null;
            }
            if (this.facesNormal != null) {
                if (modelSrc.facesNormal != null && modelSrc.facesNormal.length >= this.numFaces) {
                    modelDst.facesNormal = modelSrc.facesNormal;
                } else {
                    modelDst.facesNormal = modelSrc.facesNormal = new FaceNormal[this.numFaces];
                }
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    modelDst.facesNormal[faceIndex] = this.facesNormal[faceIndex] != null ? new FaceNormal(this.facesNormal[faceIndex]) : null;
                }
            } else {
                modelDst.facesNormal = null;
            }
        }
        if ((functionMask & RETEXTURE) != 0) {
            if (this.facesMaterial == null) {
                modelDst.facesMaterial = null;
            } else {
                if (modelSrc.facesMaterial != null && modelSrc.facesMaterial.length >= this.numFaces) {
                    modelDst.facesMaterial = modelSrc.facesMaterial;
                } else {
                    modelDst.facesMaterial = modelSrc.facesMaterial = new short[this.numFaces];
                }

                if (this.numFaces >= 0)
                    System.arraycopy(this.facesMaterial, 0, modelDst.facesMaterial, 0, this.numFaces);
            }
        } else {
            modelDst.facesMaterial = this.facesMaterial;
        }
        if ((functionMask & 0x10000) != 0) {
            if (this.faceRenderType == null) {
                modelDst.faceRenderType = null;
            } else {
                if (modelSrc.faceRenderType != null && modelSrc.faceRenderType.length >= this.numFaces) {
                    modelDst.faceRenderType = modelSrc.faceRenderType;
                } else {
                    int expandedFaceCount = expand ? this.numFaces + 100 : this.numFaces;
                    modelDst.faceRenderType = modelSrc.faceRenderType = new byte[expandedFaceCount];
                }

                if (this.numFaces >= 0)
                    System.arraycopy(this.faceRenderType, 0, modelDst.faceRenderType, 0, this.numFaces);
            }
        } else {
            modelDst.faceRenderType = this.faceRenderType;
        }
        if ((functionMask & 0xc580) != 0) {
            if (modelSrc.billboardSurfaces != null && modelSrc.billboardSurfaces.length >= this.numBillboards) {
                modelDst.billboardSurfaces = modelSrc.billboardSurfaces;

                for (int billboardIndex = 0; billboardIndex < this.numBillboards; billboardIndex++) {
                    modelDst.billboardSurfaces[billboardIndex].copy(this.billboardSurfaces[billboardIndex]);
                }
            } else {
                modelDst.billboardSurfaces = modelSrc.billboardSurfaces = new BillboardSurface[this.numBillboards];

                for (int billboardIndex = 0; billboardIndex < this.numBillboards; billboardIndex++) {
                    modelDst.billboardSurfaces[billboardIndex] = this.billboardSurfaces[billboardIndex].copy();
                }
            }
        } else {
            modelDst.billboardSurfaces = this.billboardSurfaces;
        }
        if (this.uMap != null && (functionMask & INVERT) != 0) { // is the flag correct..?
            if (modelSrc.uMap != null && modelSrc.uMap.length >= this.numFaces) {
                modelDst.uMap = modelSrc.uMap;
            } else {
                int expandedFaceCount = expand ? this.numFaces + 100 : this.numFaces;
                modelDst.uMap = modelSrc.uMap = new float[expandedFaceCount][3];
            }
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                if (this.uMap[faceIndex] != null) {
                    modelDst.uMap[faceIndex][0] = this.uMap[faceIndex][0];
                    modelDst.uMap[faceIndex][1] = this.uMap[faceIndex][1];
                    modelDst.uMap[faceIndex][2] = this.uMap[faceIndex][2];
                }
            }
            if (modelSrc.vMap != null && modelSrc.vMap.length >= this.numFaces) {
                modelDst.vMap = modelSrc.vMap;
            } else {
                int expandedFaceCount = expand ? this.numFaces + 100 : this.numFaces;
                modelDst.vMap = modelSrc.vMap = new float[expandedFaceCount][3];
            }
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                if (this.vMap[faceIndex] != null) {
                    modelDst.vMap[faceIndex][0] = this.vMap[faceIndex][0];
                    modelDst.vMap[faceIndex][1] = this.vMap[faceIndex][1];
                    modelDst.vMap[faceIndex][2] = this.vMap[faceIndex][2];
                }
            }
        } else {
            modelDst.uMap = this.uMap;
            modelDst.vMap = this.vMap;
        }
        modelDst.vertexGroups = this.vertexGroups;
        modelDst.faceGroups = this.faceGroups;
        modelDst.billboardGroups = this.billboardGroups;
        modelDst.verticesSubmeshes = this.verticesSubmeshes;
        modelDst.facesSubmeshes = this.facesSubmeshes;
        modelDst.facesLayer = this.facesLayer;
        modelDst.facesA = this.facesA;
        modelDst.facesB = this.facesB;
        modelDst.facesC = this.facesC;
        modelDst.particleEmitters = this.particleEmitters;
        modelDst.effectors = this.effectors;
        modelDst.billboardEffects = this.billboardEffects;
        modelDst.aShortArray8623 = this.aShortArray8623;
        modelDst.functionMask = functionMask;
        return modelDst;
    }

    @Override
    public int getFunctionMask() {
        return this.functionMask;
    }

    @Override
    public void uploadToGpu() {
    	// NOOP
    }

	@Override
	public void yaxisRotateWithoutNormals(int rotation) {
		if ((this.functionMask & (DISPLACE_X | DISPLACE_Z)) != 5) {
			throw new IllegalStateException();
		} 
		if (rotation == 4096) {
			rotate90DegWithoutNormals();
		} else if (rotation == 8192) {
			rotate170DegWithoutNormals();
		} else if (rotation == 12288) {
			rotate270DegWithoutNormals();
		} else {
			int xan = MathUtils.SINE[rotation];
			int zan = MathUtils.COSINE[rotation];
			synchronized (this) {
				for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
					int tmpX = xan * this.verticesZ[verticeIndex] + zan * this.verticesX[verticeIndex] >> 14;
					this.verticesZ[verticeIndex] = zan * this.verticesZ[verticeIndex] - xan * this.verticesX[verticeIndex] >> 14;
					this.verticesX[verticeIndex] = tmpX;
				}
				invalidate();
			}
		}
	}

    @Override
	public void rotateWithNormals(int rotation) {
        if ((this.functionMask & ROTATE_WITH_NORMALS) != 13) {
            throw new IllegalStateException();
        } 
        if (this.vertexNormals == null) {
        	yaxisRotateWithoutNormals(rotation);
        	return;
        } 
        if (rotation == 4096) {
            rotate90DegWithNormals();
        } else if (rotation == 8192) {
            rotate180DegWithNormals();
        } else if (rotation == 12288) {
            rotate270DegWithNormals();
        } else {
            int xan = MathUtils.SINE[rotation];
            int zan = MathUtils.COSINE[rotation];
            synchronized (this) {
                for (int currentDepth = 0; currentDepth < this.maxDepth; currentDepth++) {
                    int tmpX = xan * this.verticesZ[currentDepth] + zan * this.verticesX[currentDepth] >> 14;
                    this.verticesZ[currentDepth] = zan * this.verticesZ[currentDepth] - xan * this.verticesX[currentDepth] >> 14;
                    this.verticesX[currentDepth] = tmpX;
                    if (this.vertexNormals[currentDepth] != null) {
                        tmpX = xan * this.vertexNormals[currentDepth].z + zan * this.vertexNormals[currentDepth].x >> 14;
                        this.vertexNormals[currentDepth].z = zan * this.vertexNormals[currentDepth].z - xan * this.vertexNormals[currentDepth].x >> 14;
                        this.vertexNormals[currentDepth].x = tmpX;
                    }
                }
                if (this.facesNormal != null) {
                    for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                        if (this.facesNormal[faceIndex] != null) {
                            int tmpX = xan * this.facesNormal[faceIndex].z + zan * this.facesNormal[faceIndex].x >> 14;
                            this.facesNormal[faceIndex].z = zan * this.facesNormal[faceIndex].z - xan * this.facesNormal[faceIndex].x >> 14;
                            this.facesNormal[faceIndex].x = tmpX;
                        }
                    }
                }
                for (int currentDepth = this.maxDepth; currentDepth < this.numVertices; currentDepth++) {
                    int tmpX = xan * this.verticesZ[currentDepth] + zan * this.verticesX[currentDepth] >> 14;
                    this.verticesZ[currentDepth] = zan * this.verticesZ[currentDepth] - xan * this.verticesX[currentDepth] >> 14;
                    this.verticesX[currentDepth] = tmpX;
                }
                this.appearanceUpdateStage = 0;
                this.boundsUpdated = false;
            }
        }       
    }

    public void rotate90DegWithoutNormals() {
        synchronized (this) {
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                int tmpX = this.verticesX[verticeIndex];
                this.verticesX[verticeIndex] = this.verticesZ[verticeIndex];
                this.verticesZ[verticeIndex] = -tmpX;
            }
            invalidate();
        }
    }

    public void rotate170DegWithoutNormals() {
        synchronized (this) {
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                this.verticesX[verticeIndex] = -this.verticesX[verticeIndex];
                this.verticesZ[verticeIndex] = -this.verticesZ[verticeIndex];
            }
            invalidate();
        }
    }

    public void rotate270DegWithoutNormals() {
        synchronized (this) {
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                int tmpX = this.verticesZ[verticeIndex];
                this.verticesZ[verticeIndex] = this.verticesX[verticeIndex];
                this.verticesX[verticeIndex] = -tmpX;
            }
            invalidate();
        }
    }

    public void rotate180DegWithNormals() {
        synchronized (this) {
            for (int currentDepth = 0; currentDepth < this.maxDepth; currentDepth++) {
                this.verticesX[currentDepth] = -this.verticesX[currentDepth];
                this.verticesZ[currentDepth] = -this.verticesZ[currentDepth];
                if (this.vertexNormals[currentDepth] != null) {
                    this.vertexNormals[currentDepth].x = -this.vertexNormals[currentDepth].x;
                    this.vertexNormals[currentDepth].z = -this.vertexNormals[currentDepth].z;
                }
            }
            if (this.facesNormal != null) {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    if (this.facesNormal[faceIndex] != null) {
                        this.facesNormal[faceIndex].x = -this.facesNormal[faceIndex].x;
                        this.facesNormal[faceIndex].z = -this.facesNormal[faceIndex].z;
                    }
                }
            }
            for (int currentDepth = this.maxDepth; currentDepth < this.numVertices; currentDepth++) {
                this.verticesX[currentDepth] = -this.verticesX[currentDepth];
                this.verticesZ[currentDepth] = -this.verticesZ[currentDepth];
            }
            this.appearanceUpdateStage = 0;
            this.boundsUpdated = false;
        }
    }

    public void rotate270DegWithNormals() {
        synchronized (this) {
            for (int currentDepth = 0; currentDepth < this.maxDepth; currentDepth++) {
                int tmpX = this.verticesZ[currentDepth];
                this.verticesZ[currentDepth] = this.verticesX[currentDepth];
                this.verticesX[currentDepth] = -tmpX;
                if (this.vertexNormals[currentDepth] != null) {
                    tmpX = this.vertexNormals[currentDepth].z;
                    this.vertexNormals[currentDepth].z = this.vertexNormals[currentDepth].x;
                    this.vertexNormals[currentDepth].x = -tmpX;
                }
            }
            if (this.facesNormal != null) {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    if (this.facesNormal[faceIndex] != null) {
                        int tmpX = this.facesNormal[faceIndex].z;
                        this.facesNormal[faceIndex].z = this.facesNormal[faceIndex].x;
                        this.facesNormal[faceIndex].x = -tmpX;
                    }
                }
            }
            for (int currentDepth = this.maxDepth; currentDepth < this.numVertices; currentDepth++) {
                int tmpX = this.verticesZ[currentDepth];
                this.verticesZ[currentDepth] = this.verticesX[currentDepth];
                this.verticesX[currentDepth] = -tmpX;
            }
            this.appearanceUpdateStage = 0;
            this.boundsUpdated = false;
        }
    }

    @Override
    public void xaxisRotateWithoutNormals(int rotation) {
        if ((this.functionMask & (DISPLACE_Y | DISPLACE_Z)) != 6) {
            throw new IllegalStateException();
        } else {
            int xan = MathUtils.SINE[rotation];
            int zan = MathUtils.COSINE[rotation];
            synchronized (this) {
                for (int i_5 = 0; i_5 < this.numVertices; i_5++) {
                    int tmpY = zan * this.verticesY[i_5] - xan * this.verticesZ[i_5] >> 14;
                    this.verticesZ[i_5] = xan * this.verticesY[i_5] + zan * this.verticesZ[i_5] >> 14;
                    this.verticesY[i_5] = tmpY;
                }
                invalidate();
            }
        }
    }

    @Override
    public void zaxisRotateWithoutNormals(int rotation) {
        if ((this.functionMask & (DISPLACE_X | DISPLACE_Y)) != 3) {
            throw new IllegalStateException();
        } else {
            int xan = MathUtils.SINE[rotation];
            int zan = MathUtils.COSINE[rotation];
            synchronized (this) {
                for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                    int tmpX = xan * this.verticesY[verticeIndex] + zan * this.verticesX[verticeIndex] >> 14;
                    this.verticesY[verticeIndex] = zan * this.verticesY[verticeIndex] - xan * this.verticesX[verticeIndex] >> 14;
                    this.verticesX[verticeIndex] = tmpX;
                }
                invalidate();
            }
        }
    }

    @Override
    public void mirror() {
        if ((this.functionMask & INVERT) != 16) {
            throw new IllegalStateException();
        } 
        synchronized (this) {
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                this.verticesZ[verticeIndex] = -this.verticesZ[verticeIndex];
            }
            if (this.vertexNormals != null) {
                for (int verticeIndex = 0; verticeIndex < this.maxDepth; verticeIndex++) {
                    if (this.vertexNormals[verticeIndex] != null) {
                        this.vertexNormals[verticeIndex].z = -this.vertexNormals[verticeIndex].z;
                    }
                }
            }
            if (this.vertexNormalOffsets != null) {
                for (int currentDepth = 0; currentDepth < this.maxDepth; currentDepth++) {
                    if (this.vertexNormalOffsets[currentDepth] != null) {
                        this.vertexNormalOffsets[currentDepth].z = -this.vertexNormalOffsets[currentDepth].z;
                    }
                }
            }
            if (this.facesNormal != null) {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    if (this.facesNormal[faceIndex] != null) {
                        this.facesNormal[faceIndex].z = -this.facesNormal[faceIndex].z;
                    }
                }
            }
            short[] tmpA = this.facesA;
            this.facesA = this.facesC;
            this.facesC = tmpA;
            if (this.uMap != null) {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    float tmpV;
                    if (this.uMap[faceIndex] != null) {
                        tmpV = this.uMap[faceIndex][0];
                        this.uMap[faceIndex][0] = this.uMap[faceIndex][2];
                        this.uMap[faceIndex][2] = tmpV;
                    }

                    if (this.vMap[faceIndex] != null) {
                        tmpV = this.vMap[faceIndex][0];
                        this.vMap[faceIndex][0] = this.vMap[faceIndex][2];
                        this.vMap[faceIndex][2] = tmpV;
                    }
                }
            }
            this.boundsUpdated = false;
            this.appearanceUpdateStage = 0;
        }        
    }

    @Override
    public void scale(int x, int y, int z) {
        if (x != 128 && (this.functionMask & DISPLACE_X) != 1) {
            throw new IllegalStateException();
        } 
        if (y != 128 && (this.functionMask & DISPLACE_Y) != 2) {
            throw new IllegalStateException();
        } 
        if (z != 128 && (this.functionMask & DISPLACE_Z) != 4) {
            throw new IllegalStateException();
        } 
        synchronized (this) {
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                this.verticesX[verticeIndex] = x * this.verticesX[verticeIndex] >> 7;
                this.verticesY[verticeIndex] = y * this.verticesY[verticeIndex] >> 7;
                this.verticesZ[verticeIndex] = z * this.verticesZ[verticeIndex] >> 7;
            }
            this.boundsUpdated = false;
        }
    }

    public void invalidate() {
        this.vertexNormals = null;
        this.vertexNormalOffsets = null;
        this.facesNormal = null;
        this.boundsUpdated = false;
    }

    @Override
    public void updateParticlePosition(Matrix43 projectionMatrix) {
        copyDependencies(this.toolkit.getThreadResource(Thread.currentThread()));
        Matrix44 particleMatrix = this.threadResource.particleMatrix;
        particleMatrix.fromMatrix(projectionMatrix);
        if (this.particleEmitters != null) {
            for (int emitterIndex = 0; emitterIndex < this.particleEmitters.length; emitterIndex++) {
                EmissiveTriangle emitter = this.particleEmitters[emitterIndex];
                EmissiveTriangle childEmitter = emitter;
                if (emitter.child != null) {
                    childEmitter = emitter.child;
                }
                childEmitter.verticeXA = (int) (particleMatrix.faceList[12] + particleMatrix.faceList[0] * this.verticesX[emitter.faceA] + particleMatrix.faceList[4] * this.verticesY[emitter.faceA] + particleMatrix.faceList[8] * this.verticesZ[emitter.faceA]);
                childEmitter.verticeYA = (int) (particleMatrix.faceList[13] + particleMatrix.faceList[1] * this.verticesX[emitter.faceA] + particleMatrix.faceList[5] * this.verticesY[emitter.faceA] + particleMatrix.faceList[9] * this.verticesZ[emitter.faceA]);
                childEmitter.verticeZA = (int) (particleMatrix.faceList[14] + particleMatrix.faceList[2] * this.verticesX[emitter.faceA] + particleMatrix.faceList[6] * this.verticesY[emitter.faceA] + particleMatrix.faceList[10] * this.verticesZ[emitter.faceA]);
                childEmitter.verticeXB = (int) (particleMatrix.faceList[12] + particleMatrix.faceList[0] * this.verticesX[emitter.faceB] + particleMatrix.faceList[4] * this.verticesY[emitter.faceB] + particleMatrix.faceList[8] * this.verticesZ[emitter.faceB]);
                childEmitter.verticeYB = (int) (particleMatrix.faceList[13] + particleMatrix.faceList[1] * this.verticesX[emitter.faceB] + particleMatrix.faceList[5] * this.verticesY[emitter.faceB] + particleMatrix.faceList[9] * this.verticesZ[emitter.faceB]);
                childEmitter.verticeZB = (int) (particleMatrix.faceList[14] + particleMatrix.faceList[2] * this.verticesX[emitter.faceB] + particleMatrix.faceList[6] * this.verticesY[emitter.faceB] + particleMatrix.faceList[10] * this.verticesZ[emitter.faceB]);
                childEmitter.verticeXC = (int) (particleMatrix.faceList[12] + particleMatrix.faceList[0] * this.verticesX[emitter.faceC] + particleMatrix.faceList[4] * this.verticesY[emitter.faceC] + particleMatrix.faceList[8] * this.verticesZ[emitter.faceC]);
                childEmitter.verticeYC = (int) (particleMatrix.faceList[13] + particleMatrix.faceList[1] * this.verticesX[emitter.faceC] + particleMatrix.faceList[5] * this.verticesY[emitter.faceC] + particleMatrix.faceList[9] * this.verticesZ[emitter.faceC]);
                childEmitter.verticeZC = (int) (particleMatrix.faceList[14] + particleMatrix.faceList[2] * this.verticesX[emitter.faceC] + particleMatrix.faceList[6] * this.verticesY[emitter.faceC] + particleMatrix.faceList[10] * this.verticesZ[emitter.faceC]);
            }
        }
        if (this.effectors != null) {
            for (int emitterIndex = 0; emitterIndex < this.effectors.length; emitterIndex++) {
                EffectiveVertex effectiveVertex = this.effectors[emitterIndex];
                EffectiveVertex effectiveVertexChild = effectiveVertex;
                if (effectiveVertex.child != null) {
                    effectiveVertexChild = effectiveVertex.child;
                }
                if (effectiveVertex.matrix != null) {
                    effectiveVertex.matrix.copyFrom(particleMatrix);
                } else {
                    effectiveVertex.matrix = new Matrix44(particleMatrix);
                }
                effectiveVertexChild.verticeX = (int) (particleMatrix.faceList[12] + particleMatrix.faceList[0] * this.verticesX[effectiveVertex.appliesOnVertex] + particleMatrix.faceList[4] * this.verticesY[effectiveVertex.appliesOnVertex] + particleMatrix.faceList[8] * this.verticesZ[effectiveVertex.appliesOnVertex]);
                effectiveVertexChild.verticeY = (int) (particleMatrix.faceList[13] + particleMatrix.faceList[1] * this.verticesX[effectiveVertex.appliesOnVertex] + particleMatrix.faceList[5] * this.verticesY[effectiveVertex.appliesOnVertex] + particleMatrix.faceList[9] * this.verticesZ[effectiveVertex.appliesOnVertex]);
                effectiveVertexChild.verticeZ = (int) (particleMatrix.faceList[14] + particleMatrix.faceList[2] * this.verticesX[effectiveVertex.appliesOnVertex] + particleMatrix.faceList[6] * this.verticesY[effectiveVertex.appliesOnVertex] + particleMatrix.faceList[10] * this.verticesZ[effectiveVertex.appliesOnVertex]);
            }
        }
    }

    @Override
    public void unlock() {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized (this) {
                super.isLocked = false;
                notifyAll();
            }
        }
    }

    @Override
    public void endAnimation() {
        if (this.upscaled) {
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                this.verticesX[verticeIndex] = this.verticesX[verticeIndex] + 7 >> 4;
                this.verticesY[verticeIndex] = this.verticesY[verticeIndex] + 7 >> 4;
                this.verticesZ[verticeIndex] = this.verticesZ[verticeIndex] + 7 >> 4;
            }
            this.upscaled = false;
        }
        if (this.calculateColors) {
            recalculateColorsThreaded();
            this.calculateColors = false;
        }
        this.boundsUpdated = false;
    }

	@Override
	public void animatePartialtransform(int transformationType, int[] labels, int x, int y, int z, int rotation, boolean lights) {
		int labelCount = labels.length;
		if (transformationType == 0) {
			x <<= 4;
			y <<= 4;
			z <<= 4;
			if (!this.upscaled) {
				for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
					this.verticesX[verticeIndex] <<= 4;
					this.verticesY[verticeIndex] <<= 4;
					this.verticesZ[verticeIndex] <<= 4;
				}
				this.upscaled = true;
			}
			int transformationCount = 0;
			this.originX = 0;
			this.originY = 0;
			this.originZ = 0;
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int group = labels[labelIndex];
				if (group < this.vertexGroups.length) {
					int[] verticesIndices = this.vertexGroups[group];
					for (int index = 0; index < verticesIndices.length; index++) {
						int verticeIndex = verticesIndices[index];
						this.originX += this.verticesX[verticeIndex];
						this.originY += this.verticesY[verticeIndex];
						this.originZ += this.verticesZ[verticeIndex];
						transformationCount++;
					}
				}
			}
			if (transformationCount > 0) {
				this.originX = x + this.originX / transformationCount;
				this.originY = this.originY / transformationCount + y;
				this.originZ = z + this.originZ / transformationCount;
			} else {
				this.originX = x;
				this.originY = y;
				this.originZ = z;
			}
		} else if (transformationType == 1) {
			x <<= 4;
			y <<= 4;
			z <<= 4;
			if (!this.upscaled) {
				for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
					this.verticesX[verticeIndex] <<= 4;
					this.verticesY[verticeIndex] <<= 4;
					this.verticesZ[verticeIndex] <<= 4;
				}
				this.upscaled = true;
			}
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int group = labels[labelIndex];
				if (group < this.vertexGroups.length) {
					int[] verticeIndices = this.vertexGroups[group];
					for (int index = 0; index < verticeIndices.length; index++) {
						int vertex = verticeIndices[index];
						this.verticesX[vertex] += x;
						this.verticesY[vertex] += y;
						this.verticesZ[vertex] += z;
					}
				}
			}
		} else if (transformationType == 2) {
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int group = labels[labelIndex];
				if (group < this.vertexGroups.length) {
					int[] verticeIndices = this.vertexGroups[group];
					if ((rotation & 0x1) == 0) {
						for (int index = 0; index < verticeIndices.length; index++) {
							int vertice = verticeIndices[index];
							this.verticesX[vertice] -= this.originX;
							this.verticesY[vertice] -= this.originY;
							this.verticesZ[vertice] -= this.originZ;
							if (z != 0) {
								int xan = MathUtils.SINE[z];
								int zan = MathUtils.COSINE[z];
								int tmpY = xan * this.verticesY[vertice] + zan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesY[vertice] = zan * this.verticesY[vertice] - xan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesX[vertice] = tmpY;
							}
							if (x != 0) {
								int xan = MathUtils.SINE[x];
								int zan = MathUtils.COSINE[x];
								int tmpZ = zan * this.verticesY[vertice] - xan * this.verticesZ[vertice] + 16383 >> 14;
								this.verticesZ[vertice] = xan * this.verticesY[vertice] + zan * this.verticesZ[vertice] + 16383 >> 14;
								this.verticesY[vertice] = tmpZ;
							}
							if (y != 0) {
								int xan = MathUtils.SINE[y];
								int zan = MathUtils.COSINE[y];
								int tmpZ = xan * this.verticesZ[vertice] + zan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesZ[vertice] = zan * this.verticesZ[vertice] - xan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesX[vertice] = tmpZ;
							}
							this.verticesX[vertice] += this.originX;
							this.verticesY[vertice] += this.originY;
							this.verticesZ[vertice] += this.originZ;
						}
					} else {
						for (int index = 0; index < verticeIndices.length; index++) {
							int vertice = verticeIndices[index];
							this.verticesX[vertice] -= this.originX;
							this.verticesY[vertice] -= this.originY;
							this.verticesZ[vertice] -= this.originZ;
							if (x != 0) {
								int xan = MathUtils.SINE[x];
								int zan = MathUtils.COSINE[x];
								int tmpZ = zan * this.verticesY[vertice] - xan * this.verticesZ[vertice] + 16383 >> 14;
								this.verticesZ[vertice] = xan * this.verticesY[vertice] + zan * this.verticesZ[vertice] + 16383 >> 14;
								this.verticesY[vertice] = tmpZ;
							}
							if (z != 0) {
								int xan = MathUtils.SINE[z];
								int zan = MathUtils.COSINE[z];
								int tmpY = xan * this.verticesY[vertice] + zan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesY[vertice] = zan * this.verticesY[vertice] - xan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesX[vertice] = tmpY;
							}
							if (y != 0) {
								int xan = MathUtils.SINE[y];
								int zan = MathUtils.COSINE[y];
								int tmpZ = xan * this.verticesZ[vertice] + zan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesZ[vertice] = zan * this.verticesZ[vertice] - xan * this.verticesX[vertice] + 16383 >> 14;
								this.verticesX[vertice] = tmpZ;
							}
							this.verticesX[vertice] += this.originX;
							this.verticesY[vertice] += this.originY;
							this.verticesZ[vertice] += this.originZ;
						}
					}
				}
			}
		} else if (transformationType == 3) {
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int group = labels[labelIndex];
				if (group < this.vertexGroups.length) {
					int[] verticeIndices = this.vertexGroups[group];
					for (int index = 0; index < verticeIndices.length; index++) {
						int vertex = verticeIndices[index];
						this.verticesX[vertex] -= this.originX;
						this.verticesY[vertex] -= this.originY;
						this.verticesZ[vertex] -= this.originZ;
						this.verticesX[vertex] = x * this.verticesX[vertex] / 128;
						this.verticesY[vertex] = y * this.verticesY[vertex] / 128;
						this.verticesZ[vertex] = z * this.verticesZ[vertex] / 128;
						this.verticesX[vertex] += this.originX;
						this.verticesY[vertex] += this.originY;
						this.verticesZ[vertex] += this.originZ;
					}
				}
			}
		} else if (transformationType == 5) {
			if (this.faceGroups != null && this.facesAlpha != null) {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int group = labels[labelIndex];
					if (group < this.faceGroups.length) {
						int[] faceIndices = this.faceGroups[group];
						for (int index = 0; index < faceIndices.length; index++) {
							int face = faceIndices[index];
							int alpha = (this.facesAlpha[face] & 0xff) + x * 8;
							if (alpha < 0) {
								alpha = 0;
							} else if (alpha > 255) {
								alpha = 255;
							}
							this.facesAlpha[face] = (byte) alpha;
						}
					}
				}
				if (this.billboardEffects != null) {
					for (int billboardIndex = 0; billboardIndex < this.numBillboards; billboardIndex++) {
						BillboardEffect effect = this.billboardEffects[billboardIndex];
						BillboardSurface surface = this.billboardSurfaces[billboardIndex];
						surface.diffuseColor = surface.diffuseColor & 0xffffff | 255 - (this.facesAlpha[effect.appliesOnFace] & 0xff) << 24;
					}
				}
			}
		} else if (transformationType == 7) {
			if (this.faceGroups != null) {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.faceGroups.length) {
						int[] faceIndices = this.faceGroups[label];
						for (int index = 0; index < faceIndices.length; index++) {
							int face = faceIndices[index];
							int faceColorHsl = this.facesColor[face] & 0xffff;
							int h = faceColorHsl >> 10 & 0x3f;
							int s = faceColorHsl >> 7 & 0x7;
							int l = faceColorHsl & 0x7f;
							h = x + h & 0x3f;
							s += y;
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
						this.calculateColors = true;
					}
				}
				if (this.billboardEffects != null) {
					for (int billboardIndex = 0; billboardIndex < this.numBillboards; billboardIndex++) {
						BillboardEffect effect = this.billboardEffects[billboardIndex];
						BillboardSurface surface = this.billboardSurfaces[billboardIndex];
						surface.diffuseColor = surface.diffuseColor & ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(this.facesColor[effect.appliesOnFace] & 0xffff) & 0xffff] & 0xffffff;
					}
				}
			}
		} else if (transformationType == 8) {
			if (this.billboardGroups != null) {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.billboardGroups.length) {
						int[] billboardIndices = this.billboardGroups[label];
						for (int index = 0; index < billboardIndices.length; index++) {
							BillboardSurface surface = this.billboardSurfaces[billboardIndices[index]];
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
						int[] billboardIndices = this.billboardGroups[group];
						for (int index = 0; index < billboardIndices.length; index++) {
							BillboardSurface surface = this.billboardSurfaces[billboardIndices[index]];
							surface.scaleX = surface.scaleX * x / 128.0F;
							surface.scaleY = surface.scaleY * y / 128.0F;
						}
					}
				}
			}
		} else if (transformationType == 9 && this.billboardGroups != null) {
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int group = labels[labelIndex];
				if (group < this.billboardGroups.length) {
					int[] billboardIndices = this.billboardGroups[group];
					for (int index = 0; index < billboardIndices.length; index++) {
						BillboardSurface surface = this.billboardSurfaces[billboardIndices[index]];
						surface.rotation = x + surface.rotation & 0x3fff;
					}
				}
			}
		}
	}

	/**
	 * labels = transformed groups
	 */
	@Override
	public void animateTransform(int transformationType, int[] labels, int rotationX, int rotationY, int rotationZ, boolean lights, int submesh, int[] verticesData) {
		int labelCount = labels.length;
		if (transformationType == 0) {
			rotationX <<= 4;
			rotationY <<= 4;
			rotationZ <<= 4;
			if (!this.upscaled) {
				for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
					this.verticesX[verticeIndex] <<= 4;
					this.verticesY[verticeIndex] <<= 4;
					this.verticesZ[verticeIndex] <<= 4;
				}
				this.upscaled = true;
			}
			int transformationCount = 0;
			this.originX = 0;
			this.originY = 0;
			this.originZ = 0;
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int label = labels[labelIndex];
				if (label < this.vertexGroups.length) {
					int[] vertices = this.vertexGroups[label];
					for (int verticeIndex = 0; verticeIndex < vertices.length; verticeIndex++) {
						int vertice = vertices[verticeIndex];
						if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[vertice]) != 0) {
							this.originX += this.verticesX[vertice];
							this.originY += this.verticesY[vertice];
							this.originZ += this.verticesZ[vertice];
							transformationCount++;
						}
					}
				}
			}
			if (transformationCount > 0) {
				this.originX = rotationX + this.originX / transformationCount;
				this.originY = this.originY / transformationCount + rotationY;
				this.originZ = rotationZ + this.originZ / transformationCount;
				this.withinOrigin = true;
			} else {
				this.originX = rotationX;
				this.originY = rotationY;
				this.originZ = rotationZ;
			}
		} else if (transformationType == 1) {
			if (verticesData != null) {
				int transformX = rotationZ * verticesData[2] + verticesData[1] * rotationY + rotationX * verticesData[0] + 8192 >> 14;
				int transformY = rotationX * verticesData[3] + verticesData[5] * rotationZ + rotationY * verticesData[4] + 8192 >> 14;
				int transformZ = rotationX * verticesData[6] + verticesData[7] * rotationY + rotationZ * verticesData[8] + 8192 >> 14;
				rotationX = transformX;
				rotationY = transformY;
				rotationZ = transformZ;
			}
			rotationX <<= 4;
			rotationY <<= 4;
			rotationZ <<= 4;
			if (!this.upscaled) {
				for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
					this.verticesX[verticeIndex] <<= 4;
					this.verticesY[verticeIndex] <<= 4;
					this.verticesZ[verticeIndex] <<= 4;
				}
				this.upscaled = true;
			}
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int groups = labels[labelIndex];
				if (groups < this.vertexGroups.length) {
					int[] vertices = this.vertexGroups[groups];
					for (int vertIndex = 0; vertIndex < vertices.length; vertIndex++) {
						int vertice = vertices[vertIndex];
						if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[vertice]) != 0) {
							this.verticesX[vertice] += rotationX;
							this.verticesY[vertice] += rotationY;
							this.verticesZ[vertice] += rotationZ;
						}
					}
				}
			}
		} else if (transformationType == 2) {
			if (verticesData != null) {
				if (!this.upscaled) {
					for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
						this.verticesX[verticeIndex] <<= 4;
						this.verticesY[verticeIndex] <<= 4;
						this.verticesZ[verticeIndex] <<= 4;
					}
					this.upscaled = true;
				}
				int originalX = verticesData[9] << 4;
				int originalY = verticesData[10] << 4;
				int originalZ = verticesData[11] << 4;
				int deltaX = verticesData[12] << 4;
				int deltaY = verticesData[13] << 4;
				int deltaZ = verticesData[14] << 4;
				if (this.withinOrigin) {
					int x = verticesData[6] * this.originZ + verticesData[3] * this.originY + verticesData[0] * this.originX + 8192 >> 14;
					int y = verticesData[7] * this.originZ + verticesData[1] * this.originX + verticesData[4] * this.originY + 8192 >> 14;
					int z = verticesData[8] * this.originZ + verticesData[2] * this.originX + verticesData[5] * this.originY + 8192 >> 14;
					x += deltaX;
					y += deltaY;
					z += deltaZ;
					this.originX = x;
					this.originY = y;
					this.originZ = z;
					this.withinOrigin = false;
				}
				int[] offsets1 = new int[9];
				int zanX = MathUtils.COSINE[rotationX];
				int xanX = MathUtils.SINE[rotationX];
				int zanY = MathUtils.COSINE[rotationY];
				int xanY = MathUtils.SINE[rotationY];
				int zanZ = MathUtils.COSINE[rotationZ];
				int xanZ = MathUtils.SINE[rotationZ];
				int i_23 = xanX * zanZ + 8192 >> 14;
				int i_24 = xanX * xanZ + 8192 >> 14;
				offsets1[0] = zanY * zanZ + xanY * i_24 + 8192 >> 14;
				offsets1[1] = xanY * i_23 + -zanY * xanZ + 8192 >> 14;
				offsets1[2] = xanY * zanX + 8192 >> 14;
				offsets1[3] = zanX * xanZ + 8192 >> 14;
				offsets1[4] = zanX * zanZ + 8192 >> 14;
				offsets1[5] = -xanX;
				offsets1[6] = zanY * i_24 + -xanY * zanZ + 8192 >> 14;
				offsets1[7] = xanY * xanZ + zanY * i_23 + 8192 >> 14;
				offsets1[8] = zanY * zanX + 8192 >> 14;
				int offsetX = offsets1[2] * -this.originZ + offsets1[0] * -this.originX + offsets1[1] * -this.originY + 8192 >> 14;
				int offsetY = offsets1[5] * -this.originZ + offsets1[4] * -this.originY + offsets1[3] * -this.originX + 8192 >> 14;
				int offsetZ = offsets1[6] * -this.originX + offsets1[8] * -this.originZ + offsets1[7] * -this.originY + 8192 >> 14;
				int offsetOriginX = offsetX + this.originX;
				int offsetOriginY = offsetY + this.originY;
				int offsetOriginZ = offsetZ + this.originZ;
				int[] offsets2 = new int[9];
				for (int x2 = 0; x2 < 3; x2++) {
					for (int y2 = 0; y2 < 3; y2++) {
						int i_34 = 0;
						for (int x = 0; x < 3; x++) {
							i_34 += verticesData[x + y2 * 3] * offsets1[x + x2 * 3];
						}
						offsets2[y2 + x2 * 3] = i_34 + 8192 >> 14;
					}
				}
				int fullOffX = deltaY * offsets1[1] + deltaX * offsets1[0] + deltaZ * offsets1[2] + 8192 >> 14;
				int fullOffY = deltaY * offsets1[4] + deltaX * offsets1[3] + deltaZ * offsets1[5] + 8192 >> 14;
				int fullOffZ = offsets1[6] * deltaX + deltaZ * offsets1[8] + deltaY * offsets1[7] + 8192 >> 14;
				fullOffX += offsetOriginX;
				fullOffY += offsetOriginY;
				fullOffZ += offsetOriginZ;
				int[] offsets3 = new int[9];
				for (int x3 = 0; x3 < 3; x3++) {
					for (int y3 = 0; y3 < 3; y3++) {
						int offset = 0;
						for (int x2 = 0; x2 < 3; x2++) {
							offset += offsets2[y3 + x2 * 3] * verticesData[x2 + x3 * 3];
						}
						offsets3[y3 + x3 * 3] = offset + 8192 >> 14;
					}
				}
				int translateX = fullOffZ * verticesData[2] + fullOffX * verticesData[0] + fullOffY * verticesData[1] + 8192 >> 14;
				int translateY = fullOffX * verticesData[3] + fullOffY * verticesData[4] + fullOffZ * verticesData[5] + 8192 >> 14;
				int translateZ = fullOffX * verticesData[6] + fullOffZ * verticesData[8] + fullOffY * verticesData[7] + 8192 >> 14;
				translateX += originalX;
				translateY += originalY;
				translateZ += originalZ;

				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.vertexGroups.length) {
						int[] verticeIndices = this.vertexGroups[label];
						for (int index = 0; index < verticeIndices.length; index++) {
							int vertice = verticeIndices[index];
							if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[vertice]) != 0) {
								int x = offsets3[0] * this.verticesX[vertice] + offsets3[2] * this.verticesZ[vertice] + offsets3[1] * this.verticesY[vertice] + 8192 >> 14;
								int y = offsets3[4] * this.verticesY[vertice] + offsets3[5] * this.verticesZ[vertice] + offsets3[3] * this.verticesX[vertice] + 8192 >> 14;
								int z = offsets3[6] * this.verticesX[vertice] + offsets3[7] * this.verticesY[vertice] + offsets3[8] * this.verticesZ[vertice] + 8192 >> 14;
								x += translateX;
								y += translateY;
								z += translateZ;
								this.verticesX[vertice] = x;
								this.verticesY[vertice] = y;
								this.verticesZ[vertice] = z;
							}
						}
					}
				}
			} else {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.vertexGroups.length) {
						int[] vertices = this.vertexGroups[label];
						for (int vertIndex = 0; vertIndex < vertices.length; vertIndex++) {
							int vertice = vertices[vertIndex];
							if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[vertice]) != 0) {
								this.verticesX[vertice] -= this.originX;
								this.verticesY[vertice] -= this.originY;
								this.verticesZ[vertice] -= this.originZ;
								if (rotationZ != 0) {
									int xan = MathUtils.SINE[rotationZ];
									int zan = MathUtils.COSINE[rotationZ];
									int tmpY = xan * this.verticesY[vertice] + zan * this.verticesX[vertice] + 16383 >> 14;
									this.verticesY[vertice] = zan * this.verticesY[vertice] - xan * this.verticesX[vertice] + 16383 >> 14;
									this.verticesX[vertice] = tmpY;
								}
								if (rotationX != 0) {
									int xan = MathUtils.SINE[rotationX];
									int zan = MathUtils.COSINE[rotationX];
									int tmpZ = zan * this.verticesY[vertice] - xan * this.verticesZ[vertice] + 16383 >> 14;
									this.verticesZ[vertice] = xan * this.verticesY[vertice] + zan * this.verticesZ[vertice] + 16383 >> 14;
									this.verticesY[vertice] = tmpZ;
								}
								if (rotationY != 0) {
									int xan = MathUtils.SINE[rotationY];
									int zan = MathUtils.COSINE[rotationY];
									int tmpZ = xan * this.verticesZ[vertice] + zan * this.verticesX[vertice] + 16383 >> 14;
									this.verticesZ[vertice] = zan * this.verticesZ[vertice] - xan * this.verticesX[vertice] + 16383 >> 14;
									this.verticesX[vertice] = tmpZ;
								}
								this.verticesX[vertice] += this.originX;
								this.verticesY[vertice] += this.originY;
								this.verticesZ[vertice] += this.originZ;
							}
						}
					}
				}
			}
		} else if (transformationType == 3) {
			if (verticesData != null) {
				if (!this.upscaled) {
					for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
						this.verticesX[verticeIndex] <<= 4;
						this.verticesY[verticeIndex] <<= 4;
						this.verticesZ[verticeIndex] <<= 4;
					}
					this.upscaled = true;
				}
				int originalX = verticesData[9] << 4;
				int originalY = verticesData[10] << 4;
				int originalZ = verticesData[11] << 4;
				int deltaX = verticesData[12] << 4;
				int deltaY = verticesData[13] << 4;
				int deltaZ = verticesData[14] << 4;
				if (this.withinOrigin) {
					int x = verticesData[6] * this.originZ + verticesData[3] * this.originY + verticesData[0] * this.originX + 8192 >> 14;
					int y = verticesData[7] * this.originZ + verticesData[1] * this.originX + verticesData[4] * this.originY + 8192 >> 14;
					int z = verticesData[8] * this.originZ + verticesData[2] * this.originX + verticesData[5] * this.originY + 8192 >> 14;
					x += deltaX;
					y += deltaY;
					z += deltaZ;
					this.originX = x;
					this.originY = y;
					this.originZ = z;
					this.withinOrigin = false;
				}

				int rotX = rotationX << 15 >> 7;
				int rotY = rotationY << 15 >> 7;
				int rotZ = rotationZ << 15 >> 7;
				int offX = rotX * -this.originX + 8192 >> 14;
				int offY = rotY * -this.originY + 8192 >> 14;
				int offZ = rotZ * -this.originZ + 8192 >> 14;
				int originXOffset = offX + this.originX;
				int originYOffset = offY + this.originY;
				int originZOffset = offZ + this.originZ;
				int[] offsetCluster = {
						rotX * verticesData[0] + 8192 >> 14, rotX * verticesData[3] + 8192 >> 14,
						rotX * verticesData[6] + 8192 >> 14, rotY * verticesData[1] + 8192 >> 14,
						rotY * verticesData[4] + 8192 >> 14, rotY * verticesData[7] + 8192 >> 14,
						rotZ * verticesData[2] + 8192 >> 14, rotZ * verticesData[5] + 8192 >> 14,
						rotZ * verticesData[8] + 8192 >> 14
				};
				int offsetRotX = rotX * deltaX + 8192 >> 14;
				int offsetRotY = rotY * deltaY + 8192 >> 14;
				int offsetRotZ = rotZ * deltaZ + 8192 >> 14;
				offsetRotX += originXOffset;
				offsetRotY += originYOffset;
				offsetRotZ += originZOffset;
				int[] offsetMatrix = new int[9];
				for (int w = 0; w < 3; w++) {
					for (int h = 0; h < 3; h++) {
						int off = 0;
						for (int z2 = 0; z2 < 3; z2++) {
							off += offsetCluster[h + z2 * 3] * verticesData[z2 + w * 3];
						}
						offsetMatrix[h + w * 3] = off + 8192 >> 14;
					}
				}
				int finalOffX = offsetRotZ * verticesData[2] + offsetRotX * verticesData[0] + verticesData[1] * offsetRotY + 8192 >> 14;
				int finalOffY = offsetRotX * verticesData[3] + offsetRotY * verticesData[4] + verticesData[5] * offsetRotZ + 8192 >> 14;
				int finalOffZ = offsetRotX * verticesData[6] + offsetRotZ * verticesData[8] + verticesData[7] * offsetRotY + 8192 >> 14;
				finalOffX += originalX;
				finalOffY += originalY;
				finalOffZ += originalZ;
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int group = labels[labelIndex];
					if (group < this.vertexGroups.length) {
						int[] vertices = this.vertexGroups[group];
						for (int index = 0; index < vertices.length; index++) {
							int vertice = vertices[index];
							if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[vertice]) != 0) {
								int x = offsetMatrix[0] * this.verticesX[vertice] + offsetMatrix[2] * this.verticesZ[vertice] + offsetMatrix[1] * this.verticesY[vertice] + 8192 >> 14;
								int y = offsetMatrix[4] * this.verticesY[vertice] + offsetMatrix[5] * this.verticesZ[vertice] + offsetMatrix[3] * this.verticesX[vertice] + 8192 >> 14;
								int z = offsetMatrix[6] * this.verticesX[vertice] + offsetMatrix[7] * this.verticesY[vertice] + offsetMatrix[8] * this.verticesZ[vertice] + 8192 >> 14;
								x += finalOffX;
								y += finalOffY;
								z += finalOffZ;
								this.verticesX[vertice] = x;
								this.verticesY[vertice] = y;
								this.verticesZ[vertice] = z;
							}
						}
					}
				}
			} else {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int group = labels[labelIndex];
					if (group < this.vertexGroups.length) {
						int[] vertices = this.vertexGroups[group];
						for (int vertIndex = 0; vertIndex < vertices.length; vertIndex++) {
							int vertex = vertices[vertIndex];
							if (this.verticesSubmeshes == null || (submesh & this.verticesSubmeshes[vertex]) != 0) {
								this.verticesX[vertex] -= this.originX;
								this.verticesY[vertex] -= this.originY;
								this.verticesZ[vertex] -= this.originZ;
								this.verticesX[vertex] = rotationX * this.verticesX[vertex] / 128;
								this.verticesY[vertex] = rotationY * this.verticesY[vertex] / 128;
								this.verticesZ[vertex] = rotationZ * this.verticesZ[vertex] / 128;
								this.verticesX[vertex] += this.originX;
								this.verticesY[vertex] += this.originY;
								this.verticesZ[vertex] += this.originZ;
							}
						}
					}
				}
			}
		} else if (transformationType == 5) { // transform alpha
			if (this.faceGroups != null && this.facesAlpha != null) {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.faceGroups.length) {
						int[] vertices = this.faceGroups[label];
						for (int vertIndex = 0; vertIndex < vertices.length; vertIndex++) {
							int vertice = vertices[vertIndex];
							if (this.facesSubmeshes == null || (submesh & this.facesSubmeshes[vertice]) != 0) {
								int alpha = (this.facesAlpha[vertice] & 0xff) + rotationX * 8;
								if (alpha < 0) {
									alpha = 0;
								} else if (alpha > 255) {
									alpha = 255;
								}
								this.facesAlpha[vertice] = (byte) alpha;
							}
						}
					}
				}
				if (this.billboardEffects != null) {
					for (int billboardIndex = 0; billboardIndex < this.numBillboards; billboardIndex++) {
						BillboardEffect effect = this.billboardEffects[billboardIndex];
						BillboardSurface surface = this.billboardSurfaces[billboardIndex];
						surface.diffuseColor = surface.diffuseColor & 0xffffff | 255 - (this.facesAlpha[effect.appliesOnFace] & 0xff) << 24;
					}
				}
			}
		} else if (transformationType == 7) { // adjust color
			if (this.faceGroups != null) {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.faceGroups.length) {
						int[] faces = this.faceGroups[label];
						for (int vertIndex = 0; vertIndex < faces.length; vertIndex++) {
							int vertice = faces[vertIndex];
							if (this.facesSubmeshes == null || (submesh & this.facesSubmeshes[vertice]) != 0) {
								int color = this.facesColor[vertice] & 0xffff;
								int h = color >> 10 & 0x3f;
								int s = color >> 7 & 0x7;
								int l = color & 0x7f;
								h = rotationX + h & 0x3f;
								s += rotationY;
								if (s < 0) {
									s = 0;
								} else if (s > 7) {
									s = 7;
								}
								l += rotationZ;
								if (l < 0) {
									l = 0;
								} else if (l > 127) {
									l = 127;
								}
								this.facesColor[vertice] = (short) (h << 10 | s << 7 | l);
							}
						}
						this.calculateColors = true;
					}
				}
				if (this.billboardEffects != null) {
					for (int billboardIndex = 0; billboardIndex < this.numBillboards; billboardIndex++) {
						BillboardEffect effect = this.billboardEffects[billboardIndex];
						BillboardSurface surface = this.billboardSurfaces[billboardIndex];
						surface.diffuseColor = surface.diffuseColor & ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil
								.adjustHsv(this.facesColor[effect.appliesOnFace] & 0xffff) & 0xffff] & 0xffffff;
					}
				}
			}
		} else if (transformationType == 8) { // offset billboard
			if (this.billboardGroups != null) {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.billboardGroups.length) {
						int[] vertices = this.billboardGroups[label];
						for (int vertIndex = 0; vertIndex < vertices.length; vertIndex++) {
							BillboardSurface surface = this.billboardSurfaces[vertices[vertIndex]];
							surface.offsetX += rotationX;
							surface.offsetY += rotationY;
						}
					}
				}
			}
		} else if (transformationType == 10) { // scale billboard
			if (this.billboardGroups != null) {
				for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
					int label = labels[labelIndex];
					if (label < this.billboardGroups.length) {
						int[] vertices = this.billboardGroups[label];
						for (int vertIndex = 0; vertIndex < vertices.length; vertIndex++) {
							BillboardSurface surface = this.billboardSurfaces[vertices[vertIndex]];
							surface.scaleX = surface.scaleX * rotationX / 128.0F;
							surface.scaleY = surface.scaleY * rotationY / 128.0F;
						}
					}
				}
			}
		} else if (transformationType == 9 && this.billboardGroups != null) { // rotate billboard
			for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
				int label = labels[labelIndex];
				if (label < this.billboardGroups.length) {
					int[] vertices = this.billboardGroups[label];
					for (int vertIndex = 0; vertIndex < vertices.length; vertIndex++) {
						BillboardSurface surface = this.billboardSurfaces[vertices[vertIndex]];
						surface.rotation = rotationX + surface.rotation & 0x3fff;
					}
				}
			}
		}
	}

	@Override
	public void animateShadowtransform(int transformationType, int x, int y, int z) {
		if (transformationType == 0) {
			int transformCount = 0;
			this.originX = 0;
			this.originY = 0;
			this.originZ = 0;
			for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
				this.originX += this.verticesX[verticeIndex];
				this.originY += this.verticesY[verticeIndex];
				this.originZ += this.verticesZ[verticeIndex];
				transformCount++;
			}
			if (transformCount > 0) {
				this.originX = this.originX / transformCount + x;
				this.originY = y + this.originY / transformCount;
				this.originZ = this.originZ / transformCount + z;
			} else {
				this.originX = x;
				this.originY = y;
				this.originZ = z;
			}
		} else if (transformationType == 1) {
			for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
				this.verticesX[verticeIndex] += x;
				this.verticesY[verticeIndex] += y;
				this.verticesZ[verticeIndex] += z;
			}
		} else if (transformationType == 2) {
			for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
				this.verticesX[verticeIndex] -= this.originX;
				this.verticesY[verticeIndex] -= this.originY;
				this.verticesZ[verticeIndex] -= this.originZ;
				if (z != 0) {
					int xan = MathUtils.SINE[z];
					int zan = MathUtils.COSINE[z];
					int tmpX = xan * this.verticesY[verticeIndex] + zan * this.verticesX[verticeIndex] + 16383 >> 14;
					this.verticesY[verticeIndex] = zan * this.verticesY[verticeIndex] - xan * this.verticesX[verticeIndex] + 16383 >> 14;
					this.verticesX[verticeIndex] = tmpX;
				}
				if (x != 0) {
					int xan = MathUtils.SINE[x];
					int zan = MathUtils.COSINE[x];
					int tmpY = zan * this.verticesY[verticeIndex] - xan * this.verticesZ[verticeIndex] + 16383 >> 14;
					this.verticesZ[verticeIndex] = xan * this.verticesY[verticeIndex] + zan * this.verticesZ[verticeIndex] + 16383 >> 14;
					this.verticesY[verticeIndex] = tmpY;
				}
				if (y != 0) {
					int xan = MathUtils.SINE[y];
					int zan = MathUtils.COSINE[y];
					int tmpX = xan * this.verticesZ[verticeIndex] + zan * this.verticesX[verticeIndex] + 16383 >> 14;
					this.verticesZ[verticeIndex] = zan * this.verticesZ[verticeIndex] - xan * this.verticesX[verticeIndex] + 16383 >> 14;
					this.verticesX[verticeIndex] = tmpX;
				}
				this.verticesX[verticeIndex] += this.originX;
				this.verticesY[verticeIndex] += this.originY;
				this.verticesZ[verticeIndex] += this.originZ;
			}
		} else if (transformationType == 3) {
			for (int verticesIndex = 0; verticesIndex < this.numVertices; verticesIndex++) {
				this.verticesX[verticesIndex] -= this.originX;
				this.verticesY[verticesIndex] -= this.originY;
				this.verticesZ[verticesIndex] -= this.originZ;
				this.verticesX[verticesIndex] = x * this.verticesX[verticesIndex] / 128;
				this.verticesY[verticesIndex] = y * this.verticesY[verticesIndex] / 128;
				this.verticesZ[verticesIndex] = z * this.verticesZ[verticesIndex] / 128;
				this.verticesX[verticesIndex] += this.originX;
				this.verticesY[verticesIndex] += this.originY;
				this.verticesZ[verticesIndex] += this.originZ;
			}
		} else if (transformationType == 5) {
			for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
				int alpha = (this.facesAlpha[faceIndex] & 0xff) + x * 8;
				if (alpha < 0) {
					alpha = 0;
				} else if (alpha > 255) {
					alpha = 255;
				}
				this.facesAlpha[faceIndex] = (byte) alpha;
			}
			if (this.billboardEffects != null) {
				for (int faceIndex = 0; faceIndex < this.numBillboards; faceIndex++) {
					BillboardEffect effect = this.billboardEffects[faceIndex];
					BillboardSurface surface = this.billboardSurfaces[faceIndex];
					surface.diffuseColor = surface.diffuseColor & 0xffffff | 255 - (this.facesAlpha[effect.appliesOnFace] & 0xff) << 24;
				}
			}
		} else if (transformationType == 7) {
			for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
				int color = this.facesColor[faceIndex] & 0xffff;
				int h = color >> 10 & 0x3f;
				int s = color >> 7 & 0x7;
				int l = color & 0x7f;
				h = h + x & 0x3f;
				s += y;
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
				this.facesColor[faceIndex] = (short) (h << 10 | s << 7 | l);
			}
			this.calculateColors = true;
			if (this.billboardEffects != null) {
				for (int faceIndex = 0; faceIndex < this.numBillboards; faceIndex++) {
					BillboardEffect effect = this.billboardEffects[faceIndex];
					BillboardSurface surface = this.billboardSurfaces[faceIndex];
					surface.diffuseColor = surface.diffuseColor & ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil
							.adjustHsv(this.facesColor[effect.appliesOnFace] & 0xffff) & 0xffff] & 0xffffff;
				}
			}
		} else if (transformationType == 8) {
			for (int faceIndex = 0; faceIndex < this.numBillboards; faceIndex++) {
				BillboardSurface surface = this.billboardSurfaces[faceIndex];
				surface.offsetX += x;
				surface.offsetY += y;
			}
		} else if (transformationType == 10) {
			for (int faceIndex = 0; faceIndex < this.numBillboards; faceIndex++) {
				BillboardSurface surface = this.billboardSurfaces[faceIndex];
				surface.scaleX = surface.scaleX * x / 128.0F;
				surface.scaleY = surface.scaleY * y / 128.0F;
			}
		} else if (transformationType == 9) {
			for (int faceIndex = 0; faceIndex < this.numBillboards; faceIndex++) {
				BillboardSurface surface = this.billboardSurfaces[faceIndex];
				surface.rotation = x + surface.rotation & 0x3fff;
			}
		}
	}

    public void multithreadedDraw(Matrix43 matrix44var1, BoundingBoxPolygon renderedEntity, int rasterizerFlags) {
        if (this.maxDepth >= 1) {
            ThreadResourceSM resource = this.toolkit.getThreadResource(Thread.currentThread());
            Matrix44 particleMatrix = resource.particleMatrix;
            particleMatrix.fromMatrix(matrix44var1);
            Matrix44 matrix446 = this.toolkit.aClass3848994;
            Matrix44 matrix447 = this.toolkit.modelMatrix;
            if (!this.boundsUpdated) {
                calculateBounds();
            }
            boolean bool8 = particleMatrix.method6526();
            float[] particleOffsets = resource.particleOffsets;
            particleMatrix.scale(0.0F, this.minY, 0.0F, particleOffsets);
            float offsetXA = particleOffsets[0];
            float offsetYA = particleOffsets[1];
            float offsetZA = particleOffsets[2];
            particleMatrix.scale(0.0F, this.maxY, 0.0F, particleOffsets);
            float offsetXB = particleOffsets[0];
            float offsetYB = particleOffsets[1];
            float offsetZB = particleOffsets[2];
            float f_18;
            float f_19;
            for (int i_16 = 0; i_16 < 6; i_16++) {
                float[] floats_17 = this.toolkit.aFloatArrayArray8989[i_16];
                f_18 = floats_17[0] * offsetXA + floats_17[1] * offsetYA + floats_17[2] * offsetZA + floats_17[3] + this.size2d;
                f_19 = floats_17[0] * offsetXB + floats_17[1] * offsetYB + floats_17[2] * offsetZB + floats_17[3] + this.size2d;
                if (f_18 < 0.0F && f_19 < 0.0F) {
                    return;
                }
            }
            float nNearFaceA;
            float zNearFaceD;
            if (bool8) {
                zNearFaceD = matrix446.faceList[14];
                nNearFaceA = matrix446.faceList[6];
            } else {
                zNearFaceD = particleMatrix.faceList[12] * matrix446.faceList[2] + particleMatrix.faceList[13] * matrix446.faceList[6] + particleMatrix.faceList[14] * matrix446.faceList[10] + matrix446.faceList[14];
                nNearFaceA = particleMatrix.faceList[4] * matrix446.faceList[2] + particleMatrix.faceList[5] * matrix446.faceList[6] + particleMatrix.faceList[6] * matrix446.faceList[10];
            }
            f_18 = zNearFaceD + this.minY * nNearFaceA;
            f_19 = zNearFaceD + this.maxY * nNearFaceA;
            float f_20 = f_18 > f_19 ? f_18 + this.size2d : f_19 + this.size2d;
            float f_21 = matrix447.faceList[10] * f_20 + matrix447.faceList[14];
            if (this.toolkit.executingThreadCount > 1) {
                synchronized (this) {
                    while (this.renderLock) {
                        try {
                            wait();
                        } catch (InterruptedException ie) {
                        	// NOOP
                        }
                    }
                    this.renderLock = true;
                }
            }
            copyDependencies(resource);
            Matrix44 renderMatrix = this.threadResource.renderingMatrix;
            renderMatrix.copyFrom(particleMatrix);
            renderMatrix.multiplyBy(this.toolkit.floorMatrix);
            this.rasterizer.setWireframeMode((rasterizerFlags & ModelFunctionMaskGL.WIREFRAME_MODE) != 0);
            boolean offscreen = false;
            this.threadResource.offsetX = this.rasterizer.offsetX;
            this.threadResource.offsetY = this.rasterizer.offsetY;
            this.threadResource.aFloat2311 = this.rasterizer.aFloat1683;
            this.threadResource.aFloat2308 = 1.0F / this.rasterizer.aFloat1683;
            this.threadResource.centerX = this.rasterizer.centerX;
            this.threadResource.centerY = this.rasterizer.centerY;
            this.threadResource.aFloat2335 = this.rasterizer.aFloat1682;
            this.threadResource.clipWidth = this.rasterizer.clipWidth;
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                int verticeX = this.verticesX[verticeIndex];
                int verticeY = this.verticesY[verticeIndex];
                int verticeZ = this.verticesZ[verticeIndex];
                float matrixX = renderMatrix.faceList[0] * verticeX + renderMatrix.faceList[4] * verticeY + renderMatrix.faceList[8] * verticeZ + renderMatrix.faceList[12];
                float matrixY = renderMatrix.faceList[1] * verticeX + renderMatrix.faceList[5] * verticeY + renderMatrix.faceList[9] * verticeZ + renderMatrix.faceList[13];
                float matrixZ = renderMatrix.faceList[2] * verticeX + renderMatrix.faceList[6] * verticeY + renderMatrix.faceList[10] * verticeZ + renderMatrix.faceList[14];
                float z = renderMatrix.faceList[3] * verticeX + renderMatrix.faceList[7] * verticeY + renderMatrix.faceList[11] * verticeZ + renderMatrix.faceList[15];
                this.projectedVerticesZ[verticeIndex] = this.threadResource.aFloat2335 + this.threadResource.aFloat2311 * matrixZ / z;
                this.projectedVerticesZ2[verticeIndex] = z;
                if (matrixZ >= -z) {
                    this.projectedVerticesX[verticeIndex] = ((int) (this.threadResource.centerX + this.threadResource.offsetX * matrixX / z));
                    this.projectedVerticesY[verticeIndex] = ((int) (this.threadResource.centerY + this.threadResource.offsetY * matrixY / z));
                } else {
                    this.projectedVerticesX[verticeIndex] = -5000.0f;
                    offscreen = true;
                }
                if (this.threadResource.hasWaterUpdate) {
                    this.waterRenderHeight[verticeIndex] = (int) (particleMatrix.faceList[13] + particleMatrix.faceList[1] * verticeX + particleMatrix.faceList[5] * verticeY + particleMatrix.faceList[9] * verticeZ);
                }
            }
            if (this.billboardEffects != null) {
                for (int billboardIndex = 0; billboardIndex < this.numBillboards; billboardIndex++) {
                    BillboardEffect effect = this.billboardEffects[billboardIndex];
                    BillboardSurface surface = this.billboardSurfaces[billboardIndex];
                    short a = this.facesA[effect.appliesOnFace];
                    short b = this.facesB[effect.appliesOnFace];
                    short c = this.facesC[effect.appliesOnFace];
                    int avgX = (this.verticesX[a] + this.verticesX[b] + this.verticesX[c]) / 3;
                    int avgY = (this.verticesY[a] + this.verticesY[b] + this.verticesY[c]) / 3;
                    int avgZ = (this.verticesZ[a] + this.verticesZ[b] + this.verticesZ[c]) / 3;
                    float mX = renderMatrix.faceList[0] * avgX + renderMatrix.faceList[4] * avgY + renderMatrix.faceList[8] * avgZ + renderMatrix.faceList[12];
                    float mY = renderMatrix.faceList[1] * avgX + renderMatrix.faceList[5] * avgY + renderMatrix.faceList[9] * avgZ + renderMatrix.faceList[13];
                    float mZ = renderMatrix.faceList[2] * avgX + renderMatrix.faceList[6] * avgY + renderMatrix.faceList[10] * avgZ + renderMatrix.faceList[14];
                    float mZB2 = renderMatrix.faceList[3] * avgX + renderMatrix.faceList[7] * avgY + renderMatrix.faceList[11] * avgZ + renderMatrix.faceList[15];
                    mZ += matrix447.faceList[2] * surface.offsetX + matrix447.faceList[6] * surface.offsetY;
                    mZB2 += matrix447.faceList[3] * surface.offsetX + matrix447.faceList[7] * surface.offsetY;
                    if (mZ > -mZB2) {
                        mX += matrix447.faceList[0] * surface.offsetX + matrix447.faceList[4] * surface.offsetY;
                        mY += matrix447.faceList[1] * surface.offsetX + matrix447.faceList[5] * surface.offsetY;
                        float x = this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * mX / mZB2;
                        float y = this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * mY / mZB2;
                        float adjustedSizeX = surface.scaleX * effect.size2d;
                        float adjustedSizeY = surface.scaleY * effect.size3d;
                        float adjustX = mX + matrix447.faceList[0] * adjustedSizeX + matrix447.faceList[4] * adjustedSizeY;
                        float adjustY = mY + matrix447.faceList[1] * adjustedSizeX + matrix447.faceList[5] * adjustedSizeY;
                        float adjustZB2 = mZB2 + matrix447.faceList[3] * adjustedSizeX + matrix447.faceList[7] * adjustedSizeY;
                        float otherX = this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * adjustX / adjustZB2;
                        float otherY = this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * adjustY / adjustZB2;
                        surface.x = (int) x;
                        surface.y = (int) y;
                        surface.z = this.toolkit.aFloat8985 + this.toolkit.aFloat8978 * (mZ - matrix447.faceList[10] * effect.distance) / mZB2;
                        surface.size2d = (int) (otherX < x ? x - otherX : otherX - x);
                        surface.size3d = (int) (otherY < y ? y - otherY : otherY - y);
                    } else {
                        surface.size3d = 0;
                        surface.size2d = 0;
                    }
                }
            }
            if (renderedEntity != null) {
                boolean bool57 = false;
                boolean bool58 = true;
                int sizeDeltaX = this.minX + this.maxX >> 1;
                int sizeDeltaZ = this.minZ + this.maxZ >> 1;
                short mY = this.minY;
                float z = renderMatrix.faceList[0] * sizeDeltaX + renderMatrix.faceList[4] * mY + renderMatrix.faceList[8] * sizeDeltaZ + renderMatrix.faceList[12];
                float f_64 = renderMatrix.faceList[1] * sizeDeltaX + renderMatrix.faceList[5] * mY + renderMatrix.faceList[9] * sizeDeltaZ + renderMatrix.faceList[13];
                float f_33 = renderMatrix.faceList[2] * sizeDeltaX + renderMatrix.faceList[6] * mY + renderMatrix.faceList[10] * sizeDeltaZ + renderMatrix.faceList[14];
                float f_34 = renderMatrix.faceList[3] * sizeDeltaX + renderMatrix.faceList[7] * mY + renderMatrix.faceList[11] * sizeDeltaZ + renderMatrix.faceList[15];
                if (f_33 >= -f_34) {
                    renderedEntity.left = (int) (this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * z / f_34);
                    renderedEntity.bottom = (int) (this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * f_64 / f_34);
                } else {
                    bool57 = true;
                }
                mY = this.maxY;
                float f_35 = renderMatrix.faceList[0] * sizeDeltaX + renderMatrix.faceList[4] * mY + renderMatrix.faceList[8] * sizeDeltaZ + renderMatrix.faceList[12];
                float f_36 = renderMatrix.faceList[1] * sizeDeltaX + renderMatrix.faceList[5] * mY + renderMatrix.faceList[9] * sizeDeltaZ + renderMatrix.faceList[13];
                float f_37 = renderMatrix.faceList[2] * sizeDeltaX + renderMatrix.faceList[6] * mY + renderMatrix.faceList[10] * sizeDeltaZ + renderMatrix.faceList[14];
                float f_38 = renderMatrix.faceList[3] * sizeDeltaX + renderMatrix.faceList[7] * mY + renderMatrix.faceList[11] * sizeDeltaZ + renderMatrix.faceList[15];
                if (f_37 >= -f_38) {
                    renderedEntity.right = (int) (this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * f_35 / f_38);
                    renderedEntity.top = (int) (this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * f_36 / f_38);
                } else {
                    bool57 = true;
                }
                if (bool57) {
                    if (f_33 < -f_34 && f_37 < -f_38) {
                        bool58 = false;
                    } else if (f_33 < -f_34) {
                    	float f_39 = (f_33 + f_34) / (f_37 + f_38) - 1.0F;
                    	float f_40 = z + f_39 * (f_35 - z);
                    	float f_41 = f_64 + f_39 * (f_36 - f_64);
                    	float f_42 = f_34 + f_39 * (f_38 - f_34);
                        renderedEntity.left = (int) (this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * f_40 / f_42);
                        renderedEntity.bottom = (int) (this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * f_41 / f_42);
                    } else if (f_37 < -f_38) {
                    	float f_39 = (f_37 + f_38) / (f_33 + f_34) - 1.0F;
                    	float f_40 = f_35 + f_39 * (z - f_35);
                    	float f_41 = f_36 + f_39 * (f_64 - f_36);
                    	float f_42 = f_38 + f_39 * (f_34 - f_38);
                        renderedEntity.right = (int) (this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * f_40 / f_42);
                        renderedEntity.top = (int) (this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * f_41 / f_42);
                    }
                }

                if (bool58) {
                    if (f_33 / f_34 > f_37 / f_38) {
                    	float f_39 = z + matrix447.faceList[0] * this.size2d + matrix447.faceList[12];
                    	float f_40 = f_34 + matrix447.faceList[3] * this.size2d + matrix447.faceList[15];
                        renderedEntity.size = (int) (this.toolkit.aFloat9003 - renderedEntity.left + this.toolkit.aFloat8976 * f_39 / f_40);
                    } else {
                    	float f_39 = f_35 + matrix447.faceList[0] * this.size2d + matrix447.faceList[12];
                    	float f_40 = f_38 + matrix447.faceList[3] * this.size2d + matrix447.faceList[15];
                        renderedEntity.size = (int) (this.toolkit.aFloat9003 - renderedEntity.right + this.toolkit.aFloat8976 * f_39 / f_40);
                    }

                    renderedEntity.positionCalculated = true;
                }
            }
            appearanceUpdateThreaded(true);
            this.rasterizer.smoothEdges = (rasterizerFlags & ModelFunctionMaskGL.SMOOTH_EDGES) == 0;
            this.rasterizer.blending = false;
            try {
                draw(this.toolkit.colourBuffer != null, this.toolkit.depthBuffer != null, (rasterizerFlags & ModelFunctionMaskGL.TRANSPARENT) != 0, offscreen, this.threadResource.aBool2304 && f_21 > this.threadResource.aFloat2305 || this.threadResource.hasWaterUpdate);
            } catch (Exception ex) {
            	// NOOP
            }
            if (this.billboardEffects != null) {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    this.billboardOnFace[faceIndex] = -1;
                }
            }
            this.rasterizer = null;
            if (this.toolkit.executingThreadCount > 1) {
                synchronized (this) {
                    this.renderLock = false;
                    notifyAll();
                }
            }
        }
    }

    public boolean hasAlpha(int faceIndex) {
        return this.facesAlpha != null && this.facesAlpha[faceIndex] != 0;
    }

    @Override
    public void applyShading(Model model, int x, int y, int z, boolean renderTypeAdjustment) {
        ModelSM safeModel = (ModelSM) model;
        if ((this.functionMask & 0x10000) != 65536) {
            throw new IllegalStateException("");
        } else if ((safeModel.functionMask & 0x10000) != 65536) {
            throw new IllegalStateException("");
        } 
        copyDependencies(this.toolkit.getThreadResource(Thread.currentThread()));
        calculateBounds();
        calculateNormalsThreaded();
        safeModel.calculateBounds();
        safeModel.calculateNormalsThreaded();
        renderCycle++;
        int cycleCount = 0;
        int[] vertsX = safeModel.verticesX;
        int depth = safeModel.maxDepth;
        for (int verticeIndex = 0; verticeIndex < this.maxDepth; verticeIndex++) {
            VertexNormal normals = this.vertexNormals[verticeIndex];
            if (normals.magnitude != 0) {
                int vertY = this.verticesY[verticeIndex] - y;
                if (vertY >= safeModel.minY && vertY <= safeModel.maxY) {
                    int vertX = this.verticesX[verticeIndex] - x;
                    if (vertX >= safeModel.minX && vertX <= safeModel.maxX) {
                        int vertZ = this.verticesZ[verticeIndex] - z;
                        if (vertZ >= safeModel.minZ && vertZ <= safeModel.maxZ) {
                            for (int currDepth = 0; currDepth < depth; currDepth++) {
                                VertexNormal normals2 = safeModel.vertexNormals[currDepth];
                                if (vertX == vertsX[currDepth] && vertZ == safeModel.verticesZ[currDepth] && vertY == safeModel.verticesY[currDepth] && normals2.magnitude != 0) {
                                    if (this.vertexNormalOffsets == null) {
                                        this.vertexNormalOffsets = new VertexNormal[this.maxDepth];
                                    }
                                    if (safeModel.vertexNormalOffsets == null) {
                                        safeModel.vertexNormalOffsets = new VertexNormal[depth];
                                    }
                                    VertexNormal offsets1 = this.vertexNormalOffsets[verticeIndex];
                                    if (offsets1 == null) {
                                        offsets1 = this.vertexNormalOffsets[verticeIndex] = new VertexNormal(normals);
                                    }
                                    VertexNormal offsets2 = safeModel.vertexNormalOffsets[currDepth];
                                    if (offsets2 == null) {
                                        offsets2 = safeModel.vertexNormalOffsets[currDepth] = new VertexNormal(normals2);
                                    }
                                    offsets1.x += normals2.x;
                                    offsets1.y += normals2.y;
                                    offsets1.z += normals2.z;
                                    offsets1.magnitude += normals2.magnitude;
                                    offsets2.x += normals.x;
                                    offsets2.y += normals.y;
                                    offsets2.z += normals.z;
                                    offsets2.magnitude += normals.magnitude;
                                    cycleCount++;
                                    this.renderCycleByVertex[verticeIndex] = renderCycle;
                                    this.renderCycleByDepth[currDepth] = renderCycle;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (cycleCount >= 3 && renderTypeAdjustment) {
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                if (this.renderCycleByVertex[this.facesA[faceIndex]] == renderCycle && this.renderCycleByVertex[this.facesB[faceIndex]] == renderCycle && this.renderCycleByVertex[this.facesC[faceIndex]] == renderCycle) {
                    if (this.faceRenderType == null) {
                        this.faceRenderType = new byte[this.numFaces];
                    }
                    this.faceRenderType[faceIndex] = 2;
                }
            }
            for (int faceIndex = 0; faceIndex < safeModel.numFaces; faceIndex++) {
                if (this.renderCycleByDepth[safeModel.facesA[faceIndex]] == renderCycle && this.renderCycleByDepth[safeModel.facesB[faceIndex]] == renderCycle && this.renderCycleByDepth[safeModel.facesC[faceIndex]] == renderCycle) {
                    if (safeModel.faceRenderType == null) {
                        safeModel.faceRenderType = new byte[safeModel.numFaces];
                    }
                    safeModel.faceRenderType[faceIndex] = 2;
                }
            }
        }
    }

    @Override
    public boolean startAnimation() {
        if (this.vertexGroups == null) {
            return false;
        } 
        this.originX = 0;
        this.originY = 0;
        this.originZ = 0;
        return true;        
    }

    public void draw(boolean colorWrite, boolean depthWrite, boolean transparent, boolean offscreen, boolean allowReduce) {
        if (this.billboardEffects != null) {
            BillboardEffect effect;
            for (int billboardIndex = 0; billboardIndex < this.numBillboards; this.billboardOnFace[effect.appliesOnFace] = billboardIndex++) {
                effect = this.billboardEffects[billboardIndex];
            }
        }
        if (!this.accountForPriorities && this.billboardEffects == null) {
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce);
            }
        } else if ((this.functionMask & ALTERS_ALPHA) == 0 && this.aShortArray8623 != null) {
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                short face = this.aShortArray8623[faceIndex];
                translateToScreen(colorWrite, depthWrite, transparent, face, offscreen, allowReduce);
            }
        } else {
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                if (!hasAlpha(faceIndex) && !hasBillboardOnFace(faceIndex)) {
                    translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce);
                }
            }
            if (this.facesLayer == null) {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    if (hasAlpha(faceIndex) || hasBillboardOnFace(faceIndex)) {
                        translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce);
                    }
                }
            } else {
                for (int priorityType = 0; priorityType < 12; priorityType++) {
                    for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                        if (priorityType == this.facesLayer[faceIndex] && (hasAlpha(faceIndex) || hasBillboardOnFace(faceIndex))) {
                            translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce);
                        }
                    }
                }
            }
        }
    }

    public void reduce(boolean colorWrite, boolean depthWrite, boolean transparent, int faceIndex) {
        if (!this.threadResource.hasWaterUpdate) {
        	//System.out.println("<A>");
            int a = this.facesA[faceIndex];
            int b = this.facesB[faceIndex];
            int c = this.facesC[faceIndex];
            Matrix44 matrix448 = this.toolkit.modelMatrix;
            float f_9 = matrix448.faceList[10];
            float f_10 = matrix448.faceList[14];
            float f_11 = matrix448.faceList[11];
            float f_12 = matrix448.faceList[15];
            float f_13 = f_10 * f_11 - f_9 * f_12;
            float f_14 = (this.projectedVerticesZ[a] - this.threadResource.aFloat2335) * this.threadResource.aFloat2308;
            float f_15 = (this.projectedVerticesZ[b] - this.threadResource.aFloat2335) * this.threadResource.aFloat2308;
            float f_16 = (this.projectedVerticesZ[c] - this.threadResource.aFloat2335) * this.threadResource.aFloat2308;
            float f_17 = f_14 * f_13 / (f_14 * f_11 - f_9);
            float f_18 = f_15 * f_13 / (f_15 * f_11 - f_9);
            float f_19 = f_16 * f_13 / (f_16 * f_11 - f_9);
            float mixAlphaA = (f_17 - this.threadResource.aFloat2305) / this.threadResource.aFloat2303;
            if (mixAlphaA > 1.0F) {
                mixAlphaA = 1.0F;
            } else if (mixAlphaA < 0.0F) {
                mixAlphaA = 0.0F;
            }
            float mixAlphaB = (f_18 - this.threadResource.aFloat2305) / this.threadResource.aFloat2303;
            if (mixAlphaB > 1.0F) {
                mixAlphaB = 1.0F;
            } else if (mixAlphaB < 0.0F) {
                mixAlphaB = 0.0F;
            }
            float mixAlphaC = (f_19 - this.threadResource.aFloat2305) / this.threadResource.aFloat2303;
            if (mixAlphaC > 1.0F) {
                mixAlphaC = 1.0F;
            } else if (mixAlphaC < 0.0F) {
                mixAlphaC = 0.0F;
            }
            float totalAlpha = mixAlphaA + mixAlphaB + mixAlphaC;
            if (totalAlpha < 3.0F) {
                if (totalAlpha <= 0.0F) {
                    renderFace(colorWrite, depthWrite, transparent, faceIndex);
                } else {
                    if (this.facesAlpha == null) {
                        this.rasterizer.blendingAlpha = 0;
                    } else {
                        this.rasterizer.blendingAlpha = this.facesAlpha[faceIndex] & 0xff;
                    }
                    if (this.facesMaterial != null && this.facesMaterial[faceIndex] != -1) {
                        int transparency = ~0xffffff;
                        if (this.facesAlpha != null) {
                            transparency = 255 - (this.facesAlpha[faceIndex] & 0xff) << 24;
                        }
                        if (this.facesColourC[faceIndex] == -1) {
                            int faceAColor = transparency | this.facesColourA[faceIndex] & 0xffffff;
                            this.rasterizer.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], this.projectedVerticesZ2[a], this.projectedVerticesZ2[b], this.projectedVerticesZ2[c], this.uMap[faceIndex][0], this.uMap[faceIndex][1], this.uMap[faceIndex][2], this.vMap[faceIndex][0], this.vMap[faceIndex][1], this.vMap[faceIndex][2], faceAColor, faceAColor, faceAColor, this.threadResource.fogColor, mixAlphaA * 255.0F, mixAlphaB * 255.0F, mixAlphaC * 255.0F, this.facesMaterial[faceIndex]);
                        } else {
                            this.rasterizer.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], this.projectedVerticesZ2[a], this.projectedVerticesZ2[b], this.projectedVerticesZ2[c], this.uMap[faceIndex][0], this.uMap[faceIndex][1], this.uMap[faceIndex][2], this.vMap[faceIndex][0], this.vMap[faceIndex][1], this.vMap[faceIndex][2], transparency | this.facesColourA[faceIndex] & 0xffffff, transparency | this.facesColourB[faceIndex] & 0xffffff, transparency | this.facesColourC[faceIndex] & 0xffffff, this.threadResource.fogColor, mixAlphaA * 255.0F, mixAlphaB * 255.0F, mixAlphaC * 255.0F, this.facesMaterial[faceIndex]);
                        }
                    } else if (this.facesColourC[faceIndex] == -1) {
                        this.rasterizer.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, mixAlphaA * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, mixAlphaB * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, mixAlphaC * 255.0F));
                    } else {
                        this.rasterizer.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, mixAlphaA * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourB[faceIndex] & 0xffff], this.threadResource.fogColor, mixAlphaB * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourC[faceIndex] & 0xffff], this.threadResource.fogColor, mixAlphaC * 255.0F));
                    }
                }
            }
        } else {
        	//System.out.println("<B>");
        	int a = this.facesA[faceIndex];
        	int b = this.facesB[faceIndex];
        	int c = this.facesC[faceIndex];
            float waterAlphaMultiplierA = 0.0F;
            float waterAlphaMultiplierB = 0.0F;
            float waterAlphaMultiplierC = 0.0F;
            if (this.waterRenderHeight[a] > this.threadResource.waterScale) {
                waterAlphaMultiplierA = 1.0F;
            } else if (this.waterRenderHeight[a] > this.threadResource.waterAverageHeight) {
                waterAlphaMultiplierA = (this.threadResource.waterAverageHeight - this.waterRenderHeight[a]) * 1.0F / (this.threadResource.waterAverageHeight - this.threadResource.waterScale);
            }
            if (this.waterRenderHeight[b] > this.threadResource.waterScale) {
                waterAlphaMultiplierB = 1.0F;
            } else if (this.waterRenderHeight[b] > this.threadResource.waterAverageHeight) {
                waterAlphaMultiplierB = (this.threadResource.waterAverageHeight - this.waterRenderHeight[b]) * 1.0F / (this.threadResource.waterAverageHeight - this.threadResource.waterScale);
            }
            if (this.waterRenderHeight[c] > this.threadResource.waterScale) {
                waterAlphaMultiplierC = 1.0F;
            } else if (this.waterRenderHeight[c] > this.threadResource.waterAverageHeight) {
                waterAlphaMultiplierC = (this.threadResource.waterAverageHeight - this.waterRenderHeight[c]) * 1.0F / (this.threadResource.waterAverageHeight - this.threadResource.waterScale);
            }
            if (this.facesAlpha == null) {
                this.rasterizer.blendingAlpha = 0;
            } else {
                this.rasterizer.blendingAlpha = this.facesAlpha[faceIndex] & 0xff;
            }
            if (this.facesMaterial != null && this.facesMaterial[faceIndex] != -1) {
                int transparency = ~0xffffff;
                if (this.facesAlpha != null) {
                    transparency = 255 - (this.facesAlpha[faceIndex] & 0xff) << 24;
                }
                if (this.facesColourC[faceIndex] == -1) {
                    int finalFaceAColor = transparency | this.facesColourA[faceIndex] & 0xffffff;
                    this.rasterizer.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], this.projectedVerticesZ2[a], this.projectedVerticesZ2[b], this.projectedVerticesZ2[c], this.uMap[faceIndex][0], this.uMap[faceIndex][1], this.uMap[faceIndex][2], this.vMap[faceIndex][0], this.vMap[faceIndex][1], this.vMap[faceIndex][2], finalFaceAColor, finalFaceAColor, finalFaceAColor, this.threadResource.fogColor, waterAlphaMultiplierA * 255.0F, waterAlphaMultiplierB * 255.0F, waterAlphaMultiplierC * 255.0F, this.facesMaterial[faceIndex]);
                } else {
                    this.rasterizer.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], this.projectedVerticesZ2[a], this.projectedVerticesZ2[b], this.projectedVerticesZ2[c], this.uMap[faceIndex][0], this.uMap[faceIndex][1], this.uMap[faceIndex][2], this.vMap[faceIndex][0], this.vMap[faceIndex][1], this.vMap[faceIndex][2], transparency | this.facesColourA[faceIndex] & 0xffffff, transparency | this.facesColourB[faceIndex] & 0xffffff, transparency | this.facesColourC[faceIndex] & 0xffffff, this.threadResource.fogColor, waterAlphaMultiplierA * 255.0F, waterAlphaMultiplierB * 255.0F, waterAlphaMultiplierC * 255.0F, this.facesMaterial[faceIndex]);
                }
            } else if (this.facesColourC[faceIndex] == -1) {
                this.rasterizer.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, waterAlphaMultiplierA * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, waterAlphaMultiplierB * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, waterAlphaMultiplierC * 255.0F));
            } else {
                this.rasterizer.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c], this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c], this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff], this.threadResource.fogColor, waterAlphaMultiplierA * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourB[faceIndex] & 0xffff], this.threadResource.fogColor, waterAlphaMultiplierB * 255.0F), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable[this.facesColourC[faceIndex] & 0xffff], this.threadResource.fogColor, waterAlphaMultiplierC * 255.0F));
            }
        }

    }

    @Override
    public boolean calculateProjection(int camX, int camY, Matrix43 modelMatrix, boolean notRendered, int sizeShift) {
        copyDependencies(this.toolkit.getThreadResource(Thread.currentThread()));
        Matrix44 particleMatrix = this.threadResource.particleMatrix;
        particleMatrix.fromMatrix(modelMatrix);
        Matrix44 renderMatrix = this.threadResource.renderingMatrix;
        renderMatrix.copyFrom(particleMatrix);
        renderMatrix.multiplyBy(this.toolkit.floorMatrix);
        boolean onScreen = false;
        int left = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        int modelCenterX = this.maxX - this.minX >> 1;
        int modelCenterY = this.maxY - this.minY >> 1;
        int modelCenterZ = this.maxZ - this.minZ >> 1;
        int minXOffset = modelCenterX + this.minX;
        int minYOffset = modelCenterY + this.minY;
        int minZOffset = modelCenterZ + this.minZ;
        int xA = minXOffset - (modelCenterX << sizeShift);
        int yA = minYOffset - (modelCenterY << sizeShift);
        int zA = minZOffset - (modelCenterZ << sizeShift);
        int xB = minXOffset + (modelCenterX << sizeShift);
        int yB = minYOffset + (modelCenterY << sizeShift);
        int zC = minZOffset + (modelCenterZ << sizeShift);
        this.matrixX[0] = xA;
        this.matrixY[0] = yA;
        this.matrixZ[0] = zA;
        this.matrixX[1] = xB;
        this.matrixY[1] = yA;
        this.matrixZ[1] = zA;
        this.matrixX[2] = xA;
        this.matrixY[2] = yB;
        this.matrixZ[2] = zA;
        this.matrixX[3] = xB;
        this.matrixY[3] = yB;
        this.matrixZ[3] = zA;
        this.matrixX[4] = xA;
        this.matrixY[4] = yA;
        this.matrixZ[4] = zC;
        this.matrixX[5] = xB;
        this.matrixY[5] = yA;
        this.matrixZ[5] = zC;
        this.matrixX[6] = xA;
        this.matrixY[6] = yB;
        this.matrixZ[6] = zC;
        this.matrixX[7] = xB;
        this.matrixY[7] = yB;
        this.matrixZ[7] = zC;
        for (int matrixIndex = 0; matrixIndex < 8; matrixIndex++) {
        	int x = this.matrixX[matrixIndex];
        	int y = this.matrixY[matrixIndex];
        	int z = this.matrixZ[matrixIndex];
        	float depthA = renderMatrix.faceList[2] * x + renderMatrix.faceList[6] * y + renderMatrix.faceList[10] * z + renderMatrix.faceList[14];
        	float depthB = renderMatrix.faceList[3] * x + renderMatrix.faceList[7] * y + renderMatrix.faceList[11] * z + renderMatrix.faceList[15];
            if (depthA >= -depthB) {
            	float f_31 = renderMatrix.faceList[0] * x + renderMatrix.faceList[4] * y + renderMatrix.faceList[8] * z + renderMatrix.faceList[12];
            	float f_32 = renderMatrix.faceList[1] * x + renderMatrix.faceList[5] * y + renderMatrix.faceList[9] * z + renderMatrix.faceList[13];
                int screenPosX = (int) (this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * f_31 / depthB);
                int screenPosY = (int) (this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * f_32 / depthB);
                if (screenPosX < left) {
                    left = screenPosX;
                }
                if (screenPosX > right) {
                    right = screenPosX;
                }
                if (screenPosY < top) {
                    top = screenPosY;
                }
                if (screenPosY > bottom) {
                    bottom = screenPosY;
                }
                onScreen = true;
            }
        }
        if (onScreen && camX > left && camX < right && camY > top && camY < bottom) {
            if (notRendered) {
                return true;
            }
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
            	int x = this.verticesX[verticeIndex];
            	int y = this.verticesY[verticeIndex];
            	int z = this.verticesZ[verticeIndex];
            	float depthA = renderMatrix.faceList[2] * x + renderMatrix.faceList[6] * y + renderMatrix.faceList[10] * z + renderMatrix.faceList[14];
            	float depthB = renderMatrix.faceList[3] * x + renderMatrix.faceList[7] * y + renderMatrix.faceList[11] * z + renderMatrix.faceList[15];
                if (depthA >= -depthB) {
                	float multiplierX = renderMatrix.faceList[0] * x + renderMatrix.faceList[4] * y + renderMatrix.faceList[8] * z + renderMatrix.faceList[12];
                	float multiplierY = renderMatrix.faceList[1] * x + renderMatrix.faceList[5] * y + renderMatrix.faceList[9] * z + renderMatrix.faceList[13];
                    this.projectedVerticesX[verticeIndex] = ((int) (this.toolkit.aFloat9003 + this.toolkit.aFloat8976 * multiplierX / depthB));
                    this.projectedVerticesY[verticeIndex] = ((int) (this.toolkit.aFloat8982 + this.toolkit.aFloat9004 * multiplierY / depthB));
                } else {
                    this.projectedVerticesX[verticeIndex] = -999999.0f;
                }
            }
            for (int face = 0; face < this.numFaces; face++) {
                if (this.projectedVerticesX[this.facesA[face]] != -999999.0f && this.projectedVerticesX[this.facesB[face]] != -999999.0f && this.projectedVerticesX[this.facesC[face]] != -999999.0f && isHovering(camX, camY, this.projectedVerticesY[this.facesA[face]], this.projectedVerticesY[this.facesB[face]], this.projectedVerticesY[this.facesC[face]], this.projectedVerticesX[this.facesA[face]], this.projectedVerticesX[this.facesB[face]], this.projectedVerticesX[this.facesC[face]])) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isHovering(int i_1, int i_2, float f_3, float f_4, float f_5, float f_6, float f_7, float f_8) {
        return (!(i_2 < f_3) || !(i_2 < f_4) || !(i_2 < f_5)) && ((!(i_2 > f_3) || !(i_2 > f_4) || !(i_2 > f_5)) && ((!(i_1 < f_6) || !(i_1 < f_7) || !(i_1 < f_8)) && (i_1 <= f_6 || i_1 <= f_7 || i_1 <= f_8)));
    }

    @Override
    public void awaitLock() {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized (this) {
                while (super.isLocked) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    	// NOOP
                    }
                }
                super.isLocked = true;
            }
        }
    }

    @Override
    public int getSize2d() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.size2d;
    }

    @Override
    public int getSize3d() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.size3d;
    }

    @Override
    public int getMinX() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.minX;
    }

    @Override
    public int getMaxY() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.maxY;
    }

    @Override
    public int getMinZ() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.minZ;
    }

    @Override
    public void setAmbient(int ambient) {
        if ((this.functionMask & ALTERS_AMBIENT) != 4096) {
            throw new IllegalStateException();
        } else {
            this.ambient = ambient;
            this.appearanceUpdateStage = 0;
        }
    }

    @Override
    public int getContrast() {
        return this.contrast;
    }

    @Override
    public void recolor(short src, short dst) {
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            if (this.facesColor[faceIndex] == src) {
                this.facesColor[faceIndex] = dst;
            }
        }
        if (this.billboardEffects != null) {
            for (int index = 0; index < this.numBillboards; index++) {
                BillboardEffect effect = this.billboardEffects[index];
                BillboardSurface surface = this.billboardSurfaces[index];
                surface.diffuseColor = surface.diffuseColor & ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(this.facesColor[effect.appliesOnFace]) & 0xffff] & 0xffffff;
            }
        }
        if (this.appearanceUpdateStage == 2) {
            this.appearanceUpdateStage = 1;
        }
    }

    @Override
    public void applyEnvironmentAlpha(byte alpha, byte[] transparency) {
        if ((this.functionMask & ALTERS_LIGHT_ALPHA) == 0) {
            throw new RuntimeException();
        } 
        if (this.facesAlpha == null) {
        	this.facesAlpha = new byte[this.numFaces];
        }
        if (transparency == null) {
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            	this.facesAlpha[faceIndex] = alpha;
            }
        } else {
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                int replacementAlpha = 255 - (255 - (alpha & 0xff)) * (255 - (transparency[faceIndex] & 0xff)) / 255;
                this.facesAlpha[faceIndex] = (byte) replacementAlpha;
            }
        }
        if (this.appearanceUpdateStage == 2) {
        	this.appearanceUpdateStage = 1;
        }
    }


    @Override
    public void tint(int hue, int saturation, int lightness, int opacity) {
        if ((this.functionMask & TINT) != 524288) {
            throw new IllegalStateException();
        } 
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            int hsl = this.facesColor[faceIndex] & 0xffff;
            int h = hsl >> 10 & 0x3f;
            int s = hsl >> 7 & 0x7;
            int l = hsl & 0x7f;
            if (hue != -1) {
                h += opacity * (hue - h) >> 7;
            }
            if (saturation != -1) {
                s += opacity * (saturation - s) >> 7;
            }
            if (lightness != -1) {
                l += opacity * (lightness - l) >> 7;
            }
            this.facesColor[faceIndex] = (short) (h << 10 | s << 7 | l);
        }
        if (this.billboardEffects != null) {
            for (int faceIndex = 0; faceIndex < this.numBillboards; faceIndex++) {
                BillboardEffect effect = this.billboardEffects[faceIndex];
                BillboardSurface surface = this.billboardSurfaces[faceIndex];
                surface.diffuseColor = surface.diffuseColor & ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(this.facesColor[effect.appliesOnFace] & 0xffff) & 0xffff] & 0xffffff;
            }
        }
        if (this.appearanceUpdateStage == 2) {
            this.appearanceUpdateStage = 1;
        } 
    }

    @Override
    public EmissiveTriangle[] getEmissiveTriangles() {
        return this.particleEmitters;
    }

    @Override
    public EffectiveVertex[] getEffectiveVertices() {
        return this.effectors;
    }

    @Override
    public boolean accountForLayers() {
        return this.accountForPriorities;
    }

    @Override
    public boolean hasAnimatedTexture() {
        return this.textureAnimated;
    }
    
    @Override
    public byte[] getAlpha() {
        return this.facesAlpha;
    }

    @Override
    public void applyShading(int replacingFunctionMask) {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized (this) {
                if ((this.functionMask & SHADED) == 65536 && (replacingFunctionMask & SHADED) == 0) {
                    appearanceUpdateThreaded(true);
                }
                this.functionMask = replacingFunctionMask;
            }
        } else {
            if ((this.functionMask & SHADED) == 65536 && (replacingFunctionMask & SHADED) == 0) {
                appearanceUpdateThreaded(true);
            }
            this.functionMask = replacingFunctionMask;
        }
    }
    
    public void renderFace(boolean colorWrite, boolean depthWrite, boolean transparent, int faceIndex) {
        short a = this.facesA[faceIndex];
        short b = this.facesB[faceIndex];
        short c = this.facesC[faceIndex];
        if (this.facesMaterial != null && this.facesMaterial[faceIndex] != -1) {
            int alpha = ~0xffffff;
            if (this.facesAlpha != null) {
                alpha = 255 - (this.facesAlpha[faceIndex] & 0xff) << 24;
            }
            if (this.facesColourC[faceIndex] == -1) {
                int faceAColor = alpha | this.facesColourA[faceIndex] & 0xffffff;
                this.rasterizer.drawMaterializedTriangle(colorWrite, depthWrite, transparent,
                		this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c],
                		this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c],
                		this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c],
                		this.projectedVerticesZ2[a], this.projectedVerticesZ2[b], this.projectedVerticesZ2[c],
                		this.uMap[faceIndex][0], this.uMap[faceIndex][1], this.uMap[faceIndex][2],
                		this.vMap[faceIndex][0], this.vMap[faceIndex][1], this.vMap[faceIndex][2],
                        faceAColor, faceAColor, faceAColor, this.threadResource.fogColor,
                		0.0F, 0.0F, 0.0F, this.facesMaterial[faceIndex]);
            } else { //<== most things
                this.rasterizer.drawMaterializedTriangle(colorWrite, depthWrite, transparent,
                		this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c],
                		this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c],
                		this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c],
                		this.projectedVerticesZ2[a], this.projectedVerticesZ2[b], this.projectedVerticesZ2[c],
                		this.uMap[faceIndex][0], this.uMap[faceIndex][1], this.uMap[faceIndex][2],
                		this.vMap[faceIndex][0], this.vMap[faceIndex][1], this.vMap[faceIndex][2],
                		alpha | this.facesColourA[faceIndex] & 0xffffff,
                		alpha | this.facesColourB[faceIndex] & 0xffffff,
                		alpha | this.facesColourC[faceIndex] & 0xffffff,
                		this.threadResource.fogColor, 0.0F, 0.0F, 0.0F, this.facesMaterial[faceIndex]);
            }
        } else {
            if (this.facesAlpha == null) {
                this.rasterizer.blendingAlpha = 0;
            } else {
                this.rasterizer.blendingAlpha = this.facesAlpha[faceIndex] & 0xff;
            }
            if (this.facesColourC[faceIndex] == -1) {
                this.rasterizer.drawFlatTriangle(colorWrite, depthWrite, transparent,
                		this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c],
                		this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c],
                		this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c],
                		ColorUtil.hsvToRgbTable[this.facesColourA[faceIndex] & 0xffff]);
            } else {
                this.rasterizer.drawShadedTriangle(colorWrite, depthWrite, transparent,
                		this.projectedVerticesY[a], this.projectedVerticesY[b], this.projectedVerticesY[c],
                		this.projectedVerticesX[a], this.projectedVerticesX[b], this.projectedVerticesX[c],
                		this.projectedVerticesZ[a], this.projectedVerticesZ[b], this.projectedVerticesZ[c],
                		this.facesColourA[faceIndex] & 0xffff,
                		this.facesColourB[faceIndex] & 0xffff,
                		this.facesColourC[faceIndex] & 0xffff);
            }
        }
    }

    @Override
    public int getMinY() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.minY;
    }

    public void rotate90DegWithNormals() {
        synchronized (this) {
            for (int currentDepth = 0; currentDepth < this.maxDepth; currentDepth++) {
                int tmpX = this.verticesX[currentDepth];
                this.verticesX[currentDepth] = this.verticesZ[currentDepth];
                this.verticesZ[currentDepth] = -tmpX;
                if (this.vertexNormals[currentDepth] != null) {
                    tmpX = this.vertexNormals[currentDepth].x;
                    this.vertexNormals[currentDepth].x = this.vertexNormals[currentDepth].z;
                    this.vertexNormals[currentDepth].z = -tmpX;
                }
            }
            if (this.facesNormal != null) {
                for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                    if (this.facesNormal[faceIndex] != null) {
                        int tmpX = this.facesNormal[faceIndex].x;
                        this.facesNormal[faceIndex].x = this.facesNormal[faceIndex].z;
                        this.facesNormal[faceIndex].z = -tmpX;
                    }
                }
            }
            for (int currentDepth = this.maxDepth; currentDepth < this.numVertices; currentDepth++) {
                int tmpX = this.verticesX[currentDepth];
                this.verticesX[currentDepth] = this.verticesZ[currentDepth];
                this.verticesZ[currentDepth] = -tmpX;
            }
            this.appearanceUpdateStage = 0;
            this.boundsUpdated = false;
        }
    }

    @Override
    public Shadow createShadowSprite(Shadow shadow) {
        return null;
    }

    public void appearanceUpdateThreaded(boolean resetRenderType) {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized (this) {
                appearanceUpdate(resetRenderType);
            }
        } else {
            appearanceUpdate(resetRenderType);
        }
    }

    @Override
    public void translate(int x, int y, int z) {
        if (x != 0 && (this.functionMask & DISPLACE_X) != 1) {
            throw new IllegalStateException();
        } else if (y != 0 && (this.functionMask & DISPLACE_Y) != 2) {
            throw new IllegalStateException();
        } else if (z != 0 && (this.functionMask & DISPLACE_Z) != 4) {
            throw new IllegalStateException();
        } 
        synchronized (this) {
            for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
                this.verticesX[verticeIndex] += x;
                this.verticesY[verticeIndex] += y;
                this.verticesZ[verticeIndex] += z;
            }
        }       
    }

    public int mixLightness(int i_1, int i_21) {
        i_21 = (i_1 & 0x7f) * i_21 >> 7;
        if (i_21 < 2) {
            i_21 = 2;
        } else if (i_21 > 126) {
            i_21 = 126;
        }
        return i_21 + (i_1 & 0xff80);
    }
 
    @Override
    public void offsetSubmesh(Matrix43 matrix44var1, int submeshFlags, boolean updateIdentityMatrix) {
        if (this.verticesSubmeshes != null) {
            ThreadResourceSM resource = this.toolkit.getThreadResource(Thread.currentThread());
            Matrix43 matrix44var5 = resource.aClass2942316;
            matrix44var5.copyFrom(matrix44var1);
            if (updateIdentityMatrix) {
                matrix44var5.updateIdentitiyMatrix();
            }
            Matrix44 submeshMatrix = resource.particleMatrix;
            submeshMatrix.fromMatrix(matrix44var5);
            float[] matrix = new float[3];
            for (int currentDepth = 0; currentDepth < this.maxDepth; currentDepth++) {
                if ((submeshFlags & this.verticesSubmeshes[currentDepth]) != 0) {
                    submeshMatrix.scale(this.verticesX[currentDepth], this.verticesY[currentDepth], this.verticesZ[currentDepth], matrix);
                    this.verticesX[currentDepth] = (int) matrix[0];
                    this.verticesY[currentDepth] = (int) matrix[1];
                    this.verticesZ[currentDepth] = (int) matrix[2];
                }
            }
        }

    }

    @Override
    public int getMaxX() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        return this.maxX;
    }

    @Override
    public void contourToGround(int contourType, int contourModifier, Ground currentLevel, Ground nextLevel, int sceneX, int averageHeight, int sceneY) {
        if (contourType == 3) {
            if ((this.functionMask & FULL_DISPLACEMENT) != 7) {
                throw new IllegalStateException();
            }
        } else if ((this.functionMask & DISPLACE_Y) != 2) {
            throw new IllegalStateException();
        }
        if (!this.boundsUpdated) {
            calculateBounds();
        }
        int lowX = sceneX + this.minX;
        int highX = sceneX + this.maxX;
        int lowY = sceneY + this.minZ;
        int highY = sceneY + this.maxZ;
        if (contourType == 4 || lowX >= 0 && highX + currentLevel.tileUnits >> currentLevel.tileScale < currentLevel.width && lowY >= 0 && highY + currentLevel.tileUnits >> currentLevel.tileScale < currentLevel.length) {
            int[][] currentHeights = currentLevel.tileHeights;
            int[][] nextHeights = null;
            if (nextLevel != null) {
                nextHeights = nextLevel.tileHeights;
            }
            if (contourType != 4 && contourType != 5) {
                lowX >>= currentLevel.tileScale;
                highX = highX + (currentLevel.tileUnits - 1) >> currentLevel.tileScale;
                lowY >>= currentLevel.tileScale;
                highY = highY + (currentLevel.tileUnits - 1) >> currentLevel.tileScale;
                if (currentHeights[lowX][lowY] == averageHeight && currentHeights[highX][lowY] == averageHeight && currentHeights[lowX][highY] == averageHeight && currentHeights[highX][highY] == averageHeight) {
                    return;
                }
            } else if (nextLevel == null || lowX < 0 || nextLevel.tileUnits + highX >> nextLevel.tileScale >= nextLevel.width || lowY < 0 || nextLevel.tileUnits + highY >> nextLevel.tileScale >= nextLevel.length) {
                return;
            }
            synchronized (this) {
                if (contourType == 1) {
                	int tileMask = currentLevel.tileUnits - 1;
                    for (int v = 0; v < this.maxDepth; v++) {
                    	int x = sceneX + this.verticesX[v];
                    	int z = sceneY + this.verticesZ[v];
                    	int remainderX = x & tileMask;
                    	int remainderZ = z & tileMask;
                    	int tileX = x >> currentLevel.tileScale;
                        int tileZ = z >> currentLevel.tileScale;
                        int height1 = currentHeights[tileX][tileZ] * (currentLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] >> currentLevel.tileScale;
                        int height2 = (currentLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] >> currentLevel.tileScale;
                        int interpolatedHeight = height1 * (currentLevel.tileUnits - remainderZ) + height2 * remainderZ >> currentLevel.tileScale;
                        this.verticesY[v] = interpolatedHeight + this.verticesY[v] - averageHeight;
                    }

                    for (int v = this.maxDepth; v < this.numVertices; v++) {
                    	int x = sceneX + this.verticesX[v];
                    	int z = sceneY + this.verticesZ[v];
                    	int remainderX = x & tileMask;
                    	int remainderZ = z & tileMask;
                        int tileX = x >> currentLevel.tileScale;
                        int tileZ = z >> currentLevel.tileScale;
                        if (tileX >= 0 && tileX < currentHeights.length - 1 && tileZ >= 0 && tileZ < currentHeights[0].length - 1) {
                        	int i_23 = currentHeights[tileX][tileZ] * (currentLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] >> currentLevel.tileScale;
                            int i_24 = (currentLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] >> currentLevel.tileScale;
                            int i_25 = i_23 * (currentLevel.tileUnits - remainderZ) + i_24 * remainderZ >> currentLevel.tileScale;
                            this.verticesY[v] = i_25 + this.verticesY[v] - averageHeight;
                        }
                    }
                } else {
                    if (contourType == 2) {
                        if (this.minY == 0) {
                            return;
                        }
                        int tileMask = currentLevel.tileUnits - 1;
                        for (int v = 0; v < this.maxDepth; v++) {
                        	int i_17 = (this.verticesY[v] << 16) / this.minY;
                            if (i_17 < contourModifier) {
                            	int i_18 = sceneX + this.verticesX[v];
                            	int i_19 = sceneY + this.verticesZ[v];
                            	int i_20 = i_18 & tileMask;
                            	int i_21 = i_19 & tileMask;
                            	int i_22 = i_18 >> currentLevel.tileScale;
                                int i_23 = i_19 >> currentLevel.tileScale;
                                int i_24 = currentHeights[i_22][i_23] * (currentLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] >> currentLevel.tileScale;
                                int i_25 = (currentLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] >> currentLevel.tileScale;
                                int i_26 = i_24 * (currentLevel.tileUnits - i_21) + i_25 * i_21 >> currentLevel.tileScale;
                                this.verticesY[v] += (i_26 - averageHeight) * (contourModifier - i_17) / contourModifier;
                            } else {
                                this.verticesY[v] = this.verticesY[v];
                            }
                        }
                        for (int v = this.maxDepth; v < this.numVertices; v++) {
                        	int i_17 = (this.verticesY[v] << 16) / this.minY;
                            if (i_17 < contourModifier) {
                            	int i_18 = sceneX + this.verticesX[v];
                            	int i_19 = sceneY + this.verticesZ[v];
                            	int i_20 = i_18 & tileMask;
                            	int i_21 = i_19 & tileMask;
                            	int i_22 = i_18 >> currentLevel.tileScale;
                                int i_23 = i_19 >> currentLevel.tileScale;
                                if (i_22 >= 0 && i_22 < currentLevel.width - 1 && i_23 >= 0 && i_23 < currentLevel.length - 1) {
                                	int i_24 = currentHeights[i_22][i_23] * (currentLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] >> currentLevel.tileScale;
                                    int i_25 = (currentLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] >> currentLevel.tileScale;
                                    int i_26 = i_24 * (currentLevel.tileUnits - i_21) + i_25 * i_21 >> currentLevel.tileScale;
                                    this.verticesY[v] += (i_26 - averageHeight) * (contourModifier - i_17) / contourModifier;
                                }
                            } else {
                                this.verticesY[v] = this.verticesY[v];
                            }
                        }
                    } else if (contourType == 3) {
                    	int iiii = (contourModifier & 0xff) * 4;
                    	int iiii2 = (contourModifier >> 8 & 0xff) * 4;
                    	int i_17 = (contourModifier >> 16 & 0xff) << 6;
                    	int i_18 = (contourModifier >> 24 & 0xff) << 6;
                        if (sceneX - (iiii >> 1) < 0 || sceneX + currentLevel.tileUnits + (iiii >> 1) >= currentLevel.width << currentLevel.tileScale || sceneY - (iiii2 >> 1) < 0 || currentLevel.tileUnits + (iiii2 >> 1) + sceneY >= currentLevel.length << currentLevel.tileScale) {
                            return;
                        }
                        hillChange(currentLevel, sceneX, averageHeight, sceneY, iiii, iiii2, i_17, i_18);
                    } else if (contourType == 4) {
                    	int tileMask = nextLevel.tileUnits - 1;
                    	int sizeY = this.maxY - this.minY;
                        for (int i_17 = 0; i_17 < this.maxDepth; i_17++) {
                        	int i_18 = sceneX + this.verticesX[i_17];
                        	int i_19 = sceneY + this.verticesZ[i_17];
                        	int i_20 = i_18 & tileMask;
                        	int i_21 = i_19 & tileMask;
                        	int i_22 = i_18 >> nextLevel.tileScale;
                            int i_23 = i_19 >> nextLevel.tileScale;
                            int i_24 = i_20 * nextHeights[i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) >> nextLevel.tileScale;
                            int i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] >> nextLevel.tileScale;
                            int i_26 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 >> nextLevel.tileScale;
                            this.verticesY[i_17] += sizeY + (i_26 - averageHeight);
                        }
                        for (int i_17 = this.maxDepth; i_17 < this.numVertices; i_17++) {
                        	int i_18 = sceneX + this.verticesX[i_17];
                        	int i_19 = sceneY + this.verticesZ[i_17];
                        	int i_20 = i_18 & tileMask;
                        	int i_21 = i_19 & tileMask;
                        	int i_22 = i_18 >> nextLevel.tileScale;
                            int i_23 = i_19 >> nextLevel.tileScale;
                            if (i_22 >= 0 && i_22 < nextLevel.width - 1 && i_23 >= 0 && i_23 < nextLevel.length - 1) {
                            	int i_24 = i_20 * nextHeights[i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) >> nextLevel.tileScale;
                                int i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] >> nextLevel.tileScale;
                                int i_26 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 >> nextLevel.tileScale;
                                this.verticesY[i_17] += sizeY + (i_26 - averageHeight);
                            }
                        }
                    } else if (contourType == 5) {
                    	int tileMask = nextLevel.tileUnits - 1;
                    	int sizeY = this.maxY - this.minY;
                        for (int i_17 = 0; i_17 < this.maxDepth; i_17++) {
                        	int i_18 = sceneX + this.verticesX[i_17];
                        	int i_19 = sceneY + this.verticesZ[i_17];
                        	int i_20 = i_18 & tileMask;
                        	int i_21 = i_19 & tileMask;
                        	int i_22 = i_18 >> currentLevel.tileScale;
                            int i_23 = i_19 >> currentLevel.tileScale;
                            int i_24 = currentHeights[i_22][i_23] * (currentLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] >> currentLevel.tileScale;
                            int i_25 = (currentLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] >> currentLevel.tileScale;
                            int i_26 = i_24 * (currentLevel.tileUnits - i_21) + i_25 * i_21 >> currentLevel.tileScale;
                            i_24 = i_20 * nextHeights[i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) >> nextLevel.tileScale;
                            i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] >> nextLevel.tileScale;
                            int i_27 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 >> nextLevel.tileScale;
                            int i_28 = i_26 - i_27 - contourModifier;
                            this.verticesY[i_17] = (i_28 * ((this.verticesY[i_17] << 8) / sizeY) >> 8) - (averageHeight - i_26);
                        }
                        for (int i_17 = this.maxDepth; i_17 < this.numVertices; i_17++) {
                        	int i_18 = sceneX + this.verticesX[i_17];
                        	int i_19 = sceneY + this.verticesZ[i_17];
                        	int i_20 = i_18 & tileMask;
                        	int i_21 = i_19 & tileMask;
                        	int i_22 = i_18 >> currentLevel.tileScale;
                            int i_23 = i_19 >> currentLevel.tileScale;
                            if (i_22 >= 0 && i_22 < currentLevel.width - 1 && i_22 < nextLevel.width - 1 && i_23 >= 0 && i_23 < currentLevel.length - 1 && i_23 < nextLevel.length - 1) {
                                int i_24 = currentHeights[i_22][i_23] * (currentLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] >> currentLevel.tileScale;
                                int i_25 = (currentLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] >> currentLevel.tileScale;
                            	int i_26 = i_24 * (currentLevel.tileUnits - i_21) + i_25 * i_21 >> currentLevel.tileScale;
                                i_24 = i_20 * nextHeights[i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) >> nextLevel.tileScale;
                                i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] >> nextLevel.tileScale;
                                int i_27 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 >> nextLevel.tileScale;
                                int i_28 = i_26 - i_27 - contourModifier;
                                this.verticesY[i_17] = (i_28 * ((this.verticesY[i_17] << 8) / sizeY) >> 8) - (averageHeight - i_26);
                            }
                        }
                    }
                }
                this.boundsUpdated = false;
            }
        }
    }

    /**
     * DOES RENDER: scene models
     * DOES NOT RENDER: particle effects, floor tiles, interfaces, inventory items
     */
    public void translateToScreen(boolean colorWrite, boolean depthWrite, boolean transparent, int faceIndex, boolean offscreen, boolean reduce) {
        if (this.facesColourC[faceIndex] != -2) {
            short a = this.facesA[faceIndex];
            short b = this.facesB[faceIndex];
            short c = this.facesC[faceIndex];
            float xA = this.projectedVerticesX[a];
            float xB = this.projectedVerticesX[b];
            float xC = this.projectedVerticesX[c];
            if (!offscreen || xA != -5000.0f && xB != -5000.0f && xC != -5000.0f) {
                float yA = this.projectedVerticesY[a];
                float yB = this.projectedVerticesY[b];
                float yC = this.projectedVerticesY[c];
                if (this.billboardOnFace[faceIndex] != -1 || (xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0F) {
                    this.rasterizer.clipEdges = !(xA >= 0.0F) || !(xB >= 0.0F) || !(xC >= 0.0F) || !(xA <= this.threadResource.clipWidth) || !(xB <= this.threadResource.clipWidth) || !(xC <= this.threadResource.clipWidth);
                    if (reduce) {
                        int billboard = this.billboardOnFace[faceIndex];
                        if (billboard == -1 || !this.billboardEffects[billboard].hasUid) {
                            reduce(colorWrite, depthWrite, transparent, faceIndex);
                        }
                    } else {
                        int billboard = this.billboardOnFace[faceIndex];
                        if (billboard != -1) {
                            BillboardEffect effect = this.billboardEffects[billboard];
                            BillboardSurface surface = this.billboardSurfaces[billboard];
                            if (!effect.hasUid) {
                                renderFace(colorWrite, depthWrite, transparent, faceIndex);
                            }
                            this.toolkit.drawBillboard(colorWrite, depthWrite, transparent, surface.x, surface.y, surface.z, surface.size2d, surface.size3d, effect.materialId & 0xffff, surface.diffuseColor, effect.blendType, effect.shape);
                        } else {
                            renderFace(colorWrite, depthWrite, transparent, faceIndex);
                        }
                    }
                }
            }
        }
    }

	@Override
	public void rematerial(short src, short dst) {
		if (this.facesMaterial != null) {
			if (!this.textureAnimated && dst >= 0) {
				MaterialType material = this.toolkit.materialProvider.getMaterial(dst & 0xffff);
				if (material.textureSpeedU != 0 || material.textureSpeedV != 0) {
					this.textureAnimated = true;
				}
			}
			for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
				if (this.facesMaterial[faceIndex] == src) {
					this.facesMaterial[faceIndex] = dst;
				}
			}
		}
	}

    @Override
    public int getMaxZ() {
        if (!this.boundsUpdated) {
            calculateBounds();
        }

        return this.maxZ;
    }

    @Override
    public int getAmbient() {
        return this.ambient;
    }

    public void calculateBounds() {
        if (!this.boundsUpdated) {
            int s2d = 0;
            int s3d = 0;
            int lowX = 32767;
            int lowY = 32767;
            int lowZ = 32767;
            int highX = -32768;
            int highY = -32768;
            int highZ = -32768;
            for (int verticeIndex = 0; verticeIndex < this.maxDepth; verticeIndex++) {
                int x = this.verticesX[verticeIndex];
                int y = this.verticesY[verticeIndex];
                int z = this.verticesZ[verticeIndex];
                if (x < lowX) {
                    lowX = x;
                }
                if (x > highX) {
                    highX = x;
                }
                if (y < lowY) {
                    lowY = y;
                }
                if (y > highY) {
                    highY = y;
                }
                if (z < lowZ) {
                    lowZ = z;
                }
                if (z > highZ) {
                    highZ = z;
                }
                int size = x * x + z * z;
                if (size > s2d) {
                    s2d = size;
                }
                size += y * y;
                if (size > s3d) {
                    s3d = size;
                }
            }
            this.minX = (short) lowX;
            this.maxX = (short) highX;
            this.minY = (short) lowY;
            this.maxY = (short) highY;
            this.minZ = (short) lowZ;
            this.maxZ = (short) highZ;
            this.size2d = (short) ((int) (Math.sqrt(s2d) + 0.99D));
            this.size3d = (short) ((int) (Math.sqrt(s3d) + 0.99D));
            this.boundsUpdated = true;
        }
    }

    public void calculateNormalsThreaded() {
        if (this.appearanceUpdateStage == 0 && this.vertexNormals == null) {
            if (this.toolkit.executingThreadCount > 1) {
                synchronized (this) {
                    calculateNormals();
                }
            } else {
                calculateNormals();
            }
        }
    }

    @Override
    public boolean areTexturesCached() {
        if (this.facesMaterial != null) {
            for (int materialIndex = 0; materialIndex < this.facesMaterial.length; materialIndex++) {
                if (this.facesMaterial[materialIndex] != -1 && !this.toolkit.isMaterialCached(this.facesMaterial[materialIndex])) {
                    return false;
                }
            }
        }
        return true;
    }
}
