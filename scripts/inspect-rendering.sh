#!/bin/sh
set -eu

LOOM_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/fabric-loom"
MC_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-clientonly-deobf/26.2/*.jar' | head -n 1)
FABRIC_JAR=$(find "${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/modules-2/files-2.1" -type f -name 'fabric-renderer-api-v1-*.jar' | head -n 1 || true)

if [ -z "${MC_JAR:-}" ]; then
    echo 'Minecraft 26.2 deobfuscated client jar not found' >&2
    exit 1
fi

print_sig() {
    class_name="$1"
    printf '\n=== %s ===\n' "$class_name"
    javap -classpath "$MC_JAR${FABRIC_JAR:+:$FABRIC_JAR}" -p "$class_name" 2>/dev/null || true
}

print_code() {
    class_name="$1"
    method="$2"
    printf '\n=== bytecode %s.%s ===\n' "$class_name" "$method"
    javap -classpath "$MC_JAR${FABRIC_JAR:+:$FABRIC_JAR}" -c -p "$class_name" 2>/dev/null \
        | sed -n "/${method}(/,/^[[:space:]]*\(public\|private\|protected\) /p" \
        | head -n 420 || true
}

for class_name in \
    net.minecraft.client.renderer.block.BlockQuadOutput \
    net.minecraft.client.renderer.block.ModelBlockRenderer \
    net.minecraft.client.renderer.block.BlockStateModelSet \
    net.minecraft.client.renderer.block.dispatch.BlockStateModel \
    net.minecraft.client.renderer.block.dispatch.BlockStateModelPart \
    net.minecraft.client.renderer.chunk.SectionCompiler \
    net.minecraft.client.renderer.chunk.RenderSectionRegion \
    net.minecraft.client.renderer.chunk.ChunkSectionLayer \
    com.mojang.blaze3d.vertex.QuadInstance \
    net.minecraft.client.resources.model.geometry.BakedQuad \
    net.fabricmc.fabric.api.client.renderer.v1.render.ChunkSectionLayerMap \
    net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel
do
    print_sig "$class_name"
done

print_code net.minecraft.client.renderer.chunk.SectionCompiler compile
print_code net.minecraft.client.renderer.block.ModelBlockRenderer tesselateBlock
