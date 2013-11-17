#include "glwindow.h"

#include <QtCore/QCoreApplication>

#include <QtGui/QOpenGLContext>
#include <QtGui/QOpenGLDebugLogger>
#include <QtGui/QOpenGLPaintDevice>
#include <QtGui/QPainter>

GLWindow::GLWindow(QWindow *parent):
  QWindow(parent),
  update_pending(false),
  animating(false),
  glcontext(0),
  device(0)
{
  setSurfaceType(QWindow::OpenGLSurface);
}

GLWindow::~GLWindow()
{
  delete device;
}

void
GLWindow::render(QPainter *painter)
{
  Q_UNUSED(painter);
}

void
GLWindow::initialize()
{
}

void
GLWindow::resize()
{
}

void
GLWindow::render()
{
  if (!device)
    device = new QOpenGLPaintDevice;

  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

  device->setSize(size());

  QPainter painter(device);
  render(&painter);
}

void
GLWindow::renderLater()
{
  if (!update_pending) {
    update_pending = true;
    QCoreApplication::postEvent(this, new QEvent(QEvent::UpdateRequest));
  }
}

bool
GLWindow::event(QEvent *event)
{
  switch (event->type()) {
  case QEvent::UpdateRequest:
    update_pending = false;
    renderNow();
    return true;
  default:
    return QWindow::event(event);
  }
}

void
GLWindow::exposeEvent(QExposeEvent *event)
{
  Q_UNUSED(event);

  if (isExposed())
    renderNow();
}

void GLWindow::resizeEvent(QResizeEvent * event)
{
  if (glcontext)
    resize();
}

QOpenGLContext *
GLWindow::context() const
{
  return glcontext;
}

void
GLWindow::makeCurrent()
{
  if (glcontext)
    glcontext->makeCurrent(this);
}

void GLWindow::renderNow()
{
  if (!isExposed())
    return;

  bool needsInitialize = false;

  if (!glcontext) {
    QSurfaceFormat format = requestedFormat();
    // OpenGL 2.0 profile with debugging.
    format.setVersion(2, 0);
    format.setProfile(QSurfaceFormat::NoProfile);
#ifdef BIOFORMATS_OPENGL_DEBUG
    format.setOption(QSurfaceFormat::DebugContext);
#endif // BIOFORMATS_OPENGL_DEBUG
    format.setSamples(4);

    glcontext = new QOpenGLContext(this);
    glcontext->setFormat(format);
    glcontext->create();
    makeCurrent();

    logger = new QOpenGLDebugLogger(this);
    connect(logger, SIGNAL(messageLogged(QOpenGLDebugMessage)),
             this, SLOT(logMessage(QOpenGLDebugMessage)),
             Qt::DirectConnection);
    if (logger->initialize()) {
      logger->startLogging(QOpenGLDebugLogger::SynchronousLogging);
      logger->enableMessages();
    }

    needsInitialize = true;
  }

  makeCurrent();

  if (needsInitialize) {
    initializeOpenGLFunctions();
    initialize();
  }

  render();

  glcontext->swapBuffers(this);

  if (animating)
    renderLater();
}

void
GLWindow::setAnimating(bool animating)
{
  this->animating = animating;

  if (this->animating)
    renderLater();
}

void
GLWindow::logMessage(QOpenGLDebugMessage message)
{
  qDebug() << message;
}
