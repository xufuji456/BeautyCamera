package com.frank.media;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener {

    private final static String path = "sdcard/beyond.mp4";

    private FFMediaPlayer videoPlayer;

    private SeekBar   playBar;
    private Button    btnSpeed;
    private TextView  txtDuration;
    private TextView  txtCurPosition;
    private ImageView btnPlayControl;

    private float currentSpeed = 1.0f;

    private final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private final static int MSG_PROGRESS = 54321;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 123);
        }

        initView();
    }

    private void initView() {
        playBar        = findViewById(R.id.play_bar);
        btnSpeed       = findViewById(R.id.btn_speed);
        txtDuration    = findViewById(R.id.txt_duration);
        txtCurPosition = findViewById(R.id.txt_cur_position);
        btnPlayControl = findViewById(R.id.btn_play_pause);

        SurfaceView surfaceView = findViewById(R.id.surface_player);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

                try {
                    videoPlayer = new FFMediaPlayer();
                    videoPlayer.setDataSource(path);
                    videoPlayer.setSurface(surfaceHolder.getSurface());
                    videoPlayer.setOnPreparedListener(preparedListener);
                    videoPlayer.setOnRenderFirstFrameListener(renderFirstFrameListener);
                    videoPlayer.setOnErrorListener(errorListener);
                    videoPlayer.setOnCompletionListener(completionListener);
                    videoPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                videoPlayer.stop();
                videoPlayer.release();
                videoPlayer = null;
            }
        });

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
        btnPlayControl.setOnClickListener(this);
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
            Log.e("FFMediaPlayer", "onRenderFirstFrame, video=" + video + ", audio=" + audio);
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