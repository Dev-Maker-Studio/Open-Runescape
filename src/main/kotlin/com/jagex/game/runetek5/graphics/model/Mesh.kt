package com.jagex.game.runetek5.graphics.model

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.model.billboard.Billboard
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.utils.MathUtils

class Mesh {
    @JvmField
    var version = 12
    @JvmField
    var numVertices = 0
    @JvmField
    var maxDepth = 0
    @JvmField
    var numFaces = 0
    @JvmField
    var priority: Byte = 0
    @JvmField
    var numTextures = 0
    @JvmField
    var texturesMapping: ByteArray? = null
    @JvmField
    var verticesX: IntArray? = null
    @JvmField
    var verticesY: IntArray? = null
    @JvmField
    var verticesZ: IntArray? = null
    @JvmField
    var facesA: ShortArray? = null
    @JvmField
    var facesB: ShortArray? = null
    @JvmField
    var facesC: ShortArray? = null
    @JvmField
    var vertexGroups: IntArray? = null
    @JvmField
    var faceRenderType: ByteArray? = null
    @JvmField
    var facesLayer: ByteArray? = null
    @JvmField
    var facesAlpha: ByteArray? = null
    @JvmField
    var faceGroups: IntArray? = null
    @JvmField
    var facesMaterial: ShortArray? = null
    @JvmField
    var facesColor: ShortArray? = null
    @JvmField
    var facesTexture: ByteArray? = null
    @JvmField
    var textureFaceX: ShortArray? = null
    @JvmField
    var textureFaceY: ShortArray? = null
    @JvmField
    var textureFaceZ: ShortArray? = null
    @JvmField
    var texturesScaleX: IntArray? = null
    @JvmField
    var texturesScaleY: IntArray? = null
    @JvmField
    var texturesScaleZ: IntArray? = null
    @JvmField
    var texturesRotation: ByteArray? = null
    @JvmField
    var texturesDirection: ByteArray? = null
    @JvmField
    var texturesSpeed: IntArray? = null
    @JvmField
    var texturesUtrans: IntArray? = null
    @JvmField
    var texturesVtrans: IntArray? = null
    @JvmField
    var particleEmitters: Array<EmissiveTriangle>? = null
    @JvmField
    var particleEffectors: Array<EffectiveVertex>? = null
    @JvmField
    var billboards: Array<Billboard>? = null
    @JvmField
    var verticesSubmeshes: ShortArray? = null
    @JvmField
    var facesSubmeshes: ShortArray? = null

    constructor(data: ByteArray) {
        if (data[data.size - 1] == (-1).toByte() && data[data.size - 2] == (-1).toByte()) {
            decodeNewFormat(data)
        } else {
            decodeOldFormat(data)
        }
    }

