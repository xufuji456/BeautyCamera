package com.frank.media.player;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.frank.media.listener.IMediaPlayer;
import com.frank.media.mediainfo.MediaInfo;
import com.frank.media.mediainfo.MediaTrack;
import com.frank.media.mediainfo.MediaType;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

/**
 * @author xufulong
 * @date 2022/10/26 4:57 下午
 * @desc
 */
public class SystemMediaPlayer implements IMediaPlayer {

    private final MediaPlayer mMediaPlayer;

    private boolean hasRenderFirstFrame;

    private IMediaPlayer.OnPreparedListener onPreparedListener;
    private IMediaPlayer.OnRenderFirstFrameListener onRenderFirstFrameListener;
    private IMediaPlayer.OnErrorListener onErrorListener;
    private IMediaPlayer.OnCompletionListener onCompletionListener;

    public SystemMediaPlayer() {
        hasRenderFirstFrame = false;
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public void setSurface(Surface surface) {
        mMediaPlayer.setSurface(surface);
        setListener();
    }

    @Override
    public void setDataSource(@NonNull String path) throws IOException, IllegalArgumentException, IllegalStateException {
        mMediaPlayer.setDataSource(path);
    }

    @Override
    public void setDataSource(FileDescriptor fd, long length) throws IOException, IllegalArgumentException, IllegalStateException {
        mMediaPlayer.setDataSource(fd, 0, length);
    }

    private void setListener() {
        mMediaPlayer.setOnPreparedListener(preparedListener);
        mMediaPlayer.setOnInfoListener(infoListener);
        mMediaPlayer.setOnErrorListener(errorListener);
        mMediaPlayer.setOnCompletionListener(completionListener);
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        mMediaPlayer.prepare();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mMediaPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mMediaPlayer.start();
    }

    @Override
    public void pause() throws IllegalStateException {
        mMediaPlayer.pause();
    }

    @Override
    public void stop() throws IllegalStateException {
        mMediaPlayer.stop();
    }

    @Override
    public void resume() throws IllegalStateException {
        mMediaPlayer.start();
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        mMediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    @Override
    public int getRotate() {
        return 0;
    }

    @Override
    public int getVideoWidth() {
        return mMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mMediaPlayer.getVideoHeight();
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        mMediaPlayer.seekTo((int) msec);
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public void setVolume(float volume) {
        mMediaPlayer.setVolume(volume, volume);
    }

    @Override
    public void setMute(boolean mute) {
        mMediaPlayer.setVolume(0.0f, 0.0f);
    }

    @Override
    public void setRate(float rate) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PlaybackParams params = mMediaPlayer.getPlaybackParams();
            params.setSpeed(rate);
            mMediaPlayer.setPlaybackParams(params);
        }
    }

    @Override
    public MediaInfo getMediaInfo(MediaType mediaType) {
        return null;
    }

    @Override
    public List<MediaTrack> getMediaTrack(MediaType mediaType) {
        return null;
    }

    @Override
    public Bitmap getCurrentFrame() {
        return null;
    }

    @Override
    public void selectTrack(int trackId) {
        mMediaPlayer.selectTrack(trackId);
    }

    @Override
    public void reset() {
        mMediaPlayer.reset();
    }

    @Override
    public void release() {
        mMediaPlayer.release();
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        onPreparedListener = listener;
    }

    @Override
    public void setOnRenderFirstFrameListener(OnRenderFirstFrameListener listener) {
        onRenderFirstFrameListener = listener;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        onCompletionListener = listener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        onErrorListener = listener;
    }

    private final MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            start();
            if (onPreparedListener != null) {
                onPreparedListener.onPrepared(null);
            }
        }
    };

    private final MediaPlayer.OnInfoListener infoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && !hasRenderFirstFrame) {
                hasRenderFirstFrame = true;
                onRenderFirstFrameListener.onRenderFirstFrame(null, 1, 0);
            }
            return false;
        }
    };

    private final MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
            if (onErrorListener != null) {
                onErrorListener.onError(null, what, extra);
            }
            return false;
        }
    };

    private final MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (onCompletionListener != null) {
                onCompletionListener.onCompletion(null);
            }
        }
    };

}
