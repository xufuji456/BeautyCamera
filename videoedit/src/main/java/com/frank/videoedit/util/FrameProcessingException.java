package com.frank.videoedit.util;

public final class FrameProcessingException extends Exception {

  private static final long TIME_UNSET = Long.MIN_VALUE + 1;

  public static FrameProcessingException from(Exception exception) {
    return from(exception, /* presentationTimeUs= */ TIME_UNSET);
  }

  public static FrameProcessingException from(Exception exception, long presentationTimeUs) {
    if (exception instanceof FrameProcessingException) {
      return (FrameProcessingException) exception;
    } else {
      return new FrameProcessingException(exception, presentationTimeUs);
    }
  }

  public final long presentationTimeUs;

  public FrameProcessingException(String message) {
    this(message, /* presentationTimeUs= */ TIME_UNSET);
  }


  public FrameProcessingException(String message, long presentationTimeUs) {
    super(message);
    this.presentationTimeUs = presentationTimeUs;
  }

  public FrameProcessingException(String message, Throwable cause) {
    this(message, cause, /* presentationTimeUs= */ TIME_UNSET);
  }

  public FrameProcessingException(String message, Throwable cause, long presentationTimeUs) {
    super(message, cause);
    this.presentationTimeUs = presentationTimeUs;
  }

  public FrameProcessingException(Throwable cause) {
    this(cause, /* presentationTimeUs= */ TIME_UNSET);
  }

  public FrameProcessingException(Throwable cause, long presentationTimeUs) {
    super(cause);
    this.presentationTimeUs = presentationTimeUs;
  }
}