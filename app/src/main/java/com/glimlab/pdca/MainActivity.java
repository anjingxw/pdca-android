package com.glimlab.pdca;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.Toast;

import com.baoyz.actionsheet.ActionSheet;
import com.glimlab.pdca.RecordAudio.AudioRecorderDialog;
import com.glimlab.pdca.jsbridge.WVJBWebView;
import com.glimlab.pdca.jsbridge.WVJBWebViewClient;

import org.xwalk.core.XWalkActivity;
import org.xwalk.core.XWalkDownloadListener;
import org.xwalk.core.XWalkJavascriptResult;
import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import java.io.File;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends MyXWalkActivity implements EasyPermissions.PermissionCallbacks {
    private Button button;
    private WVJBWebView webView;
    private WVJBWebView popupView;
    private ValueCallback mUploadMessage;
    private String filePath;
    private Uri mUriCapture;
    private final int FILE_SELECTED = 10000;
    private final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 12000;
    private WVJBWebView.WVJBResponseCallback recordMp3CallBack;
    private Long exitTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FileUtils.init(getApplicationContext());

        XWalkPreferences.setValue("enable-javascript", true);        //添加对javascript支持
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        XWalkPreferences.setValue(XWalkPreferences.ALLOW_UNIVERSAL_ACCESS_FROM_FILE, true);  //置是否允许通过file url加载的Javascript可以访问其他的源,包括其他的文件和http,https等其他的源
        XWalkPreferences.setValue(XWalkPreferences.JAVASCRIPT_CAN_OPEN_WINDOW, true);        //JAVASCRIPT_CAN_OPEN_WINDOW
        XWalkPreferences.setValue(XWalkPreferences.SUPPORT_MULTIPLE_WINDOWS, true);        // enable multiple windows.
        webView = (WVJBWebView) findViewById(R.id.wv);
        popupView = (WVJBWebView) findViewById(R.id.wvPop);
        button = (Button) findViewById(R.id.rc);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.showRecordDialog();
            }
        });
        button.setVisibility(View.GONE);

        findViewById(R.id.fab12).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.reload(1);
            }
        });

        findViewById(R.id.fab22).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (webView.getNavigationHistory().canGoBack()) {
                    webView.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);//返回上一页面
                }
            }
        });

        checkPermissions();
    }

    @Override
    protected void onXWalkReady() {
        webView.addJavascriptInterface();
        webView.setUIClient(new XWalkUIClient(webView) {
            @Override
            public boolean onCreateWindowRequested(XWalkView view, XWalkUIClient.InitiateBy initiator, ValueCallback<XWalkView> callback){
                callback.onReceiveValue(MainActivity.this.popupView);
                return  true;
            }
            @Override
            public void openFileChooser(XWalkView view, ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                super.openFileChooser(view, uploadFile, acceptType, capture);
                if (mUploadMessage != null) return;
                mUploadMessage = uploadFile;

                ActionSheet.createBuilder(MainActivity.this.getBaseContext(), MainActivity.this.getSupportFragmentManager()).setCancelButtonTitle("退出").setOtherButtonTitles(new String[] { "拍照", "选择文件" }).setCancelableOnTouchOutside(true).setListener(new ActionSheet.ActionSheetListener()
                {
                    public void onDismiss(ActionSheet actionSheet, boolean isCancel)
                    {
                        if (isCancel)
                        {
                            MainActivity.this.mUploadMessage.onReceiveValue(null);
                            MainActivity.this.mUploadMessage = null;
                        }
                    }

                    public void onOtherButtonClick(ActionSheet actionSheet, int index)
                    {
                        if (index == 0)
                        {
                            if (EasyPermissions.hasPermissions(MainActivity.this.getBaseContext(), new String[] { "android.permission.CAMERA" }))
                            {
                                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                                File localFile = FileUtils.getTempFile(FileUtils.FileType.IMG);
                                if (localFile != null) {
                                    mUriCapture = Uri.fromFile(localFile);
                                }
                                intent.putExtra("output", MainActivity.this.mUriCapture);
                                MainActivity.this.startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                                return;
                            }
                            android.widget.Toast.makeText(MainActivity.this.getBaseContext(), "没有拍照权限!", Toast.LENGTH_SHORT);
                            MainActivity.this.mUploadMessage.onReceiveValue(null);
                            MainActivity.this.mUploadMessage = null;
                            return;
                        }
                        Intent intent = new Intent("android.intent.action.GET_CONTENT");
                        intent.addCategory("android.intent.category.OPENABLE");
                        intent.setType("*/*");
                        MainActivity.this.startActivityForResult(Intent.createChooser(intent, "选择文件"), 10000);
                    }
                }).show();
            }

            @Override
            public boolean onConsoleMessage(XWalkView view, String message, int lineNumber,
                                            String sourceId, ConsoleMessageType messageType) {
                Log.i("WebViewActivity", "----------------------------------------------"
                        + message + " -- From line "
                        + lineNumber + " of "
                        + sourceId);
                return super.onConsoleMessage(view, message, lineNumber, sourceId, messageType);
            }
        });

        popupView.setUIClient(new XWalkUIClient(this.popupView)
        {
            public void onPageLoadStarted(XWalkView paramAnonymousXWalkView, String paramAnonymousString)
            {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(paramAnonymousString));
                paramAnonymousXWalkView.getContext().startActivity(intent);
                paramAnonymousXWalkView.stopLoading();
            }
        });

        webView.setResourceClient(new WVJBWebViewClient(this.webView));
        webView.loadUrl("http://58.16.248.170:8088");
        //webView.load("file:///android_asset/open_file.html", null);
        //webView.load("file:///android_asset/test.html", "");
        //webView.loadUrl("http://211.149.230.45:8088");


        webView.registerHandler("recordMp3", new WVJBWebView.WVJBHandler() {
            @Override
            public void request(Object data, final WVJBWebView.WVJBResponseCallback callback) {
                if (recordMp3CallBack != null)
                    return;
                String[] perms = {Manifest.permission.RECORD_AUDIO};
                if (!EasyPermissions.hasPermissions(getBaseContext(), perms)) {
                    org.chromium.ui.widget.Toast.makeText(getBaseContext(), "请确定开启了录音权限!", Toast.LENGTH_LONG);
                    recordMp3CallBack.callback(null);
                    return;
                }

                recordMp3CallBack = callback;
                AudioRecorderDialog dialog = new AudioRecorderDialog(MainActivity.this);
                dialog.setShowAlpha(0.5f);
                dialog.showAtLocation(webView, Gravity.CENTER, 0, 0);
                dialog.setListener(new AudioRecorderDialog.AudioRecorderDialogListener() {
                    @Override
                    public void success(String string) {
                        try {
                            String encodeBase64File = Util.encodeBase64File(string);
                            callback.callback(encodeBase64File);
                            Util.deleteFile(string);
                        } catch (Exception e) {
                            callback.callback(null);
                        } finally {
                            recordMp3CallBack = null;
                        }
                    }

                    @Override
                    public void cancel() {
                        callback.callback(null);
                        recordMp3CallBack = null;
                    }
                });
            }
        });
        webView.setDownloadListener(new XWalkDownloadListener(getBaseContext()) {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Log.i("tag", "url=" + url);
                Log.i("tag", "userAgent=" + userAgent);
                Log.i("tag", "contentDisposition=" + contentDisposition);
                Log.i("tag", "mimetype=" + mimetype);
                Log.i("tag", "contentLength=" + contentLength);
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (System.currentTimeMillis() - exitTime > 1500) {
                Toast.makeText(this, "再点击一次返回", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                this.finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECTED) {
            if (null == mUploadMessage) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
            return;
        }else if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE){
            if (null == mUploadMessage) return;
            if (resultCode != RESULT_OK) return;
            mUploadMessage.onReceiveValue(mUriCapture);
            mUploadMessage = null;
        }
    }

    private void showRecordDialog() {
        AudioRecorderDialog dialog = new AudioRecorderDialog(this);
        dialog.setShowAlpha(0.5f);
        dialog.showAtLocation(webView, Gravity.CENTER, 0, 0);
    }


    private static final int RC_CAMERA_PERM = 123;

    private void checkPermissions() {
        String[] perms = {Manifest.permission.CAMERA,Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
        } else {
            EasyPermissions.requestPermissions(this, "请开启需要的手机权限:网络，录制语音，读写磁盘文件",
                    RC_CAMERA_PERM, perms);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).setRationale("请开启需要的手机权限:网络，录制语音，读写磁盘文件").build().show();
        }

    }
}