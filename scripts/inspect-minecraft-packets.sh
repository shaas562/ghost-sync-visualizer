#!/bin/sh
set -eu

GRADLE_HOME="${GRADLE_USER_HOME:-/home/gradle/.gradle}"
LOOM_CACHE="$GRADLE_HOME/caches/fabric-loom"
PATTERN='net/minecraft/(client/multiplayer/ClientPacketListener|network/protocol/game/Clientbound[^/]*(Block|Section|Chunk|Container|Slot|Inventory)[^/]*)\.class$'

if [ ! -d "$LOOM_CACHE" ]; then
    echo "Fabric Loom cache not found at $LOOM_CACHE" >&2
    exit 1
fi

printf '=== Matching Minecraft 26.2 synchronization classes ===\n'
find "$LOOM_CACHE" -type f -name '*.jar' 2>/dev/null | while IFS= read -r jar_file; do
    matches=$(jar tf "$jar_file" 2>/dev/null | grep -E "$PATTERN" || true)
    if [ -n "$matches" ]; then
        printf '\n=== %s ===\n' "$jar_file"
        printf '%s\n' "$matches"
    fi
done

find_class_jar() {
    class_path="$1"
    find "$LOOM_CACHE" -type f -name '*.jar' 2>/dev/null | while IFS= read -r jar_file; do
        if jar tf "$jar_file" 2>/dev/null | grep -q "^${class_path}\.class$"; then
            printf '%s\n' "$jar_file"
            break
        fi
    done
}

print_signature() {
    class_name="$1"
    class_path=$(printf '%s' "$class_name" | tr '.' '/')
    class_jar=$(find_class_jar "$class_path" || true)

    if [ -n "$class_jar" ]; then
        printf '\n--- %s ---\n' "$class_name"
        javap -classpath "$class_jar" -public "$class_name" 2>/dev/null || true
    else
        printf '\n--- %s: not found under expected name ---\n' "$class_name"
    fi
}

printf '\n=== Candidate signatures ===\n'
for class_name in \
    net.minecraft.client.multiplayer.ClientPacketListener \
    net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket \
    net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket \
    net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket \
    net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket \
    net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket \
    net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket \
    net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket \
    net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket \
    net.minecraft.network.protocol.game.ClientboundOpenScreenPacket \
    net.minecraft.network.protocol.game.ClientboundContainerClosePacket
do
    print_signature "$class_name"
done

printf '\n=== Block acknowledgement bytecode ===\n'
listener_class='net.minecraft.client.multiplayer.ClientPacketListener'
listener_path=$(printf '%s' "$listener_class" | tr '.' '/')
listener_jar=$(find_class_jar "$listener_path" || true)
if [ -n "$listener_jar" ]; then
    javap -classpath "$listener_jar" -c -p "$listener_class" 2>/dev/null \
        | sed -n '/handleBlockChangedAck(/,/^[[:space:]]*public /p' \
        | head -n 120 || true
fi

level_class='net.minecraft.client.multiplayer.ClientLevel'
level_path=$(printf '%s' "$level_class" | tr '.' '/')
level_jar=$(find_class_jar "$level_path" || true)
if [ -n "$level_jar" ]; then
    javap -classpath "$level_jar" -c -p "$level_class" 2>/dev/null \
        | sed -n '/handleBlockChangedAck(/,/^[[:space:]]*public /p' \
        | head -n 160 || true
fi
