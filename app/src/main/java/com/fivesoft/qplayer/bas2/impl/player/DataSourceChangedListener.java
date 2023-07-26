package com.fivesoft.qplayer.bas2.impl.player;

import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.DataSource;

import java.net.URI;

public interface DataSourceChangedListener {

    /**
     * Called when data source changed.<br>
     * If non-null data source passed, it is connected and ready to use.<br>
     *
     * @param dataSource connected data source or null if data source is not available.
     */
    void onDataSourceChanged(@Nullable DataSource dataSource, @Nullable URI uri);

}