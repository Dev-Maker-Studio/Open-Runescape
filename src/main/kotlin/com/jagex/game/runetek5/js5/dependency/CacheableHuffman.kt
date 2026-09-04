package com.jagex.game.runetek5.js5.dependency

import com.jagex.game.runetek5.js5.Js5ResourceProvider

class CacheableHuffman(
    @JvmField val resourceProvider: Js5ResourceProvider
) : IDependencyLoader {

    @JvmField
    val fileName: String = "huffman"

    override fun getCompletion(): Int {
        return if (resourceProvider.isNamedCached(fileName)) 100 else 0
    }

    override fun getDependencyType(): DependencyType {
        return DependencyType.ENCODING
    }
}
