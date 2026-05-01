#version 330 core

in float vLight;
in float vSkyLight;
in float vBlockLight;
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
uniform int uRenderDebugMode;
uniform int uUnderwater;
uniform int uSimpleWater;
uniform float uTime;
uniform float uFogStart;
uniform float uFogEnd;
uniform float uBloomStrength;
uniform float uBloomThreshold;
uniform float uGlobalBrightness;
uniform float uNightLightBoost;
uniform float uCaveDarkness;
uniform float uWeatherFlash;
uniform vec3 uFogColor;
uniform vec3 uBiomeTintColor;
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

bool translucentLayer() {
    return materialTexel(5).z > 1.5;
}

bool cutoutLayer() {
    float layer = materialTexel(5).z;
    return layer > 0.5 && layer < 1.5;
}

float biomeTintMode() {
    return materialTexel(5).x;
}

float fogAffectFactor() {
    float mode = materialTexel(5).y;
    if (mode > 1.5) {
        return 0.0;
    }
    if (mode > 0.5) {
        return 0.45;
    }
    return 1.0;
}

vec3 layerDebugColor() {
    float layer = materialTexel(5).z;
    if (layer < 0.5) {
        return vec3(0.76, 0.78, 0.82);
    }
    if (layer < 1.5) {
        return vec3(0.36, 0.88, 0.42);
    }
    return vec3(0.26, 0.58, 1.00);
}

vec3 materialDebugColor() {
    float index = float(materialIndex());
    return vec3(
            fract(index * 0.103),
            fract(index * 0.271 + 0.19),
            fract(index * 0.419 + 0.37)
    );
}

