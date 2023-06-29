package com.fivesoft.qplayer.frame;

import com.fivesoft.qplayer.common.ErrorCallback;

public interface FrameExtractListener extends ErrorCallback {

    void onFrameAvailable(Frame frame);

}
