#include <QtWidgets>

#include "window.h"

Window::Window()
{
  createActions();
  createMenus();
  createDockWindows();
  setDockOptions(AllowTabbedDocks);

  glView = new GLView2D;

  QWidget *glContainer = QWidget::createWindowContainer(glView);
  // We need a minimum size or else the size defaults to zero.
  glContainer->setMinimumSize(512, 512);



  connect(minSlider, SIGNAL(valueChanged(int)), glView, SLOT(setChannelMin(int)));
  connect(glView, SIGNAL(channelMinChanged(int)), minSlider, SLOT(setValue(int)));
  connect(maxSlider, SIGNAL(valueChanged(int)), glView, SLOT(setChannelMax(int)));
  connect(glView, SIGNAL(channelMaxChanged(int)), maxSlider, SLOT(setValue(int)));

  connect(cutSlider, SIGNAL(valueChanged(int)), glView, SLOT(setZCut(int)));
  connect(glView, SIGNAL(zCutChanged(int)), cutSlider, SLOT(setValue(int)));

  QHBoxLayout *mainLayout = new QHBoxLayout;
  mainLayout->addWidget(glContainer);

  QWidget *central = new QWidget(this);
  central->setLayout(mainLayout);

  setCentralWidget(central);

  minSlider->setValue(6 *16);
  maxSlider->setValue(100 * 16);

  cutSlider->setValue(36);

  setWindowTitle(tr("Bio-Formats GLView"));
}

void Window::createActions()
{
  openAction = new QAction(tr("&Open image..."), this);
  openAction->setShortcuts(QKeySequence::Open);
  openAction->setStatusTip(tr("Open an existing image file"));
  connect(openAction, SIGNAL(triggered()), this, SLOT(open()));

  quitAction = new QAction(tr("&Quit"), this);
  quitAction->setShortcuts(QKeySequence::Quit);
  quitAction->setStatusTip(tr("Quit the application"));
  connect(quitAction, SIGNAL(triggered()), this, SLOT(quit()));

  viewResetAction = new QAction(tr("&Reset"), this);
  viewResetAction->setShortcut(QKeySequence(Qt::CTRL + Qt::SHIFT + Qt::Key_R));
  viewResetAction->setStatusTip(tr("Reset the current view"));
  connect(viewResetAction, SIGNAL(triggered()), this, SLOT(view_reset()));

  viewZoomAction = new QAction(tr("&Zoom"), this);
  viewZoomAction->setCheckable(true);
  viewZoomAction->setShortcut(QKeySequence(Qt::CTRL + Qt::SHIFT + Qt::Key_Z));
  viewZoomAction->setStatusTip(tr("Zoom the current view"));
  connect(viewZoomAction, SIGNAL(triggered()), this, SLOT(view_zoom()));

  viewPanAction = new QAction(tr("&Pan"), this);
  viewPanAction->setCheckable(true);
  viewPanAction->setShortcut(QKeySequence(Qt::CTRL + Qt::SHIFT + Qt::Key_P));
  viewPanAction->setStatusTip(tr("Pan the current view"));
  connect(viewPanAction, SIGNAL(triggered()), this, SLOT(view_pan()));

  viewRotateAction = new QAction(tr("Rota&te"), this);
  viewRotateAction->setCheckable(true);
  viewRotateAction->setShortcut(QKeySequence(Qt::CTRL + Qt::SHIFT + Qt::Key_T));
  viewRotateAction->setStatusTip(tr("Rotate the current view"));
  connect(viewRotateAction, SIGNAL(triggered()), this, SLOT(view_rotate()));

  viewActionGroup = new QActionGroup(this);
  viewActionGroup->addAction(viewZoomAction);
  viewActionGroup->addAction(viewPanAction);
  viewActionGroup->addAction(viewRotateAction);
  viewZoomAction->setChecked(true);
}

void Window::createMenus()
{
  fileMenu = menuBar()->addMenu(tr("&File"));
  fileMenu->addAction(openAction);
  fileMenu->addSeparator();
  fileMenu->addAction(quitAction);

  viewMenu = menuBar()->addMenu(tr("&View"));
  viewMenu->addAction(viewResetAction);
  fileMenu->addSeparator();
  viewMenu->addAction(viewZoomAction);
  viewMenu->addAction(viewPanAction);
  viewMenu->addAction(viewRotateAction);

}

void Window::createDockWindows()
{
  QDockWidget *dock = new QDockWidget(tr("Navigation"), this);
  dock->setAllowedAreas(Qt::AllDockWidgetAreas);
  QVBoxLayout *mainLayout = new QVBoxLayout;
  cutSlider = createCutSlider();
  mainLayout->addWidget(cutSlider);
  QWidget *mainWidget = new QWidget(this);
  mainWidget->setLayout(mainLayout);
  dock->setWidget(mainWidget);
  addDockWidget(Qt::BottomDockWidgetArea, dock);

  viewMenu->addSeparator();
  viewMenu->addAction(dock->toggleViewAction());

  dock = new QDockWidget(tr("Rendering"), this);
  dock->setAllowedAreas(Qt::AllDockWidgetAreas);
  mainLayout = new QVBoxLayout;
  minSlider=createRangeSlider();
  maxSlider=createRangeSlider();
  mainLayout->addWidget(minSlider);
  mainLayout->addWidget(maxSlider);
  mainWidget = new QWidget(this);
  mainWidget->setLayout(mainLayout);
  dock->setWidget(mainWidget);
  addDockWidget(Qt::BottomDockWidgetArea, dock);

  viewMenu->addAction(dock->toggleViewAction());
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
  QSlider *slider = new QSlider(Qt::Horizontal);
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
  QSlider *slider = new QSlider(Qt::Horizontal);
  slider->setRange(0, 143);
  slider->setSingleStep(1);
  slider->setPageStep(8);
  slider->setTickInterval(8);
  slider->setTickPosition(QSlider::TicksRight);
  return slider;
}

void Window::open()
{
}

void Window::quit()
{
  close();
}

void Window::view_reset()
{
  glView->setZoom(0);
  glView->setXTranslation(0);
  glView->setYTranslation(0);
  glView->setZRotation(0);
}

void Window::view_zoom()
{
  glView->setMouseMode(GLView2D::MODE_ZOOM);
}

void Window::view_pan()
{
  glView->setMouseMode(GLView2D::MODE_PAN);
}

void Window::view_rotate()
{
  glView->setMouseMode(GLView2D::MODE_ROTATE);
}
