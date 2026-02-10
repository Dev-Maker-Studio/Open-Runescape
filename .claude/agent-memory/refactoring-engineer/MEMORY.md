# Refactoring Engineer Memory

## Confirmed Class Identifications

- **Class111 = ShaderVariableType** (`graphics/shader/`) -- Enum-like, 125 singletons
- **Class455_Sub1 = MaskedTextureSurface** (`toolkit/`) -- ComponentSurface for masked textures
- **Class170 = FramebufferCompositor** (`graphics/`) -- FBO manager for post-processing
- **Node_Sub5 = PostProcessEffect** (`graphics/texture/`) -- Abstract post-processing
- **NativeBloomRenderer** (`graphics/`) -- Bloom ping-pong FBO renderer
- **Node_Sub26 = AudioNode** -- Abstract audio source for PCM
- **Node_Sub15_Sub1 = VorbisPcmStream**, **Node_Sub15_Sub4 = PcmStreamScheduler**
- **Node_Sub22 = ScheduledAudioEvent**, **Node_Sub41 = OggStreamDecoder**
- **Node_Sub41_Sub1 = NullOggStreamDecoder**, **Node_Sub41_Sub2 = OggKateDecoder**
- **Class149 = TextureShapeRasterizer** (Sub1=Ellipse, Sub2=Bezier, Sub3=Line, Sub4=Rectangle) -- RENAMED
- **Class350_Sub1 = SpriteLoadingBarElement**, **Sub2 = ColorLoadingBarElement**, **Sub3 = MultiSpriteLoadingBarElement** -- RENAMED
- **Class350_Sub3_Sub1 = AnimatedLoadingBarElement** -- RENAMED
- **Class43 = LoginBackgroundSprite**, **Class52_Sub1 = SpriteLoadingBar**, **Class52_Sub3 = SolidLoadingBar** -- RENAMED
- **Class370_Sub1 = WaveNoiseGenerator**, **Sub1_Sub1 = NativeWaveTextureData**, **Sub1_Sub2 = GLWaveTextureData** -- RENAMED
- **Class370_Sub2 = BillowNoiseGenerator**, **Sub2_Sub1 = NativeBillowTextureData**, **Sub2_Sub2 = GLBillowTextureData** -- RENAMED

## CS2Interpreter.kt Deobfuscation (NEAR-COMPLETE)

### Summary: ~337 obfuscated refs -> 3 remaining (2 unique fields)
- **CS2ReturnValue**: 5/5 fields renamed (script, savedInstrPtr, intLocals, objectLocals, longLocals)
- **CS2Executor**: 7/7 fields renamed (isIfTargetMode, entityIndex, executionStepCount, debugScriptFilter, debugTraceEnabled, worldMapResultBuffer, uniqueIdCounter)
- **CS2Interpreter**: 2/2 own fields renamed (MALE_BODY_PART_SLOTS, FEMALE_BODY_PART_SLOTS)
- **Component event hooks**: 19 anObjectArray fields -> RS event names (onClickRepeat, onVarpTransmit, onStatTransmit, onTimer, onDrag, onDragComplete, onTargetEnter, onTargetLeave, onChatTransmit, onFriendTransmit, onFriendsChatTransmit, onClanSettingsTransmit, onClanChannelTransmit, onClanVarTransmit, onStockTransmit, onMiscTransmit, onKeyType, onCamFinished, onResize)
- **Component tick trackers**: 5 fields (lastServerTick, lastVarpTransmitCycle, lastContainerTransmitCycle, lastStatTransmitCycle, lastClanVarTransmitCycle)
- **Component other**: mediaId, clanVarFilter, dragDeadzone, dragDelay, lineSpacing, keyCharBindings, skillUpdateFilter
- **Client fields**: hasDisplayName, displayNameRequestStatus, isAccountCreationPending, mapOwnerIdentifier, lobbyMembershipStatus, fromBilling
- **LoginManager fields**: lobbyCreditCardExpiry, lobbyAccountPlayAge, lobbyDobRequested
- **StaticMethods fields**: lobbyLoginResponseCode, worldLoginResponseCode, lastWorldLoginResponseCode, lastLoginResponseDetail, lastLoginResponseExtraInfo, serverTimeSyncValue, cameraTargetAbsoluteZ, visibleMapWidth
- **Menu chrome**: menuBodyTransparencyOffset, menuEntryShadowColor, menuFontId, menuHeaderTileSpriteId, scrollbarCornerSpriteId, scrollbarBottomBorderSpriteId, scrollbarSideBorderSpriteId
- **Sprite fields**: scrollbarBottomBorderSprite, scrollbarRightCornerSprite, scrollbarLeftCornerSprite, menuHeaderRightSprite, menuHeaderLeftSprite, menuHeaderTileSprite, scrollbarRightBorderSprite, scrollbarLeftBorderSprite
- **WorldMap**: viewCenterX/Y, isWorldMapInfoHidden, hiddenMecTypeIds, hiddenCategoryIds, elementId, displayName, boundsMinX/MaxX/MinY/MaxY, zoomLevel
- **Other**: cameraTargetAbsoluteX, lobbyGraceExpiry, visibleMapHeight, MECType.category, WorldMapCategory.plane, QuickChatMessage.paramValues, AccountCreationStage.currentRequest, CreateAccountRequest.responseCode/NOT_STARTED/AWAITING_RESPONSE, menuBackgroundColor

