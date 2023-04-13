package com.frank.videoedit.listener;

import android.content.Context;
import android.view.Surface;

import com.frank.videoedit.effect.entity.FrameInfo;
import com.frank.videoedit.effect.entity.SurfaceInfo;
import com.frank.videoedit.effect.entity.ColorInfo;
import com.frank.videoedit.util.FrameProcessingException;

import com.google.android.exoplayer2.util.Effect;

import java.util.List;

public interface FrameProcessor {

  interface Factory {

    FrameProcessor create(
        Context context,
        Listener listener,
        List<Effect> effects,
        ColorInfo colorInfo,
        boolean releaseFramesAutomatically)
        throws FrameProcessingException;
  }

  interface Listener {

    void onOutputSizeChanged(int width, int height);

    void onOutputFrameAvailable(long presentationTimeUs);

    void onFrameProcessingError(FrameProcessingException exception);

    void onFrameProcessingEnded();
  }


  long RELEASE_OUTPUT_FRAME_IMMEDIATELY = -1;

  long DROP_OUTPUT_FRAME = -2;

  Surface getInputSurface();

  void setInputFrameInfo(FrameInfo inputFrameInfo);

  void registerInputFrame();

  int getPendingInputFrameCount();

  void setOutputSurfaceInfo(SurfaceInfo outputSurfaceInfo);

  void releaseOutputFrame(long releaseTimeNs);

  void signalEndOfInput();

  void release();
}