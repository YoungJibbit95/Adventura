#version 330 core

in float vLight;
in float vBlockId;
in float vShade;
in float vDistance;
in float vAo;
in vec3 vWorldPosition;
in vec3 vNormal;
in vec2 vFaceUv;
uniform int uFogEnabled;
uniform int uAtlasEnabled;
uniform int uBloomEnabled;
uniform float uFogStart;
uniform float uFogEnd;
uniform float uBloomStrength;
uniform vec3 uFogColor;
uniform sampler2D uBlockAtlas;
uniform vec4 uSideUv[256];
uniform vec4 uTopUv[256];
uniform vec4 uBottomUv[256];
out vec4 fragColor;

vec3 blockColor(int id) {
    if (id == 1) return vec3(0.48, 0.48, 0.47);
    if (id == 2) return vec3(0.42, 0.27, 0.15);
    if (id == 3) return vec3(0.33, 0.62, 0.24);
    if (id == 4) return vec3(0.20, 0.42, 0.82);
    if (id == 5) return vec3(0.78, 0.70, 0.45);
    if (id == 6) return vec3(0.42, 0.28, 0.16);
    if (id == 7) return vec3(0.20, 0.48, 0.24);
    if (id == 8) return vec3(0.28, 0.27, 0.26);
    if (id == 9) return vec3(1.00, 0.72, 0.30);
    if (id == 10) return vec3(0.25, 0.58, 0.20);
    if (id == 11) return vec3(0.95, 0.75, 0.20);
    if (id == 12) return vec3(0.58, 0.50, 0.43);
    if (id == 13) return vec3(0.62, 0.36, 0.20);
    if (id == 14) return vec3(0.46, 0.55, 0.62);
    if (id == 15) return vec3(0.18, 0.50, 0.25);
    if (id == 16) return vec3(0.34, 0.43, 0.32);
    if (id == 17) return vec3(0.45, 0.43, 0.38);
    if (id == 18) return vec3(0.86, 0.91, 0.91);
    if (id == 19) return vec3(0.50, 0.74, 0.88);
    if (id == 20) return vec3(0.38, 0.23, 0.13);
    if (id == 21) return vec3(0.12, 0.31, 0.25);
    if (id == 22) return vec3(0.62, 0.20, 0.16);
    if (id == 23) return vec3(0.58, 0.39, 0.20);
    if (id == 24) return vec3(0.63, 0.36, 0.22);
    if (id == 25) return vec3(0.95, 0.68, 0.28);
    if (id == 26) return vec3(0.54, 0.24, 0.24);
    if (id == 27) return vec3(0.50, 0.31, 0.18);
    if (id == 28) return vec3(0.48, 0.29, 0.17);
    if (id == 29) return vec3(0.46, 0.29, 0.16);
    if (id == 30) return vec3(0.30, 0.44, 0.28);
    if (id == 31) return vec3(0.43, 0.31, 0.17);
    if (id == 32) return vec3(0.28, 0.45, 0.24);
    if (id == 33) return vec3(0.36, 0.52, 0.30);
    if (id == 34) return vec3(0.95, 0.45, 0.20);
    if (id == 35) return vec3(0.42, 0.42, 0.40);
    if (id == 36) return vec3(0.38, 0.24, 0.13);
    if (id == 37) return vec3(0.58, 0.32, 0.26);
    if (id == 38) return vec3(0.50, 0.58, 0.62);
    if (id == 39) return vec3(0.34, 0.78, 0.92);
    if (id == 40) return vec3(1.00, 0.55, 0.20);
    if (id == 41) return vec3(0.24, 0.22, 0.20);
    return vec3(0.70, 0.30, 0.70);
}

float blockAlpha(int id) {
    if (id == 4) return 0.58;
    if (id == 19) return 0.70;
    return 1.0;
}

vec2 faceUv() {
    return clamp(vFaceUv, vec2(0.0), vec2(1.0));
}

vec4 atlasRect(int id) {
    int safeId = clamp(id, 0, 255);
    if (vNormal.y > 0.5) return uTopUv[safeId];
    if (vNormal.y < -0.5) return uBottomUv[safeId];
    return uSideUv[safeId];
}

vec4 blockSurface(int id) {
    vec4 color = vec4(blockColor(id), 1.0);
    if (uAtlasEnabled == 0) {
        return color;
    }
    vec4 rect = atlasRect(id);
    if (rect.z <= rect.x || rect.w <= rect.y) {
        return color;
    }
    vec2 uv = mix(rect.xy, rect.zw, faceUv());
    vec4 sampled = texture(uBlockAtlas, uv);
    if (sampled.a < 0.05) {
        discard;
    }
    return vec4(mix(color.rgb, sampled.rgb, sampled.a), sampled.a);
}

float emissiveStrength(int id) {
    if (id == 9) return 0.85;
    if (id == 25) return 0.78;
    if (id == 34) return 0.55;
    if (id == 39) return 0.92;
    if (id == 40) return 1.0;
    return 0.0;
}

void main() {
    int id = int(vBlockId + 0.5);
    vec4 surface = blockSurface(id);
    vec3 lit = surface.rgb * vLight * vShade * vAo;
    if (uBloomEnabled == 1) {
        float glow = emissiveStrength(id);
        lit += surface.rgb * glow * uBloomStrength * (1.0 + vLight * 0.35);
    }
    lit = pow(lit, vec3(0.92));
    lit = mix(vec3(dot(lit, vec3(0.299, 0.587, 0.114))), lit, 1.12);
    if (uFogEnabled == 1) {
        float fog = smoothstep(uFogStart, uFogEnd, vDistance);
        lit = mix(lit, uFogColor, fog);
    }
    fragColor = vec4(lit, blockAlpha(id) * surface.a);
}
