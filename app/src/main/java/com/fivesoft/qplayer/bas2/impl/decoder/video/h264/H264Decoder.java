package com.fivesoft.qplayer.bas2.impl.decoder.video.h264;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.Csd;
import com.fivesoft.qplayer.bas2.Frame;
import com.fivesoft.qplayer.bas2.MediaDecoderException;
import com.fivesoft.qplayer.bas2.Sample;
import com.fivesoft.qplayer.bas2.UnsupportedSampleException;
import com.fivesoft.qplayer.bas2.common.Constants;
import com.fivesoft.qplayer.bas2.common.Size;
import com.fivesoft.qplayer.bas2.common.Util;
import com.fivesoft.qplayer.bas2.core.FrameBuilder;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;
import com.fivesoft.qplayer.bas2.decoder.VideoDecoder;
import com.fivesoft.qplayer.track.VideoTrack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class H264Decoder extends VideoDecoder {

    private static final long DEQUEUE_INPUT_TIMEOUT_US = TimeUnit.MILLISECONDS.toMicros(0);
    private static final long DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US = TimeUnit.MILLISECONDS.toMicros(0);

    public static final String MIME = "video/avc";

    private int videoWidth = Constants.UNKNOWN_VALUE, videoHeight = Constants.UNKNOWN_VALUE;
    private float videoFrameRate = Constants.UNKNOWN_VALUE;

    private volatile MediaCodec.BufferInfo bufferInfo;
    private volatile MediaCodec codec;
    private volatile MediaFormat format;
    private volatile Csd csd;
    private volatile Surface output;
    private volatile boolean released = false;

    //Used to calculate presentationTimeUs
    private long frameIndex = 0;

    private volatile boolean wasValidSurface = false;
    private volatile boolean waitForKeyFrame = true;

    private volatile boolean ppsReceived, spsReceived = false;

    @NonNull
    private final FrameBuilder frameBuilder;

    private final MediaCodec.OnFrameRenderedListener onFrameRenderedListener =
            (codec, presentationTimeUs, nanoTime) -> {

            };

    /**
     * Creates a new media decoder for the specified video track.
     *
     * @param track The track that the decoder decodes. Cannot be null.
     * @throws UnsupportedSampleFormatException If the specified sample format is not supported.
     */

    public H264Decoder(@NonNull VideoTrack track, int sampleFormat, int maxEncodedFrameSize) throws UnsupportedSampleFormatException {
        super(track, sampleFormat, maxEncodedFrameSize);
        if (sampleFormat == MediaDecoder.FORMAT_RAW) {
            frameBuilder = FrameBuilder.RAW_FRAME_BUILDER;
        } else if (sampleFormat == MediaDecoder.FORMAT_RTP) {
            frameBuilder = new RtpH264FrameBuilder(maxEncodedFrameSize);
        } else {
            throw new UnsupportedSampleFormatException("Unsupported sample format: " + sampleFormat);
        }
    }

    @Override
    public int getVideoWidth() {
        return videoWidth;
    }

    @Override
    public int getVideoHeight() {
        return videoHeight;
    }

    @Override
    public float getVideoFrameRate() {
        return videoFrameRate;
    }

    /**
     * Creates a new media decoder for the specified video track.
     *
     * @param track        The track that the decoder decodes. Cannot be null.
     * @param sampleFormat The sample format.
     * @throws UnsupportedSampleFormatException If the specified sample format is not supported.
     */

    public H264Decoder(@NonNull VideoTrack track, int sampleFormat) throws UnsupportedSampleFormatException {
        this(track, sampleFormat, FrameBuilder.DEFAULT_MAX_FRAME_SIZE);
    }

    @Override
    public void setCsd(@Nullable Csd csd) throws IllegalStateException {
        this.csd = csd;
    }

    @Override
    public void setOutput(Surface output) throws IllegalStateException, IllegalArgumentException {
        this.output = output;
        checkReleased();

        MediaCodec codec = this.codec;
        if (codec != null) {
            codec.setOutputSurface(output);
        }
    }

    @Nullable
    @Override
    public synchronized Frame feed(@NonNull Sample sample) throws IllegalStateException, UnsupportedSampleException, NullPointerException {
        checkReleased();
        return frameBuilder.pull(sample);
    }

    @Override
    public synchronized int decode(@NonNull Frame frame)
            throws IllegalStateException, UnsupportedSampleException, MediaDecoderException, NullPointerException {
        checkReleased();

        try {
            MediaCodec codec = ensureMediaCodec();
            Surface output = this.output;

            if (frame.frameType == Frame.CONFIG_FRAME) {
                byte nalUnitType = H264Util.getNalUnitType(frame.getArray(), frame.getOffset(), frame.getLength());
                if (nalUnitType == H264Util.NAL_UNIT_TYPE_SPS) {
                    spsReceived = true;
                    obtainVideoParamsFromSPS(frame.getArray(), frame.getOffset(), frame.getLength());
                } else if (nalUnitType == H264Util.NAL_UNIT_TYPE_PPS) {
                    ppsReceived = true;
                }
            } else if (!spsReceived || !ppsReceived) {
                //Codec not configured yet
                return MediaDecoder.ACTION_NOT_CONFIGURED;
            } else if(waitForKeyFrame){
                if(frame.frameType == Frame.SYNC_FRAME){
                    codec.flush();
                    configCsd(format, codec, csd);
                    waitForKeyFrame = false;
                } else {
                    return MediaDecoder.ACTION_WAITING_FOR_KEY_FRAME;
                }
            } else if(output == null || !output.isValid()){
                waitForKeyFrame = true;
                flush();
                configCsd(format, codec, csd);
                return ACTION_INVALID_OUTPUT;
            }

            int inIndex, outIndex;

            inIndex = codec.dequeueInputBuffer(DEQUEUE_INPUT_TIMEOUT_US);
            if (inIndex >= 0) {

                ByteBuffer bb = codec.getInputBuffer(inIndex);

                if (bb != null) {
                    bb.rewind();

                    //Write nal prefix if needed
                    if (H264Util.startsWithNalPrefix(frame.getArray(), frame.getOffset(), frame.getLength()) == 0) {
                        bb.put(Constants.H264_NAL_PREFIX);
                    }

                    //Write all buffered samples
                    bb.put(frame.getArray(), frame.getOffset(), frame.getLength());

                    codec.queueInputBuffer(inIndex, 0, frame.getLength(), frameIndex++, 0);
                }
            }

            outIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US);
            boolean rendered = false;

            switch (outIndex) {
                default:
                    rendered = renderBufferOnSurface(outIndex);
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    break;
            }

            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0) {
                if(rendered){
                    return 100 + frame.frameType;
                } else {
                    if(frame.frameType == Frame.CONFIG_FRAME){
                        return ACTION_CONFIGURED;
                    }
                }
            } else {
                inIndex = codec.dequeueInputBuffer(DEQUEUE_INPUT_TIMEOUT_US);

                if (inIndex >= 0) {
                    codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }

                return MediaDecoder.ACTION_END_OF_STREAM_REACHED;
            }

        } catch (Exception e) {
            if (!isReleased()) {
                destroyMediaCodec();
                throw new MediaDecoderException(e);
            }
        } finally {
            if (isReleased()) {
                destroyMediaCodec();
            }
        }
        return MediaDecoder.ACTION_NONE;
    }

    private boolean renderBufferOnSurface(int outIndex){
        if (outIndex >= 0) {
            try {
                boolean doRender = bufferInfo.size != 0
                        && !isReleased()
                        && output != null && output.isValid();

                codec.releaseOutputBuffer(outIndex, doRender);
                return doRender;
            } catch (IllegalStateException e) {
                //when surface becomes invalid after check, this may happen
                //release without rendering
                codec.releaseOutputBuffer(outIndex, false);
            }
        }
        return false;
    }

    @Override
    public void flush() {
        MediaCodec codec = this.codec;
        if (codec != null) {
            flushCodecQuietly();
        }
    }

    @Override
    public void release() {
        MediaCodec codec = this.codec;
        if (codec != null) {
            destroyMediaCodec();
            this.codec = null;
        }
        released = true;
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    @NonNull
    private MediaCodec initCodec() throws IOException {
        MediaCodec codec = this.codec;
        Csd csd = this.csd;

        if (codec == null) {
            Bundle params = new Bundle();
            codec = MediaCodec.createDecoderByType(MIME);
            Size safeSize = getDecoderSafeWidthHeight(codec);
            format = MediaFormat.createVideoFormat(MIME, safeSize.width, safeSize.height);
            bufferInfo = new MediaCodec.BufferInfo();

            codec.setOnFrameRenderedListener(onFrameRenderedListener, null);

            //Set low latency mode if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                params.putInt(MediaCodec.PARAMETER_KEY_LOW_LATENCY, 1);
            }
            configCsd(format, codec, csd);
            codec.configure(format, output, null, 0);

            codec.setParameters(params);
            codec.start();

            this.codec = codec;
        }
        return codec;
    }

    private void configCsd(MediaFormat format, MediaCodec codec, Csd csd) {

        if(format == null || codec == null){
            return;
        }

        if (csd != null) {
            for (int i = 0; i < csd.getCapacity(); i++) {
                if (csd.hasCsd(i)) {
                    format.setByteBuffer("csd-" + i, ByteBuffer.wrap(H264Util.ensureStartsWithNalPrefix(csd.getCsd(i))));
                    if (i == 0) {
                        ppsReceived = true;
                    } else if(i == 1) {
                        byte[] csd0 = csd.getCsd(i);
                        obtainVideoParamsFromSPS(csd0, 0, csd0.length);
                        spsReceived = true;
                    }
                }
            }
        }
    }

    @NonNull
    private MediaCodec ensureMediaCodec() throws IOException {
        return initCodec();
    }

    private void destroyMediaCodec() {
        MediaCodec codec = this.codec;
        if (codec != null) {
            stopCodecQuietly();
            codec.release();
            this.codec = null;
        }
    }

    private void stopCodecQuietly() {
        try {
            codec.stop();
        } catch (Exception ignored) {
        }
    }

    private void flushCodecQuietly() {
        try {
            waitForKeyFrame = true;
            codec.flush();
        } catch (Exception ignored) {
        }
    }

    private Size getDecoderSafeWidthHeight(MediaCodec decoder) {

        MediaCodecInfo.VideoCapabilities c =
                decoder.getCodecInfo()
                        .getCapabilitiesForType(MIME)
                        .getVideoCapabilities();

        Size size;
        if (c.isSizeSupported(track.getWidth(), track.getHeight())) {
            size = new Size(track.getWidth(), track.getHeight());
        } else {
            int wa = c.getWidthAlignment();
            int ha = c.getHeightAlignment();
            size = new Size(
                    ceilDivide(track.getWidth(), wa) * wa,
                    ceilDivide(track.getHeight(), ha) * ha
            );
        }

        return size;
    }

    /**
     * Divides a {@code numerator} by a {@code denominator}, returning the ceiled result.
     *
     * @param numerator   The numerator to divide.
     * @param denominator The denominator to divide by.
     * @return The ceiled result of the division.
     */
    public static int ceilDivide(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    private void obtainVideoParamsFromSPS(byte[] sps, int off, int len){
        if (sps == null || len < 4) {
            return;
        }

        try {
            SPSParser.VideoParams videoParams =
                    SPSParser.parseSPSStatic(sps, H264Util.startsWithNalPrefix(sps, off, len));

            if (videoParams != null) {
                videoWidth = videoParams.width;
                videoHeight = videoParams.height;
                videoFrameRate = videoParams.frameRate;
                Log.println(Log.ASSERT, "H264Decoder", "obtainVideoParamsFromPPS: " + videoParams);
            }

        } catch (Exception e) {
            //ignore
            //Log.println(Log.ASSERT, "H264Decoder", "obtainVideoParamsFromPPS: " + e);
        }

    }
}
