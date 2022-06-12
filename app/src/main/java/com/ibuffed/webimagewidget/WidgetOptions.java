/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


class WidgetOptions
{
    final int appWidgetId;
    public String url;
    public boolean scaleImage;
    public boolean preserveAspectRatio;

    WidgetOptions(Context context, int appWidgetId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        url = prefs.getString("url." + appWidgetId, "");
        scaleImage = prefs.getBoolean("scale." + appWidgetId, true);
        preserveAspectRatio = prefs.getBoolean("aspect." + appWidgetId, true);
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

    public String getUrl()
    {
        return url;
    }

    public WidgetOptions setUrl(String url)
    {
        this.url = url;
        return this;
    }

    public boolean getScaleImage()
    {
        return scaleImage;
    }

    public WidgetOptions setScaleImage(boolean scaleImage)
    {
        this.scaleImage = scaleImage;
        return this;
    }

    public boolean getPreserveAspectRatio()
    {
        return preserveAspectRatio;
    }

    public WidgetOptions setPreserveAspectRatio(boolean preserveAspectRatio)
    {
        this.preserveAspectRatio = preserveAspectRatio;
        return this;
    }

    public int getAppWidgetId()
    {
        return appWidgetId;
    }
}
