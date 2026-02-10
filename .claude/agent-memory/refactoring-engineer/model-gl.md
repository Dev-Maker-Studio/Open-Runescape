# ModelGL.kt Deobfuscation

File: `graphics/model/ModelGL.kt` (~3,962 lines) -- Core 3D model renderer for legacy OpenGL pipeline.

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

## Remaining Obfuscated Identifiers (NOT in ModelGL)

These identifiers are referenced from ModelGL but defined in other files:
- **OpenGlToolkit fields**: `aClass3848361`, `aClass3848380`, `anInt8438`, `aFloat8315`, `aFloat8399`,
  `aFloatArray8396`, `aFloatArrayArray8395`, `aFloat8433`, `aFloat8434`, `aClass2948405`
- **ModelFunctionMaskGL methods**: `method2199`, `method2200`, `method2202`, etc.
