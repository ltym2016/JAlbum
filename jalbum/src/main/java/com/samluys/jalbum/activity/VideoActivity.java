package com.samluys.jalbum.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samluys.jalbum.R;
import com.samluys.jalbum.entity.FileEntity;
import com.samluys.jalbum.view.TextureVideoView;
import com.samluys.jutils.ScreenUtils;
import com.samluys.jutils.Utils;
import com.samluys.jutils.log.LogUtils;
import com.samluys.statusbar.StatusBarUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoActivity extends AppCompatActivity {

    Button btnCommit;
    TextureVideoView videoView;
    FileEntity fileEntity;
    private ExecutorService cacheThreadPool = Executors.newFixedThreadPool(12);
    private int mVideoWidth;
    private int mVideoHeight;
    private String video_name;//视频name
    private boolean isNeedCrop;// 是否需要裁剪

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        initView();

        fileEntity = getIntent().getParcelableExtra("video");

        initData();

    }

    private void initData() {
        cacheThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                //直接获取视频宽高，有些机型宽高是反的。所以先获取缩略图，得到比例。异步线程获取，防止卡顿
                Bitmap bitmap = getVideoThumbnail(getContentResolver(), fileEntity.getVideoId());
                mVideoWidth = bitmap.getWidth();
                mVideoHeight = bitmap.getHeight();

                LogUtils.e("bitmap", "mVideoWidth " + mVideoWidth + " mVideoHeight " + mVideoHeight);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkVideoDuration();
                        initVideoSize();
                    }
                });
            }
        });

    }

    private void initView() {
        videoView = findViewById(R.id.videoView);

    }

    private void initVideoSize() {

        video_name = new File(fileEntity.getPath()).getName().replace(".mp4", "");
        int maxHeight;
        if (isNeedCrop) {
            maxHeight = (int)(ScreenUtils.getScreenHeight() -
                    StatusBarUtils.getStatusBarHeight(this) - ScreenUtils.dp2px(this, 250));
        } else {
            maxHeight = ScreenUtils.getScreenHeight();
        }

        int screenWidth = ScreenUtils.getScreenWidth();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) videoView.getLayoutParams();
        if ((float) mVideoHeight / mVideoWidth > (float) maxHeight / screenWidth) {
            lp.height = maxHeight;
            lp.width = (int) ((float) maxHeight * ((float) mVideoWidth / mVideoHeight));
        } else {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth * ((float) mVideoHeight / mVideoWidth));
        }
        LogUtils.e("video", "targetWidth=====>" + lp.width + "targetHeight======>" + lp.height);
        videoView.setLayoutParams(lp);
        videoView.setVideoPath(fileEntity.getPath());
        videoView.start();
    }


    /**
     * 根据视频时间判断是否要裁剪
     */
    private void checkVideoDuration() {
        long duration = fileEntity.getDuration();
        int limitDurationMax = 10;
        if (duration > (limitDurationMax + 1) * 1000) {
            isNeedCrop = true;
        } else {
            isNeedCrop = false;
        }
    }

    /**
     * 获取视频缩略图
     *
     * @param cr
     * @param videoId
     * @return
     */
    public static Bitmap getVideoThumbnail(ContentResolver cr, long videoId) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
//select condition.
//via imageid get the bimap type thumbnail in thumbnail table.
        bitmap = MediaStore.Video.Thumbnails.getThumbnail(cr, videoId,
                MediaStore.Images.Thumbnails.MINI_KIND, options);
        if (bitmap != null) {
            LogUtils.d("thumnnail size width===>" + bitmap.getWidth() + "height====>" + bitmap.getHeight());
        }
        return bitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stop();
        }
        if (cacheThreadPool != null) {
            cacheThreadPool.shutdownNow();
        }

    }
}
