package com.frank.beautyfilter.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;

import com.frank.beautyfilter.BeautyManager;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.helper.SavePictureTask;
import com.frank.beautyfilter.util.OpenGLUtil;
import com.frank.beautyfilter.widget.base.BeautyBaseView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/25 5:15 下午
 * @desc
 */
public class BeautyImageView extends BeautyBaseView {

    private final GPUImageFilter imageFilter;

    public BeautyImageView(Context context) {
        this(context, null);
    }

    public BeautyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        imageFilter = new GPUImageFilter();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);
        imageFilter.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        super.onSurfaceChanged(gl10, width, height);
        adjustSize(0, false, false);
    }

    @Override
    public void savePicture(SavePictureTask task) {

    }

     @Override
    public void onDrawFrame(GL10 gl10) {
        super.onDrawFrame(gl10);
        if (textureId == OpenGLUtil.NO_TEXTURE) {
            textureId = OpenGLUtil.loadTexture(getBitmap(), OpenGLUtil.NO_TEXTURE);
        }
        if (filter == null) {
            imageFilter.onDrawFrame(textureId, mVertexBuffer, mTextureBuffer);
        } else {
            filter.onDrawFrame(textureId, mVertexBuffer, mTextureBuffer);
        }
    }

    public void initBeautyManager() {
        BeautyManager.getInstance().setHandler(new BeautyHandler());
        BeautyManager.getInstance().initBeauty();
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null)
            return;
        BeautyManager.getInstance().setBitmap(bitmap, true);
    }

    public Bitmap getBitmap() {
        return BeautyManager.getInstance().getBitmap();
    }

    private static class BeautyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == BeautyManager.MSG_OPERATION_END) {
                Log.e("BeautyImageView", "handle end...");
            }
        }
    }

}
