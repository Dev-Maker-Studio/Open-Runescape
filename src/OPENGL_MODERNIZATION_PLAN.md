# OpenGL Rendering Pipeline Modernization Plan

## Executive Summary

This document outlines the plan to modernize the RuneTek5 rendering engine from a fixed-function OpenGL pipeline to a modern programmable pipeline with batched draw calls. The goal is to improve performance through GPU utilization while maintaining exact visual output and ensuring future WebGL compatibility.

---

## Current Architecture Analysis

### OpenGL Binding Layer (jaggl/)

The current `jaggl.OpenGL` class is a **JNI wrapper** that provides direct access to native OpenGL functions. Key characteristics:

- **Thread-safe**: Per-thread context management via `Hashtable<Thread, OpenGL>`
- **Mixed pipeline support**: Both fixed-function and ARB extensions
- **Extension checking**: Runtime capability querying

#### Current OpenGL Features Used:

**Fixed-Function (Legacy) - To Be Replaced:**
- Matrix operations: `glLoadMatrixf`, `glMultMatrixf`, `glTranslatef`, `glRotatef`, `glScalef`
- Matrix modes: `glMatrixMode(GL_MODELVIEW/GL_PROJECTION/GL_TEXTURE)`
- Client state arrays: `glEnableClientState`, `glVertexPointer`, `glNormalPointer`, `glColorPointer`, `glTexCoordPointer`
- Immediate mode: `glBegin/glEnd` with `GL_QUADS`, `GL_TRIANGLES`
- Display lists: `glGenLists`, `glNewList`, `glCallList`, `glDeleteLists`
- Fixed-function lighting: `glLight*`, `glMaterial*`, `glColorMaterial`, `GL_LIGHTING`
- Fog: `glFog*`
- Texture environment: `glTexEnvi`, `glTexGen*`
- Alpha test: `glAlphaFunc`, `GL_ALPHA_TEST`

**Modern Features (Already Available):**
- VBOs (ARB): `glGenBuffersARB`, `glBindBufferARB`, `glBufferDataARB`
- Indexed drawing: `glDrawElements`, `glDrawArrays`
- GLSL Shaders: `glCreateShader`, `glCompileShader`, `glLinkProgram`, `glUseProgram`
- Uniform uploads: `glUniform*`, `glUniformMatrix*`
- Vertex attributes: `glVertexAttribPointer`, `glEnableVertexAttribArray`
- Framebuffers (EXT): `glGenFramebuffersEXT`, `glBindFramebufferEXT`

### Graphics Toolkit Layer (runetek5/toolkit/)

**Class Hierarchy:**
```
AbstractToolkit (abstract interface)
├── OpenGlToolkit (primary implementation ~2500 lines)
│   ├── State management (blend, depth, lighting, fog)
│   ├── Buffer management (VBOs, display lists)
│   ├── Texture cache
│   └── Material effects
└── SafeModeToolkit (software fallback)
```

**Key Rendering Methods:**
- `setupMatrixRendering()` - Configures 3D rendering state
- `renderTexture(materialId, hasLight)` - Binds material and sets up texture combiners
- `toggleDrawingClientstates(vertex, normal, color, texcoord)` - Enables vertex arrays
- `drawElements(elementsBuffer, offset, count)` - Issues indexed draw call
- `loadTextureCombineMode(mode)` - Configures GL_COMBINE texture environment

### Model Rendering (ModelGL.kt)

**Current Flow:**
1. `upload()` - Pack vertex data into interleaved buffer (position, normal, texcoord, color)
2. `buildElementsBuffer()` - Create index buffer from face arrays
3. `drawOpaqueFaces()` - Main render method:
   - Enable client states for vertex arrays
   - For each material group:
     - `toolkit.renderTexture(materialId)` - Set up material state
     - `toolkit.drawElements(buffer, offset, count)` - Draw indexed triangles

**Vertex Layout (Interleaved):**
```
struct Vertex {
    float position[3];  // 12 bytes
    float normal[3];    // 12 bytes (optional)
    float texcoord[2];  // 8 bytes (optional)
    uint8_t color[4];   // 4 bytes (optional)
};
```

### Ground/Terrain Rendering (GroundGL.kt)

**Current Flow:**
1. Tiles organized by material in `GroundMaterialGL` nodes
2. Per-tile vertex data packed into shared VBO
3. Dynamic lights applied in additive pass
4. Shadow mapping via separate FBO

---

## Modernization Strategy

### Phase 1: Add Modern OpenGL Bindings (LWJGL3)

**Rationale:** LWJGL3 provides:
- Cross-platform OpenGL 3.3+ Core Profile support
- WebGL-compatible subset (OpenGL ES 3.0 is WebGL 2.0 compatible)
- Well-maintained, actively developed
- Proper VAO support required for Core Profile