    constructor(meshes: Array<Mesh?>, meshCount: Int) {
        numVertices = 0
        numFaces = 0
        numTextures = 0
        var amountOfEmitters = 0
        var amountOfEffectiveVertices = 0
        var amountOfBillboards = 0
        var hasRenderType = false
        var hasPriorities = false
        var hasAlpha = false
        var hasFaceTexture = false
        var hasFaceMaterial = false
        var hasFaceLabel = false
        priority = -1
        for (meshIndex in 0 until meshCount) {
            val mesh = meshes[meshIndex]
            if (mesh != null) {
                numVertices += mesh.numVertices
                numFaces += mesh.numFaces
                numTextures += mesh.numTextures
                if (mesh.particleEmitters != null) {
                    amountOfEmitters += mesh.particleEmitters!!.size
                }
                if (mesh.particleEffectors != null) {
                    amountOfEffectiveVertices += mesh.particleEffectors!!.size
                }
                if (mesh.billboards != null) {
                    amountOfBillboards += mesh.billboards!!.size
                }
                hasRenderType = hasRenderType or (mesh.faceRenderType != null)
                if (mesh.facesLayer != null) {
                    hasPriorities = true
                } else {
                    if (priority == (-1).toByte()) {
                        priority = mesh.priority
                    }
                    if (priority != mesh.priority) {
                        hasPriorities = true
                    }
                }
                hasAlpha = hasAlpha or (mesh.facesAlpha != null)
                hasFaceTexture = hasFaceTexture or (mesh.facesTexture != null)
                hasFaceMaterial = hasFaceMaterial or (mesh.facesMaterial != null)
                hasFaceLabel = hasFaceLabel or (mesh.faceGroups != null)
            }
        }
        verticesX = IntArray(numVertices)
        verticesY = IntArray(numVertices)
        verticesZ = IntArray(numVertices)
        vertexGroups = IntArray(numVertices)
        verticesSubmeshes = ShortArray(numVertices)
        facesA = ShortArray(numFaces)
        facesB = ShortArray(numFaces)
        facesC = ShortArray(numFaces)
        if (hasRenderType) {
            faceRenderType = ByteArray(numFaces)
        }
        if (hasPriorities) {
            facesLayer = ByteArray(numFaces)
        }
        if (hasAlpha) {
            facesAlpha = ByteArray(numFaces)
        }
        if (hasFaceTexture) {
            facesTexture = ByteArray(numFaces)
        }
        facesColor = ShortArray(numFaces)
        if (hasFaceMaterial) {
            facesMaterial = ShortArray(numFaces)
        }
        if (hasFaceLabel) {
            faceGroups = IntArray(numFaces)
        }
        facesSubmeshes = ShortArray(numFaces)
        if (numTextures > 0) {
            texturesMapping = ByteArray(numTextures)
            textureFaceX = ShortArray(numTextures)
            textureFaceY = ShortArray(numTextures)
            textureFaceZ = ShortArray(numTextures)
            texturesScaleX = IntArray(numTextures)
            texturesScaleY = IntArray(numTextures)
            texturesScaleZ = IntArray(numTextures)
            texturesRotation = ByteArray(numTextures)
            texturesDirection = ByteArray(numTextures)
            texturesSpeed = IntArray(numTextures)
            texturesUtrans = IntArray(numTextures)
            texturesVtrans = IntArray(numTextures)
        }
        if (amountOfBillboards > 0) {
            billboards = Array(amountOfBillboards) { Billboard(0, 0, 0, 0) }
        }
        if (amountOfEmitters > 0) {
            particleEmitters = Array(amountOfEmitters) { EmissiveTriangle(0, 0, 0, 0, 0) }
        }
        if (amountOfEffectiveVertices > 0) {
            particleEffectors = Array(amountOfEffectiveVertices) { EffectiveVertex(0, 0) }
        }
        numVertices = 0
        numFaces = 0
        numTextures = 0
        amountOfEmitters = 0
        amountOfEffectiveVertices = 0
        amountOfBillboards = 0
        for (meshIndex in 0 until meshCount) {
            val meshFlag = (1 shl meshIndex).toShort()
            val mesh = meshes[meshIndex]
            if (mesh != null) {
                if (mesh.billboards != null) {
                    for (billboardIndex in mesh.billboards!!.indices) {
                        val billboard = mesh.billboards!![billboardIndex]
                        billboards!![amountOfBillboards++] = billboard.translateToFace(billboard.appliesOnFace + numFaces)
                    }
                }
                for (faceIndex in 0 until mesh.numFaces) {
                    if (hasRenderType && mesh.faceRenderType != null) {
                        faceRenderType!![numFaces] = mesh.faceRenderType!![faceIndex]
                    }
                    if (hasPriorities) {
                        if (mesh.facesLayer != null) {
                            facesLayer!![numFaces] = mesh.facesLayer!![faceIndex]
                        } else {
                            facesLayer!![numFaces] = mesh.priority
                        }
                    }
                    if (hasAlpha && mesh.facesAlpha != null) {
                        facesAlpha!![numFaces] = mesh.facesAlpha!![faceIndex]
                    }
                    if (hasFaceMaterial) {
                        if (mesh.facesMaterial != null) {
                            facesMaterial!![numFaces] = mesh.facesMaterial!![faceIndex]
                        } else {
                            facesMaterial!![numFaces] = -1
                        }
                    }
                    if (hasFaceLabel) {
                        if (mesh.faceGroups != null) {
                            faceGroups!![numFaces] = mesh.faceGroups!![faceIndex]
                        } else {
                            faceGroups!![numFaces] = -1
                        }
                    }
                    facesA!![numFaces] = connect(mesh, mesh.facesA!![faceIndex].toInt(), meshFlag).toShort()
                    facesB!![numFaces] = connect(mesh, mesh.facesB!![faceIndex].toInt(), meshFlag).toShort()
                    facesC!![numFaces] = connect(mesh, mesh.facesC!![faceIndex].toInt(), meshFlag).toShort()
                    facesSubmeshes!![numFaces] = meshFlag
                    facesColor!![numFaces] = mesh.facesColor!![faceIndex]
                    numFaces++
                }
                if (mesh.particleEmitters != null) {
                    for (emitterIndex in mesh.particleEmitters!!.indices) {
                        val x = connect(mesh, mesh.particleEmitters!![emitterIndex].faceA, meshFlag)
                        val y = connect(mesh, mesh.particleEmitters!![emitterIndex].faceB, meshFlag)
                        val z = connect(mesh, mesh.particleEmitters!![emitterIndex].faceC, meshFlag)
                        particleEmitters!![amountOfEmitters] = mesh.particleEmitters!![emitterIndex].transform(x, y, z)
                        amountOfEmitters++
                    }
                }
                if (mesh.particleEffectors != null) {
                    for (effectorIndex in mesh.particleEffectors!!.indices) {
                        val connectedVertice = connect(mesh, mesh.particleEffectors!![effectorIndex].appliesOnVertex, meshFlag)
                        particleEffectors!![amountOfEffectiveVertices] = mesh.particleEffectors!![effectorIndex].switchVertex(connectedVertice)
                        amountOfEffectiveVertices++
                    }
                }
            }
        }
        var currentFace = 0
        maxDepth = numVertices
        for (meshIndex in 0 until meshCount) {
            val meshFlag = (1 shl meshIndex).toShort()
            val mesh = meshes[meshIndex]
            if (mesh != null) {
                for (face in 0 until mesh.numFaces) {
                    if (hasFaceTexture) {
                        facesTexture!![currentFace++] = if (mesh.facesTexture != null && mesh.facesTexture!![face] != (-1).toByte()) (numTextures + mesh.facesTexture!![face]).toByte() else -1
                    }
                }
                for (textureIndex in 0 until mesh.numTextures) {
                    val textureMapping = mesh.texturesMapping!![textureIndex].also { texturesMapping!![numTextures] = it }
                    if (textureMapping == 0.toByte()) {
                        textureFaceX!![numTextures] = connect(mesh, mesh.textureFaceX!![textureIndex].toInt(), meshFlag).toShort()
                        textureFaceY!![numTextures] = connect(mesh, mesh.textureFaceY!![textureIndex].toInt(), meshFlag).toShort()
                        textureFaceZ!![numTextures] = connect(mesh, mesh.textureFaceZ!![textureIndex].toInt(), meshFlag).toShort()
                    }
                    if (textureMapping >= 1 && textureMapping <= 3) {
                        textureFaceX!![numTextures] = mesh.textureFaceX!![textureIndex]
                        textureFaceY!![numTextures] = mesh.textureFaceY!![textureIndex]
                        textureFaceZ!![numTextures] = mesh.textureFaceZ!![textureIndex]
                        texturesScaleX!![numTextures] = mesh.texturesScaleX!![textureIndex]
                        texturesScaleY!![numTextures] = mesh.texturesScaleY!![textureIndex]
                        texturesScaleZ!![numTextures] = mesh.texturesScaleZ!![textureIndex]
                        texturesRotation!![numTextures] = mesh.texturesRotation!![textureIndex]
                        texturesDirection!![numTextures] = mesh.texturesDirection!![textureIndex]
                        texturesSpeed!![numTextures] = mesh.texturesSpeed!![textureIndex]
                    }
                    if (textureMapping == 2.toByte()) {
                        texturesUtrans!![numTextures] = mesh.texturesUtrans!![textureIndex]
                        texturesVtrans!![numTextures] = mesh.texturesVtrans!![textureIndex]
                    }
                    numTextures++
                }
            }
        }
    }

