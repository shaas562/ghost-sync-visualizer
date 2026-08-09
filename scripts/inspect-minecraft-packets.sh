#!/bin/sh
set -eu

CACHE_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}"
PATTERN='net/minecraft/(client/multiplayer/ClientPacketListener|network/protocol/game/Clientbound[^/]*(Block|Section|Chunk|Container|Slot)[^/]*)\.class$'

found=0
find "$CACHE_ROOT" -type f -name '*.jar' 2>/dev/null | while IFS= read -r jar_file; do
    matches=$(jar tf "$jar_file" 2>/dev/null | grep -E "$PATTERN" || true)
    if [ -n "$matches" ]; then
        found=1
        printf '\n=== %s ===\n' "$jar_file"
        printf '%s\n' "$matches"
    fi
done

printf '\n=== Candidate signatures ===\n'
for class_name in \
    net.minecraft.client.multiplayer.ClientPacketListener \
    net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket \
    net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket \
    net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket \
    net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket \
    net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
do
    class_path=$(printf '%s' "$class_name" | tr '.' '/')
    class_jar=''
    for jar_file in $(find "$CACHE_ROOT" -type f -name '*.jar' 2>/dev/null); do
        if jar tf "$jar_file" 2>/dev/null | grep -q "^${class_path}\.class$"; then
            class_jar="$jar_file"
            break
        fi
    done

    if [ -n "$class_jar" ]; then
        printf '\n--- %s ---\n' "$class_name"
        javap -classpath "$class_jar" -public "$class_name" 2>/dev/null || true
    else
        printf '\n--- %s: not found under expected name ---\n' "$class_name"
    fi
done
