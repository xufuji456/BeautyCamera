
package com.frank.videoedit.transform.entity;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.media.MediaCodec;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class Buffer {

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef(
          flag = true,
          value = {
                  BUFFER_FLAG_KEY_FRAME,
                  BUFFER_FLAG_END_OF_STREAM,
                  BUFFER_FLAG_FIRST_SAMPLE,
                  BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA,
                  BUFFER_FLAG_LAST_SAMPLE,
                  BUFFER_FLAG_ENCRYPTED,
                  BUFFER_FLAG_DECODE_ONLY
          })
  public @interface BufferFlags {}

  public static final int BUFFER_FLAG_KEY_FRAME = MediaCodec.BUFFER_FLAG_KEY_FRAME;
  public static final int BUFFER_FLAG_END_OF_STREAM = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
  public static final int BUFFER_FLAG_FIRST_SAMPLE = 1 << 27;
  public static final int BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA = 1 << 28;
  public static final int BUFFER_FLAG_LAST_SAMPLE = 1 << 29;
  public static final int BUFFER_FLAG_ENCRYPTED = 1 << 30;
  public static final int BUFFER_FLAG_DECODE_ONLY = 1 << 31;

  public @BufferFlags int flags;

  public void clear() {
    flags = 0;
  }

  public final boolean isDecodeOnly() {
    return getFlag(BUFFER_FLAG_DECODE_ONLY);
  }

  public final boolean isFirstSample() {
    return getFlag(BUFFER_FLAG_FIRST_SAMPLE);
  }

  public final boolean isEndOfStream() {
    return getFlag(BUFFER_FLAG_END_OF_STREAM);
  }

  public final boolean isKeyFrame() {
    return getFlag(BUFFER_FLAG_KEY_FRAME);
  }

  public final boolean hasSupplementalData() {
    return getFlag(BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA);
  }

  public final void setFlags(@BufferFlags int flags) {
    this.flags = flags;
  }

  public final void addFlag(@BufferFlags int flag) {
    flags |= flag;
  }

  public final void clearFlag(@BufferFlags int flag) {
    flags &= ~flag;
  }

  protected final boolean getFlag(@BufferFlags int flag) {
    return (flags & flag) == flag;
  }
}
