/* IDirect3DSurface - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

/**
 * IDirect3DSurface represents a 2D image that can be used as a texture or a
 * render target.
 */
class IDirect3DSurface : IUnknown {

    /**
     * Creates a new IDirect3DSurface object.
     *
     * @throws Throwable
     */
    constructor() {
        throw Error()
    }

    companion object {
        /**
         * Uploads a portion of the contents of the given source buffer to this surface.
         *
         * @param surfacePtr       A pointer to the IDirect3DSurface object to upload
         *                         to.
         * @param xOffset          The x-coordinate of the upper-left corner of the
         *                         target rectangle.
         * @param yOffset          The y-coordinate of the upper-left corner of the
         *                         target rectangle.
         * @param width            The width of the source and target rectangles.
         * @param height           The height of the source and target rectangles.
         * @param srcStride        The stride of the source buffer in bytes.
         * @param srcBytesPerPixel The number of bytes per pixel in the source buffer.
         * @param srcBufferPtr     A pointer to the source buffer to upload from.
         * @return An HRESULT indicating the status of the upload operation.
         */
        @JvmStatic
        external fun Upload(
            surfacePtr: Long,
            xOffset: Int,
            yOffset: Int,
            width: Int,
            height: Int,
            srcStride: Int,
            srcBytesPerPixel: Int,
            srcBufferPtr: Long
        ): Int

        /**
         * Downloads a portion of the contents of this surface to the given destination
         * buffer.
         *
         * @param surfacePtr        A pointer to the IDirect3DSurface object to download
         *                          from.
         * @param xOffset           The x-coordinate of the upper-left corner of the
         *                          source rectangle.
         * @param yOffset           The y-coordinate of the upper-left corner of the
         *                          source rectangle.
         * @param width             The width of the source and target rectangles.
         * @param height            The height of the source and target rectangles.
         * @param destStride        The stride of the destination buffer in bytes.
         * @param destBytesPerPixel The number of bytes per pixel in the destination
         *                          buffer.
         * @param destBufferPtr     A pointer to the destination buffer to download to.
         * @return An HRESULT indicating the status of the download operation.
         */
        @JvmStatic
        external fun Download(
            surfacePtr: Long,
            xOffset: Int,
            yOffset: Int,
            width: Int,
            height: Int,
            destStride: Int,
            destBytesPerPixel: Int,
            destBufferPtr: Long
        ): Int
    }
}
