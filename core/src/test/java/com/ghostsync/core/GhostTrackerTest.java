package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    void matchingAbsentStatesAreMatchedAfterServerApply() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void matchingPresentStatesAreMatchedAfterServerApply() {
        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 1);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void localMutationRequiresNewAuthoritativeEvidence() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);

        tracker.receiveClientMutation("block", Presence.PRESENT);

        assertEquals(SyncState.PENDING, tracker.getState("block"));
    }

    @Test
    void freshServerAbsenceWithPairedComparisonConfirmsGhost() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.receiveClientMutation("block", Presence.PRESENT);

        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 2);
        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 2);

        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("block"));
    }

    @Test
    void freshServerAbsenceWithoutPairedComparisonDoesNotConfirmGhost() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.receiveClientMutation("block", Presence.PRESENT);

        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 2);

        assertEquals(SyncState.PENDING, tracker.getState("block"));
    }

    @Test
    void backgroundMismatchNeverConfirmsAgainstOldServerSnapshot() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);

        tracker.observeClientState("block", Presence.PRESENT);

        assertEquals(SyncState.PENDING, tracker.getState("block"));
    }

    @Test
    void freshServerPresenceResolvesPendingMutation() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.receiveClientMutation("block", Presence.PRESENT);

        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 2);
        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 2);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void confirmedGhostStopsRenderingImmediatelyWhenClientDisappears() {
        tracker.receiveClientMutation("slot", Presence.PRESENT);
        tracker.receiveAuthoritativeState("slot", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.PRESENT, 1);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("slot"));

        tracker.observeClientState("slot", Presence.ABSENT);

        assertNotEquals(SyncState.CONFIRMED_GHOST, tracker.getState("slot"));
    }

    @Test
    void elapsedTimeCannotConfirmAnything() {
        tracker.receiveAuthoritativeState("slot", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.ABSENT, 1);
        tracker.receiveClientMutation("slot", Presence.PRESENT);

        for (int i = 0; i < 100_000; i++) {
            assertEquals(SyncState.PENDING, tracker.getState("slot"));
        }
    }

    @Test
    void pendingActionRequiresSubsequentServerEvidence() {
        tracker.receiveAuthoritativeState("slot", Presence.PRESENT, 1);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.PRESENT, 1);
        tracker.markClientActionPending("slot");

        assertEquals(SyncState.PENDING, tracker.getState("slot"));

        tracker.receiveAuthoritativeState("slot", Presence.PRESENT, 2);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.PRESENT, 2);
        assertEquals(SyncState.MATCHED, tracker.getState("slot"));
    }

    @Test
    void duplicateServerSequenceCannotSatisfyPendingAction() {
        tracker.receiveAuthoritativeState("slot", Presence.ABSENT, 5);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.ABSENT, 5);
        tracker.receiveClientMutation("slot", Presence.PRESENT);

        tracker.receiveAuthoritativeState("slot", Presence.ABSENT, 5);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.PRESENT, 5);

        assertEquals(SyncState.PENDING, tracker.getState("slot"));
    }

    @Test
    void stalePostApplyComparisonIsIgnored() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 10);
        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 11);

        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 10);
        assertEquals(SyncState.UNKNOWN, tracker.getState("block"));

        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 11);
        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void reverseMismatchIsNotCalledGhost() {
        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);

        assertEquals(SyncState.UNKNOWN, tracker.getState("block"));
    }

    @Test
    void staleServerSequenceCannotOverrideNewerAuthoritativeState() {
        tracker.receiveAuthoritativeState("block", Presence.PRESENT, 10);
        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 10);
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 9);

        assertEquals(SyncState.MATCHED, tracker.getState("block"));
    }

    @Test
    void invalidationRemovesState() {
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 1);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("block"));

        tracker.invalidate("block");
        assertEquals(SyncState.UNKNOWN, tracker.getState("block"));
        assertEquals(0, tracker.trackedEntryCount());
    }

    @Test
    void resetClearsAllWorldState() {
        tracker.receiveAuthoritativeState("a", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("a", Presence.PRESENT, 1);
        tracker.receiveAuthoritativeState("b", Presence.PRESENT, 1);
        tracker.observeClientAfterAuthoritativeState("b", Presence.PRESENT, 1);

        tracker.reset();

        assertEquals(SyncState.UNKNOWN, tracker.getState("a"));
        assertEquals(SyncState.UNKNOWN, tracker.getState("b"));
        assertEquals(0, tracker.trackedEntryCount());
    }
}
