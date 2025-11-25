/* D3DLIGHT - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

/**
 *
 * The D3DLIGHT class represents a light object in a Direct3D application.
 */
class D3DLIGHT private constructor() {

    companion object {
        /**
         *
         * Creates a new D3DLIGHT object and returns a pointer to it.
         *
         * @return A pointer to the created D3DLIGHT object.
         */
        @JvmStatic
        external fun Create(): Long

        /**
         *
         * Destroys the specified D3DLIGHT object.
         *
         * @param light The pointer to the D3DLIGHT object to destroy.
         */
        @JvmStatic
        external fun Destroy(light: Long)

        /**
         *
         * Sets the type of the specified D3DLIGHT object.
         *
         * @param light The pointer to the D3DLIGHT object.
         * @param type  The type of the light to set.
         */
        @JvmStatic
        external fun SetType(light: Long, type: Int)

        /**
         *
         * Sets the ambient color of the specified D3DLIGHT object.
         *
         * @param light The pointer to the D3DLIGHT object.
         * @param r     The red component of the ambient color.
         * @param g     The green component of the ambient color.
         * @param b     The blue component of the ambient color.
         * @param a     The alpha component of the ambient color.
         */
        @JvmStatic
        external fun SetAmbient(light: Long, r: Float, g: Float, b: Float, a: Float)

        /**
         *
         * Sets the specular color of the specified D3DLIGHT object.
         *
         * @param light The pointer to the D3DLIGHT object.
         * @param r     The red component of the specular color.
         * @param g     The green component of the specular color.
         * @param b     The blue component of the specular color.
         * @param a     The alpha component of the specular color.
         */
        @JvmStatic
        external fun SetSpecular(light: Long, r: Float, g: Float, b: Float, a: Float)

        /**
         *
         * Sets the diffuse color of the specified D3DLIGHT object.
         *
         * @param light The pointer to the D3DLIGHT object.
         * @param r     The red component of the diffuse color.
         * @param g     The green component of the diffuse color.
         * @param b     The blue component of the diffuse color.
         * @param a     The alpha component of the diffuse color.
         */
        @JvmStatic
        external fun SetDiffuse(light: Long, r: Float, g: Float, b: Float, a: Float)

        /**
         *
         * Sets the position of the specified D3DLIGHT object.
         *
         * @param light The pointer to the D3DLIGHT object.
         * @param x     The x-coordinate of the light position.
         * @param y     The y-coordinate of the light position.
         * @param z     The z-coordinate of the light position.
         */
        @JvmStatic
        external fun SetPosition(light: Long, x: Float, y: Float, z: Float)

        /**
         *
         * Sets the direction of the specified D3DLIGHT object.
         *
         * @param light The pointer to the D3DLIGHT object.
         * @param x     The x-component of the light direction.
         * @param y     The y-component of the light direction.
         * @param z     The z-component of the light direction.
         */
        @JvmStatic
        external fun SetDirection(light: Long, x: Float, y: Float, z: Float)

        /**
         * Sets the attenuation parameters for a D3D light.
         *
         * @param light     the address of the D3D light
         * @param constant  the constant attenuation factor
         * @param linear    the linear attenuation factor
         * @param quadratic the quadratic attenuation factor
         */
        @JvmStatic
        external fun SetAttenuation(light: Long, constant: Float, linear: Float, quadratic: Float)

        /**
         * Sets the spotlight parameters for a D3D light.
         *
         * @param light    the address of the D3D light
         * @param exponent the exponent for the spotlight falloff
         * @param cutoff   the angle cutoff for the spotlight
         * @param theta    the inner cone angle for the spotlight
         */
        @JvmStatic
        external fun SetSpotParams(light: Long, exponent: Float, cutoff: Float, theta: Float)

        /**
         * Sets the range for a D3D light.
         *
         * @param light the address of the D3D light
         * @param range the range of the light
         */
        @JvmStatic
        external fun SetRange(light: Long, range: Float)
    }
}
