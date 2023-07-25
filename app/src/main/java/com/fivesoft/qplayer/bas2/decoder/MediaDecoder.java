package com.fivesoft.qplayer.bas2.decoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.Csd;
import com.fivesoft.qplayer.bas2.Frame;
import com.fivesoft.qplayer.bas2.MediaDecoderException;
import com.fivesoft.qplayer.bas2.Sample;
import com.fivesoft.qplayer.bas2.UnsupportedSampleException;
import com.fivesoft.qplayer.track.Track;

import java.util.Objects;

/**
 * Media decoders process encoded media samples and play them.<br>
 * The process consists of 3 steps:<br>
 * <ol>
 *     <li>Feed the decoder with a sample using {@link #feed(Sample)} method.</li>
 *     <li>Decode the sample using {@link #decode(Frame)} method.</li>
 *     <li>Play the decoded frame on output.</li>
 * </ol>
 * <p>
 *     Media decoders should generally be thread-safe and methods
 *     {@link #feed(Sample)} and {@link #decode(Frame)}
 *     should work independently from each other.
 * </p>
 *
 * @param <OutputType> Type of the output where frame should be played.
 *                     (e.g. {@link android.view.Surface Surface} for video decoder)
 * @param <TrackType>  Type of the track that the decoder decodes.
 */

public abstract class MediaDecoder<TrackType extends Track, OutputType> {

    public static final int ACTION_DROP_FRAME_UNRECOGNIZED = -1;

    public static final int ACTION_WAITING_FOR_KEY_FRAME = 0;
    public static final int ACTION_NOT_CONFIGURED = 1;
    public static final int ACTION_INVALID_OUTPUT = 1;

    public static final int ACTION_RENDER_SYNC_FRAME = 100 + Frame.SYNC_FRAME;
    public static final int ACTION_RENDER_NON_SYNC_FRAME = 100 + Frame.NON_SYNC_FRAME;
    public static final int ACTION_RENDER_UNKNOWN_FRAME = 100 + Frame.UNKNOWN_FRAME;
    public static final int ACTION_CONFIGURED = 100 + Frame.CONFIG_FRAME;

    public static final int ACTION_END_OF_STREAM_REACHED = 5;
    public static final int ACTION_NONE = 6;

    public static final int FORMAT_RAW = 0;
    public static final int FORMAT_ANNEX_B = 10;
    public static final int FORMAT_AVCC = 20;

    public static final int FORMAT_RTP = 100;

    /**
     * The decoder should update the track with the new information when available.<br>
     * For ex. when video width and height is extracted from the SPS.
     */

    protected final TrackType track;
    protected final int sampleFormat;

    protected final int maxEncodedFrameSize;

    /**
     * Creates a new media decoder for the specified track.
     *
     * @param track        The track that the decoder decodes. Cannot be null.
     * @param sampleFormat format of the samples passed to {@link #feed(Sample)} method.<br>
     *                     Typically one of:
     *                     <ul>
     *                     <li>{@link #FORMAT_RAW}</li>
     *                     <li>{@link #FORMAT_RTP}</li>
     *                     <li>{@link #FORMAT_ANNEX_B}</li>
     *                     <li>{@link #FORMAT_AVCC}</li>
     *                     </ul>
     * @param maxEncodedFrameSize Maximum size of the encoded frame.
     * @throws UnsupportedSampleFormatException If the sample format is not supported by the decoder.
     */
    public MediaDecoder(@NonNull TrackType track, int sampleFormat,
                        int maxEncodedFrameSize) throws UnsupportedSampleFormatException {
        this.track = Objects.requireNonNull(track);
        this.sampleFormat = sampleFormat;
        this.maxEncodedFrameSize = maxEncodedFrameSize;
    }

    /**
     * Returns the track associated with this decoder.
     *
     * @return The track associated with this decoder.
     */
    @NonNull
    public final TrackType getTrack() {
        return track;
    }

    /**
     * Returns the format of the samples passed to {@link #feed(Sample)} method.<br>
     * Typically one of:
     * <ul>
     *     <li>{@link #FORMAT_RAW}</li>
     *     <li>{@link #FORMAT_RTP}</li>
     *     <li>{@link #FORMAT_ANNEX_B}</li>
     *     <li>{@link #FORMAT_AVCC}</li>
     * </ul>
     * Sample type is set in the constructor and cannot be changed.
     *
     * @return The format of the samples passed to {@link #feed(Sample)} method.
     */

