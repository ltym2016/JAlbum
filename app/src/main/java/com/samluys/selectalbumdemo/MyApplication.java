package com.samluys.selectalbumdemo;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.samluys.jutils.Utils;

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

        Utils.init(this);
        Fresco.initialize(this);
    }
}
