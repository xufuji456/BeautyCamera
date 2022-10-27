package com.frank.media.player;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.frank.media.listener.IMediaPlayer;
import com.frank.media.mediainfo.MediaInfo;
import com.frank.media.mediainfo.MediaTrack;
import com.frank.media.mediainfo.MediaType;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FFmpegPlayer implements IMediaPlayer {

    static {
        System.loadLibrary("ffmpeg_player");
        native_init();
    }

    private static final String TAG = FFmpegPlayer.class.getSimpleName();

    private long mNativeContext;

    private final EventHandler mEventHandler;

    private static native void native_init();
    private native void native_setup(Object player);
    private native void native_setDataSource(@NonNull String path)
            throws IOException, IllegalArgumentException, IllegalStateException;
    private native void native_setDataSource(FileDescriptor fd, long length)
            throws IOException, IllegalArgumentException, IllegalStateException;
    private native void native_setVideoSurface(Surface surface);
    private native void native_prepare() throws IOException, IllegalStateException;
    private native void native_prepareAsync() throws IllegalStateException;
    private native void native_start() throws IllegalStateException;
    private native void native_pause() throws IllegalStateException;
    private native void native_resume() throws IllegalStateException;
    private native int  native_getRotate();
    private native int  native_getVideoWidth();
    private native int  native_getVideoHeight();
    private native boolean native_isPlaying();
    private native void native_seekTo(long msec) throws IllegalStateException;
    private native long native_getCurrentPosition();
    private native long native_getDuration();
    private native int  native_selectTrack(int trackId, boolean selected);
    private native void native_setRate(float rate);
    private native void native_setMute(boolean mute);
    private native void native_setVolume(float volume);
    private native void native_getMediaInfo(int mediaType, MediaInfo mediaInfo);
    private native int  native_getTrackCount(int mediaType);
    private native void native_getMediaTrack(int mediaType, int index, MediaTrack mediaTrack);
    private native void native_reset();
    private native void native_stop() throws IllegalStateException;
    private native void native_release();
    private native void native_finalize();

    public FFmpegPlayer() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        native_setup(new WeakReference<>(this));
    }

    @Override
    public void setSurface(Surface surface) {
        native_setVideoSurface(surface);
        setScreenOnWhilePlaying(true);
    }

    public static FFmpegPlayer create(String path, Surface surface) {

        try {
            FFmpegPlayer mp = new FFmpegPlayer();
            mp.setDataSource(path);
            if (surface != null) {
                mp.setSurface(surface);
            }
            mp.prepare();
            return mp;
        } catch (IOException | IllegalArgumentException e) {
            Log.d(TAG, "create player failed:", e);
        }

        return null;
    }

    public void setDataSource(@NonNull String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        native_setDataSource(path);
    }

    @Override
    public void setDataSource(FileDescriptor fd, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        native_setDataSource(fd, length);
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        native_prepare();
    }

    /**
     * Prepares the player for playback, asynchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For streams, you should call prepareAsync(),
     * which returns immediately, rather than blocking until enough data has been
     * buffered.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    @Override
    public void prepareAsync() throws IllegalStateException {
        native_prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        native_start();
    }

    @Override
    public void stop() throws IllegalStateException {
        native_stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        native_pause();
    }

    @Override
    public void resume() throws IllegalStateException {
        native_resume();
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {

    }

    @Override
    public int getRotate() {
        return native_getRotate();
    }

    @Override
    public int getVideoWidth() {
        return native_getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return native_getVideoHeight();
    }

    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     */
    @Override
    public boolean isPlaying() {
        return native_isPlaying();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        native_seekTo(msec);
    }

    @Override
    public long getCurrentPosition() {
        return native_getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return native_getDuration();
    }

    @Override
    public void setVolume(float volume) {
        native_setVolume(volume);
    }

    @Override
    public void setMute(boolean mute) {
        native_setMute(mute);
    }

    @Override
    public void setRate(float rate) {
        native_setRate(rate);
    }

    @Override
    public MediaInfo getMediaInfo(MediaType mediaType) {
        MediaInfo mediaInfo = new MediaInfo();
        native_getMediaInfo(mediaType.ordinal(), mediaInfo);
        return mediaInfo;
    }

    @Override
    public List<MediaTrack> getMediaTrack(MediaType mediaType) {
        List<MediaTrack> trackList = new ArrayList<>();
        int type = mediaType.ordinal();
        int trackCount = native_getTrackCount(type);
        for (int i=0; i<trackCount; i++) {
            MediaTrack mediaTrack = new MediaTrack();
            native_getMediaTrack(type, i, mediaTrack);
            trackList.add(mediaTrack);
        }
        return trackList;
    }

    @Override
    public void selectTrack(int trackId) {
        native_selectTrack(trackId, true);
    }

    @Override
    public Bitmap getCurrentFrame() {
        return null;
    }

    @Override
    public void reset() {
        mEventHandler.removeCallbacksAndMessages(null);
        native_reset();
    }

    @Override
    public void release() {
        setScreenOnWhilePlaying(false);
        mOnPreparedListener = null;
        mOnCompletionListener = null;
        mOnErrorListener = null;
        native_release();
    }

    @Override
    protected void finalize() throws Throwable {
        native_finalize();
        super.finalize();
    }

    private static final int MEDIA_PREPARED           = 0x01;
    private static final int MEDIA_RENDER_FIRST_FRAME = 0x02;
    private static final int MEDIA_STARTED            = 0x03;
    private static final int MEDIA_SEEK_COMPLETE      = 0x04;
    private static final int MEDIA_PLAYBACK_COMPLETE  = 0x05;
    private static final int MEDIA_TIMED_TEXT         = 0x06;
    private static final int MEDIA_ERROR              = 0x07;
    private static final int MEDIA_VIDEO_SIZE_CHANGED = 0x08;
    private static final int MEDIA_INFO               = 0x09;
    private static final int MEDIA_BUFFERING_START    = 0x0A;
    private static final int MEDIA_BUFFERING_UPDATE   = 0x0B;
    private static final int MEDIA_BUFFERING_END      = 0x0C;

    private class EventHandler extends Handler {

        private final FFmpegPlayer mMediaPlayer;

        public EventHandler(FFmpegPlayer mp, Looper looper) {
            super(looper);
            mMediaPlayer = mp;
        }

        @Override
        public void handleMessage(Message msg) {
            if (mMediaPlayer.mNativeContext == 0) {
                Log.w(TAG, "media player has been destroyed.");
                return;
            }

            switch (msg.what) {
                case MEDIA_PREPARED: {
                    if (mOnPreparedListener != null) {
                        mOnPreparedListener.onPrepared(mMediaPlayer);
                    }
                    return;
                }
                case MEDIA_RENDER_FIRST_FRAME: {
                    if (mOnRenderFirstFrameListener != null) {
                        mOnRenderFirstFrameListener.onRenderFirstFrame(mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    return;
                }
                case MEDIA_PLAYBACK_COMPLETE: {
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                    return;
                }
                case MEDIA_ERROR: {
                    Log.e(TAG, "Error:what=" + msg.arg1 + ",extra=" + msg.arg2);
                    boolean error_was_handled = false;
                    if (mOnErrorListener != null) {
                        error_was_handled = mOnErrorListener.onError(mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    if (mOnCompletionListener != null && !error_was_handled) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                    return;
                }
                case MEDIA_STARTED: {
                    Log.d(TAG, "on started");
                    return;
                }
                case MEDIA_VIDEO_SIZE_CHANGED: {
                    Log.d(TAG, "video size changed: width=" + msg.arg1 + ", height=" + msg.arg2);
                }
                default: {
                    Log.e(TAG, "Unknown msg, what:" + msg.what);
                    break;
                }
            }
        }
    }

    private static void postEventFromNative(Object mediaplayer_ref,
                                            int what, int arg1, int arg2, Object obj) {
        final FFmpegPlayer mp = (FFmpegPlayer)((WeakReference) mediaplayer_ref).get();
        if (mp == null) {
            return;
        }

        if (mp.mEventHandler != null) {
            Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mp.mEventHandler.sendMessage(m);
        }
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    private OnPreparedListener mOnPreparedListener;

    @Override
    public void setOnRenderFirstFrameListener(OnRenderFirstFrameListener listener) {
        mOnRenderFirstFrameListener = listener;
    }

    private OnRenderFirstFrameListener mOnRenderFirstFrameListener;

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    private OnCompletionListener mOnCompletionListener;

    /**
     * Register a callback to be invoked when an error has happened
     * during an asynchronous operation.
     *
     * @param listener the callback that will be run
     */
    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    private OnErrorListener mOnErrorListener;
}