    constructor(verticeCount: Int, faceCount: Int, textureCount: Int) {
        verticesX = IntArray(verticeCount)
        verticesY = IntArray(verticeCount)
        verticesZ = IntArray(verticeCount)
        vertexGroups = IntArray(verticeCount)
        facesA = ShortArray(faceCount)
        facesB = ShortArray(faceCount)
        facesC = ShortArray(faceCount)
        faceRenderType = ByteArray(faceCount)
        facesLayer = ByteArray(faceCount)
        facesAlpha = ByteArray(faceCount)
        facesTexture = ByteArray(faceCount)
        facesColor = ShortArray(faceCount)
        facesMaterial = ShortArray(faceCount)
        faceGroups = IntArray(faceCount)
        if (textureCount > 0) {
            texturesMapping = ByteArray(textureCount)
            textureFaceX = ShortArray(textureCount)
            textureFaceY = ShortArray(textureCount)
            textureFaceZ = ShortArray(textureCount)
            texturesScaleX = IntArray(textureCount)
            texturesScaleY = IntArray(textureCount)
            texturesScaleZ = IntArray(textureCount)
            texturesRotation = ByteArray(textureCount)
            texturesDirection = ByteArray(textureCount)
            texturesSpeed = IntArray(textureCount)
            texturesUtrans = IntArray(textureCount)
            texturesVtrans = IntArray(textureCount)
        }
    }

    fun connect(mesh: Mesh, verticeToConnect: Int, meshIndex: Short): Int {
        val x = mesh.verticesX!![verticeToConnect]
        val y = mesh.verticesY!![verticeToConnect]
        val z = mesh.verticesZ!![verticeToConnect]
        for (verticeIndex in 0 until numVertices) {
            if (x == verticesX!![verticeIndex] && y == verticesY!![verticeIndex] && z == verticesZ!![verticeIndex]) {
                verticesSubmeshes!![verticeIndex] = (verticesSubmeshes!![verticeIndex].toInt() or meshIndex.toInt()).toShort()
                return verticeIndex
            }
        }
        verticesX!![numVertices] = x
        verticesY!![numVertices] = y
        verticesZ!![numVertices] = z
        verticesSubmeshes!![numVertices] = meshIndex
        vertexGroups!![numVertices] = if (mesh.vertexGroups != null) mesh.vertexGroups!![verticeToConnect] else -1
        return numVertices++
    }

