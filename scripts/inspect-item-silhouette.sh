#!/bin/sh
set -eu

LOOM_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/fabric-loom"
MC_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-clientonly-deobf/26.2/*.jar' | head -n 1)
COMMON_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-common-deobf/26.2/*.jar' | head -n 1)

if [ -z "${MC_JAR:-}" ] || [ -z "${COMMON_JAR:-}" ]; then
    echo 'Minecraft 26.2 deobfuscated jars not found' >&2
    exit 1
fi

CP="$MC_JAR:$COMMON_JAR"

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
        | head -n 900 || true
}

printf '\n=== Blit/pipeline candidates ===\n'
jar tf "$MC_JAR" | grep -E '(BlitRenderState|RenderPipeline|RenderPipelines|GuiItemAtlas|GuiRenderer).*\.class$' | head -n 240 || true

for class_name in \
    net.minecraft.client.gui.render.GuiRenderer \
    net.minecraft.client.gui.render.GuiItemAtlas \
    'net.minecraft.client.gui.render.GuiItemAtlas$SlotView' \
    net.minecraft.client.gui.render.state.BlitRenderState \
    net.minecraft.client.renderer.state.gui.BlitRenderState \
    net.minecraft.client.renderer.RenderPipelines \
    com.mojang.blaze3d.pipeline.RenderPipeline \
    'com.mojang.blaze3d.pipeline.RenderPipeline$Builder' \
    'com.mojang.blaze3d.pipeline.RenderPipeline$Snippet' \
    com.mojang.blaze3d.pipeline.RenderTarget \
    com.mojang.blaze3d.systems.RenderPass
do
    print_sig "$class_name"
done

print_code net.minecraft.client.gui.render.GuiRenderer submitBlitFromItemAtlas
print_code net.minecraft.client.gui.render.GuiRenderer renderItem
print_code net.minecraft.client.gui.render.GuiRenderer render

printf '\n=== GUI-related shader resources ===\n'
jar tf "$MC_JAR" | grep -E '^assets/minecraft/shaders/.*(gui|position_tex|blit|rendertype).*' | head -n 260 || true

printf '\n=== GUI-related post/core shader text ===\n'
for resource in $(jar tf "$MC_JAR" | grep -E '^assets/minecraft/shaders/.*(gui|position_tex|blit).*\.(vsh|fsh|glsl|json)$' | head -n 40); do
    printf '\n--- %s ---\n' "$resource"
    unzip -p "$MC_JAR" "$resource" 2>/dev/null | head -n 220 || true
done
