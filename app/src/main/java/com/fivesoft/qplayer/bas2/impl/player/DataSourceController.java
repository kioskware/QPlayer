package com.fivesoft.qplayer.bas2.impl.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.DataSource;
import com.fivesoft.qplayer.bas2.core.resolvers.DataSourceResolver;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * DataSourceController is a class that manages data source. It can be used as module of player
 * to manage {@link DataSource}s. (connect, close, change)
 */

public abstract class DataSourceController {

    private final Object dataSourceLock = new Object();

    private volatile URI uri;
    private volatile DataSource dataSource;

    @NonNull
    private final DataSourceChangedListener listener;

    public DataSourceController(@NonNull DataSourceChangedListener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    /**
     * Checks if data source matches passed uri and is ready to use.<br>
     * If there is no data source set or it doesn't match passed uri, new one will be created and connected.<br>
     * Old one (if exists) will be closed.<br>
     * Result of this method will be returned by {@link DataSourceChangedListener} set in constructor,
     * but only if data source state changed.<br>
     *
     * @param uri               uri to set as data source, null to disconnect data source.
     * @param timeout           timeout for data source read operations.
     * @param connectionTimeout timeout for data source connection.
     */

    public void ensureDataSource(@Nullable URI uri, int timeout, int connectionTimeout) {
        synchronized (dataSourceLock) {
            DataSource res;
            if (!Objects.equals(this.uri, uri) || (dataSource == null && uri != null)) {
                this.uri = uri;
                destroyCurrentDataSource();
                if (uri == null) {
                    res = null;
                } else {
                    res = DataSourceResolver.resolveSource(uri);
                }
            } else {
                res = dataSource;
            }

            //Connect data source if it is not connected
            //and set timeout if not null
            if (dataSource != null) {
                dataSource.setTimeout(timeout);
                if (!dataSource.isConnected()) {
                    try {
                        dataSource.connect(Math.max(1, connectionTimeout));
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }

            //Call onDataSourceChanged if data source changed
            if (res != this.dataSource) {
                dataSource = res; //Set new data source
                listener.onDataSourceChanged(dataSource, uri);
            }
        }
    }

    /**
     * Disconnects data source and closes it.<br>
     * This is equivalent to calling {@link #ensureDataSource(URI, int, int)} with null uri.
     */

    public void destroyDataSource() {
        ensureDataSource(null, 10, 10);
    }

    //Internal methods

    private void destroyCurrentDataSource() {
        synchronized (dataSourceLock) {
            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (IOException e) {
                    //Ignore
                }
                dataSource = null;
            }
        }
    }

}