**Implementation:**
1. Add LWJGL3 dependency to build.gradle.kts
2. Create abstraction layer that can use either jaggl or LWJGL3
3. Gradually migrate to LWJGL3 calls

```kotlin
// build.gradle.kts addition
val lwjglVersion = "3.3.3"
val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        "Windows" in name -> "natives-windows"
        "Linux" in name -> "natives-linux"
        "Mac" in name || "Darwin" in name -> "natives-macos"
        else -> throw Error("Unrecognized platform")
    }
}

implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
implementation("org.lwjgl", "lwjgl")
implementation("org.lwjgl", "lwjgl-opengl")
implementation("org.lwjgl", "lwjgl-glfw")
runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
```

### Phase 2: Create Shader Infrastructure

**Required Shaders:**

#### 1. Model Shader (model.vert/frag)
```glsl
// model.vert
#version 330 core
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelViewProjection;
uniform mat4 uModelView;
uniform mat3 uNormalMatrix;
uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;

out vec2 vTexCoord;
out vec4 vColor;
out vec3 vNormal;
out float vFogFactor;

void main() {
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);
    vTexCoord = aTexCoord;
    vColor = aColor;
    vNormal = uNormalMatrix * aNormal;

    // Fog calculation
    vec4 eyePos = uModelView * vec4(aPosition, 1.0);
    float fogDist = length(eyePos.xyz);
    vFogFactor = clamp((uFogEnd - fogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
}

// model.frag
#version 330 core
in vec2 vTexCoord;
in vec4 vColor;
in vec3 vNormal;
in float vFogFactor;

uniform sampler2D uTexture;
uniform bool uHasTexture;
uniform vec4 uFogColor;
uniform int uCombineMode;

out vec4 fragColor;

void main() {
    vec4 texColor = uHasTexture ? texture(uTexture, vTexCoord) : vec4(1.0);
    vec4 baseColor = vColor * texColor;

    // Alpha test (equivalent to glAlphaFunc(GL_GREATER, 0.0))
    if (baseColor.a <= 0.0) discard;

    // Apply fog
    fragColor = mix(uFogColor, baseColor, vFogFactor);
}
```

#### 2. Ground Shader (ground.vert/frag)
Similar to model but with:
- Terrain-specific lighting
- Shadow map sampling
- Water effects uniform support

#### 3. UI/2D Shader (ui.vert/frag)
Simple ortho projection for UI elements.

### Phase 3: Create VAO/VBO Management Layer

**New Classes:**

```kotlin
// com/jagex/game/runetek5/graphics/modern/
├── ShaderProgram.kt      // Compile, link, uniform management
├── VertexArray.kt        // VAO wrapper with attribute setup
├── VertexBuffer.kt       // VBO wrapper with typed data upload
├── IndexBuffer.kt        // EBO wrapper
├── UniformBuffer.kt      // UBO for shared uniforms (matrices, lighting)
├── RenderBatch.kt        // Batching multiple models
└── ModernRenderer.kt     // Main rendering coordinator
```

**VertexArray.kt:**
```kotlin
class VertexArray(private val gl: GL33) {
    private val id: Int = gl.glGenVertexArrays()

    fun bind() = gl.glBindVertexArray(id)
    fun unbind() = gl.glBindVertexArray(0)

    fun setAttribute(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        offset: Long
    ) {
        gl.glVertexAttribPointer(index, size, type, normalized, stride, offset)
        gl.glEnableVertexAttribArray(index)
    }

    fun destroy() = gl.glDeleteVertexArrays(id)
}
```

### Phase 4: Implement Draw Call Batching

**Strategy: Material-Based Batching**

Current issue: Each model/object issues separate draw calls per material.

Solution: Batch all geometry sharing the same material into a single draw call.

```kotlin
class RenderBatch {
    private val batches = mutableMapOf<Int, MaterialBatch>()

    data class DrawCommand(
        val vertexOffset: Int,
        val indexOffset: Int,
        val indexCount: Int,
        val modelMatrix: Matrix4f
    )

    class MaterialBatch(val materialId: Int) {
        val vertices = DynamicVertexBuffer()
        val indices = DynamicIndexBuffer()
        val commands = mutableListOf<DrawCommand>()
    }

    fun addModel(model: ModelGL, transform: Matrix4f) {
        // Group model faces by material
        model.forEachMaterialGroup { materialId, faces ->
            val batch = batches.getOrPut(materialId) { MaterialBatch(materialId) }
            val vertexOffset = batch.vertices.position
            val indexOffset = batch.indices.position

            // Add vertices and indices to batch buffers
            batch.vertices.put(model.getVertices(faces))
            batch.indices.put(model.getIndices(faces), vertexOffset)
            batch.commands.add(DrawCommand(vertexOffset, indexOffset, faces.size * 3, transform))
        }
    }

    fun flush(renderer: ModernRenderer) {
        batches.forEach { (materialId, batch) ->
            renderer.bindMaterial(materialId)
            batch.vertices.upload()
            batch.indices.upload()

            // Use instanced rendering or multi-draw if available
            if (renderer.supportsMultiDraw()) {
                renderer.multiDrawElementsIndirect(batch.commands)
            } else {
                batch.commands.forEach { cmd ->
                    renderer.setModelMatrix(cmd.modelMatrix)
                    renderer.drawElements(cmd.indexOffset, cmd.indexCount)
                }
            }
        }
        clear()
    }
}
```

