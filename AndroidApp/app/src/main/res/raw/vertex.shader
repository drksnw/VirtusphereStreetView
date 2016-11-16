uniform mat4 uMVMatrix;
uniform mat4 uMVP;

attribute vec3 aNormal;

attribute vec4 aPosition;
varying mediump vec4 vTexCoord;

void main() {
    vec4 eyeCoords = uMVMatrix * vec4(aPosition.xyz, 1.0);
    vTexCoord = aPosition;
    gl_Position = uMVP * eyeCoords;
}