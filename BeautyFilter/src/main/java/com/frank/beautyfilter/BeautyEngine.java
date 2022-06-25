package com.frank.beautyfilter;

import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.beautyfilter.helper.SavePictureTask;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.widget.BeautyCameraView;
import com.frank.beautyfilter.widget.base.BeautyBaseView;

import java.io.File;

/**
 * @author xufulong
 * @date 2022/6/24 8:47 上午
 * @desc
 */
public class BeautyEngine {

    private static BeautyEngine beautyEngine;

    private BeautyEngine(Builder builder) {

    }

    public static BeautyEngine getInstance() {
        if (beautyEngine == null) {
            throw new NullPointerException("must be built first!");
        }
        return beautyEngine;
    }

    public void setFilter(BeautyFilterType type) {
        BeautyParams.beautyBaseView.setFilter(type);
    }

    public void startRecord() {
        if (BeautyParams.beautyBaseView instanceof BeautyCameraView) {
            ((BeautyCameraView) BeautyParams.beautyBaseView).changeRecordingState(true);
        }
    }

    public void stopRecord() {
        if (BeautyParams.beautyBaseView instanceof BeautyCameraView) {
            ((BeautyCameraView) BeautyParams.beautyBaseView).changeRecordingState(false);
        }
    }

    public void setBeautyLevel(int level) {
        if (BeautyParams.beautyBaseView instanceof BeautyCameraView && BeautyParams.beautyLevel != level) {
            BeautyParams.beautyLevel = level;
            ((BeautyCameraView) BeautyParams.beautyBaseView).onBeautyLevelChanged();
        }
    }

    public void savePicture(File file, SavePictureTask.OnPictureSavedListener listener) {
        SavePictureTask task = new SavePictureTask(file, listener);
        BeautyParams.beautyBaseView.savePicture(task);
    }

    public static class Builder {

        public BeautyEngine build(BeautyBaseView baseView) {
            BeautyParams.context = baseView.getContext();
            BeautyParams.beautyBaseView = baseView;
            return new BeautyEngine(this);
        }

    }

}
