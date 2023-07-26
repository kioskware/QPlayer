package com.fivesoft.qplayer.bas2.decoder;

public interface BufferReceiver {

    boolean onBufferReceived(long timestamp, byte[] buffer, int offset, int length);

}
