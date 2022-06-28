package com.frank.beautyfilter.filter.base.gpuimage;

import android.opengl.GLES20;

/**
 * @author xufulong
 * @date 2022/6/18 11:21 下午
 * @desc
 */
public class GPUImageBrightnessFilter extends GPUImageFilter {

    public final static String BRIGHTNESS_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform lowp float brightness;\n" +
            "void main() {\n" +
                "lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "gl_FragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.w);\n" +
            "}";

    private float mBrightness;
    private int mBrightnessLocation;

    public GPUImageBrightnessFilter() {
        this(0.0f);
    }

    public GPUImageBrightnessFilter(float brightness) {
        super(NORMAL_VERTEX_SHADER, BRIGHTNESS_FRAGMENT_SHADER);
        mBrightness = brightness;
    }

    @Override
    protected void onInit() {
        super.onInit();
        mBrightnessLocation = GLES20.glGetUniformLocation(getProgramId(), "brightness");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        setBrightness(mBrightness);
    }

    public void setBrightness(final float brightness) {
        mBrightness = brightness;
        setFloat(mBrightnessLocation, mBrightness);
    }

}
