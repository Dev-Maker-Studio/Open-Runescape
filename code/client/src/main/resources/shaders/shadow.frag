#version 330 core

in vec2 vTexCoord;

uniform bool uAlphaTestEnabled;
uniform sampler2D uTexture;
uniform float uAlphaThreshold;

void main() {
    if (uAlphaTestEnabled) {
        float alpha = texture(uTexture, vTexCoord).a;
        if (alpha <= uAlphaThreshold) {
            discard;
        }
    }

}
