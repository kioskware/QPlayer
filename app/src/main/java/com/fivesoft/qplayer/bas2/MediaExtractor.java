package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.track.Tracks;

import java.io.IOException;
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
        implements AutoCloseable, Timeoutable {

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
     * @throws IOException if any I/O error occurs while preparing.
     * @throws TimeoutException if the timeout is reached while preparing.
     * @throws InterruptedException if the thread is interrupted while preparing.
     */

    public abstract void prepare(int timeout) throws IOException, TimeoutException, InterruptedException;

    /**
     * Seeks to the specified time in milliseconds.<br>
     * The passed value will be rounded to the nearest key frame.<br>
     * @param timeUs the time in milliseconds to seek to.
     * @param flags (optional) for specific extractor implementation.
     * @throws IOException if any I/O error occurs while seeking.
     * @throws TimeoutException if the timeout is reached while seeking.
     * @throws UnsupportedOperationException if the extractor does not support seeking.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws IllegalArgumentException if the passed time is negative.
     * @throws InterruptedException if the thread is interrupted while seeking.
     */

    public abstract void seekToTime(long timeUs, int flags)
            throws IOException, TimeoutException, UnsupportedOperationException, InterruptedException,
            IllegalStateException, IllegalArgumentException;

    /**
     * Seeks to the specified time in milliseconds.<br>
     * The passed value will be rounded to the nearest key frame.<br>
     * @param timeUs the time in milliseconds to seek to.
     * @throws IOException if any I/O error occurs while seeking.
     * @throws TimeoutException if the timeout is reached while seeking.
     * @throws UnsupportedOperationException if the extractor does not support seeking.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws IllegalArgumentException if the passed time is negative.
     * @throws InterruptedException if the thread is interrupted while seeking.
     */

    public final void seekToTime(long timeUs)
            throws IOException, TimeoutException, UnsupportedOperationException, InterruptedException,
            IllegalStateException, IllegalArgumentException {
        seekToTime(timeUs, 0);
    }

    /**
     * Seeks to the specified sample index.<br>
     * In contrast to {@link #seekToTime(long, int)} this method
     * does not round the passed value to the nearest key frame.<br>
     * @param sampleIndex the sample index to seek to.
     * @param flags (optional) for specific extractor implementation.
     * @throws IOException if any I/O error occurs while seeking.
     * @throws TimeoutException if the timeout is reached while seeking.
     * @throws UnsupportedOperationException if the extractor does not support seeking.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws IllegalArgumentException if the passed sample index is negative.
     * @throws IndexOutOfBoundsException if the passed sample index is greater than the number of samples in the data source.
     * @throws InterruptedException if the thread is interrupted while seeking.
     */

    public abstract void seekToSample(long sampleIndex, int flags)
            throws IOException, TimeoutException, UnsupportedOperationException, InterruptedException,
            IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException;

    /**
     * Seeks to the specified sample index.<br>
     * In contrast to {@link #seekToTime(long, int)} this method
     * does not round the passed value to the nearest key frame.<br>
     * @param sampleIndex the sample index to seek to.
     * @throws IOException if any I/O error occurs while seeking.
     * @throws TimeoutException if the timeout is reached while seeking.
     * @throws UnsupportedOperationException if the extractor does not support seeking.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws IllegalArgumentException if the passed sample index is negative.
     * @throws IndexOutOfBoundsException if the passed sample index is greater than the number of samples in the data source.
     * @throws InterruptedException if the thread is interrupted while seeking.
     */

    public final void seekToSample(long sampleIndex)
            throws IOException, TimeoutException, UnsupportedOperationException, InterruptedException,
            IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        seekToSample(sampleIndex, 0);
    }

    /**
     * Reads next sample from the data source.<br>
     * @return The next sample or null if the end of the data source is reached.
     * @throws IOException if any I/O error occurs while reading the sample.
     * @throws TimeoutException if the timeout is reached while reading the sample.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws InterruptedException if the thread is interrupted while reading the sample.
     */
    public abstract Sample nextSample() throws IOException, TimeoutException,
            IllegalStateException, InterruptedException;

    /**
     * Returns tracks collection available in the data source.<br>
     * This should return the same reference for whole lifetime of the extractor.<br>
     * @return Tracks collection of the data source.
     * @throws IllegalStateException If the extractor is not prepared yet or it is closed.
     * @throws RuntimeException if other error occurs.
     */

    @NonNull
    public abstract Tracks getTracks() throws IllegalStateException, RuntimeException;

    /**
     * Gets the duration of the data source in milliseconds.<br>
     * @return The duration of the data source in milliseconds or -1
     * if the duration is unknown (most common for live streams).
     */
    public abstract long getDuration() throws IllegalStateException;

    /**
     * Gets the current position in milliseconds.<br>
     * @return The current position in milliseconds or -1 if the
     * position is unknown.
     */

    public abstract long getPosition() throws IllegalStateException;

    /**
     * Gets the number of samples in the data source.<br>
     * @return The number of samples in the data source or -1
     * if the number of samples is unknown (most common for live streams).
     */

    public abstract long getSampleCount() throws IllegalStateException;

    /**
     * Gets the current sample index.<br>
     * @return The current sample index or -1 if the index is unknown.
     */

    public abstract long getSampleIndex() throws IllegalStateException;

    /**
     * Releases all resources associated with this extractor.<br>
     * This does not close the data source associated with this extractor.<br>
     * @throws IOException if any I/O error occurs while closing the extractor.
     */

    @Override
    public abstract void close() throws IOException;

    /**
     * Sets the authentication for the data source.<br>
     * Used for data sources that require authentication. (e.g. HTTP Basic Authentication)<br>
     * @param authentication The authentication for the data source or null
     *                       if no authentication is needed.
     */

    public abstract void setAuthentication(@Nullable Authentication authentication);

    /**
     * Sets the timeout value for I/O operations.<br>
     * @param timeout The timeout value in milliseconds. Non-positive value means no timeout. (waiting forever)
     */

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

    /**
     * Gets the timeout value for I/O operations. (set by {@link #setTimeout(int)})<br>
     * @return The timeout value in milliseconds. Non-positive value means no timeout. (waiting forever)
     */

    @Override
    public int getTimeout() {
        return timeout;
    }

    /**
     * Gets data source associated with this extractor.<br>
     * @return The data source associated with this extractor.
     */

    @NonNull
    public DataSource getDataSource() {
        return dataSource;
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
