/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.annotation.SuppressLint;
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
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


public class WidgetUtil
{
    public static final String VERSION_KEY = "version";

    private enum keyType {
        STRING,
        BOOLEAN
    }

    private static final Map<String, keyType> widgetPrefs = Map.of(
            "configured", keyType.BOOLEAN,
            "name", keyType.STRING,
            "url", keyType.STRING,
            "interval", keyType.STRING,
            "wifi", keyType.BOOLEAN,
            "scale", keyType.BOOLEAN,
            "aspect", keyType.BOOLEAN
        );

    public static String getDisplayName(Context context, String name, int appWidgetId)
    {
        return name.equals("") ?
                context.getResources().getString(R.string.settings_default_name) + appWidgetId:
                name;
    }

    public static String getDisplayURL(Context context, String url)
    {
        return url.equals("") ?
                context.getResources().getString(R.string.settings_default_url) :
                url;
    }

    public static int[] getAppWidgetIds(Context context)
    {
        AppWidgetHost appWidgetHost = new AppWidgetHost(context, 1); // for removing phantoms
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName name = new ComponentName(context, WidgetProvider.class);
        ArrayList<Integer> idList = new ArrayList<>();

        for (int appWidgetId : appWidgetManager.getAppWidgetIds(name)) {
            if (prefs.getBoolean("configured." + appWidgetId, false)) {
                // Save appWidgetId
                idList.add(appWidgetId);
            } else {
                // Remove phantom widget
                appWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }

        int[] ids = new int[idList.size()];
        int i = 0;

        for (Integer e : idList)
            ids[i++] = e;

        Arrays.sort(ids);

        return ids;
    }

    public static void deleteWidget(Context context, int appWidgetId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();

        // Clean scheduled update if any
        WidgetUpdate.scheduleCancel(context, new WidgetOptions(context, appWidgetId));

        // Remove preferences
        for (String key : widgetPrefs.keySet()) {
            edit.remove(key + "." + appWidgetId);
        }

        edit.apply();
    }

    public static void restoreWidget(Context context, int oldWidgetId, int newWidgetId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();

        for (Map.Entry<String, keyType> entry : widgetPrefs.entrySet()) {
            String oldKey = entry.getKey() + "." + oldWidgetId;
            String newKey = entry.getKey() + "." + newWidgetId;

            // Remap widget preferences
            if (prefs.contains(oldKey)) {
                switch (entry.getValue()) {
                    case STRING:
                        edit.putString(newKey, prefs.getString(oldKey, ""));
                        break;
                    case BOOLEAN:
                        edit.putBoolean(newKey, prefs.getBoolean(oldKey, false));
                        break;
                }
                edit.remove(oldKey);
            }
        }

        edit.apply();
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    public static void appUpdate(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int oldVersion = prefs.getInt(VERSION_KEY, 0);

        //oldVersion = 2;
        if (oldVersion >= BuildConfig.VERSION_CODE)
            return;

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (int appWidgetId : getAppWidgetIds(context)) {
            // TODO: remove this check (for 1.1 version)
            if (oldVersion <= 2) {
                alarmManager.cancel(
                        PendingIntent.getBroadcast(
                                context,
                                0,
                                new Intent(context, WidgetProvider.class)
                                        .setAction("appWidgetAlarm." + appWidgetId),
                                // TODO: flag = 0 prevents me to update targetSdkVersion > 30
                                0
                        )
                );
            }

            int flags = 0;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            alarmManager.cancel(
                    PendingIntent.getBroadcast(
                            context,
                            appWidgetId,
                            new Intent(context, WidgetProvider.class)
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    .setAction("com.ibuffed.webimagewidget.ALARM"),
                            flags
                    )
            );
        }

        prefs.edit().putInt(VERSION_KEY, BuildConfig.VERSION_CODE).apply();

        Log.i("WidgetUtil", "canceled all pending alarms");
    }
}
