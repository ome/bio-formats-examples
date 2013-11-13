#include <QtGui/QMouseEvent>
#include <QtGui/QOpenGLContext>

#include <math.h>

#include "glview.h"

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

#include <tiffio.h>

#include <iostream>

namespace
{
  void check_gl(std::string const& message)
  {
    GLenum err = GL_NO_ERROR;
    while ((err = glGetError()) != GL_NO_ERROR)
      {
        std::cerr << "GL error (" << message << ") :";
        switch(err)
          {
          case GL_INVALID_ENUM:
            std::cerr << "Invalid enum";
            break;
          case GL_INVALID_VALUE:
            std::cerr << "Invalid value";
            break;
          case GL_INVALID_OPERATION:
            std::cerr << "Invalid operation";
            break;
          case GL_INVALID_FRAMEBUFFER_OPERATION:
            std::cerr << "Invalid framebuffer operation";
            break;
          case GL_OUT_OF_MEMORY:
            std::cerr << "Out of memory";
            break;
          case GL_STACK_UNDERFLOW:
            std::cerr << "Stack underflow";
            break;
          case GL_STACK_OVERFLOW:
            std::cerr << "Stack overflow";
            break;
          default:
            std::cerr << "Unknown (" << err << ')';
            break;
          }
        std::cerr << std::endl;
      }
  }
}

GLView::GLView(QWidget *parent):
  GLWindow(),
  etimer(),
  xRot(0),
  yRot(0),
  zRot(0),
  cmax(1.0, 1.0, 1.0),
  depth(0),
  lastPos(0, 0),
  vshader(),
  fshader(),
  sprog(),
  attr_coordloc(),
  attr_texcoord(),
  uniform_mvp(),
  uniform_texture_r(),
  uniform_texture_g(),
  uniform_cmax(),
  vbo_cube_vertices(0),
  vbo_cube_texcoords(0),
  ibo_cube_elements(0),
  texture_id_r(),
  texture_id_g()
{
}

GLView::~GLView()
{
  makeCurrent();

  if (glIsBuffer(vbo_cube_vertices))
    glDeleteBuffers(1, &vbo_cube_vertices);
  if (glIsBuffer(vbo_cube_texcoords))
    glDeleteBuffers(1, &vbo_cube_texcoords);
  if (glIsBuffer(ibo_cube_elements))
    glDeleteBuffers(1, &ibo_cube_elements);
}

QSize GLView::minimumSizeHint() const
{
  return QSize(800, 600);
}

QSize GLView::sizeHint() const
{
  return QSize(800, 600);
}

static void qNormalizeAngle(int &angle)
{
  while (angle < 0)
    angle += 360 * 16;
  while (angle > 360 * 16)
    angle -= 360 * 16;
}

void GLView::setXRotation(int angle)
{
  qNormalizeAngle(angle);
  if (angle != xRot) {
    xRot = angle;
    emit xRotationChanged(angle);
    renderLater();
  }
}

void GLView::setYRotation(int angle)
{
  qNormalizeAngle(angle);
  if (angle != yRot) {
    yRot = angle;
    emit yRotationChanged(angle);
    renderLater();
  }
}

void GLView::setZRotation(int angle)
{
  qNormalizeAngle(angle);
  if (angle != zRot) {
    zRot = angle;
    emit zRotationChanged(angle);
    renderLater();
  }
}


void GLView::setChannelMin(int min)
{
  float v = min / (255.0*16.0);
  if (cmax[0] != v)
    {
      cmax[0] = v;
      emit channelMinChanged(min);
      renderLater();
    }
}

void GLView::setChannelMax(int max)
{
  float v = max / (255.0*16.0);
  if (cmax[1] != v)
    {
      cmax[1] = v;
      emit channelMaxChanged(max);
      renderLater();
    }
}


void GLView::setZCut(int cut)
{
  if (depth != cut)
    {
      depth = cut;
      emit zCutChanged(cut);
      renderLater();
    }
}

