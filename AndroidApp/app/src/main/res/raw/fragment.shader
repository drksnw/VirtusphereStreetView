uniform samplerCube uSampler;

varying mediump vec4 vTexCoord;

void main() {
    mediump vec3 cube = vec3(textureCube(uSampler, vTexCoord.xyz));
    //gl_FragColor = vec4(cube, 1.0);
    gl_FragColor = textureCube(uSampler, vTexCoord.xyz);
    //gl_FragColor = vec4(0.0, 1.0, 1.0, 1.0);
}