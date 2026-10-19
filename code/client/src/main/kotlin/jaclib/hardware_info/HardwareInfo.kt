/* HardwareInfo - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.hardware_info

class HardwareInfo private constructor() {
    init {
        throw Error()
    }

    companion object {
        @JvmStatic
        external fun getCPUInfo(): IntArray

        @JvmStatic
        external fun getRawCPUInfo(): IntArray

        @JvmStatic
        external fun getDXDiagSystemProps(): Array<String>

        @JvmStatic
        external fun getDXDiagDisplayDevicesProps(): Array<Array<String>>

        @JvmStatic
        external fun getOpenGLProps(): Array<String>
    }
}
