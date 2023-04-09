package com.frank.videoedit.transform;

import com.google.android.exoplayer2.C;

/** A custom interface that determines the speed for media at specific timestamps. */
/* package */ interface SpeedProvider {

  /**
   * Provides the speed that the media should be played at, based on the timeUs.
   *
   * @param timeUs The timestamp of the media.
   * @return The speed that the media should be played at, based on the timeUs.
   */
  float getSpeed(long timeUs);

  /**
   * Returns the timestamp of the next speed change, if there is any.
   *
   * @param timeUs A timestamp, in microseconds.
   * @return The timestamp of the next speed change, in microseconds, or {@link C#TIME_UNSET} if
   *     there is no next speed change. If {@code timeUs} corresponds to a speed change, the
   *     returned value corresponds to the following speed change.
   */
  long getNextSpeedChangeTimeUs(long timeUs);
}
