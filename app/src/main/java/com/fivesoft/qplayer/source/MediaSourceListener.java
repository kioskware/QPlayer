package com.fivesoft.qplayer.source;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.common.ErrorCallback;

public interface MediaSourceListener extends ErrorCallback {

    default void onConnecting() {}

    default void onConnected() {}

    void onDataAvailable(@NonNull ByteArray packet);

    default void onEndOfData() {}

    default void onDisconnected() {}

}
