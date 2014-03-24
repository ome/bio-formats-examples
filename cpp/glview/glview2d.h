#ifndef GLVIEW2D_H
#define GLVIEW2D_H

#include "glwindow.h"

#include <QOpenGLShader>
#include <QElapsedTimer>

#include <tiffio.h>

#include <glm/glm.hpp>

class GLView2D : public GLWindow
{
  Q_OBJECT;

public:
  GLView2D(QWidget *parent = 0);
  ~GLView2D();

  QSize minimumSizeHint() const;
  QSize sizeHint() const;

public slots:
  void setXRotation(int angle);
  void setYRotation(int angle);
  void setZRotation(int angle);
  void setChannelMin(int min);
  void setChannelMax(int max);
  void setZCut(int cut);

signals:
  void xRotationChanged(int angle);
  void yRotationChanged(int angle);
  void zRotationChanged(int angle);
  void channelMinChanged(int min);
  void channelMaxChanged(int max);
  void zCutChanged(int cut);

protected:
  void initialize();
  void render();
  void resize();

  void mousePressEvent(QMouseEvent *event);
  void mouseMoveEvent(QMouseEvent *event);
  void timerEvent (QTimerEvent *event);

  void read_plane();

  void buffer_square(unsigned int vbo_vertices,
                     unsigned int vbo_texcoords,
                     unsigned int ibo_elements,
                     glm::vec2 xlim,
                     glm::vec2 ylim);

private:
  QElapsedTimer etimer;
  int xRot;
  int yRot;
  int zRot;
  glm::vec3 cmax;
  int depth;
  int olddepth;
  QPoint lastPos;
  QOpenGLShader *vshader;
  QOpenGLShader *fshader;
  QOpenGLShaderProgram *sprog;
  int attr_coordloc;
  int attr_texcoord;
  int uniform_mvp;
  int uniform_texture_r;
  int uniform_texture_g;
  int uniform_cmax;
  unsigned int vbo_square_vertices;
  unsigned int vbo_square_texcoords;
  unsigned int ibo_square_elements;
  unsigned int texture_id_r;
  unsigned int texture_id_g;
  TIFF *tiff;
  uint16_t *pixels;
};

#endif // GLVIEW2D_H

/*
 * Local Variables:
 * mode:C++
 * End:
 */
