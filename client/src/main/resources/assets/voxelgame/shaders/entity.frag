#version 330 core

in float vHeight;
uniform vec3 uBaseColor;
uniform vec3 uHeadColor;
out vec4 fragColor;

void main() {
    fragColor = vec4(mix(uBaseColor, uHeadColor, clamp(vHeight, 0.0, 1.0)), 1.0);
}
