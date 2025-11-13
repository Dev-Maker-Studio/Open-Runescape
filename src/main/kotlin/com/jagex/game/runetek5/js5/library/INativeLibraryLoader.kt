package com.jagex.game.runetek5.js5.library

interface INativeLibraryLoader {
    fun isJagdxLoaded(): Boolean
    fun shutdown(): Boolean
    fun loadIntoMemory(libraryName: String): Boolean
}
