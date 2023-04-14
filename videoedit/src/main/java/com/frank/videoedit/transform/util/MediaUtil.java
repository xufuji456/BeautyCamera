package com.frank.videoedit.transform.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.frank.videoedit.entity.ColorInfo;
import com.frank.videoedit.transform.Format;
import com.frank.videoedit.util.CommonUtil;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xufulong
 * @date 2023/4/6 12:19 下午
 * @desc
 */
public class MediaUtil {

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({
            Format.NO_VALUE,
            ENCODING_INVALID,
            ENCODING_PCM_8BIT,
            ENCODING_PCM_16BIT,
            ENCODING_PCM_16BIT_BIG_ENDIAN,
            ENCODING_PCM_24BIT,
            ENCODING_PCM_32BIT,
            ENCODING_PCM_FLOAT,
            ENCODING_MP3,
            ENCODING_AAC_LC,
            ENCODING_AAC_HE_V1,
            ENCODING_AAC_HE_V2
    })
    public @interface Encoding {}

    public static final int ENCODING_INVALID   = AudioFormat.ENCODING_INVALID;
    public static final int ENCODING_PCM_8BIT  = AudioFormat.ENCODING_PCM_8BIT;
    public static final int ENCODING_PCM_16BIT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int ENCODING_PCM_16BIT_BIG_ENDIAN = 0x10000000;
    public static final int ENCODING_PCM_24BIT = 0x20000000;
    public static final int ENCODING_PCM_32BIT = 0x30000000;
    public static final int ENCODING_PCM_FLOAT = AudioFormat.ENCODING_PCM_FLOAT;
    public static final int ENCODING_MP3       = AudioFormat.ENCODING_MP3;
    public static final int ENCODING_AAC_LC    = AudioFormat.ENCODING_AAC_LC;
    public static final int ENCODING_AAC_HE_V1 = AudioFormat.ENCODING_AAC_HE_V1;
    public static final int ENCODING_AAC_HE_V2 = AudioFormat.ENCODING_AAC_HE_V2;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({
            Format.NO_VALUE,
            ENCODING_INVALID,
            ENCODING_PCM_8BIT,
            ENCODING_PCM_16BIT,
            ENCODING_PCM_16BIT_BIG_ENDIAN,
            ENCODING_PCM_24BIT,
            ENCODING_PCM_32BIT,
            ENCODING_PCM_FLOAT
    })
    public @interface PcmEncoding {}

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({
            Format.NO_VALUE,
            STEREO_MODE_MONO,
            STEREO_MODE_TOP_BOTTOM,
            STEREO_MODE_LEFT_RIGHT,
            STEREO_MODE_STEREO_MESH
    })
    public @interface StereoMode {}

    /** Indicates a stereo layout: used with 360/3D/VR videos. */
    public static final int STEREO_MODE_MONO        = 0;
    public static final int STEREO_MODE_TOP_BOTTOM  = 1;
    public static final int STEREO_MODE_LEFT_RIGHT  = 2;
    public static final int STEREO_MODE_STEREO_MESH = 3;


    public static boolean isEncodingLinearPcm(@Encoding int encoding) {
        return encoding     == ENCODING_PCM_8BIT
                || encoding == ENCODING_PCM_16BIT
                || encoding == ENCODING_PCM_16BIT_BIG_ENDIAN
                || encoding == ENCODING_PCM_24BIT
                || encoding == ENCODING_PCM_32BIT
                || encoding == ENCODING_PCM_FLOAT;
    }

    public static int getPcmFrameSize(@PcmEncoding int pcmEncoding, int channelCount) {
        switch (pcmEncoding) {
            case ENCODING_PCM_8BIT:
                return channelCount;
            case ENCODING_PCM_16BIT:
            case ENCODING_PCM_16BIT_BIG_ENDIAN:
                return channelCount * 2;
            case ENCODING_PCM_24BIT:
                return channelCount * 3;
            case ENCODING_PCM_32BIT:
            case ENCODING_PCM_FLOAT:
                return channelCount * 4;
            case ENCODING_INVALID:
            case Format.NO_VALUE:
            default:
                throw new IllegalArgumentException();
        }
    }

