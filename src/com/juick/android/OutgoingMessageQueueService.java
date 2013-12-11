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
        String elementId;
        while (true) {
            elementId = OutgoingMessageQueue.peekElementId(getApplicationContext());
            if (elementId == null) {
                break; // no more messages to send
            }
            // TODO Code to send the message goes here (use OutgoingMessageQueue.getElement())
            // if we sent the message successfully then delete the message from the queue
            OutgoingMessageQueue.delete(getApplicationContext(), elementId);
        }
    }
}
