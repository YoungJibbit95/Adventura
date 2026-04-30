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
uniform float uGlobalBrightness;
uniform vec3 uFogColor;
uniform sampler2D uBlockAtlas;
uniform vec4 uSideUv[64];
uniform vec4 uTopUv[64];
uniform vec4 uBottomUv[64];
uniform vec4 uBlockColorAlpha[64];
uniform vec4 uBlockEffects[64];
out vec4 fragColor;

int safeBlockId(int id) {
    return clamp(id, 0, 63);
}

vec3 blockColor(int id) {
    return uBlockColorAlpha[safeBlockId(id)].rgb;
}

float blockAlpha(int id) {
    return uBlockColorAlpha[safeBlockId(id)].a;
}

bool fillsTextureGaps(int id) {
    return uBlockEffects[safeBlockId(id)].w > 0.5;
}

vec2 faceUv() {
    vec2 uv = vFaceUv;
    if (uv.x < 0.0 || uv.x >= 1.0) {
        uv.x = fract(uv.x);
    }
    if (uv.y < 0.0 || uv.y >= 1.0) {
        uv.y = fract(uv.y);
    }
    return uv;
}

vec4 atlasRect(int id) {
    int safeId = safeBlockId(id);
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
        if (fillsTextureGaps(id)) {
            return vec4(color.rgb, 1.0);
        }
        discard;
    }
    if (fillsTextureGaps(id)) {
        return vec4(mix(color.rgb, sampled.rgb, sampled.a), 1.0);
    }
    return vec4(mix(color.rgb, sampled.rgb, sampled.a), sampled.a);
}

float emissiveStrength(int id) {
    return uBlockEffects[safeBlockId(id)].x;
}

void main() {
    int id = int(vBlockId + 0.5);
    vec4 surface = blockSurface(id);
    vec3 lit = surface.rgb * vLight * vShade * vAo * uGlobalBrightness;
    if (uBloomEnabled == 1) {
        float glow = emissiveStrength(id);
        lit += surface.rgb * glow * uBloomStrength * (1.0 + vLight * 0.35);
    }
    lit = pow(lit, vec3(0.92));
    lit = mix(vec3(dot(lit, vec3(0.299, 0.587, 0.114))), lit, 1.12);
    if (uFogEnabled == 1) {
        float fogStart = min(uFogStart, uFogEnd - 0.001);
        float fog = smoothstep(fogStart, uFogEnd, vDistance);
        lit = mix(lit, uFogColor, fog);
    }
    fragColor = vec4(lit, blockAlpha(id) * surface.a);
}
