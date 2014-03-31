#ifndef WINDOW_H
#define WINDOW_H

#include <QMainWindow>
#include "glview2d.h"

QT_BEGIN_NAMESPACE
class QSlider;
class QMenu;
class QAction;
class QActionGroup;
QT_END_NAMESPACE
class GLView3D;

class Window : public QMainWindow
{
  Q_OBJECT;

public:
  Window();

private slots:
  void open();
  void quit();
  void view_reset();
  void view_zoom();
  void view_pan();
  void view_rotate();

private:
  void createActions();
  void createMenus();
  void createDockWindows();

  QMenu *fileMenu;
  QMenu *viewMenu;

  QAction *openAction;
  QAction *quitAction;

  QAction *viewResetAction;

  QActionGroup *viewActionGroup;
  QAction *viewZoomAction;
  QAction *viewPanAction;
  QAction *viewRotateAction;

  QSlider *createAngleSlider();
  QSlider *createRangeSlider();
  QSlider *createCutSlider();

  GLView2D *glView;
  QSlider *minSlider;
  QSlider *maxSlider;
  QSlider *cutSlider;
};

#endif // WINDOW_H

/*
 * Local Variables:
 * mode:C++
 * End:
 */
