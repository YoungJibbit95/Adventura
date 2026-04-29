#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in float aBlockId;
layout (location = 3) in float aLight;
layout (location = 4) in float aAo;

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uCameraPosition;
uniform vec3 uSunDirection;
uniform int uAmbientOcclusionEnabled;
uniform int uSoftShadowsEnabled;
uniform float uTime;
uniform float uShadowStrength;

out float vLight;
out float vBlockId;
out float vShade;
out float vDistance;
out float vAo;
out vec3 vWorldPosition;
out vec3 vNormal;

void main() {
    vLight = max(aLight, 0.12);
    vBlockId = aBlockId;
    float sun = max(dot(normalize(aNormal), normalize(uSunDirection)), 0.0);
    float floorShade = uSoftShadowsEnabled == 1 ? 0.34 : 0.50;
    vShade = mix(floorShade, 1.0, sun);
    if (uSoftShadowsEnabled == 1 && aNormal.y < 0.1) {
        float shadowNoise = sin(aPosition.x * 0.37 + aPosition.z * 0.23) * 0.5 + 0.5;
        vShade *= 1.0 - shadowNoise * uShadowStrength * 0.18;
    }
    vAo = uAmbientOcclusionEnabled == 1 ? aAo : 1.0;
    vec3 position = aPosition;
    if (int(aBlockId + 0.5) == 4) {
        position.y += sin(uTime * 2.2 + aPosition.x * 0.45 + aPosition.z * 0.33) * 0.035;
    }
    vWorldPosition = position;
    vNormal = aNormal;
    vDistance = distance(position, uCameraPosition);
    gl_Position = uProjection * uView * vec4(position, 1.0);
}
