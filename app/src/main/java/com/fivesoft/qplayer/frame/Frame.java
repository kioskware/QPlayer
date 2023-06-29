package com.fivesoft.qplayer.frame;

import com.fivesoft.qplayer.buffer.Bufferable;
import com.fivesoft.qplayer.common.ByteArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class Frame implements Bufferable {

    @NotNull
    private final ByteArray data;
    private final long timestamp;

    @NotNull
    public ByteArray getData() {
        return data;
    }

    public byte[] getArray() {
        return data.getArray();
    }

    public int getOffset() {
        return data.getOffset();
    }

    public int getLength() {
        return data.getLength();
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public Frame(@NotNull ByteArray data, long timestamp) {
        this.data = data;
        this.timestamp = timestamp;
    }

    @NotNull
    public String toString() {
        return "Frame(data=" + Arrays.toString(getArray()) + ", offset=" + getOffset() + ", length=" + getLength() + ", timestamp=" + getTimestamp() + ")";
    }

    public int hashCode() {
        return ((Arrays.hashCode(getArray()) * 31 + Integer.hashCode(getOffset())) * 31 + Integer.hashCode(getLength())) * 31 + Long.hashCode(getLength());
    }

    public boolean equals(@Nullable Object var1) {
        if (this != var1) {
            if (var1 instanceof Frame) {
                Frame var2 = (Frame) var1;
                return Arrays.equals(getArray(), var2.getArray()) &&
                        getOffset() == var2.getOffset() && getLength() == var2.getLength() &&
                        this.timestamp == var2.timestamp;
            }

            return false;
        } else {
            return true;
        }
    }

}
