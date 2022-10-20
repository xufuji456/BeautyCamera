package com.frank.beautyfilter.filter.base.gpuimage;

import android.opengl.GLES30;

/**
 * @author xufulong
 * @date 2022/6/19 12:52 上午
 * @desc
 */
public class GPUImageHueFilter extends GPUImageFilter {

    public final static String HUE_FRAGMENT_SHADER =
            "precision highp float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform highp float hueAdjust;\n" +
            "const highp vec4 kRGBToY = vec4(0.299, 0.587, 0.114, 0.0);\n" +
            "const highp vec4 kRGBToI = vec4(0.595716, -0.274453, -0.321263, 0.0);\n" +
            "const highp vec4 kRGBToQ = vec4(0.211456, -0.522591, 0.31135, 0.0);\n" +
            "const highp vec4 kRGBToR = vec4(1.0, 0.9563, 0.6210, 0.0);\n" +
            "const highp vec4 kRGBToG = vec4(1.0, -0.2721, -0.6474, 0.0);\n" +
            "const highp vec4 kRGBToB = vec4(1.0, -1.1070, 1.7046, 0.0);\n" +
            "void main() {\n" +
                "highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "highp float Y = dot(textureColor, kRGBToY);\n" +
                "highp float I = dot(textureColor, kRGBToI);\n" +
                "highp float Q = dot(textureColor, kRGBToQ);\n" +
                "\n" +
                "highp float hue = atan(Q, I);\n" +
                "highp float chroma = sqrt(I*I + Q*Q);\n" +
                "hue += (-hueAdjust);\n" +
                "Q = chroma * sin(hue);\n" +
                "I = chroma * cos(hue);\n" +
                "\n" +
                "highp vec4 yIQ = vec4(Y, I, Q, 0.0);\n" +
                "textureColor.r = dot(yIQ, kRGBToR);\n" +
                "textureColor.g = dot(yIQ, kRGBToG);\n" +
                "textureColor.b = dot(yIQ, kRGBToB);\n" +
                "gl_FragColor = textureColor;\n" +
            "}";

    private float mHue;
    private int mHueLocation;

    public GPUImageHueFilter() {
        this(0.0f);
    }

    public GPUImageHueFilter(float hue) {
        super(NORMAL_VERTEX_SHADER, HUE_FRAGMENT_SHADER);
        mHue = hue;
    }

    @Override
    protected void onInit() {
        super.onInit();
        mHueLocation = GLES30.glGetUniformLocation(getProgramId(), "hueAdjust");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        setHue(mHue);
    }

    public void setHue(final float hue) {
        mHue = hue;
        float hueAdjust = (float) ((mHue % 360.0f) * Math.PI / 180.0f);
        setFloat(mHueLocation, hueAdjust);
    }
}
