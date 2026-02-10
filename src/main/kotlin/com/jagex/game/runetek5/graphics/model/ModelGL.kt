package com.jagex.game.runetek5.graphics.model

import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.graphics.model.billboard.Billboard
import com.jagex.game.runetek5.graphics.model.billboard.BillboardSurfaceGL
import com.jagex.game.runetek5.graphics.model.billboard.BillboardEffectGL
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList
import com.jagex.game.runetek5.graphics.model.buffer.ModelElementsBufferGL
import com.jagex.game.runetek5.graphics.model.normal.FaceNormalGL
import com.jagex.game.runetek5.graphics.model.normal.ModelNormalsGL
import com.jagex.game.runetek5.graphics.model.normal.VertexNormalGL
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.graphics.shadow.ShadowGL
import com.jagex.game.runetek5.graphics.texture.TextureTransformationMatrix
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.utils.ArrayUtils
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils
import jaclib.memory.Stream
import jaggl.OpenGL
import kotlin.math.sqrt

class ModelGL : Model {

    companion object {
        @JvmField
        val uvTypes = FloatArray(2)

        @JvmField
        val matrixX = IntArray(8)

        @JvmField
        val matrixY = IntArray(8)

        @JvmField
        val matrixZ = IntArray(8)

        @JvmField
        var normalsUid: LongArray? = null

        @JvmField
        var originX: Int = 0

        @JvmField
        var originY: Int = 0

        @JvmField
        var originZ: Int = 0

        @JvmField
        var withinOrigin: Boolean = false

        @JvmStatic
        fun clone(src: FloatArray, size: Int): FloatArray {
            val dst = FloatArray(size)
            System.arraycopy(src, 0, dst, 0, size)
            return dst
        }

        @JvmStatic
        fun clone(src: ShortArray, size: Int): ShortArray {
            val dst = ShortArray(size)
            System.arraycopy(src, 0, dst, 0, size)
            return dst
        }

        @JvmStatic
        fun clone(src: ByteArray, size: Int): ByteArray {
            val dst = ByteArray(size)
            System.arraycopy(src, 0, dst, 0, size)
            return dst
        }

        @JvmStatic
        fun method13538(i_0: Int, i_1: Int): Int {
            var i_11 = i_1
            i_11 = (i_0 and 0x7f) * i_11 shr 7
            if (i_11 < 2) {
                i_11 = 2
            } else if (i_11 > 126) {
                i_11 = 126
            }
            return (i_0 and 0xff80) + i_11
        }
    }

    @JvmField
    var boundsCalculated: Boolean = false

    @JvmField
    var hasAlphaTransition: Boolean = false

    @JvmField
    var textureAnimating: Boolean = false

    @JvmField
    var numVertices: Int = 0

    @JvmField
    var maxDepth: Int = 0

    @JvmField
    var numNormals: Int = 0

    @JvmField
    var numFaces: Int = 0

    @JvmField
    var anInt8302: Int = 0

    @JvmField
    var updateRequired: Boolean = true

    @JvmField
    val toolkit: OpenGlToolkit

    @JvmField
    var functionMask: Int = 0

    @JvmField
    var preferenceFlags: Int = 0

    @JvmField
    var vertexBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var textureCoordinatesBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var normalsBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var colorsBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var modelElementsBuffer: ModelElementsBufferGL? = null

    @JvmField
    var triangles: IntArray? = null

    @JvmField
    var verticesX: IntArray? = null

    @JvmField
    var verticesY: IntArray? = null

    @JvmField
    var verticesZ: IntArray? = null

    @JvmField
    var verticesSubmeshes: ShortArray? = null

    @JvmField
    var emissiveTriangles: Array<EmissiveTriangle>? = null

    @JvmField
    var effectiveVertices: Array<EffectiveVertex>? = null

    @JvmField
    var billboardCount: Int = 0

    @JvmField
    var billboardEffects: Array<BillboardEffectGL?>? = null

    @JvmField
    var billboardSurfaces: Array<BillboardSurfaceGL?>? = null

    @JvmField
    var normalsX: ShortArray? = null

    @JvmField
    var normalsY: ShortArray? = null

    @JvmField
    var normalsZ: ShortArray? = null

    @JvmField
    var magnitudes: ByteArray? = null

    @JvmField
    var textureU: FloatArray? = null

    @JvmField
    var textureV: FloatArray? = null

    @JvmField
    var facesColor: ShortArray? = null

    @JvmField
    var facesAlpha: ByteArray? = null

    @JvmField
    var facesA: ShortArray? = null

    @JvmField
    var facesB: ShortArray? = null

    @JvmField
    var facesC: ShortArray? = null

    @JvmField
    var facesMaterial: ShortArray? = null

    @JvmField
    var facesSubmeshes: ShortArray? = null

    @JvmField
    var ambient: Short = 0

    @JvmField
    var contrast: Short = 0

    @JvmField
    var aShortArray8288: ShortArray? = null

    @JvmField
    var anIntArray8289: IntArray? = null

    @JvmField
    var vertexGroups: Array<IntArray?>? = null

    @JvmField
    var billboardGroups: Array<IntArray?>? = null

    @JvmField
    var faceGroups: Array<IntArray?>? = null

    @JvmField
    var aClass1668259: ModelNormalsGL? = null

    @JvmField
    var renderState: Byte = 0

    @JvmField
    var size3d: Int = 0

    @JvmField
    var size2d: Int = 0

    @JvmField
    var minX: Short = 0

    @JvmField
    var maxX: Short = 0

    @JvmField
    var minY: Short = 0

    @JvmField
    var maxY: Short = 0

    @JvmField
    var minZ: Short = 0

    @JvmField
    var maxZ: Short = 0

    @JvmField
    var anInterface158277: ElementsBufferGL? = null

    @JvmField
    var anInterface148305: ArrayBufferGL? = null

