package com.samluys.jalbum.common;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;


import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;

/** Junk drawer of utility methods. */
public class Util {
    static final Charset US_ASCII = Charset.forName("US-ASCII");
    static final Charset UTF_8 = Charset.forName("UTF-8");

    private Util() {
    }

    static String readFully(Reader reader) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, count);
            }
            return writer.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Deletes the contents of {@code dir}. Throws an IOException if any file
     * could not be deleted, or if {@code dir} is not a readable directory.
     */
    static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("not a readable directory: " + dir);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    static void closeQuietly(/*Auto*/Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 保存方法
     */
    public static void saveBitmap(Bitmap bitmap, String path) {
        saveBitmap(bitmap, path, true);
    }

    public static void saveBitmap(Bitmap bitmap, String path, boolean recycle) {
        if (bitmap == null) {
            return;
        }
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            if (recycle) {
                bitmap.recycle();
            }
            bitmap = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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

    private static final char[] DIGITS_LOWER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytesToHexString(byte[] var0) {
        return var0 == null ? "" : bytesToHexString(var0, DIGITS_LOWER);
    }

    public static String bytesToHexString(byte[] var0, char[] var1) {
        int var2 = var0.length;
        char[] var3 = new char[var2 << 1];
        int var4 = 0;

        for(int var5 = 0; var4 < var2; ++var4) {
            var3[var5++] = var1[(240 & var0[var4]) >>> 4];
            var3[var5++] = var1[15 & var0[var4]];
        }

        return new String(var3);
    }
}
