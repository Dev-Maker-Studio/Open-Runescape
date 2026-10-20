package com.jagex.game.runetek5.graphics.model

object ModelFunctionMask {
    const val DISPLACE_X = 0x1
    const val DISPLACE_Y = 0x2
    const val DISPLACE_Z = 0x4

    const val DISPLACE_NORMALS = 0x8
    const val INVERT = 0x10
    const val ALTERS_VERTICES = 0x20
    const val DISABLE_TEXTURES = 0x40
    const val ALTERS_COLOR = 0x80
    const val ALTERS_ALPHA = 0x100
    const val ALTERS_LIGHT = 0x200
    const val ALTERS_BILLBOARDS = 0x400
    const val HAS_TEXTURE_COORDS = 0x800

    const val ALTERS_AMBIENT = 0x1000
    const val ALTERS_CONTRAST = 0x2000
    const val RECOLOR = 0x4000
    const val RETEXTURE = 0x8000
    const val SHADED = 0x10000
     const val PROJECTION = 0x20000
    const val SHADOWED = 0x40000
    const val TINT = 0x80000
    const val ALTERS_LIGHT_ALPHA = 0x100000

    const val TST = SHADOWED or PROJECTION or SHADED
    const val FULL_DISPLACEMENT = DISPLACE_X or DISPLACE_Y or DISPLACE_Z
    const val ROTATE_WITH_NORMALS = DISPLACE_NORMALS or DISPLACE_X or DISPLACE_Z
    const val ALTERS_FACES = ALTERS_COLOR or ALTERS_ALPHA

    const val RESET_COLOR = DISPLACE_NORMALS or INVERT or ALTERS_COLOR or
                           ALTERS_AMBIENT or ALTERS_CONTRAST or TINT or
                           SHADED or RECOLOR
}
