package com.samluys.selectalbumdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.samluys.jalbum.JAlbum;
import com.samluys.jutils.log.LogUtils;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AndPermission.with(MainActivity.this)
                        .runtime()
                        .permission(Permission.READ_EXTERNAL_STORAGE,
                                Permission.WRITE_EXTERNAL_STORAGE)
                        .onGranted(new Action<List<String>>() {
                            @Override
                            public void onAction(List<String> data) {
                                JAlbum.from(MainActivity.this)
                                        .build()
                                        .maxSelectNum(10)
                                        .showGif(true)
                                        .showTakePhoto(false)
                                        .showVideoOnly(true)
                                        .showVideo(true)
                                        .setVideoMaxTime(20)
                                        .forResult(111);
                            }
                        })
                        .onDenied(new Action<List<String>>() {
                            @Override
                            public void onAction(List<String> data) {

                            }
                        })
                        .start();


            }
        });

        textView = findViewById(R.id.textView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 111) {
            if (data != null) {
                String path = data.getStringExtra("PATH");

                LogUtils.e("返回的路径 ：" + path);

                textView.setText("返回的视频路径 ：" +  path);
            }
        }
    }
}
