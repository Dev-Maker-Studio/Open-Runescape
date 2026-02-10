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

- **Client/Camera/Mouse/Console**: client-kt.md, 68+14 fields + 13 methods in Client.kt, 11+6 in Camera.kt
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
- **Shadow/WorldMap/UI/Fonts/StaticMethods/ArrayUtils/Utils/PacketEncoder**: ~75 methods + 30 fields + Phase 5C additions
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
- **Sound/Video Codec (COMPLETE own fields)**:
  - OggStreamDecoder: streamState, packetCount, processPacket (3 renames)
  - OggKateDecoder: granuleRateNumerator/Denominator, category, startTime, endTime, subtitleText, getStartTime/getEndTime/getSubtitleText (9 renames)
  - JagTheoraManager: theoraDecoder, vorbisDecoder, kateDecoder, isEndOfStream, isPaused, hasBufferedPage, isShutdown + 13 methods (shouldDecodeVideoFrame, readNextPage, decodeTheoraPacket, getVorbisDecoder, drainAllStreams, getTheoraDecoder, isPlaybackComplete, getPlaybackTime, togglePause, processStreams, getKateDecoder, processKateSubtitles, shutdown) (20 renames)
  - SoundResourceManager: midiPcmStream, pendingMidiStream, songLoadState, songStartPosition, getVideoSubtitleText, loadPendingSong (6 renames)
  - SongPlayer: channelVolumes, pcmPlayer (2 renames)
  - JagTheoraVorbisNode: vorbisInfo, vorbisComment, granuleTime, totalSamplesRead, dspState, vorbisBlock, resampler, pcmStream, getPcmStream, getBufferedSampleCount, getCurrentPlaybackTime, convertFloatToPcm16 (12 renames)
  - VorbisPcmStream: channelCount, isStopping, sampleOffset, leftVolume, rightVolume, isMuted, stop, getBufferedCount, advanceBuffer, allocateSampleData, enqueueSampleData, setMuted, getPlaybackTime, setVolume (14 renames)
  - MonoSterioData: samples, timestamp, getCacheKey (3 renames)
  - VorbisFileReference: currentChunkIndex, chunkBuffers, currentChunkPacket, prefetchChunks (4 renames)
- **AreaSoundStream (COMPLETE)**: 11 fields + 18 methods renamed
  - Fields: currentVolume, leftChannelVolume, rightChannelVolume, fadeSamplesRemaining, loopsRemaining, targetVolume, playbackPosition, panPosition, volumeFadeDelta, leftVolumeFadeDelta, rightVolumeFadeDelta
  - Methods: setPlaybackRate, isFading, reversePlaybackDirection, getTargetVolume, getPanPosition, setPlaybackPosition, isFinished, fadeToVolume, fadeToVolumeAndPan, fadeOutAndStop, getAbsolutePlaybackRate, getActiveVoiceCount, updateFadeState, muteVolume, recalculateChannelVolumes, setPanPosition, cancelFade, setVolumeAndPan
- **AudioBufferStream (COMPLETE)**: 2 own methods + 16 companion static methods renamed
  - Own: mixSamplesBackward, mixSamplesForward
  - Companion: mixForward/BackwardStereo/MonoConstant/Interpolated[Fading] (16 variants)
- **MixedAudioStream (COMPLETE)**: 2 own methods + 16 companion static methods renamed
  - Own: mixSamplesForward, mixSamplesBackward
  - Companion: mixForward/BackwardStereo/MonoConstant/Interpolated[Fading] (16 variants)
