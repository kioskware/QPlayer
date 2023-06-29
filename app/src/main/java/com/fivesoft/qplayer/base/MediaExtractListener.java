package com.fivesoft.qplayer.base;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.common.ErrorCallback;

public interface MediaExtractListener extends ErrorCallback {

    /**
     * Called when media packet is available.
     * @param packet The raw media data (payload) and metadata.
     */
    void onTrackDataAvailable(@NonNull MediaPacket packet);

}
