package com.samluys.jalbum.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.samluys.jalbum.R;
import com.samluys.jalbum.SelectionConfig;
import com.samluys.jalbum.common.Constants;
import com.samluys.jalbum.common.ImageLoader;
import com.samluys.jalbum.entity.FileEntity;
import com.samluys.jalbum.entity.VideoImageEntity;
import com.samluys.jalbum.view.TextureVideoView;
import com.samluys.jalbum.view.TwoSideSeekBar;
import com.samluys.jutils.BitmapUtils;
import com.samluys.jutils.DateUtils;
import com.samluys.jutils.FileUtils;
import com.samluys.jutils.ScreenUtils;
import com.samluys.jutils.Utils;
import com.samluys.jutils.log.LogUtils;
import com.samluys.statusbar.StatusBarUtils;
import com.samluys.uibutton.UIButton;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import io.microshow.rxffmpeg.RxFFmpegSubscriber;

import static com.samluys.jalbum.activity.PhotoActivity.VIDEO_PATH;

public class VideoActivity extends AppCompatActivity {

    Button btnCommit;
    TextureVideoView videoView;
    FileEntity fileEntity;
    private ExecutorService cacheThreadPool = Executors.newFixedThreadPool(12);
    private int mVideoWidth;
    private int mVideoHeight;
    private String video_name;//视频name
    private boolean isNeedCrop;// 是否需要裁剪
    private FrameLayout fl_crop_area;
    private RelativeLayout rl_top_back;
    private ImageView topBack;
    private TextView bottomBack;
    private List<VideoImageEntity> infos;
    private TwoSideSeekBar seekBar;
    private MyAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private boolean isScroll = false;//是否正在滑动
    private float mCurrentX;
    private float mCurrentY = 0;
    private Context mContext;
    private static Handler mHandler;
    private UIButton bottom_finish;
    private TextView tv_select_time;
    private UIButton top_finish;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mContext = this;

