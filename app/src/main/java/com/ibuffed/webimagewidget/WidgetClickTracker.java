/*
 * This file is released under the GPL.
 */
package com.ibuffed.webimagewidget;

import android.util.SparseLongArray;
import android.os.SystemClock;


/**
 * Implementation of WidgetClickTracker functionality.
 */
class WidgetClickTracker
{
    private static final WidgetClickTracker tracker = new WidgetClickTracker();
    private final SparseLongArray clicks = new SparseLongArray();
    private static final int DOUBLE_CLICK_DELAY = 500;

    boolean doubleClicked(int appWidgetId)
    {
        long thisClick = SystemClock.uptimeMillis();
        long prevClick = clicks.get(appWidgetId, 0);

        if (thisClick - prevClick <= DOUBLE_CLICK_DELAY) {
            clicks.delete(appWidgetId);
            return true;
        }

        clicks.put(appWidgetId, thisClick);
        return false;
    }

    static WidgetClickTracker getInstance()
    {
        return tracker;
    }
}
