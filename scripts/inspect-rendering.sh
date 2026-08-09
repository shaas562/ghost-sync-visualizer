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

for class_name in \
    net.minecraft.client.renderer.ViewArea \
    net.minecraft.client.RotatingSectionStorage \
    'net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection' \
    net.minecraft.client.renderer.chunk.SectionCompiler \
    com.mojang.blaze3d.vertex.QuadInstance \
    com.mojang.blaze3d.vertex.VertexConsumer \
    com.mojang.blaze3d.vertex.VertexFormatElement \
    com.mojang.blaze3d.vertex.DefaultVertexFormat
do
    print_sig "$class_name"
done

print_code com.mojang.blaze3d.vertex.QuadInstance '<init>'
print_code com.mojang.blaze3d.vertex.QuadInstance multiplyColor
print_code com.mojang.blaze3d.vertex.QuadInstance scaleColor
print_code com.mojang.blaze3d.vertex.VertexConsumer 'setColor(float'
print_code net.minecraft.client.renderer.ViewArea setDirty
print_code net.minecraft.client.renderer.ViewArea getRenderSectionAt
print_code net.minecraft.client.RotatingSectionStorage setDirty