- **PcmStream**: method12228 -> getActiveVoiceCount (abstract, updated in all 6 overrides)
- **Phase 5C Core Utils/Cache/Network/WorldMap (COMPLETE)**: ~200 renames across 30+ files
  - HashTableIterator: 2 fields + 1 method (lastReturnedNode, nextNode, reset)
  - PingRequester.anInt5864 -> cameraTargetAbsX (6 cross-ref files)
  - ServerEnvironment.anInt9282 -> flashCycleCounter (5 cross-ref files)
  - TextUtils: 3 methods (stripDiacritics, compareLocaleAware, isValidCp1252Byte)
  - Interface: 6 methods + 1 field (getOrCreateMutableComponents, removeComponentSettings, runInterfaceScripts, moveComponentToBack/Front, hasActiveMenuEntry, worldMapDragStartX)
  - PacketEncoder: 11 methods (sendSignUpForm, sendPlayerOperation, sendInterfaceDragOntoInterface, sendInterfaceOnInterface, checkSongLoaded, writeInterfaceComponentData, sendClanOperation, sendLobbyHyperlink, sendJoinFriendchat, sendInterfaceOnPlayer, sendAccountCreationStage)
  - StaticMethods: 36 fields + 40 methods (menu rendering, login state, camera, songs, etc.)
  - ArrayUtils: 14 drawing methods (drawFilledCircle/Ellipse, drawStrokedRectangle variants, clamp, etc.)
  - Parameter: 6 fields identified (BASE64_URL_DATA, JS5_AUTH_TOKEN, SERVER_ENVIRONMENT, MACHINE_UID, MAP_OWNER_IDENTIFIER, AFFILIATE_P)
  - FileOnDisk: 7 fields + 1 method (fileChannelPosition, writeBufferFilePosition, readBufferFilePosition, readBuffer, writeBuffer, readBufferValidBytes, obfuscatedWriteBufferSize, flushWriteBuffer)
  - WorldMapElement: 7 fields + 4 methods (elementId, displayName, defaultZoomLevel, boundsMinX/MaxX/MinY/MaxY, mapToGameCoords, gameToMapCoords, gameToMapCoordsForPlane, recalculateBounds)
  - WorldMapPlacement: 4 fields + 2 methods (mapMinX/MinY/MaxX/MaxY, withinMapBounds, mapToGameCoords)
  - WorldMapRasterManager: 5 fields + 7 methods (edgeBufferSize, edgeBuffer, scanEdgeIndex, nextEdgeIndex, activeEdgeStart, scanlineStartX/EndX/Y, fillPolygon, fillPolygonMasked, rasterizePolygon, resetEdgeBuffer, buildEdgesFromPolygon, sortEdgesByY, sortActiveEdgesByX, initializeScanline, advanceScanline)
  - PostProcessEffect: aBool7506->isActive, method12129->isActive()
  - RectangularTextureGL: aClass137Sub110038->bloomTextureA, aClass137Sub110034->bloomTextureB
