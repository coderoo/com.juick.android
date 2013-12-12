package com.juick.android;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA.
 * User: coderoo
 * Date: 11/12/13
 * Time: 10:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class OutgoingMessageQueueService extends IntentService {

    public OutgoingMessageQueueService() {
        super(OutgoingMessageQueueService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        OutgoingMessageQueue.processQueue(getApplicationContext());
    }

}
