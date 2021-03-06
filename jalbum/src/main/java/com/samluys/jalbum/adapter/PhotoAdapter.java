package com.samluys.jalbum.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.samluys.jalbum.R;
import com.samluys.jalbum.activity.FilePhotoSeeSelectedActivity;
import com.samluys.jalbum.activity.PhotoActivity;
import com.samluys.jalbum.common.Constants;
import com.samluys.jalbum.common.DiskLruCache;
import com.samluys.jalbum.common.ImageLoader;
import com.samluys.jalbum.common.Util;
import com.samluys.jalbum.entity.FileEntity;
import com.samluys.jutils.DateUtils;
import com.samluys.jutils.FileUtils;
import com.samluys.jutils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


public class PhotoAdapter extends BaseAdapter {
    private List<String> mSelectPath = new ArrayList<>();
    private Context mContext;
    private String mDitPath;
    private List<FileEntity> mFileEntitys;
    private List<FileEntity> mImgFileEntitys;
    private LayoutInflater mInflater;
    private Handler handler;
    private int maxNum;
    private Activity activity;
    private boolean isShowTakePhoto = true;
    private DiskLruCache mVideoCoverCache;
    private int itemWidth;
    private String videoCoverCacheFolder;
    // 当前正在选择
    private List<String> mChoosingPath = new ArrayList<>();

    public PhotoAdapter(Context context, List<FileEntity> mDatas, String dirpath,
                        List<String> mSelectPath, Handler handler, Activity activity,
                        int maxNum,  boolean showTakePhoto) {
        this.mContext = context;
        this.handler = handler;
        this.mDitPath = dirpath;
        this.mFileEntitys = mDatas;
        this.maxNum = maxNum;
        this.activity = activity;
        this.isShowTakePhoto = showTakePhoto;
        if (mSelectPath != null) {
            this.mSelectPath = mSelectPath;
        }
        this.videoCoverCacheFolder = Utils.getContext().getCacheDir() + "/video_cover_cache" + File.separator;
        initVideoCoverCache();
        mInflater = LayoutInflater.from(context);
        itemWidth = context.getResources().getDisplayMetrics().widthPixels / 4 - 1;
        FileUtils.createNoMediaFile(videoCoverCacheFolder);
    }

