package com.frank.beautyfilter.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.frank.beautyfilter.camera.CameraEngine;
import com.frank.beautyfilter.camera.CameraPrivateInfo;
import com.frank.beautyfilter.filter.advance.BeautyBeautifulFilter;
import com.frank.beautyfilter.filter.base.BeautyCameraFilter;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.beautyfilter.helper.SavePictureTask;
import com.frank.beautyfilter.recorder.video.TextureVideoRecorder;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.util.OpenGLUtil;
import com.frank.beautyfilter.util.Rotation;
import com.frank.beautyfilter.util.TextureRotateUtil;
import com.frank.beautyfilter.widget.base.BeautyBaseView;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/22 10:04 下午
 * @desc
 */
public class BeautyCameraView extends BeautyBaseView {

    private final File outputFile;
    private int recordingStatus;
    protected boolean recordEnable;

    private SurfaceTexture surfaceTexture;
    private BeautyCameraFilter cameraFilter;
    private BeautyBeautifulFilter beautyFilter;

    private final static int RECORDING_OFF = 0;
    private final static int RECORDING_ON = 1;
    private final static int RECORDING_RESUME = 2;

    private final CameraEngine cameraEngine;
    private final TextureVideoRecorder videoRecorder;

    public BeautyCameraView(Context context) {
        this(context, null);
    }

    public BeautyCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.getHolder().addCallback(this);
        recordEnable = false;
        recordingStatus = -1;
        scaleType = ScaleType.CENTER_CROP;
        cameraEngine = new CameraEngine();
        videoRecorder = new TextureVideoRecorder();
        outputFile = new File(BeautyParams.videoPath, BeautyParams.videoName);
    }

    @Override
    public void savePicture(SavePictureTask task) {
        cameraEngine.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                cameraEngine.stopPreview();
                final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        final Bitmap photo = drawPhoto(bitmap, cameraEngine.getCameraInfo().isFront);
                        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
                        task.execute(photo);
                    }
                });
                cameraEngine.startPreview();
            }
        });
    }

    private void openCamera() {
        if (cameraEngine.getCamera() == null)
            cameraEngine.openCamera();
        CameraPrivateInfo info = cameraEngine.getCameraInfo();
        if (info.orientation == 90 || info.orientation == 270) {
            imageWidth = info.previewHeight;
            imageHeight = info.previewWidth;
        } else {
            imageWidth = info.previewWidth;
            imageHeight = info.previewHeight;
        }
        cameraFilter.onInputSizeChanged(imageWidth, imageHeight);
        adjustSize(info.orientation, info.isFront, true);
        if (surfaceTexture != null)
            cameraEngine.startPreview(surfaceTexture);
    }

    private final SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestRender();
        }
    };

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);
        recordEnable = videoRecorder.isRecording();
        if (recordEnable)
            recordingStatus = RECORDING_RESUME;
        else
            recordingStatus = RECORDING_OFF;
        if (cameraFilter == null)
            cameraFilter = new BeautyCameraFilter();
        cameraFilter.init();
        if (textureId == OpenGLUtil.NO_TEXTURE) {
            textureId = OpenGLUtil.getExternalOESTextureId();
            if (textureId != OpenGLUtil.NO_TEXTURE) {
                surfaceTexture = new SurfaceTexture(textureId);
                surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        super.onSurfaceChanged(gl10, width, height);
        openCamera();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        super.onDrawFrame(gl10);
        if (surfaceTexture == null)
            return;
        surfaceTexture.updateTexImage();
        if (recordEnable) {
            switch (recordingStatus) {
                case RECORDING_OFF:
                    CameraPrivateInfo info = cameraEngine.getCameraInfo();
                    videoRecorder.setPreviewSize(info.previewWidth, info.pictureHeight);
                    videoRecorder.setTextureBuffer(mTextureBuffer);
                    videoRecorder.setCubeBuffer(mVertexBuffer);
                    videoRecorder.startRecording(new TextureVideoRecorder.RecorderConfig(
                            info.previewWidth,
                            info.pictureHeight,
                            1000000,
                            outputFile,
                            EGL14.eglGetCurrentContext()));
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUME:
                    videoRecorder.updateSharedContext(EGL14.eglGetCurrentContext());
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        } else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUME:
                    videoRecorder.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        }
        float[] mtx = new float[16];
        surfaceTexture.getTransformMatrix(mtx);
        cameraFilter.setTextureTransformMatrix(mtx);
        int id = textureId;
        if (filter == null) {
            cameraFilter.onDrawFrame(textureId, mVertexBuffer, mTextureBuffer);
        } else {
            id = cameraFilter.onDrawToTexture(textureId);
            filter.onDrawFrame(id, mVertexBuffer, mTextureBuffer);
        }
        videoRecorder.setTextureId(id);
        videoRecorder.frameAvailable(surfaceTexture);
    }

    @Override
    public void setFilter(BeautyFilterType type) {
        super.setFilter(type);
        videoRecorder.setFilter(type);
    }

    @Override
    protected void onFilterChanged() {
        super.onFilterChanged();
        cameraFilter.onOutputSizeChanged(surfaceWidth, surfaceHeight);
        if (filter != null)
            cameraFilter.initFrameBuffer(imageWidth, imageHeight);
        else
            cameraFilter.destroyFrameBuffer();
    }

    public void changeRecordingState(boolean isRecording) {
        recordEnable = isRecording;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        cameraEngine.releaseCamera();
    }

    private Bitmap drawPhoto(Bitmap bitmap, boolean isRotated) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] mFrameBuffers = new int[1];
        int[] mFrameBufferTextures = new int[1];
        if (beautyFilter == null)
            beautyFilter = new BeautyBeautifulFilter();
        beautyFilter.init();
        beautyFilter.onOutputSizeChanged(width, height);
        beautyFilter.onInputSizeChanged(width, height);

        if (filter != null) {
            filter.onInputSizeChanged(width, height);
            filter.onOutputSizeChanged(width, height);
        }
        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        GLES20.glViewport(0, 0, width, height);
        int textureId = OpenGLUtil.loadTexture(bitmap, OpenGLUtil.NO_TEXTURE, true);

        FloatBuffer glVertexBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        FloatBuffer glTextureBuffer = ByteBuffer.allocateDirect(TextureRotateUtil.TEXTURE_ROTATE_0.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glVertexBuffer.put(TextureRotateUtil.VERTEX).position(0);
        if (isRotated)
            glTextureBuffer.put(TextureRotateUtil.getRotateTexture(Rotation.NORMAL, false, false)).position(0);
        else
            glTextureBuffer.put(TextureRotateUtil.getRotateTexture(Rotation.NORMAL, false, true)).position(0);


        if (filter == null) {
            beautyFilter.onDrawFrame(textureId, glVertexBuffer, glTextureBuffer);
        } else {
            beautyFilter.onDrawFrame(textureId);
            filter.onDrawFrame(mFrameBufferTextures[0], glVertexBuffer, glTextureBuffer);
        }
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(ib);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
        GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);

        beautyFilter.destroy();
        beautyFilter = null;
        if (filter != null) {
            filter.onOutputSizeChanged(surfaceWidth, surfaceHeight);
            filter.onInputSizeChanged(imageWidth, imageHeight);
        }
        return result;
    }

    public void onBeautyLevelChanged() {
        cameraFilter.onBeautyLevelChanged();
    }

    public void switchCamera() {
        if (cameraEngine != null) {
            cameraEngine.switchCamera();
        }
    }

}
