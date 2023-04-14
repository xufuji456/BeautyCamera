package com.frank.videoedit.transform.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface RenderCapability {

    int FORMAT_HANDLED              = 0b100;
    int FORMAT_EXCEEDS_CAPABILITIES = 0b011;
    int FORMAT_UNSUPPORTED_DRM      = 0b010;
    int FORMAT_UNSUPPORTED_SUBTYPE  = 0b001;
    int FORMAT_UNSUPPORTED_TYPE     = 0b000;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
    @IntDef({
            FORMAT_HANDLED,
            FORMAT_EXCEEDS_CAPABILITIES,
            FORMAT_UNSUPPORTED_DRM,
            FORMAT_UNSUPPORTED_SUBTYPE,
            FORMAT_UNSUPPORTED_TYPE
    })
    @interface FormatSupport {}


    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({ADAPTIVE_SEAMLESS, ADAPTIVE_NOT_SEAMLESS, ADAPTIVE_NOT_SUPPORTED})
    @interface AdaptiveSupport {}

    int ADAPTIVE_SUPPORT_MASK = 0b11 << 3;
    int ADAPTIVE_SEAMLESS = 0b10 << 3;
    int ADAPTIVE_NOT_SEAMLESS = 0b01 << 3;
    int ADAPTIVE_NOT_SUPPORTED = 0;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({TUNNELING_SUPPORTED, TUNNELING_NOT_SUPPORTED})
    @interface TunnelingSupport {}

    int TUNNELING_SUPPORT_MASK = 0b1 << 5;
    int TUNNELING_SUPPORTED = 0b1 << 5;
    int TUNNELING_NOT_SUPPORTED = 0;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({
            HARDWARE_ACCELERATION_SUPPORTED,
            HARDWARE_ACCELERATION_NOT_SUPPORTED,
    })
    @interface HardwareAccelerationSupport {}

    int HARDWARE_ACCELERATION_SUPPORT_MASK = 0b1 << 6;
    int HARDWARE_ACCELERATION_SUPPORTED = 0b1 << 6;
    int HARDWARE_ACCELERATION_NOT_SUPPORTED = 0;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({DECODER_SUPPORT_FALLBACK_MIMETYPE, DECODER_SUPPORT_PRIMARY, DECODER_SUPPORT_FALLBACK})
    @interface DecoderSupport {}

    int DECODER_SUPPORT_FALLBACK_MIMETYPE = 0b10 << 7;
    int DECODER_SUPPORT_PRIMARY = 0b1 << 7;
    int DECODER_SUPPORT_FALLBACK = 0;

    @Documented
    @Retention(RetentionPolicy.SOURCE)

    @Target(TYPE_USE)
    @IntDef({})
    @interface Capabilities {}

    static @Capabilities int create(@FormatSupport int formatSupport) {
        return create(formatSupport, ADAPTIVE_NOT_SUPPORTED, TUNNELING_NOT_SUPPORTED);
    }

    static @Capabilities int create(
            @FormatSupport int formatSupport,
            @AdaptiveSupport int adaptiveSupport,
            @TunnelingSupport int tunnelingSupport) {
        return create(
                formatSupport,
                adaptiveSupport,
                tunnelingSupport,
                HARDWARE_ACCELERATION_NOT_SUPPORTED,
                DECODER_SUPPORT_PRIMARY);
    }

    @SuppressLint("WrongConstant")
    static @Capabilities int create(
            @FormatSupport int formatSupport,
            @AdaptiveSupport int adaptiveSupport,
            @TunnelingSupport int tunnelingSupport,
            @HardwareAccelerationSupport int hardwareAccelerationSupport,
            @DecoderSupport int decoderSupport) {
        return formatSupport
                | adaptiveSupport
                | tunnelingSupport
                | hardwareAccelerationSupport
                | decoderSupport;
    }

}
