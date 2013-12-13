package com.juick.android;

/**
 * Created with IntelliJ IDEA.
 * User: coderoo
 * Date: 13/12/13
 * Time: 11:50 AM
 * To change this template use File | Settings | File Templates.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class RecurringTask {

    public final static long DEFAULT_MINIMUM_DELAY_MILLIS = 15 * 1000L;
    public final static long DEFAULT_MAXIMUM_DELAY_MILLIS = 600 * 1000L;
    private final static String PREFERENCE_PREFIX = "recurringTask.";
    private final Context context;
    private final Class serviceClass;
    private final Scheduler scheduler;
    private long currentDelayMillis;

    public RecurringTask(Context context, Class serviceClass) {
        this.context = context;
        this.serviceClass = serviceClass;
        this.scheduler = new Scheduler(context, serviceClass);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public long getCurrentDelayMillis() {
        loadState();
        return currentDelayMillis;
    }

    private void setCurrentDelayMillis(long value) {
        if (value > DEFAULT_MAXIMUM_DELAY_MILLIS) {
            value = DEFAULT_MAXIMUM_DELAY_MILLIS;
        }
        currentDelayMillis = value;
        saveState();
    }

    public void taskHasSucceeded() {
        taskHasFinished(DEFAULT_MINIMUM_DELAY_MILLIS);
    }

    public void taskHasFailed() {
        taskHasFinished(getCurrentDelayMillis() * 2);
    }

    private void taskHasFinished(long newDelayMillis) {
        setCurrentDelayMillis(newDelayMillis);
        scheduleRepeatingService(true);
    }

    public void scheduleRepeatingService(boolean forceRescheduling) {
        if (forceRescheduling || getScheduler().isServiceScheduled() == false) {
            long time = System.currentTimeMillis() + getCurrentDelayMillis();
            getScheduler().scheduleRepeatingService(time, time);
        }
    }

    private void saveState() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        // there is just one field to save, so we use the service class name as a preference key
        editor.putLong(PREFERENCE_PREFIX + serviceClass.getName(), currentDelayMillis);
        editor.commit();
    }

    private void loadState() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        // see saveState()
        currentDelayMillis = sp.getLong(PREFERENCE_PREFIX + serviceClass.getName(), DEFAULT_MINIMUM_DELAY_MILLIS);
    }
}