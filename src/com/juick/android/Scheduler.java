package com.juick.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA.
 * User: coderoo
 * Date: 14/12/13
 * Time: 12:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class Scheduler {

    private Context context;
    private Class serviceClass;

    public Scheduler(Context context, Class serviceClass) {
        this.context = context;
        this.serviceClass = serviceClass;
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private Intent createIntentForService() {
        return new Intent(context, serviceClass);
    }

    private PendingIntent createPendingIntentForService(int flags) {
        return PendingIntent.getService(context, 0, createIntentForService(), flags);
    }

    public void startService() {
        context.startService(createIntentForService());
    }

    public void scheduleRepeatingService(long triggerAtTime, long interval) {
        getAlarmManager().setRepeating(AlarmManager.RTC, triggerAtTime, interval,
                createPendingIntentForService(PendingIntent.FLAG_CANCEL_CURRENT));
    }

    public boolean isServiceScheduled() {
        return createPendingIntentForService(PendingIntent.FLAG_NO_CREATE) != null;
    }

    public void cancelScheduledService() {
        getAlarmManager().cancel(createPendingIntentForService(PendingIntent.FLAG_CANCEL_CURRENT));
    }
}
