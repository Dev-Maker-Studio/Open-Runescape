# Refactoring Engineer Memory

## Topic Files (detailed notes)
- **client-kt.md**: Client.kt field/method renames (68 fields, 13 methods, 14 Phase 2B)
- **cs2-interpreter.md**: CS2Interpreter deobfuscation (~337 refs)
- **abstract-native-toolkit.md**: Toolkit hierarchy (AbstractToolkit, AbstractNativeToolkit, OpenGlToolkit, OpenGLNativeToolkit, SafeModeToolkit)
- **model-gl.md**: ModelGL own identifiers (6 fields, 7 methods, ~200 locals)
- **shader-deobfuscation.md**: Shader system (ShaderAttribute, ShaderVariableDecoder, ShaderMappingGL, Shader.kt, AbstractEnvironment/Sprite/Particle/Bloom/WaterShader)
- **sound-package.md**: Sound/audio system (AreaSoundStream, MidiPcmStream, MusicPatchNode, etc.)
- **font-loading-bar.md**: Font system + loading bar
- **login-manager.md**: LoginManager, AccountCreationResponseOpcodes, LoginStage
- **pathing-entity.md**: PathingEntity, PlayerUpdate, PlayerEntity, BasType
- **final-cleanup-pass.md**: Final cleanup (~70 renames, 128 files, remaining unrenameable identifiers)

## Confirmed Class Identifications
- Class111=ShaderVariableType, Class455_Sub1=MaskedTextureSurface, Class170=FramebufferCompositor
- Node_Sub5=PostProcessEffect, Node_Sub26=AudioNode, Node_Sub22=ScheduledAudioEvent
- Node_Sub15_Sub1=VorbisPcmStream, Node_Sub15_Sub4=PcmStreamScheduler
- Node_Sub41=OggStreamDecoder (Sub1=Null, Sub2=OggKateDecoder)
- Class149=TextureShapeRasterizer (Sub1=Ellipse, Sub2=Bezier, Sub3=Line, Sub4=Rectangle)
- Class350 subs: Sub1=SpriteLoadingBarElement, Sub2=Color, Sub3=MultiSprite, Sub3_Sub1=Animated
- Class43=LoginBackgroundSprite, Class52_Sub1=SpriteLoadingBar, Class52_Sub3=SolidLoadingBar
- Class370 subs: Sub1=WaveNoiseGenerator, Sub2=BillowNoiseGenerator

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
| `aClass150NNNN` | GLFormat |
| `aClass76NNNN` | GLActionFormat |

## Key Deobfuscation Patterns
- **CS2 opcode names** directly reveal field purposes
- **drawIndexedPrimitive** params: `(drawMode, startVertex, vertexCount, startIndex, primitiveCount)`
- **Shader attribute names** map to GLSL uniforms (DiffuseSampler, WVPMatrix, etc.)
- **OpenGL spec** identifies glFramebufferTexture2DEXT 5th param as mipmapLevel
- **VarpType.clientCode** dispatches to specific Client fields (5=menuOpenMode, 6=overheadChatFilter)

