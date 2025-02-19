package org.eclipse.paho.android.service;

import android.util.Log;

/**
 * Logging for MQTT classes
 */
public class MqttLog {
    public static String TAG = "MQTT";
    /**
     * Set to true to enable logging
     * Default is false
     */
    public static boolean isLoggable = false;

    public static void d(String tag, String msg) {
        if (isLoggable) {
            Log.d(TAG + "-" + tag, msg);
        }
    }
    
    public static void e(String tag, String msg) {
        if (isLoggable) {
            Log.e(TAG + "-" + tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (isLoggable) {
            Log.i(TAG + "-" + tag, msg);
        }
    }

    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }
}
