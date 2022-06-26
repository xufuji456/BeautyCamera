package com.frank.beautyfilter.filter.advance;

import android.opengl.GLES20;

import com.frank.beautyfilter.R;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.util.OpenGLUtil;

import java.util.Arrays;

/**
 * @author xufulong
 * @date 2022/6/22 10:27 上午
 * @desc
 */
public class BeautyPixarFilter extends GPUImageFilter {

    private int strengthLocation;
    private final int[] inputTextureHandle = {-1};
    private final int[] inputTextureUniformLocation = {-1};

    public BeautyPixarFilter() {
        super(NORMAL_VERTEX_SHADER, OpenGLUtil.readShaderFromSource(R.raw.pixar));
    }

    public void onInit() {
        super.onInit();
        for (int i=0; i<inputTextureUniformLocation.length; i++) {
            strengthLocation = GLES20.glGetUniformLocation(getProgramId(), "strength");
            inputTextureUniformLocation[i] = GLES20.glGetUniformLocation(getProgramId(), "inputImageTexture" + (i+2));
        }
    }

    public void onInitialized() {
        super.onInitialized();
        setFloat(strengthLocation, 1.0f);
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                inputTextureHandle[0] = OpenGLUtil.loadTexture(BeautyParams.context, "filter/pixar_curves.png");
            }
        });
    }

    @Override
    protected void onDrawArrayBefore() {
        for (int i=0; i<inputTextureHandle.length && inputTextureHandle[i] != OpenGLUtil.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandle[i]);
            GLES20.glUniform1i(inputTextureUniformLocation[i], i+3);
        }
    }

    @Override
    protected void onDrawArrayAfter() {
        for (int i=0; i<inputTextureHandle.length && inputTextureHandle[i] != OpenGLUtil.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(1, inputTextureHandle, 0);
        Arrays.fill(inputTextureHandle, OpenGLUtil.NO_TEXTURE);
    }

}