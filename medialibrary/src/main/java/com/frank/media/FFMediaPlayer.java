package com.frank.media;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.frank.media.annotations.AccessedByNative;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class FFMediaPlayer implements IMediaPlayer {

    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("soundtouch");
        System.loadLibrary("media_player");
        native_init();
    }

    private static final String TAG = FFMediaPlayer.class.getSimpleName();

    @AccessedByNative
    private long mNativeContext;
    @AccessedByNative
    private int mNativeSurfaceTexture; // nothing
    @AccessedByNative
    private int mListenerContext;   // nothing
    private SurfaceHolder mSurfaceHolder;
    private final EventHandler mEventHandler;

    private static native void native_init();
    private native void native_setup(Object player);
    private native void _setDataSource(@NonNull String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;
    private native void _setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException;
    private native void _setVideoSurface(Surface surface);
    private native void _prepare() throws IOException, IllegalStateException;
    public native void _prepareAsync() throws IllegalStateException;
    private native void _start() throws IllegalStateException;
    private native void _pause() throws IllegalStateException;
    private native void _resume() throws IllegalStateException;
    private native int _getRotate();
    private native int _getVideoWidth();
    private native int _getVideoHeight();
    private native boolean _isPlaying();
    private native void _seekTo(float msec) throws IllegalStateException;
    private native long _getCurrentPosition();
    private native long _getDuration();
    private native void _setRate(float rate);
    private native void _setMute(boolean mute);
    private native void _setVolume(float volume);
    private native void _reset();
    private native void _stop() throws IllegalStateException;
    private native void _release();
    private native void native_finalize();

    public FFMediaPlayer() {
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
        _setVideoSurface(surface);
        setScreenOnWhilePlaying(true);
    }

    public static FFMediaPlayer create(String path, Surface surface) {

        try {
            FFMediaPlayer mp = new FFMediaPlayer();
            mp.setDataSource(path);
            if (surface != null) {
                mp.setSurface(surface);
            }
            mp.prepare();
            return mp;
        } catch (IOException e) {
            Log.d(TAG, "create player failed:", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "create player failed:", e);
        } catch (SecurityException e) {
            Log.d(TAG, "create player failed:", e);
        }

        return null;
    }

    public void setDataSource(@NonNull String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        _setDataSource(path);
    }

    @Override
    public void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        setDataSource(fd, 0, Long.MAX_VALUE);
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        _setDataSource(fd, offset, length);
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        _prepare();
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
        _prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        _start();
    }

    @Override
    public void stop() throws IllegalStateException {
        _stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        _pause();
    }

    @Override
    public void resume() throws IllegalStateException {
        _resume();
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(screenOn);
        }
    }

    @Override
    public int getRotate() {
        return _getRotate();
    }

    @Override
    public int getVideoWidth() {
        return _getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return _getVideoHeight();
    }

    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     */
    @Override
    public boolean isPlaying() {
        return _isPlaying();
    }

    @Override
    public void seekTo(float msec) throws IllegalStateException {
        _seekTo(msec);
    }

    @Override
    public long getCurrentPosition() {
        return _getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return _getDuration();
    }

    @Override
    public void release() {
        setScreenOnWhilePlaying(false);
        mOnPreparedListener = null;
        mOnCompletionListener = null;
        mOnErrorListener = null;
        _release();
    }

    @Override
    public void reset() {
        _reset();
        // make sure none of the listeners get called anymore
        mEventHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void setVolume(float volume) {
        _setVolume(volume);
    }

    @Override
    public void setMute(boolean mute) {
        _setMute(mute);
    }

    public void setRate(float rate) {
        _setRate(rate);
    }

    @Override
    protected void finalize() throws Throwable {
        native_finalize();
        super.finalize();
    }

    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_ERROR = 100;

    private class EventHandler extends Handler {

        private final FFMediaPlayer mMediaPlayer;

        public EventHandler(FFMediaPlayer mp, Looper looper) {
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
                    if (mOnPreparedListener != null)
                        mOnPreparedListener.onPrepared(mMediaPlayer);
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
                default: {
                    Log.e(TAG, "Unknown msg, what:" + msg.what);
                    break;
                }
            }
        }
    }

    private static void postEventFromNative(Object mediaplayer_ref,
                                            int what, int arg1, int arg2, Object obj) {
        final FFMediaPlayer mp = (FFMediaPlayer)((WeakReference) mediaplayer_ref).get();
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
