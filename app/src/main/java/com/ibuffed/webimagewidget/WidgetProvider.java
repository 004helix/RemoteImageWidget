/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;


public class WidgetProvider extends AppWidgetProvider
{
    final static String EXTRA_TOAST_TEXT = "toastText";
    final static String APPWIDGET_TOAST = "com.ibuffer.webimagewidget.TOAST";
    final static String APPWIDGET_CLICK = "com.ibuffed.webimagewidget.CLICK";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        WidgetUtil.appUpdate(context);
        for (int appWidgetId : appWidgetIds) {
            WidgetOptions options = new WidgetOptions(context, appWidgetId);
            WidgetUpdate.update(context, options, false);
            WidgetUpdate.schedule(context, options);
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

        //Log.d("WidgetProvider", "action: " + action + ", appWidgetId=" + appWidgetId);

        if (action == null) {
            return;
        }

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
                WidgetUpdate.update(
                        context,
                        new WidgetOptions(context, appWidgetId),
                        true
                );
            }
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
