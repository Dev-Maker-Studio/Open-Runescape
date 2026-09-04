# ModelGL.kt Deobfuscation -- COMPLETE

File: `graphics/model/ModelGL.kt` (~3,962 lines) -- Core 3D model renderer for legacy OpenGL pipeline.
**Status: ALL own identifiers deobfuscated (0 remaining obfuscated)**

## Fields Renamed (6)

| Old Name | New Name | Purpose |
|----------|----------|---------|
| `anInt8302` | `renderedFaceCount` | Count of non-billboard faces for indexed rendering |
| `aShortArray8288` | `vertexNormalIndices` | Maps vertex positions to expanded normal indices via `connectNormal()` |
| `anIntArray8289` | `materialBatchOffsets` | Start indices for material-based draw call groups |
| `aClass1668259` | `mergedNormals` | ModelNormalsGL storing pre-merge normals for adjacent model sharing |
| `anInterface158277` | `dynamicElementsBuffer` | Reusable ElementsBufferGL for pre-allocated copy slots |
| `anInterface148305` | `dynamicArrayBuffer` | Reusable ArrayBufferGL for pre-allocated copy slots |

## Methods Renamed (7)

| Old Name | New Name | Purpose |
|----------|----------|---------|
| `method13538` | `adjustLuminance` | Static: scales HSL luminance by factor, clamps to [2,126] |
| `method13522` | `projectShadow` | Projects vertices into shadow map space, marks shaded triangles |
| `method13524` | `copyModelData` | Core copy logic -- copies all model data with flag-controlled deep/shallow copy |
| `method13525` | `releaseUploadedData` | Releases CPU-side arrays after successful GPU upload |
| `method13534` | `drawOpaqueFaces` | Uploads buffers + draws indexed geometry with material batching |
| `method13535` | `drawBillboards` | Renders billboard effects as camera-facing quads |
| `method13536` | `buildElementsBuffer` | Constructs index buffer from face vertex indices |

## Parameters Renamed

- Constructor: `i_4` -> `ambient`, `i_5` -> `contrast`
- `adjustLuminance`: `i_0` -> `hsl`, `i_1` -> `luminanceFactor`
- `applyShading(Int)`: `i_1` -> `functionMask`
- `copy()`: `b_1` -> `copyType`, `bool3` -> `recalculateBeforeCopy`, locals `class528Sub15/16` -> `dst/src`
- `copyModelData`: `bool4` -> `usesPreallocated`
- `yaxisRotateWithoutNormals`: `i_1` -> `rotation`, locals `sin/cos/tmp`
- `rotateWithNormals`: `i_1` -> `rotation`, locals `sin/cos/tmp`
- `drawBillboards`: `matrix441` -> `modelViewMatrix`
- `buildElementsBuffer`: `bool1` -> `reuseBuffer`
- `isHovering`: `i_1-i_8` -> `mouseX, mouseY, yA, yB, yC, xA, xB, xC`

## Local Variable Renames (by method)

### Constructor (~50 locals)
- `b_67`->renderFlags, `i_13`->normalSlotOffset, `i_21`->deltaY, `i_22`->`i_25`->deltaZ1/deltaX2/deltaY2/deltaZ2
- `i_29`->normalMagnitude, `b_30`->faceRenderType
- RenderType 0 (planar UV): f_38-f_62 -> texOriginX/Y/Z, texEdge1X/Y/Z, texEdge2X/Y/Z, vertexAOffX/Y/Z, etc.
- RenderType 1 (cylindrical): f_40->textureSpeed, f_41->cylindricalScale, f_42->halfCylindricalScale
- RenderType 2 (rectangular): various -> edgeDX/DY/DZ, crossX/Y/Z, scaleFactorX/Y/Z, projectedNormalX/Y/Z, uTrans, vTrans
- Face groups: i_85->maxGroupIndex, ints_68->groupCounts, s_35/s_36/s_77->texVertexA/B/C, i_95->cubeFaceIndex

### connectNormal (5 locals)
- i_11->startIndex, i_12->endIndex, i_13->slotIndex, i_14->searchIndex, s_15->normalRef

### hslToRgb (6 locals)
- i_5->rgb, i_8->ambientRgb, i_10->tempVal, i_9->scaledR, i_11->scaledB

