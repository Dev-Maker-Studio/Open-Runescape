package com.jagex.game.runetek5.graphics.model

object ModelFunctionMaskDX {

    @JvmStatic
    fun copyVerticesY(i_0: Int): Boolean {
        return (i_0 and 0x22) != 0
    }

    @JvmStatic
    fun copyVerticesZ(i_0: Int): Boolean {
        return (i_0 and 0x34) != 0
    }

    @JvmStatic
    fun method981(i_0: Int): Boolean {
        return (i_0 and 0x37) != 0
    }

    @JvmStatic
    fun method982(i_0: Int): Boolean {
        return ((i_0 and 0x220) == 544) or ((i_0 and 0x18) != 0)
    }

    @JvmStatic
    fun method983(i_0: Int, i_1: Int): Boolean {
        return (i_0 and 0x800) != 0 && (i_1 and 0x37) != 0
    }

    @JvmStatic
    fun copyAlpha(i_0: Int): Boolean {
        return (i_0 and 0x100) != 0
    }

    @JvmStatic
    fun deepCloneUv(): Boolean {
        return false
    }

    @JvmStatic
    fun deepCloneNormals(i_0: Int): Boolean {
        return (i_0 and 0x10) != 0
    }

    @JvmStatic
    fun method987(i_0: Int, i_1: Int): Boolean {
        return copyColors(i_0) || method1051(i_0, i_1)
    }

    @JvmStatic
    fun copyBillboardBackgrounds(i_0: Int): Boolean {
        return (i_0 and 0xc580) != 0
    }

    @JvmStatic
    fun method989(i_0: Int): Boolean {
        return (i_0 and 0x10000) != 0
    }

    @JvmStatic
    fun method990(i_0: Int): Boolean {
        return (i_0 and 0x800) != 0
    }

    @JvmStatic
    fun deepCloneMaterials(i_0: Int): Boolean {
        return (i_0 and 0x8000) != 0
    }

    @JvmStatic
    fun method993(i_0: Int): Boolean {
        return (i_0 and 0x800) != 0
    }

    @JvmStatic
    fun copyVerticesX(i_0: Int): Boolean {
        return (i_0 and 0x21) != 0
    }

    @JvmStatic
    fun method995(i_0: Int): Boolean {
        return (copyVerticesX(i_0) or copyVerticesY(i_0) or copyVerticesZ(i_0)) and method990(i_0)
    }

    @JvmStatic
    fun method996(i_0: Int, i_1: Int): Boolean {
        val bool2 = if ((i_1 and 0x37) != 0) deepCloneLights(i_0, i_1) else method1051(i_0, i_1)
        return ((i_0 and 0x10000) != 0) or method982(i_0) or bool2
    }

    @JvmStatic
    fun hasNormalsBuffer(i_0: Int): Boolean {
        return deepCloneNormals(i_0) and method1043(i_0)
    }

    @JvmStatic
    fun method1000(i_0: Int): Boolean {
        return (i_0 and 0x70000) != 0 || copyVerticesX(i_0) || method995(i_0)
    }

    @JvmStatic
    fun method1001(i_0: Int): Boolean {
        return (i_0 and 0x70000) != 0 || copyVerticesY(i_0) || method995(i_0)
    }

    @JvmStatic
    fun method1002(i_0: Int): Boolean {
        return (i_0 and 0x70000) != 0 || copyVerticesZ(i_0) || method995(i_0)
    }

    @JvmStatic
    fun method1004(i_0: Int, i_1: Int): Boolean {
        return ((i_0 and 0x40000) != 0) or copyAlpha(i_0) || method1051(i_0, i_1)
    }

    @JvmStatic
    fun method1005(i_0: Int): Boolean {
        return deepCloneUv() || method1056(i_0)
    }

    @JvmStatic
    fun is0x180(i_0: Int): Boolean {
        return (i_0 and 0x180) != 0
    }

    @JvmStatic
    fun is0x20(i_0: Int): Boolean {
        return (i_0 and 0x20) != 0
    }

    @JvmStatic
    fun is0x400(i_0: Int): Boolean {
        return (i_0 and 0x400) != 0
    }

    @JvmStatic
    fun method1009(i_0: Int): Boolean {
        return ((i_0 and 0x60000) != 0) or deepCloneNormals(i_0) || hasNormalsBuffer(i_0)
    }

    @JvmStatic
    fun method1010(i_0: Int, i_1: Int): Boolean {
        return ((i_0 and 0x800) != 0) or deepCloneMaterials(i_0) || method1051(i_0, i_1)
    }

    @JvmStatic
    fun deepCloneLights(i_0: Int, i_1: Int): Boolean {
        return method983(i_0, i_1) and (((i_0 and 0x2000) != 0) or method982(i_0) or method989(i_0))
    }

    @JvmStatic
    fun copyColors(i_0: Int): Boolean {
        return (i_0 and 0x84080) != 0
    }

    @JvmStatic
    fun method1043(i_0: Int): Boolean {
        return (i_0 and 0x800) != 0
    }

    @JvmStatic
    fun method1051(functionMask: Int, preferenceMask: Int): Boolean {
        return method1059(functionMask) && (((functionMask and 0x9000) != 0) or copyColors(functionMask) or copyAlpha(functionMask) || ((preferenceMask and 0x37) == 0) and (((functionMask and 0x2000) != 0) or method982(functionMask) or method989(functionMask)))
    }

    @JvmStatic
    fun method1056(i_0: Int): Boolean {
        return deepCloneUv() and method993(i_0)
    }

    @JvmStatic
    fun method1059(i_0: Int): Boolean {
        return (i_0 and 0x800) != 0
    }
}