- **Phase 5A Compression/Buffer/Water/Sprite/Matrix (COMPLETE)**: ~150 renames across 20+ files
  - Bzip2BlockEntry: 16 fields (runCount, currentByte, blockPosition, lastByte, bwtPointer, blockSize, outputPosition, bitBufferLength, bitBuffer, bytesRead, blockCrc, blockSizeMultiplier, origPtr, symbolCount, cftab, mtfmBase)
  - Bzip2Compressor: 7 methods + 1 field (readByte, readBit, readBits, buildHuffmanTable, decodeBlock, buildSymbolMap, transformTable)
  - NativeBuffer: 2 private fields + 6 methods (size, address, putBytes, getBufferSize, getCapacity, getAddress, getPointer, getBaseAddress, getNativeAddress)
  - MapBuffer: isMapped
  - Stream: 2 fields + 18 methods (targetBuffer, bufferOffset, setTarget, setOffset, putIntLittleEndian, putIntMedium, flush, etc.)
  - WaterTextureGenerator: 7 fields + 5 methods (seed, permutationTable, scaleX/Y/Z, octaveCount, octaveWeights, generateNoise, initOctaveWeights, gradientDot, initPermutationTable, computeSCurve)
  - BillowNoiseGenerator: 4 fields + 1 method (outputBuffer, writePosition, accumulator, amplitudeWeights, writeSample)
  - WaveNoiseGenerator: outputBuffer
  - GLWaveTextureData: textureData, getData
  - NativeWaveTextureData: textureData
  - NoiseGenerator/PerlinNoiseGenerator: generateNoise3D
  - NativeWaterTexture: waveTextureDataBuffer, billowTextureDataBuffer
  - TextureContainerGL: waveTextureDataBuffer, billowTextureDataBuffer, billowTextures2d, billowTexture3d, initializeTextureData
  - ColorImageCache: 7 fields + 1 method (maxCachedRows, totalRows, rgbData, cacheEntries, lruList, cachedRowCount, lastAccessedRow, SENTINEL_ENTRY, getAllPalettes)
  - ImageProducerGL2: 7 fields + 1 method (isDisposed, openGlToolkit, canvas, openGL, surfaceHandle, displayHeight, displayWidth, updateDimensions)
  - SpriteSM: 6 instance fields + 25 companion statics + 4 methods (offsetX/Y, trimRight/Bottom, imageWidth/Height, viewportStride, pixelOffset, dudx/dvdx/dudy/dvdy, subpixelOffsetX/Y, uBase/vBase, uOffsetX/vOffsetX, drawMinX/Width/MinY/Height, blendColor, colorAlpha/InverseAlpha/Red/Green/Blue, overlayAlpha/InverseAlpha/Precomputed, drawAffineTransformed, drawParticle, computeAffineTransform, drawMasked)
  - Matrix44: faceList->elements (529 refs), 16 methods (determinant, setDiagonal, isIdentity, transformPoint, setOrtho, setPerspective, setPerspectiveFromViewport, getNearClipDistance, extractRotationInto, extractLeftPlane, extractRightPlane, extractTopPlane, extractNearPlane, extractFarPlane, extractBottomPlane, setRotatedTranslation, setProduct, transpose, getComponentsXY typo fix)
- **Phase 5B Entity/Scene/Cutscene/Sound (COMPLETE)**: ~100 renames across 26 files (see previous memory entries)
- **LoginManager + AccountCreationResponseOpcodes (COMPLETE)**: login-manager.md
  - AccountCreationResponseOpcodes: 8 enum constants + 1 method (NONE, AWAITING_RESPONSE, NOT_STARTED, CONNECTION_ERROR, CONNECTION_FAILED, NAME_UNAVAILABLE, ACCOUNT_CREATION_DISABLED, ALREADY_LOGGED_IN, getReceivedResponse)
  - LoginStage: 20 enum constants (IDLE, OPENING_CONNECTION, AWAITING_INIT_RESPONSE, etc.)
  - LoginManager: 13 fields + 16 methods (connectionTickCount, connectionRetryCount, loginQueuePosition, loginResponseDetail, loginResponseExtraInfo, socialNetworkToken, socialNetworkNonce, lobbyCreditCardExpiry, membershipTimeCorrection, isDobRequested, lobbyPlayAge, loginResponseCode, loginPayloadSize; isLoginInProgress, initializeLogin, setLoginResponse, resetLoginState, resetCredentials, confirmLoginContinue, startLobbyLogin, startWorldLogin, cancelLogin, onReconnect, etc.)
