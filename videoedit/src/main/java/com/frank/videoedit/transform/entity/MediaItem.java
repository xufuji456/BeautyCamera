
package com.frank.videoedit.transform.entity;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.frank.videoedit.listener.Bundleable;

public final class MediaItem implements Bundleable {

  public static MediaItem fromUri(String uri) {
    return new Builder().setUri(uri).build();
  }

  public static MediaItem fromUri(Uri uri) {
    return new Builder().setUri(uri).build();
  }

  public static final class Builder {

    @Nullable private String mediaId;
    @Nullable private Uri uri;
    @Nullable private String mimeType;

    public Builder() {

    }

    private Builder(MediaItem mediaItem) {
      this();
      uri     = mediaItem.uri;
      mediaId = mediaItem.mediaId;
    }

    public Builder setMediaId(String mediaId) {
      this.mediaId = mediaId;
      return this;
    }

    public Builder setUri(@Nullable String uri) {
      return setUri(uri == null ? null : Uri.parse(uri));
    }

    public Builder setUri(@Nullable Uri uri) {
      this.uri = uri;
      return this;
    }

    public Builder setMimeType(@Nullable String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public MediaItem build() {
      return new MediaItem(this.mediaId, this.uri);
    }
  }

  public static final MediaItem EMPTY = new Builder().build();

  public final Uri uri;

  public final String mediaId;

  private MediaItem(String mediaId, Uri uri) {
    this.uri     = uri;
    this.mediaId = mediaId;
  }

  public Builder buildUpon() {
    return new Builder(this);
  }

  private static final String FIELD_MEDIA_ID = Integer.toString(0, Character.MAX_RADIX);
  private static final String FIELD_URI_ID = Integer.toString(1, Character.MAX_RADIX);

  @Override
  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    bundle.putString(FIELD_MEDIA_ID, mediaId);
    bundle.putString(FIELD_URI_ID, uri.toString());
    return bundle;
  }

  public static final Creator<MediaItem> CREATOR = MediaItem::fromBundle;

  private static MediaItem fromBundle(Bundle bundle) {
    String uriId = bundle.getString(FIELD_URI_ID, "");
    String mediaId = bundle.getString(FIELD_MEDIA_ID, "");
    return new MediaItem(mediaId, Uri.parse(uriId));
  }

}
