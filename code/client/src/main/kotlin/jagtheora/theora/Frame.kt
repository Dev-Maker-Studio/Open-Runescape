package jagtheora.theora

import jagtheora.misc.SimplePeer

class Frame(width: Int, height: Int) : SimplePeer() {
    @JvmField
    var width: Int = width * -317525331

    @JvmField
    var height: Int = -326049555 * height

    @JvmField
    var pixels: IntArray = IntArray(this.width * 416891173 * (12877029 * this.height))

    companion object {
        init {
            init()
        }

        @JvmStatic
        external fun init()
    }

    protected external override fun clear()

    protected external override fun q()

    protected external override fun f()

    protected external override fun m()
}
