package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Media decoders process encoded media samples and play them.<br>
 * The process consists of 3 steps:<br>
 * <ol>
 *     <li>Feed the decoder with a sample using {@link #feed(Sample)} method.</li>
 *     <li>Decode the sample using {@link #decode(Sample)} method.</li>
 *     <li>Play the decoded frame on output.</li>
 * </ol>
 * @param <OutputType> Type of the output where frame should be played.
 *                    (e.g. {@link android.view.Surface Surface} for video decoder)
 */

public abstract class MediaDecoder<OutputType> {

    /**
     * Output where the decoded frame should be played.
     * @param output Output where the decoded frame should be played.
     * @throws IllegalStateException If the decoder is released.
     * @throws IllegalArgumentException If the output is not supported by the decoder or is wrong.
     */

    public abstract void setOutput(OutputType output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Feeds decoder with a media sample.
     * If buffered data is enough to decode a frame, the frame will be returned,
     * otherwise null will be returned.
     * <p>
     *     If the sample does not contain any data, it will be ignored.
     * </p>
     * <p>
     *     If the sample contains data, but the decoder does not support it, exception will be thrown.
     * </p>
     * <p>
     *     If the sample contains data, but it doesn't match currently buffered data, exception will be thrown.
     * </p>
     * @param sample Sample to feed the decoder with. Cannot be null.
     * @throws IllegalStateException If the decoder is released.
     * @return Decoded frame or null if there is not enough data to decode a frame.
     * @throws UnsupportedSampleException If the sample is not supported by the decoder or is corrupted.
     * @throws NullPointerException If the sample is null.
     */

    @Nullable
    public abstract Frame feed(@NonNull Sample sample) throws IllegalStateException,
            UnsupportedSampleException, NullPointerException;

    /**
     * Decodes the media sample and plays the decoded frame on output.
     * @param sample Sample to decode. Cannot be null.
     * @throws IllegalStateException If the decoder is released.
     * @throws UnsupportedSampleException If the sample is not supported by the decoder or is corrupted.
     * @throws MediaDecoderException If an error occurred while decoding the sample.
     * @throws NullPointerException If the sample is null.
     */

    public abstract void decode(@NonNull Sample sample)
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

}
