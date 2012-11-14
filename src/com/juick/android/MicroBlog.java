package com.juick.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.widget.ListView;
import com.juick.android.api.JuickMessage;
import com.juick.android.api.MessageID;
import com.juick.android.juick.MessagesSource;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: san
 * Date: 11/9/12
 * Time: 1:21 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MicroBlog {
    void initialize();
    public String getCode();

    UserpicStorage.AvatarID getAvatarID(JuickMessage jmsg);

    MessageID createKey(String keyString);

    MessageMenu getMessageMenu(Activity activity, MessagesSource messagesSource, ListView listView, JuickMessagesAdapter listAdapter);

    void postReply(Activity context, MessageID mid, JuickMessage selectedReply, String msg, String attachmentUri, String attachmentMime, Utils.Function<Void, String> then);

    void postNewMessage(NewMessageActivity newMessageActivity, String msg, int pid, double lat, double lon, int acc, String attachmentUri, String attachmentMime, ProgressDialog progressDialog, Handler progressHandler, NewMessageActivity.BooleanReference progressDialogCancel, Utils.Function<Void, String> then);

    ThreadFragment.ThreadExternalUpdater getThreadExternalUpdater(Activity activity, MessageID mid);

    int getPiority();

    void addNavigationSources(ArrayList<MainActivity.NavigationItem> navigationItems, MainActivity mainActivity);

    void decorateNewMessageActivity(NewMessageActivity newMessageActivity);
}
