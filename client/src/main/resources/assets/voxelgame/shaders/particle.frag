#version 330 core

in vec4 vColor;
out vec4 fragColor;

void main() {
    float glow = smoothstep(0.72, 1.0, max(max(vColor.r, vColor.g), vColor.b));
    vec3 rgb = vColor.rgb * (1.0 + glow * 0.35);
    fragColor = vec4(rgb, vColor.a);
}