### Phase 5: Migrate ModelGL

**Changes to ModelGL.kt:**

1. Remove fixed-function buffer setup
2. Use new VertexArray/VertexBuffer classes
3. Integrate with batching system

```kotlin
// New rendering method
fun renderModern(renderer: ModernRenderer, transform: Matrix4f) {
    // Ensure GPU buffers are ready
    ensureUploaded()

    // Add to batch instead of immediate draw
    renderer.currentBatch.addModel(this, transform)
}

// Keep legacy method for compatibility during migration
fun renderLegacy() {
    drawOpaqueFaces() // Existing implementation
}
```

### Phase 6: Migrate GroundGL

Ground rendering follows similar pattern but with:
- Tile-based chunking for efficient culling
- Shadow map integration
- Dynamic lighting accumulation

### Phase 7: Migrate UI Rendering

Replace `glBegin/glEnd` immediate mode with batched quads:

```kotlin
class UiBatch {
    private val vertices = FloatArray(MAX_QUADS * 4 * 4) // pos(2) + uv(2)
    private val colors = IntArray(MAX_QUADS * 4)
    private var quadCount = 0

    fun drawQuad(x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, color: Int) {
        // Add to batch
    }

    fun flush() {
        if (quadCount == 0) return
        // Upload and draw all quads in one call
    }
}
```

---

## Implementation Order

### Milestone 1: Foundation (Non-Breaking)
1. Add LWJGL3 dependency
2. Create shader infrastructure
3. Create VAO/VBO wrapper classes
4. Create abstraction layer that wraps both jaggl and LWJGL3

### Milestone 2: Parallel Renderer
5. Create ModernRenderer that uses new infrastructure
6. Add toggle to switch between legacy and modern renderer
7. Implement model shader and test with single model

### Milestone 3: Feature Parity
8. Implement all material effects as shader variants
9. Implement fog, lighting, shadows in shaders
10. Verify visual output matches legacy renderer

### Milestone 4: Batching
11. Implement RenderBatch for models
12. Integrate batching into scene rendering
13. Performance profiling and optimization

### Milestone 5: Complete Migration
14. Remove fixed-function code paths
15. Deprecate jaggl bindings
16. Document WebGL compatibility considerations

---

## WebGL Compatibility Notes

For future WebGL port, the following constraints apply:

**WebGL 2.0 (OpenGL ES 3.0) Limitations:**
- No geometry shaders
- No tessellation shaders
- Limited texture formats
- No DSA (Direct State Access)
- Integer attributes require explicit normalization handling

**Design Decisions for Compatibility:**
- Use GLSL version 300 es compatible constructs
- Avoid unsupported texture formats
- Use standard VAO/VBO patterns
- Avoid OpenGL-specific extensions not in WebGL

---

## File Structure

```
code/client/src/main/kotlin/com/jagex/game/runetek5/graphics/modern/
├── core/
│   ├── ShaderProgram.kt
│   ├── VertexArray.kt
│   ├── VertexBuffer.kt
│   ├── IndexBuffer.kt
│   └── UniformBuffer.kt
├── batch/
│   ├── RenderBatch.kt
│   ├── MaterialBatch.kt
│   └── DrawCommand.kt
├── shader/
│   ├── ModelShader.kt
│   ├── GroundShader.kt
│   └── UiShader.kt
├── renderer/
│   ├── ModernRenderer.kt
│   ├── ModelRenderer.kt
│   └── GroundRenderer.kt
└── util/
    ├── GLWrapper.kt      // Abstracts jaggl vs LWJGL3
    └── Matrix4f.kt       // Math utilities

resources/shaders/
├── model.vert
├── model.frag
├── ground.vert
├── ground.frag
├── ui.vert
└── ui.frag
```

---

## Risk Mitigation

1. **Visual Regression**: Side-by-side comparison testing between legacy and modern renderers
2. **Performance Regression**: Profiling at each milestone
3. **Compatibility Issues**: Test on multiple GPU vendors (NVIDIA, AMD, Intel)
4. **Breaking Changes**: Feature flag to toggle between renderers during development

---

## Success Criteria

1. **Performance**: 50%+ reduction in draw calls for typical scenes
2. **Visual Fidelity**: Pixel-perfect match with legacy renderer (within floating-point tolerance)
3. **Maintainability**: Clean separation between rendering backend and game logic
4. **Future-Proof**: Clear path to WebGL port with documented limitations
