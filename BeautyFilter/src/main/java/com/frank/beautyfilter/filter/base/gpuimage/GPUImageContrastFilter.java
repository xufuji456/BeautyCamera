package com.frank.beautyfilter.filter.base.gpuimage;

import android.opengl.GLES20;

/**
 * @author xufulong
 * @date 2022/6/18 11:48 下午
 * @desc
 */
public class GPUImageContrastFilter extends GPUImageFilter {

    public final static String CONTRAST_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "uniform lowp float contrast;\n" +
                    "void main() {\n" +
                        "lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                        "gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);\n" +
                    "}";

    private float mContrast;
    private int mContrastLocation;

    public GPUImageContrastFilter() {
        this(1.0f);
    }

    public GPUImageContrastFilter(float contrast) {
        super(NORMAL_VERTEX_SHADER, CONTRAST_FRAGMENT_SHADER);
        mContrast = contrast;
    }

    @Override
    protected void onInit() {
        super.onInit();
        mContrastLocation = GLES20.glGetUniformLocation(getProgramId(), "contrast");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        setContrast(mContrast);
    }

    public void setContrast(final float contrast) {
        mContrast = contrast;
        setFloat(mContrastLocation, contrast);
    }

}
