package com.frank.beautyfilter.filter.advance;

import android.opengl.GLES30;

import com.frank.beautyfilter.R;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.OpenGLUtil;

/**
 * @author xufulong
 * @date 2022/6/22 10:20 上午
 * @desc
 */
public class BeautySketchFilter extends GPUImageFilter {

    private int strengthLocation;
    private int stepOffsetLocation;

    public BeautySketchFilter() {
        super(NORMAL_VERTEX_SHADER, OpenGLUtil.readShaderFromSource(R.raw.sketch));
    }

    protected void onInit() {
        super.onInit();
        strengthLocation = GLES30.glGetUniformLocation(getProgramId(), "strength");
        stepOffsetLocation = GLES30.glGetUniformLocation(getProgramId(), "singleStepOffset");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        setFloat(strengthLocation, 0.5f);
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        setFloatVec2(stepOffsetLocation, new float[] {1.0f / width, 1.0f / height});
    }

}
