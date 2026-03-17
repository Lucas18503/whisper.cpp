cmake_minimum_required(VERSION 3.10)

project(whisper.cpp)

set(CMAKE_CXX_STANDARD 17)
set(WHISPER_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../../../)

file(READ "${WHISPER_LIB_DIR}/CMakeLists.txt" MAIN_CMAKE_CONTENT)
string(REGEX MATCH "project\\(\"whisper\\.cpp\" VERSION ([0-9]+\\.[0-9]+\\.[0-9]+)\\)" VERSION_MATCH "${MAIN_CMAKE_CONTENT}")
if(CMAKE_MATCH_1)
    set(WHISPER_VERSION ${CMAKE_MATCH_1})
else()
    set(WHISPER_VERSION "unknown")
endif()
message(STATUS "Whisper version: ${WHISPER_VERSION}")

find_library(LOG_LIB log)

include(FetchContent)

add_library(
    whisper
    SHARED
    ${WHISPER_LIB_DIR}/src/whisper.cpp
)

FetchContent_Declare(ggml SOURCE_DIR ${WHISPER_LIB_DIR}/ggml)
# Build ggml as static libraries so they get linked into libwhisper.so,
# producing a single self-contained shared library.
set(BUILD_SHARED_LIBS OFF)
FetchContent_MakeAvailable(ggml)

target_link_libraries(whisper ${LOG_LIB} android ggml)

# WHISPER_SHARED ensures whisper_* symbols keep default visibility even when
# -fvisibility=hidden is applied, so JNA can resolve them at runtime.
# WHISPER_BUILD marks this as the library build (not a consumer).
target_compile_definitions(whisper PUBLIC WHISPER_SHARED WHISPER_BUILD)
target_compile_definitions(whisper PRIVATE GGML_USE_CPU)
target_compile_definitions(whisper PRIVATE WHISPER_VERSION="${WHISPER_VERSION}")

if (NOT ${CMAKE_BUILD_TYPE} STREQUAL "Debug")
    target_compile_options(whisper PRIVATE -O3)
    target_compile_options(whisper PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
    target_compile_options(whisper PRIVATE -ffunction-sections -fdata-sections)
endif ()

include_directories(${WHISPER_LIB_DIR})
include_directories(${WHISPER_LIB_DIR}/src)
include_directories(${WHISPER_LIB_DIR}/include)
include_directories(${WHISPER_LIB_DIR}/ggml/include)
include_directories(${WHISPER_LIB_DIR}/ggml/src)
include_directories(${WHISPER_LIB_DIR}/ggml/src/ggml-cpu)
