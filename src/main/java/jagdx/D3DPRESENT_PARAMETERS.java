/* D3DPRESENT_PARAMETERS - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

import java.awt.*;

public class D3DPRESENT_PARAMETERS {
    public static final int anInt529 = -2147483648;
    public int BackBufferWidth;
    public int BackBufferHeight;
    public int BackBufferFormat;
    public int BackBufferCount;
    public int MultiSampleType;
    public int MultiSampleQuality;
    public int SwapEffect = 1;
    public final Component DeviceWindow;
    public boolean Windowed;
    public boolean EnableAutoDepthStencil;
    public int AutoDepthStencilFormat;
    public int Flags;
    public int FullScreenRefreshRateInHz;
    public int PresentationInterval = -2147483648;

    public D3DPRESENT_PARAMETERS(Component component) {
        this.DeviceWindow = component;
    }
}
