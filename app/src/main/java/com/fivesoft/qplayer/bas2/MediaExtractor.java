package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.sql.Time;
import java.util.Iterator;
import java.util.Objects;

/**
 * MediaExtractors are used to extract media samples from a {@link DataSource}.
 * For instance it may be extracting H264 nal units from a mp4 file.<br>
 * <p>
 *     Each extractor is associated with a {@link DataSource} and can only be used with that
 *     {@link DataSource}.<br>
 * </p>
 * <p>
 *     Extractors don't need to be thread safe and they are not responsible for connecting/closing DataSource.
 * </p>
 */

public abstract class MediaExtractor
        implements AutoCloseable, Iterable<Sample>, Timeoutable {

    protected final DataSource dataSource;
    private int timeout = 15000;

    /**
     * Extractor setting, indicates whether to extract audio samples or not.<br>
     * The setting can be set only in the constructor and cannot be changed later.<br>
     * Setting to true does not guarantee that the extractor will extract audio samples,
     * because audio samples may not be present in the data source.<br>
     */
    public final boolean extractAudio;

    /**
     * Extractor setting, indicates whether to extract video samples or not.<br>
     * The setting can be set only in the constructor and cannot be changed later.<br>
     * Setting to true does not guarantee that the extractor will extract video samples,
     * because video samples may not be present in the data source.<br>
     */
    public final boolean extractVideo;

    /**
     * Extractor setting, indicates whether to extract subtitle samples or not.<br>
     * The setting can be set only in the constructor and cannot be changed later.<br>
     * Setting to true does not guarantee that the extractor will extract subtitle samples,
     * because subtitle samples may not be present in the data source.<br>
     */
    public final boolean extractSubtitle;

    /**
     * Extractor setting, indicates whether to extract non-standard tracks or not.<br>
     * The setting can be set only in the constructor and cannot be changed later.<br>
     * <br>
     * Used only by non-standard extractors, supporting non-standard tracks. (other than audio, video and subtitle tracks)<br>
     */

    public final int extractFlags;

    /**
     * Creates a new extractor for the specified data source.<br>
     * @param dataSource The data source to extract samples from.
     * @param audio Whether to extract audio samples or not.
     * @param video Whether to extract video samples or not.
     * @param subtitle Whether to extract subtitle samples or not.
     * @param flags (optional) Flags to be passed to the extractor, if data source contain non-standard tracks.
     *              (other than audio, video and subtitle tracks)
     * @throws NullPointerException if the data source is null.
     */

    public MediaExtractor(@NonNull DataSource dataSource, boolean audio, boolean video, boolean subtitle, int flags){
        this.dataSource = Objects.requireNonNull(dataSource);
        this.extractAudio = audio;
        this.extractVideo = video;
        this.extractSubtitle = subtitle;
        this.extractFlags = flags;
    }

    /**
     * Creates a new extractor for the specified data source.<br>
     * @param dataSource The data source to extract samples from.
     * @param audio Whether to extract audio samples or not.
     * @param video Whether to extract video samples or not.
     * @param subtitle Whether to extract subtitle samples or not.
     * @throws NullPointerException if the data source is null.
     */

    public MediaExtractor(@NonNull DataSource dataSource, boolean audio, boolean video, boolean subtitle){
        this(dataSource, audio, video, subtitle, 0);
    }

    /**
     * Prepares the extractor for reading samples.<br>
     */

    public abstract void prepare(int timeout) throws IOException, TimeoutException;

    /**
     * Returns the total number of samples available in the data source.<br>
     * @return The number of samples available in the data source or -1 if the number of samples is unknown.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws RuntimeException if other error occurs.
     */

    public abstract long getSampleCount() throws IllegalStateException;

    /**
     * Returns the sample at the specified index.<br>
     * @param index The index of the sample to return. (index starts from 0)
     * @return The sample at the specified index.
     * @throws IOException if any I/O error occurs while reading the sample.
     * @throws TimeoutException if the operation times out.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws IllegalArgumentException if the index is negative.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     */

    @NonNull
    public abstract Sample getSampleAt(long index) throws IOException, TimeoutException;

    /**
     * Returns tracks collection available in the data source.<br>
     * This should return the same reference for whole lifetime of the extractor.<br>
     * @return Tracks collection of the data source.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws RuntimeException if other error occurs.
     */

    @NonNull
    public abstract Tracks getTracks();

    @Override
    public abstract void close() throws IOException;

    @Override
    public void setTimeout(int timeout) {
        if(this.timeout == timeout)
            return;

        this.timeout = timeout;
        try {
            onTimeoutSet(Math.max(timeout, 0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @NonNull
    @Override
    public Iterator<Sample> iterator() {
        return new Iterator<Sample>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Sample next() {
                return null;
            }
        };
    }

    /**
     * Called when timeout is set by {@link #setTimeout(int)}.<br>
     * This is called after the timeout is set and before the timeout is applied to the data source, so
     * it will be: <code>getTimeout() == timeout</code><br>
     * The implementation should apply the timeout to the media extractor.
     * @param timeout The timeout in milliseconds or 0 if no timeout. (waiting forever)
     *                API will care to provide only non-negative values.
     * @throws IOException If an I/O error occurs while setting the timeout.
     */

    protected abstract void onTimeoutSet(int timeout) throws IOException;
}
