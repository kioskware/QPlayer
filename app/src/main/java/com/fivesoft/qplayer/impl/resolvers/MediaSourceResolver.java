package com.fivesoft.qplayer.impl.resolvers;

import com.fivesoft.qplayer.resolver.Resolver;
import com.fivesoft.qplayer.source.QMediaSource;

public class MediaSourceResolver extends Resolver<String, QMediaSource> {

    private final static MediaSourceResolver instance = new MediaSourceResolver();

    private MediaSourceResolver() {
    }

    public static MediaSourceResolver getDefault() {
        return instance;
    }



}
