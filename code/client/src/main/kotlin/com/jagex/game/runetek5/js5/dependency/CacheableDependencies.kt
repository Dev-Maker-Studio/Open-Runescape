package com.jagex.game.runetek5.js5.dependency

object CacheableDependencies {
    @JvmField val DEFAULTS = Dependency(DependencyType.CONFIG)
    @JvmField val JACLIB = Dependency(DependencyType.NATIVE)
    @JvmField val JAGGL = Dependency(DependencyType.NATIVE)
    @JvmField val JAGDX = Dependency(DependencyType.NATIVE)
    @JvmField val SW3D = Dependency(DependencyType.NATIVE)
    @JvmField val HW3D = Dependency(DependencyType.NATIVE)
    @JvmField val JAGTHEORA = Dependency(DependencyType.NATIVE)
    @JvmField val SHADERS = Dependency(DependencyType.CONFIG)
    @JvmField val MATERIALS = Dependency(DependencyType.CONFIG)
    @JvmField val CONFIG = Dependency(DependencyType.CONFIG)
    @JvmField val LOC = Dependency(DependencyType.CONFIG)
    @JvmField val ENUM = Dependency(DependencyType.CONFIG)
    @JvmField val NPC = Dependency(DependencyType.CONFIG)
    @JvmField val OBJ = Dependency(DependencyType.CONFIG)
    @JvmField val SEQ = Dependency(DependencyType.CONFIG)
    @JvmField val GFX = Dependency(DependencyType.CONFIG)
    @JvmField val VARBIT = Dependency(DependencyType.CONFIG)
    @JvmField val QC_MSG = Dependency(DependencyType.CONFIG)
    @JvmField val QC_MENU = Dependency(DependencyType.CONFIG)
    @JvmField val TEXTURE_MEDIA = Dependency(DependencyType.CONFIG)
    @JvmField val PARTICLES = Dependency(DependencyType.CONFIG)
    @JvmField val BILLBOARDS = Dependency(DependencyType.CONFIG)
    @JvmField val HUFFMAN = Dependency(DependencyType.ENCODING)
    @JvmField val INTERFACES = Dependency(DependencyType.CONFIG)
    @JvmField val CS2 = Dependency(DependencyType.CONFIG)
    @JvmField val FONTS = Dependency(DependencyType.CONFIG)
    @JvmField val WORLD_MAP = Dependency(DependencyType.LABELS)

    @JvmStatic
    fun getAll(): Array<Dependency> {
        return arrayOf(
            DEFAULTS, JACLIB, JAGGL, JAGDX, SW3D, HW3D, JAGTHEORA,
            SHADERS, MATERIALS, CONFIG, LOC, ENUM, NPC, OBJ, SEQ, GFX,
            VARBIT, QC_MSG, QC_MENU, TEXTURE_MEDIA, PARTICLES, BILLBOARDS,
            HUFFMAN, INTERFACES, CS2, FONTS, WORLD_MAP
        )
    }
}
