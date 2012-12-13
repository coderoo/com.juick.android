/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.android;

import android.net.ConnectivityManager;
import org.apache.http.util.ByteArrayBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class JASocketClient {

    Socket sock;
    InputStream is;
    OutputStream os;
    JASocketClientListener listener = null;
    public boolean shuttingDown = false;
    long lastSuccessfulActivity = 0;

    public boolean send(String str) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] bytes = str.getBytes("utf-8");
            baos.write(bytes);
            os.write(baos.toByteArray());
            os.write('\n');
            os.flush();
            markActivity();
            return true;
        } catch (IOException e) {
            System.out.println("WS send:" +e.toString());
            return false;
        }
    }

    // some successful activity happened on the socket
    private void markActivity() {
        long oldActivity = lastSuccessfulActivity;
        lastSuccessfulActivity = System.currentTimeMillis();
        if (oldActivity != 0) {
            long period = lastSuccessfulActivity - oldActivity;
            ConnectivityChangeReceiver.recordSuccessfulIdlePeriod(JuickAdvancedApplication.instance, period);
        }
    }

    public JASocketClient() {
    }

    public void setListener(JASocketClientListener listener) {
        this.listener = listener;
    }

    public boolean connect(String host, int port) {
        try {

            sock = new Socket();
            sock.connect(new InetSocketAddress(host, port));
            is = sock.getInputStream();
            os = sock.getOutputStream();
            return true;
        } catch (Exception e) {
            System.err.println(e);
            //e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        return sock.isConnected();
    }

    public void readLoop() {
        try {
            int b;
            //StringBuilder buf = new StringBuilder();
            ByteArrayBuffer buf = new ByteArrayBuffer(16);
            boolean flagInside = false;
            StringBuilder baad = new StringBuilder();
            int lenToRead = -1;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((b = is.read()) != -1) {
                markActivity();
                if (b == '\n') {
                    if (listener != null) {
                        listener.onWebSocketTextFrame(new String(baos.toByteArray(), "utf-8"));
                        baos.reset();
                    }
                } else {
                    baos.write(b);
                }
            }
            System.err.println("DISCONNECTED readLoop");
            disconnect();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void disconnect() {
        try {
            if (is != null) is.close();
            if (os != null) os.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }


}
