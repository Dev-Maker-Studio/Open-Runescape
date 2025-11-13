package com.jagex.game.runetek5.client.input.keyboard

abstract class KeyRecorder {
    abstract fun prepareAwaitingKeys()

    abstract fun held(key: Int): Boolean

    abstract fun popHead(): KeyRecord

    abstract fun addKeyListeners()

    companion object {
        @JvmField
        var keyRecorder: KeyRecorder? = null
    }
}
