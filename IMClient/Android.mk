LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
$(warning LOCAL_SRC_FILES $(LOCAL_SRC_FILES) !!!)
$(warning LOCAL_RESOURCE_DIR $(LOCAL_RESOURCE_DIR) !!!)
LOCAL_PACKAGE_NAME := IMClient
LOCAL_CERTIFICATE := platform
#LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS := tests
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_STATIC_JAVA_LIBRARIES := EventBus \
                                    android-support-v4
LOCAL_JAVA_LIBRARIES += org.apache.http.legacy
LOCAL_SDK_VERSION := current
include $(BUILD_PACKAGE)



include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := EventBus:libs/EventBus.jar
LOCAL_MODULE_TAGS := optional
include $(BUILD_MULTI_PREBUILT)


