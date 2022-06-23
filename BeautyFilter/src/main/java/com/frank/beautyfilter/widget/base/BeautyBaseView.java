package com.frank.beautyfilter.widget.base;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.filter.factory.BeautyFilterFactory;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.beautyfilter.helper.SavePictureTask;
import com.frank.beautyfilter.util.OpenGLUtil;
import com.frank.beautyfilter.util.Rotation;
import com.frank.beautyfilter.util.TextureRotateUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/22 11:11 上午
 * @desc
 */
public abstract class BeautyBaseView extends GLSurfaceView implements GLSurfaceView.Renderer {

    protected GPUImageFilter filter;

    protected int textureId = OpenGLUtil.NO_TEXTURE;

    protected FloatBuffer mVertexBuffer;

    protected FloatBuffer mTextureBuffer;

    protected int imageWidth, imageHeight;

    protected int surfaceWidth, surfaceHeight;

    protected ScaleType scaleType = ScaleType.FIX_XY;

    public BeautyBaseView(Context context) {
        super(context);
    }

    public BeautyBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mVertexBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.TEXTURE_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(TextureRotateUtil.TEXTURE_VERTEX).position(0);
        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.TEXTURE_ROTATE_0.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TextureRotateUtil.TEXTURE_ROTATE_0).position(0);

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        surfaceWidth = width;
        surfaceHeight = height;
        onFilterChanged();
    }

    protected void onFilterChanged() {
        if (filter != null) {
            filter.onInputSizeChanged(imageWidth, imageHeight);
            filter.onOutputSizeChanged(surfaceWidth, surfaceHeight);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public abstract void savePicture(SavePictureTask task);

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1- distance;
    }

    protected void adjustSize(int rotation, boolean horizontalFlip, boolean verticalFlip) {
        float[] vertexData = TextureRotateUtil.TEXTURE_VERTEX;
        float[] textureData = TextureRotateUtil.getRotateTexture(Rotation.fromInt(rotation),
                horizontalFlip, verticalFlip);
        float ratio1 = (float) surfaceWidth / imageWidth;
        float ratio2 = (float) surfaceHeight / imageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageWidth * ratioMax);
        int imageHeightNew = Math.round(imageHeight * ratioMax);
        float ratioWidth = (float) imageWidthNew / surfaceWidth;
        float ratioHeight = (float) imageHeightNew / surfaceHeight;

        if (scaleType == ScaleType.CENTER_INSIDE) {
            vertexData = new float[] {
                    TextureRotateUtil.TEXTURE_VERTEX[0] / ratioHeight, TextureRotateUtil.TEXTURE_VERTEX[1] / ratioWidth,
                    TextureRotateUtil.TEXTURE_VERTEX[2] / ratioHeight, TextureRotateUtil.TEXTURE_VERTEX[3] / ratioWidth,
                    TextureRotateUtil.TEXTURE_VERTEX[4] / ratioHeight, TextureRotateUtil.TEXTURE_VERTEX[5] / ratioWidth,
                    TextureRotateUtil.TEXTURE_VERTEX[6] / ratioHeight, TextureRotateUtil.TEXTURE_VERTEX[7] / ratioWidth,
            };
        } else if (scaleType == ScaleType.CENTER_CROP) {
            float horizontalDist = (1 - 1 / ratioWidth) / 2;
            float verticalDist   = (1 - 1 / ratioHeight) / 2;
            textureData = new float[] {
                    addDistance(textureData[0], verticalDist), addDistance(textureData[1], horizontalDist),
                    addDistance(textureData[2], verticalDist), addDistance(textureData[3], horizontalDist),
                    addDistance(textureData[4], verticalDist), addDistance(textureData[5], horizontalDist),
                    addDistance(textureData[6], verticalDist), addDistance(textureData[7], horizontalDist),
            };
        } else if (scaleType == ScaleType.FIX_XY) {

        }

        mVertexBuffer.clear();
        mVertexBuffer.put(vertexData).position(0);
        mTextureBuffer.clear();
        mTextureBuffer.put(textureData).position(0);
    }

    protected void deleteTextures() {
        if (textureId != OpenGLUtil.NO_TEXTURE) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    GLES20.glDeleteTextures(1, new int[] {textureId}, 0);
                    textureId = OpenGLUtil.NO_TEXTURE;
                }
            });
        }
    }

    public void setFilter(BeautyFilterType type) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (filter != null)
                    filter.destroy();
                filter = null;
                filter = BeautyFilterFactory.getFilter(type);
                if (filter != null)
                    filter.init();
                onFilterChanged();
            }
        });
        requestRender();
    }

    public enum ScaleType {
        CENTER_INSIDE,
        CENTER_CROP,
        FIX_XY
    }

}