package com.juick.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;
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
    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam eget orci eget turpis rutrum pharetra nec ac urna. Vestibulum hendrerit fermentum libero, ac iaculis leo vestibulum sed. Quisque lacus justo, fringilla vel dui consequat, venenatis tempor velit. Aliquam lacinia nisi vitae massa ultrices consequat. Nulla ornare, purus sed rutrum vulputate, metus lacus posuere felis, eu laoreet purus ipsum pharetra nibh. Phasellus sodales, odio non gravida semper, diam risus facilisis neque, vitae varius sapien enim ultrices nulla. Vestibulum non nibh sed enim venenatis accumsan. Duis ut enim quis justo aliquet pretium id in nunc. Morbi semper euismod ante, vel rhoncus nulla hendrerit quis.\n" +
            "\n" +
            "In ac vulputate lectus. Praesent convallis urna nec arcu vehicula, non tristique lectus interdum. Ut vitae tellus vitae risus posuere hendrerit aliquam in lectus. Integer non purus purus. Cras eget bibendum felis. Pellentesque porttitor semper tristique. Duis vel aliquam metus. Vivamus rhoncus sagittis est id gravida. Vestibulum quis vulputate nunc. Etiam vel odio eget orci sagittis tempor a eget libero. Donec ullamcorper cursus nibh nec hendrerit. Nulla in dui non arcu venenatis lobortis. In non varius lacus. Donec sapien massa, euismod eget facilisis sit amet, dictum quis nisi. Praesent mauris velit, gravida sit amet sagittis nec, tincidunt sit amet ante.\n" +
            "\n" +
            "Maecenas vitae eros risus. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Sed pellentesque elit feugiat diam fringilla adipiscing. Phasellus velit diam, adipiscing vitae tortor ut, laoreet auctor leo. Pellentesque sodales fermentum elit, ut tempor massa. Curabitur mi turpis, tristique ut faucibus quis, aliquet in augue. Sed ut tincidunt arcu. Donec et purus et risus sollicitudin pulvinar. Nullam erat nibh, mattis a eleifend ac, porta vel orci. Etiam rhoncus molestie tortor ut volutpat.\n" +
            "\n" +
            "Sed tristique elementum porttitor. Nulla nunc neque, fringilla a elementum a, posuere nec ante. Fusce sodales auctor neque malesuada pharetra. Mauris euismod, mauris vel rutrum vestibulum, erat augue vehicula felis, tristique tincidunt ligula arcu non quam. Maecenas commodo congue imperdiet. Ut ac nulla tincidunt orci tempus tincidunt. Interdum et malesuada fames ac ante ipsum primis in faucibus. Nunc ut vulputate mi, in venenatis nisl. Suspendisse et euismod odio. Phasellus dignissim dolor tempor nibh posuere fringilla. Cras lobortis egestas hendrerit. Nam lectus est, imperdiet vitae aliquam quis, blandit at turpis. Aenean interdum volutpat neque, id sodales neque elementum et. Proin nulla ante, mollis et mauris quis, placerat condimentum velit. Mauris euismod ultrices ante. Nunc imperdiet vel risus eget imperdiet.\n" +
            "\n" +
            "Nunc commodo fringilla feugiat. Suspendisse id odio sollicitudin, hendrerit diam ut, ultricies lacus. Aliquam non orci magna. Aliquam at arcu eu nibh porttitor consequat ut vel turpis. Maecenas congue urna lacus, ut suscipit libero tempus non. Integer elementum fermentum amet.";

    public static void init(Context context) {
        setRepeatingAlarmToNudgeService(context);
    }

    private static File getDir(Context context) {
        return Utils.getDir(context, QUEUE_DIR);
    }

    public static boolean debugAddDummyMessage(Context context) {
        OutgoingMessage message = new OutgoingMessage();
        int beginIndex = (int) (Math.random() * 1000);
        int len = 10 + (int) (Math.random() * 1900);
        message.setMsg(LOREM_IPSUM.substring(beginIndex, beginIndex + len).trim());
        return add(context, message);
    }

    public static boolean add(Context context, OutgoingMessage message) {
        try {
            long sendDelaySeconds = 120;
            String prefix = ElementNameUtils.generateNamePrefix(System.currentTimeMillis(), sendDelaySeconds);
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

    public static boolean delete(String elementId) {
        File file = new File(elementId);
        return file.delete();
    }

    public static OutgoingMessage getElement(String elementId) {
        return (OutgoingMessage) Utils.readObjectFromFile(elementId);
    }

    public static QueueStats getQueueStats(Context context) {
        File[] elements = loadElements(context);
        QueueStats stats = new QueueStats();
        stats.setMessagesCount(elements.length);
        long totalSizeOfMessages = 0;
        for (File element : elements) {
            totalSizeOfMessages += element.length();
        }
        stats.setTotalSizeOfMessages(totalSizeOfMessages);
        return stats;
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

    public static void clear(Context context) {
        for (File file : loadElements(context)) {
            delete(file.getAbsolutePath());
        }
    }

    public static void processQueue(Context context) {
        String elementId;
        try {
            while (true) {
                elementId = peekElementId(context);
                if (elementId == null) {
                    break; // no more messages to send
                }
                // TODO Code to send the message goes here
                OutgoingMessage message = getElement(elementId);
                // let's pretend that we are sending the message
                try {
                    Thread.sleep(message.getMsg().length());
                } catch (InterruptedException e) {
                }
                delete(elementId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
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

    public static class QueueStats {
        private int messagesCount;
        private long totalSizeOfMessages;

        public int getMessagesCount() {
            return messagesCount;
        }

        public void setMessagesCount(int messagesCount) {
            this.messagesCount = messagesCount;
        }

        public long getTotalSizeOfMessages() {
            return totalSizeOfMessages;
        }

        public void setTotalSizeOfMessages(long totalSizeOfMessages) {
            this.totalSizeOfMessages = totalSizeOfMessages;
        }

        public String asString() {
            if (getMessagesCount() == 0) {
                return "Outgoing queue: empty";
            } else {
                return "Outgoing queue: " + getMessagesCount() + " messages, "
                        + String.format("%,d", getTotalSizeOfMessages()) + " bytes";
            }
        }
    }

    public static class ElementNameUtils {
        public static String generateNamePrefix(long currentTimeMillis, long sendDelaySeconds) {
            StringBuilder sb = new StringBuilder();
            sb.append(Utils.longToPaddedHexString(currentTimeMillis)); // createdAt
            sb.append(Utils.longToPaddedHexString(currentTimeMillis + sendDelaySeconds * 1000L)); // sendAt
            return sb.toString();
        }

        public static boolean isTimeToSend(String elementName, long currentTimeMillis) {
            long sendAt = Long.parseLong(elementName.substring(16, 32), 16);
            return sendAt <= currentTimeMillis;
        }
    }
}
