/* D3DPRESENT_PARAMETERS - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

import java.awt.Component

class D3DPRESENT_PARAMETERS(
    @JvmField val DeviceWindow: Component
) {
    @JvmField var BackBufferWidth: Int = 0
    @JvmField var BackBufferHeight: Int = 0
    @JvmField var BackBufferFormat: Int = 0
    @JvmField var BackBufferCount: Int = 0
    @JvmField var MultiSampleType: Int = 0
    @JvmField var MultiSampleQuality: Int = 0
    @JvmField var SwapEffect: Int = 1
    @JvmField var Windowed: Boolean = false
    @JvmField var EnableAutoDepthStencil: Boolean = false
    @JvmField var AutoDepthStencilFormat: Int = 0
    @JvmField var Flags: Int = 0
    @JvmField var FullScreenRefreshRateInHz: Int = 0
    @JvmField var PresentationInterval: Int = anInt529

    companion object {
        const val anInt529: Int = -2147483648
    }
}
