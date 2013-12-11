package com.juick.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: coderoo
 * Date: 9/12/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class OutgoingMessageQueue {

    private static final String QUEUE_DIR = "OutgoingMessageQueue";
    private static final String FILE_SUFFIX = ".out";

    private static final long SERVICE_ALARM_INITIAL_DELAY_MILLIS = 10 * 1000L;
    private static final long SERVICE_ALARM_INTERVAL_MILLIS = 20 * 1000L;

    // this is a dummy outgoing message, it will be replaced with MicroBlogOutgoingMessage
    public class OutgoingMessage implements Serializable {
        private long sendAt;
    }

    public static void init(Context context) {
        setRepeatingAlarmToNudgeService(context);
    }

    private static File getDir(Context context) {
        return Utils.getDir(context, QUEUE_DIR);
    }

    public static boolean add(Context context, OutgoingMessage message) {
        try {
            String prefix = ElementNameUtils.generateNamePrefix(System.currentTimeMillis(), 0);
            File file = File.createTempFile(prefix, FILE_SUFFIX, getDir(context));
            boolean writeOK = Utils.writeObjectToFile(file, message);
            if (writeOK) {
                nudgeService(context);
            }
            return writeOK;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return false;
    }

    public static boolean delete(Context context, String elementId) {
        File file = new File(elementId);
        return file.delete();
    }

    public static OutgoingMessage getElement(String elementId) {
        return (OutgoingMessage) Utils.readObjectFromFile(elementId);
    }

    public static int getApproximateSize(Context context) {
        return getDir(context).list().length;
    }

    public static class ElementNameUtils {
        public static String generateNamePrefix(long currentTimeMillis, long sendDelaySeconds) {
            StringBuilder sb = new StringBuilder();
            sb.append(Long.toHexString(currentTimeMillis)); // createdAt
            sb.append(Long.toHexString(currentTimeMillis + sendDelaySeconds * 1000L)); // sendAt
            return sb.toString();
        }

        public static boolean isTimeToSend(String elementName, long currentTimeMillis) {
            long sendAt = Long.parseLong(elementName.substring(16, 32), 16);
            return sendAt <= currentTimeMillis;
        }
    }

    public static File[] loadElements(Context context) {
        File[] elements = getDir(context).listFiles();
        Arrays.sort(elements, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return (f1.getName()).compareTo(f2.getName());
            }
        });
        return elements;
    }

    public static String peekElementId(Context context) {
        File[] elements = loadElements(context);
        if (elements.length == 0) {
            return null; // the queue is empty
        }
        String suitableElementId = null;
        long currentTimeMillis = System.currentTimeMillis();
        for (File element : elements) {
            if (ElementNameUtils.isTimeToSend(element.getName(), currentTimeMillis)) {
                suitableElementId = element.getAbsolutePath();
                break;
            }
        }
        return suitableElementId;
    }

    public static void nudgeService(Context context) {
        Intent intent = new Intent(context, OutgoingMessageQueueService.class);
        context.startService(intent);
    }

    public static void setRepeatingAlarmToNudgeService(Context context) {
        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(context, OutgoingMessageQueueService.class);
        PendingIntent pintent = PendingIntent.getService(context, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtTime = cal.getTimeInMillis() + SERVICE_ALARM_INITIAL_DELAY_MILLIS;
        alarm.setRepeating(AlarmManager.RTC, triggerAtTime, SERVICE_ALARM_INTERVAL_MILLIS, pintent);
    }
}
