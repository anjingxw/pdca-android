package com.glimlab.pdca;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import org.xwalk.core.XWalkView;

/**
 * Created by wq on 2017/11/22.
 */
public class MyXWalkView extends XWalkView {
    public MyXWalkView(Context context) {
        super(context);
    }

    public MyXWalkView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK){
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
