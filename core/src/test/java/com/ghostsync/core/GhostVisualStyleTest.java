package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GhostVisualStyleTest {
    @Test
    void zeroTransparencyKeepsOriginalFullyOpaque() {
        GhostVisualStyle style = new GhostVisualStyle(0.4, 0.0);
        assertEquals(1.0, style.modelAlpha());
        assertEquals(0.4, style.whiteOverlayAlpha());
    }

    @Test
    void fullTransparencyMakesOriginalInvisibleWithoutChangingOverlayStrength() {
        GhostVisualStyle style = new GhostVisualStyle(0.25, 1.0);
        assertEquals(0.0, style.modelAlpha());
        assertEquals(0.25, style.whiteOverlayAlpha());
    }

    @Test
    void slidersAreIndependent() {
        GhostVisualStyle style = new GhostVisualStyle(0.8, 0.3);
        assertEquals(0.7, style.modelAlpha(), 1.0e-9);
        assertEquals(0.8, style.whiteOverlayAlpha(), 1.0e-9);
    }

    @Test
    void invalidAndOutOfRangeValuesAreClamped() {
        GhostVisualStyle style = new GhostVisualStyle(2.0, -1.0);
        assertEquals(1.0, style.whiteOverlayAlpha());
        assertEquals(1.0, style.modelAlpha());

        GhostVisualStyle invalid = new GhostVisualStyle(Double.NaN, Double.POSITIVE_INFINITY);
        assertEquals(0.0, invalid.whiteOverlayAlpha());
        assertEquals(1.0, invalid.modelAlpha());
    }

    @Test
    void bothEffectsAtZeroAreInvisibleToGhostRendering() {
        GhostVisualStyle style = new GhostVisualStyle(0.0, 1.0);
        assertFalse(style.visible());
        assertTrue(new GhostVisualStyle(0.01, 1.0).visible());
        assertTrue(new GhostVisualStyle(0.0, 0.99).visible());
    }
}
