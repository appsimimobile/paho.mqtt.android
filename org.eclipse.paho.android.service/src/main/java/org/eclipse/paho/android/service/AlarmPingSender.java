/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.android.service;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
    // Identifier for Intents, log messages, etc..
    private static final String TAG = "AlarmPingSender";

    private ClientComms comms;
    private final MqttService service;
    private volatile boolean hasStarted = false;
    private HandlerThread mThread;
    private Handler mHandler;

    public AlarmPingSender(MqttService service) {
        if (service == null) {
            throw new IllegalArgumentException(
                    "Neither service nor client can be null.");
        }
        this.service = service;
    }

    @Override
    public void init(ClientComms comms) {
        this.comms = comms;

        quitSafelyThread();
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public void start() {
        MqttLog.d(TAG, "Register scheduledRunnable to MqttService");
        schedule(comms.getKeepAlive());
        hasStarted = true;
    }

    @Override
    public void stop() {
        MqttLog.d(TAG, "Unregister scheduledRunnable to MqttService");
        if (hasStarted) {
            quitSafelyThread();
            hasStarted = false;
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        MqttLog.e(TAG, "schedule delayInMilliseconds = " + delayInMilliseconds);

        wakeupThread();
        mHandler.postDelayed(mRunnable, delayInMilliseconds);
    }

    private void quitSafelyThread() {
        if (mThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mThread.quitSafely();
            } else {
                mThread.quit();
            }
        }
    }

    private void wakeupThread() {
        if (mThread.getState() == Thread.State.TIMED_WAITING) {
            mThread.getLooper().getThread().interrupt(); // wakeup the thread if it is in sleep.
            MqttLog.e(TAG, "Interrupt: handlerThread Id = " + mThread.getThreadId());
        }
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            sendPing();
        }
    };

    private synchronized void sendPing() {
        MqttLog.d(TAG, "Sending Ping at:" + System.currentTimeMillis());

        // Assign new callback to token to execute code after PingResq
        // arrives. Get another wakelock even receiver already has one,
        // release it until ping response returns.
        comms.checkForActivity(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                MqttLog.d(TAG, "Success. " + System.currentTimeMillis());
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken,
                                  Throwable exception) {
                MqttLog.d(TAG, "Failure. " + System.currentTimeMillis());
            }
        });
    }
}
