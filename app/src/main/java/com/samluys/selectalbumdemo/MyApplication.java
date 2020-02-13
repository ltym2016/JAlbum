package com.samluys.selectalbumdemo;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.samluys.jutils.Utils;
import com.samluys.jutils.log.LogUtils;

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

        LogUtils.newBuilder()
                .debug(Utils.isDebug())
                .tag("CIRCLE_DIMENSION_LOG")
                .build();

        Fresco.initialize(this);
    }
}
