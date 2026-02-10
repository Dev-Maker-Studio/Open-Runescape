package com.jagex.game.runetek5.graphics.model

import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.billboard.Billboard
import com.jagex.game.runetek5.graphics.model.billboard.BillboardEffect
import com.jagex.game.runetek5.graphics.model.billboard.BillboardSurface
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList
import com.jagex.game.runetek5.graphics.model.normal.FaceNormal
import com.jagex.game.runetek5.graphics.model.normal.VertexNormal
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.safemode.RasterizerSM
import com.jagex.game.runetek5.toolkit.safemode.ThreadResourceSM
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.sqrt

open class ModelSM : Model {

    @JvmField
    val toolkit: SafeModeToolkit

    @JvmField
    var rasterizer: RasterizerSM? = null

    @JvmField
    var threadResource: ThreadResourceSM? = null

    @JvmField
    var waterRenderHeight: IntArray? = null

    @JvmField
    var projectedVerticesX: FloatArray? = null

    @JvmField
    var projectedVerticesY: FloatArray? = null

    @JvmField
    var projectedVerticesZ: FloatArray? = null

    @JvmField
    var projectedVerticesZ2: FloatArray? = null

    @JvmField
    var matrixX: IntArray? = null

    @JvmField
    var matrixY: IntArray? = null

    @JvmField
    var matrixZ: IntArray? = null

    @JvmField
    var renderCycleByVertex: IntArray? = null

    @JvmField
    var renderCycleByDepth: IntArray? = null

    @JvmField
    var billboardOnFace: IntArray? = null

    @JvmField
    var contrast: Int = 0

    @JvmField
    var appearanceUpdateStage: Int = 0

    @JvmField
    var currentViewport: ThreadResourceSM? = null

    @JvmField
    var copyDstModels: Array<ModelSM?>? = null

    @JvmField
    var copySrcModels: Array<ModelSM?>? = null

    @JvmField
    var vertexNormals: Array<VertexNormal?>? = null

    @JvmField
    var facesNormal: Array<FaceNormal?>? = null

    @JvmField
    var facesColourA: IntArray? = null

    @JvmField
    var facesColourB: IntArray? = null

    @JvmField
    var facesColourC: IntArray? = null

    @JvmField
    var facesColor: ShortArray? = null

    @JvmField
    var faceRenderType: ByteArray? = null

    @JvmField
    var vertexNormalOffsets: Array<VertexNormal?>? = null

    @JvmField
    var boundsUpdated: Boolean = false

    @JvmField
    var maxX: Short = 0

    @JvmField
    var maxY: Short = 0

    @JvmField
    var maxZ: Short = 0

    @JvmField
    var minX: Short = 0

    @JvmField
    var minY: Short = 0

    @JvmField
    var minZ: Short = 0

    @JvmField
    var size2d: Short = 0

    @JvmField
    var size3d: Short = 0

    @JvmField
    var ambient: Int = 0

    @JvmField
    var numVertices: Int = 0

    @JvmField
    var maxDepth: Int = 0

    @JvmField
    var numFaces: Int = 0

    @JvmField
    var numBillboards: Int = 0

    @JvmField
    var accountForPriorities: Boolean = false

    @JvmField
    var textureAnimated: Boolean = false

    @JvmField
    var verticesX: IntArray? = null

    @JvmField
    var verticesY: IntArray? = null

    @JvmField
    var verticesZ: IntArray? = null

    @JvmField
    var facesAlpha: ByteArray? = null

    @JvmField
    var facesMaterial: ShortArray? = null

    @JvmField
    var billboardSurfaces: Array<BillboardSurface?>? = null

    @JvmField
    var uMap: Array<FloatArray?>? = null

    @JvmField
    var vMap: Array<FloatArray?>? = null

    @JvmField
    var vertexGroups: Array<IntArray?>? = null

    @JvmField
    var faceGroups: Array<IntArray?>? = null

    @JvmField
    var billboardGroups: Array<IntArray?>? = null

    @JvmField
    var verticesSubmeshes: ShortArray? = null

    @JvmField
    var facesSubmeshes: ShortArray? = null

    @JvmField
    var facesLayer: ByteArray? = null

    @JvmField
    var facesA: ShortArray? = null

    @JvmField
    var facesB: ShortArray? = null

    @JvmField
    var facesC: ShortArray? = null

    @JvmField
    var particleEmitters: Array<EmissiveTriangle>? = null

    @JvmField
    var effectors: Array<EffectiveVertex>? = null

    @JvmField
    var billboardEffects: Array<BillboardEffect?>? = null

    @JvmField
    var aShortArray8623: ShortArray? = null

    @JvmField
    var functionMask: Int = 0

    @JvmField
    var calculateColors: Boolean = false

    @JvmField
    var upscaled: Boolean = false

    @JvmField
    var originX: Int = 0

    @JvmField
    var originY: Int = 0

    @JvmField
    var originZ: Int = 0

    @JvmField
    var withinOrigin: Boolean = false

    @JvmField
    var renderLock: Boolean = false

    constructor(hardwarerenderer1: SafeModeToolkit) {
        this.toolkit = hardwarerenderer1
    }

