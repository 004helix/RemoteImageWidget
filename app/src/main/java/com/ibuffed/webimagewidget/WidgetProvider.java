/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/**
 * Implementation of App Widget functionality.
 */
public class WidgetProvider extends AppWidgetProvider
{
    final static String EXTRA_TOAST_TEXT = "toastText";
    final static String APPWIDGET_TOAST = "com.ibuffer.webimagewidget.TOAST";
    final static String APPWIDGET_CLICK = "com.ibuffed.webimagewidget.CLICK";
    final static String APPWIDGET_ALARM = "com.ibuffed.webimagewidget.ALARM";
    final static String APPWIDGET_UPDATE = "com.ibuffed.webimagewidget.UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        for (int appWidgetId : appWidgetIds) {
            WidgetUtil.scheduleUpdate(context, appWidgetId);
            WidgetUtil.updateWidget(
                    context,
                    new WidgetOptions(context, appWidgetId)
            );
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        String action = intent.getAction();
        int appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        );

        //Log.e("Widget Provider", "action: " + action + ", appWidgetId=" + appWidgetId);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }

        // Show toast
        if (action.equals(APPWIDGET_TOAST)) {
            String toast = intent.getStringExtra(EXTRA_TOAST_TEXT);
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
        }

        // Update widget on double click
        if (action.equals(APPWIDGET_CLICK)) {
            if (WidgetClickTracker.getInstance().doubleClicked(appWidgetId)) {
                WidgetUtil.updateWidget(
                        context,
                        new WidgetOptions(context, appWidgetId),
                        true
                );
            }
        }

        // Update widget on alarm
        if (action.equals(APPWIDGET_ALARM)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean refresh = true;

            if (prefs.getBoolean("wifi." + appWidgetId, false)) {
                ConnectivityManager connManager = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

                refresh = (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) &&
                        activeNetwork.isConnected();
            }

            if (refresh) {
                WidgetUtil.updateWidget(context, new WidgetOptions(context, appWidgetId));
            }
        }

        // Force update by settings change
        if (action.equals(APPWIDGET_UPDATE)) {
            WidgetOptions options = new WidgetOptions(context, appWidgetId);

            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    switch (key) {
                        case "url":
                            options.setUrl(extras.getString("url"));
                            break;
                        case "scale":
                            options.setScaleImage(extras.getBoolean("scale"));
                            break;
                        case "aspect":
                            options.setPreserveAspectRatio(extras.getBoolean("aspect"));
                            break;
                    }
                }
            }

            WidgetUtil.updateWidget(context, options, true);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds)
    {
        for (int appWidgetId : appWidgetIds) {
            WidgetUtil.deleteWidget(context, appWidgetId);
        }
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds)
    {
        for (int i = 0; i < oldWidgetIds.length; i++) {
            WidgetUtil.restoreWidget(context, oldWidgetIds[i], newWidgetIds[i]);
        }
    }
}