### Remaining (2 fields, insufficient context)
- `Client.aBool7158` (param 2) -- used as isApplet param in Browser API, but conflicts with existing `Client.isApplet` (param 9)
- `Client.aString7165` (param 3) -- unknown string parameter, only read in one CS2 opcode

## Key Deobfuscation Pattern: CS2 Opcode Names

ClientScriptOpCode enum names directly reveal field purposes:
- `USERDETAIL_LOBBY_CCEXPIRY` -> lobbyCreditCardExpiry
- `USERDETAIL_LOBBY_PLAYAGE` -> lobbyAccountPlayAge
- `USERDETAIL_LOBBY_DOBREQUESTED` -> lobbyDobRequested
- `MEC_CATEGORY` -> category, `MAP_ISOWNER` -> mapOwnerIdentifier
- `FROMBILLING` -> fromBilling

## Rosetta Stone Mappings

| Pattern | Type |
|---------|------|
| `aClass328NNNN` | NativeBloomRenderer |
| `aClass170NNNN` | FramebufferCompositor |
| `aClass149NNNN` | TextureShapeRasterizer |
| `aClass111NNNN` | ShaderVariableType |
| `aClass166NNNN` | ModelNormalsGL |
| `aNodeSub15Sub1NNNN` | VorbisPcmStream |
| `aNodeSub15Sub4NNNN` | PcmStreamScheduler |
| `aClass384NNNN` | Matrix44 |
| `aClass294NNNN` | Matrix43 |

## Previously Completed (see separate .md files for details)

- **Client/Camera/MiniMenu/LoginManager/PathingEntity/PlayerEntity/NpcEntity**: see client-kt.md, login-manager.md, pathing-entity.md
- **AbstractNativeToolkit/OpenGlToolkit**: FULLY DEOBFUSCATED (46+ fields, 34 methods; see abstract-native-toolkit.md)
- **ModelGL/Sound/Graphics**: see model-gl.md, sound-package.md; LightGL/InterfaceRenderer/FrustumCulling/SpriteGL/NativeSprite done
- **Shader system**: ShaderAttribute, ShaderVariableDecoder, ShaderMappingGL, AbstractWaterShader, NativeWaterShader, AbstractBloomShader, NativeBloomRenderer, Shader, ModelFunctionMaskGL
- **Scene/World**: GameState, Matrix44/43, Skybox, GLFormat/GLActionFormat/TextureStageState, FramebufferCompositor
- **Water/Particles**: NativeEvtWaterMapper, NativeWaterfall, AbstractParticleShader, NativeParticleShaderLd
- **30+ Rosetta Stone field renames**: Matrix44, Matrix43, GLFormat, GLActionFormat, Component, FontMetrics, etc.

## Recent Sessions (condensed)

