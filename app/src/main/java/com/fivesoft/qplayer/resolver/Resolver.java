package com.fivesoft.qplayer.resolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Resolver<D, R> {

    private final Set<Creator<D, R>> creators = new HashSet<>();

    public void register(@NonNull Creator<D, R> creator) {
        synchronized (creators) {
            creators.add(Objects.requireNonNull(creator));
        }
    }

    public void unregister(@NonNull Creator<D, R> candidate) {
        synchronized (creators) {
            creators.remove(Objects.requireNonNull(candidate));
        }
    }

    @Nullable
    public R resolve(@NonNull D descriptor) {
        Objects.requireNonNull(descriptor, "Descriptor cannot be null");
        synchronized (creators) {
            int maxPriority = Integer.MIN_VALUE;
            Creator<D, R> bestCreator = null;
            int creatorPriority;
            for (Creator<D, R> creator : creators) {
                creatorPriority = creator.accept(descriptor);
                if (creatorPriority > 0 && creatorPriority > maxPriority) {
                    maxPriority = creatorPriority;
                    bestCreator = creator;
                }
            }
            if (bestCreator != null) {
                return bestCreator.create(descriptor);
            }
            return null;
        }
    }

}
