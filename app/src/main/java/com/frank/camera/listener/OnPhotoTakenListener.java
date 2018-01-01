package com.frank.camera.listener;

import org.opencv.core.Mat;

/**
 * Created by frank on 2017/12/30.
 * OnPhotoTakenListener
 */

public interface OnPhotoTakenListener {

    void onPhotoTaken(Mat frameData);

}