        RxFFmpegInvoke.getInstance().setDebug(Utils.isDebug());
        mHandler = new Handler();
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
                        initSeekBar();
                        initVideoSize();
                        initRecyclerView();
                    }
                });
            }
        });

    }

    private void initView() {
        videoView = findViewById(R.id.videoView);
        fl_crop_area = findViewById(R.id.fl_crop_area);
        rl_top_back = findViewById(R.id.rl_top_back);
        topBack = findViewById(R.id.topBack);
        bottomBack = findViewById(R.id.tv_cancel);
        seekBar = findViewById(R.id.seekBar);
        recyclerView = findViewById(R.id.recyclerView);
        bottom_finish = findViewById(R.id.bottom_finish);
        tv_select_time = findViewById(R.id.tv_select_time);
        top_finish = findViewById(R.id.top_finish);

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
        }

        topBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String path = FileUtils.getLocalCacheFilePath("video_image_cache");
                FileUtils.deleteFile(path);

                finish();
            }
        });

        bottomBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = FileUtils.getLocalCacheFilePath("video_image_cache");
                FileUtils.deleteFile(path);

                finish();
            }
        });

        bottom_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProgressDialog.setTitle("正在处理视频...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                crop();
            }
        });

        top_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(VIDEO_PATH, fileEntity.getPath());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void crop() {

        long startTime = getCurrentTime(mCurrentX, mCurrentY);
        if (startTime < 0) {
            startTime = 0;
        }
        long cropTime = seekBar.getCropTime();

        // 原始视频路径
        String orgPath = fileEntity.getPath();
        // 剪辑后的视频路径
        final String newPath = FileUtils.getLocalCacheFilePath("video_image_cache") + File.separator +
                "crop_" + System.currentTimeMillis() + ".mp4";
        // 选择需要剪辑的时间
        float selectTime =  (float)cropTime/1000;
        int time = Math.round(selectTime);
        // 剪辑的起始时间
        String timeformat = formatTime((int) startTime/1000);
        LogUtils.e(timeformat + ":=:" + time);
        // 剪辑视频命令：ffmpeg -i in.mp4 -ss 00:00:00 -t 10 out.ts
        String text = "ffmpeg -i "+orgPath+" -ss "+timeformat+" -t "+time+" " + newPath;
        String[] commands = text.split(" ");
        RxFFmpegInvoke.getInstance().runCommandRxJava(commands).subscribe(new RxFFmpegSubscriber() {
            @Override
            public void onFinish() {
                if (mProgressDialog != null) {
                    mProgressDialog.cancel();
                }
                LogUtils.e("处理成功");

                Intent intent = new Intent();
                intent.putExtra("PATH", newPath);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onProgress(int progress, long progressTime) {
                if (mProgressDialog != null) {
                    mProgressDialog.setProgress(progress);
                    //progressTime 可以在结合视频总时长去计算合适的进度值
                    mProgressDialog.setMessage("已处理progressTime="+(double)progressTime/1000000+"秒");
                }
            }

            @Override
            public void onCancel() {
                if (mProgressDialog != null) {
                    mProgressDialog.cancel();
                }
                LogUtils.e("已取消");
            }

            @Override
            public void onError(String message) {
                if (mProgressDialog != null) {
                    mProgressDialog.cancel();
                }
                LogUtils.e("出错了 onError：" + message);
            }
        });
    }

    private void initVideoSize() {

        video_name = new File(fileEntity.getPath()).getName().replace(".mp4", "");
        int maxHeight;
        if (isNeedCrop) {
            rl_top_back.setVisibility(View.GONE);
            fl_crop_area.setVisibility(View.VISIBLE);
            maxHeight = (int)(ScreenUtils.getScreenHeight() -
                    StatusBarUtils.getStatusBarHeight(this) - ScreenUtils.dp2px(this, 250));
        } else {
            rl_top_back.setVisibility(View.VISIBLE);
            fl_crop_area.setVisibility(View.GONE);
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
        int limitDurationMax = SelectionConfig.getInstance().maxVideoTime;
        if (duration > (limitDurationMax + 1) * 1000) {
            isNeedCrop = true;
        } else {
            isNeedCrop = false;
        }
    }

    private void initRecyclerView() {
        infos = new ArrayList<>();

        String path = FileUtils.getLocalCacheFilePath("video_image_cache");

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileUtils.createNoMediaFile(path);
        if (fileEntity.getDuration() > seekBar.getmDefaultTotalDuration() * 1000) {
            for (int i = 0; i < (int) (fileEntity.getDuration() / seekBar.getDurationPerGrid() + 2); i++) {
                infos.add(new VideoImageEntity(path + File.separator + video_name + "_" + i + Constants.IMAGE_TYPE));
            }
        } else {
            for (int i = 0; i < 12; i++) {
                infos.add(new VideoImageEntity(path + File.separator + video_name + "_" + i + Constants.IMAGE_TYPE));
            }
        }

        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isScroll = false;
                    videoView.seekTo(getCurrentTime(mCurrentX, mCurrentY));
                    seekBar.resetIndicatorAnimator();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                isScroll = true;
            }
        });
        adapter.notifyDataSetChanged();
    }

    private void initSeekBar() {
        mCurrentX = ScreenUtils.dp2px(this, 30);
        tv_select_time.setText("已选取"+ SelectionConfig.getInstance().maxVideoTime +"秒");
        seekBar.setOnVideoStateChangeListener(new TwoSideSeekBar.OnVideoStateChangeListener() {
            @Override
            public void onStart(float x, float y) {
                mCurrentX = x;
                mCurrentY = y;

                videoView.seekTo(getCurrentTime(mCurrentX, mCurrentY));

                long startTime = getCurrentTime(mCurrentX, mCurrentY);
                if (startTime < 0) {
                    startTime = 0;
                }
                long cropTime = seekBar.getCropTime();
                float selectTime =  (float)cropTime/1000;
                tv_select_time.setText("已选取"+Math.round(selectTime)+"秒");
                LogUtils.e(cropTime +"==="+ startTime);
            }

            @Override
            public void onPause() {
                if (videoView.isPlaying()) {
                    videoView.pause();
                }
            }

            @Override
            public void onEnd() {
                videoView.seekTo(getCurrentTime(mCurrentX, mCurrentY));
            }
        });
    }

    public String formatTime(int time) {
        DecimalFormat decimalFormat = new DecimalFormat("00");
        String hh = decimalFormat.format(time / 3600);
        String mm = decimalFormat.format(time % 3600 / 60);
        String ss = decimalFormat.format(time % 60);

        return hh + ":" + mm + ":" + ss;
    }

    private int getCurrentTime(float x, float y) {
        int position = recyclerView.getChildAdapterPosition(recyclerView.findChildViewUnder(x, y));
        return (position - 1) * seekBar.getDurationPerGrid();
    }

    class MyAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_edit_video, parent, false));
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final MyViewHolder viewHolder = (MyViewHolder) holder;
            viewHolder.sdv_crop.setLayoutParams(new LinearLayout.LayoutParams(seekBar.getSingleWidth(), seekBar.getSingleHeight()));
            final VideoImageEntity info = infos.get(position);
            ImageLoader.loadResize(viewHolder.sdv_crop, "file://" + info.getImagePath(), seekBar.getSingleWidth(), seekBar.getSingleHeight());
            if (!TextUtils.isEmpty(info.getImagePath()) && new File(info.getImagePath()).exists()) {//如果图片路径不为空或者路径图片存在
                info.setAsync(true);
            } else {
                if (!info.isAsync()) {
                    if (!cacheThreadPool.isShutdown() && !isScroll) {
                        cacheThreadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                if (!isScroll) {
                                    info.setAsync(true);
                                    MediaMetadataRetriever metadataRetriever = null;
                                    try {
                                        Log.e("doInBackground", position + "--start==>" + System.currentTimeMillis());
                                        metadataRetriever = new MediaMetadataRetriever();
                                        metadataRetriever.setDataSource(fileEntity.getPath());
                                        Bitmap bitmap = null;
                                        if (android.os.Build.VERSION.SDK_INT >= 27) {
                                            bitmap = metadataRetriever.getScaledFrameAtTime(position * seekBar.getDurationPerGrid() * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 100, 100);
                                        } else {
                                            bitmap = metadataRetriever.getFrameAtTime(position * seekBar.getDurationPerGrid() * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                                        }
                                        if (bitmap != null) {
                                            Bitmap scaleBitmap = BitmapUtils.scaleBitmap(bitmap, seekBar.getSingleWidth() * 2.0f / bitmap.getWidth(), bitmap.getWidth(), bitmap.getHeight());
                                            String path = FileUtils.getLocalCacheFilePath("video_image_cache") + File.separator;
                                            boolean issave = BitmapUtils.saveSViewoBitmapToSdCard(scaleBitmap, path, video_name + "_" + position + Constants.IMAGE_TYPE);
                                            if (scaleBitmap != null && !scaleBitmap.isRecycled()) {
                                                scaleBitmap.recycle();
                                                scaleBitmap = null;
                                            }
                                            if (issave) {
                                                mHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        adapter.notifyItemChanged(position);
                                                    }
                                                });
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        if (metadataRetriever != null) {
                                            metadataRetriever.release();
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            if (fileEntity.getDuration() > seekBar.getmDefaultTotalDuration() * 1000) {
                return (int) (fileEntity.getDuration() / seekBar.getDurationPerGrid() + 2);
            } else {
                return 12;
            }
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            super.onViewRecycled(holder);
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            SimpleDraweeView sdv_crop;

            public MyViewHolder(View itemView) {
                super(itemView);
                sdv_crop = itemView.findViewById(R.id.sdv_crop);
            }
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
        bitmap = MediaStore.Video.Thumbnails.getThumbnail(cr, videoId,
                MediaStore.Images.Thumbnails.MINI_KIND, options);
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
