package com.glimlab.pdca;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity
        extends AppCompatActivity
{
    protected void onCreate(Bundle paramBundle)
    {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable()
        {
            public void run()
            {
                Intent localIntent = new Intent(SplashActivity.this, MainActivity.class);
                SplashActivity.this.startActivity(localIntent);
                SplashActivity.this.finish();
            }
        }, 2000L);
    }
}