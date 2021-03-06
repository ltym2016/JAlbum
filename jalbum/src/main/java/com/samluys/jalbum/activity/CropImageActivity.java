package com.samluys.jalbum.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.samluys.jalbum.R;
import com.samluys.jalbum.common.Constants;
import com.samluys.jalbum.view.crop.ClipImageLayout;
import com.samluys.jutils.BitmapUtils;
import com.samluys.jutils.FileUtils;
import com.samluys.jutils.ImageUtils;
import com.samluys.jutils.ScreenUtils;
import com.samluys.statusbar.StatusBarUtils;

import java.io.File;
import java.io.IOException;

public class CropImageActivity extends AppCompatActivity {

    private Bitmap mBitmap;
    private String mPath;
    private ClipImageLayout cil_crop;
    private TextView tv_save;
    private ImageView iv_back;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏设置
        }

        setContentView(R.layout.activity_crop_image);
        if (getIntent() != null) {
            mPath = getIntent().getStringExtra("path");
            if (!TextUtils.isEmpty(mPath) && mPath.startsWith(Constants.FILE_PREX)) {
                mPath = mPath.replace(Constants.FILE_PREX, "");
            }
        }
        StatusBarUtils.StatusBarIconDark(this);
        initView();
        initData();
    }

    private void initView() {
        cil_crop = findViewById(R.id.cil_crop);
        tv_save = findViewById(R.id.tv_save);
        iv_back = findViewById(R.id.ivBack);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tv_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
    }

    public void initData() {
        if (!TextUtils.isEmpty(mPath)) {
            mBitmap = ImageUtils.getScaledBitmap(mPath, ScreenUtils.getScreenWidth(),
                    ScreenUtils.getScreenHeight());
            cil_crop.setImageBitmap(mBitmap);
        }
    }


    private void save() {
        mBitmap = cil_crop.clip();
        if (mBitmap != null) {
            try {
                String tempPath = FileUtils.getLocalCacheFilePath(Environment.DIRECTORY_PICTURES+ File.separator + Constants.TEMP);
                String cropPath = tempPath + System.currentTimeMillis() + "_crop.jpg";
                File file = new File(cropPath);
                try {
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    if (file.exists()) {
                        file.delete();
                    } else {
                        file.createNewFile();
                    }
                } catch (Exception e) {

                }

                BitmapUtils.writeBitmapToFile(mBitmap, file, 90);
                Intent data = new Intent();
                String path = file.getAbsolutePath();
                data.putExtra("crop_path", path);
                // 媒体资料那边需要上传原始图片
                data.putExtra("original_path", mPath);
                setResult(RESULT_OK, data);
                finish();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "裁剪图片失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBitmap = null;
    }
}
