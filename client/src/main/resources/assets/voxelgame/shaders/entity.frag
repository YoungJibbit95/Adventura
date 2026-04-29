#version 330 core

in float vHeight;
in vec3 vNormal;
uniform vec3 uBaseColor;
uniform vec3 uLightDirection;
uniform float uEmissive;
out vec4 fragColor;

void main() {
    float lambert = max(dot(normalize(vNormal), normalize(uLightDirection)), 0.0);
    float wrapped = 0.46 + lambert * 0.54;
    float topWarmth = clamp(vHeight, 0.0, 1.0) * 0.08;
    vec3 lit = uBaseColor * (wrapped + topWarmth);
    vec3 emissive = uBaseColor * (0.85 + sin(gl_FragCoord.x * 0.05 + gl_FragCoord.y * 0.03) * 0.05);
    fragColor = vec4(mix(lit, emissive, clamp(uEmissive, 0.0, 1.0)), 1.0);
}
