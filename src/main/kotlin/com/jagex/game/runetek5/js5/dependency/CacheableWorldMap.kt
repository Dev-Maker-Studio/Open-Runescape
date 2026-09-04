package com.jagex.game.runetek5.js5.dependency

import com.jagex.game.runetek5.js5.Js5ResourceProvider

class CacheableWorldMap(
    @JvmField val resourceProvider: Js5ResourceProvider
) : IDependencyLoader {

    @JvmField
    val archiveName: String = "details"

    override fun getDependencyType(): DependencyType {
        return DependencyType.LABELS
    }

    override fun getCompletion(): Int {
        return if (resourceProvider.isCached(archiveName)) 100 else resourceProvider.getArchiveLoadingPercent(archiveName)
    }
}
