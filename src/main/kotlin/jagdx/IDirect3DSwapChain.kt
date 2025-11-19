/* IDirect3DSwapChain - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

/**
 * IDirect3DSwapChain represents the swap chain that contains the back buffers
 * of a Direct3D device.
 */
class IDirect3DSwapChain : IUnknown {

    /**
     * Constructs a new IDirect3DSwapChain object.
     *
     * @throws Throwable if an error occurs.
     */
    constructor() : super() {
        throw Error()
    }

    companion object {
        /**
         * Retrieves a back buffer from the swap chain.
         *
         * @param swapChainPointer  pointer to the IDirect3DSwapChain object.
         * @param backBufferIndex   index of the back buffer to retrieve.
         * @param backBufferPointer a pointer to an IDirect3DSurface9 interface,
         *                          representing the back buffer surface.
         * @return 0 on success, otherwise an error code is returned.
         */
        @JvmStatic
        external fun GetBackBuffer(swapChainPointer: Long, backBufferIndex: Int, backBufferPointer: Int): Long

        /**
         * Presents the contents of the next buffer in the sequence of back buffers
         * owned by the swap chain.
         *
         * @param swapChainPointer    pointer to the IDirect3DSwapChain object.
         * @param hDestWindowOverride handle of the destination window to which the
         *                            contents of the next buffer should be presented.
         *                            If NULL, the swap chain's present target window
         *                            will be used.
         * @return 0 on success, otherwise an error code is returned.
         */
        @JvmStatic
        external fun Present(swapChainPointer: Long, hDestWindowOverride: Int): Int
    }
}
