#include <QtWidgets>

#include "glview.h"
#include "window.h"

Window::Window()
{
  glView = new GLView;

  xSlider = createAngleSlider();
  ySlider = createAngleSlider();
  zSlider = createAngleSlider();

  minSlider=createRangeSlider();
  maxSlider=createRangeSlider();

  cutSlider = createCutSlider();

  connect(xSlider, SIGNAL(valueChanged(int)), glView, SLOT(setXRotation(int)));
  connect(glView, SIGNAL(xRotationChanged(int)), xSlider, SLOT(setValue(int)));
  connect(ySlider, SIGNAL(valueChanged(int)), glView, SLOT(setYRotation(int)));
  connect(glView, SIGNAL(yRotationChanged(int)), ySlider, SLOT(setValue(int)));
  connect(zSlider, SIGNAL(valueChanged(int)), glView, SLOT(setZRotation(int)));
  connect(glView, SIGNAL(zRotationChanged(int)), zSlider, SLOT(setValue(int)));

  connect(minSlider, SIGNAL(valueChanged(int)), glView, SLOT(setChannelMin(int)));
  connect(glView, SIGNAL(channelMinChanged(int)), minSlider, SLOT(setValue(int)));
  connect(maxSlider, SIGNAL(valueChanged(int)), glView, SLOT(setChannelMax(int)));
  connect(glView, SIGNAL(channelMaxChanged(int)), maxSlider, SLOT(setValue(int)));

  connect(cutSlider, SIGNAL(valueChanged(int)), glView, SLOT(setZCut(int)));
  connect(glView, SIGNAL(zCutChanged(int)), cutSlider, SLOT(setValue(int)));

  QHBoxLayout *mainLayout = new QHBoxLayout;
  mainLayout->addWidget(glView);
  mainLayout->addWidget(xSlider);
  mainLayout->addWidget(ySlider);
  mainLayout->addWidget(zSlider);
  mainLayout->addWidget(minSlider);
  mainLayout->addWidget(maxSlider);
  mainLayout->addWidget(cutSlider);

  setLayout(mainLayout);

  xSlider->setValue(160 * 16);
  ySlider->setValue(180 * 16);
  zSlider->setValue(180 * 16);

  minSlider->setValue(6 *16);
  maxSlider->setValue(100 * 16);

  cutSlider->setValue(0);

  setWindowTitle(tr("Bio-Formats GLView"));
}

QSlider *Window::createAngleSlider()
{
  QSlider *slider = new QSlider(Qt::Vertical);
  slider->setRange(0, 365 * 16);
  slider->setSingleStep(16);
  slider->setPageStep(8 * 16);
  slider->setTickInterval(8 * 16);
  slider->setTickPosition(QSlider::TicksRight);
  return slider;
}

QSlider *Window::createRangeSlider()
{
  QSlider *slider = new QSlider(Qt::Vertical);
  slider->setRange(0, 255 * 16);
  slider->setSingleStep(16);
  slider->setPageStep(8 * 16);
  slider->setTickInterval(8 * 16);
  slider->setTickPosition(QSlider::TicksRight);
  return slider;
}

// Note hardcoded range
QSlider *Window::createCutSlider()
{
  QSlider *slider = new QSlider(Qt::Vertical);
  slider->setRange(0, 144);
  slider->setSingleStep(1);
  slider->setPageStep(8);
  slider->setTickInterval(8);
  slider->setTickPosition(QSlider::TicksRight);
  return slider;
}

void Window::keyPressEvent(QKeyEvent *e)
{
  if (e->key() == Qt::Key_Escape)
    close();
  else
    QWidget::keyPressEvent(e);
}
