package com.frank.videoedit.transform;

import android.util.SparseLongArray;

import com.frank.videoedit.transform.util.MediaUtil;
import com.frank.videoedit.util.CommonUtil;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.util.MediaClock;

/* package */ final class TransformerMediaClock implements MediaClock {

  private final SparseLongArray trackTypeToTimeUs;
  private long minTrackTimeUs;

  public TransformerMediaClock() {
    trackTypeToTimeUs = new SparseLongArray();
  }

  /**
   * Updates the time for a given track type. The clock time is computed based on the different
   * track times.
   */
  public void updateTimeForTrackType(@MediaUtil.TrackType int trackType, long timeUs) {
    long previousTimeUs = trackTypeToTimeUs.get(trackType, /* valueIfKeyNotFound= */ CommonUtil.TIME_UNSET);
    if (previousTimeUs != CommonUtil.TIME_UNSET && timeUs <= previousTimeUs) {
      // Make sure that the track times are increasing and therefore that the clock time is
      // increasing. This is necessary for progress updates.
      return;
    }
    trackTypeToTimeUs.put(trackType, timeUs);
    if (previousTimeUs == CommonUtil.TIME_UNSET || previousTimeUs == minTrackTimeUs) {
      minTrackTimeUs = CommonUtil.minValue(trackTypeToTimeUs);
    }
  }

  @Override
  public long getPositionUs() {
    // Use minimum position among tracks as position to ensure that the buffered duration is
    // positive. This is also useful for controlling samples interleaving.
    return minTrackTimeUs;
  }

  @Override
  public void setPlaybackParameters(PlaybackParameters playbackParameters) {}

  @Override
  public PlaybackParameters getPlaybackParameters() {
    // Playback parameters are unknown. Set default value.
    return PlaybackParameters.DEFAULT;
  }
}
