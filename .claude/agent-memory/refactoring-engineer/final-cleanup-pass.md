# Final Cleanup Pass - Deobfuscation Renames

## Status: COMPLETE (128 files changed, 1201 insertions, 1201 deletions)

## Batch 1: Framebuffer/Texture/Vorbis/Particle/Entity renames

| File | Old Name | New Name |
|------|----------|----------|
| FrameBufferTexture2D.kt | anInt3756 | mipmapLevel |
| FrameBufferRectangleTexture.kt | anInt3667 | mipmapLevel |
| FramebufferTexture2DEXT.kt | aClass137Sub11966 | texture2d |
| FramebufferTexture2DEXT.kt | anInt1967 | mipmapLevel |
| OpenGlAttachableCubeMapTexture.kt | aClass137Sub21656 | cubeMapTexture |
| OpenGlAttachableCubeMapTexture.kt | anInt1657 | cubeFaceTarget |
| OpenGlAttachableCubeMapTexture.kt | anInt1658 | mipmapLevel |
| MusicSampleCodebook.kt | anIntArray3151 | huffmanTree |
| MusicSampleCodebook.kt | method4382 | decodeVector |
| MusicSampleResidue.kt | method4382 | decodeVector |
| MusicSampleFloor.kt | method6035 | sortByX |
| ParticleBinaryTree.kt | method7734 | getNextCached |
| PathingEntitySequence.kt | anInt10065 | delayTicks |
| VarpType.kt | anInt4327 | clientCode |
| OpenGl2DTexture_Sub1.kt | method15537 | createFromPixels |
| OpenGl2DTexture_Sub1.kt | method15538 | createFromScreenCapture |
| OpenGl2DTexture_Sub1.kt | method15547 | createEmpty |
| MipmapTextureSampler2D.kt | method14494 | updateSubImageFloat |
| GroundMaterialGL.kt | method12083 | writeVertexColor |
| GroundMaterialGL.kt | method12085 | drawMaterialTriangles |
| MaterialEffect1GL.kt | aBool9025 | isLimitedTextureUnits |
| MaterialEffect1GL.kt | method14411 | initializeCombineLists |
| StillWaterEffect.kt | aBool9073 | isUsingFallbackTexture |
| ParticleProducerType.kt | anInt537 | emissionEndTime |
| ParticleProducerType.kt | aBool541 | killAboveSurface |

## Batch 2: Single-identifier files

| File | Old Name | New Name |
|------|----------|----------|
| ShortNode.kt | aShort8067 | value |
| AudioNode.kt | anInt7690 | activeVoiceCount |
| QuickChatMessage.kt | anIntArray4046 | dynamicValues |
| OpenGlRenderBuffer.kt | anInt10284 | bufferSizeBytes |
| MonochromeImageCacheEntry.kt | anInt7689 | paletteIndex |
| TextureRaster.kt | anInt3678 | scaledWidth |
| VertexDeclarationComponent.kt | method1299 | getSlotIndex |
| NativeParticleRenderer.kt | anInt504 | depthBucketBits |
| CS2Executor.kt | method1480 | generateUniqueId |
| CS2Interface.kt | method8772 | getComponentFromInterface |
| SongPlayer.kt | method3912 | clearPrimarySound |
| HintRenderer.kt | method3151 | drawMinimapHintArrows |
| SpriteType.kt | method1523 | expandToFullSize |
| NodeCollection.kt | method8076 | getFirstNode |
| PulseEventQueue.kt | method5581 | queueT17Event |
| GameShell.kt | method4658 | validateCacheDirectory |
| AbstractMousePacket.kt | aShort7079 | previousFovScaleMin |

## Batch 3: CS2ReturnValue + ScheduledAudioEvent + MapLoader + Scene

| File | Old Name | New Name |
|------|----------|----------|
| CS2ReturnValue.kt | anInt5866 | savedInstrPtr |
| CS2ReturnValue.kt | anIntArray5867 | savedIntLocals |
| ScheduledAudioEvent.kt | method12354 | processEvent |
| ScheduledAudioEvent.kt | method12355 | onRemove |
| MapLoader.kt | anIntArray3857 | shapeVertexA |
| MapLoader.kt | anIntArray3858 | shapeVertexB |
| MapLoader.kt | anIntArray3859 | shapeVertexC |
| MapLoader.kt | anIntArray3826 | OVERLAY_FACE_COUNTS |
| MapLoader.kt | anIntArray3847 | UNDERLAY_FACE_COUNTS |
| Scene.kt | method570 | updateTileVisibility |

## Remaining Identifiers (CANNOT RENAME)

### @JvmName annotations (already properly named in Kotlin, JVM name preserved)
- NodeIterator: method7683, method7684
- QueueIterator: method7790, method7800
- SongReference: method1966
- MaskedTextureSurface: method13769
- PerlinWaterTextureGenerator: method4782, method4783
- GameState: method12426, method6270, method6693

### Native JNI methods (MUST NOT rename)
- IcmpService.kt: method11597-11600
- IcmpServiceJaclib.kt: method11597-11600

### Unreferenced/unused fields (no usage = no evidence)
- Scene.kt: anInt2618-2643 (5 fields)
- WaterTile.kt: anInt948
- ParticleProducerType.kt: aBool572
- MaterialType.kt/MaterialTypeList.kt: aBool2087

### Only in comments/strings
- Bot.kt: method1150
- Shader.kt: anInt8820
- CommandExecutor.kt: anInt2826

### Insufficient evidence
- FileOnDisk.kt/LoginManager.kt: anInt5357
