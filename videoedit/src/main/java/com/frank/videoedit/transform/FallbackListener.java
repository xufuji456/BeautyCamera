package com.frank.videoedit.transform;

import com.frank.videoedit.util.CommonUtil;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.HandlerWrapper;
import com.google.android.exoplayer2.util.ListenerSet;

/* package */ final class FallbackListener {

  private final MediaItem mediaItem;
  private final TransformationRequest originalTransformationRequest;
  private final ListenerSet<Transformer.Listener> transformerListeners;
  private final HandlerWrapper transformerListenerHandler;

  private TransformationRequest fallbackTransformationRequest;
  private int trackCount;

  public FallbackListener(
      MediaItem mediaItem,
      ListenerSet<Transformer.Listener> transformerListeners,
      HandlerWrapper transformerListenerHandler,
      TransformationRequest originalTransformationRequest) {
    this.mediaItem = mediaItem;
    this.transformerListeners = transformerListeners;
    this.transformerListenerHandler = transformerListenerHandler;
    this.originalTransformationRequest = originalTransformationRequest;
    this.fallbackTransformationRequest = originalTransformationRequest;
  }

  public void registerTrack() {
    trackCount++;
  }

  public void onTransformationRequestFinalized(TransformationRequest transformationRequest) {

    TransformationRequest.Builder fallbackRequestBuilder =
        fallbackTransformationRequest.buildUpon();
    if (!CommonUtil.areEqual(
        transformationRequest.audioMimeType, originalTransformationRequest.audioMimeType)) {
      fallbackRequestBuilder.setAudioMimeType(transformationRequest.audioMimeType);
    }
    if (!CommonUtil.areEqual(
        transformationRequest.videoMimeType, originalTransformationRequest.videoMimeType)) {
      fallbackRequestBuilder.setVideoMimeType(transformationRequest.videoMimeType);
    }
    if (transformationRequest.outputHeight != originalTransformationRequest.outputHeight) {
      fallbackRequestBuilder.setResolution(transformationRequest.outputHeight);
    }
    if (transformationRequest.enableHdrEditing != originalTransformationRequest.enableHdrEditing) {
      fallbackRequestBuilder.experimental_setEnableHdrEditing(
          transformationRequest.enableHdrEditing);
    }
    if (transformationRequest.enableRequestSdrToneMapping
        != originalTransformationRequest.enableRequestSdrToneMapping) {
      fallbackRequestBuilder.setEnableRequestSdrToneMapping(
          transformationRequest.enableRequestSdrToneMapping);
    }
    TransformationRequest newFallbackTransformationRequest = fallbackRequestBuilder.build();
    fallbackTransformationRequest = newFallbackTransformationRequest;

    if (trackCount == 0 && !originalTransformationRequest.equals(fallbackTransformationRequest)) {
      transformerListenerHandler.post(
          () ->
              transformerListeners.sendEvent(
                  /* eventFlag= */ C.INDEX_UNSET,
                  listener ->
                      listener.onFallbackApplied(
                          mediaItem,
                          originalTransformationRequest,
                          newFallbackTransformationRequest)));
    }
  }
}
