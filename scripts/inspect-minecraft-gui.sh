#!/bin/sh
set -eu

GRADLE_HOME="${GRADLE_USER_HOME:-/home/gradle/.gradle}"
LOOM_CACHE="$GRADLE_HOME/caches/fabric-loom"

find_class_jar() {
    class_path="$1"
    find "$LOOM_CACHE" -type f -name '*.jar' 2>/dev/null | while IFS= read -r jar_file; do
        if jar tf "$jar_file" 2>/dev/null | grep -q "^${class_path}\.class$"; then
            printf '%s\n' "$jar_file"
            break
        fi
    done
}

for class_name in \
    net.minecraft.client.gui.screens.inventory.AbstractContainerScreen \
    net.minecraft.client.gui.GuiGraphicsExtractor \
    net.minecraft.world.inventory.Slot \
    net.minecraft.world.inventory.AbstractContainerMenu \
    net.minecraft.world.entity.player.Inventory
do
    class_path=$(printf '%s' "$class_name" | tr '.' '/')
    class_jar=$(find_class_jar "$class_path" || true)
    printf '\n=== %s ===\n' "$class_name"
    if [ -n "$class_jar" ]; then
        javap -classpath "$class_jar" -p "$class_name" 2>/dev/null || true
    else
        echo 'not found'
    fi
done
