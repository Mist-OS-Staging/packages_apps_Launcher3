/*
 * Copyright (C) 2021-2026 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.lunaris;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class OmniJawsClient {
    private static final String TAG = "OmniJawsClientBridge";
    public static final int EXTRA_ERROR_DISABLED = -100;

    public static class WeatherInfo {
        public int conditionCode;
        public String temp;
        public String tempUnits;
        public String city;
        public String condition;
    }

    public interface OmniJawsObserver {
        void weatherUpdated();
        void weatherError(int errorReason);
        void updateSettings();
    }

    private static OmniJawsClient sInstance;
    private Object mTargetClient;
    private Class<?> mTargetClass;

    public static synchronized OmniJawsClient get() {
        if (sInstance == null) {
            sInstance = new OmniJawsClient();
        }
        return sInstance;
    }

    public OmniJawsClient() {
        String[] candidates = new String[]{
            "com.android.internal.util.mist.OmniJawsClient",
            "com.android.internal.util.crdroid.OmniJawsClient",
            "org.omnirom.omnijaws.OmniJawsClient"
        };
        for (String candidate : candidates) {
            try {
                Class<?> clazz = Class.forName(candidate);
                try {
                    Method getMethod = clazz.getMethod("get");
                    mTargetClient = getMethod.invoke(null);
                } catch (NoSuchMethodException e) {
                    mTargetClient = clazz.getDeclaredConstructor().newInstance();
                }
                mTargetClass = clazz;
                Log.i(TAG, "Initialized OmniJawsClient bridge with " + candidate);
                break;
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean isOmniJawsEnabled(Context context) {
        if (mTargetClient == null) return false;
        try {
            Method m = mTargetClass.getMethod("isOmniJawsEnabled", Context.class);
            return (boolean) m.invoke(mTargetClient, context);
        } catch (Throwable t) {
            Log.w(TAG, "isOmniJawsEnabled failed", t);
            return false;
        }
    }

    public void queryWeather(Context context) {
        if (mTargetClient == null) return;
        try {
            Method m = mTargetClass.getMethod("queryWeather", Context.class);
            m.invoke(mTargetClient, context);
        } catch (Throwable t) {
            Log.w(TAG, "queryWeather failed", t);
        }
    }

    public WeatherInfo getWeatherInfo() {
        if (mTargetClient == null) return null;
        try {
            Method m = mTargetClass.getMethod("getWeatherInfo");
            Object targetInfo = m.invoke(mTargetClient);
            if (targetInfo == null) return null;
            WeatherInfo info = new WeatherInfo();
            Class<?> infoClz = targetInfo.getClass();
            Field ccField = infoClz.getField("conditionCode");
            info.conditionCode = ccField.getInt(targetInfo);
            Field tempField = infoClz.getField("temp");
            info.temp = (String) tempField.get(targetInfo);
            Field tuField = infoClz.getField("tempUnits");
            info.tempUnits = (String) tuField.get(targetInfo);
            Field cityField = infoClz.getField("city");
            info.city = (String) cityField.get(targetInfo);
            Field cField = infoClz.getField("condition");
            info.condition = (String) cField.get(targetInfo);
            return info;
        } catch (Throwable t) {
            Log.w(TAG, "getWeatherInfo failed", t);
            return null;
        }
    }

    public Drawable getWeatherConditionImage(Context context, int conditionCode) {
        if (mTargetClient == null) return null;
        try {
            Method m = mTargetClass.getMethod("getWeatherConditionImage", Context.class, int.class);
            return (Drawable) m.invoke(mTargetClient, context, conditionCode);
        } catch (Throwable t) {
            Log.w(TAG, "getWeatherConditionImage failed", t);
            return null;
        }
    }

    public void addObserver(Context context, OmniJawsObserver observer) {
        if (mTargetClient == null) return;
        try {
            Class<?> obsClz = null;
            for (Class<?> inner : mTargetClass.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("OmniJawsObserver")) {
                    obsClz = inner;
                    break;
                }
            }
            if (obsClz != null) {
                InvocationHandler handler = (proxy, method, args) -> {
                    String name = method.getName();
                    if ("weatherUpdated".equals(name)) {
                        observer.weatherUpdated();
                    } else if ("weatherError".equals(name)) {
                        observer.weatherError((int) args[0]);
                    } else if ("updateSettings".equals(name)) {
                        observer.updateSettings();
                    }
                    return null;
                };
                Object proxy = Proxy.newProxyInstance(obsClz.getClassLoader(), new Class<?>[]{obsClz}, handler);
                Method add = mTargetClass.getMethod("addObserver", Context.class, obsClz);
                add.invoke(mTargetClient, context, proxy);
            }
        } catch (Throwable t) {
            Log.w(TAG, "addObserver failed", t);
        }
    }

    public void removeObserver(Context context, OmniJawsObserver observer) {
        if (mTargetClient == null) return;
        try {
            Class<?> obsClz = null;
            for (Class<?> inner : mTargetClass.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("OmniJawsObserver")) {
                    obsClz = inner;
                    break;
                }
            }
            if (obsClz != null) {
                Method remove = mTargetClass.getMethod("removeObserver", Context.class, obsClz);
                remove.invoke(mTargetClient, context, null);
            }
        } catch (Throwable t) {
            try {
                Method remove = mTargetClass.getMethod("removeObserver", Context.class);
                remove.invoke(mTargetClient, context);
            } catch (Throwable ignored) {
            }
        }
    }
}

