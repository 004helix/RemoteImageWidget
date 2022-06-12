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

import androidx.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class WidgetDownload
{
    private static String getUserAgent(Context context)
    {
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

    @WorkerThread
    public static void run(Context context,
                           int appWidgetId,
                           String url,
                           int layoutId,
                           boolean showToasts)
    {
        boolean success = false;
        Bitmap bitmap = null;
        String msg = "";
        int rc = -1;

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestProperty("User-Agent", getUserAgent(context));
            con.setConnectTimeout(10000);  // 10 sec
            con.setReadTimeout(30000);  // 30 sec
            rc = con.getResponseCode();
            con.connect();

            InputStream inputStream = con.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                msg = "not an image";
            } else {
                success = true;
            }
        } catch (MalformedURLException e) {
            msg = "malformed url";
        } catch (UnknownHostException e) {
            msg = "unknown host";
        } catch (SocketTimeoutException e) {
            msg = "connect/read timeout";
        } catch (ConnectException e) {
            msg = "unable to connect";
        } catch (FileNotFoundException e) {
            if (rc == 404) {
                msg = "file not found";
            } else {
                msg = "http response: " + rc;
            }
        } catch (Exception e) {
            msg = e.toString();
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Intent clickIntent = new Intent(context, WidgetProvider.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .setAction(WidgetProvider.APPWIDGET_CLICK);
        int pendingIntentFlags = 0;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, clickIntent, pendingIntentFlags
        );

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        views.setOnClickPendingIntent(R.id.appwidget_layout, pendingIntent);

        if (success) {
            views.setImageViewBitmap(R.id.appwidget_image, bitmap);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);

        if (showToasts) {
            String toastText;

            if (success) {
                toastText = context.getString(R.string.toast_updated);
            } else {
                toastText = context.getString(R.string.toast_update_failed) + " " + msg;
            }

            context.sendBroadcast(
                    new Intent(context, WidgetProvider.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            .putExtra(WidgetProvider.EXTRA_TOAST_TEXT, toastText)
                            .setAction(WidgetProvider.APPWIDGET_TOAST)
            );
        }

        if (success) {
            Log.i("WidgetDownload", "widget #" + appWidgetId + " updated");
        } else {
            Log.e("WidgetDownload", "widget #" + appWidgetId + " update failed: " + msg);
        }
    }
}
