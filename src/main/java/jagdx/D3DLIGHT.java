/* D3DLIGHT - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

/**
 * 
 * The D3DLIGHT class represents a light object in a Direct3D application.
 */
public class D3DLIGHT {

	/**
	 * 
	 * Constructs a new D3DLIGHT object.
	 * 
	 * @throws Error if an error occurred while creating the object.
	 */
	public D3DLIGHT() throws Error {
		throw new Error();
	}

	/**
	 * 
	 * Creates a new D3DLIGHT object and returns a pointer to it.
	 * 
	 * @return A pointer to the created D3DLIGHT object.
	 */
	public static native long Create();

	/**
	 * 
	 * Destroys the specified D3DLIGHT object.
	 * 
	 * @param light The pointer to the D3DLIGHT object to destroy.
	 */
	public static native void Destroy(long light);

	/**
	 * 
	 * Sets the type of the specified D3DLIGHT object.
	 * 
	 * @param light The pointer to the D3DLIGHT object.
	 * @param type  The type of the light to set.
	 */
	public static native void SetType(long light, int type);

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
	public static native void SetAmbient(long light, float r, float g, float b, float a);

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
	public static native void SetSpecular(long light, float r, float g, float b, float a);

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
	public static native void SetDiffuse(long light, float r, float g, float b, float a);

	/**
	 * 
	 * Sets the position of the specified D3DLIGHT object.
	 * 
	 * @param light The pointer to the D3DLIGHT object.
	 * @param x     The x-coordinate of the light position.
	 * @param y     The y-coordinate of the light position.
	 * @param z     The z-coordinate of the light position.
	 */
	public static native void SetPosition(long light, float x, float y, float z);

	/**
	 * 
	 * Sets the direction of the specified D3DLIGHT object.
	 * 
	 * @param light The pointer to the D3DLIGHT object.
	 * @param x     The x-component of the light direction.
	 * @param y     The y-component of the light direction.
	 * @param z     The z-component of the light direction.
	 */
	public static native void SetDirection(long light, float x, float y, float z);

	/**
	 * Sets the attenuation parameters for a D3D light.
	 *
	 * @param light     the address of the D3D light
	 * @param constant  the constant attenuation factor
	 * @param linear    the linear attenuation factor
	 * @param quadratic the quadratic attenuation factor
	 */
	public static native void SetAttenuation(long light, float constant, float linear, float quadratic);

	/**
	 * Sets the spotlight parameters for a D3D light.
	 *
	 * @param light    the address of the D3D light
	 * @param exponent the exponent for the spotlight falloff
	 * @param cutoff   the angle cutoff for the spotlight
	 * @param theta    the inner cone angle for the spotlight
	 */
	public static native void SetSpotParams(long light, float exponent, float cutoff, float theta);

	/**
	 * Sets the range for a D3D light.
	 *
	 * @param light the address of the D3D light
	 * @param range the range of the light
	 */
	public static native void SetRange(long light, float range);

}
