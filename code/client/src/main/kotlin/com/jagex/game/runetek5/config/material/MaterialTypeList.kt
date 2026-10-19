package com.jagex.game.runetek5.config.material

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.CacheableNodeQueue
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.config.material.texture.TextureType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class MaterialTypeList(
    materialResource: Js5ResourceProvider,
    textureResource: Js5ResourceProvider,
    spriteResource: Js5ResourceProvider
) : MaterialProvider {

    val textureTypeCache = CacheableNodeQueue(256)
    var textureResource: Js5ResourceProvider = textureResource
        private set
    var spriteResource: Js5ResourceProvider = spriteResource
        private set
    var numMaterials: Int = 0
        private set
    var materials: Array<MaterialType?> = emptyArray()
        private set

    init {
        load(materialResource)
    }

    fun rebind(materialResource: Js5ResourceProvider, textureResource: Js5ResourceProvider, spriteResource: Js5ResourceProvider) {
        this.textureResource = textureResource
        this.spriteResource = spriteResource
        textureTypeCache.reset()
        load(materialResource)
    }

    private fun load(materialResource: Js5ResourceProvider) {
        val materialBuffer = Packet(materialResource.getFile(0, 0)!!)
        numMaterials = materialBuffer.readUShort()
        materials = arrayOfNulls(numMaterials)

        for (index in 0 until numMaterials) {
            if (materialBuffer.readUByte() == 1) {
                materials[index] = MaterialType()
            }
        }

        for (index in 0 until numMaterials) {
            materials[index]?.detailsOnly = materialBuffer.readUByte() == 0
        }

        for (index in 0 until numMaterials) {
            materials[index]?.isSmall = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            materials[index]?.skipTriangles = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            materials[index]?.brightness = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.alpha = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.effectId = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.effectParam1 = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.colorHsl = materialBuffer.readUShort().toShort()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.textureSpeedU = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.textureSpeedV = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.aBool2087 = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            materials[index]?.isBrickTile = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            materials[index]?.mipmapping = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.repeatS = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            materials[index]?.repeatT = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            materials[index]?.hdr = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            materials[index]?.combineMode = materialBuffer.readUByte()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.effectParam2 = materialBuffer.readInt()
        }

        for (index in 0 until numMaterials) {
            materials[index]?.effectCombiner = materialBuffer.readUByte()
        }
    }

    fun getTextureType(materialId: Int): TextureType? {
        val cachedTextureType = textureTypeCache.get(materialId.toLong()) as? TextureType
        if (cachedTextureType != null) {
            return cachedTextureType
        }

        val data = textureResource.getFile(materialId) ?: return null
        val textureType = TextureType(Packet(data))
        textureTypeCache.put(textureType, materialId.toLong())
        return textureType
    }

    override fun getSize(): Int = numMaterials

    override fun getMaterial(materialId: Int): MaterialType {
        return materials[materialId] ?: throw RuntimeException("Material not found: $materialId")
    }

    override fun cycle() {
        // Empty implementation
    }

    override fun getPixelsArgb(materialId: Int, width: Int, height: Int): IntArray {
        val textureType = getTextureType(materialId)
            ?: throw RuntimeException("Texture type not found: $materialId")
        return textureType.getPixelsArgb(
            spriteResource,
            this,
            width,
            height,
            materials[materialId]?.isBrickTile ?: false
        )
    }

    override fun getPixelsHdr(materialId: Int, width: Int, height: Int): FloatArray {
        val textureType = getTextureType(materialId)
            ?: throw RuntimeException("Texture type not found: $materialId")
        return textureType.getPixelsHdr(
            spriteResource,
            this,
            width,
            height,
            materials[materialId]?.isBrickTile ?: false
        )
    }

    override fun isCached(materialId: Int): Boolean {
        val textureType = getTextureType(materialId) ?: return false
        return textureType.isCached(spriteResource, this)
    }

    override fun getPixels(
        materialId: Int,
        brightness: Float,
        width: Int,
        height: Int,
        readReversed: Boolean
    ): IntArray {
        val textureType = getTextureType(materialId)
            ?: throw RuntimeException("Texture type not found: $materialId")
        return textureType.getPixelsRgb(
            spriteResource,
            this,
            brightness.toDouble(),
            width,
            height,
            readReversed,
            materials[materialId]?.isBrickTile ?: false
        )
    }

    companion object {
        @JvmStatic
        fun isOpaqueEffect(effectId: Int): Boolean {
            return effectId != 1 && effectId != 7
        }
    }
}
