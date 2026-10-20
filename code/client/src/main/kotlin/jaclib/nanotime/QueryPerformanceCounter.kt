package jaclib.nanotime

class QueryPerformanceCounter private constructor() {
    init {
        throw Error()
    }

    companion object {
        @JvmStatic
        external fun init(): Boolean

        @JvmStatic
        external fun nanoTime(): Long

        @JvmStatic
        external fun quit()
    }
}
