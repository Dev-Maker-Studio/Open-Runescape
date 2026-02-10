# Refactoring Engineer Memory

## Confirmed Class Identifications

- **Class111 = ShaderVariableType** (`graphics/shader/`) -- Enum-like, 125 singletons
- **Class455_Sub1 = MaskedTextureSurface** (`toolkit/`) -- ComponentSurface for masked textures
- **Class170 = FramebufferCompositor** (`graphics/`) -- FBO manager for post-processing
- **Node_Sub5 = PostProcessEffect** (`graphics/texture/`) -- Abstract post-processing
- **NativeBloomRenderer** (`graphics/`) -- Bloom ping-pong FBO renderer
- **Node_Sub26 = AudioNode**, Node_Sub15_Sub1 = VorbisPcmStream, Node_Sub15_Sub4 = PcmStreamScheduler
- **Node_Sub22 = ScheduledAudioEvent**, Node_Sub41 = OggStreamDecoder, Sub1 = Null, Sub2 = OggKateDecoder
- **Class149 = TextureShapeRasterizer** (Sub1=Ellipse, Sub2=Bezier, Sub3=Line, Sub4=Rectangle)
- **Class350 subs**: Sub1=SpriteLoadingBarElement, Sub2=Color, Sub3=MultiSprite, Sub3_Sub1=Animated
- **Class43 = LoginBackgroundSprite**, Class52_Sub1 = SpriteLoadingBar, Class52_Sub3 = SolidLoadingBar
- **Class370 subs**: Sub1=WaveNoiseGenerator, Sub2=BillowNoiseGenerator (with Native/GL variants)

## Rosetta Stone Mappings

| Pattern | Type |
|---------|------|
| `aClass384NNNN` | Matrix44 |
| `aClass294NNNN` | Matrix43 |
| `aClass328NNNN` | NativeBloomRenderer |
| `aClass170NNNN` | FramebufferCompositor |
| `aClass149NNNN` | TextureShapeRasterizer |
| `aClass111NNNN` | ShaderVariableType |
| `aClass166NNNN` | ModelNormalsGL |

## Key Deobfuscation Patterns

- **CS2 opcode names** directly reveal field purposes (e.g., `USERDETAIL_LOBBY_CCEXPIRY` -> lobbyCreditCardExpiry)
- **drawIndexedPrimitive** params: `(drawMode, startVertex, vertexCount, startIndex, primitiveCount)`
- **Shader attribute names** map directly to GLSL uniforms (DiffuseSampler, WVPMatrix, etc.)

## Completed Deobfuscation Summary (see topic files for details)

- **Client/Camera/Mouse/Console**: client-kt.md, 68 fields + 13 methods in Client.kt, 11+6 in Camera.kt
- **CS2Interpreter**: cs2-interpreter.md, ~337 refs, 2 remaining uncertain fields
- **Toolkit Hierarchy (COMPLETE)**: abstract-native-toolkit.md, ALL own fields + methods deobfuscated
  - AbstractToolkit: 5 abstract methods renamed (hasHardwareRendering, getScratchMatrix, compositeBloomEffects, beginBloomCapture, drawSurfaceLine)
  - AbstractNativeToolkit: 36+ fields + 38 methods (viewport, matrices, fog, lighting, bloom, state)
  - OpenGlToolkit: 33+ fields + 11 methods (GL state, view/projection, fog, sun)
  - OpenGLNativeToolkit: 2 own fields + 1 method (currentElementsBuffer, activeArrayBuffers, toGlTextureSource)
  - SafeModeToolkit: 12+ fields + 6 methods (viewport, depth, frustum, dash lines, particles)
  - scratchMatrix (was incorrectly named shadowProjectionMatrix -- only used as getScratchMatrix() return)
- **ModelGL/Sound/Graphics**: model-gl.md, sound-package.md
- **Shader system**: shader-deobfuscation.md (THIS SESSION + previous)
  - ShaderAttribute, ShaderVariableDecoder, ShaderMappingGL (all fields/methods)
  - AbstractEnvironmentShader (8 fields), AbstractSpriteShader (7 fields)
  - AbstractParticleShader (3 methods), AbstractBloomShader (5 methods)
  - AbstractWaterShader (10 methods), NativeWaterShader (5 methods)
  - NativeEvtWaterMapper (5 fields + 2 methods), NativeWaterfall (9 fields + 2 methods)
  - NativeParticleShaderLd (1 method), all impl files clean