void GLView::initialize()
{
  glEnable(GL_DEPTH_TEST);
  check_gl("Enable depth test");
  glEnable(GL_CULL_FACE);
  check_gl("Enable cull face");
  glEnable(GL_MULTISAMPLE);
  check_gl("Enable multisampling");
  glEnable(GL_BLEND);
  check_gl("Enable blending");
  glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
  check_gl("Set blend function");

  vshader = new QOpenGLShader(QOpenGLShader::Vertex, this);
  vshader->compileSourceFile("../cube.v.glsl");
  if (!vshader->isCompiled())
    {
      std::cerr << "Failed to compile vertex shader\n" << vshader->log().toStdString() << std::endl;
    }

  fshader = new QOpenGLShader(QOpenGLShader::Fragment, this);
  fshader->compileSourceFile("../cube.f.glsl");
  if (!fshader->isCompiled())
    {
      std::cerr << "Failed to compile fragment shader\n" << fshader->log().toStdString() << std::endl;
    }

  sprog = new QOpenGLShaderProgram(this);
  sprog->addShader(vshader);
  sprog->addShader(fshader);
  sprog->link();

  if (!sprog->isLinked())
    {
      std::cerr << "Failed to link shader program\n" << sprog->log().toStdString() << std::endl;
    }

  attr_coordloc = sprog->attributeLocation("coord3d");
  if (attr_coordloc == -1)
    std::cerr << "Failed to bind coordinate location" << std::endl;

  attr_texcoord = sprog->attributeLocation("texcoord");
  if (attr_texcoord == -1)
    std::cerr << "Failed to bind texture coordinates" << std::endl;

  uniform_mvp = sprog->uniformLocation("mvp");
  if (uniform_mvp == -1)
    std::cerr << "Failed to bind transform" << std::endl;

  uniform_texture_r = sprog->uniformLocation("texture_r");
  if (uniform_texture_r == -1)
    std::cerr << "Failed to bind texture R" << std::endl;

  uniform_texture_g = sprog->uniformLocation("texture_g");
  if (uniform_texture_g == -1)
    std::cerr << "Failed to bind texture G" << std::endl;

  uniform_cmax = sprog->uniformLocation("cmax");
  if (uniform_cmax == -1)
    std::cerr << "Failed to bind cmax" << std::endl;

  glm::vec2 xlim(-512.0, 512.0);
  glm::vec2 ylim(-512.0, 512.0);
  glm::vec2 zlim(-72.0*1.772701, 72.0*1.772701);

  glGenBuffers(1, &vbo_cube_vertices);
  check_gl("Generate cube vertex buffer");
  glGenBuffers(1, &vbo_cube_texcoords);
  check_gl("Generate cube texture coords buffer");
  glGenBuffers(1, &ibo_cube_elements);
  check_gl("Generate cube elements buffer");
  buffer_cube(vbo_cube_vertices, vbo_cube_texcoords, ibo_cube_elements,
              xlim, ylim, zlim);

  GLint max_combined_texture_image_units;
  glGetIntegerv(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, &max_combined_texture_image_units);
  std::cout << "Texture unit count: " << max_combined_texture_image_units << std::endl;

  TIFF *tiff = TIFFOpen("../vessels.ome.tiff", "r");
  if (tiff == 0) {
    std::cerr << "Error opening tiff" << std::endl;
  }

  TIFFSetDirectory(tiff, 0);

  uint32_t w, h, d = 144;
  TIFFGetField(tiff, TIFFTAG_IMAGEWIDTH, &w);
  TIFFGetField(tiff, TIFFTAG_IMAGELENGTH, &h);

  uint32_t npixels = w * h * d;
  uint32_t stripsize = TIFFStripSize(tiff);
  uint32_t strips = TIFFNumberOfStrips(tiff);
  uint32_t striprows = TIFFGetField(tiff, TIFFTAG_ROWSPERSTRIP, &striprows);

  std::cout << "Image size: w=" << w << " h=" << h
            << " stripsize=" << stripsize
            << " strips=" << strips << std::endl;

  uint16_t *pixels = (uint16_t *) _TIFFmalloc(stripsize);
  std::cout << "Alloc: " << npixels * sizeof (uint16_t) << std::endl;

  {
    glGenTextures(1, &texture_id_r);
    check_gl("Generate texture");
    glBindTexture(GL_TEXTURE_3D, texture_id_r);
    check_gl("Bind texture");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
    check_gl("Set texture min filter");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    check_gl("Set texture mag filter");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    check_gl("Set texture wrap s");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    check_gl("Set texture wrap t");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
    check_gl("Set texture wrap r");
    glTexImage3D(GL_TEXTURE_3D, // target
                 0,  // level, 0 = base, no minimap,
                 GL_R16, // internalformat
                 w,  // width
                 h,  // height
                 d,  // depth
                 0,  // border
                 GL_RED,  // format
                 GL_UNSIGNED_SHORT, // type
                 0);
    check_gl("Texture create");

    if (pixels != 0)
      {
        for (unsigned int ifd = 0, z=0; ifd<288; ifd+=2, ++z)
          {
            int ok = TIFFSetDirectory(tiff, ifd);
            if (!ok)
              std::cout << "Error setting TIFF directory to " << ifd << std::endl;
            //            std::cout << "Reading IFD " << ifd << " (plane " << z << ")" << std::endl;
            std::cout << '.';

            for (tstrip_t nstrip = 0, y=0; nstrip < strips; nstrip++, y+=striprows)
              {
                TIFFReadEncodedStrip(tiff, nstrip, pixels, (tsize_t) -1);
                glTexSubImage3D(GL_TEXTURE_3D, // target
                                0,  // level, 0 = base, no minimap,
                                0, y, z,
                                w,  // width
                                striprows,  // height
                                1,  // depth
                                GL_RED,  // format
                                GL_UNSIGNED_SHORT, // type
                                pixels);
                check_gl("Texture set pixels in subregion");
              }
          }
        std::cout << " done.\n";
      }
    else
      std::cerr << "Not allocated tiff pixel buffer" << std::endl;
    std::cout << "Texture loaded" << std::endl;
    glGenerateMipmap(GL_TEXTURE_3D);
    check_gl("Generate mipmaps");
  }

  {
    glGenTextures(1, &texture_id_g);
    check_gl("Generate texture");
    glBindTexture(GL_TEXTURE_3D, texture_id_g);
    check_gl("Bind texture");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
    check_gl("Set texture min filter");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    check_gl("Set texture mag filter");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    check_gl("Set texture wrap s");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    check_gl("Set texture wrap t");
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
    check_gl("Set texture wrap r");
    glTexImage3D(GL_TEXTURE_3D, // target
                 0,  // level, 0 = base, no minimap,
                 GL_R16, // internalformat
                 w,  // width
                 h,  // height
                 d,  // depth
                 0,  // border
                 GL_RED,  // format
                 GL_UNSIGNED_SHORT, // type
                 0);
    check_gl("Texture create");

    if (pixels != 0)
      {
        for (unsigned int ifd = 1, z=0; ifd<288; ifd+=2, ++z)
          {
            int ok = TIFFSetDirectory(tiff, ifd);
            if (!ok)
              std::cout << "Error setting TIFF directory to " << ifd << std::endl;
            //            std::cout << "Reading IFD " << ifd << " (plane " << z << ")" << std::endl;
            std::cout << '.';

            for (tstrip_t nstrip = 0, y=0; nstrip < strips; nstrip++, y+=striprows)
              {
                TIFFReadEncodedStrip(tiff, nstrip, pixels, (tsize_t) -1);
                glTexSubImage3D(GL_TEXTURE_3D, // target
                                0,  // level, 0 = base, no minimap,
                                0, y, z,
                                w,  // width
                                striprows,  // height
                                1,  // depth
                                GL_RED,  // format
                                GL_UNSIGNED_SHORT, // type
                                pixels);
                check_gl("Texture set pixels in subregion");
              }
          }
        std::cout << " done.\n";
      }
    else
      std::cerr << "Not allocated tiff pixel buffer" << std::endl;
    std::cout << "Texture loaded" << std::endl;
    glGenerateMipmap(GL_TEXTURE_3D);
    check_gl("Generate mipmaps");
  }
  _TIFFfree(pixels);
  TIFFClose(tiff);

  // Start timers
  startTimer(0);
  etimer.start();

  // Size viewport
  resize();
}

