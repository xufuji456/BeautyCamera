package com.frank.beautyfilter;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * @author xufulong
 * @date 2022/6/26 6:19 下午
 * @desc
 */
public class BeautyManager {

    private static final String TAG = BeautyManager.class.getSimpleName();
    
    private Handler mHandler;

    private BeautyListener mBeautyListener;

    private static ByteBuffer mBitmapBuffer;

    public final static int MSG_OPERATION_END = 456;

    static {
        System.loadLibrary("BeautyManager");
    }

    private BeautyManager() {}

    private static final class MBeautyManagerHolder {
        static final BeautyManager mBeautyManager = new BeautyManager();
    }

    public static BeautyManager getInstance() {
        return MBeautyManagerHolder.mBeautyManager;
    }

    public interface BeautyListener {
        void onEnd();
    }

    public void setBeautyListener(BeautyListener listener) {
        mBeautyListener = listener;
    }

    public void initBeauty() {
        if (mBitmapBuffer == null) {
            Log.e(TAG, "bitmap should be set first...");
            return;
        }
        nativeInitBeauty(mBitmapBuffer);
    }

    public void setBitmap(Bitmap bitmap, boolean recycle) {
        if (bitmap == null) {
            return;
        }
        if (mBitmapBuffer != null) {
            freeBitmap();
        }
        mBitmapBuffer = nativeSetBitmap(bitmap);
        if (recycle) {
            bitmap.recycle();
        }
    }
    
    public Bitmap getBitmap() {
        if (mBitmapBuffer == null) {
            return null;
        }
        return nativeGetBitmap(mBitmapBuffer);
    }

    public void freeBitmap() {
        if (mBitmapBuffer == null) {
            return;
        }
        nativeFreeBitmap(mBitmapBuffer);
        mBitmapBuffer = null;
    }
    
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void onStartSkinSmooth(float level) {
        if (mBitmapBuffer == null)
            return;
        if (level < 0 || level > 10) {
            Log.e(TAG, "skin smooth level must be in [0, 10]");
            return;
        }
        nativeStartSkinSmooth(level);
        mHandler.sendEmptyMessage(MSG_OPERATION_END);
        if (mBeautyListener != null) {
            mBeautyListener.onEnd();
        }
    }

    public void onStartSkinWhite(float level) {
        if (mBitmapBuffer == null)
            return;
        if (level < 0 || level > 5) {
            Log.e(TAG, "skin white level must be in [0, 5]");
            return;
        }
        nativeStartSkinWhite(level);
        mHandler.sendEmptyMessage(MSG_OPERATION_END);
        if (mBeautyListener != null) {
            mBeautyListener.onEnd();
        }
    }

    public void unInitBeauty() {
        nativeUnInitBeauty();
    }

    public void onDestroy() {
        freeBitmap();
        unInitBeauty();
    }


    private native ByteBuffer nativeSetBitmap(Bitmap bitmap);

    private native Bitmap nativeGetBitmap(ByteBuffer bitmapBuffer);

    private native void nativeFreeBitmap(ByteBuffer bitmapBuffer);

    private native void nativeInitBeauty(ByteBuffer bitmapBuffer);

    private native void nativeStartSkinSmooth(float level);

    private native void nativeStartSkinWhite(float level);

    private native void nativeUnInitBeauty();

}