    constructor(toolkit: OpenGlToolkit) {
        this.toolkit = toolkit
        this.vertexBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        this.textureCoordinatesBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 2, 0)
        this.normalsBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        this.colorsBuffer = OpenGlArrayDrawBuffer(null, GL_UNSIGNED_BYTE, 4, 0)
        this.modelElementsBuffer = ModelElementsBufferGL()
    }

    constructor(renderer: OpenGlToolkit, mesh: Mesh, rasterizerFlags: Int, i_4: Int, i_5: Int, otherFlags: Int) {
        this.toolkit = renderer
        this.functionMask = rasterizerFlags
        this.preferenceFlags = otherFlags

        if (ModelFunctionMaskGL.hasVertices(rasterizerFlags)) {
            this.vertexBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        }

        if (ModelFunctionMaskGL.hasTextureCoordinates(rasterizerFlags)) {
            this.textureCoordinatesBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 2, 0)
        }

        if (ModelFunctionMaskGL.hasNormals(rasterizerFlags, otherFlags)) {
            this.normalsBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        }

        if (ModelFunctionMaskGL.hasColors(rasterizerFlags)) {
            this.colorsBuffer = OpenGlArrayDrawBuffer(null, GL_UNSIGNED_BYTE, 4, 0)
        }

        if (ModelFunctionMaskGL.hasElementsBuffer(rasterizerFlags)) {
            this.modelElementsBuffer = ModelElementsBufferGL()
        }

        val materialProvider: MaterialProvider = renderer.materialProvider!!
        val faces = IntArray(mesh.numFaces)
        this.triangles = IntArray(mesh.maxDepth + 1)

        for (faceIndex in 0 until mesh.numFaces) {
            if (mesh.faceRenderType == null || mesh.faceRenderType!![faceIndex].toInt() != 2) {
                if (mesh.facesMaterial != null && mesh.facesMaterial!![faceIndex].toInt() != -1) {
                    val meshMaterial = materialProvider.getMaterial(mesh.facesMaterial!![faceIndex].toInt() and 0xffff)
                    if ((this.preferenceFlags and 0x40) == 0 || !meshMaterial.detailsOnly) {
                        if (meshMaterial.skipTriangles) {
                            continue
                        }
                    } else {
                        continue
                    }
                }

                faces[this.numFaces++] = faceIndex
                this.triangles!![mesh.facesA!![faceIndex].toInt()]++
                this.triangles!![mesh.facesB!![faceIndex].toInt()]++
                this.triangles!![mesh.facesC!![faceIndex].toInt()]++
            }
        }

        this.anInt8302 = this.numFaces
        val billboardUids = LongArray(this.numFaces)
        val hasTransitions = (this.functionMask and 0x100) != 0

        var currentFace: Int
        var billboardMainUid: Int
        var material: MaterialType
        var billboardMaterialId: Short
        var appliesOnFace: Int

        for (triangleIndex in 0 until this.numFaces) {
            currentFace = faces[triangleIndex]
            var billboardMaterial: MaterialType? = null
            billboardMainUid = 0
            var b_67: Byte = 0
            var effectId: Byte = 0
            var effectParam1: Byte = 0

            if (mesh.billboards != null) {
                var hasUid = false

                for (texturedFace in mesh.billboards!!.indices) {
                    val vertexNormal = mesh.billboards!![texturedFace]
                    if (currentFace == vertexNormal.appliesOnFace) {
                        val billboardType = BillboardTypeList.list(vertexNormal.billboardId)
                        if (billboardType.hasUid) {
                            hasUid = true
                        }

                        if (billboardType.materialId != -1) {
                            material = materialProvider.getMaterial(billboardType.materialId)
                            if (material.effectCombiner.toInt() == 2) {
                                this.hasAlphaTransition = true
                            }
                        }
                    }
                }

                if (hasUid) {
                    billboardUids[triangleIndex] = Long.MAX_VALUE
                    this.anInt8302--
                    continue
                }
            }

            billboardMaterialId = -1

            if (mesh.facesMaterial != null) {
                billboardMaterialId = mesh.facesMaterial!![currentFace]
                if (billboardMaterialId.toInt() != -1) {
                    billboardMaterial = materialProvider.getMaterial(billboardMaterialId.toInt() and 0xffff)
                    if ((this.preferenceFlags and 0x40) != 0 && billboardMaterial.detailsOnly) {
                        billboardMaterialId = -1
                        billboardMaterial = null
                    } else {
                        effectId = billboardMaterial.effectId
                        effectParam1 = billboardMaterial.effectParam1
                        if (billboardMaterial.textureSpeedU.toInt() != 0 || billboardMaterial.textureSpeedV.toInt() != 0) {
                            this.textureAnimating = true
                        }
                    }
                }
            }

            val alphaTransition = mesh.facesAlpha != null && mesh.facesAlpha!![currentFace].toInt() != 0 || billboardMaterial != null && billboardMaterial.effectCombiner.toInt() != 0
            if ((hasTransitions || alphaTransition) && mesh.facesLayer != null) {
                billboardMainUid += mesh.facesLayer!![currentFace].toInt() shl 17
            }

            if (alphaTransition) {
                billboardMainUid += 65536
            }

            billboardMainUid += (effectId.toInt() and 0xff) shl 8
            billboardMainUid += effectParam1.toInt() and 0xff
            appliesOnFace = b_67 + ((billboardMaterialId.toInt() and 0xffff) shl 16)
            appliesOnFace += triangleIndex and 0xffff
            billboardUids[triangleIndex] = (billboardMainUid.toLong() shl 32) + appliesOnFace
            this.hasAlphaTransition = this.hasAlphaTransition or alphaTransition
            this.textureAnimating = this.textureAnimating or (billboardMaterial != null && (billboardMaterial.textureSpeedU.toInt() != 0 || billboardMaterial.textureSpeedV.toInt() != 0))
        }

        Model.addBillboard(billboardUids, faces)
        this.numVertices = mesh.numVertices
        this.maxDepth = mesh.maxDepth
        this.verticesX = mesh.verticesX
        this.verticesY = mesh.verticesY
        this.verticesZ = mesh.verticesZ
        this.verticesSubmeshes = mesh.verticesSubmeshes
        val vertexNormals = arrayOfNulls<VertexNormalGL>(this.maxDepth)
        this.emissiveTriangles = mesh.particleEmitters
        this.effectiveVertices = mesh.particleEffectors

        var triangleIndex: Int

        if (mesh.billboards != null) {
            this.billboardCount = mesh.billboards!!.size
            this.billboardEffects = arrayOfNulls(this.billboardCount)
            this.billboardSurfaces = arrayOfNulls(this.billboardCount)

            currentFace = 0
            while (currentFace < this.billboardCount) {
                val billboard = mesh.billboards!![currentFace]
                val billboardType = BillboardTypeList.list(billboard.billboardId)
                appliesOnFace = -1

                triangleIndex = 0
                while (triangleIndex < this.numFaces) {
                    if (billboard.appliesOnFace == faces[triangleIndex]) {
                        appliesOnFace = triangleIndex
                        break
                    }
                    triangleIndex++
                }

                if (appliesOnFace == -1) {
                    throw RuntimeException()
                }

                triangleIndex = ColorUtil.hslToRgb!![mesh.facesColor!![billboard.appliesOnFace].toInt() and 0xffff] and 0xffffff
                triangleIndex = triangleIndex or ((255 - (if (mesh.facesAlpha != null) mesh.facesAlpha!![billboard.appliesOnFace].toInt() else 0)) shl 24)
                this.billboardEffects!![currentFace] = BillboardEffectGL(
                    appliesOnFace,
                    mesh.facesA!![billboard.appliesOnFace].toInt(),
                    mesh.facesB!![billboard.appliesOnFace].toInt(),
                    mesh.facesC!![billboard.appliesOnFace].toInt(),
                    billboardType.size2d,
                    billboardType.size3d,
                    billboardType.materialId,
                    billboardType.shape,
                    billboardType.stationary,
                    billboard.distance
                )
                this.billboardSurfaces!![currentFace] = BillboardSurfaceGL(triangleIndex)
                currentFace++
            }
        }

        currentFace = this.numFaces * 3
        this.normalsX = ShortArray(currentFace)
        this.normalsY = ShortArray(currentFace)
        this.normalsZ = ShortArray(currentFace)
        this.magnitudes = ByteArray(currentFace)
        this.textureU = FloatArray(currentFace)
        this.textureV = FloatArray(currentFace)
        this.facesColor = ShortArray(this.numFaces)
        this.facesAlpha = ByteArray(this.numFaces)
        this.facesA = ShortArray(this.numFaces)
        this.facesB = ShortArray(this.numFaces)
        this.facesC = ShortArray(this.numFaces)
        this.facesMaterial = ShortArray(this.numFaces)

        if (mesh.facesSubmeshes != null) {
            this.facesSubmeshes = ShortArray(this.numFaces)
        }

        this.ambient = i_4.toShort()
        this.contrast = i_5.toShort()
        this.aShortArray8288 = ShortArray(currentFace)
        normalsUid = LongArray(currentFace)
        var i_13 = 0

        billboardMainUid = 0
        while (billboardMainUid < mesh.maxDepth) {
            appliesOnFace = this.triangles!![billboardMainUid]
            this.triangles!![billboardMainUid] = i_13
            i_13 += appliesOnFace
            vertexNormals[billboardMainUid] = VertexNormalGL()
            billboardMainUid++
        }

        this.triangles!![mesh.maxDepth] = i_13
        val transformMatrix = generateTextureMatrix(mesh, faces, this.numFaces)
        val faceNormals = arrayOfNulls<FaceNormalGL>(mesh.numFaces)

        var alphaAmt: Int
        var i_21: Int
        var lastMaterial: Short

        for (faceTriangleIndex in 0 until mesh.numFaces) {
            lastMaterial = mesh.facesA!![faceTriangleIndex]
            billboardMaterialId = mesh.facesB!![faceTriangleIndex]
            val materialId = mesh.facesC!![faceTriangleIndex]
            alphaAmt = this.verticesX!![billboardMaterialId.toInt()] - this.verticesX!![lastMaterial.toInt()]
            i_21 = this.verticesY!![billboardMaterialId.toInt()] - this.verticesY!![lastMaterial.toInt()]
            var i_22 = this.verticesZ!![billboardMaterialId.toInt()] - this.verticesZ!![lastMaterial.toInt()]
            var i_23 = this.verticesX!![materialId.toInt()] - this.verticesX!![lastMaterial.toInt()]
            var i_24 = this.verticesY!![materialId.toInt()] - this.verticesY!![lastMaterial.toInt()]
            var i_25 = this.verticesZ!![materialId.toInt()] - this.verticesZ!![lastMaterial.toInt()]
            var x = i_21 * i_25 - i_24 * i_22
            var y = i_22 * i_23 - i_25 * alphaAmt

            var z = alphaAmt * i_24 - i_23 * i_21
            while (x > 8192 || y > 8192 || z > 8192 || x < -8192 || y < -8192 || z < -8192) {
                x = x shr 1
                y = y shr 1
                z = z shr 1
            }

            var i_29 = sqrt((x * x + y * y + z * z).toDouble()).toInt()
            if (i_29 <= 0) {
                i_29 = 1
            }

            x = x * 256 / i_29
            y = y * 256 / i_29
            z = z * 256 / i_29

            val b_30: Byte = if (mesh.faceRenderType == null) 0 else mesh.faceRenderType!![faceTriangleIndex]
            if (b_30.toInt() == 0) {
                var vertexNormal = vertexNormals[lastMaterial.toInt()]!!
                vertexNormal.x += x
                vertexNormal.y += y
                vertexNormal.z += z
                vertexNormal.magnitude++
                vertexNormal = vertexNormals[billboardMaterialId.toInt()]!!
                vertexNormal.x += x
                vertexNormal.y += y
                vertexNormal.z += z
                vertexNormal.magnitude++
                vertexNormal = vertexNormals[materialId.toInt()]!!
                vertexNormal.x += x
                vertexNormal.y += y
                vertexNormal.z += z
                vertexNormal.magnitude++
            } else if (b_30.toInt() == 1) {
                val faceNormal = FaceNormalGL()
                faceNormals[faceTriangleIndex] = faceNormal
                faceNormal.x = x
                faceNormal.y = y
                faceNormal.z = z
            }
        }

        for (faceTriangleIndex in 0 until this.numFaces) {
            val texturedFace: Int
            val faceIndex = faces[faceTriangleIndex]
            val color = mesh.facesColor!![faceIndex].toInt() and 0xffff
            texturedFace = if (mesh.facesTexture == null) {
                -1
            } else {
                mesh.facesTexture!![faceIndex].toInt()
            }

            alphaAmt = if (mesh.facesAlpha == null) {
                0
            } else {
                mesh.facesAlpha!![faceIndex].toInt() and 0xff
            }

            var materialId = if (mesh.facesMaterial == null) (-1).toShort() else mesh.facesMaterial!![faceIndex]
            if (materialId.toInt() != -1 && (this.preferenceFlags and 0x40) != 0) {
                material = materialProvider.getMaterial(materialId.toInt() and 0xffff)
                if (material.detailsOnly) {
                    materialId = -1
                }
            }

            var aU = 0.0f
            var aV = 0.0f
            var bU = 0.0f
            var bV = 0.0f
            var cU = 0.0f
            var cV = 0.0f
            var uidOffsetB: Byte = 0
            var uidOffsetC: Byte = 0
            var i_95 = 0
            var renderType: Byte

            if (materialId.toInt() != -1) {
                if (texturedFace == -1) {
                    aU = 0.0f
                    aV = 1.0f
                    bU = 1.0f
                    bV = 1.0f
                    uidOffsetB = 1
                    cU = 0.0f
                    cV = 0.0f
                    uidOffsetC = 2
                } else {
                    val texFace = texturedFace and 0xff
                    renderType = mesh.texturesMapping!![texFace]
                    val faceA = mesh.facesA!![faceIndex]
                    val faceB = mesh.facesB!![faceIndex]
                    val faceC = mesh.facesC!![faceIndex]

                    if (renderType.toInt() == 0) {
                        val s_35 = mesh.textureFaceX!![texFace]
                        val s_36 = mesh.textureFaceY!![texFace]
                        val s_77 = mesh.textureFaceZ!![texFace]
                        val f_38 = mesh.verticesX!![s_35.toInt()].toFloat()
                        val f_39 = mesh.verticesY!![s_35.toInt()].toFloat()
                        val f_40 = mesh.verticesZ!![s_35.toInt()].toFloat()
                        val f_41 = mesh.verticesX!![s_36.toInt()].toFloat() - f_38
                        val f_42 = mesh.verticesY!![s_36.toInt()].toFloat() - f_39
                        val f_43 = mesh.verticesZ!![s_36.toInt()].toFloat() - f_40
                        val f_44 = mesh.verticesX!![s_77.toInt()].toFloat() - f_38
                        val f_45 = mesh.verticesY!![s_77.toInt()].toFloat() - f_39
                        val f_46 = mesh.verticesZ!![s_77.toInt()].toFloat() - f_40
                        val f_47 = mesh.verticesX!![faceA.toInt()].toFloat() - f_38
                        val f_48 = mesh.verticesY!![faceA.toInt()].toFloat() - f_39
                        val f_49 = mesh.verticesZ!![faceA.toInt()].toFloat() - f_40
                        val f_50 = mesh.verticesX!![faceB.toInt()].toFloat() - f_38
                        val f_51 = mesh.verticesY!![faceB.toInt()].toFloat() - f_39
                        val f_52 = mesh.verticesZ!![faceB.toInt()].toFloat() - f_40
                        val f_53 = mesh.verticesX!![faceC.toInt()].toFloat() - f_38
                        val f_54 = mesh.verticesY!![faceC.toInt()].toFloat() - f_39
                        val f_55 = mesh.verticesZ!![faceC.toInt()].toFloat() - f_40
                        val f_56 = f_42 * f_46 - f_43 * f_45
                        val f_57 = f_43 * f_44 - f_41 * f_46
                        val f_58 = f_41 * f_45 - f_42 * f_44
                        var f_59 = f_45 * f_58 - f_46 * f_57
                        var f_60 = f_46 * f_56 - f_44 * f_58
                        var f_61 = f_44 * f_57 - f_45 * f_56
                        var f_62 = 1.0f / (f_59 * f_41 + f_60 * f_42 + f_61 * f_43)
                        aU = (f_59 * f_47 + f_60 * f_48 + f_61 * f_49) * f_62
                        bU = (f_59 * f_50 + f_60 * f_51 + f_61 * f_52) * f_62
                        cU = (f_59 * f_53 + f_60 * f_54 + f_61 * f_55) * f_62
                        f_59 = f_42 * f_58 - f_43 * f_57
                        f_60 = f_43 * f_56 - f_41 * f_58
                        f_61 = f_41 * f_57 - f_42 * f_56
                        f_62 = 1.0f / (f_59 * f_44 + f_60 * f_45 + f_61 * f_46)
                        aV = (f_59 * f_47 + f_60 * f_48 + f_61 * f_49) * f_62
                        bV = (f_59 * f_50 + f_60 * f_51 + f_61 * f_52) * f_62
                        cV = (f_59 * f_53 + f_60 * f_54 + f_61 * f_55) * f_62
                    } else {
                        val centerX = transformMatrix!!.centerX!![texFace]
                        val centerY = transformMatrix.centerY!![texFace]
                        val centerZ = transformMatrix.centerZ!![texFace]
                        val matrix = transformMatrix.matrix!![texFace]
                        val direction = mesh.texturesDirection!![texFace]
                        val f_40 = mesh.texturesSpeed!![texFace] / 256.0f

                        if (renderType.toInt() == 1) {
                            val f_41 = mesh.texturesScaleZ!![texFace] / 1024.0f
                            mapCylindericalUv(mesh.verticesX!![faceA.toInt()], mesh.verticesY!![faceA.toInt()], mesh.verticesZ!![faceA.toInt()], centerX, centerY, centerZ, matrix, f_41, direction.toInt(), f_40, uvTypes)
                            aU = uvTypes[0]
                            aV = uvTypes[1]
                            mapCylindericalUv(mesh.verticesX!![faceB.toInt()], mesh.verticesY!![faceB.toInt()], mesh.verticesZ!![faceB.toInt()], centerX, centerY, centerZ, matrix, f_41, direction.toInt(), f_40, uvTypes)
                            bU = uvTypes[0]
                            bV = uvTypes[1]
                            mapCylindericalUv(mesh.verticesX!![faceC.toInt()], mesh.verticesY!![faceC.toInt()], mesh.verticesZ!![faceC.toInt()], centerX, centerY, centerZ, matrix, f_41, direction.toInt(), f_40, uvTypes)
                            cU = uvTypes[0]
                            cV = uvTypes[1]
                            val f_42 = f_41 / 2.0f

                            if ((direction.toInt() and 0x1) == 0) {
                                if (bU - aU > f_42) {
                                    bU -= f_41
                                    uidOffsetB = 1
                                } else if (aU - bU > f_42) {
                                    bU += f_41
                                    uidOffsetB = 2
                                }

                                if (cU - aU > f_42) {
                                    cU -= f_41
                                    uidOffsetC = 1
                                } else if (aU - cU > f_42) {
                                    cU += f_41
                                    uidOffsetC = 2
                                }
                            } else {
                                if (bV - aV > f_42) {
                                    bV -= f_41
                                    uidOffsetB = 1
                                } else if (aV - bV > f_42) {
                                    bV += f_41
                                    uidOffsetB = 2
                                }

                                if (cV - aV > f_42) {
                                    cV -= f_41
                                    uidOffsetC = 1
                                } else if (aV - cV > f_42) {
                                    cV += f_41
                                    uidOffsetC = 2
                                }
                            }
                        } else if (renderType.toInt() == 2) {
                            val f_41 = mesh.texturesUtrans!![texFace] / 256.0f
                            val f_42 = mesh.texturesVtrans!![texFace] / 256.0f
                            val i_100 = mesh.verticesX!![faceB.toInt()] - mesh.verticesX!![faceA.toInt()]
                            val i_101 = mesh.verticesY!![faceB.toInt()] - mesh.verticesY!![faceA.toInt()]
                            val i_102 = mesh.verticesZ!![faceB.toInt()] - mesh.verticesZ!![faceA.toInt()]
                            val i_103 = mesh.verticesX!![faceC.toInt()] - mesh.verticesX!![faceA.toInt()]
                            val i_104 = mesh.verticesY!![faceC.toInt()] - mesh.verticesY!![faceA.toInt()]
                            val i_105 = mesh.verticesZ!![faceC.toInt()] - mesh.verticesZ!![faceA.toInt()]
                            val i_106 = i_101 * i_105 - i_104 * i_102
                            val i_107 = i_102 * i_103 - i_105 * i_100
                            val i_108 = i_100 * i_104 - i_103 * i_101
                            val f_52 = 64.0f / mesh.texturesScaleX!![texFace]
                            val f_53 = 64.0f / mesh.texturesScaleY!![texFace]
                            val f_54 = 64.0f / mesh.texturesScaleZ!![texFace]
                            val f_55 = (i_106 * matrix!![0] + i_107 * matrix[1] + i_108 * matrix[2]) / f_52
                            val f_56 = (i_106 * matrix[3] + i_107 * matrix[4] + i_108 * matrix[5]) / f_53
                            val f_57 = (i_106 * matrix[6] + i_107 * matrix[7] + i_108 * matrix[8]) / f_54
                            i_95 = getCubeFace(f_55, f_56, f_57)
                            mapRectangularUv(mesh.verticesX!![faceA.toInt()], mesh.verticesY!![faceA.toInt()], mesh.verticesZ!![faceA.toInt()], centerX, centerY, centerZ, i_95, matrix, direction.toInt(), f_40, f_41, f_42, uvTypes)
                            aU = uvTypes[0]
                            aV = uvTypes[1]
                            mapRectangularUv(mesh.verticesX!![faceB.toInt()], mesh.verticesY!![faceB.toInt()], mesh.verticesZ!![faceB.toInt()], centerX, centerY, centerZ, i_95, matrix, direction.toInt(), f_40, f_41, f_42, uvTypes)
                            bU = uvTypes[0]
                            bV = uvTypes[1]
                            mapRectangularUv(mesh.verticesX!![faceC.toInt()], mesh.verticesY!![faceC.toInt()], mesh.verticesZ!![faceC.toInt()], centerX, centerY, centerZ, i_95, matrix, direction.toInt(), f_40, f_41, f_42, uvTypes)
                            cU = uvTypes[0]
                            cV = uvTypes[1]
                        } else if (renderType.toInt() == 3) {
                            mapSphericalUv(mesh.verticesX!![faceA.toInt()], mesh.verticesY!![faceA.toInt()], mesh.verticesZ!![faceA.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), f_40, uvTypes)
                            aU = uvTypes[0]
                            aV = uvTypes[1]
                            mapSphericalUv(mesh.verticesX!![faceB.toInt()], mesh.verticesY!![faceB.toInt()], mesh.verticesZ!![faceB.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), f_40, uvTypes)
                            bU = uvTypes[0]
                            bV = uvTypes[1]
                            mapSphericalUv(mesh.verticesX!![faceC.toInt()], mesh.verticesY!![faceC.toInt()], mesh.verticesZ!![faceC.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), f_40, uvTypes)
                            cU = uvTypes[0]
                            cV = uvTypes[1]

                            if ((direction.toInt() and 0x1) == 0) {
                                if (bU - aU > 0.5f) {
                                    bU--
                                    uidOffsetB = 1
                                } else if (aU - bU > 0.5f) {
                                    bU++
                                    uidOffsetB = 2
                                }

                                if (cU - aU > 0.5f) {
                                    cU--
                                    uidOffsetC = 1
                                } else if (aU - cU > 0.5f) {
                                    cU++
                                    uidOffsetC = 2
                                }
                            } else {
                                if (bV - aV > 0.5f) {
                                    bV--
                                    uidOffsetB = 1
                                } else if (aV - bV > 0.5f) {
                                    bV++
                                    uidOffsetB = 2
                                }

                                if (cV - aV > 0.5f) {
                                    cV--
                                    uidOffsetC = 1
                                } else if (aV - cV > 0.5f) {
                                    cV++
                                    uidOffsetC = 2
                                }
                            }
                        }
                    }
                }
            }

            renderType = if (mesh.faceRenderType == null) {
                0
            } else {
                mesh.faceRenderType!![faceIndex]
            }

            if (renderType.toInt() == 0) {
                val uid = (texturedFace.toLong() shl 2) + ((i_95.toLong() shl 24) + (color.toLong() shl 8) + alphaAmt shl 32)
                val faceA = mesh.facesA!![faceIndex]
                val faceB = mesh.facesB!![faceIndex]
                val faceC = mesh.facesC!![faceIndex]
                var vertexNormal = vertexNormals[faceA.toInt()]!!
                this.facesA!![faceTriangleIndex] = connectNormal(faceA.toInt(), uid, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, aU, aV)
                vertexNormal = vertexNormals[faceB.toInt()]!!
                this.facesB!![faceTriangleIndex] = connectNormal(faceB.toInt(), uid + uidOffsetB, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, bU, bV)
                vertexNormal = vertexNormals[faceC.toInt()]!!
                this.facesC!![faceTriangleIndex] = connectNormal(faceC.toInt(), uid + uidOffsetC, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, cU, cV)
            } else if (renderType.toInt() == 1) {
                val faceNormal = faceNormals[faceIndex]!!
                val uid = ((texturedFace.toLong() shl 2) + (if (faceNormal.x > 0) 1024 else 2048) + ((faceNormal.y + 256).toLong() shl 12) + ((faceNormal.z + 256).toLong() shl 22)) + ((i_95.toLong() shl 24) + (color.toLong() shl 8) + alphaAmt shl 32)
                this.facesA!![faceTriangleIndex] = connectNormal(mesh.facesA!![faceIndex].toInt(), uid, faceNormal.x, faceNormal.y, faceNormal.z, 0, aU, aV)
                this.facesB!![faceTriangleIndex] = connectNormal(mesh.facesB!![faceIndex].toInt(), uid + uidOffsetB, faceNormal.x, faceNormal.y, faceNormal.z, 0, bU, bV)
                this.facesC!![faceTriangleIndex] = connectNormal(mesh.facesC!![faceIndex].toInt(), uid + uidOffsetC, faceNormal.x, faceNormal.y, faceNormal.z, 0, cU, cV)
            }

            if (mesh.facesAlpha != null) {
                this.facesAlpha!![faceTriangleIndex] = mesh.facesAlpha!![faceIndex]
            }

            if (mesh.facesSubmeshes != null) {
                this.facesSubmeshes!![faceTriangleIndex] = mesh.facesSubmeshes!![faceIndex]
            }

            this.facesColor!![faceTriangleIndex] = mesh.facesColor!![faceIndex]
            this.facesMaterial!![faceTriangleIndex] = materialId
        }

        triangleIndex = 0
        lastMaterial = -10000

        for (i_85 in 0 until this.anInt8302) {
            val materialId = this.facesMaterial!![i_85]
            if (materialId != lastMaterial) {
                triangleIndex++
                lastMaterial = materialId
            }
        }

        this.anIntArray8289 = IntArray(triangleIndex + 1)
        triangleIndex = 0
        lastMaterial = -10000

        for (face in 0 until this.anInt8302) {
            val materialId = this.facesMaterial!![face]
            if (materialId != lastMaterial) {
                this.anIntArray8289!![triangleIndex++] = face
                lastMaterial = materialId
            }
        }

        this.anIntArray8289!![triangleIndex] = this.anInt8302
        normalsUid = null
        this.normalsX = clone(this.normalsX!!, this.numNormals)
        this.normalsY = clone(this.normalsY!!, this.numNormals)
        this.normalsZ = clone(this.normalsZ!!, this.numNormals)
        this.magnitudes = clone(this.magnitudes!!, this.numNormals)
        this.textureU = clone(this.textureU!!, this.numNormals)
        this.textureV = clone(this.textureV!!, this.numNormals)

        if (mesh.vertexGroups != null && ModelFunctionMaskGL.altersVertices(rasterizerFlags)) {
            this.vertexGroups = mesh.createVertexGroups(false) as Array<IntArray?>?
        }

        if (mesh.billboards != null && ModelFunctionMaskGL.altersBillboards(rasterizerFlags)) {
            this.billboardGroups = mesh.createBillboardGroups() as Array<IntArray?>?
        }

        if (mesh.faceGroups != null && ModelFunctionMaskGL.altersFaces(rasterizerFlags)) {
            var i_85 = 0
            val ints_68 = IntArray(256)

            for (faceAlphaAmt in 0 until this.numFaces) {
                i_21 = mesh.faceGroups!![faces[faceAlphaAmt]]
                if (i_21 >= 0) {
                    ++ints_68[i_21]
                    if (i_21 > i_85) {
                        i_85 = i_21
                    }
                }
            }

            this.faceGroups = arrayOfNulls(i_85 + 1)

            for (faceAlphaAmt in 0..i_85) {
                this.faceGroups!![faceAlphaAmt] = IntArray(ints_68[faceAlphaAmt])
                ints_68[faceAlphaAmt] = 0
            }

            for (faceAlphaAmt in 0 until this.numFaces) {
                i_21 = mesh.faceGroups!![faces[faceAlphaAmt]]
                if (i_21 >= 0) {
                    this.faceGroups!![i_21]!![ints_68[i_21]++] = faceAlphaAmt
                }
            }
        }
    }

    private fun connectNormal(face: Int, uid: Long, x: Int, y: Int, z: Int, magnitude: Int, u: Float, v: Float): Short {
        val i_11 = this.triangles!![face]
        val i_12 = this.triangles!![face + 1]
        var i_13 = 0

        for (i_14 in i_11 until i_12) {
            val s_15 = this.aShortArray8288!![i_14]
            if (s_15.toInt() == 0) {
                i_13 = i_14
                break
            }

            if (normalsUid!![i_14] == uid) {
                return (s_15 - 1).toShort()
            }
        }

        this.aShortArray8288!![i_13] = (this.numNormals + 1).toShort()
        normalsUid!![i_13] = uid
        this.normalsX!![this.numNormals] = x.toShort()
        this.normalsY!![this.numNormals] = y.toShort()
        this.normalsZ!![this.numNormals] = z.toShort()
        this.magnitudes!![this.numNormals] = magnitude.toByte()
        this.textureU!![this.numNormals] = u
        this.textureV!![this.numNormals] = v
        return (this.numNormals++).toShort()
    }

    fun method13522(shadowgl: ShadowGL) {
        if (this.toolkit.projectedVerticesX.size < this.numNormals) {
            this.toolkit.projectedVerticesX = IntArray(this.numNormals)
            this.toolkit.projectedVerticesY = IntArray(this.numNormals)
        }

        val projectedVerticesX = this.toolkit.projectedVerticesX
        val projectedVerticesY = this.toolkit.projectedVerticesY

        for (v in 0 until this.maxDepth) {
            val x = ((this.verticesX!![v] - (this.toolkit.shadowOffsetX * this.verticesY!![v] shr 8)) shr this.toolkit.shadowScale) - shadowgl.startX
            val y = ((this.verticesZ!![v] - (this.toolkit.shadowOffsetZ * this.verticesY!![v] shr 8)) shr this.toolkit.shadowScale) - shadowgl.startY
            val i_7 = this.triangles!![v]
            val i_8 = this.triangles!![v + 1]

            for (i_9 in i_7 until i_8) {
                val i_10 = (this.aShortArray8288!![i_9].toInt() and 0xffff) - 1
                if (i_10 == -1) {
                    break
                }

                projectedVerticesX[i_10] = x
                projectedVerticesY[i_10] = y
            }
        }

        for (face in 0 until this.anInt8302) {
            val faceA = this.facesA!![face].toInt() and 0xffff
            val faceB = this.facesB!![face].toInt() and 0xffff
            val faceC = this.facesC!![face].toInt() and 0xffff
            val xA = projectedVerticesX[faceA]
            val xB = projectedVerticesX[faceB]
            val xC = projectedVerticesX[faceC]
            val yA = projectedVerticesY[faceA]
            val yB = projectedVerticesY[faceB]
            val yC = projectedVerticesY[faceC]
            if ((xA - xB) * (yB - yC) - (yB - yA) * (xC - xB) > 0) {
                shadowgl.markShadedTriangle(yA, yB, yC, xA, xB, xC)
            }
        }
    }

    override fun applyShading(i_1: Int) {
        this.functionMask = i_1
        if (this.aClass1668259 != null && (this.functionMask and 0x10000) == 0) {
            this.normalsX = this.aClass1668259!!.normalsX
            this.normalsY = this.aClass1668259!!.normalsY
            this.normalsZ = this.aClass1668259!!.normalsZ
            this.magnitudes = this.aClass1668259!!.magnitude
            this.aClass1668259 = null
        }

        this.updateRequired = true
        method13525()
    }

    override fun copy(b_1: Byte, functionMask: Int, bool3: Boolean): Model? {
        var bool4 = false
        val class528Sub15: ModelGL
        val class528Sub16: ModelGL

        if (b_1 > 0 && b_1 <= 7) {
            class528Sub16 = this.toolkit.copyDst[b_1 - 1]!!
            class528Sub15 = this.toolkit.copySrc[b_1 - 1]!!
            bool4 = true
        } else {
            class528Sub15 = ModelGL(this.toolkit)
            class528Sub16 = class528Sub15
        }

        return method13524(class528Sub15, class528Sub16, functionMask, bool4)
    }

    fun method13524(dst: ModelGL, src: ModelGL, flags: Int, bool4: Boolean): Model {
        dst.renderState = 0
        dst.functionMask = flags
        dst.preferenceFlags = this.preferenceFlags
        dst.ambient = this.ambient
        dst.contrast = this.contrast
        dst.numVertices = this.numVertices
        // Clamp maxDepth to numVertices to prevent ArrayIndexOutOfBoundsException
        dst.maxDepth = minOf(this.maxDepth, this.numVertices)
        dst.numNormals = this.numNormals
        dst.numFaces = this.numFaces
        dst.anInt8302 = this.anInt8302
        dst.billboardCount = this.billboardCount

        if ((flags and 0x100) != 0) {
            dst.hasAlphaTransition = true
        } else {
            dst.hasAlphaTransition = this.hasAlphaTransition
        }

        dst.textureAnimating = this.textureAnimating
        val copyVerticesX = ModelFunctionMaskGL.altersVerticesX(flags)
        val copyVerticesY = ModelFunctionMaskGL.altersVerticesY(flags)
        val copyVerticesZ = ModelFunctionMaskGL.altersVerticesZ(flags)
        val copyAnyVertices = copyVerticesX or copyVerticesY or copyVerticesZ
        var i_10: Int

        if (copyAnyVertices) {
            if (copyVerticesX) {
                if (src.verticesX != null && src.verticesX!!.size >= this.numVertices) {
                    dst.verticesX = src.verticesX
                } else {
                    dst.verticesX = IntArray(this.numVertices)
                    src.verticesX = dst.verticesX
                }
            } else {
                dst.verticesX = this.verticesX
            }

            if (copyVerticesY) {
                if (src.verticesY != null && src.verticesY!!.size >= this.numVertices) {
                    dst.verticesY = src.verticesY
                } else {
                    dst.verticesY = IntArray(this.numVertices)
                    src.verticesY = dst.verticesY
                }
            } else {
                dst.verticesY = this.verticesY
            }

            if (copyVerticesZ) {
                if (src.verticesZ != null && src.verticesZ!!.size >= this.numVertices) {
                    dst.verticesZ = src.verticesZ
                } else {
                    dst.verticesZ = IntArray(this.numVertices)
                    src.verticesZ = dst.verticesZ
                }
            } else {
                dst.verticesZ = this.verticesZ
            }

            for (i in 0 until this.numVertices) {
                if (copyVerticesX) {
                    dst.verticesX!![i] = this.verticesX!![i]
                }

                if (copyVerticesY) {
                    dst.verticesY!![i] = this.verticesY!![i]
                }

                if (copyVerticesZ) {
                    dst.verticesZ!![i] = this.verticesZ!![i]
                }
            }
        } else {
            dst.verticesX = this.verticesX
            dst.verticesY = this.verticesY
            dst.verticesZ = this.verticesZ
        }

        if (ModelFunctionMaskGL.altersAnyVerticeType(flags)) {
            if (bool4) {
                dst.renderState = (dst.renderState.toInt() or 0x1).toByte()
            }

            dst.vertexBuffer = src.vertexBuffer
            dst.vertexBuffer!!.offset = this.vertexBuffer!!.offset
            dst.vertexBuffer!!.buffer = this.vertexBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasVertices(flags)) {
            dst.vertexBuffer = this.vertexBuffer
        } else {
            dst.vertexBuffer = null
        }

        if (ModelFunctionMaskGL.altersColor(flags)) {
            if (src.facesColor != null && src.facesColor!!.size >= this.numFaces) {
                dst.facesColor = src.facesColor
            } else {
                dst.facesColor = ShortArray(this.numFaces)
                src.facesColor = dst.facesColor
            }

            for (i in 0 until this.numFaces) {
                dst.facesColor!![i] = this.facesColor!![i]
            }
        } else {
            dst.facesColor = this.facesColor
        }

        if (ModelFunctionMaskGL.altersAlpha(flags)) {
            if (src.facesAlpha != null && src.facesAlpha!!.size >= this.numFaces) {
                dst.facesAlpha = src.facesAlpha
            } else {
                dst.facesAlpha = ByteArray(this.numFaces)
                src.facesAlpha = dst.facesAlpha
            }

            for (i in 0 until this.numFaces) {
                dst.facesAlpha!![i] = this.facesAlpha!![i]
            }
        } else {
            dst.facesAlpha = this.facesAlpha
        }

        if (ModelFunctionMaskGL.method2199(flags, this.preferenceFlags)) {
            if (bool4) {
                dst.renderState = (dst.renderState.toInt() or 0x2).toByte()
            }

            dst.colorsBuffer = src.colorsBuffer
            dst.colorsBuffer!!.offset = this.colorsBuffer!!.offset
            dst.colorsBuffer!!.buffer = this.colorsBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasColors(flags)) {
            dst.colorsBuffer = this.colorsBuffer
        } else {
            dst.colorsBuffer = null
        }

        var i_11: Int
        if (ModelFunctionMaskGL.altersLight(flags)) {
            if (src.normalsX != null && src.normalsX!!.size >= this.numNormals) {
                dst.normalsX = src.normalsX
                dst.normalsY = src.normalsY
                dst.normalsZ = src.normalsZ
            } else {
                i_10 = this.numNormals
                dst.normalsX = ShortArray(i_10)
                src.normalsX = dst.normalsX
                dst.normalsY = ShortArray(i_10)
                src.normalsY = dst.normalsY
                dst.normalsZ = ShortArray(i_10)
                src.normalsZ = dst.normalsZ
            }

            if (this.aClass1668259 != null) {
                if (src.aClass1668259 == null) {
                    src.aClass1668259 = ModelNormalsGL()
                }

                val class16612 = src.aClass1668259!!
                dst.aClass1668259 = class16612

                if (class16612.normalsX == null || class16612.normalsX!!.size < this.numNormals) {
                    i_11 = this.numNormals
                    class16612.normalsX = ShortArray(i_11)
                    class16612.normalsY = ShortArray(i_11)
                    class16612.normalsZ = ShortArray(i_11)
                    class16612.magnitude = ByteArray(i_11)
                }

                for (idx in 0 until this.numNormals) {
                    dst.normalsX!![idx] = this.normalsX!![idx]
                    dst.normalsY!![idx] = this.normalsY!![idx]
                    dst.normalsZ!![idx] = this.normalsZ!![idx]
                    class16612.normalsX!![idx] = this.aClass1668259!!.normalsX!![idx]
                    class16612.normalsY!![idx] = this.aClass1668259!!.normalsY!![idx]
                    class16612.normalsZ!![idx] = this.aClass1668259!!.normalsZ!![idx]
                    class16612.magnitude!![idx] = this.aClass1668259!!.magnitude!![idx]
                }
            } else {
                for (idx in 0 until this.numNormals) {
                    dst.normalsX!![idx] = this.normalsX!![idx]
                    dst.normalsY!![idx] = this.normalsY!![idx]
                    dst.normalsZ!![idx] = this.normalsZ!![idx]
                }
            }
        } else {
            dst.aClass1668259 = this.aClass1668259
            dst.normalsX = this.normalsX
            dst.normalsY = this.normalsY
            dst.normalsZ = this.normalsZ
        }

        dst.magnitudes = this.magnitudes

        if (ModelFunctionMaskGL.method2261(flags, this.preferenceFlags)) {
            if (bool4) {
                dst.renderState = (dst.renderState.toInt() or 0x4).toByte()
            }

            dst.normalsBuffer = src.normalsBuffer
            dst.normalsBuffer!!.offset = this.normalsBuffer!!.offset
            dst.normalsBuffer!!.buffer = this.normalsBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasNormals(flags, this.preferenceFlags)) {
            dst.normalsBuffer = this.normalsBuffer
        } else {
            dst.normalsBuffer = null
        }

        dst.textureU = this.textureU
        dst.textureV = this.textureV

        if (ModelFunctionMaskGL.method2200()) {
            if (bool4) {
                dst.renderState = (dst.renderState.toInt() or 0x8).toByte()
            }

            dst.textureCoordinatesBuffer = src.textureCoordinatesBuffer
            dst.textureCoordinatesBuffer!!.offset = this.textureCoordinatesBuffer!!.offset
            dst.textureCoordinatesBuffer!!.buffer = this.textureCoordinatesBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasTextureCoordinates(flags)) {
            dst.textureCoordinatesBuffer = this.textureCoordinatesBuffer
        } else {
            dst.textureCoordinatesBuffer = null
        }

        if (ModelFunctionMaskGL.altersFacing(flags)) {
            if (src.facesA != null && src.facesA!!.size >= this.numFaces) {
                dst.facesA = src.facesA
                dst.facesB = src.facesB
                dst.facesC = src.facesC
            } else {
                i_10 = this.numFaces
                dst.facesA = ShortArray(i_10)
                src.facesA = dst.facesA
                dst.facesB = ShortArray(i_10)
                src.facesB = dst.facesB
                dst.facesC = ShortArray(i_10)
                src.facesC = dst.facesC
            }

            for (i in 0 until this.numFaces) {
                dst.facesA!![i] = this.facesA!![i]
                dst.facesB!![i] = this.facesB!![i]
                dst.facesC!![i] = this.facesC!![i]
            }
        } else {
            dst.facesA = this.facesA
            dst.facesB = this.facesB
            dst.facesC = this.facesC
        }

        if (ModelFunctionMaskGL.copyElementsBuffer(flags)) {
            if (bool4) {
                dst.renderState = (dst.renderState.toInt() or 0x10).toByte()
            }

            dst.modelElementsBuffer = src.modelElementsBuffer
            dst.modelElementsBuffer!!.openglElementsBuffer = this.modelElementsBuffer!!.openglElementsBuffer
        } else if (ModelFunctionMaskGL.hasElementsBuffer(flags)) {
            dst.modelElementsBuffer = this.modelElementsBuffer
        } else {
            dst.modelElementsBuffer = null
        }

        if (ModelFunctionMaskGL.altersTextures(flags)) {
            if (src.facesMaterial != null && src.facesMaterial!!.size >= this.numFaces) {
                dst.facesMaterial = src.facesMaterial
            } else {
                i_10 = this.numFaces
                dst.facesMaterial = ShortArray(i_10)
                src.facesMaterial = dst.facesMaterial
            }

            for (i in 0 until this.numFaces) {
                dst.facesMaterial!![i] = this.facesMaterial!![i]
            }
        } else {
            dst.facesMaterial = this.facesMaterial
        }

        if (ModelFunctionMaskGL.copyBillboards(flags)) {
            if (src.billboardSurfaces != null && src.billboardSurfaces!!.size >= this.billboardCount) {
                dst.billboardSurfaces = src.billboardSurfaces

                for (i in 0 until this.billboardCount) {
                    dst.billboardSurfaces!![i]!!.copyFrom(this.billboardSurfaces!![i]!!)
                }
            } else {
                i_10 = this.billboardCount
                dst.billboardSurfaces = arrayOfNulls(i_10)
                src.billboardSurfaces = dst.billboardSurfaces

                for (idx in 0 until this.billboardCount) {
                    dst.billboardSurfaces!![idx] = this.billboardSurfaces!![idx]!!.copy()
                }
            }
        } else {
            dst.billboardSurfaces = this.billboardSurfaces
        }

        dst.billboardEffects = this.billboardEffects

        if (this.boundsCalculated) {
            dst.size3d = this.size3d
            dst.size2d = this.size2d
            dst.minX = this.minX
            dst.maxX = this.maxX
            dst.minY = this.minY
            dst.maxY = this.maxY
            dst.minZ = this.minZ
            dst.maxZ = this.maxZ
            dst.boundsCalculated = true
        } else {
            dst.boundsCalculated = false
        }

        dst.vertexGroups = this.vertexGroups
        dst.faceGroups = this.faceGroups
        dst.billboardGroups = this.billboardGroups
        dst.aShortArray8288 = this.aShortArray8288
        dst.triangles = this.triangles
        dst.verticesSubmeshes = this.verticesSubmeshes
        dst.facesSubmeshes = this.facesSubmeshes
        dst.anIntArray8289 = this.anIntArray8289
        dst.emissiveTriangles = this.emissiveTriangles
        dst.effectiveVertices = this.effectiveVertices
        return dst
    }

    fun method13525() {
        if (this.updateRequired) {
            this.updateRequired = false
            if (this.emissiveTriangles == null && this.effectiveVertices == null && this.billboardEffects == null && !ModelFunctionMaskGL.altersVerticePosition(this.functionMask)) {
                var verticesXUploaded = false
                var verticesYUploaded = false
                var verticesZUploaded = false

                if (this.verticesX != null && !ModelFunctionMaskGL.method2202(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer!!.buffer == null) {
                        this.updateRequired = true
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds()
                        }

                        verticesXUploaded = true
                    }
                }

                if (this.verticesY != null && !ModelFunctionMaskGL.method2229(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer!!.buffer == null) {
                        this.updateRequired = true
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds()
                        }

                        verticesYUploaded = true
                    }
                }

                if (this.verticesZ != null && !ModelFunctionMaskGL.method2252(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer!!.buffer == null) {
                        this.updateRequired = true
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds()
                        }

                        verticesZUploaded = true
                    }
                }

                if (verticesXUploaded) {
                    this.verticesX = null
                }

                if (verticesYUploaded) {
                    this.verticesY = null
                }

                if (verticesZUploaded) {
                    this.verticesZ = null
                }
            }

            if (this.aShortArray8288 != null && this.verticesX == null && this.verticesY == null && this.verticesZ == null) {
                this.aShortArray8288 = null
                this.triangles = null
            }

            if (this.magnitudes != null && !ModelFunctionMaskGL.method2204(this.functionMask, this.preferenceFlags)) {
                if (this.normalsBuffer != null) {
                    if (this.normalsBuffer!!.buffer != null) {
                        this.normalsZ = null
                        this.normalsY = null
                        this.normalsX = null
                        this.magnitudes = null
                    } else {
                        this.updateRequired = true
                    }
                } else if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.normalsZ = null
                    this.normalsY = null
                    this.normalsX = null
                    this.magnitudes = null
                }
            }

            if (this.facesColor != null && !ModelFunctionMaskGL.method2205(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.facesColor = null
                }
            }

            if (this.facesAlpha != null && !ModelFunctionMaskGL.method2206(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.facesAlpha = null
                }
            }

            if (this.textureU != null && !ModelFunctionMaskGL.method2241()) {
                if (this.textureCoordinatesBuffer != null && this.textureCoordinatesBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.textureV = null
                    this.textureU = null
                }
            }

            if (this.facesMaterial != null && !ModelFunctionMaskGL.method2212(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.facesMaterial = null
                }
            }

            if (this.facesA != null && !ModelFunctionMaskGL.method2211(this.functionMask, this.preferenceFlags)) {
                if ((this.modelElementsBuffer != null && this.modelElementsBuffer!!.openglElementsBuffer == null) || (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null)) {
                    this.updateRequired = true
                } else {
                    this.facesC = null
                    this.facesB = null
                    this.facesA = null
                }
            }

            if (this.faceGroups != null && !ModelFunctionMaskGL.altersFaces(this.functionMask)) {
                this.faceGroups = null
                this.facesSubmeshes = null
            }

            if (this.vertexGroups != null && !ModelFunctionMaskGL.altersVertices(this.functionMask)) {
                this.vertexGroups = null
                this.verticesSubmeshes = null
            }

            if (this.billboardGroups != null && !ModelFunctionMaskGL.altersBillboards(this.functionMask)) {
                this.billboardGroups = null
            }

            if (this.anIntArray8289 != null && (this.functionMask and 0x800) == 0 && (this.functionMask and 0x40000) == 0) {
                this.anIntArray8289 = null
            }
        }
    }

    override fun yaxisRotateWithoutNormals(i_1: Int) {
        val i_2 = MathUtils.SINE[i_1]
        val i_3 = MathUtils.COSINE[i_1]

        for (i_4 in 0 until this.maxDepth) {
            val i_5 = (i_2 * this.verticesZ!![i_4] + i_3 * this.verticesX!![i_4]) shr 14
            this.verticesZ!![i_4] = (i_3 * this.verticesZ!![i_4] - i_2 * this.verticesX!![i_4]) shr 14
            this.verticesX!![i_4] = i_5
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun rotateWithNormals(i_1: Int) {
        val i_2 = MathUtils.SINE[i_1]
        val i_3 = MathUtils.COSINE[i_1]

        var i_4: Int
        var i_5: Int

        for (v in 0 until this.maxDepth) {
            i_5 = (i_2 * this.verticesZ!![v] + i_3 * this.verticesX!![v]) shr 14
            this.verticesZ!![v] = (i_3 * this.verticesZ!![v] - i_2 * this.verticesX!![v]) shr 14
            this.verticesX!![v] = i_5
        }

        for (n in 0 until this.numNormals) {
            i_5 = (this.normalsZ!![n] * i_2 + this.normalsX!![n] * i_3) shr 14
            this.normalsZ!![n] = ((this.normalsZ!![n] * i_3 - this.normalsX!![n] * i_2) shr 14).toShort()
            this.normalsX!![n] = i_5.toShort()
        }

        if (this.normalsBuffer == null && this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer!!.buffer = null
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    fun method13534() {
        if (this.anInt8302 != 0) {
            if (this.renderState.toInt() != 0) {
                upload(true)
            }

            upload(false)
            val modelElements = this.modelElementsBuffer
            if (modelElements != null) {
                if (modelElements.openglElementsBuffer == null) {
                    method13536((this.renderState.toInt() and 0x10) != 0)
                }

                val glElementsBuffer = modelElements.openglElementsBuffer
                val intArray = this.anIntArray8289
                val facesMaterialLocal = this.facesMaterial
                if (glElementsBuffer != null && intArray != null && facesMaterialLocal != null) {
                    this.toolkit.setLightingEnabled(this.normalsBuffer != null)
                    this.toolkit.toggleDrawingClientstates(this.vertexBuffer, this.normalsBuffer, this.colorsBuffer, this.textureCoordinatesBuffer)
                    val i_1 = intArray.size - 1

                    for (i_2 in 0 until i_1) {
                        val i_3 = intArray[i_2]
                        val i_4 = intArray[i_2 + 1]
                        var materialId = facesMaterialLocal[i_3].toInt() and 0xffff
                        if (materialId == 65535) {
                            materialId = -1
                        }

                        this.toolkit.renderTexture(materialId, this.normalsBuffer != null)
                        this.toolkit.drawElements(glElementsBuffer, i_3 * 3, (i_4 - i_3) * 3)
                    }
                }
            }

            method13525()
        }
    }

    fun method13535(matrix441: Matrix44) {
        val billboardEffectsLocal = this.billboardEffects
        val billboardSurfacesLocal = this.billboardSurfaces
        val verticesXLocal = this.verticesX
        val verticesYLocal = this.verticesY
        val verticesZLocal = this.verticesZ
        val elements = matrix441.elements

        if (billboardEffectsLocal != null && billboardSurfacesLocal != null &&
            verticesXLocal != null && verticesYLocal != null && verticesZLocal != null && elements != null) {
            val matrix442 = this.toolkit.billboardMatrix
            this.toolkit.pushMatrix()
            this.toolkit.setDepthBuffering(!this.hasAlphaTransition)
            this.toolkit.setLightingEnabled(false)
            this.toolkit.toggleDrawingClientstates(this.toolkit.quadVertexBuffer, null, null, this.toolkit.quadTexCoordBuffer)

            for (b in 0 until this.billboardCount) {
                val effect = billboardEffectsLocal[b] ?: continue
                val surface = billboardSurfacesLocal[b] ?: continue
                if (!effect.stationary || !this.toolkit.bloomEnabled()) {
                    val f_6 = (verticesXLocal[effect.faceA] + verticesXLocal[effect.faceB] + verticesXLocal[effect.faceC]) * 0.3333333f
                    val f_7 = (verticesYLocal[effect.faceA] + verticesYLocal[effect.faceB] + verticesYLocal[effect.faceC]) * 0.3333333f
                    val f_8 = (verticesZLocal[effect.faceA] + verticesZLocal[effect.faceB] + verticesZLocal[effect.faceC]) * 0.3333333f
                    val f_9 = elements[0] * f_6 + elements[4] * f_7 + elements[8] * f_8 + elements[12]
                    val f_10 = elements[1] * f_6 + elements[5] * f_7 + elements[9] * f_8 + elements[13]
                    val f_11 = elements[2] * f_6 + elements[6] * f_7 + elements[10] * f_8 + elements[14]
                    val f_12 = (1.0 / sqrt((f_9 * f_9 + f_10 * f_10 + f_11 * f_11).toDouble())).toFloat() * effect.distance
                    matrix442.setRotatedTranslation(surface.rotation, (surface.scaleX * effect.sizeX) shr 7, (surface.scaleY * effect.sizeY) shr 7, f_9 + surface.offsetX - f_9 * f_12, f_10 + surface.offsetY - f_10 * f_12, f_11 - f_11 * f_12)
                    this.toolkit.loadMatrix(matrix442)
                    val diffuseColor = surface.diffuseColor
                    OpenGL.glColor4ub((diffuseColor shr 16).toByte(), (diffuseColor shr 8).toByte(), diffuseColor.toByte(), (diffuseColor shr 24).toByte())
                    this.toolkit.renderTexture(effect.materialId.toInt())
                    this.toolkit.setBlendMode(effect.shape.toInt())
                    this.toolkit.drawArrays(4)
                }
            }

            this.toolkit.setDepthBuffering(true)
            this.toolkit.popMatrix()
        }
    }

    fun method13536(bool1: Boolean) {
        val facesALocal = this.facesA
        val facesBLocal = this.facesB
        val facesCLocal = this.facesC
        val modelElements = this.modelElementsBuffer

        if (facesALocal == null || facesBLocal == null || facesCLocal == null || modelElements == null) {
            return
        }

        if (this.toolkit.textureBuffer.buffer.size < this.anInt8302 * 6) {
            this.toolkit.textureBuffer = GlBitBuffer((this.anInt8302 + 100) * 6)
        } else {
            this.toolkit.textureBuffer.currentPosition = 0
        }

        val textureBuffer = this.toolkit.textureBuffer
        if (this.toolkit.bigEndianess) {
            for (i_3 in 0 until this.anInt8302) {
                textureBuffer.writeShort(facesALocal[i_3].toInt())
                textureBuffer.writeShort(facesBLocal[i_3].toInt())
                textureBuffer.writeShort(facesCLocal[i_3].toInt())
            }
        } else {
            for (i_3 in 0 until this.anInt8302) {
                textureBuffer.writeShortLittle(facesALocal[i_3].toInt())
                textureBuffer.writeShortLittle(facesBLocal[i_3].toInt())
                textureBuffer.writeShortLittle(facesCLocal[i_3].toInt())
            }
        }

        if (textureBuffer.currentPosition != 0) {
            if (bool1) {
                val existingBuffer = this.anInterface158277
                if (existingBuffer == null) {
                    this.anInterface158277 = this.toolkit.createElementsArrayBuffer(textureBuffer.buffer, textureBuffer.currentPosition, true)
                } else {
                    existingBuffer.putElements(textureBuffer.buffer, textureBuffer.currentPosition)
                }

                modelElements.openglElementsBuffer = this.anInterface158277
            } else {
                modelElements.openglElementsBuffer = this.toolkit.createElementsArrayBuffer(textureBuffer.buffer, textureBuffer.currentPosition, false)
            }

            if (!bool1) {
                this.updateRequired = true
            }
        }
    }

    fun upload(checkFlags: Boolean) {
        val colorsBufferLocal = this.colorsBuffer
        val normalsBufferLocal = this.normalsBuffer
        val vertexBufferLocal = this.vertexBuffer
        val textureCoordinatesBufferLocal = this.textureCoordinatesBuffer

        var hasColors = colorsBufferLocal != null && colorsBufferLocal.buffer == null
        var hasNormals = normalsBufferLocal != null && normalsBufferLocal.buffer == null
        var hasVertices = vertexBufferLocal != null && vertexBufferLocal.buffer == null
        var hasTextures = textureCoordinatesBufferLocal != null && textureCoordinatesBufferLocal.buffer == null
        if (checkFlags) {
            hasColors = hasColors && (this.renderState.toInt() and 0x2) != 0
            hasNormals = hasNormals && (this.renderState.toInt() and 0x4) != 0
            hasVertices = hasVertices && (this.renderState.toInt() and 0x1) != 0
            hasTextures = hasTextures && (this.renderState.toInt() and 0x8) != 0
        }

        var entrySize = 0
        val verticesOffset = 0
        var colorsOffset = 0
        var normalsOffset = 0
        var texturesOffset = 0
        if (hasVertices) {
            entrySize += 12
        }

        if (hasColors) {
            colorsOffset = entrySize
            entrySize += 4
        }

        if (hasNormals) {
            normalsOffset = entrySize
            entrySize += 12
        }

        if (hasTextures) {
            texturesOffset = entrySize
            entrySize += 8
        }

        if (entrySize != 0) {
            if (this.toolkit.textureBuffer.buffer.size < entrySize * this.numNormals) {
                this.toolkit.textureBuffer = GlBitBuffer((this.numNormals + 100) * entrySize)
            } else {
                this.toolkit.textureBuffer.currentPosition = 0
            }

            val bitBuffer = this.toolkit.textureBuffer
            if (hasVertices) {
                val verticesXLocal = this.verticesX
                val verticesYLocal = this.verticesY
                val verticesZLocal = this.verticesZ
                val trianglesLocal = this.triangles
                val shortArrayLocal = this.aShortArray8288

                if (verticesXLocal == null || verticesYLocal == null || verticesZLocal == null ||
                    trianglesLocal == null || shortArrayLocal == null) {
                    return
                }

                if (this.toolkit.bigEndianess) {
                    for (vertice in 0 until this.maxDepth) {
                        val vertexX = Stream.floatToRawIntBits(verticesXLocal[vertice].toFloat())
                        val vertexY = Stream.floatToRawIntBits(verticesYLocal[vertice].toFloat())
                        val vertexZ = Stream.floatToRawIntBits(verticesZLocal[vertice].toFloat())
                        val i_16 = trianglesLocal[vertice]
                        val i_17 = trianglesLocal[vertice + 1]

                        for (i_18 in i_16 until i_17) {
                            val i_19 = shortArrayLocal[i_18] - 1
                            if (i_19 == -1) {
                                break
                            }

                            bitBuffer.currentPosition = i_19 * entrySize
                            bitBuffer.writeInt(vertexX)
                            bitBuffer.writeInt(vertexY)
                            bitBuffer.writeInt(vertexZ)
                        }
                    }
                } else {
                    for (v in 0 until this.maxDepth) {
                        val vertexX = Stream.floatToRawIntBits(verticesXLocal[v].toFloat())
                        val vertexY = Stream.floatToRawIntBits(verticesYLocal[v].toFloat())
                        val vertexZ = Stream.floatToRawIntBits(verticesZLocal[v].toFloat())
                        val i_16 = trianglesLocal[v]
                        val i_17 = trianglesLocal[v + 1]

                        for (i_18 in i_16 until i_17) {
                            val i_19 = (shortArrayLocal[i_18].toInt() and 0xffff) - 1
                            if (i_19 == -1) {
                                break
                            }

                            bitBuffer.currentPosition = i_19 * entrySize
                            bitBuffer.writeIntLittle(vertexX)
                            bitBuffer.writeIntLittle(vertexY)
                            bitBuffer.writeIntLittle(vertexZ)
                        }
                    }
                }
            }

            var f_20: Float
            var shorts_34: ShortArray?
            var shorts_35: ShortArray?
            var shorts_36: ShortArray?
            var bytes_37: ByteArray?
            var f_38: Float
            var f_39: Float
            if (hasColors) {
                val facesColorLocal = this.facesColor
                val facesMaterialLocal = this.facesMaterial
                val facesAlphaLocal = this.facesAlpha
                val facesALocal = this.facesA
                val facesBLocal = this.facesB
                val facesCLocal = this.facesC

                if (facesColorLocal == null || facesMaterialLocal == null || facesAlphaLocal == null ||
                    facesALocal == null || facesBLocal == null || facesCLocal == null) {
                    return
                }

                if (normalsBufferLocal == null) {
                    val normalsClass = this.aClass1668259
                    if (normalsClass != null) {
                        shorts_34 = normalsClass.normalsX
                        shorts_35 = normalsClass.normalsY
                        shorts_36 = normalsClass.normalsZ
                        bytes_37 = normalsClass.magnitude
                    } else {
                        shorts_34 = this.normalsX
                        shorts_35 = this.normalsY
                        shorts_36 = this.normalsZ
                        bytes_37 = this.magnitudes
                    }

                    val sunDirection = this.toolkit.sunDirection
                    if (sunDirection == null || shorts_34 == null || shorts_35 == null ||
                        shorts_36 == null || bytes_37 == null) {
                        return
                    }

                    f_38 = sunDirection[0]
                    f_39 = sunDirection[1]
                    val f_40 = sunDirection[2]
                    val f_41 = this.toolkit.sunBrightness
                    f_20 = this.toolkit.sunColorIntensity * 768.0f / this.contrast
                    val f_21 = this.toolkit.antiSunColorIntensity * 768.0f / this.contrast

                    for (i_22 in 0 until this.numFaces) {
                        val i_23 = hslToRgb(facesColorLocal[i_22], facesMaterialLocal[i_22], this.ambient, facesAlphaLocal[i_22])
                        val f_24 = (i_23 ushr 24) * this.toolkit.sunColorRed
                        val f_25 = ((i_23 shr 16) and 0xff) * this.toolkit.sunColorGreen
                        val f_26 = ((i_23 shr 8) and 0xff) * this.toolkit.sunColorBlue

                        var s_27 = facesALocal[i_22]
                        var s_28 = bytes_37[s_27.toInt()].toInt()
                        var f_29: Float
                        if (s_28 == 0) {
                            f_29 = (f_38 * shorts_34[s_27.toInt()] + f_39 * shorts_35[s_27.toInt()] + f_40 * shorts_36[s_27.toInt()]) * 0.0026041667f
                        } else {
                            f_29 = (f_38 * shorts_34[s_27.toInt()] + f_39 * shorts_35[s_27.toInt()] + f_40 * shorts_36[s_27.toInt()]) / (s_28 * 256)
                        }

                        var f_30 = f_41 + f_29 * (if (f_29 < 0.0f) f_21 else f_20)
                        var i_31 = (f_24 * f_30).toInt()
                        if (i_31 < 0) {
                            i_31 = 0
                        } else if (i_31 > 255) {
                            i_31 = 255
                        }

                        var i_32 = (f_25 * f_30).toInt()
                        if (i_32 < 0) {
                            i_32 = 0
                        } else if (i_32 > 255) {
                            i_32 = 255
                        }

                        var i_33 = (f_26 * f_30).toInt()
                        if (i_33 < 0) {
                            i_33 = 0
                        } else if (i_33 > 255) {
                            i_33 = 255
                        }

                        bitBuffer.currentPosition = colorsOffset + s_27 * entrySize
                        bitBuffer.writeByte(i_31)
                        bitBuffer.writeByte(i_32)
                        bitBuffer.writeByte(i_33)
                        bitBuffer.writeByte(255 - (facesAlphaLocal[i_22].toInt() and 0xff))
                        s_27 = facesBLocal[i_22]
                        s_28 = bytes_37[s_27.toInt()].toInt()
                        if (s_28 == 0) {
                            f_29 = (f_38 * shorts_34[s_27.toInt()] + f_39 * shorts_35[s_27.toInt()] + f_40 * shorts_36[s_27.toInt()]) * 0.0026041667f
                        } else {
                            f_29 = (f_38 * shorts_34[s_27.toInt()] + f_39 * shorts_35[s_27.toInt()] + f_40 * shorts_36[s_27.toInt()]) / (s_28 * 256)
                        }

                        f_30 = f_41 + f_29 * (if (f_29 < 0.0f) f_21 else f_20)
                        i_31 = (f_24 * f_30).toInt()
                        if (i_31 < 0) {
                            i_31 = 0
                        } else if (i_31 > 255) {
                            i_31 = 255
                        }

                        i_32 = (f_25 * f_30).toInt()
                        if (i_32 < 0) {
                            i_32 = 0
                        } else if (i_32 > 255) {
                            i_32 = 255
                        }

                        i_33 = (f_26 * f_30).toInt()
                        if (i_33 < 0) {
                            i_33 = 0
                        } else if (i_33 > 255) {
                            i_33 = 255
                        }

                        bitBuffer.currentPosition = colorsOffset + s_27 * entrySize
                        bitBuffer.writeByte(i_31)
                        bitBuffer.writeByte(i_32)
                        bitBuffer.writeByte(i_33)
                        bitBuffer.writeByte(255 - (facesAlphaLocal[i_22].toInt() and 0xff))
                        s_27 = facesCLocal[i_22]
                        s_28 = bytes_37[s_27.toInt()].toInt()
                        if (s_28 == 0) {
                            f_29 = (f_38 * shorts_34[s_27.toInt()] + f_39 * shorts_35[s_27.toInt()] + f_40 * shorts_36[s_27.toInt()]) * 0.0026041667f
                        } else {
                            f_29 = (f_38 * shorts_34[s_27.toInt()] + f_39 * shorts_35[s_27.toInt()] + f_40 * shorts_36[s_27.toInt()]) / (s_28 * 256)
                        }

                        f_30 = f_41 + f_29 * (if (f_29 < 0.0f) f_21 else f_20)
                        i_31 = (f_24 * f_30).toInt()
                        if (i_31 < 0) {
                            i_31 = 0
                        } else if (i_31 > 255) {
                            i_31 = 255
                        }

                        i_32 = (f_25 * f_30).toInt()
                        if (i_32 < 0) {
                            i_32 = 0
                        } else if (i_32 > 255) {
                            i_32 = 255
                        }

                        i_33 = (f_26 * f_30).toInt()
                        if (i_33 < 0) {
                            i_33 = 0
                        } else if (i_33 > 255) {
                            i_33 = 255
                        }

                        bitBuffer.currentPosition = colorsOffset + s_27 * entrySize
                        bitBuffer.writeByte(i_31)
                        bitBuffer.writeByte(i_32)
                        bitBuffer.writeByte(i_33)
                        bitBuffer.writeByte(255 - (facesAlphaLocal[i_22].toInt() and 0xff))
                    }
                } else {
                    for (vertice in 0 until this.numFaces) {
                        val vvvv = hslToRgb(facesColorLocal[vertice], facesMaterialLocal[vertice], this.ambient, facesAlphaLocal[vertice])
                        bitBuffer.currentPosition = colorsOffset + entrySize * (facesALocal[vertice].toInt() and 0xffff)
                        bitBuffer.writeInt(vvvv)
                        bitBuffer.currentPosition = colorsOffset + entrySize * (facesBLocal[vertice].toInt() and 0xffff)
                        bitBuffer.writeInt(vvvv)
                        bitBuffer.currentPosition = colorsOffset + entrySize * (facesCLocal[vertice].toInt() and 0xffff)
                        bitBuffer.writeInt(vvvv)
                    }
                }
            }

            if (hasNormals) {
                val normalsClass = this.aClass1668259
                if (normalsClass != null) {
                    shorts_34 = normalsClass.normalsX
                    shorts_35 = normalsClass.normalsY
                    shorts_36 = normalsClass.normalsZ
                    bytes_37 = normalsClass.magnitude
                } else {
                    shorts_34 = this.normalsX
                    shorts_35 = this.normalsY
                    shorts_36 = this.normalsZ
                    bytes_37 = this.magnitudes
                }

                if (shorts_34 == null || shorts_35 == null || shorts_36 == null || bytes_37 == null) {
                    return
                }

                f_38 = 3.0f / this.contrast
                f_39 = 3.0f / (this.contrast / 2 + this.contrast)
                bitBuffer.currentPosition = normalsOffset
                if (this.toolkit.bigEndianess) {
                    for (i_18 in 0 until this.numNormals) {
                        val i_19 = bytes_37[i_18].toInt() and 0xff
                        if (i_19 == 0) {
                            bitBuffer.writeFloatBigEndian(shorts_34[i_18] * f_39)
                            bitBuffer.writeFloatBigEndian(shorts_35[i_18] * f_39)
                            bitBuffer.writeFloatBigEndian(shorts_36[i_18] * f_39)
                        } else {
                            f_20 = f_38 / i_19
                            bitBuffer.writeFloatBigEndian(shorts_34[i_18] * f_20)
                            bitBuffer.writeFloatBigEndian(shorts_35[i_18] * f_20)
                            bitBuffer.writeFloatBigEndian(shorts_36[i_18] * f_20)
                        }

                        bitBuffer.currentPosition += entrySize - 12
                    }
                } else {
                    for (i_18 in 0 until this.numNormals) {
                        val i_19 = bytes_37[i_18].toInt() and 0xff
                        if (i_19 == 0) {
                            bitBuffer.writeFloatLittleEndian(shorts_34[i_18] * f_39)
                            bitBuffer.writeFloatLittleEndian(shorts_35[i_18] * f_39)
                            bitBuffer.writeFloatLittleEndian(shorts_36[i_18] * f_39)
                        } else {
                            f_20 = f_38 / i_19
                            bitBuffer.writeFloatLittleEndian(shorts_34[i_18] * f_20)
                            bitBuffer.writeFloatLittleEndian(shorts_35[i_18] * f_20)
                            bitBuffer.writeFloatLittleEndian(shorts_36[i_18] * f_20)
                        }

                        bitBuffer.currentPosition += entrySize - 12
                    }
                }
            }

            if (hasTextures) {
                val textureULocal = this.textureU
                val textureVLocal = this.textureV
                if (textureULocal == null || textureVLocal == null) {
                    return
                }

                bitBuffer.currentPosition = texturesOffset
                if (this.toolkit.bigEndianess) {
                    for (vertice in 0 until this.numNormals) {
                        bitBuffer.writeFloatBigEndian(textureULocal[vertice])
                        bitBuffer.writeFloatBigEndian(textureVLocal[vertice])
                        bitBuffer.currentPosition += entrySize - 8
                    }
                } else {
                    for (vertice in 0 until this.numNormals) {
                        bitBuffer.writeFloatLittleEndian(textureULocal[vertice])
                        bitBuffer.writeFloatLittleEndian(textureVLocal[vertice])
                        bitBuffer.currentPosition += entrySize - 8
                    }
                }
            }

            bitBuffer.currentPosition = entrySize * this.numNormals
            val buffer: ArrayBufferGL?
            if (checkFlags) {
                val existingBuffer = this.anInterface148305
                if (existingBuffer == null) {
                    this.anInterface148305 = this.toolkit.createArrayBuffer(entrySize, bitBuffer.buffer, bitBuffer.currentPosition, true)
                } else {
                    existingBuffer.putArray(entrySize, bitBuffer.buffer, bitBuffer.currentPosition)
                }

                buffer = this.anInterface148305
                this.renderState = 0
            } else {
                buffer = this.toolkit.createArrayBuffer(entrySize, bitBuffer.buffer, bitBuffer.currentPosition, false)
                this.updateRequired = true
            }

            if (hasVertices) {
                vertexBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = verticesOffset.toByte()
                }
            }

            if (hasTextures) {
                textureCoordinatesBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = texturesOffset.toByte()
                }
            }

            if (hasColors) {
                colorsBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = colorsOffset.toByte()
                }
            }

            if (hasNormals) {
                normalsBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = normalsOffset.toByte()
                }
            }
        }
    }

    override fun uploadToGpu() {
        if (this.numNormals > 0 && this.anInt8302 > 0) {
            upload(false)
            val elementsBuffer = this.modelElementsBuffer
            if ((this.renderState.toInt() and 0x10) == 0 && elementsBuffer != null && elementsBuffer.openglElementsBuffer == null) {
                method13536(false)
            }

            method13525()
        }
    }

    override fun awaitLock() {
        while (this.isLocked) {
            // Wait for lock to be released
        }
    }

    override fun unlock() {
        this.isLocked = false
    }

    override fun calculateProjection(camX: Int, camY: Int, modelMatrix: Matrix43, notRendered: Boolean, sizeShift: Int): Boolean {
        val matrix446 = this.toolkit.tempModelMatrix
        matrix446.fromMatrix(modelMatrix)
        matrix446.multiplyBy(this.toolkit.viewProjectionMatrix)
        var bool7 = false
        var i_8 = Int.MAX_VALUE
        var i_9 = Int.MIN_VALUE
        var i_10 = Int.MAX_VALUE
        var i_11 = Int.MIN_VALUE
        if (!this.boundsCalculated) {
            calculateBounds()
        }

        val centerX = (this.maxX - this.minX) shr 1
        val centerY = (this.maxY - this.minY) shr 1
        val centerZ = (this.maxZ - this.minZ) shr 1
        val i_15 = centerX + this.minX
        val i_16 = centerY + this.minY
        val i_17 = centerZ + this.minZ
        val i_18 = i_15 - (centerX shl sizeShift)
        val i_19 = i_16 - (centerY shl sizeShift)
        val i_20 = i_17 - (centerZ shl sizeShift)
        val i_21 = i_15 + (centerX shl sizeShift)
        val i_22 = i_16 + (centerY shl sizeShift)
        val i_23 = i_17 + (centerZ shl sizeShift)
        matrixX[0] = i_18
        matrixY[0] = i_19
        matrixZ[0] = i_20
        matrixX[1] = i_21
        matrixY[1] = i_19
        matrixZ[1] = i_20
        matrixX[2] = i_18
        matrixY[2] = i_22
        matrixZ[2] = i_20
        matrixX[3] = i_21
        matrixY[3] = i_22
        matrixZ[3] = i_20
        matrixX[4] = i_18
        matrixY[4] = i_19
        matrixZ[4] = i_23
        matrixX[5] = i_21
        matrixY[5] = i_19
        matrixZ[5] = i_23
        matrixX[6] = i_18
        matrixY[6] = i_22
        matrixZ[6] = i_23
        matrixX[7] = i_21
        matrixY[7] = i_22
        matrixZ[7] = i_23

        for (i_24 in 0 until 8) {
            val i_39 = matrixX[i_24]
            val i_26 = matrixY[i_24]
            val i_27 = matrixZ[i_24]
            val f_28 = matrix446.elements!![2] * i_39 + matrix446.elements!![6] * i_26 + matrix446.elements!![10] * i_27 + matrix446.elements!![14]
            val f_29 = matrix446.elements!![3] * i_39 + matrix446.elements!![7] * i_26 + matrix446.elements!![11] * i_27 + matrix446.elements!![15]
            if (f_28 >= -f_29) {
                val f_30 = matrix446.elements!![0] * i_39 + matrix446.elements!![4] * i_26 + matrix446.elements!![8] * i_27 + matrix446.elements!![12]
                val f_31 = matrix446.elements!![1] * i_39 + matrix446.elements!![5] * i_26 + matrix446.elements!![9] * i_27 + matrix446.elements!![13]
                val i_32 = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * f_30 / f_29).toInt()
                val i_33 = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * f_31 / f_29).toInt()
                if (i_32 < i_8) {
                    i_8 = i_32
                }

                if (i_32 > i_9) {
                    i_9 = i_32
                }

                if (i_33 < i_10) {
                    i_10 = i_33
                }

                if (i_33 > i_11) {
                    i_11 = i_33
                }

                bool7 = true
            }
        }

        if (bool7 && camX > i_8 && camX < i_9 && camY > i_10 && camY < i_11) {
            if (notRendered) {
                return true
            }

            if (this.toolkit.projectedVerticesX!!.size < this.numNormals) {
                this.toolkit.projectedVerticesX = IntArray(this.numNormals)
                this.toolkit.projectedVerticesY = IntArray(this.numNormals)
            }

            val projectedVerticesX = this.toolkit.projectedVerticesX!!
            val projectedVerticesY = this.toolkit.projectedVerticesY!!

            for (i_26 in 0 until this.maxDepth) {
                val i_27 = this.verticesX!![i_26]
                val i_40 = this.verticesY!![i_26]
                val i_41 = this.verticesZ!![i_26]
                val f_30 = matrix446.elements!![2] * i_27 + matrix446.elements!![6] * i_40 + matrix446.elements!![10] * i_41 + matrix446.elements!![14]
                val f_31 = matrix446.elements!![3] * i_27 + matrix446.elements!![7] * i_40 + matrix446.elements!![11] * i_41 + matrix446.elements!![15]
                if (f_30 >= -f_31) {
                    val f_42 = matrix446.elements!![0] * i_27 + matrix446.elements!![4] * i_40 + matrix446.elements!![8] * i_41 + matrix446.elements!![12]
                    val f_43 = matrix446.elements!![1] * i_27 + matrix446.elements!![5] * i_40 + matrix446.elements!![9] * i_41 + matrix446.elements!![13]
                    val i_34 = this.triangles!![i_26]
                    val i_35 = this.triangles!![i_26 + 1]

                    for (i_36 in i_34 until i_35) {
                        val i_37 = this.aShortArray8288!![i_36] - 1
                        if (i_37 == -1) {
                            break
                        }

                        projectedVerticesX[i_37] = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * f_42 / f_31).toInt()
                        projectedVerticesY[i_37] = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * f_43 / f_31).toInt()
                    }
                } else {
                    val i_32 = this.triangles!![i_26]
                    val i_33 = this.triangles!![i_26 + 1]

                    for (i_34 in i_32 until i_33) {
                        val i_35 = this.aShortArray8288!![i_34] - 1
                        if (i_35 == -1) {
                            break
                        }

                        projectedVerticesX[this.aShortArray8288!![i_34] - 1] = -999999
                    }
                }
            }

            for (i_26 in 0 until this.numFaces) {
                if (projectedVerticesX[this.facesA!![i_26].toInt()] != -999999 && projectedVerticesX[this.facesB!![i_26].toInt()] != -999999 && projectedVerticesX[this.facesC!![i_26].toInt()] != -999999 && isHovering(camX, camY, projectedVerticesY[this.facesA!![i_26].toInt()], projectedVerticesY[this.facesB!![i_26].toInt()], projectedVerticesY[this.facesC!![i_26].toInt()], projectedVerticesX[this.facesA!![i_26].toInt()], projectedVerticesX[this.facesB!![i_26].toInt()], projectedVerticesX[this.facesC!![i_26].toInt()])) {
                    return true
                }
            }
        }

        return false
    }

    fun isHovering(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int): Boolean {
        return (i_2 >= i_3 || i_2 >= i_4 || i_2 >= i_5) && ((i_2 <= i_3 || i_2 <= i_4 || i_2 <= i_5) && ((i_1 >= i_6 || i_1 >= i_7 || i_1 >= i_8) && (i_1 <= i_6 || i_1 <= i_7 || i_1 <= i_8)))
    }

    override fun drawMatrixThreaded(projectionMatrix: Matrix43, boundingBox: BoundingBoxPolygon?, rasterizerFunctionMask: Int) {
        if ((rasterizerFunctionMask and 0x2) != 0) {
            OpenGL.glPolygonMode(1032, 6913)
        }

        if (this.numNormals != 0) {
            val matrix444 = this.toolkit.currentModelMatrix
            val matrix445 = this.toolkit.tempModelMatrix
            val matrix446 = this.toolkit.tempProjectionMatrix
            matrix445.fromMatrix(projectionMatrix)
            matrix446.copyFrom(matrix445)
            matrix446.multiplyBy(this.toolkit.viewProjectionMatrix)
            if (!this.boundsCalculated) {
                calculateBounds()
            }

            val floats_7 = this.toolkit.lightParameterBuffer
            matrix445.scale(0.0f, this.minY.toFloat(), 0.0f, floats_7)
            val f_8 = floats_7[0]
            val f_9 = floats_7[1]
            val f_10 = floats_7[2]
            matrix445.scale(0.0f, this.maxY.toFloat(), 0.0f, floats_7)
            val f_11 = floats_7[0]
            val f_12 = floats_7[1]
            val f_13 = floats_7[2]

            for (i_14 in 0 until 6) {
                val floats_33 = this.toolkit.frustumPlanes!![i_14]
                val f_16 = floats_33[0] * f_8 + floats_33[1] * f_9 + floats_33[2] * f_10 + floats_33[3] + this.size2d
                val f_17 = floats_33[0] * f_11 + floats_33[1] * f_12 + floats_33[2] * f_13 + floats_33[3] + this.size2d
                if (f_16 < 0.0f && f_17 < 0.0f) {
                    return
                }
            }

            if (boundingBox != null) {
                var bool34 = false
                var bool15 = true
                val i_35 = (this.minX + this.maxX) shr 1
                val i_36 = (this.minZ + this.maxZ) shr 1
                var s_19 = this.minY
                var f_21 = matrix446.elements!![0] * i_35 + matrix446.elements!![4] * s_19 + matrix446.elements!![8] * i_36 + matrix446.elements!![12]
                var f_22 = matrix446.elements!![1] * i_35 + matrix446.elements!![5] * s_19 + matrix446.elements!![9] * i_36 + matrix446.elements!![13]
                var f_23 = matrix446.elements!![2] * i_35 + matrix446.elements!![6] * s_19 + matrix446.elements!![10] * i_36 + matrix446.elements!![14]
                var f_24 = matrix446.elements!![3] * i_35 + matrix446.elements!![7] * s_19 + matrix446.elements!![11] * i_36 + matrix446.elements!![15]
                if (f_23 >= -f_24) {
                    boundingBox.left = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * f_21 / f_24).toInt()
                    boundingBox.bottom = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * f_22 / f_24).toInt()
                } else {
                    bool34 = true
                }

                s_19 = this.maxY
                val f_25 = matrix446.elements!![0] * i_35 + matrix446.elements!![4] * s_19 + matrix446.elements!![8] * i_36 + matrix446.elements!![12]
                val f_26 = matrix446.elements!![1] * i_35 + matrix446.elements!![5] * s_19 + matrix446.elements!![9] * i_36 + matrix446.elements!![13]
                val f_27 = matrix446.elements!![2] * i_35 + matrix446.elements!![6] * s_19 + matrix446.elements!![10] * i_36 + matrix446.elements!![14]
                val f_28 = matrix446.elements!![3] * i_35 + matrix446.elements!![7] * s_19 + matrix446.elements!![11] * i_36 + matrix446.elements!![15]
                if (f_27 >= -f_28) {
                    boundingBox.right = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * f_25 / f_28).toInt()
                    boundingBox.top = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * f_26 / f_28).toInt()
                } else {
                    bool34 = true
                }

                var f_29: Float
                var f_30: Float
                if (bool34) {
                    if (f_23 < -f_24 && f_27 < -f_28) {
                        bool15 = false
                    } else {
                        var f_31: Float
                        var f_32: Float
                        if (f_23 < -f_24) {
                            f_29 = (f_23 + f_24) / (f_27 + f_28) - 1.0f
                            f_30 = f_21 + f_29 * (f_25 - f_21)
                            f_31 = f_22 + f_29 * (f_26 - f_22)
                            f_32 = f_24 + f_29 * (f_28 - f_24)
                            boundingBox.left = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * f_30 / f_32).toInt()
                            boundingBox.bottom = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * f_31 / f_32).toInt()
                        } else if (f_27 < -f_28) {
                            f_29 = (f_27 + f_28) / (f_23 + f_24) - 1.0f
                            f_30 = f_25 + f_29 * (f_21 - f_25)
                            f_31 = f_26 + f_29 * (f_22 - f_26)
                            f_32 = f_28 + f_29 * (f_24 - f_28)
                            boundingBox.right = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * f_30 / f_32).toInt()
                            boundingBox.top = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * f_31 / f_32).toInt()
                        }
                    }
                }

                if (bool15) {
                    if (f_23 / f_24 > f_27 / f_28) {
                        f_29 = f_21 + matrix444.elements!![0] * this.size2d + matrix444.elements!![12]
                        f_30 = f_24 + matrix444.elements!![3] * this.size2d + matrix444.elements!![15]
                        boundingBox.size = (this.toolkit.viewportOriginX - boundingBox.left + this.toolkit.imageProducerCenterX * f_29 / f_30).toInt()
                    } else {
                        f_29 = f_25 + matrix444.elements!![0] * this.size2d + matrix444.elements!![12]
                        f_30 = f_28 + matrix444.elements!![3] * this.size2d + matrix444.elements!![15]
                        boundingBox.size = (this.toolkit.viewportOriginX - boundingBox.right + this.toolkit.imageProducerCenterX * f_29 / f_30).toInt()
                    }

                    boundingBox.positionCalculated = true
                }
            }

            this.toolkit.setupMatrixRendering()
            this.toolkit.multiMatrix(matrix445)
            method13534()
            this.toolkit.popMatrix()
            matrix445.multiplyBy(this.toolkit.viewMatrix44)
            method13535(matrix445)
            if ((rasterizerFunctionMask and 0x2) != 0) {
                OpenGL.glPolygonMode(1032, 6914)  // Reset GL_FRONT_AND_BACK to GL_FILL
            }
        }
    }

    override fun translate(x: Int, y: Int, z: Int) {
        for (i in 0 until this.maxDepth) {
            this.verticesX!![i] += x
            this.verticesY!![i] += y
            this.verticesZ!![i] += z
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun mirror() {
        for (i in 0 until this.maxDepth) {
            this.verticesZ!![i] = -this.verticesZ!![i]
        }

        for (i in 0 until this.numNormals) {
            this.normalsZ!![i] = (-this.normalsZ!![i].toInt()).toShort()
        }

        if (this.aClass1668259 != null) {
            for (i in 0 until this.numNormals) {
                this.aClass1668259!!.normalsZ!![i] = (-this.aClass1668259!!.normalsZ!![i].toInt()).toShort()
            }
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun scale(x: Int, y: Int, z: Int) {
        for (i in 0 until this.maxDepth) {
            this.verticesX!![i] = (this.verticesX!![i] * x) shr 7
            this.verticesY!![i] = (this.verticesY!![i] * y) shr 7
            this.verticesZ!![i] = (this.verticesZ!![i] * z) shr 7
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun setContrast(contrast: Int) {
        this.contrast = contrast.toShort()
        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
        if (this.normalsBuffer != null) {
            this.normalsBuffer!!.buffer = null
        }
    }

    override fun recolor(src: Short, dst: Short) {
        for (face in 0 until this.numFaces) {
            if (this.facesColor!![face] == src) {
                this.facesColor!![face] = dst
            }
        }

        if (this.billboardEffects != null) {
            for (billboard in 0 until this.billboardCount) {
                val effect = this.billboardEffects!![billboard]!!
                val surface = this.billboardSurfaces!![billboard]!!
                surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    override fun rematerial(src: Short, dst: Short) {
        val provider = this.toolkit.materialProvider!!

        for (face in 0 until this.numFaces) {
            if (this.facesMaterial!![face] == src) {
                this.facesMaterial!![face] = dst
            }
        }

        var srcAlpha: Byte = 0
        var srcBrightness: Byte = 0
        if (src.toInt() != -1) {
            val srcMaterial = provider.getMaterial(src.toInt() and 0xffff)
            srcAlpha = srcMaterial.alpha
            srcBrightness = srcMaterial.brightness
        }

        var dstAlpha: Byte = 0
        var dstBrightness: Byte = 0
        if (dst.toInt() != -1) {
            val dstMaterial = provider.getMaterial(dst.toInt() and 0xffff)
            dstAlpha = dstMaterial.alpha
            dstBrightness = dstMaterial.brightness
            if (dstMaterial.textureSpeedU.toInt() != 0 || dstMaterial.textureSpeedV.toInt() != 0) {
                this.textureAnimating = true
            }
        }

        if ((srcAlpha != dstAlpha) or (srcBrightness != dstBrightness)) {
            if (this.billboardEffects != null) {
                for (billboard in 0 until this.billboardCount) {
                    val effect = this.billboardEffects!![billboard]!!
                    val surface = this.billboardSurfaces!![billboard]!!
                    surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                }
            }

            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }
        }
    }

    override fun tint(hue: Int, saturation: Int, lightness: Int, opacity: Int) {
        for (i in 0 until this.numFaces) {
            val hsl = this.facesColor!![i].toInt() and 0xffff
            var h = (hsl shr 10) and 0x3f
            var s = (hsl shr 7) and 0x7
            var l = hsl and 0x7f
            if (hue != -1) {
                h += (hue - h) * opacity shr 7
            }
            if (saturation != -1) {
                s += opacity * (saturation - s) shr 7
            }
            if (lightness != -1) {
                l += (lightness - l) * opacity shr 7
            }
            this.facesColor!![i] = ((h shl 10) or (s shl 7) or l).toShort()
        }

        if (this.billboardEffects != null) {
            for (billboard in 0 until this.billboardCount) {
                val effect = this.billboardEffects!![billboard]!!
                val surface = this.billboardSurfaces!![billboard]!!
                surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    override fun setAmbient(ambient: Int) {
        this.ambient = ambient.toShort()
        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    override fun applyEnvironmentAlpha(alpha: Byte, transparency: ByteArray?) {
        if (transparency == null) {
            for (f in 0 until this.numFaces) {
                this.facesAlpha!![f] = alpha
            }
        } else {
            for (f in 0 until this.numFaces) {
                val antiAlpha = 255 - (255 - (alpha.toInt() and 0xff)) * (255 - (transparency[f].toInt() and 0xff)) / 255
                this.facesAlpha!![f] = antiAlpha.toByte()
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    fun hslToRgb(colorHsl: Short, materialId: Short, ambient: Short, alpha: Byte): Int {
        var i_5 = ColorUtil.hslToRgb!![method13538(colorHsl.toInt(), ambient.toInt())]
        if (materialId.toInt() != -1) {
            val materialType = this.toolkit.materialProvider!!.getMaterial(materialId.toInt() and 0xffff)
            val a = materialType.alpha.toInt() and 0xff
            var i_10: Int
            if (a != 0) {
                val i_8: Int
                if (ambient < 0) {
                    i_8 = 0
                } else if (ambient > 127) {
                    i_8 = 16777215
                } else {
                    i_8 = ambient.toInt() * 131586
                }

                i_10 = 256 - a
                i_5 = (((i_8 and 0xff00ff) * a + (i_5 and 0xff00ff) * i_10 and -16711936) + (a * (i_8 and 0xff00) + i_10 * (i_5 and 0xff00) and 0xff0000)) shr 8
            }

            val b = (materialType.brightness.toInt() and 0xff)
            if (b != 0) {
                val brightness = b + 256
                var i_9 = brightness * ((i_5 and 0xff0000) shr 16)
                if (i_9 > 65535) {
                    i_9 = 65535
                }

                i_10 = brightness * ((i_5 and 0xff00) shr 8)
                if (i_10 > 65535) {
                    i_10 = 65535
                }

                var i_11 = (i_5 and 0xff) * brightness
                if (i_11 > 65535) {
                    i_11 = 65535
                }

                i_5 = (i_11 shr 8) + (i_10 and 0xff00) + ((i_9 shl 8) and 0xff0000)
            }
        }

        return (i_5 shl 8) or (255 - (alpha.toInt() and 0xff))
    }

    override fun createShadowSprite(shadow: Shadow?): Shadow? {
        if (shadow == null) {
            return null
        }
        val shadowGL = shadow as ShadowGL
        method13522(shadowGL)
        return shadowGL
    }

    override fun applyShading(model: Model, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        val openglModel = model as ModelGL
        if (this.numFaces != 0 && openglModel.numFaces != 0) {
            val glMaxDepth = openglModel.maxDepth
            val vertX = openglModel.verticesX
            val vertY = openglModel.verticesY
            val vertZ = openglModel.verticesZ
            val normX = openglModel.normalsX
            val normY = openglModel.normalsY
            val normZ = openglModel.normalsZ
            val normMag = openglModel.magnitudes
            var shorts_15: ShortArray?
            var shorts_16: ShortArray?
            var shorts_17: ShortArray?
            var bytes_18: ByteArray?
            if (this.aClass1668259 != null) {
                shorts_15 = this.aClass1668259!!.normalsX
                shorts_16 = this.aClass1668259!!.normalsY
                shorts_17 = this.aClass1668259!!.normalsZ
                bytes_18 = this.aClass1668259!!.magnitude
            } else {
                shorts_15 = null
                shorts_16 = null
                shorts_17 = null
                bytes_18 = null
            }

            var shorts_19: ShortArray?
            var shorts_20: ShortArray?
            var shorts_21: ShortArray?
            var bytes_22: ByteArray?
            if (openglModel.aClass1668259 != null) {
                shorts_19 = openglModel.aClass1668259!!.normalsX
                shorts_20 = openglModel.aClass1668259!!.normalsY
                shorts_21 = openglModel.aClass1668259!!.normalsZ
                bytes_22 = openglModel.aClass1668259!!.magnitude
            } else {
                shorts_19 = null
                shorts_20 = null
                shorts_21 = null
                bytes_22 = null
            }

            val ints_23 = openglModel.triangles
            val shorts_24 = openglModel.aShortArray8288
            if (!openglModel.boundsCalculated) {
                openglModel.calculateBounds()
            }

            val s_25 = openglModel.minY
            val s_26 = openglModel.maxY
            val s_27 = openglModel.minX
            val s_28 = openglModel.maxX
            val s_29 = openglModel.minZ
            val s_30 = openglModel.maxZ

            for (i_31 in 0 until this.maxDepth) {
                val i_32 = this.verticesY!![i_31] - y
                if (i_32 >= s_25 && i_32 <= s_26) {
                    val i_33 = this.verticesX!![i_31] - x
                    if (i_33 >= s_27 && i_33 <= s_28) {
                        val i_34 = this.verticesZ!![i_31] - z
                        if (i_34 >= s_29 && i_34 <= s_30) {
                            var i_35 = -1
                            var i_36 = this.triangles!![i_31]
                            var i_37 = this.triangles!![i_31 + 1]

                            var i_38 = i_36
                            while (i_38 < i_37) {
                                i_35 = this.aShortArray8288!![i_38] - 1
                                if (i_35 == -1 || this.magnitudes!![i_35].toInt() != 0) {
                                    break
                                }
                                i_38++
                            }

                            if (i_35 != -1) {
                                i_38 = 0
                                while (i_38 < glMaxDepth) {
                                    if (i_33 == vertX!![i_38] && i_34 == vertZ!![i_38] && i_32 == vertY!![i_38]) {
                                        var i_39 = -1
                                        i_36 = ints_23!![i_38]
                                        i_37 = ints_23[i_38 + 1]

                                        for (i_40 in i_36 until i_37) {
                                            i_39 = shorts_24!![i_40] - 1
                                            if (i_39 == -1 || normMag!![i_39].toInt() != 0) {
                                                break
                                            }
                                        }

                                        if (i_39 != -1) {
                                            if (shorts_15 == null) {
                                                this.aClass1668259 = ModelNormalsGL()
                                                shorts_15 = ArrayUtils.arraycopy(this.normalsX)
                                                this.aClass1668259!!.normalsX = shorts_15
                                                shorts_16 = ArrayUtils.arraycopy(this.normalsY)
                                                this.aClass1668259!!.normalsY = shorts_16
                                                shorts_17 = ArrayUtils.arraycopy(this.normalsZ)
                                                this.aClass1668259!!.normalsZ = shorts_17
                                                bytes_18 = ArrayUtils.safeFullArraycopy(this.magnitudes)
                                                this.aClass1668259!!.magnitude = bytes_18
                                            }

                                            if (shorts_19 == null) {
                                                val class16646 = ModelNormalsGL()
                                                openglModel.aClass1668259 = class16646
                                                shorts_19 = ArrayUtils.arraycopy(openglModel.normalsX)
                                                class16646.normalsX = shorts_19
                                                shorts_20 = ArrayUtils.arraycopy(openglModel.normalsY)
                                                class16646.normalsY = shorts_20
                                                shorts_21 = ArrayUtils.arraycopy(openglModel.normalsZ)
                                                class16646.normalsZ = shorts_21
                                                bytes_22 = ArrayUtils.safeFullArraycopy(openglModel.magnitudes)
                                                class16646.magnitude = bytes_22
                                            }

                                            shorts_15!![i_35] = (shorts_15[i_35] + normX!![i_39]).toShort()
                                            shorts_16!![i_35] = (shorts_16[i_35] + normY!![i_39]).toShort()
                                            shorts_17!![i_35] = (shorts_17[i_35] + normZ!![i_39]).toShort()
                                            bytes_18!![i_35] = (bytes_18[i_35] + normMag!![i_39]).toByte()
                                            shorts_19!![i_39] = (shorts_19[i_39] + this.normalsX!![i_35]).toShort()
                                            shorts_20!![i_39] = (shorts_20[i_39] + this.normalsY!![i_35]).toShort()
                                            shorts_21!![i_39] = (shorts_21[i_39] + this.normalsZ!![i_35]).toShort()
                                            bytes_22!![i_39] = (bytes_22[i_39] + this.magnitudes!![i_35]).toByte()
                                        }
                                        break
                                    }
                                    i_38++
                                }
                            }
                        }
                    }
                }
            }

            if (this.normalsBuffer != null) {
                this.normalsBuffer!!.buffer = null
            }

            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }

            if (openglModel.normalsBuffer != null) {
                openglModel.normalsBuffer!!.buffer = null
            }

            if (openglModel.colorsBuffer != null) {
                openglModel.colorsBuffer!!.buffer = null
            }
        }
    }

    override fun offsetSubmesh(var1: Matrix43, functionMask: Int, var3: Boolean) {
        if (this.verticesSubmeshes != null) {
            var matrix44var4 = var1
            if (var3) {
                matrix44var4 = this.toolkit.inverseSunMatrix43
                matrix44var4.updateMatrix(var1)
            }

            val floats_5 = FloatArray(3)

            for (i_6 in 0 until this.maxDepth) {
                if ((functionMask and this.verticesSubmeshes!![i_6].toInt()) != 0) {
                    matrix44var4.calculatePerspective(this.verticesX!![i_6].toFloat(), this.verticesY!![i_6].toFloat(), this.verticesZ!![i_6].toFloat(), floats_5)
                    this.verticesX!![i_6] = floats_5[0].toInt()
                    this.verticesY!![i_6] = floats_5[1].toInt()
                    this.verticesZ!![i_6] = floats_5[2].toInt()
                }
            }
        }
    }

    override fun updateParticlePosition(projectionMatrix: Matrix43) {
        val matrix442 = this.toolkit.tempModelMatrix
        matrix442.fromMatrix(projectionMatrix)
        if (this.emissiveTriangles != null) {
            for (i_3 in this.emissiveTriangles!!.indices) {
                val class874 = this.emissiveTriangles!![i_3]
                val class875 = if (class874.child != null) class874.child!! else class874

                class875.verticeXA = (matrix442.elements!![12] + matrix442.elements!![0] * this.verticesX!![class874.faceA] + matrix442.elements!![4] * this.verticesY!![class874.faceA] + matrix442.elements!![8] * this.verticesZ!![class874.faceA]).toInt()
                class875.verticeYA = (matrix442.elements!![13] + matrix442.elements!![1] * this.verticesX!![class874.faceA] + matrix442.elements!![5] * this.verticesY!![class874.faceA] + matrix442.elements!![9] * this.verticesZ!![class874.faceA]).toInt()
                class875.verticeZA = (matrix442.elements!![14] + matrix442.elements!![2] * this.verticesX!![class874.faceA] + matrix442.elements!![6] * this.verticesY!![class874.faceA] + matrix442.elements!![10] * this.verticesZ!![class874.faceA]).toInt()
                class875.verticeXB = (matrix442.elements!![12] + matrix442.elements!![0] * this.verticesX!![class874.faceB] + matrix442.elements!![4] * this.verticesY!![class874.faceB] + matrix442.elements!![8] * this.verticesZ!![class874.faceB]).toInt()
                class875.verticeYB = (matrix442.elements!![13] + matrix442.elements!![1] * this.verticesX!![class874.faceB] + matrix442.elements!![5] * this.verticesY!![class874.faceB] + matrix442.elements!![9] * this.verticesZ!![class874.faceB]).toInt()
                class875.verticeZB = (matrix442.elements!![14] + matrix442.elements!![2] * this.verticesX!![class874.faceB] + matrix442.elements!![6] * this.verticesY!![class874.faceB] + matrix442.elements!![10] * this.verticesZ!![class874.faceB]).toInt()
                class875.verticeXC = (matrix442.elements!![12] + matrix442.elements!![0] * this.verticesX!![class874.faceC] + matrix442.elements!![4] * this.verticesY!![class874.faceC] + matrix442.elements!![8] * this.verticesZ!![class874.faceC]).toInt()
                class875.verticeYC = (matrix442.elements!![13] + matrix442.elements!![1] * this.verticesX!![class874.faceC] + matrix442.elements!![5] * this.verticesY!![class874.faceC] + matrix442.elements!![9] * this.verticesZ!![class874.faceC]).toInt()
                class875.verticeZC = (matrix442.elements!![14] + matrix442.elements!![2] * this.verticesX!![class874.faceC] + matrix442.elements!![6] * this.verticesY!![class874.faceC] + matrix442.elements!![10] * this.verticesZ!![class874.faceC]).toInt()
            }
        }

        if (this.effectiveVertices != null) {
            for (i_3 in this.effectiveVertices!!.indices) {
                val class1726 = this.effectiveVertices!![i_3]
                val class1727 = if (class1726.child != null) class1726.child!! else class1726

                if (class1726.matrix != null) {
                    class1726.matrix!!.copyFrom(matrix442)
                } else {
                    class1726.matrix = Matrix44(matrix442)
                }

                class1727.verticeX = (matrix442.elements!![12] + matrix442.elements!![0] * this.verticesX!![class1726.appliesOnVertex] + matrix442.elements!![4] * this.verticesY!![class1726.appliesOnVertex] + matrix442.elements!![8] * this.verticesZ!![class1726.appliesOnVertex]).toInt()
                class1727.verticeY = (matrix442.elements!![13] + matrix442.elements!![1] * this.verticesX!![class1726.appliesOnVertex] + matrix442.elements!![5] * this.verticesY!![class1726.appliesOnVertex] + matrix442.elements!![9] * this.verticesZ!![class1726.appliesOnVertex]).toInt()
                class1727.verticeZ = (matrix442.elements!![14] + matrix442.elements!![2] * this.verticesX!![class1726.appliesOnVertex] + matrix442.elements!![6] * this.verticesY!![class1726.appliesOnVertex] + matrix442.elements!![10] * this.verticesZ!![class1726.appliesOnVertex]).toInt()
            }
        }
    }

    override fun getSize2d(): Int = this.size2d

    override fun getSize3d(): Int = this.size3d

    override fun getMinX(): Int = this.minX.toInt()

    override fun getMaxX(): Int = this.maxX.toInt()

    override fun getMinY(): Int = this.minY.toInt()

    override fun getMaxY(): Int = this.maxY.toInt()

    override fun getMinZ(): Int = this.minZ.toInt()

    override fun getMaxZ(): Int = this.maxZ.toInt()

    override fun getFunctionMask(): Int = this.functionMask

    override fun getAmbient(): Int = this.ambient.toInt()

    override fun getContrast(): Int = this.contrast.toInt()

    override fun getAlpha(): ByteArray? = this.facesAlpha

    override fun getEmissiveTriangles(): Array<EmissiveTriangle>? = this.emissiveTriangles

    override fun getEffectiveVertices(): Array<EffectiveVertex>? = this.effectiveVertices

    override fun areTexturesCached(): Boolean {
        if (this.facesMaterial != null) {
            for (i_1 in this.facesMaterial!!.indices) {
                if (this.facesMaterial!![i_1].toInt() != -1 && !this.toolkit.materialProvider!!.isCached(this.facesMaterial!![i_1].toInt())) {
                    return false
                }
            }
        }
        return true
    }

    override fun hasAnimatedTexture(): Boolean = this.textureAnimating

    override fun accountForLayers(): Boolean = this.hasAlphaTransition

    override fun xaxisRotateWithoutNormals(rotation: Int) {
        val sin = MathUtils.SINE[rotation]
        val cos = MathUtils.COSINE[rotation]

        for (i in 0 until this.maxDepth) {
            val y = (cos * this.verticesY!![i] - sin * this.verticesZ!![i]) shr 14
            this.verticesZ!![i] = (sin * this.verticesY!![i] + cos * this.verticesZ!![i]) shr 14
            this.verticesY!![i] = y
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun zaxisRotateWithoutNormals(rotation: Int) {
        val sin = MathUtils.SINE[rotation]
        val cos = MathUtils.COSINE[rotation]

        for (i in 0 until this.maxDepth) {
            val x = (cos * this.verticesX!![i] + sin * this.verticesY!![i]) shr 14
            this.verticesY!![i] = (cos * this.verticesY!![i] - sin * this.verticesX!![i]) shr 14
            this.verticesX!![i] = x
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun startAnimation(): Boolean {
        if (this.vertexGroups == null) {
            return false
        }
        // Scale vertices up by 16 (shift left 4) for animation precision
        for (v in 0 until this.numVertices) {
            this.verticesX!![v] = this.verticesX!![v] shl 4
            this.verticesY!![v] = this.verticesY!![v] shl 4
            this.verticesZ!![v] = this.verticesZ!![v] shl 4
        }
        originX = 0
        originY = 0
        originZ = 0
        return true
    }

    override fun endAnimation() {
        // Scale vertices back down by 16 (shift right 4) after animation
        // Adding 7 before shift provides proper rounding
        for (v in 0 until this.numVertices) {
            this.verticesX!![v] = this.verticesX!![v] + 7 shr 4
            this.verticesY!![v] = this.verticesY!![v] + 7 shr 4
            this.verticesZ!![v] = this.verticesZ!![v] + 7 shr 4
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun animatePartialtransform(transformationType: Int, labels: IntArray, xIn: Int, yIn: Int, zIn: Int, rotation: Int, lights: Boolean) {
        var x = xIn
        var y = yIn
        var z = zIn
        val labelCount = labels.size

        if (transformationType == 0) {
            x = x shl 4
            y = y shl 4
            z = z shl 4
            var translateCount = 0
            originX = 0
            originY = 0
            originZ = 0

            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![group]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        originX += this.verticesX!![v]
                        originY += this.verticesY!![v]
                        originZ += this.verticesZ!![v]
                        translateCount++
                    }
                }
            }

            if (translateCount > 0) {
                originX = x + originX / translateCount
                originY = y + originY / translateCount
                originZ = z + originZ / translateCount
            } else {
                originX = x
                originY = y
                originZ = z
            }
        } else if (transformationType == 1) {
            x = x shl 4
            y = y shl 4
            z = z shl 4

            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![group]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        this.verticesX!![v] += x
                        this.verticesY!![v] += y
                        this.verticesZ!![v] += z
                    }
                }
            }
        } else if (transformationType == 2) {
            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![group]!!
                    if ((rotation and 0x1) == 0) {
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            this.verticesX!![v] -= originX
                            this.verticesY!![v] -= originY
                            this.verticesZ!![v] -= originZ
                            if (z != 0) {
                                val sin = MathUtils.SINE[z]
                                val cos = MathUtils.COSINE[z]
                                val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            if (x != 0) {
                                val sin = MathUtils.SINE[x]
                                val cos = MathUtils.COSINE[x]
                                val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesY!![v] = tmp
                            }
                            if (y != 0) {
                                val sin = MathUtils.SINE[y]
                                val cos = MathUtils.COSINE[y]
                                val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            this.verticesX!![v] += originX
                            this.verticesY!![v] += originY
                            this.verticesZ!![v] += originZ
                        }
                    } else {
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            this.verticesX!![v] -= originX
                            this.verticesY!![v] -= originY
                            this.verticesZ!![v] -= originZ
                            if (x != 0) {
                                val sin = MathUtils.SINE[x]
                                val cos = MathUtils.COSINE[x]
                                val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesY!![v] = tmp
                            }
                            if (z != 0) {
                                val sin = MathUtils.SINE[z]
                                val cos = MathUtils.COSINE[z]
                                val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            if (y != 0) {
                                val sin = MathUtils.SINE[y]
                                val cos = MathUtils.COSINE[y]
                                val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            this.verticesX!![v] += originX
                            this.verticesY!![v] += originY
                            this.verticesZ!![v] += originZ
                        }
                    }
                }
            }

            if (lights) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < (this.vertexGroups?.size ?: 0)) {
                        val vertexGroup = this.vertexGroups!![group]!!
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            val triStart = this.triangles!![v]
                            val triEnd = this.triangles!![v + 1]
                            for (triIdx in triStart until triEnd) {
                                val n = this.aShortArray8288!![triIdx].toInt() - 1
                                if (n == -1) break
                                if (z != 0) {
                                    val sin = MathUtils.SINE[z]
                                    val cos = MathUtils.COSINE[z]
                                    val tmp = ((this.normalsY!![n] * sin + this.normalsX!![n] * cos + 16383) shr 14)
                                    this.normalsY!![n] = (((this.normalsY!![n] * cos - this.normalsX!![n] * sin + 16383) shr 14)).toShort()
                                    this.normalsX!![n] = tmp.toShort()
                                }
                                if (x != 0) {
                                    val sin = MathUtils.SINE[x]
                                    val cos = MathUtils.COSINE[x]
                                    val tmp = ((this.normalsY!![n] * cos - this.normalsZ!![n] * sin + 16383) shr 14)
                                    this.normalsZ!![n] = (((this.normalsY!![n] * sin + this.normalsZ!![n] * cos + 16383) shr 14)).toShort()
                                    this.normalsY!![n] = tmp.toShort()
                                }
                                if (y != 0) {
                                    val sin = MathUtils.SINE[y]
                                    val cos = MathUtils.COSINE[y]
                                    val tmp = ((this.normalsZ!![n] * sin + this.normalsX!![n] * cos + 16383) shr 14)
                                    this.normalsZ!![n] = (((this.normalsZ!![n] * cos - this.normalsX!![n] * sin + 16383) shr 14)).toShort()
                                    this.normalsX!![n] = tmp.toShort()
                                }
                            }
                        }
                    }
                }

                if (this.normalsBuffer == null && this.colorsBuffer != null) {
                    this.colorsBuffer!!.buffer = null
                }
                if (this.normalsBuffer != null) {
                    this.normalsBuffer!!.buffer = null
                }
            }
        } else if (transformationType == 3) {
            for (labelIndex in 0 until labelCount) {
                val group = labels[labelIndex]
                if (group < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![group]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        this.verticesX!![v] -= originX
                        this.verticesY!![v] -= originY
                        this.verticesZ!![v] -= originZ
                        this.verticesX!![v] = (x * this.verticesX!![v]) shr 7
                        this.verticesY!![v] = (y * this.verticesY!![v]) shr 7
                        this.verticesZ!![v] = (z * this.verticesZ!![v]) shr 7
                        this.verticesX!![v] += originX
                        this.verticesY!![v] += originY
                        this.verticesZ!![v] += originZ
                    }
                }
            }
        } else if (transformationType == 5) {
            if (this.faceGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.faceGroups!!.size) {
                        val faceGroup = this.faceGroups!![group]!!
                        for (i in faceGroup.indices) {
                            val face = faceGroup[i]
                            var alpha = (this.facesAlpha!![face].toInt() and 0xff) + x * 8
                            if (alpha < 0) alpha = 0
                            else if (alpha > 255) alpha = 255
                            this.facesAlpha!![face] = alpha.toByte()
                        }
                        if (faceGroup.isNotEmpty() && this.colorsBuffer != null) {
                            this.colorsBuffer!!.buffer = null
                        }
                    }
                }
                if (this.billboardEffects != null) {
                    for (b in 0 until this.billboardCount) {
                        val effect = this.billboardEffects!![b]!!
                        val surface = this.billboardSurfaces!![b]!!
                        surface.diffuseColor = (surface.diffuseColor and 0xffffff) or ((255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff)) shl 24)
                    }
                }
            }
        } else if (transformationType == 7) {
            if (this.faceGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.faceGroups!!.size) {
                        val faceGroup = this.faceGroups!![group]!!
                        for (i in faceGroup.indices) {
                            val face = faceGroup[i]
                            val hsl = this.facesColor!![face].toInt() and 0xffff
                            var h = (hsl shr 10) and 0x3f
                            var s = (hsl shr 7) and 0x7
                            var l = hsl and 0x7f
                            h = (x + h) and 0x3f
                            s += y / 4
                            if (s < 0) s = 0
                            else if (s > 7) s = 7
                            l += z
                            if (l < 0) l = 0
                            else if (l > 127) l = 127
                            this.facesColor!![face] = ((h shl 10) or (s shl 7) or l).toShort()
                        }
                        if (faceGroup.isNotEmpty() && this.colorsBuffer != null) {
                            this.colorsBuffer!!.buffer = null
                        }
                    }
                }
                if (this.billboardEffects != null) {
                    for (b in 0 until this.billboardCount) {
                        val effect = this.billboardEffects!![b]!!
                        val surface = this.billboardSurfaces!![b]!!
                        surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                    }
                }
            }
        } else if (transformationType == 8) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.billboardGroups!!.size) {
                        val billboardGroup = this.billboardGroups!![group]!!
                        for (i in billboardGroup.indices) {
                            val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                            surface.offsetX += x
                            surface.offsetY += y
                        }
                    }
                }
            }
        } else if (transformationType == 9) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.billboardGroups!!.size) {
                        val billboardGroup = this.billboardGroups!![group]!!
                        for (i in billboardGroup.indices) {
                            val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                            surface.rotation = (x + surface.rotation) and 0x3fff
                        }
                    }
                }
            }
        } else if (transformationType == 10) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val group = labels[labelIndex]
                    if (group < this.billboardGroups!!.size) {
                        val billboardGroup = this.billboardGroups!![group]!!
                        for (i in billboardGroup.indices) {
                            val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                            surface.scaleX = (x * surface.scaleX) shr 7
                            surface.scaleY = (y * surface.scaleY) shr 7
                        }
                    }
                }
            }
        }
    }

    override fun animateTransform(transformationType: Int, labels: IntArray, rotationX: Int, rotationY: Int, rotationZ: Int, lights: Boolean, submesh: Int, verticesData: IntArray) {
        var i_31 = rotationX
        var i_41 = rotationY
        var i_51 = rotationZ
        val numLabels = labels.size

        if (transformationType == 0) {
            i_31 = i_31 shl 4
            i_41 = i_41 shl 4
            i_51 = i_51 shl 4
            var count = 0
            originX = 0
            originY = 0
            originZ = 0

            for (labelIdx in 0 until numLabels) {
                val label = labels[labelIdx]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val group = this.vertexGroups!![label]!!
                    for (i in group.indices) {
                        val v = group[i]
                        if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                            originX += this.verticesX!![v]
                            originY += this.verticesY!![v]
                            originZ += this.verticesZ!![v]
                            count++
                        }
                    }
                }
            }

            if (count > 0) {
                originX = i_31 + originX / count
                originY = i_41 + originY / count
                originZ = i_51 + originZ / count
                withinOrigin = true
            } else {
                originX = i_31
                originY = i_41
                originZ = i_51
            }
        } else if (transformationType == 1) {
            if (verticesData != null) {
                var i_10 = ((i_51 * verticesData[2] + verticesData[1] * i_41 + i_31 * verticesData[0] + 8192) shr 14)
                var i_11 = ((i_31 * verticesData[3] + i_51 * verticesData[5] + i_41 * verticesData[4] + 8192) shr 14)
                var i_12 = ((i_31 * verticesData[6] + verticesData[7] * i_41 + i_51 * verticesData[8] + 8192) shr 14)
                i_31 = i_10
                i_41 = i_11
                i_51 = i_12
            }

            i_31 = i_31 shl 4
            i_41 = i_41 shl 4
            i_51 = i_51 shl 4

            for (labelIdx in 0 until numLabels) {
                val label = labels[labelIdx]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val group = this.vertexGroups!![label]!!
                    for (i in group.indices) {
                        val v = group[i]
                        if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                            this.verticesX!![v] += i_31
                            this.verticesY!![v] += i_41
                            this.verticesZ!![v] += i_51
                        }
                    }
                }
            }
        } else if (transformationType == 2) {
            if (verticesData != null) {
                val i_10 = verticesData[9] shl 4
                val i_11 = verticesData[10] shl 4
                val i_12 = verticesData[11] shl 4
                val i_48 = verticesData[12] shl 4
                val i_14 = verticesData[13] shl 4
                val i_15 = verticesData[14] shl 4
                if (withinOrigin) {
                    var i_16 = ((verticesData[0] * originX + verticesData[6] * originZ + verticesData[3] * originY + 8192) shr 14)
                    var i_17 = ((verticesData[7] * originZ + verticesData[4] * originY + verticesData[1] * originX + 8192) shr 14)
                    var i_18 = ((verticesData[8] * originZ + verticesData[2] * originX + verticesData[5] * originY + 8192) shr 14)
                    i_16 += i_48
                    i_17 += i_14
                    i_18 += i_15
                    originX = i_16
                    originY = i_17
                    originZ = i_18
                    withinOrigin = false
                }

                val ints_49 = IntArray(9)
                val i_17 = MathUtils.COSINE[i_31]
                val i_18 = MathUtils.SINE[i_31]
                val i_19 = MathUtils.COSINE[i_41]
                val i_20 = MathUtils.SINE[i_41]
                val i_21 = MathUtils.COSINE[i_51]
                val i_22 = MathUtils.SINE[i_51]
                val i_23 = ((i_18 * i_21 + 8192) shr 14)
                val i_24 = ((i_18 * i_22 + 8192) shr 14)
                ints_49[0] = ((i_19 * i_21 + i_20 * i_24 + 8192) shr 14)
                ints_49[1] = ((i_20 * i_23 + -i_19 * i_22 + 8192) shr 14)
                ints_49[2] = ((i_20 * i_17 + 8192) shr 14)
                ints_49[3] = ((i_17 * i_22 + 8192) shr 14)
                ints_49[4] = ((i_17 * i_21 + 8192) shr 14)
                ints_49[5] = -i_18
                ints_49[6] = ((i_19 * i_24 + -i_20 * i_21 + 8192) shr 14)
                ints_49[7] = ((i_20 * i_22 + i_19 * i_23 + 8192) shr 14)
                ints_49[8] = ((i_19 * i_17 + 8192) shr 14)
                val i_25 = ((ints_49[1] * -originY + ints_49[0] * -originX + ints_49[2] * -originZ + 8192) shr 14)
                val i_26 = ((-originY * ints_49[4] + ints_49[3] * -originX + -originZ * ints_49[5] + 8192) shr 14)
                val i_27 = ((ints_49[7] * -originY + ints_49[6] * -originX + ints_49[8] * -originZ + 8192) shr 14)
                val i_28 = i_25 + originX
                val i_29 = i_26 + originY
                val i_30 = i_27 + originZ
                val ints_31 = IntArray(9)

                for (i_32 in 0 until 3) {
                    for (i_33 in 0 until 3) {
                        var i_34 = 0
                        for (i_53 in 0 until 3) {
                            i_34 += verticesData[i_53 + i_33 * 3] * ints_49[i_53 + i_32 * 3]
                        }
                        ints_31[i_33 + i_32 * 3] = ((i_34 + 8192) shr 14)
                    }
                }

                var i_32 = ((i_14 * ints_49[1] + i_48 * ints_49[0] + i_15 * ints_49[2] + 8192) shr 14)
                var i_33 = ((i_14 * ints_49[4] + i_48 * ints_49[3] + i_15 * ints_49[5] + 8192) shr 14)
                var i_34 = ((ints_49[6] * i_48 + i_15 * ints_49[8] + ints_49[7] * i_14 + 8192) shr 14)
                i_32 += i_28
                i_33 += i_29
                i_34 += i_30
                val ints_35 = IntArray(9)

                for (i_36 in 0 until 3) {
                    for (i_37 in 0 until 3) {
                        var i_38 = 0
                        for (i_39 in 0 until 3) {
                            i_38 += ints_31[i_37 + i_39 * 3] * verticesData[i_39 + i_36 * 3]
                        }
                        ints_35[i_37 + i_36 * 3] = ((i_38 + 8192) shr 14)
                    }
                }

                var i_36 = ((i_34 * verticesData[2] + i_32 * verticesData[0] + i_33 * verticesData[1] + 8192) shr 14)
                var i_37 = ((i_32 * verticesData[3] + i_33 * verticesData[4] + verticesData[5] * i_34 + 8192) shr 14)
                var i_38 = ((i_32 * verticesData[6] + i_34 * verticesData[8] + verticesData[7] * i_33 + 8192) shr 14)
                i_36 += i_10
                i_37 += i_11
                i_38 += i_12

                for (i_39 in 0 until numLabels) {
                    val i_40 = labels[i_39]
                    if (i_40 < (this.vertexGroups?.size ?: 0)) {
                        val ints_41 = this.vertexGroups!![i_40]!!
                        for (i_42 in ints_41.indices) {
                            val i_43 = ints_41[i_42]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![i_43].toInt()) != 0) {
                                var i_44 = ((ints_35[0] * this.verticesX!![i_43] + ints_35[2] * this.verticesZ!![i_43] + ints_35[1] * this.verticesY!![i_43] + 8192) shr 14)
                                var i_45 = ((ints_35[4] * this.verticesY!![i_43] + ints_35[5] * this.verticesZ!![i_43] + ints_35[3] * this.verticesX!![i_43] + 8192) shr 14)
                                var i_46 = ((ints_35[6] * this.verticesX!![i_43] + ints_35[7] * this.verticesY!![i_43] + ints_35[8] * this.verticesZ!![i_43] + 8192) shr 14)
                                i_44 += i_36
                                i_45 += i_37
                                i_46 += i_38
                                this.verticesX!![i_43] = i_44
                                this.verticesY!![i_43] = i_45
                                this.verticesZ!![i_43] = i_46
                            }
                        }
                    }
                }
            } else {
                // No verticesData - simple rotation
                for (labelIdx in 0 until numLabels) {
                    val label = labels[labelIdx]
                    if (label < (this.vertexGroups?.size ?: 0)) {
                        val group = this.vertexGroups!![label]!!
                        for (i in group.indices) {
                            val v = group[i]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                this.verticesX!![v] -= originX
                                this.verticesY!![v] -= originY
                                this.verticesZ!![v] -= originZ
                                if (i_51 != 0) {
                                    val sin = MathUtils.SINE[i_51]
                                    val cos = MathUtils.COSINE[i_51]
                                    val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesX!![v] = tmp
                                }
                                if (i_31 != 0) {
                                    val sin = MathUtils.SINE[i_31]
                                    val cos = MathUtils.COSINE[i_31]
                                    val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                                    this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                                    this.verticesY!![v] = tmp
                                }
                                if (i_41 != 0) {
                                    val sin = MathUtils.SINE[i_41]
                                    val cos = MathUtils.COSINE[i_41]
                                    val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesX!![v] = tmp
                                }
                                this.verticesX!![v] += originX
                                this.verticesY!![v] += originY
                                this.verticesZ!![v] += originZ
                            }
                        }
                    }
                }

                if (lights) {
                    for (labelIdx in 0 until numLabels) {
                        val label = labels[labelIdx]
                        if (label < (this.vertexGroups?.size ?: 0)) {
                            val group = this.vertexGroups!![label]!!
                            for (i in group.indices) {
                                val v = group[i]
                                if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                    val triStart = this.triangles!![v]
                                    val triEnd = this.triangles!![v + 1]
                                    for (triIdx in triStart until triEnd) {
                                        val normalIdx = this.aShortArray8288!![triIdx].toInt() - 1
                                        if (normalIdx == -1) break
                                        if (i_51 != 0) {
                                            val sin = MathUtils.SINE[i_51]
                                            val cos = MathUtils.COSINE[i_51]
                                            val tmp = ((this.normalsY!![normalIdx] * sin + this.normalsX!![normalIdx] * cos + 16383) shr 14)
                                            this.normalsY!![normalIdx] = (((this.normalsY!![normalIdx] * cos - this.normalsX!![normalIdx] * sin + 16383) shr 14)).toShort()
                                            this.normalsX!![normalIdx] = tmp.toShort()
                                        }
                                        if (i_31 != 0) {
                                            val sin = MathUtils.SINE[i_31]
                                            val cos = MathUtils.COSINE[i_31]
                                            val tmp = ((this.normalsY!![normalIdx] * cos - this.normalsZ!![normalIdx] * sin + 16383) shr 14)
                                            this.normalsZ!![normalIdx] = (((this.normalsY!![normalIdx] * sin + this.normalsZ!![normalIdx] * cos + 16383) shr 14)).toShort()
                                            this.normalsY!![normalIdx] = tmp.toShort()
                                        }
                                        if (i_41 != 0) {
                                            val sin = MathUtils.SINE[i_41]
                                            val cos = MathUtils.COSINE[i_41]
                                            val tmp = ((this.normalsZ!![normalIdx] * sin + this.normalsX!![normalIdx] * cos + 16383) shr 14)
                                            this.normalsZ!![normalIdx] = (((this.normalsZ!![normalIdx] * cos - this.normalsX!![normalIdx] * sin + 16383) shr 14)).toShort()
                                            this.normalsX!![normalIdx] = tmp.toShort()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (this.normalsBuffer == null && this.colorsBuffer != null) {
                        this.colorsBuffer!!.buffer = null
                    }
                    if (this.normalsBuffer != null) {
                        this.normalsBuffer!!.buffer = null
                    }
                }
            }
        } else if (transformationType == 3) {
            if (verticesData != null) {
                val i_10 = verticesData[9] shl 4
                val i_11 = verticesData[10] shl 4
                val i_12 = verticesData[11] shl 4
                val i_48 = verticesData[12] shl 4
                val i_14 = verticesData[13] shl 4
                val i_15 = verticesData[14] shl 4
                if (withinOrigin) {
                    var i_16 = ((verticesData[0] * originX + verticesData[6] * originZ + verticesData[3] * originY + 8192) shr 14)
                    var i_17 = ((verticesData[7] * originZ + verticesData[4] * originY + verticesData[1] * originX + 8192) shr 14)
                    var i_18 = ((verticesData[8] * originZ + verticesData[2] * originX + verticesData[5] * originY + 8192) shr 14)
                    i_16 += i_48
                    i_17 += i_14
                    i_18 += i_15
                    originX = i_16
                    originY = i_17
                    originZ = i_18
                    withinOrigin = false
                }

                val i_16 = (i_31 shl 15) shr 7
                val i_17 = (i_41 shl 15) shr 7
                val i_18 = (i_51 shl 15) shr 7
                val i_19 = ((i_16 * -originX + 8192) shr 14)
                val i_20 = ((i_17 * -originY + 8192) shr 14)
                val i_21 = ((i_18 * -originZ + 8192) shr 14)
                val i_22 = i_19 + originX
                val i_23 = i_20 + originY
                val i_24 = i_21 + originZ
                val ints_50 = intArrayOf(
                    ((i_16 * verticesData[0] + 8192) shr 14),
                    ((i_16 * verticesData[3] + 8192) shr 14),
                    ((i_16 * verticesData[6] + 8192) shr 14),
                    ((i_17 * verticesData[1] + 8192) shr 14),
                    ((i_17 * verticesData[4] + 8192) shr 14),
                    ((verticesData[7] * i_17 + 8192) shr 14),
                    ((i_18 * verticesData[2] + 8192) shr 14),
                    ((verticesData[5] * i_18 + 8192) shr 14),
                    ((i_18 * verticesData[8] + 8192) shr 14)
                )
                var i_26 = ((i_16 * i_48 + 8192) shr 14)
                var i_27 = ((i_17 * i_14 + 8192) shr 14)
                var i_28 = ((i_18 * i_15 + 8192) shr 14)
                i_26 += i_22
                i_27 += i_23
                i_28 += i_24
                val ints_51 = IntArray(9)

                for (i_30 in 0 until 3) {
                    for (i_52 in 0 until 3) {
                        var i_32 = 0
                        for (i_33 in 0 until 3) {
                            i_32 += ints_50[i_52 + i_33 * 3] * verticesData[i_33 + i_30 * 3]
                        }
                        ints_51[i_52 + i_30 * 3] = ((i_32 + 8192) shr 14)
                    }
                }

                var i_30 = ((i_28 * verticesData[2] + i_26 * verticesData[0] + i_27 * verticesData[1] + 8192) shr 14)
                var i_52 = ((i_26 * verticesData[3] + i_27 * verticesData[4] + verticesData[5] * i_28 + 8192) shr 14)
                var i_32 = ((i_26 * verticesData[6] + i_28 * verticesData[8] + verticesData[7] * i_27 + 8192) shr 14)
                i_30 += i_10
                i_52 += i_11
                i_32 += i_12

                for (i_33 in 0 until numLabels) {
                    val i_34 = labels[i_33]
                    if (i_34 < (this.vertexGroups?.size ?: 0)) {
                        val ints_35 = this.vertexGroups!![i_34]!!
                        for (i_36 in ints_35.indices) {
                            val i_37 = ints_35[i_36]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![i_37].toInt()) != 0) {
                                var i_38 = ((ints_51[0] * this.verticesX!![i_37] + ints_51[2] * this.verticesZ!![i_37] + ints_51[1] * this.verticesY!![i_37] + 8192) shr 14)
                                var i_39 = ((ints_51[4] * this.verticesY!![i_37] + ints_51[5] * this.verticesZ!![i_37] + ints_51[3] * this.verticesX!![i_37] + 8192) shr 14)
                                var i_40 = ((ints_51[6] * this.verticesX!![i_37] + ints_51[7] * this.verticesY!![i_37] + ints_51[8] * this.verticesZ!![i_37] + 8192) shr 14)
                                i_38 += i_30
                                i_39 += i_52
                                i_40 += i_32
                                this.verticesX!![i_37] = i_38
                                this.verticesY!![i_37] = i_39
                                this.verticesZ!![i_37] = i_40
                            }
                        }
                    }
                }
            } else {
                // No verticesData - simple scale
                for (labelIdx in 0 until numLabels) {
                    val label = labels[labelIdx]
                    if (label < (this.vertexGroups?.size ?: 0)) {
                        val group = this.vertexGroups!![label]!!
                        for (i in group.indices) {
                            val v = group[i]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                this.verticesX!![v] -= originX
                                this.verticesY!![v] -= originY
                                this.verticesZ!![v] -= originZ
                                this.verticesX!![v] = (i_31 * this.verticesX!![v]) shr 7
                                this.verticesY!![v] = (i_41 * this.verticesY!![v]) shr 7
                                this.verticesZ!![v] = (i_51 * this.verticesZ!![v]) shr 7
                                this.verticesX!![v] += originX
                                this.verticesY!![v] += originY
                                this.verticesZ!![v] += originZ
                            }
                        }
                    }
                }
            }
        } else {
            // Transform types 5, 7, 8, 9, 10 - face/billboard transforms
            if (transformationType == 5) {
                // Alpha transform
                if (this.faceGroups != null) {
                    for (labelIdx in 0 until numLabels) {
                        val label = labels[labelIdx]
                        if (label < this.faceGroups!!.size) {
                            val group = this.faceGroups!![label]!!
                            for (i in group.indices) {
                                val face = group[i]
                                if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![face].toInt()) != 0) {
                                    var alpha = (this.facesAlpha!![face].toInt() and 0xff) + i_31 * 8
                                    if (alpha < 0) alpha = 0
                                    else if (alpha > 255) alpha = 255
                                    this.facesAlpha!![face] = alpha.toByte()
                                    if (this.colorsBuffer != null) {
                                        this.colorsBuffer!!.buffer = null
                                    }
                                }
                            }
                        }
                    }

                    if (this.billboardEffects != null) {
                        for (b in 0 until this.billboardCount) {
                            val effect = this.billboardEffects!![b]!!
                            val surface = this.billboardSurfaces!![b]!!
                            surface.diffuseColor = (surface.diffuseColor and 0xffffff) or ((255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff)) shl 24)
                        }
                    }
                }
            } else if (transformationType == 7) {
                // Color transform
                if (this.faceGroups != null) {
                    for (labelIdx in 0 until numLabels) {
                        val label = labels[labelIdx]
                        if (label < this.faceGroups!!.size) {
                            val group = this.faceGroups!![label]!!
                            for (i in group.indices) {
                                val face = group[i]
                                if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![face].toInt()) != 0) {
                                    val hsl = this.facesColor!![face].toInt() and 0xffff
                                    var h = (hsl shr 10) and 0x3f
                                    var s = (hsl shr 7) and 0x7
                                    var l = hsl and 0x7f
                                    h = (i_31 + h) and 0x3f
                                    s += i_41 / 4
                                    if (s < 0) s = 0
                                    else if (s > 7) s = 7
                                    l += i_51
                                    if (l < 0) l = 0
                                    else if (l > 127) l = 127
                                    this.facesColor!![face] = ((h shl 10) or (s shl 7) or l).toShort()
                                    if (this.colorsBuffer != null) {
                                        this.colorsBuffer!!.buffer = null
                                    }
                                }
                            }
                        }
                    }

                    if (this.billboardEffects != null) {
                        for (b in 0 until this.billboardCount) {
                            val effect = this.billboardEffects!![b]!!
                            val surface = this.billboardSurfaces!![b]!!
                            surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                        }
                    }
                }
            } else {
                if (transformationType == 8) {
                    // Billboard offset transform
                    if (this.billboardGroups != null) {
                        for (labelIdx in 0 until numLabels) {
                            val label = labels[labelIdx]
                            if (label < this.billboardGroups!!.size) {
                                val group = this.billboardGroups!![label]!!
                                for (i in group.indices) {
                                    val surface = this.billboardSurfaces!![group[i]]!!
                                    surface.offsetX += i_31
                                    surface.offsetY += i_41
                                }
                            }
                        }
                    }
                } else if (transformationType == 10) {
                    // Billboard scale transform
                    if (this.billboardGroups != null) {
                        for (labelIdx in 0 until numLabels) {
                            val label = labels[labelIdx]
                            if (label < this.billboardGroups!!.size) {
                                val group = this.billboardGroups!![label]!!
                                for (i in group.indices) {
                                    val surface = this.billboardSurfaces!![group[i]]!!
                                    surface.scaleX = (i_31 * surface.scaleX) shr 7
                                    surface.scaleY = (i_41 * surface.scaleY) shr 7
                                }
                            }
                        }
                    }
                } else if (transformationType == 9 && this.billboardGroups != null) {
                    // Billboard rotation transform
                    for (labelIdx in 0 until numLabels) {
                        val label = labels[labelIdx]
                        if (label < this.billboardGroups!!.size) {
                            val group = this.billboardGroups!![label]!!
                            for (i in group.indices) {
                                val surface = this.billboardSurfaces!![group[i]]!!
                                surface.rotation = (i_31 + surface.rotation) and 0x3fff
                            }
                        }
                    }
                }
            }
        }
    }

    override fun animateShadowtransform(transformType: Int, x: Int, y: Int, z: Int) {
        if (transformType == 0) {
            var transformCount = 0
            originX = 0
            originY = 0
            originZ = 0
            for (v in 0 until this.maxDepth) {
                originX += this.verticesX!![v]
                originY += this.verticesY!![v]
                originZ += this.verticesZ!![v]
                transformCount++
            }
            if (transformCount > 0) {
                originX = x + originX / transformCount
                originY = y + originY / transformCount
                originZ = z + originZ / transformCount
            } else {
                originX = x
                originY = y
                originZ = z
            }
        } else if (transformType == 1) {
            for (v in 0 until this.maxDepth) {
                this.verticesX!![v] += x
                this.verticesY!![v] += y
                this.verticesZ!![v] += z
            }
        } else if (transformType == 2) {
            for (v in 0 until this.maxDepth) {
                this.verticesX!![v] -= originX
                this.verticesY!![v] -= originY
                this.verticesZ!![v] -= originZ
                if (z != 0) {
                    val sin = MathUtils.SINE[z]
                    val cos = MathUtils.COSINE[z]
                    val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                    this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                    this.verticesX!![v] = tmp
                }
                if (x != 0) {
                    val sin = MathUtils.SINE[x]
                    val cos = MathUtils.COSINE[x]
                    val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                    this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                    this.verticesY!![v] = tmp
                }
                if (y != 0) {
                    val sin = MathUtils.SINE[y]
                    val cos = MathUtils.COSINE[y]
                    val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                    this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                    this.verticesX!![v] = tmp
                }
                this.verticesX!![v] += originX
                this.verticesY!![v] += originY
                this.verticesZ!![v] += originZ
            }
        } else if (transformType == 3) {
            for (v in 0 until this.maxDepth) {
                this.verticesX!![v] -= originX
                this.verticesY!![v] -= originY
                this.verticesZ!![v] -= originZ
                this.verticesX!![v] = x * this.verticesX!![v] / 128
                this.verticesY!![v] = y * this.verticesY!![v] / 128
                this.verticesZ!![v] = z * this.verticesZ!![v] / 128
                this.verticesX!![v] += originX
                this.verticesY!![v] += originY
                this.verticesZ!![v] += originZ
            }
        } else if (transformType == 5) {
            for (f in 0 until this.numFaces) {
                var alpha = (this.facesAlpha!![f].toInt() and 0xff) + x * 8
                if (alpha < 0) alpha = 0
                else if (alpha > 255) alpha = 255
                this.facesAlpha!![f] = alpha.toByte()
            }
            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }
            if (this.billboardEffects != null) {
                for (b in 0 until this.billboardCount) {
                    val effect = this.billboardEffects!![b]!!
                    val surface = this.billboardSurfaces!![b]!!
                    surface.diffuseColor = (surface.diffuseColor and 0xffffff) or ((255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff)) shl 24)
                }
            }
        } else if (transformType == 7) {
            for (f in 0 until this.numFaces) {
                val hsl = this.facesColor!![f].toInt() and 0xffff
                var h = (hsl shr 10) and 0x3f
                var s = (hsl shr 7) and 0x7
                var l = hsl and 0x7f
                h = (h + x) and 0x3f
                s += y / 4
                if (s < 0) s = 0
                else if (s > 7) s = 7
                l += z
                if (l < 0) l = 0
                else if (l > 127) l = 127
                this.facesColor!![f] = ((h shl 10) or (s shl 7) or l).toShort()
            }
            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }
            if (this.billboardEffects != null) {
                for (b in 0 until this.billboardCount) {
                    val effect = this.billboardEffects!![b]!!
                    val surface = this.billboardSurfaces!![b]!!
                    surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                }
            }
        } else if (transformType == 8) {
            for (b in 0 until this.billboardCount) {
                val surface = this.billboardSurfaces!![b]!!
                surface.offsetX += x
                surface.offsetY += y
            }
        } else if (transformType == 9) {
            for (b in 0 until this.billboardCount) {
                val surface = this.billboardSurfaces!![b]!!
                surface.rotation = (x + surface.rotation) and 0x3fff
            }
        } else if (transformType == 10) {
            for (b in 0 until this.billboardCount) {
                val surface = this.billboardSurfaces!![b]!!
                surface.scaleX = (x * surface.scaleX) shr 7
                surface.scaleY = (y * surface.scaleY) shr 7
            }
        }
    }

    override fun contourToGround(contourType: Int, contourModifier: Int, currentLvl: Ground?, nextLvl: Ground?, sceneX: Int, averageHeight: Int, sceneY: Int) {
        if (!this.boundsCalculated) {
            calculateBounds()
        }

        var minTileX = sceneX + this.minX
        var maxTileX = sceneX + this.maxX
        var minTileY = sceneY + this.minZ
        var maxTileY = sceneY + this.maxZ
        if (contourType != 1 && contourType != 2 && contourType != 3 && contourType != 5 || minTileX >= 0 && (maxTileX + currentLvl!!.tileUnits shr currentLvl.tileScale) < currentLvl.width && minTileY >= 0 && (maxTileY + currentLvl.tileUnits shr currentLvl.tileScale) < currentLvl.length) {
            if (contourType != 4 && contourType != 5) {
                minTileX = minTileX shr currentLvl!!.tileScale
                maxTileX = (maxTileX + (currentLvl.tileUnits - 1)) shr currentLvl.tileScale
                minTileY = minTileY shr currentLvl.tileScale
                maxTileY = (maxTileY + (currentLvl.tileUnits - 1)) shr currentLvl.tileScale
                if (currentLvl.getHeight(minTileX, minTileY) == averageHeight && currentLvl.getHeight(maxTileX, minTileY) == averageHeight && currentLvl.getHeight(minTileX, maxTileY) == averageHeight && currentLvl.getHeight(maxTileX, maxTileY) == averageHeight) {
                    return
                }
            } else if (nextLvl == null || minTileX < 0 || (nextLvl.tileUnits + maxTileX shr nextLvl.tileScale) >= nextLvl.width || minTileY < 0 || (nextLvl.tileUnits + maxTileY shr nextLvl.tileScale) >= nextLvl.length) {
                return
            }

            if (contourType == 1) {
                for (v in 0 until this.maxDepth) {
                    this.verticesY!![v] = this.verticesY!![v] + currentLvl!!.getAverageHeight(this.verticesX!![v] + sceneX, this.verticesZ!![v] + sceneY) - averageHeight
                }
            } else if (contourType == 2) {
                val minYVal = this.minY
                if (minYVal.toInt() == 0) {
                    return
                }

                for (v in 0 until this.maxDepth) {
                    val i_14 = (this.verticesY!![v] shl 16) / minYVal
                    if (i_14 < contourModifier) {
                        this.verticesY!![v] += (currentLvl!!.getAverageHeight(this.verticesX!![v] + sceneX, this.verticesZ!![v] + sceneY) - averageHeight) * (contourModifier - i_14) / contourModifier
                    }
                }
            } else if (contourType == 3) {
                val width = (contourModifier and 0xff) * 4
                val length = ((contourModifier shr 8) and 0xff) * 4
                val xan = ((contourModifier shr 16) and 0xff) shl 6
                val zan = ((contourModifier shr 24) and 0xff) shl 6
                if (sceneX - (width shr 1) < 0 || sceneX + currentLvl!!.tileUnits + (width shr 1) >= currentLvl.width shl currentLvl.tileScale || sceneY - (length shr 1) < 0 || currentLvl.tileUnits + (length shr 1) + sceneY >= currentLvl.length shl currentLvl.tileScale) {
                    return
                }

                hillChange(currentLvl, sceneX, averageHeight, sceneY, width, length, xan, zan)
            } else if (contourType == 4) {
                val sizeY = this.maxY - this.minY

                for (v in 0 until this.maxDepth) {
                    this.verticesY!![v] += (nextLvl!!.getAverageHeight(this.verticesX!![v] + sceneX, this.verticesZ!![v] + sceneY) - averageHeight) + sizeY
                }
            } else if (contourType == 5) {
                val sizeY = this.maxY - this.minY

                for (v in 0 until this.maxDepth) {
                    val x = sceneX + this.verticesX!![v]
                    val z = sceneY + this.verticesZ!![v]
                    val currentAvgHeight = currentLvl!!.getAverageHeight(x, z)
                    val nextAvgHeight = nextLvl!!.getAverageHeight(x, z)
                    val delta = currentAvgHeight - nextAvgHeight - contourModifier
                    this.verticesY!![v] = (delta * ((this.verticesY!![v] shl 8) / sizeY) shr 8) - (averageHeight - currentAvgHeight)
                }
            }

            if (this.vertexBuffer != null) {
                this.vertexBuffer!!.buffer = null
            }

            this.boundsCalculated = false
        }
    }

    override fun hillChange(ground: Ground, sceneX: Int, averageHeight: Int, sceneY: Int, width: Int, length: Int, xan: Int, zan: Int) {
        val sin = MathUtils.SINE[xan]
        val cos = MathUtils.COSINE[xan]
        val sin2 = MathUtils.SINE[zan]
        val cos2 = MathUtils.COSINE[zan]

        for (v in 0 until this.maxDepth) {
            var x = this.verticesX!![v]
            var y = this.verticesY!![v]
            var z = this.verticesZ!![v]
            if (xan != 0) {
                val tmp = (y * cos - z * sin) shr 14
                z = (y * sin + z * cos) shr 14
                y = tmp
            }

            if (zan != 0) {
                val tmp = (z * sin2 + x * cos2) shr 14
                z = (z * cos2 - x * sin2) shr 14
                x = tmp
            }

            x += sceneX
            y += averageHeight
            z += sceneY
            val groundHeight = ground.getAverageHeight(x, z)
            this.verticesY!![v] = this.verticesY!![v] + (groundHeight - y)
        }
    }

    private fun calculateBounds() {
        var minXVal = 32767
        var minYVal = 32767
        var minZVal = 32767
        var maxXVal = -32768
        var maxYVal = -32768
        var maxZVal = -32768
        var size2dSquared = 0
        var size3dSquared = 0

        for (v in 0 until this.maxDepth) {
            val vertexX = this.verticesX!![v]
            val vertexY = this.verticesY!![v]
            val vertexZ = this.verticesZ!![v]

            if (vertexX < minXVal) minXVal = vertexX
            if (vertexX > maxXVal) maxXVal = vertexX
            if (vertexY < minYVal) minYVal = vertexY
            if (vertexY > maxYVal) maxYVal = vertexY
            if (vertexZ < minZVal) minZVal = vertexZ
            if (vertexZ > maxZVal) maxZVal = vertexZ

            val s2d = vertexX * vertexX + vertexZ * vertexZ
            if (s2d > size2dSquared) {
                size2dSquared = s2d
            }

            val s3d = vertexX * vertexX + vertexZ * vertexZ + vertexY * vertexY
            if (s3d > size3dSquared) {
                size3dSquared = s3d
            }
        }

        this.minX = minXVal.toShort()
        this.maxX = maxXVal.toShort()
        this.minY = minYVal.toShort()
        this.maxY = maxYVal.toShort()
        this.minZ = minZVal.toShort()
        this.maxZ = maxZVal.toShort()
        this.size2d = (sqrt(size2dSquared.toDouble()) + 0.99).toInt()
        this.size3d = (sqrt(size3dSquared.toDouble()) + 0.99).toInt()

        this.boundsCalculated = true
    }
}
