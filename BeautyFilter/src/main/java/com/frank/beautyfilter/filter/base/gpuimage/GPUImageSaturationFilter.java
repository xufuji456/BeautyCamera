package com.frank.beautyfilter.filter.base.gpuimage;

import android.opengl.GLES20;

/**
 * @author xufulong
 * @date 2022/6/19 11:36 下午
 * @desc
 */
public class GPUImageSaturationFilter extends GPUImageFilter {

    public final static String SATURATION_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform lowp float saturation;\n" +
            "const mediump vec3 lumaWeight = vec3(0.2125, 0.7154, 0.0721);\n" +
            "void main() {" +
                "lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "lowp float luma = dot(textureColor.rgb, lumaWeight);\n" +
                "lowp vec3 scaleColor = vec3(luma);\n" +
                "gl_FragColor = vec4(mix(scaleColor, textureColor.rgb, saturation), textureColor.w);\n" +
            "}";

    private float mSaturation;
    private int mSaturationLocation;

    public GPUImageSaturationFilter() {
        this(1.0f);
    }

    public GPUImageSaturationFilter(float saturation) {
        super(NORMAL_VERTEX_SHADER, SATURATION_FRAGMENT_SHADER);
        mSaturation = saturation;
    }

    @Override
    protected void onInit() {
        super.onInit();
        mSaturationLocation = GLES20.glGetUniformLocation(getProgramId(), "saturation");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        setSaturation(mSaturation);
    }

    public void setSaturation(final float saturation) {
        mSaturation = saturation;
        setFloat(mSaturationLocation, mSaturation);
    }

}
