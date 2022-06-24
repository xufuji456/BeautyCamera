package com.frank.beautyfilter.helper;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author xufulong
 * @date 2022/6/18 5:31 下午
 * @desc
 */
public class SavePictureTask {

    private final File mFile;
    private final OnPictureSavedListener mListener;

    public interface OnPictureSavedListener {
        void onSaved(String result);
    }

    public SavePictureTask(File file, OnPictureSavedListener listener) {
        this.mFile = file;
        this.mListener = listener;
    }

    private String saveBitmap(Bitmap bitmap) {
        if (bitmap == null || mFile == null)
            return null;
        if (mFile.exists())
            mFile.delete();
        try {
            FileOutputStream outputStream = new FileOutputStream(mFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return mFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void execute(Bitmap bitmap) {
        if (bitmap == null)
            return;
        Thread thread = new Thread(() -> {
            String result = saveBitmap(bitmap);
            if (mListener != null)
                mListener.onSaved(result);
        });
        thread.start();
    }

}