### copyModelData (3 locals)
- i_10->arraySize, i_11->normalCount, class16612->dstMergedNormals

### drawOpaqueFaces (3 locals)
- i_2->batchIndex, i_3->batchStart, i_4->batchEnd

### drawBillboards (7 locals)
- f_6->centerX, f_7->centerY, f_8->centerZ, f_9->eyeX, f_10->eyeY, f_11->eyeZ, f_12->distanceScale

### buildElementsBuffer (1 local)
- i_3->faceIndex

### upload (~20 locals)
- i_16->triStart, i_17->triEnd, i_18->triIdx, i_19->normalIdx
- shorts_34->normalsXArr, shorts_35->normalsYArr, shorts_36->normalsZArr, bytes_37->magnitudesArr
- f_38->sunX, f_39->sunY, f_40->sunZ, f_41->sunBrightness
- f_20->normalScale, f_21->antiSunScale
- i_22->faceIdx, i_23->rgbColor, f_24->redComponent, f_25->greenComponent, f_26->blueComponent
- s_27->vertexRef, s_28->normalMag, f_29->lightFactor, f_30->brightness
- i_31->red, i_32->green, i_33->blue

### projectShadow (4 locals)
- i_7->triStart, i_8->triEnd, i_9->triIdx, i_10->normalIdx

### yaxisRotateWithoutNormals (4 locals)
- i_2->sin, i_3->cos, i_4->v, i_5->tmp

### rotateWithNormals (4 locals)
- i_2->sin, i_3->cos, i_4->unusedVar, i_5->tmp

### calculateProjection (~30 locals)
- bool7->isInView, i_8->screenMinX, i_9->screenMaxX, i_10->screenMinY, i_11->screenMaxY
- i_15->centerX, i_16->centerY, i_17->centerZ
- i_18->bbMinX, i_19->bbMinY, i_20->bbMinZ, i_21->bbMaxX, i_22->bbMaxY, i_23->bbMaxZ
- Corner loop: i_24->cornerIdx, i_39->cornerX, i_26->cornerY, i_27->cornerZ
- f_28->clipZ, f_29->clipW, f_30->clipX, f_31->clipY, i_32->screenX, i_33->screenY
- Vertex loop: vertIdx, vx, vy, vz, clipZ/W/X/Y, triStart/triEnd, triIdx, normalIdx
- Face loop: faceIdx

### drawMatrixThreaded (~25 locals)
- floats_7->scaleBuffer, f_8->minWorldX, f_9->minWorldY, f_10->minWorldZ
- f_11->maxWorldX, f_12->maxWorldY, f_13->maxWorldZ
- i_14->planeIdx, floats_33->frustumPlane, f_16->distMin, f_17->distMax
- bool34->hasBehindFrustum, bool15->isVisible
- i_35->centerModelX, i_36->centerModelZ, s_19->modelY
- Clipping: clipMinX/Y/Z/W, clipMaxX/Y/Z/W, interpFactor, interpX/Y/W

### applyShading(model) (~25 locals)
- shorts_15->mergedNormalsX, shorts_16->mergedNormalsY, shorts_17->mergedNormalsZ, bytes_18->mergedMagnitudes
- shorts_19->otherNormalsX, etc., ints_23->otherTriangles, shorts_24->otherNormalIndices
- s_25-s_30->otherMinY/MaxY/MinX/MaxX/MinZ/MaxZ
- i_31->vertexIdx, i_32->vertexY, i_33->vertexX, i_34->vertexZ
- i_35->thisNormalIdx, i_36->bucketStart, i_37->bucketEnd, i_38->searchIdx
- i_39->otherNormalIdx, i_40->bucketIdx, class16646->otherMergedNormalsGL

