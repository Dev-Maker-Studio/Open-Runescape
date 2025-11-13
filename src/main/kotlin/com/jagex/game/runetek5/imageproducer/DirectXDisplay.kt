package com.jagex.game.runetek5.imageproducer

/**
 *
 * @author Brainpower - 22/04/2022
 *
 */
interface DirectXDisplay {
    fun setWindowed()
    fun cleanup()
    fun reload(): Boolean
}
