package com.frank.beautyfilter.display;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;

import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.filter.factory.BeautyFilterFactory;
import com.frank.beautyfilter.filter.helper.BeautyFilterAdjuster;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.beautyfilter.helper.SavePictureTask;
import com.frank.beautyfilter.util.OpenGLUtil;
import com.frank.beautyfilter.util.TextureRotateUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/26 4:10 下午
 * @desc
 */
public class BeautyBaseDisplay implements Renderer {

    protected final Context mContext;

    protected GPUImageFilter mFilter;

    protected SavePictureTask mPictureTask;

    protected final FloatBuffer mVertexBuffer;

    protected final FloatBuffer mTextureBuffer;

    protected int mImageWidth, mImageHeight;

    protected int mSurfaceWidth, mSurfaceHeight;

    protected BeautyFilterAdjuster mFilterAdjust;

    protected final GLSurfaceView mGLSurfaceView;

    protected int mTextureId = OpenGLUtil.NO_TEXTURE;

    private final static int EGL_CONTEXT_VERSION = 2;

    public BeautyBaseDisplay(Context context, GLSurfaceView surfaceView) {
        mContext = context;
        mGLSurfaceView = surfaceView;

        mFilter = BeautyFilterFactory.getFilter(BeautyFilterType.NONE);
        mFilterAdjust = new BeautyFilterAdjuster(mFilter);

        mVertexBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(TextureRotateUtil.VERTEX).position(0);
        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.TEXTURE_ROTATE_0.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TextureRotateUtil.TEXTURE_ROTATE_0).position(0);

        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setEGLContextClientVersion(EGL_CONTEXT_VERSION);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setFilter(BeautyFilterType filterType) {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFilter != null) {
                    mFilter.destroy();
                }
                mFilter = null;
                mFilter = BeautyFilterFactory.getFilter(filterType);
                if (mFilter != null) {
                    mFilter.init();
                }
                onFilterChanged();
                mFilterAdjust = new BeautyFilterAdjuster(mFilter);
            }
        });
    }

    protected void onFilterChanged() {
        if (mFilter != null) {
            mFilter.onInputSizeChanged(mImageWidth, mImageHeight);
            mFilter.onOutputSizeChanged(mSurfaceWidth, mSurfaceHeight);
        }
    }

    protected void onResume() {}

    protected void onPause() {}

    protected void onDestroy() {}

    protected void getBitmapFromGLSurface(Bitmap bitmap, boolean newTexture) {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] frameBuffers = new int[1];
                int[] frameBufferTextures = new int[1];
                GLES20.glGenFramebuffers(1, frameBuffers, 0);
                GLES20.glGenTextures(1, frameBufferTextures, 0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures[0]);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, frameBufferTextures[0], 0);
                int textureId;
                if (newTexture) {
                    textureId = OpenGLUtil.loadTexture(bitmap, OpenGLUtil.NO_TEXTURE, true);
                } else {
                    textureId = mTextureId;
                }
                mFilter.onDrawFrame(textureId);
                IntBuffer buffer = IntBuffer.allocate(width * height);
                GLES20.glReadPixels(0 , 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                newBitmap.copyPixelsFromBuffer(buffer);
                if (newTexture) {
                    GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
                }
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                GLES20.glDeleteTextures(1, frameBufferTextures, 0);
                GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
                GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
                mFilter.destroy();
                mFilter.init();
                mFilter.onOutputSizeChanged(mImageWidth, mImageHeight);
                onGetBitmapFromGLSurface(newBitmap);
            }
        });
    }

    protected void onGetBitmapFromGLSurface(Bitmap bitmap) {

    }

    protected void deleteTexture() {
        if (mTextureId != OpenGLUtil.NO_TEXTURE) {
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
                    mTextureId = OpenGLUtil.NO_TEXTURE;
                }
            });
        }
    }

    public void adjustFilter(int percent, int type) {
        if (mFilterAdjust != null && mFilterAdjust.canAdjust()) {
            mFilterAdjust.adjust(percent, type);
            mGLSurfaceView.requestRender();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {

    }

    @Override
    public void onDrawFrame(GL10 gl10) {

    }
}
