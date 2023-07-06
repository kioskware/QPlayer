package com.fivesoft.qplayer.bas2;

/**
 * Thrown when exception occurs during media decoding.
 * @see MediaDecoder
 */

public class MediaDecoderException extends Exception {

    public MediaDecoderException(String message) {
        super(message);
    }

    public MediaDecoderException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaDecoderException(Throwable cause) {
        super(cause);
    }

}
