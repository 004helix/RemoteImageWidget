/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    @NonNull
    public static String getDisplayName(Context context, @Nullable String name, int appWidgetId)
    {
        return name == null || name.equals("") ?
                context.getResources().getString(R.string.settings_title_default_name) + appWidgetId:
                name;
    }

    @NonNull
    public static String getDisplayURL(Context context, @Nullable String url)
    {
        return url == null || url.equals("") ?
                context.getResources().getString(R.string.settings_title_default_url) :
                url;
    }

    @NonNull
    public static int[] getAppWidgetIds(Context context)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName name = new ComponentName(context, WidgetProvider.class);
        ArrayList<Integer> idList = new ArrayList<>();

        for (int appWidgetId : appWidgetManager.getAppWidgetIds(name))
            if (sp.getBoolean("configured." + appWidgetId, false))
                idList.add(appWidgetId);

        int[] ids = new int[idList.size()];
        int i = 0;

        for (Integer e : idList)
            ids[i++] = e;

        Arrays.sort(ids);

        return ids;
    }

    public static void deleteWidget(Context context, int appWidgetId, boolean force)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();

        // Clean scheduled update if any
        WidgetUpdate.scheduleCancel(context, new WidgetOptions(context, appWidgetId));

        // Remove preferences
        for (String key : widgetPrefs.keySet())
            edit.remove(key + "." + appWidgetId);

        edit.apply();

        // Remove it from host
        if (force)
            new AppWidgetHost(context, 1).deleteAppWidgetId(appWidgetId);
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

        if (oldVersion >= BuildConfig.VERSION_CODE)
            return;

        prefs.edit().putInt(VERSION_KEY, BuildConfig.VERSION_CODE).apply();

        Log.i("WidgetUtil", "canceled all pending alarms");
    }
}
