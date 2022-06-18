package com.frank.beautyfilter.util;

import android.content.Context;
import android.os.Environment;

/**
 * @author xufulong
 * @date 2022/6/18 4:54 下午
 * @desc
 */
public class MagicParams {

    public static Context context;
//    public static MagicBaseView magicBaseView;

    public static String videoName = "beauty.mp4";
    public static String videoPath = Environment.getExternalStorageDirectory().getPath();

    public static int beautyLevel = 5;

}
