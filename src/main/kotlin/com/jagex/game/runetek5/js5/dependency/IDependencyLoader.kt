package com.jagex.game.runetek5.js5.dependency

interface IDependencyLoader {
    fun getCompletion(): Int
    fun getDependencyType(): DependencyType
}
