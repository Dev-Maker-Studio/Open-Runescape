/* IDirect3DIndexBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

/**
 * IDirect3DIndexBuffer is an interface for accessing and managing index buffers in DirectX.
 */
class IDirect3DIndexBuffer : IUnknown {

    /**
     * Creates a new IDirect3DIndexBuffer instance.
     * @throws Throwable if an error occurs during instantiation.
     */
    constructor() {
        throw Error()
    }

    companion object {
        /**
         * Locks a range of indices in an index buffer.
         * @param indexBuffer the index buffer to lock.
         * @param offset the offset from the beginning of the buffer to the first index to lock.
         * @param size the number of indices to lock.
         * @param flags a set of flags that determine how the buffer is locked.
         * @return a pointer to the locked indices.
         */
        @JvmStatic
        external fun Lock(indexBuffer: Long, offset: Int, size: Int, flags: Int): Long

        /**
         * Unlocks an index buffer that was previously locked.
         * @param indexBuffer the index buffer to unlock.
         */
        @JvmStatic
        external fun Unlock(indexBuffer: Long)

        /**
         * Uploads data to an index buffer.
         * @param indexBuffer the index buffer to upload data to.
         * @param offset the offset from the beginning of the buffer to start uploading data to.
         * @param size the size of the data to upload.
         * @param flags a set of flags that determine how the data is uploaded.
         * @param data a pointer to the data to upload.
         * @return 0 if the upload was successful, or a negative value if an error occurred.
         */
        @JvmStatic
        external fun Upload(indexBuffer: Long, offset: Int, size: Int, flags: Int, data: Long): Int

        /**
         * Downloads data from an index buffer.
         * @param indexBuffer the index buffer to download data from.
         * @param offset the offset from the beginning of the buffer to start downloading data from.
         * @param size the size of the data to download.
         * @param flags a set of flags that determine how the data is downloaded.
         * @param data a pointer to the buffer to store the downloaded data in.
         * @return 0 if the download was successful, or a negative value if an error occurred.
         */
        @JvmStatic
        external fun Download(indexBuffer: Long, offset: Int, size: Int, flags: Int, data: Long): Int
    }
}
