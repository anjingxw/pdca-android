package com.glimlab.pdca.jsbridge;

import android.text.TextUtils;
import android.util.Log;

import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zhengzihui on 2017/11/25.
 */
public class WVJBWebViewClient extends XWalkResourceClient {
    private  WVJBWebView mWVJBWebView;

    public  WVJBWebViewClient(WVJBWebView view){
        super(view);
        mWVJBWebView = view;
    }
    @Override
    public void onDocumentLoadedInFrame(XWalkView view, long frameId) {
        super.onDocumentLoadedInFrame(view,frameId);
    }

    @Override
    public void onLoadStarted(XWalkView view, String url) {
        super.onLoadStarted(view, url);
    }

    @Override
    public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView view, XWalkWebResourceRequest request) {
        String url = request.getUrl().toString();
        if (!TextUtils.isEmpty(url)&&url.startsWith(WVJBConstants.SCHEME)) {
            if (url.indexOf(WVJBConstants.BRIDGE_LOADED) > 0) {
                mWVJBWebView.injectJavascriptFile();
            } else if (url.indexOf(WVJBConstants.MESSAGE) > 0) {
                mWVJBWebView.flushMessageQueue();
            } else {
                Log.d("WVJBWebViewClient","UnkownMessage:" + url);
            }
            String str = "ready";
            InputStream   in_nocode   =   new ByteArrayInputStream(str.getBytes());
            return createXWalkWebResourceResponse("text/html","UTF-8", in_nocode);//
        }

       return super.shouldInterceptLoadRequest(view,request);
    }


    @Override
    public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
        return super.shouldOverrideUrlLoading(view, url);
    }

}
