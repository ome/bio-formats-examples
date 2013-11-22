#ifndef GLWINDOW_H
#define GLWINDOW_H

#include <QtGui/QWindow>
#include <QtGui/QOpenGLFunctions>
#include <QtGui/QOpenGLDebugMessage>

class QPainter;
class QOpenGLContext;
class QOpenGLPaintDevice;
class QOpenGLDebugLogger;

class GLWindow : public QWindow, protected QOpenGLFunctions
{
  Q_OBJECT
public:
  explicit GLWindow(QWindow *parent = 0);
  ~GLWindow();

  virtual void render(QPainter *painter);
  virtual void render();

  virtual void initialize();
  virtual void resize();

  void setAnimating(bool animating);

public slots:
  void renderLater();
  void renderNow();
  void logMessage(QOpenGLDebugMessage message);

protected:
  bool event(QEvent *event);

  void exposeEvent(QExposeEvent *event);

  void resizeEvent(QResizeEvent *event);

  QOpenGLContext *
  context() const;

  void
  makeCurrent();

private:
  bool update_pending;
  bool animating;

  QOpenGLContext *glcontext;
  QOpenGLPaintDevice *device;
  QOpenGLDebugLogger *logger;
};


#endif // GLWINDOW_H

/*
 * Local Variables:
 * mode:C++
 * End:
 */