    public static final int FORMAT_HANDLED              = 0b100;
    public static final int FORMAT_EXCEEDS_CAPABILITIES = 0b011;
    public static final int FORMAT_UNSUPPORTED_DRM      = 0b010;
    public static final int FORMAT_UNSUPPORTED_SUBTYPE  = 0b001;
    public static final int FORMAT_UNSUPPORTED_TYPE     = 0b000;

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
    public @interface FormatSupport {}

    public static final int RESULT_NOTHING_READ = -3;
    public static final int RESULT_BUFFER_READ  = -4;
    public static final int RESULT_FORMAT_READ  = -5;


    public static final int TRACK_TYPE_NONE          = -2;
    public static final int TRACK_TYPE_UNKNOWN       = -1;
    public static final int TRACK_TYPE_DEFAULT       = 0;
    public static final int TRACK_TYPE_AUDIO         = 1;
    public static final int TRACK_TYPE_VIDEO         = 2;
    public static final int TRACK_TYPE_TEXT          = 3;
    public static final int TRACK_TYPE_IMAGE         = 4;
    public static final int TRACK_TYPE_METADATA      = 5;
    public static final int TRACK_TYPE_CAMERA_MOTION = 6;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef(
            open = true,
            value = {
                    TRACK_TYPE_UNKNOWN,
                    TRACK_TYPE_DEFAULT,
                    TRACK_TYPE_AUDIO,
                    TRACK_TYPE_VIDEO,
                    TRACK_TYPE_TEXT,
                    TRACK_TYPE_IMAGE,
                    TRACK_TYPE_METADATA,
                    TRACK_TYPE_CAMERA_MOTION,
                    TRACK_TYPE_NONE,
            })
    public @interface TrackType {}


    public static final String BASE_TYPE_VIDEO       = "video";
    public static final String BASE_TYPE_AUDIO       = "audio";
    public static final String BASE_TYPE_TEXT        = "text";
    public static final String BASE_TYPE_IMAGE       = "image";
    public static final String BASE_TYPE_APPLICATION = "application";

    public static final String VIDEO_H263         = BASE_TYPE_VIDEO + "/3gpp";
    public static final String VIDEO_H264         = BASE_TYPE_VIDEO + "/avc";
    public static final String VIDEO_H265         = BASE_TYPE_VIDEO + "/hevc";
    public static final String VIDEO_MP4V         = BASE_TYPE_VIDEO + "/mp4v-es";
    public static final String VIDEO_VP9          = BASE_TYPE_VIDEO + "/x-vnd.on2.vp9";
    public static final String VIDEO_AV1          = BASE_TYPE_VIDEO + "/av01";
    public static final String VIDEO_DOLBY_VISION = BASE_TYPE_VIDEO + "/dolby-vision";

    public static final String AUDIO_AAC          = BASE_TYPE_AUDIO + "/mp4a-latm";
    public static final String AUDIO_AMR_NB       = BASE_TYPE_AUDIO + "/3gpp";
    public static final String AUDIO_AMR_WB       = BASE_TYPE_AUDIO + "/amr-wb";
    public static final String AUDIO_AC3          = BASE_TYPE_AUDIO + "/ac3";
    public static final String AUDIO_E_AC3        = BASE_TYPE_AUDIO + "/eac3";
    public static final String AUDIO_AC4          = BASE_TYPE_AUDIO + "/ac4";

