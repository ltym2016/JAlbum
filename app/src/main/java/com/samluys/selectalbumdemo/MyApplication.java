package com.samluys.selectalbumdemo;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

/**
 * @author luys
 * @describe
 * @date 2020-01-16
 * @email samluys@foxmail.com
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();




        Fresco.initialize(this);
    }
}
