/* IDirect3DCubeTexture - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

/**
 * The IDirect3DCubeTexture class represents a cube texture in DirectX.
 * It inherits from the IDirect3DBaseTexture class.
 */
public class IDirect3DCubeTexture extends IDirect3DBaseTexture {
    
    /**
     * Constructs an IDirect3DCubeTexture object.
     * @throws Throwable if an error occurs.
     */
    public IDirect3DCubeTexture() throws Throwable {
        throw new Error();
    }

    /**
     * Uploads data to a cube texture resource.
     * @param textureHandle The handle to the cube texture resource to upload data to.
     * @param faceIndex The index of the face of the cube texture to upload data to.
     * @param x The x-coordinate of the upper-left corner of the region to upload data to.
     * @param y The y-coordinate of the upper-left corner of the region to upload data to.
     * @param w The width of the region to upload data to.
     * @param h The height of the region to upload data to.
     * @param level The level of the cube texture to upload data to.
     * @param pitch The pitch of the data being uploaded.
     * @param slicePitch The pitch of the slice data being uploaded.
     * @param data The data to upload.
     * @return An HRESULT indicating success or failure of the operation.
     */
    public static native int Upload(long textureHandle, int faceIndex, int x, int y, int w, int h, int level, int pitch, int slicePitch, long data);

    /**
     * Downloads data from a cube texture resource.
     * @param textureHandle The handle to the cube texture resource to download data from.
     * @param faceIndex The index of the face of the cube texture to download data from.
     * @param x The x-coordinate of the upper-left corner of the region to download data from.
     * @param y The y-coordinate of the upper-left corner of the region to download data from.
     * @param w The width of the region to download data from.
     * @param h The height of the region to download data from.
     * @param level The level of the cube texture to download data from.
     * @param pitch The pitch of the data being downloaded.
     * @param slicePitch The pitch of the slice data being downloaded.
     * @param data The buffer to store the downloaded data in.
     * @return An HRESULT indicating success or failure of the operation.
     */
    public static native int Download(long textureHandle, int faceIndex, int x, int y, int w, int h, int level, int pitch, int slicePitch, long data);
}
