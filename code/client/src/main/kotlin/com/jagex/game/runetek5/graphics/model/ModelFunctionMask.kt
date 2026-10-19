package com.jagex.game.runetek5.graphics.model

/**
 * Bitmask constants defining various model function operations.
 * These masks control model transformations, rendering properties, and visual effects.
 *
 * Usage: import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*
 */
object ModelFunctionMask {
    // Individual displacement masks
    const val DISPLACE_X = 0x1                          // 1
    const val DISPLACE_Y = 0x2                          // 2
    const val DISPLACE_Z = 0x4                          // 4

    /**
     * Displaces normals (may be incorrect)
     */
    const val DISPLACE_NORMALS = 0x8                    // 8
    const val INVERT = 0x10                             // 16
    const val ALTERS_VERTICES = 0x20                    // 32
    const val DISABLE_TEXTURES = 0x40                   // 64
    const val ALTERS_COLOR = 0x80                       // 128
    const val ALTERS_ALPHA = 0x100                      // 256
    const val ALTERS_LIGHT = 0x200                      // 512
    const val ALTERS_BILLBOARDS = 0x400                 // 1024
    const val HAS_TEXTURE_COORDS = 0x800                // 2048

    // Ambient and contrast
    const val ALTERS_AMBIENT = 0x1000                   // 4096
    const val ALTERS_CONTRAST = 0x2000                  // 8192
    const val RECOLOR = 0x4000                          // 16384
    const val RETEXTURE = 0x8000                        // 32768
    const val SHADED = 0x10000                          // 65536
    /*FIXME: */ const val PROJECTION = 0x20000          // 131072
    const val SHADOWED = 0x40000                        // 262144
    const val TINT = 0x80000                            // 524288
    const val ALTERS_LIGHT_ALPHA = 0x100000             // 1048576

    // Merged/composite masks
    const val TST = SHADOWED or PROJECTION or SHADED
    const val FULL_DISPLACEMENT = DISPLACE_X or DISPLACE_Y or DISPLACE_Z                    // 7, 0x7
    const val ROTATE_WITH_NORMALS = DISPLACE_NORMALS or DISPLACE_X or DISPLACE_Z            // 13, 0xd
    const val ALTERS_FACES = ALTERS_COLOR or ALTERS_ALPHA                                   // 384, 0x180

    /**
     * Merged mask for resetting color properties.
     * Combines: DISPLACE_NORMALS, INVERT, ALTERS_COLOR, ALTERS_AMBIENT,
     * ALTERS_CONTRAST, TINT, SHADED, RECOLOR
     */
    const val RESET_COLOR = DISPLACE_NORMALS or INVERT or ALTERS_COLOR or
                           ALTERS_AMBIENT or ALTERS_CONTRAST or TINT or
                           SHADED or RECOLOR                                                // 0x97098, 618648
}
