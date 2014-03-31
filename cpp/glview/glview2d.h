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
  enum MouseMode
    {
      MODE_ZOOM,
      MODE_PAN,
      MODE_ROTATE
    };

  GLView2D(QWidget *parent = 0);
  ~GLView2D();

  QSize minimumSizeHint() const;
  QSize sizeHint() const;

public slots:
  void setZoom(int zoom);
  void setXTranslation(int xtran);
  void setYTranslation(int ytran);
  void setZRotation(int angle);
  void setChannelMin(int min);
  void setChannelMax(int max);
  void setZCut(int cut);
  void setMouseMode(MouseMode mode);

public:
  MouseMode getMouseMode() const;

signals:
  void zoomChanged(int zoom);
  void xTranslationChanged(int xtran);
  void yTranslationChanged(int ytran);
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
  MouseMode mouseMode;
  QElapsedTimer etimer;
  int zoom;
  int xTran;
  int yTran;
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
  unsigned int texture_id[2];
  TIFF *tiff;
  uint16_t *pixels;
};

#endif // GLVIEW2D_H

/*
 * Local Variables:
 * mode:C++
 * End:
 */