### Shadow/WorldMap/UI/Fonts Session (~75 methods + 30 fields)
- ShadowMapperGL, NodeCollection, WorldMap, WorldMapRasterManager, WorldMapElement, WorldMapPlacement, WorldMapSub1
- ColorUtil, HitSplatType, ShadowTileGL, TextUtils, FontMetrics, GameState, AccountCreationStage, QuickChatMessageType
- Interface.kt, StaticMethods.kt (31 methods + 17 fields), ArrayUtils.kt (14 methods), Utils.kt (5), PacketEncoder.kt (10)

### Method Cleanup Pass (~120 methods, reduced to 3 remaining stubs)
- **JagTheora**: 13 JagTheoraManager + 4 JagTheoraVorbisNode + 8 Node_Sub15_Sub1 + 3 Node_Sub41_Sub2 + 2 Node_Sub22 + misc
- **Fonts**: 3 AbstractFontRenderer + 4 FontRendererSM + 2 OpaqueFontRendererSM + 2 TransparentFontRendererSM
- **Sprites**: 4 SpriteSM abstract (setupTransform/drawTransformedPixels/drawPointSprite/drawTransformedMaskedPixels) + overrides
- **Scene/UI/Map**: Scene.calculateRoofVisibility, InterfaceComponentRenderer (drawMinimapLabel/setTooltipComponent), WorldMapCategory.findNearestCategory
- **Textures**: OpenGl2DTexture_Sub1 (createFromPixels/createEmpty/createFromFormat), OpenGlTexture.uploadMipmapped, MipmapTextureSampler2D.uploadSubImage
- **SafeMode**: 5 methods (drawDashedHorizontal/VerticalLine, drawParticleShape, updateFrustumPlanes, createImageProducer)
- **Water Shader**: resetModelMatrix, activatePass, resetModelMatrixForPass
- **Remaining unrenameable**: 3 Node_Sub22 stubs (method12356/12357/12358), 4 IcmpService JNI callbacks

## Lessons Learned

- **replace_all is dangerous**: `Class111` also matched `aClass1111109`. Check substrings.
- **Linter silently reverts**: Use `sed -i` via Bash instead of Edit tool for renames.
- **Build between every class rename**: Class renames touch many files; verify immediately.
- **Pre-existing build errors exist**: Filter with `grep "^e:"` and check only your files.
- **getAll() array index != id value**: Binary format uses array index, not the id field.
- **PathingEntity partial renames**: Always grep for ALL references before considering done.
- **Stale TAR artifacts**: Use `./gradlew clean build` to resolve incremental build issues.
- **cameraCalculationType state machine**: 1=smooth transition, 2=follow player, 3=direct cutscene, 4=orbit target, 5=scripted move/look
- **CS2 opcode names are strongest evidence**: Named opcodes in ClientScriptOpCode enum directly reveal field purposes
- **Component event hook decode order**: Fixed binary format order maps to RS event names (see cs2-interpreter.md)
- **Parent class field name clashes**: When renaming fields in OpenGlToolkit (parent), ModernGLToolkit (child) may have private fields with same names. Prefix parent fields with "gl" to avoid.
- **Override method rename collision**: When a file has both `override fun foo()` and `fun method1234()`, renaming method1234 to foo causes conflict. Use different name (e.g., applyViewportSettings).
- **Inherited field rename propagation**: OpenGlTexture fields propagate to subclasses (OpenGl1DTexture, OpenGl2DTexture, OpenGl3DTexture, OpenGlArbCubeMapTexture). Always grep for subclass references.
- **ShaderAttribute init block reads from decoder**: Renaming decoder fields requires also updating the ShaderAttribute init block references.
- **grep ALL references before sed**: Always do a full grep for the method/field before using sed. Missing one file (e.g., missed `method822` in AbstractNativeToolkit) causes build failure.
- **Class111 aClass1111109 = ARRAY type**: In ShaderVariableType, id 109 indicates an array uniform; decoder then reads elementType + arraySize.
- **Linter aggressively reverts renames**: The project linter reverts Edit tool changes AND recreates deleted files. ALWAYS use `sed -i` via Bash for renames, delete via `rm -f`, and build immediately after. Restore files from `git checkout HEAD --` before applying only YOUR changes to avoid compounding linter damage.
- **Match HEAD field/method names exactly**: When creating new renamed files, always check `git show HEAD:path` to get the EXACT field/method names from the committed version, not the linter-modified version that may have been partially renamed by a previous agent.
- **Read tool cache is STALE**: Read tool may return cached versions from previous agents that had deobfuscated names. ALWAYS verify actual file content via `cat` in Bash before writing new files. Use `cp` + `sed` instead of Write for class renames.
- **Use ABSOLUTE paths with sed**: `sed -i` with relative paths silently fails when the Bash working directory is unexpected. Always use absolute paths.
- **Pre-existing build errors**: The codebase may have 10-50 pre-existing errors from incomplete previous renames. Capture error list at start and diff against it to verify no new errors added.
- **Substring matching for class hierarchies**: When renaming X_Sub1_Sub2, X_Sub1, X_Sub2, X -- rename LONGEST names first to avoid partial matches. Use `\b` word boundaries in sed for safety.
- **Class149_Sub1 is Ellipse not Rectangle**: Previous agent labels were SWAPPED. Sub1=Ellipse (rasterEllipse), Sub3=Line (drawLineClipped). Always verify code behavior, not just previous agent claims.

