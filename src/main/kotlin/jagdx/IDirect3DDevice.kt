/* IDirect3DDevice - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

class IDirect3DDevice : IUnknown() {

    companion object {
        /**
         * Destroys the specified Direct3D device.
         *
         * @param device a pointer to the Direct3D device to destroy.
         */
        @JvmStatic
        external fun Destroy(device: Long)

        /**
         * Retrieves a pointer to the back buffer of the specified swap chain.
         *
         * @param swapChain       a pointer to the Direct3D swap chain to retrieve the
         *                        back buffer from.
         * @param backBufferIndex the index of the back buffer to retrieve.
         * @return a pointer to the back buffer of the specified swap chain.
         */
        @JvmStatic
        external fun GetRenderTarget(swapChain: Long, backBufferIndex: Int): Long

        /**
         * Copies the contents of the source surface to the destination surface.
         *
         * @param sourceSurface      a pointer to the source surface.
         * @param sourceRect         a RECT structure that defines the portion of the
         *                           source surface to copy.
         * @param destinationSurface a pointer to the destination surface.
         * @return D3D_OK if the operation succeeds, or an error code otherwise.
         */
        @JvmStatic
        external fun GetRenderTargetData(sourceSurface: Long, sourceRect: Long, destinationSurface: Long)

        /**
         * Sets the render target for the Direct3D device.
         *
         * @param device            a pointer to the Direct3D device.
         * @param renderTargetIndex the index of the render target to set.
         * @param renderTarget      a pointer to the surface to set as the render
         *                          target.
         * @return D3D_OK if the operation succeeds, or an error code otherwise.
         */
        @JvmStatic
        external fun SetRenderTarget(device: Long, renderTargetIndex: Int, renderTarget: Long): Int

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
        @JvmStatic
        external fun CreateRenderTarget(device: Long, width: Int, height: Int, format: Int, multiSampleType: Int,
                                        multiSampleQuality: Int, lockable: Boolean): Long

        /**
         * Retrieves a pointer to the specified swap chain.
         *
         * @param device         a pointer to the Direct3D device.
         * @param swapChainIndex the index of the swap chain to retrieve.
         * @return a pointer to the specified swap chain, or null if the operation
         *         fails.
         */
        @JvmStatic
        external fun GetSwapChain(device: Long, swapChainIndex: Int): Long

        /**
         * Creates an additional swap chain for the Direct3D device.
         *
         * @param device        a pointer to the Direct3D device.
         * @param presentParams a D3DPRESENT_PARAMETERS structure that describes the
         *                      swap chain to create.
         * @return a pointer to the newly created swap chain, or null if the operation
         *         fails.
         */
        @JvmStatic
        external fun CreateAdditionalSwapChain(device: Long, presentParams: D3DPRESENT_PARAMETERS): Long

        /**
         * Retrieves the depth-stencil surface associated with the current render
         * target.
         *
         * @param deviceContext Pointer to the device context object.
         * @return A pointer to the depth-stencil surface object.
         */
        @JvmStatic
        external fun GetDepthStencilSurface(deviceContext: Long): Long

        /**
         * Sets the depth-stencil surface that should be used for rendering.
         *
         * @param deviceContext Pointer to the device context object.
         * @param surface       Pointer to the depth-stencil surface object.
         * @return HRESULT indicating success or failure.
         */
        @JvmStatic
        external fun SetDepthStencilSurface(deviceContext: Long, surface: Long): Int

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
        @JvmStatic
        external fun CreateDepthStencilSurface(device: Long, width: Int, height: Int, format: Int,
                                               multiSampleType: Int, multisampleQuality: Int, discard: Boolean): Long

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
        @JvmStatic
        external fun CreateOffscreenPlainSurface(device: Long, width: Int, height: Int, format: Int, pool: Int): Long

        /**
         * Retrieves a back buffer from the swap chain.
         *
         * @param swapChain      Pointer to the swap chain object.
         * @param i              The index of the back buffer to retrieve.
         * @param backBufferType The type of surface to return.
         * @param backBuffer     Pointer to a pointer to the back buffer surface.
         * @return HRESULT indicating success or failure.
         */
        @JvmStatic
        external fun GetBackBuffer(swapChain: Long, i: Int, backBufferType: Int, backBuffer: IntArray): Long

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
        @JvmStatic
        external fun StretchRect(destinationSurface: Long, sourceSurface: Long, destinationX: Int,
                                 destinationY: Int, destinationWidth: Int, destinationHeight: Int, sourceRectangle: Long, sourceX: Int,
                                 sourceY: Int, sourceWidth: Int, sourceHeight: Int, filter: Int): Int

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
        @JvmStatic
        external fun CreateVertexBuffer(device: Long, length: Int, usage: Int, fvf: Int, pool: Int): Long

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
        @JvmStatic
        external fun CreateIndexBuffer(device: Long, length: Int, usage: Int, format: Int, pool: Int): Long

        /**
         *
         * Creates a new vertex declaration object.
         *
         * @param devicePointer         Pointer to the IDirect3DDevice9 interface
         * @param vertexElementsPointer Pointer to an array of D3DVERTEXELEMENT9
         *                              structures
         * @return Pointer to the newly created vertex declaration object
         */
        @JvmStatic
        external fun CreateVertexDeclaration(devicePointer: Long, vertexElementsPointer: Long): Long

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
        @JvmStatic
        external fun SetStreamSource(device: Long, streamNumber: Int, buffer: Long, offset: Int, stride: Int): Int

        /**
         * Sets the vertex declaration.
         *
         * @param device      A pointer to the IDirect3DDevice9 interface.
         * @param declaration A pointer to the vertex declaration interface.
         *
         * @return A HRESULT value indicating whether the operation succeeded or failed.
         */
        @JvmStatic
        external fun SetVertexDeclaration(device: Long, declaration: Long): Int

        /**
         * Sets the index data for a device.
         *
         * @param device A pointer to the IDirect3DDevice9 interface.
         * @param buffer A pointer to the index buffer.
         *
         * @return A HRESULT value indicating whether the operation succeeded or failed.
         */
        @JvmStatic
        external fun SetIndices(device: Long, buffer: Long): Int

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
        @JvmStatic
        external fun DrawIndexedPrimitive(device: Long, primitiveType: Int, baseVertexIndex: Int,
                                          minVertexIndex: Int, numVertices: Int, startIndex: Int, primitiveCount: Int): Int

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
        @JvmStatic
        external fun DrawIndexedPrimitiveIB(device: Long, indexBuffer: Long, primitiveType: Int,
                                            baseVertexIndex: Int, minIndex: Int, numVertices: Int, startIndex: Int, primitiveCount: Int): Int

        /**
         * Draw non-indexed primitives of a specified type.
         *
         * @param device         a pointer to the Direct3D device.
         * @param primitiveType  the type of primitive to draw.
         * @param startVertex    the index of the first vertex to use.
         * @param primitiveCount the number of primitives to draw.
         * @return HRESULT indicating success or failure of the operation.
         */
        @JvmStatic
        external fun DrawPrimitive(device: Long, primitiveType: Int, startVertex: Int, primitiveCount: Int): Int

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
        @JvmStatic
        external fun CreateTexture(device: Long, width: Int, height: Int, levels: Int, usage: Int, format: Int,
                                   pool: Int): Long

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
        @JvmStatic
        external fun CreateCubeTexture(device: Long, edgeLength: Int, levels: Int, usage: Int, format: Int,
                                       pool: Int): Long

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
        @JvmStatic
        external fun CreateVolumeTexture(device: Long, width: Int, height: Int, depth: Int, levels: Int, usage: Int,
                                         format: Int, pool: Int): Long

        /**
         *
         * Sets a texture to a specific texture stage for rendering.
         *
         * @param target The texture stage target.
         * @param slot   The texture stage slot.
         * @param handle The handle to the texture.
         * @return The result of the operation.
         */
        @JvmStatic
        external fun SetTexture(target: Long, slot: Int, handle: Long): Int

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
        @JvmStatic
        external fun SetTextureStageState(device: Long, stage: Int, state: Int, value: Int): Int

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
        @JvmStatic
        external fun SetSamplerState(device: Long, sampler: Int, state: Int, value: Int): Int

        /**
         *
         * Updates a texture with data from a source texture.
         *
         * @param device      The device associated with the texture.
         * @param source      The source texture to update from.
         * @param destination The destination texture to update.
         * @return The result of the operation.
         */
        @JvmStatic
        external fun UpdateTexture(device: Long, source: Long, destination: Long): Int

        /**
         *
         * Creates a vertex shader.
         *
         * @param device   The device to associate the shader with.
         * @param bytecode The bytecode for the shader.
         * @return The handle to the created shader.
         */
        @JvmStatic
        external fun CreateVertexShader(device: Long, bytecode: ByteArray): Long

        /**
         *
         * Creates a pixel shader.
         *
         * @param device   The device to associate the shader with.
         * @param bytecode The bytecode for the shader.
         * @return The handle to the created shader.
         */
        @JvmStatic
        external fun CreatePixelShader(device: Long, bytecode: ByteArray): Long

        /**
         *
         * Sets a pixel shader for rendering.
         *
         * @param device The device associated with the shader.
         * @param shader The handle to the shader to set.
         * @return The result of the operation.
         */
        @JvmStatic
        external fun SetPixelShader(device: Long, shader: Long): Int

        /**
         *
         * Sets a vertex shader for rendering.
         *
         * @param device The device associated with the shader.
         * @param shader The handle to the shader to set.
         * @return The result of the operation.
         */
        @JvmStatic
        external fun SetVertexShader(device: Long, shader: Long): Int

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
        @JvmStatic
        external fun SetPixelShaderConstantF(device: Long, startRegister: Int, values: Long, count: Int): Int

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
        @JvmStatic
        external fun SetVertexShaderConstantF(device: Long, startRegister: Int, values: Long, count: Int): Int

        /**
         *
         * Creates a new event query.
         *
         * @param device The device to associate the query with.
         * @return The handle to the created query.
         */
        @JvmStatic
        external fun CreateEventQuery(device: Long): Long

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
        @JvmStatic
        external fun Clear(device: Long, buffers: Int, color: Int, depth: Float, stencil: Int): Int

        /**
         *
         * Signals the start of a scene.
         *
         * @param deviceHandle a handle to the device to use for rendering.
         * @return S_OK if successful; otherwise, returns D3DERR_INVALIDCALL.
         */
        @JvmStatic
        external fun BeginScene(deviceHandle: Long): Int

        /**
         *
         * Signals the end of a scene.
         *
         * @param deviceHandle a handle to the device to use for rendering.
         * @return S_OK if successful; otherwise, returns D3DERR_INVALIDCALL.
         */
        @JvmStatic
        external fun EndScene(deviceHandle: Long): Int

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
        @JvmStatic
        external fun SetTransform(deviceHandle: Long, state: Int, matrix: FloatArray): Int

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
        @JvmStatic
        external fun SetScissorRect(devicePointer: Long, left: Int, top: Int, right: Int, bottom: Int): Int

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
        @JvmStatic
        external fun SetViewport(deviceHandle: Long, x: Int, y: Int, width: Int, height: Int, minZ: Float,
                                 maxZ: Float): Int

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
        @JvmStatic
        external fun SetRenderState(deviceHandle: Long, state: Int, value: Int): Int

        /**
         * Sets a floating-point render state for the device.
         *
         * @param device The device to set the render state for.
         * @param state  The render state to set.
         * @param value  The value to set the render state to.
         * @return HRESULT indicating success or failure of the operation.
         */
        @JvmStatic
        external fun SetRenderStatef(device: Long, state: Int, value: Float): Int

        /**
         * Sets a Boolean render state for the device.
         *
         * @param device The device to set the render state for.
         * @param state  The render state to set.
         * @param value  The value to set the render state to.
         * @return HRESULT indicating success or failure of the operation.
         */
        @JvmStatic
        external fun SetRenderStateb(device: Long, state: Int, value: Boolean): Int

        /**
         * Sets a light for the device.
         *
         * @param device The device to set the light for.
         * @param index  The index of the light to set.
         * @param light  The light to set.
         * @return HRESULT indicating success or failure of the operation.
         */
        @JvmStatic
        external fun SetLight(device: Long, index: Int, light: Long): Int

        /**
         * Enables or disables a light for the device.
         *
         * @param device The device to enable or disable the light for.
         * @param index  The index of the light to enable or disable.
         * @param enable Whether to enable or disable the light.
         * @return True if the operation was successful; false otherwise.
         */
        @JvmStatic
        external fun LightEnable(device: Long, index: Int, enable: Boolean): Boolean

        /**
         * Resets the device to its initial state.
         *
         * @param device        The device to reset.
         * @param presentParams The presentation parameters to use when resetting the
         *                      device.
         * @return HRESULT indicating success or failure of the operation.
         */
        @JvmStatic
        external fun Reset(device: Long, presentParams: D3DPRESENT_PARAMETERS): Int

        /**
         * Checks the cooperative level of the device.
         *
         * @param device The device to check the cooperative level for.
         * @return A HRESULT indicating the cooperative level of the device.
         */
        @JvmStatic
        external fun TestCooperativeLevel(device: Long): Int

        @JvmStatic
        fun setRenderStateF(device: Long, state: Int, value: Float): Int {
            return SetRenderStatef(device, state, value)
        }

        @JvmStatic
        fun setRenderStateB(device: Long, state: Int, value: Boolean): Int {
            return SetRenderStateb(device, state, value)
        }
    }
}