    fun decodeNewFormat(data: ByteArray) {
        val first = Packet(data)
        val second = Packet(data)
        val third = Packet(data)
        val fourth = Packet(data)
        val fifth = Packet(data)
        val seventh = Packet(data)
        val eight = Packet(data)
        first.currentPosition = data.size - 23
        numVertices = first.readUShort()
        numFaces = first.readUShort()
        numTextures = first.readUByte()
        val footerFlags = first.readUByte()
        val hasFaceTypes = (footerFlags and 0x1) == 1
        val hasParticleEffects = (footerFlags and 0x2) == 2
        val hasBillboards = (footerFlags and 0x4) == 4
        val hasVersion = (footerFlags and 0x8) == 8
        if (hasVersion) {
            first.currentPosition -= 7
            version = first.readUByte()
            first.currentPosition += 6
        }
        val modelPriority = first.readUByte()
        val hasFaceAlpha = first.readUByte()
        val hasFaceSkins = first.readUByte()
        val hasFaceTextures = first.readUByte()
        val hasVertexSkins = first.readUByte()
        val modelVerticesX = first.readUShort()
        val modelVerticesY = first.readUShort()
        val modelVerticesZ = first.readUShort()
        val faceIndices = first.readUShort()
        val textureIndices = first.readUShort()
        var simpleTextureFaceCount = 0
        var complexTextureFaceCount = 0
        var cubeTextureFaceCount = 0
        if (numTextures > 0) {
            texturesMapping = ByteArray(numTextures)
            first.currentPosition = 0
            for (textureIndex in 0 until numTextures) {
                val textureMapping = first.readByte().also { texturesMapping!![textureIndex] = it }
                if (textureMapping == 0.toByte()) {
                    simpleTextureFaceCount++
                }
                if (textureMapping >= 1 && textureMapping <= 3) {
                    complexTextureFaceCount++
                }
                if (textureMapping == 2.toByte()) {
                    cubeTextureFaceCount++
                }
            }
        }
        var index = numTextures
        val vertexFlagsOffset = index
        index += numVertices
        val faceTypesOffset = index
        if (hasFaceTypes) {
            index += numFaces
        }
        val faceIndexTypesOffset = index
        index += numFaces
        val i_31 = index
        if (modelPriority == 255) {
            index += numFaces
        }
        val i_32 = index
        if (hasFaceSkins == 1) {
            index += numFaces
        }
        val i_33 = index
        if (hasVertexSkins == 1) {
            index += numVertices
        }
        val i_34 = index
        if (hasFaceAlpha == 1) {
            index += numFaces
        }
        val i_35 = index
        index += faceIndices
        val i_36 = index
        if (hasFaceTextures == 1) {
            index += numFaces * 2
        }
        val i_37 = index
        index += textureIndices
        val i_38 = index
        index += numFaces * 2
        val i_39 = index
        index += modelVerticesX
        val i_40 = index
        index += modelVerticesY
        val i_41 = index
        index += modelVerticesZ
        val simpleTexPmnOffset = index
        index += simpleTextureFaceCount * 6
        val i_43 = index
        index += complexTextureFaceCount * 6
        var textureBytes: Byte = 6
        if (version == 14) {
            textureBytes = 7
        } else if (version >= 15) {
            textureBytes = 9
        }
        val i_45 = index
        index += complexTextureFaceCount * textureBytes
        val i_46 = index
        index += complexTextureFaceCount
        val i_47 = index
        index += complexTextureFaceCount
        val i_48 = index
        index = cubeTextureFaceCount * 2 + index + complexTextureFaceCount
        verticesX = IntArray(numVertices)
        verticesY = IntArray(numVertices)
        verticesZ = IntArray(numVertices)
        facesA = ShortArray(numFaces)
        facesB = ShortArray(numFaces)
        facesC = ShortArray(numFaces)
        if (hasVertexSkins == 1) {
            vertexGroups = IntArray(numVertices)
        }
        if (hasFaceTypes) {
            faceRenderType = ByteArray(numFaces)
        }
        if (modelPriority == 255) {
            facesLayer = ByteArray(numFaces)
        } else {
            priority = modelPriority.toByte()
        }
        if (hasFaceAlpha == 1) {
            facesAlpha = ByteArray(numFaces)
        }
        if (hasFaceSkins == 1) {
            faceGroups = IntArray(numFaces)
        }
        if (hasFaceTextures == 1) {
            facesMaterial = ShortArray(numFaces)
        }
        if (hasFaceTextures == 1 && numTextures > 0) {
            facesTexture = ByteArray(numFaces)
        }
        facesColor = ShortArray(numFaces)
        if (numTextures > 0) {
            textureFaceX = ShortArray(numTextures)
            textureFaceY = ShortArray(numTextures)
            textureFaceZ = ShortArray(numTextures)
            if (complexTextureFaceCount > 0) {
                texturesScaleX = IntArray(complexTextureFaceCount)
                texturesScaleY = IntArray(complexTextureFaceCount)
                texturesScaleZ = IntArray(complexTextureFaceCount)
                texturesRotation = ByteArray(complexTextureFaceCount)
                texturesDirection = ByteArray(complexTextureFaceCount)
                texturesSpeed = IntArray(complexTextureFaceCount)
            }
            if (cubeTextureFaceCount > 0) {
                texturesUtrans = IntArray(cubeTextureFaceCount)
                texturesVtrans = IntArray(cubeTextureFaceCount)
            }
        }
        first.currentPosition = vertexFlagsOffset
        second.currentPosition = i_39
        third.currentPosition = i_40
        fourth.currentPosition = i_41
        fifth.currentPosition = i_33
        var lastVerticeX = 0
        var lastVerticeY = 0
        var lastVerticeZ = 0
        for (verticeIndex in 0 until numVertices) {
            val offsetFlags = first.readUByte()
            var verticeXOffset = 0
            if ((offsetFlags and 0x1) != 0) {
                verticeXOffset = second.readSmart()
            }
            var verticeYOffset = 0
            if ((offsetFlags and 0x2) != 0) {
                verticeYOffset = third.readSmart()
            }
            var verticeZOffset = 0
            if ((offsetFlags and 0x4) != 0) {
                verticeZOffset = fourth.readSmart()
            }
            verticesX!![verticeIndex] = lastVerticeX + verticeXOffset
            verticesY!![verticeIndex] = lastVerticeY + verticeYOffset
            verticesZ!![verticeIndex] = lastVerticeZ + verticeZOffset
            lastVerticeX = verticesX!![verticeIndex]
            lastVerticeY = verticesY!![verticeIndex]
            lastVerticeZ = verticesZ!![verticeIndex]
            if (hasVertexSkins == 1) {
                vertexGroups!![verticeIndex] = fifth.readUByte()
            }
        }
        first.currentPosition = i_38
        second.currentPosition = faceTypesOffset
        third.currentPosition = i_31
        fourth.currentPosition = i_34
        fifth.currentPosition = i_32
        seventh.currentPosition = i_36
        eight.currentPosition = i_37
        for (faceIndex in 0 until numFaces) {
            facesColor!![faceIndex] = first.readUShort().toShort()
            if (hasFaceTypes) {
                faceRenderType!![faceIndex] = second.readByte()
            }
            if (modelPriority == 255) {
                facesLayer!![faceIndex] = third.readByte()
            }
            if (hasFaceAlpha == 1) {
                facesAlpha!![faceIndex] = fourth.readByte()
            }
            if (hasFaceSkins == 1) {
                faceGroups!![faceIndex] = fifth.readUByte()
            }
            if (hasFaceTextures == 1) {
                facesMaterial!![faceIndex] = (seventh.readUShort() - 1).toShort()
            }
            if (facesTexture != null) {
                if (facesMaterial!![faceIndex] != (-1).toShort()) {
                    facesTexture!![faceIndex] = (eight.readUByte() - 1).toByte()
                } else {
                    facesTexture!![faceIndex] = -1
                }
            }
        }
        maxDepth = -1
        first.currentPosition = i_35
        second.currentPosition = faceIndexTypesOffset
        calculateMaxDepth(first, second)
        first.currentPosition = simpleTexPmnOffset
        second.currentPosition = i_43
        third.currentPosition = i_45
        fourth.currentPosition = i_46
        fifth.currentPosition = i_47
        seventh.currentPosition = i_48
        decodeTexturedTriangles(first, second, third, fourth, fifth, seventh)
        first.currentPosition = index
        if (hasParticleEffects) {
            val emitterCount = first.readUByte()
            if (emitterCount > 0) {
                particleEmitters = Array(emitterCount) { EmissiveTriangle(0, 0, 0, 0, 0) }
                for (emitterIndex in 0 until emitterCount) {
                    val particleId = first.readUShort()
                    val faceIndex = first.readUShort()
                    val pri: Byte = if (modelPriority == 255) {
                        facesLayer!![faceIndex]
                    } else {
                        modelPriority.toByte()
                    }
                    particleEmitters!![emitterIndex] = EmissiveTriangle(particleId, facesA!![faceIndex].toInt(), facesB!![faceIndex].toInt(), facesC!![faceIndex].toInt(), pri)
                }
            }
            val effectorCount = first.readUByte()
            if (effectorCount > 0) {
                particleEffectors = Array(effectorCount) { EffectiveVertex(0, 0) }
                for (effectorIndex in 0 until effectorCount) {
                    val effectId = first.readUShort()
                    val appliesOnVertex = first.readUShort()
                    particleEffectors!![effectorIndex] = EffectiveVertex(effectId, appliesOnVertex)
                }
            }
        }
        if (hasBillboards) {
            val billboardCount = first.readUByte()
            if (billboardCount > 0) {
                billboards = Array(billboardCount) { Billboard(0, 0, 0, 0) }
                for (billboardIndex in 0 until billboardCount) {
                    val id = first.readUShort()
                    val face = first.readUShort()
                    val depth = first.readUByte()
                    val distance = first.readByte()
                    billboards!![billboardIndex] = Billboard(id, face, depth, distance.toInt())
                }
            }
        }
    }