    public final int getSampleFormat() {
        return sampleFormat;
    }

    /**
     * Returns the maximum size of the encoded frame.<br>
     * THe value is set in the constructor and cannot be changed later.
     * @return The maximum size of the encoded frame. (in bytes)
     */

    public final int getMaxEncodedFrameSize() {
        return maxEncodedFrameSize;
    }

    /**
     * Sets the codec specific data of the decoder. (e.g. SPS and PPS for H.264 decoder)<br>
     * This method may be called at any time.<br>
     * THis may be used to speed up the decoding process if the CSD is known before the first sample is fed.
     *
     * @param csd Codec specific data of the decoder.
     * @throws IllegalStateException If the decoder is released.
     */
    public abstract void setCsd(@Nullable Csd csd) throws IllegalStateException;

    /**
     * Output where the decoded frame should be played.
     *
     * @param output Output where the decoded frame should be played.
     * @throws IllegalStateException    If the decoder is released.
     * @throws IllegalArgumentException If the output is not supported by the decoder or is wrong.
     */

    public abstract void setOutput(OutputType output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Feeds decoder with a media sample.
     * If buffered data is enough to decode a frame, the encoded frame data will be returned,
     * otherwise null will be returned.
     * <p>
     * This method works independently from {@link #decode(Frame)} method,
     * so it can be called from another thread. (It's also recommended to do so)
     * </p>
     * <p>
     * If the sample does not contain any data, it will be ignored.
     * </p>
     * <p>
     * If the sample contains data, but the decoder does not support it, exception will be thrown.
     * </p>
     * <p>
     * If the sample contains data, but it doesn't match currently buffered data, exception will be thrown.
     * </p>
     *
     * @param sample Sample to feed the decoder with. Cannot be null.
     * @return Full encoded frame data if the frame is ready to be decoded, otherwise null.
     * @throws IllegalStateException      If the decoder is released.
     * @throws UnsupportedSampleException If the sample is not supported by the decoder or is corrupted.
     * @throws NullPointerException       If the sample is null.
     */

    @Nullable
    public abstract Frame feed(@NonNull Sample sample) throws IllegalStateException,
            UnsupportedSampleException, NullPointerException;

    /**
     * Decodes the media sample and plays the decoded frame on output.<br>
     * The frame playback may be asynchronous, so the method may return before the frame is played.
     *
     * @param sample Sample to decode, returned by {@link #feed(Sample)} method. Cannot be null.
     * @throws IllegalStateException      If the decoder is released.
     * @throws UnsupportedSampleException If the sample is not supported by the decoder or is corrupted.
     * @throws MediaDecoderException      If an error occurred while decoding the sample.
     * @throws NullPointerException       If the sample is null.
     */

    public abstract int decode(@NonNull Frame sample)
            throws IllegalStateException, UnsupportedSampleException,
            MediaDecoderException, NullPointerException;

    /**
     * Flushes the decoder. All buffered data will be discarded.
     */

    public abstract void flush();

    /**
     * Releases all resources used by the decoder.<br>
     * After calling this method, the decoder cannot be used anymore.<br>
     * Use this method to don't relay only on garbage collector.
     */

    public abstract void release();

    /**
     * Returns true if the decoder is released.
     *
     * @return True if the decoder is released.
     */
    public abstract boolean isReleased();

    /**
     * Convenience method for subclasses to ensure that the decoder is not released.
     *
     * @throws IllegalStateException If the decoder is released.
     */

    protected void checkReleased() throws IllegalStateException {
        if (isReleased())
            throw new IllegalStateException("The decoder is released");
    }

    /**
     * Exception thrown while construction of the decoder, if sample format is not supported by the decoder.
     */
    public static class UnsupportedSampleFormatException extends RuntimeException {

        public UnsupportedSampleFormatException() {
            super();
        }

        public UnsupportedSampleFormatException(String message) {
            super(message);
        }

        public UnsupportedSampleFormatException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnsupportedSampleFormatException(Throwable cause) {
            super(cause);
        }

    }


}
