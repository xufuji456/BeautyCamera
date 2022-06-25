package com.frank.beautyfilter.filter.base;

import android.opengl.GLES20;

import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.OpenGLUtil;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * @author xufulong
 * @date 2022/6/20 12:27 上午
 * @desc
 */
public class BeautyAdjustFilter extends GPUImageFilter {

    private int frameWidth = -1;
    private int frameHeight = -1;

    protected List<GPUImageFilter> filters;
    protected int[] frameBuffer;
    protected int[] frameBufferTexture;

    public BeautyAdjustFilter(List<GPUImageFilter> filters) {
        this.filters = filters;
    }

    @Override
    public void init() {
        for (GPUImageFilter filter : filters) {
            filter.init();
        }
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);

        int size = filters.size();
        for (int i=0; i<size; i++) {
            filters.get(i).onInputSizeChanged(width, height);
        }
        if (frameBuffer != null && (width != frameWidth || height != frameHeight || frameBuffer.length != size-1)) {
            destroyFrameBuffer();
            frameWidth = width;
            frameHeight = height;
        }
        if (frameBuffer == null) {
            frameBuffer = new int[size-1];
            frameBufferTexture = new int[size-1];

            for (int i=0; i<size-1; i++) {
                GLES20.glGenFramebuffers(1, frameBuffer, i);
                GLES20.glGenTextures(1, frameBufferTexture, i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[i]);

                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[i]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, frameBufferTexture[i], 0);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
    }

    private int drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        if (frameBuffer == null || frameBufferTexture == null) {
            return OpenGLUtil.NOT_INIT;
        }

        int prevTextureId = textureId;
        for (int i=0; i<filters.size(); i++) {
            GPUImageFilter filter = filters.get(i);
            boolean isLast = (i == filters.size() - 1);
            if (isLast) {
                GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
                filter.onDrawFrame(prevTextureId, vertexBuffer, textureBuffer);
            } else {
                GLES20.glViewport(0, 0, mInputWidth, mInputHeight);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[i]);
                GLES20.glClearColor(0, 0, 0, 0);
                filter.onDrawFrame(prevTextureId, mVertexBuffer, mTextureBuffer);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                prevTextureId = frameBufferTexture[i];
            }
        }
        return OpenGLUtil.ON_DRAWN;
    }

    @Override
    public int onDrawFrame(int textureId) {
        return drawFrame(textureId, mVertexBuffer, mTextureBuffer);
    }

    @Override
    public int onDrawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        return drawFrame(textureId, vertexBuffer, textureBuffer);
    }

    @Override
    protected void onDestroy() {
        for (GPUImageFilter filter : filters) {
            filter.destroy();
        }
        destroyFrameBuffer();
    }

    private void destroyFrameBuffer() {
        if (frameBufferTexture != null) {
            GLES20.glDeleteTextures(frameBufferTexture.length, frameBufferTexture, 0);
            frameBufferTexture = null;
        }
        if (frameBuffer != null) {
            GLES20.glDeleteFramebuffers(frameBuffer.length, frameBuffer, 0);
            frameBuffer = null;
        }
    }

    public int getSize() {
        return filters != null ? filters.size() : 0;
    }
}
