package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Deterministic randomized tests for the detector's most important safety rule:
 * client-local or stale evidence must never manufacture CONFIRMED_GHOST.
 */
class GhostTrackerInvariantFuzzTest {
    private static final String KEY = "slot";

    @Test
    void localOnlyRandomEventStreamsNeverConfirm() {
        for (int seed = 0; seed < 250; seed++) {
            GhostTracker<String> tracker = new GhostTracker<>();
            Random random = new Random(seed);

            for (int step = 0; step < 500; step++) {
                Presence presence = random.nextBoolean() ? Presence.PRESENT : Presence.ABSENT;
                switch (random.nextInt(3)) {
                    case 0 -> tracker.observeClientState(KEY, presence);
                    case 1 -> tracker.receiveClientMutation(KEY, presence);
                    case 2 -> tracker.markClientActionPending(KEY);
                    default -> throw new AssertionError();
                }

                assertNotEquals(
                        SyncState.CONFIRMED_GHOST,
                        tracker.getState(KEY),
                        "local-only evidence confirmed a ghost at seed=" + seed + " step=" + step);
            }
        }
    }

    @Test
    void staleAuthorityCannotReconfirmAfterAnyNewLocalMutation() {
        for (int seed = 0; seed < 250; seed++) {
            GhostTracker<String> tracker = new GhostTracker<>();
            Random random = new Random(seed);

            tracker.receiveAuthoritativeState(KEY, Presence.ABSENT, 10);
            tracker.observeClientAfterAuthoritativeState(KEY, Presence.PRESENT, 10);
            assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState(KEY));

            for (int step = 0; step < 100; step++) {
                Presence localPresence = random.nextBoolean() ? Presence.PRESENT : Presence.ABSENT;
                if (random.nextBoolean()) {
                    tracker.receiveClientMutation(KEY, localPresence);
                } else {
                    tracker.markClientActionPending(KEY);
                }

                assertNotEquals(SyncState.CONFIRMED_GHOST, tracker.getState(KEY));

                // Old/equal server sequence and an attempted pairing must remain stale.
                long staleSequence = random.nextBoolean() ? 9 : 10;
                tracker.receiveAuthoritativeState(KEY, Presence.ABSENT, staleSequence);
                tracker.observeClientAfterAuthoritativeState(KEY, Presence.PRESENT, staleSequence);
                assertNotEquals(
                        SyncState.CONFIRMED_GHOST,
                        tracker.getState(KEY),
                        "stale authority reconfirmed at seed=" + seed + " step=" + step);

                long freshSequence = 11L + step;
                tracker.receiveAuthoritativeState(KEY, Presence.ABSENT, freshSequence);
                assertNotEquals(SyncState.CONFIRMED_GHOST, tracker.getState(KEY));
                tracker.observeClientAfterAuthoritativeState(KEY, Presence.PRESENT, freshSequence);
                assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState(KEY));
            }
        }
    }
}
