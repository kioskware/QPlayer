package com.fivesoft.qplayer.bas2.impl.player;

import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.DataSource;
import com.fivesoft.qplayer.bas2.MediaExtractor;
import com.fivesoft.qplayer.bas2.TrackSelector;
import com.fivesoft.qplayer.bas2.core.resolvers.MediaExtractorResolver;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public class MediaExtractorController implements DataSourceChangedListener {

    private int prepareTimeout = 5000;

    private final Object mediaExtractorLock = new Object();

    private volatile MediaExtractor extractor;
    private volatile TrackSelector selector;

    private volatile URI uri;

    private volatile DataSource dataSource;

    @Override
    public void onDataSourceChanged(@Nullable DataSource dataSource, @Nullable URI uri) {
        ensureMediaExtractor(uri, dataSource, selector, prepareTimeout);
    }

    public void ensureMediaExtractor(@Nullable URI uri, @Nullable DataSource dataSource, @Nullable TrackSelector selector, int prepareTimeout) {
        synchronized (mediaExtractorLock) {
            boolean requestDestroy = uri == null || dataSource == null;
            if(requestDestroy){
                destroyCurrentMediaExtractor();
            } else {
                boolean changed = !Objects.equals(this.uri, uri) ||
                        !Objects.equals(this.dataSource, dataSource) ||
                        !Objects.equals(this.selector, selector);
                if(changed) {
                    destroyCurrentMediaExtractor();
                    MediaExtractor.Descriptor descriptor = new MediaExtractor.Descriptor(dataSource, selector, uri);
                    @SuppressWarnings("resource")
                    MediaExtractor res = MediaExtractorResolver.resolveExtractor(descriptor);
                    if(res == null) {
                        //Failed to resolve extractor
                    } else {
                        try {
                            res.prepare(prepareTimeout);
                        } catch (Exception e) {
                            return;
                        }
                        this.uri = uri;
                        this.dataSource = dataSource;
                        this.selector = selector;
                    }
                }
            }
        }
    }

    public void destroyMediaExtractor() {

    }

    private void destroyCurrentMediaExtractor() {
        synchronized (mediaExtractorLock) {
            if(extractor != null) {
                try {
                    extractor.close();
                } catch (IOException e) {
                    //Ignore
                }
                extractor = null;
                selector = null;
                uri = null;
                dataSource = null;
            }
        }
    }


}
