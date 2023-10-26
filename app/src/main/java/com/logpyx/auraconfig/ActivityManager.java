package com.logpyx.auraconfig;

public class ActivityManager {
    private static DeviceControlActivity currentActivity;

    public static void setCurrentActivity(DeviceControlActivity activity) {
        currentActivity = activity;
    }

    public static DeviceControlActivity getCurrentActivity() {
        return currentActivity;
    }
}
