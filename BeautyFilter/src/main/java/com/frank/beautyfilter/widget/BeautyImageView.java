package com.frank.beautyfilter.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.frank.beautyfilter.BeautyManager;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.filter.helper.BeautyFilterAdjuster;
import com.frank.beautyfilter.filter.helper.BeautyFilterParam;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.beautyfilter.helper.SavePictureTask;
import com.frank.beautyfilter.util.OpenGLUtil;
import com.frank.beautyfilter.util.TextureRotateUtil;
import com.frank.beautyfilter.widget.base.BeautyBaseView;

import java.io.File;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/25 5:15 下午
 * @desc
 */
public class BeautyImageView extends BeautyBaseView {

    private Bitmap mOriginBitmap;

    private boolean mIsSaving = false;

    private SavePictureTask mPictureTask;

    private final BeautyManager mBeautyManager;

    private BeautyFilterAdjuster mFilterAdjust;

    public BeautyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFilter = new GPUImageFilter();
        mFilterAdjust = new BeautyFilterAdjuster(mFilter);
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
        mFilter.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        adjustImageDisplaySize();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClearColor(0, 0, 0, 0);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        if (mTextureId == OpenGLUtil.NO_TEXTURE) {
            mTextureId = OpenGLUtil.loadTexture(mBeautyManager.getBitmap(), OpenGLUtil.NO_TEXTURE);
        }
        mFilter.onDrawFrame(mTextureId, mVertexBuffer, mTextureBuffer);
    }

    @Override
    public void savePicture(SavePictureTask task) {

    }

    @Override
    protected void onFilterChanged(GPUImageFilter filter) {
        super.onFilterChanged(filter);
        if (filter != null) {
            mFilterAdjust = new BeautyFilterAdjuster(filter);
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
        deleteTextures();
        this.requestRender();
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
//        super.onDestroy();
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
            deleteTextures();
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

    public void adjustFilter(int percent, BeautyFilterType type) {
        if (mFilterAdjust != null && mFilterAdjust.canAdjust()) {
            mFilterAdjust.adjust(percent, type);
            this.requestRender();
        }
    }

    private void getBitmapFromGLSurface(Bitmap bitmap, boolean newTexture) {
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] frameBuffers = new int[1];
                int[] frameBufferTextures = new int[1];
                GLES30.glGenFramebuffers(1, frameBuffers, 0);
                GLES30.glGenTextures(1, frameBufferTextures, 0);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frameBufferTextures[0]);
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height,
                        0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffers[0]);
                GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                        GLES30.GL_TEXTURE_2D, frameBufferTextures[0], 0);
                int textureId;
                if (newTexture) {
                    textureId = OpenGLUtil.loadTexture(bitmap, OpenGLUtil.NO_TEXTURE, true);
                } else {
                    textureId = mTextureId;
                }
                mFilter.onDrawFrame(textureId);
                IntBuffer buffer = IntBuffer.allocate(width * height);
                GLES30.glReadPixels(0 , 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);
                Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                newBitmap.copyPixelsFromBuffer(buffer);
                if (newTexture) {
                    GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
                }
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
                GLES30.glDeleteTextures(1, frameBufferTextures, 0);
                GLES30.glDeleteFramebuffers(1, frameBuffers, 0);
                GLES30.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
                mFilter.destroy();
                mFilter.init();
                mFilter.onOutputSizeChanged(mImageWidth, mImageHeight);
            }
        });
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
