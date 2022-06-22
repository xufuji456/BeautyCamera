package com.frank.beautyfilter.filter.advance;

import android.opengl.GLES20;

import com.frank.beautyfilter.R;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.util.OpenGLUtil;

/**
 * @author xufulong
 * @date 2022/6/21 3:50 下午
 * @desc
 */
public class BeautyBeautifulFilter extends GPUImageFilter {

    private int paramLocation;
    private int stepOffsetLocation;

    public BeautyBeautifulFilter() {
        super(NORMAL_VERTEX_SHADER, OpenGLUtil.readShaderFromSource(R.raw.beauty));
    }

    protected void onInit() {
        super.onInit();
        paramLocation = GLES20.glGetUniformLocation(getProgramId(), "params");
        stepOffsetLocation = GLES20.glGetUniformLocation(getProgramId(), "singleStepOffset");
        setBeautyLevel(BeautyParams.beautyLevel);
    }

    private void setTextSize(int width, int height) {
        setFloatVec2(stepOffsetLocation, new float[]{2.0f / width, 2.0f / height});
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        setTextSize(width, height);
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
}
