package com.ghostsync.fabric.client.config;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Mod Menu configuration screen. */
public final class GhostSyncConfigScreen extends Screen {
    private final Screen parent;
    private final GhostSyncConfig config;
    private boolean advancedExpanded;
    private Button loggingButton;

    public GhostSyncConfigScreen(Screen parent) {
        super(Component.literal("Ghost Sync Visualizer"));
        this.parent = parent;
        this.config = GhostSyncConfigManager.current();
    }

    @Override
    protected void init() {
        int columnWidth = 200;
        int left = this.width / 2 - 210;
        int right = this.width / 2 + 10;
        int top = 36;
        int row = 24;

        addBooleanButton(left, top, columnWidth, "Master", () -> config.enabled, value -> config.enabled = value);
        addBooleanButton(left, top + row, columnWidth, "Ghost blocks", () -> config.blocks.enabled,
                value -> config.blocks.enabled = value);
        addRenderableWidget(new PercentSlider(left, top + row * 2, columnWidth, "Block white overlay",
                config.blocks.overlayStrength, value -> config.blocks.overlayStrength = value));
        addRenderableWidget(new PercentSlider(left, top + row * 3, columnWidth, "Block transparency",
                config.blocks.transparencyStrength, value -> config.blocks.transparencyStrength = value));
        addRenderableWidget(new IntSlider(left, top + row * 4, columnWidth, "Detection distance", " chunks",
                config.blocks.detectionDistanceChunks, 2, 32,
                value -> config.blocks.detectionDistanceChunks = value));
        addRenderableWidget(new IntSlider(left, top + row * 5, columnWidth, "Performance", " / 5",
                config.blocks.performanceLevel, 1, 5,
                value -> config.blocks.performanceLevel = value));

        addBooleanButton(right, top, columnWidth, "Ghost items", () -> config.items.enabled,
                value -> config.items.enabled = value);
        addRenderableWidget(new PercentSlider(right, top + row, columnWidth, "Item white overlay",
                config.items.overlayStrength, value -> config.items.overlayStrength = value));
        addRenderableWidget(new PercentSlider(right, top + row * 2, columnWidth, "Item transparency",
                config.items.transparencyStrength, value -> config.items.transparencyStrength = value));

        loggingButton = addBooleanButton(right, top + row * 4, columnWidth, "Technical logging",
                () -> config.advanced.loggingEnabled,
                value -> config.advanced.loggingEnabled = value);
        loggingButton.visible = advancedExpanded;

        addRenderableWidget(Button.builder(advancedLabel(), button -> {
                    advancedExpanded = !advancedExpanded;
                    loggingButton.visible = advancedExpanded;
                    button.setMessage(advancedLabel());
                })
                .bounds(right, top + row * 3, columnWidth, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> closeAndSave())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        closeAndSave();
    }

    private void closeAndSave() {
        GhostSyncConfigManager.save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private Button addBooleanButton(
            int x,
            int y,
            int width,
            String label,
            BooleanSupplier getter,
            Consumer<Boolean> setter) {
        return addRenderableWidget(Button.builder(booleanLabel(label, getter.getAsBoolean()), button -> {
                    boolean value = !getter.getAsBoolean();
                    setter.accept(value);
                    button.setMessage(booleanLabel(label, value));
                })
                .bounds(x, y, width, 20)
                .build());
    }

    private Component advancedLabel() {
        return Component.literal("Advanced: " + (advancedExpanded ? "Open" : "Closed"));
    }

    private static Component booleanLabel(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "On" : "Off"));
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private final String label;
        private final DoubleConsumer consumer;

        private PercentSlider(int x, int y, int width, String label, double value, DoubleConsumer consumer) {
            super(x, y, width, 20, Component.empty(), value);
            this.label = label;
            this.consumer = consumer;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + Math.round(value * 100.0) + "%"));
        }

        @Override
        protected void applyValue() {
            consumer.accept(value);
        }
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final String label;
        private final String suffix;
        private final int min;
        private final int max;
        private final IntConsumer consumer;

        private IntSlider(
                int x,
                int y,
                int width,
                String label,
                String suffix,
                int initial,
                int min,
                int max,
                IntConsumer consumer) {
            super(x, y, width, 20, Component.empty(), toSlider(initial, min, max));
            this.label = label;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.consumer = consumer;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + currentValue() + suffix));
        }

        @Override
        protected void applyValue() {
            consumer.accept(currentValue());
        }

        private int currentValue() {
            return min + (int) Math.round(value * (max - min));
        }

        private static double toSlider(int value, int min, int max) {
            return (Math.max(min, Math.min(max, value)) - min) / (double) (max - min);
        }
    }
}
