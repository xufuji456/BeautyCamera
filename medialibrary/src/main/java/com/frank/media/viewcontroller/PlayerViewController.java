package com.frank.media.viewcontroller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.frank.media.FFMediaPlayer;
import com.frank.media.IMediaPlayer;
import com.frank.media.R;
import com.frank.media.TimeUtil;
import com.frank.media.mediainfo.MediaTrack;
import com.frank.media.mediainfo.MediaType;

import java.io.IOException;
import java.util.List;

/**
 * @author xufulong
 * @date 2022/10/25 2:59 下午
 * @desc
 */
public class PlayerViewController implements View.OnClickListener {

    private final Context mContext;

    private View mVideoView;

    private FFMediaPlayer videoPlayer;

    private SeekBar playBar;
    private Button btnSpeed;
    private TextView txtDuration;
    private TextView  txtCurPosition;
    private ImageView btnPlayControl;

    private float currentSpeed = 1.0f;

    private final static int MSG_PROGRESS = 54321;

    private final static String path = "sdcard/angry_birds.mp4";

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_PROGRESS) {
                if (videoPlayer == null)
                    return;
                long currentPosition = videoPlayer.getCurrentPosition();
                txtCurPosition.setText(TimeUtil.getVideoTime(currentPosition));
                playBar.setProgress((int) currentPosition);

                sendEmptyMessageDelayed(MSG_PROGRESS, 1000);
            }
        }
    };

    public PlayerViewController(Context context) {
        mContext = context;
    }

    public void initView(View view) {
        playBar        = view.findViewById(R.id.play_bar);
        btnSpeed       = view.findViewById(R.id.btn_speed);
        txtDuration    = view.findViewById(R.id.txt_duration);
        txtCurPosition = view.findViewById(R.id.txt_cur_position);
        btnPlayControl = view.findViewById(R.id.btn_play_pause);
        Button btnAudioTrack  = view.findViewById(R.id.btn_audio_track);

        mVideoView = view.findViewById(R.id.surface_player);
        setVideoViewListener(mVideoView);

        playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                videoPlayer.seekTo(seekBar.getProgress());
            }
        });

        btnSpeed.setOnClickListener(this);
        btnAudioTrack.setOnClickListener(this);
        btnPlayControl.setOnClickListener(this);
    }

    private void initPlayer(Surface surface) {
        try {
            videoPlayer = new FFMediaPlayer();
            videoPlayer.setDataSource(path);
            videoPlayer.setSurface(surface);
            videoPlayer.setOnPreparedListener(preparedListener);
            videoPlayer.setOnRenderFirstFrameListener(renderFirstFrameListener);
            videoPlayer.setOnErrorListener(errorListener);
            videoPlayer.setOnCompletionListener(completionListener);
            videoPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releasePlayer() {
        videoPlayer.stop();
        videoPlayer.release();
        videoPlayer = null;
    }

    private void setVideoViewListener(View videoView) {
        if (videoView instanceof TextureView) {
            ((TextureView)videoView).setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                    Surface surface = new Surface(surfaceTexture);
                    initPlayer(surface);
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                    releasePlayer();
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

                }
            });
        } else if (videoView instanceof SurfaceView) {
            ((SurfaceView)videoView).getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                    initPlayer(surfaceHolder.getSurface());
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                    releasePlayer();
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_play_pause) {
            if (videoPlayer.isPlaying()) {
                videoPlayer.pause();
                btnPlayControl.setImageResource(R.drawable.ic_play);
            } else {
                videoPlayer.start();
                btnPlayControl.setImageResource(R.drawable.ic_pause);
            }
        } else if (view.getId() == R.id.btn_speed) {
            currentSpeed += 0.5f;
            if (currentSpeed > 2.0f) {
                currentSpeed = 0.5f;
            }
            videoPlayer.setRate(currentSpeed);
            btnSpeed.setText(String.format("%s", currentSpeed));
        } else if (view.getId() == R.id.btn_audio_track) {
            List<MediaTrack> audioTrackList = videoPlayer.getMediaTrack(MediaType.MEDIA_TYPE_AUDIO);
            String[] tracks = new String[audioTrackList.size()];
            for (int i=0; i<audioTrackList.size(); i++) {
                tracks[i] = audioTrackList.get(i).language;
            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setTitle("选择音轨")
                    .setItems(tracks, (dialogInterface, i) -> {
                        String msg = "select:" + audioTrackList.get(i).language;
                        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                        videoPlayer.selectTrack(audioTrackList.get(i).trackId);
                    })
                    .show();
        }
    }

    public void onResume() {
        if (videoPlayer != null) {
            videoPlayer.resume();
        }
    }

    public void onPause() {
        if (videoPlayer != null) {
            videoPlayer.pause();
        }
    }

    public void onStop() {
        if (videoPlayer != null) {
            videoPlayer.stop();
        }
    }

    private final IMediaPlayer.OnPreparedListener preparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            videoPlayer.start();
        }
    };

    private final IMediaPlayer.OnRenderFirstFrameListener renderFirstFrameListener = new IMediaPlayer.OnRenderFirstFrameListener() {
        @Override
        public void onRenderFirstFrame(IMediaPlayer mp, int video, int audio) {
            Log.i("FFMediaPlayer", "onRenderFirstFrame, video=" + video + ", audio=" + audio);
            if (video == 1 || audio == 1) {
                long playProgress = videoPlayer.getDuration();
                txtDuration.setText(TimeUtil.getVideoTime(playProgress));
                playBar.setMax((int) playProgress);
                mHandler.sendEmptyMessageDelayed(MSG_PROGRESS, 1000);
                btnPlayControl.setImageResource(R.drawable.ic_pause);
            }
        }
    };

    private final IMediaPlayer.OnErrorListener errorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            Log.i("FFMediaPlayer", "onError, what=" + what);
            return false;
        }
    };

    private final IMediaPlayer.OnCompletionListener completionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            Log.i("FFMediaPlayer", "onCompletion...");
        }
    };

}
