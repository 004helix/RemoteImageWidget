/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.MultiDex;


public class ActivityDispatcher extends AppCompatActivity
{
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Perform app update
        WidgetUtil.appUpdate(this);

        // Find the appWidgetId from the intent
        int appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        );

        // Dispatch widget preference fragment if appWidgetId is valid
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPreferenceFragment fragment = new WidgetPreferenceFragment();

            // Pass appWidgetId to WidgetPreferenceFragment
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            fragmentArguments.putBoolean(WidgetPreferenceFragment.INITIAL, true);
            fragment.setArguments(fragmentArguments);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_CANCELED, resultValue);

            // Show WidgetPreferenceFragment
            getSupportFragmentManager().beginTransaction().replace(
                    android.R.id.content,
                    fragment
            ).commit();

            return;
        }

        // Find all configured widgets
        int[] appWidgetIds = WidgetUtil.getAppWidgetIds(this);

        // Dispatch widget preference activity
        if (appWidgetIds.length > 0) {
            // Show WidgetPreferenceActivity
            getSupportFragmentManager().beginTransaction().replace(
                    android.R.id.content,
                    new WidgetPreferenceActivity()
            ).commit();

            return;
        }

        // No widgets found
        setTitle(R.string.activity_widgets);
        setContentView(R.layout.activity_empty);
    }
}
