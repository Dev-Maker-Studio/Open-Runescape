package jagtheora.misc

abstract class SimplePeer {
    companion object {
        init {
            init()
        }

        @JvmStatic
        private external fun init()
    }

    @JvmField
    var peer: Long = 0

    protected constructor()

    fun peerPresent(): Boolean {
        return this.peer == 0L
    }

    fun setPeer(peer: Long) {
        this.peer = peer
    }

    fun resetPeer() {
        if (!peerPresent()) {
            clear()
        }
    }

    protected abstract fun clear()

    @Suppress("removal")
    @Deprecated("Deprecated in Java")
    protected fun finalize() {
        try {
            if (!peerPresent()) {
                resetPeer()
            }
        } finally {
        }
    }

    protected abstract fun q()

    protected abstract fun f()

    protected abstract fun m()
}
