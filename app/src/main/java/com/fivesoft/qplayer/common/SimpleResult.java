package com.fivesoft.qplayer.common;

public class SimpleResult {

    public final int code;
    public final String message;

    public SimpleResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return code == Flow.SUCCESS;
    }

}
