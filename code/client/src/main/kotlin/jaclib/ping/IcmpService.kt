package jaclib.ping

abstract class IcmpService : Runnable {
    protected constructor()

    protected abstract fun notify(state: Int)

    protected abstract fun notify(bitpackedPosition: Int, meta: Int, dummyAlways0: Int)

    external override fun run()

    external fun quit()

    external fun m()

    external fun h()

    protected abstract fun method11597(state: Int)

    protected abstract fun method11598(state: Int)

    protected abstract fun method11599(bitpackedPosition: Int, meta: Int, dummyAlways0: Int)

    protected abstract fun method11600(bitpackedPosition: Int, meta: Int, dummyAlways0: Int)

    companion object {
        @JvmStatic
        external fun available(): Boolean
    }
}
