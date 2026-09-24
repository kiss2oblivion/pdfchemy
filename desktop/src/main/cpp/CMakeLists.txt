cmake_minimum_required(VERSION 3.15)
project(ArkhamLauncher)

set(CMAKE_CXX_STANDARD 17)

if(WIN32)
    add_executable(arkham-launcher arkham-launcher.cpp)
    target_link_libraries(arkham-launcher PRIVATE Userenv Bcrypt Advapi32)
elseif(UNIX AND NOT APPLE)
    add_executable(arkham-launcher-linux arkham-launcher-linux.cpp)
endif()