    private void initVideoCoverCache() {
        File file = new File(videoCoverCacheFolder);
        FileUtils.makeDirs(videoCoverCacheFolder);

        try {
            mVideoCoverCache = DiskLruCache.open(file, 1000, 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        if (isShowTakePhoto) {
            return mFileEntitys.size() + 1;
        } else {
            return mFileEntitys.size();
        }
    }

    @Override
    public Object getItem(int position) {
        return mFileEntitys.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {

            convertView = mInflater.inflate(R.layout.item_photo_activity, parent, false);
            holder = new ViewHolder();
            holder.item_image = convertView.findViewById(R.id.item_image);
            holder.mSelect = convertView.findViewById(R.id.id_item_select);
            holder.item_take_photo_image = convertView.findViewById(R.id.item_take_photo_image);
            holder.imvVideoMark = convertView.findViewById(R.id.imv_video_mark);
            holder.tvVideoDuration = convertView.findViewById(R.id.tv_video_duration);
            convertView.setLayoutParams(new GridView.LayoutParams(itemWidth, itemWidth));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (isShowTakePhoto && position == 0) {//拍照
            try {
                holder.mSelect.setVisibility(View.GONE);
                holder.item_image.setVisibility(View.GONE);
                holder.item_take_photo_image.setVisibility(View.VISIBLE);
                holder.imvVideoMark.setVisibility(View.GONE);
                holder.tvVideoDuration.setVisibility(View.GONE);
                holder.item_take_photo_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Utils.isFastDoubleClick()) {
                            return;
                        }
                        handler.sendEmptyMessage(1567);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                final FileEntity fileEntity;
                if (isShowTakePhoto) {
                    fileEntity = mFileEntitys.get(position - 1);
                } else {
                    fileEntity = mFileEntitys.get(position);
                }
                if (fileEntity.getType() == FileEntity.IMAGE) {
                    holder.item_take_photo_image.setVisibility(View.GONE);
                    holder.item_image.setVisibility(View.VISIBLE);
                    holder.mSelect.setVisibility(View.VISIBLE);
                    holder.imvVideoMark.setVisibility(View.GONE);
                    holder.tvVideoDuration.setVisibility(View.GONE);

                    holder.mSelect.setImageResource(R.drawable.picture_unselected);
                    holder.item_image.setColorFilter(null);
                    String filePath = null;
                    if (mDitPath.equals("allimgs") || mDitPath.equals("allVideos")) {
                        filePath = Constants.FILE_PREX + fileEntity.getPath();
                    } else {
                        filePath = Constants.FILE_PREX + mDitPath + "/" + fileEntity.getPath();
                    }
                    ImageLoader.loadResize(holder.item_image, filePath, itemWidth, itemWidth);
                    final String finalFilePath = filePath;
                    holder.mSelect.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int size = mSelectPath.size();
                            //已经被选择
                            if (mSelectPath.contains(finalFilePath)) {

                                if (mChoosingPath.contains(finalFilePath)) {
                                    mChoosingPath.remove(finalFilePath);
                                }

                                mSelectPath.remove(finalFilePath);
                                holder.item_image.setColorFilter(null);
                                holder.mSelect.setImageResource(R.drawable.picture_unselected);
                                Message msg = new Message();
                                msg.what = PhotoActivity.CHANGE_NUM;
                                msg.arg1 = mSelectPath.size();
                                handler.sendMessage(msg);
                            } else {//未被选择
                                if (size >= maxNum) {
                                    Toast.makeText(mContext, "最多选择" + maxNum + "张", Toast.LENGTH_LONG).show();

                                } else {
                                    mSelectPath.add(finalFilePath);
                                    mChoosingPath.add(finalFilePath);
                                    holder.item_image.setColorFilter(Color.parseColor("#77000000"));
                                    holder.mSelect.setImageResource(R.drawable.pictures_selected);
                                    Message msg = new Message();
                                    msg.what = PhotoActivity.CHANGE_NUM;
                                    msg.arg1 = mSelectPath.size();
                                    handler.sendMessage(msg);
                                }
                            }
                        }
                    });

                    if (mSelectPath.contains(filePath)) {
                        holder.item_image.setColorFilter(Color.parseColor("#77000000"));
                        holder.mSelect.setImageResource(R.drawable.pictures_selected);
                    }

                    holder.item_image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            Intent intent = new Intent(mContext, FilePhotoSeeSelectedActivity.class);
                            if (mDitPath.equals("allimgs")) {
                                intent.putExtra("position", mImgFileEntitys.indexOf(fileEntity));
                            } else {
                                if (isShowTakePhoto) {
                                    intent.putExtra("position", position - 1);
                                } else {
                                    intent.putExtra("position", position);
                                }
                            }
                            intent.putExtra("max_size", maxNum);
                            intent.putExtra("dirpath", "" + mDitPath);
                            intent.putExtra("selectimage", (Serializable) mSelectPath);
                            PhotoActivity.setImagePathInPhone(getImagePath(mFileEntitys));//这里这么写是防止图片数量过多，导致intent传递的数据量过大导致奔溃。
                            activity.startActivityForResult(intent, 888);
                        }
                    });
                } else if (fileEntity.getType() == FileEntity.VIDEO) {
                    holder.item_take_photo_image.setVisibility(View.GONE);
                    holder.item_image.setVisibility(View.VISIBLE);
                    holder.mSelect.setVisibility(View.GONE);
                    holder.imvVideoMark.setVisibility(View.VISIBLE);
                    holder.tvVideoDuration.setVisibility(View.VISIBLE);

                    holder.item_take_photo_image.setVisibility(View.GONE);
                    holder.item_image.setVisibility(View.VISIBLE);
                    holder.mSelect.setVisibility(View.GONE);
                    holder.imvVideoMark.setVisibility(View.VISIBLE);
                    holder.tvVideoDuration.setVisibility(View.VISIBLE);
                    holder.tvVideoDuration.setText(DateUtils.formatVideoDuration(fileEntity.getDuration()));

                    GetVideoFrameTask oldTask = (GetVideoFrameTask) holder.item_image.getTag();
                    if (oldTask != null) {
                        oldTask.cancel(true);
                    }
                    GetVideoFrameTask newTask = new GetVideoFrameTask(holder.item_image);
                    newTask.execute(fileEntity);
                    holder.item_image.setTag(newTask);

                    String selectVideoPath = fileEntity.getPath();
                    if (!selectVideoPath.startsWith("file://")) {
                        selectVideoPath = "file://" + selectVideoPath;
                    }

                    holder.item_image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mSelectPath.size() > 0) {
                                Toast.makeText(mContext, "不能同时选择图片和视频", Toast.LENGTH_SHORT).show();
                            } else {
                                Message msg = new Message();
                                msg.what = PhotoActivity.MSG_VIEW_VIDEO;
                                if (isShowTakePhoto) {
                                    msg.arg1 = position - 1;
                                } else {
                                    msg.arg1 = position;
                                }
                                msg.obj = fileEntity;
                                handler.sendMessage(msg);
                            }
                        }
                    });

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return convertView;
    }


    private List<String> getImagePath(List<FileEntity> mFilePaths) {
        List<String> images = new ArrayList<>();
        for (FileEntity entity : mFilePaths) {
            if (entity.getType() == FileEntity.IMAGE) {
                images.add(entity.getPath());
            }
        }
        return images;
    }

    public List<String> getmSelectPath() {
        if (mSelectPath == null) {
            return null;
        }
        return mSelectPath;
    }

    public void setmSelectPath(List<String> mSelectPath) {
        if (mSelectPath != null) {
            this.mSelectPath = mSelectPath;
        } else {
            this.mSelectPath.clear();
        }
        this.notifyDataSetChanged();
    }

    public void setAllImage(List<FileEntity> allImageFile) {
        this.mImgFileEntitys = allImageFile;
    }

    private class ViewHolder {
        SimpleDraweeView item_image;
        ImageButton mSelect;
        SimpleDraweeView item_take_photo_image;
        ImageView imvVideoMark;
        TextView tvVideoDuration;
    }

    class GetVideoFrameTask extends AsyncTask<FileEntity, Integer, String> {

        private SimpleDraweeView simpleDraweeView;

        public GetVideoFrameTask(SimpleDraweeView imageView) {
            this.simpleDraweeView = imageView;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            simpleDraweeView.setImageURI((new Uri.Builder()).scheme("res").path(String.valueOf(R.drawable.pictures_no)).build());
        }

        @Override
        protected String doInBackground(FileEntity... params) {
            try {
                String originThumbPath = getVideoCoverPath(params[0].getPath());
                String key = hashKeyForDisk(originThumbPath);
                String cacheFilePath = videoCoverCacheFolder + key + ".0";
                if (!FileUtils.isFileExist(cacheFilePath)) {
                    try {
                        if (mVideoCoverCache != null) {
                            DiskLruCache.Editor editor = null;
                            editor = mVideoCoverCache.edit(key);
                            if (editor != null) {
                                OutputStream outputStream = editor.newOutputStream(0);
                                Bitmap bitmap = Util.getVideoThumbnail(Utils.getContext().getContentResolver(), params[0].getVideoId());
                                if (bitmap != null) {
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                                    outputStream.flush();
                                    outputStream.close();
                                    bitmap.recycle();
                                    bitmap = null;
                                    editor.commit();
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return cacheFilePath;
            } catch (Exception e) {
                //如果使用Lru缓存报错，则改用原始的图片缓存方式。
                String thumbPath = getVideoCoverPath(params[0].getPath());
                if (!FileUtils.isFileExist(thumbPath)) {
                    Util.saveBitmap(Util.getVideoThumbnail(Utils.getContext().getContentResolver(), params[0].getVideoId()), thumbPath);
                }
                return thumbPath;
            }
        }

        @Override
        protected void onPostExecute(String path) {
            if (!TextUtils.isEmpty(path)) {
                simpleDraweeView.setImageURI(Uri.parse("file://" + path));
            }
        }

        public String hashKeyForDisk(String key) {
            String cacheKey;
            try {
                final MessageDigest mDigest = MessageDigest.getInstance("MD5");
                mDigest.update(key.getBytes());
                cacheKey = Util.bytesToHexString(mDigest.digest());
            } catch (NoSuchAlgorithmException e) {
                cacheKey = String.valueOf(key.hashCode());
            }
            return cacheKey;
        }
    }



    /**
     * 关闭缓存
     */
    public void closeCache() {
        if (mVideoCoverCache != null) {
            try {
                mVideoCoverCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getVideoCoverPath(String videoPath) {
        String path =  videoCoverCacheFolder + new File(videoPath).getName().replace("mp4", "jpg");
        FileUtils.makeDirs(videoCoverCacheFolder);
        return path;
    }

}
