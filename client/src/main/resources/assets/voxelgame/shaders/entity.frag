#version 330 core

in float vHeight;
in float vDistance;
in vec3 vNormal;
in vec3 vWorldPosition;
uniform vec3 uBaseColor;
uniform vec3 uLightDirection;
uniform vec3 uFogColor;
uniform float uEmissive;
uniform float uEntityLight;
uniform float uGlobalBrightness;
uniform float uWeatherFlash;
uniform float uFogStart;
uniform float uFogEnd;
uniform int uFogEnabled;
uniform int uUnderwater;
out vec4 fragColor;

void main() {
    float lambert = max(dot(normalize(vNormal), normalize(uLightDirection)), 0.0);
    float wrapped = 0.46 + lambert * 0.54;
    float topWarmth = clamp(vHeight, 0.0, 1.0) * 0.08;
    vec3 lit = uBaseColor * (wrapped + topWarmth) * uEntityLight * uGlobalBrightness;
    float emissivePulse = 0.85 + sin(vHeight * 7.0) * 0.05;
    vec3 emissive = uBaseColor * emissivePulse;
    lit = mix(lit, emissive, clamp(uEmissive, 0.0, 1.0));
    if (uUnderwater == 1) {
        lit = mix(lit, lit * vec3(0.62, 0.82, 0.92) + vec3(0.01, 0.04, 0.06), 0.28);
    }
    float flash = clamp(uWeatherFlash, 0.0, 1.0);
    if (flash > 0.0) {
        lit = mix(lit, vec3(0.78, 0.86, 1.00), flash * 0.30);
    }
    if (uFogEnabled == 1) {
        float fogStart = min(uFogStart, uFogEnd - 0.001);
        float fog = smoothstep(fogStart, uFogEnd, vDistance);
        lit = mix(lit, uFogColor, fog);
    }
    fragColor = vec4(lit, 1.0);
}
