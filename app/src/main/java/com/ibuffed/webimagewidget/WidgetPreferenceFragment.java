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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;


/**
 * This fragment shows widget settings.
 */
public class WidgetPreferenceFragment extends PreferenceFragmentCompat
{
    public static final String INITIAL = "initial";
    private boolean initial;
    private int appWidgetId;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        // Create new PreferenceScreen
        Context context = getContext();
        Activity activity = getActivity();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);

        // Load preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Get appWidgetId and initial status from arguments
        appWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
        initial = getArguments().getBoolean(INITIAL, false);

        // Set title
        activity.setTitle(context.getResources().getString(R.string.activity_widget) + appWidgetId);

        // Widget Name
        EditTextPreference name = new EditTextPreference(context);
        name.setKey("name." + appWidgetId);
        name.setTitle(R.string.settings_title_name);
        name.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            editText.setMaxLines(1);
            editText.setSingleLine(true);
        });
        name.setIcon(R.drawable.ic_title_black_24dp);
        name.setOnPreferenceChangeListener(onPrefChange);
        updatePrefSummary(name, WidgetUtil.getDisplayName(
                context, prefs.getString(name.getKey(), ""), appWidgetId
        ));
        screen.addPreference(name);

        // Image URL
        EditTextPreference url = new EditTextPreference(context);
        url.setKey("url." + appWidgetId);
        url.setTitle(R.string.settings_title_url);
        url.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            editText.setMaxLines(1);
            editText.setSingleLine(true);
        });
        url.setIcon(R.drawable.ic_image_black_24dp);
        url.setOnPreferenceChangeListener(onPrefChange);
        updatePrefSummary(url, WidgetUtil.getDisplayURL(
                context, prefs.getString(url.getKey(), "")
        ));
        screen.addPreference(url);

        // Refresh interval
        ListPreference interval = new ListPreference(context);
        interval.setKey("interval." + appWidgetId);
        interval.setTitle(R.string.settings_title_interval);
        interval.setEntries(R.array.settings_interval_titles);
        interval.setEntryValues(R.array.settings_interval_values);
        interval.setIcon(R.drawable.ic_sync_black_24dp);
        interval.setOnPreferenceChangeListener(onPrefChange);
        updatePrefSummary(interval, prefs.getString(interval.getKey(), "-1"));
        screen.addPreference(interval);

        // Update over WiFi only
        SwitchPreference wifi = new SwitchPreference(context);
        wifi.setKey("wifi." + appWidgetId);
        wifi.setTitle(R.string.settings_title_wifi);
        wifi.setDefaultValue(false);
        wifi.setIcon(R.drawable.ic_wifi_black_24dp);
        screen.addPreference(wifi);

        // Scale image
        SwitchPreference scale = new SwitchPreference(context);
        scale.setKey("scale." + appWidgetId);
        scale.setTitle(R.string.settings_title_scale);
        scale.setDefaultValue(true);
        scale.setIcon(R.drawable.ic_scale_black_24dp);
        scale.setOnPreferenceChangeListener(onPrefChange);
        screen.addPreference(scale);

        // Preserve aspect ratio
        SwitchPreference aspect = new SwitchPreference(context);
        aspect.setKey("aspect." + appWidgetId);
        aspect.setTitle(R.string.settings_title_aspect);
        aspect.setDefaultValue(true);
        aspect.setIcon(R.drawable.ic_aspect_black_24dp);
        aspect.setOnPreferenceChangeListener(onPrefChange);
        screen.addPreference(aspect);

        // Dependencies
        aspect.setDependency(scale.getKey());
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        LinearLayout view = (LinearLayout) super.onCreateView(inflater,
                container, savedInstanceState);

        if (!initial) {
            return view;
        }

        Button button = new Button(getActivity());
        button.setText(R.string.activity_create_button);
        button.setOnClickListener(v -> {
            Activity activity = getActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            activity.setResult(Activity.RESULT_OK, resultValue);

            prefs.edit().putBoolean("configured." + appWidgetId, true).apply();

            activity.finish();
        });
        view.addView(button);

        return view;
    }

    /**
     * Update the preference's summary to reflect its new value.
     */
    private static void updatePrefSummary(Preference pref, Object value)
    {
        String stringValue = value.toString();

        if (pref instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) pref;
            int index = listPreference.findIndexOfValue(stringValue);

            pref.setSummary(index >= 0 ? (String) listPreference.getEntries()[index] : null);
            return;
        }

        pref.setSummary(stringValue);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value and reschedule alarm for widget if interval changed.
     */
    private final Preference.OnPreferenceChangeListener
            onPrefChange = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference pref, Object value) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(pref.getContext());
            String key = pref.getKey();
            String val = value.toString();

            // Update widget name
            if (key.startsWith("name.") && !prefs.getString(key, "").equals(val)) {
                updatePrefSummary(pref, WidgetUtil.getDisplayName(
                        pref.getContext(), val, appWidgetId));
                return true;
            }

            // Update widget url
            if (key.startsWith("url.") && !prefs.getString(key, "").equals(val)) {
                WidgetUtil.updateURL(pref.getContext(), appWidgetId, val);
                updatePrefSummary(pref, WidgetUtil.getDisplayURL(
                        pref.getContext(), val));
                return true;
            }

            // Update widget refresh interval
            if (key.startsWith("interval.") && !prefs.getString(key, "-1").equals(val)) {
                WidgetUtil.scheduleUpdate(pref.getContext(), appWidgetId, Long.parseLong(val));
                updatePrefSummary(pref, value);
                return true;
            }

            // Update wifi setting
            if (key.startsWith("wifi.") && prefs.getBoolean(key, true) != (Boolean) value) {
                return true;
            }

            // Update scale setting
            if (key.startsWith("scale.") && prefs.getBoolean(key, true) != (Boolean) value) {
                WidgetUtil.updateScale(pref.getContext(), appWidgetId, (Boolean) value);
                return true;
            }

            // Update aspect setting
            if (key.startsWith("aspect.") && prefs.getBoolean(key, true) != (Boolean) value) {
                WidgetUtil.updateAspect(pref.getContext(), appWidgetId, (Boolean) value);
                return true;
            }

            // Unknown options
            return false;
        }
    };
}