    public static final String APPLICATION_ID3    = BASE_TYPE_APPLICATION + "/id3";
    public static final String APPLICATION_CEA608 = BASE_TYPE_APPLICATION + "/cea-608";
    public static final String APPLICATION_CEA708 = BASE_TYPE_APPLICATION + "/cea-708";
    public static final String APPLICATION_SUBRIP = BASE_TYPE_APPLICATION + "/x-subrip";
    public static final String APPLICATION_TTML   = BASE_TYPE_APPLICATION + "/ttml+xml";
    public static final String APPLICATION_TX3G   = BASE_TYPE_APPLICATION + "/x-quicktime-tx3g";
    public static final String APPLICATION_MP4VTT = BASE_TYPE_APPLICATION + "/x-mp4-vtt";
    public static final String APPLICATION_PGS    = BASE_TYPE_APPLICATION + "/pgs";
    public static final String APPLICATION_EMSG   = BASE_TYPE_APPLICATION + "/x-emsg";
    public static final String APPLICATION_SCTE35 = BASE_TYPE_APPLICATION + "/x-scte35";

    private static String getTopLevelType(@Nullable String mimeType) {
        if (mimeType == null) {
            return null;
        }
        int indexOfSlash = mimeType.indexOf('/');
        if (indexOfSlash == -1) {
            return null;
        }
        return mimeType.substring(0, indexOfSlash);
    }

    public static boolean isAudio(@Nullable String mimeType) {
        return BASE_TYPE_AUDIO.equals(getTopLevelType(mimeType));
    }

    public static boolean isVideo(@Nullable String mimeType) {
        return BASE_TYPE_VIDEO.equals(getTopLevelType(mimeType));
    }

    public static boolean isText(@Nullable String mimeType) {
        return BASE_TYPE_TEXT.equals(getTopLevelType(mimeType))
                || APPLICATION_CEA608.equals(mimeType)
                || APPLICATION_CEA708.equals(mimeType)
                || APPLICATION_SUBRIP.equals(mimeType)
                || APPLICATION_TTML.equals(mimeType)
                || APPLICATION_TX3G.equals(mimeType)
                || APPLICATION_MP4VTT.equals(mimeType)
                || APPLICATION_PGS.equals(mimeType);
    }

    public static boolean isImage(@Nullable String mimeType) {
        return BASE_TYPE_IMAGE.equals(getTopLevelType(mimeType));
    }

