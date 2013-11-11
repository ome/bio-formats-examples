varying vec3 f_texcoord;
uniform sampler3D texture_r;
uniform sampler3D texture_g;
uniform vec3 cmax;

void main(void) {
  vec3 flipped_texcoord = vec3(f_texcoord.x, 1.0 - f_texcoord.y, f_texcoord.z);
  vec4 texval_r = texture3D(texture_r, flipped_texcoord);
  vec4 texval_g = texture3D(texture_g, flipped_texcoord);

  gl_FragColor[0] = ((texval_r[0] * 64.0) - (cmax[0])) / cmax[1] - (cmax[0]);
  gl_FragColor[1] = ((texval_g[0] * 64.0) - (cmax[0])) / ((cmax[1] - (cmax[0]))*2.0);
  gl_FragColor[2] = 0.0;
  gl_FragColor[3] = 1.0;
}
