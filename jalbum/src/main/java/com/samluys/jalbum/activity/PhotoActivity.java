package com.samluys.jalbum.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;


import com.samluys.jalbum.SelectionConfig;
import com.samluys.jalbum.view.ListImageDirPopWindow;
import com.samluys.jalbum.R;
import com.samluys.jalbum.adapter.PhotoAdapter;
import com.samluys.jalbum.common.Constants;
import com.samluys.jalbum.entity.FileEntity;
import com.samluys.jalbum.entity.FolderBean;
import com.samluys.jutils.FileUtils;
import com.samluys.jutils.Utils;
import com.samluys.statusbar.StatusBarUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author luys
 * @describe 相册页面
 * @date 2019-11-22
 * @email samluys@foxmail.com
 */
public class PhotoActivity extends AppCompatActivity {


    public static final int REQUEST_CODE_SELECT_PHOTO = 998;
    public static final int REQUEST_CODE_SELECT_VIDEO = 999;

    private static final int DATA_LOADED = 0X110;
    public static final int CHANGE_NUM = 5678;

    public static final int MSG_VIEW_VIDEO = 888;
    private static final int SELECTPHOTO = 527;
    // 调系统相机
    private static final int REQUEST_CODE_TAKE_PICTURE = 1001;
    private static final int REQUEST_CODE_VIDEO = 1002;

    private static final int MSG_FINISH = 666;
    private SelectionConfig mSelectionConfig;
    private int mPhotoNum;
    private boolean mIsShowVideo = false;//是否可以选择本地视频
    private boolean mIsShowVideoOnly = false;//是否只显示本地视频
    private boolean mIsShowTakePhoto = true;//是否显示拍照，默认显示
    private boolean mIsShowGif = true;
    private long maxVideoSize = 0;//视频的大小限制，默认0 不限制
    private String mCurrentPhotoPath;

