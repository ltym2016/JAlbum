package com.samluys.jalbum;

/**
 * @author luys
 * @describe 选项配置
 * @date 2020-01-17
 * @email samluys@foxmail.com
 */
public class SelectionConfig {

    public int maxSelectNum = -1;
    public boolean showGif = true;
    public boolean showVideo = false;
    public boolean showTakePhoto = true;
    public boolean showVideoOnly = false;
    public int maxVideoTime = 10;

    private SelectionConfig() {

    }

    public static SelectionConfig getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final class InstanceHolder {
        private static final SelectionConfig INSTANCE = new SelectionConfig();
    }

}