- **Scene/World**: GameState, Matrix44/43, Skybox, FramebufferCompositor
- **GL Support Classes (COMPLETE)**:
  - GLFormat: RGB, LUMINANCE, LUMINANCE_ALPHA, DEPTH, COMPRESSED_DXT1, COMPRESSED_DXT5 (NOTE: existing "RGBA" is actually ALPHA, "HDR" is RGBA -- previous misrenames)
  - GLActionFormat: BYTE, SHORT, INT, UNSIGNED_INT + renamed anInt757 to dataTypeId
  - TextureStageState: TEXTURE, PRIMARY_COLOR, PREVIOUS, CONSTANT
  - VertexDeclarationAction: POSITION, COLOR, TEXCOORD, TEXCOORD_1F/3F/4F, BONE_INDICES, BONE_WEIGHTS, TANGENT, BINORMAL, GENERIC_4F + anInt705->componentCount, anInt702->BITS_PER_SLOT
  - MatrixMode: ORTHO (was aClass33380, default/2D mode)
- **Sprites**: SpriteGL (4 fields + 3 methods), NativeSprite (3 companion + 12 methods)
- **Lighting**: LightGL (18 fields + 3 methods)
- **Class renames**: 8 Node_Sub, 6 unidentified package migrations, 18 Class renames
- **Loading bar**: TipRenderDetails constants, decoder functions
- **CreateAccountRequest + AccountCreationStage**: fields, methods, enum constants
- **Shadow/WorldMap/UI/Fonts/StaticMethods/ArrayUtils/Utils/PacketEncoder**: ~75 methods + 30 fields
- **Texture Operations (COMPLETE)**: All fields + methods in `config/material/texture/operation/` deobfuscated
  - 29 TextureOp files + 5 rasterizer files, ~130 field renames + ~25 method renames
  - TextureShapeRasterizer: fillColor, strokeColor, strokeWidth, pixelBuffer, drawFilled/Stroked/FilledAndStroked, drawLine, drawVerticalLine, drawClippedVerticalLine
  - TextureOpRasterizer: clipMinX/Y, clipMaxX/Y, rasterizeShapes, setClipBounds
  - EllipseRasterizer: centerX/Y, radiusX/Y, drawClipped/UnclippedEllipse, drawCircle, drawEllipse
  - BezierCurveRasterizer: controlX0-3/controlY0-3, drawBresenhamLine, drawClipped/UnclippedBezier, drawBezierCurve
  - LineRasterizer: startX/Y, endX/Y
  - RectangleRasterizer: left, top, right, bottom, drawStrokedRectangle, drawClipped/UnclippedStrokedRectangle
  - TextureOpIrregularBricks.backgroundColor: cross-referenced from CS2Interpreter + NativeSprite (NOT unused!)
  - Remaining external refs (outside scope): StaticMethods.anInt118 (Voronoi F2 distance), TextureRaster.anInt3678
- **WorldMapSub1 (COMPLETE own fields/methods)**: 14 fields + 6 methods renamed
  - mapCenterX/Y, scrollTargetX/Y, requestedScrollX/Y, useTargetPosition, pendingMapElementId
  - viewBottomTileY, hideIcons, highlightedCategoryId, hiddenMecIds, hiddenCategoryIds, categoryIterator
  - clampMapCenter, setMapCenterX, setMapCenterY, highlightCategory, renderOffscreenIndicator, getFirstActiveCategory
- **WorldMap.kt (COMPLETE own fields/methods)**: 12 fields + 8 methods renamed
  - tileViewStart/EndX/Y, pixelViewStart/EndX/Y, tileShapeAndRotations, underlayGreenBlue, halfZoomLevel, tileShapePatterns
  - getViewportCategories, buildMapCategories, setViewportBounds, positionCategory, renderCategoryLines, positionAllCategories, loadMapTileData, getNextActiveCategory
