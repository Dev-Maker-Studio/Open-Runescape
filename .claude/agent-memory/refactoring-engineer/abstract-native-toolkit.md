# AbstractNativeToolkit.kt Deobfuscation Notes

## Field Renames (Batches 1-6, 14, 17)

### Matrix44 Fields (Rosetta Stone: aClass384NNNN)
| Old | New | Purpose |
|-----|-----|---------|
| aClass3848729 | viewProjectionMatrix | Combined VP matrix for frustum culling |
| aClass3848774 | orthoProjectionMatrix | 2D orthographic projection |
| aClass3848725 | identityMatrix | Default identity, used as activeProjectionSource in DEFAULT mode |
| aClass3848708 | activeProjectionSource | Points to identity/ortho/scene matrix based on mode |
| aClass3848724 | sceneMatrix | 3D scene projection matrix |
| aClass3848683 | perspectiveMatrix | Scratch matrix for perspective calculations |
| aClass3848814 | eyePositionMatrix | Inverted view matrix for eye-space transforms |
| aClass3848841 | scratchMatrix | Temporary matrix returned by getScratchMatrix() |
| aClass3848715 | unusedMatrix | Never referenced anywhere (dead code) |
| aClass3848712 (companion) | IDENTITY_MATRIX | Static fallback for non-MODEL_VIEW mode |

### Matrix43 Fields (Rosetta Stone: aClass294NNNN)
| Old | New | Purpose |
|-----|-----|---------|
| aClass2948706 | viewMatrix43 | Camera view matrix (43-format) |
| aClass2948704 | localModelMatrix43 | Model transform (43-format) |
| aClass2948710 | modelViewMatrix43 | Combined model*view (43-format) |
| aClass2948804 | invertedViewMatrix43 | Inverted view for eye-space |
| aClass2948842 | renderingMatrix | Scene rendering transform |

### Viewport Fields
| Old | New | Used in |
|-----|-----|---------|
| anInt8749 | glViewportX | OpenGLNativeToolkit |
| anInt8776 | glViewportY | OpenGLNativeToolkit |
| anInt8751 | glViewportWidth | OpenGLNativeToolkit |
| anInt8752 | glViewportHeight | OpenGLNativeToolkit |
| anInt8823 | viewportOffsetX | OpenGLNativeToolkit |
| anInt8754 | viewportOffsetY | OpenGLNativeToolkit |

### Boolean State Fields
| Old | New | Purpose |
|-----|-----|---------|
| aBool8756 | depthWriteEnabled | GL depth write mask |
| aBool8684 | lightingEnabled | Lighting toggle state |
| aBool8760 | forceLightsDisabled | Force lights off (const) |
| aBool8720 | orthoProjectionDirty | Ortho matrix needs rebuild |
| aBool8723 | modelViewDirty | ModelView needs rebuild |
| aBool8681 | isShutdown | Toolkit has been shut down |
| aBool8805 | hasWaterReflection | Water reflection enabled (const) |

### Float/Int Fields
| Old | New | Purpose |
|-----|-----|---------|
| aFloat8737 | depthClipNear | GL depth range near |
| aFloat8738 | depthClipFar | GL depth range far |
| aFloat8797 | screenOriginX | Screen space center X |
| aFloat8741 | screenOriginY | Screen space center Y |
| anInt8844 | alphaBlendMode | Current alpha blend mode |
| anInt8680 | fogOffset | Fog distance offset |
| anInt8709 | shaderMode | Shader quality mode |
| aFloatArrayArray8730 | frustumPlanes | 6x4 frustum plane equations |
| aFloatArray8717 | unusedFloatArray | Never referenced (dead code) |

### Object/Interface Fields
| Old | New | Type |
|-----|-----|------|
| aClass498694 | waterTexture | NativeWaterTexture |
| aClass418753 | activeWaterShader | AbstractWaterShader |
| aClass152Sub28731 | sunCubeTextureGenerator | AbstractNativeCubeTextureGenerator |
| anInterface48746 | quadVertexStream | NativeStream |
| anInterface48693 | circleVertexStream | NativeStream |
| aClass708786 | quadVertexDeclaration | VertexDeclaration |
| aClass708832 | posColorVertexDeclaration | VertexDeclaration |
| aClass708836 | separateStreamVertexDeclaration | VertexDeclaration |
| aClass708721 | posColorTexVertexDeclaration | VertexDeclaration |
| anInterface328834 | indexBuffer | NativeToolkitBuffer |
| anInterface68795 | blackTextureSampler | NativeTextureSampler |
| aNativeSpriteSub38790 | whiteSprite | NativeSprite |
| aNativeSpriteSub38791 | blackSprite | NativeSprite |
| aClass328714 | bloomRenderer | NativeBloomRenderer |

## Method Renames (Batches 7-16)

### Matrix Mode Methods
| Old | New | Behavior |
|-----|-----|----------|
| method13934 | setDefaultMatrixMode | Sets mode to DEFAULT, uses identityMatrix |
| method13935 | setProjectionMatrixMode | Sets mode to PROJECTION, builds ortho matrix |
| method13937 | setModelViewMatrixMode | Sets mode to MODEL_VIEW, marks dirty |
| method13936 | invalidateOrthoProjection | Forces ortho matrix rebuild |
| method13938 | invalidateModelView | Forces modelView matrix rebuild |
| method13940 | markModelViewDirty | Sets modelViewDirty = true |