## Completed Class Renames (previous session, BUILD VERIFIED)

All 8 Node_Sub class renames completed with clean builds:
1. `Node_Sub5` -> `PostProcessEffect` (graphics/texture/)
2. `Node_Sub26` -> `AudioNode` (cache/node/impl/)
3. `Node_Sub22` -> `ScheduledAudioEvent` (sound/jagtheora/)
4. `Node_Sub41` -> `OggStreamDecoder` (sound/jagtheora/)
5. `Node_Sub41_Sub1` -> `NullOggStreamDecoder` (sound/jagtheora/)
6. `Node_Sub41_Sub2` -> `OggKateDecoder` (sound/jagtheora/)
7. `Node_Sub15_Sub1` -> `VorbisPcmStream` (sound/jagtheora/)
8. `Node_Sub15_Sub4` -> `PcmStreamScheduler` (sound/jagtheora/)

## Completed Unidentified Package Migration (BUILD VERIFIED, clean build passes)

All 6 files moved OUT of `unidentified/` package. Directory deleted.
1. `Class111.kt` -> `ShaderVariableType.kt` in `graphics/shader/` (class renamed + field `anInt1232` -> `id`)
2. `Class170.kt` -> `FramebufferCompositor.kt` in `graphics/` (class renamed)
3. `Class455_Sub1.kt` -> `MaskedTextureSurface.kt` in `toolkit/` (class renamed)
4. `NativeBloomRenderer.kt` -> `graphics/NativeBloomRenderer.kt` (package move only)
5. `ComponentDimensionSM.kt` -> `toolkit/ComponentDimensionSM.kt` (package move only)
6. `SpriteSurfaceGL.kt` -> `toolkit/SpriteSurfaceGL.kt` (package move only)

### Key linter handling lessons from this migration:
- **Linter restores `rm`'d files**: Must use `git rm -f` instead of `rm` to delete tracked files
- **Linter reverts `sed -i` and `Edit` tool changes**: Must use `Write` tool to completely overwrite files
- **Strategy that works**: (1) Create new file with Write/cp+sed (2) Update consumers with Edit (3) `git rm -f` old file (4) Build immediately
- **Wildcard imports**: When moving a class to same package, remove the wildcard import entirely
- **Same-package imports are unnecessary**: Kotlin doesn't need imports for same-package classes

## Completed Class Renames (previous session, BUILD VERIFIED, 0 errors)

Group 1 - Texture shape rasterizers (config/material/texture/operation/):
1. `Class149` -> `TextureShapeRasterizer` (abstract base)
2. `Class149_Sub1` -> `EllipseRasterizer` (NOT Rectangle -- rasterEllipse)
3. `Class149_Sub2` -> `BezierCurveRasterizer` (drawBezierCurve)
4. `Class149_Sub3` -> `LineRasterizer` (drawLineClipped only)
5. `Class149_Sub4` -> `RectangleRasterizer` (rasterRectangle*)
Also: `aClass149Array9862` -> `shapes` in TextureOpRasterizer

