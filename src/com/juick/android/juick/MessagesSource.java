package com.juick.android.juick;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.juick.android.MicroBlog;
import com.juick.android.Utils;
import com.juickadvanced.data.juick.JuickMessage;
import com.juickadvanced.data.MessageID;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: san
 * Date: 9/5/12
 * Time: 10:44 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MessagesSource implements Serializable {

    protected transient Context ctx;
    protected transient SharedPreferences sp;
    public boolean canNext = true;
    public String kind;

    public MessagesSource(Context ctx, String kind) {
        setContext(ctx);
        this.kind = kind;
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
        sp = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public Context getContext() {
        return ctx;
    }

    public abstract boolean supportsBackwardRefresh();

    public void rememberSavedPosition(MessageID mid) {

    }

    public void resetSavedPosition() {

    }

    public abstract void getFirst(Utils.Notification notifications, Utils.Function<Void, ArrayList<JuickMessage>> cont);

    public abstract void getNext(Utils.Notification notifications, Utils.Function<Void, ArrayList<JuickMessage>> cont);


    public abstract void getChildren(MessageID mid, Utils.Notification notifications, Utils.Function<Void, ArrayList<JuickMessage>> cont);

    public abstract CharSequence getTitle();

    public final String getKind() {
        return kind;
    }

    public abstract MicroBlog getMicroBlog();

    public boolean canNext() {
        return canNext;
    }

    public void setCanNext(boolean canNext) {
        this.canNext = canNext;
    }


}
