/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import java.util.Arrays;
import java.util.Objects;


public class WidgetPreferenceActivity extends PreferenceFragmentCompat
{
    private static final String backStateName = WidgetPreferenceActivity.class.getName();

    private void addWidget(PreferenceScreen screen, int appWidgetId)
    {
        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        assert context != null;

        Preference widget = new Preference(context) {
            @Override
            public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
                super.onBindViewHolder(holder);
                holder.itemView.setOnLongClickListener(preference -> {
                    new AlertDialog.Builder(context)
                            .setMessage(R.string.activity_remove)
                            .setPositiveButton(R.string.activity_dialog_yes, (dialogInterface, i) -> {
                                WidgetUtil.deleteWidget(context, appWidgetId);
                                onCreatePreferences(null, null);
                            })
                            .setNegativeButton(R.string.activity_dialog_no, null)
                            .show();

                    return true;
                });
            }
        };
        widget.setKey("name." + appWidgetId);
        widget.setIcon(R.drawable.ic_widgets_24dp);
        widget.setTitle(WidgetUtil.getDisplayName(
                context,
                Objects.requireNonNull(prefs.getString("name." + appWidgetId, "")),
                appWidgetId
        ));
        widget.setSummaryProvider(preference -> WidgetUtil.getDisplayURL(
                context,
                Objects.requireNonNull(prefs.getString("url." + appWidgetId, ""))
        ));
        widget.setOnPreferenceClickListener(preference -> {
            WidgetPreferenceFragment fragment = new WidgetPreferenceFragment();
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            fragment.setArguments(fragmentArguments);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(backStateName)
                    .commit();

            return true;
        });

        screen.addPreference(widget);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        Context context = getContext();

        assert context != null;

        // Create new screen
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        // Add widgets
        for (int appWidgetId : WidgetUtil.getAppWidgetIds(context)) {
            this.addWidget(screen, appWidgetId);
        }

        // Update screen
        setPreferenceScreen(screen);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        LinearLayout view = (LinearLayout) super.onCreateView(
                inflater,
                container,
                savedInstanceState
        );

        assert view != null;

        Context context = getContext();
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        if (context == null || activity == null)
            return view;

        // Update title
        activity.setTitle(context.getResources().getString(R.string.activity_widgets));

        // Update widget titles
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference widget = screen.getPreference(i);
            String widgetKey = widget.getKey();  // name.<id>
            int appWidgetId = Integer.parseInt(widgetKey.substring(5));
            widget.setTitle(
                    WidgetUtil.getDisplayName(
                            context,
                            Objects.requireNonNull(prefs.getString(widgetKey, "")),
                            appWidgetId
                    )
            );
        }

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Check widgets was added and/or deleted
        PreferenceScreen screen = getPreferenceScreen();
        int count = screen.getPreferenceCount();
        int[] ids = new int[count];

        for (int i = 0; i < count; i++) {
            String key = screen.getPreference(i).getKey();
            ids[i] = Integer.parseInt(key.substring(5));  // name.<id>
        }

        Arrays.sort(ids);

        if (!Arrays.equals(ids, WidgetUtil.getAppWidgetIds(getContext())))
            onCreatePreferences(null, null);
    }
}
