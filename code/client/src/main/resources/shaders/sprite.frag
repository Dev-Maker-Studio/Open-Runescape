#version 330 core

in vec2 vTexCoord;
in vec4 vColor;
flat in int vTexIndex;
flat in vec4 vClip;

uniform sampler2D uTextures[8];

out vec4 fragColor;

void main() {
    if (gl_FragCoord.x < vClip.x || gl_FragCoord.x >= vClip.z ||
        gl_FragCoord.y < vClip.y || gl_FragCoord.y >= vClip.w) {
        discard;
    }
    if (vTexIndex >= 0) {
        vec4 texColor;
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
        fragColor = vColor;
    }

    if (fragColor.a < 0.004) {
        discard;
    }
}
