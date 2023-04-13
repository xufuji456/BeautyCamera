
package com.frank.videoedit.transform.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.frank.videoedit.transform.Format;

import java.util.Arrays;
import java.util.List;

public final class Metadata implements Parcelable {

  public interface Entry extends Parcelable {

    default Format getWrappedMetadataFormat() {
      return null;
    }

    default byte[] getWrappedMetadataBytes() {
      return null;
    }

//    default void populateMediaMetadata(MediaMetadata.Builder builder) {}
  }

  private final Entry[] entries;

  public final long presentationTimeUs;

  public Metadata(Entry... entries) {
    this(Long.MIN_VALUE + 1, entries);
  }

  public Metadata(long presentationTimeUs, Entry... entries) {
    this.presentationTimeUs = presentationTimeUs;
    this.entries = entries;
  }

  public Metadata(List<? extends Entry> entries) {
    this(entries.toArray(new Entry[0]));
  }

  public Metadata(long presentationTimeUs, List<? extends Entry> entries) {
    this(presentationTimeUs, entries.toArray(new Entry[0]));
  }

  /* package */ Metadata(Parcel in) {
    entries = new Entry[in.readInt()];
    for (int i = 0; i < entries.length; i++) {
      entries[i] = in.readParcelable(Entry.class.getClassLoader());
    }
    presentationTimeUs = in.readLong();
  }

  public int length() {
    return entries.length;
  }

  public Entry get(int index) {
    return entries[index];
  }

  public Metadata copyWithAppendedEntriesFrom(@Nullable Metadata other) {
    if (other == null) {
      return this;
    }
    return copyWithAppendedEntries(other.entries);
  }

  public static <T> T[] nullSafeArrayConcatenation(T[] first, T[] second) {
    T[] concatenation = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, concatenation, first.length, second.length);
    return concatenation;
  }

  public Metadata copyWithAppendedEntries(Entry... entriesToAppend) {
    if (entriesToAppend.length == 0) {
      return this;
    }
    return new Metadata(presentationTimeUs, nullSafeArrayConcatenation(entries, entriesToAppend));
  }

  public Metadata copyWithPresentationTimeUs(long presentationTimeUs) {
    if (this.presentationTimeUs == presentationTimeUs) {
      return this;
    }
    return new Metadata(presentationTimeUs, entries);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(entries.length);
    for (Entry entry : entries) {
      dest.writeParcelable(entry, 0);
    }
    dest.writeLong(presentationTimeUs);
  }

  public static final Creator<Metadata> CREATOR =
      new Creator<Metadata>() {
        @Override
        public Metadata createFromParcel(Parcel in) {
          return new Metadata(in);
        }

        @Override
        public Metadata[] newArray(int size) {
          return new Metadata[size];
        }
      };
}