    constructor(safeModeToolkit: SafeModeToolkit, mesh: Mesh, mFlags: Int, mAmbient: Int, mContrast: Int, preferenceMask: Int) {
        this.toolkit = safeModeToolkit
        this.functionMask = mFlags
        this.ambient = mAmbient
        this.contrast = mContrast
        val materialProvider = this.toolkit.materialProvider
        this.numVertices = mesh.numVertices
        this.maxDepth = mesh.maxDepth
        this.verticesX = mesh.verticesX
        this.verticesY = mesh.verticesY
        this.verticesZ = mesh.verticesZ
        this.numFaces = mesh.numFaces
        this.facesA = mesh.facesA
        this.facesB = mesh.facesB
        this.facesC = mesh.facesC
        this.facesLayer = mesh.facesLayer
        this.facesColor = mesh.facesColor
        this.facesAlpha = mesh.facesAlpha
        this.facesSubmeshes = mesh.facesSubmeshes
        this.faceRenderType = mesh.faceRenderType
        this.particleEmitters = mesh.particleEmitters
        this.effectors = mesh.particleEffectors
        this.verticesSubmeshes = mesh.verticesSubmeshes
        val faces = IntArray(this.numFaces)
        for (faceIndex in 0 until this.numFaces) {
            faces[faceIndex] = faceIndex
        }
        val billboardUids = LongArray(this.numFaces)
        val hasPriorities = (this.functionMask and ModelFunctionMask.ALTERS_ALPHA) != 0
        for (faceIndex in 0 until this.numFaces) {
            val face = faces[faceIndex]
            var material1: MaterialType? = null
            var billboardLayer = 0
            var materialEffectId: Byte = 0
            var materialEffectParam1: Byte = 0
            if (mesh.billboards != null) {
                var hasUid = false
                for (billboardIndex in mesh.billboards!!.indices) {
                    val billboard = mesh.billboards!![billboardIndex]
                    if (face == billboard.appliesOnFace) {
                        val billboardType = BillboardTypeList.list(billboard.billboardId)
                        if (billboardType.hasUid) {
                            hasUid = true
                        }
                        if (billboardType.materialId != -1) {
                            val material = materialProvider!!.getMaterial(billboardType.materialId)
                            if (material.effectCombiner.toInt() == 2) {
                                this.accountForPriorities = true
                            }
                        }
                    }
                }
                if (hasUid) {
                    billboardUids[faceIndex] = Long.MAX_VALUE
                }
            }
            var materialId: Short = -1
            if (mesh.facesMaterial != null) {
                materialId = mesh.facesMaterial!![face]
                if (materialId.toInt() != -1) {
                    material1 = materialProvider!!.getMaterial(materialId.toInt() and 0xffff)
                    if ((preferenceMask and ModelFunctionMask.DISABLE_TEXTURES) != 0 && material1.detailsOnly) {
                        materialId = -1
                    } else {
                        materialEffectId = material1.effectId
                        materialEffectParam1 = material1.effectParam1
                    }
                }
            }
            val considerLayers = this.facesAlpha != null && this.facesAlpha!![face].toInt() != 0 || material1 != null && material1.effectCombiner.toInt() == 2
            if ((hasPriorities || considerLayers) && this.facesLayer != null) {
                billboardLayer += this.facesLayer!![face].toInt() shl 17
            }
            if (considerLayers) {
                billboardLayer += 65536
            }
            billboardLayer += (materialEffectId.toInt() and 0xff) shl 8
            billboardLayer += materialEffectParam1.toInt() and 0xff
            var billboardFace = (materialId.toInt() and 0xffff) shl 16
            billboardFace += faceIndex and 0xffff
            billboardUids[faceIndex] = (billboardLayer.toLong() shl 32) + billboardFace
            this.accountForPriorities = this.accountForPriorities or considerLayers
        }
        Model.addBillboard(billboardUids, faces)
        if (mesh.billboards != null) {
            this.numBillboards = mesh.billboards!!.size
            this.billboardEffects = arrayOfNulls(this.numBillboards)
            this.billboardSurfaces = arrayOfNulls(this.numBillboards)
            for (billboardIndex in mesh.billboards!!.indices) {
                val billboard = mesh.billboards!![billboardIndex]
                val billboardType = BillboardTypeList.list(billboard.billboardId)
                var billboardFaceRgb = ColorUtil.hsvToRgbTable!![mesh.facesColor!![billboard.appliesOnFace].toInt() and 0xffff] and 0xffffff
                billboardFaceRgb = billboardFaceRgb or ((255 - (if (mesh.facesAlpha != null) mesh.facesAlpha!![billboard.appliesOnFace].toInt() and 0xff else 0)) shl 24)
                this.billboardEffects!![billboardIndex] = BillboardEffect(billboard.appliesOnFace, billboardType.size2d, billboardType.size3d, billboardType.materialId, billboardType.blendType, billboardType.shape, billboardType.hasUid, billboard.distance)
                this.billboardSurfaces!![billboardIndex] = BillboardSurface(billboardFaceRgb)
            }
        }
        this.uMap = arrayOfNulls(this.numFaces)
        this.vMap = arrayOfNulls(this.numFaces)
        val textureMatrix = generateTextureMatrix(mesh, faces, this.numFaces)
        val renderingThread = this.toolkit.getThreadResource(Thread.currentThread())!!
        val uvTypes = renderingThread.uvTypes
        var materialized = false
        for (faceIndex in 0 until this.numFaces) {
            val face = faces[faceIndex]
            val textureOnFace: Byte = if (mesh.facesTexture == null) {
                -1
            } else {
                mesh.facesTexture!![face]
            }
            var materialId: Short = if (mesh.facesMaterial == null) -1 else mesh.facesMaterial!![face]
            if (materialId.toInt() != -1 && (preferenceMask and ModelFunctionMask.DISABLE_TEXTURES) != 0) {
                val material = materialProvider!!.getMaterial(materialId.toInt() and 0xffff)
                if (material.detailsOnly) {
                    materialId = -1
                }
            }
            if (materialId.toInt() != -1) {
                materialized = true
                val u = FloatArray(3)
                this.uMap!![face] = u
                val v = FloatArray(3)
                this.vMap!![face] = v
                if (textureOnFace.toInt() == -1) {
                    u[0] = 0.0f
                    v[0] = 1.0f
                    u[1] = 1.0f
                    v[1] = 1.0f
                    u[2] = 0.0f
                    v[2] = 0.0f
                } else {
                    val textureIndex = textureOnFace.toInt() and 0xff
                    val textureMapping = mesh.texturesMapping!![textureIndex]
                    val a: Short
                    val b: Short
                    val c: Short
                    if (textureMapping.toInt() == 0) {
                        a = this.facesA!![face]
                        b = this.facesB!![face]
                        c = this.facesC!![face]
                        val texX = mesh.textureFaceX!![textureIndex]
                        val texY = mesh.textureFaceY!![textureIndex]
                        val texZ = mesh.textureFaceZ!![textureIndex]
                        val f_28 = this.verticesX!![texX.toInt()].toFloat()
                        val f_29 = this.verticesY!![texX.toInt()].toFloat()
                        val f_30 = this.verticesZ!![texX.toInt()].toFloat()
                        val f_31 = this.verticesX!![texY.toInt()] - f_28
                        val f_32 = this.verticesY!![texY.toInt()] - f_29
                        val f_33 = this.verticesZ!![texY.toInt()] - f_30
                        val f_34 = this.verticesX!![texZ.toInt()] - f_28
                        val f_35 = this.verticesY!![texZ.toInt()] - f_29
                        val f_36 = this.verticesZ!![texZ.toInt()] - f_30
                        val f_37 = this.verticesX!![a.toInt()] - f_28
                        val f_38 = this.verticesY!![a.toInt()] - f_29
                        val f_39 = this.verticesZ!![a.toInt()] - f_30
                        val f_40 = this.verticesX!![b.toInt()] - f_28
                        val f_41 = this.verticesY!![b.toInt()] - f_29
                        val f_42 = this.verticesZ!![b.toInt()] - f_30
                        val f_43 = this.verticesX!![c.toInt()] - f_28
                        val f_44 = this.verticesY!![c.toInt()] - f_29
                        val f_45 = this.verticesZ!![c.toInt()] - f_30
                        val f_46 = f_32 * f_36 - f_33 * f_35
                        val f_47 = f_33 * f_34 - f_31 * f_36
                        val f_48 = f_31 * f_35 - f_32 * f_34
                        var f_49 = f_35 * f_48 - f_36 * f_47
                        var f_50 = f_36 * f_46 - f_34 * f_48
                        var f_51 = f_34 * f_47 - f_35 * f_46
                        var f_52 = 1.0f / (f_49 * f_31 + f_50 * f_32 + f_51 * f_33)
                        u[0] = (f_49 * f_37 + f_50 * f_38 + f_51 * f_39) * f_52
                        u[1] = (f_49 * f_40 + f_50 * f_41 + f_51 * f_42) * f_52
                        u[2] = (f_49 * f_43 + f_50 * f_44 + f_51 * f_45) * f_52
                        f_49 = f_32 * f_48 - f_33 * f_47
                        f_50 = f_33 * f_46 - f_31 * f_48
                        f_51 = f_31 * f_47 - f_32 * f_46
                        f_52 = 1.0f / (f_49 * f_34 + f_50 * f_35 + f_51 * f_36)
                        v[0] = (f_49 * f_37 + f_50 * f_38 + f_51 * f_39) * f_52
                        v[1] = (f_49 * f_40 + f_50 * f_41 + f_51 * f_42) * f_52
                        v[2] = (f_49 * f_43 + f_50 * f_44 + f_51 * f_45) * f_52
                    } else {
                        a = this.facesA!![face]
                        b = this.facesB!![face]
                        c = this.facesC!![face]
                        val centerX = textureMatrix!!.centerX!![textureIndex]
                        val centerY = textureMatrix.centerY!![textureIndex]
                        val centerZ = textureMatrix.centerZ!![textureIndex]
                        val matrix = textureMatrix.matrix!![textureIndex]
                        val direction = mesh.texturesDirection!![textureIndex]
                        val f_30 = mesh.texturesSpeed!![textureIndex] / 256.0f
                        if (textureMapping.toInt() == 1) {
                            val f_31 = mesh.texturesScaleZ!![textureIndex] / 1024.0f
                            mapCylindericalUv(this.verticesX!![a.toInt()], this.verticesY!![a.toInt()], this.verticesZ!![a.toInt()], centerX, centerY, centerZ, matrix, f_31, direction.toInt(), f_30, uvTypes)
                            u[0] = uvTypes!![0]
                            v[0] = uvTypes[1]
                            mapCylindericalUv(this.verticesX!![b.toInt()], this.verticesY!![b.toInt()], this.verticesZ!![b.toInt()], centerX, centerY, centerZ, matrix, f_31, direction.toInt(), f_30, uvTypes)
                            u[1] = uvTypes[0]
                            v[1] = uvTypes[1]
                            mapCylindericalUv(this.verticesX!![c.toInt()], this.verticesY!![c.toInt()], this.verticesZ!![c.toInt()], centerX, centerY, centerZ, matrix, f_31, direction.toInt(), f_30, uvTypes)
                            u[2] = uvTypes[0]
                            v[2] = uvTypes[1]
                            val f_32 = f_31 / 2.0f
                            if ((direction.toInt() and 0x1) == 0) {
                                if (u[1] - u[0] > f_32) {
                                    u[1] -= f_31
                                } else if (u[0] - u[1] > f_32) {
                                    u[1] += f_31
                                }
                                if (u[2] - u[0] > f_32) {
                                    u[2] -= f_31
                                } else if (u[0] - u[2] > f_32) {
                                    u[2] += f_31
                                }
                            } else {
                                if (v[1] - v[0] > f_32) {
                                    v[1] -= f_31
                                } else if (v[0] - v[1] > f_32) {
                                    v[1] += f_31
                                }
                                if (v[2] - v[0] > f_32) {
                                    v[2] -= f_31
                                } else if (v[0] - v[2] > f_32) {
                                    v[2] += f_31
                                }
                            }
                        } else if (textureMapping.toInt() == 2) {
                            val f_31 = mesh.texturesUtrans!![textureIndex] / 256.0f
                            val f_32 = mesh.texturesVtrans!![textureIndex] / 256.0f
                            val i_77 = this.verticesX!![b.toInt()] - this.verticesX!![a.toInt()]
                            val i_78 = this.verticesY!![b.toInt()] - this.verticesY!![a.toInt()]
                            val i_79 = this.verticesZ!![b.toInt()] - this.verticesZ!![a.toInt()]
                            val i_80 = this.verticesX!![c.toInt()] - this.verticesX!![a.toInt()]
                            val i_81 = this.verticesY!![c.toInt()] - this.verticesY!![a.toInt()]
                            val i_82 = this.verticesZ!![c.toInt()] - this.verticesZ!![a.toInt()]
                            val i_83 = i_78 * i_82 - i_81 * i_79
                            val i_84 = i_79 * i_80 - i_82 * i_77
                            val i_85 = i_77 * i_81 - i_80 * i_78
                            val f_42 = 64.0f / mesh.texturesScaleX!![textureIndex]
                            val f_43 = 64.0f / mesh.texturesScaleY!![textureIndex]
                            val f_44 = 64.0f / mesh.texturesScaleZ!![textureIndex]
                            val f_45 = (i_83 * matrix!![0] + i_84 * matrix[1] + i_85 * matrix[2]) / f_42
                            val f_46 = (i_83 * matrix[3] + i_84 * matrix[4] + i_85 * matrix[5]) / f_43
                            val f_47 = (i_83 * matrix[6] + i_84 * matrix[7] + i_85 * matrix[8]) / f_44
                            val i_86 = getCubeFace(f_45, f_46, f_47)
                            mapRectangularUv(this.verticesX!![a.toInt()], this.verticesY!![a.toInt()], this.verticesZ!![a.toInt()], centerX, centerY, centerZ, i_86, matrix, direction.toInt(), f_30, f_31, f_32, uvTypes)
                            u[0] = uvTypes!![0]
                            v[0] = uvTypes[1]
                            mapRectangularUv(this.verticesX!![b.toInt()], this.verticesY!![b.toInt()], this.verticesZ!![b.toInt()], centerX, centerY, centerZ, i_86, matrix, direction.toInt(), f_30, f_31, f_32, uvTypes)
                            u[1] = uvTypes[0]
                            v[1] = uvTypes[1]
                            mapRectangularUv(this.verticesX!![c.toInt()], this.verticesY!![c.toInt()], this.verticesZ!![c.toInt()], centerX, centerY, centerZ, i_86, matrix, direction.toInt(), f_30, f_31, f_32, uvTypes)
                            u[2] = uvTypes[0]
                            v[2] = uvTypes[1]
                        } else if (textureMapping.toInt() == 3) {
                            mapSphericalUv(this.verticesX!![a.toInt()], this.verticesY!![a.toInt()], this.verticesZ!![a.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), f_30, uvTypes)
                            u[0] = uvTypes!![0]
                            v[0] = uvTypes[1]
                            mapSphericalUv(this.verticesX!![b.toInt()], this.verticesY!![b.toInt()], this.verticesZ!![b.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), f_30, uvTypes)
                            u[1] = uvTypes[0]
                            v[1] = uvTypes[1]
                            mapSphericalUv(this.verticesX!![c.toInt()], this.verticesY!![c.toInt()], this.verticesZ!![c.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), f_30, uvTypes)
                            u[2] = uvTypes[0]
                            v[2] = uvTypes[1]
                            if ((direction.toInt() and 0x1) == 0) {
                                if (u[1] - u[0] > 0.5f) {
                                    --u[1]
                                } else if (u[0] - u[1] > 0.5f) {
                                    ++u[1]
                                }
                                if (u[2] - u[0] > 0.5f) {
                                    --u[2]
                                } else if (u[0] - u[2] > 0.5f) {
                                    ++u[2]
                                }
                            } else {
                                if (v[1] - v[0] > 0.5f) {
                                    --v[1]
                                } else if (v[0] - v[1] > 0.5f) {
                                    ++v[1]
                                }
                                if (v[2] - v[0] > 0.5f) {
                                    --v[2]
                                } else if (v[0] - v[2] > 0.5f) {
                                    ++v[2]
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!materialized) {
            this.vMap = null
            this.uMap = null
        }
        if (mesh.vertexGroups != null && (this.functionMask and ModelFunctionMask.ALTERS_VERTICES) != 0) {
            @Suppress("UNCHECKED_CAST")
            this.vertexGroups = mesh.createVertexGroups(true) as Array<IntArray?>?
        }
        if (mesh.faceGroups != null && (this.functionMask and ModelFunctionMask.ALTERS_FACES) != 0) {
            @Suppress("UNCHECKED_CAST")
            this.faceGroups = mesh.createFaceGroups() as Array<IntArray?>?
        }
        if (mesh.billboards != null && (this.functionMask and ModelFunctionMask.ALTERS_BILLBOARDS) != 0) {
            @Suppress("UNCHECKED_CAST")
            this.billboardGroups = mesh.createBillboardGroups() as Array<IntArray?>?
        }
        if (mesh.facesMaterial != null) {
            this.facesMaterial = ShortArray(this.numFaces)
            var hasMaterial = false
            for (faceIndex in 0 until this.numFaces) {
                val materialId = mesh.facesMaterial!![faceIndex]
                if (materialId.toInt() != -1) {
                    val material = this.toolkit.materialProvider!!.getMaterial(materialId.toInt())
                    if ((preferenceMask and ModelFunctionMask.DISABLE_TEXTURES) != 0 && material.detailsOnly) {
                        this.facesMaterial!![faceIndex] = -1
                    } else {
                        this.facesMaterial!![faceIndex] = materialId
                        hasMaterial = true
                        if (material.effectCombiner.toInt() == 2) {
                            this.accountForPriorities = true
                        }
                        if (material.textureSpeedU.toInt() != 0 || material.textureSpeedV.toInt() != 0) {
                            this.textureAnimated = true
                        }
                    }
                } else {
                    this.facesMaterial!![faceIndex] = -1
                }
            }
            if (!hasMaterial) {
                this.facesMaterial = null
            }
        } else {
            this.facesMaterial = null
        }
        if (this.accountForPriorities || this.billboardEffects != null) {
            this.aShortArray8623 = ShortArray(this.numFaces)
            for (faceIndex in 0 until this.numFaces) {
                this.aShortArray8623!![faceIndex] = faces[faceIndex].toShort()
            }
        }
    }

    override fun drawMatrixThreaded(matrix44var1: Matrix43, boundingBox: BoundingBoxPolygon?, rasterizerFunctionMask: Int) {
        multithreadedDraw(matrix44var1, boundingBox, rasterizerFunctionMask)
    }

    fun copyDependencies(resource: ThreadResourceSM) {
        this.rasterizer = resource.rasterizer
        if (resource !== this.threadResource) {
            this.threadResource = resource
            this.waterRenderHeight = this.threadResource!!.waterRenderHeight
            this.projectedVerticesX = this.threadResource!!.projectedVerticesX
            this.projectedVerticesY = this.threadResource!!.projectedVerticesY
            this.projectedVerticesZ = this.threadResource!!.projectedVerticesZ
            this.projectedVerticesZ2 = this.threadResource!!.projectedVerticesZ2
            this.matrixX = this.threadResource!!.matrixX
            this.matrixY = this.threadResource!!.matrixY
            this.matrixZ = this.threadResource!!.matrixZ
            this.renderCycleByVertex = this.threadResource!!.renderCycleByVertex
            this.renderCycleByDepth = this.threadResource!!.renderCycleByDepth
            this.billboardOnFace = this.threadResource!!.billboardIdByFace
        }
    }

    override fun setContrast(i_1: Int) {
        if ((this.functionMask and ModelFunctionMask.ALTERS_CONTRAST) != 8192) {
            throw IllegalStateException()
        } else {
            this.contrast = i_1
            this.appearanceUpdateStage = 0
        }
    }

    fun setToolkitForThread(thread: Thread) {
        val viewport = this.toolkit.getThreadResource(thread)
        if (viewport !== this.currentViewport) {
            this.currentViewport = viewport
            @Suppress("UNCHECKED_CAST")
            this.copyDstModels = this.currentViewport!!.copyDstModels as Array<ModelSM?>?
            @Suppress("UNCHECKED_CAST")
            this.copySrcModels = this.currentViewport!!.copySrcModels as Array<ModelSM?>?
        }
    }

    fun calculateNormals() {
        this.vertexNormals = arrayOfNulls(this.maxDepth)
        for (faceIndex in 0 until this.maxDepth) {
            this.vertexNormals!![faceIndex] = VertexNormal()
        }
        for (faceIndex in 0 until this.numFaces) {
            val a = this.facesA!![faceIndex]
            val b = this.facesB!![faceIndex]
            val c = this.facesC!![faceIndex]
            val deltaXBa = this.verticesX!![b.toInt()] - this.verticesX!![a.toInt()]
            val deltaYBa = this.verticesY!![b.toInt()] - this.verticesY!![a.toInt()]
            val deltaZBa = this.verticesZ!![b.toInt()] - this.verticesZ!![a.toInt()]
            val deltaXCa = this.verticesX!![c.toInt()] - this.verticesX!![a.toInt()]
            val deltaYCa = this.verticesY!![c.toInt()] - this.verticesY!![a.toInt()]
            val deltaZCa = this.verticesZ!![c.toInt()] - this.verticesZ!![a.toInt()]
            var deltaX = deltaYBa * deltaZCa - deltaYCa * deltaZBa
            var deltaY = deltaZBa * deltaXCa - deltaZCa * deltaXBa
            var deltaZ = deltaXBa * deltaYCa - deltaXCa * deltaYBa
            while (deltaX > 8192 || deltaY > 8192 || deltaZ > 8192 || deltaX < -8192 || deltaY < -8192 || deltaZ < -8192) {
                deltaX = deltaX shr 1
                deltaY = deltaY shr 1
                deltaZ = deltaZ shr 1
            }
            var divider = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toInt()
            if (divider <= 0) {
                divider = 1
            }
            deltaX = deltaX * 256 / divider
            deltaY = deltaY * 256 / divider
            deltaZ = deltaZ * 256 / divider
            val renderType: Byte = if (this.faceRenderType == null) {
                0
            } else {
                this.faceRenderType!![faceIndex]
            }
            if (renderType.toInt() == 0) {
                var normal = this.vertexNormals!![a.toInt()]!!
                normal.x += deltaX
                normal.y += deltaY
                normal.z += deltaZ
                normal.magnitude++
                normal = this.vertexNormals!![b.toInt()]!!
                normal.x += deltaX
                normal.y += deltaY
                normal.z += deltaZ
                normal.magnitude++
                normal = this.vertexNormals!![c.toInt()]!!
                normal.x += deltaX
                normal.y += deltaY
                normal.z += deltaZ
                normal.magnitude++
            } else if (renderType.toInt() == 1) {
                if (this.facesNormal == null) {
                    this.facesNormal = arrayOfNulls(this.numFaces)
                }
                val faceNormal = FaceNormal()
                this.facesNormal!![faceIndex] = faceNormal
                faceNormal.x = deltaX
                faceNormal.y = deltaY
                faceNormal.z = deltaZ
            }
        }
    }

    fun appearanceUpdate(resetRenderType: Boolean) {
        if (this.appearanceUpdateStage == 1) {
            recalculateColorsThreaded()
        } else if (this.appearanceUpdateStage == 2) {
            if ((this.functionMask and ModelFunctionMask.RESET_COLOR) == 0 && this.uMap == null) {
                this.facesColor = null
            }
            if (resetRenderType) {
                this.faceRenderType = null
            }
        } else {
            calculateNormalsThreaded()
            val lightX = this.toolkit.lightX
            val lightY = this.toolkit.lightY
            val lightZ = this.toolkit.lightZ
            val light = this.toolkit.lightIntensity shr 8
            var lowContrast = 0
            var highContrast = 0
            if (this.contrast != 0) {
                lowContrast = this.toolkit.lowestContrast * 768 / this.contrast
                highContrast = this.toolkit.highestContrast * 768 / this.contrast
            }
            if (this.facesColourA == null) {
                this.facesColourA = IntArray(this.numFaces)
                this.facesColourB = IntArray(this.numFaces)
                this.facesColourC = IntArray(this.numFaces)
            }
            for (faceIndex in 0 until this.numFaces) {
                var renderType: Byte = if (this.faceRenderType == null) {
                    0
                } else {
                    this.faceRenderType!![faceIndex]
                }
                val alpha: Byte = if (this.facesAlpha == null) {
                    0
                } else {
                    this.facesAlpha!![faceIndex]
                }
                val materialId: Short = if (this.facesMaterial == null) {
                    -1
                } else {
                    this.facesMaterial!![faceIndex]
                }
                if (alpha.toInt() == -2) {
                    renderType = 3
                }
                if (alpha.toInt() == -1) {
                    renderType = 2
                }
                if (materialId.toInt() == -1) {
                    if (renderType.toInt() != 0) {
                        if (renderType.toInt() == 1) {
                            val colorHsl = this.facesColor!![faceIndex].toInt() and 0xffff
                            val finalAmbient = (colorHsl and 0x7f) * this.ambient shr 7
                            val signedHsl = ColorUtil.adjustHsv(colorHsl and -128 or finalAmbient)
                            val faceNormal = this.facesNormal!![faceIndex]!!
                            val lightness = lightX * faceNormal.x + lightY * faceNormal.y + lightZ * faceNormal.z shr 16
                            val finalContrast = if (lightness > 256) lowContrast else highContrast
                            val finalLight = (light shr 1) + (finalContrast * lightness shr 17)
                            this.facesColourA!![faceIndex] = finalLight shl 17 or ColorUtil.repackHsl(signedHsl.toInt(), finalLight)
                            this.facesColourC!![faceIndex] = -1
                        } else if (renderType.toInt() == 3) {
                            this.facesColourA!![faceIndex] = 128
                            this.facesColourC!![faceIndex] = -1
                        } else {
                            this.facesColourC!![faceIndex] = -2
                        }
                    } else {
                        val colorHsl = this.facesColor!![faceIndex].toInt() and 0xffff
                        val finalAmbient = (colorHsl and 0x7f) * this.ambient shr 7
                        val signedHsl = ColorUtil.adjustHsv(colorHsl and -128 or finalAmbient)
                        var vertexNormal: VertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesA!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesA!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesA!![faceIndex].toInt()]!!
                        }
                        var lightness = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        var finalContrast = if (lightness > 256) lowContrast else highContrast
                        var finalLight = (light shr 1) + (finalContrast * lightness shr 17)
                        this.facesColourA!![faceIndex] = finalLight shl 17 or ColorUtil.repackHsl(signedHsl.toInt(), finalLight)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesB!![faceIndex].toInt()]!!
                        }
                        lightness = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (lightness > 256) lowContrast else highContrast
                        finalLight = (light shr 1) + (finalContrast * lightness shr 17)
                        this.facesColourB!![faceIndex] = finalLight shl 17 or ColorUtil.repackHsl(signedHsl.toInt(), finalLight)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesC!![faceIndex].toInt()]!!
                        }
                        lightness = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (lightness > 256) lowContrast else highContrast
                        finalLight = (light shr 1) + (finalContrast * lightness shr 17)
                        this.facesColourC!![faceIndex] = finalLight shl 17 or ColorUtil.repackHsl(signedHsl.toInt(), finalLight)
                    }
                } else {
                    val colorHsl = this.facesColor!![faceIndex].toInt() and 0xffff
                    if (renderType.toInt() != 0) {
                        if (renderType.toInt() == 1) {
                            val faceNormal = this.facesNormal!![faceIndex]!!
                            val i_14 = lightX * faceNormal.x + lightY * faceNormal.y + lightZ * faceNormal.z shr 16
                            val i_20 = if (i_14 > 256) lowContrast else highContrast
                            val lightness = signLight((light shr 2) + (i_20 * i_14 shr 18))
                            this.facesColourA!![faceIndex] = lightness shl 24 or getMaterialColor(colorHsl, materialId, lightness)
                            this.facesColourC!![faceIndex] = -1
                        } else {
                            this.facesColourC!![faceIndex] = -2
                        }
                    } else {
                        var vertexNormal: VertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesA!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesA!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesA!![faceIndex].toInt()]!!
                        }
                        var i_14 = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        var finalContrast = if (i_14 > 256) lowContrast else highContrast
                        var lightness = signLight((light shr 2) + (finalContrast * i_14 shr 18))
                        this.facesColourA!![faceIndex] = lightness shl 24 or getMaterialColor(colorHsl, materialId, lightness)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesB!![faceIndex].toInt()]!!
                        }
                        i_14 = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (i_14 > 256) lowContrast else highContrast
                        lightness = signLight((light shr 2) + (finalContrast * i_14 shr 18))
                        this.facesColourB!![faceIndex] = lightness shl 24 or getMaterialColor(colorHsl, materialId, lightness)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesC!![faceIndex].toInt()]!!
                        }
                        i_14 = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (i_14 > 256) lowContrast else highContrast
                        lightness = signLight((light shr 2) + (finalContrast * i_14 shr 18))
                        this.facesColourC!![faceIndex] = lightness shl 24 or getMaterialColor(colorHsl, materialId, lightness)
                    }
                }
            }
            this.vertexNormals = null
            this.vertexNormalOffsets = null
            this.facesNormal = null
            if ((this.functionMask and ModelFunctionMask.RESET_COLOR) == 0 && this.uMap == null) {
                this.facesColor = null
            }
            if (resetRenderType) {
                this.faceRenderType = null
            }
            this.appearanceUpdateStage = 2
        }
    }

    fun getMaterialColor(hsl: Int, materialId: Short, lightness: Int): Int {
        var color = ColorUtil.hslToRgb!![mixLightness(hsl, lightness)]
        val material = this.toolkit.materialProvider!!.getMaterial(materialId.toInt() and 0xffff)
        val contrast = material.alpha.toInt() and 0xff
        var brightness: Int
        if (contrast != 0) {
            brightness = lightness * 131586
            val transparency = 256 - contrast
            color = ((brightness and 0xff00ff) * contrast + (color and 0xff00ff) * transparency and -16711936) + (contrast * (brightness and 0xff00) + transparency * (color and 0xff00) and 0xff0000) shr 8
        }
        brightness = material.brightness.toInt() and 0xff
        if (brightness != 0) {
            brightness += 256
            var r = brightness * ((color and 0xff0000) shr 16)
            if (r > 65535) {
                r = 65535
            }
            var g = brightness * ((color and 0xff00) shr 8)
            if (g > 65535) {
                g = 65535
            }
            var b = (color and 0xff) * brightness
            if (b > 65535) {
                b = 65535
            }
            color = (b shr 8) + (g and 0xff00) + (r shl 8 and 0xff0000)
        }
        return color
    }

    override fun copy(recentuseIndex: Byte, copyFlags: Int, recalculateBeforeCopy: Boolean): Model? {
        setToolkitForThread(Thread.currentThread())
        var fromCache = false
        val dstModel: ModelSM
        val srcModel: ModelSM
        if (recentuseIndex > 0 && recentuseIndex <= 7) {
            srcModel = this.copySrcModels!![recentuseIndex - 1]!!
            dstModel = this.copyDstModels!![recentuseIndex - 1]!!
            fromCache = true
        } else {
            val newModel = ModelSM(this.toolkit)
            srcModel = newModel
            dstModel = newModel
        }
        return copy(dstModel, srcModel, copyFlags, fromCache, recalculateBeforeCopy)
    }

    fun signLight(i_1: Int): Int {
        var i_11 = i_1
        if (i_11 < 2) {
            i_11 = 2
        } else if (i_11 > 126) {
            i_11 = 126
        }
        return i_11
    }

    fun recalculateColorsThreaded() {
        if (this.appearanceUpdateStage == 0) {
            appearanceUpdateThreaded(false)
        } else if (this.toolkit.executingThreadCount > 1) {
            synchronized(this) {
                recalculateColors()
            }
        } else {
            recalculateColors()
        }
    }

    fun recalculateColors() {
        for (faceIndex in 0 until this.numFaces) {
            val faceMaterialId: Short = if (this.facesMaterial != null) this.facesMaterial!![faceIndex] else -1
            if (faceMaterialId.toInt() == -1) {
                val color = this.facesColor!![faceIndex].toInt() and 0xffff
                val colorMask = (color and 0x7f) * this.ambient shr 7
                val adjustedColor = ColorUtil.adjustHsv(color and -128 or colorMask)
                val lightness: Int
                if (this.facesColourC!![faceIndex] == -1) {
                    lightness = this.facesColourA!![faceIndex] and -131072
                    this.facesColourA!![faceIndex] = lightness or ColorUtil.repackHsl(adjustedColor.toInt(), lightness shr 17)
                } else if (this.facesColourC!![faceIndex] != -2) {
                    lightness = this.facesColourA!![faceIndex] and -131072
                    this.facesColourA!![faceIndex] = lightness or ColorUtil.repackHsl(adjustedColor.toInt(), lightness shr 17)
                    val lightnessB = this.facesColourB!![faceIndex] and -131072
                    this.facesColourB!![faceIndex] = lightnessB or ColorUtil.repackHsl(adjustedColor.toInt(), lightnessB shr 17)
                    val lightnessC = this.facesColourC!![faceIndex] and -131072
                    this.facesColourC!![faceIndex] = lightnessC or ColorUtil.repackHsl(adjustedColor.toInt(), lightnessC shr 17)
                }
            }
        }
        this.appearanceUpdateStage = 2
    }

    fun hasBillboardOnFace(face: Int): Boolean {
        return this.billboardOnFace != null && this.billboardOnFace!![face] != -1
    }

    fun copy(modelDst: ModelSM, modelSrc: ModelSM, functionMask: Int, expand: Boolean, recalculateBeforeCopy: Boolean): Model {
        modelDst.boundsUpdated = this.boundsUpdated
        if (this.boundsUpdated) {
            modelDst.maxX = this.maxX
            modelDst.maxY = this.maxY
            modelDst.maxZ = this.maxZ
            modelDst.minX = this.minX
            modelDst.minY = this.minY
            modelDst.minZ = this.minZ
            modelDst.size2d = this.size2d
            modelDst.size3d = this.size3d
        }
        modelDst.ambient = this.ambient
        modelDst.contrast = this.contrast
        modelDst.numVertices = this.numVertices
        modelDst.maxDepth = this.maxDepth
        modelDst.numFaces = this.numFaces
        modelDst.numBillboards = this.numBillboards
        if ((functionMask and ModelFunctionMask.ALTERS_ALPHA) != 0) {
            modelDst.accountForPriorities = true
        } else {
            modelDst.accountForPriorities = this.accountForPriorities
        }
        modelDst.textureAnimated = this.textureAnimated
        val groundAdjustment = ((functionMask and ModelFunctionMask.FULL_DISPLACEMENT) == 7) || ((functionMask and 0x20) != 0)
        val displacementX = groundAdjustment || (functionMask and ModelFunctionMask.DISPLACE_X) != 0
        val displacementY = groundAdjustment || (functionMask and ModelFunctionMask.DISPLACE_Y) != 0
        val displacementZ = groundAdjustment || (functionMask and ModelFunctionMask.DISPLACE_Z) != 0 || (functionMask and ModelFunctionMask.INVERT) != 0
        if (!displacementX && !displacementY && !displacementZ) {
            modelDst.verticesX = this.verticesX
            modelDst.verticesY = this.verticesY
            modelDst.verticesZ = this.verticesZ
        } else {
            if (!displacementX) {
                modelDst.verticesX = this.verticesX
            } else {
                if (modelSrc.verticesX != null && modelSrc.verticesX!!.size >= this.numVertices) {
                    modelDst.verticesX = modelSrc.verticesX
                } else {
                    modelDst.verticesX = IntArray(this.numVertices)
                    modelSrc.verticesX = modelDst.verticesX
                }
                if (this.numVertices >= 0) System.arraycopy(this.verticesX!!, 0, modelDst.verticesX!!, 0, this.numVertices)
            }
            if (!displacementY) {
                modelDst.verticesY = this.verticesY
            } else {
                if (modelSrc.verticesY != null && modelSrc.verticesY!!.size >= this.numVertices) {
                    modelDst.verticesY = modelSrc.verticesY
                } else {
                    modelDst.verticesY = IntArray(this.numVertices)
                    modelSrc.verticesY = modelDst.verticesY
                }
                if (this.numVertices >= 0) System.arraycopy(this.verticesY!!, 0, modelDst.verticesY!!, 0, this.numVertices)
            }
            if (displacementZ) {
                if (modelSrc.verticesZ != null && modelSrc.verticesZ!!.size >= this.numVertices) {
                    modelDst.verticesZ = modelSrc.verticesZ
                } else {
                    modelDst.verticesZ = IntArray(this.numVertices)
                    modelSrc.verticesZ = modelDst.verticesZ
                }
                if (this.numVertices >= 0) System.arraycopy(this.verticesZ!!, 0, modelDst.verticesZ!!, 0, this.numVertices)
            } else {
                modelDst.verticesZ = this.verticesZ
            }
        }
        if ((functionMask and 0x84080) != 0) {
            if (modelSrc.facesColor != null && modelSrc.facesColor!!.size >= this.numFaces) {
                modelDst.facesColor = modelSrc.facesColor
            } else {
                modelDst.facesColor = ShortArray(this.numFaces)
                modelSrc.facesColor = modelDst.facesColor
            }
            if (this.numFaces >= 0) System.arraycopy(this.facesColor!!, 0, modelDst.facesColor!!, 0, this.numFaces)
        } else {
            modelDst.facesColor = this.facesColor
        }
        if ((functionMask and 0x97018) != 0) {
            modelDst.appearanceUpdateStage = 0
            modelDst.facesColourC = null
            modelDst.facesColourB = null
            modelDst.facesColourA = null
        } else if ((functionMask and 0x80) != 0) {
            if (recalculateBeforeCopy) {
                appearanceUpdateThreaded(false)
            }
            if (this.facesColourA != null) {
                if (modelSrc.facesColourA != null && modelSrc.facesColourA!!.size >= this.numFaces) {
                    modelDst.facesColourA = modelSrc.facesColourA
                    modelDst.facesColourB = modelSrc.facesColourB
                    modelDst.facesColourC = modelSrc.facesColourC
                } else {
                    modelDst.facesColourA = IntArray(this.numFaces)
                    modelSrc.facesColourA = modelDst.facesColourA
                    modelDst.facesColourB = IntArray(this.numFaces)
                    modelSrc.facesColourB = modelDst.facesColourB
                    modelDst.facesColourC = IntArray(this.numFaces)
                    modelSrc.facesColourC = modelDst.facesColourC
                }
                for (faceIndex in 0 until this.numFaces) {
                    modelDst.facesColourA!![faceIndex] = this.facesColourA!![faceIndex]
                    modelDst.facesColourB!![faceIndex] = this.facesColourB!![faceIndex]
                    modelDst.facesColourC!![faceIndex] = this.facesColourC!![faceIndex]
                }
            }
            modelDst.appearanceUpdateStage = this.appearanceUpdateStage
        } else {
            if (recalculateBeforeCopy) {
                appearanceUpdateThreaded(false)
            }
            modelDst.facesColourA = this.facesColourA
            modelDst.facesColourB = this.facesColourB
            modelDst.facesColourC = this.facesColourC
            modelDst.appearanceUpdateStage = this.appearanceUpdateStage
        }
        if ((functionMask and ModelFunctionMask.ALTERS_ALPHA) != 0) {
            if (modelSrc.facesAlpha != null && modelSrc.facesAlpha!!.size >= this.numFaces) {
                modelDst.facesAlpha = modelSrc.facesAlpha
            } else {
                modelDst.facesAlpha = ByteArray(this.numFaces)
                modelSrc.facesAlpha = modelDst.facesAlpha
            }
            if (this.facesAlpha != null) {
                if (this.numFaces >= 0) System.arraycopy(this.facesAlpha!!, 0, modelDst.facesAlpha!!, 0, this.numFaces)
            } else {
                for (faceIndex in 0 until this.numFaces) {
                    modelDst.facesAlpha!![faceIndex] = 0
                }
            }
        } else {
            modelDst.facesAlpha = this.facesAlpha
        }
        if ((functionMask and 0x8) == 0 && (functionMask and ModelFunctionMask.INVERT) == 0) {
            if (recalculateBeforeCopy) {
                calculateNormalsThreaded()
            }
            modelDst.vertexNormals = this.vertexNormals
            modelDst.facesNormal = this.facesNormal
        } else {
            if (modelSrc.vertexNormals != null && modelSrc.vertexNormals!!.size >= this.maxDepth) {
                modelDst.vertexNormals = modelSrc.vertexNormals
            } else {
                modelDst.vertexNormals = arrayOfNulls(this.maxDepth)
                modelSrc.vertexNormals = modelDst.vertexNormals
            }
            if (this.vertexNormals != null) {
                for (depth in 0 until this.maxDepth) {
                    modelDst.vertexNormals!![depth] = VertexNormal(this.vertexNormals!![depth]!!)
                }
            } else {
                modelDst.vertexNormals = null
            }
            if (this.facesNormal != null) {
                if (modelSrc.facesNormal != null && modelSrc.facesNormal!!.size >= this.numFaces) {
                    modelDst.facesNormal = modelSrc.facesNormal
                } else {
                    modelDst.facesNormal = arrayOfNulls(this.numFaces)
                    modelSrc.facesNormal = modelDst.facesNormal
                }
                for (faceIndex in 0 until this.numFaces) {
                    modelDst.facesNormal!![faceIndex] = if (this.facesNormal!![faceIndex] != null) FaceNormal(this.facesNormal!![faceIndex]!!) else null
                }
            } else {
                modelDst.facesNormal = null
            }
        }
        if ((functionMask and ModelFunctionMask.RETEXTURE) != 0) {
            if (this.facesMaterial == null) {
                modelDst.facesMaterial = null
            } else {
                if (modelSrc.facesMaterial != null && modelSrc.facesMaterial!!.size >= this.numFaces) {
                    modelDst.facesMaterial = modelSrc.facesMaterial
                } else {
                    modelDst.facesMaterial = ShortArray(this.numFaces)
                    modelSrc.facesMaterial = modelDst.facesMaterial
                }
                if (this.numFaces >= 0)
                    System.arraycopy(this.facesMaterial!!, 0, modelDst.facesMaterial!!, 0, this.numFaces)
            }
        } else {
            modelDst.facesMaterial = this.facesMaterial
        }
        if ((functionMask and 0x10000) != 0) {
            if (this.faceRenderType == null) {
                modelDst.faceRenderType = null
            } else {
                if (modelSrc.faceRenderType != null && modelSrc.faceRenderType!!.size >= this.numFaces) {
                    modelDst.faceRenderType = modelSrc.faceRenderType
                } else {
                    val expandedFaceCount = if (expand) this.numFaces + 100 else this.numFaces
                    modelDst.faceRenderType = ByteArray(expandedFaceCount)
                    modelSrc.faceRenderType = modelDst.faceRenderType
                }
                if (this.numFaces >= 0)
                    System.arraycopy(this.faceRenderType!!, 0, modelDst.faceRenderType!!, 0, this.numFaces)
            }
        } else {
            modelDst.faceRenderType = this.faceRenderType
        }
        if ((functionMask and 0xc580) != 0) {
            if (modelSrc.billboardSurfaces != null && modelSrc.billboardSurfaces!!.size >= this.numBillboards) {
                modelDst.billboardSurfaces = modelSrc.billboardSurfaces
                for (billboardIndex in 0 until this.numBillboards) {
                    modelDst.billboardSurfaces!![billboardIndex]!!.copy(this.billboardSurfaces!![billboardIndex]!!)
                }
            } else {
                modelDst.billboardSurfaces = arrayOfNulls(this.numBillboards)
                modelSrc.billboardSurfaces = modelDst.billboardSurfaces
                for (billboardIndex in 0 until this.numBillboards) {
                    modelDst.billboardSurfaces!![billboardIndex] = this.billboardSurfaces!![billboardIndex]!!.copy()
                }
            }
        } else {
            modelDst.billboardSurfaces = this.billboardSurfaces
        }
        if (this.uMap != null && (functionMask and ModelFunctionMask.INVERT) != 0) {
            if (modelSrc.uMap != null && modelSrc.uMap!!.size >= this.numFaces) {
                modelDst.uMap = modelSrc.uMap
            } else {
                val expandedFaceCount = if (expand) this.numFaces + 100 else this.numFaces
                modelDst.uMap = Array(expandedFaceCount) { FloatArray(3) }
                modelSrc.uMap = modelDst.uMap
            }
            for (faceIndex in 0 until this.numFaces) {
                if (this.uMap!![faceIndex] != null) {
                    modelDst.uMap!![faceIndex]!![0] = this.uMap!![faceIndex]!![0]
                    modelDst.uMap!![faceIndex]!![1] = this.uMap!![faceIndex]!![1]
                    modelDst.uMap!![faceIndex]!![2] = this.uMap!![faceIndex]!![2]
                }
            }
            if (modelSrc.vMap != null && modelSrc.vMap!!.size >= this.numFaces) {
                modelDst.vMap = modelSrc.vMap
            } else {
                val expandedFaceCount = if (expand) this.numFaces + 100 else this.numFaces
                modelDst.vMap = Array(expandedFaceCount) { FloatArray(3) }
                modelSrc.vMap = modelDst.vMap
            }
            for (faceIndex in 0 until this.numFaces) {
                if (this.vMap!![faceIndex] != null) {
                    modelDst.vMap!![faceIndex]!![0] = this.vMap!![faceIndex]!![0]
                    modelDst.vMap!![faceIndex]!![1] = this.vMap!![faceIndex]!![1]
                    modelDst.vMap!![faceIndex]!![2] = this.vMap!![faceIndex]!![2]
                }
            }
        } else {
            modelDst.uMap = this.uMap
            modelDst.vMap = this.vMap
        }
        modelDst.vertexGroups = this.vertexGroups
        modelDst.faceGroups = this.faceGroups
        modelDst.billboardGroups = this.billboardGroups
        modelDst.verticesSubmeshes = this.verticesSubmeshes
        modelDst.facesSubmeshes = this.facesSubmeshes
        modelDst.facesLayer = this.facesLayer
        modelDst.facesA = this.facesA
        modelDst.facesB = this.facesB
        modelDst.facesC = this.facesC
        modelDst.particleEmitters = this.particleEmitters
        modelDst.effectors = this.effectors
        modelDst.billboardEffects = this.billboardEffects
        modelDst.aShortArray8623 = this.aShortArray8623
        modelDst.functionMask = functionMask
        return modelDst
    }

    override fun getFunctionMask(): Int {
        return this.functionMask
    }

    override fun uploadToGpu() {
        // NOOP
    }

    override fun yaxisRotateWithoutNormals(rotation: Int) {
        if ((this.functionMask and (ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Z)) != 5) {
            throw IllegalStateException()
        }
        if (rotation == 4096) {
            rotate90DegWithoutNormals()
        } else if (rotation == 8192) {
            rotate170DegWithoutNormals()
        } else if (rotation == 12288) {
            rotate270DegWithoutNormals()
        } else {
            val xan = MathUtils.SINE[rotation]
            val zan = MathUtils.COSINE[rotation]
            synchronized(this) {
                for (verticeIndex in 0 until this.numVertices) {
                    val tmpX = xan * this.verticesZ!![verticeIndex] + zan * this.verticesX!![verticeIndex] shr 14
                    this.verticesZ!![verticeIndex] = zan * this.verticesZ!![verticeIndex] - xan * this.verticesX!![verticeIndex] shr 14
                    this.verticesX!![verticeIndex] = tmpX
                }
                invalidate()
            }
        }
    }

    override fun rotateWithNormals(rotation: Int) {
        if ((this.functionMask and ModelFunctionMask.ROTATE_WITH_NORMALS) != 13) {
            throw IllegalStateException()
        }
        if (this.vertexNormals == null) {
            yaxisRotateWithoutNormals(rotation)
            return
        }
        if (rotation == 4096) {
            rotate90DegWithNormals()
        } else if (rotation == 8192) {
            rotate180DegWithNormals()
        } else if (rotation == 12288) {
            rotate270DegWithNormals()
        } else {
            val xan = MathUtils.SINE[rotation]
            val zan = MathUtils.COSINE[rotation]
            synchronized(this) {
                for (currentDepth in 0 until this.maxDepth) {
                    var tmpX = xan * this.verticesZ!![currentDepth] + zan * this.verticesX!![currentDepth] shr 14
                    this.verticesZ!![currentDepth] = zan * this.verticesZ!![currentDepth] - xan * this.verticesX!![currentDepth] shr 14
                    this.verticesX!![currentDepth] = tmpX
                    if (this.vertexNormals!![currentDepth] != null) {
                        tmpX = xan * this.vertexNormals!![currentDepth]!!.z + zan * this.vertexNormals!![currentDepth]!!.x shr 14
                        this.vertexNormals!![currentDepth]!!.z = zan * this.vertexNormals!![currentDepth]!!.z - xan * this.vertexNormals!![currentDepth]!!.x shr 14
                        this.vertexNormals!![currentDepth]!!.x = tmpX
                    }
                }
                if (this.facesNormal != null) {
                    for (faceIndex in 0 until this.numFaces) {
                        if (this.facesNormal!![faceIndex] != null) {
                            val tmpX = xan * this.facesNormal!![faceIndex]!!.z + zan * this.facesNormal!![faceIndex]!!.x shr 14
                            this.facesNormal!![faceIndex]!!.z = zan * this.facesNormal!![faceIndex]!!.z - xan * this.facesNormal!![faceIndex]!!.x shr 14
                            this.facesNormal!![faceIndex]!!.x = tmpX
                        }
                    }
                }
                for (currentDepth in this.maxDepth until this.numVertices) {
                    val tmpX = xan * this.verticesZ!![currentDepth] + zan * this.verticesX!![currentDepth] shr 14
                    this.verticesZ!![currentDepth] = zan * this.verticesZ!![currentDepth] - xan * this.verticesX!![currentDepth] shr 14
                    this.verticesX!![currentDepth] = tmpX
                }
                this.appearanceUpdateStage = 0
                this.boundsUpdated = false
            }
        }
    }

    fun rotate90DegWithoutNormals() {
        synchronized(this) {
            for (verticeIndex in 0 until this.numVertices) {
                val tmpX = this.verticesX!![verticeIndex]
                this.verticesX!![verticeIndex] = this.verticesZ!![verticeIndex]
                this.verticesZ!![verticeIndex] = -tmpX
            }
            invalidate()
        }
    }

    fun rotate170DegWithoutNormals() {
        synchronized(this) {
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesX!![verticeIndex] = -this.verticesX!![verticeIndex]
                this.verticesZ!![verticeIndex] = -this.verticesZ!![verticeIndex]
            }
            invalidate()
        }
    }

    fun rotate270DegWithoutNormals() {
        synchronized(this) {
            for (verticeIndex in 0 until this.numVertices) {
                val tmpX = this.verticesZ!![verticeIndex]
                this.verticesZ!![verticeIndex] = this.verticesX!![verticeIndex]
                this.verticesX!![verticeIndex] = -tmpX
            }
            invalidate()
        }
    }

    fun rotate180DegWithNormals() {
        synchronized(this) {
            for (currentDepth in 0 until this.maxDepth) {
                this.verticesX!![currentDepth] = -this.verticesX!![currentDepth]
                this.verticesZ!![currentDepth] = -this.verticesZ!![currentDepth]
                if (this.vertexNormals!![currentDepth] != null) {
                    this.vertexNormals!![currentDepth]!!.x = -this.vertexNormals!![currentDepth]!!.x
                    this.vertexNormals!![currentDepth]!!.z = -this.vertexNormals!![currentDepth]!!.z
                }
            }
            if (this.facesNormal != null) {
                for (faceIndex in 0 until this.numFaces) {
                    if (this.facesNormal!![faceIndex] != null) {
                        this.facesNormal!![faceIndex]!!.x = -this.facesNormal!![faceIndex]!!.x
                        this.facesNormal!![faceIndex]!!.z = -this.facesNormal!![faceIndex]!!.z
                    }
                }
            }
            for (currentDepth in this.maxDepth until this.numVertices) {
                this.verticesX!![currentDepth] = -this.verticesX!![currentDepth]
                this.verticesZ!![currentDepth] = -this.verticesZ!![currentDepth]
            }
            this.appearanceUpdateStage = 0
            this.boundsUpdated = false
        }
    }

    fun rotate270DegWithNormals() {
        synchronized(this) {
            for (currentDepth in 0 until this.maxDepth) {
                var tmpX = this.verticesZ!![currentDepth]
                this.verticesZ!![currentDepth] = this.verticesX!![currentDepth]
                this.verticesX!![currentDepth] = -tmpX
                if (this.vertexNormals!![currentDepth] != null) {
                    tmpX = this.vertexNormals!![currentDepth]!!.z
                    this.vertexNormals!![currentDepth]!!.z = this.vertexNormals!![currentDepth]!!.x
                    this.vertexNormals!![currentDepth]!!.x = -tmpX
                }
            }
            if (this.facesNormal != null) {
                for (faceIndex in 0 until this.numFaces) {
                    if (this.facesNormal!![faceIndex] != null) {
                        val tmpX = this.facesNormal!![faceIndex]!!.z
                        this.facesNormal!![faceIndex]!!.z = this.facesNormal!![faceIndex]!!.x
                        this.facesNormal!![faceIndex]!!.x = -tmpX
                    }
                }
            }
            for (currentDepth in this.maxDepth until this.numVertices) {
                val tmpX = this.verticesZ!![currentDepth]
                this.verticesZ!![currentDepth] = this.verticesX!![currentDepth]
                this.verticesX!![currentDepth] = -tmpX
            }
            this.appearanceUpdateStage = 0
            this.boundsUpdated = false
        }
    }

    override fun xaxisRotateWithoutNormals(rotation: Int) {
        if ((this.functionMask and (ModelFunctionMask.DISPLACE_Y or ModelFunctionMask.DISPLACE_Z)) != 6) {
            throw IllegalStateException()
        } else {
            val xan = MathUtils.SINE[rotation]
            val zan = MathUtils.COSINE[rotation]
            synchronized(this) {
                for (i_5 in 0 until this.numVertices) {
                    val tmpY = zan * this.verticesY!![i_5] - xan * this.verticesZ!![i_5] shr 14
                    this.verticesZ!![i_5] = xan * this.verticesY!![i_5] + zan * this.verticesZ!![i_5] shr 14
                    this.verticesY!![i_5] = tmpY
                }
                invalidate()
            }
        }
    }

    override fun zaxisRotateWithoutNormals(rotation: Int) {
        if ((this.functionMask and (ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Y)) != 3) {
            throw IllegalStateException()
        } else {
            val xan = MathUtils.SINE[rotation]
            val zan = MathUtils.COSINE[rotation]
            synchronized(this) {
                for (verticeIndex in 0 until this.numVertices) {
                    val tmpX = xan * this.verticesY!![verticeIndex] + zan * this.verticesX!![verticeIndex] shr 14
                    this.verticesY!![verticeIndex] = zan * this.verticesY!![verticeIndex] - xan * this.verticesX!![verticeIndex] shr 14
                    this.verticesX!![verticeIndex] = tmpX
                }
                invalidate()
            }
        }
    }

    override fun mirror() {
        if ((this.functionMask and ModelFunctionMask.INVERT) != 16) {
            throw IllegalStateException()
        }
        synchronized(this) {
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesZ!![verticeIndex] = -this.verticesZ!![verticeIndex]
            }
            if (this.vertexNormals != null) {
                for (verticeIndex in 0 until this.maxDepth) {
                    if (this.vertexNormals!![verticeIndex] != null) {
                        this.vertexNormals!![verticeIndex]!!.z = -this.vertexNormals!![verticeIndex]!!.z
                    }
                }
            }
            if (this.vertexNormalOffsets != null) {
                for (currentDepth in 0 until this.maxDepth) {
                    if (this.vertexNormalOffsets!![currentDepth] != null) {
                        this.vertexNormalOffsets!![currentDepth]!!.z = -this.vertexNormalOffsets!![currentDepth]!!.z
                    }
                }
            }
            if (this.facesNormal != null) {
                for (faceIndex in 0 until this.numFaces) {
                    if (this.facesNormal!![faceIndex] != null) {
                        this.facesNormal!![faceIndex]!!.z = -this.facesNormal!![faceIndex]!!.z
                    }
                }
            }
            val tmpA = this.facesA
            this.facesA = this.facesC
            this.facesC = tmpA
            if (this.uMap != null) {
                for (faceIndex in 0 until this.numFaces) {
                    var tmpV: Float
                    if (this.uMap!![faceIndex] != null) {
                        tmpV = this.uMap!![faceIndex]!![0]
                        this.uMap!![faceIndex]!![0] = this.uMap!![faceIndex]!![2]
                        this.uMap!![faceIndex]!![2] = tmpV
                    }
                    if (this.vMap!![faceIndex] != null) {
                        tmpV = this.vMap!![faceIndex]!![0]
                        this.vMap!![faceIndex]!![0] = this.vMap!![faceIndex]!![2]
                        this.vMap!![faceIndex]!![2] = tmpV
                    }
                }
            }
            this.boundsUpdated = false
            this.appearanceUpdateStage = 0
        }
    }

    override fun scale(x: Int, y: Int, z: Int) {
        if (x != 128 && (this.functionMask and ModelFunctionMask.DISPLACE_X) != 1) {
            throw IllegalStateException()
        }
        if (y != 128 && (this.functionMask and ModelFunctionMask.DISPLACE_Y) != 2) {
            throw IllegalStateException()
        }
        if (z != 128 && (this.functionMask and ModelFunctionMask.DISPLACE_Z) != 4) {
            throw IllegalStateException()
        }
        synchronized(this) {
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesX!![verticeIndex] = x * this.verticesX!![verticeIndex] shr 7
                this.verticesY!![verticeIndex] = y * this.verticesY!![verticeIndex] shr 7
                this.verticesZ!![verticeIndex] = z * this.verticesZ!![verticeIndex] shr 7
            }
            this.boundsUpdated = false
        }
    }

    fun invalidate() {
        this.vertexNormals = null
        this.vertexNormalOffsets = null
        this.facesNormal = null
        this.boundsUpdated = false
    }

    override fun updateParticlePosition(projectionMatrix: Matrix43) {
        copyDependencies(this.toolkit.getThreadResource(Thread.currentThread())!!)
        val particleMatrix = this.threadResource!!.particleMatrix
        particleMatrix.fromMatrix(projectionMatrix)
        if (this.particleEmitters != null) {
            for (emitterIndex in this.particleEmitters!!.indices) {
                val emitter = this.particleEmitters!![emitterIndex]
                val childEmitter = emitter.child ?: emitter
                childEmitter.verticeXA = (particleMatrix.elements[12] + particleMatrix.elements[0] * this.verticesX!![emitter.faceA] + particleMatrix.elements[4] * this.verticesY!![emitter.faceA] + particleMatrix.elements[8] * this.verticesZ!![emitter.faceA]).toInt()
                childEmitter.verticeYA = (particleMatrix.elements[13] + particleMatrix.elements[1] * this.verticesX!![emitter.faceA] + particleMatrix.elements[5] * this.verticesY!![emitter.faceA] + particleMatrix.elements[9] * this.verticesZ!![emitter.faceA]).toInt()
                childEmitter.verticeZA = (particleMatrix.elements[14] + particleMatrix.elements[2] * this.verticesX!![emitter.faceA] + particleMatrix.elements[6] * this.verticesY!![emitter.faceA] + particleMatrix.elements[10] * this.verticesZ!![emitter.faceA]).toInt()
                childEmitter.verticeXB = (particleMatrix.elements[12] + particleMatrix.elements[0] * this.verticesX!![emitter.faceB] + particleMatrix.elements[4] * this.verticesY!![emitter.faceB] + particleMatrix.elements[8] * this.verticesZ!![emitter.faceB]).toInt()
                childEmitter.verticeYB = (particleMatrix.elements[13] + particleMatrix.elements[1] * this.verticesX!![emitter.faceB] + particleMatrix.elements[5] * this.verticesY!![emitter.faceB] + particleMatrix.elements[9] * this.verticesZ!![emitter.faceB]).toInt()
                childEmitter.verticeZB = (particleMatrix.elements[14] + particleMatrix.elements[2] * this.verticesX!![emitter.faceB] + particleMatrix.elements[6] * this.verticesY!![emitter.faceB] + particleMatrix.elements[10] * this.verticesZ!![emitter.faceB]).toInt()
                childEmitter.verticeXC = (particleMatrix.elements[12] + particleMatrix.elements[0] * this.verticesX!![emitter.faceC] + particleMatrix.elements[4] * this.verticesY!![emitter.faceC] + particleMatrix.elements[8] * this.verticesZ!![emitter.faceC]).toInt()
                childEmitter.verticeYC = (particleMatrix.elements[13] + particleMatrix.elements[1] * this.verticesX!![emitter.faceC] + particleMatrix.elements[5] * this.verticesY!![emitter.faceC] + particleMatrix.elements[9] * this.verticesZ!![emitter.faceC]).toInt()
                childEmitter.verticeZC = (particleMatrix.elements[14] + particleMatrix.elements[2] * this.verticesX!![emitter.faceC] + particleMatrix.elements[6] * this.verticesY!![emitter.faceC] + particleMatrix.elements[10] * this.verticesZ!![emitter.faceC]).toInt()
            }
        }
        if (this.effectors != null) {
            for (emitterIndex in this.effectors!!.indices) {
                val effectiveVertex = this.effectors!![emitterIndex]
                val effectiveVertexChild = effectiveVertex.child ?: effectiveVertex
                if (effectiveVertex.matrix != null) {
                    effectiveVertex.matrix!!.copyFrom(particleMatrix)
                } else {
                    effectiveVertex.matrix = Matrix44(particleMatrix)
                }
                effectiveVertexChild.verticeX = (particleMatrix.elements[12] + particleMatrix.elements[0] * this.verticesX!![effectiveVertex.appliesOnVertex] + particleMatrix.elements[4] * this.verticesY!![effectiveVertex.appliesOnVertex] + particleMatrix.elements[8] * this.verticesZ!![effectiveVertex.appliesOnVertex]).toInt()
                effectiveVertexChild.verticeY = (particleMatrix.elements[13] + particleMatrix.elements[1] * this.verticesX!![effectiveVertex.appliesOnVertex] + particleMatrix.elements[5] * this.verticesY!![effectiveVertex.appliesOnVertex] + particleMatrix.elements[9] * this.verticesZ!![effectiveVertex.appliesOnVertex]).toInt()
                effectiveVertexChild.verticeZ = (particleMatrix.elements[14] + particleMatrix.elements[2] * this.verticesX!![effectiveVertex.appliesOnVertex] + particleMatrix.elements[6] * this.verticesY!![effectiveVertex.appliesOnVertex] + particleMatrix.elements[10] * this.verticesZ!![effectiveVertex.appliesOnVertex]).toInt()
            }
        }
    }

    override fun unlock() {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized(this) {
                super.isLocked = false
                (this as Object).notifyAll()
            }
        }
    }

    override fun endAnimation() {
        if (this.upscaled) {
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] + 7 shr 4
                this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] + 7 shr 4
                this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] + 7 shr 4
            }
            this.upscaled = false
        }
        if (this.calculateColors) {
            recalculateColorsThreaded()
            this.calculateColors = false
        }
        this.boundsUpdated = false
    }

    override fun animatePartialtransform(transformationType: Int, labels: IntArray, x: Int, y: Int, z: Int, rotation: Int, lights: Boolean) {
        var x1 = x
        var y1 = y
        var z1 = z
        val labelCount = labels.size
        if (transformationType == 0) {
            x1 = x1 shl 4
            y1 = y1 shl 4
            z1 = z1 shl 4
            if (!this.upscaled) {
                for (verticeIndex in 0 until this.numVertices) {
                    this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                    this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                    this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                }
                this.upscaled = true
            }
            var transformationCount = 0
            this.originX = 0
            this.originY = 0
            this.originZ = 0
            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < this.vertexGroups!!.size) {
                    val verticesIndices = this.vertexGroups!![group]!!
                    for (index in verticesIndices.indices) {
                        val verticeIndex = verticesIndices[index]
                        this.originX += this.verticesX!![verticeIndex]
                        this.originY += this.verticesY!![verticeIndex]
                        this.originZ += this.verticesZ!![verticeIndex]
                        transformationCount++
                    }
                }
            }
            if (transformationCount > 0) {
                this.originX = x1 + this.originX / transformationCount
                this.originY = this.originY / transformationCount + y1
                this.originZ = z1 + this.originZ / transformationCount
            } else {
                this.originX = x1
                this.originY = y1
                this.originZ = z1
            }
        } else if (transformationType == 1) {
            x1 = x1 shl 4
            y1 = y1 shl 4
            z1 = z1 shl 4
            if (!this.upscaled) {
                for (verticeIndex in 0 until this.numVertices) {
                    this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                    this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                    this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                }
                this.upscaled = true
            }
            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < this.vertexGroups!!.size) {
                    val verticeIndices = this.vertexGroups!![group]!!
                    for (index in verticeIndices.indices) {
                        val vertex = verticeIndices[index]
                        this.verticesX!![vertex] += x1
                        this.verticesY!![vertex] += y1
                        this.verticesZ!![vertex] += z1
                    }
                }
            }
        } else if (transformationType == 2) {
            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < this.vertexGroups!!.size) {
                    val verticeIndices = this.vertexGroups!![group]!!
                    if ((rotation and 0x1) == 0) {
                        for (index in verticeIndices.indices) {
                            val vertice = verticeIndices[index]
                            this.verticesX!![vertice] -= this.originX
                            this.verticesY!![vertice] -= this.originY
                            this.verticesZ!![vertice] -= this.originZ
                            if (z1 != 0) {
                                val xan = MathUtils.SINE[z1]
                                val zan = MathUtils.COSINE[z1]
                                val tmpY = xan * this.verticesY!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = zan * this.verticesY!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesX!![vertice] = tmpY
                            }
                            if (x1 != 0) {
                                val xan = MathUtils.SINE[x1]
                                val zan = MathUtils.COSINE[x1]
                                val tmpZ = zan * this.verticesY!![vertice] - xan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesZ!![vertice] = xan * this.verticesY!![vertice] + zan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = tmpZ
                            }
                            if (y1 != 0) {
                                val xan = MathUtils.SINE[y1]
                                val zan = MathUtils.COSINE[y1]
                                val tmpZ = xan * this.verticesZ!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesZ!![vertice] = zan * this.verticesZ!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesX!![vertice] = tmpZ
                            }
                            this.verticesX!![vertice] += this.originX
                            this.verticesY!![vertice] += this.originY
                            this.verticesZ!![vertice] += this.originZ
                        }
                    } else {
                        for (index in verticeIndices.indices) {
                            val vertice = verticeIndices[index]
                            this.verticesX!![vertice] -= this.originX
                            this.verticesY!![vertice] -= this.originY
                            this.verticesZ!![vertice] -= this.originZ
                            if (x1 != 0) {
                                val xan = MathUtils.SINE[x1]
                                val zan = MathUtils.COSINE[x1]
                                val tmpZ = zan * this.verticesY!![vertice] - xan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesZ!![vertice] = xan * this.verticesY!![vertice] + zan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = tmpZ
                            }
                            if (z1 != 0) {
                                val xan = MathUtils.SINE[z1]
                                val zan = MathUtils.COSINE[z1]
                                val tmpY = xan * this.verticesY!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = zan * this.verticesY!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesX!![vertice] = tmpY
                            }
                            if (y1 != 0) {
                                val xan = MathUtils.SINE[y1]
                                val zan = MathUtils.COSINE[y1]
                                val tmpZ = xan * this.verticesZ!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesZ!![vertice] = zan * this.verticesZ!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesX!![vertice] = tmpZ
                            }
                            this.verticesX!![vertice] += this.originX
                            this.verticesY!![vertice] += this.originY
                            this.verticesZ!![vertice] += this.originZ
                        }
                    }
                }
            }
        } else if (transformationType == 3) {
            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < this.vertexGroups!!.size) {
                    val verticeIndices = this.vertexGroups!![group]!!
                    for (index in verticeIndices.indices) {
                        val vertex = verticeIndices[index]
                        this.verticesX!![vertex] -= this.originX
                        this.verticesY!![vertex] -= this.originY
                        this.verticesZ!![vertex] -= this.originZ
                        this.verticesX!![vertex] = x1 * this.verticesX!![vertex] / 128
                        this.verticesY!![vertex] = y1 * this.verticesY!![vertex] / 128
                        this.verticesZ!![vertex] = z1 * this.verticesZ!![vertex] / 128
                        this.verticesX!![vertex] += this.originX
                        this.verticesY!![vertex] += this.originY
                        this.verticesZ!![vertex] += this.originZ
                    }
                }
            }
        } else if (transformationType == 5) {
            if (this.faceGroups != null && this.facesAlpha != null) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.faceGroups!!.size) {
                        val faceIndices = this.faceGroups!![group]!!
                        for (index in faceIndices.indices) {
                            val face = faceIndices[index]
                            var alpha = (this.facesAlpha!![face].toInt() and 0xff) + x1 * 8
                            if (alpha < 0) {
                                alpha = 0
                            } else if (alpha > 255) {
                                alpha = 255
                            }
                            this.facesAlpha!![face] = alpha.toByte()
                        }
                    }
                }
                if (this.billboardEffects != null) {
                    for (billboardIndex in 0 until this.numBillboards) {
                        val effect = this.billboardEffects!![billboardIndex]!!
                        val surface = this.billboardSurfaces!![billboardIndex]!!
                        surface.diffuseColor = surface.diffuseColor and 0xffffff or (255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff) shl 24)
                    }
                }
            }
        } else if (transformationType == 7) {
            if (this.faceGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.faceGroups!!.size) {
                        val faceIndices = this.faceGroups!![label]!!
                        for (index in faceIndices.indices) {
                            val face = faceIndices[index]
                            val faceColorHsl = this.facesColor!![face].toInt() and 0xffff
                            var h = faceColorHsl shr 10 and 0x3f
                            var s = faceColorHsl shr 7 and 0x7
                            var l = faceColorHsl and 0x7f
                            h = x1 + h and 0x3f
                            s += y1
                            if (s < 0) {
                                s = 0
                            } else if (s > 7) {
                                s = 7
                            }
                            l += z1
                            if (l < 0) {
                                l = 0
                            } else if (l > 127) {
                                l = 127
                            }
                            this.facesColor!![face] = (h shl 10 or (s shl 7) or l).toShort()
                        }
                        this.calculateColors = true
                    }
                }
                if (this.billboardEffects != null) {
                    for (billboardIndex in 0 until this.numBillboards) {
                        val effect = this.billboardEffects!![billboardIndex]!!
                        val surface = this.billboardSurfaces!![billboardIndex]!!
                        surface.diffuseColor = surface.diffuseColor and 0xffffff.inv() or (ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(this.facesColor!![effect.appliesOnFace].toInt() and 0xffff).toInt() and 0xffff] and 0xffffff)
                    }
                }
            }
        } else if (transformationType == 8) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val billboardIndices = this.billboardGroups!![label]!!
                        for (index in billboardIndices.indices) {
                            val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                            surface.offsetX += x1
                            surface.offsetY += y1
                        }
                    }
                }
            }
        } else if (transformationType == 10) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.billboardGroups!!.size) {
                        val billboardIndices = this.billboardGroups!![group]!!
                        for (index in billboardIndices.indices) {
                            val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                            surface.scaleX = surface.scaleX * x1 / 128.0f
                            surface.scaleY = surface.scaleY * y1 / 128.0f
                        }
                    }
                }
            }
        } else if (transformationType == 9 && this.billboardGroups != null) {
            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < this.billboardGroups!!.size) {
                    val billboardIndices = this.billboardGroups!![group]!!
                    for (index in billboardIndices.indices) {
                        val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                        surface.rotation = x1 + surface.rotation and 0x3fff
                    }
                }
            }
        }
    }

    override fun animateTransform(transformationType: Int, labels: IntArray, rotationX: Int, rotationY: Int, rotationZ: Int, lights: Boolean, submesh: Int, verticesData: IntArray) {
        var rotX = rotationX
        var rotY = rotationY
        var rotZ = rotationZ
        val labelCount = labels.size
        if (transformationType == 0) {
            rotX = rotX shl 4
            rotY = rotY shl 4
            rotZ = rotZ shl 4
            if (!this.upscaled) {
                for (verticeIndex in 0 until this.numVertices) {
                    this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                    this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                    this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                }
                this.upscaled = true
            }
            var transformationCount = 0
            this.originX = 0
            this.originY = 0
            this.originZ = 0
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < this.vertexGroups!!.size) {
                    val vertices = this.vertexGroups!![label]!!
                    for (verticeIndex in vertices.indices) {
                        val vertice = vertices[verticeIndex]
                        if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                            this.originX += this.verticesX!![vertice]
                            this.originY += this.verticesY!![vertice]
                            this.originZ += this.verticesZ!![vertice]
                            transformationCount++
                        }
                    }
                }
            }
            if (transformationCount > 0) {
                this.originX = rotX + this.originX / transformationCount
                this.originY = this.originY / transformationCount + rotY
                this.originZ = rotZ + this.originZ / transformationCount
                this.withinOrigin = true
            } else {
                this.originX = rotX
                this.originY = rotY
                this.originZ = rotZ
            }
        } else if (transformationType == 1) {
            if (verticesData.isNotEmpty()) {
                val transformX = rotZ * verticesData[2] + verticesData[1] * rotY + rotX * verticesData[0] + 8192 shr 14
                val transformY = rotX * verticesData[3] + verticesData[5] * rotZ + rotY * verticesData[4] + 8192 shr 14
                val transformZ = rotX * verticesData[6] + verticesData[7] * rotY + rotZ * verticesData[8] + 8192 shr 14
                rotX = transformX
                rotY = transformY
                rotZ = transformZ
            }
            rotX = rotX shl 4
            rotY = rotY shl 4
            rotZ = rotZ shl 4
            if (!this.upscaled) {
                for (verticeIndex in 0 until this.numVertices) {
                    this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                    this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                    this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                }
                this.upscaled = true
            }
            for (labelIndex in 0 until labelCount) {
                val groups = labels[labelIndex]
                if (groups < this.vertexGroups!!.size) {
                    val vertices = this.vertexGroups!![groups]!!
                    for (vertIndex in vertices.indices) {
                        val vertice = vertices[vertIndex]
                        if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                            this.verticesX!![vertice] += rotX
                            this.verticesY!![vertice] += rotY
                            this.verticesZ!![vertice] += rotZ
                        }
                    }
                }
            }
        } else if (transformationType == 2) {
            if (verticesData.isNotEmpty()) {
                if (!this.upscaled) {
                    for (verticeIndex in 0 until this.numVertices) {
                        this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                        this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                        this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                    }
                    this.upscaled = true
                }
                val originalX = verticesData[9] shl 4
                val originalY = verticesData[10] shl 4
                val originalZ = verticesData[11] shl 4
                val deltaX = verticesData[12] shl 4
                val deltaY = verticesData[13] shl 4
                val deltaZ = verticesData[14] shl 4
                if (this.withinOrigin) {
                    var x = verticesData[6] * this.originZ + verticesData[3] * this.originY + verticesData[0] * this.originX + 8192 shr 14
                    var y = verticesData[7] * this.originZ + verticesData[1] * this.originX + verticesData[4] * this.originY + 8192 shr 14
                    var z = verticesData[8] * this.originZ + verticesData[2] * this.originX + verticesData[5] * this.originY + 8192 shr 14
                    x += deltaX
                    y += deltaY
                    z += deltaZ
                    this.originX = x
                    this.originY = y
                    this.originZ = z
                    this.withinOrigin = false
                }
                val offsets1 = IntArray(9)
                val zanX = MathUtils.COSINE[rotX]
                val xanX = MathUtils.SINE[rotX]
                val zanY = MathUtils.COSINE[rotY]
                val xanY = MathUtils.SINE[rotY]
                val zanZ = MathUtils.COSINE[rotZ]
                val xanZ = MathUtils.SINE[rotZ]
                val i_23 = xanX * zanZ + 8192 shr 14
                val i_24 = xanX * xanZ + 8192 shr 14
                offsets1[0] = zanY * zanZ + xanY * i_24 + 8192 shr 14
                offsets1[1] = xanY * i_23 + -zanY * xanZ + 8192 shr 14
                offsets1[2] = xanY * zanX + 8192 shr 14
                offsets1[3] = zanX * xanZ + 8192 shr 14
                offsets1[4] = zanX * zanZ + 8192 shr 14
                offsets1[5] = -xanX
                offsets1[6] = zanY * i_24 + -xanY * zanZ + 8192 shr 14
                offsets1[7] = xanY * xanZ + zanY * i_23 + 8192 shr 14
                offsets1[8] = zanY * zanX + 8192 shr 14
                val offsetX = offsets1[2] * -this.originZ + offsets1[0] * -this.originX + offsets1[1] * -this.originY + 8192 shr 14
                val offsetY = offsets1[5] * -this.originZ + offsets1[4] * -this.originY + offsets1[3] * -this.originX + 8192 shr 14
                val offsetZ = offsets1[6] * -this.originX + offsets1[8] * -this.originZ + offsets1[7] * -this.originY + 8192 shr 14
                val offsetOriginX = offsetX + this.originX
                val offsetOriginY = offsetY + this.originY
                val offsetOriginZ = offsetZ + this.originZ
                val offsets2 = IntArray(9)
                for (x2 in 0..2) {
                    for (y2 in 0..2) {
                        var i_34 = 0
                        for (x in 0..2) {
                            i_34 += verticesData[x + y2 * 3] * offsets1[x + x2 * 3]
                        }
                        offsets2[y2 + x2 * 3] = i_34 + 8192 shr 14
                    }
                }
                var fullOffX = deltaY * offsets1[1] + deltaX * offsets1[0] + deltaZ * offsets1[2] + 8192 shr 14
                var fullOffY = deltaY * offsets1[4] + deltaX * offsets1[3] + deltaZ * offsets1[5] + 8192 shr 14
                var fullOffZ = offsets1[6] * deltaX + deltaZ * offsets1[8] + deltaY * offsets1[7] + 8192 shr 14
                fullOffX += offsetOriginX
                fullOffY += offsetOriginY
                fullOffZ += offsetOriginZ
                val offsets3 = IntArray(9)
                for (x3 in 0..2) {
                    for (y3 in 0..2) {
                        var offset = 0
                        for (x2 in 0..2) {
                            offset += offsets2[y3 + x2 * 3] * verticesData[x2 + x3 * 3]
                        }
                        offsets3[y3 + x3 * 3] = offset + 8192 shr 14
                    }
                }
                var translateX = fullOffZ * verticesData[2] + fullOffX * verticesData[0] + fullOffY * verticesData[1] + 8192 shr 14
                var translateY = fullOffX * verticesData[3] + fullOffY * verticesData[4] + fullOffZ * verticesData[5] + 8192 shr 14
                var translateZ = fullOffX * verticesData[6] + fullOffZ * verticesData[8] + fullOffY * verticesData[7] + 8192 shr 14
                translateX += originalX
                translateY += originalY
                translateZ += originalZ
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.vertexGroups!!.size) {
                        val verticeIndices = this.vertexGroups!![label]!!
                        for (index in verticeIndices.indices) {
                            val vertice = verticeIndices[index]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                                val x = offsets3[0] * this.verticesX!![vertice] + offsets3[2] * this.verticesZ!![vertice] + offsets3[1] * this.verticesY!![vertice] + 8192 shr 14
                                val y = offsets3[4] * this.verticesY!![vertice] + offsets3[5] * this.verticesZ!![vertice] + offsets3[3] * this.verticesX!![vertice] + 8192 shr 14
                                val z = offsets3[6] * this.verticesX!![vertice] + offsets3[7] * this.verticesY!![vertice] + offsets3[8] * this.verticesZ!![vertice] + 8192 shr 14
                                this.verticesX!![vertice] = x + translateX
                                this.verticesY!![vertice] = y + translateY
                                this.verticesZ!![vertice] = z + translateZ
                            }
                        }
                    }
                }
            } else {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.vertexGroups!!.size) {
                        val vertices = this.vertexGroups!![label]!!
                        for (vertIndex in vertices.indices) {
                            val vertice = vertices[vertIndex]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                                this.verticesX!![vertice] -= this.originX
                                this.verticesY!![vertice] -= this.originY
                                this.verticesZ!![vertice] -= this.originZ
                                if (rotZ != 0) {
                                    val xan = MathUtils.SINE[rotZ]
                                    val zan = MathUtils.COSINE[rotZ]
                                    val tmpY = xan * this.verticesY!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                    this.verticesY!![vertice] = zan * this.verticesY!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                    this.verticesX!![vertice] = tmpY
                                }
                                if (rotX != 0) {
                                    val xan = MathUtils.SINE[rotX]
                                    val zan = MathUtils.COSINE[rotX]
                                    val tmpZ = zan * this.verticesY!![vertice] - xan * this.verticesZ!![vertice] + 16383 shr 14
                                    this.verticesZ!![vertice] = xan * this.verticesY!![vertice] + zan * this.verticesZ!![vertice] + 16383 shr 14
                                    this.verticesY!![vertice] = tmpZ
                                }
                                if (rotY != 0) {
                                    val xan = MathUtils.SINE[rotY]
                                    val zan = MathUtils.COSINE[rotY]
                                    val tmpZ = xan * this.verticesZ!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                    this.verticesZ!![vertice] = zan * this.verticesZ!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                    this.verticesX!![vertice] = tmpZ
                                }
                                this.verticesX!![vertice] += this.originX
                                this.verticesY!![vertice] += this.originY
                                this.verticesZ!![vertice] += this.originZ
                            }
                        }
                    }
                }
            }
        } else if (transformationType == 3) {
            if (verticesData.isNotEmpty()) {
                if (!this.upscaled) {
                    for (verticeIndex in 0 until this.numVertices) {
                        this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                        this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                        this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                    }
                    this.upscaled = true
                }
                val originalX = verticesData[9] shl 4
                val originalY = verticesData[10] shl 4
                val originalZ = verticesData[11] shl 4
                val deltaX = verticesData[12] shl 4
                val deltaY = verticesData[13] shl 4
                val deltaZ = verticesData[14] shl 4
                if (this.withinOrigin) {
                    var x = verticesData[6] * this.originZ + verticesData[3] * this.originY + verticesData[0] * this.originX + 8192 shr 14
                    var y = verticesData[7] * this.originZ + verticesData[1] * this.originX + verticesData[4] * this.originY + 8192 shr 14
                    var z = verticesData[8] * this.originZ + verticesData[2] * this.originX + verticesData[5] * this.originY + 8192 shr 14
                    x += deltaX
                    y += deltaY
                    z += deltaZ
                    this.originX = x
                    this.originY = y
                    this.originZ = z
                    this.withinOrigin = false
                }
                val rotXScaled = rotX shl 15 shr 7
                val rotYScaled = rotY shl 15 shr 7
                val rotZScaled = rotZ shl 15 shr 7
                val offX = rotXScaled * -this.originX + 8192 shr 14
                val offY = rotYScaled * -this.originY + 8192 shr 14
                val offZ = rotZScaled * -this.originZ + 8192 shr 14
                val originXOffset = offX + this.originX
                val originYOffset = offY + this.originY
                val originZOffset = offZ + this.originZ
                val offsetCluster = intArrayOf(
                    rotXScaled * verticesData[0] + 8192 shr 14, rotXScaled * verticesData[3] + 8192 shr 14,
                    rotXScaled * verticesData[6] + 8192 shr 14, rotYScaled * verticesData[1] + 8192 shr 14,
                    rotYScaled * verticesData[4] + 8192 shr 14, rotYScaled * verticesData[7] + 8192 shr 14,
                    rotZScaled * verticesData[2] + 8192 shr 14, rotZScaled * verticesData[5] + 8192 shr 14,
                    rotZScaled * verticesData[8] + 8192 shr 14
                )
                var offsetRotX = rotXScaled * deltaX + 8192 shr 14
                var offsetRotY = rotYScaled * deltaY + 8192 shr 14
                var offsetRotZ = rotZScaled * deltaZ + 8192 shr 14
                offsetRotX += originXOffset
                offsetRotY += originYOffset
                offsetRotZ += originZOffset
                val offsetMatrix = IntArray(9)
                for (w in 0..2) {
                    for (h in 0..2) {
                        var off = 0
                        for (z2 in 0..2) {
                            off += offsetCluster[h + z2 * 3] * verticesData[z2 + w * 3]
                        }
                        offsetMatrix[h + w * 3] = off + 8192 shr 14
                    }
                }
                var finalOffX = offsetRotZ * verticesData[2] + offsetRotX * verticesData[0] + verticesData[1] * offsetRotY + 8192 shr 14
                var finalOffY = offsetRotX * verticesData[3] + offsetRotY * verticesData[4] + verticesData[5] * offsetRotZ + 8192 shr 14
                var finalOffZ = offsetRotX * verticesData[6] + offsetRotZ * verticesData[8] + verticesData[7] * offsetRotY + 8192 shr 14
                finalOffX += originalX
                finalOffY += originalY
                finalOffZ += originalZ
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.vertexGroups!!.size) {
                        val vertices = this.vertexGroups!![label]!!
                        for (index in vertices.indices) {
                            val vertice = vertices[index]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                                val x = offsetMatrix[0] * this.verticesX!![vertice] + offsetMatrix[2] * this.verticesZ!![vertice] + offsetMatrix[1] * this.verticesY!![vertice] + 8192 shr 14
                                val y = offsetMatrix[4] * this.verticesY!![vertice] + offsetMatrix[5] * this.verticesZ!![vertice] + offsetMatrix[3] * this.verticesX!![vertice] + 8192 shr 14
                                val z = offsetMatrix[6] * this.verticesX!![vertice] + offsetMatrix[7] * this.verticesY!![vertice] + offsetMatrix[8] * this.verticesZ!![vertice] + 8192 shr 14
                                this.verticesX!![vertice] = x + finalOffX
                                this.verticesY!![vertice] = y + finalOffY
                                this.verticesZ!![vertice] = z + finalOffZ
                            }
                        }
                    }
                }
            } else {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.vertexGroups!!.size) {
                        val vertices = this.vertexGroups!![group]!!
                        for (vertIndex in vertices.indices) {
                            val vertex = vertices[vertIndex]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertex].toInt()) != 0) {
                                this.verticesX!![vertex] -= this.originX
                                this.verticesY!![vertex] -= this.originY
                                this.verticesZ!![vertex] -= this.originZ
                                this.verticesX!![vertex] = rotX * this.verticesX!![vertex] / 128
                                this.verticesY!![vertex] = rotY * this.verticesY!![vertex] / 128
                                this.verticesZ!![vertex] = rotZ * this.verticesZ!![vertex] / 128
                                this.verticesX!![vertex] += this.originX
                                this.verticesY!![vertex] += this.originY
                                this.verticesZ!![vertex] += this.originZ
                            }
                        }
                    }
                }
            }
        } else if (transformationType == 5) {
            if (this.faceGroups != null && this.facesAlpha != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.faceGroups!!.size) {
                        val vertices = this.faceGroups!![label]!!
                        for (vertIndex in vertices.indices) {
                            val vertice = vertices[vertIndex]
                            if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![vertice].toInt()) != 0) {
                                var alpha = (this.facesAlpha!![vertice].toInt() and 0xff) + rotX * 8
                                if (alpha < 0) {
                                    alpha = 0
                                } else if (alpha > 255) {
                                    alpha = 255
                                }
                                this.facesAlpha!![vertice] = alpha.toByte()
                            }
                        }
                    }
                }
                if (this.billboardEffects != null) {
                    for (billboardIndex in 0 until this.numBillboards) {
                        val effect = this.billboardEffects!![billboardIndex]!!
                        val surface = this.billboardSurfaces!![billboardIndex]!!
                        surface.diffuseColor = surface.diffuseColor and 0xffffff or (255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff) shl 24)
                    }
                }
            }
        } else if (transformationType == 7) {
            if (this.faceGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.faceGroups!!.size) {
                        val faces = this.faceGroups!![label]!!
                        for (vertIndex in faces.indices) {
                            val vertice = faces[vertIndex]
                            if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![vertice].toInt()) != 0) {
                                val color = this.facesColor!![vertice].toInt() and 0xffff
                                var h = color shr 10 and 0x3f
                                var s = color shr 7 and 0x7
                                var l = color and 0x7f
                                h = rotX + h and 0x3f
                                s += rotY
                                if (s < 0) {
                                    s = 0
                                } else if (s > 7) {
                                    s = 7
                                }
                                l += rotZ
                                if (l < 0) {
                                    l = 0
                                } else if (l > 127) {
                                    l = 127
                                }
                                this.facesColor!![vertice] = (h shl 10 or (s shl 7) or l).toShort()
                            }
                        }
                        this.calculateColors = true
                    }
                }
                if (this.billboardEffects != null) {
                    for (billboardIndex in 0 until this.numBillboards) {
                        val effect = this.billboardEffects!![billboardIndex]!!
                        val surface = this.billboardSurfaces!![billboardIndex]!!
                        surface.diffuseColor = surface.diffuseColor and 0xffffff.inv() or (ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(this.facesColor!![effect.appliesOnFace].toInt() and 0xffff).toInt() and 0xffff] and 0xffffff)
                    }
                }
            }
        } else if (transformationType == 8) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val vertices = this.billboardGroups!![label]!!
                        for (vertIndex in vertices.indices) {
                            val surface = this.billboardSurfaces!![vertices[vertIndex]]!!
                            surface.offsetX += rotX
                            surface.offsetY += rotY
                        }
                    }
                }
            }
        } else if (transformationType == 10) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val vertices = this.billboardGroups!![label]!!
                        for (vertIndex in vertices.indices) {
                            val surface = this.billboardSurfaces!![vertices[vertIndex]]!!
                            surface.scaleX = surface.scaleX * rotX / 128.0f
                            surface.scaleY = surface.scaleY * rotY / 128.0f
                        }
                    }
                }
            }
        } else if (transformationType == 9 && this.billboardGroups != null) {
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < this.billboardGroups!!.size) {
                    val vertices = this.billboardGroups!![label]!!
                    for (vertIndex in vertices.indices) {
                        val surface = this.billboardSurfaces!![vertices[vertIndex]]!!
                        surface.rotation = rotX + surface.rotation and 0x3fff
                    }
                }
            }
        }
    }

    override fun animateShadowtransform(transformationType: Int, x: Int, y: Int, z: Int) {
        if (transformationType == 0) {
            var transformCount = 0
            this.originX = 0
            this.originY = 0
            this.originZ = 0
            for (verticeIndex in 0 until this.numVertices) {
                this.originX += this.verticesX!![verticeIndex]
                this.originY += this.verticesY!![verticeIndex]
                this.originZ += this.verticesZ!![verticeIndex]
                transformCount++
            }
            if (transformCount > 0) {
                this.originX = this.originX / transformCount + x
                this.originY = y + this.originY / transformCount
                this.originZ = this.originZ / transformCount + z
            } else {
                this.originX = x
                this.originY = y
                this.originZ = z
            }
        } else if (transformationType == 1) {
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesX!![verticeIndex] += x
                this.verticesY!![verticeIndex] += y
                this.verticesZ!![verticeIndex] += z
            }
        } else if (transformationType == 2) {
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesX!![verticeIndex] -= this.originX
                this.verticesY!![verticeIndex] -= this.originY
                this.verticesZ!![verticeIndex] -= this.originZ
                if (z != 0) {
                    val xan = MathUtils.SINE[z]
                    val zan = MathUtils.COSINE[z]
                    val tmpX = xan * this.verticesY!![verticeIndex] + zan * this.verticesX!![verticeIndex] + 16383 shr 14
                    this.verticesY!![verticeIndex] = zan * this.verticesY!![verticeIndex] - xan * this.verticesX!![verticeIndex] + 16383 shr 14
                    this.verticesX!![verticeIndex] = tmpX
                }
                if (x != 0) {
                    val xan = MathUtils.SINE[x]
                    val zan = MathUtils.COSINE[x]
                    val tmpY = zan * this.verticesY!![verticeIndex] - xan * this.verticesZ!![verticeIndex] + 16383 shr 14
                    this.verticesZ!![verticeIndex] = xan * this.verticesY!![verticeIndex] + zan * this.verticesZ!![verticeIndex] + 16383 shr 14
                    this.verticesY!![verticeIndex] = tmpY
                }
                if (y != 0) {
                    val xan = MathUtils.SINE[y]
                    val zan = MathUtils.COSINE[y]
                    val tmpX = xan * this.verticesZ!![verticeIndex] + zan * this.verticesX!![verticeIndex] + 16383 shr 14
                    this.verticesZ!![verticeIndex] = zan * this.verticesZ!![verticeIndex] - xan * this.verticesX!![verticeIndex] + 16383 shr 14
                    this.verticesX!![verticeIndex] = tmpX
                }
                this.verticesX!![verticeIndex] += this.originX
                this.verticesY!![verticeIndex] += this.originY
                this.verticesZ!![verticeIndex] += this.originZ
            }
        } else if (transformationType == 3) {
            for (verticesIndex in 0 until this.numVertices) {
                this.verticesX!![verticesIndex] -= this.originX
                this.verticesY!![verticesIndex] -= this.originY
                this.verticesZ!![verticesIndex] -= this.originZ
                this.verticesX!![verticesIndex] = x * this.verticesX!![verticesIndex] / 128
                this.verticesY!![verticesIndex] = y * this.verticesY!![verticesIndex] / 128
                this.verticesZ!![verticesIndex] = z * this.verticesZ!![verticesIndex] / 128
                this.verticesX!![verticesIndex] += this.originX
                this.verticesY!![verticesIndex] += this.originY
                this.verticesZ!![verticesIndex] += this.originZ
            }
        } else if (transformationType == 5) {
            for (faceIndex in 0 until this.numFaces) {
                var alpha = (this.facesAlpha!![faceIndex].toInt() and 0xff) + x * 8
                if (alpha < 0) {
                    alpha = 0
                } else if (alpha > 255) {
                    alpha = 255
                }
                this.facesAlpha!![faceIndex] = alpha.toByte()
            }
            if (this.billboardEffects != null) {
                for (faceIndex in 0 until this.numBillboards) {
                    val effect = this.billboardEffects!![faceIndex]!!
                    val surface = this.billboardSurfaces!![faceIndex]!!
                    surface.diffuseColor = surface.diffuseColor and 0xffffff or (255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff) shl 24)
                }
            }
        } else if (transformationType == 7) {
            for (faceIndex in 0 until this.numFaces) {
                val color = this.facesColor!![faceIndex].toInt() and 0xffff
                var h = color shr 10 and 0x3f
                var s = color shr 7 and 0x7
                var l = color and 0x7f
                h = h + x and 0x3f
                s += y
                if (s < 0) {
                    s = 0
                } else if (s > 7) {
                    s = 7
                }
                l += z
                if (l < 0) {
                    l = 0
                } else if (l > 127) {
                    l = 127
                }
                this.facesColor!![faceIndex] = (h shl 10 or (s shl 7) or l).toShort()
            }
            this.calculateColors = true
            if (this.billboardEffects != null) {
                for (faceIndex in 0 until this.numBillboards) {
                    val effect = this.billboardEffects!![faceIndex]!!
                    val surface = this.billboardSurfaces!![faceIndex]!!
                    surface.diffuseColor = surface.diffuseColor and 0xffffff.inv() or (ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(this.facesColor!![effect.appliesOnFace].toInt() and 0xffff).toInt() and 0xffff] and 0xffffff)
                }
            }
        } else if (transformationType == 8) {
            for (faceIndex in 0 until this.numBillboards) {
                val surface = this.billboardSurfaces!![faceIndex]!!
                surface.offsetX += x
                surface.offsetY += y
            }
        } else if (transformationType == 10) {
            for (faceIndex in 0 until this.numBillboards) {
                val surface = this.billboardSurfaces!![faceIndex]!!
                surface.scaleX = surface.scaleX * x / 128.0f
                surface.scaleY = surface.scaleY * y / 128.0f
            }
        } else if (transformationType == 9) {
            for (faceIndex in 0 until this.numBillboards) {
                val surface = this.billboardSurfaces!![faceIndex]!!
                surface.rotation = x + surface.rotation and 0x3fff
            }
        }
    }

    fun multithreadedDraw(matrix44var1: Matrix43, renderedEntity: BoundingBoxPolygon?, rasterizerFlags: Int) {
        if (this.maxDepth >= 1) {
            val resource = this.toolkit.getThreadResource(Thread.currentThread())!!
            val particleMatrix = resource.particleMatrix
            particleMatrix.fromMatrix(matrix44var1)
            val matrix446 = this.toolkit.viewMatrix44!!
            val matrix447 = this.toolkit.modelMatrix!!
            if (!this.boundsUpdated) {
                calculateBounds()
            }
            val bool8 = particleMatrix.isIdentity()
            val particleOffsets = resource.particleOffsets
            particleMatrix.scale(0.0f, this.minY.toFloat(), 0.0f, particleOffsets)
            val offsetXA = particleOffsets!![0]
            val offsetYA = particleOffsets[1]
            val offsetZA = particleOffsets[2]
            particleMatrix.scale(0.0f, this.maxY.toFloat(), 0.0f, particleOffsets)
            val offsetXB = particleOffsets[0]
            val offsetYB = particleOffsets[1]
            val offsetZB = particleOffsets[2]
            var f_18: Float
            var f_19: Float
            for (i_16 in 0..5) {
                val floats_17 = this.toolkit.frustumPlanes!![i_16]
                f_18 = floats_17[0] * offsetXA + floats_17[1] * offsetYA + floats_17[2] * offsetZA + floats_17[3] + this.size2d
                f_19 = floats_17[0] * offsetXB + floats_17[1] * offsetYB + floats_17[2] * offsetZB + floats_17[3] + this.size2d
                if (f_18 < 0.0f && f_19 < 0.0f) {
                    return
                }
            }
            val nNearFaceA: Float
            val zNearFaceD: Float
            if (bool8) {
                zNearFaceD = matrix446.elements[14]
                nNearFaceA = matrix446.elements[6]
            } else {
                zNearFaceD = particleMatrix.elements[12] * matrix446.elements[2] + particleMatrix.elements[13] * matrix446.elements[6] + particleMatrix.elements[14] * matrix446.elements[10] + matrix446.elements[14]
                nNearFaceA = particleMatrix.elements[4] * matrix446.elements[2] + particleMatrix.elements[5] * matrix446.elements[6] + particleMatrix.elements[6] * matrix446.elements[10]
            }
            f_18 = zNearFaceD + this.minY * nNearFaceA
            f_19 = zNearFaceD + this.maxY * nNearFaceA
            val f_20 = if (f_18 > f_19) f_18 + this.size2d else f_19 + this.size2d
            val f_21 = matrix447.elements[10] * f_20 + matrix447.elements[14]
            if (this.toolkit.executingThreadCount > 1) {
                synchronized(this) {
                    while (this.renderLock) {
                        try {
                            (this as Object).wait()
                        } catch (ie: InterruptedException) {
                            // NOOP
                        }
                    }
                    this.renderLock = true
                }
            }
            copyDependencies(resource)
            val renderMatrix = this.threadResource!!.renderingMatrix
            renderMatrix.copyFrom(particleMatrix)
            renderMatrix.multiplyBy(this.toolkit.floorMatrix!!)
            this.rasterizer!!.setWireframeMode((rasterizerFlags and ModelFunctionMaskGL.WIREFRAME_MODE) != 0)
            var offscreen = false
            this.threadResource!!.offsetX = this.rasterizer!!.offsetX
            this.threadResource!!.offsetY = this.rasterizer!!.offsetY
            this.threadResource!!.aFloat2311 = this.rasterizer!!.aFloat1683
            this.threadResource!!.aFloat2308 = 1.0f / this.rasterizer!!.aFloat1683
            this.threadResource!!.centerX = this.rasterizer!!.centerX
            this.threadResource!!.centerY = this.rasterizer!!.centerY
            this.threadResource!!.aFloat2335 = this.rasterizer!!.aFloat1682
            this.threadResource!!.clipWidth = this.rasterizer!!.clipWidth
            for (verticeIndex in 0 until this.numVertices) {
                val verticeX = this.verticesX!![verticeIndex]
                val verticeY = this.verticesY!![verticeIndex]
                val verticeZ = this.verticesZ!![verticeIndex]
                val matrixX = renderMatrix.elements[0] * verticeX + renderMatrix.elements[4] * verticeY + renderMatrix.elements[8] * verticeZ + renderMatrix.elements[12]
                val matrixY = renderMatrix.elements[1] * verticeX + renderMatrix.elements[5] * verticeY + renderMatrix.elements[9] * verticeZ + renderMatrix.elements[13]
                val matrixZ = renderMatrix.elements[2] * verticeX + renderMatrix.elements[6] * verticeY + renderMatrix.elements[10] * verticeZ + renderMatrix.elements[14]
                val zz = renderMatrix.elements[3] * verticeX + renderMatrix.elements[7] * verticeY + renderMatrix.elements[11] * verticeZ + renderMatrix.elements[15]
                this.projectedVerticesZ!![verticeIndex] = this.threadResource!!.aFloat2335 + this.threadResource!!.aFloat2311 * matrixZ / zz
                this.projectedVerticesZ2!![verticeIndex] = zz
                if (matrixZ >= -zz) {
                    this.projectedVerticesX!![verticeIndex] = (this.threadResource!!.centerX + this.threadResource!!.offsetX * matrixX / zz)
                    this.projectedVerticesY!![verticeIndex] = (this.threadResource!!.centerY + this.threadResource!!.offsetY * matrixY / zz)
                } else {
                    this.projectedVerticesX!![verticeIndex] = -5000.0f
                    offscreen = true
                }
                if (this.threadResource!!.hasWaterUpdate) {
                    this.waterRenderHeight!![verticeIndex] = (particleMatrix.elements[13] + particleMatrix.elements[1] * verticeX + particleMatrix.elements[5] * verticeY + particleMatrix.elements[9] * verticeZ).toInt()
                }
            }
            if (this.billboardEffects != null) {
                for (billboardIndex in 0 until this.numBillboards) {
                    val effect = this.billboardEffects!![billboardIndex]!!
                    val surface = this.billboardSurfaces!![billboardIndex]!!
                    val a = this.facesA!![effect.appliesOnFace]
                    val b = this.facesB!![effect.appliesOnFace]
                    val c = this.facesC!![effect.appliesOnFace]
                    val avgX = (this.verticesX!![a.toInt()] + this.verticesX!![b.toInt()] + this.verticesX!![c.toInt()]) / 3
                    val avgY = (this.verticesY!![a.toInt()] + this.verticesY!![b.toInt()] + this.verticesY!![c.toInt()]) / 3
                    val avgZ = (this.verticesZ!![a.toInt()] + this.verticesZ!![b.toInt()] + this.verticesZ!![c.toInt()]) / 3
                    var mX = renderMatrix.elements[0] * avgX + renderMatrix.elements[4] * avgY + renderMatrix.elements[8] * avgZ + renderMatrix.elements[12]
                    var mY = renderMatrix.elements[1] * avgX + renderMatrix.elements[5] * avgY + renderMatrix.elements[9] * avgZ + renderMatrix.elements[13]
                    var mZ = renderMatrix.elements[2] * avgX + renderMatrix.elements[6] * avgY + renderMatrix.elements[10] * avgZ + renderMatrix.elements[14]
                    var mZB2 = renderMatrix.elements[3] * avgX + renderMatrix.elements[7] * avgY + renderMatrix.elements[11] * avgZ + renderMatrix.elements[15]
                    mZ += matrix447.elements[2] * surface.offsetX + matrix447.elements[6] * surface.offsetY
                    mZB2 += matrix447.elements[3] * surface.offsetX + matrix447.elements[7] * surface.offsetY
                    if (mZ > -mZB2) {
                        mX += matrix447.elements[0] * surface.offsetX + matrix447.elements[4] * surface.offsetY
                        mY += matrix447.elements[1] * surface.offsetX + matrix447.elements[5] * surface.offsetY
                        val xx = this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * mX / mZB2
                        val yy = this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * mY / mZB2
                        val adjustedSizeX = surface.scaleX * effect.size2d
                        val adjustedSizeY = surface.scaleY * effect.size3d
                        val adjustX = mX + matrix447.elements[0] * adjustedSizeX + matrix447.elements[4] * adjustedSizeY
                        val adjustY = mY + matrix447.elements[1] * adjustedSizeX + matrix447.elements[5] * adjustedSizeY
                        val adjustZB2 = mZB2 + matrix447.elements[3] * adjustedSizeX + matrix447.elements[7] * adjustedSizeY
                        val otherX = this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * adjustX / adjustZB2
                        val otherY = this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * adjustY / adjustZB2
                        surface.x = xx.toInt()
                        surface.y = yy.toInt()
                        surface.z = this.toolkit.depthRangeNear + this.toolkit.depthRangeFar * (mZ - matrix447.elements[10] * effect.distance) / mZB2
                        surface.size2d = (if (otherX < xx) xx - otherX else otherX - xx).toInt()
                        surface.size3d = (if (otherY < yy) yy - otherY else otherY - yy).toInt()
                    } else {
                        surface.size3d = 0
                        surface.size2d = 0
                    }
                }
            }
            if (renderedEntity != null) {
                var bool57 = false
                var bool58 = true
                val sizeDeltaX = this.minX + this.maxX shr 1
                val sizeDeltaZ = this.minZ + this.maxZ shr 1
                var mY = this.minY
                var zz = renderMatrix.elements[0] * sizeDeltaX + renderMatrix.elements[4] * mY + renderMatrix.elements[8] * sizeDeltaZ + renderMatrix.elements[12]
                var f_64 = renderMatrix.elements[1] * sizeDeltaX + renderMatrix.elements[5] * mY + renderMatrix.elements[9] * sizeDeltaZ + renderMatrix.elements[13]
                var f_33 = renderMatrix.elements[2] * sizeDeltaX + renderMatrix.elements[6] * mY + renderMatrix.elements[10] * sizeDeltaZ + renderMatrix.elements[14]
                var f_34 = renderMatrix.elements[3] * sizeDeltaX + renderMatrix.elements[7] * mY + renderMatrix.elements[11] * sizeDeltaZ + renderMatrix.elements[15]
                if (f_33 >= -f_34) {
                    renderedEntity.left = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * zz / f_34).toInt()
                    renderedEntity.bottom = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * f_64 / f_34).toInt()
                } else {
                    bool57 = true
                }
                mY = this.maxY
                val f_35 = renderMatrix.elements[0] * sizeDeltaX + renderMatrix.elements[4] * mY + renderMatrix.elements[8] * sizeDeltaZ + renderMatrix.elements[12]
                val f_36 = renderMatrix.elements[1] * sizeDeltaX + renderMatrix.elements[5] * mY + renderMatrix.elements[9] * sizeDeltaZ + renderMatrix.elements[13]
                val f_37 = renderMatrix.elements[2] * sizeDeltaX + renderMatrix.elements[6] * mY + renderMatrix.elements[10] * sizeDeltaZ + renderMatrix.elements[14]
                val f_38 = renderMatrix.elements[3] * sizeDeltaX + renderMatrix.elements[7] * mY + renderMatrix.elements[11] * sizeDeltaZ + renderMatrix.elements[15]
                if (f_37 >= -f_38) {
                    renderedEntity.right = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * f_35 / f_38).toInt()
                    renderedEntity.top = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * f_36 / f_38).toInt()
                } else {
                    bool57 = true
                }
                if (bool57) {
                    if (f_33 < -f_34 && f_37 < -f_38) {
                        bool58 = false
                    } else if (f_33 < -f_34) {
                        val f_39 = (f_33 + f_34) / (f_37 + f_38) - 1.0f
                        val f_40 = zz + f_39 * (f_35 - zz)
                        val f_41 = f_64 + f_39 * (f_36 - f_64)
                        val f_42 = f_34 + f_39 * (f_38 - f_34)
                        renderedEntity.left = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * f_40 / f_42).toInt()
                        renderedEntity.bottom = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * f_41 / f_42).toInt()
                    } else if (f_37 < -f_38) {
                        val f_39 = (f_37 + f_38) / (f_33 + f_34) - 1.0f
                        val f_40 = f_35 + f_39 * (zz - f_35)
                        val f_41 = f_36 + f_39 * (f_64 - f_36)
                        val f_42 = f_38 + f_39 * (f_34 - f_38)
                        renderedEntity.right = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * f_40 / f_42).toInt()
                        renderedEntity.top = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * f_41 / f_42).toInt()
                    }
                }
                if (bool58) {
                    if (f_33 / f_34 > f_37 / f_38) {
                        val f_39 = zz + matrix447.elements[0] * this.size2d + matrix447.elements[12]
                        val f_40 = f_34 + matrix447.elements[3] * this.size2d + matrix447.elements[15]
                        renderedEntity.size = (this.toolkit.viewportOriginX - renderedEntity.left + this.toolkit.viewportHalfWidth * f_39 / f_40).toInt()
                    } else {
                        val f_39 = f_35 + matrix447.elements[0] * this.size2d + matrix447.elements[12]
                        val f_40 = f_38 + matrix447.elements[3] * this.size2d + matrix447.elements[15]
                        renderedEntity.size = (this.toolkit.viewportOriginX - renderedEntity.right + this.toolkit.viewportHalfWidth * f_39 / f_40).toInt()
                    }
                    renderedEntity.positionCalculated = true
                }
            }
            appearanceUpdateThreaded(true)
            this.rasterizer!!.smoothEdges = (rasterizerFlags and ModelFunctionMaskGL.SMOOTH_EDGES) == 0
            this.rasterizer!!.blending = false
            try {
                draw(this.toolkit.colourBuffer != null, this.toolkit.depthBuffer != null, (rasterizerFlags and ModelFunctionMaskGL.TRANSPARENT) != 0, offscreen, this.threadResource!!.aBool2304 && f_21 > this.threadResource!!.aFloat2305 || this.threadResource!!.hasWaterUpdate)
            } catch (ex: Exception) {
                // NOOP
            }
            if (this.billboardEffects != null) {
                for (faceIndex in 0 until this.numFaces) {
                    this.billboardOnFace!![faceIndex] = -1
                }
            }
            this.rasterizer = null
            if (this.toolkit.executingThreadCount > 1) {
                synchronized(this) {
                    this.renderLock = false
                    (this as Object).notifyAll()
                }
            }
        }
    }

    fun hasAlpha(faceIndex: Int): Boolean {
        return this.facesAlpha != null && this.facesAlpha!![faceIndex].toInt() != 0
    }

    override fun applyShading(model: Model, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        val safeModel = model as ModelSM
        if ((this.functionMask and 0x10000) != 65536) {
            throw IllegalStateException("")
        } else if ((safeModel.functionMask and 0x10000) != 65536) {
            throw IllegalStateException("")
        }
        copyDependencies(this.toolkit.getThreadResource(Thread.currentThread())!!)
        calculateBounds()
        calculateNormalsThreaded()
        safeModel.calculateBounds()
        safeModel.calculateNormalsThreaded()
        renderCycle++
        var cycleCount = 0
        val vertsX = safeModel.verticesX
        val depth = safeModel.maxDepth
        for (verticeIndex in 0 until this.maxDepth) {
            val normals = this.vertexNormals!![verticeIndex]!!
            if (normals.magnitude != 0) {
                val vertY = this.verticesY!![verticeIndex] - y
                if (vertY >= safeModel.minY && vertY <= safeModel.maxY) {
                    val vertX = this.verticesX!![verticeIndex] - x
                    if (vertX >= safeModel.minX && vertX <= safeModel.maxX) {
                        val vertZ = this.verticesZ!![verticeIndex] - z
                        if (vertZ >= safeModel.minZ && vertZ <= safeModel.maxZ) {
                            for (currDepth in 0 until depth) {
                                val normals2 = safeModel.vertexNormals!![currDepth]!!
                                if (vertX == vertsX!![currDepth] && vertZ == safeModel.verticesZ!![currDepth] && vertY == safeModel.verticesY!![currDepth] && normals2.magnitude != 0) {
                                    if (this.vertexNormalOffsets == null) {
                                        this.vertexNormalOffsets = arrayOfNulls(this.maxDepth)
                                    }
                                    if (safeModel.vertexNormalOffsets == null) {
                                        safeModel.vertexNormalOffsets = arrayOfNulls(depth)
                                    }
                                    var offsets1 = this.vertexNormalOffsets!![verticeIndex]
                                    if (offsets1 == null) {
                                        offsets1 = VertexNormal(normals)
                                        this.vertexNormalOffsets!![verticeIndex] = offsets1
                                    }
                                    var offsets2 = safeModel.vertexNormalOffsets!![currDepth]
                                    if (offsets2 == null) {
                                        offsets2 = VertexNormal(normals2)
                                        safeModel.vertexNormalOffsets!![currDepth] = offsets2
                                    }
                                    offsets1.x += normals2.x
                                    offsets1.y += normals2.y
                                    offsets1.z += normals2.z
                                    offsets1.magnitude += normals2.magnitude
                                    offsets2.x += normals.x
                                    offsets2.y += normals.y
                                    offsets2.z += normals.z
                                    offsets2.magnitude += normals.magnitude
                                    cycleCount++
                                    this.renderCycleByVertex!![verticeIndex] = renderCycle
                                    this.renderCycleByDepth!![currDepth] = renderCycle
                                }
                            }
                        }
                    }
                }
            }
        }
        if (cycleCount >= 3 && renderTypeAdjustment) {
            for (faceIndex in 0 until this.numFaces) {
                if (this.renderCycleByVertex!![this.facesA!![faceIndex].toInt()] == renderCycle && this.renderCycleByVertex!![this.facesB!![faceIndex].toInt()] == renderCycle && this.renderCycleByVertex!![this.facesC!![faceIndex].toInt()] == renderCycle) {
                    if (this.faceRenderType == null) {
                        this.faceRenderType = ByteArray(this.numFaces)
                    }
                    this.faceRenderType!![faceIndex] = 2
                }
            }
            for (faceIndex in 0 until safeModel.numFaces) {
                if (this.renderCycleByDepth!![safeModel.facesA!![faceIndex].toInt()] == renderCycle && this.renderCycleByDepth!![safeModel.facesB!![faceIndex].toInt()] == renderCycle && this.renderCycleByDepth!![safeModel.facesC!![faceIndex].toInt()] == renderCycle) {
                    if (safeModel.faceRenderType == null) {
                        safeModel.faceRenderType = ByteArray(safeModel.numFaces)
                    }
                    safeModel.faceRenderType!![faceIndex] = 2
                }
            }
        }
    }

    override fun startAnimation(): Boolean {
        if (this.vertexGroups == null) {
            return false
        }
        this.originX = 0
        this.originY = 0
        this.originZ = 0
        return true
    }

    fun draw(colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean, offscreen: Boolean, allowReduce: Boolean) {
        if (this.billboardEffects != null) {
            var effect: BillboardEffect
            for (billboardIndex in 0 until this.numBillboards) {
                effect = this.billboardEffects!![billboardIndex]!!
                this.billboardOnFace!![effect.appliesOnFace] = billboardIndex
            }
        }
        if (!this.accountForPriorities && this.billboardEffects == null) {
            for (faceIndex in 0 until this.numFaces) {
                translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce)
            }
        } else if ((this.functionMask and ModelFunctionMask.ALTERS_ALPHA) == 0 && this.aShortArray8623 != null) {
            for (faceIndex in 0 until this.numFaces) {
                val face = this.aShortArray8623!![faceIndex]
                translateToScreen(colorWrite, depthWrite, transparent, face.toInt(), offscreen, allowReduce)
            }
        } else {
            for (faceIndex in 0 until this.numFaces) {
                if (!hasAlpha(faceIndex) && !hasBillboardOnFace(faceIndex)) {
                    translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce)
                }
            }
            if (this.facesLayer == null) {
                for (faceIndex in 0 until this.numFaces) {
                    if (hasAlpha(faceIndex) || hasBillboardOnFace(faceIndex)) {
                        translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce)
                    }
                }
            } else {
                for (priorityType in 0 until 12) {
                    for (faceIndex in 0 until this.numFaces) {
                        if (priorityType == this.facesLayer!![faceIndex].toInt() && (hasAlpha(faceIndex) || hasBillboardOnFace(faceIndex))) {
                            translateToScreen(colorWrite, depthWrite, transparent, faceIndex, offscreen, allowReduce)
                        }
                    }
                }
            }
        }
    }

    fun reduce(colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean, faceIndex: Int) {
        if (!this.threadResource!!.hasWaterUpdate) {
            val a = this.facesA!![faceIndex].toInt()
            val b = this.facesB!![faceIndex].toInt()
            val c = this.facesC!![faceIndex].toInt()
            val matrix448 = this.toolkit.modelMatrix!!
            val f_9 = matrix448.elements[10]
            val f_10 = matrix448.elements[14]
            val f_11 = matrix448.elements[11]
            val f_12 = matrix448.elements[15]
            val f_13 = f_10 * f_11 - f_9 * f_12
            val f_14 = (this.projectedVerticesZ!![a] - this.threadResource!!.aFloat2335) * this.threadResource!!.aFloat2308
            val f_15 = (this.projectedVerticesZ!![b] - this.threadResource!!.aFloat2335) * this.threadResource!!.aFloat2308
            val f_16 = (this.projectedVerticesZ!![c] - this.threadResource!!.aFloat2335) * this.threadResource!!.aFloat2308
            val f_17 = f_14 * f_13 / (f_14 * f_11 - f_9)
            val f_18 = f_15 * f_13 / (f_15 * f_11 - f_9)
            val f_19 = f_16 * f_13 / (f_16 * f_11 - f_9)
            var mixAlphaA = (f_17 - this.threadResource!!.aFloat2305) / this.threadResource!!.aFloat2303
            if (mixAlphaA > 1.0f) {
                mixAlphaA = 1.0f
            } else if (mixAlphaA < 0.0f) {
                mixAlphaA = 0.0f
            }
            var mixAlphaB = (f_18 - this.threadResource!!.aFloat2305) / this.threadResource!!.aFloat2303
            if (mixAlphaB > 1.0f) {
                mixAlphaB = 1.0f
            } else if (mixAlphaB < 0.0f) {
                mixAlphaB = 0.0f
            }
            var mixAlphaC = (f_19 - this.threadResource!!.aFloat2305) / this.threadResource!!.aFloat2303
            if (mixAlphaC > 1.0f) {
                mixAlphaC = 1.0f
            } else if (mixAlphaC < 0.0f) {
                mixAlphaC = 0.0f
            }
            val totalAlpha = mixAlphaA + mixAlphaB + mixAlphaC
            if (totalAlpha < 3.0f) {
                if (totalAlpha <= 0.0f) {
                    renderFace(colorWrite, depthWrite, transparent, faceIndex)
                } else {
                    if (this.facesAlpha == null) {
                        this.rasterizer!!.blendingAlpha = 0
                    } else {
                        this.rasterizer!!.blendingAlpha = this.facesAlpha!![faceIndex].toInt() and 0xff
                    }
                    if (this.facesMaterial != null && this.facesMaterial!![faceIndex].toInt() != -1) {
                        var transparency = 0xffffff.inv()
                        if (this.facesAlpha != null) {
                            transparency = 255 - (this.facesAlpha!![faceIndex].toInt() and 0xff) shl 24
                        }
                        if (this.facesColourC!![faceIndex] == -1) {
                            val faceAColor = transparency or (this.facesColourA!![faceIndex] and 0xffffff)
                            this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], faceAColor, faceAColor, faceAColor, this.threadResource!!.fogColor, mixAlphaA * 255.0f, mixAlphaB * 255.0f, mixAlphaC * 255.0f, this.facesMaterial!![faceIndex].toInt())
                        } else {
                            this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], transparency or (this.facesColourA!![faceIndex] and 0xffffff), transparency or (this.facesColourB!![faceIndex] and 0xffffff), transparency or (this.facesColourC!![faceIndex] and 0xffffff), this.threadResource!!.fogColor, mixAlphaA * 255.0f, mixAlphaB * 255.0f, mixAlphaC * 255.0f, this.facesMaterial!![faceIndex].toInt())
                        }
                    } else if (this.facesColourC!![faceIndex] == -1) {
                        this.rasterizer!!.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, mixAlphaA * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, mixAlphaB * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, mixAlphaC * 255.0f))
                    } else {
                        this.rasterizer!!.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, mixAlphaA * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourB!![faceIndex] and 0xffff], this.threadResource!!.fogColor, mixAlphaB * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourC!![faceIndex] and 0xffff], this.threadResource!!.fogColor, mixAlphaC * 255.0f))
                    }
                }
            }
        } else {
            val a = this.facesA!![faceIndex].toInt()
            val b = this.facesB!![faceIndex].toInt()
            val c = this.facesC!![faceIndex].toInt()
            var waterAlphaMultiplierA = 0.0f
            var waterAlphaMultiplierB = 0.0f
            var waterAlphaMultiplierC = 0.0f
            if (this.waterRenderHeight!![a] > this.threadResource!!.waterScale) {
                waterAlphaMultiplierA = 1.0f
            } else if (this.waterRenderHeight!![a] > this.threadResource!!.waterAverageHeight) {
                waterAlphaMultiplierA = (this.threadResource!!.waterAverageHeight - this.waterRenderHeight!![a]) * 1.0f / (this.threadResource!!.waterAverageHeight - this.threadResource!!.waterScale)
            }
            if (this.waterRenderHeight!![b] > this.threadResource!!.waterScale) {
                waterAlphaMultiplierB = 1.0f
            } else if (this.waterRenderHeight!![b] > this.threadResource!!.waterAverageHeight) {
                waterAlphaMultiplierB = (this.threadResource!!.waterAverageHeight - this.waterRenderHeight!![b]) * 1.0f / (this.threadResource!!.waterAverageHeight - this.threadResource!!.waterScale)
            }
            if (this.waterRenderHeight!![c] > this.threadResource!!.waterScale) {
                waterAlphaMultiplierC = 1.0f
            } else if (this.waterRenderHeight!![c] > this.threadResource!!.waterAverageHeight) {
                waterAlphaMultiplierC = (this.threadResource!!.waterAverageHeight - this.waterRenderHeight!![c]) * 1.0f / (this.threadResource!!.waterAverageHeight - this.threadResource!!.waterScale)
            }
            if (this.facesAlpha == null) {
                this.rasterizer!!.blendingAlpha = 0
            } else {
                this.rasterizer!!.blendingAlpha = this.facesAlpha!![faceIndex].toInt() and 0xff
            }
            if (this.facesMaterial != null && this.facesMaterial!![faceIndex].toInt() != -1) {
                var transparency = 0xffffff.inv()
                if (this.facesAlpha != null) {
                    transparency = 255 - (this.facesAlpha!![faceIndex].toInt() and 0xff) shl 24
                }
                if (this.facesColourC!![faceIndex] == -1) {
                    val finalFaceAColor = transparency or (this.facesColourA!![faceIndex] and 0xffffff)
                    this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], finalFaceAColor, finalFaceAColor, finalFaceAColor, this.threadResource!!.fogColor, waterAlphaMultiplierA * 255.0f, waterAlphaMultiplierB * 255.0f, waterAlphaMultiplierC * 255.0f, this.facesMaterial!![faceIndex].toInt())
                } else {
                    this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], transparency or (this.facesColourA!![faceIndex] and 0xffffff), transparency or (this.facesColourB!![faceIndex] and 0xffffff), transparency or (this.facesColourC!![faceIndex] and 0xffffff), this.threadResource!!.fogColor, waterAlphaMultiplierA * 255.0f, waterAlphaMultiplierB * 255.0f, waterAlphaMultiplierC * 255.0f, this.facesMaterial!![faceIndex].toInt())
                }
            } else if (this.facesColourC!![faceIndex] == -1) {
                this.rasterizer!!.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, waterAlphaMultiplierA * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, waterAlphaMultiplierB * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, waterAlphaMultiplierC * 255.0f))
            } else {
                this.rasterizer!!.drawTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff], this.threadResource!!.fogColor, waterAlphaMultiplierA * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourB!![faceIndex] and 0xffff], this.threadResource!!.fogColor, waterAlphaMultiplierB * 255.0f), ColorUtil.mixColorsWithTransparency(ColorUtil.hsvToRgbTable!![this.facesColourC!![faceIndex] and 0xffff], this.threadResource!!.fogColor, waterAlphaMultiplierC * 255.0f))
            }
        }
    }

    override fun calculateProjection(camX: Int, camY: Int, modelMatrix: Matrix43, notRendered: Boolean, sizeShift: Int): Boolean {
        copyDependencies(this.toolkit.getThreadResource(Thread.currentThread())!!)
        val particleMatrix = this.threadResource!!.particleMatrix
        particleMatrix.fromMatrix(modelMatrix)
        val renderMatrix = this.threadResource!!.renderingMatrix
        renderMatrix.copyFrom(particleMatrix)
        renderMatrix.multiplyBy(this.toolkit.floorMatrix!!)
        var onScreen = false
        var left = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var top = Int.MAX_VALUE
        var bottom = Int.MIN_VALUE
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        val modelCenterX = this.maxX - this.minX shr 1
        val modelCenterY = this.maxY - this.minY shr 1
        val modelCenterZ = this.maxZ - this.minZ shr 1
        val minXOffset = modelCenterX + this.minX
        val minYOffset = modelCenterY + this.minY
        val minZOffset = modelCenterZ + this.minZ
        val xA = minXOffset - (modelCenterX shl sizeShift)
        val yA = minYOffset - (modelCenterY shl sizeShift)
        val zA = minZOffset - (modelCenterZ shl sizeShift)
        val xB = minXOffset + (modelCenterX shl sizeShift)
        val yB = minYOffset + (modelCenterY shl sizeShift)
        val zC = minZOffset + (modelCenterZ shl sizeShift)
        this.matrixX!![0] = xA
        this.matrixY!![0] = yA
        this.matrixZ!![0] = zA
        this.matrixX!![1] = xB
        this.matrixY!![1] = yA
        this.matrixZ!![1] = zA
        this.matrixX!![2] = xA
        this.matrixY!![2] = yB
        this.matrixZ!![2] = zA
        this.matrixX!![3] = xB
        this.matrixY!![3] = yB
        this.matrixZ!![3] = zA
        this.matrixX!![4] = xA
        this.matrixY!![4] = yA
        this.matrixZ!![4] = zC
        this.matrixX!![5] = xB
        this.matrixY!![5] = yA
        this.matrixZ!![5] = zC
        this.matrixX!![6] = xA
        this.matrixY!![6] = yB
        this.matrixZ!![6] = zC
        this.matrixX!![7] = xB
        this.matrixY!![7] = yB
        this.matrixZ!![7] = zC
        for (matrixIndex in 0..7) {
            val x = this.matrixX!![matrixIndex]
            val y = this.matrixY!![matrixIndex]
            val z = this.matrixZ!![matrixIndex]
            val depthA = renderMatrix.elements[2] * x + renderMatrix.elements[6] * y + renderMatrix.elements[10] * z + renderMatrix.elements[14]
            val depthB = renderMatrix.elements[3] * x + renderMatrix.elements[7] * y + renderMatrix.elements[11] * z + renderMatrix.elements[15]
            if (depthA >= -depthB) {
                val f_31 = renderMatrix.elements[0] * x + renderMatrix.elements[4] * y + renderMatrix.elements[8] * z + renderMatrix.elements[12]
                val f_32 = renderMatrix.elements[1] * x + renderMatrix.elements[5] * y + renderMatrix.elements[9] * z + renderMatrix.elements[13]
                val screenPosX = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * f_31 / depthB).toInt()
                val screenPosY = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * f_32 / depthB).toInt()
                if (screenPosX < left) {
                    left = screenPosX
                }
                if (screenPosX > right) {
                    right = screenPosX
                }
                if (screenPosY < top) {
                    top = screenPosY
                }
                if (screenPosY > bottom) {
                    bottom = screenPosY
                }
                onScreen = true
            }
        }
        if (onScreen && camX > left && camX < right && camY > top && camY < bottom) {
            if (notRendered) {
                return true
            }
            for (verticeIndex in 0 until this.numVertices) {
                val x = this.verticesX!![verticeIndex]
                val y = this.verticesY!![verticeIndex]
                val z = this.verticesZ!![verticeIndex]
                val depthA = renderMatrix.elements[2] * x + renderMatrix.elements[6] * y + renderMatrix.elements[10] * z + renderMatrix.elements[14]
                val depthB = renderMatrix.elements[3] * x + renderMatrix.elements[7] * y + renderMatrix.elements[11] * z + renderMatrix.elements[15]
                if (depthA >= -depthB) {
                    val multiplierX = renderMatrix.elements[0] * x + renderMatrix.elements[4] * y + renderMatrix.elements[8] * z + renderMatrix.elements[12]
                    val multiplierY = renderMatrix.elements[1] * x + renderMatrix.elements[5] * y + renderMatrix.elements[9] * z + renderMatrix.elements[13]
                    this.projectedVerticesX!![verticeIndex] = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * multiplierX / depthB)
                    this.projectedVerticesY!![verticeIndex] = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * multiplierY / depthB)
                } else {
                    this.projectedVerticesX!![verticeIndex] = -999999.0f
                }
            }
            for (face in 0 until this.numFaces) {
                if (this.projectedVerticesX!![this.facesA!![face].toInt()] != -999999.0f && this.projectedVerticesX!![this.facesB!![face].toInt()] != -999999.0f && this.projectedVerticesX!![this.facesC!![face].toInt()] != -999999.0f && isHovering(camX, camY, this.projectedVerticesY!![this.facesA!![face].toInt()], this.projectedVerticesY!![this.facesB!![face].toInt()], this.projectedVerticesY!![this.facesC!![face].toInt()], this.projectedVerticesX!![this.facesA!![face].toInt()], this.projectedVerticesX!![this.facesB!![face].toInt()], this.projectedVerticesX!![this.facesC!![face].toInt()])) {
                    return true
                }
            }
        }
        return false
    }

    fun isHovering(i_1: Int, i_2: Int, f_3: Float, f_4: Float, f_5: Float, f_6: Float, f_7: Float, f_8: Float): Boolean {
        return (i_2 >= f_3 || i_2 >= f_4 || i_2 >= f_5) && (i_2 <= f_3 || i_2 <= f_4 || i_2 <= f_5) && (i_1 >= f_6 || i_1 >= f_7 || i_1 >= f_8) && (i_1 <= f_6 || i_1 <= f_7 || i_1 <= f_8)
    }

    override fun awaitLock() {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized(this) {
                while (super.isLocked) {
                    try {
                        (this as Object).wait()
                    } catch (ie: InterruptedException) {
                        // NOOP
                    }
                }
                super.isLocked = true
            }
        }
    }

    override fun getSize2d(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.size2d.toInt()
    }

    override fun getSize3d(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.size3d.toInt()
    }

    override fun getMinX(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.minX.toInt()
    }

    override fun getMaxY(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.maxY.toInt()
    }

    override fun getMinZ(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.minZ.toInt()
    }

    override fun setAmbient(ambient: Int) {
        if ((this.functionMask and ModelFunctionMask.ALTERS_AMBIENT) != 4096) {
            throw IllegalStateException()
        } else {
            this.ambient = ambient
            this.appearanceUpdateStage = 0
        }
    }

    override fun getContrast(): Int {
        return this.contrast
    }

    override fun recolor(src: Short, dst: Short) {
        for (faceIndex in 0 until this.numFaces) {
            if (this.facesColor!![faceIndex] == src) {
                this.facesColor!![faceIndex] = dst
            }
        }
        if (this.billboardEffects != null) {
            for (index in 0 until this.numBillboards) {
                val effect = this.billboardEffects!![index]!!
                val surface = this.billboardSurfaces!![index]!!
                surface.diffuseColor = surface.diffuseColor and 0xffffff.inv() or (ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(this.facesColor!![effect.appliesOnFace].toInt()).toInt() and 0xffff] and 0xffffff)
            }
        }
        if (this.appearanceUpdateStage == 2) {
            this.appearanceUpdateStage = 1
        }
    }

    override fun applyEnvironmentAlpha(alpha: Byte, transparency: ByteArray?) {
        if ((this.functionMask and ModelFunctionMask.ALTERS_LIGHT_ALPHA) == 0) {
            throw RuntimeException()
        }
        if (this.facesAlpha == null) {
            this.facesAlpha = ByteArray(this.numFaces)
        }
        if (transparency == null) {
            for (faceIndex in 0 until this.numFaces) {
                this.facesAlpha!![faceIndex] = alpha
            }
        } else {
            for (faceIndex in 0 until this.numFaces) {
                val replacementAlpha = 255 - (255 - (alpha.toInt() and 0xff)) * (255 - (transparency[faceIndex].toInt() and 0xff)) / 255
                this.facesAlpha!![faceIndex] = replacementAlpha.toByte()
            }
        }
        if (this.appearanceUpdateStage == 2) {
            this.appearanceUpdateStage = 1
        }
    }

    override fun tint(hue: Int, saturation: Int, lightness: Int, opacity: Int) {
        if ((this.functionMask and ModelFunctionMask.TINT) != 524288) {
            throw IllegalStateException()
        }
        for (faceIndex in 0 until this.numFaces) {
            val hsl = this.facesColor!![faceIndex].toInt() and 0xffff
            var h = hsl shr 10 and 0x3f
            var s = hsl shr 7 and 0x7
            var l = hsl and 0x7f
            if (hue != -1) {
                h += opacity * (hue - h) shr 7
            }
            if (saturation != -1) {
                s += opacity * (saturation - s) shr 7
            }
            if (lightness != -1) {
                l += opacity * (lightness - l) shr 7
            }
            this.facesColor!![faceIndex] = (h shl 10 or (s shl 7) or l).toShort()
        }
        if (this.billboardEffects != null) {
            for (faceIndex in 0 until this.numBillboards) {
                val effect = this.billboardEffects!![faceIndex]!!
                val surface = this.billboardSurfaces!![faceIndex]!!
                surface.diffuseColor = surface.diffuseColor and 0xffffff.inv() or (ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(this.facesColor!![effect.appliesOnFace].toInt() and 0xffff).toInt() and 0xffff] and 0xffffff)
            }
        }
        if (this.appearanceUpdateStage == 2) {
            this.appearanceUpdateStage = 1
        }
    }

    override fun getEmissiveTriangles(): Array<EmissiveTriangle>? {
        return this.particleEmitters
    }

    override fun getEffectiveVertices(): Array<EffectiveVertex>? {
        return this.effectors
    }

    override fun accountForLayers(): Boolean {
        return this.accountForPriorities
    }

    override fun hasAnimatedTexture(): Boolean {
        return this.textureAnimated
    }

    override fun getAlpha(): ByteArray? {
        return this.facesAlpha
    }

    override fun applyShading(replacingFunctionMask: Int) {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized(this) {
                if ((this.functionMask and ModelFunctionMask.SHADED) == 65536 && (replacingFunctionMask and ModelFunctionMask.SHADED) == 0) {
                    appearanceUpdateThreaded(true)
                }
                this.functionMask = replacingFunctionMask
            }
        } else {
            if ((this.functionMask and ModelFunctionMask.SHADED) == 65536 && (replacingFunctionMask and ModelFunctionMask.SHADED) == 0) {
                appearanceUpdateThreaded(true)
            }
            this.functionMask = replacingFunctionMask
        }
    }

    fun renderFace(colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean, faceIndex: Int) {
        val a = this.facesA!![faceIndex]
        val b = this.facesB!![faceIndex]
        val c = this.facesC!![faceIndex]
        if (this.facesMaterial != null && this.facesMaterial!![faceIndex].toInt() != -1) {
            var alpha = 0xffffff.inv()
            if (this.facesAlpha != null) {
                alpha = 255 - (this.facesAlpha!![faceIndex].toInt() and 0xff) shl 24
            }
            if (this.facesColourC!![faceIndex] == -1) {
                val faceAColor = alpha or (this.facesColourA!![faceIndex] and 0xffffff)
                this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a.toInt()], this.projectedVerticesY!![b.toInt()], this.projectedVerticesY!![c.toInt()], this.projectedVerticesX!![a.toInt()], this.projectedVerticesX!![b.toInt()], this.projectedVerticesX!![c.toInt()], this.projectedVerticesZ!![a.toInt()], this.projectedVerticesZ!![b.toInt()], this.projectedVerticesZ!![c.toInt()], this.projectedVerticesZ2!![a.toInt()], this.projectedVerticesZ2!![b.toInt()], this.projectedVerticesZ2!![c.toInt()], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], faceAColor, faceAColor, faceAColor, this.threadResource!!.fogColor, 0.0f, 0.0f, 0.0f, this.facesMaterial!![faceIndex].toInt())
            } else {
                this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a.toInt()], this.projectedVerticesY!![b.toInt()], this.projectedVerticesY!![c.toInt()], this.projectedVerticesX!![a.toInt()], this.projectedVerticesX!![b.toInt()], this.projectedVerticesX!![c.toInt()], this.projectedVerticesZ!![a.toInt()], this.projectedVerticesZ!![b.toInt()], this.projectedVerticesZ!![c.toInt()], this.projectedVerticesZ2!![a.toInt()], this.projectedVerticesZ2!![b.toInt()], this.projectedVerticesZ2!![c.toInt()], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], alpha or (this.facesColourA!![faceIndex] and 0xffffff), alpha or (this.facesColourB!![faceIndex] and 0xffffff), alpha or (this.facesColourC!![faceIndex] and 0xffffff), this.threadResource!!.fogColor, 0.0f, 0.0f, 0.0f, this.facesMaterial!![faceIndex].toInt())
            }
        } else {
            if (this.facesAlpha == null) {
                this.rasterizer!!.blendingAlpha = 0
            } else {
                this.rasterizer!!.blendingAlpha = this.facesAlpha!![faceIndex].toInt() and 0xff
            }
            if (this.facesColourC!![faceIndex] == -1) {
                this.rasterizer!!.drawFlatTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a.toInt()], this.projectedVerticesY!![b.toInt()], this.projectedVerticesY!![c.toInt()], this.projectedVerticesX!![a.toInt()], this.projectedVerticesX!![b.toInt()], this.projectedVerticesX!![c.toInt()], this.projectedVerticesZ!![a.toInt()], this.projectedVerticesZ!![b.toInt()], this.projectedVerticesZ!![c.toInt()], ColorUtil.hsvToRgbTable!![this.facesColourA!![faceIndex] and 0xffff])
            } else {
                this.rasterizer!!.drawShadedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a.toInt()], this.projectedVerticesY!![b.toInt()], this.projectedVerticesY!![c.toInt()], this.projectedVerticesX!![a.toInt()], this.projectedVerticesX!![b.toInt()], this.projectedVerticesX!![c.toInt()], this.projectedVerticesZ!![a.toInt()], this.projectedVerticesZ!![b.toInt()], this.projectedVerticesZ!![c.toInt()], (this.facesColourA!![faceIndex] and 0xffff).toFloat(), (this.facesColourB!![faceIndex] and 0xffff).toFloat(), (this.facesColourC!![faceIndex] and 0xffff).toFloat())
            }
        }
    }

    override fun getMinY(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.minY.toInt()
    }

    fun rotate90DegWithNormals() {
        synchronized(this) {
            for (currentDepth in 0 until this.maxDepth) {
                var tmpX = this.verticesX!![currentDepth]
                this.verticesX!![currentDepth] = this.verticesZ!![currentDepth]
                this.verticesZ!![currentDepth] = -tmpX
                if (this.vertexNormals!![currentDepth] != null) {
                    tmpX = this.vertexNormals!![currentDepth]!!.x
                    this.vertexNormals!![currentDepth]!!.x = this.vertexNormals!![currentDepth]!!.z
                    this.vertexNormals!![currentDepth]!!.z = -tmpX
                }
            }
            if (this.facesNormal != null) {
                for (faceIndex in 0 until this.numFaces) {
                    if (this.facesNormal!![faceIndex] != null) {
                        val tmpX = this.facesNormal!![faceIndex]!!.x
                        this.facesNormal!![faceIndex]!!.x = this.facesNormal!![faceIndex]!!.z
                        this.facesNormal!![faceIndex]!!.z = -tmpX
                    }
                }
            }
            for (currentDepth in this.maxDepth until this.numVertices) {
                val tmpX = this.verticesX!![currentDepth]
                this.verticesX!![currentDepth] = this.verticesZ!![currentDepth]
                this.verticesZ!![currentDepth] = -tmpX
            }
            this.appearanceUpdateStage = 0
            this.boundsUpdated = false
        }
    }

    override fun createShadowSprite(shadow: Shadow?): Shadow? {
        return null
    }

    fun appearanceUpdateThreaded(resetRenderType: Boolean) {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized(this) {
                appearanceUpdate(resetRenderType)
            }
        } else {
            appearanceUpdate(resetRenderType)
        }
    }

    override fun translate(x: Int, y: Int, z: Int) {
        if (x != 0 && (this.functionMask and ModelFunctionMask.DISPLACE_X) != 1) {
            throw IllegalStateException()
        } else if (y != 0 && (this.functionMask and ModelFunctionMask.DISPLACE_Y) != 2) {
            throw IllegalStateException()
        } else if (z != 0 && (this.functionMask and ModelFunctionMask.DISPLACE_Z) != 4) {
            throw IllegalStateException()
        }
        synchronized(this) {
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesX!![verticeIndex] += x
                this.verticesY!![verticeIndex] += y
                this.verticesZ!![verticeIndex] += z
            }
        }
    }

    fun mixLightness(i_1: Int, i_21: Int): Int {
        var i_211 = (i_1 and 0x7f) * i_21 shr 7
        if (i_211 < 2) {
            i_211 = 2
        } else if (i_211 > 126) {
            i_211 = 126
        }
        return i_211 + (i_1 and 0xff80)
    }

    override fun offsetSubmesh(matrix44var1: Matrix43, submeshFlags: Int, updateIdentityMatrix: Boolean) {
        if (this.verticesSubmeshes != null) {
            val resource = this.toolkit.getThreadResource(Thread.currentThread())!!
            val matrix44var5 = resource.aClass2942316
            matrix44var5.copyFrom(matrix44var1)
            if (updateIdentityMatrix) {
                matrix44var5.updateIdentitiyMatrix()
            }
            val submeshMatrix = resource.particleMatrix
            submeshMatrix.fromMatrix(matrix44var5)
            val matrix = FloatArray(3)
            for (currentDepth in 0 until this.maxDepth) {
                if ((submeshFlags and this.verticesSubmeshes!![currentDepth].toInt()) != 0) {
                    submeshMatrix.scale(this.verticesX!![currentDepth].toFloat(), this.verticesY!![currentDepth].toFloat(), this.verticesZ!![currentDepth].toFloat(), matrix)
                    this.verticesX!![currentDepth] = matrix[0].toInt()
                    this.verticesY!![currentDepth] = matrix[1].toInt()
                    this.verticesZ!![currentDepth] = matrix[2].toInt()
                }
            }
        }
    }

    override fun getMaxX(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.maxX.toInt()
    }

    override fun contourToGround(contourType: Int, contourModifier: Int, currentLevel: Ground?, nextLevel: Ground?, sceneX: Int, averageHeight: Int, sceneY: Int) {
        if (contourType == 3) {
            if ((this.functionMask and ModelFunctionMask.FULL_DISPLACEMENT) != 7) {
                throw IllegalStateException()
            }
        } else if ((this.functionMask and ModelFunctionMask.DISPLACE_Y) != 2) {
            throw IllegalStateException()
        }
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        val lowX = sceneX + this.minX
        val highX = sceneX + this.maxX
        val lowY = sceneY + this.minZ
        val highY = sceneY + this.maxZ
        val currLevel = currentLevel!!
        if (contourType == 4 || lowX >= 0 && highX + currLevel.tileUnits shr currLevel.tileScale < currLevel.width && lowY >= 0 && highY + currLevel.tileUnits shr currLevel.tileScale < currLevel.length) {
            val currentHeights = currLevel.tileHeights
            var nextHeights: Array<IntArray>? = null
            if (nextLevel != null) {
                nextHeights = nextLevel.tileHeights
            }
            if (contourType != 4 && contourType != 5) {
                val lowXScaled = lowX shr currLevel.tileScale
                val highXScaled = highX + (currLevel.tileUnits - 1) shr currLevel.tileScale
                val lowYScaled = lowY shr currLevel.tileScale
                val highYScaled = highY + (currLevel.tileUnits - 1) shr currLevel.tileScale
                if (currentHeights[lowXScaled][lowYScaled] == averageHeight && currentHeights[highXScaled][lowYScaled] == averageHeight && currentHeights[lowXScaled][highYScaled] == averageHeight && currentHeights[highXScaled][highYScaled] == averageHeight) {
                    return
                }
            } else if (nextLevel == null || lowX < 0 || nextLevel.tileUnits + highX shr nextLevel.tileScale >= nextLevel.width || lowY < 0 || nextLevel.tileUnits + highY shr nextLevel.tileScale >= nextLevel.length) {
                return
            }
            synchronized(this) {
                when (contourType) {
                    1 -> {
                        val tileMask = currLevel.tileUnits - 1
                        for (v in 0 until this.maxDepth) {
                            val x = sceneX + this.verticesX!![v]
                            val z = sceneY + this.verticesZ!![v]
                            val remainderX = x and tileMask
                            val remainderZ = z and tileMask
                            val tileX = x shr currLevel.tileScale
                            val tileZ = z shr currLevel.tileScale
                            val height1 = currentHeights[tileX][tileZ] * (currLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] shr currLevel.tileScale
                            val height2 = (currLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] shr currLevel.tileScale
                            val interpolatedHeight = height1 * (currLevel.tileUnits - remainderZ) + height2 * remainderZ shr currLevel.tileScale
                            this.verticesY!![v] = interpolatedHeight + this.verticesY!![v] - averageHeight
                        }
                        for (v in this.maxDepth until this.numVertices) {
                            val x = sceneX + this.verticesX!![v]
                            val z = sceneY + this.verticesZ!![v]
                            val remainderX = x and tileMask
                            val remainderZ = z and tileMask
                            val tileX = x shr currLevel.tileScale
                            val tileZ = z shr currLevel.tileScale
                            if (tileX >= 0 && tileX < currentHeights.size - 1 && tileZ >= 0 && tileZ < currentHeights[0].size - 1) {
                                val i_23 = currentHeights[tileX][tileZ] * (currLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] shr currLevel.tileScale
                                val i_24 = (currLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] shr currLevel.tileScale
                                val i_25 = i_23 * (currLevel.tileUnits - remainderZ) + i_24 * remainderZ shr currLevel.tileScale
                                this.verticesY!![v] = i_25 + this.verticesY!![v] - averageHeight
                            }
                        }
                    }
                    2 -> {
                        if (this.minY.toInt() == 0) {
                            return
                        }
                        val tileMask = currLevel.tileUnits - 1
                        for (v in 0 until this.maxDepth) {
                            val i_17 = (this.verticesY!![v] shl 16) / this.minY
                            if (i_17 < contourModifier) {
                                val i_18 = sceneX + this.verticesX!![v]
                                val i_19 = sceneY + this.verticesZ!![v]
                                val i_20 = i_18 and tileMask
                                val i_21 = i_19 and tileMask
                                val i_22 = i_18 shr currLevel.tileScale
                                val i_23 = i_19 shr currLevel.tileScale
                                val i_24 = currentHeights[i_22][i_23] * (currLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] shr currLevel.tileScale
                                val i_25 = (currLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] shr currLevel.tileScale
                                val i_26 = i_24 * (currLevel.tileUnits - i_21) + i_25 * i_21 shr currLevel.tileScale
                                this.verticesY!![v] += (i_26 - averageHeight) * (contourModifier - i_17) / contourModifier
                            }
                        }
                        for (v in this.maxDepth until this.numVertices) {
                            val i_17 = (this.verticesY!![v] shl 16) / this.minY
                            if (i_17 < contourModifier) {
                                val i_18 = sceneX + this.verticesX!![v]
                                val i_19 = sceneY + this.verticesZ!![v]
                                val i_20 = i_18 and tileMask
                                val i_21 = i_19 and tileMask
                                val i_22 = i_18 shr currLevel.tileScale
                                val i_23 = i_19 shr currLevel.tileScale
                                if (i_22 >= 0 && i_22 < currLevel.width - 1 && i_23 >= 0 && i_23 < currLevel.length - 1) {
                                    val i_24 = currentHeights[i_22][i_23] * (currLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] shr currLevel.tileScale
                                    val i_25 = (currLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] shr currLevel.tileScale
                                    val i_26 = i_24 * (currLevel.tileUnits - i_21) + i_25 * i_21 shr currLevel.tileScale
                                    this.verticesY!![v] += (i_26 - averageHeight) * (contourModifier - i_17) / contourModifier
                                }
                            }
                        }
                    }
                    3 -> {
                        val iiii = (contourModifier and 0xff) * 4
                        val iiii2 = (contourModifier shr 8 and 0xff) * 4
                        val i_17 = (contourModifier shr 16 and 0xff) shl 6
                        val i_18 = (contourModifier shr 24 and 0xff) shl 6
                        if (sceneX - (iiii shr 1) < 0 || sceneX + currLevel.tileUnits + (iiii shr 1) >= currLevel.width shl currLevel.tileScale || sceneY - (iiii2 shr 1) < 0 || currLevel.tileUnits + (iiii2 shr 1) + sceneY >= currLevel.length shl currLevel.tileScale) {
                            return
                        }
                        hillChange(currLevel, sceneX, averageHeight, sceneY, iiii, iiii2, i_17, i_18)
                    }
                    4 -> {
                        val tileMask = nextLevel!!.tileUnits - 1
                        val sizeY = this.maxY - this.minY
                        for (i_17 in 0 until this.maxDepth) {
                            val i_18 = sceneX + this.verticesX!![i_17]
                            val i_19 = sceneY + this.verticesZ!![i_17]
                            val i_20 = i_18 and tileMask
                            val i_21 = i_19 and tileMask
                            val i_22 = i_18 shr nextLevel.tileScale
                            val i_23 = i_19 shr nextLevel.tileScale
                            val i_24 = i_20 * nextHeights!![i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) shr nextLevel.tileScale
                            val i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] shr nextLevel.tileScale
                            val i_26 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 shr nextLevel.tileScale
                            this.verticesY!![i_17] += sizeY + (i_26 - averageHeight)
                        }
                        for (i_17 in this.maxDepth until this.numVertices) {
                            val i_18 = sceneX + this.verticesX!![i_17]
                            val i_19 = sceneY + this.verticesZ!![i_17]
                            val i_20 = i_18 and tileMask
                            val i_21 = i_19 and tileMask
                            val i_22 = i_18 shr nextLevel.tileScale
                            val i_23 = i_19 shr nextLevel.tileScale
                            if (i_22 >= 0 && i_22 < nextLevel.width - 1 && i_23 >= 0 && i_23 < nextLevel.length - 1) {
                                val i_24 = i_20 * nextHeights!![i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) shr nextLevel.tileScale
                                val i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] shr nextLevel.tileScale
                                val i_26 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 shr nextLevel.tileScale
                                this.verticesY!![i_17] += sizeY + (i_26 - averageHeight)
                            }
                        }
                    }
                    5 -> {
                        val tileMask = nextLevel!!.tileUnits - 1
                        val sizeY = this.maxY - this.minY
                        for (i_17 in 0 until this.maxDepth) {
                            val i_18 = sceneX + this.verticesX!![i_17]
                            val i_19 = sceneY + this.verticesZ!![i_17]
                            val i_20 = i_18 and tileMask
                            val i_21 = i_19 and tileMask
                            val i_22 = i_18 shr currLevel.tileScale
                            val i_23 = i_19 shr currLevel.tileScale
                            var i_24 = currentHeights[i_22][i_23] * (currLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] shr currLevel.tileScale
                            var i_25 = (currLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] shr currLevel.tileScale
                            val i_26 = i_24 * (currLevel.tileUnits - i_21) + i_25 * i_21 shr currLevel.tileScale
                            i_24 = i_20 * nextHeights!![i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) shr nextLevel.tileScale
                            i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] shr nextLevel.tileScale
                            val i_27 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 shr nextLevel.tileScale
                            val i_28 = i_26 - i_27 - contourModifier
                            this.verticesY!![i_17] = (i_28 * ((this.verticesY!![i_17] shl 8) / sizeY) shr 8) - (averageHeight - i_26)
                        }
                        for (i_17 in this.maxDepth until this.numVertices) {
                            val i_18 = sceneX + this.verticesX!![i_17]
                            val i_19 = sceneY + this.verticesZ!![i_17]
                            val i_20 = i_18 and tileMask
                            val i_21 = i_19 and tileMask
                            val i_22 = i_18 shr currLevel.tileScale
                            val i_23 = i_19 shr currLevel.tileScale
                            if (i_22 >= 0 && i_22 < currLevel.width - 1 && i_22 < nextLevel.width - 1 && i_23 >= 0 && i_23 < currLevel.length - 1 && i_23 < nextLevel.length - 1) {
                                var i_24 = currentHeights[i_22][i_23] * (currLevel.tileUnits - i_20) + i_20 * currentHeights[i_22 + 1][i_23] shr currLevel.tileScale
                                var i_25 = (currLevel.tileUnits - i_20) * currentHeights[i_22][i_23 + 1] + i_20 * currentHeights[i_22 + 1][i_23 + 1] shr currLevel.tileScale
                                val i_26 = i_24 * (currLevel.tileUnits - i_21) + i_25 * i_21 shr currLevel.tileScale
                                i_24 = i_20 * nextHeights!![i_22 + 1][i_23] + nextHeights[i_22][i_23] * (nextLevel.tileUnits - i_20) shr nextLevel.tileScale
                                i_25 = (nextLevel.tileUnits - i_20) * nextHeights[i_22][i_23 + 1] + i_20 * nextHeights[i_22 + 1][i_23 + 1] shr nextLevel.tileScale
                                val i_27 = (nextLevel.tileUnits - i_21) * i_24 + i_25 * i_21 shr nextLevel.tileScale
                                val i_28 = i_26 - i_27 - contourModifier
                                this.verticesY!![i_17] = (i_28 * ((this.verticesY!![i_17] shl 8) / sizeY) shr 8) - (averageHeight - i_26)
                            }
                        }
                    }
                }
                this.boundsUpdated = false
            }
        }
    }

    fun translateToScreen(colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean, faceIndex: Int, offscreen: Boolean, reduce: Boolean) {
        if (this.facesColourC!![faceIndex] != -2) {
            val a = this.facesA!![faceIndex]
            val b = this.facesB!![faceIndex]
            val c = this.facesC!![faceIndex]
            val xA = this.projectedVerticesX!![a.toInt()]
            val xB = this.projectedVerticesX!![b.toInt()]
            val xC = this.projectedVerticesX!![c.toInt()]
            if (!offscreen || xA != -5000.0f && xB != -5000.0f && xC != -5000.0f) {
                val yA = this.projectedVerticesY!![a.toInt()]
                val yB = this.projectedVerticesY!![b.toInt()]
                val yC = this.projectedVerticesY!![c.toInt()]
                if (this.billboardOnFace!![faceIndex] != -1 || (xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0f) {
                    this.rasterizer!!.clipEdges = xA < 0.0f || xB < 0.0f || xC < 0.0f || xA > this.threadResource!!.clipWidth || xB > this.threadResource!!.clipWidth || xC > this.threadResource!!.clipWidth
                    if (reduce) {
                        val billboard = this.billboardOnFace!![faceIndex]
                        if (billboard == -1 || !this.billboardEffects!![billboard]!!.hasUid) {
                            reduce(colorWrite, depthWrite, transparent, faceIndex)
                        }
                    } else {
                        val billboard = this.billboardOnFace!![faceIndex]
                        if (billboard != -1) {
                            val effect = this.billboardEffects!![billboard]!!
                            val surface = this.billboardSurfaces!![billboard]!!
                            if (!effect.hasUid) {
                                renderFace(colorWrite, depthWrite, transparent, faceIndex)
                            }
                            this.toolkit.drawBillboard(colorWrite, depthWrite, transparent, surface.x, surface.y, surface.z, surface.size2d, surface.size3d, effect.materialId.toInt() and 0xffff, surface.diffuseColor, effect.blendType.toInt(), effect.shape.toInt())
                        } else {
                            renderFace(colorWrite, depthWrite, transparent, faceIndex)
                        }
                    }
                }
            }
        }
    }

    override fun rematerial(src: Short, dst: Short) {
        if (this.facesMaterial != null) {
            if (!this.textureAnimated && dst >= 0) {
                val material = this.toolkit.materialProvider!!.getMaterial(dst.toInt() and 0xffff)
                if (material.textureSpeedU.toInt() != 0 || material.textureSpeedV.toInt() != 0) {
                    this.textureAnimated = true
                }
            }
            for (faceIndex in 0 until this.numFaces) {
                if (this.facesMaterial!![faceIndex] == src) {
                    this.facesMaterial!![faceIndex] = dst
                }
            }
        }
    }

    override fun getMaxZ(): Int {
        if (!this.boundsUpdated) {
            calculateBounds()
        }
        return this.maxZ.toInt()
    }

    override fun getAmbient(): Int {
        return this.ambient
    }

    fun calculateBounds() {
        if (!this.boundsUpdated) {
            var s2d = 0
            var s3d = 0
            var lowX = 32767
            var lowY = 32767
            var lowZ = 32767
            var highX = -32768
            var highY = -32768
            var highZ = -32768
            for (verticeIndex in 0 until this.maxDepth) {
                val x = this.verticesX!![verticeIndex]
                val y = this.verticesY!![verticeIndex]
                val z = this.verticesZ!![verticeIndex]
                if (x < lowX) {
                    lowX = x
                }
                if (x > highX) {
                    highX = x
                }
                if (y < lowY) {
                    lowY = y
                }
                if (y > highY) {
                    highY = y
                }
                if (z < lowZ) {
                    lowZ = z
                }
                if (z > highZ) {
                    highZ = z
                }
                var size = x * x + z * z
                if (size > s2d) {
                    s2d = size
                }
                size += y * y
                if (size > s3d) {
                    s3d = size
                }
            }
            this.minX = lowX.toShort()
            this.maxX = highX.toShort()
            this.minY = lowY.toShort()
            this.maxY = highY.toShort()
            this.minZ = lowZ.toShort()
            this.maxZ = highZ.toShort()
            this.size2d = (sqrt(s2d.toDouble()) + 0.99).toInt().toShort()
            this.size3d = (sqrt(s3d.toDouble()) + 0.99).toInt().toShort()
            this.boundsUpdated = true
        }
    }

    fun calculateNormalsThreaded() {
        if (this.appearanceUpdateStage == 0 && this.vertexNormals == null) {
            if (this.toolkit.executingThreadCount > 1) {
                synchronized(this) {
                    calculateNormals()
                }
            } else {
                calculateNormals()
            }
        }
    }

    override fun areTexturesCached(): Boolean {
        if (this.facesMaterial != null) {
            for (materialIndex in this.facesMaterial!!.indices) {
                if (this.facesMaterial!![materialIndex].toInt() != -1 && !this.toolkit.isMaterialCached(this.facesMaterial!![materialIndex].toInt())) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        @JvmField
        var maxModelVertices: Int = 4096

        @JvmField
        var maxModelTriangles: Int = 4096

        @JvmStatic
        var renderCycle: Int = 0
    }
}
