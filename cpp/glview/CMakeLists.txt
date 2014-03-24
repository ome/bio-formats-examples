cmake_minimum_required(VERSION 2.8)
PROJECT(glview)
set(VERSION 1.0)

set(CMAKE_AUTOMOC ON)
set(CMAKE_INCLUDE_CURRENT_DIR ON)

FIND_PACKAGE(Qt5Core REQUIRED)
FIND_PACKAGE(Qt5Gui REQUIRED)
FIND_PACKAGE(Qt5Widgets REQUIRED)
FIND_PACKAGE(Qt5Svg REQUIRED)

include(FindOpenGL)

include_directories(${OPENGL_INCLUDE_DIR})

SET(glview_SOURCES main.cpp window.cpp glwindow.cpp glview3d.cpp)
SET(glview_HEADERS window.h glwindow.h glview.h)

add_executable(glview ${glview_SOURCES} ${glview_HEADERS_MOC} ${glview_RESOURCES})
target_link_libraries(glview tiff)
qt5_use_modules(glview Core Gui Widgets Svg)
target_link_libraries(glview ${OPENGL_gl_LIBRARY})
