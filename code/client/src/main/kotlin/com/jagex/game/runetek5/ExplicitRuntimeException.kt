package com.jagex.game.runetek5

@Suppress("serial")
class ExplicitRuntimeException(
    @JvmField val throwable: Throwable,
    @JvmField val error: String
) : RuntimeException() {
    init {
        initCause(throwable)
    }
}
