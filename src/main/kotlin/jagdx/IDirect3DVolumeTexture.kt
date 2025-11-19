/* IDirect3DVolumeTexture - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

internal open class IDirect3DVolumeTexture : IDirect3DBaseTexture() {
    init {
        throw Error()
    }

    companion object {
        /**
         * Uploads a buffer to the specified device context.
         *
         * @param deviceContext a handle to the device context to upload the buffer to.
         * @param dstX the x-coordinate of the upper-left corner of the destination rectangle.
         * @param dstY the y-coordinate of the upper-left corner of the destination rectangle.
         * @param dstZ the z-coordinate of the upper-left corner of the destination rectangle.
         * @param width the width of the source and destination rectangles.
         * @param height the height of the source and destination rectangles.
         * @param depth the depth of the source and destination rectangles.
         * @param srcData a pointer to the source data in memory.
         * @param srcRowPitch the distance between the start of each row in bytes.
         * @param srcDepthPitch the distance between the start of each depth level in bytes.
         * @param dstResource a handle to the destination resource.
         *
         * @return HRESULT code indicating success or failure of the operation.
         */
        @JvmStatic
        external fun Upload(deviceContext: Long, dstX: Int, dstY: Int, dstZ: Int, width: Int, height: Int, depth: Int, srcData: Long, srcRowPitch: Int, srcDepthPitch: Int, dstResource: Long): Int

        /**
         * Downloads a buffer from the specified device context.
         *
         * @param deviceContext a handle to the device context to download the buffer from.
         * @param srcX the x-coordinate of the upper-left corner of the source rectangle.
         * @param srcY the y-coordinate of the upper-left corner of the source rectangle.
         * @param srcZ the z-coordinate of the upper-left corner of the source rectangle.
         * @param width the width of the source and destination rectangles.
         * @param height the height of the source and destination rectangles.
         * @param depth the depth of the source and destination rectangles.
         * @param dstData a pointer to the destination memory where the downloaded buffer will be stored.
         * @param dstRowPitch the distance between the start of each row in bytes.
         * @param dstDepthPitch the distance between the start of each depth level in bytes.
         * @param srcResource a handle to the source resource.
         *
         * @return HRESULT code indicating success or failure of the operation.
         */
        @JvmStatic
        external fun Download(deviceContext: Long, srcX: Int, srcY: Int, srcZ: Int, width: Int, height: Int, depth: Int, dstData: Long, dstRowPitch: Int, dstDepthPitch: Int, srcResource: Long): Int
    }
}
