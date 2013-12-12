package com.juick.android;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: coderoo
 * Date: 12/12/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class OutgoingMessage implements Serializable {
    private static final long serialVersionUID = 5652904283363658401L;
    private String msg;
    private int pid;
    private double lat;
    private double lon;
    private int acc;
    private String attachmentUri;
    private String attachmentMime;
    private byte[] attachment;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getAcc() {
        return acc;
    }

    public void setAcc(int acc) {
        this.acc = acc;
    }

    public String getAttachmentUri() {
        return attachmentUri;
    }

    public void setAttachmentUri(String attachmentUri) {
        this.attachmentUri = attachmentUri;
    }

    public String getAttachmentMime() {
        return attachmentMime;
    }

    public void setAttachmentMime(String attachmentMime) {
        this.attachmentMime = attachmentMime;
    }

    public byte[] getAttachment() {
        return attachment;
    }

    public void setAttachment(byte[] attachment) {
        this.attachment = attachment;
    }
}
