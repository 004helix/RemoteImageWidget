/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


class WidgetUtil
{
    private static final String[] widgetPrefKeys = {
            "configured", "name", "url", "interval",
            "wifi", "scale", "aspect"
    };

    private static final Class[] widgetPrefClasses = {
            boolean.class, String.class, String.class, String.class,
            boolean.class, boolean.class, boolean.class
    };

    final static String EXTRA_APPWIDGET_URL = "appWidgetURL";
    final static String EXTRA_APPWIDGET_LAYOUT_ID = "appWidgetLayoutId";
    final static String EXTRA_APPWIDGET_SHOW_TOASTS = "appWidgetShowToasts";

    public static String getDisplayName(Context context, String name, int appWidgetId)
    {
        if (name.equals("")) {
            return context.getResources().getString(R.string.settings_default_name) + appWidgetId;
        } else {
            return name;
        }
    }

    public static String getDisplayURL(Context context, String url)
    {
        return url.equals("") ? "http://" : url;
    }

    public static int[] getAppWidgetIds(Context context)
    {
        AppWidgetHost appWidgetHost = new AppWidgetHost(context, 1); // for removing phantoms
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName name = new ComponentName(context, WidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(name);
        int configuredCount = 0;

        for (int i = 0; i < appWidgetIds.length; i++) {
            if (prefs.getBoolean("configured." + appWidgetIds[i], false)) {
                configuredCount++;
            } else {
                // Remove phantom widget
                appWidgetHost.deleteAppWidgetId(appWidgetIds[i]);
                appWidgetIds[i] = AppWidgetManager.INVALID_APPWIDGET_ID;
            }
        }

        int[] configuredIds = new int[configuredCount];
        int i = 0;

        for (int appWidgetId : appWidgetIds) {
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                configuredIds[i++] = appWidgetId;
        }

        Arrays.sort(configuredIds);

        return configuredIds;
    }

    public static void deleteWidget(Context context, int appWidgetId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();

        // Clean update alarm
        clearUpdate(context, appWidgetId);

        // Remove preferences
        for (String key : widgetPrefKeys) {
            edit.remove(key + "." + appWidgetId);
        }

        edit.apply();
    }

    public static void restoreWidget(Context context, int oldWidgetId, int newWidgetId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();

        // Remap widget preferences
        for (int key = 0; key < widgetPrefKeys.length; key++) {
            String oldKey = widgetPrefKeys[key] + "." + oldWidgetId;
            String newKey = widgetPrefKeys[key] + "." + newWidgetId;

            if (prefs.contains(oldKey)) {
                if (widgetPrefClasses[key].equals(String.class)) {
                    edit.putString(newKey, prefs.getString(oldKey, ""));
                } else if (widgetPrefClasses[key].equals(int.class)) {
                    edit.putInt(newKey, prefs.getInt(oldKey, 0));
                } else if (widgetPrefClasses[key].equals(float.class)) {
                    edit.putFloat(newKey, prefs.getFloat(oldKey, 0));
                } else if (widgetPrefClasses[key].equals(boolean.class)) {
                    edit.putBoolean(newKey, prefs.getBoolean(oldKey, false));
                }
                edit.remove(oldKey);
            }
        }

        edit.apply();
    }

    private static PendingIntent getAlarmPendingIntent(Context context, int appWidgetId)
    {
        int flags = 0;
        Intent intent = new Intent(context, WidgetProvider.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .setAction(WidgetProvider.APPWIDGET_ALARM);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, appWidgetId, intent, flags);
    }

    public static void clearUpdate(Context context, int appWidgetId)
    {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getAlarmPendingIntent(context, appWidgetId));
    }

    public static void scheduleUpdate(Context context, int appWidgetId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        scheduleUpdate(context, appWidgetId, Long.parseLong(
                prefs.getString("interval." + appWidgetId, "-1")
        ));
    }

    public static void scheduleUpdate(Context context, int appWidgetId, long interval)
    {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmPendingIntent(context, appWidgetId);

        // Cancel current alarm if any
        alarmManager.cancel(pendingIntent);

        // Schedule new alarm
        if (interval > 0) {
            alarmManager.setInexactRepeating(AlarmManager.RTC,
                    System.currentTimeMillis(), interval * 60 * 1000, pendingIntent
            );
        }
    }

    private static Intent getUpdateIntent(Context context, int appWidgetId)
    {
        return new Intent(context, WidgetProvider.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .setAction(WidgetProvider.APPWIDGET_UPDATE);
    }

    public static void updateURL(Context context, int appWidgetId, String url)
    {
        context.sendBroadcast(
                getUpdateIntent(context, appWidgetId).putExtra("url", url)
        );
    }

    public static void updateScale(Context context, int appWidgetId, boolean scale)
    {
        context.sendBroadcast(
                getUpdateIntent(context, appWidgetId).putExtra("scale", scale)
        );
    }

    public static void updateAspect(Context context, int appWidgetId, boolean aspect)
    {
        context.sendBroadcast(
                getUpdateIntent(context, appWidgetId).putExtra("aspect", aspect)
        );
    }

    public static void updateWidget(Context context, WidgetOptions options)
    {
        updateWidget(context, options, false);
    }

    public static void updateWidget(Context context, WidgetOptions options, boolean showToasts)
    {
        Data.Builder data = new Data.Builder();
        data.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, options.getAppWidgetId());
        data.putString(EXTRA_APPWIDGET_URL, options.getUrl());
        data.putInt(EXTRA_APPWIDGET_LAYOUT_ID, options.getLayoutId());
        data.putBoolean(EXTRA_APPWIDGET_SHOW_TOASTS, showToasts);

        getWorkManager(context).enqueue(
                new OneTimeWorkRequest.Builder(WidgetWorker.class)
                        .setInputData(data.build())
                        .addTag("WidgetUpdate")
                        .build()
        );
    }

    /*
     * workaround
     * https://issuetracker.google.com/issues/115575872
     * https://commonsware.com/blog/2018/11/24/workmanager-app-widgets-side-effects.html
     */
    public static WorkManager getWorkManager(Context context)
    {
        WorkManager workManager = WorkManager.getInstance(context);
        boolean dummyRunning = false;

        try {
            List<WorkInfo> workInfoList =
                    workManager.getWorkInfosByTag(DummyWorker.DUMMY_WORKER_TAG).get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                dummyRunning = state == WorkInfo.State.RUNNING |
                        state == WorkInfo.State.ENQUEUED;
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!dummyRunning) {
            DummyWorker.schedule(context);
        }

        return workManager;
    }

    public static class DummyWorker extends Worker
    {
        final static String DUMMY_WORKER_TAG = "DummyWorker";
        final static String DUMMY_WORK_NAME = "DummyWork";

        public DummyWorker (@NonNull Context context, @NonNull WorkerParameters workerParams)
        {
            super (context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork()
        {
            schedule(getApplicationContext());
            return Result.success();
        }

        @AnyThread
        public static void schedule(Context context)
        {
            WorkManager.getInstance(context).enqueueUniqueWork(
                    DUMMY_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    new OneTimeWorkRequest.Builder(DummyWorker.class)
                            .addTag(DUMMY_WORKER_TAG)
                            .setInitialDelay(10L * 365L, TimeUnit.DAYS)
                            .build()
            );
        }
    }
}
