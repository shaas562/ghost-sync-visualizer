package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServerSequenceCounterTest {
    @Test
    void sequencesAreStrictlyIncreasing() {
        ServerSequenceCounter counter = new ServerSequenceCounter();

        assertEquals(0, counter.current());
        assertEquals(1, counter.next());
        assertEquals(2, counter.next());
        assertEquals(2, counter.current());
    }
}
