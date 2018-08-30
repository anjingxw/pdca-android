package com.glimlab.pdca;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import org.xwalk.core.XWalkActivityDelegate;
import org.xwalk.core.XWalkDialogManager;

public abstract class MyXWalkActivity
        extends FragmentActivity
{
    private XWalkActivityDelegate mActivityDelegate;

    protected XWalkDialogManager getDialogManager()
    {
        return this.mActivityDelegate.getDialogManager();
    }

    public boolean isDownloadMode()
    {
        return this.mActivityDelegate.isDownloadMode();
    }

    public boolean isSharedMode()
    {
        return this.mActivityDelegate.isSharedMode();
    }

    public boolean isXWalkReady()
    {
        return this.mActivityDelegate.isXWalkReady();
    }

    protected void onCreate(Bundle paramBundle)
    {
        super.onCreate(paramBundle);
        this.mActivityDelegate = new XWalkActivityDelegate(this, new Runnable()
        {
            public void run()
            {
                MyXWalkActivity.this.onXWalkFailed();
            }
        }, new Runnable()
        {
        public void run()
            {
                MyXWalkActivity.this.onXWalkReady();
            }
        });
    }

    protected void onResume()
    {
        super.onResume();
        this.mActivityDelegate.onResume();
    }

    protected void onXWalkFailed()
    {
        finish();
    }

    protected abstract void onXWalkReady();
}

