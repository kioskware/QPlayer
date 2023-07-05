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
     */

    public abstract void setOutput(OutputType output);

    /**
     *
     * @param sample
     * @return
     */

    @Nullable
    public abstract Frame feed(@NonNull Sample sample);

    public abstract void decode(@NonNull Sample sample);

    public abstract void flush();

    public abstract void reset();

    public abstract void release();

}
