package com.jagex.game.runetek5.js5.dependency

import com.jagex.game.runetek5.js5.Js5ResourceProvider

class CacheableResource(
    @JvmField val resourceProvider: Js5ResourceProvider
) : IDependencyLoader {

    override fun getDependencyType(): DependencyType {
        return DependencyType.CONFIG
    }

    override fun getCompletion(): Int {
        return if (resourceProvider.fullyLoaded()) 100 else resourceProvider.getLoadingPercentage()
    }
}
