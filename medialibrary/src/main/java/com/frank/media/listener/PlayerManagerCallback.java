package com.frank.media.listener;

/**
 * @author xufulong
 * @date 2022/10/26 6:02 下午
 * @desc
 */
public interface PlayerManagerCallback {

    void onPrepared();

    void onRenderFirstFrame(int video, int audio);

    void onCompletion();

    boolean onError(int what, int extra);

}
