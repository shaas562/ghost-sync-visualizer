package com.ghostsync.core;

import java.util.Objects;

/**
 * One server-authoritative observation. The adapter assigns a strictly increasing
 * sequence for each relevant server update it accepts.
 */
public record AuthoritativeEvidence<K>(K key, Presence presence, long serverSequence) {
    public AuthoritativeEvidence {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");
        if (serverSequence < 0) {
            throw new IllegalArgumentException("serverSequence must be >= 0");
        }
    }
}
