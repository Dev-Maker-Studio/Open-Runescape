/* IUnknown - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

open class IUnknown {
    init {
        throw Error()
    }

    companion object {
        @JvmStatic
        external fun AddRef(l: Long): Int

        @JvmStatic
        external fun Release(l: Long): Int
    }
}
