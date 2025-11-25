package com.jagex.game.runetek5.graphics.font

import java.util.HashMap

import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.TypeResult
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 *
 * @author Brainpower - 13/09/2021
 *
 */
class FontCombo(
    @JvmField val loadingFontResource: Js5ResourceProvider,
    @JvmField val fontmetricsResource: Js5ResourceProvider,
    @JvmField val fontFileIds: IntArray?
) {

    @JvmField var fontMap: MutableMap<Int, TypeResult>? = null
    @JvmField val cache = LRUCache(20)

    fun initializeFontMap(fontprovider: FontProvider) {
        fontMap = HashMap(fontFileIds!!.size)
        for (index in fontFileIds.indices) {
            val fileId = fontFileIds[index]
            val fontMetrics = FontMetrics.list(fontmetricsResource, fileId)
            val fontData = loadingFontResource.getFile(fileId)
            val fontRenderer = fontprovider.createFontRenderer(fontData, fontMetrics, true)
            fontMap!![index] = TypeResult(fontRenderer, fontMetrics)
        }
    }

    fun getNumCached(): Int {
        return getNumCached(false)
    }

    fun getNumCached(direct: Boolean): Int {
        if (fontFileIds == null) {
            return 0
        }
        if (!direct && fontMap != null) {
            return fontFileIds.size * 2
        }
        var numCached = 0
        for (index in fontFileIds.indices) {
            val fileId = fontFileIds[index]
            if (loadingFontResource.isFileCached(fileId)) {
                numCached++
            }
            if (fontmetricsResource.isFileCached(fileId)) {
                numCached++
            }
        }
        return numCached
    }

    fun listFontCombo(fontProvider: FontProvider, fontId: Int, includeFontmetrics: Boolean, monospaced: Boolean): TypeResult? {
        if (fontId == -1) {
            return null
        }
        if (fontFileIds != null) {
            for (index in fontFileIds.indices) {
                if (fontId == fontFileIds[index]) {
                    return fontMap!![index]
                }
            }
        }
        var typeResult = cache.get((fontId.toLong() shl 1) or if (monospaced) 1 else 0) as TypeResult?
        if (typeResult != null) {
            if (includeFontmetrics && typeResult.companionObject == null) {
                val companionMetrics = FontMetrics.list(fontmetricsResource, fontId)
                    ?: return null
                typeResult.companionObject = companionMetrics
            }
            return typeResult
        }
        val loadingFontData = loadingFontResource.getFile(fontId) ?: return null
        val fontMetrics = FontMetrics.list(fontmetricsResource, fontId) ?: return null
        typeResult = if (includeFontmetrics) {
            TypeResult(fontProvider.createFontRenderer(loadingFontData, fontMetrics, monospaced), fontMetrics)
        } else {
            TypeResult(fontProvider.createFontRenderer(loadingFontData, fontMetrics, monospaced), null)
        }
        cache.put(typeResult, (fontId.toLong() shl 1) or if (monospaced) 1 else 0)
        return typeResult
    }

    fun resetCache() {
        cache.reset()
    }

    fun sweepCache() {
        cache.clean(50)
    }

    fun clearCacheQueue() {
        cache.resize()
    }

    fun list(fontProvider: FontProvider, fontId: Int): FontMetrics? {
        val typeResult = listFontCombo(fontProvider, fontId, true, true)
        return if (typeResult == null) null else typeResult.companionObject as FontMetrics?
    }

    fun getFontCount(): Int {
        return if (fontFileIds == null) 0 else fontFileIds.size * 2
    }

    fun list(fontProvider: FontProvider, fontId: Int, includeFontmetrics: Boolean, monospaced: Boolean): Any? {
        val typeResult = listFontCombo(fontProvider, fontId, includeFontmetrics, monospaced)
        return typeResult?.mainObject
    }

    fun resetFontMap() {
        fontMap = null
    }
}