- **BasType.kt (COMPLETE)**: 13 fields (yaw/roll/pitch accel/maxVelocity/targetAngle, modelWidth/Length, hillRotateX/Z, iconHeightOverride)
- **GroundItem.kt**: tileHeightOffset
- **PathingEntity.kt**: 4 fields + 5 methods (overhead sprites, ground contour, stalled steps, walk speed, equipment overrides, overhead interface)
- **PlayerUpdate.kt**: 4 fields + 5 methods (inactive/update mask player indices, hint details, var change, entity priorities, entity positioning)
- **PlayerEntity.kt**: 2 bools (hasPendingPositionUpdate, shouldSuppressMovingAnimation)
- **CutsceneActionType.kt (COMPLETE)**: 16 constants renamed (PLAY_SONG/SYNTH/JINGLE/VORBIS, FADE_SCREEN, TILE_MESSAGE, POSITIONED_GFX, PROJECTILE_*, SET_VARIABLE, SET_BIT_VARIABLE, EXECUTE_SCRIPT, FINISHED, UNUSED_43)
- **Component/InterfaceRenderer/InterfaceComponentRenderer (COMPLETE)**: All own obfuscated identifiers renamed
  - Component.kt: 35 fields (event hooks, params, tick trackers, drag settings, video ID), 7 methods, 1 enum
  - InterfaceRenderer.kt: 3 fields (worldMapDragStartY, dragTickCount, translatedUptext), 1 method (startDragging)
  - InterfaceComponentRenderer.kt: 6 fields (deferredDrag*, chatDisplay*), 2 methods (setChatDisplay, drawMinimapText)

## Lessons Learned

- **replace_all is dangerous**: `Class111` matches `aClass1111109`. Use `\b` word boundaries.
- **Linter silently reverts**: Use `sed -i` via Bash instead of Edit tool for renames.
- **Build between every class rename**: Class renames touch many files; verify immediately.
- **Pre-existing build errors exist**: Filter with `grep "^e:"` and check only your files.
- **Parent class field name clashes**: Check parent class fields before naming (e.g., NativeWaterfall.modelMatrix vs NativeWaterShader.modelMatrix). Prefix to avoid.
- **Override method rename collision**: When a file has both `override fun foo()` and `fun method1234()`, renaming method1234 to foo causes conflict.
- **grep ALL references before sed**: Always do a full grep before using sed. Missing one file causes build failure.
- **Substring matching for class hierarchies**: Rename LONGEST names first. Use `\b` word boundaries.
- **Class149_Sub1 is Ellipse not Rectangle**: Always verify code behavior, not previous agent claims.
- **Linter restores `rm`'d files**: Must use `git rm -f` to delete tracked files.
- **Read tool cache is STALE**: Verify actual file content via `cat` before writing new files.
- **Stale TAR artifacts**: Use `./gradlew clean build` to resolve incremental build issues.
- **CS2 opcode names are strongest evidence** for field naming.
- **ShaderAttribute init block** reads from decoder: renaming decoder fields requires updating init block references.
- **Edit tool replace_all does substring match**: `UnkSetting1` replace matched `anUnkSetting11` producing `anDragType1`. Always verify composite identifiers after bulk replace.
- **Companion static fields may be cross-referenced from unrelated classes**: anInt9792 in TextureOpIrregularBricks was used by CS2Interpreter and NativeSprite as a backgroundColor. Always grep the ENTIRE codebase for companion field references before assuming "unused".

## Remaining Uncertain Fields (deliberately left obfuscated)

### Client.kt (14 uncertain)
aBool7151(p29), aBool7158(p2), aBool7159(p25), aString7165(p3), aBool7189(p28), aBool7323(unused login), aByte7458(deprecated packet), aString7156(p4), aBool7160(p22), anInt7163(p27), aBool7241(hide NPCs?), aBool7322(post-email), aString5555(p13), anInt2831(p30)

### CS2Interpreter.kt (2 uncertain)
Client.aBool7158 (isApplet conflict), Client.aString7165 (unknown param)