vec3 lightRamp(float value, vec3 tint) {
    float light = clamp(value, 0.0, 1.0);
    return mix(vec3(0.035, 0.040, 0.050), tint, light);
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

bool animatedFluid() {
    return materialTexel(1).y > 0.5;
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
    vec2 localUv = faceUv();
    if (animatedFluid() && uSimpleWater == 0) {
        localUv += vec2(
                sin(uTime * 0.42 + vWorldPosition.z * 0.08),
                cos(uTime * 0.36 + vWorldPosition.x * 0.08)
        ) * 0.018;
    }
    vec2 uv = mix(rect.xy, rect.zw, fract(localUv));
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

float terrainLight(float emissive) {
    float sky = clamp(vSkyLight, 0.0, 1.0) * uGlobalBrightness;
    float block = clamp(vBlockLight, 0.0, 1.0) * (0.74 + uNightLightBoost * 0.34);
    float light = max(sky * 0.86, block);
    if (cutoutLayer()) {
        light += 0.07;
    }
    if (animatedFluid()) {
        light = max(light, 0.18 + vBlockLight * 0.22);
    }
    float cave = (1.0 - smoothstep(0.18, 0.72, clamp(vSkyLight, 0.0, 1.0))) * uCaveDarkness;
    cave *= 1.0 - clamp(vBlockLight * 1.35 + emissive, 0.0, 1.0);
    light *= mix(1.0, 0.54, cave);
    return clamp(max(light, 0.12), 0.0, 1.18);
}

float aoForLight(float emissive) {
    float blockLift = clamp(vBlockLight * 0.50 + emissive * 0.70, 0.0, 0.68);
    return mix(clamp(vAo, 0.0, 1.0), 1.0, blockLift);
}

float bloomAmount(float emissive) {
    float threshold = clamp(uBloomThreshold, 0.0, 0.95);
    return smoothstep(threshold, 1.0, clamp(emissive, 0.0, 1.0));
}

vec3 applyBiomeTint(vec3 color) {
    float mode = biomeTintMode();
    if (mode < 0.5) {
        return color;
    }
    float strength = mode > 2.5 ? 0.30 : 0.22;
    if (mode > 1.5 && mode < 2.5) {
        strength = 0.16;
    }
    return mix(color, color * uBiomeTintColor, strength);
}

void main() {
    vec4 surface = materialSurface();
    surface.rgb = applyBiomeTint(surface.rgb);
    if (uRenderDebugMode == 1) {
        fragColor = vec4(materialDebugColor(), max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 2) {
        float light = clamp(vLight, 0.0, 1.0);
        fragColor = vec4(vec3(light), max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 3) {
        fragColor = vec4(vec3(clamp(vAo, 0.0, 1.0)), max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 4) {
        vec3 tint = biomeTintMode() < 0.5 ? vec3(0.18, 0.18, 0.18) : uBiomeTintColor;
        fragColor = vec4(tint, max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 5) {
        fragColor = vec4(layerDebugColor(), max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 6) {
        vec2 uv = faceUv();
        float checker = mod(floor(uv.x * 8.0) + floor(uv.y * 8.0), 2.0);
        fragColor = vec4(uv.x, uv.y, checker, max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 7) {
        float layer = materialTexel(5).z;
        float alpha = layer > 1.5 ? 0.92 : 0.65;
        vec3 color = layer > 1.5
                ? vec3(0.12, 0.52, 1.00)
                : layer > 0.5 ? vec3(0.32, 0.92, 0.36) : vec3(0.16, 0.16, 0.18);
        fragColor = vec4(color, alpha);
        return;
    }
    if (uRenderDebugMode == 8) {
        fragColor = vec4(lightRamp(vSkyLight, vec3(0.40, 0.66, 1.00)), max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 9) {
        fragColor = vec4(lightRamp(vBlockLight, vec3(1.00, 0.58, 0.18)), max(0.72, materialAlpha() * surface.a));
        return;
    }
    if (uRenderDebugMode == 10) {
        float glow = emissiveStrength();
        vec3 color = mix(vec3(0.035, 0.025, 0.045), surface.rgb, clamp(glow, 0.0, 1.0));
        fragColor = vec4(color, max(0.72, materialAlpha() * surface.a));
        return;
    }

    float emissive = emissiveStrength();
    float normalizedLight = terrainLight(emissive);
    float ao = aoForLight(emissive);
    float foliageShade = biomeTintMode() > 1.5 && biomeTintMode() < 2.5 ? 0.94 : 1.0;
    vec3 lit = surface.rgb * normalizedLight * vShade * ao * foliageShade;
    vec3 glowContribution = vec3(0.0);
    float bloom = bloomAmount(emissive);
    if (uBloomEnabled == 1 && bloom > 0.0) {
        glowContribution = surface.rgb * bloom * uBloomStrength * (1.0 + uNightLightBoost * 0.85 + normalizedLight * 0.25);
        glowContribution = clamp(glowContribution, vec3(0.0), vec3(0.42));
        lit += glowContribution * 0.55;
    }
    if (animatedFluid()) {
        float waterLift = uSimpleWater == 1 ? 0.12 : 0.20;
        lit = max(lit, surface.rgb * waterLift);
        lit = mix(lit, lit + vec3(0.03, 0.06, 0.10), uSimpleWater == 1 ? 0.12 : 0.25);
    }
    if (uUnderwater == 1) {
        lit = mix(lit, lit * vec3(0.60, 0.82, 0.92) + vec3(0.01, 0.04, 0.06), 0.32);
    }
    lit = pow(lit, vec3(0.92));
    lit = mix(vec3(dot(lit, vec3(0.299, 0.587, 0.114))), lit, 1.12);
    if (uFogEnabled == 1) {
        float fogStart = min(uFogStart, uFogEnd - 0.001);
        float fog = smoothstep(fogStart, uFogEnd, vDistance) * fogAffectFactor();
        lit = mix(lit, uFogColor, fog);
    }
    float flash = clamp(uWeatherFlash, 0.0, 1.0);
    if (flash > 0.0) {
        vec3 lightningColor = vec3(0.78, 0.86, 1.00);
        lit = mix(lit, lightningColor, flash * 0.34);
        lit += lightningColor * flash * 0.08;
    }
    if (uBloomEnabled == 1) {
        lit += glowContribution * 0.45;
    } else if (emissive > 0.0) {
        lit += surface.rgb * emissive * 0.10;
    }
    fragColor = vec4(lit, materialAlpha() * surface.a);
}
