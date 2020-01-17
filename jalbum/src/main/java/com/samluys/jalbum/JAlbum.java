package com.samluys.jalbum;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.samluys.jalbum.activity.PhotoActivity;

import java.lang.ref.WeakReference;

/**
 * @author luys
 * @describe 调用本地相册的入口类
 * @date 2020-01-17
 * @email samluys@foxmail.com
 */
public final class JAlbum {

    private final WeakReference<Activity> mContext;
    private final WeakReference<Fragment> mFragment;

    private JAlbum(Activity activity) {
        this(activity, null);
    }

    private JAlbum(Fragment fragment) {
        this(fragment.getActivity(), fragment);
    }

    private JAlbum(Activity activity, Fragment fragment) {
        mContext = new WeakReference<>(activity);
        mFragment = new WeakReference<>(fragment);
    }

    /**
     * 在Activity里面调用
     * @param activity
     * @return
     */
    public static JAlbum from(Activity activity) {
        return new JAlbum(activity);
    }

    /**
     * 在Fragment里面调用
     * @param fragment
     * @return
     */
    public static JAlbum from(Fragment fragment) {
        return new JAlbum(fragment);
    }

    @Nullable
    Activity getActivity() {
        return mContext.get();
    }

    @Nullable
    Fragment getFragment() {
        return mFragment != null ? mFragment.get() : null;
    }

    public Builder build() {
        return new Builder(getActivity(), getFragment());
    }

    public static class Builder {

        private Activity mActivity;
        private Fragment mFragment;
        private final SelectionConfig mSelectionConfig;

        private Builder(Activity activity, Fragment fragment) {
            mActivity = activity;
            mFragment = fragment;
            mSelectionConfig = SelectionConfig.getInstance();
        }

        /**
         * 设置最大选择数量
         * @param maxSelectNum
         * @return
         */
        public Builder maxSelectNum(int maxSelectNum) {
            mSelectionConfig.maxSelectNum = maxSelectNum;
            return this;
        }

        /**
         * 设置是否显示GIF图片
         * @param showGif
         * @return
         */
        public Builder showGif(boolean showGif) {
            mSelectionConfig.showGif = showGif;
            return this;
        }

        /**
         * 设置是否显示视频
         * @param showVideo
         * @return
         */
        public Builder showVideo(boolean showVideo) {
            mSelectionConfig.showVideo = showVideo;
            return this;
        }

        /**
         * 设置是否开启拍摄功能
         * @param showTakePhoto
         * @return
         */
        public Builder showTakePhoto(boolean showTakePhoto) {
            mSelectionConfig.showTakePhoto = showTakePhoto;
            return this;
        }

        /**
         * 设置是否只显示视频
         * @param showVideoOnly
         * @return
         */
        public Builder showVideoOnly(boolean showVideoOnly) {
            mSelectionConfig.showVideoOnly = showVideoOnly;
            return this;
        }

        public void forResult(int requestCode) {
            if (mActivity == null) {
                return;
            }

            Intent intent = new Intent(mActivity, PhotoActivity.class);

            if (mFragment != null) {
                mFragment.startActivityForResult(intent, requestCode);
            } else {
                mActivity.startActivityForResult(intent, requestCode);
            }
        }
    }

}
