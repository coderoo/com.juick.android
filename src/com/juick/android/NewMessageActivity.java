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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import com.juick.android.juick.JuickCompatibleURLMessagesSource;
import com.juick.android.juick.JuickMicroBlog;
import com.juick.android.juick.MessagesSource;
import com.juickadvanced.R;

import java.io.*;
import java.util.ArrayList;

/**
 *
 * @author Ugnich Anton
 */
public class NewMessageActivity extends Activity implements OnClickListener, DialogInterface.OnClickListener {

    private static final int ACTIVITY_LOCATION = 1;
    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;
    public static final int ACTIVITY_ATTACHMENT_VIDEO = 3;
    private static final int ACTIVITY_TAGS = 4;
    public EditText etTo;
    private EditText etMessage;
    public Button bLocationHint;
    public ImageButton bTags;
    public ImageButton bLocation;
    public ImageButton bAttachment;
    private ImageButton bSend;
    private ProgressBar progressSend;

    public static class DialogData implements Serializable {

        public DialogData() {
        }

        private int pid = 0;
        public int pidHint = 0;
        private String pname = null;
        private double lat = 0;
        private double lon = 0;
        private int acc = 0;
        private String attachmentUri = null;
        private String attachmentMime = null;
        public MessagesSource messagesSource;
    }

    public DialogData data = new DialogData();

    private ProgressDialog progressDialog = null;
    private BooleanReference progressDialogCancel = new BooleanReference(false);
    private Handler progressHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (progressDialog.getMax() < msg.what) {
                progressDialog.setMax(msg.what);
            } else {
                progressDialog.setProgress(msg.what);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        JuickAdvancedApplication.setupTheme(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.newmessage);

        data.messagesSource = (MessagesSource)getIntent().getSerializableExtra("messagesSource");
        checkMessagesSource();


        etTo = (EditText) findViewById(R.id.editTo);
        etMessage = (EditText) findViewById(R.id.editMessage);
        bLocationHint = (Button) findViewById(R.id.buttonLocationHint);
        bTags = (ImageButton) findViewById(R.id.buttonTags);
        bLocation = (ImageButton) findViewById(R.id.buttonLocation);
        bAttachment = (ImageButton) findViewById(R.id.buttonAttachment);
        bSend = (ImageButton) findViewById(R.id.buttonSend);
        progressSend = (ProgressBar) findViewById(R.id.progressSend);

        bLocationHint.setOnClickListener(this);
        bTags.setOnClickListener(this);
        bLocation.setOnClickListener(this);
        bAttachment.setOnClickListener(this);
        bSend.setOnClickListener(this);

        resetForm();
        handleIntent(getIntent());
        MainActivity.restyleChildrenOrWidget(getWindow().getDecorView());

    }

