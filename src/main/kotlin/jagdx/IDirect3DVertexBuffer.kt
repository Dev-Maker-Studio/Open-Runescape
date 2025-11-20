/* IDirect3DVertexBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

/**
 * Represents a Direct3D vertex buffer.
 */
class IDirect3DVertexBuffer : IUnknown {

    /**
     * Constructs a new IDirect3DVertexBuffer object.
     *
     * @throws Throwable if an error occurs
     */
    @Throws(Throwable::class)
    constructor() {
        throw Error()
    }

    companion object {
        /**
         * Locks a range of vertices in the vertex buffer.
         *
         * @param buffer a pointer to the vertex buffer
         * @param offset offset from the beginning of the buffer to the start of the range to lock
         * @param size size of the range to lock
         * @param flags lock options
         * @return a pointer to the locked vertex data
         */
        @JvmStatic
        external fun Lock(buffer: Long, offset: Int, size: Int, flags: Int): Long

        /**
         * Unlocks a previously locked vertex buffer.
         *
         * @param buffer a pointer to the vertex buffer
         */
        @JvmStatic
        external fun Unlock(buffer: Long)

        /**
         * Uploads vertex data from system memory to the vertex buffer.
         *
         * @param buffer a pointer to the vertex buffer
         * @param offset offset from the beginning of the buffer to the start of the data to upload
         * @param size size of the data to upload
         * @param flags upload options
         * @param data a pointer to the data to upload
         * @return HRESULT indicating success or failure
         */
        @JvmStatic
        external fun Upload(buffer: Long, offset: Int, size: Int, flags: Int, data: Long): Int

        /**
         * Downloads vertex data from the vertex buffer to system memory.
         *
         * @param buffer a pointer to the vertex buffer
         * @param offset offset from the beginning of the buffer to the start of the data to download
         * @param size size of the data to download
         * @param flags download options
         * @param data a pointer to the buffer to receive the downloaded data
         * @return HRESULT indicating success or failure
         */
        @JvmStatic
        external fun Download(buffer: Long, offset: Int, size: Int, flags: Int, data: Long): Int
    }
}
