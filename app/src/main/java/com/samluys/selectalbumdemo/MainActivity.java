package com.samluys.selectalbumdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.samluys.jalbum.JAlbum;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.List;

public class MainActivity extends AppCompatActivity {

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
    }
}
