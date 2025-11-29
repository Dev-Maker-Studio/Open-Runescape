package com.jagex.game.runetek5.config.material

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.CacheableNodeQueue
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.config.material.texture.TextureType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class MaterialTypeList(
    materialResource: Js5ResourceProvider,
    val textureResource: Js5ResourceProvider,
    val spriteResource: Js5ResourceProvider
) : MaterialProvider {

    val textureTypeCache = CacheableNodeQueue(256)
    val numMaterials: Int
    val textures: Array<MaterialType?>

    init {
        val materialBuffer = Packet(materialResource.getFile(0, 0))
        numMaterials = materialBuffer.readUShort()
        textures = arrayOfNulls(numMaterials)

        for (index in 0 until numMaterials) {
            if (materialBuffer.readUByte() == 1) {
                textures[index] = MaterialType()
            }
        }

        for (index in 0 until numMaterials) {
            textures[index]?.detailsOnly = materialBuffer.readUByte() == 0
        }

        for (index in 0 until numMaterials) {
            textures[index]?.isSmall = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            textures[index]?.skipTriangles = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            textures[index]?.brightness = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.alpha = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.effectId = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.effectParam1 = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.colorHsl = materialBuffer.readUShort().toShort()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.textureSpeedU = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.textureSpeedV = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.aBool2087 = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            textures[index]?.isBrickTile = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            textures[index]?.mipmapping = materialBuffer.readByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.repeatS = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            textures[index]?.repeatT = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            textures[index]?.hdr = materialBuffer.readUByte() == 1
        }

        for (index in 0 until numMaterials) {
            textures[index]?.combineMode = materialBuffer.readUByte()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.effectParam2 = materialBuffer.readInt()
        }

        for (index in 0 until numMaterials) {
            textures[index]?.effectCombiner = materialBuffer.readUByte()
        }
    }

    fun getTextureType(textureId: Int): TextureType? {
        val cachedTextureType = textureTypeCache.get(textureId.toLong()) as? TextureType
        if (cachedTextureType != null) {
            return cachedTextureType
        }

        val data = textureResource.getFile(textureId) ?: return null
        val textureType = TextureType(Packet(data))
        textureTypeCache.put(textureType, textureId.toLong())
        return textureType
    }

    override fun getSize(): Int = numMaterials

    override fun getMaterial(textureId: Int): MaterialType {
        return textures[textureId] ?: throw RuntimeException("Material not found: $textureId")
    }

    override fun cycle() {
        // Empty implementation
    }

    override fun getPixelsArgb(textureId: Int, width: Int, height: Int): IntArray {
        val textureType = getTextureType(textureId)
            ?: throw RuntimeException("Texture type not found: $textureId")
        return textureType.getPixelsArgb(
            spriteResource,
            this,
            width,
            height,
            textures[textureId]?.isBrickTile ?: false
        )
    }

    override fun getPixelsHdr(textureId: Int, width: Int, height: Int): FloatArray {
        val textureType = getTextureType(textureId)
            ?: throw RuntimeException("Texture type not found: $textureId")
        return textureType.getPixelsHdr(
            spriteResource,
            this,
            width,
            height,
            textures[textureId]?.isBrickTile ?: false
        )
    }

    override fun isCached(textureId: Int): Boolean {
        val textureType = getTextureType(textureId) ?: return false
        return textureType.isCached(spriteResource, this)
    }

    override fun getPixels(
        materialId: Int,
        brightness: Float,
        width: Int,
        length: Int,
        readReversed: Boolean
    ): IntArray {
        val textureType = getTextureType(materialId)
            ?: throw RuntimeException("Texture type not found: $materialId")
        return textureType.getPixelsRgb(
            spriteResource,
            this,
            brightness.toDouble(),
            width,
            length,
            readReversed,
            textures[materialId]?.isBrickTile ?: false
        )
    }

    companion object {
        @JvmStatic
        fun isOpaqueEffect(effectId: Int): Boolean {
            return effectId != 1 && effectId != 7
        }
    }
}
