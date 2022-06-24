package com.frank.beautyfilter.util;

import android.content.Context;
import android.os.Environment;

import com.frank.beautyfilter.widget.base.BeautyBaseView;

/**
 * @author xufulong
 * @date 2022/6/18 4:54 下午
 * @desc
 */
public class BeautyParams {

    public static Context context;
    public static BeautyBaseView beautyBaseView;

    public static String videoName = "beauty.mp4";
    public static String videoPath = Environment.getExternalStorageDirectory().getPath();

    public static int beautyLevel = 5;

}
