/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import java.util.Arrays;
import java.util.Objects;


public class WidgetPreference
        extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener
{
    public static final String INITIAL = "initial";
    private boolean initial;
    private int appWidgetId;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        Context context = getContext();
        Bundle arguments = getArguments();

        assert context != null;
        assert arguments != null;

        // Create screen
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);

        // Get preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        // Get appWidgetId and initial status from arguments
        appWidgetId = arguments.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
        initial = arguments.getBoolean(INITIAL, false);

        // Add "Widget Name" preference
        EditTextPreference name = new EditTextPreference(context);
        name.setKey("name." + appWidgetId);
        name.setTitle(R.string.settings_title_name);
        name.setDialogTitle(R.string.settings_title_name);
        name.setDialogMessage(R.string.settings_title_name_description);
        name.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            editText.setMaxLines(1);
            editText.setSingleLine(true);
        });
        name.setIcon(R.drawable.ic_title_24dp);
        name.setOnPreferenceChangeListener(this);
        name.setSummaryProvider(preference -> WidgetUtil.getDisplayName(
                context,
                sp.getString(preference.getKey(), ""),
                appWidgetId
        ));
        screen.addPreference(name);

        // Add "Image URL" preference
        EditTextPreference url = new EditTextPreference(context);
        url.setKey("url." + appWidgetId);
        url.setTitle(R.string.settings_title_url);
        url.setDialogTitle(R.string.settings_title_url);
        url.setDialogMessage(R.string.settings_title_url_description);
        url.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            editText.setMaxLines(1);
            editText.setSingleLine(true);
        });
        url.setIcon(R.drawable.ic_image_24dp);
        url.setOnPreferenceChangeListener(this);
        url.setSummaryProvider(preference -> WidgetUtil.getDisplayURL(
                context,
                sp.getString(preference.getKey(), "")
        ));
        screen.addPreference(url);

        // Add "Refresh interval" preference
        ListPreference interval = new ListPreference(context);
        interval.setKey("interval." + appWidgetId);
        interval.setTitle(R.string.settings_title_interval);
        interval.setDefaultValue("-1");
        interval.setDialogTitle(R.string.settings_title_interval);
        interval.setEntries(R.array.settings_interval_titles);
        interval.setEntryValues(R.array.settings_interval_values);
        interval.setIcon(R.drawable.ic_sync_24dp);
        interval.setOnPreferenceChangeListener(this);
        interval.setSummaryProvider(preference -> {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(
                    sp.getString(preference.getKey(), "-1")
            );
            return index >= 0 ? listPreference.getEntries()[index] : null;
        });
        screen.addPreference(interval);

        // Add "Update over WiFi only" preference
        SwitchPreference wifi = new SwitchPreference(context);
        wifi.setKey("wifi." + appWidgetId);
        wifi.setTitle(R.string.settings_title_wifi);
        wifi.setDefaultValue(false);
        wifi.setIcon(R.drawable.ic_wifi_24dp);
        wifi.setOnPreferenceChangeListener(this);
        screen.addPreference(wifi);

        // Add "Scale image" preference
        SwitchPreference scale = new SwitchPreference(context);
        scale.setKey("scale." + appWidgetId);
        scale.setTitle(R.string.settings_title_scale);
        scale.setDefaultValue(true);
        scale.setIcon(R.drawable.ic_scale_24dp);
        scale.setOnPreferenceChangeListener(this);
        screen.addPreference(scale);

        // Add "Preserve aspect ratio" preference
        SwitchPreference aspect = new SwitchPreference(context);
        aspect.setKey("aspect." + appWidgetId);
        aspect.setTitle(R.string.settings_title_aspect);
        aspect.setDefaultValue(true);
        aspect.setIcon(R.drawable.ic_aspect_24dp);
        aspect.setOnPreferenceChangeListener(this);
        screen.addPreference(aspect);

        // Add scale->aspect dependency
        aspect.setDependency(scale.getKey());
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        Context context = getContext();
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        assert context != null;
        assert activity != null;

        // Set title
        activity.setTitle(context.getResources().getString(R.string.activity_widget) + appWidgetId);

        if (!initial) {
            super.onViewCreated(view, savedInstanceState);
            return;
        }

        activity.setResult(Activity.RESULT_CANCELED, new Intent()
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));

        // Add "create widget" button
        Button button = new Button(activity);
        button.setText(R.string.activity_create_button);
        button.setBackgroundColor(
                context.getResources().getColor(R.color.colorPrimaryDark)
        );
        button.setOnClickListener(v -> {
            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean("configured." + appWidgetId, true)
                    .apply();

            activity.setResult(Activity.RESULT_OK, new Intent()
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));

            activity.finish();
        });

        ((LinearLayout) view).addView(button);

        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value, update widget and (re)schedule alarm if interval
     * was changed.
     */
    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object value)
    {
        Context context = pref.getContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        WidgetOptions options = new WidgetOptions(context, appWidgetId);
        String key = pref.getKey();
        String val = value.toString();

        // Update widget name
        if (key.startsWith("name.") && !Objects.equals(sp.getString(key, ""), val))
            return true;

        // Update widget url
        if (key.startsWith("url.") && !Objects.equals(sp.getString(key, ""), val)) {
            WidgetUpdate.update(
                    context,
                    options.setUrl(val),
                    true
            );
            return true;
        }

        // Update widget refresh interval
        if (key.startsWith("interval.") && !Objects.equals(sp.getString(key, "-1"), val)) {
            WidgetUpdate.schedule(
                    context,
                    options.setInterval(Integer.parseInt(val))
            );
            return true;
        }

        // Update wifi setting
        if (key.startsWith("wifi.") && sp.getBoolean(key, false) != (Boolean) value) {
            WidgetUpdate.schedule(
                    context,
                    options.setWifi((Boolean) value)
            );
            return true;
        }

        // Update scale setting
        if (key.startsWith("scale.") && sp.getBoolean(key, true) != (Boolean) value) {
            WidgetUpdate.update(
                    context,
                    options.setScaleImage((Boolean) value),
                    true
            );
            return true;
        }

        // Update aspect setting
        if (key.startsWith("aspect.") && sp.getBoolean(key, true) != (Boolean) value) {
            WidgetUpdate.update(
                    context,
                    options.setPreserveAspectRatio((Boolean) value),
                    true
            );
            return true;
        }

        // Unknown pref
        return false;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (initial)
            return;

        // Check if widget was deleted
        if (Arrays.binarySearch(WidgetUtil.getAppWidgetIds(getContext()), appWidgetId) < 0)
            getParentFragmentManager().popBackStack();
    }
}
