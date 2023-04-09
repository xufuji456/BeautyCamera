package com.frank.videoedit.transform;

import androidx.annotation.IntRange;

/** Holds a progress percentage. */
public final class ProgressHolder {

  /** The held progress, expressed as an integer percentage. */
  @IntRange(from = 0, to = 100)
  public int progress;
}
