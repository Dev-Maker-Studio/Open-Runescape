package com.jagex.game.runetek5.config.`object`

/**
 * [J]
 * @author Steven - 22/11/2023
 *
 */
class ObjectTypeCustomisation(
    @JvmField val id: Long,
    @JvmField val objectModelIds: IntArray?,
    @JvmField val recolorDst: ShortArray?,
    @JvmField val retexDst: ShortArray?
) {
    companion object {
        @JvmField
        var locCustomisationId: Long = 1L
    }
}
