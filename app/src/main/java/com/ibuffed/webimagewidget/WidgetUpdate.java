/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class WidgetUpdate
{
    final static String SCHEDULE_WORKER_TAG = "PeriodicWorker";
    final static String UPDATE_WORKER_TAG = "OneTimeWorker";

    final static String EXTRA_APPWIDGET_URL = "appWidgetURL";
    final static String EXTRA_APPWIDGET_LAYOUT_ID = "appWidgetLayoutId";
    final static String EXTRA_APPWIDGET_SHOW_TOAST = "appWidgetShowToast";

    public static void log(int appWidgetId, String msg)
    {
        Log.i("WidgetUpdate", "widget #" + appWidgetId + " " + msg);
    }

    public static class DummyWorker extends Worker
    {
        final static String DUMMY_WORK_NAME = "DummyWork";

        public DummyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
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
                            .setInitialDelay(10L * 365L, TimeUnit.DAYS)
                            .build()
            );
        }
    }

    /*
     * workaround
     * https://issuetracker.google.com/issues/115575872
     * https://commonsware.com/blog/2018/11/24/workmanager-app-widgets-side-effects.html
     */
    @NonNull
    public static WorkManager getWorkManager(Context context)
    {
        WorkManager workManager = WorkManager.getInstance(context);
        boolean dummyRunning = false;

        try {
            List<WorkInfo> workInfoList =
                    workManager.getWorkInfosForUniqueWork(DummyWorker.DUMMY_WORK_NAME).get();
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

        if (!dummyRunning)
            DummyWorker.schedule(context);

        return workManager;
    }

    public static void scheduleCancel(Context context, @NonNull WidgetOptions options)
    {
        getWorkManager(context)
                .cancelAllWorkByTag(SCHEDULE_WORKER_TAG + "#" + options.getAppWidgetId());

        log(options.getAppWidgetId(), "any scheduled update was canceled");
    }

    public static void schedule(Context context, @NonNull WidgetOptions options)
    {
        WorkManager workManager = getWorkManager(context);
        String tag = SCHEDULE_WORKER_TAG + "#" + options.getAppWidgetId();

        workManager.cancelAllWorkByTag(tag);

        if (options.getInterval() > 0) {
            Data.Builder data = new Data.Builder();
            data.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, options.getAppWidgetId());
            data.putString(EXTRA_APPWIDGET_URL, options.getUrl());
            data.putInt(EXTRA_APPWIDGET_LAYOUT_ID, options.getLayoutId());
            data.putBoolean(EXTRA_APPWIDGET_SHOW_TOAST, false);

            NetworkType networkType =
                    options.getWifi() ? NetworkType.UNMETERED : NetworkType.CONNECTED;
            Constraints constraints =
                    new Constraints.Builder().setRequiredNetworkType(networkType).build();
            PeriodicWorkRequest.Builder periodicWorkRequestBuilder =
                    new PeriodicWorkRequest.Builder(
                            WidgetWorker.class,
                            options.getInterval(),
                            TimeUnit.MINUTES,
                            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                            TimeUnit.MILLISECONDS
                    );

            workManager.enqueue(
                    periodicWorkRequestBuilder
                            .setConstraints(constraints)
                            .setInputData(data.build())
                            .addTag(tag)
                            .build()
            );

            log(options.getAppWidgetId(),
                    "scheduled update every " + options.getInterval() + " minutes");
        } else {
            log(options.getAppWidgetId(), "any scheduled update was canceled");
        }
    }

    public static void update(Context context, @NonNull WidgetOptions options, boolean showToast)
    {
        Data.Builder data = new Data.Builder();
        data.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, options.getAppWidgetId());
        data.putString(EXTRA_APPWIDGET_URL, options.getUrl());
        data.putInt(EXTRA_APPWIDGET_LAYOUT_ID, options.getLayoutId());
        data.putBoolean(EXTRA_APPWIDGET_SHOW_TOAST, showToast);

        Constraints.Builder constraints = new Constraints.Builder();
        if (!showToast)
            constraints.setRequiredNetworkType(NetworkType.CONNECTED);

        getWorkManager(context).enqueue(
                new OneTimeWorkRequest.Builder(WidgetWorker.class)
                        .addTag(UPDATE_WORKER_TAG + "#" + options.getAppWidgetId())
                        .setConstraints(constraints.build())
                        .setInputData(data.build())
                        .build()
        );
    }
}
