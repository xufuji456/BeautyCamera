package com.frank.beautyfilter.filter.base.gpuimage;

import android.opengl.GLES20;

/**
 * @author xufulong
 * @date 2022/6/19 11:48 下午
 * @desc
 */
public class GPUImageSharpenFilter extends GPUImageFilter {

    public final static String SHARPEN_VERTEX_SHADER =
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 leftTextureCoordinate;\n" +
            "varying vec2 rightTextureCoordinate;\n" +
            "varying vec2 topTextureCoordinate;\n" +
            "varying vec2 bottomTextureCoordinate;\n" +
            "varying float centerMultiplier;\n" +
            "varying float edgeMultiplier;\n" +
            "uniform float imageWidthFactor;\n" +
            "uniform float imageHeightFactor;\n" +
            "uniform float sharpness;\n" +
            "void main() {\n" +
                "gl_Position = position;\n" +
                "mediump vec2 width = vec2(imageWidthFactor, 0.0);\n" +
                "mediump vec2 height = vec2(0.0, imageHeightFactor);\n" +
                "\n" +
                "textureCoordinate = inputTextureCoordinate.xy;\n" +
                "leftTextureCoordinate = inputTextureCoordinate.xy - width;\n" +
                "rightTextureCoordinate = inputTextureCoordinate.xy + width;\n" +
                "topTextureCoordinate = inputTextureCoordinate.xy + height;\n" +
                "bottomTextureCoordinate = inputTextureCoordinate.xy - height;\n" +
                "\n" +
                "centerMultiplier = 1.0 + 4.0 * sharpness;\n" +
                "edgeMultiplier = sharpness;\n" +
            "}";

    public final static String SHARPEN_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 leftTextureCoordinate;\n" +
            "varying highp vec2 rightTextureCoordinate;\n" +
            "varying highp vec2 topTextureCoordinate;\n" +
            "varying highp vec2 bottomTextureCoordinate;\n" +
            "varying highp float centerMultiplier;\n" +
            "varying highp float edgeMultiplier;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "void main() {" +
                "mediump vec3 textureColor = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
                "mediump vec3 leftTextureColor = texture2D(inputImageTexture, leftTextureCoordinate).rgb;\n" +
                "mediump vec3 rightTextureColor = texture2D(inputImageTexture, rightTextureCoordinate).rgb;\n" +
                "mediump vec3 topTextureColor = texture2D(inputImageTexture, topTextureCoordinate).rgb;\n" +
                "mediump vec3 bottomTextureColor = texture2D(inputImageTexture, bottomTextureCoordinate).rgb;\n" +
                "gl_FragColor = vec4((textureColor * centerMultiplier - " +
                    "((leftTextureColor+rightTextureColor+topTextureColor+bottomTextureColor)* edgeMultiplier)), " +
                    "texture2D(inputImageTexture, bottomTextureCoordinate).w);\n" +
            "}";

    private float mSharpness;
    private int mSharpenLocation;
    private int mImageWidthLocation;
    private int mImageHeightLocation;

    public GPUImageSharpenFilter() {
        this(0.0f);
    }

    public GPUImageSharpenFilter(float sharpness) {
        super(SHARPEN_VERTEX_SHADER, SHARPEN_FRAGMENT_SHADER);
        mSharpness = sharpness;
    }

    @Override
    protected void onInit() {
        super.onInit();
        mSharpenLocation = GLES20.glGetUniformLocation(getProgramId(), "sharpen");
        mImageWidthLocation = GLES20.glGetUniformLocation(getProgramId(), "imageWidthFactor");
        mImageHeightLocation = GLES20.glGetUniformLocation(getProgramId(), "imageHeightFactor");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        setSharpness(mSharpness);
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        setFloat(mImageWidthLocation, 1.0f / width);
        setFloat(mImageHeightLocation, 1.0f / height);
    }

    public void setSharpness(final float sharpness) {
        mSharpness = sharpness;
        setFloat(mSharpenLocation, mSharpness);
    }
}