### View/Projection Methods
| Old | New | Behavior |
|-----|-----|----------|
| method13926 | buildViewProjectionMatrix | particle*scene -> frustum planes + VP matrix |
| method13931 | getViewProjectionMatrix | Getter for viewProjectionMatrix |

### Render State Methods
| Old | New | Behavior |
|-----|-----|----------|
| method13942 | setDepthWriteEnabled | Toggle depth write mask |
| method14003 | set2DRenderState | PROJECTION mode, no fog/lights/depth |
| method14004 | set3DRenderState | MODEL_VIEW mode, fog/depth/blending on |
| method13976 | applyAllRenderState | Applies ALL GL state settings |
| method13985 | resetAlphaBlendMode | Sets alphaBlendMode to 0 |

### Initialization Methods
| Old | New | Behavior |
|-----|-----|----------|
| method13895 | initializeShaders | Creates water, environment, sprite, particle shaders + bloom |
| method13899 | initializeResources | Creates bloom resources, particle renderer, vertex streams |
| method13903 | updateLightSources | Configures lights, swaps light count trackers |
| method13919 | resetViewportOffsets | Resets viewport offsets to 0 |

### Texture Methods
| Old | New | Behavior |
|-----|-----|----------|
| method13960 | resetTextureSlot | Reset texture state to INITIALIZE for current slot |
| method13966 | setTextureCombineMode | Set RGB + alpha combine modes |
| method13967 | setTextureColorStageState | Set color stage state for given slot |
| method13969 | setTextureAlphaStageState | Set alpha stage state for given slot |
| method13973 | getCurrentTextureMatrix | Returns textureMatrices[currentTextureSlot] |
| method14133 | applyTextureMatrix | Wrapper calling setTextureMatrix() |
| method14062 | updateWaterShaderAndFog | Notifies water shader + reconfigures fog |

### Vertex/Drawing Methods
| Old | New | Behavior |
|-----|-----|----------|
| method13944 | initQuadVertexStream | Creates quad with pos+color+texcoord*2 |
| method14009 | initLineVertexStream | Creates line with position-only |
| method14177 | initCircleVertexStream | Creates 256-segment circle |
| method14001 | drawQuadPrimitive | Binds quad stream, draws primitive |
| method14150 | drawFullscreenQuad | Draws quad as TRIANGLE_FAN |
| method13908 | uploadLineVertices | Uploads 2 vertex positions for line |
| method13911 | ensureIndexBufferCapacity | Ensures index buffer has enough space |

### Abstract Factory Methods
| Old | New | Behavior |
|-----|-----|----------|
| method13993 | createIndexBuffer | Creates NativeToolkitBuffer |
| method13995 | createVertexDeclaration | Creates VertexDeclaration from components |
| method13997 | bindIndexBuffer | Binds index buffer for drawing |
| method13954 | supportsTextureFormat | Format support check (always true in GL) |
| method13955 | supportsTexture3DFormat | Texture3D format support check |
| method13962 | createTexture3DSampler | Creates 3D texture sampler |
| method14064 | createRectangleTextureSampler | Creates rectangle sampler (NPOT handling) |
| method14072 | createEmptyTextureSampler | Creates empty sampler (delegates to rectangle) |
| method13905 | getTexelOffset | Pixel alignment offset (0.0 in GL, 0.5 in D3D) |

### Parent Class Methods (AbstractToolkit)
| Old | New | Behavior |
|-----|-----|----------|
| method8449 | getScratchMatrix | Returns temp Matrix44 for building projections |
| method8404 | supportsHardwareRendering | True for GL toolkits, false for SafeMode |
| method8525 | beginBloomCapture | Start bloom capture (width, height) |
| method8474 | endBloomCapture | End bloom capture |
| method8669 | drawTexturedSurfaceLine | Draw textured line on ComponentSurface |

## External Constant Renames

| Old | New | File |
|-----|-----|------|
| MatrixMode.aClass33380 | MatrixMode.DEFAULT | MatrixMode.kt |
| VertexDeclarationAction.aClass69695 | POSITION | VertexDeclarationAction.kt |
| VertexDeclarationAction.aClass69690 | COLOR | VertexDeclarationAction.kt |
| VertexDeclarationAction.aClass69692 | TEXCOORD | VertexDeclarationAction.kt |

## Remaining Obfuscated in VertexDeclarationAction.kt

| Field | Slot | Size | Format | Hypothesis |
|-------|------|------|--------|-----------|
| aClass69691 | 3 | 1 float | F32 | Point size? Used only in NativeBloomRenderer |
| aClass69693 | 5 | 3 float | F32 | Tangent/secondary color? |
| aClass69694 | 6 | 4 float | F32 | Bone weights? |
| aClass69696 | 7 | 4 float | F32 | Bone indices? |
| aClass69700 | 8 | 4 float | F32 | Unknown |
| aClass69699 | 9 | 4 float | F32 | Unknown |
| aClass69688 | 10 | 3 float | F32 | Unknown |
| aClass69697 | 11 | 3 float | F32 | Unknown |
