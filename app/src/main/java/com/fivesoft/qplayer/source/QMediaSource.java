package com.fivesoft.qplayer.source;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.common.Flow;
import com.fivesoft.qplayer.common.ListenerManager;
import com.fivesoft.qplayer.common.ReadOnlyByteArray;
import com.fivesoft.qplayer.common.Task;

import java.util.Objects;

/**
 * QMediaSource represents a source of media data.<br>
 * This may be anything from a file to a network stream.
 */

public abstract class QMediaSource extends ListenerManager<MediaSourceListener> implements Flow, Task {

    /*
    The last time data was sent or received. Internal use only.
     */
    private volatile long lastSent = Integer.MIN_VALUE,
            lastReturned = Integer.MIN_VALUE;

    /**
     * Indicates that the send message operation is not supported by this source.
     */
    public static final int ERROR_SEND_MESSAGE_UNSUPPORTED = -67000002;

    /**
     * Indicates that reopen operation is not supported by this source.
     */

    public static final int ERROR_REOPEN_UNSUPPORTED = -67000003;

    /**
     * Called when the source returns data.<br>
     * This may be overridden to handle the event internally.
     * @param data The data returned.
     */

    protected void onDataReturned(@NonNull ReadOnlyByteArray data){}

    /**
     * Called when message is send to the source.<br>
     * If supported, the implementation should be provided by the subclass,
     * otherwise {@link #ERROR_SEND_MESSAGE_UNSUPPORTED} should be returned.
     */

    protected abstract int onMessageSend(@NonNull byte[] data, int offset, int length);

    /**
     * Notifies the listeners that data is available. This should be single packet of data. (ex. rtsp packet)
     * @param data The data returned.
     */

    protected void returnData(@NonNull ByteArray data) {
        onDataReturned(data);
        lastReturned = SystemClock.elapsedRealtime();
        forEachListener((id, listener) -> listener.onDataAvailable(data));
    }

    /**
     * Notifies the listeners that the source is connecting.
     */

    protected final void notifyConnecting() {
        forEachListener((s, mediaSourceListener) -> mediaSourceListener.onConnecting());
    }

    /**
     * Notifies the listeners that the source is connected.
     */

    protected final void notifyConnected() {
        forEachListener((s, mediaSourceListener) -> mediaSourceListener.onConnected());
    }

    /**
     * Notifies the listeners that the source has disconnected.
     */

    protected final void notifyDisconnected() {
        forEachListener((s, mediaSourceListener) -> mediaSourceListener.onDisconnected());
    }

    /**
     * Notifies the listeners that an error has occurred.
     * @param e The error that occurred.
     */

    protected final void notifyError(@NonNull Exception e) {
        Objects.requireNonNull(e);
        forEachListener((s, mediaSourceListener) -> mediaSourceListener.onError(e));
    }

    /**
     * Notifies the listeners that the source has reached the end of data.
     */

    protected final void notifyEndOfData() {
        forEachListener((s, mediaSourceListener) -> mediaSourceListener.onEndOfData());
    }

    /**
     * Checks if the source supports sending messages.
     */

    public abstract boolean isSendMessageSupported();

    /**
     * Checks if media source is connected (if will return data when available).
     */

    public abstract boolean isConnected();

    /**
     * Checks if media source is connecting.
     */

    public abstract boolean isConnecting();

    /**
     * Reopens the source. If supported, this can be also done after closing the source.
     * @return 0 if successful or error code. (if not supported, {@link #ERROR_REOPEN_UNSUPPORTED} will be returned)
     */

    public abstract int reopen();

    /**
     * Sets media source read timeout. (in milliseconds)
     * @param timeout The timeout in milliseconds.
     */

    public abstract void setReadTimeout(int timeout);

    /**
     * Sets media source connection timeout. (in milliseconds)
     * @param timeout The timeout in milliseconds.
     */

    public abstract void setConnectTimeout(int timeout);

    /**
     * Sends message to the source. This may be useful for some transport protocols.<br>
     * This method may not be supported by all sources. If so, it will return {@link #ERROR_SEND_MESSAGE_UNSUPPORTED}.
     * @param data The data to send.
     * @param offset The offset of the data in the array.
     * @param length The length of the data.
     * @return 0 if successful or error code.
     */

    public int sendMessage(@NonNull byte[] data, int offset, int length){
        int result = onMessageSend(data, offset, length);
        if(result == 0)
            lastSent = SystemClock.elapsedRealtimeNanos();
        return result;
    }

    /**
     * Returns the time elapsed since the last data was sent.
     * @return The time elapsed since the last data was sent. (in nanoseconds)
     */

    public final long sinceLastSentNanos() {
        return SystemClock.elapsedRealtimeNanos() - lastSent;
    }

    /**
     * Returns the time elapsed since the last data was returned.
     * @return The time elapsed since the last data was returned. (in nanoseconds)
     */

    public final long sinceLastReturnNanos() {
        return SystemClock.elapsedRealtimeNanos() - lastReturned;
    }

    /**
     * Returns the time elapsed since the last data was sent.
     * @return The time elapsed since the last data was sent. (in milliseconds)
     */

    public final long sinceLastSent() {
        return SystemClock.elapsedRealtime() - lastSent / 1000000;
    }

    /**
     * Returns the time elapsed since the last data was returned.
     * @return The time elapsed since the last data was returned. (in milliseconds)
     */

    public final long sinceLastReturn() {
        return SystemClock.elapsedRealtime() - lastReturned / 1000000;
    }

}
