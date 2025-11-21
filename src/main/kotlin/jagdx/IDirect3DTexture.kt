/* IDirect3DTexture - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

/**
 * This class represents an IDirect3DTexture object in Direct3D.
 * IDirect3DTexture is a base class for Direct3D textures.
 */
internal class IDirect3DTexture : IDirect3DBaseTexture {

    /**
     * Constructs a new IDirect3DTexture object.
     * @throws Throwable if an error occurs while constructing the object
     */
    constructor() {
        throw Error()
    }

    companion object {
        /**
         * Retrieves a surface from the texture, using a zero-based index.
         * @param texture The texture to retrieve the surface from
         * @param level Zero-based index of the level of the texture resource to access
         * @return The surface retrieved from the texture
         */
        @JvmStatic
        external fun GetSurfaceLevel(texture: Long, level: Int): Long

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
        @JvmStatic
        external fun Upload(
            texture: Long,
            level: Int,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            depth: Int,
            pitch: Int,
            data: Long
        ): Int

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
        @JvmStatic
        external fun Download(
            texture: Long,
            level: Int,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            depth: Int,
            pitch: Int,
            data: Long
        ): Int
    }
}
