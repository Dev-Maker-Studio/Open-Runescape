/* OpenGL - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaggl;

import java.awt.*;
import java.util.Hashtable;

public class OpenGL {

	public static final Hashtable<Thread, OpenGL> ASSOCIATED_THREADS = new Hashtable<>();
	long peer;
	public Hashtable<String, String> supportedExtensions;
	public Thread threadPeer;

	public boolean supportsExtension(String extensionName) {
		if (this.supportedExtensions == null) {
			this.supportedExtensions = new Hashtable<>();
			String oglString = glGetString(7939);
			int charIndex = 0;
			while (true) {
				int oglWord = oglString.indexOf(' ', charIndex);
				if (oglWord == -1) {
					break;
				}
				String oglWord2 = oglString.substring(charIndex, oglWord).trim();
				if (!oglWord2.isEmpty()) {
					this.supportedExtensions.put(oglWord2, oglWord2);
				}
				charIndex = 1 + oglWord;
			}
			String string8 = oglString.substring(charIndex).trim();
			if (!string8.isEmpty()) {
				this.supportedExtensions.put(string8, string8);
			}
		}
		return this.supportedExtensions.containsKey(extensionName);
	}

	public synchronized boolean attach() {
		Thread thread = Thread.currentThread();
		if (attachPeer()) {
			OpenGL released = ASSOCIATED_THREADS.put(thread, this);
			if (released != null) {
				released.threadPeer = null;
			}
			this.threadPeer = thread;
			return true;
		}
		return false;
	}

	public synchronized boolean detach() {
		if (this.threadPeer != Thread.currentThread()) {
			return false;
		}
		detachPeer();
		ASSOCIATED_THREADS.remove(this.threadPeer);
		this.threadPeer = null;
		return true;
	}

	/**
	 * 
	 * Enables a specific capability.
	 * 
	 * @param capability the capability to enable
	 */
	public static native void glEnable(int capability);

	/**
	 * 
	 * Disables a specific capability.
	 * 
	 * @param capability the capability to disable
	 */
	public static native void glDisable(int capability);

	/**
	 * 
	 * Specifies whether front or back facing polygons are candidates for culling.
	 * 
	 * @param mode the culling mode to set
	 */
	public static native void glCullFace(int mode);

	/**
	 * 
	 * Pushes the server attribute stack.
	 * 
	 * @param mask the mask to specify which attributes to push
	 */
	public static native void glPushAttrib(int mask);

	/**
	 * 
	 * Pops the server attribute stack.
	 */
	public static native void glPopAttrib();

	/**
	 * 
	 * Specifies the shading model.
	 * 
	 * @param mode the shading model to set
	 */
	public static native void glShadeModel(int mode);

	/**
	 * 
	 * Specifies implementation-specific hints.
	 * 
	 * @param target the hint target
	 * @param hint   the hint value
	 */
	public static native void glHint(int target, int hint);

	/**
	 * 
	 * Blocks until all previous OpenGL commands are completed.
	 */
	public static native void glFinish();

	/**
	 * 
	 * Empties all buffers.
	 */
	public static native void glFlush();

	/**
	 * 
	 * Clears the specified buffers to their preset values.
	 * 
	 * @param mask the mask to specify which buffers to clear
	 */
	public static native void glClear(int mask);

	/**
	 * 
	 * Specifies the clear value for the color buffer.
	 * 
	 * @param red   the red component of the clear color
	 * @param green the green component of the clear color
	 * @param blue  the blue component of the clear color
	 * @param alpha the alpha component of the clear color
	 */
	public static native void glClearColor(float red, float green, float blue, float alpha);

	/**
	 * 
	 * Specifies the clear value for the depth buffer.
	 * 
	 * @param depth the depth value used to clear the depth buffer
	 */
	public static native void glClearDepth(float depth);

	/**
	 * 
	 * Specifies the function used to compare each incoming pixel depth value with
	 * the depth value present in the depth buffer.
	 * 
	 * @param func the comparison function to set
	 */
	public static native void glDepthFunc(int func);

	/**
	 * 
	 * Enables or disables writing into the depth buffer.
	 * 
	 * @param flag whether or not depth buffer writing is enabled
	 */
	public static native void glDepthMask(boolean flag);

	/**
	 * 
	 * Specifies the mapping of depth values from normalized device coordinates to
	 * window coordinates.
	 * 
	 * @param near the mapping of the near clipping plane to window coordinates
	 * @param far  the mapping of the far clipping plane to window coordinates
	 */
	public static native void glDepthRange(float near, float far);

	/**
	 * 
	 * Specifies the alpha comparison function.
	 * 
	 * @param func the alpha comparison function to set
	 * @param ref  the reference alpha value used in the comparison
	 */
	public static native void glAlphaFunc(int func, float ref);

	/**
	 * 
	 * Masks the color channels to be written into the color buffer.
	 * 
	 * @param red   whether or not to write to the red channel
	 * @param green whether or not to write to the green channel
	 * @param blue  whether or not to write to the blue channel
	 * @param alpha whether or not to write to the alpha channel
	 */
	public static native void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

	/**
	 * 
	 * Specifies the color buffer that should be used for reading.
	 * 
	 * @param mode the buffer to set for reading
	 */
	public static native void glReadBuffer(int mode);

	/**
	 * 
	 * Specifies the color buffer that should be used for drawing.
	 * 
	 * @param mode the buffer to set for drawing
	 */
	public static native void glDrawBuffer(int mode);

	/**
	 * Specifies a list of color buffers to be drawn into.
	 * 
	 * @param n       the number of buffers in the list
	 * @param buffers an array containing the indices of the buffers to be drawn
	 *                into
	 * @param offset  the offset into the buffer array
	 */
	public static native void glDrawBuffersARB(int n, int[] buffers, int offset);

	/**
	 * Enables the specified matrix mode.
	 * 
	 * @param mode The matrix mode to enable. Use GL_MODELVIEW, GL_PROJECTION, or
	 *             GL_TEXTURE.
	 */
	public static native void glMatrixMode(int mode);

	/**
	 * Pushes the current matrix stack down by one, duplicating the current matrix.
	 */
	public static native void glPushMatrix();

	/**
	 * Pops the current matrix stack, replacing the current matrix with the one
	 * below it on the stack.
	 */
	public static native void glPopMatrix();

	/**
	 * Replaces the current matrix with the identity matrix.
	 */
	public static native void glLoadIdentity();

	/**
	 * Sets up an orthographic viewing volume.
	 * 
	 * @param left    The coordinate for the left vertical clipping plane.
	 * @param right   The coordinate for the right vertical clipping plane.
	 * @param bottom  The coordinate for the bottom horizontal clipping plane.
	 * @param top     The coordinate for the top horizontal clipping plane.
	 * @param nearVal The distance to the near depth clipping plane.
	 * @param farVal  The distance to the far depth clipping plane.
	 */
	public static native void glOrtho(double left, double right, double bottom, double top, double nearVal,
			double farVal);

	/**
	 * Sets up a perspective viewing frustum.
	 * 
	 * @param left    The coordinate for the left vertical clipping plane.
	 * @param right   The coordinate for the right vertical clipping plane.
	 * @param bottom  The coordinate for the bottom horizontal clipping plane.
	 * @param top     The coordinate for the top horizontal clipping plane.
	 * @param nearVal The distance to the near depth clipping plane.
	 * @param farVal  The distance to the far depth clipping plane.
	 */
	public static native void glFrustum(double left, double right, double bottom, double top, double nearVal,
			double farVal);

	/**
	 * Translates the current matrix.
	 * 
	 * @param x The x translation value.
	 * @param y The y translation value.
	 * @param z The z translation value.
	 */
	public static native void glTranslatef(float x, float y, float z);

	/**
	 * Rotates the current matrix.
	 * 
	 * @param angle The angle of rotation in degrees.
	 * @param x     The x component of the rotation vector.
	 * @param y     The y component of the rotation vector.
	 * @param z     The z component of the rotation vector.
	 */
	public static native void glRotatef(float angle, float x, float y, float z);

	/**
	 * Scales the current matrix.
	 * 
	 * @param x The x scale factor.
	 * @param y The y scale factor.
	 * @param z The z scale factor.
	 */
	public static native void glScalef(float x, float y, float z);

	/**
	 * Replaces the current matrix with the one specified.
	 * 
	 * @param matrix The matrix to load.
	 * @param offset The offset into the array where the matrix starts.
	 */
	public static native void glLoadMatrixf(float[] matrix, int offset);

	/**
	 * Multiplies the current matrix by the one specified.
	 * 
	 * @param matrix The matrix to multiply.
	 * @param offset The offset into the array where the matrix starts.
	 */
	public static native void glMultMatrixf(float[] matrix, int offset);

	/**
	 * Sets the viewport.
	 * 
	 * @param x      The x coordinate of the viewport.
	 * @param y      The y coordinate of the viewport.
	 * @param width  The width of the viewport.
	 * @param height The height of the viewport.
	 */
	public static native void glViewport(int x, int y, int width, int height);

	/**
	 * Sets the scissor box rectangle for rendering
	 *
	 * @param x      The x-coordinate of the lower-left corner of the scissor box
	 * @param y      The y-coordinate of the lower-left corner of the scissor box
	 * @param width  The width of the scissor box
	 * @param height The height of the scissor box
	 */
	public static native void glScissor(int x, int y, int width, int height);

	/**
	 * Generates a contiguous set of empty display lists
	 *
	 * @param range The number of contiguous empty display lists to be generated
	 * @return The first display list that was generated
	 */
	public static native int glGenLists(int range);

	/**
	 * Deletes a set of contiguous display lists
	 *
	 * @param list  The first display list to be deleted
	 * @param range The number of contiguous display lists to be deleted
	 */
	public static native void glDeleteLists(int list, int range);

	/**
	 * Starts defining a new display list
	 *
	 * @param list The display list identifier
	 * @param mode The display list mode: GL_COMPILE or GL_COMPILE_AND_EXECUTE
	 */
	public static native void glNewList(int list, int mode);

	/**
	 * Ends the definition of a display list
	 */
	public static native void glEndList();

	/**
	 * Executes a display list
	 *
	 * @param list The display list identifier
	 */
	public static native void glCallList(int list);

	/**
	 * Starts defining vertices for a new primitive
	 *
	 * @param mode The primitive type: GL_POINTS, GL_LINES, GL_LINE_STRIP,
	 *             GL_LINE_LOOP, GL_TRIANGLES, GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN,
	 *             GL_QUADS, GL_QUAD_STRIP, GL_POLYGON
	 */
	public static native void glBegin(int mode);

	/**
	 * Specifies a 2D vertex
	 *
	 * @param x The x-coordinate of the vertex
	 * @param y The y-coordinate of the vertex
	 */
	public static native void glVertex2i(int x, int y);

	/**
	 * Specifies a 2D vertex
	 *
	 * @param x The x-coordinate of the vertex
	 * @param y The y-coordinate of the vertex
	 */
	public static native void glVertex2f(float x, float y);

	/**
	 * Specifies a 3D vertex
	 *
	 * @param x The x-coordinate of the vertex
	 * @param y The y-coordinate of the vertex
	 * @param z The z-coordinate of the vertex
	 */
	public static native void glVertex3f(float x, float y, float z);

	/**
	 * Specify the current normal vector.
	 *
	 * @param x the x-component of the normal vector.
	 * @param y the y-component of the normal vector.
	 * @param z the z-component of the normal vector.
	 */
	public static native void glNormal3f(float x, float y, float z);

	/**
	 * Set the current color.
	 *
	 * @param r the red component of the color.
	 * @param g the green component of the color.
	 * @param b the blue component of the color.
	 */
	public static native void glColor3f(float r, float g, float b);

	/**
	 * Set the current color with an alpha component.
	 *
	 * @param r the red component of the color.
	 * @param g the green component of the color.
	 * @param b the blue component of the color.
	 * @param a the alpha component of the color.
	 */
	public static native void glColor4f(float r, float g, float b, float a);

	/**
	 * Set the current color, specified as unsigned bytes.
	 *
	 * @param r the red component of the color.
	 * @param g the green component of the color.
	 * @param b the blue component of the color.
	 */
	public static native void glColor3ub(byte r, byte g, byte b);

	/**
	 * Set the current color with an alpha component, specified as unsigned bytes.
	 *
	 * @param r the red component of the color.
	 * @param g the green component of the color.
	 * @param b the blue component of the color.
	 * @param a the alpha component of the color.
	 */
	public static native void glColor4ub(byte r, byte g, byte b, byte a);

	/**
	 * Set the current texture coordinates with two integer values.
	 *
	 * @param s the s-coordinate of the texture coordinates.
	 * @param t the t-coordinate of the texture coordinates.
	 */
	public static native void glTexCoord2i(int s, int t);

	/**
	 * Set the current texture coordinates with three integer values.
	 *
	 * @param s the s-coordinate of the texture coordinates.
	 * @param t the t-coordinate of the texture coordinates.
	 * @param r the r-coordinate of the texture coordinates.
	 */
	public static native void glTexCoord3i(int s, int t, int r);

	/**
	 * Set the current texture coordinates with two floating-point values.
	 *
	 * @param s the s-coordinate of the texture coordinates.
	 * @param t the t-coordinate of the texture coordinates.
	 */
	public static native void glTexCoord2f(float s, float t);

	/**
	 * Set the current texture coordinates with three floating-point values.
	 *
	 * @param s the s-coordinate of the texture coordinates.
	 * @param t the t-coordinate of the texture coordinates.
	 * @param r the r-coordinate of the texture coordinates.
	 */
	public static native void glTexCoord3f(float s, float t, float r);

	/**
	 * Set the current texture coordinates for a specified texture unit with two
	 * integer values.
	 *
	 * @param unit the texture unit to set the texture coordinates for.
	 * @param s    the s-coordinate of the texture coordinates.
	 * @param t    the t-coordinate of the texture coordinates.
	 */
	public static native void glMultiTexCoord2i(int unit, int s, int t);

	/**
	 * Set the current texture coordinates for a specified texture unit with three
	 * integer values.
	 *
	 * @param unit the texture unit to set the texture coordinates for.
	 * @param s    the s-coordinate of the texture coordinates.
	 * @param t    the t-coordinate of the texture coordinates.
	 * @param r    the r-coordinate of the texture coordinates.
	 */
	public static native void glMultiTexCoord3i(int unit, int s, int t, int r);

	/**
	 * Specify the s and t texture coordinates for a texture unit.
	 *
	 * @param textureUnit the texture unit
	 * @param s           the s texture coordinate
	 * @param t           the t texture coordinate
	 */
	public static native void glMultiTexCoord2f(int textureUnit, float s, float t);

	/**
	 * Set the raster position.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	public static native void glRasterPos2i(int x, int y);

	/**
	 * End the current primitive.
	 */
	public static native void glEnd();

	/**
	 * Enable a client-side capability.
	 *
	 * @param capability the capability to enable
	 */
	public static native void glEnableClientState(int capability);

	/**
	 * Disable a client-side capability.
	 *
	 * @param capability the capability to disable
	 */
	public static native void glDisableClientState(int capability);

	/**
	 * Specify the texture coordinate set to be used.
	 *
	 * @param texture the texture unit
	 */
	public static native void glClientActiveTexture(int texture);

	/**
	 * Define an array of vertex data.
	 *
	 * @param size    the number of coordinates per vertex
	 * @param type    the data type of each coordinate
	 * @param stride  the byte offset between consecutive vertices
	 * @param pointer the vertex data
	 */
	public static native void glVertexPointer(int size, int type, int stride, long pointer);

	/**
	 * Define an array of normal vectors.
	 *
	 * @param type    the data type of each component
	 * @param stride  the byte offset between consecutive normals
	 * @param pointer the normal data
	 */
	public static native void glNormalPointer(int type, int stride, long pointer);

	/**
	 * Define an array of color data.
	 *
	 * @param size    the number of components per color
	 * @param type    the data type of each component
	 * @param stride  the byte offset between consecutive colors
	 * @param pointer the color data
	 */
	public static native void glColorPointer(int size, int type, int stride, long pointer);

	/**
	 * Define an array of texture coordinates.
	 *
	 * @param size    the number of coordinates per texture coordinate set
	 * @param type    the data type of each coordinate
	 * @param stride  the byte offset between consecutive texture coordinates
	 * @param pointer the texture coordinate data
	 */
	public static native void glTexCoordPointer(int size, int type, int stride, long pointer);

	/**
	 * Define an array of generic vertex attribute data.
	 *
	 * @param index      the index of the generic vertex attribute
	 * @param size       the number of components per attribute
	 * @param type       the data type of each component
	 * @param normalized whether the data should be normalized
	 * @param stride     the byte offset between consecutive attributes
	 * @param pointer    the attribute data
	 */
	public static native void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
			long pointer);

	/**
	 * Enable a generic vertex attribute array.
	 *
	 * @param index the index of the attribute
	 */
	public static native void glEnableVertexAttribArray(int index);

	/**
	 * Disable a generic vertex attribute array.
	 *
	 * @param index the index of the attribute
	 */
	public static native void glDisableVertexAttribArray(int index);

	/**
	 * Bind a user-defined attribute variable to a generic vertex attribute index.
	 *
	 * @param program the program object to which the attribute variable belongs
	 * @param index   the index of the generic vertex attribute to which to bind the
	 *                attribute variable
	 * @param name    the name of the attribute variable
	 */
	public static native void glBindAttribLocation(int program, int index, String name);

	/**
	 * Generates buffer object names.
	 * 
	 * @param n       the number of buffer object names to generate
	 * @param buffers an array in which the generated buffer object names are stored
	 * @param offset  the offset in the array where the generated buffer object
	 *                names are stored
	 */
	public static native void glGenBuffersARB(int n, int[] buffers, int offset);

	/**
	 * Binds a buffer object to a target.
	 * 
	 * This function binds the buffer object with the specified ID to the given target.
	 * Once bound, the buffer object becomes the current buffer for the specified target,
	 * and subsequent OpenGL operations related to that target will affect the bound buffer.
	 * 
	 * @param target The target to which the buffer object will be bound. 
	 *               Common targets include GL_ARRAY_BUFFER_ARB and GL_ELEMENT_ARRAY_BUFFER_ARB.
	 * @param buffer The ID of the buffer object to bind.
	 * @throws IllegalArgumentException If the target is not a valid OpenGL buffer target.
	 * @see <a href="https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glBindBuffer.xml">glBindBufferARB documentation</a>
	 */
	public static native void glBindBufferARB(int target, int buffer);

	/**
	 * Creates and initializes a buffer object's data store.
	 * 
	 * @param target the target to which the buffer object is bound
	 * @param size   the size in bytes of the buffer object's new data store
	 * @param data   a pointer to data that will be copied into the data store for
	 *               initialization, or null if no data is to be copied
	 * @param usage  the expected usage pattern of the data store
	 */
	public static native void glBufferDataARBa(int target, int size, long data, int usage);

	/**
	 * Creates and initializes a buffer object's data store.
	 * 
	 * @param target the target to which the buffer object is bound
	 * @param size   the size in bytes of the buffer object's new data store
	 * @param data   a byte array containing the data to copy into the data store
	 *               for initialization
	 * @param offset the offset in the data array where copying begins
	 * @param usage  the expected usage pattern of the data store
	 */
	public static native void glBufferDataARBub(int target, int size, byte[] data, int offset, int usage);

	/**
	 * Updates a subset of a buffer object's data store.
	 * 
	 * @param target the target to which the buffer object is bound
	 * @param offset the offset into the buffer object's data store where data
	 *               replacement will begin
	 * @param size   the size in bytes of the data store region being replaced
	 * @param data   a pointer to the new data that will be copied into the data
	 *               store
	 */
	public static native void glBufferSubDataARBa(int target, int offset, int size, long data);

	/**
	 * Updates a subset of a buffer object's data store.
	 * 
	 * @param target  the target to which the buffer object is bound
	 * @param offset  the offset into the buffer object's data store where data
	 *                replacement will begin
	 * @param size    the size in bytes of the data store region being replaced
	 * @param data    a byte array containing the new data to copy into the data
	 *                store
	 * @param offset2 the offset in the data array where copying begins
	 */
	public static native void glBufferSubDataARBub(int target, int offset, int size, byte[] data, int offset2);

	/**
	 * Returns a pointer to a mapped buffer object's data store.
	 * 
	 * @param target the target to which the buffer object is bound
	 * @param access the requested access policy
	 * @return a pointer to the mapped buffer object's data store
	 */
	public static native long glMapBufferARB(int target, int access);

	/**
	 * Unmaps a buffer object's mapped data store.
	 *
	 * @param bufferObjectID the ID of the buffer object to unmap
	 * @return {@code true} if the data store was unmapped successfully;
	 *         {@code false} otherwise
	 */
	public static native boolean glUnmapBufferARB(int bufferObjectID);

	/**
	 * Deletes buffer objects.
	 *
	 * @param n       The number of buffer objects to delete.
	 * @param buffers An array containing the buffer objects to be deleted.
	 * @param offset  The offset into the array of buffer objects to start deleting
	 *                from.
	 */
	public static native void glDeleteBuffersARB(int n, int[] buffers, int offset);

	/**
	 * Sets the line width for rendering lines.
	 *
	 * @param width The line width to set.
	 */
	public static native void glLineWidth(float width);

	/**
	 * Sets the size of points for rendering point primitives.
	 *
	 * @param size The point size to set.
	 */
	public static native void glPointSize(float size);

	/**
	 * Sets the polygon rasterization mode.
	 *
	 * @param face The faces to apply the rasterization mode to, which must be one
	 *             of GL_FRONT, GL_BACK, or GL_FRONT_AND_BACK.
	 * @param mode The rasterization mode, which must be one of GL_POINT, GL_LINE,
	 *             or GL_FILL.
	 */
	public static native void glPolygonMode(int face, int mode);

	/**
	 * Renders primitives from array data.
	 *
	 * @param mode  The type of primitives to render, which must be one of
	 *              GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES,
	 *              GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN, GL_TRIANGLES, or
	 *              GL_QUAD_STRIP.
	 * @param first The starting index in the enabled arrays.
	 * @param count The number of vertices to render.
	 */
	public static native void glDrawArrays(int mode, int first, int count);

	/**
	 * Draws a sequence of geometric primitives with indices from bound array data.
	 *
	 * @param mode    the type of primitives to draw
	 * @param count   the number of elements to be rendered
	 * @param type    the type of the values in indices
	 * @param indices a long value specifying the offset into the buffer object's
	 *                data store where the indices start
	 */
	public static native void glDrawElements(int mode, int count, int type, long indices);

	/**
	 * Generates texture names.
	 *
	 * @param n        the number of texture names to generate
	 * @param textures an array in which the generated texture names are stored
	 * @param offset   the offset into the array where to store the first generated
	 *                 name
	 */
	public static native void glGenTextures(int n, int[] textures, int offset);

	/**
	 * Deletes named textures.
	 *
	 * @param n        the number of texture names to delete
	 * @param textures an array containing the names of the textures to be deleted
	 * @param offset   the offset into the array where to start deleting textures
	 */
	public static native void glDeleteTextures(int n, int[] textures, int offset);

	/**
	 * Selects which texture unit subsequent texture state calls will affect.
	 *
	 * @param texture the index of the texture unit to be made active
	 */
	public static native void glActiveTexture(int texture);

	/**
	 * Binds a named texture to a texturing target.
	 *
	 * @param target  the target to which the texture is bound
	 * @param texture the name of the texture to bind
	 */
	public static native void glBindTexture(int target, int texture);

	/**
	 * Set the value of a texture environment parameter.
	 *
	 * @param target the target texture environment
	 * @param pname  the symbolic name of a single-valued texture environment
	 *               parameter
	 * @param param  the value that pname is set to
	 */
	public static native void glTexEnvi(int target, int pname, int param);

	/**
	 * Set the value of a texture environment parameter.
	 *
	 * @param target the target texture environment
	 * @param pname  the symbolic name of a texture environment parameter
	 * @param param  the value that pname is set to
	 */
	public static native void glTexEnvf(int target, int pname, float param);

	/**
	 * Set the value of a texture environment parameter.
	 *
	 * @param target the target texture environment
	 * @param pname  the symbolic name of a texture environment parameter
	 * @param params the value or values that pname is set to
	 * @param offset the offset into the array where to start reading values
	 */
	public static native void glTexEnvfv(int target, int pname, float[] params, int offset);

	/**
	 * Set the value of a texture coordinate generation parameter.
	 *
	 * @param coord the texture coordinate for which to set the generation function
	 * @param pname the symbolic name of a texture coordinate generation parameter
	 * @param param the value that pname is set to
	 */
	public static native void glTexGeni(int coord, int pname, int param);

	/**
	 * Set the value of a texture coordinate generation parameter.
	 *
	 * @param coord  the texture coordinate for which to set the generation function
	 * @param pname  the symbolic name of a texture coordinate generation parameter
	 * @param params the value or values that pname is set to
	 * @param offset the offset into the array where to start reading values
	 */
	public static native void glTexGenfv(int coord, int pname, float[] params, int offset);

	/**
	 * Sets the value of a texture parameter for the specified texture target.
	 * 
	 * @param target the texture target
	 * @param pname  the name of the parameter to set
	 * @param param  the new value for the parameter
	 */
	public static native void glTexParameterf(int target, int pname, float param);

	/**
	 * Sets the value of a texture parameter for the specified texture target.
	 * 
	 * @param target the texture target
	 * @param pname  the name of the parameter to set
	 * @param param  the new value for the parameter
	 */
	public static native void glTexParameteri(int target, int pname, int param);

	/**
	 * Reads a block of pixels from the frame buffer into an array of integers.
	 * 
	 * @param target the texture target
	 * @param level  the level-of-detail number
	 * @param format the format of the pixel data
	 * @param type   the data type of the pixel data
	 * @param pixels the buffer to receive the pixel data
	 * @param offset the starting offset of the buffer
	 */
	public static native void glGetTexImagei(int target, int level, int format, int type, int[] pixels, int offset);

	/**
	 * Reads a block of pixels from the frame buffer into an array of bytes.
	 * 
	 * @param target the texture target
	 * @param level  the level-of-detail number
	 * @param format the format of the pixel data
	 * @param type   the data type of the pixel data
	 * @param pixels the buffer to receive the pixel data
	 * @param offset the starting offset of the buffer
	 */
	public static native void glGetTexImageub(int target, int level, int format, int type, byte[] pixels, int offset);

	/**
	 * Returns the value of a texture parameter for the specified texture target and
	 * level-of-detail.
	 * 
	 * @param target the texture target
	 * @param level  the level-of-detail number
	 * @param pname  the name of the parameter to query
	 * @return the value of the parameter
	 */
	public static native float glGetTexLevelParameterfv(int target, int level, int pname);

	/**
	 * Returns the value of a texture parameter for the specified texture target and
	 * level-of-detail.
	 * 
	 * @param target the texture target
	 * @param level  the level-of-detail number
	 * @param pname  the name of the parameter to query
	 * @return the value of the parameter
	 */
	public static native int glGetTexLevelParameteriv(int target, int level, int pname);

	/**
	 * Reads a block of pixels from the frame buffer into an array of bytes.
	 * 
	 * @param target the texture target
	 * @param level  the level-of-detail number
	 * @param format the format of the pixel data
	 * @param type   the data type of the pixel data
	 * @param pixels the buffer to receive the pixel data
	 */
	public static native void glGetTexImage(int target, int level, int format, int type, byte[] pixels);

	/**
	 * Defines a 1D texture image with 8-bit data.
	 * 
	 * @param target         the texture target
	 * @param level          the level-of-detail number
	 * @param internalformat the number of color components in the texture
	 * @param width          the width of the texture image
	 * @param border         the width of the border
	 * @param format         the format of the pixel data
	 * @param type           the data type of the pixel data
	 * @param pixels         the buffer containing the pixel data
	 * @param offset         the starting offset of the buffer
	 */
	public static native void glTexImage1Dub(int target, int level, int internalformat, int width, int border,
			int format, int type, byte[] pixels, int offset);

	/**
	 * Copies the texture image from the current framebuffer to a new texture object
	 * 
	 * @param target         the texture target (e.g. GL_TEXTURE_2D)
	 * @param level          the level-of-detail number
	 * @param internalFormat the internal format of the texture (e.g. GL_RGBA8)
	 * @param x              the x-coordinate of the lower left corner of the
	 *                       rectangular region of pixels to be copied
	 * @param y              the y-coordinate of the lower left corner of the
	 *                       rectangular region of pixels to be copied
	 * @param width          the width of the rectangular region of pixels to be
	 *                       copied
	 * @param height         the height of the rectangular region of pixels to be
	 *                       copied
	 * @param border         the width of the border. Must be 0 or 1
	 */
	public static native void glCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int width,
			int height, int border);

	/**
	 * Copies a rectangular portion of the framebuffer to a specified region in the
	 * texture
	 * 
	 * @param target  the texture target (e.g. GL_TEXTURE_2D)
	 * @param level   the level-of-detail number
	 * @param xoffset the x-coordinate of the lower left corner of the texture image
	 * @param yoffset the y-coordinate of the lower left corner of the texture image
	 * @param x       the x-coordinate of the lower left corner of the rectangular
	 *                region of pixels to be copied
	 * @param y       the y-coordinate of the lower left corner of the rectangular
	 *                region of pixels to be copied
	 * @param width   the width of the rectangular region of pixels to be copied
	 * @param height  the height of the rectangular region of pixels to be copied
	 */
	public static native void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y,
			int width, int height);

	/**
	 * Specifies a two-dimensional texture image in pixel data
	 * 
	 * @param target         the texture target (e.g. GL_TEXTURE_2D)
	 * @param level          the level-of-detail number
	 * @param internalFormat the internal format of the texture (e.g. GL_RGBA8)
	 * @param width          the width of the texture image
	 * @param height         the height of the texture image
	 * @param border         the width of the border. Must be 0 or 1
	 * @param format         the format of the pixel data (e.g. GL_RGBA)
	 * @param type           the data type of the pixel data (e.g. GL_UNSIGNED_BYTE)
	 * @param pixels         the pixel data
	 * @param offset         the offset into the array in bytes
	 */
	public static native void glTexImage2Di(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, int[] pixels, int offset);

	/**
	 * Specifies a two-dimensional texture image in floating point format.
	 * 
	 * @param target         the target texture
	 * @param level          the level-of-detail number
	 * @param internalFormat the number of color components in the texture
	 * @param width          the width of the texture image in pixels
	 * @param height         the height of the texture image in pixels
	 * @param border         the width of the border
	 * @param format         the format of the pixel data
	 * @param type           the data type of the pixel data
	 * @param data           the texture image data
	 * @param offset         the offset in the data array where the texture image
	 *                       data begins
	 */
	public static native void glTexImage2Df(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, float[] data, int offset);

	/**
	 * Specifies a two-dimensional texture image in unsigned byte format.
	 * 
	 * @param target         the target texture
	 * @param level          the level-of-detail number
	 * @param internalFormat the number of color components in the texture
	 * @param width          the width of the texture image in pixels
	 * @param height         the height of the texture image in pixels
	 * @param border         the width of the border
	 * @param format         the format of the pixel data
	 * @param type           the data type of the pixel data
	 * @param data           the texture image data
	 * @param offset         the offset in the data array where the texture image
	 *                       data begins
	 */
	public static native void glTexImage2Dub(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, byte[] data, int offset);
	
	/**
	 * Updates a 2D texture region with pixel data.
	 *
	 * @param target      The target texture.
	 * @param level       The level-of-detail number.
	 * @param offsetX     The X offset within the texture.
	 * @param offsetY     The Y offset within the texture.
	 * @param width       The width of the texture region.
	 * @param height      The height of the texture region.
	 * @param format      The format of the pixel data.
	 * @param type        The data type of the pixel data.
	 * @param pixels      The pixel data to be applied.
	 * @param pixelOffset The offset in the pixel data array.
	 */
	public static native void glTexSubImage2Di(
	    int target,
	    int level,
	    int offsetX,
	    int offsetY,
	    int width,
	    int height,
	    int format,
	    int type,
	    int[] pixels,
	    int pixelOffset
	);

	/**
	 * 
	 * Sets a two-dimensional texture image from an array of floating-point values
	 * 
	 * @param target  the target texture, either GL_TEXTURE_2D or
	 *                GL_PROXY_TEXTURE_2D
	 * @param level   the level-of-detail number
	 * @param xoffset the texel offset in the x direction within the texture array
	 * @param yoffset the texel offset in the y direction within the texture array
	 * @param width   the width of the texture image
	 * @param height  the height of the texture image
	 * @param format  the format of the pixel data, e.g., GL_RGBA
	 * @param type    the data type of the pixel data, e.g., GL_FLOAT
	 * @param data    the texture image data
	 * @param offset  the offset into the array data where the texture data starts
	 */
	public static native void glTexSubImage2Df(int target, int level, int xoffset, int yoffset, int width, int height,
			int format, int type, float[] data, int offset);

	/**
	 * 
	 * Sets a two-dimensional texture image from an array of unsigned byte values
	 * 
	 * @param target  the target texture, either GL_TEXTURE_2D or
	 *                GL_PROXY_TEXTURE_2D
	 * @param level   the level-of-detail number
	 * @param xoffset the texel offset in the x direction within the texture array
	 * @param yoffset the texel offset in the y direction within the texture array
	 * @param width   the width of the texture image
	 * @param height  the height of the texture image
	 * @param format  the format of the pixel data, e.g., GL_RGBA
	 * @param type    the data type of the pixel data, e.g., GL_UNSIGNED_BYTE
	 * @param data    the texture image data
	 * @param offset  the offset into the array data where the texture data starts
	 */
	public static native void glTexSubImage2Dub(int target, int level, int xoffset, int yoffset, int width, int height,
			int format, int type, byte[] data, int offset);

	/**
	 * 
	 * Copies a subregion of a 3D texture image into a 2D texture image
	 * 
	 * @param target  the target texture, either GL_TEXTURE_2D or
	 *                GL_PROXY_TEXTURE_2D
	 * @param level   the level-of-detail number
	 * @param xoffset the texel offset in the x direction within the texture array
	 * @param yoffset the texel offset in the y direction within the texture array
	 * @param zoffset the texel offset in the z direction within the texture array
	 * @param x       the x-coordinate of the lower left corner of the subregion
	 * @param y       the y-coordinate of the lower left corner of the subregion
	 * @param width   the width of the subregion
	 * @param height  the height of the subregion
	 */
	public static native void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x,
			int y, int width, int height);

	/**
	 * Specifies a 3D texture image with 8-bit RGB components.
	 * 
	 * @param target   The target texture.
	 * @param level    The level-of-detail number.
	 * @param internal The internal format of the texture.
	 * @param width    The width of the texture image.
	 * @param height   The height of the texture image.
	 * @param depth    The depth of the texture image.
	 * @param border   The width of the border. Must be 0.
	 * @param format   The format of the pixel data.
	 * @param type     The type of the pixel data.
	 * @param data     A pointer to the image data in memory.
	 * @param offset   The offset into the data array where the image data starts.
	 */
	public static native void glTexImage3Dub(int target, int level, int internal, int width, int height, int depth,
			int border, int format, int type, byte[] data, int offset);

	/**
	 * Specifies a 2D texture image in a compressed format.
	 * 
	 * @param target    The target texture.
	 * @param level     The level-of-detail number.
	 * @param internal  The internal format of the compressed image.
	 * @param width     The width of the texture image.
	 * @param height    The height of the texture image.
	 * @param border    The width of the border. Must be 0.
	 * @param imageSize The size in bytes of the compressed texture image.
	 * @param data      A pointer to the compressed texture data.
	 * @param offset    The offset into the data array where the texture data
	 *                  starts.
	 */
	public static native void glCompressedTexImage2Dub(int target, int level, int internal, int width, int height,
			int border, int imageSize, byte[] data, int offset);

	/**
	 * Copies pixel values from the framebuffer to a buffer.
	 * 
	 * @param x      The x coordinate of the lower left corner of the source
	 *               rectangle.
	 * @param y      The y coordinate of the lower left corner of the source
	 *               rectangle.
	 * @param width  The width of the source rectangle.
	 * @param height The height of the source rectangle.
	 * @param type   Specifies the pixel type for the output buffer.
	 */
	public static native void glCopyPixels(int x, int y, int width, int height, int type);

	/**
	 * Reads a block of pixels from the framebuffer to an integer array.
	 *
	 * @param x      the x coordinate of the lower left corner of the block
	 * @param y      the y coordinate of the lower left corner of the block
	 * @param width  the width of the block
	 * @param height the height of the block
	 * @param format the format of the pixel data (e.g. GL_RGBA)
	 * @param type   the data type of the pixel data (e.g. GL_UNSIGNED_BYTE)
	 * @param pixels the integer array to receive the pixel data
	 * @param offset the starting index within the array
	 */
	public static native void glReadPixelsi(int x, int y, int width, int height, int format, int type, int[] pixels,
			int offset);

	/**
	 * Reads a block of pixels from the framebuffer to a byte array.
	 *
	 * @param x      the x coordinate of the lower left corner of the block
	 * @param y      the y coordinate of the lower left corner of the block
	 * @param width  the width of the block
	 * @param height the height of the block
	 * @param format the format of the pixel data (e.g. GL_RGBA)
	 * @param type   the data type of the pixel data (e.g. GL_UNSIGNED_BYTE)
	 * @param pixels the byte array to receive the pixel data
	 * @param offset the starting index within the array
	 */
	public static native void glReadPixelsub(int x, int y, int width, int height, int format, int type, byte[] pixels,
			int offset);

	/**
	 * Draws a block of pixels from an integer array to the framebuffer.
	 *
	 * @param width  the width of the block
	 * @param height the height of the block
	 * @param format the format of the pixel data (e.g. GL_RGBA)
	 * @param pixels the integer array containing the pixel data
	 * @param offset the starting index within the array
	 */
	public static native void glDrawPixelsi(int width, int height, int format, int[] pixels, int offset);

	/**
	 * This method draws a block of pixels in the current framebuffer color buffer,
	 * using unsigned bytes as pixel data.
	 *
	 * @param x      the x-coordinate of the lower-left corner of the block
	 * @param y      the y-coordinate of the lower-left corner of the block
	 * @param width  the width of the block in pixels
	 * @param height the height of the block in pixels
	 * @param pixels the pixel data in unsigned bytes
	 * @param offset the starting offset in the pixel data array
	 */
	public static native void glDrawPixelsub(int x, int y, int width, int height, byte[] pixels, int offset);

	/**
	 * This method sets the pixel zoom factors, which determine the ratio of the
	 * sizes of pixels in the x and y dimensions.
	 *
	 * @param xfactor the zoom factor for the x-dimension
	 * @param yfactor the zoom factor for the y-dimension
	 */
	public static native void glPixelZoom(float xfactor, float yfactor);

	/**
	 * This method sets a pixel storage parameter to a specified value.
	 *
	 * @param parameter the parameter to set
	 * @param value     the value to set the parameter to
	 */
	public static native void glPixelStorei(int parameter, int value);

	/**
	 * This method sets a pixel transfer parameter to a floating-point value.
	 *
	 * @param parameter the parameter to set
	 * @param value     the floating-point value to set the parameter to
	 */
	public static native void glPixelTransferf(int parameter, float value);

	/**
	 * This method enables or disables color material tracking.
	 *
	 * @param face the face to enable or disable material tracking for
	 * @param mode the material tracking mode to set
	 */
	public static native void glColorMaterial(int face, int mode);

	/**
	 * Sets the specified parameter of the specified light source to the given
	 * value.
	 * 
	 * @param light the light source for which to set the parameter
	 * @param pname the name of the parameter to set
	 * @param param the value to set the parameter to
	 */
	public static native void glLightf(int light, int pname, float param);

	/**
	 * Sets the specified parameter of the specified light source to the given
	 * values.
	 * 
	 * @param light  the light source for which to set the parameter
	 * @param pname  the name of the parameter to set
	 * @param params the values to set the parameter to
	 * @param offset the offset into the array of parameter values
	 */
	public static native void glLightfv(int light, int pname, float[] params, int offset);

	/**
	 * Sets the specified parameter of the lighting model to the given values.
	 * 
	 * @param pname  the name of the parameter to set
	 * @param params the values to set the parameter to
	 * @param offset the offset into the array of parameter values
	 */
	public static native void glLightModelfv(int pname, float[] params, int offset);

	/**
	 * Sets the specified material property of the specified face to the given
	 * value(s).
	 * 
	 * @param face   the face for which to set the material property
	 * @param pname  the name of the material property to set
	 * @param params the value(s) to set the material property to
	 * @param offset the offset into the array of parameter values
	 */
	public static native void glMaterialfv(int face, int pname, float[] params, int offset);

	/**
	 * Sets the specified fog parameter to the given integer value.
	 * 
	 * @param pname the name of the fog parameter to set
	 * @param param the integer value to set the fog parameter to
	 */
	public static native void glFogi(int pname, int param);

	/**
	 * Sets the specified fog parameter to the given float value.
	 * 
	 * @param pname the name of the fog parameter to set
	 * @param param the float value to set the fog parameter to
	 */
	public static native void glFogf(int pname, float param);

	/**
	 * Sets the specified fog parameter to the given float values.
	 * 
	 * @param pname  the name of the fog parameter to set
	 * @param params the float values to set the fog parameter to
	 * @param offset the offset into the array of parameter values
	 */
	public static native void glFogfv(int pname, float[] params, int offset);

	/**
	 * Specifies the pixel arithmetic for blending pixel values.
	 * 
	 * @param sfactor the source blending factor
	 * @param dfactor the destination blending factor
	 */
	public static native void glBlendFunc(int sfactor, int dfactor);

	/**
	 * Specifies the pixel arithmetic separately for RGB and alpha components.
	 * 
	 * @param srcRGB   the source blending factor for RGB components
	 * @param dstRGB   the destination blending factor for RGB components
	 * @param srcAlpha the source blending factor for alpha component
	 * @param dstAlpha the destination blending factor for alpha component
	 */
	public static native void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

	/**
	 * 
	 * Sets the constant blend color used by subsequent calls to
	 * {@link #glBlendFuncSeparate(int, int, int, int)}.
	 * 
	 * @param red   the red component of the blend color
	 * @param green the green component of the blend color
	 * @param blue  the blue component of the blend color
	 * @param alpha the alpha component of the blend color
	 */
	public static native void glBlendColor(float red, float green, float blue, float alpha);

	/**
	 * 
	 * Generates framebuffer object names.
	 * 
	 * @param n            the number of framebuffer object names to generate
	 * @param framebuffers an array of integers in which to store the generated
	 *                     framebuffer object names
	 * @param offset       the offset into the array at which to store the first
	 *                     framebuffer object name
	 */
	public static native void glGenFramebuffersEXT(int n, int[] framebuffers, int offset);

	/**
	 * 
	 * Deletes framebuffer objects.
	 * 
	 * @param n            the number of framebuffer object names to delete
	 * @param framebuffers an array of integers containing the framebuffer object
	 *                     names to delete
	 * @param offset       the offset into the array at which the first framebuffer
	 *                     object name is located
	 */
	public static native void glDeleteFramebuffersEXT(int n, int[] framebuffers, int offset);

	/**
	 * 
	 * Binds a framebuffer object to the specified target.
	 * 
	 * @param target      the framebuffer target to which the framebuffer object
	 *                    should be bound
	 * @param framebuffer the framebuffer object name
	 */
	public static native void glBindFramebufferEXT(int target, int framebuffer);

	/**
	 * 
	 * Attaches a texture object to a framebuffer object.
	 * 
	 * @param target     the framebuffer target to which the texture should be
	 *                   attached
	 * @param attachment the attachment point of the texture
	 * @param textarget  the texture target to which the texture should be bound
	 * @param texture    the texture object name
	 * @param level      the level-of-detail number
	 */
	public static native void glFramebufferTexture2DEXT(int target, int attachment, int textarget, int texture,
			int level);

	/**
	 * 
	 * Attaches a texture object to a framebuffer object.
	 * 
	 * @param target     the framebuffer target to which the texture should be
	 *                   attached
	 * @param attachment the attachment point of the texture
	 * @param textarget  the texture target to which the texture should be bound
	 * @param texture    the texture object name
	 * @param level      the level-of-detail number
	 * @param zoffset    the offset in the z-direction of the texture slice to be
	 *                   attached
	 */
	public static native void glFramebufferTexture3DEXT(int target, int attachment, int textarget, int texture,
			int level, int zoffset);

	/**
	 * 
	 * Attaches a renderbuffer object to a framebuffer object.
	 * 
	 * @param target             the framebuffer target to which the renderbuffer
	 *                           should be attached
	 * @param attachment         the attachment point of the renderbuffer
	 * @param renderbuffertarget the renderbuffer target to which the renderbuffer
	 *                           should be bound
	 * @param renderbuffer       the renderbuffer object name
	 */
	public static native void glFramebufferRenderbufferEXT(int target, int attachment, int renderbuffertarget,
			int renderbuffer);

	/**
	 * 
	 * Returns the status of the framebuffer target.
	 * 
	 * @param target the target framebuffer to check
	 * @return the status of the target framebuffer
	 * @see <a href=
	 *      "https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glCheckFramebufferStatus.xml">glCheckFramebufferStatus</a>
	 */
	public static native int glCheckFramebufferStatusEXT(int target);

	/**
	 * 
	 * Copies a block of pixels from the source framebuffer to the destination
	 * framebuffer with the specified mask and filter.
	 * 
	 * @param srcX0  the left X coordinate of the source rectangle
	 * @param srcY0  the bottom Y coordinate of the source rectangle
	 * @param srcX1  the right X coordinate of the source rectangle
	 * @param srcY1  the top Y coordinate of the source rectangle
	 * @param dstX0  the left X coordinate of the destination rectangle
	 * @param dstY0  the bottom Y coordinate of the destination rectangle
	 * @param dstX1  the right X coordinate of the destination rectangle
	 * @param dstY1  the top Y coordinate of the destination rectangle
	 * @param mask   the bitwise OR of the flags indicating which buffers are to be
	 *               copied
	 * @param filter the filter to use when rescaling the image
	 * @see <a href=
	 *      "https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glBlitFramebuffer.xml">glBlitFramebuffer</a>
	 */
	public static native void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0,
			int dstX1, int dstY1, int mask, int filter);

	/**
	 * 
	 * Generates mipmaps for the specified texture target.
	 * 
	 * @param target the texture target to generate mipmaps for
	 * @see <a href=
	 *      "https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glGenerateMipmap.xml">glGenerateMipmap</a>
	 */
	public static native void glGenerateMipmapEXT(int target);

	/**
	 * 
	 * Generates renderbuffer object names.
	 * 
	 * @param n             the number of names to generate
	 * @param renderbuffers the array to store the generated names in
	 * @param offset        the starting offset in the array
	 * @see <a href=
	 *      "https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glGenRenderbuffers.xml">glGenRenderbuffers</a>
	 */
	public static native void glGenRenderbuffersEXT(int n, int[] renderbuffers, int offset);

	/**
	 * 
	 * Deletes specified renderbuffer objects.
	 * 
	 * @param n             the number of renderbuffer objects to delete
	 * @param renderbuffers the array containing the names of the renderbuffer
	 *                      objects to delete
	 * @param offset        the starting offset in the array
	 * @see <a href=
	 *      "https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glDeleteRenderbuffers.xml">glDeleteRenderbuffers</a>
	 */
	public static native void glDeleteRenderbuffersEXT(int n, int[] renderbuffers, int offset);

	/**
	 * 
	 * Binds a named renderbuffer object.
	 * 
	 * @param target       the target of the renderbuffer object
	 * @param renderbuffer the name of the renderbuffer object
	 * @see <a href=
	 *      "https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glBindRenderbuffer.xml">glBindRenderbuffer</a>
	 */
	public static native void glBindRenderbufferEXT(int target, int renderbuffer);

	/**
	 * 
	 * Sets storage for a renderbuffer object's data store.
	 * 
	 * @param target         the target to which the renderbuffer is bound (must be
	 *                       GL_RENDERBUFFER_EXT)
	 * @param internalFormat the internal format to use for the renderbuffer's data
	 *                       store
	 * @param width          the width of the renderbuffer's data store
	 * @param height         the height of the renderbuffer's data store
	 */
	public static native void glRenderbufferStorageEXT(int target, int internalFormat, int width, int height);

	/**
	 * 
	 * Sets storage for a multisample renderbuffer object's data store.
	 * 
	 * @param target         the target to which the renderbuffer is bound (must be
	 *                       GL_RENDERBUFFER_EXT)
	 * @param samples        the number of samples per pixel
	 * @param internalFormat the internal format to use for the renderbuffer's data
	 *                       store
	 * @param width          the width of the renderbuffer's data store
	 * @param height         the height of the renderbuffer's data store
	 */
	public static native void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalFormat,
			int width, int height);

	/**
	 * 
	 * Generates a program object ID.
	 * 
	 * @return a new program object ID
	 */
	public static native int glGenProgramARB();

	/**
	 * 
	 * Deletes a program object.
	 * 
	 * @param program the program object to delete
	 */
	public static native void glDeleteProgramARB(int program);

	/**
	 * 
	 * Binds a program object.
	 * 
	 * @param target  the target to which the program object will be bound (must be
	 *                GL_PROGRAM_ARB)
	 * @param program the program object to bind
	 */
	public static native void glBindProgramARB(int target, int program);

	/**
	 * 
	 * Loads a program object from a string.
	 * 
	 * @param target the target to which the program object will be bound (must be
	 *               GL_PROGRAM_ARB)
	 * @param format the format of the string (must be GL_PROGRAM_FORMAT_ASCII_ARB)
	 * @param string the program string to load
	 */
	public static native void glProgramStringARB(int target, int format, String string);

	/**
	 * 
	 * Loads a program object from a byte array.
	 * 
	 * @param target the target to which the program object will be bound (must be
	 *               GL_PROGRAM_ARB)
	 * @param format the format of the string (must be GL_PROGRAM_FORMAT_RAW_ARB)
	 * @param string the program string to load, as a byte array
	 */
	public static native void glProgramRawARB(int target, int format, byte[] string);

	/**
	 * 
	 * Sets a program object's local parameter.
	 * 
	 * @param target the target to which the program object is bound (must be
	 *               GL_VERTEX_PROGRAM_ARB or GL_FRAGMENT_PROGRAM_ARB)
	 * @param index  the index of the parameter to set
	 * @param x      the value of the first component of the parameter
	 * @param y      the value of the second component of the parameter
	 * @param z      the value of the third component of the parameter
	 * @param w      the value of the fourth component of the parameter
	 */
	public static native void glProgramLocalParameter4fARB(int target, int index, float x, float y, float z, float w);

	/**
	 * 
	 * Sets a program object's local parameter.
	 * 
	 * @param target the target to which the program object is bound (must be
	 *               GL_VERTEX_PROGRAM_ARB or GL_FRAGMENT_PROGRAM_ARB)
	 * @param index  the index of the parameter to set
	 * @param params an array containing the parameter values
	 * @param offset the offset into the array of the first parameter value to set
	 */
	public static native void glProgramLocalParameter4fvARB(int target, int index, float[] params, int offset);

	/**
	 * 
	 * Retrieves a parameter from an OpenGL program object.
	 * 
	 * @param programHandle the handle of the program object to retrieve the
	 *                      parameter from
	 * @param pname         the symbolic name of the parameter to retrieve
	 * @param params        the array to store the retrieved parameter in
	 * @param offset        the offset in the array to start storing the retrieved
	 *                      parameter in
	 */
	public static native void glGetProgramivARB(int programHandle, int pname, int[] params, int offset);

	/**
	 * 
	 * Creates an empty OpenGL program object and returns its handle.
	 * 
	 * @return the handle of the created program object
	 */
	public static native int glCreateProgram();

	/**
	 * 
	 * Creates an empty shader object of the specified type and returns its handle.
	 * 
	 * @param shaderType the type of shader object to create
	 * @return the handle of the created shader object
	 */
	public static native int glCreateShader(int shaderType);

	/**
	 * 
	 * Deletes an OpenGL program object.
	 * 
	 * @param programHandle the handle of the program object to delete
	 */
	public static native void glDeleteProgram(int programHandle);

	/**
	 * 
	 * Deletes an OpenGL shader object.
	 * 
	 * @param shaderHandle the handle of the shader object to delete
	 */
	public static native void glDeleteShader(int shaderHandle);

	/**
	 * 
	 * Sets the source code in an OpenGL shader object.
	 * 
	 * @param shaderHandle the handle of the shader object to set the source code in
	 * @param shaderSource the source code to set in the shader object
	 */
	public static native void glShaderSource(int shaderHandle, String shaderSource);

	/**
	 * 
	 * Compiles an OpenGL shader object.
	 * 
	 * @param shaderHandle the handle of the shader object to compile
	 */
	public static native void glCompileShader(int shaderHandle);

	/**
	 * 
	 * Attaches an OpenGL shader object to an OpenGL program object.
	 * 
	 * @param programHandle the handle of the program object to attach the shader
	 *                      object to
	 * @param shaderHandle  the handle of the shader object to attach to the program
	 *                      object
	 */
	public static native void glAttachShader(int programHandle, int shaderHandle);

	/**
	 * 
	 * Detaches a shader from a program.
	 * 
	 * @param programHandle the handle of the program from which the shader will be
	 *                      detached
	 * @param shaderHandle  the handle of the shader to be detached
	 */
	public static native void glDetachShader(int programHandle, int shaderHandle);

	/**
	 * 
	 * Links a program object.
	 * 
	 * @param programHandle the handle of the program object to be linked
	 */
	public static native void glLinkProgram(int programHandle);

	/**
	 * 
	 * Installs a program object as part of the current rendering state.
	 * 
	 * @param programHandle the handle of the program object to be used
	 */
	public static native void glUseProgram(int programHandle);

	/**
	 * 
	 * Returns the location of a uniform variable within a program object.
	 * 
	 * @param programHandle the handle of the program object that contains the
	 *                      uniform variable
	 * @param uniformName   the name of the uniform variable whose location is to be
	 *                      queried
	 * @return the location of the uniform variable, or -1 if the uniform variable
	 *         is not found
	 */
	public static native int glGetUniformLocation(int programHandle, String uniformName);

	/**
	 * 
	 * Sets the value of a uniform variable of type int or sampler to a value.
	 * 
	 * @param location the location of the uniform variable to be modified
	 * @param value    the value to set the uniform variable to
	 */
	public static native void glUniform1i(int location, int value);

	/**
	 * 
	 * Sets the value of a uniform variable of type float to a value.
	 * 
	 * @param location the location of the uniform variable to be modified
	 * @param value    the value to set the uniform variable to
	 */
	public static native void glUniform1f(int location, float value);

	/**
	 * 
	 * Sets the value of a uniform variable of type vec2 to two values.
	 * 
	 * @param location the location of the uniform variable to be modified
	 * @param value1   the first value to set the uniform variable to
	 * @param value2   the second value to set the uniform variable to
	 */
	public static native void glUniform2f(int location, float value1, float value2);

	/**
	 * 
	 * Sets the value of a uniform variable of type vec3 to three values.
	 * 
	 * @param location the location of the uniform variable to be modified
	 * @param value1   the first value to set the uniform variable to
	 * @param value2   the second value to set the uniform variable to
	 * @param value3   the third value to set the uniform variable to
	 */
	public static native void glUniform3f(int location, float value1, float value2, float value3);

	/**
	 * 
	 * Sets the value of a uniform variable of type vec4 to four values.
	 * 
	 * @param location the location of the uniform variable to be modified
	 * @param value1   the first value to set the uniform variable to
	 * @param value2   the second value to set the uniform variable to
	 * @param value3   the third value to set the uniform variable to
	 * @param value4   the fourth value to set the uniform variable to
	 */
	public static native void glUniform4f(int location, float value1, float value2, float value3, float value4);

	/**
	 * 
	 * Sets the value of a uniform variable of type vec2 to an array of values.
	 * 
	 * @param location the location of the uniform variable to be modified
	 * @param count    the number of elements in the array
	 * @param values   the array of values to set the uniform variable to
	 * @param offset   the offset into the array where the first value to be used is
	 *                 located
	 */
	public static native void glUniform2fv(int location, int count, float[] values, int offset);

	/**
	 * 
	 * Sets the value of a uniform variable in the currently active program object.
	 * This method sets a 3-component floating point vector array uniform variable.
	 * 
	 * @param location the location of the uniform variable to set
	 * @param count    the number of 3-component floating point vectors in the array
	 * @param values   an array containing the new values for the uniform variable
	 * @param offset   the starting offset in the values array
	 */
	public static native void glUniform3fv(int location, int count, float[] values, int offset);

	/**
	 * 
	 * Sets the value of a uniform variable in the currently active program object.
	 * This method sets a 4-component floating point vector array uniform variable.
	 * 
	 * @param location the location of the uniform variable to set
	 * @param count    the number of 4-component floating point vectors in the array
	 * @param values   an array containing the new values for the uniform variable
	 * @param offset   the starting offset in the values array
	 */
	public static native void glUniform4fv(int location, int count, float[] values, int offset);

	/**
	 * 
	 * Sets the value of a uniform variable in the currently active program object.
	 * This method sets a 2x2 floating point matrix array uniform variable.
	 * 
	 * @param location  the location of the uniform variable to set
	 * @param count     the number of 2x2 floating point matrices in the array
	 * @param transpose whether to transpose the matrices
	 * @param values    an array containing the new values for the uniform variable
	 * @param offset    the starting offset in the values array
	 */
	public static native void glUniformMatrix2fv(int location, int count, boolean transpose, float[] values,
			int offset);

	/**
	 * 
	 * Sets the value of a uniform variable in the currently active program object.
	 * This method sets a 3x3 floating point matrix array uniform variable.
	 * 
	 * @param location  the location of the uniform variable to set
	 * @param count     the number of 3x3 floating point matrices in the array
	 * @param transpose whether to transpose the matrices
	 * @param values    an array containing the new values for the uniform variable
	 * @param offset    the starting offset in the values array
	 */
	public static native void glUniformMatrix3fv(int location, int count, boolean transpose, float[] values,
			int offset);

	/**
	 * 
	 * Sets the value of a uniform variable in the currently active program object.
	 * This method sets a 4x4 floating point matrix array uniform variable.
	 * 
	 * @param location  the location of the uniform variable to set
	 * @param count     the number of 4x4 floating point matrices in the array
	 * @param transpose whether to transpose the matrices
	 * @param values    an array containing the new values for the uniform variable
	 * @param offset    the starting offset in the values array
	 */
	public static native void glUniformMatrix4fv(int location, int count, boolean transpose, float[] values,
			int offset);

	/**
	 * 
	 * Retrieves information about a program object. This method retrieves a
	 * specified parameter of the specified program object.
	 * 
	 * @param programHandle the name of the program object to query
	 * @param pname         the parameter to retrieve
	 * @param params        an array in which to place the retrieved parameter value
	 * @param offset        the offset into the params array where the retrieved
	 *                      parameter value should be placed
	 */
	public static native void glGetProgramiv(int programHandle, int pname, int[] params, int offset);

	/**
	 * 
	 * Gets a parameter of a shader object.
	 * 
	 * @param shaderHandle The handle of the shader object to query.
	 * @param pname        The name of the parameter to query.
	 * @param params       An array to store the value of the parameter.
	 * @param offset       The offset in the array where to store the value.
	 */
	public static native void glGetShaderiv(int shaderHandle, int pname, int[] params, int offset);

	/**
	 * 
	 * Retrieves the information log for a program object.
	 * 
	 * @param programHandle The handle of the program object.
	 * @param maxLength     The maximum number of characters to be written to
	 *                      infoLog.
	 * @param length        An array to store the number of characters written to
	 *                      infoLog.
	 * @param lengthOffset  The offset in the array where to store the length.
	 * @param infoLog       An array to store the log.
	 * @param infoLogOffset The offset in the array where to store the log.
	 */
	public static native void glGetProgramInfoLog(int programHandle, int maxLength, int[] length, int lengthOffset,
			byte[] infoLog, int infoLogOffset);

	/**
	 * 
	 * Retrieves the information log for a shader object.
	 * 
	 * @param shaderHandle  The handle of the shader object.
	 * @param maxLength     The maximum number of characters to be written to
	 *                      infoLog.
	 * @param length        An array to store the number of characters written to
	 *                      infoLog.
	 * @param lengthOffset  The offset in the array where to store the length.
	 * @param infoLog       An array to store the log.
	 * @param infoLogOffset The offset in the array where to store the log.
	 */
	public static native void glGetShaderInfoLog(int shaderHandle, int maxLength, int[] length, int lengthOffset,
			byte[] infoLog, int infoLogOffset);

	/**
	 * 
	 * Returns the error flag.
	 * 
	 * @return The error flag.
	 */
	public static native int glGetError();

	/**
	 * 
	 * Returns a string describing the current GL connection.
	 * 
	 * @param name The name of the parameter to query.
	 * @return A string describing the current GL connection.
	 */
	public static native String glGetString(int name);

	/**
	 * 
	 * Retrieves the value or values of a float parameter.
	 * 
	 * @param pname  The name of the parameter to query.
	 * @param params An array to store the value or values of the parameter.
	 * @param offset The offset in the array where to store the value or values.
	 */
	public static native void glGetFloatv(int pname, float[] params, int offset);

	/**
	 * 
	 * Retrieves the value or values of an integer parameter.
	 * 
	 * @param pname  The name of the parameter to query.
	 * @param params An array to store the value or values of the parameter.
	 * @param offset The offset in the array where to store the value or values.
	 */
	public static native void glGetIntegerv(int pname, int[] params, int offset);

	/**
	 * 
	 * Sets the function used to compare the stencil buffer value with the reference
	 * value.
	 * 
	 * @param func The comparison function.
	 * @param ref  The reference value.
	 * @param mask The stencil mask.
	 */
	public static native void glStencilFunc(int func, int ref, int mask);

	/**
	 * 
	 * Sets what action to take when the stencil test fails.
	 * 
	 * @param fail  The action to take when the stencil test fails.
	 * @param zfail The action to take when the stencil test passes, but the depth
	 *              test fails.
	 * @param zpass The action to take when both the stencil test and the depth test
	 *              pass.
	 */
	public static native void glStencilOp(int fail, int zfail, int zpass);

	/**
	 * Initializes the OpenGL context for the specified canvas with the requested
	 * pixel format attributes.
	 * 
	 * @param canvas      The canvas to initialize the OpenGL context for.
	 * @param redSize     The number of bits for the red color component.
	 * @param greenSize   The number of bits for the green color component.
	 * @param blueSize    The number of bits for the blue color component.
	 * @param alphaSize   The number of bits for the alpha color component.
	 * @param depthSize   The number of bits for the depth buffer.
	 * @param stencilSize The number of bits for the stencil buffer.
	 * @return A handle to the initialized OpenGL context.
	 */
	public native long init(Canvas canvas, int redSize, int greenSize, int blueSize, int alphaSize, int depthSize,
			int stencilSize);

	/**
	 * Checks if pbuffers are available.
	 * 
	 * @return true if pbuffers are available, false otherwise.
	 */
	public native boolean arePbuffersAvailable();

	/**
	 * Attaches the current thread to the OpenGL context.
	 * 
	 * @return true if the current thread was successfully attached, false
	 *         otherwise.
	 */
	public native boolean attachPeer();

	/**
	 * Detaches the current thread from the OpenGL context.
	 */
	public native void detachPeer();

	/**
	 * Prepares the surface for rendering.
	 *
	 * @param canvas the canvas to render to
	 * @return a handle to the prepared surface
	 */
	public native long prepareSurface(Canvas canvas);

	/**
	 * Notifies that the surface has been resized.
	 *
	 * @param surfaceHandle the handle to the surface
	 */
	public native void surfaceResized(long surfaceHandle);

	/**
	 * Releases the surface.
	 *
	 * @param canvas        the canvas associated with the surface
	 * @param surfaceHandle the handle to the surface to release
	 */
	public native void releaseSurface(Canvas canvas, long surfaceHandle);

	/**
	 * Sets the active surface.
	 *
	 * @param surfaceHandle the handle to the surface to activate
	 * @return {@code true} if the surface was successfully set, {@code false}
	 *         otherwise
	 */
	public native boolean setSurface(long surfaceHandle);

	/**
	 * Creates a P-buffer with the specified dimensions.
	 *
	 * @param width  the width of the P-buffer
	 * @param height the height of the P-buffer
	 * @return a handle to the created P-buffer
	 */
	public native long createPbuffer(int width, int height);

	/**
	 * Releases the specified P-buffer.
	 *
	 * @param pbufferHandle the handle to the P-buffer to release
	 */
	public native void releasePbuffer(long pbufferHandle);

	/**
	 * Sets the specified P-buffer as the active surface.
	 *
	 * @param pbufferHandle the handle to the P-buffer to set
	 */
	public native void setPbuffer(long pbufferHandle);

	/**
	 * Swaps the buffers of the specified surface.
	 *
	 * @param surfaceHandle the handle to the surface
	 */
	public native void swapBuffers(long surfaceHandle);

	/**
	 * Sets the swap interval.
	 *
	 * @param interval the desired swap interval
	 */
	public native void setSwapInterval(int interval);

	/**
	 * Release any resources held by this object.
	 */
	public native void release();

}