## Completed Systems (ALL APPLIED + BUILD VERIFIED)
- Client/Camera/Mouse/Console (82 fields + 13 methods)
- CS2Interpreter (~337 references)
- Toolkit Hierarchy: AbstractToolkit, AbstractNativeToolkit, OpenGlToolkit, OpenGLNativeToolkit, SafeModeToolkit (ALL OWN identifiers COMPLETE)
- ModelGL + ModelFunctionMaskGL (ALL OWN identifiers COMPLETE)
- ModelSM + GroundSM + SafeMode Pipeline (RasterizerSM, ThreadResourceSM, TextureBlendedTile, ShapedTileBlended)
- Sound/Video Codec (OggStreamDecoder, OggKateDecoder, JagTheoraManager, SoundResourceManager, SongPlayer, etc.)
- Audio Streams (AreaSoundStream, AudioBufferStream, MixedAudioStream, VorbisPcmStream, PcmStream)
- MIDI/Music (MidiPcmStream, MusicPatchNode, MusicPatchNode2, MusicPatchPcmStream, MidiFileReader, MusicTrack)
- Shader system (8 abstract/concrete shader classes)
- GL Support (GLFormat, GLActionFormat, TextureStageState, VertexDeclarationAction, MatrixMode)
- OpenGlTexture hierarchy + Sprites (SpriteGL, NativeSprite, SpriteSM)
- Lighting (LightGL: 18 fields + 3 methods)
- Texture Operations (29 TextureOp files + 5 rasterizer files)
- Font System (TransparentFontRendererSM, FontMetrics, AbstractFontRenderer, FontRendererSM, etc.)
- Component/InterfaceRenderer/InterfaceComponentRenderer (35+3+6 fields, 7+1+2 methods)
- MiniMenu/MiniMenuEntry/MenuInteraction/MiniMenuBuilder (25+6+1 fields, 19+1 methods)
- LoginManager + AccountCreation (13 fields + 16 methods + 28 enum constants)
- Parameter.kt (10+6 constants), HookRequest.kt (2 fields)
- Entity/Scene: PathingEntity (4f+5m), PlayerUpdate (4f+5m), PlayerEntity (2b), BasType (13f), GroundItem (1f)
- CutsceneActionType (16 constants), CutsceneBasicMovement (1f)
- WorldMap/WorldMapSub1/WorldMapElement/WorldMapPlacement/WorldMapRasterManager
- StaticMethods (36f+40m), ArrayUtils (14m), PacketEncoder (11m), Interface (6m+1f)
- Compression (Bzip2BlockEntry 16f, Bzip2Compressor 7m), Buffer (NativeBuffer 6m, Stream 18m, MapBuffer)
- Water (WaterTextureGenerator 7f+5m, BillowNoiseGenerator 4f+1m, NativeWaterTexture, TextureContainerGL)
- Cache (ColorImageCache, MonochromeImageCache, HashTableIterator, FileOnDisk)
- Matrix44 (elements + 16 methods), ImageProducerGL2
- Phase 5B: Scene/Cutscene/Sound (~100 renames across 26 files)
- Phase 5C: Core Utils/Cache/Network/WorldMap (~200 renames across 30+ files)
- Final Cleanup: Framebuffer/Texture/Vorbis/Particle/Entity/MapLoader (~70 renames, 128 files)

## Remaining Unrenameable Identifiers (~35 total)
- **@JvmName** (13): NodeIterator, QueueIterator, SongReference, MaskedTextureSurface, PerlinWaterTextureGenerator, GameState
- **Native JNI** (8): IcmpService.kt/IcmpServiceJaclib.kt method11597-11600
- **Unreferenced** (8): Scene.kt 5 fields, WaterTile.anInt948, ParticleProducerType.aBool572, MaterialType.aBool2087
- **Comments-only** (3): Bot.kt method1150, Shader.kt anInt8820, CommandExecutor.kt anInt2826
- **Insufficient evidence** (3): FileOnDisk.anInt5357, LoginManager.anInt5357
- **Excluded by scope**: CS2Interpreter.java (809 opcode handlers), TransparentSpriteSM/OpaqueSpriteSM/PaletteSpriteSM
- **ShaderVariableType**: All 117 obfuscated constants renamed (14 MATRxC + 93 TYPE_XX placeholders). No external refs existed.

## Lessons Learned
- **replace_all is dangerous**: `Class111` matches `aClass1111109`. Use `\b` word boundaries.
- **Linter silently reverts**: Use `sed -i` via Bash, NOT Edit tool for renames.
- **Build between every class rename**: Class renames touch many files; verify immediately.
- **Pre-existing build errors exist**: Filter with `grep "^e:"` and check only your files.
- **grep ALL references before sed**: Missing one file causes build failure.
- **NEVER sed-replace f_N/i_N locals file-wide**: They recur in different scopes with different meanings.
- **Companion static fields may be cross-referenced from unrelated classes**: Always grep entire codebase.
- **Linter modifies ModelFunctionMaskGL.kt and ModelGL.kt**: Revert after builds.
- **Edit tool replace_all does substring match**: Verify composite identifiers after bulk replace.
- **Constructor param name conflicts in Kotlin**: @JvmField val fields can't share constructor param name.
- **Incremental build cache can mask pre-existing errors**: Use `./gradlew clean build` if needed.
- **CS2 opcode names are strongest evidence** for field naming.
- **For large methods with reused vars**: Write entire renamed method body, then sed delete+insert.
- **Pre-existing GL constant errors**: After rendering refactor commit, many GL constants unresolved. NOT caused by deobfuscation.
- **Linter applies renames from memory .md files**: Verify linter edits with grep before building.
