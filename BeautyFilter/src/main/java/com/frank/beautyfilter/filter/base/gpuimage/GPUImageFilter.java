package com.frank.beautyfilter.filter.base.gpuimage;

import android.graphics.PointF;
import android.opengl.GLES20;

import com.frank.beautyfilter.util.OpenGLUtil;
import com.frank.beautyfilter.util.Rotation;
import com.frank.beautyfilter.util.TextureRotateUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * @author xufulong
 * @date 2022/6/18 8:34 下午
 * @desc
 */
public class GPUImageFilter {

    public final static String NORMAL_VERTEX_SHADER =
                    "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "void main() {\n" +
                    "   gl_Position = position;\n" +
                    "   textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "}";

    public final static String NORMAL_FRAGMENT_SHADER =
                    "varying highp vec2 textureCoordinate;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "void main() {" +
                        "gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "}";

    private final String mVertexShader;
    private final String mFragmentShader;
    private final LinkedList<Runnable> mRunnableDraw;

    protected int mProgramId;
    protected int mInputWidth;
    protected int mInputHeight;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected int mUniformTexture;
    protected int mAttributePosition;
    protected int mAttributeTextureCoordinate;
    protected boolean mHasInitialized;
    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureBuffer;

    public GPUImageFilter() {
        this(NORMAL_VERTEX_SHADER, NORMAL_FRAGMENT_SHADER);
    }

    public GPUImageFilter(String vertexShader, String fragmentShader) {
        mRunnableDraw = new LinkedList<>();
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        mVertexBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.TEXTURE_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(TextureRotateUtil.TEXTURE_VERTEX).position(0);

        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.TEXTURE_ROTATE_0.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TextureRotateUtil.getRotateTexture(Rotation.NORMAL, false, true))
                .position(0);
    }

    protected void onInit() {
        mProgramId = OpenGLUtil.loadProgram(mVertexShader, mFragmentShader);
        mAttributePosition = GLES20.glGetAttribLocation(mProgramId, "position");
        mUniformTexture = GLES20.glGetUniformLocation(mProgramId, "inputImageTexture");
        mAttributeTextureCoordinate = GLES20.glGetAttribLocation(mProgramId, "inputTextureCoordinate");
    }

    protected void onInitialized() {

    }

    public void init() {
        onInit();
        mHasInitialized = true;
        onInitialized();
    }

    protected void onDestroy() {

    }

    public void destroy() {
        mHasInitialized = false;
        GLES20.glDeleteProgram(mProgramId);
        onDestroy();
    }

    public void onInputSizeChanged(final int width, final int height) {
        mInputWidth = width;
        mInputHeight = height;
    }

    protected void runPendingOnDrawTask() {
        while (!mRunnableDraw.isEmpty()) {
            mRunnableDraw.removeFirst().run();
        }
    }

    protected void onDrawArrayBefore() {

    }

    protected void onDrawArrayAfter() {

    }

    public int onDrawFrame(final int textureId) {
        return onDrawFrame(textureId, mVertexBuffer, mTextureBuffer);
    }

    public int onDrawFrame(final int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        if (!mHasInitialized)
            return OpenGLUtil.NOT_INIT;

        GLES20.glUseProgram(mProgramId);
        runPendingOnDrawTask();
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttributePosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mAttributePosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttributeTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoordinate);

        if (textureId != OpenGLUtil.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mUniformTexture, 0);
        }

        onDrawArrayBefore();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mAttributePosition);
        GLES20.glDisableVertexAttribArray(mAttributeTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        onDrawArrayAfter();
        return OpenGLUtil.ON_DRAWN;
    }

    public boolean hasInitialized() {
        return mHasInitialized;
    }

    public int getInputWidth() {
        return mInputWidth;
    }

    public int getInputHeight() {
        return mInputHeight;
    }

    public int getProgramId() {
        return mProgramId;
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunnableDraw) {
            mRunnableDraw.addLast(runnable);
        }
    }

    public void setInteger(final int location, final int intVal) {
        runOnDraw(() -> GLES20.glUniform1i(location, intVal));
    }

    public void setFloat(final int location, final float floatVal) {
        runOnDraw(() -> GLES20.glUniform1f(location, floatVal));
    }

    public void setFloatVec2(final int location, final float[] floatArray) {
        runOnDraw(() -> GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(floatArray)));
    }

    public void setFloatVec3(final int location, final float[] floatArray) {
        runOnDraw(() -> GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(floatArray)));
    }

    public void setFloatArray(final int location, final float[] floatArray) {
        runOnDraw(() -> GLES20.glUniform1fv(location, 1, FloatBuffer.wrap(floatArray)));
    }

    protected void setPoint(final int location, final PointF pointF) {
        runOnDraw(() -> {
            float[] vec2 = new float[2];
            vec2[0] = pointF.x;
            vec2[1] = pointF.y;
            GLES20.glUniform2fv(location, 1, vec2, 0);
        });
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

}