    fun calculateMaxDepth(first: Packet, second: Packet) {
        var a: Short = 0
        var b: Short = 0
        var c: Short = 0
        var tmp: Short = 0
        for (faceIndex in 0 until numFaces) {
            val mappingType = second.readUByte()
            if (mappingType == 1) {
                a = (first.readSmart() + tmp).toShort()
                b = (first.readSmart() + a).toShort()
                c = (first.readSmart() + b).toShort()
                tmp = c
                facesA!![faceIndex] = a
                facesB!![faceIndex] = b
                facesC!![faceIndex] = c
                if (a > maxDepth) {
                    maxDepth = a.toInt()
                }
                if (b > maxDepth) {
                    maxDepth = b.toInt()
                }
                if (c > maxDepth) {
                    maxDepth = c.toInt()
                }
            }
            if (mappingType == 2) {
                b = c
                c = (first.readSmart() + tmp).toShort()
                tmp = c
                facesA!![faceIndex] = a
                facesB!![faceIndex] = b
                facesC!![faceIndex] = c
                if (c > maxDepth) {
                    maxDepth = c.toInt()
                }
            }
            if (mappingType == 3) {
                a = c
                c = (first.readSmart() + tmp).toShort()
                tmp = c
                facesA!![faceIndex] = a
                facesB!![faceIndex] = b
                facesC!![faceIndex] = c
                if (c > maxDepth) {
                    maxDepth = c.toInt()
                }
            }
            if (mappingType == 4) {
                val s_9 = a
                a = b
                b = s_9
                c = (first.readSmart() + tmp).toShort()
                tmp = c
                facesA!![faceIndex] = a
                facesB!![faceIndex] = s_9
                facesC!![faceIndex] = c
                if (c > maxDepth) {
                    maxDepth = c.toInt()
                }
            }
        }
        maxDepth++
    }

    fun decodeTexturedTriangles(rsbytebuffer1: Packet, rsbytebuffer2: Packet, rsbytebuffer3: Packet, rsbytebuffer4: Packet, rsbytebuffer5: Packet, rsbytebuffer6: Packet) {
        for (textureIndex in 0 until numTextures) {
            val mappingType = texturesMapping!![textureIndex].toInt() and 0xff
            if (mappingType == 0) {
                textureFaceX!![textureIndex] = rsbytebuffer1.readUShort().toShort()
                textureFaceY!![textureIndex] = rsbytebuffer1.readUShort().toShort()
                textureFaceZ!![textureIndex] = rsbytebuffer1.readUShort().toShort()
            }
            if (mappingType == 1) {
                textureFaceX!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                textureFaceY!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                textureFaceZ!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                if (version < 15) {
                    texturesScaleX!![textureIndex] = rsbytebuffer3.readUShort()
                    if (version < 14) {
                        texturesScaleY!![textureIndex] = rsbytebuffer3.readUShort()
                    } else {
                        texturesScaleY!![textureIndex] = rsbytebuffer3.readUMedium()
                    }
                    texturesScaleZ!![textureIndex] = rsbytebuffer3.readUShort()
                } else {
                    texturesScaleX!![textureIndex] = rsbytebuffer3.readUMedium()
                    texturesScaleY!![textureIndex] = rsbytebuffer3.readUMedium()
                    texturesScaleZ!![textureIndex] = rsbytebuffer3.readUMedium()
                }
                texturesRotation!![textureIndex] = rsbytebuffer4.readByte()
                texturesDirection!![textureIndex] = rsbytebuffer5.readByte()
                texturesSpeed!![textureIndex] = rsbytebuffer6.readByte().toInt()
            }
            if (mappingType == 2) {
                textureFaceX!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                textureFaceY!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                textureFaceZ!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                if (version < 15) {
                    texturesScaleX!![textureIndex] = rsbytebuffer3.readUShort()
                    if (version < 14) {
                        texturesScaleY!![textureIndex] = rsbytebuffer3.readUShort()
                    } else {
                        texturesScaleY!![textureIndex] = rsbytebuffer3.readUMedium()
                    }
                    texturesScaleZ!![textureIndex] = rsbytebuffer3.readUShort()
                } else {
                    texturesScaleX!![textureIndex] = rsbytebuffer3.readUMedium()
                    texturesScaleY!![textureIndex] = rsbytebuffer3.readUMedium()
                    texturesScaleZ!![textureIndex] = rsbytebuffer3.readUMedium()
                }
                texturesRotation!![textureIndex] = rsbytebuffer4.readByte()
                texturesDirection!![textureIndex] = rsbytebuffer5.readByte()
                texturesSpeed!![textureIndex] = rsbytebuffer6.readByte().toInt()
                texturesUtrans!![textureIndex] = rsbytebuffer6.readByte().toInt()
                texturesVtrans!![textureIndex] = rsbytebuffer6.readByte().toInt()
            }
            if (mappingType == 3) {
                textureFaceX!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                textureFaceY!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                textureFaceZ!![textureIndex] = rsbytebuffer2.readUShort().toShort()
                if (version < 15) {
                    texturesScaleX!![textureIndex] = rsbytebuffer3.readUShort()
                    if (version < 14) {
                        texturesScaleY!![textureIndex] = rsbytebuffer3.readUShort()
                    } else {
                        texturesScaleY!![textureIndex] = rsbytebuffer3.readUMedium()
                    }
                    texturesScaleZ!![textureIndex] = rsbytebuffer3.readUShort()
                } else {
                    texturesScaleX!![textureIndex] = rsbytebuffer3.readUMedium()
                    texturesScaleY!![textureIndex] = rsbytebuffer3.readUMedium()
                    texturesScaleZ!![textureIndex] = rsbytebuffer3.readUMedium()
                }
                texturesRotation!![textureIndex] = rsbytebuffer4.readByte()
                texturesDirection!![textureIndex] = rsbytebuffer5.readByte()
                texturesSpeed!![textureIndex] = rsbytebuffer6.readByte().toInt()
            }
        }
    }

