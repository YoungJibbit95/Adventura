#version 330 core

in vec2 vTexCoord;
in vec4 vColor;

uniform sampler2D uTexture;

out vec4 fragColor;

void main() {
    vec4 sampled = texture(uTexture, vTexCoord) * vColor;
    if (sampled.a < 0.02) {
        discard;
    }
    fragColor = sampled;
}
