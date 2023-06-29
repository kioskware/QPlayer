package com.fivesoft.qplayer.common;

import androidx.annotation.Nullable;

public interface ConnectionListener {

    /**
     * Called when connection is about to be established.<br>
     */

    default void onConnectingStart() {}

    /**
     * Called when connection is established or failed.<br>
     * In a contrast to onDisconnected(), this method is called only when the connection haven't
     * been established yet or has been just established.
     * @param code The code of the result.
     * @param message The message of the result.
     */

    default void onConnectingEnd(int code, @Nullable String message) {}

    /**
     * Called when data is ready to be send.
     * @param data The data to be send.
     * @param offset The offset of the data.
     * @param length The length of the data.
     * @return The number of bytes sent or -1 if error occurred.
     */


    default void onDisconnected(int code, @Nullable String message) {}

}
