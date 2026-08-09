package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NormalizedKeyTest {
    @Test
    void worldEpochSeparatesSameCoordinatesAcrossReloads() {
        BlockKey first = new BlockKey(3, 10, "minecraft:overworld", 4, 64, 8);
        BlockKey second = new BlockKey(3, 11, "minecraft:overworld", 4, 64, 8);

        assertNotEquals(first, second);
    }

    @Test
    void connectionEpochSeparatesSameContainerAcrossReconnects() {
        SlotKey first = new SlotKey(5, 20, 7, 12);
        SlotKey second = new SlotKey(6, 20, 7, 12);

        assertNotEquals(first, second);
    }

    @Test
    void identicalNormalizedKeysRemainEqual() {
        assertEquals(
                new BlockKey(1, 2, "minecraft:the_nether", -2, 80, 99),
                new BlockKey(1, 2, "minecraft:the_nether", -2, 80, 99));
        assertEquals(new SlotKey(1, 9, 4, 0), new SlotKey(1, 9, 4, 0));
    }

    @Test
    void invalidKeyInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new BlockKey(-1, 0, "minecraft:overworld", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BlockKey(0, -1, "minecraft:overworld", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BlockKey(0, 0, " ", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SlotKey(0, 0, 1, -1));
    }
}
