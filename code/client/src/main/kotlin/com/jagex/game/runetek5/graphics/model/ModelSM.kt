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
    var sortedFaceIndices: ShortArray? = null

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

    constructor(safeModeToolkit: SafeModeToolkit) {
        this.toolkit = safeModeToolkit
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
            var faceMaterial: MaterialType? = null
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
                    faceMaterial = materialProvider!!.getMaterial(materialId.toInt() and 0xffff)
                    if ((preferenceMask and ModelFunctionMask.DISABLE_TEXTURES) != 0 && faceMaterial.detailsOnly) {
                        materialId = -1
                    } else {
                        materialEffectId = faceMaterial.effectId
                        materialEffectParam1 = faceMaterial.effectParam1
                    }
                }
            }
            val considerLayers = this.facesAlpha != null && this.facesAlpha!![face].toInt() != 0 || faceMaterial != null && faceMaterial.effectCombiner.toInt() == 2
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
                        val texOriginX = this.verticesX!![texX.toInt()].toFloat()
                        val texOriginY = this.verticesY!![texX.toInt()].toFloat()
                        val texOriginZ = this.verticesZ!![texX.toInt()].toFloat()
                        val edge1X = this.verticesX!![texY.toInt()] - texOriginX
                        val edge1Y = this.verticesY!![texY.toInt()] - texOriginY
                        val edge1Z = this.verticesZ!![texY.toInt()] - texOriginZ
                        val edge2X = this.verticesX!![texZ.toInt()] - texOriginX
                        val edge2Y = this.verticesY!![texZ.toInt()] - texOriginY
                        val edge2Z = this.verticesZ!![texZ.toInt()] - texOriginZ
                        val aOffX = this.verticesX!![a.toInt()] - texOriginX
                        val aOffY = this.verticesY!![a.toInt()] - texOriginY
                        val aOffZ = this.verticesZ!![a.toInt()] - texOriginZ
                        val bOffX = this.verticesX!![b.toInt()] - texOriginX
                        val bOffY = this.verticesY!![b.toInt()] - texOriginY
                        val bOffZ = this.verticesZ!![b.toInt()] - texOriginZ
                        val cOffX = this.verticesX!![c.toInt()] - texOriginX
                        val cOffY = this.verticesY!![c.toInt()] - texOriginY
                        val cOffZ = this.verticesZ!![c.toInt()] - texOriginZ
                        val crossX = edge1Y * edge2Z - edge1Z * edge2Y
                        val crossY = edge1Z * edge2X - edge1X * edge2Z
                        val crossZ = edge1X * edge2Y - edge1Y * edge2X
                        var basisX = edge2Y * crossZ - edge2Z * crossY
                        var basisY = edge2Z * crossX - edge2X * crossZ
                        var basisZ = edge2X * crossY - edge2Y * crossX
                        var scale = 1.0f / (basisX * edge1X + basisY * edge1Y + basisZ * edge1Z)
                        u[0] = (basisX * aOffX + basisY * aOffY + basisZ * aOffZ) * scale
                        u[1] = (basisX * bOffX + basisY * bOffY + basisZ * bOffZ) * scale
                        u[2] = (basisX * cOffX + basisY * cOffY + basisZ * cOffZ) * scale
                        basisX = edge1Y * crossZ - edge1Z * crossY
                        basisY = edge1Z * crossX - edge1X * crossZ
                        basisZ = edge1X * crossY - edge1Y * crossX
                        scale = 1.0f / (basisX * edge2X + basisY * edge2Y + basisZ * edge2Z)
                        v[0] = (basisX * aOffX + basisY * aOffY + basisZ * aOffZ) * scale
                        v[1] = (basisX * bOffX + basisY * bOffY + basisZ * bOffZ) * scale
                        v[2] = (basisX * cOffX + basisY * cOffY + basisZ * cOffZ) * scale
                    } else {
                        a = this.facesA!![face]
                        b = this.facesB!![face]
                        c = this.facesC!![face]
                        val centerX = textureMatrix!!.centerX!![textureIndex]
                        val centerY = textureMatrix.centerY!![textureIndex]
                        val centerZ = textureMatrix.centerZ!![textureIndex]
                        val matrix = textureMatrix.matrix!![textureIndex]
                        val direction = mesh.texturesDirection!![textureIndex]
                        val speed = mesh.texturesSpeed!![textureIndex] / 256.0f
                        if (textureMapping.toInt() == 1) {
                            val offset = mesh.texturesScaleZ!![textureIndex] / 1024.0f
                            mapCylindericalUv(this.verticesX!![a.toInt()], this.verticesY!![a.toInt()], this.verticesZ!![a.toInt()], centerX, centerY, centerZ, matrix, offset, direction.toInt(), speed, uvTypes)
                            u[0] = uvTypes!![0]
                            v[0] = uvTypes[1]
                            mapCylindericalUv(this.verticesX!![b.toInt()], this.verticesY!![b.toInt()], this.verticesZ!![b.toInt()], centerX, centerY, centerZ, matrix, offset, direction.toInt(), speed, uvTypes)
                            u[1] = uvTypes[0]
                            v[1] = uvTypes[1]
                            mapCylindericalUv(this.verticesX!![c.toInt()], this.verticesY!![c.toInt()], this.verticesZ!![c.toInt()], centerX, centerY, centerZ, matrix, offset, direction.toInt(), speed, uvTypes)
                            u[2] = uvTypes[0]
                            v[2] = uvTypes[1]
                            val half = offset / 2.0f
                            if ((direction.toInt() and 0x1) == 0) {
                                if (u[1] - u[0] > half) {
                                    u[1] -= offset
                                } else if (u[0] - u[1] > half) {
                                    u[1] += offset
                                }
                                if (u[2] - u[0] > half) {
                                    u[2] -= offset
                                } else if (u[0] - u[2] > half) {
                                    u[2] += offset
                                }
                            } else {
                                if (v[1] - v[0] > half) {
                                    v[1] -= offset
                                } else if (v[0] - v[1] > half) {
                                    v[1] += offset
                                }
                                if (v[2] - v[0] > half) {
                                    v[2] -= offset
                                } else if (v[0] - v[2] > half) {
                                    v[2] += offset
                                }
                            }
                        } else if (textureMapping.toInt() == 2) {
                            val uTrans = mesh.texturesUtrans!![textureIndex] / 256.0f
                            val vTrans = mesh.texturesVtrans!![textureIndex] / 256.0f
                            val edge1X = this.verticesX!![b.toInt()] - this.verticesX!![a.toInt()]
                            val edge1Y = this.verticesY!![b.toInt()] - this.verticesY!![a.toInt()]
                            val edge1Z = this.verticesZ!![b.toInt()] - this.verticesZ!![a.toInt()]
                            val edge2X = this.verticesX!![c.toInt()] - this.verticesX!![a.toInt()]
                            val edge2Y = this.verticesY!![c.toInt()] - this.verticesY!![a.toInt()]
                            val edge2Z = this.verticesZ!![c.toInt()] - this.verticesZ!![a.toInt()]
                            val crossX = edge1Y * edge2Z - edge2Y * edge1Z
                            val crossY = edge1Z * edge2X - edge2Z * edge1X
                            val crossZ = edge1X * edge2Y - edge2X * edge1Y
                            val scaleX = 64.0f / mesh.texturesScaleX!![textureIndex]
                            val scaleY = 64.0f / mesh.texturesScaleY!![textureIndex]
                            val scaleZ = 64.0f / mesh.texturesScaleZ!![textureIndex]
                            val normalX = (crossX * matrix!![0] + crossY * matrix[1] + crossZ * matrix[2]) / scaleX
                            val normalY = (crossX * matrix[3] + crossY * matrix[4] + crossZ * matrix[5]) / scaleY
                            val normalZ = (crossX * matrix[6] + crossY * matrix[7] + crossZ * matrix[8]) / scaleZ
                            val cubeFace = getCubeFace(normalX, normalY, normalZ)
                            mapRectangularUv(this.verticesX!![a.toInt()], this.verticesY!![a.toInt()], this.verticesZ!![a.toInt()], centerX, centerY, centerZ, cubeFace, matrix, direction.toInt(), speed, uTrans, vTrans, uvTypes)
                            u[0] = uvTypes!![0]
                            v[0] = uvTypes[1]
                            mapRectangularUv(this.verticesX!![b.toInt()], this.verticesY!![b.toInt()], this.verticesZ!![b.toInt()], centerX, centerY, centerZ, cubeFace, matrix, direction.toInt(), speed, uTrans, vTrans, uvTypes)
                            u[1] = uvTypes[0]
                            v[1] = uvTypes[1]
                            mapRectangularUv(this.verticesX!![c.toInt()], this.verticesY!![c.toInt()], this.verticesZ!![c.toInt()], centerX, centerY, centerZ, cubeFace, matrix, direction.toInt(), speed, uTrans, vTrans, uvTypes)
                            u[2] = uvTypes[0]
                            v[2] = uvTypes[1]
                        } else if (textureMapping.toInt() == 3) {
                            mapSphericalUv(this.verticesX!![a.toInt()], this.verticesY!![a.toInt()], this.verticesZ!![a.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), speed, uvTypes)
                            u[0] = uvTypes!![0]
                            v[0] = uvTypes[1]
                            mapSphericalUv(this.verticesX!![b.toInt()], this.verticesY!![b.toInt()], this.verticesZ!![b.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), speed, uvTypes)
                            u[1] = uvTypes[0]
                            v[1] = uvTypes[1]
                            mapSphericalUv(this.verticesX!![c.toInt()], this.verticesY!![c.toInt()], this.verticesZ!![c.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), speed, uvTypes)
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
            this.sortedFaceIndices = ShortArray(this.numFaces)
            for (faceIndex in 0 until this.numFaces) {
                this.sortedFaceIndices!![faceIndex] = faces[faceIndex].toShort()
            }
        }
    }

    override fun drawMatrixThreaded(projectionMatrix: Matrix43, boundingBox: BoundingBoxPolygon?, rasterizerFunctionMask: Int) {
        multithreadedDraw(projectionMatrix, boundingBox, rasterizerFunctionMask)
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

    override fun setContrast(contrast: Int) {
        if ((this.functionMask and ModelFunctionMask.ALTERS_CONTRAST) != 8192) {
            throw IllegalStateException()
        } else {
            this.contrast = contrast
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
        for (verticeIndex in 0 until this.maxDepth) {
            this.vertexNormals!![verticeIndex] = VertexNormal()
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
                            val ambientLightness = (colorHsl and 0x7f) * this.ambient shr 7
                            val adjustedHsl = ColorUtil.adjustHsv(colorHsl and -128 or ambientLightness)
                            val faceNormal = this.facesNormal!![faceIndex]!!
                            val dotProduct = lightX * faceNormal.x + lightY * faceNormal.y + lightZ * faceNormal.z shr 16
                            val finalContrast = if (dotProduct > 256) lowContrast else highContrast
                            val lightness = (light shr 1) + (finalContrast * dotProduct shr 17)
                            this.facesColourA!![faceIndex] = lightness shl 17 or ColorUtil.repackHsl(adjustedHsl.toInt(), lightness)
                            this.facesColourC!![faceIndex] = -1
                        } else if (renderType.toInt() == 3) {
                            this.facesColourA!![faceIndex] = 128
                            this.facesColourC!![faceIndex] = -1
                        } else {
                            this.facesColourC!![faceIndex] = -2
                        }
                    } else {
                        val colorHsl = this.facesColor!![faceIndex].toInt() and 0xffff
                        val ambientLightness = (colorHsl and 0x7f) * this.ambient shr 7
                        val adjustedHsl = ColorUtil.adjustHsv(colorHsl and -128 or ambientLightness)
                        var vertexNormal: VertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesA!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesA!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesA!![faceIndex].toInt()]!!
                        }
                        var dotProduct = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        var finalContrast = if (dotProduct > 256) lowContrast else highContrast
                        var lightness = (light shr 1) + (finalContrast * dotProduct shr 17)
                        this.facesColourA!![faceIndex] = lightness shl 17 or ColorUtil.repackHsl(adjustedHsl.toInt(), lightness)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesB!![faceIndex].toInt()]!!
                        }
                        dotProduct = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (dotProduct > 256) lowContrast else highContrast
                        lightness = (light shr 1) + (finalContrast * dotProduct shr 17)
                        this.facesColourB!![faceIndex] = lightness shl 17 or ColorUtil.repackHsl(adjustedHsl.toInt(), lightness)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesC!![faceIndex].toInt()]!!
                        }
                        dotProduct = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (dotProduct > 256) lowContrast else highContrast
                        lightness = (light shr 1) + (finalContrast * dotProduct shr 17)
                        this.facesColourC!![faceIndex] = lightness shl 17 or ColorUtil.repackHsl(adjustedHsl.toInt(), lightness)
                    }
                } else {
                    val colorHsl = this.facesColor!![faceIndex].toInt() and 0xffff
                    if (renderType.toInt() != 0) {
                        if (renderType.toInt() == 1) {
                            val faceNormal = this.facesNormal!![faceIndex]!!
                            val dotProduct = lightX * faceNormal.x + lightY * faceNormal.y + lightZ * faceNormal.z shr 16
                            val finalContrast = if (dotProduct > 256) lowContrast else highContrast
                            val lightness = signLight((light shr 2) + (finalContrast * dotProduct shr 18))
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
                        var dotProduct = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        var finalContrast = if (dotProduct > 256) lowContrast else highContrast
                        var lightness = signLight((light shr 2) + (finalContrast * dotProduct shr 18))
                        this.facesColourA!![faceIndex] = lightness shl 24 or getMaterialColor(colorHsl, materialId, lightness)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesB!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesB!![faceIndex].toInt()]!!
                        }
                        dotProduct = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (dotProduct > 256) lowContrast else highContrast
                        lightness = signLight((light shr 2) + (finalContrast * dotProduct shr 18))
                        this.facesColourB!![faceIndex] = lightness shl 24 or getMaterialColor(colorHsl, materialId, lightness)
                        vertexNormal = if (this.vertexNormalOffsets != null && this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()] != null) {
                            this.vertexNormalOffsets!![this.facesC!![faceIndex].toInt()]!!
                        } else {
                            this.vertexNormals!![this.facesC!![faceIndex].toInt()]!!
                        }
                        dotProduct = (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / vertexNormal.magnitude shr 16
                        finalContrast = if (dotProduct > 256) lowContrast else highContrast
                        lightness = signLight((light shr 2) + (finalContrast * dotProduct shr 18))
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
        val materialAlpha = material.alpha.toInt() and 0xff
        var brightness: Int
        if (materialAlpha != 0) {
            brightness = lightness * 131586
            val inverseAlpha = 256 - materialAlpha
            color = ((brightness and 0xff00ff) * materialAlpha + (color and 0xff00ff) * inverseAlpha and -16711936) + (materialAlpha * (brightness and 0xff00) + inverseAlpha * (color and 0xff00) and 0xff0000) shr 8
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

    fun signLight(lightValue: Int): Int {
        var clamped = lightValue
        if (clamped < 2) {
            clamped = 2
        } else if (clamped > 126) {
            clamped = 126
        }
        return clamped
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
                val colorHsl = this.facesColor!![faceIndex].toInt() and 0xffff
                val ambientLightness = (colorHsl and 0x7f) * this.ambient shr 7
                val adjustedHsl = ColorUtil.adjustHsv(colorHsl and -128 or ambientLightness)
                val lightnessA: Int
                if (this.facesColourC!![faceIndex] == -1) {
                    lightnessA = this.facesColourA!![faceIndex] and -131072
                    this.facesColourA!![faceIndex] = lightnessA or ColorUtil.repackHsl(adjustedHsl.toInt(), lightnessA shr 17)
                } else if (this.facesColourC!![faceIndex] != -2) {
                    lightnessA = this.facesColourA!![faceIndex] and -131072
                    this.facesColourA!![faceIndex] = lightnessA or ColorUtil.repackHsl(adjustedHsl.toInt(), lightnessA shr 17)
                    val lightnessB = this.facesColourB!![faceIndex] and -131072
                    this.facesColourB!![faceIndex] = lightnessB or ColorUtil.repackHsl(adjustedHsl.toInt(), lightnessB shr 17)
                    val lightnessC = this.facesColourC!![faceIndex] and -131072
                    this.facesColourC!![faceIndex] = lightnessC or ColorUtil.repackHsl(adjustedHsl.toInt(), lightnessC shr 17)
                }
            }
        }
        this.appearanceUpdateStage = 2
    }

    fun hasBillboardOnFace(faceIndex: Int): Boolean {
        return this.billboardOnFace != null && this.billboardOnFace!![faceIndex] != -1
    }

    fun copy(modelDst: ModelSM, modelSrc: ModelSM, copyFlags: Int, expand: Boolean, recalculateBeforeCopy: Boolean): Model {
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
        if ((copyFlags and ModelFunctionMask.ALTERS_ALPHA) != 0) {
            modelDst.accountForPriorities = true
        } else {
            modelDst.accountForPriorities = this.accountForPriorities
        }
        modelDst.textureAnimated = this.textureAnimated
        val groundAdjustment = ((copyFlags and ModelFunctionMask.FULL_DISPLACEMENT) == 7) || ((copyFlags and 0x20) != 0)
        val displacementX = groundAdjustment || (copyFlags and ModelFunctionMask.DISPLACE_X) != 0
        val displacementY = groundAdjustment || (copyFlags and ModelFunctionMask.DISPLACE_Y) != 0
        val displacementZ = groundAdjustment || (copyFlags and ModelFunctionMask.DISPLACE_Z) != 0 || (copyFlags and ModelFunctionMask.INVERT) != 0
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
        if ((copyFlags and 0x84080) != 0) {
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
        if ((copyFlags and 0x97018) != 0) {
            modelDst.appearanceUpdateStage = 0
            modelDst.facesColourC = null
            modelDst.facesColourB = null
            modelDst.facesColourA = null
        } else if ((copyFlags and 0x80) != 0) {
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
        if ((copyFlags and ModelFunctionMask.ALTERS_ALPHA) != 0) {
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
        if ((copyFlags and 0x8) == 0 && (copyFlags and ModelFunctionMask.INVERT) == 0) {
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
        if ((copyFlags and ModelFunctionMask.RETEXTURE) != 0) {
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
        if ((copyFlags and 0x10000) != 0) {
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
        if ((copyFlags and 0xc580) != 0) {
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
        if (this.uMap != null && (copyFlags and ModelFunctionMask.INVERT) != 0) {
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
        modelDst.sortedFaceIndices = this.sortedFaceIndices
        modelDst.functionMask = copyFlags
        return modelDst
    }

    override fun getFunctionMask(): Int {
        return this.functionMask
    }

    override fun uploadToGpu() {
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
                for (verticeIndex in 0 until this.maxDepth) {
                    var tmpX = xan * this.verticesZ!![verticeIndex] + zan * this.verticesX!![verticeIndex] shr 14
                    this.verticesZ!![verticeIndex] = zan * this.verticesZ!![verticeIndex] - xan * this.verticesX!![verticeIndex] shr 14
                    this.verticesX!![verticeIndex] = tmpX
                    if (this.vertexNormals!![verticeIndex] != null) {
                        tmpX = xan * this.vertexNormals!![verticeIndex]!!.z + zan * this.vertexNormals!![verticeIndex]!!.x shr 14
                        this.vertexNormals!![verticeIndex]!!.z = zan * this.vertexNormals!![verticeIndex]!!.z - xan * this.vertexNormals!![verticeIndex]!!.x shr 14
                        this.vertexNormals!![verticeIndex]!!.x = tmpX
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
                for (verticeIndex in this.maxDepth until this.numVertices) {
                    val tmpX = xan * this.verticesZ!![verticeIndex] + zan * this.verticesX!![verticeIndex] shr 14
                    this.verticesZ!![verticeIndex] = zan * this.verticesZ!![verticeIndex] - xan * this.verticesX!![verticeIndex] shr 14
                    this.verticesX!![verticeIndex] = tmpX
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
            for (verticeIndex in 0 until this.maxDepth) {
                this.verticesX!![verticeIndex] = -this.verticesX!![verticeIndex]
                this.verticesZ!![verticeIndex] = -this.verticesZ!![verticeIndex]
                if (this.vertexNormals!![verticeIndex] != null) {
                    this.vertexNormals!![verticeIndex]!!.x = -this.vertexNormals!![verticeIndex]!!.x
                    this.vertexNormals!![verticeIndex]!!.z = -this.vertexNormals!![verticeIndex]!!.z
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
            for (verticeIndex in this.maxDepth until this.numVertices) {
                this.verticesX!![verticeIndex] = -this.verticesX!![verticeIndex]
                this.verticesZ!![verticeIndex] = -this.verticesZ!![verticeIndex]
            }
            this.appearanceUpdateStage = 0
            this.boundsUpdated = false
        }
    }

    fun rotate270DegWithNormals() {
        synchronized(this) {
            for (verticeIndex in 0 until this.maxDepth) {
                var tmpX = this.verticesZ!![verticeIndex]
                this.verticesZ!![verticeIndex] = this.verticesX!![verticeIndex]
                this.verticesX!![verticeIndex] = -tmpX
                if (this.vertexNormals!![verticeIndex] != null) {
                    tmpX = this.vertexNormals!![verticeIndex]!!.z
                    this.vertexNormals!![verticeIndex]!!.z = this.vertexNormals!![verticeIndex]!!.x
                    this.vertexNormals!![verticeIndex]!!.x = -tmpX
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
            for (verticeIndex in this.maxDepth until this.numVertices) {
                val tmpX = this.verticesZ!![verticeIndex]
                this.verticesZ!![verticeIndex] = this.verticesX!![verticeIndex]
                this.verticesX!![verticeIndex] = -tmpX
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
                for (verticeIndex in 0 until this.numVertices) {
                    val tmpY = zan * this.verticesY!![verticeIndex] - xan * this.verticesZ!![verticeIndex] shr 14
                    this.verticesZ!![verticeIndex] = xan * this.verticesY!![verticeIndex] + zan * this.verticesZ!![verticeIndex] shr 14
                    this.verticesY!![verticeIndex] = tmpY
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
                for (verticeIndex in 0 until this.maxDepth) {
                    if (this.vertexNormalOffsets!![verticeIndex] != null) {
                        this.vertexNormalOffsets!![verticeIndex]!!.z = -this.vertexNormalOffsets!![verticeIndex]!!.z
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

    override fun animatePartialtransform(transformationType: Int, labels: IntArray, transformX: Int, transformY: Int, transformZ: Int, rotation: Int, lights: Boolean) {
        var transX = transformX
        var transY = transformY
        var transZ = transformZ
        val labelCount = labels.size
        if (transformationType == 0) {
            transX = transX shl 4
            transY = transY shl 4
            transZ = transZ shl 4
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
                    val verticeIndices = this.vertexGroups!![label]!!
                    for (index in verticeIndices.indices) {
                        val vertice = verticeIndices[index]
                        this.originX += this.verticesX!![vertice]
                        this.originY += this.verticesY!![vertice]
                        this.originZ += this.verticesZ!![vertice]
                        transformationCount++
                    }
                }
            }
            if (transformationCount > 0) {
                this.originX = transX + this.originX / transformationCount
                this.originY = this.originY / transformationCount + transY
                this.originZ = transZ + this.originZ / transformationCount
            } else {
                this.originX = transX
                this.originY = transY
                this.originZ = transZ
            }
        } else if (transformationType == 1) {
            transX = transX shl 4
            transY = transY shl 4
            transZ = transZ shl 4
            if (!this.upscaled) {
                for (verticeIndex in 0 until this.numVertices) {
                    this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                    this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                    this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                }
                this.upscaled = true
            }
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < this.vertexGroups!!.size) {
                    val verticeIndices = this.vertexGroups!![label]!!
                    for (index in verticeIndices.indices) {
                        val vertice = verticeIndices[index]
                        this.verticesX!![vertice] += transX
                        this.verticesY!![vertice] += transY
                        this.verticesZ!![vertice] += transZ
                    }
                }
            }
        } else if (transformationType == 2) {
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < this.vertexGroups!!.size) {
                    val verticeIndices = this.vertexGroups!![label]!!
                    if ((rotation and 0x1) == 0) {
                        for (index in verticeIndices.indices) {
                            val vertice = verticeIndices[index]
                            this.verticesX!![vertice] -= this.originX
                            this.verticesY!![vertice] -= this.originY
                            this.verticesZ!![vertice] -= this.originZ
                            if (transZ != 0) {
                                val xan = MathUtils.SINE[transZ]
                                val zan = MathUtils.COSINE[transZ]
                                val tmpY = xan * this.verticesY!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = zan * this.verticesY!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesX!![vertice] = tmpY
                            }
                            if (transX != 0) {
                                val xan = MathUtils.SINE[transX]
                                val zan = MathUtils.COSINE[transX]
                                val tmpZ = zan * this.verticesY!![vertice] - xan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesZ!![vertice] = xan * this.verticesY!![vertice] + zan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = tmpZ
                            }
                            if (transY != 0) {
                                val xan = MathUtils.SINE[transY]
                                val zan = MathUtils.COSINE[transY]
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
                            if (transX != 0) {
                                val xan = MathUtils.SINE[transX]
                                val zan = MathUtils.COSINE[transX]
                                val tmpZ = zan * this.verticesY!![vertice] - xan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesZ!![vertice] = xan * this.verticesY!![vertice] + zan * this.verticesZ!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = tmpZ
                            }
                            if (transZ != 0) {
                                val xan = MathUtils.SINE[transZ]
                                val zan = MathUtils.COSINE[transZ]
                                val tmpY = xan * this.verticesY!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesY!![vertice] = zan * this.verticesY!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                this.verticesX!![vertice] = tmpY
                            }
                            if (transY != 0) {
                                val xan = MathUtils.SINE[transY]
                                val zan = MathUtils.COSINE[transY]
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
                val label = labels[labelIndex]
                if (label < this.vertexGroups!!.size) {
                    val verticeIndices = this.vertexGroups!![label]!!
                    for (index in verticeIndices.indices) {
                        val vertice = verticeIndices[index]
                        this.verticesX!![vertice] -= this.originX
                        this.verticesY!![vertice] -= this.originY
                        this.verticesZ!![vertice] -= this.originZ
                        this.verticesX!![vertice] = transX * this.verticesX!![vertice] / 128
                        this.verticesY!![vertice] = transY * this.verticesY!![vertice] / 128
                        this.verticesZ!![vertice] = transZ * this.verticesZ!![vertice] / 128
                        this.verticesX!![vertice] += this.originX
                        this.verticesY!![vertice] += this.originY
                        this.verticesZ!![vertice] += this.originZ
                    }
                }
            }
        } else if (transformationType == 5) {
            if (this.faceGroups != null && this.facesAlpha != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.faceGroups!!.size) {
                        val faceIndices = this.faceGroups!![label]!!
                        for (index in faceIndices.indices) {
                            val face = faceIndices[index]
                            var alpha = (this.facesAlpha!![face].toInt() and 0xff) + transX * 8
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
                            h = transX + h and 0x3f
                            s += transY
                            if (s < 0) {
                                s = 0
                            } else if (s > 7) {
                                s = 7
                            }
                            l += transZ
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
                            surface.offsetX += transX
                            surface.offsetY += transY
                        }
                    }
                }
            }
        } else if (transformationType == 10) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val billboardIndices = this.billboardGroups!![label]!!
                        for (index in billboardIndices.indices) {
                            val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                            surface.scaleX = surface.scaleX * transX / 128.0f
                            surface.scaleY = surface.scaleY * transY / 128.0f
                        }
                    }
                }
            }
        } else if (transformationType == 9 && this.billboardGroups != null) {
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < this.billboardGroups!!.size) {
                    val billboardIndices = this.billboardGroups!![label]!!
                    for (index in billboardIndices.indices) {
                        val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                        surface.rotation = transX + surface.rotation and 0x3fff
                    }
                }
            }
        }
    }

    override fun animateTransform(transformationType: Int, labels: IntArray, transformX: Int, transformY: Int, transformZ: Int, lights: Boolean, submesh: Int, verticesData: IntArray) {
        var transX = transformX
        var transY = transformY
        var transZ = transformZ
        val labelCount = labels.size
        if (transformationType == 0) {
            transX = transX shl 4
            transY = transY shl 4
            transZ = transZ shl 4
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
                    val verticeIndices = this.vertexGroups!![label]!!
                    for (index in verticeIndices.indices) {
                        val vertice = verticeIndices[index]
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
                this.originX = transX + this.originX / transformationCount
                this.originY = this.originY / transformationCount + transY
                this.originZ = transZ + this.originZ / transformationCount
                this.withinOrigin = true
            } else {
                this.originX = transX
                this.originY = transY
                this.originZ = transZ
            }
        } else if (transformationType == 1) {
            if (verticesData.isNotEmpty()) {
                val rotatedX = transZ * verticesData[2] + verticesData[1] * transY + transX * verticesData[0] + 8192 shr 14
                val rotatedY = transX * verticesData[3] + verticesData[5] * transZ + transY * verticesData[4] + 8192 shr 14
                val rotatedZ = transX * verticesData[6] + verticesData[7] * transY + transZ * verticesData[8] + 8192 shr 14
                transX = rotatedX
                transY = rotatedY
                transZ = rotatedZ
            }
            transX = transX shl 4
            transY = transY shl 4
            transZ = transZ shl 4
            if (!this.upscaled) {
                for (verticeIndex in 0 until this.numVertices) {
                    this.verticesX!![verticeIndex] = this.verticesX!![verticeIndex] shl 4
                    this.verticesY!![verticeIndex] = this.verticesY!![verticeIndex] shl 4
                    this.verticesZ!![verticeIndex] = this.verticesZ!![verticeIndex] shl 4
                }
                this.upscaled = true
            }
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < this.vertexGroups!!.size) {
                    val verticeIndices = this.vertexGroups!![label]!!
                    for (index in verticeIndices.indices) {
                        val vertice = verticeIndices[index]
                        if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                            this.verticesX!![vertice] += transX
                            this.verticesY!![vertice] += transY
                            this.verticesZ!![vertice] += transZ
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
                val rotationMatrix = IntArray(9)
                val zanX = MathUtils.COSINE[transX]
                val xanX = MathUtils.SINE[transX]
                val zanY = MathUtils.COSINE[transY]
                val xanY = MathUtils.SINE[transY]
                val zanZ = MathUtils.COSINE[transZ]
                val xanZ = MathUtils.SINE[transZ]
                val sinXCosZ = xanX * zanZ + 8192 shr 14
                val sinXSinZ = xanX * xanZ + 8192 shr 14
                rotationMatrix[0] = zanY * zanZ + xanY * sinXSinZ + 8192 shr 14
                rotationMatrix[1] = xanY * sinXCosZ + -zanY * xanZ + 8192 shr 14
                rotationMatrix[2] = xanY * zanX + 8192 shr 14
                rotationMatrix[3] = zanX * xanZ + 8192 shr 14
                rotationMatrix[4] = zanX * zanZ + 8192 shr 14
                rotationMatrix[5] = -xanX
                rotationMatrix[6] = zanY * sinXSinZ + -xanY * zanZ + 8192 shr 14
                rotationMatrix[7] = xanY * xanZ + zanY * sinXCosZ + 8192 shr 14
                rotationMatrix[8] = zanY * zanX + 8192 shr 14
                val offsetX = rotationMatrix[2] * -this.originZ + rotationMatrix[0] * -this.originX + rotationMatrix[1] * -this.originY + 8192 shr 14
                val offsetY = rotationMatrix[5] * -this.originZ + rotationMatrix[4] * -this.originY + rotationMatrix[3] * -this.originX + 8192 shr 14
                val offsetZ = rotationMatrix[6] * -this.originX + rotationMatrix[8] * -this.originZ + rotationMatrix[7] * -this.originY + 8192 shr 14
                val offsetOriginX = offsetX + this.originX
                val offsetOriginY = offsetY + this.originY
                val offsetOriginZ = offsetZ + this.originZ
                val offsetCluster = IntArray(9)
                for (row in 0..2) {
                    for (col in 0..2) {
                        var offset = 0
                        for (k in 0..2) {
                            offset += verticesData[k + col * 3] * rotationMatrix[k + row * 3]
                        }
                        offsetCluster[col + row * 3] = offset + 8192 shr 14
                    }
                }
                var fullOffX = deltaY * rotationMatrix[1] + deltaX * rotationMatrix[0] + deltaZ * rotationMatrix[2] + 8192 shr 14
                var fullOffY = deltaY * rotationMatrix[4] + deltaX * rotationMatrix[3] + deltaZ * rotationMatrix[5] + 8192 shr 14
                var fullOffZ = rotationMatrix[6] * deltaX + deltaZ * rotationMatrix[8] + deltaY * rotationMatrix[7] + 8192 shr 14
                fullOffX += offsetOriginX
                fullOffY += offsetOriginY
                fullOffZ += offsetOriginZ
                val offsetMatrix = IntArray(9)
                for (row in 0..2) {
                    for (col in 0..2) {
                        var offset = 0
                        for (k in 0..2) {
                            offset += offsetCluster[col + k * 3] * verticesData[k + row * 3]
                        }
                        offsetMatrix[col + row * 3] = offset + 8192 shr 14
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
                                val x = offsetMatrix[0] * this.verticesX!![vertice] + offsetMatrix[2] * this.verticesZ!![vertice] + offsetMatrix[1] * this.verticesY!![vertice] + 8192 shr 14
                                val y = offsetMatrix[4] * this.verticesY!![vertice] + offsetMatrix[5] * this.verticesZ!![vertice] + offsetMatrix[3] * this.verticesX!![vertice] + 8192 shr 14
                                val z = offsetMatrix[6] * this.verticesX!![vertice] + offsetMatrix[7] * this.verticesY!![vertice] + offsetMatrix[8] * this.verticesZ!![vertice] + 8192 shr 14
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
                        val verticeIndices = this.vertexGroups!![label]!!
                        for (index in verticeIndices.indices) {
                            val vertice = verticeIndices[index]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                                this.verticesX!![vertice] -= this.originX
                                this.verticesY!![vertice] -= this.originY
                                this.verticesZ!![vertice] -= this.originZ
                                if (transZ != 0) {
                                    val xan = MathUtils.SINE[transZ]
                                    val zan = MathUtils.COSINE[transZ]
                                    val tmpY = xan * this.verticesY!![vertice] + zan * this.verticesX!![vertice] + 16383 shr 14
                                    this.verticesY!![vertice] = zan * this.verticesY!![vertice] - xan * this.verticesX!![vertice] + 16383 shr 14
                                    this.verticesX!![vertice] = tmpY
                                }
                                if (transX != 0) {
                                    val xan = MathUtils.SINE[transX]
                                    val zan = MathUtils.COSINE[transX]
                                    val tmpZ = zan * this.verticesY!![vertice] - xan * this.verticesZ!![vertice] + 16383 shr 14
                                    this.verticesZ!![vertice] = xan * this.verticesY!![vertice] + zan * this.verticesZ!![vertice] + 16383 shr 14
                                    this.verticesY!![vertice] = tmpZ
                                }
                                if (transY != 0) {
                                    val xan = MathUtils.SINE[transY]
                                    val zan = MathUtils.COSINE[transY]
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
                val transXScaled = transX shl 15 shr 7
                val transYScaled = transY shl 15 shr 7
                val transZScaled = transZ shl 15 shr 7
                val offX = transXScaled * -this.originX + 8192 shr 14
                val offY = transYScaled * -this.originY + 8192 shr 14
                val offZ = transZScaled * -this.originZ + 8192 shr 14
                val originXOffset = offX + this.originX
                val originYOffset = offY + this.originY
                val originZOffset = offZ + this.originZ
                val offsetCluster = intArrayOf(
                    transXScaled * verticesData[0] + 8192 shr 14, transXScaled * verticesData[3] + 8192 shr 14,
                    transXScaled * verticesData[6] + 8192 shr 14, transYScaled * verticesData[1] + 8192 shr 14,
                    transYScaled * verticesData[4] + 8192 shr 14, transYScaled * verticesData[7] + 8192 shr 14,
                    transZScaled * verticesData[2] + 8192 shr 14, transZScaled * verticesData[5] + 8192 shr 14,
                    transZScaled * verticesData[8] + 8192 shr 14
                )
                var offsetTransX = transXScaled * deltaX + 8192 shr 14
                var offsetTransY = transYScaled * deltaY + 8192 shr 14
                var offsetTransZ = transZScaled * deltaZ + 8192 shr 14
                offsetTransX += originXOffset
                offsetTransY += originYOffset
                offsetTransZ += originZOffset
                val offsetMatrix = IntArray(9)
                for (row in 0..2) {
                    for (col in 0..2) {
                        var offset = 0
                        for (k in 0..2) {
                            offset += offsetCluster[col + k * 3] * verticesData[k + row * 3]
                        }
                        offsetMatrix[col + row * 3] = offset + 8192 shr 14
                    }
                }
                var finalOffX = offsetTransZ * verticesData[2] + offsetTransX * verticesData[0] + verticesData[1] * offsetTransY + 8192 shr 14
                var finalOffY = offsetTransX * verticesData[3] + offsetTransY * verticesData[4] + verticesData[5] * offsetTransZ + 8192 shr 14
                var finalOffZ = offsetTransX * verticesData[6] + offsetTransZ * verticesData[8] + verticesData[7] * offsetTransY + 8192 shr 14
                finalOffX += originalX
                finalOffY += originalY
                finalOffZ += originalZ
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.vertexGroups!!.size) {
                        val verticeIndices = this.vertexGroups!![label]!!
                        for (index in verticeIndices.indices) {
                            val vertice = verticeIndices[index]
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
                    val label = labels[labelIndex]
                    if (label < this.vertexGroups!!.size) {
                        val verticeIndices = this.vertexGroups!![label]!!
                        for (index in verticeIndices.indices) {
                            val vertice = verticeIndices[index]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![vertice].toInt()) != 0) {
                                this.verticesX!![vertice] -= this.originX
                                this.verticesY!![vertice] -= this.originY
                                this.verticesZ!![vertice] -= this.originZ
                                this.verticesX!![vertice] = transX * this.verticesX!![vertice] / 128
                                this.verticesY!![vertice] = transY * this.verticesY!![vertice] / 128
                                this.verticesZ!![vertice] = transZ * this.verticesZ!![vertice] / 128
                                this.verticesX!![vertice] += this.originX
                                this.verticesY!![vertice] += this.originY
                                this.verticesZ!![vertice] += this.originZ
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
                        val faceIndices = this.faceGroups!![label]!!
                        for (index in faceIndices.indices) {
                            val face = faceIndices[index]
                            if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![face].toInt()) != 0) {
                                var alpha = (this.facesAlpha!![face].toInt() and 0xff) + transX * 8
                                if (alpha < 0) {
                                    alpha = 0
                                } else if (alpha > 255) {
                                    alpha = 255
                                }
                                this.facesAlpha!![face] = alpha.toByte()
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
                        val faceIndices = this.faceGroups!![label]!!
                        for (index in faceIndices.indices) {
                            val face = faceIndices[index]
                            if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![face].toInt()) != 0) {
                                val faceColorHsl = this.facesColor!![face].toInt() and 0xffff
                                var h = faceColorHsl shr 10 and 0x3f
                                var s = faceColorHsl shr 7 and 0x7
                                var l = faceColorHsl and 0x7f
                                h = transX + h and 0x3f
                                s += transY
                                if (s < 0) {
                                    s = 0
                                } else if (s > 7) {
                                    s = 7
                                }
                                l += transZ
                                if (l < 0) {
                                    l = 0
                                } else if (l > 127) {
                                    l = 127
                                }
                                this.facesColor!![face] = (h shl 10 or (s shl 7) or l).toShort()
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
                        val billboardIndices = this.billboardGroups!![label]!!
                        for (index in billboardIndices.indices) {
                            val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                            surface.offsetX += transX
                            surface.offsetY += transY
                        }
                    }
                }
            }
        } else if (transformationType == 10) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val billboardIndices = this.billboardGroups!![label]!!
                        for (index in billboardIndices.indices) {
                            val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                            surface.scaleX = surface.scaleX * transX / 128.0f
                            surface.scaleY = surface.scaleY * transY / 128.0f
                        }
                    }
                }
            }
        } else if (transformationType == 9 && this.billboardGroups != null) {
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < this.billboardGroups!!.size) {
                    val billboardIndices = this.billboardGroups!![label]!!
                    for (index in billboardIndices.indices) {
                        val surface = this.billboardSurfaces!![billboardIndices[index]]!!
                        surface.rotation = transX + surface.rotation and 0x3fff
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
            for (verticeIndex in 0 until this.numVertices) {
                this.verticesX!![verticeIndex] -= this.originX
                this.verticesY!![verticeIndex] -= this.originY
                this.verticesZ!![verticeIndex] -= this.originZ
                this.verticesX!![verticeIndex] = x * this.verticesX!![verticeIndex] / 128
                this.verticesY!![verticeIndex] = y * this.verticesY!![verticeIndex] / 128
                this.verticesZ!![verticeIndex] = z * this.verticesZ!![verticeIndex] / 128
                this.verticesX!![verticeIndex] += this.originX
                this.verticesY!![verticeIndex] += this.originY
                this.verticesZ!![verticeIndex] += this.originZ
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
                for (billboardIndex in 0 until this.numBillboards) {
                    val effect = this.billboardEffects!![billboardIndex]!!
                    val surface = this.billboardSurfaces!![billboardIndex]!!
                    surface.diffuseColor = surface.diffuseColor and 0xffffff or (255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff) shl 24)
                }
            }
        } else if (transformationType == 7) {
            for (faceIndex in 0 until this.numFaces) {
                val faceColorHsl = this.facesColor!![faceIndex].toInt() and 0xffff
                var h = faceColorHsl shr 10 and 0x3f
                var s = faceColorHsl shr 7 and 0x7
                var l = faceColorHsl and 0x7f
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
                for (billboardIndex in 0 until this.numBillboards) {
                    val effect = this.billboardEffects!![billboardIndex]!!
                    val surface = this.billboardSurfaces!![billboardIndex]!!
                    surface.diffuseColor = surface.diffuseColor and 0xffffff.inv() or (ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(this.facesColor!![effect.appliesOnFace].toInt() and 0xffff).toInt() and 0xffff] and 0xffffff)
                }
            }
        } else if (transformationType == 8) {
            for (billboardIndex in 0 until this.numBillboards) {
                val surface = this.billboardSurfaces!![billboardIndex]!!
                surface.offsetX += x
                surface.offsetY += y
            }
        } else if (transformationType == 10) {
            for (billboardIndex in 0 until this.numBillboards) {
                val surface = this.billboardSurfaces!![billboardIndex]!!
                surface.scaleX = surface.scaleX * x / 128.0f
                surface.scaleY = surface.scaleY * y / 128.0f
            }
        } else if (transformationType == 9) {
            for (billboardIndex in 0 until this.numBillboards) {
                val surface = this.billboardSurfaces!![billboardIndex]!!
                surface.rotation = x + surface.rotation and 0x3fff
            }
        }
    }

    fun multithreadedDraw(projectionMatrix: Matrix43, renderedEntity: BoundingBoxPolygon?, rasterizerFlags: Int) {
        if (this.maxDepth >= 1) {
            val resource = this.toolkit.getThreadResource(Thread.currentThread())!!
            val particleMatrix = resource.particleMatrix
            particleMatrix.fromMatrix(projectionMatrix)
            val viewMatrix = this.toolkit.viewMatrix44!!
            val modelMatrix = this.toolkit.modelMatrix!!
            if (!this.boundsUpdated) {
                calculateBounds()
            }
            val isIdentityMatrix = particleMatrix.isIdentity()
            val particleOffsets = resource.particleOffsets
            particleMatrix.scale(0.0f, this.minY.toFloat(), 0.0f, particleOffsets)
            val offsetXA = particleOffsets!![0]
            val offsetYA = particleOffsets[1]
            val offsetZA = particleOffsets[2]
            particleMatrix.scale(0.0f, this.maxY.toFloat(), 0.0f, particleOffsets)
            val offsetXB = particleOffsets[0]
            val offsetYB = particleOffsets[1]
            val offsetZB = particleOffsets[2]
            var distMinY: Float
            var distMaxY: Float
            for (planeIndex in 0..5) {
                val plane = this.toolkit.frustumPlanes!![planeIndex]
                distMinY = plane[0] * offsetXA + plane[1] * offsetYA + plane[2] * offsetZA + plane[3] + this.size2d
                distMaxY = plane[0] * offsetXB + plane[1] * offsetYB + plane[2] * offsetZB + plane[3] + this.size2d
                if (distMinY < 0.0f && distMaxY < 0.0f) {
                    return
                }
            }
            val viewDepthPerY: Float
            val viewDepthBase: Float
            if (isIdentityMatrix) {
                viewDepthBase = viewMatrix.elements[14]
                viewDepthPerY = viewMatrix.elements[6]
            } else {
                viewDepthBase = particleMatrix.elements[12] * viewMatrix.elements[2] + particleMatrix.elements[13] * viewMatrix.elements[6] + particleMatrix.elements[14] * viewMatrix.elements[10] + viewMatrix.elements[14]
                viewDepthPerY = particleMatrix.elements[4] * viewMatrix.elements[2] + particleMatrix.elements[5] * viewMatrix.elements[6] + particleMatrix.elements[6] * viewMatrix.elements[10]
            }
            distMinY = viewDepthBase + this.minY * viewDepthPerY
            distMaxY = viewDepthBase + this.maxY * viewDepthPerY
            val furthestDist = if (distMinY > distMaxY) distMinY + this.size2d else distMaxY + this.size2d
            val fogDepth = modelMatrix.elements[10] * furthestDist + modelMatrix.elements[14]
            if (this.toolkit.executingThreadCount > 1) {
                synchronized(this) {
                    while (this.renderLock) {
                        try {
                            (this as Object).wait()
                        } catch (ie: InterruptedException) {
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
            this.threadResource!!.cachedDepthRangeFar = this.rasterizer!!.depthRangeFar
            this.threadResource!!.inverseDepthRangeFar = 1.0f / this.rasterizer!!.depthRangeFar
            this.threadResource!!.centerX = this.rasterizer!!.centerX
            this.threadResource!!.centerY = this.rasterizer!!.centerY
            this.threadResource!!.cachedDepthRangeNear = this.rasterizer!!.depthRangeNear
            this.threadResource!!.clipWidth = this.rasterizer!!.clipWidth
            for (verticeIndex in 0 until this.numVertices) {
                val verticeX = this.verticesX!![verticeIndex]
                val verticeY = this.verticesY!![verticeIndex]
                val verticeZ = this.verticesZ!![verticeIndex]
                val clipX = renderMatrix.elements[0] * verticeX + renderMatrix.elements[4] * verticeY + renderMatrix.elements[8] * verticeZ + renderMatrix.elements[12]
                val clipY = renderMatrix.elements[1] * verticeX + renderMatrix.elements[5] * verticeY + renderMatrix.elements[9] * verticeZ + renderMatrix.elements[13]
                val clipZ = renderMatrix.elements[2] * verticeX + renderMatrix.elements[6] * verticeY + renderMatrix.elements[10] * verticeZ + renderMatrix.elements[14]
                val clipW = renderMatrix.elements[3] * verticeX + renderMatrix.elements[7] * verticeY + renderMatrix.elements[11] * verticeZ + renderMatrix.elements[15]
                this.projectedVerticesZ!![verticeIndex] = this.threadResource!!.cachedDepthRangeNear + this.threadResource!!.cachedDepthRangeFar * clipZ / clipW
                this.projectedVerticesZ2!![verticeIndex] = clipW
                if (clipZ >= -clipW) {
                    this.projectedVerticesX!![verticeIndex] = (this.threadResource!!.centerX + this.threadResource!!.offsetX * clipX / clipW)
                    this.projectedVerticesY!![verticeIndex] = (this.threadResource!!.centerY + this.threadResource!!.offsetY * clipY / clipW)
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
                    var clipX = renderMatrix.elements[0] * avgX + renderMatrix.elements[4] * avgY + renderMatrix.elements[8] * avgZ + renderMatrix.elements[12]
                    var clipY = renderMatrix.elements[1] * avgX + renderMatrix.elements[5] * avgY + renderMatrix.elements[9] * avgZ + renderMatrix.elements[13]
                    var clipZ = renderMatrix.elements[2] * avgX + renderMatrix.elements[6] * avgY + renderMatrix.elements[10] * avgZ + renderMatrix.elements[14]
                    var clipW = renderMatrix.elements[3] * avgX + renderMatrix.elements[7] * avgY + renderMatrix.elements[11] * avgZ + renderMatrix.elements[15]
                    clipZ += modelMatrix.elements[2] * surface.offsetX + modelMatrix.elements[6] * surface.offsetY
                    clipW += modelMatrix.elements[3] * surface.offsetX + modelMatrix.elements[7] * surface.offsetY
                    if (clipZ > -clipW) {
                        clipX += modelMatrix.elements[0] * surface.offsetX + modelMatrix.elements[4] * surface.offsetY
                        clipY += modelMatrix.elements[1] * surface.offsetX + modelMatrix.elements[5] * surface.offsetY
                        val screenX = this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * clipX / clipW
                        val screenY = this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * clipY / clipW
                        val adjustedSizeX = surface.scaleX * effect.size2d
                        val adjustedSizeY = surface.scaleY * effect.size3d
                        val adjustX = clipX + modelMatrix.elements[0] * adjustedSizeX + modelMatrix.elements[4] * adjustedSizeY
                        val adjustY = clipY + modelMatrix.elements[1] * adjustedSizeX + modelMatrix.elements[5] * adjustedSizeY
                        val adjustClipW = clipW + modelMatrix.elements[3] * adjustedSizeX + modelMatrix.elements[7] * adjustedSizeY
                        val edgeScreenX = this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * adjustX / adjustClipW
                        val edgeScreenY = this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * adjustY / adjustClipW
                        surface.x = screenX.toInt()
                        surface.y = screenY.toInt()
                        surface.z = this.toolkit.depthRangeNear + this.toolkit.depthRangeFar * (clipZ - modelMatrix.elements[10] * effect.distance) / clipW
                        surface.size2d = (if (edgeScreenX < screenX) screenX - edgeScreenX else edgeScreenX - screenX).toInt()
                        surface.size3d = (if (edgeScreenY < screenY) screenY - edgeScreenY else edgeScreenY - screenY).toInt()
                    } else {
                        surface.size3d = 0
                        surface.size2d = 0
                    }
                }
            }
            if (renderedEntity != null) {
                var behindNearPlane = false
                var visible = true
                val boundsCenterX = this.minX + this.maxX shr 1
                val boundsCenterZ = this.minZ + this.maxZ shr 1
                var boundsY = this.minY
                var minYClipX = renderMatrix.elements[0] * boundsCenterX + renderMatrix.elements[4] * boundsY + renderMatrix.elements[8] * boundsCenterZ + renderMatrix.elements[12]
                var minYClipY = renderMatrix.elements[1] * boundsCenterX + renderMatrix.elements[5] * boundsY + renderMatrix.elements[9] * boundsCenterZ + renderMatrix.elements[13]
                var minYClipZ = renderMatrix.elements[2] * boundsCenterX + renderMatrix.elements[6] * boundsY + renderMatrix.elements[10] * boundsCenterZ + renderMatrix.elements[14]
                var minYClipW = renderMatrix.elements[3] * boundsCenterX + renderMatrix.elements[7] * boundsY + renderMatrix.elements[11] * boundsCenterZ + renderMatrix.elements[15]
                if (minYClipZ >= -minYClipW) {
                    renderedEntity.left = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * minYClipX / minYClipW).toInt()
                    renderedEntity.bottom = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * minYClipY / minYClipW).toInt()
                } else {
                    behindNearPlane = true
                }
                boundsY = this.maxY
                val maxYClipX = renderMatrix.elements[0] * boundsCenterX + renderMatrix.elements[4] * boundsY + renderMatrix.elements[8] * boundsCenterZ + renderMatrix.elements[12]
                val maxYClipY = renderMatrix.elements[1] * boundsCenterX + renderMatrix.elements[5] * boundsY + renderMatrix.elements[9] * boundsCenterZ + renderMatrix.elements[13]
                val maxYClipZ = renderMatrix.elements[2] * boundsCenterX + renderMatrix.elements[6] * boundsY + renderMatrix.elements[10] * boundsCenterZ + renderMatrix.elements[14]
                val maxYClipW = renderMatrix.elements[3] * boundsCenterX + renderMatrix.elements[7] * boundsY + renderMatrix.elements[11] * boundsCenterZ + renderMatrix.elements[15]
                if (maxYClipZ >= -maxYClipW) {
                    renderedEntity.right = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * maxYClipX / maxYClipW).toInt()
                    renderedEntity.top = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * maxYClipY / maxYClipW).toInt()
                } else {
                    behindNearPlane = true
                }
                if (behindNearPlane) {
                    if (minYClipZ < -minYClipW && maxYClipZ < -maxYClipW) {
                        visible = false
                    } else if (minYClipZ < -minYClipW) {
                        val clipT = (minYClipZ + minYClipW) / (maxYClipZ + maxYClipW) - 1.0f
                        val clipX = minYClipX + clipT * (maxYClipX - minYClipX)
                        val clipY = minYClipY + clipT * (maxYClipY - minYClipY)
                        val clipW = minYClipW + clipT * (maxYClipW - minYClipW)
                        renderedEntity.left = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * clipX / clipW).toInt()
                        renderedEntity.bottom = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * clipY / clipW).toInt()
                    } else if (maxYClipZ < -maxYClipW) {
                        val clipT = (maxYClipZ + maxYClipW) / (minYClipZ + minYClipW) - 1.0f
                        val clipX = maxYClipX + clipT * (minYClipX - maxYClipX)
                        val clipY = maxYClipY + clipT * (minYClipY - maxYClipY)
                        val clipW = maxYClipW + clipT * (minYClipW - maxYClipW)
                        renderedEntity.right = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * clipX / clipW).toInt()
                        renderedEntity.top = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * clipY / clipW).toInt()
                    }
                }
                if (visible) {
                    if (minYClipZ / minYClipW > maxYClipZ / maxYClipW) {
                        val edgeClipX = minYClipX + modelMatrix.elements[0] * this.size2d + modelMatrix.elements[12]
                        val edgeClipW = minYClipW + modelMatrix.elements[3] * this.size2d + modelMatrix.elements[15]
                        renderedEntity.size = (this.toolkit.viewportOriginX - renderedEntity.left + this.toolkit.viewportHalfWidth * edgeClipX / edgeClipW).toInt()
                    } else {
                        val edgeClipX = maxYClipX + modelMatrix.elements[0] * this.size2d + modelMatrix.elements[12]
                        val edgeClipW = maxYClipW + modelMatrix.elements[3] * this.size2d + modelMatrix.elements[15]
                        renderedEntity.size = (this.toolkit.viewportOriginX - renderedEntity.right + this.toolkit.viewportHalfWidth * edgeClipX / edgeClipW).toInt()
                    }
                    renderedEntity.positionCalculated = true
                }
            }
            appearanceUpdateThreaded(true)
            this.rasterizer!!.smoothEdges = (rasterizerFlags and ModelFunctionMaskGL.SMOOTH_EDGES) == 0
            this.rasterizer!!.blending = false
            try {
                draw(this.toolkit.colourBuffer != null, this.toolkit.depthBuffer != null, (rasterizerFlags and ModelFunctionMaskGL.TRANSPARENT) != 0, offscreen, this.threadResource!!.fogEnabled && fogDepth > this.threadResource!!.fogStartDistance || this.threadResource!!.hasWaterUpdate)
            } catch (ex: Exception) {
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
        val otherVerticesX = safeModel.verticesX
        val otherMaxDepth = safeModel.maxDepth
        for (verticeIndex in 0 until this.maxDepth) {
            val vertexNormal = this.vertexNormals!![verticeIndex]!!
            if (vertexNormal.magnitude != 0) {
                val vertY = this.verticesY!![verticeIndex] - y
                if (vertY >= safeModel.minY && vertY <= safeModel.maxY) {
                    val vertX = this.verticesX!![verticeIndex] - x
                    if (vertX >= safeModel.minX && vertX <= safeModel.maxX) {
                        val vertZ = this.verticesZ!![verticeIndex] - z
                        if (vertZ >= safeModel.minZ && vertZ <= safeModel.maxZ) {
                            for (otherVerticeIndex in 0 until otherMaxDepth) {
                                val otherVertexNormal = safeModel.vertexNormals!![otherVerticeIndex]!!
                                if (vertX == otherVerticesX!![otherVerticeIndex] && vertZ == safeModel.verticesZ!![otherVerticeIndex] && vertY == safeModel.verticesY!![otherVerticeIndex] && otherVertexNormal.magnitude != 0) {
                                    if (this.vertexNormalOffsets == null) {
                                        this.vertexNormalOffsets = arrayOfNulls(this.maxDepth)
                                    }
                                    if (safeModel.vertexNormalOffsets == null) {
                                        safeModel.vertexNormalOffsets = arrayOfNulls(otherMaxDepth)
                                    }
                                    var normalOffset = this.vertexNormalOffsets!![verticeIndex]
                                    if (normalOffset == null) {
                                        normalOffset = VertexNormal(vertexNormal)
                                        this.vertexNormalOffsets!![verticeIndex] = normalOffset
                                    }
                                    var otherNormalOffset = safeModel.vertexNormalOffsets!![otherVerticeIndex]
                                    if (otherNormalOffset == null) {
                                        otherNormalOffset = VertexNormal(otherVertexNormal)
                                        safeModel.vertexNormalOffsets!![otherVerticeIndex] = otherNormalOffset
                                    }
                                    normalOffset.x += otherVertexNormal.x
                                    normalOffset.y += otherVertexNormal.y
                                    normalOffset.z += otherVertexNormal.z
                                    normalOffset.magnitude += otherVertexNormal.magnitude
                                    otherNormalOffset.x += vertexNormal.x
                                    otherNormalOffset.y += vertexNormal.y
                                    otherNormalOffset.z += vertexNormal.z
                                    otherNormalOffset.magnitude += vertexNormal.magnitude
                                    cycleCount++
                                    this.renderCycleByVertex!![verticeIndex] = renderCycle
                                    this.renderCycleByDepth!![otherVerticeIndex] = renderCycle
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
        } else if ((this.functionMask and ModelFunctionMask.ALTERS_ALPHA) == 0 && this.sortedFaceIndices != null) {
            for (faceIndex in 0 until this.numFaces) {
                val face = this.sortedFaceIndices!![faceIndex]
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
                for (layer in 0 until 12) {
                    for (faceIndex in 0 until this.numFaces) {
                        if (layer == this.facesLayer!![faceIndex].toInt() && (hasAlpha(faceIndex) || hasBillboardOnFace(faceIndex))) {
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
            val modelMatrix = this.toolkit.modelMatrix!!
            val clipZScale = modelMatrix.elements[10]
            val clipZOffset = modelMatrix.elements[14]
            val clipWScale = modelMatrix.elements[11]
            val clipWOffset = modelMatrix.elements[15]
            val depthDeterminant = clipZOffset * clipWScale - clipZScale * clipWOffset
            val depthA = (this.projectedVerticesZ!![a] - this.threadResource!!.cachedDepthRangeNear) * this.threadResource!!.inverseDepthRangeFar
            val depthB = (this.projectedVerticesZ!![b] - this.threadResource!!.cachedDepthRangeNear) * this.threadResource!!.inverseDepthRangeFar
            val depthC = (this.projectedVerticesZ!![c] - this.threadResource!!.cachedDepthRangeNear) * this.threadResource!!.inverseDepthRangeFar
            val viewDistanceA = depthA * depthDeterminant / (depthA * clipWScale - clipZScale)
            val viewDistanceB = depthB * depthDeterminant / (depthB * clipWScale - clipZScale)
            val viewDistanceC = depthC * depthDeterminant / (depthC * clipWScale - clipZScale)
            var mixAlphaA = (viewDistanceA - this.threadResource!!.fogStartDistance) / this.threadResource!!.fogRange
            if (mixAlphaA > 1.0f) {
                mixAlphaA = 1.0f
            } else if (mixAlphaA < 0.0f) {
                mixAlphaA = 0.0f
            }
            var mixAlphaB = (viewDistanceB - this.threadResource!!.fogStartDistance) / this.threadResource!!.fogRange
            if (mixAlphaB > 1.0f) {
                mixAlphaB = 1.0f
            } else if (mixAlphaB < 0.0f) {
                mixAlphaB = 0.0f
            }
            var mixAlphaC = (viewDistanceC - this.threadResource!!.fogStartDistance) / this.threadResource!!.fogRange
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
                        var alphaBits = 0xffffff.inv()
                        if (this.facesAlpha != null) {
                            alphaBits = 255 - (this.facesAlpha!![faceIndex].toInt() and 0xff) shl 24
                        }
                        if (this.facesColourC!![faceIndex] == -1) {
                            val faceAColor = alphaBits or (this.facesColourA!![faceIndex] and 0xffffff)
                            this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], faceAColor, faceAColor, faceAColor, this.threadResource!!.fogColor, mixAlphaA * 255.0f, mixAlphaB * 255.0f, mixAlphaC * 255.0f, this.facesMaterial!![faceIndex].toInt())
                        } else {
                            this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], alphaBits or (this.facesColourA!![faceIndex] and 0xffffff), alphaBits or (this.facesColourB!![faceIndex] and 0xffffff), alphaBits or (this.facesColourC!![faceIndex] and 0xffffff), this.threadResource!!.fogColor, mixAlphaA * 255.0f, mixAlphaB * 255.0f, mixAlphaC * 255.0f, this.facesMaterial!![faceIndex].toInt())
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
                var alphaBits = 0xffffff.inv()
                if (this.facesAlpha != null) {
                    alphaBits = 255 - (this.facesAlpha!![faceIndex].toInt() and 0xff) shl 24
                }
                if (this.facesColourC!![faceIndex] == -1) {
                    val faceAColor = alphaBits or (this.facesColourA!![faceIndex] and 0xffffff)
                    this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], faceAColor, faceAColor, faceAColor, this.threadResource!!.fogColor, waterAlphaMultiplierA * 255.0f, waterAlphaMultiplierB * 255.0f, waterAlphaMultiplierC * 255.0f, this.facesMaterial!![faceIndex].toInt())
                } else {
                    this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a], this.projectedVerticesY!![b], this.projectedVerticesY!![c], this.projectedVerticesX!![a], this.projectedVerticesX!![b], this.projectedVerticesX!![c], this.projectedVerticesZ!![a], this.projectedVerticesZ!![b], this.projectedVerticesZ!![c], this.projectedVerticesZ2!![a], this.projectedVerticesZ2!![b], this.projectedVerticesZ2!![c], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], alphaBits or (this.facesColourA!![faceIndex] and 0xffffff), alphaBits or (this.facesColourB!![faceIndex] and 0xffffff), alphaBits or (this.facesColourC!![faceIndex] and 0xffffff), this.threadResource!!.fogColor, waterAlphaMultiplierA * 255.0f, waterAlphaMultiplierB * 255.0f, waterAlphaMultiplierC * 255.0f, this.facesMaterial!![faceIndex].toInt())
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
                val clipX = renderMatrix.elements[0] * x + renderMatrix.elements[4] * y + renderMatrix.elements[8] * z + renderMatrix.elements[12]
                val clipY = renderMatrix.elements[1] * x + renderMatrix.elements[5] * y + renderMatrix.elements[9] * z + renderMatrix.elements[13]
                val screenPosX = (this.toolkit.viewportOriginX + this.toolkit.viewportHalfWidth * clipX / depthB).toInt()
                val screenPosY = (this.toolkit.viewportOriginY + this.toolkit.viewportHalfHeight * clipY / depthB).toInt()
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

    fun isHovering(mouseX: Int, mouseY: Int, yA: Float, yB: Float, yC: Float, xA: Float, xB: Float, xC: Float): Boolean {
        return (mouseY >= yA || mouseY >= yB || mouseY >= yC) && (mouseY <= yA || mouseY <= yB || mouseY <= yC) && (mouseX >= xA || mouseX >= xB || mouseX >= xC) && (mouseX <= xA || mouseX <= xB || mouseX <= xC)
    }

    override fun awaitLock() {
        if (this.toolkit.executingThreadCount > 1) {
            synchronized(this) {
                while (super.isLocked) {
                    try {
                        (this as Object).wait()
                    } catch (ie: InterruptedException) {
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
            for (billboardIndex in 0 until this.numBillboards) {
                val effect = this.billboardEffects!![billboardIndex]!!
                val surface = this.billboardSurfaces!![billboardIndex]!!
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
            for (billboardIndex in 0 until this.numBillboards) {
                val effect = this.billboardEffects!![billboardIndex]!!
                val surface = this.billboardSurfaces!![billboardIndex]!!
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
            var alphaBits = 0xffffff.inv()
            if (this.facesAlpha != null) {
                alphaBits = 255 - (this.facesAlpha!![faceIndex].toInt() and 0xff) shl 24
            }
            if (this.facesColourC!![faceIndex] == -1) {
                val faceAColor = alphaBits or (this.facesColourA!![faceIndex] and 0xffffff)
                this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a.toInt()], this.projectedVerticesY!![b.toInt()], this.projectedVerticesY!![c.toInt()], this.projectedVerticesX!![a.toInt()], this.projectedVerticesX!![b.toInt()], this.projectedVerticesX!![c.toInt()], this.projectedVerticesZ!![a.toInt()], this.projectedVerticesZ!![b.toInt()], this.projectedVerticesZ!![c.toInt()], this.projectedVerticesZ2!![a.toInt()], this.projectedVerticesZ2!![b.toInt()], this.projectedVerticesZ2!![c.toInt()], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], faceAColor, faceAColor, faceAColor, this.threadResource!!.fogColor, 0.0f, 0.0f, 0.0f, this.facesMaterial!![faceIndex].toInt())
            } else {
                this.rasterizer!!.drawMaterializedTriangle(colorWrite, depthWrite, transparent, this.projectedVerticesY!![a.toInt()], this.projectedVerticesY!![b.toInt()], this.projectedVerticesY!![c.toInt()], this.projectedVerticesX!![a.toInt()], this.projectedVerticesX!![b.toInt()], this.projectedVerticesX!![c.toInt()], this.projectedVerticesZ!![a.toInt()], this.projectedVerticesZ!![b.toInt()], this.projectedVerticesZ!![c.toInt()], this.projectedVerticesZ2!![a.toInt()], this.projectedVerticesZ2!![b.toInt()], this.projectedVerticesZ2!![c.toInt()], this.uMap!![faceIndex]!![0], this.uMap!![faceIndex]!![1], this.uMap!![faceIndex]!![2], this.vMap!![faceIndex]!![0], this.vMap!![faceIndex]!![1], this.vMap!![faceIndex]!![2], alphaBits or (this.facesColourA!![faceIndex] and 0xffffff), alphaBits or (this.facesColourB!![faceIndex] and 0xffffff), alphaBits or (this.facesColourC!![faceIndex] and 0xffffff), this.threadResource!!.fogColor, 0.0f, 0.0f, 0.0f, this.facesMaterial!![faceIndex].toInt())
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
            for (verticeIndex in 0 until this.maxDepth) {
                var tmpX = this.verticesX!![verticeIndex]
                this.verticesX!![verticeIndex] = this.verticesZ!![verticeIndex]
                this.verticesZ!![verticeIndex] = -tmpX
                if (this.vertexNormals!![verticeIndex] != null) {
                    tmpX = this.vertexNormals!![verticeIndex]!!.x
                    this.vertexNormals!![verticeIndex]!!.x = this.vertexNormals!![verticeIndex]!!.z
                    this.vertexNormals!![verticeIndex]!!.z = -tmpX
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
            for (verticeIndex in this.maxDepth until this.numVertices) {
                val tmpX = this.verticesX!![verticeIndex]
                this.verticesX!![verticeIndex] = this.verticesZ!![verticeIndex]
                this.verticesZ!![verticeIndex] = -tmpX
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

    fun mixLightness(hsl: Int, lightness: Int): Int {
        var result = (hsl and 0x7f) * lightness shr 7
        if (result < 2) {
            result = 2
        } else if (result > 126) {
            result = 126
        }
        return result + (hsl and 0xff80)
    }

    override fun offsetSubmesh(matrix: Matrix43, submeshFlags: Int, updateIdentityMatrix: Boolean) {
        if (this.verticesSubmeshes != null) {
            val resource = this.toolkit.getThreadResource(Thread.currentThread())!!
            val submeshMatrix43 = resource.submeshMatrix43
            submeshMatrix43.copyFrom(matrix)
            if (updateIdentityMatrix) {
                submeshMatrix43.updateIdentitiyMatrix()
            }
            val submeshMatrix = resource.particleMatrix
            submeshMatrix.fromMatrix(submeshMatrix43)
            val resultBuffer = FloatArray(3)
            for (verticeIndex in 0 until this.maxDepth) {
                if ((submeshFlags and this.verticesSubmeshes!![verticeIndex].toInt()) != 0) {
                    submeshMatrix.scale(this.verticesX!![verticeIndex].toFloat(), this.verticesY!![verticeIndex].toFloat(), this.verticesZ!![verticeIndex].toFloat(), resultBuffer)
                    this.verticesX!![verticeIndex] = resultBuffer[0].toInt()
                    this.verticesY!![verticeIndex] = resultBuffer[1].toInt()
                    this.verticesZ!![verticeIndex] = resultBuffer[2].toInt()
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
                                val height1 = currentHeights[tileX][tileZ] * (currLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] shr currLevel.tileScale
                                val height2 = (currLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] shr currLevel.tileScale
                                val interpolatedHeight = height1 * (currLevel.tileUnits - remainderZ) + height2 * remainderZ shr currLevel.tileScale
                                this.verticesY!![v] = interpolatedHeight + this.verticesY!![v] - averageHeight
                            }
                        }
                    }
                    2 -> {
                        if (this.minY.toInt() == 0) {
                            return
                        }
                        val tileMask = currLevel.tileUnits - 1
                        for (v in 0 until this.maxDepth) {
                            val heightRatio = (this.verticesY!![v] shl 16) / this.minY
                            if (heightRatio < contourModifier) {
                                val x = sceneX + this.verticesX!![v]
                                val z = sceneY + this.verticesZ!![v]
                                val remainderX = x and tileMask
                                val remainderZ = z and tileMask
                                val tileX = x shr currLevel.tileScale
                                val tileZ = z shr currLevel.tileScale
                                val height1 = currentHeights[tileX][tileZ] * (currLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] shr currLevel.tileScale
                                val height2 = (currLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] shr currLevel.tileScale
                                val interpolatedHeight = height1 * (currLevel.tileUnits - remainderZ) + height2 * remainderZ shr currLevel.tileScale
                                this.verticesY!![v] += (interpolatedHeight - averageHeight) * (contourModifier - heightRatio) / contourModifier
                            }
                        }
                        for (v in this.maxDepth until this.numVertices) {
                            val heightRatio = (this.verticesY!![v] shl 16) / this.minY
                            if (heightRatio < contourModifier) {
                                val x = sceneX + this.verticesX!![v]
                                val z = sceneY + this.verticesZ!![v]
                                val remainderX = x and tileMask
                                val remainderZ = z and tileMask
                                val tileX = x shr currLevel.tileScale
                                val tileZ = z shr currLevel.tileScale
                                if (tileX >= 0 && tileX < currLevel.width - 1 && tileZ >= 0 && tileZ < currLevel.length - 1) {
                                    val height1 = currentHeights[tileX][tileZ] * (currLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] shr currLevel.tileScale
                                    val height2 = (currLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] shr currLevel.tileScale
                                    val interpolatedHeight = height1 * (currLevel.tileUnits - remainderZ) + height2 * remainderZ shr currLevel.tileScale
                                    this.verticesY!![v] += (interpolatedHeight - averageHeight) * (contourModifier - heightRatio) / contourModifier
                                }
                            }
                        }
                    }
                    3 -> {
                        val width = (contourModifier and 0xff) * 4
                        val length = (contourModifier shr 8 and 0xff) * 4
                        val xan = (contourModifier shr 16 and 0xff) shl 6
                        val zan = (contourModifier shr 24 and 0xff) shl 6
                        if (sceneX - (width shr 1) < 0 || sceneX + currLevel.tileUnits + (width shr 1) >= currLevel.width shl currLevel.tileScale || sceneY - (length shr 1) < 0 || currLevel.tileUnits + (length shr 1) + sceneY >= currLevel.length shl currLevel.tileScale) {
                            return
                        }
                        hillChange(currLevel, sceneX, averageHeight, sceneY, width, length, xan, zan)
                    }
                    4 -> {
                        val tileMask = nextLevel!!.tileUnits - 1
                        val sizeY = this.maxY - this.minY
                        for (v in 0 until this.maxDepth) {
                            val x = sceneX + this.verticesX!![v]
                            val z = sceneY + this.verticesZ!![v]
                            val remainderX = x and tileMask
                            val remainderZ = z and tileMask
                            val tileX = x shr nextLevel.tileScale
                            val tileZ = z shr nextLevel.tileScale
                            val height1 = remainderX * nextHeights!![tileX + 1][tileZ] + nextHeights[tileX][tileZ] * (nextLevel.tileUnits - remainderX) shr nextLevel.tileScale
                            val height2 = (nextLevel.tileUnits - remainderX) * nextHeights[tileX][tileZ + 1] + remainderX * nextHeights[tileX + 1][tileZ + 1] shr nextLevel.tileScale
                            val nextAvgHeight = (nextLevel.tileUnits - remainderZ) * height1 + height2 * remainderZ shr nextLevel.tileScale
                            this.verticesY!![v] += sizeY + (nextAvgHeight - averageHeight)
                        }
                        for (v in this.maxDepth until this.numVertices) {
                            val x = sceneX + this.verticesX!![v]
                            val z = sceneY + this.verticesZ!![v]
                            val remainderX = x and tileMask
                            val remainderZ = z and tileMask
                            val tileX = x shr nextLevel.tileScale
                            val tileZ = z shr nextLevel.tileScale
                            if (tileX >= 0 && tileX < nextLevel.width - 1 && tileZ >= 0 && tileZ < nextLevel.length - 1) {
                                val height1 = remainderX * nextHeights!![tileX + 1][tileZ] + nextHeights[tileX][tileZ] * (nextLevel.tileUnits - remainderX) shr nextLevel.tileScale
                                val height2 = (nextLevel.tileUnits - remainderX) * nextHeights[tileX][tileZ + 1] + remainderX * nextHeights[tileX + 1][tileZ + 1] shr nextLevel.tileScale
                                val nextAvgHeight = (nextLevel.tileUnits - remainderZ) * height1 + height2 * remainderZ shr nextLevel.tileScale
                                this.verticesY!![v] += sizeY + (nextAvgHeight - averageHeight)
                            }
                        }
                    }
                    5 -> {
                        val tileMask = nextLevel!!.tileUnits - 1
                        val sizeY = this.maxY - this.minY
                        for (v in 0 until this.maxDepth) {
                            val x = sceneX + this.verticesX!![v]
                            val z = sceneY + this.verticesZ!![v]
                            val remainderX = x and tileMask
                            val remainderZ = z and tileMask
                            val tileX = x shr currLevel.tileScale
                            val tileZ = z shr currLevel.tileScale
                            var height1 = currentHeights[tileX][tileZ] * (currLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] shr currLevel.tileScale
                            var height2 = (currLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] shr currLevel.tileScale
                            val currentAvgHeight = height1 * (currLevel.tileUnits - remainderZ) + height2 * remainderZ shr currLevel.tileScale
                            height1 = remainderX * nextHeights!![tileX + 1][tileZ] + nextHeights[tileX][tileZ] * (nextLevel.tileUnits - remainderX) shr nextLevel.tileScale
                            height2 = (nextLevel.tileUnits - remainderX) * nextHeights[tileX][tileZ + 1] + remainderX * nextHeights[tileX + 1][tileZ + 1] shr nextLevel.tileScale
                            val nextAvgHeight = (nextLevel.tileUnits - remainderZ) * height1 + height2 * remainderZ shr nextLevel.tileScale
                            val delta = currentAvgHeight - nextAvgHeight - contourModifier
                            this.verticesY!![v] = (delta * ((this.verticesY!![v] shl 8) / sizeY) shr 8) - (averageHeight - currentAvgHeight)
                        }
                        for (v in this.maxDepth until this.numVertices) {
                            val x = sceneX + this.verticesX!![v]
                            val z = sceneY + this.verticesZ!![v]
                            val remainderX = x and tileMask
                            val remainderZ = z and tileMask
                            val tileX = x shr currLevel.tileScale
                            val tileZ = z shr currLevel.tileScale
                            if (tileX >= 0 && tileX < currLevel.width - 1 && tileX < nextLevel.width - 1 && tileZ >= 0 && tileZ < currLevel.length - 1 && tileZ < nextLevel.length - 1) {
                                var height1 = currentHeights[tileX][tileZ] * (currLevel.tileUnits - remainderX) + remainderX * currentHeights[tileX + 1][tileZ] shr currLevel.tileScale
                                var height2 = (currLevel.tileUnits - remainderX) * currentHeights[tileX][tileZ + 1] + remainderX * currentHeights[tileX + 1][tileZ + 1] shr currLevel.tileScale
                                val currentAvgHeight = height1 * (currLevel.tileUnits - remainderZ) + height2 * remainderZ shr currLevel.tileScale
                                height1 = remainderX * nextHeights!![tileX + 1][tileZ] + nextHeights[tileX][tileZ] * (nextLevel.tileUnits - remainderX) shr nextLevel.tileScale
                                height2 = (nextLevel.tileUnits - remainderX) * nextHeights[tileX][tileZ + 1] + remainderX * nextHeights[tileX + 1][tileZ + 1] shr nextLevel.tileScale
                                val nextAvgHeight = (nextLevel.tileUnits - remainderZ) * height1 + height2 * remainderZ shr nextLevel.tileScale
                                val delta = currentAvgHeight - nextAvgHeight - contourModifier
                                this.verticesY!![v] = (delta * ((this.verticesY!![v] shl 8) / sizeY) shr 8) - (averageHeight - currentAvgHeight)
                            }
                        }
                    }
                }
                this.boundsUpdated = false
            }
        }
    }

    fun translateToScreen(colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean, faceIndex: Int, offscreen: Boolean, allowReduce: Boolean) {
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
                    if (allowReduce) {
                        val billboardIndex = this.billboardOnFace!![faceIndex]
                        if (billboardIndex == -1 || !this.billboardEffects!![billboardIndex]!!.hasUid) {
                            reduce(colorWrite, depthWrite, transparent, faceIndex)
                        }
                    } else {
                        val billboardIndex = this.billboardOnFace!![faceIndex]
                        if (billboardIndex != -1) {
                            val effect = this.billboardEffects!![billboardIndex]!!
                            val surface = this.billboardSurfaces!![billboardIndex]!!
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
            for (faceIndex in this.facesMaterial!!.indices) {
                if (this.facesMaterial!![faceIndex].toInt() != -1 && !this.toolkit.isMaterialCached(this.facesMaterial!![faceIndex].toInt())) {
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
