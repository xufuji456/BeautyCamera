package com.frank.media.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.PixelCopy;
import android.view.Surface;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtil {

    public static String path = Environment.getExternalStorageDirectory().getPath() + "/screen_shot.jpg";

    public static boolean savePhoto(Bitmap bitmap, String path, Context context) {
        if (bitmap == null || TextUtils.isEmpty(path) || context == null) {
            return false;
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public static Bitmap copyBitmapFromPixel(Surface surface, int width, int height, int rotateDegree) {
        if (height > 1080) {
            width = width * 1080 / height;
            height = 1080;
        }
        if (rotateDegree == 90 || rotateDegree == 270) {
            int temp = width;
            //noinspection
            width  = height;
            height = temp;
        }
        Bitmap bitmap;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                if (bitmap == null || surface == null) return null;
                Handler mHandler = new Handler(Looper.getMainLooper());
                ConditionVariable conditionVariable = new ConditionVariable();
                final int[] result = {-1};
                PixelCopy.request(surface, bitmap, copyResult -> {
                    result[0] = copyResult;
                    conditionVariable.open();
                }, mHandler);
                conditionVariable.block(2000);
                if (result[0] != PixelCopy.SUCCESS) {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return bitmap;
    }

}