    public static @TrackType int getTrackType(@Nullable String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return TRACK_TYPE_UNKNOWN;
        } else if (isAudio(mimeType)) {
            return TRACK_TYPE_AUDIO;
        } else if (isVideo(mimeType)) {
            return TRACK_TYPE_VIDEO;
        } else if (isText(mimeType)) {
            return TRACK_TYPE_TEXT;
        } else if (isImage(mimeType)) {
            return TRACK_TYPE_IMAGE;
        } else if (APPLICATION_ID3.equals(mimeType)
                || APPLICATION_EMSG.equals(mimeType)
                || APPLICATION_SCTE35.equals(mimeType)) {
            return TRACK_TYPE_METADATA;
        } else {
            return TRACK_TYPE_CAMERA_MOTION;
        }
    }



    public static void setCsdBuffers(MediaFormat format, List<byte[]> csdBuffers) {
        for (int i = 0; i < csdBuffers.size(); i++) {
            format.setByteBuffer("csd-" + i, ByteBuffer.wrap(csdBuffers.get(i)));
        }
    }

    public static void maybeSetString(MediaFormat format, String key, @Nullable String value) {
        if (value != null) {
            format.setString(key, value);
        }
    }

    public static void maybeSetInteger(MediaFormat format, String key, int value) {
        if (value != Format.NO_VALUE) {
            format.setInteger(key, value);
        }
    }

    public static void maybeSetByteBuffer(MediaFormat format, String key, @Nullable byte[] value) {
        if (value != null) {
            format.setByteBuffer(key, ByteBuffer.wrap(value));
        }
    }

    public static void maybeSetColorInfo(MediaFormat format, @Nullable ColorInfo colorInfo) {
        if (colorInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            maybeSetInteger(format, MediaFormat.KEY_COLOR_TRANSFER, colorInfo.colorTransfer);
            maybeSetInteger(format, MediaFormat.KEY_COLOR_STANDARD, colorInfo.colorSpace);
            maybeSetInteger(format, MediaFormat.KEY_COLOR_RANGE, colorInfo.colorRange);
            maybeSetByteBuffer(format, MediaFormat.KEY_HDR_STATIC_INFO, colorInfo.hdrStaticInfo);
        }
    }

    public static byte[] getArray(ByteBuffer byteBuffer) {
        byte[] array = new byte[byteBuffer.remaining()];
        byteBuffer.get(array);
        return array;
    }

    public static ColorInfo getColorInfo(MediaFormat mediaFormat) {
        if (Build.VERSION.SDK_INT < 29) {
            return null;
        }
        int colorSpace =
                mediaFormat.getInteger(MediaFormat.KEY_COLOR_STANDARD, /* defaultValue= */ Format.NO_VALUE);
        int colorRange =
                mediaFormat.getInteger(MediaFormat.KEY_COLOR_RANGE, /* defaultValue= */ Format.NO_VALUE);
        int colorTransfer =
                mediaFormat.getInteger(MediaFormat.KEY_COLOR_TRANSFER, /* defaultValue= */ Format.NO_VALUE);
        @Nullable
        ByteBuffer hdrStaticInfoByteBuffer = mediaFormat.getByteBuffer(MediaFormat.KEY_HDR_STATIC_INFO);
        @Nullable
        byte[] hdrStaticInfo =
                hdrStaticInfoByteBuffer != null ? getArray(hdrStaticInfoByteBuffer) : null;
        if (!ColorInfo.isValidColorSpace(colorSpace)) {
            colorSpace = Format.NO_VALUE;
        }
        if (!ColorInfo.isValidColorRange(colorRange)) {
            colorRange = Format.NO_VALUE;
        }
        if (!ColorInfo.isValidColorTransfer(colorTransfer)) {
            colorTransfer = Format.NO_VALUE;
        }

        if (colorSpace != Format.NO_VALUE
                || colorRange != Format.NO_VALUE
                || colorTransfer != Format.NO_VALUE
                || hdrStaticInfo != null) {
            return new ColorInfo(colorSpace, colorRange, colorTransfer, hdrStaticInfo);
        }
        return null;
    }


    // AVC.
    private static final String CODEC_ID_AVC1 = "avc1";
    private static final String CODEC_ID_AVC2 = "avc2";
    // VP9
    private static final String CODEC_ID_VP09 = "vp09";
    // HEVC.
    private static final String CODEC_ID_HEV1 = "hev1";
    private static final String CODEC_ID_HVC1 = "hvc1";
    // AV1.
    private static final String CODEC_ID_AV01 = "av01";
    // MP4A AAC.
    private static final String CODEC_ID_MP4A = "mp4a";

    private static final Pattern PROFILE_PATTERN = Pattern.compile("^\\D?(\\d+)$");

    private static int avcProfileNumberToConst(int profileNumber) {
        switch (profileNumber) {
            case 66:
                return MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline;
            case 77:
                return MediaCodecInfo.CodecProfileLevel.AVCProfileMain;
            case 88:
                return MediaCodecInfo.CodecProfileLevel.AVCProfileExtended;
            case 100:
                return MediaCodecInfo.CodecProfileLevel.AVCProfileHigh;
            case 110:
                return MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10;
            case 122:
                return MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422;
            case 244:
                return MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444;
            default:
                return -1;
        }
    }

    private static int avcLevelNumberToConst(int levelNumber) {
        switch (levelNumber) {
            case 10:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel1;
            case 11:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel11;
            case 12:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel12;
            case 13:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel13;
            case 20:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel2;
            case 21:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel21;
            case 22:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel22;
            case 30:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel3;
            case 31:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel31;
            case 32:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel32;
            case 40:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel4;
            case 41:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel41;
            case 42:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel42;
            case 50:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel5;
            case 51:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel51;
            case 52:
                return MediaCodecInfo.CodecProfileLevel.AVCLevel52;
            default:
                return -1;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static int vp9ProfileNumberToConst(int profileNumber) {
        switch (profileNumber) {
            case 0:
                return MediaCodecInfo.CodecProfileLevel.VP9Profile0;
            case 1:
                return MediaCodecInfo.CodecProfileLevel.VP9Profile1;
            case 2:
                return MediaCodecInfo.CodecProfileLevel.VP9Profile2;
            case 3:
                return MediaCodecInfo.CodecProfileLevel.VP9Profile3;
            default:
                return -1;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static int vp9LevelNumberToConst(int levelNumber) {
        switch (levelNumber) {
            case 10:
                return MediaCodecInfo.CodecProfileLevel.VP9Level1;
            case 11:
                return MediaCodecInfo.CodecProfileLevel.VP9Level11;
            case 20:
                return MediaCodecInfo.CodecProfileLevel.VP9Level2;
            case 21:
                return MediaCodecInfo.CodecProfileLevel.VP9Level21;
            case 30:
                return MediaCodecInfo.CodecProfileLevel.VP9Level3;
            case 31:
                return MediaCodecInfo.CodecProfileLevel.VP9Level31;
            case 40:
                return MediaCodecInfo.CodecProfileLevel.VP9Level4;
            case 41:
                return MediaCodecInfo.CodecProfileLevel.VP9Level41;
            case 50:
                return MediaCodecInfo.CodecProfileLevel.VP9Level5;
            case 51:
                return MediaCodecInfo.CodecProfileLevel.VP9Level51;
            case 60:
                return MediaCodecInfo.CodecProfileLevel.VP9Level6;
            case 61:
                return MediaCodecInfo.CodecProfileLevel.VP9Level61;
            case 62:
                return MediaCodecInfo.CodecProfileLevel.VP9Level62;
            default:
                return -1;
        }
    }

    @Nullable
    private static Integer hevcCodecStringToProfileLevel(@Nullable String codecString) {
        if (codecString == null) {
            return null;
        }
        switch (codecString) {
            case "L30":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1;
            case "L60":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel2;
            case "L63":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel21;
            case "L90":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel3;
            case "L93":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel31;
            case "L120":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel4;
            case "L123":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel41;
            case "L150":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel5;
            case "L153":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel51;
            case "L156":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel52;
            case "L180":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel6;
            case "L183":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel61;
            case "L186":
                return MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel62;
            case "H30":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel1;
            case "H60":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel2;
            case "H63":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel21;
            case "H90":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel3;
            case "H93":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel31;
            case "H120":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel4;
            case "H123":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel41;
            case "H150":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel5;
            case "H153":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel51;
            case "H156":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52;
            case "H180":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel6;
            case "H183":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel61;
            case "H186":
                return MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62;
            default:
                return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    private static Integer dolbyVisionStringToProfile(@Nullable String profileString) {
        if (profileString == null) {
            return null;
        }
        switch (profileString) {
            case "00":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavPer;
            case "01":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavPen;
            case "02":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDer;
            case "03":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDen;
            case "04":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDtr;
            case "05":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheStn;
            case "06":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDth;
            case "07":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDtb;
            case "08":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt;
            case "09":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavSe;
            default:
                return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    private static Integer dolbyVisionStringToLevel(@Nullable String levelString) {
        if (levelString == null) {
            return null;
        }

        switch (levelString) {
            case "01":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelHd24;
            case "02":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelHd30;
            case "03":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelFhd24;
            case "04":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelFhd30;
            case "05":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelFhd60;
            case "06":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd24;
            case "07":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd30;
            case "08":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd48;
            case "09":
                return MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd60;
            case "10":
                return 0x200;
            case "11":
                return 0x400;
            case "12":
                return 0x800;
            case "13":
                return 0x1000;
            default:
                return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static int av1LevelNumberToConst(int levelNumber) {
        // See https://aomediacodec.github.io/av1-spec/av1-spec.pdf Annex A
        switch (levelNumber) {
            case 0:
                return MediaCodecInfo.CodecProfileLevel.AV1Level2;
            case 1:
                return MediaCodecInfo.CodecProfileLevel.AV1Level21;
            case 2:
                return MediaCodecInfo.CodecProfileLevel.AV1Level22;
            case 3:
                return MediaCodecInfo.CodecProfileLevel.AV1Level23;
            case 4:
                return MediaCodecInfo.CodecProfileLevel.AV1Level3;
            case 5:
                return MediaCodecInfo.CodecProfileLevel.AV1Level31;
            case 6:
                return MediaCodecInfo.CodecProfileLevel.AV1Level32;
            case 7:
                return MediaCodecInfo.CodecProfileLevel.AV1Level33;
            case 8:
                return MediaCodecInfo.CodecProfileLevel.AV1Level4;
            case 9:
                return MediaCodecInfo.CodecProfileLevel.AV1Level41;
            case 10:
                return MediaCodecInfo.CodecProfileLevel.AV1Level42;
            case 11:
                return MediaCodecInfo.CodecProfileLevel.AV1Level43;
            case 12:
                return MediaCodecInfo.CodecProfileLevel.AV1Level5;
            case 13:
                return MediaCodecInfo.CodecProfileLevel.AV1Level51;
            case 14:
                return MediaCodecInfo.CodecProfileLevel.AV1Level52;
            case 15:
                return MediaCodecInfo.CodecProfileLevel.AV1Level53;
            case 16:
                return MediaCodecInfo.CodecProfileLevel.AV1Level6;
            case 17:
                return MediaCodecInfo.CodecProfileLevel.AV1Level61;
            case 18:
                return MediaCodecInfo.CodecProfileLevel.AV1Level62;
            case 19:
                return MediaCodecInfo.CodecProfileLevel.AV1Level63;
            case 20:
                return MediaCodecInfo.CodecProfileLevel.AV1Level7;
            case 21:
                return MediaCodecInfo.CodecProfileLevel.AV1Level71;
            case 22:
                return MediaCodecInfo.CodecProfileLevel.AV1Level72;
            case 23:
                return MediaCodecInfo.CodecProfileLevel.AV1Level73;
            default:
                return -1;
        }
    }

    private static int mp4aAudioObjectTypeToProfile(int profileNumber) {
        switch (profileNumber) {
            case 1:
                return MediaCodecInfo.CodecProfileLevel.AACObjectMain;
            case 2:
                return MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            case 3:
                return MediaCodecInfo.CodecProfileLevel.AACObjectSSR;
            case 4:
                return MediaCodecInfo.CodecProfileLevel.AACObjectLTP;
            case 5:
                return MediaCodecInfo.CodecProfileLevel.AACObjectHE;
            case 6:
                return MediaCodecInfo.CodecProfileLevel.AACObjectScalable;
            case 17:
                return MediaCodecInfo.CodecProfileLevel.AACObjectERLC;
            case 20:
                return MediaCodecInfo.CodecProfileLevel.AACObjectERScalable;
            case 23:
                return MediaCodecInfo.CodecProfileLevel.AACObjectLD;
            case 29:
                return MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS;
            case 39:
                return MediaCodecInfo.CodecProfileLevel.AACObjectELD;
            case 42:
                return MediaCodecInfo.CodecProfileLevel.AACObjectXHE;
            default:
                return -1;
        }
    }

    private static Pair<Integer, Integer> getAvcProfileAndLevel(String codec, String[] parts) {
        if (parts.length < 2) {
            Log.e("MediaUtil", "Invalid Avc codec string: " + codec);
            return null;
        }
        int profileInteger;
        int levelInteger;
        try {
            if (parts[1].length() == 6) {
                // Format: avc1.xxccyy, where xx is profile and yy level, both hexadecimal.
                profileInteger = Integer.parseInt(parts[1].substring(0, 2), 16);
                levelInteger = Integer.parseInt(parts[1].substring(4), 16);
            } else if (parts.length >= 3) {
                // Format: avc1.xx.[y]yy where xx is profile and [y]yy level, both decimal.
                profileInteger = Integer.parseInt(parts[1]);
                levelInteger = Integer.parseInt(parts[2]);
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }

        int profile = avcProfileNumberToConst(profileInteger);
        if (profile == -1) {
            return null;
        }
        int level = avcLevelNumberToConst(levelInteger);
        if (level == -1) {
            return null;
        }
        return new Pair<>(profile, level);
    }

    private static Pair<Integer, Integer> getHevcProfileAndLevel(
            String codec, String[] parts, @Nullable ColorInfo colorInfo) {
        if (parts.length < 4) {
            Log.e("MediaUtil", "Invalid Hevc codec string: " + codec);
            return null;
        }
        Matcher matcher = PROFILE_PATTERN.matcher(parts[1]);
        if (!matcher.matches()) {
            return null;
        }
        @Nullable String profileString = matcher.group(1);
        int profile;
        if ("1".equals(profileString)) {
            profile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain;
        } else if ("2".equals(profileString)) {
            if (colorInfo != null && colorInfo.colorTransfer == ColorInfo.COLOR_TRANSFER_ST2084) {
                profile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10;
            } else {
                // For all other cases, we map to the Main10 profile. Note that this includes HLG
                // HDR. On Android 13+, the platform guarantees that a decoder that advertises
                // HEVCProfileMain10 will be able to decode HLG.
                profile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10;
            }
        } else {
            return null;
        }
        @Nullable String levelString = parts[3];
        @Nullable Integer level = hevcCodecStringToProfileLevel(levelString);
        if (level == null) {
            return null;
        }
        return new Pair<>(profile, level);
    }

    private static Pair<Integer, Integer> getVp9ProfileAndLevel(String codec, String[] parts) {
        if (parts.length < 3) {
            Log.e("MediaUtil", "Invalid VP9 codec string: " + codec);
            return null;
        }
        int profileInteger;
        int levelInteger;
        try {
            profileInteger = Integer.parseInt(parts[1]);
            levelInteger = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        int profile = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            profile = vp9ProfileNumberToConst(profileInteger);
        }
        int level = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            level = vp9LevelNumberToConst(levelInteger);
        }
        if (profile == 0 || level == -1) {
            return null;
        }
        return new Pair<>(profile, level);
    }

    private static Pair<Integer, Integer> getAv1ProfileAndLevel(
            String codec, String[] parts, @Nullable ColorInfo colorInfo) {
        if (parts.length < 4) {
            Log.e("MediaUtil", "Invalid AV1 codec string: " + codec);
            return null;
        }
        int profileInteger;
        int levelInteger;
        int bitDepthInteger;
        try {
            profileInteger = Integer.parseInt(parts[1]);
            levelInteger = Integer.parseInt(parts[2].substring(0, 2));
            bitDepthInteger = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (profileInteger != 0) {
            return null;
        }
        if (bitDepthInteger != 8 && bitDepthInteger != 10) {
            return null;
        }
        int profile = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (bitDepthInteger == 8) {
                profile = MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8;
            } else if (colorInfo != null
                    && (colorInfo.hdrStaticInfo != null
                    || colorInfo.colorTransfer == ColorInfo.COLOR_TRANSFER_HLG
                    || colorInfo.colorTransfer == ColorInfo.COLOR_TRANSFER_ST2084)) {
                profile = MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10;
            } else {
                profile = MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10;
            }
        }

        int level = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            level = av1LevelNumberToConst(levelInteger);
        }
        if (level == -1) {
            return null;
        }
        return new Pair<>(profile, level);
    }

    private static Pair<Integer, Integer> getDolbyVisionProfileAndLevel(String codec, String[] parts) {
        if (parts.length < 3) {
            Log.e("MediaUtil", "Invalid DolbyVision codec string: " + codec);
            return null;
        }
        Matcher matcher = PROFILE_PATTERN.matcher(parts[1]);
        if (!matcher.matches()) {
            return null;
        }
        String profileString = matcher.group(1);
        String levelString = parts[2];
        Integer profile = null;
        Integer level   = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            profile = dolbyVisionStringToProfile(profileString);
            level   = dolbyVisionStringToLevel(levelString);
        }
        if (profile == null || level == null) {
            return null;
        }
        return new Pair<>(profile, level);
    }

    public static String getMimeTypeFromMp4ObjectType(int objectType) {
        switch (objectType) {
            case 0x20:
                return VIDEO_MP4V;
            case 0x21:
                return VIDEO_H264;
            case 0x23:
                return VIDEO_H265;
            case 0xB1:
                return VIDEO_VP9;
            case 0x40:
            case 0x66:
            case 0x67:
            case 0x68:
                return AUDIO_AAC;
            case 0xA5:
                return AUDIO_AC3;
            case 0xA6:
                return AUDIO_E_AC3;
            case 0xAE:
                return AUDIO_AC4;
            default:
                return null;
        }
    }

    private static Pair<Integer, Integer> getAacCodecProfileAndLevel(String codec, String[] parts) {
        if (parts.length != 3) {
            Log.e("MediaUtil", "Invalid MP4A codec string: " + codec);
            return null;
        }
        try {
            int objectTypeIndication = Integer.parseInt(parts[1], 16);
            @Nullable String mimeType = getMimeTypeFromMp4ObjectType(objectTypeIndication);
            if (AUDIO_AAC.equals(mimeType)) {
                int audioObjectTypeIndication = Integer.parseInt(parts[2]);
                int profile = mp4aAudioObjectTypeToProfile(audioObjectTypeIndication);
                if (profile != -1) {
                    return new Pair<>(profile, 0);
                }
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(e.getMessage());
        }
        return null;
    }

    public static Pair<Integer, Integer> getCodecProfileAndLevel(Format format) {
        if (format.codecs == null) {
            return null;
        }
        String[] parts = format.codecs.split("\\.");

        if (VIDEO_DOLBY_VISION.equals(format.sampleMimeType)) {
            return getDolbyVisionProfileAndLevel(format.codecs, parts);
        }
        switch (parts[0]) {
            case CODEC_ID_AVC1:
            case CODEC_ID_AVC2:
                return getAvcProfileAndLevel(format.codecs, parts);
            case CODEC_ID_VP09:
                return getVp9ProfileAndLevel(format.codecs, parts);
            case CODEC_ID_HEV1:
            case CODEC_ID_HVC1:
                return getHevcProfileAndLevel(format.codecs, parts, format.colorInfo);
            case CODEC_ID_AV01:
                return getAv1ProfileAndLevel(format.codecs, parts, format.colorInfo);
            case CODEC_ID_MP4A:
                return getAacCodecProfileAndLevel(format.codecs, parts);
            default:
                return null;
        }
    }


    public static String getMediaMimeType(@Nullable String codec) {
        if (codec == null) {
            return null;
        }
        codec = CommonUtil.toLowerCase(codec.trim());
        if (codec.startsWith("avc1") || codec.startsWith("avc3")) {
            return VIDEO_H264;
        } else if (codec.startsWith("hev1") || codec.startsWith("hvc1")) {
            return VIDEO_H265;
        } else if (codec.startsWith("dvav")
                || codec.startsWith("dva1")
                || codec.startsWith("dvhe")
                || codec.startsWith("dvh1")) {
            return VIDEO_DOLBY_VISION;
        } else if (codec.startsWith("av01")) {
            return VIDEO_AV1;
        } else if (codec.startsWith("vp9") || codec.startsWith("vp09")) {
            return VIDEO_VP9;
        } else if (codec.startsWith("mp4a")) {
            return AUDIO_AAC;
        }
        return null;
    }

    public static String[] split(String value, String regex) {
        return value.split(regex, /* limit= */ -1);
    }

    public static String[] splitCodecs(@Nullable String codecs) {
        if (TextUtils.isEmpty(codecs)) {
            return new String[0];
        }
        return split(codecs.trim(), "(\\s*,\\s*)");
    }

    public static String getCodecsOfType(@Nullable String codecs, @TrackType int trackType) {
        String[] codecArray = splitCodecs(codecs);
        if (codecArray.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String codec : codecArray) {
            if (trackType == getTrackType(getMediaMimeType(codec))) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(codec);
            }
        }
        return builder.length() > 0 ? builder.toString() : null;
    }


}
