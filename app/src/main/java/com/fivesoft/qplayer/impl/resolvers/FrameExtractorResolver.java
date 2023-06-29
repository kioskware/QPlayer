package com.fivesoft.qplayer.impl.resolvers;

import com.fivesoft.qplayer.base.FrameExtractor;
import com.fivesoft.qplayer.resolver.Resolver;

public class FrameExtractorResolver extends Resolver<String, FrameExtractor> {

    private final static FrameExtractorResolver instance = new FrameExtractorResolver();

    private FrameExtractorResolver() {
    }

    public static FrameExtractorResolver getDefault() {
        return instance;
    }



}
