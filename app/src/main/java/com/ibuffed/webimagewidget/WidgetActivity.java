/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;


public class WidgetActivity
        extends AppCompatActivity
        implements FragmentManager.OnBackStackChangedListener
{
    private static final String THEME_KEY = "theme";
    private static int theme = 0;

    private void applyTheme()
    {
        int mode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED;

        switch (theme) {
            case 0:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            case 1:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case 2:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }

        if (AppCompatDelegate.getDefaultNightMode() != mode)
            AppCompatDelegate.setDefaultNightMode(mode);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_theme) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String[] themes = {
                    this.getResources().getString(R.string.theme_auto),
                    this.getResources().getString(R.string.theme_light),
                    this.getResources().getString(R.string.theme_dark),
            };

            new AlertDialog.Builder(this)
                    .setTitle(R.string.menu_item_theme)
                    .setSingleChoiceItems(themes, theme, (dialog, i) -> {
                        sp.edit().putInt(THEME_KEY, theme = i).apply();
                        dialog.dismiss();
                        applyTheme();
                    })
                    .setNegativeButton(R.string.activity_dialog_cancel, (dialog, i) ->
                            theme = sp.getInt(THEME_KEY, 0)
                    )
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
        theme = PreferenceManager.getDefaultSharedPreferences(this).getInt(THEME_KEY, 0);
        applyTheme();

        // Auto back button
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        onBackStackChanged();

        // Set content view
        setContentView(R.layout.activity_main);

        // Check saved instance
        if (savedInstanceState != null)
            return;

        // Find the appWidgetId from the intent
        int appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        );

        // Display widget preference if appWidgetId is valid
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPreference fragment = new WidgetPreference();

            // Pass appWidgetId to WidgetPreferenceFragment
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            fragmentArguments.putBoolean(WidgetPreference.INITIAL, true);
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
        } else {
            // Display widget list
            fragmentManager.beginTransaction().replace(
                    android.R.id.content,
                    new WidgetList()
            ).commit();
        }
    }
}
