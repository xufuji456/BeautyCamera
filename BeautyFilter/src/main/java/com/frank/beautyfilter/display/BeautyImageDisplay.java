package com.frank.beautyfilter.display;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.frank.beautyfilter.BeautyManager;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.filter.helper.BeautyFilterParam;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.beautyfilter.helper.SavePictureTask;
import com.frank.beautyfilter.util.OpenGLUtil;
import com.frank.beautyfilter.util.TextureRotateUtil;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/26 5:59 下午
 * @desc
 */
public class BeautyImageDisplay extends BeautyBaseDisplay {

    private Bitmap mOriginBitmap;

    private boolean mIsSaving = false;

    private final BeautyManager mBeautyManager;

    private final GPUImageFilter mGpuImageFilter;

    public BeautyImageDisplay(Context context, GLSurfaceView surfaceView) {
        super(context, surfaceView);
        mGpuImageFilter = new GPUImageFilter();
        mBeautyManager = BeautyManager.getInstance();
        mBeautyManager.setHandler(new BeautyHandler());
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES30.glDisable(GL10.GL_DITHER);
        GLES30.glClearColor(0, 0, 0, 0);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        BeautyFilterParam.initFilterParam(gl10);
        mGpuImageFilter.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        adjustImageDisplaySize();
        onFilterChanged();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClearColor(0, 0, 0, 0);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        if (mTextureId == OpenGLUtil.NO_TEXTURE) {
            mTextureId = OpenGLUtil.loadTexture(mBeautyManager.getBitmap(), OpenGLUtil.NO_TEXTURE);
        }
        if (mFilter == null) {
            mGpuImageFilter.onDrawFrame(mTextureId, mVertexBuffer, mTextureBuffer);
        } else {
            mFilter.onDrawFrame(mTextureId, mVertexBuffer, mTextureBuffer);
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled())
            return;
        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mOriginBitmap = bitmap;
        adjustImageDisplaySize();
        mBeautyManager.setBitmap(mOriginBitmap, false);
        refreshDisplay();
    }

    private void refreshDisplay() {
        deleteTexture();
        mGLSurfaceView.requestRender();
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mBeautyManager != null) {
            mBeautyManager.onDestroy();
        }
    }

    private void adjustImageDisplaySize() {
        float ratio1 = (float) mSurfaceWidth / mImageWidth;
        float ratio2 = (float) mSurfaceHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / (float) mSurfaceWidth;
        float ratioHeight = imageHeightNew / (float) mSurfaceHeight;

        float[] cube = new float[]{
                TextureRotateUtil.VERTEX[0] / ratioHeight, TextureRotateUtil.VERTEX[1] / ratioWidth,
                TextureRotateUtil.VERTEX[2] / ratioHeight, TextureRotateUtil.VERTEX[3] / ratioWidth,
                TextureRotateUtil.VERTEX[4] / ratioHeight, TextureRotateUtil.VERTEX[5] / ratioWidth,
                TextureRotateUtil.VERTEX[6] / ratioHeight, TextureRotateUtil.VERTEX[7] / ratioWidth,
        };
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }

    protected void onGetBitmapFromGL(Bitmap bitmap) {
        mOriginBitmap = bitmap;
        if (mIsSaving) {
            mPictureTask.execute(mOriginBitmap);
            mIsSaving = false;
        } else {
            mBeautyManager.setBitmap(mOriginBitmap, false);
        }
    }

    public void restore() {
        if (mFilter != null) {
            setFilter(BeautyFilterType.NONE);
        } else {
            setImageBitmap(mOriginBitmap);
        }
    }

    public void commit() {
        if (mFilter != null) {
            getBitmapFromGLSurface(mOriginBitmap, false);
            deleteTexture();
            setFilter(BeautyFilterType.NONE);
        } else {
            mOriginBitmap.recycle();
            mOriginBitmap = mBeautyManager.getBitmap();
        }
    }

    public void savaImage(File output, SavePictureTask.OnPictureSavedListener listener) {
        mPictureTask = new SavePictureTask(output, listener);
        mIsSaving = true;
        if (mFilter != null) {
            getBitmapFromGLSurface(mOriginBitmap, false);
        } else {
            onGetBitmapFromGL(mOriginBitmap);
        }
    }

    @SuppressLint("HandlerLeak")
    private class BeautyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == BeautyManager.MSG_OPERATION_END) {
                refreshDisplay();
            }
        }
    }


}
