package com.fivesoft.qplayer.base;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.common.DataListener;
import com.fivesoft.qplayer.common.ListenerManager;

import java.util.Objects;

public abstract class QMediaExtractor
        extends ListenerManager<MediaExtractListener> implements DataListener {

    /**
     * This may be used to handle returned track data internally.
     * @param track The track info.
     * @param data The data returned.
     * @param offset The offset of the data in the array.
     * @param length The length of the data.
     */

    protected abstract void onMediaPacketReturned(@NonNull MediaPacket packet);

    /**
     * This method should be used by subclasses to return media data.<br>
     * This will notify the listeners that data is available.
     * @param packet The raw media data (payload) and metadata.
     */

    protected final void returnPacket(@NonNull MediaPacket packet) {
        onMediaPacketReturned(Objects.requireNonNull(packet));
        forEachListener((id, listener) -> listener.onTrackDataAvailable(packet));
    }

}