void
GLView::buffer_cube(unsigned int vbo_vertices,
                    unsigned int vbo_texcoords,
                    unsigned int ibo_elements,
                    glm::vec2 xlim,
                    glm::vec2 ylim,
                    glm::vec2 zlim)
{
  GLfloat cube_vertices[] = {
    // front
    xlim[0], ylim[0], zlim[1],
    xlim[1], ylim[0], zlim[1],
    xlim[1], ylim[1], zlim[1],
    xlim[0], ylim[1], zlim[1],
    // top
    xlim[0], ylim[1], zlim[1],
    xlim[1], ylim[1], zlim[1],
    xlim[1], ylim[1], zlim[0],
    xlim[0], ylim[1], zlim[0],
    // back
    xlim[1], ylim[0], zlim[0],
    xlim[0], ylim[0], zlim[0],
    xlim[0], ylim[1], zlim[0],
    xlim[1], ylim[1], zlim[0],
    // bottom
    xlim[0], ylim[0], zlim[0],
    xlim[1], ylim[0], zlim[0],
    xlim[1], ylim[0], zlim[1],
    xlim[0], ylim[0], zlim[1],
    // left
    xlim[0], ylim[0], zlim[0],
    xlim[0], ylim[0], zlim[1],
    xlim[0], ylim[1], zlim[1],
    xlim[0], ylim[1], zlim[0],
    // right
    xlim[1], ylim[0], zlim[1],
    xlim[1], ylim[0], zlim[0],
    xlim[1], ylim[1], zlim[0],
    xlim[1], ylim[1], zlim[1]
  };

  glBindBuffer(GL_ARRAY_BUFFER, vbo_vertices);
  check_gl("Bind vertex buffer");
  glBufferData(GL_ARRAY_BUFFER, sizeof(cube_vertices), cube_vertices, GL_DYNAMIC_DRAW);
  check_gl("Set vertex buffer");

  glm::vec2 texxlim(0.0, 1.0);
  glm::vec2 texylim(0.0, 1.0);
  glm::vec2 texzlim(0.0, (zlim[1]-zlim[0])/(144.0*1.772701));
  GLfloat cube_texcoords[] = {
    // front
    texxlim[0], texylim[0], texzlim[1],
    texxlim[1], texylim[0], texzlim[1],
    texxlim[1], texylim[1], texzlim[1],
    texxlim[0], texylim[1], texzlim[1],
    // top
    texxlim[0], texylim[1], texzlim[1],
    texxlim[1], texylim[1], texzlim[1],
    texxlim[1], texylim[1], texzlim[0],
    texxlim[0], texylim[1], texzlim[0],
    // back
    texxlim[1], texylim[0], texzlim[0],
    texxlim[0], texylim[0], texzlim[0],
    texxlim[0], texylim[1], texzlim[0],
    texxlim[1], texylim[1], texzlim[0],
    // bottom
    texxlim[0], texylim[0], texzlim[0],
    texxlim[1], texylim[0], texzlim[0],
    texxlim[1], texylim[0], texzlim[1],
    texxlim[0], texylim[0], texzlim[1],
    // left
    texxlim[0], texylim[0], texzlim[0],
    texxlim[0], texylim[0], texzlim[1],
    texxlim[0], texylim[1], texzlim[1],
    texxlim[0], texylim[1], texzlim[0],
    // right
    texxlim[1], texylim[0], texzlim[1],
    texxlim[1], texylim[0], texzlim[0],
    texxlim[1], texylim[1], texzlim[0],
    texxlim[1], texylim[1], texzlim[1]
  };

  glBindBuffer(GL_ARRAY_BUFFER, vbo_texcoords);
  check_gl("Bind texture coords buffer");
  glBufferData(GL_ARRAY_BUFFER, sizeof(cube_texcoords), cube_texcoords, GL_DYNAMIC_DRAW);
  check_gl("Set texture coords buffer");

  GLushort cube_elements[] = {
    // front
    0,  1,  2,
    2,  3,  0,
    // top
    4,  5,  6,
    6,  7,  4,
    // back
    8,  9, 10,
    10, 11,  8,
    // bottom
    12, 13, 14,
    14, 15, 12,
    // left
    16, 17, 18,
    18, 19, 16,
    // right
    20, 21, 22,
    22, 23, 20,
  };
  glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo_elements);
  check_gl("Bind elements buffer");
  glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(cube_elements), cube_elements, GL_DYNAMIC_DRAW);
  check_gl("Set elements buffer");
}

