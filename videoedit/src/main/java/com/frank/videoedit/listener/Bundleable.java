package com.frank.videoedit.listener;

import android.os.Bundle;

public interface Bundleable {

    Bundle toBundle();

    interface Creator<T extends Bundleable> {
        T fromBundle(Bundle bundle);
    }
}