Group 2 - Loading bar elements (config/gametip/component/):
6. `Class350_Sub1` -> `SpriteLoadingBarElement`
7. `Class350_Sub2` -> `ColorLoadingBarElement`
8. `Class350_Sub3` -> `MultiSpriteLoadingBarElement`
9. `Class350_Sub3_Sub1` -> `AnimatedLoadingBarElement`

Group 3 - Water noise generators (graphics/texture/water/):
10. `Class370_Sub1` -> `WaveNoiseGenerator`
11. `Class370_Sub1_Sub1` -> `NativeWaveTextureData`
12. `Class370_Sub1_Sub2` -> `GLWaveTextureData`
13. `Class370_Sub2` -> `BillowNoiseGenerator`
14. `Class370_Sub2_Sub1` -> `NativeBillowTextureData`
15. `Class370_Sub2_Sub2` -> `GLBillowTextureData`

Group 4 - Login screen components (graphics/loginscreen/component/):
16. `Class43` -> `LoginBackgroundSprite`
17. `Class52_Sub1` -> `SpriteLoadingBar`
18. `Class52_Sub3` -> `SolidLoadingBar`

TipRenderDetails enum constants:
- `aClass60601` -> `TIPS_SPRITE`
- `aClass60602` -> `COLOR_LOADING_BAR`
- `aClass60603` -> `SPRITE_LOADING_BAR`
- `aClass60608` -> `MULTI_SPRITE_LOADING_BAR`
- `aClass60610` -> `ANIMATED_LOADING_BAR`

Decoder functions (AnimatedLoadingBarElement.kt):
- `decodeSub1` -> `decodeSpriteLoadingBar`
- `decodeSub2` -> `decodeColorLoadingBar`
- `decodeSub3` -> `decodeMultiSpriteLoadingBar`
- `decodeSub3Sub1` -> `decodeAnimatedLoadingBar`

## CreateAccountRequest + AccountCreationStage (BUILD VERIFIED, 0 errors)

### CreateAccountRequest.kt (1 field + 1 method + 14 enum constants renamed)
Field: `anInt5793`->responseCode. Method: `method8038`->getAll.
Enum constants: NOT_STARTED(-2), AWAITING_RESPONSE(-3), SUCCESS(2), UNKNOWN_ERROR(3), USERNAME_UNAVAILABLE(9), INVALID_USERNAME(10), INVALID_PASSWORD(20), PASSWORD_TOO_SIMILAR(21), INVALID_DOB(30), EMAIL_ALREADY_IN_USE(31), INVALID_EMAIL(32), TERMS_NOT_ACCEPTED(33), TOO_MANY_ATTEMPTS(34), SERVER_UNAVAILABLE(38).

### AccountCreationStage.kt (2 fields + 3 methods renamed)
Fields: `anInt134`->connectionRetryCount, `anInt133`->connectionTickCount, `aClass4942916`->currentRequest.
Methods: `method4251`->initAccountCreation, `method297`->retryOrFailConnection, `method1466`->isCreatingAccount, `method12948`->setCurrentRequest.

Consumer files updated: PacketDecoder.kt, PacketEncoder.kt, CS2Interpreter.kt, Client.kt, GameState.kt, LoginManager.kt.

## Client.kt Field + Method Deobfuscation (BUILD VERIFIED, 0 errors)

### Methods (13 renamed, 0 remaining in Client.kt)
processLobbyTick, processGameTick, drawInterfaceFrame, drawGameFrame, processDrag, resetGraphicsState, getDragTarget, clearWorldMapCaches, applyCameraYawRight, buildOverlappingEntityList, resetGameState, isInLobbyOrCreation, isInLobbyConnection

