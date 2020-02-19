package com.samluys.selectalbumdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.samluys.jalbum.JAlbum;
import com.samluys.jalbum.activity.PhotoActivity;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    private List<String> selectImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectImages = new ArrayList<>();
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
                                        .maxSelectNum(10 - selectImages.size())
                                        .showGif(true)
                                        .showTakePhoto(false)
                                        .showVideoOnly(false)
                                        .showVideo(true)
                                        .setVideoMaxTime(20)
                                        .forResult(PhotoActivity.REQUEST_CODE_SELECT_VIDEO);
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

        if (requestCode == PhotoActivity.REQUEST_CODE_SELECT_PHOTO) {
            if (data != null) {
                List<String> list = JAlbum.obtainPathResult(data);

                selectImages.addAll(list);

                textView.setText("返回的路径 ：" +  selectImages.toString());
            }
        } else if (requestCode == PhotoActivity.REQUEST_CODE_SELECT_VIDEO) {
            if (data != null) {
                String videoPath = JAlbum.obtainVideoPathResult(data);
                textView.setText("返回的视频路径 ：" +  videoPath);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        JAlbum.clearCache();
    }
}