    fun addVertex(x: Int, y: Int, z: Int): Int {
        for (verticeIndex in 0 until numVertices) {
            if (verticesX!![verticeIndex] == x && y == verticesY!![verticeIndex] && z == verticesZ!![verticeIndex]) {
                return verticeIndex
            }
        }
        verticesX!![numVertices] = x
        verticesY!![numVertices] = y
        verticesZ!![numVertices] = z
        maxDepth = numVertices + 1
        return numVertices++
    }

    fun addFace(faceA: Int, faceB: Int, faceC: Int, faceRenderType: Byte, faceTexture: Byte, faceColor: Short, faceAlpha: Byte, faceMaterial: Short): Int {
        this.facesA!![numFaces] = faceA.toShort()
        this.facesB!![numFaces] = faceB.toShort()
        this.facesC!![numFaces] = faceC.toShort()
        this.faceRenderType!![numFaces] = faceRenderType
        this.facesTexture!![numFaces] = faceTexture
        this.facesColor!![numFaces] = faceColor
        this.facesAlpha!![numFaces] = faceAlpha
        this.facesMaterial!![numFaces] = faceMaterial
        return numFaces++
    }

    fun addSunTexture(): Byte {
        if (numTextures >= 255) {
            throw IllegalStateException()
        }
        texturesMapping!![numTextures] = 3
        textureFaceX!![numTextures] = 0
        textureFaceY!![numTextures] = 32767
        textureFaceZ!![numTextures] = 0
        texturesScaleX!![numTextures] = 1024
        texturesScaleY!![numTextures] = 1024
        texturesScaleZ!![numTextures] = 1024
        texturesRotation!![numTextures] = 0
        texturesDirection!![numTextures] = 0
        texturesSpeed!![numTextures] = 0
        return (numTextures++).toByte()
    }

    /**
     * @author Tamatea
     * https://github.com/tamateea/RuneBlend
     */
    fun createVertexGroups(fromNumVertices: Boolean): Array<IntArray> {
        val vertexGroupCount = IntArray(256)
        var count = 0
        val max = if (fromNumVertices) numVertices else maxDepth
        for (v in 0 until max) {
            val group = vertexGroups!![v]
            if (group >= 0) {
                vertexGroupCount[group]++
                if (group > count) {
                    count = group
                }
            }
        }
        val groupedVertices = Array(count + 1) { IntArray(0) }
        for (group in 0..count) {
            groupedVertices[group] = IntArray(vertexGroupCount[group])
            vertexGroupCount[group] = 0
        }
        for (v in 0 until max) {
            val group = vertexGroups!![v]
            if (group >= 0) {
                groupedVertices[group][vertexGroupCount[group]++] = v
            }
        }
        return groupedVertices
    }

    /**
     * @author Tamatea
     * https://github.com/tamateea/RuneBlend
     */
    fun createFaceGroups(): Array<IntArray> {
        val faceGroupCount = IntArray(256)
        var count = 0
        for (face in 0 until numFaces) {
            val group = faceGroups!![face]
            if (group >= 0) {
                faceGroupCount[group]++
                if (group > count) {
                    count = group
                }
            }
        }
        val groupedFaces = Array(count + 1) { IntArray(0) }
        for (group in 0..count) {
            groupedFaces[group] = IntArray(faceGroupCount[group])
            faceGroupCount[group] = 0
        }
        for (face in 0 until numFaces) {
            val group = faceGroups!![face]
            if (group >= 0) {
                groupedFaces[group][faceGroupCount[group]++] = face
            }
        }
        return groupedFaces
    }

    fun createBillboardGroups(): Array<IntArray> {
        val billboardGroupCount = IntArray(256)
        var count = 0
        for (billboard in billboards!!.indices) {
            val group = billboards!![billboard].depth
            if (group >= 0) {
                billboardGroupCount[group]++
                if (group > count) {
                    count = group
                }
            }
        }
        val groupedBillboards = Array(count + 1) { IntArray(0) }
        for (group in 0..count) {
            groupedBillboards[group] = IntArray(billboardGroupCount[group])
            billboardGroupCount[group] = 0
        }
        for (billboard in billboards!!.indices) {
            val group = billboards!![billboard].depth
            if (group >= 0) {
                groupedBillboards[group][billboardGroupCount[group]++] = billboard
            }
        }
        return groupedBillboards
    }

    fun recolor(src: Short, dst: Short) {
        for (faceIndex in 0 until numFaces) {
            if (facesColor!![faceIndex] == src) {
                facesColor!![faceIndex] = dst
            }
        }
    }

    fun retexture(src: Short, dst: Short) {
        if (facesMaterial != null) {
            for (faceIndex in 0 until numFaces) {
                if (facesMaterial!![faceIndex] == src) {
                    facesMaterial!![faceIndex] = dst
                }
            }
        }
    }

