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
        | head -n 420 || true
}

printf '\n=== render-region candidate classes ===\n'
jar tf "$MC_JAR" | grep -E 'net/minecraft/client/renderer/chunk/.*(RenderSectionRegion|SectionRegion|RegionBuilder).*\.class$' | head -n 120 || true

for class_name in \
    net.minecraft.client.Minecraft \
    net.minecraft.client.renderer.GameRenderer \
    net.minecraft.client.Camera \
    net.minecraft.client.renderer.LevelRenderer \
    net.minecraft.client.renderer.ViewArea \
    net.minecraft.client.RotatingSectionStorage \
    net.minecraft.client.renderer.chunk.SectionRenderDispatcher \
    'net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection' \
    net.minecraft.client.renderer.chunk.RenderSectionRegion \
    net.minecraft.client.renderer.chunk.RenderSectionRegionBuilder \
    net.minecraft.client.renderer.chunk.SectionCompiler \
    com.mojang.blaze3d.vertex.QuadInstance \
    com.mojang.blaze3d.vertex.VertexConsumer
do
    print_sig "$class_name"
done

printf '\n=== camera-related Minecraft/GameRenderer members ===\n'
javap -classpath "$CP" -p net.minecraft.client.Minecraft 2>/dev/null | grep -i -E 'camera|blockColors' || true
javap -classpath "$CP" -p net.minecraft.client.renderer.GameRenderer 2>/dev/null | grep -i camera || true

printf '\n=== LevelRenderer bytecode around compileAsync ===\n'
javap -classpath "$CP" -c -p net.minecraft.client.renderer.LevelRenderer 2>/dev/null \
    | grep -B 60 -A 100 'compileAsync' \
    | head -n 360 || true

printf '\n=== LevelRenderer bytecode around RenderSectionRegion ===\n'
javap -classpath "$CP" -c -p net.minecraft.client.renderer.LevelRenderer 2>/dev/null \
    | grep -B 60 -A 100 'RenderSectionRegion' \
    | head -n 420 || true

print_code net.minecraft.client.renderer.chunk.SectionRenderDispatcher rebuildSectionSync
print_code 'net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection' compileAsync
print_code 'net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection' compileSync
print_code com.mojang.blaze3d.vertex.QuadInstance multiplyColor
print_code com.mojang.blaze3d.vertex.QuadInstance scaleColor

printf '\n=== diagnostics revision: camera-accessor-v2 ===\n'
