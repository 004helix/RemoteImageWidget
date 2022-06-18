/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import java.util.Arrays;


public class WidgetList extends PreferenceFragmentCompat
{
    private static final String backStateName = WidgetList.class.getName();

    private void updateActivity()
    {
        Context context = getContext();
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        assert context != null;
        assert activity != null;

        activity.setTitle(context.getResources().getString(R.string.activity_widgets));
        TextView textView = (TextView) activity.findViewById(R.id.center_text);

        if (textView == null)
            return;

        textView.setText(
                getPreferenceScreen().getPreferenceCount() > 0 ? "" :
                        context.getResources().getText(R.string.no_widgets)
        );
    }

    @NonNull
    private Preference getWidgetPreference(Context context,
                                           @NonNull SharedPreferences sp,
                                           int appWidgetId)
    {
        Preference widget = new Preference(context)
        {
            @Override
            public void onBindViewHolder(@NonNull final PreferenceViewHolder holder)
            {
                super.onBindViewHolder(holder);
                holder.itemView.setOnLongClickListener(preference -> {
                    new AlertDialog.Builder(context)
                            .setMessage(R.string.activity_remove)
                            .setPositiveButton(R.string.activity_dialog_yes, (dialog, i) -> {
                                WidgetUtil.deleteWidget(context, appWidgetId, true);
                                onCreatePreferences(null, null);
                                updateActivity();
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
                sp.getString("name." + appWidgetId, ""),
                appWidgetId
        ));
        widget.setSummaryProvider(preference -> WidgetUtil.getDisplayURL(
                context, sp.getString("url." + appWidgetId, "")
        ));
        widget.setOnPreferenceClickListener(preference -> {
            WidgetPreference fragment = new WidgetPreference();
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

        return widget;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        Context context = getContext();
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        assert context != null;
        assert activity != null;

        // Create new screen
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        // Get shared preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        // Add widgets
        for (int appWidgetId : WidgetUtil.getAppWidgetIds(context))
            screen.addPreference(getWidgetPreference(context, sp, appWidgetId));

        // Set screen
        setPreferenceScreen(screen);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        Context context = getContext();
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        assert context != null;
        assert activity != null;

        // Update widget titles
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference widget = screen.getPreference(i);
            String widgetKey = widget.getKey();  // name.<id>
            int appWidgetId = Integer.parseInt(widgetKey.substring(5));
            widget.setTitle(
                    WidgetUtil.getDisplayName(
                            context,
                            sp.getString(widgetKey, ""),
                            appWidgetId
                    )
            );
        }

        // Update activity
        updateActivity();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Check widgets was added and/or deleted
        PreferenceScreen screen = getPreferenceScreen();
        int count = screen.getPreferenceCount();
        int[] ids = new int[count];

        for (int i = 0; i < count; i++) {
            String key = screen.getPreference(i).getKey();
            ids[i] = Integer.parseInt(key.substring(5));  // name.<id>
        }

        Arrays.sort(ids);

        if (!Arrays.equals(ids, WidgetUtil.getAppWidgetIds(getContext()))) {
            onCreatePreferences(null, null);
            updateActivity();
        }
    }
}