    fun rotateWithoutNormals(x: Int, y: Int, z: Int) {
        if (z != 0) {
            val i_4 = MathUtils.SINE[z]
            val i_5 = MathUtils.COSINE[z]
            for (v in 0 until numVertices) {
                val i_7 = i_4 * verticesY!![v] + i_5 * verticesX!![v] shr 14
                verticesY!![v] = i_5 * verticesY!![v] - i_4 * verticesX!![v] shr 14
                verticesX!![v] = i_7
            }
        }
        if (x != 0) {
            val i_4 = MathUtils.SINE[x]
            val i_5 = MathUtils.COSINE[x]
            for (v in 0 until numVertices) {
                val i_7 = i_5 * verticesY!![v] - i_4 * verticesZ!![v] shr 14
                verticesZ!![v] = i_4 * verticesY!![v] + i_5 * verticesZ!![v] shr 14
                verticesY!![v] = i_7
            }
        }
        if (y != 0) {
            val i_4 = MathUtils.SINE[y]
            val i_5 = MathUtils.COSINE[y]
            for (v in 0 until numVertices) {
                val i_7 = i_4 * verticesZ!![v] + i_5 * verticesX!![v] shr 14
                verticesZ!![v] = i_5 * verticesZ!![v] - i_4 * verticesX!![v] shr 14
                verticesX!![v] = i_7
            }
        }
    }

    fun decodeOldFormat(bytes_1: ByteArray) {
        var hasFaceTypes = false
        var hasMaterials = false
        val rsbytebuffer4 = Packet(bytes_1)
        val rsbytebuffer5 = Packet(bytes_1)
        val rsbytebuffer6 = Packet(bytes_1)
        val rsbytebuffer7 = Packet(bytes_1)
        val rsbytebuffer8 = Packet(bytes_1)
        rsbytebuffer4.currentPosition = bytes_1.size - 18
        numVertices = rsbytebuffer4.readUShort()
        numFaces = rsbytebuffer4.readUShort()
        numTextures = rsbytebuffer4.readUByte()
        val i_9 = rsbytebuffer4.readUByte()
        val i_10 = rsbytebuffer4.readUByte()
        val i_11 = rsbytebuffer4.readUByte()
        val i_12 = rsbytebuffer4.readUByte()
        val i_13 = rsbytebuffer4.readUByte()
        val i_14 = rsbytebuffer4.readUShort()
        val i_15 = rsbytebuffer4.readUShort()
        val i_16 = rsbytebuffer4.readUShort()
        val i_17 = rsbytebuffer4.readUShort()
        val b_18: Byte = 0
        var i_42 = b_18 + numVertices
        val i_20 = i_42
        i_42 += numFaces
        val i_21 = i_42
        if (i_10 == 255) {
            i_42 += numFaces
        }
        val i_22 = i_42
        if (i_12 == 1) {
            i_42 += numFaces
        }
        val i_23 = i_42
        if (i_9 == 1) {
            i_42 += numFaces
        }
        val i_24 = i_42
        if (i_13 == 1) {
            i_42 += numVertices
        }
        val i_25 = i_42
        if (i_11 == 1) {
            i_42 += numFaces
        }
        val i_26 = i_42
        i_42 += i_17
        val i_27 = i_42
        i_42 += numFaces * 2
        val i_28 = i_42
        i_42 += numTextures * 6
        val i_29 = i_42
        i_42 += i_14
        val i_30 = i_42
        i_42 += i_15
        val i_10000 = i_42 + i_16
        verticesX = IntArray(numVertices)
        verticesY = IntArray(numVertices)
        verticesZ = IntArray(numVertices)
        facesA = ShortArray(numFaces)
        facesB = ShortArray(numFaces)
        facesC = ShortArray(numFaces)
        if (numTextures > 0) {
            texturesMapping = ByteArray(numTextures)
            textureFaceX = ShortArray(numTextures)
            textureFaceY = ShortArray(numTextures)
            textureFaceZ = ShortArray(numTextures)
        }
        if (i_13 == 1) {
            vertexGroups = IntArray(numVertices)
        }
        if (i_9 == 1) {
            faceRenderType = ByteArray(numFaces)
            facesTexture = ByteArray(numFaces)
            facesMaterial = ShortArray(numFaces)
        }
        if (i_10 == 255) {
            facesLayer = ByteArray(numFaces)
        } else {
            priority = i_10.toByte()
        }
        if (i_11 == 1) {
            facesAlpha = ByteArray(numFaces)
        }
        if (i_12 == 1) {
            faceGroups = IntArray(numFaces)
        }
        facesColor = ShortArray(numFaces)
        rsbytebuffer4.currentPosition = b_18.toInt()
        rsbytebuffer5.currentPosition = i_29
        rsbytebuffer6.currentPosition = i_30
        rsbytebuffer7.currentPosition = i_42
        rsbytebuffer8.currentPosition = i_24
        var i_32 = 0
        var i_33 = 0
        var i_34 = 0
        var i_35: Int
        var i_36: Int
        var i_39: Int
        i_35 = 0
        while (i_35 < numVertices) {
            i_36 = rsbytebuffer4.readUByte()
            var i_37 = 0
            if ((i_36 and 0x1) != 0) {
                i_37 = rsbytebuffer5.readSmart()
            }
            var i_38 = 0
            if ((i_36 and 0x2) != 0) {
                i_38 = rsbytebuffer6.readSmart()
            }
            i_39 = 0
            if ((i_36 and 0x4) != 0) {
                i_39 = rsbytebuffer7.readSmart()
            }
            verticesX!![i_35] = i_32 + i_37
            verticesY!![i_35] = i_33 + i_38
            verticesZ!![i_35] = i_34 + i_39
            i_32 = verticesX!![i_35]
            i_33 = verticesY!![i_35]
            i_34 = verticesZ!![i_35]
            if (i_13 == 1) {
                vertexGroups!![i_35] = rsbytebuffer8.readUByte()
            }
            i_35++
        }
        rsbytebuffer4.currentPosition = i_27
        rsbytebuffer5.currentPosition = i_23
        rsbytebuffer6.currentPosition = i_21
        rsbytebuffer7.currentPosition = i_25
        rsbytebuffer8.currentPosition = i_22
        i_35 = 0
        while (i_35 < numFaces) {
            facesColor!![i_35] = rsbytebuffer4.readUShort().toShort()
            if (i_9 == 1) {
                i_36 = rsbytebuffer5.readUByte()
                if ((i_36 and 0x1) == 1) {
                    faceRenderType!![i_35] = 1
                    hasFaceTypes = true
                } else {
                    faceRenderType!![i_35] = 0
                }
                if ((i_36 and 0x2) == 2) {
                    facesTexture!![i_35] = (i_36 shr 2).toByte()
                    facesMaterial!![i_35] = facesColor!![i_35]
                    facesColor!![i_35] = 127
                    if (facesMaterial!![i_35] != (-1).toShort()) {
                        hasMaterials = true
                    }
                } else {
                    facesTexture!![i_35] = -1
                    facesMaterial!![i_35] = -1
                }
            }
            if (i_10 == 255) {
                facesLayer!![i_35] = rsbytebuffer6.readByte()
            }
            if (i_11 == 1) {
                facesAlpha!![i_35] = rsbytebuffer7.readByte()
            }
            if (i_12 == 1) {
                faceGroups!![i_35] = rsbytebuffer8.readUByte()
            }
            i_35++
        }
        maxDepth = -1
        rsbytebuffer4.currentPosition = i_26
        rsbytebuffer5.currentPosition = i_20
        var s_43: Short = 0
        var s_44: Short = 0
        var s_45: Short = 0
        var s_46: Short = 0
        var i_40: Int
        i_39 = 0
        while (i_39 < numFaces) {
            i_40 = rsbytebuffer5.readUByte()
            if (i_40 == 1) {
                s_43 = (rsbytebuffer4.readSmart() + s_46).toShort()
                s_44 = (rsbytebuffer4.readSmart() + s_43).toShort()
                s_45 = (rsbytebuffer4.readSmart() + s_44).toShort()
                s_46 = s_45
                facesA!![i_39] = s_43
                facesB!![i_39] = s_44
                facesC!![i_39] = s_45
                if (s_43 > maxDepth) {
                    maxDepth = s_43.toInt()
                }
                if (s_44 > maxDepth) {
                    maxDepth = s_44.toInt()
                }
                if (s_45 > maxDepth) {
                    maxDepth = s_45.toInt()
                }
            }
            if (i_40 == 2) {
                s_44 = s_45
                s_45 = (rsbytebuffer4.readSmart() + s_46).toShort()
                s_46 = s_45
                facesA!![i_39] = s_43
                facesB!![i_39] = s_44
                facesC!![i_39] = s_45
                if (s_45 > maxDepth) {
                    maxDepth = s_45.toInt()
                }
            }
            if (i_40 == 3) {
                s_43 = s_45
                s_45 = (rsbytebuffer4.readSmart() + s_46).toShort()
                s_46 = s_45
                facesA!![i_39] = s_43
                facesB!![i_39] = s_44
                facesC!![i_39] = s_45
                if (s_45 > maxDepth) {
                    maxDepth = s_45.toInt()
                }
            }
            if (i_40 == 4) {
                val s_41 = s_43
                s_43 = s_44
                s_44 = s_41
                s_45 = (rsbytebuffer4.readSmart() + s_46).toShort()
                s_46 = s_45
                facesA!![i_39] = s_43
                facesB!![i_39] = s_41
                facesC!![i_39] = s_45
                if (s_45 > maxDepth) {
                    maxDepth = s_45.toInt()
                }
            }
            i_39++
        }
        maxDepth++
        rsbytebuffer4.currentPosition = i_28
        i_39 = 0
        while (i_39 < numTextures) {
            texturesMapping!![i_39] = 0
            textureFaceX!![i_39] = rsbytebuffer4.readUShort().toShort()
            textureFaceY!![i_39] = rsbytebuffer4.readUShort().toShort()
            textureFaceZ!![i_39] = rsbytebuffer4.readUShort().toShort()
            i_39++
        }
        if (facesTexture != null) {
            var hasTextures = false
            i_40 = 0
            while (i_40 < numFaces) {
                val textureIndex = facesTexture!![i_40].toInt() and 0xff
                if (textureIndex != 255) {
                    if (facesA!![i_40] == (textureFaceX!![textureIndex].toInt() and 0xffff).toShort() && facesB!![i_40] == (textureFaceY!![textureIndex].toInt() and 0xffff).toShort() && facesC!![i_40] == (textureFaceZ!![textureIndex].toInt() and 0xffff).toShort()) {
                        facesTexture!![i_40] = -1
                    } else {
                        hasTextures = true
                    }
                }
                i_40++
            }
            if (!hasTextures) {
                facesTexture = null
            }
        }
        if (!hasMaterials) {
            facesMaterial = null
        }
        if (!hasFaceTypes) {
            faceRenderType = null
        }
    }

