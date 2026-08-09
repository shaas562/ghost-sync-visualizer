#!/bin/sh
set -eu

LOOM_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/fabric-loom"
CLIENT_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-clientonly-deobf/26.2/*.jar' | head -n 1)
COMMON_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-common-deobf/26.2/*.jar' | head -n 1)

if [ -z "${CLIENT_JAR:-}" ] || [ -z "${COMMON_JAR:-}" ]; then
    echo 'Minecraft 26.2 deobfuscated jars not found' >&2
    exit 1
fi

CP="$CLIENT_JAR:$COMMON_JAR"

print_sig() {
    class_name="$1"
    printf '\n=== %s ===\n' "$class_name"
    javap -classpath "$CP" -p "$class_name" 2>/dev/null || true
}

print_code() {
    class_name="$1"
    method="$2"
    printf '\n=== bytecode %s.%s ===\n' "$class_name" "$method"
    javap -classpath "$CP" -c -p "$class_name" 2>/dev/null \
        | sed -n "/${method}(/,/^[[:space:]]*\(public\|private\|protected\) /p" \
        | head -n 500 || true
}

for class_name in \
    net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket \
    net.minecraft.client.multiplayer.ClientPacketListener \
    net.minecraft.client.gui.screens.inventory.AbstractContainerScreen \
    net.minecraft.world.inventory.AbstractContainerMenu \
    net.minecraft.client.gui.GuiGraphicsExtractor
do
    print_sig "$class_name"
done

printf '\n=== cursor/item-related AbstractContainerScreen members ===\n'
javap -classpath "$CP" -p net.minecraft.client.gui.screens.inventory.AbstractContainerScreen 2>/dev/null \
    | grep -i -E 'cursor|carried|floating|item|extract' || true

printf '\n=== cursor-related AbstractContainerMenu members ===\n'
javap -classpath "$CP" -p net.minecraft.world.inventory.AbstractContainerMenu 2>/dev/null \
    | grep -i -E 'carried|cursor|stateId' || true

print_code net.minecraft.client.multiplayer.ClientPacketListener handleSetCursorItem
print_code net.minecraft.client.gui.screens.inventory.AbstractContainerScreen extractRenderState
print_code net.minecraft.client.gui.screens.inventory.AbstractContainerScreen extractFloatingItem
print_code net.minecraft.client.gui.screens.inventory.AbstractContainerScreen extractCursorItem
print_code net.minecraft.client.gui.screens.inventory.AbstractContainerScreen extractSlot
