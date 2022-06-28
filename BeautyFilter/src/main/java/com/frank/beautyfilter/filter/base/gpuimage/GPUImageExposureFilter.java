package com.frank.beautyfilter.filter.base.gpuimage;

import android.opengl.GLES20;

/**
 * @author xufulong
 * @date 2022/6/19 12:45 上午
 * @desc
 */
public class GPUImageExposureFilter extends GPUImageFilter {

    public final static String EXPOSURE_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "uniform highp float exposure;\n" +
                    "void main() {\n" +
                        "highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                        "gl_FragColor = vec4(textureColor.rgb * pow(2.0, exposure), textureColor.w);\n" +
                    "}";

    private float mExposure;
    private int mExposureLocation;

    public GPUImageExposureFilter() {
        this(0.0f);
    }

    public GPUImageExposureFilter(float exposure) {
        super(NORMAL_VERTEX_SHADER, EXPOSURE_FRAGMENT_SHADER);
        mExposure = exposure;
    }

    @Override
    protected void onInit() {
        super.onInit();
        mExposureLocation = GLES20.glGetUniformLocation(getProgramId(), "exposure");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        setExposure(mExposure);
    }

    public void setExposure(final float exposure) {
        mExposure = exposure;
        setFloat(mExposureLocation, mExposure);
    }

}
