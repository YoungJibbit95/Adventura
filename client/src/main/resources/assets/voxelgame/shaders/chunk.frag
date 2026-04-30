#version 330 core

in float vLight;
in float vMaterialIndex;
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
uniform sampler2D uMaterialLut;
uniform int uMaterialCount;
out vec4 fragColor;

int materialIndex() {
    return clamp(int(vMaterialIndex + 0.5), 0, max(uMaterialCount - 1, 0));
}

vec4 materialTexel(int row) {
    return texelFetch(uMaterialLut, ivec2(materialIndex(), row), 0);
}

vec3 materialColor() {
    return materialTexel(0).rgb;
}

float materialAlpha() {
    return materialTexel(0).a;
}

bool flagSet(float flags, float flag) {
    return mod(floor(flags / flag), 2.0) >= 1.0;
}

float materialFlags() {
    return materialTexel(1).z;
}

bool fillsTextureGaps() {
    return flagSet(materialFlags(), 8.0);
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

vec4 atlasRect() {
    if (vNormal.y > 0.5) return materialTexel(3);
    if (vNormal.y < -0.5) return materialTexel(4);
    return materialTexel(2);
}

vec4 materialSurface() {
    vec4 color = vec4(materialColor(), 1.0);
    if (uAtlasEnabled == 0) {
        return color;
    }
    vec4 rect = atlasRect();
    if (rect.z <= rect.x || rect.w <= rect.y) {
        return color;
    }
    vec2 uv = mix(rect.xy, rect.zw, faceUv());
    vec4 sampled = texture(uBlockAtlas, uv);
    if (sampled.a < materialTexel(1).w) {
        if (fillsTextureGaps()) {
            return vec4(color.rgb, 1.0);
        }
        discard;
    }
    if (fillsTextureGaps()) {
        return vec4(mix(color.rgb, sampled.rgb, sampled.a), 1.0);
    }
    return vec4(mix(color.rgb, sampled.rgb, sampled.a), sampled.a);
}

float emissiveStrength() {
    return materialTexel(1).x;
}

bool animatedFluid() {
    return materialTexel(1).y > 0.5;
}

void main() {
    vec4 surface = materialSurface();
    vec3 lit = surface.rgb * vLight * vShade * vAo * uGlobalBrightness;
    if (uBloomEnabled == 1) {
        float glow = emissiveStrength();
        lit += surface.rgb * glow * uBloomStrength * (1.0 + vLight * 0.35);
    }
    if (animatedFluid()) {
        lit = max(lit, surface.rgb * 0.18);
        lit = mix(lit, lit + vec3(0.03, 0.06, 0.10), 0.25);
    }
    lit = pow(lit, vec3(0.92));
    lit = mix(vec3(dot(lit, vec3(0.299, 0.587, 0.114))), lit, 1.12);
    if (uFogEnabled == 1) {
        float fogStart = min(uFogStart, uFogEnd - 0.001);
        float fog = smoothstep(fogStart, uFogEnd, vDistance);
        lit = mix(lit, uFogColor, fog);
    }
    fragColor = vec4(lit, materialAlpha() * surface.a);
}
