#ifndef WINDOW_H
#define WINDOW_H

#include <QWidget>

QT_BEGIN_NAMESPACE
class QSlider;
QT_END_NAMESPACE
class GLView;

class Window : public QWidget
{
  Q_OBJECT;

public:
  Window();

protected:
  void keyPressEvent(QKeyEvent *event);

private:
  QSlider *createAngleSlider();
  QSlider *createRangeSlider();
  QSlider *createCutSlider();

  GLView *glView;
  QSlider *xSlider;
  QSlider *ySlider;
  QSlider *zSlider;
  QSlider *minSlider;
  QSlider *maxSlider;
  QSlider *cutSlider;
};

#endif // WINDOW_H