    fun upscale() {
        for (verticeIndex in 0 until numVertices) {
            verticesX!![verticeIndex] = verticesX!![verticeIndex] shl 2
            verticesY!![verticeIndex] = verticesY!![verticeIndex] shl 2
            verticesZ!![verticeIndex] = verticesZ!![verticeIndex] shl 2
        }
        if (numTextures > 0 && texturesScaleX != null) {
            for (textureIndex in texturesScaleX!!.indices) {
                texturesScaleX!![textureIndex] = texturesScaleX!![textureIndex] shl 2
                texturesScaleY!![textureIndex] = texturesScaleY!![textureIndex] shl 2
                if (texturesMapping!![textureIndex] != 1.toByte()) {
                    texturesScaleZ!![textureIndex] = texturesScaleZ!![textureIndex] shl 2
                }
            }
        }
    }

    fun translate(deltaX: Int, deltaY: Int, deltaZ: Int) {
        for (i in 0 until numVertices) {
            verticesX!![i] += deltaX
            verticesY!![i] += deltaY
            verticesZ!![i] += deltaZ
        }
    }

    companion object {
        @JvmStatic
        fun fromResource(index: Js5ResourceProvider, fileId: Int): Mesh? {
            val data = index.getFile(fileId, 0)
            return if (data == null) null else Mesh(data)
        }
    }
}