### Fields (68 renamed, 14 uncertain left obfuscated)
- Tick/Timestamp (8): serverTick, lastChatMessageTick, lastFriendIgnoreUpdateTick, lastFriendsChatUpdateTick, lastClanSettingsUpdateTick, lastClanChannelUpdateTick, lastGrandExchangeUpdateTick, lastMiscUpdateTick
- Camera (8): cameraYawVelocity, cameraPitchVelocity, cameraYawVelocityApplied, cameraPitchVelocityApplied, cameraTargetAbsX, cameraTargetAbsY, cameraRandomizationCycle, lastCameraSmoothCycle
- Cutscene Spline (13): cameraSplinePaths, cameraPositionPathIndex, cameraLookAtPathIndex, cameraPositionKeyframe, cameraLookAtKeyframe, cameraSplineProgress, cameraSplineSpeedStart, cameraSplineSpeedEnd, cameraBaseZoom, cameraMaxZoom, cutsceneCameraActive, cutsceneCameraCompleted, cutsceneCameraPreviouslyCompleted
- Viewport/FOV (9): viewportFovMin, viewportFovMax, cameraZoomMin, cameraZoomMax, fovScaleMin, fovScaleMax, aspectFovMin, aspectFovMax, viewportScaledFov
- Minimap (3): minimapRandomizationCycle, minimapRotationStep, minimapZoomStep
- Input/UI (7): mouseWheelDelta, isDragThresholdMet, menuItemHoverCycle, menuOpenMode, selectedComponentHash, selectedSlotId, selectedItemId
- Rendering (7): lastRedrawTimeMillis, componentBackgroundCount, lastRenderedCycle, drawsSinceLastPacket, interfaceDebugMode, overlappingEntityCount, overheadChatFilter
- Chat/Skills (4): chatMessageHashIndex, skillUpdateCycle, recentSkillUpdates, recentKeyUpdates
- Camera Shake (3): CAM_SHAKE_JITTER, CAM_SHAKE_FREQUENCY, CAM_SHAKE_PHASE
- Constants/Other (6): DEBUG_TEXT_COLOR, OVERHEAD_TEXT_COLORS, ANTI_MACRO_TEXT_BOUNDS, PLAYER_OP_BASE_TYPES, clientRandomSeed, activeDialogueContinue, TICK_HOOK_REQUESTS, MOUSE_HOOK_REQUESTS
- Identity (4): js5AuthToken, machineUid, lobbyMembershipStatus, mapOwnerIdentifier

### 14 Remaining Uncertain Fields (deliberately left obfuscated)
aBool7151 (param29), aBool7158 (param2), aBool7159 (param25), aString7165 (param3), aBool7189 (param28), aBool7323 (unused login bool), aByte7458 (deprecated packet), aString7156 (param4), aBool7160 (param22), anInt7163 (param27), aBool7241 (hide NPCs?), aBool7322 (post-email flag), aString5555 (param13), anInt2831 (param30)

### Cross-file impact: 37 files modified across codebase

## Camera/Mouse/Console Deobfuscation (BUILD VERIFIED, 0 new errors)

### Camera.kt (11 fields + 6 methods renamed)
Fields: `aClass3847167`->projectionMatrix, `anInt9461/2349/9575`->transitionStartZ/X/Y, `anInt3968`->transitionStartYaw, `anInt5029`->transitionStartRoll, `aClass384158`->viewProjectionMatrix, `aClass294155`->sunMatrix, `anInt176/184`->gameAreaWidth/Height, `anInt1527`->lookAtHeightOffset
Methods: `method6286`->updateFollowPlayerCamera, `method4974`->updateOrbitTargetCamera, `method6934`->storePickingState, `method3920`->updateSmoothTransitionCamera, `method13296`->storeViewProjectionMatrix, `method12606`->startSmoothTransition

### MouseManager.kt (4 fields + 1 method renamed)
`aClass4827941`->pendingMouseActions, `anInt7938/7939`->snapshotMouseX/Y, `anInt7940`->snapshotClickMask, `method3589`->syncMouseState (also in AbstractMouseManager)

### Console.kt (4 fields renamed)
`anInt2230`->inputLineHeight, `anInt2222`->outputLineHeight, `anInt2223`->scrollOffset, `anInt2226`->historyIndex
