#version 330 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform vec3 uCameraPosition;

out float vHeight;
out float vDistance;
out vec3 vNormal;
out vec3 vWorldPosition;

void main() {
    vHeight = aPosition.y;
    vNormal = normalize(mat3(transpose(inverse(uModel))) * aNormal);
    vec4 worldPosition = uModel * vec4(aPosition, 1.0);
    vWorldPosition = worldPosition.xyz;
    vDistance = distance(vWorldPosition, uCameraPosition);
    gl_Position = uProjection * uView * worldPosition;
}
