/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class WidgetWorker extends Worker
{
    public WidgetWorker (@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super (context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork()
    {
        Data inputData = getInputData();

        WidgetDownload.run(
                getApplicationContext(),
                inputData.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                ),
                inputData.getString(
                        WidgetUtil.EXTRA_APPWIDGET_URL
                ),
                inputData.getInt(
                        WidgetUtil.EXTRA_APPWIDGET_LAYOUT_ID,
                        -1
                ),
                inputData.getBoolean(
                        WidgetUtil.EXTRA_APPWIDGET_SHOW_TOASTS,
                        false
                )
        );

        return Result.success();
    }
}
