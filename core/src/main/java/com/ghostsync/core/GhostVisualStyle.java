package com.ghostsync.core;

/**
 * Renderer-independent visual settings for a confirmed ghost.
 *
 * @param whiteOverlayStrength 0 = original color, 1 = fully white tint
 * @param transparencyStrength 0 = fully opaque, 1 = fully transparent
 */
public record GhostVisualStyle(double whiteOverlayStrength, double transparencyStrength) {
    public GhostVisualStyle {
        whiteOverlayStrength = clamp01(whiteOverlayStrength);
        transparencyStrength = clamp01(transparencyStrength);
    }

    /** Alpha used for the original textured model. */
    public double modelAlpha() {
        return 1.0 - transparencyStrength;
    }

    /** Alpha used for a white model-shaped overlay pass. */
    public double whiteOverlayAlpha() {
        return whiteOverlayStrength;
    }

    public boolean visible() {
        return modelAlpha() > 0.0 || whiteOverlayAlpha() > 0.0;
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
