package com.frank.beautyfilter.filter.advance;

import android.opengl.GLES20;

import com.frank.beautyfilter.R;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.util.OpenGLUtil;

import java.util.Arrays;

/**
 * @author xufulong
 * @date 2022/6/22 2:57 下午
 * @desc
 */
public class BeautyRiseFilter extends GPUImageFilter {

    private final int[] inputTextureHandles = {-1, -1, -1};
    private final int[] inputTextureUniformLocations = {-1, -1, -1};

    public BeautyRiseFilter() {
        super(NORMAL_VERTEX_SHADER, OpenGLUtil.readShaderFromSource(R.raw.rise));
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(inputTextureHandles.length, inputTextureHandles, 0);
        Arrays.fill(inputTextureHandles, -1);
    }

    protected void onDrawArraysAfter() {
        for (int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGLUtil.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i + 3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }

    protected void onDrawArraysPre() {
        for (int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGLUtil.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i + 3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
            GLES20.glUniform1i(inputTextureUniformLocations[i], (i + 3));
        }
    }

    public void onInit() {
        super.onInit();
        for (int i = 0; i < inputTextureUniformLocations.length; i++) {
            inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(getProgramId(), "inputImageTexture" + (2 + i));
        }
    }

    public void onInitialized() {
        super.onInitialized();
        runOnDraw(new Runnable() {
            public void run() {
                inputTextureHandles[0] = OpenGLUtil.loadTexture(BeautyParams.context, "filter/blackboard1024.png");
                inputTextureHandles[1] = OpenGLUtil.loadTexture(BeautyParams.context, "filter/overlaymap.png");
                inputTextureHandles[2] = OpenGLUtil.loadTexture(BeautyParams.context, "filter/risemap.png");
            }
        });
    }

}
