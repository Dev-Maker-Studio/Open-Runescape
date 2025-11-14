package com.jagex.game.runetek5.toolkit.opengl

/**
 * @author Steven - 15/10/2023
 */
object BlendType {
    /**
     * No blending
     */
    const val DISABLED = 0

    /**
     * Used for standard alpha blending,
     * creating smooth and transparent effects
     */
    const val ALPHA = 1

    /**
     * Used for additive blending,
     * which brightens and intensifies colors
     */
    const val ADDITIVE = 2

    /**
     * Used for multiplying the source and destination colors,
     * often for shadowing and darkening effects
     */
    const val MULTIPLY = 3
}
