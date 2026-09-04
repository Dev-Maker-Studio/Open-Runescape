# Shader System Deobfuscation Details

## This Session (BUILD VERIFIED, all groups passed)

### Group 1-7 (SpriteGL, NativeSprite, FramebufferCompositor, LightGL, ShaderMappingGL, ShaderVariableDecoder, ShaderAttribute)
See MEMORY.md for summaries. All build-verified.

### Group 8 - Abstract Shader Fields (BUILD VERIFIED)

**AbstractEnvironmentShader (8 fields)**:
- `anInt467` -> `startVertex` (drawIndexedPrimitive param 2)
- `anInt468` -> `vertexCount` (drawIndexedPrimitive param 3)
- `anInt469` -> `startIndex` (drawIndexedPrimitive param 4)
- `anInt470` -> `primitiveCount` (drawIndexedPrimitive param 5)
- `aFloat456` -> `specularExponent` (loaded as SpecularExponent uniform, values 2-32)
- `aFloat453` -> `specularIntensity` (loaded alongside specularExponent, values 0.5-0.8)
- `anInterface31451` -> `environmentSampler` (CubeTextureSampler, loaded as EnvironmentSampler)
- `aClass384465` -> `worldMatrix` (Matrix44, loaded as WorldMatrix uniform)

**AbstractSpriteShader (7 fields)**:
- `anInterface6441` -> `maskSampler` (loaded as MaskSampler at slot 1)
- `anInterface6443` -> `spriteSampler` (loaded as SpriteSampler at slot 0)
- `anInterface4445` -> `vertexStream` (NativeStream, passed to setStreamSource)
- `anInt446` -> `startVertex` (passed to drawPrimitive)
- `aClass70447` -> `vertexDeclaration` (passed to setVertexDeclaration)
- `aClass384440` -> `spriteTexCoordMatrix` (loaded as SpriteTexCoordMatrix uniform)
- `aClass384444` -> `maskTexCoordMatrix` (loaded as MaskTexCoordMatrix uniform)

Files: AbstractEnvironmentShader.kt, AbstractSpriteShader.kt, NativeEnvironmentShaderHd.kt, NativeEnvironmentShaderLd.kt, NativeSpriteShaderHd.kt, NativeSpriteShaderLd.kt, NativeFont.kt

### Group 9 - Shader Methods + Water/Particle Fields (BUILD VERIFIED)

**AbstractParticleShader (3 methods)**:
- `method1787` -> `drawParticles` (draws indexed particle quads)
- `method1788` -> `setViewProjectionMatrix` (copies + multiplies by frustumClipMatrix)
- `method1790` -> `drawPointSprite` (draws single quad via drawQuadTriangleFan)
Files: AbstractParticleShader.kt, NativeParticleShaderHd.kt, NativeParticleShaderLd.kt, NativeParticleRenderer.kt

**NativeParticleShaderLd (1 method)**:
- `method14513` -> `loadTextureState` (sets up texture stages for LD particle rendering)

**AbstractBloomShader (5 methods)**:
- `method1097` -> `loadShader` (loads/compiles shader programs, returns bool)
- `method1099` -> `reloadShader` (wrapper calling loadShader, overrideable)
- `method1102` -> `onRendererInitialized` (called when bloom renderer FBOs ready)
- `method1105` -> `onShutdown` (called during toolkit shutdown)
- `method1107` -> `isLinked` (returns hasLinkedVector flag)
Files: AbstractBloomShader.kt, NativeFilterBloomShader.kt, NativeBloomRenderer.kt, AbstractNativeToolkit.kt

**AbstractWaterShader (10 methods)**:
- `method873` -> `activate` (begin rendering, set active mapping + flags)
- `method875` -> `prepareForRendering` (hook after activation)
- `method876` -> `beginWaterTile` (hook before water tile rendering)
- `method877` -> `updateWaterState` (update water fog/state)
- `method878` -> `destroy` (cleanup on shutdown)
- `method879` -> `loadDefaultModelMatrix` (loads identity model matrix)
- `method885` -> `deactivate` (reset shader)
- `method886` -> `resetModelMatrix` (loads identity into model matrix)
- `method888` -> `activateForPass` (activate with full flag reset)
- `method889` -> `resetModelMatrixForPass` (identity model matrix for render pass)
Files: AbstractWaterShader.kt, NativeWaterShader.kt, AbstractNativeToolkit.kt

**NativeWaterShader (5 methods)**:
- `method14526` -> `loadShaderUniforms` (loads lights + texture matrix + matrices)
- `method14529` -> `loadEyePositionOS` (transforms eye to object space)
- `method14532` -> `loadModelMatrix` (loads model + derived matrices)
- `method14549` -> `loadSunDirectionOS` (transforms sun to object space)
- `method14550` -> `resetTextureState` (identity + setTextured)

**NativeEvtWaterMapper (5 fields + 2 methods)**:
- `anInt10094` -> `startIndex`, `anInt10110` -> `startVertex`, `anInt10111` -> `vertexCount`, `anInt10112` -> `primitiveCount`
- `aFloat10114` -> `textureScale` (1/4096 texture UV scale factor)
- `method15517` -> `updateWaterParameters` (sets sun/wave/texture vectors)
- `method15520` -> `renderWater` (full render: update params + load program + draw)

**NativeWaterfall (9 fields + 2 methods)**:
- `anInt10142` -> `startIndex`, `anInt10152` -> `startVertex`, `anInt10153` -> `vertexCount`, `anInt10155` -> `primitiveCount`
- `aClass38410138` -> `viewProjectionMatrix`, `aClass38410146` -> `waterfallModelMatrix` (renamed from modelMatrix to avoid clash with parent NativeWaterShader.modelMatrix)
- `aFloat10149` -> `flowTime`, `aFloatArray10148` -> `uGenerationCoefficients`, `aFloatArray10150` -> `vGenerationCoefficients`
- `aClass4910147` -> `waterTexture` (NativeWaterTexture)
- `method15549` -> `renderWaterfall`, `method15550` -> `updateFlowParameters`

## drawIndexedPrimitive Parameter Semantics

`drawIndexedPrimitive(drawMode, startVertex, vertexCount, startIndex, primitiveCount)`
- Used consistently across: AbstractEnvironmentShader, NativeEvtWaterMapper, NativeWaterfall, NativeParticleShaderHd/Ld
- Confirmed via OpenGLNativeToolkit.kt implementation: i_4 (startIndex) used as buffer offset, amt (primitiveCount) used to calculate element count

## Name Clash Lesson: NativeWaterfall.modelMatrix

When renaming `aClass38410146` (Matrix44 data) in NativeWaterfall to `modelMatrix`, it clashed with parent NativeWaterShader's `modelMatrix` (ShaderAttribute). Resolved by using `waterfallModelMatrix` for the data field. Always check parent class fields before naming.
