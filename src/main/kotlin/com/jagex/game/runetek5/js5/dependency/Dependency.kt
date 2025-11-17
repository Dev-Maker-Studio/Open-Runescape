package com.jagex.game.runetek5.js5.dependency

class Dependency(
    @JvmField var dependencyType: DependencyType
) {
    @JvmField var dependencyLoader: IDependencyLoader? = null
    @JvmField var archiveCount: Int = 1

    fun getArchiveCount(): Int {
        return this.archiveCount
    }

    fun inializeDependency(dependencyLoader: IDependencyLoader) {
        if (dependencyLoader.getDependencyType() != this.dependencyType) {
            throw IllegalArgumentException()
        }
        this.dependencyLoader = dependencyLoader
    }

    fun setArchiveCount(archiveCount: Int) {
        this.archiveCount = archiveCount
    }

    fun getDependency(): IDependencyLoader? {
        return this.dependencyLoader
    }
}
