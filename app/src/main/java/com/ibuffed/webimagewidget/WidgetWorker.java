/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;


public class WidgetWorker extends Worker
{
    private String message;
    private Bitmap bitmap;

    public WidgetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super (context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork()
    {
        Data inputData = getInputData();
        Context context = getApplicationContext();

        // Parse input data
        String url = inputData.getString(
                WidgetUpdate.EXTRA_APPWIDGET_URL
        );
        int appWidgetId = inputData.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        );
        int layoutId = inputData.getInt(
                WidgetUpdate.EXTRA_APPWIDGET_LAYOUT_ID,
                -1
        );
        boolean showToast = inputData.getBoolean(
                WidgetUpdate.EXTRA_APPWIDGET_SHOW_TOAST,
                false
        );

        // Check input data
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || layoutId == -1) {
            return Result.failure();
        }

        // Create remote views
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Download image from url
        if (download(url)) {
            views.setImageViewBitmap(R.id.appwidget_image, bitmap);
        }

        // Create pending intent
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                new Intent(context, WidgetProvider.class)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        .setAction(WidgetProvider.APPWIDGET_CLICK),
                pendingIntentFlags
        );

        // Set onClick action
        views.setOnClickPendingIntent(R.id.appwidget_layout, pendingIntent);

        // Update widget
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);

        // Show toast
        if (showToast) {
            String toastText;

            if (message == null) {
                toastText = context.getString(R.string.toast_updated);
            } else {
                toastText = context.getString(R.string.toast_update_failed) + " " + message;
            }

            context.sendBroadcast(
                    new Intent(context, WidgetProvider.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            .putExtra(WidgetProvider.EXTRA_TOAST_TEXT, toastText)
                            .setAction(WidgetProvider.APPWIDGET_TOAST)
            );
        }

        // Log result
        if (message == null) {
            log(appWidgetId, "updated");
        } else {
            log(appWidgetId, "update failed: " + message);
        }

        // Done
        return Result.success();
    }

    private void log(int appWidgetId, String message)
    {
        Log.i("WidgetWorker", "widget #" + appWidgetId + " " + message);
    }

    private String getUserAgent()
    {
        Context context = getApplicationContext();
        StringBuilder ua = new StringBuilder("WebImageWidget/");

        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            ua.append(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            ua.append("DEV");
        }

        return ua.append(" (Android/").append(Build.VERSION.RELEASE).append(")").toString();
    }

    private boolean download(String url)
    {
        int rc = -1;

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestProperty("User-Agent", getUserAgent());
            con.setConnectTimeout(10000);  // 10 sec
            con.setReadTimeout(30000);  // 30 sec
            rc = con.getResponseCode();
            con.connect();

            InputStream inputStream = con.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                message = "not an image";
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            message = "malformed url";
        } catch (UnknownHostException e) {
            message = "unknown host";
        } catch (SocketTimeoutException e) {
            message = "connect/read timeout";
        } catch (ConnectException e) {
            message = "unable to connect";
        } catch (FileNotFoundException e) {
            if (rc == 404) {
                message = "file not found";
            } else {
                message = "http response: " + rc;
            }
        } catch (Exception e) {
            message = e.toString();
        }

        return false;
    }
}
