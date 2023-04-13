package com.frank.videoedit.transform.listener;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface AudioProcessor {

    final class AudioFormat {
        public static final AudioFormat NOT_SET =
                new AudioFormat(
                        Format.NO_VALUE,
                        Format.NO_VALUE,
                        Format.NO_VALUE);

        public final int sampleRate;
        public final int channelCount;
        public final @C.PcmEncoding int encoding;
        public final int bytesPerFrame;

        public AudioFormat(int sampleRate, int channelCount, @C.PcmEncoding int encoding) {
            this.sampleRate = sampleRate;
            this.channelCount = channelCount;
            this.encoding = encoding;
            bytesPerFrame =
                    Util.isEncodingLinearPcm(encoding)
                            ? Util.getPcmFrameSize(encoding, channelCount)
                            : Format.NO_VALUE;
        }

    }

    final class UnhandledAudioFormatException extends Exception {

        public UnhandledAudioFormatException(AudioFormat inputAudioFormat) {
            super("Unhandled format: " + inputAudioFormat);
        }
    }

    ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());

    AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException;

    boolean isActive();

    void queueInput(ByteBuffer inputBuffer);

    void queueEndOfStream();

    ByteBuffer getOutput();

    boolean isEnded();

    void flush();

    void reset();
}
