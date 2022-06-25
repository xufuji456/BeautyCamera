package com.frank.beautyfilter.filter.advance;

import android.opengl.GLES20;

import com.frank.beautyfilter.R;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.util.OpenGLUtil;

/**
 * @author xufulong
 * @date 2022/6/25 6:12 下午
 * @desc
 */
public class BeautyCrayonFilter extends GPUImageFilter {

    // 1.0--5.0
    private int mStrengthLocation;

    private int mStepOffsetLocation;

    public BeautyCrayonFilter() {
        super(NORMAL_VERTEX_SHADER, OpenGLUtil.readShaderFromSource(R.raw.crayon));
    }

    protected void onInit() {
        super.onInit();
        mStrengthLocation = GLES20.glGetUniformLocation(getProgramId(), "strength");
        mStepOffsetLocation = GLES20.glGetUniformLocation(getProgramId(), "singleStepOffset");
        setFloat(mStrengthLocation, 2.0f); // repeat
    }

    protected void onInitialized() {
        super.onInitialized();
        setFloat(mStrengthLocation, 0.5f); // repeat
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        setFloatVec2(mStepOffsetLocation, new float[] {1.0f / width, 1.0f / height});
    }

    protected void onDestroy() {
        super.onDestroy();
    }

}
