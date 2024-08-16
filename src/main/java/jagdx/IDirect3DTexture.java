/* IDirect3DTexture - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

/**
 * This class represents an IDirect3DTexture object in Direct3D.
 * IDirect3DTexture is a base class for Direct3D textures.
 */
public class IDirect3DTexture extends IDirect3DBaseTexture {

    /**
     * Constructs a new IDirect3DTexture object.
     * @throws Throwable if an error occurs while constructing the object
     */
    public IDirect3DTexture() throws Throwable {
        throw new Error();
    }

    /**
     * Retrieves a surface from the texture, using a zero-based index.
     * @param texture The texture to retrieve the surface from
     * @param level Zero-based index of the level of the texture resource to access
     * @return The surface retrieved from the texture
     */
    public static native long GetSurfaceLevel(long texture, int level);

    /**
     * Uploads data to the specified level of a texture resource.
     * @param texture The texture to upload data to
     * @param level Zero-based index of the level of the texture resource to access
     * @param x X coordinate of the upper-left corner of the texture subregion to update
     * @param y Y coordinate of the upper-left corner of the texture subregion to update
     * @param width Width of the texture subregion to update
     * @param height Height of the texture subregion to update
     * @param depth Depth of the texture subregion to update
     * @param pitch Pitch of the texture subregion to update
     * @param data Pointer to the data to upload
     * @return HRESULT indicating success or failure of the operation
     */
    public static native int Upload(long texture, int level, int x, int y, int width, int height, int depth, int pitch, long data);

    /**
     * Downloads the contents of a texture subregion.
     * @param texture The texture to download data from
     * @param level Zero-based index of the level of the texture resource to access
     * @param x X coordinate of the upper-left corner of the texture subregion to download
     * @param y Y coordinate of the upper-left corner of the texture subregion to download
     * @param width Width of the texture subregion to download
     * @param height Height of the texture subregion to download
     * @param depth Depth of the texture subregion to download
     * @param pitch Pitch of the texture subregion to download
     * @param data Pointer to a buffer to receive the downloaded data
     * @return HRESULT indicating success or failure of the operation
     */
    public static native int Download(long texture, int level, int x, int y, int width, int height, int depth, int pitch, long data);
}
