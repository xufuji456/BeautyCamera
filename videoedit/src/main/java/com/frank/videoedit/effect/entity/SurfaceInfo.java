package com.frank.videoedit.effect.entity;

import android.view.Surface;

public final class SurfaceInfo {

    public final Surface surface;

    public final int width;

    public final int height;

    public final int orientationDegrees;

    public SurfaceInfo(Surface surface, int width, int height) {
        this(surface, width, height, /* orientationDegrees= */ 0);
    }

    public SurfaceInfo(Surface surface, int width, int height, int orientationDegrees) {
        this.surface = surface;
        this.width   = width;
        this.height  = height;
        this.orientationDegrees = orientationDegrees;
    }

}