    private void checkMessagesSource() {
        if (data.messagesSource == null) {
            data.messagesSource = new JuickCompatibleURLMessagesSource("X", "dummy", this, "http://nonsense.x/");
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);    //To change body of overridden methods use File | Settings | File Templates.
        if (savedInstanceState != null && savedInstanceState.containsKey("dialogData")) {
            data = (DialogData)savedInstanceState.getSerializable("dialogData");
        }

        bAttachment.setSelected(data.attachmentUri != null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dialogData", data);
    }

    private void resetForm() {
        setProgressBarIndeterminateVisibility(true);
        etMessage.setText("");
        bLocationHint.setVisibility(View.GONE);
        bLocation.setSelected(false);
        bAttachment.setSelected(false);
        data.pid = 0;
        data.pidHint = 0;
        data.pname = null;
        data.lat = 0;
        data.lon = 0;
        data.acc = 0;
        data.attachmentUri = null;
        data.attachmentMime = null;
        progressDialog = null;
        progressDialogCancel.bool = false;
        etMessage.requestFocus();
        checkMessagesSource();
        data.messagesSource.getMicroBlog().decorateNewMessageActivity(this);
        TextView oldTitle = (TextView)findViewById(R.id.old_title);
        oldTitle.setText(data.messagesSource.getMicroBlog().getMicroblogName(this));

    }

    private void setFormEnabled(boolean state) {
        etMessage.setEnabled(state);
        bLocationHint.setEnabled(state);
        bTags.setEnabled(state);
        bLocation.setEnabled(state);
        bAttachment.setEnabled(state);
        bSend.setVisibility(state ? View.VISIBLE : View.GONE);
        progressSend.setVisibility(state ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        resetForm();
        handleIntent(intent);
    }

    private void handleIntent(Intent i) {
        String action = i.getAction();
        if (action != null && action.equals(Intent.ACTION_SEND)) {
            String mime = i.getType();
            final Bundle extras = i.getExtras();
            if (mime.equals("image/*")) {
                Object extraStream = extras.get(Intent.EXTRA_STREAM);
                if (extraStream != null && (extraStream.toString().toLowerCase().endsWith(".jpg") || extraStream.toString().toLowerCase().endsWith(".jpeg"))) {
                    mime = "image/jpeg";
                } else {
                    Toast.makeText(this, "Cannot prove file is jpeg: "+extraStream, Toast.LENGTH_LONG).show();
                }
            }
            if (mime.equals("text/plain")) {
                etMessage.append(extras.getString(Intent.EXTRA_TEXT));
            } else if (mime.equals("image/jpeg") || mime.equals("video/3gpp") || mime.equals("video/mp4")) {
                data.attachmentUri = extras.get(Intent.EXTRA_STREAM).toString();
                data.attachmentMime = mime;
                bAttachment.setSelected(true);
                if (mime.equals("image/jpeg")) {
                    maybeResizePicture(this, data.attachmentUri, new Utils.Function<Void, String>() {
                        @Override
                        public Void apply(String s) {
                            data.attachmentUri = s;
                            bAttachment.setSelected(data.attachmentUri != null);
                            return null;
                        }
                    });
                }
            } else {
                String errorMessage;
                try {
                    errorMessage = String.format(getString(R.string.JuickBadMimetype)+" - "+extras.get(Intent.EXTRA_STREAM), mime);
                } catch (Exception ex) {
                    /// java.util.MissingFormatArgumentException: Format specifier: 20i  (???)
                    errorMessage = "Use JPEG/3GP/MP4 only";
                }
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.ForcedAttachment))
                        .setMessage(errorMessage)
                        .setPositiveButton(getString(R.string.AsJPEG), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                data.attachmentUri = extras.get(Intent.EXTRA_STREAM).toString();
                                data.attachmentMime = "image/jpeg";
                                bAttachment.setSelected(true);
                                maybeResizePicture(NewMessageActivity.this, data.attachmentUri, new Utils.Function<Void, String>() {
                                    @Override
                                    public Void apply(String s) {
                                        data.attachmentUri = s;
                                        bAttachment.setSelected(data.attachmentUri != null);
                                        return null;
                                    }
                                });
                            }
                        })
                        .setNeutralButton(getString(R.string.AsMP4Video), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                data.attachmentUri = extras.get(Intent.EXTRA_STREAM).toString();
                                data.attachmentMime = "video/mp4";
                                bAttachment.setSelected(true);
                            }
                        })
                        .setCancelable(true)
                        .setNegativeButton(R.string.DontAttach, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
            }
        }
    }

    public void onClick(View v) {
        if (v == bTags) {
            JuickMicroBlog.withUserId(this, new Utils.Function<Void, Pair<Integer,String>>() {
                @Override
                public Void apply(Pair<Integer, String> cred) {
                    Intent i = new Intent(NewMessageActivity.this, TagsActivity.class);
                    i.setAction(Intent.ACTION_PICK);
                    i.putExtra("uid", cred.first.intValue());
                    i.putExtra("multi", true);
                    startActivityForResult(i, ACTIVITY_TAGS);
                    return null;
                }
            });
        } else if (v == bLocationHint) {
            bLocationHint.setVisibility(View.GONE);
            data.pid = data.pidHint;
            data.pname = null;
            data.lat = 0;
            data.lon = 0;
            data.acc = 0;
            bLocation.setSelected(true);
        } else if (v == bLocation) {
            bLocationHint.setVisibility(View.GONE);
            if (data.pid == 0 && data.lat == 0) {
                startActivityForResult(new Intent(this, PickPlaceActivity.class), ACTIVITY_LOCATION);
            } else {
                data.pid = 0;
                data.pname = null;
                data.lat = 0;
                data.lon = 0;
                data.acc = 0;
                bLocation.setSelected(false);
            }
        } else if (v == bAttachment) {
            if (data.attachmentUri == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.Attach);
                builder.setAdapter(new AttachAdapter(this), this);
                builder.show();
            } else {
                data.attachmentUri = null;
                data.attachmentMime = null;
                bAttachment.setSelected(false);
            }
        } else if (v == bSend) {
            final String msg = etMessage.getText().toString();
            if (msg.length() < 3) {
                Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
                return;
            }
            setFormEnabled(false);
            if (data.attachmentUri != null) {
                progressDialog = new ProgressDialog(this);
                progressDialogCancel.bool = false;
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    public void onCancel(DialogInterface arg0) {
                        NewMessageActivity.this.progressDialogCancel.bool = true;
                    }
                });
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(0);
                progressDialog.show();
            }
            Thread thr = new Thread(new Runnable() {

                public void run() {
                    data.messagesSource.getMicroBlog().postNewMessage(NewMessageActivity.this, msg, data.pid, data.lat, data.lon, data.acc, data.attachmentUri, data.attachmentMime, progressDialog, progressHandler, progressDialogCancel, new Utils.Function<Void, String>() {
                        @Override
                        public Void apply(String errorText) {
                            if (progressDialog != null) {
                                try {
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    //
                                }
                            }
                            setFormEnabled(true);
                            if (errorText == null) {
                                resetForm();
                                getPhotoCaptureFile().delete(); // if any
                            }
                            if (data.attachmentUri == null) {
                                Toast.makeText(NewMessageActivity.this, errorText == null ? getString(R.string.Message_posted) : errorText, Toast.LENGTH_LONG).show();
                            } else {
                                try {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(NewMessageActivity.this);
                                    builder.setNeutralButton(R.string.OK, null);
                                    if (errorText == null) {
                                        builder.setIcon(android.R.drawable.ic_dialog_info);
                                        builder.setMessage(R.string.Message_posted);
                                    } else {
                                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                                        builder.setMessage(errorText);
                                    }
                                    builder.show();
                                } catch (WindowManager.BadTokenException e) {
                                    // window is not active
                                }
                            }
                            return null;
                        }
                    });
                }
            },"Post message (large)");
            thr.start();
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        Intent intent;
        switch (which) {
            case 0:
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_IMAGE);
                break;
            case 1:
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                boolean useTempFileForCapture = sp.getBoolean("useTempFileForCapture", true);
                if (useTempFileForCapture) {
                    File file = getPhotoCaptureFile();
                    file.delete();
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                } else {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                }
                startActivityForResult(intent, ACTIVITY_ATTACHMENT_IMAGE);
                break;
            case 2:
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_VIDEO);
                break;
            case 3:
                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                startActivityForResult(intent, ACTIVITY_ATTACHMENT_VIDEO);
                break;
        }
    }

    public static File getPhotoCaptureFile() {
        return new File(Environment.getExternalStorageDirectory(), "juick_tmp_capture.jpg");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTIVITY_TAGS) {
                String tag = data.getStringExtra("tag");
                if (tag.trim().length() == 0) return;
                if (!tag.startsWith("*")) tag = "*"+tag; // compatible
                etMessage.setText(tag + " " + etMessage.getText());
            } else if (requestCode == ACTIVITY_LOCATION) {
                this.data.pid = data.getIntExtra("pid", 0);
                this.data.lat = data.getDoubleExtra("lat", 0);
                this.data.lon = data.getDoubleExtra("lon", 0);
                this.data.acc = data.getIntExtra("acc", 0);
                this.data.pname = data.getStringExtra("pname");
                if ((this.data.pid > 0 || this.data.lat != 0) && this.data.pname != null) {
                    bLocation.setSelected(true);
                }
            } else if ((requestCode == ACTIVITY_ATTACHMENT_IMAGE || requestCode == ACTIVITY_ATTACHMENT_VIDEO)) {
                if (data != null) {
                    this.data.attachmentUri = data.getDataString();
                } else if (getPhotoCaptureFile().exists()) {
                    this.data.attachmentUri = Uri.fromFile(getPhotoCaptureFile()).toString();
                }
                if (requestCode == ACTIVITY_ATTACHMENT_IMAGE) {
                    maybeResizePicture(this, this.data.attachmentUri, new Utils.Function<Void, String>() {
                        @Override
                        public Void apply(String s) {
                            NewMessageActivity.this.data.attachmentUri = s;
                            bAttachment.setSelected(NewMessageActivity.this.data.attachmentUri != null);
                            return null;
                        }
                    });
                }
                this.data.attachmentMime = (requestCode == ACTIVITY_ATTACHMENT_IMAGE) ? "image/jpeg" : "video/3gpp";
                bAttachment.setSelected(this.data.attachmentUri != null);
            }
        }
    }

    public static void maybeResizePicture(final Activity parent, final String attachmentUri, final Utils.Function<Void, String> function) {
        if (attachmentUri == null) {
            Toast.makeText(parent, "Missing attachment URI ;(", Toast.LENGTH_LONG).show();
            return;
        }
        boolean askForResize = PreferenceManager.getDefaultSharedPreferences(parent).getBoolean("askForResize", false);
        if (askForResize) {
            File deleteFile = null;
            try {
                File tmpfile = new File(Uri.parse(attachmentUri).getPath());
                if (!tmpfile.exists()) {
                    deleteFile = tmpfile = getQuickTempFile(parent);
                    FileInputStream inputStream = parent.getContentResolver().openAssetFileDescriptor(Uri.parse(attachmentUri), "r").createInputStream();
                    copyStreamToFile(inputStream, tmpfile);
                }
                final BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(tmpfile.getPath(), opts);
                int outHeight = opts.outHeight;
                int outWidth = opts.outWidth;
                LinearLayout ll = new LinearLayout(parent);
                ll.setOrientation(LinearLayout.VERTICAL);
                if (outHeight > 400 || outWidth > 400) {
                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(parent)
                            .setView(ll)
                            .setTitle(parent.getString(R.string.ResizeImage));

                    TextView tv = new TextView(parent);
                    tv.setLayoutParams(new ViewGroup.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                    tv.setText(parent.getString(R.string.Current__) + outWidth + " x " + outHeight + " " + parent.getString(R.string.FileSize_) + " " + tmpfile.length() / 1024 + " KB");
                    ll.addView(tv);
                    RadioGroup rg = new RadioGroup(parent);
                    rg.setLayoutParams(new ViewGroup.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                    final ArrayList<RadioButton> rbs = new ArrayList<RadioButton>();
                    for(int i=2; i<6; i++) {
                        RadioButton rb = new RadioButton(parent);
                        rb.setLayoutParams(new ViewGroup.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                        int nh = outHeight / i;
                        int nw = outWidth / i;
                        rb.setText(parent.getString(R.string.Resize__)+" " + nw + " x " + nh);
                        rbs.add(rb);
                        rg.addView(rb);
                        rb.setTag(i);
                        if (i == 2) {
                            rb.setChecked(true);
                        }
                    }
                    ll.addView(rg);
                    builder.setCancelable(true);
                    builder.setNeutralButton(parent.getString(R.string.DontAttach), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            function.apply(null);
                            dialog.cancel();
                        }
                    });
                    builder.setNegativeButton(parent.getString(R.string.KeepOrigSize), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.setPositiveButton(parent.getString(R.string.Preview), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            for (RadioButton rb : rbs) {
                                if (rb.isChecked()) {
                                    File deleteFile = null;
                                    try {
                                        Integer skipSize = (Integer)rb.getTag();
                                        BitmapFactory.Options opts = new BitmapFactory.Options();
                                        opts.inJustDecodeBounds = false;
                                        opts.inSampleSize = Math.max(skipSize, skipSize);
                                        File tmpfile = new File(Uri.parse(attachmentUri).getPath());
                                        if (!tmpfile.exists()) {
                                            deleteFile = tmpfile = getQuickTempFile(parent);
                                            FileInputStream inputStream = parent.getContentResolver().openAssetFileDescriptor(Uri.parse(attachmentUri), "r").createInputStream();
                                            copyStreamToFile(inputStream, tmpfile);
                                        }
                                        Bitmap bitmap = BitmapFactory.decodeFile(tmpfile.getPath(), opts);
                                        int orientation = ExifInterface.ORIENTATION_NORMAL;
                                        try {
                                            orientation = new ExifInterface(tmpfile.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                        } catch (Exception ex) {
                                            // sorry
                                        }
                                        if (deleteFile != null) tmpfile.delete();
                                        int sourceWidth = bitmap.getWidth();
                                        int sourceHeight = bitmap.getHeight();
                                        float angle = 0;
                                        switch(orientation) {
                                            case ExifInterface.ORIENTATION_ROTATE_90: {
                                                angle = 90;
                                                break;
                                            }
                                            case ExifInterface.ORIENTATION_ROTATE_270: {
                                                angle = 270;
                                                break;
                                            }
                                            case ExifInterface.ORIENTATION_ROTATE_180: {
                                                angle = 180;
                                                break;
                                            }

                                        }

                                        if (angle != 0) {
                                            Matrix matrix = new Matrix();
                                            matrix.postRotate(angle);
                                            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, sourceWidth, sourceHeight, matrix, true);
                                            bitmap.recycle();
                                            bitmap = newBitmap;
                                        }

                                        final File outFile = new File(parent.getCacheDir(), "juick_capture_resized.jpg");
                                        outFile.delete();
                                        FileOutputStream fos = new FileOutputStream(outFile);
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
                                        fos.close();

                                        Bitmap displayBitmap = bitmap;
                                        if (displayBitmap.getWidth() > 256) {
                                            double scale = 256.0/displayBitmap.getWidth();
                                            displayBitmap = Bitmap.createScaledBitmap(bitmap, (int)(scale * displayBitmap.getWidth()), (int)(scale * displayBitmap.getHeight()), false);
                                        }

                                        BitmapDrawable icon = new BitmapDrawable(displayBitmap);
                                        AlertDialog alertDialog = new AlertDialog.Builder(parent)
                                                .setTitle(parent.getString(R.string.ScaleResult))
                                                .setMessage(parent.getString(R.string.NewSize__) + " " + bitmap.getWidth() + " x " + bitmap.getHeight() + " " + parent.getString(R.string.FileSize_) + " " + outFile.length() / 1024 + " KB")
                                                .setIcon(icon)
                                                .setNegativeButton(parent.getString(R.string.KeepOrigSize), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.cancel();
                                                    }
                                                })
                                                .setPositiveButton(parent.getString(R.string.Finish), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        function.apply(Uri.fromFile(outFile).toString());
                                                    }
                                                })
                                                .setNeutralButton(parent.getString(R.string.TryOther), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        maybeResizePicture(parent, attachmentUri, function);
                                                    }
                                                })
                                                .create();
                                        alertDialog.show();
                                    } catch (IOException e) {
                                        Toast.makeText(parent, "Error: "+e.toString(), Toast.LENGTH_LONG).show();
                                        return;
                                    } finally {
                                        if (deleteFile != null)
                                            deleteFile.delete();
                                    }
                                }
                            }
                        }
                    });
                    MainActivity.restyleChildrenOrWidget(ll);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    Toast.makeText(parent, String.format(parent.getString(R.string.SkippingResize), tmpfile.length()/1024, outWidth, outHeight),Toast.LENGTH_LONG).show();
                }
            } catch (IOException ex) {
                Toast.makeText(parent, ex.toString(), Toast.LENGTH_LONG).show();
            } finally {
                if (deleteFile != null)
                    deleteFile.delete();
            }
        }
    }

    private static File getQuickTempFile(Activity parent) {
        return new File(parent.getCacheDir(), "tmp.jpg");
    }

    private static void copyStreamToFile(InputStream inputStream, File tmpfile) throws IOException {
        FileOutputStream fos = new FileOutputStream(tmpfile);
        byte[] arr = new byte[4096];
        while(true) {
            int len = inputStream.read(arr);
            if (len < 1) break;
            fos.write(arr);
        }
        fos.close();
        inputStream.close();
    }

    public static class BooleanReference {

        public boolean bool;

        public BooleanReference(boolean bool) {
            this.bool = bool;
        }
    }
}