### animateTransform (~55 locals, all renamed)
- Top-level: i_31->paramX, i_41->paramY, i_51->paramZ
- Type 1 (translate): i_10/11/12->transformedX/Y/Z
- Type 2 (rotation with verticesData):
  - i_10/11/12->parentOriginX/Y/Z, i_48/14/15->parentTranslateX/Y/Z
  - i_16/17/18 (withinOrigin)->txOriginX/Y/Z
  - ints_49->rotationMatrix, i_17/18->cosX/sinX, i_19/20->cosY/sinY, i_21/22->cosZ/sinZ
  - i_23->sinXcosZ, i_24->sinXsinZ
  - i_25/26/27->invOriginRotX/Y/Z, i_28/29/30->rotOriginOffsetX/Y/Z
  - ints_31->combinedRotMatrix, i_32/33/34->translatedX/Y/Z (after matrix loops: rowIdx/colIdx/dotProduct/innerIdx)
  - ints_35->finalMatrix, i_36/37/38->finalTranslateX/Y/Z (after loops: rowIdx/colIdx/dotProduct/innerIdx)
  - Vertex loop: i_39->labelIdx, i_40->label, ints_41->group, i_42->i, i_43->v, i_44/45/46->newX/Y/Z
- Type 3 (scale with verticesData):
  - i_10/11/12->parentOriginX/Y/Z, i_48/14/15->parentTranslateX/Y/Z
  - i_16/17/18->scaleX/Y/Z, i_19/20/21->scaledInvOriginX/Y/Z, i_22/23/24->scaleOriginOffsetX/Y/Z
  - ints_50->scaleMatrix, i_26/27/28->scaledTranslateX/Y/Z
  - ints_51->combinedScaleMatrix, i_30/52/32->finalTranslateX/Y/Z (loops: rowIdx/colIdx/dotProduct/innerIdx)
  - Vertex loop: i_33->labelIdx, i_34->label, ints_35->group, i_36->i, i_37->v, i_38/39/40->newX/Y/Z

### offsetSubmesh (2 locals)
- floats_5->resultBuffer, i_6->vertexIdx

### updateParticlePosition (5 locals)
- i_3->idx, class874->emitter, class875->activeEmitter, class1726->effector, class1727->activeEffector

## Rosetta Stone: aClass166NNNN Fields

All `aClass166NNNN` fields are `ModelNormalsGL` instances. The "166" encodes the original
obfuscated class number (Class166 = ModelNormalsGL). Only known instance: `aClass1668259`
in ModelGL -> renamed to `mergedNormals`.

## Rendering Pipeline Notes

- `drawMatrixThreaded()` orchestrates: calls `drawOpaqueFaces()` then `drawBillboards()`
- `drawOpaqueFaces()`: calls `upload()` -> `buildElementsBuffer()` -> renders with material batching
- `drawBillboards()`: renders billboard quads as camera-facing effects
- `renderedFaceCount` starts as `numFaces` but billboard faces with UIDs are excluded
- `vertexNormalIndices` created by `connectNormal()` which deduplicates normals by position+UV+normal direction
- `mergedNormals`: used by two-arg `applyShading()` overload to merge normals between adjacent models
- `dynamicElementsBuffer`/`dynamicArrayBuffer`: reusable GPU buffers for pre-allocated copy slots (types 1-7)
- `releaseUploadedData()`: frees CPU-side arrays after successful GPU upload

## External References (defined in other files, referenced from ModelGL)

These identifiers are referenced from ModelGL but defined in other files:
- **OpenGlToolkit fields**: `aClass3848361`, `aClass3848380`, `anInt8438`, `aFloat8315`, `aFloat8399`,
  `aFloatArray8396`, `aFloatArrayArray8395`, `aFloat8433`, `aFloat8434`, `aClass2948405`

# ModelFunctionMaskGL.kt Deobfuscation -- COMPLETE

File: `graphics/model/ModelFunctionMaskGL.kt` (~196 lines) -- Bitmask utility for model data retention checks.
**Status: ALL identifiers deobfuscated (0 remaining obfuscated)**

## Methods Renamed (12)

| Old Name | New Name |
|----------|----------|
| `method2199` | `requiresColorBufferRebuild` |
| `method2200` | `altersTextureCoordinates` |
| `method2202` | `retainsVerticesX` |
| `method2204` | `retainsNormals` |
| `method2205` | `retainsFaceColors` |
| `method2206` | `retainsFaceAlphas` |
| `method2211` | `retainsFaceIndices` |
| `method2212` | `retainsFaceMaterials` |
| `method2229` | `retainsVerticesY` |
| `method2241` | `altersTextureUV` |
| `method2252` | `retainsVerticesZ` |
| `method2261` | `requiresNormalsBufferRebuild` |

## Parameters Renamed (all methods)
- `i_0` -> `functionMask`, `i_1` -> `preferenceFlags`