void
GLView::render()
{
  glClearColor(1.0, 1.0, 1.0, 1.0);
  check_gl("Clear colour");
  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
  check_gl("Clear buffers");

  sprog->bind();

  glActiveTexture(GL_TEXTURE0);
  check_gl("Activate texture 0");
  glBindTexture(GL_TEXTURE_3D, texture_id_r);
  check_gl("Bind texture 0");
  glUniform1i(uniform_texture_r, /*GL_TEXTURE*/1);
  check_gl("Set uniform 0");
  glActiveTexture(GL_TEXTURE1);
  check_gl("Activate texture 1");
  glBindTexture(GL_TEXTURE_3D, texture_id_g);
  check_gl("Bind texture 1");
  glUniform1i(uniform_texture_g, /*GL_TEXTURE*/0);
  check_gl("Set uniform 1");
  glUniform3fv(uniform_cmax, 1, glm::value_ptr(cmax));
  check_gl("Set uniform 2");

  glEnableVertexAttribArray(attr_coordloc);
  check_gl("Enable coords array");
  glBindBuffer(GL_ARRAY_BUFFER, vbo_cube_vertices);
  check_gl("Bind coords array");
  glVertexAttribPointer(attr_coordloc, // attribute
                        3,             // number of elements per vertex, here (x,y,z)
                        GL_FLOAT,      // the type of each element
                        GL_FALSE,      // take our values as-is
                        0,             // no extra data between each position
                        0);            // offset of first element
  check_gl("Set coords array");

  glEnableVertexAttribArray(attr_texcoord);
  check_gl("Enable texture coords array");
  glBindBuffer(GL_ARRAY_BUFFER, vbo_cube_texcoords);
  check_gl("Enable bind coords array");
  glVertexAttribPointer(
        attr_texcoord, // attribute
        3,                 // number of elements per vertex, here (R,G,B)
        GL_FLOAT,          // the type of each element
        GL_FALSE,          // take our values as-is
        0,                 // no extra data between each position
        0);                // offset of first element
  check_gl("Set texture coords array");

  // Push each element in buffer_vertices to the vertex shader
  glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo_cube_elements);
  check_gl("Bind elements array");
  int size;  glGetBufferParameteriv(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_SIZE, &size);
  glDrawElements(GL_TRIANGLES, size/sizeof(GLushort), GL_UNSIGNED_SHORT, 0);

  glDisableVertexAttribArray(attr_texcoord);
  glDisableVertexAttribArray(attr_coordloc);
}

