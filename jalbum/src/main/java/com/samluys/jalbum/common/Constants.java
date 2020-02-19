package com.samluys.jalbum.common;

import android.os.Environment;

import com.samluys.jutils.Utils;

import java.io.File;

/**
 * @author luys
 * @describe
 * @date 2020-01-16
 * @email samluys@foxmail.com
 */
public class Constants {

    /**
     * 文件前缀
     */
    public static final String FILE_PREX = "file://";

    /**
     * 图片临时路径
     */
    public static final String TEMP = "temp" + File.separator;

    /**
     * 临时视频每秒关键帧图片缩略图存放文件夹
     */
    public static final String VIDEO_IMAGR_CACHE = "video_image_cache" + File.separator;

    /**
     * 临时视频每秒关键帧图片缩略图保存的后缀名称
     */
    public final static String IMAGE_TYPE = ".jpeg";
}
