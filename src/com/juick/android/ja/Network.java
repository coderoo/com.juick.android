package com.juick.android.ja;

import android.content.Context;
import android.util.Log;
import com.juick.android.Utils;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: san
 * Date: 11/9/12
 * Time: 12:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class Network {
    public static Utils.RESTResponse postJSONHome(Context context, String path, String data) {
        Utils.RESTResponse ret = null;
        try {
            URL jsonURL = new URL("http://" + Utils.JA_IP + ":" + Utils.JA_PORT + path);
            HttpURLConnection conn = (HttpURLConnection) jsonURL.openConnection();

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.connect();

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.close();

            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                ret = Utils.streamToString(inputStream, null);
                inputStream.close();
            } else {
                return new Utils.RESTResponse("HTTP "+conn.getResponseCode()+" " + conn.getResponseMessage(), false, null);
            }

            conn.disconnect();
            return ret;
        } catch (Exception e) {
            Log.e("getJSONHome", e.toString());
            return new Utils.RESTResponse(e.toString(), false, null);
        }
    }
}