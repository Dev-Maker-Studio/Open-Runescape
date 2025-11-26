package com.jagex.game.runetek5.js5.dependency

import com.jagex.game.runetek5.config.Resource

class CacheableDLL(
    @JvmField val nativeName: String
) : IDependencyLoader {

    @JvmField var doneLoading: Boolean = false

    override fun getDependencyType(): DependencyType {
        return DependencyType.NATIVE
    }

    fun isReady(): Boolean {
        return this.doneLoading
    }

    override fun getCompletion(): Int {
        if (this.doneLoading) {
            return 100
        }
        val loadPercentage = Resource.NATIVE_LIBRARY_LOADER!!.getLibraryLoadingPercent(this.nativeName)
        if (loadPercentage >= 0 && loadPercentage <= 100) {
            return loadPercentage
        }
        this.doneLoading = true
        return 100
    }
}