void GLView::resize()
{
  QSize newsize = size();
  glViewport(0, 0, newsize.width(), newsize.height());
}


void GLView::mousePressEvent(QMouseEvent *event)
{
  lastPos = event->pos();
}

void GLView::mouseMoveEvent(QMouseEvent *event)
{
  int dx = event->x() - lastPos.x();
  int dy = event->y() - lastPos.y();

  if (event->buttons() & Qt::LeftButton) {
    setXRotation(xRot + 8 * dy);
    setYRotation(yRot + 8 * -dx);
  } else if (event->buttons() & Qt::RightButton) {
    setXRotation(xRot + 8 * dy);
    setZRotation(zRot + 8 * -dx);
  }
  lastPos = event->pos();
}

void GLView::timerEvent (QTimerEvent *event)
{
  //int64_t elapsed = etimer.elapsed();
  QSize s = size();
  // Size may be zero if the window is not yet mapped.

  //float angle = elapsed / 1000.0 * 1;  // 1° per second
  glm::vec3 axis_x(1, 0, 0);
  glm::vec3 axis_y(0, 1, 0);
  glm::vec3 axis_z(0, 0, 1);
  glm::mat4 rotx = glm::rotate(glm::mat4(1.0f), static_cast<float>(xRot)/16.0f, axis_x);
  glm::mat4 roty = glm::rotate(glm::mat4(1.0f), static_cast<float>(yRot)/16.0f, axis_y);
  glm::mat4 rotz = glm::rotate(glm::mat4(1.0f), static_cast<float>(zRot)/16.0f, axis_z);
  glm::mat4 model = glm::scale(glm::translate(glm::mat4(1.0f), glm::vec3(0.0, 0.0, -4.0)), glm::vec3(1.0/256.0, 1.0/256.0, 1.0/256.0));
  glm::mat4 view = glm::lookAt(glm::vec3(0.0, 2.0, 0.0), glm::vec3(0.0, 0.0, -4.0), glm::vec3(0.0, 1.0, 0.0));
  glm::mat4 projection = glm::perspective(60.0f, 1.0f*s.width()/s.height(), 0.1f, 10.0f);

  glm::mat4 mvp = projection * view * model * rotx * roty * rotz;

  sprog->bind();
  glUniformMatrix4fv(uniform_mvp, 1, GL_FALSE, glm::value_ptr(mvp));

  GLWindow::timerEvent(event);

  glm::vec2 xlim(-512.0, 512.0);
  glm::vec2 ylim(-512.0, 512.0);
  glm::vec2 zlim(-72.0*1.772701, ((144.0-depth)-72.0)*1.772701);
  buffer_cube(vbo_cube_vertices, vbo_cube_texcoords, ibo_cube_elements, xlim, ylim, zlim);

  renderLater();
}
