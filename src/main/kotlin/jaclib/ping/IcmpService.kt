/* IcmpService - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.ping

abstract class IcmpService : Runnable {
    protected constructor()

    protected abstract fun notify(i: Int)

    protected abstract fun notify(i: Int, i0: Int, i1: Int)

    external override fun run()

    external fun quit()

    external fun m()

    external fun h()

    protected abstract fun method11597(i: Int)

    protected abstract fun method11598(i: Int)

    protected abstract fun method11599(i: Int, i2: Int, i3: Int)

    protected abstract fun method11600(i: Int, i4: Int, i5: Int)

    companion object {
        @JvmStatic
        external fun available(): Boolean
    }
}
