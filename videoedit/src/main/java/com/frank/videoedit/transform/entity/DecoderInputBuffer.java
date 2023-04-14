
package com.frank.videoedit.transform.entity;

import static java.lang.annotation.ElementType.TYPE_USE;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

//import com.frank.videoedit.transform.Format;

import com.google.android.exoplayer2.Format;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

public class DecoderInputBuffer extends Buffer {

  public static final class InsufficientCapacityException extends IllegalStateException {

    public final int currentCapacity;

    public final int requiredCapacity;

    public InsufficientCapacityException(int currentCapacity, int requiredCapacity) {
      super("Buffer too small (" + currentCapacity + " < " + requiredCapacity + ")");
      this.currentCapacity = currentCapacity;
      this.requiredCapacity = requiredCapacity;
    }
  }

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    BUFFER_REPLACEMENT_MODE_DISABLED,
    BUFFER_REPLACEMENT_MODE_NORMAL,
    BUFFER_REPLACEMENT_MODE_DIRECT
  })
  public @interface BufferReplacementMode {}

  public static final int BUFFER_REPLACEMENT_MODE_DISABLED = 0;

  public static final int BUFFER_REPLACEMENT_MODE_NORMAL = 1;

  public static final int BUFFER_REPLACEMENT_MODE_DIRECT = 2;

  @Nullable public Format format;

  @Nullable public ByteBuffer data;

  public boolean waitingForKeys;

  public long timeUs;

  @Nullable public ByteBuffer supplementalData;

  private final @BufferReplacementMode int bufferReplacementMode;
  private final int paddingSize;

  public static DecoderInputBuffer newNoDataInstance() {
    return new DecoderInputBuffer(BUFFER_REPLACEMENT_MODE_DISABLED);
  }

  public DecoderInputBuffer(@BufferReplacementMode int bufferReplacementMode) {
    this(bufferReplacementMode, 0);
  }

  public DecoderInputBuffer(@BufferReplacementMode int bufferReplacementMode, int paddingSize) {
    this.bufferReplacementMode = bufferReplacementMode;
    this.paddingSize = paddingSize;
  }

  public void resetSupplementalData(int length) {
    if (supplementalData == null || supplementalData.capacity() < length) {
      supplementalData = ByteBuffer.allocate(length);
    } else {
      supplementalData.clear();
    }
  }

  public void ensureSpaceForWrite(int length) {
    length += paddingSize;
    @Nullable ByteBuffer currentData = data;
    if (currentData == null) {
      data = createReplacementByteBuffer(length);
      return;
    }
    // Check whether the current buffer is sufficient.
    int capacity = currentData.capacity();
    int position = currentData.position();
    int requiredCapacity = position + length;
    if (capacity >= requiredCapacity) {
      data = currentData;
      return;
    }
    // Instantiate a new buffer if possible.
    ByteBuffer newData = createReplacementByteBuffer(requiredCapacity);
    newData.order(currentData.order());
    // Copy data up to the current position from the old buffer to the new one.
    if (position > 0) {
      currentData.flip();
      newData.put(currentData);
    }
    // Set the new buffer.
    data = newData;
  }

  public final boolean isEncrypted() {
//    return getFlag(C.BUFFER_FLAG_ENCRYPTED);
    return false;
  }

  public final void flip() {
    if (data != null) {
      data.flip();
    }
    if (supplementalData != null) {
      supplementalData.flip();
    }
  }

  @Override
  public void clear() {
    super.clear();
    if (data != null) {
      data.clear();
    }
    if (supplementalData != null) {
      supplementalData.clear();
    }
    waitingForKeys = false;
  }

  private ByteBuffer createReplacementByteBuffer(int requiredCapacity) {
    if (bufferReplacementMode == BUFFER_REPLACEMENT_MODE_NORMAL) {
      return ByteBuffer.allocate(requiredCapacity);
    } else if (bufferReplacementMode == BUFFER_REPLACEMENT_MODE_DIRECT) {
      return ByteBuffer.allocateDirect(requiredCapacity);
    } else {
      int currentCapacity = data == null ? 0 : data.capacity();
      throw new InsufficientCapacityException(currentCapacity, requiredCapacity);
    }
  }
}
