package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GhostTrackerTest {
    private GhostTracker<String> tracker;

    @BeforeEach
    void setUp() {
        tracker = new GhostTracker<>();
    }

    @Test
    void unknownUntilBothSidesAreKnown() {
        assertEquals(SyncState.UNKNOWN, tracker.getState("slot-5"));

        tracker.receiveAuthoritativeState("slot-5", Presence.ABSENT, 1);
        assertEquals(SyncState.UNKNOWN, tracker.getState("slot-5"));
    }

    @Test
    void matchingAbsentStatesAreMatched() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientState("block", Presence.ABSENT);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void matchingPresentStatesAreMatched() {
        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 1);
        tracker.observeClientState("block", Presence.PRESENT);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void localMutationRequiresNewAuthoritativeEvidence() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientState("block", Presence.ABSENT);

        tracker.receiveClientMutation("block", Presence.PRESENT);

        assertEquals(SyncState.PENDING, tracker.getState("block"));
    }

    @Test
    void freshServerAbsenceConfirmsGhost() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientState("block", Presence.ABSENT);
        tracker.receiveClientMutation("block", Presence.PRESENT);

        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 2);
        tracker.observeClientState("block", Presence.PRESENT);

        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("block"));
    }

    @Test
    void freshServerPresenceResolvesPendingMutation() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientState("block", Presence.ABSENT);
        tracker.receiveClientMutation("block", Presence.PRESENT);

        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 2);
        tracker.observeClientState("block", Presence.PRESENT);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void confirmedGhostClearsImmediatelyWhenClientBecomesAbsent() {
        tracker.receiveClientMutation("slot", Presence.PRESENT);
        tracker.receiveAuthoritativeState("slot", Presence.ABSENT, 1);
        tracker.observeClientState("slot", Presence.PRESENT);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("slot"));

        tracker.observeClientState("slot", Presence.ABSENT);
        assertEquals(SyncState.MATCHED, tracker.getState("slot"));
    }

    @Test
    void elapsedTimeCannotConfirmAnything() {
        tracker.receiveAuthoritativeState("slot", Presence.ABSENT, 1);
        tracker.observeClientState("slot", Presence.ABSENT);
        tracker.receiveClientMutation("slot", Presence.PRESENT);

        for (int i = 0; i < 100_000; i++) {
            assertEquals(SyncState.PENDING, tracker.getState("slot"));
        }
    }

    @Test
    void pendingActionRequiresSubsequentServerEvidence() {
        tracker.receiveAuthoritativeState("slot", Presence.PRESENT, 1);
        tracker.observeClientState("slot", Presence.PRESENT);
        tracker.markClientActionPending("slot");

        assertEquals(SyncState.PENDING, tracker.getState("slot"));

        tracker.receiveAuthoritativeState("slot", Presence.PRESENT, 2);
        assertEquals(SyncState.MATCHED, tracker.getState("slot"));
    }

    @Test
    void reverseMismatchIsNotCalledGhost() {
        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 1);
        tracker.observeClientState("block", Presence.ABSENT);

        assertEquals(SyncState.UNKNOWN, tracker.getState("block"));
    }

    @Test
    void staleRevisionCannotOverrideNewerAuthoritativeState() {
        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 10);
        tracker.observeClientState("block", Presence.PRESENT);
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 9);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void invalidationRemovesState() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientState("block", Presence.PRESENT);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("block"));

        tracker.invalidate("block");
        assertEquals(SyncState.UNKNOWN, tracker.getState("block"));
        assertEquals(0, tracker.trackedEntryCount());
    }

    @Test
    void resetClearsAllWorldState() {
        tracker.receiveAuthoritativeState("a", Presence.ABSENT, 1);
        tracker.observeClientState("a", Presence.PRESENT);
        tracker.receiveAuthoritativeState("b", Presence.PRESENT, 1);
        tracker.observeClientState("b", Presence.PRESENT);

        tracker.reset();

        assertEquals(SyncState.UNKNOWN, tracker.getState("a"));
        assertEquals(SyncState.UNKNOWN, tracker.getState("b"));
        assertEquals(0, tracker.trackedEntryCount());
    }
}
