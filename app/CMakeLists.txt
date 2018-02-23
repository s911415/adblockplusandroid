# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set (CMAKE_CXX_STANDARD 11)


set(CPP_DIR "${CMAKE_SOURCE_DIR}/src/main/cpp")

include_directories(${CPP_DIR}/libadblockplus-binaries/include)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


add_library( # Sets the name of the library.
             libadblockplus
             STATIC
             ${CPP_DIR}/libadblockplus-binaries/android_${ANDROID_ABI}/libadblockplus.a
)
SET_TARGET_PROPERTIES(libadblockplus PROPERTIES LINKER_LANGUAGE C)


add_library( # Sets the name of the library.
             v8-base
             STATIC
             ${CPP_DIR}/libadblockplus-binaries/android_${ANDROID_ABI}/libv8_base.a
)
SET_TARGET_PROPERTIES(v8-base PROPERTIES LINKER_LANGUAGE C)

add_library( # Sets the name of the library.
             v8-snapshot
             STATIC
             ${CPP_DIR}/libadblockplus-binaries/android_${ANDROID_ABI}/libv8_snapshot.a
)
SET_TARGET_PROPERTIES(v8-snapshot PROPERTIES LINKER_LANGUAGE C)

add_library( # Sets the name of the library.
             libadblockplus-jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${CPP_DIR}/JniJsEngine.cpp ${CPP_DIR}/JniFilterEngine.cpp ${CPP_DIR}/JniJsValue.cpp
             ${CPP_DIR}/JniFilter.cpp ${CPP_DIR}/JniSubscription.cpp ${CPP_DIR}/JniEventCallback.cpp
             ${CPP_DIR}/JniLogSystem.cpp ${CPP_DIR}/JniWebRequest.cpp
             ${CPP_DIR}/JniUpdateAvailableCallback.cpp ${CPP_DIR}/JniUpdateCheckDoneCallback.cpp
             ${CPP_DIR}/JniFilterChangeCallback.cpp ${CPP_DIR}/JniCallbacks.cpp ${CPP_DIR}/Utils.cpp
             ${CPP_DIR}/JniNotification.cpp ${CPP_DIR}/JniShowNotificationCallback.cpp

)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       libadblockplus-jni

                       # Links the target library to the log library
                       # included in the NDK.
                       libadblockplus v8-base v8-snapshot

)