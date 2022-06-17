/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Objects;


public class WidgetOptions
{
    private final int appWidgetId;
    private String url;
    private int interval;
    private boolean wifi;
    private boolean scaleImage;
    private boolean preserveAspectRatio;

    WidgetOptions(Context context, int appWidgetId)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        url = sp.getString("url." + appWidgetId, "");
        interval = Integer.parseInt(
                Objects.requireNonNull(sp.getString("interval." + appWidgetId, "-1"))
        );
        scaleImage = sp.getBoolean("scale." + appWidgetId, true);
        preserveAspectRatio = sp.getBoolean("aspect." + appWidgetId, true);
        wifi = sp.getBoolean("wifi." + appWidgetId, false);
        this.appWidgetId = appWidgetId;
    }

    public int getLayoutId()
    {
        if (scaleImage) {
            if (preserveAspectRatio) {
                return R.layout.widget_fit_center;
            } else {
                return R.layout.widget_fit_xy;
            }
        } else {
            return R.layout.widget_center;
        }
    }

    public int getAppWidgetId()
    {
        return appWidgetId;
    }

    public String getUrl()
    {
        return url;
    }

    public WidgetOptions setUrl(String url)
    {
        this.url = url;
        return this;
    }

    public int getInterval()
    {
        return interval;
    }

    public WidgetOptions setInterval(int interval)
    {
        this.interval = interval;
        return this;
    }

    public boolean getWifi()
    {
        return wifi;
    }

    public WidgetOptions setWifi(boolean wifi)
    {
        this.wifi = wifi;
        return this;
    }

    public WidgetOptions setScaleImage(boolean scaleImage)
    {
        this.scaleImage = scaleImage;
        return this;
    }

    public WidgetOptions setPreserveAspectRatio(boolean preserveAspectRatio)
    {
        this.preserveAspectRatio = preserveAspectRatio;
        return this;
    }
}
