package com.frank.beautyfilter.filter.base;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.frank.beautyfilter.R;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.util.OpenGLUtil;

import java.nio.FloatBuffer;

/**
 * @author xufulong
 * @date 2022/6/21 10:50 上午
 * @desc
 */
public class BeautyCameraFilter extends GPUImageFilter {

    private int frameWidth = -1;
    private int frameHeight = -1;
    private int[] frameBuffer = null;
    private int[] frameBufferTexture = null;

    private int paramLocation;
    private int stepOffsetLocation;
    private int textureTransformLocation;
    private float[] textureTransformMatrix;

    public BeautyCameraFilter() {
        super(OpenGLUtil.readShaderFromSource(R.raw.default_vertex),
                OpenGLUtil.readShaderFromSource(R.raw.default_fragment));
    }

    protected void onInit() {
        super.onInit();
        paramLocation = GLES20.glGetUniformLocation(getProgramId(), "params");
        stepOffsetLocation = GLES20.glGetUniformLocation(getProgramId(), "singleStepOffset");
        textureTransformLocation = GLES20.glGetUniformLocation(getProgramId(), "textureTransform");
        setBeautyLevel(BeautyParams.beautyLevel);
    }

    public void setTextureTransformMatrix(float[] matrix) {
        textureTransformMatrix = matrix;
    }

    @Override
    public int onDrawFrame(int textureId) {
        return onDrawFrame(textureId, mVertexBuffer, mTextureBuffer);
    }

    @Override
    public int onDrawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        if (!hasInitialized()) {
            return OpenGLUtil.NOT_INIT;
        }
        GLES20.glUseProgram(getProgramId());
        runPendingOnDrawTask();
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttributePosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mAttributePosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttributeTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoordinate);
        GLES20.glUniformMatrix4fv(textureTransformLocation, 1, false, textureTransformMatrix, 0);

        if (textureId != OpenGLUtil.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mUniformTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mAttributePosition);
        GLES20.glDisableVertexAttribArray(mAttributeTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return OpenGLUtil.ON_DRAWN;
    }

    public int onDrawToTexture(int textureId) {
        if (!hasInitialized()) {
            return OpenGLUtil.NOT_INIT;
        }
        if (frameBuffer == null) {
            return OpenGLUtil.NO_TEXTURE;
        }
        GLES20.glUseProgram(getProgramId());
        runPendingOnDrawTask();
        GLES20.glViewport(0, 0, frameWidth, frameHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttributePosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mAttributePosition);
        mTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttributeTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoordinate);
        GLES20.glUniformMatrix4fv(textureTransformLocation, 1, false, textureTransformMatrix, 0);

        if (textureId != OpenGLUtil.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mUniformTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mAttributePosition);
        GLES20.glDisableVertexAttribArray(mAttributeTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
        return frameBufferTexture[0];
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        setTextSize(width, height);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyFrameBuffer();
    }

    public void initFrameBuffer(int width, int height) {
        if (frameBuffer != null && (frameWidth == -1 || frameHeight == -1))
            destroyFrameBuffer();
        if (frameBuffer == null) {
            frameWidth = width;
            frameHeight = height;
            frameBuffer = new int[1];
            frameBufferTexture = new int[1];

            GLES20.glGenFramebuffers(1, frameBuffer, 0);
            GLES20.glGenTextures(1, frameBufferTexture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, frameBufferTexture[0], 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    private void setTextSize(int width, int height) {
        setFloatVec2(stepOffsetLocation, new float[]{2.0f / width, 2.0f / height});
    }

    public void setBeautyLevel(int beautyLevel) {
        switch (beautyLevel) {
            case 0:
                setFloat(paramLocation, 0.0f);
                break;
            case 1:
                setFloat(paramLocation, 1.0f);
                break;
            case 2:
                setFloat(paramLocation, 0.8f);
                break;
            case 3:
                setFloat(paramLocation, 0.6f);
                break;
            case 4:
                setFloat(paramLocation, 0.4f);
                break;
            case 5:
                setFloat(paramLocation, 0.2f);
                break;
            default:
                break;
        }
    }

    public void onBeautyLevelChanged() {
        setBeautyLevel(BeautyParams.beautyLevel);
    }

    public void destroyFrameBuffer() {
        if (frameBufferTexture != null) {
            GLES20.glDeleteTextures(1, frameBufferTexture, 0);
            frameBufferTexture = null;
        }
        if (frameBuffer != null) {
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
            frameBuffer = null;
        }
        frameWidth = -1;
        frameHeight = -1;
    }

}
