/* IDirect3DIndexBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

/**
 * IDirect3DIndexBuffer is an interface for accessing and managing index buffers in DirectX.
 */
public class IDirect3DIndexBuffer extends IUnknown {

    /**
     * Creates a new IDirect3DIndexBuffer instance.
     * @throws Throwable if an error occurs during instantiation.
     */
    public IDirect3DIndexBuffer() throws Throwable {
        throw new Error();
    }

    /**
     * Locks a range of indices in an index buffer.
     * @param indexBuffer the index buffer to lock.
     * @param offset the offset from the beginning of the buffer to the first index to lock.
     * @param size the number of indices to lock.
     * @param flags a set of flags that determine how the buffer is locked.
     * @return a pointer to the locked indices.
     */
    public static native long Lock(long indexBuffer, int offset, int size, int flags);

    /**
     * Unlocks an index buffer that was previously locked.
     * @param indexBuffer the index buffer to unlock.
     */
    public static native void Unlock(long indexBuffer);

    /**
     * Uploads data to an index buffer.
     * @param indexBuffer the index buffer to upload data to.
     * @param offset the offset from the beginning of the buffer to start uploading data to.
     * @param size the size of the data to upload.
     * @param flags a set of flags that determine how the data is uploaded.
     * @param data a pointer to the data to upload.
     * @return 0 if the upload was successful, or a negative value if an error occurred.
     */
    public static native int Upload(long indexBuffer, int offset, int size, int flags, long data);

    /**
     * Downloads data from an index buffer.
     * @param indexBuffer the index buffer to download data from.
     * @param offset the offset from the beginning of the buffer to start downloading data from.
     * @param size the size of the data to download.
     * @param flags a set of flags that determine how the data is downloaded.
     * @param data a pointer to the buffer to store the downloaded data in.
     * @return 0 if the download was successful, or a negative value if an error occurred.
     */
    public static native int Download(long indexBuffer, int offset, int size, int flags, long data);
}

