package com.darthwithap.okstickerapp;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class AnalyticsApplication {
    private static GoogleAnalytics sAnalytics;
    private static Tracker sTracker;

    public AnalyticsApplication(Context context) {
        sAnalytics = GoogleAnalytics.getInstance(context);
    }

    synchronized public Tracker getDefaultTracker() {
        if (sTracker == null) {
            sTracker = sAnalytics.newTracker(R.xml.global_tracker);
        }
        return sTracker;
    }
}
