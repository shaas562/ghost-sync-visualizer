#!/bin/sh
set -eu

LOOM_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/fabric-loom"
MC_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-clientonly-deobf/26.2/*.jar' | head -n 1)
FABRIC_RENDERER=$(find "${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/modules-2/files-2.1" -type f -name 'fabric-renderer-api-v1-*.jar' | head -n 1 || true)

if [ -z "${MC_JAR:-}" ]; then
    echo 'Minecraft 26.2 client jar not found' >&2
    exit 1
fi

CP="$MC_JAR${FABRIC_RENDERER:+:$FABRIC_RENDERER}"

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
    net.minecraft.client.renderer.item.ItemStackRenderState \
    'net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState' \
    net.minecraft.client.renderer.item.ItemModelResolver \
    net.minecraft.client.renderer.item.ItemRenderer \
    net.minecraft.client.renderer.ItemInHandRenderer \
    net.minecraft.client.gui.GuiGraphicsExtractor \
    net.minecraft.client.gui.screens.inventory.AbstractContainerScreen \
    net.fabricmc.fabric.api.client.renderer.v1.render.FabricLayerRenderState \
    net.fabricmc.fabric.api.client.renderer.v1.render.FabricItemStackRenderState
do
    print_sig "$class_name"
done

print_code net.minecraft.client.renderer.item.ItemModelResolver update
print_code net.minecraft.client.renderer.item.ItemRenderer renderItem
print_code net.minecraft.client.renderer.ItemInHandRenderer renderArmWithItem
print_code net.minecraft.client.gui.GuiGraphicsExtractor item
