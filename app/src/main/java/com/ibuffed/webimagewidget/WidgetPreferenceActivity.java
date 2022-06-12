/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;


public class WidgetPreferenceActivity extends PreferenceFragmentCompat
{
    public void addWidgetToScreen(int appWidgetId, PreferenceScreen screen)
    {
        Context context = getContext();
        String backStateName = this.getClass().getName();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Preference widget = new Preference(context) {
            @Override
            public void onBindViewHolder(final PreferenceViewHolder holder) {
                super.onBindViewHolder(holder);
                View itemView = holder.itemView;

                itemView.setOnClickListener(preference -> {
                    WidgetPreferenceFragment fragment = new WidgetPreferenceFragment();
                    Bundle fragmentArguments = new Bundle();
                    fragmentArguments.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    fragment.setArguments(fragmentArguments);

                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, fragment)
                            .addToBackStack(backStateName)
                            .commit();
                });

                itemView.setOnLongClickListener(preference -> {
                    DialogInterface.OnClickListener clickListener = (dialog, witch) -> {
                        if (witch == DialogInterface.BUTTON_POSITIVE) {
                            WidgetUtil.deleteWidget(context, appWidgetId);
                            rebuildWidgetScreen();
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.activity_remove)
                            .setPositiveButton(R.string.activity_remove_yes, clickListener)
                            .setNegativeButton(R.string.activity_remove_no, clickListener)
                            .show();

                    return true;
                });
            }
        };

        widget.setIcon(R.drawable.ic_widgets_black_24dp);
        widget.setTitle(
                WidgetUtil.getDisplayName(
                        context,
                        prefs.getString("name." + appWidgetId, ""),
                        appWidgetId
                )
        );
        widget.setSummary(
                WidgetUtil.getDisplayURL(
                        context,
                        prefs.getString("url." + appWidgetId, "")
                )
        );

        screen.addPreference(widget);
    }

    public void rebuildWidgetScreen()
    {
        Context context = getContext();
        Activity activity = getActivity();

        // Create new screen
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        for (int appWidgetId : WidgetUtil.getAppWidgetIds(context)) {
            this.addWidgetToScreen(appWidgetId, screen);
        }

        activity.setTitle(context.getResources().getString(R.string.activity_widgets));
        setPreferenceScreen(screen);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        // Create new PreferenceScreen
        Context context = getContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        rebuildWidgetScreen();
    }
}