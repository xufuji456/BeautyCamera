package com.frank.beautyfilter.filter.advance;

import android.opengl.GLES20;

import com.frank.beautyfilter.R;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.util.OpenGLUtil;

/**
 * @author xufulong
 * @date 2022/6/25 6:24 下午
 * @desc
 */
public class BeautyHudsonFilter extends GPUImageFilter {

    private final int[] inputTextureHandles = {-1, -1, -1};
    private final int[] inputTextureUniformLocations = {-1, -1, -1};
    private int mGLStrengthLocation;

    public BeautyHudsonFilter() {
        super(NORMAL_VERTEX_SHADER, OpenGLUtil.readShaderFromSource(R.raw.hudson));
    }

    protected void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(inputTextureHandles.length, inputTextureHandles, 0);
        for (int i = 0; i < inputTextureHandles.length; i++)
            inputTextureHandles[i] = -1;
    }

    @Override
    protected void onDrawArrayBefore() {
        for (int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGLUtil.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i + 3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
            GLES20.glUniform1i(inputTextureUniformLocations[i], (i + 3));
        }
    }

    @Override
    protected void onDrawArrayAfter() {
        for (int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGLUtil.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i + 3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }

    protected void onInit() {
        super.onInit();
        for (int i = 0; i < inputTextureUniformLocations.length; i++) {
            inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(getProgramId(), "inputImageTexture" + (2 + i));
        }
        mGLStrengthLocation = GLES20.glGetUniformLocation(getProgramId(), "strength");
    }

    protected void onInitialized() {
        super.onInitialized();
        setFloat(mGLStrengthLocation, 1.0f);
        runOnDraw(new Runnable() {
            public void run() {
                inputTextureHandles[0] = OpenGLUtil.loadTexture(BeautyParams.context, "filter/hudsonbackground.png");
                inputTextureHandles[1] = OpenGLUtil.loadTexture(BeautyParams.context, "filter/overlaymap.png");
                inputTextureHandles[2] = OpenGLUtil.loadTexture(BeautyParams.context, "filter/hudsonmap.png");
            }
        });
    }
}