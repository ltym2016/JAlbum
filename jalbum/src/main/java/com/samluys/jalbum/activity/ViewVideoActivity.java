package com.samluys.jalbum.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.samluys.jalbum.R;
import com.samluys.jalbum.entity.FileEntity;
import com.samluys.jalbum.view.FixedViewPager;
import com.samluys.jalbum.view.PhotoDraweeView;
import com.samluys.jutils.FileUtils;
import com.samluys.jutils.ScreenUtils;
import com.samluys.jutils.StringUtils;
import com.samluys.jutils.Utils;
import com.samluys.jutils.log.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class ViewVideoActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener, View.OnClickListener {

    private static final int LIMIT_DURATION_MIN = 2;

    private static final long DEFAULT_MAX_VIDEO_SIZE = 1073741824L;

    RelativeLayout rlFinish;
    TextView tvPosition;
    Button btnCommit;
    Toolbar toolbar;
    FixedViewPager viewPager;
    TextView tvEdit;
    RelativeLayout rlBottom;
    TextView tvReasonTitle;
    TextView tvReasonDetail;
    TextView tvEditRight;

    private ArrayList<FileEntity> allVideoFile;
    private int currentPostion;
    private ViewVideoPagerAdapter adapter;

    private long mLimitMaxSize;
    private boolean mNeedCheckMaxDuration = true;
    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        init(savedInstanceState);
    }

    protected void init(Bundle savedInstanceState) {
        setContentView(R.layout.activity_view_video);
        initView();
        toolbar.setContentInsetsAbsolute(0, 0);

        mLimitMaxSize = DEFAULT_MAX_VIDEO_SIZE;

        allVideoFile = getIntent().getParcelableArrayListExtra("all_video");
        currentPostion = getIntent().getIntExtra("position", 0);
        mNeedCheckMaxDuration = getIntent().getBooleanExtra("NEED_CHECK_MAX_DURATION", true);



        adapter = new ViewVideoPagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);
        tvEdit.setOnClickListener(this);
        tvEditRight.setOnClickListener(this);
        rlFinish.setOnClickListener(this);
        btnCommit.setOnClickListener(this);

        viewPager.setCurrentItem(currentPostion);
        setCurrentPositionText(currentPostion);
        checkVideoDuration(currentPostion);
        checkVideoSize(currentPostion);
        checkVideoFormat(currentPostion);
    }

    private void initView() {
        rlFinish = findViewById(R.id.rl_finish);
        tvPosition = findViewById(R.id.tv_position);
        btnCommit = findViewById(R.id.btn_commit);
        toolbar = findViewById(R.id.toolbar);
        viewPager = findViewById(R.id.viewPager);
        tvEdit = findViewById(R.id.tv_edit);
        rlBottom = findViewById(R.id.rl_bottom);
        tvReasonTitle = findViewById(R.id.tv_reason_title);
        tvReasonDetail = findViewById(R.id.tv_reason_detail);
        tvEditRight = findViewById(R.id.tv_edit_right);
    }


    /**
     * 根据视频时间显示底部提示
     *
     * @param position
     */
    private boolean checkVideoDuration(int position) {
//        LogUtils.d("media duration====>"+VideoUtil.getDuration(allVideoFile.get(position).getPath()));
        long duration = allVideoFile.get(position).getDuration();
        int limitDurationMax = 10;
        if (duration < LIMIT_DURATION_MIN * 1000) {
            tvEdit.setVisibility(View.GONE);
            tvEditRight.setVisibility(View.GONE);
            btnCommit.setEnabled(false);
            tvReasonTitle.setVisibility(View.VISIBLE);
            tvReasonDetail.setVisibility(View.VISIBLE);
            tvReasonTitle.setText(Utils.getStringFromConfig(R.string.edit_video_short_title));
            tvReasonDetail.setText("不能分享短于" + LIMIT_DURATION_MIN + "s的视频");
            return false;
        } else if (duration > (limitDurationMax + 1) * 1000 && mNeedCheckMaxDuration) {
            tvEdit.setVisibility(View.GONE);
            tvReasonTitle.setVisibility(View.VISIBLE);
            tvReasonDetail.setVisibility(View.VISIBLE);
            btnCommit.setEnabled(false);
            tvReasonTitle.setText(Utils.getStringFromConfig(R.string.edit_video_edit_video_title));
            tvReasonDetail.setText("只能分享" + limitDurationMax + "s内的视频，需进行编辑");
            tvEditRight.setVisibility(View.VISIBLE);
            return false;
        } else {
            tvEdit.setVisibility(View.VISIBLE);
            btnCommit.setEnabled(true);
            tvEditRight.setVisibility(View.GONE);
            tvReasonTitle.setVisibility(View.GONE);
            tvReasonDetail.setVisibility(View.GONE);
            return true;
        }
    }

    /**
     * 根据文件大小显示底部提示
     *
     * @param position
     */
    private boolean checkVideoSize(int position) {
        File file = new File(allVideoFile.get(position).getPath());
        long size = 0;
        long maxSize = mLimitMaxSize;
        if (file.exists()) {
            size = file.length();
        }
        if (size <= 0) {
            tvEdit.setVisibility(View.GONE);
            tvEditRight.setVisibility(View.GONE);
            tvReasonTitle.setVisibility(View.VISIBLE);
            tvReasonDetail.setVisibility(View.VISIBLE);
            btnCommit.setEnabled(false);
            tvReasonTitle.setText(Utils.getStringFromConfig(R.string.edit_video_file_destory_title));
            tvReasonDetail.setText(Utils.getStringFromConfig(R.string.edit_video_file_destory_detail));
            return false;
        } else if (size > maxSize) {//大于1G
            tvEdit.setVisibility(View.GONE);
            tvEditRight.setVisibility(View.GONE);
            tvReasonTitle.setVisibility(View.VISIBLE);
            tvReasonDetail.setVisibility(View.VISIBLE);
            btnCommit.setEnabled(false);
            tvReasonTitle.setText(Utils.getStringFromConfig(R.string.edit_video_big_title));
            String strTooBig = "不能分享大于" + FileUtils.convertSize(maxSize) + "的视频";
            tvReasonDetail.setText(strTooBig);
            return false;
        } else {
            return true;
        }
    }

    /**
     * 判断是否为支持的格式
     *
     * @param currentPostion
     */
    private void checkVideoFormat(int currentPostion) {
        String path = allVideoFile.get(currentPostion).getPath();
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = extractor.getTrackFormat(i);
                String format = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (format.contains("video")) {
                    LogUtils.d("mediaformat===>" + mediaFormat.getString(MediaFormat.KEY_MIME));
                    if (!format.contains("avc") && !format.contains("hevc") && !format.contains("mp4v-es")) {
                        tvEdit.setVisibility(View.GONE);
                        tvEditRight.setVisibility(View.GONE);
                        tvReasonTitle.setVisibility(View.VISIBLE);
                        tvReasonDetail.setVisibility(View.VISIBLE);
                        btnCommit.setEnabled(false);
                        tvReasonTitle.setText(Utils.getStringFromConfig(R.string.edit_video_file_unsupport_type_title));
                        tvReasonDetail.setText(Utils.getStringFromConfig(R.string.edit_video_file_unsupport_type_detail));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (path.contains(".mov") || path.contains(".wmv")) {
            tvEdit.setVisibility(View.GONE);
            tvEditRight.setVisibility(View.GONE);
            tvReasonTitle.setVisibility(View.VISIBLE);
            tvReasonDetail.setVisibility(View.VISIBLE);
            btnCommit.setEnabled(false);
            tvReasonTitle.setText(Utils.getStringFromConfig(R.string.edit_video_file_unsupport_type_title));
            tvReasonDetail.setText(Utils.getStringFromConfig(R.string.edit_video_file_unsupport_type_detail));
        }
    }

    private void setCurrentPositionText(int position) {
        tvPosition.setText((position + 1) + "/" + allVideoFile.size());
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        setCurrentPositionText(position);
        checkVideoDuration(position);
        checkVideoSize(position);
        checkVideoFormat(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_edit || id == R.id.tv_edit_right) {
//            Intent intent = new Intent(this, EditVideoActivity.class);
//            intent.putExtra("video", allVideoFile.get(viewPager.getCurrentItem()));
//            if (getIntent() != null) {
//                intent.putExtras(getIntent().getExtras());
//            }
//            intent.putExtra("FROM_FORUM", mFrom);
//            intent.putExtra("ADD_POSITION", mAddPosition);
//            intent.putExtra("WEBVIEW_TAG", "" + webview_tag);
//            intent.putExtra("JSTYPE", jsType);
//
//            startActivity(intent);
        } else if (id == R.id.rl_finish) {
            finish();
        } else if (id == R.id.btn_commit) {
//            Bitmap bitmap = VideoUtil.getVideoThumbnail(getContentResolver(), allVideoFile.get(viewPager.getCurrentItem()).getVideoId());
//            if (bitmap != null) {
//                if (StringUtils.isEmpty(mFrom)) {//本地圈发布
//                    MyApplication.getBus().post(new SelectVideoEvent());
//
//                    Intent intentPublish = new Intent(mContext, PaiPublishActivity.class);
//                    intentPublish.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    if (getIntent().getExtras() != null) {
//                        intentPublish.putExtras(getIntent().getExtras());
//                    }
//
//                    intentPublish.putExtra(StaticUtil.PaiPublishActivity.VIDEO_PATH, allVideoFile.get(viewPager.getCurrentItem()).getPath());
//                    intentPublish.putExtra(StaticUtil.PaiPublishActivity.NEED_START_PHOTO_SELECT_ACTIVITY, false);
//                    intentPublish.putExtra(StaticUtil.PaiPublishActivity.VIDEO_WIDTH, bitmap.getWidth());
//                    intentPublish.putExtra(StaticUtil.PaiPublishActivity.VIDEO_HEIGHT, bitmap.getHeight());
//                    mContext.startActivity(intentPublish);
//
//                } else if (StaticUtil.PhotoActivity.FROM_FORUM.equals(mFrom)) {//
//
//                    String videoPath = allVideoFile.get(viewPager.getCurrentItem()).getPath();
//                    if (!videoPath.startsWith("file://")) {
//                        videoPath = "file://" + videoPath;
//                    }
//                    MyApplication.getmSeletedImg().add(videoPath);
//
//                    SelectVideoEvent event = new SelectVideoEvent();
//                    event.setVideoPath(allVideoFile.get(viewPager.getCurrentItem()).getPath());
//                    event.setAddPosition(mAddPosition);
//                    event.setVideoWidth(bitmap.getWidth());
//                    event.setVideoHeight(bitmap.getHeight());
//
//                    MyApplication.getBus().post(event);
//                } else if (StaticUtil.PhotoActivity.FROM_EDIT_INFO.equals(mFrom)) {//
//                    String videoPath = allVideoFile.get(viewPager.getCurrentItem()).getPath();
//                    if (!videoPath.startsWith("file://")) {
//                        videoPath = "file://" + videoPath;
//                    }
//                    MyApplication.getmSeletedImg().add(videoPath);
//
//                    SelectVideoEvent event = new SelectVideoEvent();
//                    event.setVideoPath(allVideoFile.get(viewPager.getCurrentItem()).getPath());
//                    event.setVideoWidth(bitmap.getWidth());
//                    event.setVideoHeight(bitmap.getHeight());
//                    MyApplication.getBus().post(event);
//                } else if (StaticUtil.PhotoActivity.FROM_CAMERA.equals(mFrom)) {
//                    String videoPath = allVideoFile.get(viewPager.getCurrentItem()).getPath();
//                    if (!videoPath.startsWith("file://")) {
//                        videoPath = "file://" + videoPath;
//                    }
//                    MyApplication.getmSeletedImg().add(videoPath);
//
//                    SelectVideoEvent event = new SelectVideoEvent();
//                    event.setVideoPath(allVideoFile.get(viewPager.getCurrentItem()).getPath());
//                    event.setVideoWidth(bitmap.getWidth());
//                    event.setVideoHeight(bitmap.getHeight());
//                    MyApplication.getBus().post(event);
//                }
//            }
            finish();
        }
    }


    class ViewVideoPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return allVideoFile.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            RelativeLayout layout = (RelativeLayout) LayoutInflater.from(ViewVideoActivity.this).inflate(R.layout.layout_view_video, container, false);
            final PhotoDraweeView imvVideoThumb = layout.findViewById(R.id.imv_video_thumb);
            ImageView imvPlay = layout.findViewById(R.id.imv_play);
            final FileEntity fileEntity = allVideoFile.get(position);
            GetVideoFrameTask newTask = new GetVideoFrameTask(imvVideoThumb);
            newTask.execute(fileEntity);
            imvPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    String bpath = "file://" + fileEntity.getPath();
                    intent.setDataAndType(Uri.parse(bpath), "video/*");
                    if (null != intent.resolveActivity(mContext.getPackageManager())) {
                        mContext.startActivity(intent);
                    }
                }
            });
            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    class GetVideoFrameTask extends AsyncTask<FileEntity, Integer, String> {

        private PhotoDraweeView simpleDraweeView;

        public GetVideoFrameTask(PhotoDraweeView imageView) {
            this.simpleDraweeView = imageView;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            simpleDraweeView.setImageURI((new Uri.Builder())
                    .scheme("res").path(String.valueOf(R.drawable.pictures_no)).build());
        }

        @Override
        protected String doInBackground(FileEntity... params) {
//            String thumbPath = FileUtils.getVideoCoverPath(params[0].getPath());
//            if (!FileUtils.fileIsExists(thumbPath)) {
//                FileUtils.saveBitmap(VideoUtil.getVideoThumbnail(ApplicationUtils.getApp().getContentResolver(), params[0].getVideoId()), thumbPath);
//            }
            return params[0].getPath();
        }

        @Override
        protected void onPostExecute(String path) {
            if (!TextUtils.isEmpty(path)) {
                simpleDraweeView.setImageURI(Uri.parse("file://" + path));
                ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse("file://" + path))
                        .setResizeOptions(new ResizeOptions(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight()))
                        .build();
                PipelineDraweeController controller = (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
                        .setOldController(simpleDraweeView.getController())
                        .setImageRequest(request)
                        .setAutoPlayAnimations(true)
                        .setControllerListener(new BaseControllerListener<ImageInfo>() {
                            @Override
                            public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                                super.onFinalImageSet(id, imageInfo, animatable);
                                if (imageInfo == null) {
                                    return;
                                }
                                simpleDraweeView.update(imageInfo.getWidth(), imageInfo.getHeight());
                            }
                        })
                        .build();
                simpleDraweeView.setController(controller);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
