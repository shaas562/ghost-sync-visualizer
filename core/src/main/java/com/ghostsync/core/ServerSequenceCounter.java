package com.ghostsync.core;

/**
 * Generates monotonically increasing ids for accepted server-authoritative
 * observations within one connection. Create a new counter on reconnect.
 */
public final class ServerSequenceCounter {
    private long value;

    public synchronized long next() {
        if (value == Long.MAX_VALUE) {
            throw new IllegalStateException("Server evidence sequence exhausted");
        }
        return ++value;
    }

    public synchronized long current() {
        return value;
    }
}
