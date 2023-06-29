package com.fivesoft.qplayer.common;

import androidx.annotation.NonNull;

/**
 * Listener for receiving data.
 */
public interface DataListener {

    /**
     * Called when data is available after processing.
     *
     * @param data   The data available.
     */
    void onDataAvailable(@NonNull ReadOnlyByteArray data);

}