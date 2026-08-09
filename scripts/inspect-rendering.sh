#!/bin/sh
set -eu

LOOM_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/fabric-loom"
MC_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-clientonly-deobf/26.2/*.jar' | head -n 1)
FABRIC_JAR=$(find "${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/modules-2/files-2.1" -type f -name 'fabric-renderer-api-v1-*.jar' | head -n 1 || true)

if [ -z "${MC_JAR:-}" ]; then
    echo 'Minecraft 26.2 deobfuscated client jar not found' >&2
    exit 1
fi

CP="$MC_JAR${FABRIC_JAR:+:$FABRIC_JAR}"

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
        | head -n 360 || true
}

for class_name in \
    net.minecraft.client.renderer.LevelRenderer \
    net.minecraft.client.renderer.chunk.SectionRenderDispatcher \
    'net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection' \
    net.minecraft.client.renderer.chunk.SectionCompiler \
    net.minecraft.client.renderer.chunk.ChunkSectionLayer \
    net.minecraft.client.resources.model.geometry.BakedQuad \
    'net.minecraft.client.resources.model.geometry.BakedQuad$MaterialInfo' \
    com.mojang.blaze3d.vertex.QuadInstance \
    com.mojang.blaze3d.vertex.VertexConsumer \
    net.minecraft.util.ARGB \
    net.minecraft.util.FastColor \
    'net.minecraft.util.FastColor$ARGB32'
do
    print_sig "$class_name"
done

print_code com.mojang.blaze3d.vertex.QuadInstance setColor
print_code com.mojang.blaze3d.vertex.VertexConsumer putBlockBakedQuad
print_code net.minecraft.client.renderer.LevelRenderer setSectionDirty
print_code net.minecraft.client.renderer.LevelRenderer setBlockDirty
print_code net.minecraft.client.renderer.chunk.SectionCompiler 'lambda$compile$0'
