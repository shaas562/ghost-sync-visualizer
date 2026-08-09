package com.ghostsync.core;

/**
 * Internal certainty state. Only CONFIRMED_GHOST is allowed to affect rendering.
 */
public enum SyncState {
    UNKNOWN,
    MATCHED,
    PENDING,
    CONFIRMED_GHOST
}
