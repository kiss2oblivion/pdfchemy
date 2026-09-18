cmake_minimum_required(VERSION 3.15)
project(ArkhamLauncher)

set(CMAKE_CXX_STANDARD 17)

add_executable(arkham-launcher arkham-launcher.cpp)

target_link_libraries(arkham-launcher PRIVATE Userenv Bcrypt Advapi32)

add_executable(hello hello.cpp)
