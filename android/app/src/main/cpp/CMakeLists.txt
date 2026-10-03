cmake_minimum_required(VERSION 3.22)
project(fieldtalk_asr LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/whisper.cpp
                 ${CMAKE_CURRENT_BINARY_DIR}/whisper)

add_library(fieldtalk_asr SHARED fieldtalk_asr.cpp)
target_include_directories(fieldtalk_asr PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/whisper.cpp/include)
target_link_libraries(fieldtalk_asr PRIVATE whisper log)
