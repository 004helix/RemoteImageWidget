/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;


public class ActivityDispatcher
        extends AppCompatActivity
        implements FragmentManager.OnBackStackChangedListener
{
    private static int theme = 0;

    private void applyTheme()
    {
        switch (theme) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        getSupportFragmentManager().popBackStack(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
        );
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_theme) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String[] themes = {
                    this.getResources().getString(R.string.theme_auto),
                    this.getResources().getString(R.string.theme_light),
                    this.getResources().getString(R.string.theme_dark),
            };

            new AlertDialog.Builder(this)
                    .setTitle(R.string.menu_item_theme)
                    .setSingleChoiceItems(themes, theme, (dialogInterface, i) -> theme = i)
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        prefs.edit().putInt("theme", theme).apply();
                        applyTheme();
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) ->
                            theme = prefs.getInt("theme", 0))
                    .show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackStackChanged()
    {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(
                getSupportFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Perform app update
        WidgetUtil.appUpdate(this);

        // Apply selected theme
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        theme = prefs.getInt("theme", 0);
        applyTheme();

        // Auto back button
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

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
            fragmentManager.beginTransaction().replace(
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
            fragmentManager.beginTransaction().replace(
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
