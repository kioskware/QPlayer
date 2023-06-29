package com.fivesoft.qplayer.base;

import com.fivesoft.qplayer.common.ListenerManager;
import com.fivesoft.qplayer.frame.FrameExtractListener;

public abstract class FrameExtractor extends ListenerManager<FrameExtractListener> {

    public abstract void onMediaPacketAvailable(MediaPacket packet);

}
