
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := kernet
LOCAL_SDK_VERSION := 17
LOCAL_SRC_FILES := $(call all-java-files-under, src)

include $(BUILD_STATIC_JAVA_LIBRARY)

# Include this library in the build server's output directory
# TODO: Not yet.
#$(call dist-for-goals, dist_files, $(LOCAL_BUILT_MODULE):kernet.jar)

# Include build files in subdirectories
include $(call all-makefiles-under,$(LOCAL_PATH))

