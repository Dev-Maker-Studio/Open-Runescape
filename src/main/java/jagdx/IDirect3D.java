/* IDirect3D - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

import java.awt.*;

public class IDirect3D extends IUnknown {
    public IDirect3D() throws Throwable {
        throw new Error();
    }

    /**
     * Returns a pointer to the Direct3D COM object.
     *
     * @return a pointer to the Direct3D COM object.
     */
    public static native long Direct3DCreate();

    /**
     * Gets the number of display adapters available on the system.
     *
     * @param d3d the pointer to the Direct3D COM object.
     * @return the number of display adapters.
     */
    public static native int GetAdapterCount(long d3d);

    /**
     * Gets the identifier for the display adapter at the specified index.
     *
     * @param d3d the pointer to the Direct3D COM object.
     * @param adapterIndex the index of the display adapter to query.
     * @param flags the flags to use when querying the adapter.
     * @param adapterIdentifier the D3DADAPTER_IDENTIFIER object to receive the adapter information.
     * @return 0 on success, otherwise a Direct3D error code.
     */
    public static native int GetAdapterIdentifier(long d3d, int adapterIndex, int flags, D3DADAPTER_IDENTIFIER adapterIdentifier);

    /**
     * Gets the capabilities of the specified device type for the specified display adapter.
     *
     * @param d3d the pointer to the Direct3D COM object.
     * @param adapterIndex the index of the display adapter to query.
     * @param deviceType the device type to query (e.g. D3DDEVTYPE_HAL for hardware acceleration).
     * @param deviceCaps the D3DCAPS object to receive the device capabilities.
     * @return 0 on success, otherwise a Direct3D error code.
     */
    public static native int GetDeviceCaps(long d3d, int adapterIndex, int deviceType, D3DCAPS deviceCaps);

    /**
     * Gets the current display mode of the specified display adapter.
     *
     * @param d3d the pointer to the Direct3D COM object.
     * @param adapterIndex the index of the display adapter to query.
     * @param displayMode the D3DDISPLAYMODE object to receive the current display mode.
     * @return 0 on success, otherwise a Direct3D error code.
     */
    public static native int GetAdapterDisplayMode(long d3d, int adapterIndex, D3DDISPLAYMODE displayMode);

    /**
     * Creates a Direct3D device and returns a pointer to the device's COM object.
     *
     * @param d3d the pointer to the Direct3D COM object.
     * @param adapterIndex the index of the display adapter to use.
     * @param deviceType the device type to use (e.g. D3DDEVTYPE_HAL for hardware acceleration).
     * @param component the Component object representing the rendering target.
     * @param behaviorFlags the behavior flags to use when creating the device.
     * @param presentationParameters the D3DPRESENT_PARAMETERS object representing the presentation parameters.
     * @return a pointer to the Direct3D device COM object.
     */
    public static native long CreateDeviceContext(long d3d, int adapterIndex, int deviceType, Component component, int behaviorFlags, D3DPRESENT_PARAMETERS presentationParameters);

    /**
     * Determines if a device with the specified capabilities can be created.
     *
     * @param d3d The Direct3D object.
     * @param adapterIndex The index of the adapter to check.
     * @param deviceType The type of device to check.
     * @param displayFormat The pixel format of the display.
     * @param backBufferFormat The pixel format of the back buffer.
     * @param windowed True to create a windowed device, false to create a full-screen device.
     * @return 0 if the device can be created, otherwise an error code.
     */
    public static native int CheckDeviceType(long d3d, int adapterIndex, int deviceType, int displayFormat, int backBufferFormat, boolean windowed);

    /**
     * Determines if a surface of the specified format can be created as a texture, depth-stencil buffer, or render target.
     *
     * @param d3d The Direct3D object.
     * @param adapterIndex The index of the adapter to check.
     * @param deviceType The type of device to check.
     * @param adapterFormat The pixel format of the adapter.
     * @param usage The intended usage of the surface.
     * @param resourceType The type of resource to create.
     * @param checkFormat The format to check.
     * @return 0 if the surface can be created, otherwise an error code.
     */
    public static native int CheckDeviceFormat(long d3d, int adapterIndex, int deviceType, int adapterFormat, int usage, int resourceType, int checkFormat);

    /**
     * Determines if a depth-stencil format is compatible with a specified adapter format and render-target format.
     *
     * @param d3d The Direct3D object.
     * @param adapterIndex The index of the adapter to check.
     * @param deviceType The type of device to check.
     * @param adapterFormat The pixel format of the adapter.
     * @param renderTargetFormat The pixel format of the render target.
     * @param depthStencilFormat The depth-stencil format to check.
     * @return 0 if the format is compatible, otherwise an error code.
     */
    public static native int CheckDepthStencilMatch(long d3d, int adapterIndex, int deviceType, int adapterFormat, int renderTargetFormat, int depthStencilFormat);

    /**
     * Determines if a specified level of multisampling is supported for a given device, surface format, and display mode.
     *
     * @param d3d The Direct3D object.
     * @param adapterIndex The index of the adapter to check.
     * @param deviceType The type of device to check.
     * @param surfaceFormat The pixel format of the surface.
     * @param windowed True to check for windowed mode, false to check for full-screen mode.
     * @param multiSampleType The level of multisampling to check.
     * @return 0 if the level is supported, otherwise an error code.
     */
    public static native int CheckDeviceMultiSampleType(long d3d, int adapterIndex, int deviceType, int surfaceFormat, boolean windowed, int multiSampleType);

}
