package com.juick.android.juick;

import android.content.Context;
import com.juick.android.MainActivity;
import com.juick.android.MicroBlog;
import com.juickadvanced.data.juick.JuickMessage;
import com.juickadvanced.data.juick.JuickMessageID;
import com.juickadvanced.data.juick.JuickUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * User: san
 * Date: 11/6/12
 * Time: 1:07 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class JuickMessagesSource extends MessagesSource {

    public JuickMessagesSource(Context ctx, String kind) {
        super(ctx, kind);
    }

    public static JuickMessage initFromJSON(JSONObject json) throws JSONException {
        JuickMessage jmsg = new JuickMessage();
        jmsg.source = json.toString();
        final int mid = json.has("mid") ? json.getInt("mid") : json.getInt("parent_mid");   // for replies unprocessed
        jmsg.setMID(new JuickMessageID(mid));
        if (json.has("rid")) {
            jmsg.setRID(json.getInt("rid"));
        }
        if (json.has("replyto")) {
            jmsg.setReplyTo(json.getInt("replyto"));
        }
        jmsg.Text = json.getString("body").replace("&quot;", "\"");
        jmsg.User = parseUserJSON(json.getJSONObject("user"));

        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            jmsg.Timestamp = df.parse(json.getString("timestamp"));
        } catch (ParseException e) {
        }

        if (json.has("tags")) {
            JSONArray tags = json.getJSONArray("tags");
            for (int n = 0; n < tags.length(); n++) {
                jmsg.tags.add(tags.getString(n).replace("&quot;", "\""));
            }
        }

        if (json.has("replies")) {
            jmsg.replies = json.getInt("replies");
        }

        if (json.has("photo")) {
            jmsg.Photo = json.getJSONObject("photo").getString("small");
        }
        if (json.has("video")) {
            jmsg.Video = json.getJSONObject("video").getString("mp4");
        }
        if (json.has("context_post")) {
            jmsg.contextPost = initFromJSON(json.getJSONObject("context_post"));
        }
        if (json.has("context_reply")) {
            jmsg.contextReply = initFromJSON(json.getJSONObject("context_reply"));
        }
        if (json.has("context_reply")) {
            jmsg.contextReply = initFromJSON(json.getJSONObject("context_reply"));
        }
        if (json.has("my_found_count")) {
            jmsg.myFoundCount = (int)json.getDouble("my_found_count");
        }
        if (json.has("his_found_count")) {
            jmsg.hisFoundCount = (int)json.getDouble("his_found_count");
        }
        jmsg.microBlogCode = JuickMessageID.CODE;

        return jmsg;
    }

    public static JuickUser parseUserJSON(JSONObject json) throws JSONException {
        JuickUser juser = new JuickUser();
        juser.UID = json.getInt("uid");
        juser.UName = json.getString("uname");
        if (json.has("fullname")) {
            juser.FullName = json.getString("fullname");
        }
        return juser;
    }

    @Override
    public MicroBlog getMicroBlog() {
        return MainActivity.getMicroBlog(JuickMessageID.CODE);
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
