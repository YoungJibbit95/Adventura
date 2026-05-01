#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in float aMaterialIndex;
layout (location = 3) in float aLight;
layout (location = 4) in float aSkyLight;
layout (location = 5) in float aBlockLight;
layout (location = 6) in float aAo;
layout (location = 7) in vec2 aFaceUv;

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uCameraPosition;
uniform vec3 uSunDirection;
uniform int uAmbientOcclusionEnabled;
uniform int uSoftShadowsEnabled;
uniform int uSimpleWater;
uniform int uWindEnabled;
uniform float uTime;
uniform float uShadowStrength;
uniform sampler2D uMaterialLut;
uniform int uMaterialCount;

out float vLight;
out float vSkyLight;
out float vBlockLight;
out float vMaterialIndex;
out float vShade;
out float vDistance;
out float vAo;
out vec3 vWorldPosition;
out vec3 vNormal;
out vec2 vFaceUv;

vec4 materialTexel(float materialIndex, int row) {
    int safeIndex = clamp(int(materialIndex + 0.5), 0, max(uMaterialCount - 1, 0));
    return texelFetch(uMaterialLut, ivec2(safeIndex, row), 0);
}

bool animatedFluid(float materialIndex) {
    return materialTexel(materialIndex, 1).y > 0.5;
}

float biomeTintMode(float materialIndex) {
    return materialTexel(materialIndex, 5).x;
}

float renderLayer(float materialIndex) {
    return materialTexel(materialIndex, 5).z;
}

bool windAnimatedCutout(float materialIndex) {
    float layer = renderLayer(materialIndex);
    return uWindEnabled == 1 && layer > 0.5 && layer < 1.5 && biomeTintMode(materialIndex) > 0.5;
}

void main() {
    vLight = clamp(aLight, 0.08, 1.20);
    vSkyLight = clamp(aSkyLight, 0.0, 1.0);
    vBlockLight = clamp(aBlockLight, 0.0, 1.0);
    vMaterialIndex = aMaterialIndex;
    float sun = max(dot(normalize(aNormal), normalize(uSunDirection)), 0.0);
    float floorShade = uSoftShadowsEnabled == 1 ? 0.34 : 0.50;
    vShade = mix(floorShade, 1.0, sun);
    if (uSoftShadowsEnabled == 1 && aNormal.y < 0.1) {
        float shadowNoise = sin(aPosition.x * 0.37 + aPosition.z * 0.23) * 0.5 + 0.5;
        vShade *= 1.0 - shadowNoise * uShadowStrength * 0.18;
    }
    vAo = uAmbientOcclusionEnabled == 1 ? aAo : 1.0;
    vec3 position = aPosition;
    if (animatedFluid(aMaterialIndex) && uSimpleWater == 0) {
        position.y += sin(uTime * 2.2 + aPosition.x * 0.45 + aPosition.z * 0.33) * 0.035;
    }
    if (windAnimatedCutout(aMaterialIndex)) {
        float sway = sin(uTime * 1.7 + aPosition.x * 0.29 + aPosition.z * 0.41) * 0.028;
        position.x += sway;
        position.z += sway * 0.45;
    }
    vWorldPosition = position;
    vNormal = aNormal;
    vFaceUv = aFaceUv;
    vDistance = distance(position, uCameraPosition);
    gl_Position = uProjection * uView * vec4(position, 1.0);
}
