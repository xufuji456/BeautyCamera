package com.frank.videoedit.effect.entity;

public class FrameInfo {

    public final int width;

    public final int height;

    public final float pixelWidthHeightRatio;

    public final long streamOffsetUs;

    public FrameInfo(int width, int height, float pixelWidthHeightRatio, long streamOffsetUs) {

        this.width = width;
        this.height = height;
        this.pixelWidthHeightRatio = pixelWidthHeightRatio;
        this.streamOffsetUs = streamOffsetUs;
    }
}
