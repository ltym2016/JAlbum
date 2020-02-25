package com.samluys.selectalbumdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.samluys.jalbum.JAlbum;
import com.samluys.jalbum.activity.PhotoActivity;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
                                        .showTakePhoto(true)
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

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndPermission.with(MainActivity.this)
                        .runtime()
                        .permission(Permission.CAMERA,
                                Permission.RECORD_AUDIO)
                        .onGranted(new Action<List<String>>() {
                            @Override
                            public void onAction(List<String> data) {
                                File cameraPhoto = new File(MainActivity.this.getExternalCacheDir().toString() + "/myvideo");
                                Intent takePhotoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                Uri photoUri = FileProvider.getUriForFile(
                                        MainActivity.this,
                                        getPackageName() + ".fileprovider",
                                        cameraPhoto);
                                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                                startActivityForResult(takePhotoIntent, 1);
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

    private File createMediaFile() throws IOException {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CameraDemo");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "VID_" + timeStamp;
        String suffix = ".mp4";
        File mediaFile = new File(mediaStorageDir + File.separator + imageFileName + suffix);
        return mediaFile;
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