    public static final String VIDEO_PATH = "video_path";
    public static final String IMAGE_LIST_PATH = "image_list_path";

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_FINISH) {
                mProgressDialog.dismiss();
                Toast.makeText(PhotoActivity.this, "请检查是否拥有读取SD卡权限", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    };

    private Toolbar toolbar;
    private Button btn_commit;
    private RelativeLayout rl_finish;
    private TextView tv_yulan;
    private TextView tv_title;

    private ListImageDirPopWindow popwindow;
    public static List<String> imagePathInPhone = new ArrayList<>();

    private GridView mGridView;
    private List<String> mImgs = new ArrayList<>();
    protected PhotoAdapter adapter;


    private RelativeLayout mBottomLy;
    private TextView mDirname;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<>();

    private List<FileEntity> allImageFile = new ArrayList<>();
    private ArrayList<FileEntity> allVideoFile = new ArrayList<>();
    private List<FileEntity> allFile = new ArrayList<>();
    public static ArrayList<String> selectImages = new ArrayList<>();// 当前选择的图片
    private List<String> mSelectedImages;// 已经选择的图片
    private int allpicSize = 0;

    private ProgressDialog mProgressDialog;


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DATA_LOADED:
                    //绑定数据到View中
                    data2View();
                    initPopwindow();
                    setCommitText();
                    mProgressDialog.dismiss();
                    break;
                case CHANGE_NUM:
                    setCommitText(msg.arg1);
                    break;
                case 1567:
                    takePhoto();
                    break;
                case MSG_VIEW_VIDEO:
                    FileEntity fileEntity = (FileEntity) msg.obj;
                    Intent intent = new Intent(PhotoActivity.this, VideoActivity.class);
                    intent.putExtra("video", fileEntity);
                    startActivityForResult(intent,REQUEST_CODE_VIDEO);
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo);

        Utils.init(this.getApplicationContext());
        StatusBarUtils.StatusBarIconDark(this);
        initView();
        initEvent();
        initDatas();
    }

    private void initView() {
        mSelectionConfig = SelectionConfig.getInstance();
        toolbar = findViewById(R.id.toolbar);
        btn_commit = findViewById(R.id.btn_commit);
        rl_finish = findViewById(R.id.rl_finish);
        tv_yulan = findViewById(R.id.tv_yulan);
        tv_title = findViewById(R.id.tv_title);

        toolbar.setContentInsetsAbsolute(0, 0);
        mGridView = findViewById(R.id.id_gridView);
        mBottomLy = findViewById(R.id.bottom_ly);
        mDirname = (Button) findViewById(R.id.id_dir_name);

        mPhotoNum = mSelectionConfig.maxSelectNum;
        mIsShowGif = mSelectionConfig.showGif;
        mIsShowVideo = mSelectionConfig.showVideo;
        mIsShowVideoOnly = mSelectionConfig.showVideoOnly;
        mIsShowTakePhoto = mSelectionConfig.showTakePhoto;
        selectImages.clear();

        mSelectedImages = new ArrayList<>();
        if (mPhotoNum == -1) {
            mPhotoNum = 9;
        }

        if (mIsShowVideo) {
            tv_title.setText("图片和视频");
        } else {
            tv_title.setText("图片");
        }
    }

    private void initPopwindow() {

        popwindow = new ListImageDirPopWindow(this, mFolderBeans);
        popwindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
            }
        });
        popwindow.setOnDirSelectedListener(new ListImageDirPopWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                if (adapter.getmSelectPath() != null) {


                    List<String> mImgPaths = adapter.getmSelectPath();//当前以及选择的图片
                    if (folderBean.getName().equals("所有图片")) {
                        adapter = new PhotoAdapter(PhotoActivity.this, allFile,
                                "allimgs", mImgPaths,
                                mHandler, PhotoActivity.this,
                                mPhotoNum,
                                mIsShowTakePhoto);
                        adapter.setAllImage(allImageFile);
                    } else if (folderBean.getName().equals("所有视频")) {
                        adapter = new PhotoAdapter(PhotoActivity.this, allVideoFile,
                                "allimgs",
                                mImgPaths,
                                mHandler,
                                PhotoActivity.this,
                                mPhotoNum,
                                mIsShowTakePhoto);
                    } else {
                        mCurrentDir = new File(folderBean.getDir());
                        mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                String lowercaseName = filename.toLowerCase();//大写转小写
                                if (lowercaseName.endsWith(".jpg")
                                        || lowercaseName.endsWith(".jpeg")
                                        || lowercaseName.endsWith("png")
                                        || lowercaseName.endsWith(".gif")) {
                                    return true;
                                }
                                return false;
                            }
                        }));
                        Collections.reverse(mImgs);
                        List<FileEntity> folderImages = new ArrayList<FileEntity>();
                        for (String path : mImgs) {
                            FileEntity fileEntity = new FileEntity();
                            fileEntity.setPath(path);
                            fileEntity.setType(FileEntity.IMAGE);
                            folderImages.add(fileEntity);
                        }

                        adapter = new PhotoAdapter(PhotoActivity.this, folderImages,
                                mCurrentDir.getAbsolutePath(), mImgPaths, mHandler,
                                PhotoActivity.this,
                                mPhotoNum,
                                mIsShowTakePhoto);
                    }
                    mGridView.setAdapter(adapter);
                    mDirname.setText("" + folderBean.getName());
                }
                popwindow.dismiss();

            }
        });
    }

    /**
     * 绑定数据到view
     */
    private void data2View() {
        if ((allImageFile.isEmpty() || allpicSize == 0) && allVideoFile.isEmpty()) {
            Toast.makeText(PhotoActivity.this, "未扫描到任何图片", Toast.LENGTH_LONG).show();
        }
        if (!allImageFile.isEmpty()) {
            mFolderBeans.get(0).setFirstImgPath(allImageFile.get(0).getPath());
            mFolderBeans.get(0).setVideo(false);
        } else if (!allVideoFile.isEmpty()) {
            mFolderBeans.get(0).setVideo(true);
            mFolderBeans.get(0).setCount(allVideoFile.size());
        }

        List<String> mImgPaths = new ArrayList<>();
        for (String path : mSelectedImages) {
            if (path != null) {
                String url = null;
                if (!path.startsWith(Constants.FILE_PREX) && (!path.startsWith("http://") && !path.startsWith("https://"))) {
                    url = Constants.FILE_PREX + path;
                } else {
                    url = path;
                }
                mImgPaths.add(url);
            }
        }

        adapter = new PhotoAdapter(this, allFile, "allimgs",
                mImgPaths, mHandler, PhotoActivity.this,
                mPhotoNum, mIsShowTakePhoto);
        adapter.setAllImage(allImageFile);
        mGridView.setAdapter(adapter);
        mDirname.setText("" + mFolderBeans.get(0).getName());
    }

    /**
     * 设置顶部选择图片的数量
     *
     * @param num
     */
    private void setCommitText(int num) {
        if (num > 0) {
            tv_yulan.setEnabled(true);
            btn_commit.setEnabled(true);
            btn_commit.setText("完成(" + (num) + "/" + mPhotoNum + ")");
        } else {
            btn_commit.setEnabled(false);
            tv_yulan.setEnabled(false);
            btn_commit.setText("完成");
        }

    }

    private void setCommitText() {

        btn_commit.setEnabled(false);
        btn_commit.setText("完成");
        tv_yulan.setEnabled(false);
    }


    /**
     * 利用ContentProvider扫描手机中所有图片(视频)
     */
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用!", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");
        new Thread() {
            @Override
            public void run() {
                if (mIsShowVideoOnly) {
                    loadAllVideoData();
                } else {
                    loadAllImageData();
                    //通知handler扫描图片完成
                    if (mIsShowVideo) {
                        loadAllVideoData();
                    }
                }
                mHandler.sendEmptyMessage(DATA_LOADED);
            }
        }.start();
    }


    private void initEvent() {
        rl_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mDirname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popwindow.setAnimationStyle(R.style.dir_popupwindow_anim);
                popwindow.showAsDropDown(mBottomLy, 0, 0);
            }
        });
        btn_commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitAndFinish();
            }
        });
        tv_yulan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isFastDoubleClick()) {
                    return;
                }
                Intent intent = new Intent(PhotoActivity.this, PreviewPhotoActivity.class);
                intent.putExtra("list", (Serializable) adapter.getmSelectPath());
                intent.putExtra("max_size", mPhotoNum);
                startActivityForResult(intent, SELECTPHOTO);
            }
        });
    }

    private void commit() {
        getSelectImages().clear();
        getSelectImages().addAll(adapter.getmSelectPath());
    }

    private void commitAndFinish() {

        commit();
        Intent data = new Intent();
        data.putStringArrayListExtra(IMAGE_LIST_PATH,getSelectImages());
        setResult(RESULT_OK, data);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == SELECTPHOTO) {//预览图片后返回
                try {
                    List<String> infos = (List<String>) data.getSerializableExtra("list");
                    if (infos != null) {
                        int size = infos.size();
                        adapter.setmSelectPath(infos);
                        setCommitText(size);
                        boolean shouldCommit = data.getBooleanExtra("should_commit", true);
                        if (shouldCommit) {
                            commitAndFinish();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == 888) {
                boolean close = data.getBooleanExtra("close", false);
                List<String> paths = data.getStringArrayListExtra("simage");
                if (close) {
                    adapter.setmSelectPath(paths);
                    commitAndFinish();
                } else {
                    setCommitText(paths.size());
                    adapter.setmSelectPath(paths);
                }
            } else if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
                Intent intent = new Intent();
                intent.putExtra("take_photo", mCurrentPhotoPath);
                setResult(RESULT_OK, intent);
                finish();
            } else if (requestCode == REQUEST_CODE_VIDEO) {
                if (data != null) {
                    String path = data.getStringExtra(VIDEO_PATH);
                    Intent intent = new Intent();
                    intent.putExtra("PATH", path);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
    }

    /**
     * 从本地获取所有的图片
     */
    private void loadAllImageData() {
        Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = PhotoActivity.this.getContentResolver();
        Cursor cursor = null;
        try {
            String[] queryType = new String[0];

            if (mIsShowGif) {
                queryType = new String[]{"image/jpeg", "image/png", "image/gif"};
            } else {
                queryType = new String[]{"image/jpeg", "image/png"};
            }
            cursor = cr.query(mImgUri, null,
                    MediaStore.Images.Media.MIME_TYPE + "=? or "
                            + MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                    queryType,
                    MediaStore.Images.Media.DATE_TAKEN);
        } catch (SecurityException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.dismiss();
                    Toast.makeText(PhotoActivity.this, "请检查是否拥有读取SD卡权限", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            return;
        }
        Set<String> mDirPath = new HashSet<String>();
        FolderBean allFolderBean = new FolderBean();//所有图片的bean
        allFolderBean.setName("所有图片");
        if (cursor == null) {
            finish();
            return;
        }
        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            long dateTaken = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));
            File parentFile = new File(path).getParentFile();
            File file = new File(path);
            if (TextUtils.isEmpty(path) || parentFile == null || file.length() <= 0) {
                continue;
            }
            File tempFile = new File(path);
            if (!tempFile.exists()) {
                continue;
            }
            String lowercasePath = path.toLowerCase();
            if (lowercasePath.endsWith(".jpg")
                    || lowercasePath.endsWith(".jpeg")
                    || lowercasePath.endsWith(".png")
                    || lowercasePath.endsWith(".gif")) {
                FileEntity fileEntity = new FileEntity();
                fileEntity.setPath(path);
                fileEntity.setDateTaken(dateTaken);
                fileEntity.setType(FileEntity.IMAGE);
                allImageFile.add(fileEntity);
            }
            final String dirPath = parentFile.getAbsolutePath();
            FolderBean folderBean = null;
            if (mDirPath.contains(dirPath)) {
                continue;
            } else {
                mDirPath.add(dirPath);
                folderBean = new FolderBean();
                folderBean.setDir(dirPath);
                folderBean.setFirstImgPath(path);
                folderBean.setVideo(false);
            }

            if (parentFile.list() == null) {
                continue;
            }
            int picSize = parentFile.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    String lowercaseName = filename.toLowerCase();//大写转小写
                    if (lowercaseName.endsWith(".jpg")
                            || lowercaseName.endsWith(".jpeg")
                            || lowercaseName.endsWith(".png")
                            || lowercaseName.endsWith(".gif")) {
                        return true;
                    }
                    return false;
                }
            }).length;
            allpicSize = allpicSize + picSize;
            folderBean.setCount(picSize);
            mFolderBeans.add(0, folderBean);

            if (picSize > mMaxCount) {
                mMaxCount = picSize;
                mCurrentDir = parentFile;
            }

        }
        cursor.close();
        Collections.reverse(allImageFile);
        allFile.addAll(allImageFile);
        allFolderBean.setAllFile(allImageFile);
        allFolderBean.setCount(allpicSize);
        mFolderBeans.add(0, allFolderBean);
    }

    /**
     * 获取所有视频数据
     */
    private void loadAllVideoData() {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = null;
        try {
            if (maxVideoSize <= 0) {
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DATE_TAKEN);
            } else {
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Video.Media.SIZE + "<" + maxVideoSize, null, MediaStore.Video.Media.DATE_TAKEN);
            }
        } catch (Exception e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    Toast.makeText(PhotoActivity.this, "请检查是否拥有读取SD卡权限", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            return;
        }
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                long dateTaken = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN));
                long videoId = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                if (TextUtils.isEmpty(path) || new File(path).length() <= 0) {
                    continue;
                }
                FileEntity fileEntity = new FileEntity();
                fileEntity.setPath(path);
                fileEntity.setDateTaken(dateTaken);
                fileEntity.setType(FileEntity.VIDEO);
                fileEntity.setVideoId(videoId);
                fileEntity.setDuration(duration);
                allVideoFile.add(fileEntity);
            }
            cursor.close();
        }
        if (allVideoFile.size() <= 0) {
            return;
        }
        Collections.reverse(allVideoFile);
        FolderBean videoFolder = new FolderBean();
        videoFolder.setName("所有视频");
        videoFolder.setAllFile(allVideoFile);
        videoFolder.setVideo(true);
        videoFolder.setCount(allVideoFile.size());
        allFile.addAll(allVideoFile);
        Collections.sort(allFile, new Comparator<FileEntity>() {
            @Override
            public int compare(FileEntity o1, FileEntity o2) {
                long dateTaken1 = o1.getDateTaken();
                long dateTaken2 = o2.getDateTaken();
                return -Long.valueOf(dateTaken1).compareTo(dateTaken2);
            }
        });
        if (mFolderBeans.isEmpty()) {
            mFolderBeans.add(0, videoFolder);
        } else {
            mFolderBeans.add(1, videoFolder);
        }
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (photoFile != null) {
                String authority = Utils.getContext().getPackageName() + ".fileprovider";
                mCurrentPhotoPath = photoFile.getAbsolutePath();
                Uri currentPhotoUri = FileProvider.getUriForFile(this,
                        authority, photoFile);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    List<ResolveInfo> resInfoList = this.getPackageManager()
                            .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, currentPhotoUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
                startActivityForResult(captureIntent, REQUEST_CODE_TAKE_PICTURE);
            }
        }
    }


    private File createImageFile() {

        String tempPath = FileUtils.getLocalCacheFilePath(Environment.DIRECTORY_PICTURES+
                File.separator + Constants.TEMP);
        File file = new File(tempPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(tempPath + System.currentTimeMillis() + ".jpg");
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }

        return tempFile;
    }

    @Override
    public void onBackPressed() {

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adapter != null) {
            adapter.closeCache();
        }
    }

    public static List<String> getImagePathInPhone() {
        return imagePathInPhone;
    }

    public static void setImagePathInPhone(List<String> imagePathInPhone) {
        PhotoActivity.imagePathInPhone = imagePathInPhone;
    }

    public static ArrayList<String> getSelectImages() {
        return selectImages;
    }

}
