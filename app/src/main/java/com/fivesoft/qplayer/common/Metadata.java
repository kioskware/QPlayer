package com.fivesoft.qplayer.common;

import java.util.HashMap;

public class Metadata {

    private final HashMap<String, Object> metadata = new HashMap<>();

    public void setMetadata(String key, Object value) {
        synchronized (metadata) {
            metadata.put(key, value);
        }
    }

    public Object getMetadata(String key) {
        synchronized (metadata) {
            return metadata.get(key);
        }
    }

    public <T>T getMetadataOrDefault(String key, T defaultValue) {
        synchronized (metadata) {
            Object value = metadata.get(key);
            if (value == null) {
                return defaultValue;
            }
            //noinspection unchecked
            return (T) value;
        }
    }

}