- **MiniMenu/MiniMenuEntry/MenuInteraction/MiniMenuBuilder (COMPLETE)**: ~55 renames across 17 files
  - MiniMenu.kt: 25 fields + 19 methods (entryList, coloredEntryMap, sortedColoredEntries, coloredEntryCache, menuWidth/Height, submenuX/Y/Width, entryRowHeight, isCollapsed, coloredEntryCount, maxVisibleEntries, topEntry, activeEntry, secondaryEntry, deferredEntry, activeSubmenu, deferredClickType, viewportX/Y, menuTextColor, menuHoverHighlightColor, lowPriorityEntries, interfaceEntries; render, renderOpenMenu/Simple/Styled, drawMenuEntry, renderTooltipText, openSubmenu, closeSubmenu, removeEntry, removeMatchingEntries, insertSortedEntry, hasHigherPriority, isInterfaceClickType, getActionText, getColoredNameText, getSecondaryEntry, isTopEntryInterfaceAction, setViewportOrigin, raycastTerrainIntersection)
  - MiniMenuEntry.kt: 6 fields (param0, param1, isSelectable, showOtherLevelIcon, isUncategorized, targetName)
  - MenuInteraction.kt: 1 field + 1 method (lastHoveredComponent, processMenuInput)
  - Linter bonus renames: InteractionDefaultsType (3 fields: primaryClickSubscriber, secondaryClickSubscriber, contextMenuSubscriber), StaticMethods (unprojectionBuffer, storedViewProjectionMatrix, pendingMusicTrack, pendingSoundBankCache), NodeCollection (spliceInto, transferAllTo)

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
- **Linter applies renames from sound-package.md**: The linter reads agent memory files (e.g. sound-package.md) and applies method renames (method15095->setChannelVolume etc.) that haven't actually been applied to the declaration file. Always verify linter edits with grep before building.
- **Use sed -i for ALL renames**: The Write tool triggers the linter which can revert ALL changes. Always use sed -i via Bash for field/method renames, never the Write tool for files with many renames.
- **Pre-existing build errors**: AreaSoundStream, AudioBufferStream, MixedAudioStream, MusicPatchPcmStream have pre-existing errors from previous rename sessions where MusicPatchNode fields and AbstractAudioFile methods were renamed but not all cross-references were updated.
- **Pre-existing GL constant errors**: After "severe refactor of rendering" commit (993ebe91), many GL constants (GL_TEXTURE1, GL_VIEWPORT_BIT, GL_QUADS, GL_MODULATE, etc.) are unresolved in FramebufferCompositor.kt, RectangularTextureGL.kt, OpenGlToolkit.kt. These are NOT caused by refactoring agent renames. Also Bot.kt/MenuInteraction.kt have unresolved hashFromInterface/componentIdFromHash/interfaceIdFromHash.
- **Constructor param name conflicts in Kotlin**: When renaming @JvmField val fields, the constructor params cannot share the same name. Rename constructor params differently (e.g., `toolkit` for param, `openGlToolkit` for field) and use `this.` prefix for assignment.
- **Incremental build cache can mask pre-existing errors**: First `./gradlew build` can succeed due to cache. Use `./gradlew clean build` if you see errors that seem unrelated to your changes.
- **Matrix44.scale() shares name with Model.scale()**: Cannot rename with sed without distinguishing types. Leave alone or use file-specific targeting.

- **Parameter.kt (COMPLETE)**: 10 parameter constants renamed (USE_BROWSER_APPLET, CUSTOM_STRING_PARAM, SUPPLEMENTARY_LOGIN_DATA, LOGIN_TOKEN, SIGNED_TERMS_AGREEMENT, SUPPRESS_TUTORIALS, SESSION_TOKEN, ACCOUNT_CREATED_FLAG, BILLING_FLAG, LOGIN_TYPE)
- **Client.kt Phase 2B (COMPLETE)**: 14 remaining fields renamed (useBrowserAppletMode, customStringParam, supplementaryLoginData, loginToken, hasSignedTermsAgreement, suppressTutorials, sessionToken, hasJustCreatedAccount, billingFlag, loginType, hasDisplayNamePending, hasAccountSecurity, displayNameChangeState, hideNpcs)
- **HookRequest.kt**: 2 fields renamed (targetComponent from aClass1188057, clickedOption from anInt8051)
- **VorbisNode.kt**: 2 methods renamed (processVorbisStreams, cleanupStaleStreams)
- **LoginManager.kt Phase 2B**: 4 methods + 2 fields renamed (isLoginInProgress, startWorldLogin, startLobbyLogin, resetLoginState, loginResponseDetail, loginResponseExtraInfo)
- **SongPlayer.kt**: resetChannelVolumes (from method12658)
- **CutsceneBasicMovement.kt**: musicFadeStep (from anInt9440)
- **CS2Interpreter.kt**: sendDisplayNameChangeRequest (from sendAClass3794624)
