package com.jagex.game.runetek5

/**
 *
 * @author Brainpower - 28/01/2023
 *
 */
@Suppress("serial")
class ExplicitRuntimeException(
    @JvmField val throwable: Throwable,
    @JvmField val error: String
) : RuntimeException() {
    init {
        initCause(throwable)
    }
}
