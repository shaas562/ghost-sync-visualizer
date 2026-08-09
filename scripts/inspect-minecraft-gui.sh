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
        | head -n 700 || true
}

printf '\n=== GUI/item state candidate classes ===\n'
jar tf "$MC_JAR" | grep -E 'net/minecraft/client/renderer/state/gui/.*Item.*\.class$|net/minecraft/client/gui/render/.*Item.*\.class$' | head -n 200 || true

for class_name in \
    net.minecraft.client.renderer.item.ItemStackRenderState \
    'net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState' \
    net.minecraft.client.renderer.item.ItemModelResolver \
    net.minecraft.client.gui.GuiGraphicsExtractor \
    net.minecraft.client.renderer.state.gui.GuiRenderState \
    net.minecraft.client.renderer.state.gui.GuiItemRenderState \
    net.minecraft.client.gui.render.GuiRenderer \
    net.minecraft.client.gui.render.GuiItemAtlas \
    'net.minecraft.client.gui.render.GuiItemAtlas$SlotView' \
    net.fabricmc.fabric.api.client.renderer.v1.render.FabricLayerRenderState \
    net.fabricmc.fabric.api.client.renderer.v1.render.FabricItemStackRenderState
do
    print_sig "$class_name"
done

print_code net.minecraft.client.renderer.item.ItemStackRenderState submit
print_code 'net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState' submit

printf '\n=== full private GUI item extraction path ===\n'
javap -classpath "$CP" -c -p net.minecraft.client.gui.GuiGraphicsExtractor 2>/dev/null \
    | sed -n '/private void item(net.minecraft.world.entity.LivingEntity, net.minecraft.world.level.Level, net.minecraft.world.item.ItemStack, int, int, int);/,/public void fakeItem/p' \
    | head -n 800 || true

printf '\n=== GuiRenderState bytecode around item states ===\n'
javap -classpath "$CP" -c -p net.minecraft.client.renderer.state.gui.GuiRenderState 2>/dev/null \
    | grep -B 80 -A 140 -E 'ItemStackRenderState|GuiItem|ItemRender' \
    | head -n 700 || true

print_code net.minecraft.client.gui.render.GuiRenderer renderItem
print_code net.minecraft.client.gui.render.GuiRenderer prepareItem
print_code net.minecraft.client.gui.render.GuiRenderer prepareItemInitially
print_code net.minecraft.client.gui.render.GuiRenderer render
print_code net.minecraft.client.gui.render.GuiItemAtlas prepareItem
print_code net.minecraft.client.gui.render.GuiItemAtlas renderItem

printf '\n=== GuiRenderer bytecode around GuiItemAtlas ===\n'
javap -classpath "$CP" -c -p net.minecraft.client.gui.render.GuiRenderer 2>/dev/null \
    | grep -B 120 -A 180 -E 'GuiItemAtlas|SlotView|GuiItemRenderState' \
    | head -n 1000 || true
