#version 330 core

// Inputs from vertex shader
in vec2 vTexCoord;
in vec4 vColor;
flat in int vTexIndex;

// Texture samplers for multi-texture batching (up to 8 textures bound simultaneously)
uniform sampler2D uTextures[8];

// Output
out vec4 fragColor;

void main() {
    if (vTexIndex >= 0) {
        vec4 texColor;
        // Select texture by index. Switch statement compiles to efficient branching
        // on all modern GPU architectures. The flat interpolation qualifier ensures
        // all fragments in a primitive use the same index, so there is zero divergence
        // within a triangle.
        switch (vTexIndex) {
            case 0:  texColor = texture(uTextures[0], vTexCoord); break;
            case 1:  texColor = texture(uTextures[1], vTexCoord); break;
            case 2:  texColor = texture(uTextures[2], vTexCoord); break;
            case 3:  texColor = texture(uTextures[3], vTexCoord); break;
            case 4:  texColor = texture(uTextures[4], vTexCoord); break;
            case 5:  texColor = texture(uTextures[5], vTexCoord); break;
            case 6:  texColor = texture(uTextures[6], vTexCoord); break;
            default: texColor = texture(uTextures[7], vTexCoord); break;
        }
        fragColor = texColor * vColor;
    } else {
        // Untextured (solid color)
        fragColor = vColor;
    }

    // Discard fully transparent fragments
    if (fragColor.a < 0.004) {
        discard;
    }
}
