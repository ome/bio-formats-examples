#version 110

attribute vec3 coord3d;
attribute vec3 texcoord;
varying vec3 f_texcoord;
uniform mat4 mvp;

void main(void) {
  gl_Position = mvp * vec4(coord3d, 1.0);
  f_texcoord = texcoord;
}
