
package com.frank.videoedit;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.frank.videoedit.effect.Contrast;
import com.frank.videoedit.effect.HslAdjustment;
import com.frank.videoedit.effect.listener.GlEffect;
import com.frank.videoedit.transform.DefaultEncoderFactory;
import com.frank.videoedit.transform.ProgressHolder;
import com.frank.videoedit.transform.TransformationException;
import com.frank.videoedit.transform.TransformationRequest;
import com.frank.videoedit.transform.TransformationResult;
import com.frank.videoedit.transform.Transformer;
import com.frank.videoedit.util.CommonUtil;
import com.frank.videoedit.view.MaterialCardView;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.Effect;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** An {@link Activity} that transforms and plays media using {@link Transformer}. */
public final class TransformerActivity extends AppCompatActivity {
  private static final String TAG = "TransformerActivity";

  private Button displayInputButton;
  private MaterialCardView inputCardView;
  private SurfaceView inputPlayerView;
  private SurfaceView outputPlayerView;
  private TextView informationTextView;
  private ViewGroup progressViewGroup;
  private LinearProgressIndicator progressIndicator;
  private Stopwatch transformationStopwatch;

  private MediaPlayer inputPlayer;
  private MediaPlayer outputPlayer;
  private Transformer transformer;
  private File externalCacheFile;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_transform);

    inputCardView = findViewById(R.id.input_card_view);
    inputPlayerView = findViewById(R.id.input_player_view);
    outputPlayerView = findViewById(R.id.output_player_view);
    informationTextView = findViewById(R.id.information_text_view);
    progressViewGroup = findViewById(R.id.progress_view_group);
    progressIndicator = findViewById(R.id.progress_indicator);
    displayInputButton = findViewById(R.id.display_input_button);
    displayInputButton.setOnClickListener(this::toggleInputVideoDisplay);

    transformationStopwatch =
        Stopwatch.createUnstarted(
            new Ticker() {
              @Override
              public long read() {
                return android.os.SystemClock.elapsedRealtimeNanos();
              }
            });
  }

  @Override
  protected void onStart() {
    super.onStart();

    startTransformation();
  }

  @Override
  protected void onStop() {
    super.onStop();

    transformer.cancel();
    transformer = null;

    // The stop watch is reset after cancelling the transformation, in case cancelling causes the
    // stop watch to be stopped in a transformer callback.
    transformationStopwatch.reset();

    // TODO: pause
    releasePlayer();

    externalCacheFile.delete();
    externalCacheFile = null;
  }

  private void startTransformation() {
    requestTransformerPermission();

    Intent intent = getIntent();
    Uri uri = intent.getData();
    try {
      externalCacheFile = createExternalCacheFile("transform_output.mp4");
      String filePath = externalCacheFile.getAbsolutePath();
      Bundle bundle = intent.getExtras();
      MediaItem mediaItem = createMediaItem(bundle, uri);
      Transformer transformer = createTransformer(bundle, filePath);
      transformationStopwatch.start();
      assert mediaItem.localConfiguration != null;
      MediaItem newMediaItem =
              MediaItem.fromUri(mediaItem.localConfiguration.uri);
      transformer.startTransformation(/*mediaItem*/newMediaItem, filePath);
      this.transformer = transformer;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    informationTextView.setText(R.string.transformation_started);
    inputCardView.setVisibility(View.GONE);
    outputPlayerView.setVisibility(View.GONE);
    Handler mainHandler = new Handler(getMainLooper());
    ProgressHolder progressHolder = new ProgressHolder();
    mainHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (transformer != null
                && transformer.getProgress(progressHolder)
                    != Transformer.PROGRESS_STATE_NO_TRANSFORMATION) {
              progressIndicator.setProgress(progressHolder.progress);
              informationTextView.setText(
                  getString(
                      R.string.transformation_timer,
                      transformationStopwatch.elapsed(TimeUnit.SECONDS)));
              mainHandler.postDelayed(/* r= */ this, /* delayMillis= */ 500);
            }
          }
        });
  }

  private MediaItem createMediaItem(@Nullable Bundle bundle, Uri uri) {
    MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);
    return mediaItemBuilder.build();
  }

  private Transformer createTransformer(@Nullable Bundle bundle, String filePath) {
    Transformer.Builder transformerBuilder = new Transformer.Builder(/* context= */ this);
    if (bundle != null) {
      TransformationRequest.Builder requestBuilder = new TransformationRequest.Builder();
      String audioMimeType = bundle.getString(EditActivity.AUDIO_MIME_TYPE);
      if (audioMimeType != null) {
        requestBuilder.setAudioMimeType(audioMimeType);
      }
      String videoMimeType = bundle.getString(EditActivity.VIDEO_MIME_TYPE);
      if (videoMimeType != null) {
        requestBuilder.setVideoMimeType(videoMimeType);
      }
      int resolutionHeight =
          bundle.getInt(EditActivity.RESOLUTION_HEIGHT, CommonUtil.LENGTH_UNSET);
      if (resolutionHeight != CommonUtil.LENGTH_UNSET) {
        requestBuilder.setResolution(resolutionHeight);
      }

      requestBuilder.setEnableRequestSdrToneMapping(
          bundle.getBoolean(EditActivity.ENABLE_REQUEST_SDR_TONE_MAPPING));
      requestBuilder.experimental_setForceInterpretHdrVideoAsSdr(
          bundle.getBoolean(EditActivity.FORCE_INTERPRET_HDR_VIDEO_AS_SDR));
      requestBuilder.experimental_setEnableHdrEditing(
          bundle.getBoolean(EditActivity.ENABLE_HDR_EDITING));
      transformerBuilder
          .setTransformationRequest(requestBuilder.build())
          .setEncoderFactory(
              new DefaultEncoderFactory.Builder(this.getApplicationContext())
                  .setEnableFallback(bundle.getBoolean(EditActivity.ENABLE_FALLBACK))
                  .build());

      transformerBuilder.setVideoEffects(createVideoEffectsListFromBundle(bundle));
    }
    return transformerBuilder
        .addListener(
            new Transformer.Listener() {
              @Override
              public void onTransformationCompleted(
                      MediaItem mediaItem, TransformationResult transformationResult) {
                TransformerActivity.this.onTransformationCompleted(filePath, mediaItem);
              }

              @Override
              public void onTransformationError(
                      MediaItem mediaItem, TransformationException exception) {
                TransformerActivity.this.onTransformationError(exception);
              }
            })
        .build();
  }

  /** Creates a cache file, resetting it if it already exists. */
  private File createExternalCacheFile(String fileName) throws IOException {
    String path = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
            ? Environment.getExternalStorageDirectory().getPath()
            : getExternalCacheDir().getAbsolutePath();

    File file = new File(path, fileName);
    if (file.exists() && !file.delete()) {
      throw new IllegalStateException("Could not delete the previous transformer output file");
    }
    if (!file.createNewFile()) {
      throw new IllegalStateException("Could not create the transformer output file");
    }
    return file;
  }

  private ImmutableList<Effect> createVideoEffectsListFromBundle(Bundle bundle) {
    @Nullable
    boolean[] selectedEffects =
        bundle.getBooleanArray(EditActivity.DEMO_EFFECTS_SELECTIONS);
    if (selectedEffects == null) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<Effect> effects = new ImmutableList.Builder<>();
    if (selectedEffects[0]) {
      effects.add(
          new HslAdjustment.Builder()
              .adjustHue(bundle.getFloat(EditActivity.HSL_ADJUSTMENTS_HUE))
              .adjustSaturation(bundle.getFloat(EditActivity.HSL_ADJUSTMENTS_SATURATION))
              .adjustLightness(bundle.getFloat(EditActivity.HSL_ADJUSTMENTS_LIGHTNESS))
              .build());
    }
    if (selectedEffects[1]) {
      effects.add(new Contrast(bundle.getFloat(EditActivity.CONTRAST_VALUE)));
    }
    if (selectedEffects[2]) {
      effects.add(
          (GlEffect)
              (Context context, boolean useHdr) ->
                  new PeriodicVignetteProcessor(
                      context,
                      useHdr,
                      bundle.getFloat(EditActivity.PERIODIC_VIGNETTE_CENTER_X),
                      bundle.getFloat(EditActivity.PERIODIC_VIGNETTE_CENTER_Y),
                      /* minInnerRadius= */ bundle.getFloat(
                          EditActivity.PERIODIC_VIGNETTE_INNER_RADIUS),
                      /* maxInnerRadius= */ bundle.getFloat(
                          EditActivity.PERIODIC_VIGNETTE_OUTER_RADIUS),
                      bundle.getFloat(EditActivity.PERIODIC_VIGNETTE_OUTER_RADIUS)));
    }
    if (selectedEffects[3]) {
      effects.add(MatrixTransformationFactory.createZoomInTransition());
    }
    if (selectedEffects[4]) {
      effects.add((GlEffect) BitmapOverlayProcessor::new);
    }
    return effects.build();
  }

  private void onTransformationError(TransformationException exception) {
    transformationStopwatch.stop();
    informationTextView.setText(R.string.transformation_error);
    progressViewGroup.setVisibility(View.GONE);
    Toast.makeText(getApplicationContext(), "Transformation error: " + exception, Toast.LENGTH_LONG)
        .show();
    Log.e(TAG, "Transformation error", exception);
  }

  private void onTransformationCompleted(String filePath, MediaItem inputMediaItem) {
    transformationStopwatch.stop();
    informationTextView.setText(
        getString(
            R.string.transformation_completed, transformationStopwatch.elapsed(TimeUnit.SECONDS)));
    progressViewGroup.setVisibility(View.GONE);
    inputCardView.setVisibility(View.VISIBLE);
    outputPlayerView.setVisibility(View.VISIBLE);
    displayInputButton.setVisibility(View.VISIBLE);
    Uri inputPath = null;
    if (inputMediaItem.localConfiguration != null) {
      inputPath = inputMediaItem.localConfiguration.uri;
    }
    playMediaItems(inputPath, Uri.parse("file://" + filePath));
    Log.d(TAG, "Output file path: file://" + filePath);
  }

  private MediaPlayer startUp(SurfaceView surfaceView, Uri path, boolean disableAudio) {
    MediaPlayer player = new MediaPlayer();
    surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

      @Override
      public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        player.setSurface(surfaceHolder.getSurface());
      }

      @Override
      public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
      }

      @Override
      public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
      }
    });
    try {
      player.setDataSource(this, path);
      player.prepare();
      player.start();
      if (disableAudio) {
        player.setVolume(0, 0);
      }
    } catch (IOException e) {
      Log.e(TAG, "");
      return null;
    }
    return player;
  }

  private void playMediaItems(Uri inputPath, Uri outputPath) {
    releasePlayer();

    inputPlayer  = startUp(inputPlayerView, inputPath, false);
    outputPlayer = startUp(outputPlayerView, outputPath, true);
  }

  private void releasePlayer() {
    if (inputPlayer != null) {
      inputPlayer.release();
      inputPlayer = null;
    }
    if (outputPlayer != null) {
      outputPlayer.release();
      outputPlayer = null;
    }
  }

  private void requestTransformerPermission() {
    if (Build.VERSION.SDK_INT < 23) {
      return;
    }
    if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {READ_EXTERNAL_STORAGE}, /* requestCode= */ 0);
    }
  }

  private void toggleInputVideoDisplay(View view) {
    if (inputCardView.getVisibility() == View.GONE) {
      inputCardView.setVisibility(View.VISIBLE);
      displayInputButton.setText(getString(R.string.hide_input_video));
    } else if (inputCardView.getVisibility() == View.VISIBLE) {
      inputPlayer.pause();
      inputCardView.setVisibility(View.GONE);
      displayInputButton.setText(getString(R.string.show_input_video));
    }
  }

}
