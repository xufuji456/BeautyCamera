package com.frank.videoedit.util;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.os.Looper;
import android.util.SparseLongArray;

import androidx.annotation.Nullable;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommonUtil {

    public static final int INDEX_UNSET = -1;

    public static final int LENGTH_UNSET = -1;

    public static final float RATE_UNSET = -Float.MAX_VALUE;

    public static final int RATE_UNSET_INT = Integer.MIN_VALUE + 1;

    public static final long TIME_UNSET = Long.MIN_VALUE + 1;

    public static final long TIME_END_OF_SOURCE = Long.MIN_VALUE;

    public static final long MILLIS_PER_SECOND = 1000L;

    public static final long MICROS_PER_SECOND = 1000000L;


    public static boolean areEqual(Object a, Object b) {
        return a != null && a.equals(b);
    }

    public static Looper getCurrentOrMainLooper() {
        @Nullable Looper myLooper = Looper.myLooper();
        return myLooper != null ? myLooper : Looper.getMainLooper();
    }

    public static long usToMs(long timeUs) {
        return (timeUs == TIME_UNSET || timeUs == TIME_END_OF_SOURCE) ? timeUs : (timeUs / 1000);
    }

    public static long msToUs(long timeMs) {
        return (timeMs == TIME_UNSET || timeMs == TIME_END_OF_SOURCE) ? timeMs : (timeMs * 1000);
    }

    public static long minValue(SparseLongArray sparseLongArray) {
        if (sparseLongArray.size() == 0) {
            throw new NoSuchElementException();
        }
        long min = Long.MAX_VALUE;
        for (int i = 0; i < sparseLongArray.size(); i++) {
            min = min(min, sparseLongArray.valueAt(i));
        }
        return min;
    }

    public static long maxValue(SparseLongArray sparseLongArray) {
        if (sparseLongArray.size() == 0) {
            throw new NoSuchElementException();
        }
        long max = Long.MIN_VALUE;
        for (int i = 0; i < sparseLongArray.size(); i++) {
            max = max(max, sparseLongArray.valueAt(i));
        }
        return max;
    }

    public static long scaleLargeTimestamp(long timestamp, long multiplier, long divisor) {
        if (divisor >= multiplier && (divisor % multiplier) == 0) {
            long divisionFactor = divisor / multiplier;
            return timestamp / divisionFactor;
        } else if (divisor < multiplier && (multiplier % divisor) == 0) {
            long multiplicationFactor = multiplier / divisor;
            return timestamp * multiplicationFactor;
        } else {
            double multiplicationFactor = (double) multiplier / divisor;
            return (long) (timestamp * multiplicationFactor);
        }
    }

    public static ExecutorService newSingleThreadExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, threadName));
    }

}
