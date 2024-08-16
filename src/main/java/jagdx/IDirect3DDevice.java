/* IDirect3DDevice - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

public final class IDirect3DDevice extends IUnknown {
	public IDirect3DDevice() throws Throwable {
		throw new Error();
	}

	/**
	 * Destroys the specified Direct3D device.
	 *
	 * @param device a pointer to the Direct3D device to destroy.
	 */
	public static native void Destroy(long device);

	/**
	 * Retrieves a pointer to the back buffer of the specified swap chain.
	 *
	 * @param swapChain       a pointer to the Direct3D swap chain to retrieve the
	 *                        back buffer from.
	 * @param backBufferIndex the index of the back buffer to retrieve.
	 * @return a pointer to the back buffer of the specified swap chain.
	 */
	public static native long GetRenderTarget(long swapChain, int backBufferIndex);

	/**
	 * Copies the contents of the source surface to the destination surface.
	 *
	 * @param sourceSurface      a pointer to the source surface.
	 * @param sourceRect         a RECT structure that defines the portion of the
	 *                           source surface to copy.
	 * @param destinationSurface a pointer to the destination surface.
	 * @return D3D_OK if the operation succeeds, or an error code otherwise.
	 */
	public static native void GetRenderTargetData(long sourceSurface, long sourceRect, long destinationSurface);

	/**
	 * Sets the render target for the Direct3D device.
	 *
	 * @param device            a pointer to the Direct3D device.
	 * @param renderTargetIndex the index of the render target to set.
	 * @param renderTarget      a pointer to the surface to set as the render
	 *                          target.
	 * @return D3D_OK if the operation succeeds, or an error code otherwise.
	 */
	public static native int SetRenderTarget(long device, int renderTargetIndex, long renderTarget);

	/**
	 * Creates a new render target surface.
	 *
	 * @param device             a pointer to the Direct3D device.
	 * @param width              the width of the render target surface.
	 * @param height             the height of the render target surface.
	 * @param format             the pixel format of the render target surface.
	 * @param multiSampleType    the multisample type of the render target surface.
	 * @param multiSampleQuality the multisample quality of the render target
	 *                           surface.
	 * @param lockable           true if the surface should be lockable, false
	 *                           otherwise.
	 * @return a pointer to the newly created render target surface, or null if the
	 *         operation fails.
	 */
	public static native long CreateRenderTarget(long device, int width, int height, int format, int multiSampleType,
			int multiSampleQuality, boolean lockable);

	/**
	 * Retrieves a pointer to the specified swap chain.
	 *
	 * @param device         a pointer to the Direct3D device.
	 * @param swapChainIndex the index of the swap chain to retrieve.
	 * @return a pointer to the specified swap chain, or null if the operation
	 *         fails.
	 */
	public static native long GetSwapChain(long device, int swapChainIndex);

	/**
	 * Creates an additional swap chain for the Direct3D device.
	 *
	 * @param device        a pointer to the Direct3D device.
	 * @param presentParams a D3DPRESENT_PARAMETERS structure that describes the
	 *                      swap chain to create.
	 * @return a pointer to the newly created swap chain, or null if the operation
	 *         fails.
	 */
	public static native long CreateAdditionalSwapChain(long device, D3DPRESENT_PARAMETERS presentParams);

	/**
	 * Retrieves the depth-stencil surface associated with the current render
	 * target.
	 *
	 * @param deviceContext Pointer to the device context object.
	 * @return A pointer to the depth-stencil surface object.
	 */
	public static native long GetDepthStencilSurface(long deviceContext);

	/**
	 * Sets the depth-stencil surface that should be used for rendering.
	 *
	 * @param deviceContext Pointer to the device context object.
	 * @param surface       Pointer to the depth-stencil surface object.
	 * @return HRESULT indicating success or failure.
	 */
	public static native int SetDepthStencilSurface(long deviceContext, long surface);

	/**
	 * Creates a depth-stencil surface.
	 *
	 * @param device             Pointer to the device object.
	 * @param width              The width of the surface in pixels.
	 * @param height             The height of the surface in pixels.
	 * @param format             The pixel format of the surface.
	 * @param multiSampleType    The type of multisampling to use.
	 * @param multisampleQuality The quality level of the multisampling.
	 * @param discard            Whether the contents of the surface can be
	 *                           discarded when it is no longer needed.
	 * @return A pointer to the created depth-stencil surface object.
	 */
	public static native long CreateDepthStencilSurface(long device, int width, int height, int format,
			int multiSampleType, int multisampleQuality, boolean discard);

	/**
	 * Creates an offscreen plain surface.
	 *
	 * @param device Pointer to the device object.
	 * @param width  The width of the surface in pixels.
	 * @param height The height of the surface in pixels.
	 * @param format The pixel format of the surface.
	 * @param pool   The memory pool from which to allocate memory for the surface.
	 * @return A pointer to the created offscreen plain surface object.
	 */
	public static native long CreateOffscreenPlainSurface(long device, int width, int height, int format, int pool);

	/**
	 * Retrieves a back buffer from the swap chain.
	 *
	 * @param swapChain      Pointer to the swap chain object.
	 * @param i              The index of the back buffer to retrieve.
	 * @param backBufferType The type of surface to return.
	 * @param backBuffer     Pointer to a pointer to the back buffer surface.
	 * @return HRESULT indicating success or failure.
	 */
	public static native long GetBackBuffer(long swapChain, int i, int backBufferType, int[] backBuffer);

	/**
	 * Copies a rectangular portion of one surface to another.
	 * 
	 * @param destinationSurface   The surface to copy to.
	 * @param sourceSurface        The surface to copy from.
	 * @param destinationRectangle The destination rectangle to copy to.
	 * @param sourceRectangle      The source rectangle to copy from.
	 * 
	 * @return HRESULT indicating success or failure of the operation.
	 */
	public static native int StretchRect(long destinationSurface, long sourceSurface, int destinationX,
			int destinationY, int destinationWidth, int destinationHeight, long sourceRectangle, int sourceX,
			int sourceY, int sourceWidth, int sourceHeight, int filter);

	/**
	 * Creates a vertex buffer.
	 *
	 * @param device Pointer to the device object.
	 * @param length The length of the buffer in bytes.
	 * @param usage  The usage of the buffer.
	 * @param fvf    The fixed function vertex format of the buffer.
	 * @param pool   The memory pool from which to allocate memory for the buffer.
	 * @return A pointer to the created vertex buffer object.
	 */
	public static native long CreateVertexBuffer(long device, int length, int usage, int fvf, int pool);

	/**
	 * Creates a new index buffer.
	 *
	 * @param device A pointer to the IDirect3DDevice9 interface.
	 * @param length The length of the index buffer, in bytes.
	 * @param usage  The intended usage of the index buffer.
	 * @param format The format of the index data.
	 * @param pool   The memory pool to be used for the index buffer.
	 *
	 * @return A pointer to the newly created index buffer interface.
	 */
	public static native long CreateIndexBuffer(long device, int length, int usage, int format, int pool);

	/**
	 * 
	 * Creates a new vertex declaration object.
	 * 
	 * @param devicePointer         Pointer to the IDirect3DDevice9 interface
	 * @param vertexElementsPointer Pointer to an array of D3DVERTEXELEMENT9
	 *                              structures
	 * @return Pointer to the newly created vertex declaration object
	 */
	public static native long CreateVertexDeclaration(long devicePointer, long vertexElementsPointer);

	/**
	 * Sets the source data for a vertex buffer stream.
	 *
	 * @param device       A pointer to the IDirect3DDevice9 interface.
	 * @param streamNumber The index of the vertex buffer stream.
	 * @param buffer       A pointer to the vertex buffer.
	 * @param offset       The offset from the beginning of the buffer to the first
	 *                     element to be used.
	 * @param stride       The size of each vertex element, in bytes.
	 *
	 * @return A HRESULT value indicating whether the operation succeeded or failed.
	 */
	public static native int SetStreamSource(long device, int streamNumber, long buffer, int offset, int stride);

	/**
	 * Sets the vertex declaration.
	 *
	 * @param device      A pointer to the IDirect3DDevice9 interface.
	 * @param declaration A pointer to the vertex declaration interface.
	 *
	 * @return A HRESULT value indicating whether the operation succeeded or failed.
	 */
	public static native int SetVertexDeclaration(long device, long declaration);

	/**
	 * Sets the index data for a device.
	 *
	 * @param device A pointer to the IDirect3DDevice9 interface.
	 * @param buffer A pointer to the index buffer.
	 *
	 * @return A HRESULT value indicating whether the operation succeeded or failed.
	 */
	public static native int SetIndices(long device, long buffer);

	/**
	 * Renders a sequence of geometric primitives of the specified type using the
	 * specified index data.
	 *
	 * @param device          A pointer to the IDirect3DDevice9 interface.
	 * @param primitiveType   The type of primitive to render.
	 * @param baseVertexIndex The value added to each index before accessing a
	 *                        vertex from the vertex buffer.
	 * @param minVertexIndex  The minimum index value for vertices used during this
	 *                        call.
	 * @param numVertices     The number of vertices used during this call.
	 * @param startIndex      The index of the first vertex to be processed.
	 * @param primitiveCount  The number of primitives to render.
	 *
	 * @return A HRESULT value indicating whether the operation succeeded or failed.
	 */
	public static native int DrawIndexedPrimitive(long device, int primitiveType, int baseVertexIndex,
			int minVertexIndex, int numVertices, int startIndex, int primitiveCount);

	/**
	 * Draw an indexed primitive using an index buffer.
	 *
	 * @param device          a pointer to the Direct3D device.
	 * @param indexBuffer     a pointer to the index buffer to use.
	 * @param primitiveType   the type of primitive to draw.
	 * @param baseVertexIndex the offset of the first vertex in the vertex buffer.
	 * @param minIndex        the minimum index value for vertices used during this
	 *                        call.
	 * @param numVertices     the number of vertices to use during this call.
	 * @param startIndex      the offset of the first index to use.
	 * @param primitiveCount  the number of primitives to draw.
	 * @return HRESULT indicating success or failure of the operation.
	 */
	public static native int DrawIndexedPrimitiveIB(long device, long indexBuffer, int primitiveType,
			int baseVertexIndex, int minIndex, int numVertices, int startIndex, int primitiveCount);

	/**
	 * Draw non-indexed primitives of a specified type.
	 *
	 * @param device         a pointer to the Direct3D device.
	 * @param primitiveType  the type of primitive to draw.
	 * @param startVertex    the index of the first vertex to use.
	 * @param primitiveCount the number of primitives to draw.
	 * @return HRESULT indicating success or failure of the operation.
	 */
	public static native int DrawPrimitive(long device, int primitiveType, int startVertex, int primitiveCount);

	/**
	 * Create a texture resource.
	 *
	 * @param device a pointer to the Direct3D device.
	 * @param width  the width of the texture, in pixels.
	 * @param height the height of the texture, in pixels.
	 * @param levels the number of mip levels in the texture.
	 * @param usage  the intended usage of the texture.
	 * @param format the pixel format of the texture.
	 * @param pool   the memory pool to be used for the texture.
	 * @return a pointer to the created texture resource.
	 */
	public static native long CreateTexture(long device, int width, int height, int levels, int usage, int format,
			int pool);

	/**
	 * Create a cube texture resource.
	 *
	 * @param device     a pointer to the Direct3D device.
	 * @param edgeLength the length of each edge of the cube, in pixels.
	 * @param levels     the number of mip levels in the texture.
	 * @param usage      the intended usage of the texture.
	 * @param format     the pixel format of the texture.
	 * @param pool       the memory pool to be used for the texture.
	 * @return a pointer to the created texture resource.
	 */
	public static native long CreateCubeTexture(long device, int edgeLength, int levels, int usage, int format,
			int pool);

	/**
	 * Create a volume texture resource.
	 *
	 * @param device a pointer to the Direct3D device.
	 * @param width  the width of the texture, in pixels.
	 * @param height the height of the texture, in pixels.
	 * @param depth  the depth of the texture, in pixels.
	 * @param levels the number of mip levels in the texture.
	 * @param usage  the intended usage of the texture.
	 * @param format the pixel format of the texture.
	 * @param pool   the memory pool to be used for the texture.
	 * @return a pointer to the created texture resource.
	 */
	public static native long CreateVolumeTexture(long device, int width, int height, int depth, int levels, int usage,
			int format, int pool);

	/**
	 * 
	 * Sets a texture to a specific texture stage for rendering.
	 * 
	 * @param target The texture stage target.
	 * @param slot   The texture stage slot.
	 * @param handle The handle to the texture.
	 * @return The result of the operation.
	 */
	public static native int SetTexture(long target, int slot, long handle);

	/**
	 * 
	 * Sets a stage-specific state for a texture.
	 * 
	 * @param device The device associated with the texture stage.
	 * @param stage  The stage for which to set the state.
	 * @param state  The state to set.
	 * @param value  The value for the state.
	 * @return The result of the operation.
	 */
	public static native int SetTextureStageState(long device, int stage, int state, int value);

	/**
	 * 
	 * Sets a sampler state.
	 * 
	 * @param device  The device to associate the state with.
	 * @param sampler The index of the sampler state to set.
	 * @param state   The state to set.
	 * @param value   The value for the state.
	 * @return The result of the operation.
	 */
	public static native int SetSamplerState(long device, int sampler, int state, int value);

	/**
	 * 
	 * Updates a texture with data from a source texture.
	 * 
	 * @param device      The device associated with the texture.
	 * @param source      The source texture to update from.
	 * @param destination The destination texture to update.
	 * @return The result of the operation.
	 */
	public static native int UpdateTexture(long device, long source, long destination);

	/**
	 * 
	 * Creates a vertex shader.
	 * 
	 * @param device   The device to associate the shader with.
	 * @param bytecode The bytecode for the shader.
	 * @return The handle to the created shader.
	 */
	public static native long CreateVertexShader(long device, byte[] bytecode);

	/**
	 * 
	 * Creates a pixel shader.
	 * 
	 * @param device   The device to associate the shader with.
	 * @param bytecode The bytecode for the shader.
	 * @return The handle to the created shader.
	 */
	public static native long CreatePixelShader(long device, byte[] bytecode);

	/**
	 * 
	 * Sets a pixel shader for rendering.
	 * 
	 * @param device The device associated with the shader.
	 * @param shader The handle to the shader to set.
	 * @return The result of the operation.
	 */
	public static native int SetPixelShader(long device, long shader);

	/**
	 * 
	 * Sets a vertex shader for rendering.
	 * 
	 * @param device The device associated with the shader.
	 * @param shader The handle to the shader to set.
	 * @return The result of the operation.
	 */
	public static native int SetVertexShader(long device, long shader);

	/**
	 * 
	 * Sets a pixel shader constant to an array of floats.
	 * 
	 * @param device        The device associated with the shader.
	 * @param startRegister The starting register index to set.
	 * @param values        The values to set.
	 * @param count         The number of float values to set.
	 * @return The result of the operation.
	 */
	public static native int SetPixelShaderConstantF(long device, int startRegister, long values, int count);

	/**
	 * 
	 * Sets a vertex shader constant to an array of floats.
	 * 
	 * @param device        The device associated with the shader.
	 * @param startRegister The starting register index to set.
	 * @param values        The values to set.
	 * @param count         The number of float values to set.
	 * @return The result of the operation.
	 */
	public static native int SetVertexShaderConstantF(long device, int startRegister, long values, int count);

	/**
	 * 
	 * Creates a new event query.
	 * 
	 * @param device The device to associate the query with.
	 * @return The handle to the created query.
	 */
	public static native long CreateEventQuery(long device);

	/**
	 * Clears the specified buffers to the given values.
	 *
	 * @param device  the device to use for clearing the buffers
	 * @param buffers a bitwise OR of one or more values indicating which buffers to
	 *                clear
	 * @param color   the color to use for clearing the color buffer
	 * @param depth   the value to use for clearing the depth buffer
	 * @param stencil the value to use for clearing the stencil buffer
	 * @return the result code indicating success or failure of the operation
	 */
	public static native int Clear(long device, int buffers, int color, float depth, int stencil);

	/**
	 * 
	 * Signals the start of a scene.
	 * 
	 * @param deviceHandle a handle to the device to use for rendering.
	 * @return S_OK if successful; otherwise, returns D3DERR_INVALIDCALL.
	 */
	public static native int BeginScene(long deviceHandle);

	/**
	 * 
	 * Signals the end of a scene.
	 * 
	 * @param deviceHandle a handle to the device to use for rendering.
	 * @return S_OK if successful; otherwise, returns D3DERR_INVALIDCALL.
	 */
	public static native int EndScene(long deviceHandle);

	/**
	 * 
	 * Sets the specified device transformation state.
	 * 
	 * @param deviceHandle a handle to the device to set the transformation state
	 *                     for.
	 * @param state        a D3DTRANSFORMSTATETYPE value that specifies the type of
	 *                     transformation to set.
	 * @param matrix       an array of 16 floating-point values that represent the
	 *                     matrix to set.
	 * @return S_OK if successful; otherwise, returns D3DERR_INVALIDCALL.
	 */
	public static native int SetTransform(long deviceHandle, int state, float[] matrix);

	/**
	 * Sets the scissor rectangle for a specified device.
	 * 
	 * @param devicePointer pointer to the device
	 * @param left          the x-coordinate of the upper-left corner of the
	 *                      rectangle
	 * @param top           the y-coordinate of the upper-left corner of the
	 *                      rectangle
	 * @param right         the x-coordinate of the lower-right corner of the
	 *                      rectangle
	 * @param bottom        the y-coordinate of the lower-right corner of the
	 *                      rectangle
	 * @return HRESULT indicating success or failure of the operation
	 */
	public static native int SetScissorRect(long devicePointer, int left, int top, int right, int bottom);

	/**
	 * 
	 * Sets the viewport parameters for the device.
	 * 
	 * @param deviceHandle a handle to the device to set the viewport for.
	 * @param x            the x-coordinate of the upper-left corner of the
	 *                     viewport.
	 * @param y            the y-coordinate of the upper-left corner of the
	 *                     viewport.
	 * @param width        the width of the viewport.
	 * @param height       the height of the viewport.
	 * @param minZ         the minimum depth value of the viewport.
	 * @param maxZ         the maximum depth value of the viewport.
	 * @return S_OK if successful; otherwise, returns D3DERR_INVALIDCALL.
	 */
	public static native int SetViewport(long deviceHandle, int x, int y, int width, int height, float minZ,
			float maxZ);

	/**
	 * 
	 * Sets a render-state value.
	 * 
	 * @param deviceHandle a handle to the device to set the render state for.
	 * @param state        a D3DRENDERSTATETYPE value that specifies the render
	 *                     state to set.
	 * @param value        the value to set the render state to.
	 * @return S_OK if successful; otherwise, returns D3DERR_INVALIDCALL.
	 */
	public static native int SetRenderState(long deviceHandle, int state, int value);

	/**
	 * Sets a floating-point render state for the device.
	 *
	 * @param device The device to set the render state for.
	 * @param state  The render state to set.
	 * @param value  The value to set the render state to.
	 * @return HRESULT indicating success or failure of the operation.
	 */
	public static native int SetRenderStatef(long device, int state, float value);

	/**
	 * Sets a Boolean render state for the device.
	 *
	 * @param device The device to set the render state for.
	 * @param state  The render state to set.
	 * @param value  The value to set the render state to.
	 * @return HRESULT indicating success or failure of the operation.
	 */
	public static native int SetRenderStateb(long device, int state, boolean value);

	/**
	 * Sets a light for the device.
	 *
	 * @param device The device to set the light for.
	 * @param index  The index of the light to set.
	 * @param light  The light to set.
	 * @return HRESULT indicating success or failure of the operation.
	 */
	public static native int SetLight(long device, int index, long light);

	/**
	 * Enables or disables a light for the device.
	 *
	 * @param device The device to enable or disable the light for.
	 * @param index  The index of the light to enable or disable.
	 * @param enable Whether to enable or disable the light.
	 * @return True if the operation was successful; false otherwise.
	 */
	public static native boolean LightEnable(long device, int index, boolean enable);

	/**
	 * Resets the device to its initial state.
	 *
	 * @param device        The device to reset.
	 * @param presentParams The presentation parameters to use when resetting the
	 *                      device.
	 * @return HRESULT indicating success or failure of the operation.
	 */
	public static native int Reset(long device, D3DPRESENT_PARAMETERS presentParams);

	/**
	 * Checks the cooperative level of the device.
	 *
	 * @param device The device to check the cooperative level for.
	 * @return A HRESULT indicating the cooperative level of the device.
	 */
	public static native int TestCooperativeLevel(long device);

	public static int setRenderStateF(long device, int state, float value) {
		return SetRenderStatef(device, state, value);
	}

	public static int setRenderStateB(long device, int state, boolean value) {
		return SetRenderStateb(device, state, value);
	}
}
